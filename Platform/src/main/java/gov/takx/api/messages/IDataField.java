package gov.takx.api.messages;

import gov.takx.api.annotation.NoPluginImplementations;
import gov.takx.api.plugin.IMetadata;

/**
 * Data field definition.
 *
 */
@NoPluginImplementations
public interface IDataField extends IDataType, Comparable<IDataField>, IMetadata
{
    int getOrdinalPosition();

    String getFieldName();

    int getFieldType();

    Object getDefaultValue();

    Integer getId();

    IStructure getStructure();

    Integer getFieldLength();

    /**
     * @return The neutralized field name ("F_" prepended and all spaces removed)
     * @since 2.0.0
     */
    String getNeutralizedFieldName();

    /**
     * Determine if this field's data type represents a primitive type (boolean, short, int, long, float, or double).
     *
     * @return {@code true} if this field's data type represents a primitive data type
     * @since 2.14
     */
    default boolean isPrimitive()
    {
        return IDataType.isPrimitiveType(getFieldType());
    }

    /**
     * @return {@code true} if this field's data type represents a scalar data type (String, Date, or primitive)
     * @since 3.0
     */
    default boolean isScalar()
    {
        return IDataType.isScalarType(getFieldType());
    }

    /**
     * @return {@code true} if this field's data type is an array data type.
     * @since 3.0
     */
    default boolean isArrayType()
    {
        return IDataType.isArrayType(getFieldType());
    }

    /**
     * @return {@code true} if this field's data type is sortable (i.e., implements the {@link Comparable} interface).
     * @since 3.0
     */
    default boolean isSortable()
    {
        return IDataType.isScalarType(getFieldType());
    }

    /**
     * @return The Java type (Class) that this field's type represents
     * @since 2.22
     */
    default Class<?> getJavaType()
    {
        return IDataType.getJavaType(getFieldType());
    }

    /**
     * @return Return the name of this field's type
     * @since 2.22
     */
    default String getDataTypeName()
    {
        return IDataType.getDataTypeName(getFieldType());
    }

    /**
     * @return The SQL type used to represent this data field in the database
     * @since 2.24
     */
    int getSQLType();

    /**
     * @return the display processor on this data field
     * @since 2.29
     */
    IFieldDisplayProcessor getFieldDisplayProcessor();
}
