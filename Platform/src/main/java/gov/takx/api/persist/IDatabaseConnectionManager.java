package gov.takx.api.persist;

import gov.takx.api.annotation.Internal;
import gov.takx.api.annotation.NoPluginImplementations;
import gov.takx.api.messages.IPrePersistRaptorDataMessage;
import gov.takx.api.messages.IStructure;
import gov.takx.dataservices.upgrade.UpgradeStatement;
import org.hibernate.Session;
import org.hibernate.StatelessSession;
import org.hibernate.dialect.Dialect;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;
import java.util.function.Consumer;

/**
 * Contract for database connection managers.  A connection manager is responsible for creating the session factory
 * for use in accessing the database, and for creating JDBC connections.
 *
 * @since 3.0 - major rework, but has existed since 1.4
 */
@NoPluginImplementations
@Internal
public interface IDatabaseConnectionManager
{
    /**
     * Open a hibernate session.
     *
     * @return Hibernate session
     */
    Session openSession();

    /**
     * Perform a given {@code operation} using a newly created {@link StatelessSession}.
     *
     * @param operation Operation to perform
     * @param exceptionHandler Handler to invoke in the case an exception is thrown by {@code operation}
     * @since 5.5.1
     */
    void withNewStatelessSession(Consumer<StatelessSession> operation, Consumer<Exception> exceptionHandler);

    /**
     * Close the session and clean up any resources that we can.
     *
     * @param session Hibernate session
     */
    void closeSession(Session session);

    /**
     * Close all open sessions and, optionally, the session factory.
     *
     * @param closeFactory Pass {@code true} to close the session factory as well
     */
    void closeAllSessions(boolean closeFactory);

    /**
     * @return The dialect in use for this database
     */
    Dialect getDialect();

    /**
     * Obtain a connection to the database.
     *
     * @return JDBC Connection
     */
    Connection getConnection() throws SQLException;

    /**
     * Close any open resources.
     */
    void close();

    /**
     * Apply a list of upgrade (DDL) statements to the schema.
     *
     * @param stmts The list of statements to execute.
     */
    void performUpgradeStatements(List<UpgradeStatement> stmts);

    /**
     * A way of "gutting" the database without having to completely destroy it and then recreate it.
     */
    void dropTables();

    /**
     * @return {@code true} if the current database supports insertion of arrays of native types
     * @since 5.4
     */
    boolean supportsNativeArrays();

    /**
     * @return {@code true} if this current database supports performing a high-speed, bulk insert of RDMs,
     * {@code false} otherwise.
     */
    boolean supportsBulkInsertRDMs();

    /**
     * Perform a bulk insert of RDMs core data (as appropriate for the given database).
     *
     * @param rdms The collection of RDMs to insert
     * @return number of messages inserted, -1 on error
     */
    long bulkInsertRDMs(Iterable<IPrePersistRaptorDataMessage> rdms);

    /**
     * Perform a bulk insert of RDMs extended data (as appropriate for the given database).
     *
     * @param structure The structure defining the data to be persisted
     * @param rdms      The collection of RDMs to insert
     * @return number of messages inserted, -1 on error
     */
    long bulkInsertRDMsExtendedData(IStructure structure, Iterable<IPrePersistRaptorDataMessage> rdms);
}
