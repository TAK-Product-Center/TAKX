package gov.takx.core.view.gui.maps.importers.wizards;

import gov.tak.platform.cot.importer.CotImportHelper;
import gov.takx.api.annotation.InvokeOnEDT;
import gov.takx.api.annotation.PseudoBean;
import gov.takx.api.coordinatesystem.ILocation;
import gov.takx.api.filesystem.IVirtualFileSystem;
import gov.takx.api.logging.SystemLogLevel;
import gov.takx.api.systemmessage.ISystemMessageManager;
import gov.takx.api.systemmessage.SystemMessage;
import gov.takx.api.tasks.ITaskManager;
import gov.takx.api.tasks.ITaskStatusHandler;
import gov.takx.api.tasks.TaskGroup;
import gov.takx.commons.file.FilePath;
import gov.takx.commons.file.FileUtils;
import gov.takx.commons.file.IFileProgress;
import gov.takx.commons.swing.AlwaysOnTopJOptionPane;
import gov.takx.commons.swing.YesNoToAll;
import gov.takx.core.tasks.AManagedTask;
import gov.takx.platform.io.panels.CommonWizardSettings;
import gov.takx.platform.ui.UIResources;
import jakarta.annotation.Nullable;
import jakarta.inject.Inject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.JFrame;
import javax.swing.JOptionPane;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.lang.invoke.MethodHandles;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.HashMap;
import java.util.Map;

import static gov.takx.commons.swing.YesNoToAll.YES;
import static gov.takx.platform.ui.ResourceDirectory.PLATFORM_ICONS;

/**
 * Simple file drop on map.
 */
@PseudoBean
public class FileDropImportWizard extends AFileImportWizard
{
    private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    public static final String TYPE = "File On Globe";

    @Inject
    private ITaskManager taskManager;

    @Inject
    private ISystemMessageManager messageManager;

    @Inject
    private CotImportHelper cotImportHelper;

    /**
     * A reference to the VFS where the files will be placed.
     */
    private IVirtualFileSystem fileSystem;

    /**
     * The location where the files were dropped. May be {@code null}.
     */
    private ILocation location;

    @Override
    public void preconfigureWizard(Map<String, Object> configuration)
    {
        super.preconfigureWizard(configuration);

        fileSystem = (IVirtualFileSystem) configuration.get(CommonWizardSettings.VFS);
        location = (ILocation) configuration.get(CommonWizardSettings.LOCATION);
    }

    @Override
    public String getDisplayName()
    {
        return TYPE;
    }

    @Override
    public URL getIcon()
    {
        return UIResources.getUrl(PLATFORM_ICONS, "files_filled.png");
    }

    @Override
    public String getHelpMessage()
    {
        return "Drops a file onto the globe as a simple file Map Object.";
    }

    @Override
    public boolean offerToProcessFile(String fileExtension, File file)
    {
        // This importer will offer to import ALL files but NO directories.
        return !file.isDirectory();
    }

    @Override
    public boolean supportsMultipleFiles()
    {
        return true;
    }

    @Override
    public String getType()
    {
        return TYPE;
    }

    @Override
    @InvokeOnEDT
    public void runOperation(@Nullable Map<String, Object> preconfigProps)
    {
        YesNoToAll overwriteExistingFile = YES;
        YesNoToAll overrideLocation = YES;

        Map<File, ILocation> filesToReallyDrop = new HashMap<>();
        boolean cotFileImported = false;

        // First, collect all the information on any conflicts (existing files, and files with geo-loc)
        for (final File file : files)
        {
            // CoT files should be handled as if we're importing them
            if (isCotEvent(file))
            {
                try
                {
                    cotImportHelper.parseAndImportCot(Files.readString(file.toPath()));
                    cotFileImported = true;
                    continue;
                } catch (IOException ex)
                {
                    logger.error("Failed importing cot file '{}'", file.getName(), ex);
                    if (AlwaysOnTopJOptionPane.showConfirmDialog(null,
                            "The CoT file " + file.getName() + " couldn't be imported.\n"
                                    + "Would you like to import it as a generic file map object instead?",
                            "Failed importing CoT", JOptionPane.YES_NO_OPTION) != JOptionPane.YES_OPTION)
                    {
                        continue;
                    }
                }
            }

            if (fileSystem.getFile(new FilePath(file.getName())) != null)
            {
                if (!overwriteExistingFile.isToAll())
                {
                    overwriteExistingFile = AlwaysOnTopJOptionPane.showYesNoToAllDialog(JFrame.getFrames()[0],
                            "The file '" + file.getName() + "' already exists. Do you want to overwrite it?",
                            "Overwrite File?");
                }

                if (overwriteExistingFile.isNo()) continue;
            }

            ILocation location = fileSystem.geoDecode(file);

            if (location != null && this.location != null)
            {
                if (!overrideLocation.isToAll())
                {
                    overrideLocation = AlwaysOnTopJOptionPane.showYesNoToAllDialog(JFrame.getFrames()[0],
                            "The file '" + file.getName() + "' contains a location. Do you want to override that location?",
                            "Override Location?");
                }

                if (overrideLocation.isYes()) this.location = location;
            } else if (location == null)
            {
                location = this.location;
            }

            filesToReallyDrop.put(file, location);
        }

        if (cotFileImported) cotImportHelper.importDeferredEvents();

        if (!filesToReallyDrop.isEmpty())
        {
            // Now we are ready to do the actual File import
            for (Map.Entry<File, ILocation> entry : filesToReallyDrop.entrySet())
            {
                final File file = entry.getKey();
                final ILocation location = entry.getValue();

                taskManager.execute(new Copier(file, location));
            }
        }
    }

    /**
     * @return {@code true} if the file extension is {@code cot}, or it is {@code xml} and the content
     *         of the file contains a CoT "event" start element
     * @since 5.4
     */
    private static boolean isCotEvent(File file)
    {
        final String extension = FileUtils.getExtension(file);
        if ("cot".equalsIgnoreCase(extension)) return true;
        if (!"xml".equalsIgnoreCase(extension)) return false;

        try (var inputStream = new FileInputStream(file))
        {
            return new String(inputStream.readNBytes(1024), StandardCharsets.UTF_8).contains("<event ");
        } catch (IOException e)
        {
            logger.error("Failed to read {}", file.getAbsolutePath(), e);
        }
        return false;
    }

    @Override
    @Nullable
    public Map<String, Object> showImporterWizardDialog()
    {
        return null;
    }

    @Override
    public String toString()
    {
        return getDisplayName();
    }

    /**
     * Internal class to perform the actual file copying as a managed background task.
     */
    private class Copier extends AManagedTask
    {
        private final File file;
        private final ILocation location;

        private Copier(File file, ILocation location)
        {
            super(TaskGroup.FILE_COPY, "Import File: " + file.getName());

            this.file = file;
            this.location = location;
        }

        @Override
        public void execute()
        {
            // Redirect file copy progress to our status handler
            final ITaskStatusHandler statusHandler = getTaskStatusHandler();

            statusHandler.printMessage("Copying data to VFS...");

            try
            {
                fileSystem.createFileMapObject(new FilePath(file.getName()), file, location, new IFileProgress()
                {
                    int maximum;

                    @Override
                    public void setFileMaximum(final int maximum)
                    {
                        statusHandler.setMaxProgress(maximum);
                        this.maximum = maximum;
                    }

                    @Override
                    public void setFileMinimum(final int minimum)
                    {
                        statusHandler.setMinProgress(minimum);
                    }

                    @Override
                    public void setFileProgress(final int progress)
                    {
                        statusHandler.setProgress(progress);
                        if (progress == maximum) statusHandler.setComplete();
                    }
                });

                // Now register the system message for our operation

                final String msg = "Import File: " + file.getAbsolutePath(); // Put full path in message
                final SystemMessage sysMsg = new SystemMessage(SystemLogLevel.SYSTEM_INFO, msg);

                if (location != null)
                {
                    statusHandler.setLocationOfInterest(location);
                    sysMsg.addAction(makeGotoAction());
                }

                messageManager.addMessage(sysMsg);
            } catch (IOException e)
            {
                logger.error("Failed to import file: {}", file, e);

                statusHandler.printMessage("Error importing " + file.getName());
                statusHandler.setException(e);

                messageManager.error("Import File: " + file.getAbsolutePath(), e);
            }
        }
    }
}
