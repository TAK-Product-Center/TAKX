package gov.takx.dataservices.upgrade;

import gov.takx.api.messages.IDataType;
import gov.takx.platform.utils.DatabaseUtils;

/**
 */
public class ColumnDefinition
{
    private final String name;
    private final int type;
    private final Integer size;
    private final boolean notNull;

    private boolean isPrimary = false;

    /**
     * Construct with the given values.
     *
     * @param name Name of column
     * @param type Data type
     * @since 2.29
     */
    public ColumnDefinition(String name, int type)
    {
        this(name, type, null, false);
    }

    /**
     * Construct with the given values.
     *
     * @param name Name of column
     * @param type Data type
     * @since 2.29
     */
    public ColumnDefinition(String name, int type, boolean notNull)
    {
        this(name, type, null, notNull);
    }

    /**
     * Construct with the given values.
     *
     * @param name Name of column
     * @param type Data type
     * @param size Allowed length
     * @since 2.29
     */
    public ColumnDefinition(String name, int type, Integer size)
    {
        this(name, type, size, false);
    }

    /**
     * Construct with the given values.
     *
     * @param name Name of column
     * @param type Data type
     * @param size Allowed length
     * @since 2.29
     */
    public ColumnDefinition(String name, int type, Integer size, boolean notNull)
    {
        this.name = name;
        this.type = type;
        this.size = size;
        this.notNull = notNull;
    }

    /**
     * The name of the column.
     *
     * @return Column name
     */
    public String getName()
    {
        return name;
    }

    /**
     * The data type of the column. This will be an {@link IDataType} type so the DB plugin
     * will need to translate to SQL data types as needed.<br>
     * <br>
     * See {@link DatabaseUtils#getColumnType} for help.<br>
     *
     * @return Data type
     */
    public int getType()
    {
        return type;
    }

    /**
     * @return true, if the value must not be {@code null}
     */
    public boolean isNotNull()
    {
        return notNull;
    }

    /**
     * Some data types will support a size parameter. Such as VARCHAR. If the datatype does not support a
     * size or if the default is desired this value will be {@code null}.
     *
     * @return Size
     */
    public Integer getSize()
    {
        return size;
    }

    /**
     * Mark this column as the primary key with no auto-generation method (keys are self managed).
     */
    public ColumnDefinition setPrimary()
    {
        isPrimary = true;
        return this;
    }

    /**
     * Is the column "the" primary key. At the moment we have no accommodation for a composite primary key.
     *
     * @return true, if primary key
     */
    public boolean isPrimary()
    {
        return isPrimary;
    }

    @Override
    public String toString()
    {
        return "[ColumnDefinition: " +
                "name=" + name +
                ", type=" + type + " " + IDataType.getDataTypeName(type) +
                ", size=" + size +
                ", notNull=" + notNull +
                ", primary=" + isPrimary +
                "]";
    }
}
