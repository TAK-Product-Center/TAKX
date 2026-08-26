package gov.takx.core.configuration;

import gov.takx.api.configuration.IProgramEnvironment;
import gov.takx.commons.file.FileUtils;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import org.apache.commons.lang3.SystemUtils;
import org.apache.logging.log4j.core.config.Configurator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.lang.invoke.MethodHandles;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.TimeZone;

/**
 * This base class offers the core functionality of {@link ProgramEnvironment} without any support from the CDI container,
 * thus the name BareProgramEnvironment.
 *
 * @since 2.0.0
 */
public class BareProgramEnvironment
{
    private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    /**
     * The path to the default program properties file.
     */
    public static final String DEFAULT_PROGRAM_PROPERTIES_XML = "conf/DefaultProgramProperties.xml";

    /**
     * The path to the default database properties file.
     */
    public static final String DEFAULT_DATABASE_PROPERTIES_XML = "conf/DefaultDatabaseProperties.xml";

    /**
     * The path to the default user preferences file.
     */
    public static final String DEFAULT_USER_PREFERENCES_XML = "conf/DefaultUserPreferences.xml";

    /**
     * The path to the default tool preferences file.
     */
    public static final String DEFAULT_TOOL_PREFERENCES_XML = "conf/DefaultToolPreferences.xml";

    /**
     * System property key to specify the location of the Project Data directory.
     */
    public static final String SYSPROP_PROJECTDATA_DIR = "projectdata.dir";

    /**
     * Value of {@link #SYSPROP_PROJECTDATA_DIR} indicating to place ProjectData in the user's home directory.
     * @since 5.8
     */
    public static final String USE_USER_DIR = "$user_home";

    /**
     * The local program properties file, within ProjectData.
     * @since 5.1
     */
    public static final String LOCAL_PROGRAM_PROPERTIES_XML = "LocalProgramProperties.xml";

    public static final String GLOBALS_DIR_NAME = "Globals";
    public static final String PROJECT_DATA = "ProjectData";
    public static final String USERS_DIR_NAME = "Users";
    public static final String DEFAULT_USER_NAME = "Default";
    public static final String TOOLS_FILE_NAME = "Tools";

    protected final TimeZone localTimeZone = TimeZone.getDefault();
    protected BareApplicationInstance applicationInstance;
    protected String programRoot;

    public BareProgramEnvironment(BareApplicationInstance applicationInstance)
    {
        this.applicationInstance = applicationInstance;

        initialize();
    }

    /**
     * As defined by {@link IProgramEnvironment#getLocalTimeZone()}.
     */
    public TimeZone getLocalTimeZone()
    {
        return localTimeZone;
    }

    /**
     * As defined by {@link IProgramEnvironment#getProgramRoot()}
     */
    public String getProgramRoot()
    {
        return programRoot;
    }

    /**
     * As defined by {@link IProgramEnvironment#getGlobalFilesDir()}
     */
    public String getGlobalFilesDir()
    {
        return getProjectDataDirectory() + File.separator + GLOBALS_DIR_NAME;
    }

    /**
     * As defined by {@link IProgramEnvironment#getProjectDataDirectory()}
     */
    public String getProjectDataDirectory()
    {
        return getProgramRoot() + File.separator + PROJECT_DATA;
    }

    /**
     * @return The program properties file for the default TAKX instance
     */
    protected String getProgramPropertiesPathForDefaultInstance()
    {
        return getProgramPropertiesPath(BareApplicationInstance.DEFAULT_INSTANCE);
    }

    /**
     * Generate the program properties file name for the given application instance number.
     *
     * @param instance application instance number
     * @return "ProgramProperties.xml" for the default application instance, else "ProgramProperties-&lt;instance&gt;.xml"
     */
    protected String getProgramPropertiesPath(int instance)
    {
        return getInstancePath(getProgramRoot() + File.separator + PROJECT_DATA,
                "ProgramProperties", instance);
    }

    /**
     * @return File for the {@link #LOCAL_PROGRAM_PROPERTIES_XML} in {@code ProjectData}
     * @since 5.1
     */
    public File getLocalProgramPropertiesFile()
    {
        return Paths.get(getProgramRoot(), PROJECT_DATA, LOCAL_PROGRAM_PROPERTIES_XML).toFile();
    }

    /**
     * @return File referencing the database properties
     * @since 3.0
     */
    public File getDatabasePropertiesFile()
    {
        return Paths.get(getProgramRoot(), PROJECT_DATA, "DatabaseProperties.xml").toFile();
    }

    /**
     * Generate the user preferences file name for the given application instance number.
     *
     * @param instance application instance number
     * @return "username.xml" for the default application instance, else "username-&lt;instance&gt;.xml"
     * @since 2.35
     */
    protected String getUserPreferencesPath(int instance)
    {
        return getInstancePath(getUserFilesDir().getPath(), DEFAULT_USER_NAME, instance);
    }

    /**
     * Generate the tool preferences file name for the given application instance number.
     *
     * @param instance application instance number
     * @return "Tools.xml" for the default application instance, else "Tools-&lt;instance&gt;.xml"
     * @since 5.0
     */
    protected String getToolPreferencesPath(int instance)
    {
        return getInstancePath(getProjectDataDirectory(), TOOLS_FILE_NAME, instance);
    }

    /**
     * Create a file path based on the application instance number.
     *
     * @param directory The file directory
     * @param name The file name
     * @param instance application instance number
     * @return "name.xml" for the default application instance, else "name-&lt;instance&gt;.xml"
     * @since 5.0
     */
    private String getInstancePath(@Nonnull String directory, @Nonnull String name, int instance)
    {
        // Construct the file name from the instance number if it is not the default
        final String fileName = instance != BareApplicationInstance.DEFAULT_INSTANCE
                ? name + "-" + instance + ".xml"
                : name + ".xml";

        return directory + File.separator + fileName;
    }

    /**
     * @return File referencing directory that contains user-specific files for the given user name
     * @since 2.35
     */
    public File getUserFilesDir()
    {
        return new File(getProjectDataDirectory() + File.separator + USERS_DIR_NAME + File.separator + DEFAULT_USER_NAME);
    }

    /**
     * Determine our program root and initialize our needed directories/files.
     */
    protected void initialize()
    {
        try
        {
            determineProgramRoot();
            createProgramFiles();
        } catch (IOException e)
        {
            logger.error("Failed to initialize program environment", e);
        }
    }

    /**
     * Ensure that our program files exist every time we start.  We create:
     *
     * <ul>
     * <li>ProjectData directory (if needed)</li>
     * <li>Projects directory</li>
     * <li>LocalProgramProperties.xml file (copied from DefaultProgramProperties.xml)</li>
     * <li>DatabaseProperties.xml file (copied from DefaultDatabaseProperties.xml)</li>
     * <li>Tools.xml file (copied from DefaultToolPreferences.xml)</li>
     * <li>Users/Default/Default.xml file (copied from DefaultUserPreferences.xml)</li>
     * </ul>
     *
     * @throws IOException On failure to create the program files
     */
    private void createProgramFiles() throws IOException
    {
        logger.debug("createProgramFiles");

        final File projectDataDir = new File(getProjectDataDirectory());
        if (!projectDataDir.exists() && !projectDataDir.mkdirs())
        {
            logger.error("Failed to create project data directory: {}", projectDataDir);
        }

        final File projectsDir = new File(projectDataDir, "Projects");
        if (!projectsDir.exists())
        {
            if (!projectsDir.mkdir()) logger.error("Failed to create Projects directory: {}", projectsDir);

            // Create a README file warning the user not to modify the project directories
            final File templateReadMeFile = new File("conf", "README.txt");
            final File newReadmeFile = new File(projectsDir, templateReadMeFile.getName());

            if (templateReadMeFile.exists() && projectsDir.exists() && !newReadmeFile.exists())
            {
                FileUtils.copy(templateReadMeFile, newReadmeFile);
            }
        }

        final File userFileDir = getUserFilesDir();
        if (!userFileDir.exists() && !userFileDir.mkdirs()) logger.error("Failed to create Users directory: {}", userFileDir);

        // Make a copy of DPP.xml to LPP.xml, if it doesn't exist.  If it does exist, we don't want to overwrite it.
        final File localProgramPropertiesFile = Paths.get(getProgramRoot(), PROJECT_DATA, LOCAL_PROGRAM_PROPERTIES_XML).toFile();
        if (!localProgramPropertiesFile.exists())
        {
            FileUtils.copyFileOrResource(DEFAULT_PROGRAM_PROPERTIES_XML, localProgramPropertiesFile);

            // Since this is the first update to the new properties handling model, we need to delete any existing
            // ProgramProperties[-n].xml file(s), or they will inappropriately override properties in LPP.xml.
            deleteOldProgramPropertiesFiles();
        }

        final File dbProperties = getDatabasePropertiesFile();
        if (!dbProperties.exists()) FileUtils.copyFileOrResource(DEFAULT_DATABASE_PROPERTIES_XML, dbProperties);

        final File usersFile = new File(getUserPreferencesPath(BareApplicationInstance.DEFAULT_INSTANCE));
        if (!usersFile.exists()) FileUtils.copyFileOrResource(DEFAULT_USER_PREFERENCES_XML, usersFile);

        final File toolsFile = new File(getToolPreferencesPath(BareApplicationInstance.DEFAULT_INSTANCE));
        if (!toolsFile.exists()) FileUtils.copyFileOrResource(DEFAULT_TOOL_PREFERENCES_XML, toolsFile);
    }

    /**
     * Delete all files in ProjectData that start with {@code "ProgramProperties"}.
     * @since 5.1
     */
    private void deleteOldProgramPropertiesFiles()
    {
        try (var dirStream = Files.newDirectoryStream(Paths.get(getProjectDataDirectory()), "ProgramProperties*"))
        {
            for (Path filePath : dirStream) Files.delete(filePath);
        } catch (IOException e)
        {
            logger.warn("Failed to delete old 'ProgramProperties' files: {}", e.getMessage());
        }
    }

    /**
     * @see IProgramEnvironment#getProgramRoot()
     */
    private void determineProgramRoot()
    {
        if (programRoot != null)
        {
            logger.debug("Already have a Program Root: {}", programRoot);
            return;
        }

        programRoot = System.getProperty(SYSPROP_PROJECTDATA_DIR);
        if (programRoot == null) programRoot = System.getenv(SYSPROP_PROJECTDATA_DIR);

        if (USE_USER_DIR.equalsIgnoreCase(programRoot))
        {
            programRoot = getUserHomeDir();
            if (programRoot != null)
            {
                logger.debug("using user home: {}", programRoot);

                // Force a reconfiguration of our logger context to use the new file location
                System.setProperty("takx.logFileDir", programRoot);
                Configurator.reconfigure(new File("log4j2.xml").toURI());

                logger.debug("Moved logging to: {}", programRoot);
                return;
            }
        }

        final String userDir = SystemUtils.USER_DIR;
        final File userDirFile = new File(userDir, PROJECT_DATA);

        if (programRoot == null && userDirFile.exists()) programRoot = userDir;
        if (programRoot == null) programRoot = getPlatformDefaultDataRoot();
        if (programRoot == null) programRoot = userDir;

        logger.debug("getProgramRoot chose: {}", programRoot);
    }

    /**
     * @return Appropriate directory within the user's home directory to place our log file and ProjectData dir
     * @since 5.8
     */
    @Nullable
    private String getUserHomeDir()
    {
        String userHome;
        if (SystemUtils.IS_OS_WINDOWS)
        {
            userHome = System.getenv("APPDATA");
            if (userHome == null) userHome = System.getenv("LOCALAPPDATA");
            if (userHome == null) userHome = System.getenv("HOMEDRIVE") + '\\' + System.getenv("HOMEPATH");
            return userHome + "\\TAKX";
        } else if (SystemUtils.IS_OS_LINUX || SystemUtils.IS_OS_MAC)
        {
            return System.getenv("HOME") + "/TAKX";
        }

        logger.warn("Can't determine user home directory");
        return null;
    }

    /**
     * @return Platform-specific program data root directory plus {@code "/TAKX"}
     * @see IProgramEnvironment#getProgramRoot()
     */
    private String getPlatformDefaultDataRoot()
    {
        final String baseDir;

        if (SystemUtils.IS_OS_WINDOWS)
        {
            final String allUsersProfile = System.getenv("ALLUSERSPROFILE");
            baseDir = allUsersProfile == null ? "C:/ProgramData" : allUsersProfile;
        } else if (SystemUtils.IS_OS_LINUX)
        {
            baseDir = "/usr/local";
        } else if (SystemUtils.IS_OS_MAC)
        {
            final String home = System.getenv("HOME");
            baseDir = home == null ? "/Library/Application Support" : home + "/Documents";
        } else
        {
            logger.warn("Don't know how to handle the OS we're on {}", SystemUtils.OS_NAME);
            baseDir = null;
        }

        return baseDir == null ? null : baseDir + "/TAKX";
    }
}
