package gov.takx.core.reporting;

import gov.takx.api.annotation.PseudoBean;
import gov.takx.api.commservices.ICommServiceManager;
import gov.takx.api.commservices.ISerialPortStatus;
import gov.takx.api.connection.IConnectionManager;
import gov.takx.api.gateway.IDeviceGateway;
import gov.takx.api.messages.IMapEntity;
import gov.takx.api.persist.IMapObjectFinder;
import gov.takx.api.plugin.IPluginDefinition;
import gov.takx.api.plugin.PluginCategory;
import gov.takx.api.project.IProject;
import gov.takx.api.project.IProjectManager;
import gov.takx.api.registry.IPluginInspector;
import gov.takx.api.registry.IPluginRegistry;
import gov.takx.core.configuration.ProgramEnvironment;
import gov.takx.core.registry.PluginRegistryManager;
import jakarta.inject.Inject;
import org.apache.commons.lang3.SystemUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileFilter;
import java.io.FileOutputStream;
import java.io.IOException;
import java.lang.invoke.MethodHandles;
import java.lang.management.ManagementFactory;
import java.lang.management.ThreadInfo;
import java.nio.file.Files;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

/**
 * @since 2.8.1 (renamed from RaptorIssueReportDataCollector in 3.4)
 */
@PseudoBean
public class IssueReportDataCollector
{
    private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    private static final String SECTION_HEADER = "\n\n=======================\n";
    private static final String SECTION_TRAILER = "=======================\n\n";

    private static final FileFilter LOG_FILTER =
            f -> !f.isDirectory() && f.getName().toLowerCase().startsWith("takx.log");

    private static final FileFilter VMOPTIONS_FILTER =
            f -> !f.isDirectory() && f.getName().toLowerCase().endsWith(".vmoptions");

    private static final Comparator<IPluginDefinition<?>> PLUGIN_DEFINITION_COMPARATOR =
            Comparator.comparing((IPluginDefinition<?> pd) -> pd.getFamily())
                    .thenComparing(IPluginDefinition::getType);

    @Inject
    private ProgramEnvironment programEnvironment;

    @Inject
    private IPluginInspector pluginInspector;

    @Inject
    private PluginRegistryManager pluginRegistryManager;

    @Inject
    private IProjectManager projectManager;

    @Inject
    private IConnectionManager connectionManager;

    @Inject
    private IDeviceGateway deviceGateway;

    @Inject
    private ICommServiceManager commServiceManager;

    @Inject
    private IMapObjectFinder mapObjectFinderService;

    private File tempDirectory;

    /**
     * Export the configuration data to a zip file.
     *
     * @param zipFile File to write to
     * @param extraConfig Extra info to append to the "standard" configuration listing
     * @param includeProjectInfo Pass {@code true} to include open project info
     * @param includeLogs Pass {@code true} to attach application logs
     * @since 3.5
     */
    public void exportConfiguration(File zipFile, String extraConfig, boolean includeProjectInfo, boolean includeLogs) throws IOException
    {
        if (zipFile.exists() && !zipFile.delete()) logger.error("Failed to delete {}", zipFile);

        File configLog = buildConfigurationFile(includeProjectInfo, extraConfig);

        final ZipOutputStream zos = new ZipOutputStream(new BufferedOutputStream(new FileOutputStream(zipFile)));

        addFileToZip(zos, null, configLog, "Configuration Log");

        if (!configLog.delete()) logger.error("Failed to delete temp log file: {}", configLog);

        // Add log files
        if (includeLogs)
        {
            addFilesToZip(zos, "bin", new File("."), LOG_FILTER);
            addFilesToZip(zos, "project", new File(projectManager.getProjectDirectory()), LOG_FILTER);
        }

        // Add vmoptions file
        addFilesToZip(zos, "bin", new File("."), VMOPTIONS_FILTER);

        zos.flush();
        zos.close();
    }

    /**
     * Collects all required data for an error report into a zip and return the zip file.
     *
     * @return the resulting file
     */
    public File collectErrorReport(String summary, String description) throws IOException
    {
        tempDirectory = Files.createTempDirectory("issueReport").toFile();

        final String timestamp = DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss").format(LocalDateTime.now());
        String filename = "issue_report." + timestamp + ".rpt";
        File zipFile = new File(SystemUtils.getUserHome(), filename);

        if (zipFile.exists() && !zipFile.delete()) logger.error("Failed to delete {}", zipFile);

        File reportFile = buildReportFile(summary, description);
        File configLog = buildConfigurationFile(true, null);
        File threadDump = buildThreadDumpFile();

        final ZipOutputStream zos = new ZipOutputStream(new BufferedOutputStream(new FileOutputStream(zipFile)));

        addFileToZip(zos, null, reportFile, "Issue Report");
        addFileToZip(zos, null, configLog, "Configuration Log");
        addFileToZip(zos, null, threadDump, "thread dump");

        if (!reportFile.delete()) logger.error("Failed to delete issue report file: {}", reportFile);
        if (!configLog.delete()) logger.error("Failed to delete temp log file: {}", configLog);
        if (!threadDump.delete()) logger.error("Failed to delete threadDump: {}", threadDump);

        // Adding log files
        addFilesToZip(zos, "bin", new File("."), LOG_FILTER);
        addFilesToZip(zos, "project", new File(projectManager.getProjectDirectory()), LOG_FILTER);

        // Adding vmoptions file
        addFilesToZip(zos, "bin", new File("."), VMOPTIONS_FILTER);

        zos.flush();
        zos.close();

        return zipFile;
    }

    /**
     * Collect configuration info, including program properties, plugins, connections, etc.
     *
     * @param includeProjectInfo Pass {@code true} to include open project info
     * @return Collected info
     */
    public String collectConfigurationInfo(boolean includeProjectInfo)
    {
        StringBuilder familyListing = new StringBuilder();
        familyListing.append(SECTION_HEADER);
        familyListing.append("Registered Plugin Families\n");
        familyListing.append(SECTION_TRAILER);

        pluginInspector.listInstalledFamilies().stream()
                .sorted()
                .forEach(f -> familyListing.append(f).append("\n"));

        // Process the registries
        StringBuilder pluginListing = new StringBuilder()
                .append(SECTION_HEADER)
                .append("Active Plugin Configuration\n")
                .append(SECTION_TRAILER);

        pluginRegistryManager.allRegistries().stream()
                .filter(registry -> registry.getPluginCategory() != PluginCategory.SyntheticPlugin)
                .sorted(Comparator.comparing(IPluginRegistry::getPluginCategory))
                .forEach(registry -> {
                    pluginListing.append("\n").append(registry.getPluginCategory()).append("\n\n");

                    registry.getPluginDefinitions().stream()
                            .sorted(PLUGIN_DEFINITION_COMPARATOR)
                            .forEach(def -> pluginListing
                                    .append("    ")
                                    .append(def.getFamily())
                                    .append(" | ").append(def.getType())
                                    .append(" | ").append(def.getVersion())
                                    .append('\n'));
                });

        StringBuilder serialStatusListing = new StringBuilder();
        serialStatusListing.append(SECTION_HEADER);
        serialStatusListing.append("Serial Port Listing\n");
        serialStatusListing.append(SECTION_TRAILER);

        for (ISerialPortStatus status : commServiceManager.getSerialPortCommStatus())
        {
            serialStatusListing.append(status.getCommName())
                    .append(" // ").append(status.getBaudRate())
                    .append(" // Echoing? ").append(status.isEchoingData())
                    .append(" // Open? ").append(status.isPortOpen())
                    .append("\n");
        }

        StringBuilder sb = new StringBuilder();

        sb.append(programEnvironment.getProgramProperties());
        sb.append(familyListing);
        sb.append(pluginListing);
        sb.append(serialStatusListing);

        if (includeProjectInfo) sb.append(collectProjectInfo());

        return sb.toString();
    }

    private File buildReportFile(String summary, String description) throws IOException
    {
        File reportFile = new File(tempDirectory, "README.txt");

        String report = "Report Summary\n" +
                "==============\n" +
                summary +
                "\n\n\n" +
                "Report Description\n" +
                "==================\n" +
                description;

        Files.writeString(reportFile.toPath(), report);

        return reportFile;
    }

    private File buildConfigurationFile(boolean includeProjectInfo, String extraConfig) throws IOException
    {
        File configLog = new File(tempDirectory, "configuration.log");
        final String configurationInfo = collectConfigurationInfo(includeProjectInfo);
        final String fullConfig = extraConfig != null ? configurationInfo + '\n' + extraConfig : configurationInfo;
        Files.writeString(configLog.toPath(), fullConfig);

        return configLog;
    }

    private String collectProjectInfo()
    {
        StringBuilder modListing = new StringBuilder();
        modListing.append(SECTION_HEADER);
        modListing.append("Map Object Definitions\n");
        modListing.append(SECTION_TRAILER);

        // Collect all Map Entities
        for (IMapEntity entity : mapObjectFinderService.findAllMapEntities())
        {
            modListing.append(entity.getFamily())
                    .append(" // ").append(entity.getType())
                    .append("\n");
        }

        IProject currentProject = projectManager.getCurrentOpenProject();

        StringBuilder sb = new StringBuilder();
        sb.append(SECTION_HEADER);
        sb.append("Current Project\n");
        sb.append(SECTION_TRAILER);

        sb.append("Project Name: ").append(currentProject.getProjectName()).append("\n");
        sb.append("Project Version: ").append(currentProject.getProjectVersion()).append("\n");
        sb.append("Is New Flag: ").append(currentProject.isNew()).append("\n");

        sb.append(modListing);

        sb.append(SECTION_HEADER);
        sb.append("Instantiated Parsers & Framers\n");
        sb.append(SECTION_TRAILER);

        sb.append("\nPARSERS:\n");
        deviceGateway.getParserKeys().forEach(key -> sb.append(key).append("\n"));

        sb.append("\nFRAMERS:\n");
        deviceGateway.getFramerKeys().forEach(key -> sb.append(key).append("\n"));

        sb.append(SECTION_HEADER);
        sb.append("Instantiated Connection Controllers\n");
        sb.append(SECTION_TRAILER);

        connectionManager.getConnectionControllers().forEach(
                controller -> sb
                        .append(controller.getUniqueKey())
                        .append(" state: ").append(controller.getState())
                        .append("\n"));

        return sb.toString();
    }

    private File buildThreadDumpFile() throws IOException
    {
        final StringBuilder sb = new StringBuilder();

        Map<Thread, StackTraceElement[]> map = Thread.getAllStackTraces();
        List<Thread> threadList = new ArrayList<>(map.keySet());

        threadList.sort(Comparator.comparingLong(Thread::getId));
        long[] threadIds = threadList.stream().mapToLong(Thread::getId).toArray();
        ThreadInfo[] threadInfos = ManagementFactory.getThreadMXBean().getThreadInfo(threadIds);

        for (int i = 0; i < threadIds.length; i++)
        {
            ThreadInfo info = threadInfos[i];
            Thread thread = threadList.get(i);

            String daemon = thread.isDaemon() ? " daemon" : "";
            String lockOwner = info != null && info.getLockOwnerId() > 0L
                    ? " owned by \"" + info.getLockOwnerName() + "\"" + " id=" + info.getLockOwnerId()
                    : "";
            String lockInfo = info != null && info.getLockName() != null
                    ? " (lock=" + info.getLockName() + lockOwner + ")"
                    : "";

            sb.append('"').append(thread.getName()).append('"')
                    .append(daemon)
                    .append(" prio=").append(thread.getPriority())
                    .append(" id=").append(thread.getId())
                    .append(' ').append(thread.getState())
                    .append(lockInfo).append('\n');

            for (StackTraceElement trace : map.get(thread)) sb.append("\tat ").append(trace).append('\n');

            sb.append('\n');
        }

        File dumpFile = new File(tempDirectory, "thread_dump.txt");
        Files.writeString(dumpFile.toPath(), sb);

        return dumpFile;
    }

    /**
     * Add all matching files as entries to the zip file.
     *
     * @param zos Zip file output stream
     * @param prefix Directory name for creating the entry name (to avoid name conflicts for things like takx.log)
     * @param directory Directory to scan for matching files
     * @param filter File filter to select files
     * @since 3.5
     */
    private void addFilesToZip(ZipOutputStream zos, String prefix, File directory, FileFilter filter) throws IOException
    {
        final File[] files = directory.listFiles(filter);
        if (files != null)
        {
            for (File file : files)
            {
                final String comment = "Modified " + Files.getLastModifiedTime(file.toPath());
                addFileToZip(zos, prefix, file, comment);
            }
        }
    }

    /**
     * Add an entry to the zip file with the content taken from the given file.
     *
     * @param zos Zip file output stream
     * @param prefix Directory name for creating the entry name (to avoid name conflicts for things like takx.log)
     * @param file Source file for zip entry content
     * @param comment Comment to add to the zip entry
     * @since 3.5
     */
    private void addFileToZip(ZipOutputStream zos, String prefix, File file, String comment) throws IOException
    {
        logger.debug("Attaching log file {} with comment {}", file, comment);
        final String entryName = (prefix != null ? prefix + "/" : "") + file.getName();
        final ZipEntry ze = new ZipEntry(entryName);
        ze.setComment(comment);
        zos.putNextEntry(ze);
        zos.write(Files.readAllBytes(file.toPath()));
        zos.closeEntry();
    }
}
