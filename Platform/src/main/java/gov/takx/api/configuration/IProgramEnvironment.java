package gov.takx.api.configuration;

import gov.takx.api.annotation.NoPluginImplementations;

import java.io.File;
import java.util.TimeZone;

/**
 * Contract for access to the running program's environment, which includes directory locations.
 *
 * @since 2.0.0
 */
@NoPluginImplementations
public interface IProgramEnvironment
{
    /**
     * Returns the original local time zone when TAKX was started. Internally, TAKX sets the default TimeZone
     * to "UTC" so that the underlying database can properly perform Date-based calculations. The original default
     * TimeZone is recorded, an available via this method.
     *
     * @return original local time zone when TAKX was started
     * @since 2.35
     */
    TimeZone getLocalTimeZone();

    /**
     * @return The version string from the version information
     * @since 5.0
     */
    String getPlatformVersionString();

    /**
     * @return The pre-release tag for this build, like 'SNAPSHOT' or 'RC1'
     */
    String getVersionPreReleaseTag();

    /**
     * @return The build number from the version information, this is an abbreviated build number
     */
    String getProgramBuild();

    /**
     * @return The build date from the version information
     */
    String getProgramBuildDate();

    /**
     * @return {@code true} if concurrent handling of plugin deployments is enabled
     */
    boolean isConcurrentDeployment();

    /**
     * @return True if the platform is operating in a test mode
     */
    boolean isPlatformUnderTest();

    /**
     * @return if we are running headless
     */
    boolean isHeadless();

    /**
     * Set if this takx session is to operate in a headless fashion.  When running headless, no Swing UI
     * components will be created, but the core system will function as normal.
     *
     * @param headless Pass true to run headless
     */
    void setHeadless(boolean headless);

    /**
     * Get the location for the ProjectData directory. The order of preference is:
     * <ol>
     * <li>Property {@code -Dprojectdata.dir}</li>
     * <li>Property {@code user.dir} if the ProjectData directory already exists</li>
     * <li>The default platform-specific program data root, if non null</li>
     * <ul>
     *     <li>Windows: {@code $ALLUSERSPROFILE} or {@code "C:/ProgramData"} if not defined</li>
     *     <li>Linux: {@code "/usr/local"}</li>
     *     <li>MacOS: {@code $HOME/Documents} or {@code "/Library/Application Support"} if HOME is not defined</li>
     * </ul>
     * <li>Property {@code user.dir} as a final fallback</li>
     * </ol>
     *
     * @return the program root path
     */
    String getProgramRoot();

    /**
     * @return the full path to the {@code ProjectData} directory, under which the project and user files are kept.
     */
    String getProjectDataDirectory();

    /**
     * Support for multiple takx instances with unique preferences, look for a file named
     * ProgramProperties-&lt;instanceNumber&gt;.xml first.  If that is found, then return it.  If not,
     * then default to ProgramProperties.xml.
     *
     * @return The program properties file to use for the current takx instance
     * @since 2.25
     */
    File getInstanceProgramPropertiesFile();

    /**
     * @return File referencing the {@code localProgramProperties.xml} file in {@code ProjectData}.  This is the file
     * expected to be edited by users to make program configuration changes.  The {@code DefaultProgramProperties.xml}
     * file in {@code <installation dir>/conf/} should <b>NEVER</b> be edited by users as it will be overwritten when
     * a new version is installed.
     *
     * @since 5.1
     */
    File getLocalProgramPropertiesFile();

    /**
     * @return File referencing the database properties
     * @since 3.0
     */
    File getDatabasePropertiesFile();

    /**
     * @return File referencing directory that contains user-specific files for the given user name
     * @since 2.35
     */
    File getUserFilesDir();

    /**
     * Generate the user preferences file for the current takx instance number.
     *
     * @return File referencing "username.xml" for the default takx instance, else "username-&lt;instance&gt;.xml"
     * @since 2.35
     */
    File getInstanceUserPreferencesFile();

    /**
     * Generate the user tool preferences file for the current TAKX instance number.
     *
     * @return File referencing "Tools.xml" for the default TAKX instance, else "Tools-&lt;instance&gt;.xml"
     * @since 5.0
     */
    File getInstanceToolPreferencesFile();

    /**
     * Get the path to Globals directory within the Project Data.
     *
     * @return the path as a string
     * @since 1.16
     */
    String getGlobalFilesDir();

    /**
     * @return The path to the globeLayers.xml file
     */
    String getGlobeLayersFilePath();

    /**
     * @return The path to the directory used to store layer definitions
     */
    String getGlobeLayersDirectoryPath();

    /**
     * @return The path to the Docking.xml file
     * @since 2.11
     */
    String getDockingFilePath();

    /**
     * @return file containing the default globe layers
     */
    File getGlobeLayersFile();

    /**
     * @return directory containing the configuration files for default globe layers
     */
    File getGlobeLayersConfigurationDirectory();

    /**
     * @return the full program name, version, and build number
     * @since 2.30
     */
    String getProgramNameVersionAndBuild();
}
