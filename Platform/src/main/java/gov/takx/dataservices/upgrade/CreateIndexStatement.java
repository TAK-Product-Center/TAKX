package gov.takx.dataservices.upgrade;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * Describes an index to be created on a table.
 *
 * @since 1.14
 */
public class CreateIndexStatement extends UpgradeStatement
{
    private final String indexName;
    private final boolean unique;
    private final List<String> columnList = new ArrayList<>();

    /**
     * @param tableName Table on which to create the index
     * @param indexName Name of index to create
     * @param unique    {@code true} to create a unique index (constraint)
     * @param columns   The columns to include in the index
     */
    public CreateIndexStatement(String tableName, String indexName, boolean unique, String... columns)
    {
        super(tableName);

        this.indexName = indexName;
        this.unique = unique;
        columnList.addAll(Arrays.asList(columns));
    }

    /**
     * @return The name of the index (this might be {@code null}).
     */
    public String getName()
    {
        return indexName;
    }

    /**
     * @return The names of the columns in the index. This is an ordered list.
     */
    public List<String> getColumnNames()
    {
        return Collections.unmodifiableList(columnList);
    }

    /**
     * @return True, if the index is expected to be unique.
     */
    public boolean isUnique()
    {
        return unique;
    }

    /**
     * Add a column to this index.
     *
     * @param colName Additional column to include in the index
     */
    public void addColumn(String colName)
    {
        columnList.add(colName);
    }

    @Override
    public String toString()
    {
        return "CreateIndexStatement{" +
                "indexName='" + indexName + '\'' +
                ", unique=" + unique +
                ", columnList=" + columnList +
                '}';
    }
}
