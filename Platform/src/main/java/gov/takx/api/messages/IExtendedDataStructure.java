package gov.takx.api.messages;

import java.sql.PreparedStatement;
import java.sql.ResultSet;

/**
 * Contract for all dynamically generated classes holding structure-defined (extended) data for a structured
 * message.
 *
 */
public interface IExtendedDataStructure
{
    /**
     * Determine if the value of the field defined by the given dataField is {@code null}.
     *
     * @param dataField Data field defining the field of interest
     * @return {@code true} if the field doesn't exist in the structure or has a {@code null} value
     * @since 2.14
     */
    boolean isNull(IDataField dataField);

    /**
     * Get the value of the field defined by the given dataField.
     * <p>
     * For performance reasons, when working with primitive field types, it is better to use one of the
     * type-specific getter methods, such as {@link #getInt(IDataField)}, instead of using this method
     * since this method requires the "boxing" of any primitive return value.  If differentiating between
     * an actual zero value (vs. a 0 returned because the value is {@code null}) is required, use the
     * {@link #isNull(IDataField)} method.
     *
     * @param dataField Data field defining the field of interest
     * @return The value of the field, may be {@code null}
     * @see #getBoolean(IDataField)
     * @see #getShort(IDataField)
     * @see #getInt(IDataField)
     * @see #getLong(IDataField)
     * @see #getFloat(IDataField)
     * @see #getDouble(IDataField)
     * @since 2.14
     */
    Object getField(IDataField dataField);

    /**
     * Set the value of the field defined by the given dataField.
     * <p>
     * For performance reasons, when working with primitive field types, it is better to use one of the
     * type-specific setter methods, such as {@link #setInt(IDataField, int)}, instead of using this method
     * since this method requires the "boxing" of any primitive value.
     *
     * @param dataField Data field defining the field of interest
     * @param value     New value, may be {@code null}
     * @see #setBoolean(IDataField, boolean)
     * @see #setShort(IDataField, short)
     * @see #setInt(IDataField, int)
     * @see #setLong(IDataField, long)
     * @see #setFloat(IDataField, float)
     * @see #setDouble(IDataField, double)
     * @since 2.14
     */
    void setField(IDataField dataField, Object value);

    /**
     * Set a boolean value for the given field.  If the field is not of the expected type a StructureAccessException
     * is thrown.
     *
     * @param dataField Data field defining the field of interest
     * @param value     New value
     * @throws StructureAccessException if the given field is not a boolean field
     * @since 2.22
     */
    void setBoolean(IDataField dataField, boolean value);

    /**
     * Set a short value for the given field.  If the field is not of the expected type a StructureAccessException
     * is thrown.
     *
     * @param dataField Data field defining the field of interest
     * @param value     New value
     * @throws StructureAccessException if the given field is not a short field
     * @since 2.22
     */
    void setShort(IDataField dataField, short value);

    /**
     * Set an int value for the given field.  If the field is not of the expected type a StructureAccessException
     * is thrown.
     *
     * @param dataField Data field defining the field of interest
     * @param value     New value
     * @throws StructureAccessException if the given field is not an int field
     * @since 2.22
     */
    void setInt(IDataField dataField, int value);

    /**
     * Set a long value for the given field.  If the field is not of the expected type a StructureAccessException
     * is thrown.
     *
     * @param dataField Data field defining the field of interest
     * @param value     New value
     * @throws StructureAccessException if the given field is not a long field
     * @since 2.22
     */
    void setLong(IDataField dataField, long value);

    /**
     * Set a float value for the given field.  If the field is not of the expected type a StructureAccessException
     * is thrown.
     *
     * @param dataField Data field defining the field of interest
     * @param value     New value
     * @throws StructureAccessException if the given field is not a float field
     * @since 2.22
     */
    void setFloat(IDataField dataField, float value);

    /**
     * Set a double value for the given field.  If the field is not of the expected type a StructureAccessException
     * is thrown.
     *
     * @param dataField Data field defining the field of interest
     * @param value     New value
     * @throws StructureAccessException if the given field is not a double field
     * @since 2.22
     */
    void setDouble(IDataField dataField, double value);

    /**
     * Get a boolean field value.
     *
     * @param dataField Field to retrieve
     * @return field value, or false if the field is actually {@code null}
     * @since 2.22
     */
    boolean getBoolean(IDataField dataField);

    /**
     * Get a short field value.
     *
     * @param dataField Field to retrieve
     * @return field value, or 0 if the field is actually {@code null}
     * @since 2.22
     */
    short getShort(IDataField dataField);

    /**
     * Get an int field value.
     *
     * @param dataField Field to retrieve
     * @return field value, or 0 if the field is actually {@code null}
     * @since 2.22
     */
    int getInt(IDataField dataField);

    /**
     * Get a long field value.
     *
     * @param dataField Field to retrieve
     * @return field value, or 0 if the field is actually {@code null}
     * @since 2.22
     */
    long getLong(IDataField dataField);

    /**
     * Get a float field value.
     *
     * @param dataField Field to retrieve
     * @return field value, or 0 if the field is actually {@code null}
     * @since 2.22
     */
    float getFloat(IDataField dataField);

    /**
     * Get a double field value.
     *
     * @param dataField Field to retrieve
     * @return field value, or 0 if the field is actually {@code null}
     * @since 2.22
     */
    double getDouble(IDataField dataField);

    /**
     * Bind the field values into the given prepared statement.
     *
     * @param statement Prepared statement to bind into
     * @param rdmId     The Id of the RDM to which this extended data object belongs
     * @since 2.14
     */
    void bindFieldValues(PreparedStatement statement, int rdmId);

    /**
     * Load the values in the result set into this extended data object.
     *
     * @param results The result set to process
     * @since 2.14
     */
    void loadFieldValues(ResultSet results);

    /**
     * Build the String data form of this extended data object for use in a Postgres "COPY FROM" statement.
     *
     * @param sb    StringBuilder to which to append the data
     * @param rdmId The Id of the RDM to which this extended data object belongs
     * @since 2.14
     */
    void buildCopyString(StringBuilder sb, int rdmId);
}
