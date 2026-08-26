package gov.takx.core.database.provider.postgres;

import com.atakmap.net.AtakAuthenticationCredentials;
import com.atakmap.net.AtakAuthenticationDatabase;
import gov.takx.api.annotation.InvokeAndWaitOnEDT;
import gov.takx.api.persist.IDatabaseConnectionManager;
import gov.takx.commons.swing.AlwaysOnTopJOptionPane;
import gov.takx.core.database.exception.LoadCancelledException;
import gov.takx.core.database.provider.ADatabaseServer;
import gov.takx.core.database.provider.ServerSpecification;
import org.hibernate.dialect.PostgreSQLDialect;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.JFrame;
import javax.swing.JOptionPane;

import java.io.File;
import java.lang.invoke.MethodHandles;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.hibernate.cfg.AvailableSettings.DIALECT;
import static org.hibernate.cfg.AvailableSettings.PASS;
import static org.hibernate.cfg.AvailableSettings.URL;
import static org.hibernate.cfg.AvailableSettings.USER;

/**
 * Database server for PostgreSQL.
 *
 * @since 3.0
 */
class PostgresDatabaseServer extends ADatabaseServer
{
    private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    private static final String PGDATA = "pgdata";
    private static final String USERNAME = "takx";
    private static final String CREDENTIAL_KEY = "TAKX_PG_CREDENTIALS"; // Key into AtakAuthenticationDatabase

    private static final PostgreSQLInstallation latestInstallation = PostgreSQLInstallation.latest();

    /**
     * @return the Postgres JDBC URL using the given {@code port} and {@code databaseName}.
     * @since 5.4
     */
    static String makeJdbcUrl(int port, String databaseName)
    {
        return "jdbc:postgresql://127.0.0.1:" + port + "/" + databaseName;
    }

    private final Map<String, Object> connectionProperties = new HashMap<>();
    private final int port;
    private final PostgreSQLInstance postgresqlInstance;

    private boolean running;
    private boolean upgraded;

    PostgresDatabaseServer(ServerSpecification serverSpecification, int port)
    {
        super(serverSpecification);

        this.port = port;

        final String serverUrl = makeJdbcUrl(port, CORE_DB_NAME);
        logger.debug("JDBC URL: {}", serverUrl);

        final var credentials = getCredentials();

        connectionProperties.put(DIALECT, PostgreSQLDialect.class.getName());
        connectionProperties.put(URL, serverUrl);
        connectionProperties.put(USER, credentials.username);
        connectionProperties.put(PASS, credentials.password);

        // This is the instance that will (eventually) be used to open the project DB
        postgresqlInstance = new PostgreSQLInstance(latestInstallation, port, serverSpecification, credentials);
    }

    /**
     * Get the credentials to use for accessing the core database.  If it is not already set, one is created
     * on demand and stored in our encrypted authentication database.
     *
     * @since 5.4
     */
    private static AtakAuthenticationCredentials getCredentials()
    {
        final var credentials = AtakAuthenticationDatabase.getCredentials(CREDENTIAL_KEY);

        if (credentials != null)
        {
            return credentials;
        } else
        {
            // Need to create a new random password
            final String uuid = UUID.randomUUID().toString();
            AtakAuthenticationDatabase.saveCredentials(CREDENTIAL_KEY, USERNAME, uuid, false);
            return AtakAuthenticationDatabase.getCredentials(CREDENTIAL_KEY);
        }
    }

    @Override
    protected IDatabaseConnectionManager createDatabaseConnectionManager()
    {
        // Determine if we need to upgrade this project
        final var dataDir = new File(getRootDirectory(), PGDATA);
        final var projectInstallation = PostgreSQLInstallation.getInstallationForProject(dataDir);

        if (!projectInstallation.equals(latestInstallation))
        {
            final var sourceInstance = new PostgreSQLInstance(projectInstallation, port, getServerSpecification(), getCredentials());
            convertPGData(sourceInstance, postgresqlInstance);
            upgraded = true;
        }

        return new PostgresDatabaseConnectionManager(connectionProperties, postgresqlInstance);
    }

    @Override
    public boolean isUpgraded()
    {
        return upgraded;
    }

    @Override
    public long start()
    {
        try
        {
            final long start = System.currentTimeMillis();

            running = postgresqlInstance.start();

            if (logger.isDebugEnabled()) logger.debug("start() took {} ms", System.currentTimeMillis() - start);

            final Long pid = postgresqlInstance.getPid();
            return pid != null ? pid : NO_PID; // Avoid an NPE if we couldn't get the PID for some reason
        } catch (InterruptedException e)
        {
            Thread.currentThread().interrupt();
        }

        return NO_PID;
    }

    @Override
    public void shutdown()
    {
        super.shutdown();

        if (running)
        {
            logger.debug("stopping database instance");
            postgresqlInstance.stop();
            running = false;
        }
    }

    @Override
    protected void createDatabase()
    {
        postgresqlInstance.createDatabase();
    }

    @Override
    public List<String> getPackageExcludes()
    {
        return List.of(PGDATA);
    }

    @Override
    public void dumpData()
    {
        postgresqlInstance.dump();
    }

    @Override
    public void restoreData()
    {
        postgresqlInstance.restore();
    }

    /**
     * Ask the user if they want to upgrade Postgres.
     *
     * @return The JOptionPane response value.
     */
    @InvokeAndWaitOnEDT
    private int showUpgradePostgresConfirmationDialog(String sourceVersion, String targetVersion)
    {
        return AlwaysOnTopJOptionPane.showConfirmDialog(JFrame.getFrames()[0],
                "To open this project, the Postgres database files need to be upgraded from " + sourceVersion + " to " +
                        targetVersion + ".\n" +
                        "You will no longer be able to open the project in older versions of this application.\n\n" +
                        "Would you like to continue?",
                "Upgrade PostgreSQL Data",
                JOptionPane.YES_NO_OPTION);
    }

    /**
     * Upgrade the given DB project instance to the configured target version.
     */
    private void convertPGData(PostgreSQLInstance sourceInstance, PostgreSQLInstance targetInstance)
    {
        final String sourceVersion = sourceInstance.getInstallation().getVersionString();
        final String targetVersion = targetInstance.getInstallation().getVersionString();

        int ret = showUpgradePostgresConfirmationDialog(sourceVersion, targetVersion);

        if (ret != JOptionPane.YES_OPTION)
        {
            logger.debug("User chose not to upgrade upgrade.");
            throw new LoadCancelledException("User chose not to upgrade Postgres");
        }

        logger.info("converting pgdata from {} to {}", sourceVersion, targetVersion);

        try
        {
            targetInstance.upgradeFrom(sourceInstance.getInstallation());
        } catch (Exception e)
        {
            final String message = "Upgrade process failed when upgrading from Postgres "
                    + sourceVersion + " to " + targetVersion;

            logger.error(message, e);
            throw new LoadCancelledException(message);
        }
    }
}
