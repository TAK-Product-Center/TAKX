package gov.takx.api.messages;

import gov.takx.api.annotation.Internal;
import gov.takx.api.mapobject.IMapObject;
import gov.takx.api.persist.IPersistable;

import java.util.Collection;
import java.util.Date;
import java.util.Set;
import java.util.UUID;

/**
 * @since 1.3
 */
@SuppressWarnings("UnusedDeclaration") // called from plugins
public interface IRaptorMessage extends IPersistable
{
    /**
     * Constant indicating that a DB Id has not been assigned.
     *
     * @see #getIdAsInt()
     * @since 2.14
     */
    int NO_ID = -1;

    /**
     * @return The assigned DB id as an int (avoiding allocating an Integer), {@link #NO_ID} if no Id
     * @since 2.14
     */
    int getIdAsInt();

    /**
     * Set the id.
     *
     * @param id Id to assign
     * @since 3.0 (moved out of IPersistable interface)
     */
    @Internal
    void setId(int id);

    /**
     * Reset the id to {@link #NO_ID}.
     */
    @Internal
    default void resetId()
    {
        setId(NO_ID);
    }

    /**
     * @return {@code true} if this message has been assigned a DB Id
     * @since 2.14
     */
    default boolean hasId()
    {
        return getIdAsInt() != NO_ID;
    }

    UUID getUUID();

    /**
     * @return The MSB of the UUID
     * @since 2.24
     */
    long getUuidMsb();

    /**
     * @return The LSB of the UUID
     * @since 2.24
     */
    long getUuidLsb();

    String getUnitID();

    String getFamily();

    String getType();

    String getVersion();

    String getMessageType();

    String getUniqueKey();

    boolean hasField(String fieldName);

    Set<String> getFieldNames();

    Collection<IDataField> getFields();

    /**
     * Determine if a field has a non-null value.  Essentially, the inverse of {@link #isNull(String)}.
     */
    boolean isNotNull(String fieldName);

    /**
     * Determine if a given field has a {@code null} value.  If the given field name doesn't exist on the structure
     * defining this message, then {@code true} is returned.
     *
     * @param fieldName Name of field of interest
     * @return {@code true} if the field has a {@code null} value or doesn't exist on our structure
     */
    boolean isNull(String fieldName);

    /**
     * @param fieldName Name of the field to retrieve
     * @return The value. If the RDM value is {@code null}, return {@code null}.
     */
    String getString(String fieldName);

    /**
     * @param fieldName Name of the field to retrieve
     * @return The value. If the RDM value is {@code null}, return {@code null}.
     */
    String[] getStringArray(String fieldName);

    /**
     * @param fieldName Name of the field to retrieve
     * @return The value. If the RDM value is {@code null}, return "false".
     */
    boolean getBoolean(String fieldName);

    /**
     * @param fieldName Name of the field to retrieve
     * @return The value. If the RDM value is {@code null}, return {@code null}.
     */
    boolean[] getBooleanArray(String fieldName);

    /**
     * @param fieldName Name of the field to retrieve
     * @return The value. If the RDM value is {@code null}, return "0".
     */
    int getInt(String fieldName);

    /**
     * @param fieldName Name of the field to retrieve
     * @return The value. If the RDM value is {@code null}, return {@code null}.
     */
    int[] getIntArray(String fieldName);

    /**
     * @param fieldName Name of the field to retrieve
     * @return The value. If the RDM value is {@code null}, return "0".
     */
    float getFloat(String fieldName);

    /**
     * @param fieldName Name of the field to retrieve
     * @return The value. If the RDM value is {@code null}, return {@code null}.
     */
    float[] getFloatArray(String fieldName);

    /**
     * @param fieldName Name of the field to retrieve
     * @return The value. If the RDM value is {@code null}, return "0".
     */
    short getShort(String fieldName);

    /**
     * @param fieldName Name of the field to retrieve
     * @return The value. If the RDM value is {@code null}, return {@code null}.
     */
    short[] getShortArray(String fieldName);

    /**
     * @param fieldName Name of the field to retrieve
     * @return The value. If the RDM value is {@code null}, return "0".
     */
    long getLong(String fieldName);

    /**
     * @param fieldName Name of the field to retrieve
     * @return The value. If the RDM value is {@code null}, return {@code null}.
     */
    long[] getLongArray(String fieldName);

    /**
     * @param fieldName Name of the field to retrieve
     * @return The value. If the RDM value is {@code null}, return "0".
     */
    double getDouble(String fieldName);

    /**
     * @param fieldName Name of the field to retrieve
     * @return The value. If the RDM value is {@code null}, return {@code null}.
     */
    double[] getDoubleArray(String fieldName);

    /**
     * @param fieldName Name of the field to retrieve
     * @return The value. If the RDM value is {@code null}, return {@code null}.
     */
    Date getDate(String fieldName);

    /**
     * @param fieldName Name of the field to retrieve
     * @return The value. If the RDM value is {@code null}, return {@code null}.
     */
    Date[] getDateArray(String fieldName);

    /**
     * @param fieldName Name of the field to retrieve
     * @return The value. If the RDM value is {@code null}, return {@code null}.
     */
    byte[] getByteArray(String fieldName);

    /**
     * Retrieve a given field's value.
     *
     * @param fieldName Name of field to retrieve
     * @return field value, may be {@code null} if fieldName is not found on the structure defining this message or
     * no value was ever set on this field
     */
    Object getField(String fieldName);

    /**
     * @return The Map Object associated with this message
     * @since 2.36
     */
    IMapObject getMapObject();

    IStructure getStructure();

    @Internal
    void setDataObject(IExtendedDataStructure dataObject);

    @Internal
    IExtendedDataStructure getDataObject();
}
