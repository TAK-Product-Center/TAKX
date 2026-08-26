package gov.takx.dataservices.persist.upgraders;

import gov.takx.commons.utils.StringUtils;
import gov.takx.dataservices.persist.DatabaseVersionManager.DatabaseState;
import gov.takx.platform.utils.DatabaseUtils;
import jakarta.annotation.Nonnull;
import org.hibernate.dialect.Dialect;
import org.hibernate.dialect.HSQLDialect;
import org.hibernate.exception.SQLGrammarException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.lang.invoke.MethodHandles;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

/**
 * Abstract base for DB upgraders, offering numerous support methods for altering the DB structure.
 *
 * @since the dark ages, but heavily reworked in 3.0
 */
@SuppressWarnings("UnusedDeclaration")
abstract public class AUpgrader
{
    private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    // SQL types for use in column additions
    public static final String SQL_BOOL = "bool";
    public static final String SQL_INT = "int4";
    public static final String SQL_LONG = "int8";
    public static final String SQL_DOUBLE = "float8";
    public static final String SQL_STRING = "varchar(255)";
    public static final String SQL_TIMESTAMP = "timestamp";
    public static final String SQL_LOB_STRING = "text";
    public static final String SQL_BYTE_ARRAY = "bytea";

    protected static final String DEFAULT_0 = "not null default 0";

    protected Dialect dialect;
    protected DatabaseState databaseState;
    protected File workingDir;
    protected Connection connection;

    protected AUpgrader()
    {
    }

    /**
     * Perform the upgrade operations, saving the given parameters, and then calling {@link #doUpgrade()}.
     *
     * @param dialect       The hibernate dialect in use
     * @param databaseState Current database state
     * @param workingDir    Working directory of project being processed.
     * @param connection    Connection to use
     * @throws UpgraderException If any operation fails (typically it will wrap a SQL exception)
     */
    public void upgrade(Dialect dialect, DatabaseState databaseState, File workingDir, Connection connection) throws UpgraderException
    {
        this.dialect = dialect;
        this.databaseState = databaseState;
        this.workingDir = workingDir;
        this.connection = connection;

        doUpgrade();
    }

    /**
     * Subclasses implement this method to perform the actual upgrade.
     *
     * @throws UpgraderException If any operation fails (typically it will wrap a SQL exception)
     */
    public abstract void doUpgrade() throws UpgraderException;

    /**
     * Add a column to a table.
     *
     * @param tableName Name of the table where column is to be added
     * @param colName   Name of new column
     * @param colType   Type of the new column. If applicable, include not null constraint and default value here
     */
    protected void addColumn(String tableName, String colName, String colType)
    {
        addColumn(tableName, colName, colType, null);
    }

    /**
     * Add a column to a table with additional constraints.
     *
     * @param tableName   Name of the table where column is to be added
     * @param colName     Name of new column
     * @param colType     Type of the new column. If applicable, include not null constraint and default value here
     * @param constraints Any additional constraints to specify during column creation
     */
    protected void addColumn(String tableName, String colName, String colType, String constraints)
    {
        final String sql = String.format("alter table %1$s add column %2$s %3$s %4$s", tableName, colName,
                resolveColumnType(colType), constraints == null ? "" : constraints);
        executeUpdate(sql);
    }

    /**
     * Changes the max varchar length to the new max
     *
     * @param tableName Name of the table where column is to be added
     * @param colName   Name of new column
     * @param length    The new max supported varchars for the column
     */
    protected void changeVarCharColumnLength(String tableName, String colName, int length)
    {
        final String sql = String.format("alter table %1$s alter column %2$s TYPE varchar(%3$s)", tableName, colName, length);
        executeUpdate(sql);
    }

    /**
     * Changes a varchar column to a text column
     *
     * @param tableName Name of the table
     * @param colName   Name of the column
     * @since 5.2
     */
    protected void changeVarCharColumnToText(String tableName, String colName)
    {
        final String sql = String.format("alter table %1$s alter column %2$s TYPE text", tableName, colName);
        executeUpdate(sql);
    }

    /**
     * Remove a column from a table
     *
     * @param tableName Name of the table where column is to be dropped
     * @param colName   Name of dropped column
     */
    protected void dropColumn(String tableName, String colName)
    {
        final String sql = String.format("alter table %1$s drop column %2$s", tableName, colName);
        executeUpdate(sql);
    }

    /**
     * Add a primary key constraint
     *
     * @param tableName Name of the table where the constraint is to be added
     * @param colNames  comma separated list of columns
     */
    protected void addPrimaryKey(String tableName, String colNames)
    {
        final String sql = String.format("alter table %1$s add constraint %1$s%2$s primary key(%3$s)",
                tableName, "_pkey", colNames);
        executeUpdate(sql);
    }

    /**
     * Drop the primary key constraint on the given table.
     *
     * @param tableName Table on which to operate
     */
    protected void dropPrimaryKey(String tableName)
    {
        final String sql = String.format("alter table %1$s drop constraint %1$s_pkey", tableName);
        executeUpdate(sql);
    }

    /**
     * Add a unique constraint. The constraint name defaults to the first specified in {@code colNames}.
     *
     * @param tableName Name of the table where the constraint is to be added
     * @param colNames  array of column names
     */
    protected void addUniqueConstraint(String tableName, String[] colNames)
    {
        addUniqueConstraint(tableName, colNames, colNames[0]);
    }

    /**
     * Add a unique constraint with a specified name.
     *
     * @param tableName      Name of the table where the constraint is to be added
     * @param colNames       array of column names
     * @param constraintName Constraint name to append to table name (resulting in 'tableName_constraintName')
     * @since 2.28
     */
    protected void addUniqueConstraint(String tableName, String[] colNames, String constraintName)
    {
        if (colNames.length > 0)
        {
            final String sql = String.format("alter table %1$s add constraint %1$s_%2$s unique(%3$s)",
                    tableName, constraintName, StringUtils.join(colNames));

            executeUpdate(sql);
        } else
        {
            throw new IllegalArgumentException("colNames is empty, table=" + tableName);
        }
    }

    /**
     * Add an index to a table.
     *
     * @param tableName Name of the table where the constraint is to be added
     * @param indexName Name of index to create
     * @param colNames  array of column names
     * @since 2.14
     */
    protected void addIndex(String tableName, String indexName, String... colNames)
    {
        final String sql = String.format("create index %s on %s (%s)", indexName, tableName, StringUtils.join(colNames));
        executeUpdate(sql);
    }

    /**
     * Add a simple foreign key constraint where one column references the primary key of an entity table
     *
     * @param tableName Name of the table where the constraint is to be added
     * @param colName   foreign key column name
     * @param refTable  referenced table
     */
    protected void addForeignKey(String tableName, String colName, String refTable)
    {
        String fkName = String.format("%1$s_%2$s_%3$s_fkey", tableName.toLowerCase(), colName.toLowerCase(), refTable.toLowerCase());
        String[] fkCols = {colName};
        String[] pkCols = {"id"};

        final String sql = String.format("alter table %1$s %2$s",
                tableName,
                dialect.getAddForeignKeyConstraintString(fkName, fkCols, refTable, pkCols, true));

        executeUpdate(sql);
    }

    /**
     * Drop a named constraint
     *
     * @param tableName      Name of table where constraint is defined
     * @param constraintName Name of constraint to be dropped
     */
    protected void dropConstraint(String tableName, String constraintName)
    {
        final String sql = String.format("alter table %1$s drop constraint %2$s", tableName, constraintName);
        executeUpdate(sql);
    }

    /**
     * Drop table - references to the table need to be deleted before this method is used.
     *
     * @param tableName Table to be renamed
     */
    protected void dropTable(String tableName)
    {
        dropTable(tableName, false);
    }

    protected void dropTable(String tableName, boolean cascade)
    {
        executeUpdate(String.format("drop table %1$s%2$s", tableName, cascade ? " cascade" : ""));
    }

    /**
     * Rename table
     *
     * @param oldName Table to be renamed
     * @param newName The new name
     */
    protected void renameTable(String oldName, String newName)
    {
        final String sql = String.format("alter table %1$s rename to %2$s", oldName, newName);

        executeUpdate(sql);
    }

    /**
     * Rename column
     *
     * @param tableName Table on which to operate
     * @param oldName   Current column name
     * @param newName   New column name
     */
    protected void renameColumn(String tableName, String oldName, String newName)
    {
        final String sql = String.format("alter table %1$s rename column %2$s to %3$s", tableName, oldName, newName);

        executeUpdate(sql);
    }

    protected void executeUpdate(String sql)
    {
        try (Statement stmt = connection.createStatement())
        {
            logger.debug("Executing SQL: {}", sql);
            stmt.executeUpdate(sql);
        } catch (SQLException e)
        {
            throw new UpgraderException(e);
        }
    }

    /**
     * Execute an array of SQL statements
     *
     * @param sqlArray the array of SQL statements
     */
    protected void executeUpdates(String[] sqlArray)
    {
        for (String sql : sqlArray)
        {
            executeUpdate(sql);
        }
    }

    protected boolean constraintExists(String constraintName)
    {
        Long cnt = 1L;
        final String sql = "select count(*)" +
                " from information_schema.table_constraints" +
                " where constraint_name='" + constraintName + "'";

        try (Statement statement = connection.createStatement();
             ResultSet q = statement.executeQuery(sql))
        {
            q.next();
            cnt = q.getLong(1);
        } catch (SQLGrammarException e)
        {
            // This should be a "not found" exception
        } catch (SQLException e)
        {
            throw new UpgraderException(e);
        }
        return cnt.equals(1L);
    }

    /**
     * Determine if a table exists in the database.
     *
     * @param tableName Name of table to test for
     * @return {@code true} if the table exists
     */
    protected boolean tableExists(String tableName)
    {
        return DatabaseUtils.tableExists(connection, tableName);
    }

    /**
     * Determine if a column exists in a given table.
     *
     * @param tableName  Name of table to test
     * @param columnName Name of column to test for
     * @return {@code true} if the table exists
     */
    protected boolean columnExists(String tableName, String columnName)
    {
        final String where = "lower(table_name) = '" + tableName.toLowerCase() + "'"
                + " and lower(column_name) = '" + columnName.toLowerCase() + "'";

        return getCount("information_schema.columns", where) > 0;
    }

    /**
     * Get a count of the results for a given query against a table.  The created query will be of the form
     * 'select COUNT(*) from [tableName] where [where]'.
     *
     * @param tableName Name of table to query
     * @param where     Where clause to apply
     * @return The 'count' value
     * @since 2.7
     */
    private long getCount(String tableName, String where)
    {
        final String sql = "SELECT count(*) FROM " + tableName + " WHERE " + where;

        try (Statement statement = connection.createStatement();
             ResultSet q = statement.executeQuery(sql))
        {
            q.next();
            return q.getLong(1); // Get the 'COUNT(*)' value
        } catch (SQLException e)
        {
            throw new UpgraderException(e);
        }
    }

    /**
     * Perform a query to extract the 'id' of a row in a table that matches a given 'where' clause.  If the query
     * matches more than one row, the id of the first matching row is returned.
     *
     * @param tableName Name of table to query
     * @param where     Where clause to apply
     * @return Id of matching row
     * @since 2.7
     */
    protected int queryForId(String tableName, String where) throws SQLException
    {
        final String sql = "select id from " + tableName + " where " + where;
        logger.debug("SQL: {}", sql);

        try (Statement statement = connection.createStatement();
             ResultSet rs = statement.executeQuery(sql))
        {
            rs.next();
            return rs.getInt(1);
        }
    }

    /**
     * Perform a query to extract the ids of all rows in a table that match a given 'where' clause.
     *
     * @param tableName Name of table to query
     * @param where     Where clause to apply
     * @return List of ids of matching rows, may be empty
     * @since 2.7
     */
    protected List<Integer> queryForIds(String tableName, String where) throws SQLException
    {
        final List<Integer> ids = new ArrayList<>();

        final String sql = "select id from " + tableName + " where " + where;
        logger.debug("SQL: {}", sql);

        try (Statement statement = connection.createStatement();
             ResultSet rs = statement.executeQuery(sql))
        {
            while (rs.next())
            {
                ids.add(rs.getInt(1));
            }
        }

        return ids;
    }

    /**
     * @return the appropriate SQL data type to use for a given {@code columnType}.
     * @since 5.7
     */
    private String resolveColumnType(@Nonnull String columnType)
    {
        // The 'text' type is not natively supported by HSQL, it is specific to Postgres.  So, convert it.
        return "text".equalsIgnoreCase(columnType) && dialect instanceof HSQLDialect ? "longvarchar" : columnType;
    }
}
