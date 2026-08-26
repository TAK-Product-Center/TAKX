package gov.takx.dataservices.persist;

import gov.takx.api.annotation.Internal;
import gov.takx.api.mapobject.IMapObject;
import gov.takx.api.messages.IDataField;
import gov.takx.api.messages.IExtendedDataStructure;
import gov.takx.api.messages.IPrePersistRaptorMessage;
import gov.takx.api.messages.IRaptorDataStructure;
import gov.takx.api.messages.IStructure;
import gov.takx.api.messages.StructureAccessException;
import gov.takx.api.persist.IPersistService;
import gov.takx.api.persist.IPersistServiceProvider;
import gov.takx.commons.utils.UUIDUtils;
import gov.takx.platform.KeyUtils;
import jakarta.annotation.Nonnull;
import org.jboss.weld.environment.se.WeldContainer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.annotation.Annotation;
import java.lang.invoke.MethodHandles;
import java.sql.SQLException;
import java.util.Collection;
import java.util.Date;
import java.util.Set;
import java.util.TreeSet;
import java.util.UUID;

/**
 */
@Internal
public abstract class ARaptorMessage implements IPrePersistRaptorMessage
{
    private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    // This is semi-hackish, but making this static saves an injection operation and a 4 byte reference in every RDM.
    protected static WeldContainer container;

    protected int id = NO_ID;

    private IMapObject mapObject = null;

    protected IStructure structure;

    private long uuid_msb;
    private long uuid_lsb;

    protected int flags; // Flags to represent various states and avoid 1 byte-per flag

    private String uniqueKey = null;    // Constructed as needed

    // Instead of directly holding 3 String references use a single object to hold all 3 values.
    //
    // This optimizes memory usage since the vast majority of RDMs have a MO associated with them and there
    // is no reason to hold 3 references (12 bytes) instead of 1 (4 bytes).  Further, since the case where
    // no MO is present is rare, the additional overhead for the FTU object header is irrelevant.
    @SuppressWarnings("FieldAccessedSynchronizedAndUnsynchronized")
    private FTU familyTypeUnit = null; // Only used until MO is set

    protected IExtendedDataStructure dataObject;

    /**
     * Set the container in which we are running so that RDMs can gain access to other beans.
     *
     * @param container Weld container
     * @since 2.14
     */
    public static void setContainer(WeldContainer container)
    {
        ARaptorMessage.container = container;
    }

    private ARaptorMessage()
    {
        setUUID(UUIDUtils.generateType1UUID());
    }

    protected ARaptorMessage(IStructure structure, IMapObject mapObject)
    {
        this();

        this.structure = structure;
        this.mapObject = mapObject;
    }

    protected ARaptorMessage(IStructure structure, String family, String type, String unitId)
    {
        this();

        this.structure = structure;
        familyTypeUnit = new FTU(family, type, unitId);
    }

    /**
     * Set/clear a flag bit.
     *
     * @param bitPosition The bit position to manipulate
     * @param newState    Pass {@code true} to set the bit, {@code false} to clear it
     * @since 2.14
     */
    protected void setFlag(int bitPosition, boolean newState)
    {
        if (newState)
        {
            flags |= bitPosition;
        } else
        {
            flags &= ~bitPosition;
        }
    }

    /**
     * Test a flag bit.
     *
     * @param bitPosition Bit to test
     * @return {@code true} if the bit is set
     * @since 2.14
     */
    protected boolean getFlag(int bitPosition)
    {
        return (flags & bitPosition) != 0;
    }

    @Override
    public UUID getUUID()
    {
        return uuid_msb == 0 && uuid_lsb == 0
                ? null
                : new UUID(uuid_msb, uuid_lsb);
    }

    @Override
    public long getUuidMsb()
    {
        return uuid_msb;
    }

    @Override
    public long getUuidLsb()
    {
        return uuid_lsb;
    }

    @Override
    public void setUUID(UUID uuid)
    {
        uuid_msb = uuid.getMostSignificantBits();
        uuid_lsb = uuid.getLeastSignificantBits();
    }

    @Override
    public String getFamily()
    {
        return mapObject != null ? mapObject.getFamily() : (familyTypeUnit != null ? familyTypeUnit.family : null);
    }

    @Override
    public String getUnitID()
    {
        return mapObject != null ? mapObject.getUnitID() : (familyTypeUnit != null ? familyTypeUnit.unitId : null);
    }

    @Override
    public String getType()
    {
        return mapObject != null ? mapObject.getType() : (familyTypeUnit != null ? familyTypeUnit.type : null);
    }

    @Override
    public void setFamily(String family)
    {
        if (mapObject != null) throw new IllegalStateException("Cannot use 'setFamily' on a message after the MO");

        getFTU().family = family;
        uniqueKey = null;    // Force recreation of map object key
    }

    @Override
    public void setType(String type)
    {
        if (mapObject != null) throw new IllegalStateException("Cannot use 'setType' on a message after the MO");

        getFTU().type = type;
        uniqueKey = null;    // Force recreation of map object key
    }

    @Override
    public void setUnitID(String unitID)
    {
        if (mapObject != null) throw new IllegalStateException("Cannot use 'setUnitID' on a message after the MO");

        getFTU().unitId = unitID;
        uniqueKey = null;    // Force recreation of map object key
    }

    /**
     * @return Return the FTU, create on demand
     * @since 5.1
     */
    private synchronized FTU getFTU()
    {
        return familyTypeUnit != null ? familyTypeUnit : (familyTypeUnit = new FTU());
    }

    @Override
    public String getMessageType()
    {
        return structure != null ? structure.getMessageType() : null;
    }

    @Override
    public String getUniqueKey()
    {
        // It's OK to leave this unsynchronized.  The worst case scenario is that we redundantly create the value.
        if (uniqueKey == null)
        {
            uniqueKey = KeyUtils.getUniqueKey(this).intern(); // Intern this since all RDMs created by the same device share this key
        }
        return uniqueKey;
    }

    /**
     * Does the data message contain a field?
     *
     * @param fieldName name of field to search for.
     * @return true if the message contains a field called <tt>fieldName</tt>
     */
    @Override
    public boolean hasField(String fieldName)
    {
        // Return true only if the field has a non-null value (in addition to being defined in the structure)
        return !isNull(fieldName);
    }

    @Override
    public boolean isNotNull(String fieldName)
    {
        return !isNull(fieldName);
    }

    @Override
    public boolean isNull(String fieldName)
    {
        final IDataField dataField = structure.getField(fieldName);
        if (dataField == null) return true; // Missing field => null

        return getDataObject().isNull(dataField);
    }

    @Override
    public Object getField(String fieldName)
    {
        // Special/different semantics for handling a field that isn't defined on the structure.
        // I have no idea of the history on this, but getField (vs. getInt, getBoolean, etc.) supports being called
        // with a field name that is not defined on the structure, and in that case, it returns a null value.

        final IDataField dataField = structure.getField(fieldName);
        if (dataField == null) return null;

        return getDataObject().getField(dataField);
    }

    @Override
    public void setField(String fieldName, Object value) throws StructureAccessException
    {
        getDataObject().setField(getDataField(fieldName), value);
    }

    @Override
    public boolean removeField(String fieldName)
    {
        if (!hasField(fieldName)) return false; // No such field

        setField(fieldName, null);
        return true;
    }

    /**
     * Get the names of all the fields in the message.
     *
     * @return set of field names in this message
     */
    @Override
    public Set<String> getFieldNames()
    {
        return new TreeSet<>(structure.getKeys());
    }

    /**
     * Gets a list of data fields in this message.
     *
     * @return a list of data fields
     */
    @Override
    public Collection<IDataField> getFields()
    {
        return structure.getFields();
    }

    @Override
    public Integer getId()
    {
        return id < 0 ? null : id;
    }

    @Override
    public void setId(int id)
    {
        this.id = id;
    }

    @Override
    public int getIdAsInt()
    {
        return id;
    }

    @Override
    public void setString(String fieldName, String value)
    {
        setField(fieldName, value);
    }

    @Override
    public String getString(String fieldName)
    {
        return getTypedField(fieldName);
    }

    @Override
    public void setStringArray(String fieldName, String[] value)
    {
        setField(fieldName, value);
    }

    @Override
    public String[] getStringArray(String fieldName)
    {
        return getTypedField(fieldName);
    }

    @Override
    public void setBoolean(String fieldName, boolean value)
    {
        getDataObject().setBoolean(getDataField(fieldName), value);
    }

    @Override
    public boolean getBoolean(String fieldName)
    {
        return getDataObject().getBoolean(getDataField(fieldName));
    }

    @Override
    public void setBooleanArray(String fieldName, boolean[] value)
    {
        setField(fieldName, value);
    }

    @Override
    public boolean[] getBooleanArray(String fieldName)
    {
        return getTypedField(fieldName);
    }

    @Override
    public void setInt(String fieldName, int value)
    {
        getDataObject().setInt(getDataField(fieldName), value);
    }

    @Override
    public int getInt(String fieldName)
    {
        return getDataObject().getInt(getDataField(fieldName));
    }

    @Override
    public void setFloat(String fieldName, float value)
    {
        getDataObject().setFloat(getDataField(fieldName), value);
    }

    @Override
    public float getFloat(String fieldName)
    {
        return getDataObject().getFloat(getDataField(fieldName));
    }

    @Override
    public void setShort(String fieldName, short value)
    {
        getDataObject().setShort(getDataField(fieldName), value);
    }

    @Override
    public short getShort(String fieldName)
    {
        return getDataObject().getShort(getDataField(fieldName));
    }

    @Override
    public void setShortArray(String fieldName, short[] value)
    {
        setField(fieldName, value);
    }

    @Override
    public short[] getShortArray(String fieldName)
    {
        return getTypedField(fieldName);
    }

    @Override
    public void setLong(String fieldName, long value)
    {
        getDataObject().setLong(getDataField(fieldName), value);
    }

    @Override
    public long getLong(String fieldName)
    {
        return getDataObject().getLong(getDataField(fieldName));
    }

    @Override
    public void setLongArray(String fieldName, long[] value)
    {
        setField(fieldName, value);
    }

    @Override
    public long[] getLongArray(String fieldName)
    {
        return getTypedField(fieldName);
    }

    @Override
    public void setDouble(String fieldName, double value)
    {
        getDataObject().setDouble(getDataField(fieldName), value);
    }

    @Override
    public double getDouble(String fieldName)
    {
        return getDataObject().getDouble(getDataField(fieldName));
    }

    @Override
    public void setDate(String fieldName, Date value)
    {
        setField(fieldName, value);
    }

    @Override
    public Date getDate(String fieldName)
    {
        return getTypedField(fieldName);
    }

    @Override
    public void setDateArray(String fieldName, Date[] value)
    {
        setField(fieldName, value);
    }

    @Override
    public Date[] getDateArray(String fieldName)
    {
        return getTypedField(fieldName);
    }

    @Override
    public void setByteArray(String fieldName, byte[] value)
    {
        setField(fieldName, value);
    }

    @Override
    public byte[] getByteArray(String fieldName)
    {
        return getTypedField(fieldName);
    }

    @Override
    public void setIntArray(String fieldName, int[] value)
    {
        setField(fieldName, value);
    }

    @Override
    public int[] getIntArray(String fieldName)
    {
        return getTypedField(fieldName);
    }

    @Override
    public void setFloatArray(String fieldName, float[] value)
    {
        setField(fieldName, value);
    }

    @Override
    public float[] getFloatArray(String fieldName)
    {
        return getTypedField(fieldName);
    }

    @Override
    public void setDoubleArray(String fieldName, double[] value)
    {
        setField(fieldName, value);
    }

    @Override
    public double[] getDoubleArray(String fieldName)
    {
        return getTypedField(fieldName);
    }

    @Override
    public void setDataObject(IExtendedDataStructure dataObject)
    {
        this.dataObject = dataObject;
    }

    /**
     * Generic method to return a properly cast field value.
     *
     * @param fieldName Name of field to retrieve
     * @param <T>       Return type (used to cast field value)
     * @return The field value, properly cast
     * @since 2.14
     */
    private <T> T getTypedField(String fieldName)
    {
        try
        {
            //noinspection unchecked
            return (T) getField(fieldName);
        } catch (ClassCastException e)
        {
            logger.error("Class Cast Exception for fieldName = {}", fieldName, e);
            throw e;
        }
    }

    /**
     * Get the data field with the given name.  The named field must exist on our defining structure or an
     * exception will be thrown.
     *
     * @param fieldName Name of field to retrieve
     * @return {@code IDataField} for the given name, or an exception is thrown if not found
     * @throws StructureAccessException If the field is not present on this message's structure
     * @since 2.19
     */
    private IDataField getDataField(@Nonnull String fieldName)
    {
        final IDataField field = structure.getField(fieldName);
        if (field == null)
        {
            final String message = "Unknown field: " + fieldName + " on " + structure;
            logger.error(message);
            throw new StructureAccessException(message);
        }
        return field;
    }

    @Override
    public IExtendedDataStructure getDataObject()
    {
        if (dataObject == null)
        {
            createDataObject();
        }

        return dataObject;
    }

    private void createDataObject()
    {
        if (hasId())
        {
            // If we do have an Id, then make the data object and load it with data
            try
            {
                setDataObject(getPersistService().loadExtendedData(this));
            } catch (SQLException e)
            {
                logger.warn("Unable to load extended data for {}: {}: ", getClass().getSimpleName(), getId(), e);
            }
        } else
        {
            setDataObject(getStructure().makeExtendedDataObject()); // Otherwise, just create an empty data object
        }
    }

    @Override
    public void setMapObject(IMapObject mapObject)
    {
        this.mapObject = mapObject;
        familyTypeUnit = null;  // Now that we have a MO, we can ditch the FTU data
    }

    @Override
    public IMapObject getMapObject()
    {
        return mapObject;
    }

    /**
     * @return hashcode as determined by that of our RDMs ID, this is consistent with our equals implementation
     */
    @Override
    public int hashCode()
    {
        return id;
    }

    @Override
    public boolean equals(Object obj)
    {
        return id > 0
                && obj != null
                && getClass() == obj.getClass()
                && id == ((ARaptorMessage) obj).id;
    }

    /**
     * @return The "toString" form of the core data fields in the message
     */
    protected abstract String coreFieldsToString();

    @Override
    public String toString()
    {
        StringBuilder s = new StringBuilder();

        s.append("{").append(getClass().getName()).append(": ")
                .append("id=").append(id)
                .append(", uuid=").append(getUUID())
                .append(", family=").append(getFamily())
                .append(", type=").append(getType())
                .append(", version=").append(getVersion())
                .append(", mapObjectId=").append(getUnitID())
                .append(", messageType=").append(getMessageType())
                .append(", ").append(coreFieldsToString())
                .append(", fields = [");

        // Only include fields actually set on the message
        getFieldNames().stream()
                .filter(this::hasField)
                .forEach(fieldName -> s.append(fieldName).append("=").append(getField(fieldName)).append(", "));

        s.append("]}");

        return s.toString();
    }

    @Override
    public IStructure getStructure()
    {
        return structure;
    }

    @Override
    public void setStructure(IRaptorDataStructure rds)
    {
        structure = rds.getStructure();
    }

    @Override
    public String getVersion()
    {
        return structure != null ? structure.getVersion() : null;
    }

    /**
     * @return The persist service instance to use
     */
    protected IPersistService getPersistService()
    {
        return getBean(IPersistServiceProvider.class).getPersistService();
    }

    /**
     * Get a bean from the container.
     *
     * @param beanClass  Class of bean to obtain
     * @param qualifiers the additional required qualifiers, if any
     * @return bean instance
     */
    protected <T> T getBean(Class<T> beanClass, Annotation... qualifiers)
    {
        return container.select(beanClass, qualifiers).get();
    }

    /**
     * Simple holder object for the family, type, and Unit ID prior to the MO being set.
     *
     * @since 2.14
     */
    private static final class FTU
    {
        String family;
        String type;
        String unitId;

        FTU()
        {
        }

        FTU(String family, String type, String unitId)
        {
            this.family = family;
            this.type = type;
            this.unitId = unitId;
        }
    }
}
