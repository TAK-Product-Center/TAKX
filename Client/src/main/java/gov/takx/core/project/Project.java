package gov.takx.core.project;

import gov.takx.api.annotation.PseudoBean;
import gov.takx.api.annotation.VFSManagerScope;
import gov.takx.api.filesystem.IVirtualFileSystemManager;
import gov.takx.api.filesystem.VFSScope;
import gov.takx.api.globe.IApplicationGlobe;
import gov.takx.api.globe.IExtendedGlobeManager;
import gov.takx.api.mapobject.IMapObject;
import gov.takx.api.mapobject.IMapObjectManager;
import gov.takx.api.persist.IPersistServiceProvider;
import gov.takx.api.platform.IProgressMonitor;
import gov.takx.api.preferences.IProjectPreferenceManager;
import gov.takx.api.project.IProject;
import gov.takx.api.project.IProjectMetadata;
import gov.takx.api.project.ProjectConstants;
import gov.takx.commons.file.FileUtils;
import gov.takx.commons.file.IZipFileProgress;
import gov.takx.commons.utils.PlatformVersion;
import gov.takx.commons.utils.UUIDUtils;
import gov.takx.core.configuration.ProgramEnvironment;
import gov.takx.core.globe.widgets.ProjectLabelWidgetManager;
import gov.takx.core.launcher.ApplicationLauncher;
import gov.takx.core.propertymanager.SystemPropertyManager;
import gov.takx.dataservices.persist.DatabaseVersionManager;
import gov.takx.platform.logging.LoggerUtils;
import gov.takx.platform.preferences.ProjectPreferenceEntry;
import jakarta.inject.Inject;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.core.appender.FileAppender;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.zeroturnaround.zip.NameMapper;

import java.io.File;
import java.io.IOException;
import java.lang.invoke.MethodHandles;
import java.nio.channels.FileChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.time.Instant;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

/**
 * A TAKX project references a specific project on disk. A project's primary "reference point" is either a
 * packaged project (a zip file), which will be expanded on demand when it is opened, or a plain project file
 * inside a project directory.  The project preferences, database backup, and user files are all stored within
 * the directory containing the project file.
 * <p>
 * When a project is opened a new log4j log file is opened in the directory that gets a copy of all the logging
 * output while the project is open.
 *
 * @since 3.4 (renamed from RaptorProject)
 */
@SuppressWarnings("CdiManagedBeanInconsistencyInspection")
@PseudoBean
public class Project implements IProject
{
    private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    private static final String DEFAULT_PROJECT_PREFERENCES_XML = "conf/DefaultProjectPreferences.xml";

    private static final String PROJECT_APPENDER = "ProjectAppender"; // Name of project-level log appender
    private static final String LOG_FILE_NAME = "takx.log";

    private static FileAppender projectAppender;

    protected String projectFilePath;
    protected String projectName;

    private boolean newProject;
    private boolean fromPackage = false;

    // For handling the lock on the project
    public static final String LOCK_FILE_NAME = ".projectLock";
    private FileChannel lockChannel;

    @Inject
    protected ProgramEnvironment programEnvironment;

    @Inject
    protected IProjectPreferenceManager projectPreferenceManager;

    @Inject
    protected SystemPropertyManager systemPropertyManager;

    @Inject
    protected IPersistServiceProvider persistServiceProvider;

    @Inject
    private ProjectLabelWidgetManager labelWidgetManager;

    @Inject
    private IMapObjectManager mapObjectManager;

    @Inject
    private IExtendedGlobeManager globeManager;

    @Inject
    @VFSManagerScope(VFSScope.Project)
    private IVirtualFileSystemManager projectVFSManager;

    @Inject
    private ApplicationLauncher applicationLauncher;

    /**
     * Construct a project on the specified project file.
     *
     * @param projectFilePath Path to project or package file
     */
    public Project(String projectFilePath)
    {
        this.projectFilePath = projectFilePath;

        newProject = !new File(projectFilePath).exists();
        projectName = FileUtils.extractFileNameWithoutExtensionFromPath(projectFilePath);
    }

    @Override
    public boolean isNew()
    {
        return newProject;
    }

    @Override
    public void setNew(boolean val)
    {
        newProject = val;
    }

    @Override
    public boolean isFromPackage()
    {
        return fromPackage;
    }

    @Override
    public void initializeNewProjectPreferences(IProjectMetadata projectMetadata)
    {
        final PlatformVersion pv = programEnvironment.getPlatformVersion();

        // every project needs a UUID.
        // It should have already been created when the project was opened, so validate that.
        if (ProjectPreferenceEntry.UUID.getStringValue().isEmpty())
        {
            logger.error("Open project has no UUID!");
            throw new RuntimeException("Project has no UUID");
        }

        ProjectPreferenceEntry.CREATION_TIME.put(Instant.now().toEpochMilli());
        ProjectPreferenceEntry.VERSION.put(pv.toString());
        ProjectPreferenceEntry.DATABASE_VERSION.put(DatabaseVersionManager.CURRENT_VERSION_STRING);

        if (projectMetadata != null)
        {
            ProjectPreferenceEntry.TITLE.put(projectMetadata.getTitle());
            ProjectPreferenceEntry.START_DATE.put(projectMetadata.getStartDate());
            ProjectPreferenceEntry.END_DATE.put(projectMetadata.getEndDate());
            ProjectPreferenceEntry.SIZE.put(projectMetadata.getSize());
            ProjectPreferenceEntry.VFS.put(projectMetadata.getVfsSize());
            ProjectPreferenceEntry.DESCRIPTION.put(projectMetadata.getDescription());
            ProjectPreferenceEntry.LOCATION.put(projectMetadata.getLocation());
            ProjectPreferenceEntry.NOTES.put(projectMetadata.getNotes());
        }

        saveProjectPreferences();
    }

    @Override
    public void setWMSOfflineMode(boolean offlineMode)
    {
        ProjectPreferenceEntry.WMS_OFFLINE_MODE.put(offlineMode);
    }

    @Override
    public boolean isWMSOfflineMode()
    {
        return programEnvironment.isPlatformUnderTest() || ProjectPreferenceEntry.WMS_OFFLINE_MODE.getBooleanValue();
    }

    @Override
    public String getProjectName()
    {
        return projectName;
    }

    @Override
    public String getProjectFilePath()
    {
        return projectFilePath;
    }

    @Override
    public String getProjectDirectory()
    {
        File f = new File(projectFilePath);

        // PLUGIN-4206
        // This code was really old and crusty, and it seemed to be assuming that the projectFilePath could be either
        // a directory or pointing at the actual .projx file.  It was just using an "isFile()" test to determine
        // if it was pointing at a .projx file, which ignores the case when it doesn't exist (yet).
        // So, I reworked this to always use the files parent directory, which will be correct, even on a new project.

        logger.debug("path: {} isNew={}", projectFilePath, newProject);

        return f.getParent();
    }

    @Override
    public void saveProjectPreferences()
    {
        saveUpdatedPreferenceFile();
    }

    @Override
    public UUID getProjectUUID()
    {
        return UUID.fromString(ProjectPreferenceEntry.UUID.getStringValue());
    }

    @Override
    public String getProjectVersion()
    {
        final String pv = ProjectPreferenceEntry.VERSION.getStringValue();
        return pv.isEmpty() ? PlatformVersion.CURRENT_PLATFORM_VERSION.toString() : pv;
    }

    @Override
    public long getProjectCreationTime()
    {
        return ProjectPreferenceEntry.CREATION_TIME.getLongValue();
    }

    @Override
    public void open() throws IOException
    {
        // Initialize the project directory
        final boolean isPackage = ProjectConstants.PACKAGE_EXTENSION.equals(FileUtils.getExtension(projectFilePath));

        if (isPackage)
        {
            extractPackage(); // Expand project and user files if it is a packaged project
        } else
        {
            createProjectSetupIfNeeded();
        }

        switchProjectAppender(getProjectDirectory());

        lockProject(); // Get the lock
        validateProjectVersion(); // Check the version of the project
        setProjectUuidIfNeeded(isPackage); // Ensure we have a project UUID, or a new one for a package
    }

    /**
     * Extract the contents of the package into the project's directory so it can be opened.
     */
    private void extractPackage()
    {
        fromPackage = true;

        // By default, we'll offer the user the default project data directory, but the user can choose to
        // store the extracted project somewhere else.
        String defaultProjectLocation = programEnvironment.getProjectDataDirectory() + File.separator + "Projects";

        // Let the user choose the extraction location.
        String extractionPath = applicationLauncher.getAppFrame().doWithoutLock(() -> Package.choosePackageExtractionPath(defaultProjectLocation));

        // Get the package info.
        Package packagedProject = new Package(projectFilePath, extractionPath);

        // The user's selection will be the new project root.
        String projectDirectory = packagedProject.getExtractedProjectParentDirectory();
        programEnvironment.setProjectRoot(projectDirectory);

        // Get the project name from the package.
        projectName = packagedProject.getProjectName();

        // Set the path to the new project directory.
        projectFilePath = packagedProject.getProjectFile();
    }

    /**
     * Validate the project version is reasonable for the running program version.
     */
    private void validateProjectVersion()
    {
        final String projectVersion = getProjectVersion();
        final String programVersion = programEnvironment.getPlatformVersionString();

        if (!isValidProjectVersion(programVersion, projectVersion))
        {
            final String msg = "The project selected is for a newer program version:\n" +
                    "   Program Version: " + programVersion + "\n" +
                    "   Project Version:   " + projectVersion + "\n" +
                    "Please upgrade your program if you wish to open this project.";
            throw new RuntimeException(msg);
        }
    }

    /**
     * If this is a package being extracted, or the project doesn't have a UUID, then assign one.
     * This will automatically upgrade older project files, and deal with the fact that an extracted project is NOT the
     * same project as the original.
     */
    protected void setProjectUuidIfNeeded(boolean isPackage)
    {
        final String uuidStr = ProjectPreferenceEntry.UUID.getStringValue();

        if (isPackage || uuidStr.isEmpty())
        {
            ProjectPreferenceEntry.UUID.put(UUIDUtils.generateType1UUID().toString());
            saveProjectPreferences();
        }
    }

    @Override
    public void close(IProgressMonitor progressMonitor)
    {
        unlockProject();
        closeProjectAppender();
    }

    @Override
    public void createPackage(String packageName, String packagePath, boolean includeImageryCache, IZipFileProgress zipFileProgress)
    {
        logger.debug("createPackage: {}", packagePath);

        // Ensure that the project is saved prior to packaging.
        createPackageZip(packageName, packagePath, includeImageryCache, zipFileProgress);
    }

    /**
     * Create the package zip file.
     *
     * @param packageName         Name to give the new package.
     * @param packagePath         Destination path of package
     * @param includeImageryCache {@code true} if imagery must be included in the package.
     */
    private void createPackageZip(String packageName, String packagePath, boolean includeImageryCache, IZipFileProgress zipFileProgress)
    {
        // Prepare set of files to zip and those to exclude
        Set<File> sources = new HashSet<>();
        Set<File> excludes = new HashSet<>();
        final String projectDirectoryPath = getProjectDirectory();

        sources.add(new File(projectDirectoryPath));

        // Exclude any files/directories specified by our DB provider
        persistServiceProvider.getPersistService().getDatabaseProvider().getCoreServer().getPackageExcludes().stream()
                .map(filename -> new File(projectDirectoryPath, filename))
                .forEach(excludes::add);

        // Can't include the lock file as it would cause the zip operation to fail
        excludes.add(new File(projectDirectoryPath, LOCK_FILE_NAME));

        // Add imagery into the sources list if wanted
        if (includeImageryCache)
        {
            final IApplicationGlobe globe = globeManager.getPrimaryGlobeDelegate();
            File dataFileCache = globe.getImageryDirectoryFile();
            sources.add(dataFileCache);

            for (File f : globe.getImageryDirectoryFileExtra())
            {
                boolean isInstall = globe.isInstallLocation(f.getAbsolutePath());
                if (isInstall && f.getParentFile().equals(dataFileCache.getParentFile()))
                {
                    sources.add(f);
                    break;
                }
            }
        }

        packageName = new File(packageName).getName();
        File packageToCreate = new File(packagePath, (packageName + '.' + ProjectConstants.PACKAGE_EXTENSION));

        // if package already exists, create a new name for the package
        int packageCount = 2;
        while (packageToCreate.exists())
        {
            logger.debug("package exists {}", packageCount);
            packageToCreate = new File(packagePath, (packageName + packageCount + '.' + ProjectConstants.PACKAGE_EXTENSION));
            packageCount++;
        }

        final Path oldRootPath = Paths.get(projectName);
        final Path oldProjxPath = oldRootPath.resolve(projectName + '.' + ProjectConstants.PROJECT_EXTENSION);

        final Path newRootPath = Paths.get(packageName);
        final String newProjxFileName = packageName + '.' + ProjectConstants.PROJECT_EXTENSION;
        final Path newProjxPath = newRootPath.resolve(newProjxFileName);

        // This name mapper will properly rename the existing project directory and projx file as they
        // are being added to the package zip.

        final NameMapper nameMapper = pathString -> {
            final Path path = Paths.get(pathString);

            if (path.equals(oldProjxPath))
            {
                return newProjxPath.toString();
            } else if (path.startsWith(oldRootPath))
            {
                final int nameCount = path.getNameCount();
                final Path newPath = nameCount > 1
                        ? newRootPath.resolve(path.subpath(1, nameCount))
                        : newRootPath;
                return newPath.toString();
            } else
            {
                return pathString;
            }
        };

        // zip up everything into the package file
        FileUtils.zipCollection(sources, packageToCreate, excludes, nameMapper, zipFileProgress);

        logger.info("Created package of {} in {}", projectDirectoryPath, packageToCreate);
    }

    /**
     * Switch the log4j file appender to the new/current project.  Close any old appender if present.
     *
     * @param projectDir Full path/name of project being opened
     */
    private static void switchProjectAppender(String projectDir)
    {
        closeProjectAppender();

        // Now construct the new appender targeted at the current project directory
        projectAppender = FileAppender.newBuilder()
                .setName(PROJECT_APPENDER)
                .withFileName(new File(projectDir, LOG_FILE_NAME).getAbsolutePath())
                .setLayout(LoggerUtils.getConsoleLayout())
                .build();

        LoggerUtils.addAppender(projectAppender, Level.INFO);
    }

    /**
     * Close the project log appender.
     */
    private static void closeProjectAppender()
    {
        if (projectAppender != null)
        {
            LoggerUtils.removeAppender(projectAppender);
            projectAppender = null;
        }
    }

    /**
     * Determine if the project version is compatible with the program version.
     *
     * @param programVersionString Version of running program
     * @param projectVersionString Version from the project
     * @return true if the versions are compatible
     */
    public static boolean isValidProjectVersion(String programVersionString, String projectVersionString)
    {
        return PlatformVersion.compare(projectVersionString, programVersionString) <= 0;
    }

    /**
     * If this is a new project (the project file doesn't exist), then create the files and initialize the database.
     *
     * @throws IOException If the project file creation fails
     */
    protected void createProjectSetupIfNeeded() throws IOException
    {
        File projFile = new File(projectFilePath);

        if (!projFile.exists())
        {
            createProjectFileFromDefaults(projFile);

            setNew(true);
        } else
        {
            setNew(false);
        }
    }

    /**
     * Create the project file from the default project preferences file.
     *
     * @param projectFile File path to create
     * @throws IOException if an IO error occurs
     */
    private void createProjectFileFromDefaults(File projectFile) throws IOException
    {
        FileUtils.copy(new File(DEFAULT_PROJECT_PREFERENCES_XML), projectFile);
    }

    private void saveUpdatedPreferenceFile()
    {
        // Always ensure that we update the project VERSION when we save the preferences
        PlatformVersion pv = programEnvironment.getPlatformVersion();

        ProjectPreferenceEntry.VERSION.put(pv.toString());
        ProjectPreferenceEntry.LAST_UPDATE.put(Instant.now().toEpochMilli());

        ProjectPreferenceEntry.MAP_OBJECTS_NODE.node().putAsValues(mapObjectManager.getMapObjectControllers(), moc -> {
            final IMapObject mo = moc.getMapObject();
            return mo.getKey() + "," + mo.getName();
        });

        // Get dir info for project directory
        FileUtils.DirInfo dirInfo = FileUtils.getDirectoryInfo(new File(getProjectDirectory()));
        ProjectPreferenceEntry.SIZE.put(FileUtils.formatFileSize(dirInfo.size));

        dirInfo = FileUtils.getDirectoryInfo(projectVFSManager.getRootDirectory());
        String vfs = dirInfo.fileCount + " (" + FileUtils.formatFileSize(dirInfo.size) + ")";
        ProjectPreferenceEntry.VFS.put(vfs);

        // And write them all out
        projectPreferenceManager.savePreferences();
    }

    /**
     * Obtain a concurrent access lock in this project to prevent multiple instances of TAKX from
     * opening the same project.
     *
     * @throws IOException if the lock operation fails
     */
    private void lockProject() throws IOException
    {
        unlockProject(); // Always unlock any previous project

        File file = new File(getProjectDirectory(), LOCK_FILE_NAME);
        if (logger.isDebugEnabled()) logger.debug("Locking file: {}", file);

        if (!file.exists()) Files.createFile(file.toPath()); // Create if not present

        // Attempt to lock the whole file
        lockChannel = FileChannel.open(file.toPath(), StandardOpenOption.APPEND);
        if (lockChannel.tryLock() == null)
        {
            // Lock is held by another process
            logger.error("Project is locked: {}", file.getParent());
            throw new IOException("Project is in use by another instance of TAKX");
        }
    }

    /**
     * Release the lock held on this project, if any.
     */
    private void unlockProject()
    {
        if (lockChannel != null)
        {
            try
            {
                logger.debug("Unlocking project");
                lockChannel.close(); // Closing the channel releases all locks
                lockChannel = null;
            } catch (IOException e)
            {
                logger.error("Failed to release project lock", e);
            }
        }
    }
}
