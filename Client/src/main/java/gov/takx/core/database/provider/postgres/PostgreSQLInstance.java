package gov.takx.core.database.provider.postgres;

import com.atakmap.net.AtakAuthenticationCredentials;
import com.google.common.util.concurrent.Uninterruptibles;
import gov.takx.commons.file.FileUtils;
import gov.takx.core.database.exception.DatabaseIOException;
import gov.takx.core.database.exception.MissingRestoreFileException;
import gov.takx.core.database.provider.ServerSpecification;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import org.apache.commons.lang3.SystemUtils;
import org.postgresql.ds.PGSimpleDataSource;
import org.postgresql.util.PSQLException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.lang.invoke.MethodHandles;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.text.NumberFormat;
import java.text.ParseException;
import java.util.concurrent.TimeUnit;

import static gov.takx.core.database.provider.ADatabaseServer.CORE_DB_NAME;

/**
 * Represents an instance of PostgreSQL for a given project directory, running locally.
 * <p>
 * Methods here offer general management operations on the instance, like starting, stopping,
 * initializing, exporting, etc.
 */
public class PostgreSQLInstance
{
    private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    private static final String PGDATA = "pgdata";
    private static final String UPGRADED_PGDATA = "upgraded_pgdata";
    private static final String POSTGRES_PID_FILE = "postmaster.pid";
    private static final String SUPERUSER_FILE_NAME = "superuser";
    private static final String DEVELOPER = "developer";
    private static final String LOCAL_HOST = "127.0.0.1"; // We are always local
    private static final String DB_EXISTS_QUERY = "SELECT datname FROM pg_catalog.pg_database WHERE lower(datname) = '" + CORE_DB_NAME.toLowerCase() + "'";

    private static final long START_DB_TIMEOUT_MS = TimeUnit.SECONDS.toMillis(20);
    private static final long STOP_DB_TIMEOUT_MS = TimeUnit.SECONDS.toMillis(10);

    private static final InetAddress LOOPBACK_ADDRESS = InetAddress.getLoopbackAddress();

    private final PostgreSQLInstallation installation;

    private final int port;
    private final AtakAuthenticationCredentials credentials;

    private final File rootDir;
    private final File dataDir;
    private String oldAuthSuperuser;

    /**
     * Construct an instance.
     *
     * @param installation        The installation holding the PG executables
     * @param port                The port that the server should listen on
     * @param serverSpecification Server specification (root dir, properties, etc.)
     * @param credentials         Core user credentials
     */
    PostgreSQLInstance(PostgreSQLInstallation installation, int port, ServerSpecification serverSpecification, AtakAuthenticationCredentials credentials)
    {
        this.installation = installation;
        this.port = port;
        this.credentials = credentials;

        rootDir = serverSpecification.rootDir();
        dataDir = new File(rootDir, PGDATA);

        checkForOldAuthModel();

        logger.info("Using PostgreSQL {}", installation.getVersionString());
    }

    /**
     * If we're accessing an existing database that contains an old {@code superuser} file, then we will need to create
     * the new 'takx' core superuser once the DB server is started.  Pull out the current superuser name for later use.
     *
     * @since 5.4
     */
    private void checkForOldAuthModel()
    {
        final File suFile = new File(dataDir, SUPERUSER_FILE_NAME);
        if (suFile.exists())
        {
            try
            {
                // Grab old superuser name for later
                oldAuthSuperuser = Files.readString(suFile.toPath()).trim();
            } catch (IOException e)
            {
                logger.warn("Failed to read old auth credentials from {}", suFile, e);
            } finally
            {
                // Remove this file to avoid doing this twice
                if (!suFile.delete()) logger.warn("Failed to delete {}", suFile.getAbsolutePath());
            }
        }
    }

    PostgreSQLInstallation getInstallation()
    {
        return installation;
    }

    /**
     * Start the PG server instance, initializing the data directory if it does not exist.
     *
     * @return {@code true} if the server was started
     */
    public boolean start() throws InterruptedException
    {
        checkForRogueServer();

        final boolean newDatabase = !dataDir.exists();

        try
        {
            if (newDatabase) initDB(dataDir);

            startPostgreSQL();

            // Upgrade existing projects if they are using the old authentication model
            if (!newDatabase && oldAuthSuperuser != null) addTakxRole();

            logger.debug("POSTGRES START STATUS OK");
            return true;
        } catch (IOException | SQLException e)
        {
            logger.error("Error: ", e);
        }

        return false;
    }

    /**
     * Create the 'takx' superuser role.
     * @since 5.4
     */
    @SuppressWarnings("SqlSourceToSinkFlow")
    private void addTakxRole()
    {
        final String roleName = credentials.username;

        // Get the connection using the old superuser name
        try (var connection = getPGConnection(oldAuthSuperuser, null, "postgres"))
        {
            final boolean roleExists = queryResultExists(connection, "SELECT 1 FROM pg_roles WHERE rolname='" + roleName + "'");
            logger.debug("role '{}' exists: {}", roleName, roleExists);

            if (!roleExists)
            {
                logger.debug("Create role '{}'", roleName);

                try (var stmt = connection.createStatement())
                {
                    stmt.executeUpdate("begin work");
                    stmt.executeUpdate(String.format("CREATE USER %s SUPERUSER LOGIN PASSWORD '%s'", roleName, credentials.password));
                    stmt.executeUpdate("commit work");
                } catch (SQLException e)
                {
                    logger.error("Error creating role '{}'", roleName, e);
                }
            }
        } catch (Exception e)
        {
            logger.error("Error creating role '{}'", roleName, e);
        }
    }

    /**
     * @return {@code true} if the given query has a result
     * @since 5.4
     */
    private boolean queryResultExists(@Nonnull Connection connection, @Nonnull String query)
    {
        try (var stmt = connection.createStatement(); var rs = stmt.executeQuery("select exists(" + query + ")"))
        {
            rs.next();
            return rs.getBoolean(1);
        } catch (SQLException e)
        {
            logger.warn("Failed to test for existence; query={}", query, e);
            return false;
        }
    }

    /**
     * See if there is a roque server using our port, but not running in our data directory.
     */
    private void checkForRogueServer()
    {
        // When we start, see if we can connect to our configured host/port.
        // If so, then we need to terminate that instance.

        if (!testConnect()) return; // Try fast connect first

        // Now try for a real JDBC connection
        try (Connection connection = getPGConnection())
        {
            final String dataDir = getCurrentRunningDataDir(connection);

            if (dataDir != null)
            {
                logger.warn("There appears to be another instance of Postgres running that's associated with the data " +
                        "directory {}. Attempting to kill the process.", dataDir);

                // If the database reports that it is not running, but yet we are seeing a running data dir
                // That can only mean that the wrong data dir is actually running and that we should therefore
                // KILL IT!!!
                kill(new File(dataDir));
            }
        } catch (SQLException ignore)
        {
            // Expected case
        }
    }

    /**
     * @return {@code true} if a socket connection can be made to the server (faster than making a JDBC connection)
     */
    private boolean testConnect()
    {
        try (Socket socket = new Socket())
        {
            // Use a short timeout on this test.  Since the server is always local, there are no
            // network delays to worry about, and if it is running, the connection time should be just a few
            // milliseconds.  No reason to wait for the default 2 seconds.
            socket.connect(new InetSocketAddress(LOOPBACK_ADDRESS, port), 50);
            return true;
        } catch (IOException e)
        {
            return false; // Apparently not
        }
    }

    /**
     * Get the current data directory from the current running postgres process.
     *
     * @param connection The DB connection to use
     * @return directory path
     */
    private String getCurrentRunningDataDir(Connection connection)
    {
        String path = null;

        if (connection == null) return null;

        try
        {
            try (Statement stmt = connection.createStatement();
                 ResultSet rs = stmt.executeQuery("show data_directory"))
            {
                if (rs.next())
                {
                    path = rs.getString(1);
                }
            }
        } catch (SQLException e)
        {
            logger.error("Error: ", e);
        }

        return path;
    }

    /**
     * Create the core database.
     */
    public void createDatabase()
    {
        try (Connection connection = getPGConnection())
        {
            // See if the database already exists
            if (!queryResultExists(connection, DB_EXISTS_QUERY))
            {
                try (Statement stmt = connection.createStatement())
                {
                    stmt.execute("CREATE DATABASE \"" + CORE_DB_NAME + "\"");
                    logger.debug("Created database '{}'", CORE_DB_NAME);
                }

                addDeveloperUser();
            }
        } catch (SQLException e)
        {
            logger.error("Error creating database '{}'", CORE_DB_NAME, e);
            throw new DatabaseIOException(e);
        }
    }

    /**
     * Create and configure the 'developer' role such that it can read all tables, but can make no modifications.
     * @since 5.4
     */
    private void addDeveloperUser()
    {
        try (var connection = getPGConnection(credentials.username, credentials.password, CORE_DB_NAME);
             var stmt = connection.createStatement())
        {
            stmt.executeUpdate("begin work");
            stmt.executeUpdate("CREATE USER " + DEVELOPER + " NOSUPERUSER LOGIN PASSWORD '" + DEVELOPER + "'");
            stmt.executeUpdate("GRANT CONNECT ON DATABASE " + CORE_DB_NAME + " TO " + DEVELOPER);
            stmt.executeUpdate("GRANT SELECT ON ALL TABLES IN SCHEMA public TO " + DEVELOPER);
            // The next statement ensures that any future tables created in this DB are readable by the developer role
            stmt.executeUpdate("ALTER DEFAULT PRIVILEGES IN SCHEMA public GRANT SELECT ON TABLES TO " + DEVELOPER);
            stmt.executeUpdate("commit work");
            logger.debug("Added developer user");
        } catch (SQLException e)
        {
            logger.error("Error creating developer role", e);
        }
    }

    /**
     * Drop the schema (and all its tables).
     * <p>
     * This is only used during unit tests, and then only if Postgres has been configured as the DB provider.
     */
    @SuppressWarnings({"SqlResolve", "SqlSourceToSinkFlow"})
    public void dropTables(Connection connection)
    {
        logger.debug("DROPPING TABLES...");

        try (Statement stmt = connection.createStatement())
        {
            stmt.executeUpdate("begin work");

            stmt.executeUpdate("DROP SCHEMA public CASCADE");
            stmt.executeUpdate("CREATE SCHEMA public");
            stmt.executeUpdate("GRANT ALL ON SCHEMA public TO public");
            stmt.executeUpdate("GRANT ALL ON SCHEMA public TO " + credentials.username);

            stmt.executeUpdate("commit work");
        } catch (SQLException e)
        {
            logger.error("Error dropping DB tables", e);
            throw new DatabaseIOException(e);
        }
    }

    /**
     * Delete the pgdata directory.
     */
    private void deleteDataDir()
    {
        if (!FileUtils.deleteDirectory(dataDir))
        {
            final String message = "Unable to delete directory: " + dataDir.getAbsolutePath();

            logger.error(message);
            throw new UnsupportedOperationException(message);
        }
    }

    /**
     * Initialize a new PG data directory.
     * <p>
     * This should only be called if the database data dir does not exist.
     *
     * @param dataDir Directory to initialize
     */
    private void initDB(File dataDir) throws IOException
    {
        logger.debug("INITIALIZING DATABASE...");

        // Have to pass credentials in a file.  No way to do it on the CLI or via the environment.
        final Path pgPwFile = Files.createTempFile("takx_pg_", null);
        pgPwFile.toFile().deleteOnExit();
        Files.writeString(pgPwFile, credentials.password);

        // This will initialize the database with the core user as the superuser.
        final String[] cmd = {
                installation.getBinary("initdb"),
                "--locale=en_US",
                "-E UTF8",
                "-D", dataDir.getAbsolutePath(),
                "--username=" + credentials.username,
                "--auth=md5",
                "--pwfile=" + pgPwFile.toAbsolutePath(),
        };

        try
        {
            ProcessUtils.runProcess("initializing", installation.getEnvironment(), cmd, () -> null);
        } finally
        {
            // Don't leave this around any longer than necessary
            if (!pgPwFile.toFile().delete()) logger.warn("Failed to delete {}", pgPwFile.toAbsolutePath());
        }
    }

    /**
     * Start the server instance using pg_ctl.
     */
    private void startPostgreSQL() throws IOException, SQLException
    {
        // '-o "-k /tmp"' is necessary on some Linux system to override the system default location for
        // UNIX domain sockets.  The default on many distros is a directory in /var/run in which only the
        // postgres user can create files.  On Windows this option is rejected, so the command has to be
        // built differently depending on OS.

        final String socketOpt = SystemUtils.IS_OS_WINDOWS ? "" : " -k /tmp";

        // For performance, we want to enable 'asynchronous commits' and bump up the shared memory pools
        final String walOpts = " -c synchronous_commit=off -c wal_writer_delay=2000 -c shared_buffers=256MB -c wal_buffers=16MB";

        // Combine all the options that get passed to '-o'
        final String dashOOpts = "--port=" + port + socketOpt + walOpts;

        final String[] cmd = {
                installation.getBinary("pg_ctl"),
                "start",
                "-D", dataDir.getAbsolutePath(),
                "-w",
                "-t 30",
                "-o", dashOOpts
        };

        ProcessUtils.runProcess("starting", installation.getEnvironment(), cmd, () -> {
            try
            {
                // Wait until the database is ready to accept connections
                waitForDatabaseToStart();
            } catch (SQLException e)
            {
                return new IOException(e);
            }
            return null;
        },
                "The process cannot access the file because it is being used by another process.",
                "another server might be running");
    }

    /**
     * Wait for the database to start.
     *
     * @throws SQLException If the wait times out.
     */
    private void waitForDatabaseToStart() throws SQLException
    {
        if (logger.isTraceEnabled()) logger.trace("WAITING FOR POSTGRES TO START...");

        final long start = System.currentTimeMillis();

        while (true)
        {
            try (Connection ignore = oldAuthSuperuser != null
                    ? getPGConnection(oldAuthSuperuser, null, "postgres")
                    : getPGConnection())
            {
                break; // We got a connection, so the database must be up and ready for use.
            } catch (PSQLException e)
            {
                // If we exceeded the timeout, throw a wrapped exception, otherwise try again in a few milliseconds.
                if (System.currentTimeMillis() - start >= START_DB_TIMEOUT_MS)
                {
                    throw new SQLException("Timeout waiting for database to start", e);
                }

                Uninterruptibles.sleepUninterruptibly(50, TimeUnit.MILLISECONDS);
            }
        }
    }

    /**
     * @return a JDBC connection to the 'postgres' database using the core user/password.
     */
    private Connection getPGConnection() throws SQLException
    {
        return getPGConnection(credentials.username, credentials.password, "postgres");
    }

    /**
     * @return a JDBC connection to the given database. This is used for operations like starting the DB,
     * creating the target DB, etc.  This connection is not tracked, it is up to the caller to ensure it gets closed.
     *
     * @param username username to use for connection authentication
     * @param password Password to use for the given username, pass {@code null} for no password
     * @param databaseName Name of the database to connect to
     * @since 5.4
     */
    private Connection getPGConnection(@Nonnull String username, @Nullable String password, @Nonnull String databaseName) throws SQLException
    {
        final var dataSource = new PGSimpleDataSource();
        dataSource.setUrl(PostgresDatabaseServer.makeJdbcUrl(port, databaseName));
        dataSource.setUser(username);
        if (password != null) dataSource.setPassword(password);

        return dataSource.getConnection();
    }

    /**
     * Stop the PG server instance.
     */
    public void stop()
    {
        final long start = System.currentTimeMillis();

        try
        {
            logger.debug("STOPPING POSTGRES DATABASE...");

            // Attempt to stop the database in the standard manner.

            final String[] cmd = {
                    installation.getBinary("pg_ctl"),
                    "stop",
                    "-D", dataDir.getAbsolutePath(),
                    "-W",         // Do not wait for startup or shutdown to complete
                    "-m", "fast", // mode 'fast', force disconnect all clients, rollback all transactions
            };

            ProcessUtils.runProcess("stopping", installation.getEnvironment(), cmd);

            boolean doKill = false;
            final long startWait = System.currentTimeMillis();

            while (true)
            {
                // Look for the Postgres PID file. If the file is still there, assume that Postgres is still running.
                final File pidFile = new File(dataDir, POSTGRES_PID_FILE);

                if (!pidFile.exists()) break;

                Uninterruptibles.sleepUninterruptibly(50, TimeUnit.MILLISECONDS);

                if (System.currentTimeMillis() - startWait > STOP_DB_TIMEOUT_MS)
                {
                    logger.error("Timeout waiting for database to stop.");
                    doKill = true;
                    break;
                }
            }

            if (doKill) kill(dataDir); // Do a "hard" kill on the postgres process

            if (logger.isDebugEnabled()) logger.debug("Stop took {} ms", System.currentTimeMillis() - start);
        } catch (IOException e)
        {
            logger.error("Error stopping the database.", e);
        }
    }

    /**
     * Kill the PG server running within the given data directory.
     *
     * @param dataDir Data directory (for obtaining the PG process ID)
     */
    private void kill(File dataDir)
    {
        logger.info("KILLING POSTGRES...");
        try
        {
            final Long pid = getPid(dataDir);
            if (pid != null)
            {
                ProcessUtils.runProcess("killing", installation.getEnvironment(), installation.getBinary("pg_ctl"),
                        "kill",
                        "INT",
                        Long.toString(pid));
            } else
            {
                logger.warn("Unable to kill Postgres process associated with the data directory {} because the PID " +
                        "could not be determined.", dataDir.getAbsolutePath());
            }
        } catch (IOException e)
        {
            logger.error("Error trying to kill running version of Postgres.", e);
        }
    }

    /**
     * Get the instance's primary process id.
     *
     * @return The instance's process id.
     */
    public Long getPid()
    {
        return getPid(dataDir);
    }

    /**
     * Get the PID for the postgres process running in the given data directory.
     *
     * @return The PID, or {@code null} if it could not be determined
     */
    @Nullable
    private Long getPid(File dataDir)
    {
        final var pidPath = Paths.get(dataDir.getPath(), POSTGRES_PID_FILE);
        if (pidPath.toFile().canRead())
        {
            try
            {
                final var lines = Files.readAllLines(pidPath, StandardCharsets.UTF_8);
                if (!lines.isEmpty())
                {
                    final String pidLine = lines.get(0); // PID is the first line of the file
                    logger.debug("PID line: {}", pidLine);
                    return NumberFormat.getInstance().parse(pidLine).longValue();
                } else
                {
                    logger.warn("{} is empty", pidPath);
                }
            } catch (IOException | ParseException e)
            {
                logger.error("Error getting server PID from {}: ", pidPath, e);
            }
        } else
        {
            logger.debug("No PID file: {}", pidPath);
        }
        return null;
    }

    /**
     * Upgrade the database from a given installation (version) to our current/target version.
     *
     * @param sourceInstallation Current installation (which created the existing pgdata contents)
     */
    void upgradeFrom(PostgreSQLInstallation sourceInstallation) throws IOException
    {
        logger.debug("UPGRADING DATABASE...");

        final Path newPgData = Paths.get(rootDir.getPath(), UPGRADED_PGDATA);

        initDB(newPgData.toFile()); // Initialize a new (temporary) pgdata dir as the target for the upgrade

        // Now use pg_upgrade to convert the data
        final String[] cmd = {
                installation.getBinary("pg_upgrade"),
                "--old-datadir", dataDir.getPath(),
                "--old-bindir", sourceInstallation.getBinDir().getPath(),
                "--new-datadir", newPgData.toString(),
                "--new-bindir", installation.getBinDir().getPath(),
        };

        ProcessUtils.runProcess("upgrade", installation.getEnvironment(), cmd, () -> {
            try
            {
                // Now that the data has been upgraded, we need to replace the old data dir with the new one
                deleteDataDir();

                Files.move(newPgData, newPgData.resolveSibling(PGDATA));
            } catch (IOException e)
            {
                return e;
            }
            return null;
        });
    }

    /**
     * @return The file to read/write for database restore/dump operations
     */
    private File getRestoreFile()
    {
        return new File(rootDir, CORE_DB_NAME + "_db.tar");
    }

    /**
     * Dump the content of the database.
     */
    public void dump()
    {
        logger.info("DUMPING DATA...");
        try
        {
            runWithAuthEnvironment("dumping",
                    installation.getBinary("pg_dump"),
                    "-h", LOCAL_HOST,
                    "-p", String.valueOf(port),
                    "-U", credentials.username,
                    "--no-password", // Password will be in the environment
                    "--format=t", // tar file format
                    "--no-owner", // Prevents errors when importing to a DB with a different superuser name
                    "-f", getRestoreFile().getPath(),
                    CORE_DB_NAME);
        } catch (IOException | RuntimeException e)
        {
            logger.error("Error: ", e);
            throw new DatabaseIOException(e);
        }
    }

    /**
     * Restore the contents of a dump file to the named database.
     */
    public void restore()
    {
        final File archiveFile = getRestoreFile();
        if (!archiveFile.exists())
        {
            final String msg = "Restore file does not exist: " + archiveFile;
            logger.warn(msg);
            throw new MissingRestoreFileException(msg);
        }

        logger.info("Starting restore of: {}", archiveFile.getAbsolutePath());

        final long start = System.currentTimeMillis();
        try
        {
            createDatabase(); // Create the database to restore into

            runWithAuthEnvironment("restoring",
                    installation.getBinary("pg_restore"),
                    "-h", LOCAL_HOST,
                    "-p", String.valueOf(port),
                    "--no-owner",
                    "-U", credentials.username,
                    "--no-password", // Password will be in the environment
                    "--format=t",
                    "--dbname=" + CORE_DB_NAME,
                    archiveFile.getPath());

            if (logger.isDebugEnabled()) logger.debug("Restore took {} ms", System.currentTimeMillis() - start);
        } catch (IOException | RuntimeException e)
        {
            logger.error("Error: ", e);
            throw new DatabaseIOException(e.getMessage());
        }
    }

    /**
     * Run the given {@code cmd} with an environment augmented with the required authentication credentials.
     *
     * @param tag Descriptive tag for debug/error output
     * @param cmd Command to execute
     * @since 5.4
     */
    private void runWithAuthEnvironment(String tag, String... cmd) throws IOException
    {
        final var environment = installation.getEnvironment();
        environment.put("PGPASSWORD", credentials.password);

        ProcessUtils.runProcess(tag, environment, cmd, () -> {
            logger.info("{} complete", tag);
            return null;
        });
    }
}
