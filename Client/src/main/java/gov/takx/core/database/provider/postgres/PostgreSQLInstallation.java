package gov.takx.core.database.provider.postgres;

import gov.takx.api.annotation.PseudoBean;
import gov.takx.api.annotation.VFSManagerScope;
import gov.takx.api.filesystem.IVirtualFileSystemManager;
import gov.takx.api.filesystem.VFSScope;
import gov.takx.commons.utils.PlatformVersion;
import gov.takx.core.database.exception.MissingDatabasePathException;
import jakarta.annotation.Nonnull;
import jakarta.inject.Inject;
import org.apache.commons.lang3.SystemUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.lang.invoke.MethodHandles;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.NavigableMap;
import java.util.TreeMap;
import java.util.stream.Stream;

/**
 * Represents an installation of PostgreSQL, which contain the binaries needed to manage a Postgres server instance.
 *
 * @since 3.0 - existed earlier in the Postgres DB plugin
 */
@SuppressWarnings("CdiManagedBeanInconsistencyInspection")
@PseudoBean
class PostgreSQLInstallation
{
    private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    private static final String DEFAULT_WINDOWS_PROGRAMS_DIR = "programs/windows/postgresql";
    private static final String DEFAULT_MACOSX_PROGRAMS_DIR = "programs/macosx/postgresql";
    private static final String DEFAULT_LINUX_PROGRAMS_DIR = "programs/linux/postgresql";
    private static final String PG_VERSION_FILE_NAME = "PG_VERSION";

    private static final String programsPathForOs = SystemUtils.IS_OS_MAC ? DEFAULT_MACOSX_PROGRAMS_DIR
            : SystemUtils.IS_OS_LINUX ? DEFAULT_LINUX_PROGRAMS_DIR : DEFAULT_WINDOWS_PROGRAMS_DIR;

    private static final File programsDir = new File(programsPathForOs);

    private static final NavigableMap<PlatformVersion, PostgreSQLInstallation> postgreSQLInstallations = findInstallations();
    private static final PostgreSQLInstallation latestInstallation = postgreSQLInstallations.lastEntry().getValue();

    @Inject
    @VFSManagerScope(VFSScope.Global)
    private IVirtualFileSystemManager globalVFSManager;

    private final File binDir;
    private final File libDir;
    private final String versionString;
    private final PlatformVersion version;

    /**
     * @return The latest (highest version number) PG installation
     */
    static PostgreSQLInstallation latest()
    {
        return latestInstallation;
    }

    /**
     * @return The PG installation associated with the current project
     */
    static PostgreSQLInstallation getInstallationForProject(File dataDir)
    {
        final String version = getPostgresVersion(dataDir);

        if (version != null)
        {
            final PlatformVersion projectVersion = new PlatformVersion(version);
            final PostgreSQLInstallation installation = postgreSQLInstallations.get(projectVersion);

            if (installation == null)
            {
                logger.error("Cannot find Postgres installation version [{}] for project: {}", version, dataDir);
                throw new MissingDatabasePathException("Cannot locate required version of PostgreSQL: " + version);
            }

            return installation;
        } else
        {
            return latestInstallation;
        }
    }

    /**
     * @return The version of the postgres database in the given data directory (or {@code null} if there isn't one).
     */
    private static String getPostgresVersion(File dataDir)
    {
        if (dataDir.exists())
        {
            try
            {
                final File versionFile = new File(dataDir, PG_VERSION_FILE_NAME);
                return Files.readAllLines(versionFile.toPath()).get(0);
            } catch (IOException e)
            {
                logger.error("Unable to acquire Postgres version.", e);
            }
        } else
        {
            logger.info("The Postgres data directory does not exist. This may be a new project.");
        }
        return null;
    }

    /**
     * Gather data on all versions of Postgres available in the 'programs' directory.
     *
     * @return A map from each available Postgres version to its location and version info.
     */
    private static NavigableMap<PlatformVersion, PostgreSQLInstallation> findInstallations()
    {
        final NavigableMap<PlatformVersion, PostgreSQLInstallation> installations = new TreeMap<>();

        try (Stream<Path> files = Files.list(programsDir.toPath()))
        {
            files.filter(path -> path.toFile().isDirectory())
                    .forEach(path -> {
                        final String fileName = path.getFileName().toString();

                        // Looking for directories like '9.6.6', just check for leading digit
                        if (Character.isDigit(fileName.charAt(0)))
                        {
                            logger.debug("Found Postgres version {} at {}", fileName, path);

                            final PostgreSQLInstallation installation = new PostgreSQLInstallation(path.toFile(), fileName);
                            final PlatformVersion installationVersion = installation.getVersion();
                            installations.put(installationVersion, installation);

                            // As of PG 13 (maybe earlier), the version stored in 'pgdata/PG_VERSION' is just the major
                            // version number like '13'.  So, also store the installation under that.

                            final PlatformVersion majorVersion = new PlatformVersion(installationVersion.getMajor(), 0, 0);
                            installations.put(majorVersion, installation);
                        }
                    });

            return installations;
        } catch (IOException e)
        {
            final String message = "Failed to scan: " + programsDir;
            logger.error(message, e);
            throw new MissingDatabasePathException(message);
        }
    }

    private PostgreSQLInstallation(File dir, String versionString)
    {
        // We want just the major.minor, so strip off the last portion
        final int index = versionString.lastIndexOf('.');
        if (index <= 0) throw new IllegalArgumentException("Invalid Postgres version string: " + versionString);

        version = new PlatformVersion(versionString.substring(0, index));
        binDir = new File(dir, "bin");
        libDir = new File(dir, "lib");
        this.versionString = versionString;

        logger.debug("PG version {}, root dir: {}", version, dir);
    }

    /**
     * @return 'bin' directory for the installation
     */
    public File getBinDir()
    {
        return binDir;
    }

    /**
     * @return Map of environment variables to set on spawned PG sub-processes.
     * @since 5.1
     */
    @Nonnull
    public Map<String, String> getEnvironment()
    {
        return Map.of("LD_LIBRARY_PATH", libDir.toString());
    }

    /**
     * @return File referencing the requested binary (appending '.exe' on Windows).
     */
    public String getBinary(String binary)
    {
        final File binPath = SystemUtils.IS_OS_WINDOWS
                ? new File(binDir, binary + ".exe")
                : new File(binDir, binary);

        if (!binPath.exists())
        {
            throw new MissingDatabasePathException("Postgres directory [" + binDir + "] exists but is missing a binary: " + binary);
        }

        return binPath.getPath();
    }

    /**
     * @return Our version as a string
     */
    public String getVersionString()
    {
        return versionString;
    }

    /**
     * @return Our version as a {@link PlatformVersion}
     */
    public PlatformVersion getVersion()
    {
        return version;
    }

    @Override
    public boolean equals(Object o)
    {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        return version.equals(((PostgreSQLInstallation) o).version);
    }

    @Override
    public int hashCode()
    {
        return version.hashCode();
    }
}
