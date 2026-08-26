package gov.takx.dataservices.upgrade;

/**
 */
public abstract class UpgradeStatement
{
    private final String tableName;

    protected UpgradeStatement(String tableName)
    {
        this.tableName = tableName;
    }

    /**
     * Get the table name for the statement
     *
     * @return Table name
     */
    public String getTableName()
    {
        return tableName;
    }
}
