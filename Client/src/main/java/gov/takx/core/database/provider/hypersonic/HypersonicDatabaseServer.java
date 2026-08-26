package gov.takx.core.database.provider.hypersonic;

import gov.takx.api.annotation.PlatformUnderTest;
import gov.takx.api.annotation.PseudoBean;
import gov.takx.api.configuration.IApplicationInstance;
import gov.takx.api.persist.IDatabaseConnectionManager;
import gov.takx.core.database.provider.ADatabaseServer;
import gov.takx.core.database.provider.ServerSpecification;
import gov.takx.platform.preferences.PreferencesNode;
import jakarta.annotation.PostConstruct;
import jakarta.inject.Inject;
import org.hibernate.dialect.HSQLDialect;
import org.hsqldb.Database;
import org.hsqldb.server.Server;
import org.hsqldb.server.ServerConstants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.invoke.MethodHandles;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

import static org.hibernate.cfg.AvailableSettings.DIALECT;
import static org.hibernate.cfg.AvailableSettings.URL;

/**
 * Database server for HSQLDB.  This "server" never actually creates an external process.  It simply relies
 * on the normal URL handling of the HSQLDB JDBC driver and an "in-process" server.
 *
 * @since 3.0
 */
@SuppressWarnings("CdiManagedBeanInconsistencyInspection")
@PseudoBean
class HypersonicDatabaseServer extends ADatabaseServer
{
    private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    private static final String JDBC_PFX = "jdbc:hsqldb:";
    private static final String JDBC_SERVER_PFX = "jdbc:hsqldb:hsql://localhost:";
    private static final String DB_DIRECTORY = "hsql_data";

    private static final String CORE_STORAGE = "coreStorage";
    private static final String TEST_STORAGE = "testStorage";

    private static final String MEM = "mem";
    private static final String FILE = "file";
    private static final String SERVER = "server";

    private static final Map<String, String> DEFAULT_PROPERTIES = Map.of(
            // Our transaction handling config is not changeable
            "hsqldb.tx_conflict_rollback", "true",
            "hsqldb.tx", "mvcc",

            // When using FILE storage, cache the tables (meaning write them to disk).  If this is not set, then
            // all the tables are actually kept in memory (uncached), and a script to regenerate them is dumped
            // when the DB is shutdown, and the DB content is rebuilt from that when it is opened again.  We don't want to
            // pay the price of that reconstruction.
            "hsqldb.default_table_type", "cached");

    // In server mode, we need to specify our properties as a string appended to the DB file path.
    // Appending them to the JDBC URL doesn't work as it does for direct (non-server) usage.
    private static final String DEFAULT_PROPS_AS_STRING = DEFAULT_PROPERTIES.entrySet().stream()
            .map(e -> e.getKey() + "=" + e.getValue())
            .collect(Collectors.joining(";"));

    @Inject
    private IApplicationInstance appInstance;

    @Inject
    @PlatformUnderTest
    private boolean platformUnderTest;

    private final String databaseFilePath = getRootDirectory().getAbsolutePath() + "/" + DB_DIRECTORY + "/" + CORE_DB_NAME;

    private boolean serverMode;
    private int serverPort;
    private Server server;

    HypersonicDatabaseServer(ServerSpecification serverSpecification)
    {
        super(serverSpecification);
    }

    @PostConstruct
    private void postConstruct()
    {
        serverMode = !platformUnderTest && SERVER.equalsIgnoreCase(getConfiguration().get(CORE_STORAGE, FILE));
        serverPort = ServerConstants.SC_DEFAULT_HSQL_SERVER_PORT + appInstance.getInstanceNumber();
    }

    @Override
    protected IDatabaseConnectionManager createDatabaseConnectionManager()
    {
        return new HypersonicDatabaseConnectionManager(makeSettings());
    }

    @Override
    public long start()
    {
        // In server mode, we start up an in-process server to allow access to the DB from external tools,
        // like the HSQL database manager.
        if (serverMode)
        {
            logger.info("SERVER MODE: Starting HSQL Server on port {}", serverPort);

            server = new Server();
            server.setSilent(!logger.isTraceEnabled()); // Setting trace will log all SQL statements
            server.setAddress("localhost");
            server.setPort(serverPort);
            server.setDatabasePath(0, databaseFilePath + ";" + DEFAULT_PROPS_AS_STRING);
            server.setDatabaseName(0, CORE_DB_NAME);
            server.start();
        }

        return NO_PID; // No external process is created
    }

    @Override
    public void shutdown()
    {
        // If we're not running a server, then we need to execute a SQL SHUTDOWN command
        if (server == null)
        {
            try (Connection connection = getConnectionManager().getConnection(); Statement stmt = connection.createStatement())
            {
                stmt.execute("SHUTDOWN");
            } catch (SQLException e)
            {
                logger.warn("Failed to SHUTDOWN", e);
            }
        }

        super.shutdown();

        if (server != null)
        {
            server.shutdownWithCatalogs(Database.CLOSEMODE_NORMAL);
            server = null;
        }
    }

    @Override
    protected void createDatabase()
    {
        // Nothing to do, HSQLDB creates the database automatically upon first connection
    }

    @Override
    public void dumpData()
    {
    }

    @Override
    public void restoreData()
    {
    }

    /**
     * @return Database/connection settings for the given database name
     */
    private Map<String, Object> makeSettings()
    {
        final PreferencesNode configuration = getConfiguration();

        final String jdbcURL;

        if (serverMode)
        {
            jdbcURL = JDBC_SERVER_PFX + serverPort + "/" + CORE_DB_NAME;
        } else
        {
            // Storage during tests is specified via the 'testStorage' property
            final String storage = configuration.get(platformUnderTest ? TEST_STORAGE : CORE_STORAGE, FILE);

            // Have to specify our properties in the URL when running embedded
            final var configurationAsMap = configuration.keyValuesAsMap();
            configurationAsMap.putAll(DEFAULT_PROPERTIES);

            final var urlBuilder = new StringBuilder();
            urlBuilder.append(JDBC_PFX)
                    .append(storage)
                    .append(':')
                    .append(MEM.equals(storage) ? CORE_DB_NAME : databaseFilePath);

            configurationAsMap.forEach((key, value) -> {
                if (key.startsWith("hsqldb")) urlBuilder.append(";").append(key).append("=").append(value);
            });

            jdbcURL = urlBuilder.toString();
        }

        logger.debug("JDBC URL: {}", jdbcURL);

        final Map<String, Object> connectionProperties = new HashMap<>();
        connectionProperties.put(URL, jdbcURL);
        connectionProperties.put(DIALECT, HSQLDialect.class.getName());

        return connectionProperties;
    }
}
