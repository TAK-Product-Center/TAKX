package gov.takx.core.database.provider;

import gov.takx.api.persist.IDatabaseConnectionManager;
import gov.takx.commons.utils.StringUtils;
import gov.takx.commons.utils.UUIDUtils;
import gov.takx.dataservices.upgrade.ColumnDefinition;
import gov.takx.dataservices.upgrade.CreateForeignKeyStatement;
import gov.takx.dataservices.upgrade.CreateIndexStatement;
import gov.takx.dataservices.upgrade.CreatePrimaryKeyStatement;
import gov.takx.dataservices.upgrade.CreateTableStatement;
import gov.takx.dataservices.upgrade.UpgradeStatement;
import gov.takx.platform.utils.DatabaseUtils;
import org.hibernate.boot.model.relational.Database;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.invoke.MethodHandles;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.List;

/**
 * This class performs schema maintenance (DDL operations), like table creation.
 *
 * @since 3.0
 */
class SchemaUpdater
{
    private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    private final Database database;

    /**
     * @param database The database in use (for access to schema/type information)
     */
    SchemaUpdater(Database database)
    {
        this.database = database;
    }

    /**
     * Perform a set of DDL statements on the database according to the dialect.
     *
     * @param conn              Connection to use
     * @param upgradeStatements DDL statements to execute
     * @see IDatabaseConnectionManager#performUpgradeStatements(List)
     */
    void performStatements(Connection conn, final List<UpgradeStatement> upgradeStatements) throws SQLException
    {
        try (Statement sqlStmt = conn.createStatement())
        {
            for (UpgradeStatement upgrade : upgradeStatements)
            {
                final String sql;

                if (upgrade instanceof CreateTableStatement)
                {
                    sql = createTableStatement((CreateTableStatement) upgrade);
                } else if (upgrade instanceof CreateForeignKeyStatement)
                {
                    sql = createForeignKeyStatement((CreateForeignKeyStatement) upgrade);
                } else if (upgrade instanceof CreateIndexStatement)
                {
                    sql = createIndexStatement((CreateIndexStatement) upgrade);
                } else
                {
                    throw new SQLException("Unknown statement type: " + upgrade.getClass().getSimpleName());
                }

                if (logger.isDebugEnabled()) logger.debug(sql);
                sqlStmt.executeUpdate(sql);
            }

            conn.commit(); // Get all this nice work committed!
        } catch (SQLException e)
        {
            conn.rollback();
            throw e;
        }
    }

    /**
     * Generate the SQL statement to execute from the given create table statement.
     *
     * @param createTableStatement Declaration of table contents
     * @return SQL to execute
     */
    private String createTableStatement(CreateTableStatement createTableStatement)
    {
        final StringBuilder sql = new StringBuilder();

        sql.append("create table ").append(createTableStatement.getTableName()).append(" (");

        boolean first = true;

        for (ColumnDefinition column : createTableStatement.getColumnList())
        {
            if (!first) sql.append(",");
            first = false;

            sql.append(column.getName()).append(" ");

            sql.append(DatabaseUtils.getColumnType(database, column.getType(), column.getSize()));

            if (column.isPrimary()) sql.append(" PRIMARY KEY");
            if (column.isNotNull()) sql.append(" NOT NULL");
        }

        // Add any primary key specification (in case no columns were the primary key)
        final CreatePrimaryKeyStatement primaryKeyStatement = createTableStatement.getPrimaryKeyStatement();
        if (primaryKeyStatement != null)
        {
            sql.append(", primary key (")
                    .append(StringUtils.join(primaryKeyStatement.getColumnNames()))
                    .append(")");
        }

        sql.append(")");

        return sql.toString();
    }

    private String createForeignKeyStatement(CreateForeignKeyStatement foreignKeyStatement)
    {
        final StringBuilder sql = new StringBuilder();
        sql.append("alter table ").append(foreignKeyStatement.getTableName()).append(" add ");

        final String constraintName = foreignKeyStatement.getConstraintName();
        if (constraintName != null) sql.append(" constraint ").append(constraintName);

        sql.append(" foreign key (").append(foreignKeyStatement.getColumn()).append(") ");
        sql.append("references ").append(foreignKeyStatement.getForeignTableName());
        sql.append(" (").append(foreignKeyStatement.getForeignColumn()).append(")");

        return sql.toString();
    }

    private String createIndexStatement(CreateIndexStatement indexStatement)
    {
        return "create " +
                (indexStatement.isUnique() ? "unique " : "") +
                "index " +
                getIndexName(indexStatement) +
                " on " +
                indexStatement.getTableName() +
                " (" +
                String.join(",", indexStatement.getColumnNames()) +
                ")";
    }

    private String getIndexName(CreateIndexStatement indexStatement)
    {
        final String indexName = indexStatement.getName();

        if (indexName == null)
        {
            // Generate a name
            final StringBuilder name = new StringBuilder(indexStatement.getTableName());
            for (String c : indexStatement.getColumnNames())
            {
                name.append("_").append(c);
            }
            name.append("_idx");

            return "idx_" + UUIDUtils.generateType3UUID(name.toString()).toString().replace("-", "_");
        } else
        {
            return indexName;
        }
    }
}
