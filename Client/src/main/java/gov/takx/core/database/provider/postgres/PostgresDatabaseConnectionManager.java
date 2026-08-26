package gov.takx.core.database.provider.postgres;

import gov.takx.api.messages.IPrePersistRaptorDataMessage;
import gov.takx.api.messages.IStructure;
import gov.takx.core.database.exception.DatabaseIOException;
import gov.takx.core.database.provider.ADatabaseConnectionManager;
import org.hibernate.Session;
import org.postgresql.jdbc.PgConnection;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.Map;

/**
 * Connection manager for Postgres, supporting "bulk insert" operations on RDMs to improve performance (using specific
 * Postgres APIs and not standard JDBC).
 *
 * @since 3.0 (was previously in the Postgres plugin)
 */
public class PostgresDatabaseConnectionManager extends ADatabaseConnectionManager
{
    private final BulkCopyHandler bulkCopyHandler = new BulkCopyHandler();
    private final PostgreSQLInstance postgresql;

    /**
     * Construct a connection manager with the given settings.
     *
     * @param settings     Properties of the connection
     * @param postgresql   Postgresql instance
     */
    public PostgresDatabaseConnectionManager(Map<String, Object> settings, PostgreSQLInstance postgresql)
    {
        super(settings);

        this.postgresql = postgresql;
    }

    @Override
    public void dropTables()
    {
        closeAllSessions();

        try (final Connection connection = getConnection())
        {
            postgresql.dropTables(connection);
        } catch (SQLException e)
        {
            throw new DatabaseIOException(e);
        }
    }

    @Override
    public boolean supportsNativeArrays()
    {
        return true;
    }

    @Override
    public boolean supportsBulkInsertRDMs()
    {
        return true;
    }

    @Override
    public long bulkInsertRDMs(Iterable<IPrePersistRaptorDataMessage> rdms)
    {
        if (Thread.interrupted()) return -1;

        final Session session = openSession();
        final Long count = session.doReturningWork(connection -> bulkCopyHandler.doBulkInsertCoreData(connection.unwrap(PgConnection.class), rdms));
        closeSession(session);
        return count;
    }

    @Override
    public long bulkInsertRDMsExtendedData(IStructure structure, Iterable<IPrePersistRaptorDataMessage> rdms)
    {
        if (Thread.interrupted()) return -1;

        final Session session = openSession();
        final long count = session.doReturningWork(connection -> bulkCopyHandler.doBulkInsertExtendedData(connection.unwrap(PgConnection.class), structure, rdms));
        closeSession(session);
        return count;
    }
}
