package gov.takx.dataservices.upgrade;

import gov.takx.commons.utils.StringUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 */
public class CreateTableStatement extends UpgradeStatement
{
    List<ColumnDefinition> columnDefinitions = new ArrayList<>();
    CreatePrimaryKeyStatement primaryKeyStatement = null;

    public CreateTableStatement(String tableName)
    {
        super(tableName);
    }

    public CreateTableStatement addColumnDefinition(ColumnDefinition columnDefinition)
    {
        columnDefinitions.add(columnDefinition);
        return this;
    }

    /**
     * The list of columns that need to be added to the new table.
     *
     * @return Collection of column definitions
     */
    public List<ColumnDefinition> getColumnList()
    {
        return Collections.unmodifiableList(columnDefinitions);
    }

    /**
     * A custom primary key statement.
     *
     * @return Statement, or {@code null} if not specified
     * @since 2.29
     */
    public CreatePrimaryKeyStatement getPrimaryKeyStatement()
    {
        return primaryKeyStatement;
    }

    /**
     * Specify the custom primary key definition for this table.
     *
     * @param primaryKeyStatement Primary key statement
     * @since 2.29
     */
    public CreateTableStatement withPrimaryKey(CreatePrimaryKeyStatement primaryKeyStatement)
    {
        this.primaryKeyStatement = primaryKeyStatement;
        return this;
    }

    @Override
    public String toString()
    {
        return "[CreateTableStatement: " +
                "name=" + getTableName() +
                ", columns=[\n   " + StringUtils.join(columnDefinitions, "\n   ", null) + "]" +
                (primaryKeyStatement != null ? "\n   " + primaryKeyStatement : "") +
                "\n]";
    }
}
