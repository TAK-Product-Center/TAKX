package gov.takx.dataservices.upgrade;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 */
public class CreatePrimaryKeyStatement extends UpgradeStatement
{
    private final List<String> columns = new ArrayList<>();

    public CreatePrimaryKeyStatement(String... columns)
    {
        super(null);
        this.columns.addAll(Arrays.asList(columns));
    }

    /**
     * @return the column names participating in the constraint
     */
    public List<String> getColumnNames()
    {
        return columns;
    }

    @Override
    public String toString()
    {
        return "[CreatePrimaryKeyStatement: columns=" + columns + "]";
    }
}
