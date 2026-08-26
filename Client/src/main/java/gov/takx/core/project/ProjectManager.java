package gov.takx.core.project;

import gov.takx.api.annotation.ProgramPreference;
import gov.takx.api.event.StartBackgroundProjectServices;
import gov.takx.api.event.StartPlugins;
import gov.takx.api.event.StartProjectServices;
import gov.takx.api.event.StopProjectServices;
import gov.takx.api.event.project.ProjectClosed;
import gov.takx.api.event.project.ProjectClosing;
import gov.takx.api.event.project.ProjectOpened;
import gov.takx.api.event.project.ProjectOpening;
import gov.takx.api.event.project.ProjectPackaged;
import gov.takx.api.event.project.ProjectPackaging;
import gov.takx.api.event.project.ProjectSaved;
import gov.takx.api.event.project.ProjectSaving;
import gov.takx.api.platform.IProgressMonitor;
import gov.takx.api.preferences.IPreferenceManager;
import gov.takx.api.preferences.IToolPreferenceManager;
import gov.takx.api.preferences.IUserPreferenceManager;
import gov.takx.api.project.IProject;
import gov.takx.api.project.IProjectFactory;
import gov.takx.api.project.IProjectManager;
import gov.takx.api.project.ProjectConstants;
import gov.takx.api.tasks.ITaskManager;
import gov.takx.api.tasks.ITaskStatusHandler;
import gov.takx.api.tasks.TaskGroup;
import gov.takx.commons.file.CopyDirectoryFileVisitor;
import gov.takx.commons.file.FileUtils;
import gov.takx.commons.file.IZipFileProgress;
import gov.takx.commons.utils.StringUtils;
import gov.takx.core.configuration.ProgramEnvironment;
import gov.takx.core.database.exception.FatalDatabaseException;
import gov.takx.core.database.exception.LoadCancelledException;
import gov.takx.core.registry.PluginRegistryManager;
import gov.takx.core.tasks.AManagedTask;
import gov.takx.core.view.gui.screenimageutils.ProjectSnapshotService;
import gov.takx.platform.logging.NullProgressMonitor;
import gov.takx.platform.preferences.PreferenceEntry;
import gov.takx.platform.preferences.PreferencesNode;
import gov.takx.platform.preferences.ProjectPreferenceEntry;
import gov.takx.platform.preferences.UserPreferenceEntry;
import jakarta.annotation.Nonnull;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.jboss.weld.environment.se.WeldContainer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.lang.invoke.MethodHandles;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

/**
 * Project manager for handling activities related to TAKX Projects.
 *
 */
@ApplicationScoped
public class ProjectManager implements IProjectManager
{
    private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy_MM_dd_HH_mm_ss").withZone(ZoneId.systemDefault());

    private IProjectFactory projectFactory = new DefaultProjectFactory();
    private IProject openProject = null;
    private File projectsDirectory;

    private final ProgramEnvironment programEnvironment;
    private final IPreferenceManager preferenceManager;
    private final IUserPreferenceManager userPreferenceManager;
    private final IToolPreferenceManager toolPreferenceManager;
    private final PluginRegistryManager pluginRegistryManager;
    private final boolean wmsOfflineMode;
    private final WeldContainer container;
    private final ITaskManager taskManager;

    @Inject
    public ProjectManager(ProgramEnvironment programEnvironment, IPreferenceManager preferenceManager,
                          IUserPreferenceManager userPreferenceManager, IToolPreferenceManager toolPreferencesManager,
                          PluginRegistryManager pluginRegistryManager,
                          @ProgramPreference(PreferenceEntry.DEFAULT_WMS_OFFLINE_MODE) boolean wmsOfflineMode,
                          WeldContainer container, ITaskManager taskManager)
    {
        this.programEnvironment = programEnvironment;
        this.preferenceManager = preferenceManager;
        this.userPreferenceManager = userPreferenceManager;
        this.toolPreferenceManager = toolPreferencesManager;
        this.pluginRegistryManager = pluginRegistryManager;
        this.wmsOfflineMode = wmsOfflineMode;
        this.container = container;
        this.taskManager = taskManager;

        final String lastProjectsDir = PreferenceEntry.LAST_PROJECTS_DIR.getStringValue();

        projectsDirectory = lastProjectsDir == null
                ? new File(programEnvironment.getProjectDataDirectory(), "Projects")
                : new File(lastProjectsDir);
    }

    @Override
    public IProject getCurrentOpenProject()
    {
        return openProject;
    }

    @Override
    public boolean isValidSession()
    {
        return openProject != null;
    }

    @Override
    public String getProjectDirectory()
    {
        return openProject != null ? openProject.getProjectDirectory() : null;
    }

    @Override
    public String getProjectName()
    {
        return openProject != null ? openProject.getProjectName() : null;
    }

    @Override
    public UUID getProjectUUID()
    {
        return openProject != null ? openProject.getProjectUUID() : null;
    }

    @Override
    public File getProjectsDirectory()
    {
        return projectsDirectory;
    }

    @Override
    public File getProjectDirectory(String projectName)
    {
        final String root = programEnvironment.getProjectRoot();
        final File projectsDir = root == null ? getProjectsDirectory() : new File(root);
        return new File(projectsDir, projectName);
    }

    @Override
    public String getProjectFilePath(String projectName)
    {
        return getProjectDirectory(projectName) + File.separator + projectName + "." + ProjectConstants.PROJECT_EXTENSION;
    }

    @Override
    public String getProjectNameFromPath(String projectFilePath)
    {
        return FileUtils.extractFileNameWithoutExtensionFromPath(projectFilePath);
    }

    @Override
    public boolean doesProjectExist(@Nonnull String projectName)
    {
        return !StringUtils.isEmpty(projectName) && getProjectDirectory(projectName).exists();
    }

    @Override
    public boolean isValidNewProjectName(@Nonnull String projectName)
    {
        if (!StringUtils.isEmpty(projectName, true))
        {
            File projectDir = getProjectDirectory(projectName);
            return !projectDir.exists() && isFileValid(projectDir);
        }
        return false;
    }

    @Override
    public String getDefaultProjectName()
    {
        return DATE_FORMATTER.format(LocalDateTime.now());
    }

    @Override
    public boolean openProject(@Nonnull String projectName) throws Exception
    {
        // See if we're creating a new project or opening an existing one
        final String projectPath = getProjectFilePath(projectName);

        ProjectSelection projectSelection = doesProjectExist(projectName)
                ? ProjectSelection.makeExisting(projectPath)
                : ProjectSelection.makeNew(projectPath, null, wmsOfflineMode);

        return openProject(projectSelection, new NullProgressMonitor());
    }

    @Override
    public boolean openProject(@Nonnull ProjectSelection projectSelection, IProgressMonitor progressMonitor) throws IOException
    {
        return internalOpenProject(projectSelection, progressMonitor, null, true);
    }

    @Override
    public boolean openProjectForTest(@Nonnull ProjectSelection projectSelection, IProgressMonitor progressMonitor, Runnable dbInitializer, boolean loadServices) throws IOException
    {
        return internalOpenProject(projectSelection, progressMonitor, dbInitializer, loadServices);
    }

    /**
     * Internal handler to open a project.  This was separated out so that the two primary entry points for
     * opening a project have clear names to separate normal use from tests.
     *
     * @see #openProject(ProjectSelection, IProgressMonitor)
     * @see #openProjectForTest(ProjectSelection, IProgressMonitor, Runnable, boolean)
     * @since 2.37
     */
    private boolean internalOpenProject(ProjectSelection projectSelection, IProgressMonitor progressMonitor, Runnable dbInitializer, boolean loadServices) throws IOException
    {
        // It is necessary to have all plugins loaded before opening a project
        pluginRegistryManager.waitForPluginRegistriesToLoad();

        // If we already have a project open, then close it
        if (openProject != null) closeProject(progressMonitor);

        final String projectPath = projectSelection.getProjectPath();
        final String projectName = getProjectNameFromPath(projectPath);

        logger.debug("Opening project: {}", projectPath);

        container.event().select(ProjectOpening.class).fire(new ProjectOpening(projectName, progressMonitor));

        openProjectSession(projectPath);

        // Update initial preferences for new projects
        if (projectSelection.isNewProject())
        {
            openProject.initializeNewProjectPreferences(projectSelection.getProjectMetadata());
        }

        // ApplicationGlobe looks up offline mode in onLoadState so we need to set it here.
        // The WMS offline mode should only be applied if a value was specified in the selection.
        // Otherwise, we should use the setting for WMS offline mode stored in the project file.
        if (projectSelection.isWmsOfflineSet())
        {
            ProjectPreferenceEntry.WMS_OFFLINE_MODE.put(projectSelection.isWmsOffline());
        }

        if (dbInitializer != null) dbInitializer.run();

        try
        {
            if (loadServices) startServices(progressMonitor);

            container.event().select(ProjectOpened.class).fire(new ProjectOpened(projectName, progressMonitor));
        } catch (LoadCancelledException e)
        {
            logger.info("Database load cancelled.");
            closeProject(progressMonitor);
            return false;
        }

        return true;
    }

    /**
     * Low-level open of the physical project.
     *
     * @param projectOrPackageFilePath Full path to project or package file
     */
    private void openProjectSession(String projectOrPackageFilePath) throws IOException
    {
        final boolean isMostRecent = projectOrPackageFilePath.equals(getLastOpenedProjectPath());
        openProject = projectFactory.createProject(projectOrPackageFilePath, isMostRecent);

        String dir = openProject.getProjectDirectory();

        if (dir != null)
        {
            File f = new File(dir);
            if (f.isAbsolute())
            {
                programEnvironment.setProjectRoot(f.getParent());
            }
        }

        openProject.open();
        setLastOpenedProjectPath(openProject.getProjectFilePath());
    }

    /**
     * Fire the events to start the project services.
     *
     * @since 2.37
     */
    private void startServices(final IProgressMonitor progressMonitor)
    {
        final long tStart = System.currentTimeMillis();
        logger.debug("Starting services");

        progressMonitor.setMaximum(IProgressMonitor.INDETERMINATE);

        logger.info("Loading core services");

        // Crank up all the project-level services
        try
        {
            // See ServiceOrdering for details on service startup ordering
            container.event().select(StartProjectServices.class).fire(new StartProjectServices(progressMonitor));

            logger.info("Loaded core services");

            progressMonitor.setText("Starting plugins...");

            // And, now start plugins
            container.event().select(StartPlugins.class).fire(new StartPlugins(progressMonitor));

            // Finally, the background services (execute immediately when under test)
            if (programEnvironment.isPlatformUnderTest())
            {
                new BackgroundServicesTask().execute();
            } else
            {
                taskManager.execute(new BackgroundServicesTask());
            }

            if (logger.isDebugEnabled()) logger.debug("load time: {} ms)", System.currentTimeMillis() - tStart);
        } catch (FatalDatabaseException | LoadCancelledException e)
        {
            container.event().select(StopProjectServices.class).fire(new StopProjectServices(progressMonitor));
            throw e;
        }
    }

    @Override
    public List<File> getRecentProjects()
    {
        return PreferenceEntry.RECENT_PROJECTS_NODE.getNodeAs(RecentProjects.class, true).getProjectList();
    }

    @Override
    public void closeProject(IProgressMonitor progressMonitor)
    {
        if (openProject != null)
        {
            final String projectName = openProject.getProjectName();

            // Ensure the current project is saved prior to closing.
            saveProject(progressMonitor);

            container.event().select(ProjectClosing.class).fire(new ProjectClosing(projectName, progressMonitor));

            // See ServiceOrdering for details on service stop ordering
            container.event().select(StopProjectServices.class).fire(new StopProjectServices(progressMonitor));

            // Clear the state on the project session and unlock the project directory
            openProject.close(progressMonitor);

            openProject = null;
            container.event().select(ProjectClosed.class).fire(new ProjectClosed(projectName, progressMonitor));
        }
    }

    @Override
    public void saveProject(IProgressMonitor progressMonitor)
    {
        final String projectName = getProjectName();

        container.event().select(ProjectSaving.class).fire(new ProjectSaving(projectName, progressMonitor));

        // All appropriate classes will have responded to that event and saved their state

        openProject.saveProjectPreferences();
        userPreferenceManager.savePreferences();
        toolPreferenceManager.savePreferences();

        container.event().select(ProjectSaved.class).fire(new ProjectSaved(projectName, progressMonitor));
    }

    @Override
    public void setProjectFactory(IProjectFactory projectFactory)
    {
        this.projectFactory = projectFactory;
    }

    @Override
    public String getLastOpenedProjectPath()
    {
        return UserPreferenceEntry.LAST_OPEN_PROJECT.getStringValue();
    }

    private void setLastOpenedProjectPath(String projectPath)
    {
        UserPreferenceEntry.LAST_OPEN_PROJECT.putAndSave(projectPath);

        // If the path is not empty (being cleared), then update the "last projects dir" preference
        // and the list of recent projects.

        if (!projectPath.isEmpty())
        {
            try
            {
                // projectPath points to the actual .projx file, so we need to go up 2 levels to get the Projects dir
                projectsDirectory = new File(projectPath).getParentFile().getParentFile();
                PreferenceEntry.LAST_PROJECTS_DIR.put(projectsDirectory.getCanonicalPath());

                final RecentProjects recentProjects = PreferenceEntry.RECENT_PROJECTS_NODE.getNodeAs(RecentProjects.class, true);
                recentProjects.updateProjectList(openProject.getProjectFilePath());

                preferenceManager.savePreferences();
            } catch (IOException e)
            {
                logger.error("Failed to save last opened project in application preferences", e);
            }
        }
    }

    @Override
    public String getLastOpenProjectNameIfExists()
    {
        String path = getLastOpenedProjectPath();

        return (!path.isEmpty() && new File(path).exists()) ? getProjectNameFromPath(path) : "";
    }

    @Override
    public void clearLastOpenedProjectPath()
    {
        setLastOpenedProjectPath("");
    }

    /**
     * Test whether a File has a valid path.
     *
     * @param file file to test for validity.
     * @return {@code true} if valid, or {@code false} otherwise.
     * <p>
     * Modification of code cited online:
     * <a href="http://stackoverflow.com/questions/893977/java-how-to-find-out-whether-a-file-name-is-valid">Stack Overflow</a>
     */
    protected boolean isFileValid(File file)
    {
        try
        {
            //noinspection ResultOfMethodCallIgnored
            file.getCanonicalPath();
            return true;
        } catch (Exception e)
        {
            return false;
        }
    }

    @Override
    public void saveProjectAs(String newProjectFilePath, IProgressMonitor progressMonitor) throws Exception
    {
        // First check to see if we're saving to the current project name.  If so, just call save.
        final String currentProjectName = openProject.getProjectName();
        final String currentProjectFolder = openProject.getProjectDirectory();
        final String saveToProjectFolder = FileUtils.extractFullDirectoryPathFromPath(newProjectFilePath);

        // Use equalsIgnoreCase here because the java File class does not differentiate between upper
        // and lower case when opening a file on Windows, at least. (see ProjectManager.doesProjectExist())
        if (currentProjectFolder.equalsIgnoreCase(saveToProjectFolder))
        {
            saveProject(progressMonitor);
            return;
        }

        progressMonitor.setText("Performing Save As operations...");

        // Close the current project in preparation for the copy
        closeProject(progressMonitor);

        // Copy the project data to the new project directory
        final File newProjectFile = new File(newProjectFilePath);
        final String newProjectDir = newProjectFile.getParent();
        final String newProjectName = getProjectNameFromPath(newProjectFilePath);

        copyProject(currentProjectFolder, currentProjectName, newProjectDir, newProjectName, progressMonitor);

        // And, finally, open the new project
        final ProjectSelection projectSelection = ProjectSelection.makeExisting(newProjectFilePath);

        openProject(projectSelection, progressMonitor);
    }

    /**
     * Copy a project from once location to another, renaming it in the process.
     *
     * @param originalProjectLocation Path to project to copy
     * @param originalProjectName     Name of project to copy
     * @param newProjectLocation      The new project location
     * @param newProjectName          The new project name
     * @param progressMonitor         For progress status updates
     */
    private void copyProject(String originalProjectLocation, String originalProjectName, String newProjectLocation, String newProjectName, IProgressMonitor progressMonitor) throws IOException
    {
        progressMonitor.setText("Copying project data...");

        final File newProjectDir = new File(newProjectLocation);

        // Don't copy these files into the new project.
        final List<Path> filesToIgnore = Arrays.asList(
                Paths.get(originalProjectLocation, Project.LOCK_FILE_NAME),
                Paths.get(originalProjectLocation, ProjectSnapshotService.PROJECT_SNAP_SHOT_NAME));

        // Perform the actual deep copy
        Files.walkFileTree(Paths.get(originalProjectLocation),
                new CopyDirectoryFileVisitor(newProjectDir.toPath(), progressMonitor, filesToIgnore));

        // Get a reference to the new .projx file. It will still have the same name as the old .projx file.
        final String currentProjectFileName = originalProjectName + '.' + ProjectConstants.PROJECT_EXTENSION;
        final File newProjectFile = new File(newProjectDir, currentProjectFileName);
        final String newProjectFileName = newProjectName + '.' + ProjectConstants.PROJECT_EXTENSION;

        // Calculate the new name.
        final File renamedProjectFile = new File(newProjectDir, newProjectFileName);

        // Rename.
        //noinspection ResultOfMethodCallIgnored
        newProjectFile.renameTo(renamedProjectFile);
    }

    @Override
    public void createPackage(String packageName, String packagePath, boolean includeImageryCache, IProgressMonitor progressMonitor)
    {
        // Begin to zip the package.
        // Notify project lifecycle listeners that a package operation is beginning
        final String projectName = getProjectName();

        container.event().select(ProjectPackaging.class).fire(new ProjectPackaging(projectName, progressMonitor));

        // All appropriate classes will have responded to that event and saved their state

        // We should really have a middle phase here, like packagePrepareForZip, which is when everyone ensures
        // that the disk state (including things like DB backups) have been created.  Then, the "packaged" phase
        // would truly be a "we're all done now" phase.

        // Now that the DB is in a clean state, fire the packaged event.  This is when the DB backup file is created.
        container.event().select(ProjectPackaged.class).fire(new ProjectPackaged(projectName, progressMonitor));

        // Actually create the ZIP of the project content
        openProject.createPackage(packageName, packagePath, includeImageryCache, new ZipFileProgressAdapter(progressMonitor));
    }

    /**
     * Adapter to make an {@link IProgressMonitor} work as an {@link IZipFileProgress}.
     *
     * @since 2.37
     */
    private static final class ZipFileProgressAdapter implements IZipFileProgress
    {
        private final IProgressMonitor progressMonitor;

        private ZipFileProgressAdapter(IProgressMonitor progressMonitor)
        {
            this.progressMonitor = progressMonitor;
        }

        @Override
        public void increment()
        {
            progressMonitor.increment();
        }

        @Override
        public void setFileCount(int fileCount)
        {
            progressMonitor.setMaximum(fileCount);
        }

        @Override
        public void setText(String text)
        {
            progressMonitor.setText(text);
        }

        @Override
        public void setSubstatusText(String text)
        {
            progressMonitor.setSubstatus(text);
        }

        @Override
        public void setSubstatusMaximum(int maximum)
        {
            progressMonitor.setSubstatusMaximum(maximum);
        }

        @Override
        public void incrementSubstatus()
        {
            progressMonitor.incrementSubstatus();
        }
    }

    /**
     * Interns the preferences node defining the recently opened projects.
     *
     * @since 2.37
     */
    private static final class RecentProjects extends PreferencesNode
    {
        private static final int MAX_PROJECTS_TO_RECORD = 12;

        // Order the dates newest first
        private static final Comparator<Object> DATE_COMPARATOR =
                Comparator.comparingLong(key -> LocalDateTime.parse((String) key, DATE_FORMATTER).toEpochSecond(ZoneOffset.UTC))
                        .reversed();

        private final List<String> projectOpenDates;

        /**
         * Construct from the given preferences node.
         */
        private RecentProjects(PreferencesNode prefs)
        {
            super(prefs);

            projectOpenDates = keys();
            projectOpenDates.sort(DATE_COMPARATOR);
        }

        /**
         * @return List of File for recent projects
         */
        private List<File> getProjectList()
        {
            return projectOpenDates.stream()
                    .map(openDate -> new File(get(openDate, null)))
                    .toList();
        }

        /**
         * Update the preferences to add the new/current project, removing any older projects beyond our configured MAX.
         *
         * @param currentProject Project being opened
         * @return List of project files now in the "recent" list
         */
        private List<File> updateProjectList(String currentProject)
        {
            final List<File> projects = new ArrayList<>();

            for (String openDate : projectOpenDates)
            {
                final String projectPath = get(openDate, null);

                if (projectPath.equals(currentProject) || projects.size() > MAX_PROJECTS_TO_RECORD)
                {
                    remove(openDate);
                } else
                {
                    projects.add(new File(projectPath));
                }
            }

            put(DATE_FORMATTER.format(LocalDateTime.now()), currentProject); // Add our "new" entry

            return projects;
        }
    }

    /**
     * Managed task to start the background services.
     * @since 5.2
     */
    private class BackgroundServicesTask extends AManagedTask
    {
        protected BackgroundServicesTask()
        {
            super(TaskGroup.OTHER, "Starting Background Services");
        }

        @Override
        public void execute()
        {
            container.event().select(StartBackgroundProjectServices.class).fire(new StartBackgroundProjectServices());

            final ITaskStatusHandler statusHandler = getTaskStatusHandler();
            if (statusHandler != null) statusHandler.setComplete();
        }
    }
}
