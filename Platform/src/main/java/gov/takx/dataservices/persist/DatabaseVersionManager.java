package gov.takx.dataservices.persist;

import gov.takx.api.annotation.Internal;
import gov.takx.platform.utils.DatabaseUtils;
import jakarta.enterprise.context.ApplicationScoped;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

/**
 * Manages the database version in persistence.  The version is maintained in a database table named
 * {@code DatabaseVersion}.
 *
 * @since 3.0
 */
@Internal
@ApplicationScoped
public class DatabaseVersionManager
{
    private static final String TABLE_NAME = "DatabaseVersion";

    public static final int CURRENT_VERSION = DatabaseUpgradeManager.CURRENT_VERSION;
    public static final String CURRENT_VERSION_STRING = String.valueOf(CURRENT_VERSION);

    private static final String CREATE_TABLE_SQL = String.format("create table %s (id int not null, version int not null)", TABLE_NAME);
    private static final String GET_VERSION_SQL = String.format("select version from %s where id=1", TABLE_NAME);
    private static final String INSERT_CURRENT_SQL = String.format("insert into %s(id, version) values (1,%s)", TABLE_NAME, CURRENT_VERSION_STRING);
    private static final String UPDATE_CURRENT_SQL = String.format("update %s set version = %s where id=1", TABLE_NAME, CURRENT_VERSION_STRING);

    /**
     * Insert or update the version entry with the current DB version.  The version table is created if needed.
     *
     * @param connection Connection to use
     */
    public void insertCurrentVersion(Connection connection) throws SQLException
    {
        final boolean created = createTable(connection); // Ensure the version table exists

        try (final Statement statement = connection.createStatement())
        {
            // If the table exists, then update the version row, otherwise insert the row
            final String sql = created ? INSERT_CURRENT_SQL : UPDATE_CURRENT_SQL;

            statement.executeUpdate(sql);
            connection.commit();
        }
    }

    /**
     * Get the database state from persistence.
     *
     * @param connection Connection to use
     * @return state read from the database
     */
    public DatabaseState getState(Connection connection) throws SQLException
    {
        // Read the version from the DatabaseVersion.
        if (DatabaseUtils.tableExists(connection, TABLE_NAME))
        {
            try (
                 Statement statement = connection.createStatement();
                 ResultSet resultSet = statement.executeQuery(GET_VERSION_SQL))
            {
                resultSet.next();

                return new DatabaseState(resultSet.getInt(1));
            }
        } else
        {
            throw new IllegalStateException("Can't get the database version; table '" + TABLE_NAME + "' is missing!");
        }
    }

    /**
     * Create our version table, if it doesn't already exist.
     *
     * @param connection Connection to use
     * @return {@code true} if the table was created, {@code false} if it already existed
     */
    private boolean createTable(Connection connection) throws SQLException
    {
        if (!DatabaseUtils.tableExists(connection, TABLE_NAME))
        {
            try (final Statement statement = connection.createStatement())
            {
                statement.executeUpdate(CREATE_TABLE_SQL);
            }
            return true;
        }
        return false;
    }

    /**
     * Holds the state of a project's database, offering access to version numbers amd checks to determine
     * if the DB needs to be upgraded.
     */
    public record DatabaseState(int version)
    {
        /**
         * @return {@code true} if the database needs to be upgraded in order to work with this version of TAKX
         */
        public boolean needsUpgrade()
        {
            return version < CURRENT_VERSION;
        }

        /**
         * @return {@code true} if the database is at a version from which we can perform an upgrade
         * @since 5.0
         */
        public boolean canUpgrade()
        {
            return version >= DatabaseUpgradeManager.UPGRADE_VERSION_BASE;
        }
    }
}
