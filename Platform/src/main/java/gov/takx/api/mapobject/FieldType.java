package gov.takx.api.mapobject;

import gov.takx.api.messages.IDataType;

/**
 */
public enum FieldType
{
    BOOLEAN(IDataType.BOOLEAN),
    INTEGER(IDataType.INT),
    FLOAT(IDataType.FLOAT),
    DOUBLE(IDataType.DOUBLE),
    STRING(IDataType.STRING),
    DATE(IDataType.DATE),
    SHORT(IDataType.SHORT),
    LONG(IDataType.LONG);

    private final int dataType;

    /**
     * @param dataType The matching structure data type
     * @see IDataType
     * @since 2.33
     */
    FieldType(int dataType)
    {
        this.dataType = dataType;
    }

    /**
     * @return Get's the matching structure data type
     * @see IDataType
     * @since 2.33
     */
    public int getDataType()
    {
        return dataType;
    }

    /**
     * @param dataType The data type to convert
     * @return The matching field type or {@code null} if none is found
     * @since 2.33
     */
    public static FieldType getFieldType(int dataType)
    {
        switch (dataType)
        {
            case IDataType.STRING:
                return STRING;
            case IDataType.BOOLEAN:
                return BOOLEAN;
            case IDataType.INT:
                return INTEGER;
            case IDataType.FLOAT:
                return FLOAT;
            case IDataType.SHORT:
                return SHORT;
            case IDataType.LONG:
                return LONG;
            case IDataType.DOUBLE:
                return DOUBLE;
            case IDataType.DATE:
                return DATE;

            default:
                return null;
        }
    }
}
