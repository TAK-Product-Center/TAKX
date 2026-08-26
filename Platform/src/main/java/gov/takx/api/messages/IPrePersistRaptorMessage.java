package gov.takx.api.messages;

import gov.takx.api.annotation.Internal;
import gov.takx.api.mapobject.IMapObject;

import java.util.Date;
import java.util.UUID;

/**
 * @since 1.3
 */
@SuppressWarnings("UseOfObsoleteDateTimeApi")
public interface IPrePersistRaptorMessage extends IRaptorMessage
{
    @Internal
    void setUUID(UUID uuid);

    /**
     * Set a String valued field.
     *
     * @param fieldName Name of field to set
     * @param value     Field value
     */
    void setString(String fieldName, String value);

    /**
     * Set an array of String values.
     *
     * @param fieldName Name of field to set
     * @param value     array of values
     */
    void setStringArray(String fieldName, String[] value);

    /**
     * Set a boolean valued field.
     *
     * @param fieldName Name of field to set
     * @param value     Field value
     */
    void setBoolean(String fieldName, boolean value);

    /**
     * Set an array of boolean values.
     *
     * @param fieldName Name of field to set
     * @param value     array of values
     */
    void setBooleanArray(String fieldName, boolean[] value);

    /**
     * Set an int valued field.
     *
     * @param fieldName Name of field to set
     * @param value     Field value
     */
    void setInt(String fieldName, int value);

    /**
     * Set an array of int values.
     *
     * @param fieldName Name of field to set
     * @param value     array of values
     */
    void setIntArray(String fieldName, int[] value);

    /**
     * Set a float valued field.
     *
     * @param fieldName Name of field to set
     * @param value     Field value
     */
    void setFloat(String fieldName, float value);

    /**
     * Set an array of float values.
     *
     * @param fieldName Name of field to set
     * @param value     array of values
     */
    void setFloatArray(String fieldName, float[] value);

    /**
     * Set a short valued field.
     *
     * @param fieldName Name of field to set
     * @param value     Field value
     */
    void setShort(String fieldName, short value);

    /**
     * Set an array of short values.
     *
     * @param fieldName Name of field to set
     * @param value     array of values
     */
    void setShortArray(String fieldName, short[] value);

    /**
     * Set a long valued field.
     *
     * @param fieldName Name of field to set
     * @param value     Field value
     */
    void setLong(String fieldName, long value);

    /**
     * Set an array of long values.
     *
     * @param fieldName Name of field to set
     * @param value     array of values
     */
    void setLongArray(String fieldName, long[] value);

    /**
     * Set a double valued field.
     *
     * @param fieldName Name of field to set
     * @param value     Field value
     */
    void setDouble(String fieldName, double value);

    /**
     * Set an array of double values.
     *
     * @param fieldName Name of field to set
     * @param value     array of values
     */
    void setDoubleArray(String fieldName, double[] value);

    /**
     * Set a Date valued field.
     *
     * @param fieldName Name of field to set
     * @param value     Field value
     */
    void setDate(String fieldName, Date value);

    /**
     * Set an array of date values.
     *
     * @param fieldName Name of field to set
     * @param value     array of values
     */
    void setDateArray(String fieldName, Date[] value);

    /**
     * Set a byte array valued field.
     *
     * @param fieldName Name of field to set
     * @param value     Field value
     */
    void setByteArray(String fieldName, byte[] value);

    /**
     * Set a field's value.
     *
     * @param fieldName Name of field to set
     * @param value     Field value, may be {@code null}
     */
    void setField(String fieldName, Object value) throws StructureAccessException;

    /**
     * Set the given field to {@code null}, return {@code true} if the field had a previous value.
     *
     * @param fieldName Name of field to set
     * @return {@code true} if the field had a prior value, {@code false} otherwise
     */
    boolean removeField(String fieldName);

    @Internal
    void setMapObject(IMapObject mapObject);

    @Internal
    void setStructure(IRaptorDataStructure rds);

    /**
     * Set the unit ID.  This can only be done if a map object has NOT been previously set either in a constructor
     * or via a call to {@link #setMapObject(IMapObject)}.
     *
     * @since 5.1
     * @throws IllegalStateException If a Map Object has been previously set
     * @implNote Default implementation always throws an IllegalStateException
     */
    default void setUnitID(String unitID) throws IllegalStateException
    {
        throw new IllegalStateException("no implementation");
    }

    /**
     * Set the family.  This can only be done if a map object has NOT been previously set either in a constructor
     * or via a call to {@link #setMapObject(IMapObject)}.
     *
     * @since 5.1
     * @throws IllegalStateException If a Map Object has been previously set
     * @implNote Default implementation always throws an IllegalStateException
     */
    default void setFamily(String family) throws IllegalStateException
    {
        throw new IllegalStateException("no implementation");
    }

    /**
     * Set the type.  This can only be done if a map object has NOT been previously set either in a constructor
     * or via a call to {@link #setMapObject(IMapObject)}.
     *
     * @since 5.1
     * @throws IllegalStateException If a Map Object has been previously set
     * @implNote Default implementation always throws an IllegalStateException
     */
    default void setType(String type) throws IllegalStateException
    {
        throw new IllegalStateException("no implementation");
    }
}
