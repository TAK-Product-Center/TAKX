package gov.takx.api.messages;

import gov.takx.dataservices.persist.DataField;

/**
 * DataField Builder.  This builder constructs an instance of {@link IDataField} that is not associated with any
 * structure.  This can be useful for unifying some forms of processing around a field name/type tuple.
 * <p>
 * This builder is no longer used to create fields that are part of an {@link IStructure}, for that you must use
 * {@link DataStructureBuilder}.
 *
 * @since 1.3
 */
public class DataFieldBuilder
{
    private final String fieldName;
    private final int fieldType;
    private final int ordinalPosition;

    public DataFieldBuilder(String fieldName, int fieldType)
    {
        this(fieldName, fieldType, 999);
    }

    public DataFieldBuilder(String fieldName, int fieldType, int ordinalPosition)
    {
        this.fieldName = fieldName;
        this.fieldType = fieldType;
        this.ordinalPosition = ordinalPosition;
    }

    /**
     * Builds an IDataField based on the definitions of this DataFieldBuilder instance.
     *
     * @return IDataField instance
     */
    public IDataField build()
    {
        return new DataField(fieldName, fieldType, null, ordinalPosition);
    }

    public String getName()
    {
        return fieldName;
    }

    public int getType()
    {
        return fieldType;
    }

    public int getOrdinalPosition()
    {
        return ordinalPosition;
    }
}
