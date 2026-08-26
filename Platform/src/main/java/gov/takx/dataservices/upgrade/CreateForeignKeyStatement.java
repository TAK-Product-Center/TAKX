package gov.takx.dataservices.upgrade;

/**
 * Describes foreign key constraint to be created on a table.
 *
 */
public class CreateForeignKeyStatement extends UpgradeStatement
{
    private final String constraintName;
    private final String foreignTableName;
    private final String column;
    private final String foreignColumn;

    /**
     * Make a CreateForeignKeyStatement on the given foreign table.
     *
     * @param constraintName   Name of FK constraint to create
     * @param tableName        Table on which to create the constraint
     * @param column           Local table column name (to verify against the foreign table)
     * @param foreignTableName Foreign table name (target of constraint)
     */
    public CreateForeignKeyStatement(String constraintName, String tableName, String column, String foreignTableName, String foreignColumn)
    {
        super(tableName);
        this.column = column;
        this.constraintName = constraintName;
        this.foreignTableName = foreignTableName;
        this.foreignColumn = foreignColumn;
    }

    /**
     * @return the name of the constraint
     */
    public String getConstraintName()
    {
        return constraintName;
    }

    /**
     * @return Local table column name
     */
    public String getColumn()
    {
        return column;
    }

    /**
     * @return the name of the parent table
     */
    public String getForeignTableName()
    {
        return foreignTableName;
    }

    /**
     * @return the foreign key column
     */
    public String getForeignColumn()
    {
        return foreignColumn;
    }

    @Override
    public String toString()
    {
        return "CreateForeignKeyStatement{" +
                "name=" + getConstraintName() +
                ", column=" + column +
                ", foreignTable='" + foreignTableName + '\'' +
                ", foreignColumn=" + foreignColumn +
                '}';
    }
}
