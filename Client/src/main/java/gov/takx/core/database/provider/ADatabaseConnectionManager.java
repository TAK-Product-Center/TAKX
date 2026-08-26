package gov.takx.core.database.provider;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.PropertyAccessor;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.module.SimpleModule;
import gov.tak.api.engine.map.coords.IGeoPoint;
import gov.tak.api.maps.coords.GeoPoint;
import gov.tak.api.util.AttributeSet;
import gov.takx.api.coordinatesystem.ILocation;
import gov.takx.api.coordinatesystem.RaptorLocation;
import gov.takx.api.messages.IPrePersistRaptorDataMessage;
import gov.takx.api.messages.IStructure;
import gov.takx.api.persist.IDatabaseConnectionManager;
import gov.takx.core.database.DatabaseManager;
import gov.takx.dataservices.upgrade.UpgradeStatement;
import gov.takx.platform.utils.json.AttributeSetDeserializer;
import gov.takx.platform.utils.json.AttributeSetSerializer;
import gov.takx.platform.utils.json.ColorDeserializer;
import gov.takx.platform.utils.json.ColorSerializer;
import gov.takx.platform.utils.json.GeoPointDeserializer;
import gov.takx.platform.utils.json.IGeoPointDeserializer;
import gov.takx.platform.utils.json.IGeoPointSerializer;
import gov.takx.platform.utils.json.LocationDeserializer;
import gov.takx.platform.utils.json.RaptorLocationSerializer;
import org.hibernate.HibernateException;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.StatelessSession;
import org.hibernate.Transaction;
import org.hibernate.boot.Metadata;
import org.hibernate.boot.MetadataSources;
import org.hibernate.boot.registry.BootstrapServiceRegistry;
import org.hibernate.boot.registry.BootstrapServiceRegistryBuilder;
import org.hibernate.boot.registry.StandardServiceRegistryBuilder;
import org.hibernate.cfg.AvailableSettings;
import org.hibernate.dialect.Dialect;
import org.hibernate.engine.jdbc.connections.spi.ConnectionProvider;
import org.hibernate.engine.jdbc.spi.JdbcServices;
import org.hibernate.service.ServiceRegistry;
import org.hibernate.tool.hbm2ddl.SchemaExport;
import org.hibernate.tool.schema.TargetType;
import org.hibernate.type.jackson.JacksonJsonFormatMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.Color;
import java.lang.invoke.MethodHandles;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

/**
 * Abstract base for database connection managers.  This base class is responsible for creating the hibernate
 * bootstrap classes (the service registry and metadata), and then creating a session factory for use to create
 * sessions for consumers like the persist service.  It tracks all sessions (normal and stateless) created,
 * and will close them when {@link #close()} or {@link #closeAllSessions()} is called.
 *
 * @since 3.0 - existed previously as DefaultDatabaseConnectionManager
 */
public abstract class ADatabaseConnectionManager implements IDatabaseConnectionManager
{
    private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    private static final Map<String, Object> HIBERNATE_SETTINGS = new HashMap<>();
    private static final ObjectMapper customObjectMapper = new ObjectMapper();

    static
    {
        // Instead of reading these settings from the standard hibernate.cfg.xml, we set them programmatically.
        // Primarily because it removes a potential XML XXE attack vector (MITRE finding), and we rarely (if ever)
        // need to change them in the field since changing most of these options would break operations.

        if (logger.isTraceEnabled())
        {
            HIBERNATE_SETTINGS.put("hibernate.show_sql", "true");
            HIBERNATE_SETTINGS.put("hibernate.use_sql_comments", "true");
        }

        HIBERNATE_SETTINGS.put("hibernate.archive.autodetection", "class");
        HIBERNATE_SETTINGS.put("hibernate.hbm2ddl.auto", "none");

        HIBERNATE_SETTINGS.put("javax.persistence.validation.mode", "none"); // No validation of schema

        // Use "component path" naming for handling multiple instances of embeddable types in a single
        // entity, which happens in our ShapeMapObject hierarchy.
        HIBERNATE_SETTINGS.put("hibernate.implicit_naming_strategy", "component-path");

        // Don't close the connection until the session is closed
        HIBERNATE_SETTINGS.put("hibernate.connection.handling_mode", "DELAYED_ACQUISITION_AND_HOLD");
        HIBERNATE_SETTINGS.put("hibernate.connection.autocommit", "false");
        HIBERNATE_SETTINGS.put("hibernate.jdbc.batch_size", "50");
        HIBERNATE_SETTINGS.put("hibernate.jdbc.use_streams_for_binary", "false");
        HIBERNATE_SETTINGS.put("hibernate.temp.use_jdbc_metadata_defaults", "false");

        // Enable automatic session context management
        HIBERNATE_SETTINGS.put("hibernate.current_session_context_class", "thread");

        // No 2nd level cache
        HIBERNATE_SETTINGS.put("hibernate.cache.use_second_level_cache", "false");
        HIBERNATE_SETTINGS.put("hibernate.cache.use_query_cache", "false");

        // connection pool
        HIBERNATE_SETTINGS.put("hibernate.connection.provider_class", "org.hibernate.hikaricp.internal.HikariCPConnectionProvider");
        HIBERNATE_SETTINGS.put("hibernate.hikari.minimumIdle", "2");
        HIBERNATE_SETTINGS.put("hibernate.hikari.maximumPoolSize", "50");

        final SimpleModule module = new SimpleModule();
        module.addSerializer(GeoPoint.class, new IGeoPointSerializer());
        module.addDeserializer(GeoPoint.class, new GeoPointDeserializer());
        module.addSerializer(IGeoPoint.class, new IGeoPointSerializer());
        module.addDeserializer(IGeoPoint.class, new IGeoPointDeserializer());
        module.addSerializer(Color.class, new ColorSerializer());
        module.addDeserializer(Color.class, new ColorDeserializer());
        module.addSerializer(RaptorLocation.class, new RaptorLocationSerializer());
        module.addDeserializer(ILocation.class, new LocationDeserializer());
        module.addSerializer(AttributeSet.class, new AttributeSetSerializer());
        module.addDeserializer(AttributeSet.class, new AttributeSetDeserializer());

        customObjectMapper.registerModule(module);
        customObjectMapper.setVisibility(PropertyAccessor.ALL, JsonAutoDetect.Visibility.NONE);
        customObjectMapper.setVisibility(PropertyAccessor.FIELD, JsonAutoDetect.Visibility.ANY);
        HIBERNATE_SETTINGS.put(AvailableSettings.JSON_FORMAT_MAPPER, new JacksonJsonFormatMapper(customObjectMapper));
    }

    // All open sessions
    private final Set<Session> sessions = ConcurrentHashMap.newKeySet();

    // The bootstrap and session factory for creating connections
    private final ServiceRegistry serviceRegistry;
    private final Metadata metadata;
    protected volatile SessionFactory factory;

    /**
     * Construct a connection manager for the core database using the provided hibernate settings.
     * This performs the initial boostrap setup, creating the service registry and Metadata.
     *
     * @param settings Hibernate settings to use during hibernate bootstrap
     */
    protected ADatabaseConnectionManager(Map<String, Object> settings)
    {
        // Don't log our password
        if (logger.isDebugEnabled()) logger.debug("settings: {}", settings.entrySet().stream().filter(e -> !e.getKey().contains("password")).toList());

        // Install our class loader to get optimal performance when hibernate does class lookups
        final BootstrapServiceRegistry bootstrapServiceRegistry = new BootstrapServiceRegistryBuilder()
                .applyClassLoader(getClass().getClassLoader())
                .build();

        // Construct our service registry
        serviceRegistry = new StandardServiceRegistryBuilder(bootstrapServiceRegistry)
                .applySettings(HIBERNATE_SETTINGS) // Apply default settings
                .applySettings(settings) // Apply server-specific settings
                .build();

        // Construct the metadata, essentially information about the collection of mapped classes
        final MetadataSources sources = new MetadataSources(serviceRegistry);
        DatabaseManager.getMappedClasses().forEach(sources::addAnnotatedClass);
        metadata = sources.buildMetadata();
    }

    /**
     * Attempt to get a JDBC Connection from the Session.
     *
     * @return Connection based on the URL, username, and password from the configuration properties.
     * @throws SQLException when the connection fails
     */
    @Override
    public Connection getConnection() throws SQLException
    {
        openFactory();
        return serviceRegistry.getService(ConnectionProvider.class).getConnection();
    }

    /**
     * Open the session factory, creating it if necessary.
     */
    protected synchronized void openFactory()
    {
        if (factory == null)
        {
            final long start = System.currentTimeMillis();
            factory = metadata.getSessionFactoryBuilder().build();

            if (logger.isDebugEnabled()) logger.debug("SF creation took {}", System.currentTimeMillis() - start);
        }

        createTablesAndFunctions();
    }

    @Override
    public Session openSession()
    {
        openFactory();
        Session session = factory.openSession();
        sessions.add(session);

        return session;
    }

    @Override
    public void withNewStatelessSession(Consumer<StatelessSession> operation, Consumer<Exception> exceptionHandler)
    {
        openFactory();

        Transaction tx = null;
        try (var statelessSession = factory.openStatelessSession())
        {
            tx = statelessSession.beginTransaction();
            operation.accept(statelessSession);
            tx.commit();
        } catch (Exception e)
        {
            logger.error("Rolling back transaction", e);
            if (tx != null) tx.rollback();
            exceptionHandler.accept(e);
        }
    }

    @Override
    public void closeSession(Session session)
    {
        if (session.isOpen()) session.close();
        sessions.remove(session);
    }

    @Override
    public void close()
    {
        closeAllSessions();
    }

    /**
     * Create the database schema tables and custom functions, if necessary.
     * @since 5.5 (renamed from createTables)
     */
    protected void createTablesAndFunctions()
    {
        // Test to see if one of our tables exists or not.  If not, we have Hibernate create the schema.

        try (Session session = factory.openSession())
        {
            session.doWork(connection -> {

                //noinspection SqlResolve
                try (Statement stmt = connection.createStatement(); ResultSet ignore = stmt.executeQuery("select 1 from Structure"))
                {
                    logger.trace("Tables exist");
                } catch (SQLException e)
                {
                    logger.debug("Creating tables");
                    doCreateTables();
                    doCreateFunctions(session);
                }
            });
        }
    }

    /**
     * Actually create the tables.
     * @since 5.5.1
     */
    protected void doCreateTables()
    {
        new SchemaExport().createOnly(EnumSet.of(TargetType.DATABASE), metadata);
    }

    /**
     * Actually create any custom functions.
     * @since 5.5.1
     */
    protected void doCreateFunctions(Session session)
    {
    }

    @Override
    public Dialect getDialect()
    {
        return serviceRegistry.getService(JdbcServices.class).getDialect();
    }

    /**
     * Close all sessions, connections and the session factory.
     */
    protected void closeAllSessions()
    {
        closeAllSessions(true);
    }

    @Override
    public void closeAllSessions(boolean closeFactory)
    {
        if (logger.isDebugEnabled()) logger.debug("Closing all sessions: open count = {}", sessions.size());

        new ArrayList<>(sessions).forEach(this::closeSession);

        if (closeFactory && factory != null && !factory.isClosed())
        {
            logger.debug("Closing factory");

            factory.close();
            factory = null;
        }
    }

    @Override
    public boolean supportsNativeArrays()
    {
        return false; // No support by default
    }

    @Override
    public boolean supportsBulkInsertRDMs()
    {
        return false; // No support by default
    }

    @Override
    public long bulkInsertRDMs(Iterable<IPrePersistRaptorDataMessage> rdms)
    {
        throw new UnsupportedOperationException("Bulk RDM insert is not available!");
    }

    @Override
    public long bulkInsertRDMsExtendedData(IStructure structure, Iterable<IPrePersistRaptorDataMessage> rdms)
    {
        throw new UnsupportedOperationException("Bulk RDM insert is not available!");
    }

    @Override
    public void performUpgradeStatements(final List<UpgradeStatement> stmts)
    {
        openFactory();

        try (StatelessSession session = factory.openStatelessSession())
        {
            final SchemaUpdater schemaUpdater = new SchemaUpdater(metadata.getDatabase());

            session.doWork(connection -> schemaUpdater.performStatements(connection, stmts));
        } catch (HibernateException e)
        {
            logger.error("Failed to perform DDL statements", e);
        }
    }
}
