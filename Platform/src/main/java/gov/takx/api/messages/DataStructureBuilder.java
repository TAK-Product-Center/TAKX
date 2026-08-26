package gov.takx.api.messages;

import com.google.common.collect.ImmutableMap;
import gov.takx.api.annotation.PseudoBean;
import gov.takx.api.query.CoreField;
import gov.takx.api.query.NoSuchFieldException;
import gov.takx.api.registry.IDevicePluginRegistry;
import gov.takx.dataservices.persist.DataField;
import gov.takx.dataservices.persist.IndexDefinition;
import gov.takx.dataservices.persist.RaptorDataStructure;
import gov.takx.dataservices.persist.Structure;
import gov.takx.dataservices.utils.HashUtils;
import gov.takx.dataservices.utils.StructurePersistenceSupport;
import gov.takx.platform.schema.structure.BaseFieldType;
import gov.takx.platform.schema.structure.BooleanArrayFieldType;
import gov.takx.platform.schema.structure.BooleanFieldType;
import gov.takx.platform.schema.structure.ByteArrayFieldType;
import gov.takx.platform.schema.structure.DateArrayFieldType;
import gov.takx.platform.schema.structure.DateTimeFieldType;
import gov.takx.platform.schema.structure.DoubleArrayFieldType;
import gov.takx.platform.schema.structure.DoubleFieldType;
import gov.takx.platform.schema.structure.FieldCollection;
import gov.takx.platform.schema.structure.FloatArrayFieldType;
import gov.takx.platform.schema.structure.FloatFieldType;
import gov.takx.platform.schema.structure.IndexCollection;
import gov.takx.platform.schema.structure.IntArrayFieldType;
import gov.takx.platform.schema.structure.IntegerFieldType;
import gov.takx.platform.schema.structure.LongArrayFieldType;
import gov.takx.platform.schema.structure.LongFieldType;
import gov.takx.platform.schema.structure.MetadataType;
import gov.takx.platform.schema.structure.ShortArrayFieldType;
import gov.takx.platform.schema.structure.ShortFieldType;
import gov.takx.platform.schema.structure.StringArrayFieldType;
import gov.takx.platform.schema.structure.StringFieldType;
import gov.takx.platform.schema.structure.StructureType;
import gov.takx.platform.schema.structure.StructuresCollection;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import jakarta.inject.Inject;
import jakarta.xml.bind.JAXBContext;
import jakarta.xml.bind.JAXBElement;
import jakarta.xml.bind.JAXBException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.xml.datatype.XMLGregorianCalendar;
import javax.xml.transform.stream.StreamSource;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectOutputStream;
import java.lang.invoke.MethodHandles;
import java.lang.reflect.InvocationTargetException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

/**
 * Builder for {@link IRaptorDataStructure} (RDS) instances. Each RDS associates a map object family and type with an
 * underlying {@link IStructure}. That structure is an abstraction of a strongly typed table.
 * Each structure represents a collection of 0, 1, or many plugin developer custom fields.
 * <p>
 * The following field data types are presently supported:
 * <ul>
 * <li>Boolean</li>
 * <li>Byte[]</li>
 * <li>Date</li>
 * <li>Date[]</li>
 * <li>Double</li>
 * <li>Double[]</li>
 * <li>Float</li>
 * <li>Float[]</li>
 * <li>Integer</li>
 * <li>Integer[]</li>
 * <li>Long</li>
 * <li>Long[]</li>
 * <li>Short</li>
 * <li>Short[]</li>
 * <li>String</li>
 * <li>String[]</li>
 * </ul>
 * The {@code add*DataType*()} methods provide the definition of a field name (mandatory), default value
 * (optional, {@code null} by default if not specified), and ordinal position (optional, 999 by default if not specified).
 * <p>
 * Developers should strongly consider providing an ordinal position when they define a custom field.  If no position
 * is provided, then the value of 0 is used.  The underlying DataField instances support the ability to sort by
 * ordinal position, then by case-insensitive field name.  If no ordinal position is provided when the custom field
 * is defined, then sorting of the fields within the data structure will be determined using case-insensitive field
 * name comparisons.  Sorting is valuable as it defines the column sequence in the Device Log Dialog that is used
 * to review a device instance's RaptorDataMessage history.
 * <p>
 * <b>Note:</b> Ordinal position is loosely enforced, meaning that the system will not throw an exception or behave
 * irrationally if 2 custom fields are placed into the same ordinal slot.  In these situations, sorting automatically
 * defers to a case-insensitive name comparison between the fields.
 * <p>
 * <b>Note:</b> Every {@link IRaptorDataMessage} (RDM) automatically provides getters/setters for location (including
 * latitude, longitude, elevation), heading, speed, pitch, and roll. Therefore, plugin developers do not need to define
 * these fields inside of a {@link RaptorDataStructure}. Every RDM may or may not populate these fields based on the
 * message being constructed.
 *
 * @since 1.3
 */
@SuppressWarnings("CdiManagedBeanInconsistencyInspection")
@PseudoBean
public class DataStructureBuilder
{
    private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    /**
     * TAKX requires each map object to define a data structure with message type LOCATION_MESSAGE_TYPE.
     * If the plugin does not provide this structure, TAKX will create it automatically. When the user manually
     * places on object on the globe, the location data will be packed into a message with this structure.
     *
     * @see #DataStructureBuilder(String, String, String, String)
     */
    public static final String LOCATION_MESSAGE_TYPE = "_ManualLocation";

    /**
     * {@link DataStructureBuilder#LOCATION_MESSAGE_TYPE} structure version
     */
    public static final String LOCATION_MESSAGE_VERSION = "1.0.1";

    /**
     * {@link DataStructureBuilder#LOCATION_MESSAGE_TYPE} structure metadata details
     */
    public static final String LOCATION_MESSAGE_DESCRIPTION = "Manual Location Message";

    public static final String LOCATION_MESSAGE_DETAILS = """
            Manual locations usually occur from explicit user interaction.
            Clicking on a map object and dragging it to another part of the
            globe will generate this type of record. It is rare for a manual
            location to occur without user interaction.""";

    // Default ordinal position if one is not specified in the addXxx call
    private static final int DEFAULT_ORDINAL_POSITION = 999;

    private static JAXBContext jaxbContext = null;

    // Map from the JAXB class type to a function to extract the default value.
    // This is needed because the getDefaultValue method is defined in each unique type, not in the BaseFieldType class.

    private static final Map<Class<? extends BaseFieldType>, Function<BaseFieldType, Object>> DEFAULT_VALUE_EXTRACTOR =
            ImmutableMap.<Class<? extends BaseFieldType>, Function<BaseFieldType, Object>>builder()
                    .put(StringFieldType.class, field -> ((StringFieldType) field).getDefaultValue())
                    .put(BooleanFieldType.class, field -> ((BooleanFieldType) field).isDefaultValue())
                    .put(IntegerFieldType.class, field -> ((IntegerFieldType) field).getDefaultValue())
                    .put(FloatFieldType.class, field -> ((FloatFieldType) field).getDefaultValue())
                    .put(ShortFieldType.class, field -> ((ShortFieldType) field).getDefaultValue())
                    .put(LongFieldType.class, field -> ((LongFieldType) field).getDefaultValue())
                    .put(DoubleFieldType.class, field -> ((DoubleFieldType) field).getDefaultValue())
                    .put(DateTimeFieldType.class, field -> {
                        final XMLGregorianCalendar defaultValue = ((DateTimeFieldType) field).getDefaultValue();
                        return defaultValue == null ? null : defaultValue.toGregorianCalendar().getTime();
                    })
                    .put(ByteArrayFieldType.class, field -> ((ByteArrayFieldType) field).getDefaultValue())
                    .put(BooleanArrayFieldType.class, field -> null)
                    .put(StringArrayFieldType.class, field -> null)
                    .put(ShortArrayFieldType.class, field -> null)
                    .put(IntArrayFieldType.class, field -> null)
                    .put(LongArrayFieldType.class, field -> null)
                    .put(FloatArrayFieldType.class, field -> null)
                    .put(DoubleArrayFieldType.class, field -> null)
                    .put(DateArrayFieldType.class, field -> null)
                    .build();

    // Map from the JAXB class type to the data field type to use to define the field.
    private static final Map<Class<? extends BaseFieldType>, Integer> XML_TYPE_TO_FIELD_TYPE =
            ImmutableMap.<Class<? extends BaseFieldType>, Integer>builder()
                    .put(StringFieldType.class, IDataType.STRING)
                    .put(BooleanFieldType.class, IDataType.BOOLEAN)
                    .put(IntegerFieldType.class, IDataType.INT)
                    .put(FloatFieldType.class, IDataType.FLOAT)
                    .put(ShortFieldType.class, IDataType.SHORT)
                    .put(LongFieldType.class, IDataType.LONG)
                    .put(DoubleFieldType.class, IDataType.DOUBLE)
                    .put(DateTimeFieldType.class, IDataType.DATE)
                    .put(ByteArrayFieldType.class, IDataType.BYTE_ARRAY)
                    .put(BooleanArrayFieldType.class, IDataType.BOOLEAN_ARRAY)
                    .put(StringArrayFieldType.class, IDataType.STRING_ARRAY)
                    .put(ShortArrayFieldType.class, IDataType.SHORT_ARRAY)
                    .put(IntArrayFieldType.class, IDataType.INT_ARRAY)
                    .put(LongArrayFieldType.class, IDataType.LONG_ARRAY)
                    .put(FloatArrayFieldType.class, IDataType.FLOAT_ARRAY)
                    .put(DoubleArrayFieldType.class, IDataType.DOUBLE_ARRAY)
                    .put(DateArrayFieldType.class, IDataType.DATE_ARRAY)
                    .build();

    static
    {
        try
        {
            // JAXB contexts are thread safe, so we only need to create this once
            jaxbContext = JAXBContext.newInstance(StructuresCollection.class.getPackage().getName(),
                    DataStructureBuilder.class.getClassLoader()); // Setting CL because TCCL is sometimes null, resulting in NoClassDefFound
        } catch (JAXBException e)
        {
            logger.error("Failed to allocate JAXBContext for XML processing", e);
        }
    }

    @Inject
    private IDevicePluginRegistry devicePluginRegistry;

    @Inject
    protected StructurePersistenceSupport structurePersistenceSupport;

    private final String mapObjectFamily;
    private final String mapObjectType;
    private final String messageType;
    private final String version;
    private boolean renderable = true;

    private final Map<String, DataField> dataMap = new HashMap<>();
    private final Map<String, String> neutralizedFieldMap = new HashMap<>();
    private final Map<String, String> structureMetadataMap = new HashMap<>();
    private final List<IndexDefinition> indexes = new ArrayList<>();

    private boolean built = false;

    /**
     * Factory method to construct a "manual location" message structure with standard description and details.
     *
     * @param family Family to use in RDS
     * @param type   Type to use in RDS
     * @return data structure
     * @since 2.29
     */
    @Nonnull
    public static IRaptorDataStructure createManualLocationStructure(@Nonnull String family, @Nonnull String type)
    {
        return new DataStructureBuilder(family, type, LOCATION_MESSAGE_VERSION, LOCATION_MESSAGE_TYPE)
                .addStructureMetadata(IRaptorDataStructure.METADATA_STRUCTURE_DESCRIPTION, LOCATION_MESSAGE_DESCRIPTION)
                .addStructureMetadata(IRaptorDataStructure.METADATA_STRUCTURE_DETAILS, LOCATION_MESSAGE_DETAILS)
                .setRenderable(false)
                .build();
    }

    /**
     * Create a structure builder for the given family, type, message type, and version.
     *
     * @param mapObjectFamily The family of the map object.  Must not be {@code null}.
     * @param mapObjectType   The type of map object that you need a data structure for. Must not be {@code null}.
     * @param version         The version of the data structure. Must not be {@code null}.
     * @param messageType     Message type name.  Must not be {@code null}.
     * @throws IllegalArgumentException If any of the parameters are {@code null}
     */
    public DataStructureBuilder(@Nonnull String mapObjectFamily, @Nonnull String mapObjectType, @Nonnull String version,
                                @Nonnull String messageType)
    {
        this.mapObjectFamily = mapObjectFamily;
        this.mapObjectType = mapObjectType;
        this.messageType = messageType;
        this.version = version;
    }

    /**
     * Establish the default renderability of any RDMs created from this structure such that if the RDM's hasLocation()
     * returns true, but isRenderable() returns false, no track point is automatically generated on the globe. In this
     * instance, the developer is left to visualize the data as they see fit in some arbitrary fashion.
     * <p>
     * Default is true.
     *
     * @param renderable Pass {@code false} to indicate that RDMs should not be rendered as track points,
     *                   even if hasLocation() returns true.
     * @return this builder
     */
    public DataStructureBuilder setRenderable(boolean renderable)
    {
        checkBuilt();
        this.renderable = renderable;
        return this;
    }

    /**
     * Create a map of structures built from an XML document.
     *
     * @param mapObjectFamily The family of the map object. Must not be {@code null}.
     * @param mapObjectType   The type of map object that you need a data structure for. Must not be {@code null}.
     * @param xmlStructUrl    URL that points to an XML document that can be validated against the
     *                        http://www.doe.gov/2010/RaptorStructureSchema schema.
     * @return Map of structures defined by the XML document.
     * @throws IOException          if the xmlStructUrl URL cannot be accessed
     * @throws JAXBException        if the XML document pointed to by xmlStructUrl is invalid and cannot be bound
     *                              to Java objects
     */
    public static Map<String, IRaptorDataStructure> buildFromXML(@Nonnull String mapObjectFamily,
                                                                 @Nonnull String mapObjectType,
                                                                 @Nonnull URL xmlStructUrl)
            throws IOException, JAXBException
    {
        try (InputStream inputStream = xmlStructUrl.openStream())
        {
            return buildFromXML(mapObjectFamily, mapObjectType, inputStream);
        }
    }

    /**
     * Create a map of structures built from an XML document.
     *
     * @param mapObjectFamily The family of the map object. Must not be {@code null}.
     * @param mapObjectType   The type of map object that you need a data structure for. Must not be {@code null}.
     * @param inputStream     InputStream on an XML document that can be validated against the
     *                        http://www.doe.gov/2010/RaptorStructureSchema schema.
     * @return Map of structures defined by the XML document.
     * @throws IllegalArgumentException if any of the parameters are {@code null}
     * @throws JAXBException            if the XML document input stream is invalid and cannot be bound to Java objects
     */
    public static Map<String, IRaptorDataStructure> buildFromXML(@Nonnull String mapObjectFamily,
                                                                 @Nonnull String mapObjectType,
                                                                 @Nonnull InputStream inputStream)
            throws JAXBException
    {
        final Map<String, IRaptorDataStructure> structureMap = new HashMap<>();

        final StructuresCollection structuresCollection = jaxbContext.createUnmarshaller()
                .unmarshal(new StreamSource(inputStream), StructuresCollection.class)
                .getValue();

        for (StructureType struct : structuresCollection.getStructure())
        {
            final String messageType = struct.getMessageType();

            if (structureMap.containsKey(messageType))
            {
                logger.warn("Message type '{}' was already defined within the XML document- definition skipped", messageType);
                continue;
            }

            // For some reason, even though a default value is specified in the schema, no default value is
            // provided for the renderable setting.  So, handle it manually.

            final Boolean renderable = struct.isRenderable();
            final boolean isRenderable = renderable != null ? renderable : true; // default to true

            String version = struct.getVersion();

            DataStructureBuilder dsb = new DataStructureBuilder(mapObjectFamily, mapObjectType, version, messageType);

            dsb.setRenderable(isRenderable);

            try
            {
                dsb.processStructureFromXML(struct);
            } catch (NoSuchFieldException | InvalidFieldProcessorException e)
            {
                throw new JAXBException("Failed to process structure XML", e);
            }

            structureMap.put(messageType, dsb.build());

            logger.debug("Processed structure {}", messageType);
        }

        return structureMap;
    }

    /**
     * Builds an {@link IRaptorDataStructure} based on the configuration of this builder instance.
     *
     * @return IRaptorDataStructure instance
     */
    public IRaptorDataStructure build()
    {
        built = true; // No modifications after we've built

        // See if there's an existing structure with this hash
        final String hash = calculateHash();

        IStructure structure = structurePersistenceSupport.findDataStructure(hash);
        if (structure == null)
        {
            structure = Structure.createFromBuilder(this, hash);

            // assign result of persist to structure in case the structure was already added by another thread.
            structure = structurePersistenceSupport.persistDataStructure(structure);
        } else
        {
            for (DataField dataField : getDataFields())
            {
                final IFieldDisplayProcessor processor = dataField.getFieldDisplayProcessor();
                if (processor != null)
                {
                    ((DataField) structure.getField(dataField.getFieldName())).setFieldDisplayProcessor(processor);
                }
            }
        }

        return new RaptorDataStructure(mapObjectFamily, mapObjectType, structure);
    }

    /**
     * Calculate the structure hash based on the contents of this builder.
     *
     * @return hash for this configuration
     * @since 3.0 (was previously in the Structure class)
     */
    @SuppressWarnings("ConstantConditions")
    public String calculateHash()
    {
        final String messageType = getMessageType();
        final String version = getVersion();

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        StringBuilder listing = null;
        if (logger.isTraceEnabled())
        {
            listing = new StringBuilder();
            listing.append("Listing for ").append(messageType).append(' ').append(version).append(':');
        }

        try (ObjectOutputStream oos = new ObjectOutputStream(out))
        {
            // We use ObjectOutputStream.writeUnshared in order avoid generating different hashes at different
            // times for the exact same definition due to the Serialization decision to either use a new encoding
            // for every reference or to use a reference to a previously written object when the values are equal.

            oos.writeUnshared(messageType);
            oos.writeUnshared(version);

            final Collection<DataField> dataFields = getDataFields();

            if (dataFields != null)
            {
                final List<DataField> sortedFields = new ArrayList<>(dataFields);
                sortedFields.sort(Comparator.comparing(DataField::getFieldName));

                for (DataField dataField : sortedFields)
                {
                    oos.writeUnshared(dataField.getFieldName());
                    oos.writeUnshared(dataField.getFieldType());
                    oos.writeUnshared(dataField.getDefaultValue());
                    oos.writeUnshared(dataField.getOrdinalPosition());
                    oos.writeUnshared(dataField.getFieldLength());

                    if (logger.isTraceEnabled()) listing.append("\n    ").append(dataField.fullString());
                }
            }

            oos.flush();
        } catch (IOException e)
        {
            logger.error("Error generating structure hash", e);
        }

        final byte[] bytes = out.toByteArray();
        final String hash = HashUtils.getHash(bytes);

        if (logger.isTraceEnabled())
        {
            listing.append("\n  Byte Array (").append(bytes.length).append("):");
            listing.append("\n    ");
            int i = 1;
            for (byte b : bytes)
            {
                listing.append(String.format("%02X", b));
                if (i % 8 == 0) listing.append(" ");
                if (i % 32 == 0) listing.append("\n    ");
                i++;
            }
            listing.append("\nFinal Hash: ").append(hash);

            logger.trace(listing.toString());
        }

        return hash;
    }

    /**
     * Identifies if the structure's RDMs should default to a renderable state or not. RDMs with a valid location
     * but renderable set to false are not represented on the globe as track points.
     *
     * @return true, if RDMs based upon this structure are renderable by default.
     */
    public boolean isRenderable()
    {
        return renderable;
    }

    /**
     * Check if the {@link #build()} method has already been called.  Throw an exception if so.
     *
     * @throws IllegalStateException If {@link #build()} has already been called
     * @since 3.0
     */
    private void checkBuilt()
    {
        if (built)
        {
            throw new IllegalStateException("Modifications are not allowed after calling build()! Structure: " + messageType);
        }
    }

    /**
     * Identifies the Map Object Family associated with this structure definition.
     * This method was kept for backward compatibility.
     *
     * @return Map Object Family
     */
    public String getMapObjectFamily()
    {
        return mapObjectFamily;
    }

    /**
     * Identifies the Map Object Type associated with this structure definition.
     *
     * @return Map Object Type
     */
    public String getMapObjectType()
    {
        return mapObjectType;
    }

    /**
     * @return The structure version
     */
    public String getVersion()
    {
        return version;
    }

    /**
     * Identifies the Structure Message Type, an identifier that is used as part of the key that defines this structure.
     * This method was kept for backward compatibility.
     *
     * @return Message Type
     */
    public String getMessageType()
    {
        return messageType;
    }

    /**
     * @return The collection of data fields defined in this builder
     * @since 3.0
     */
    public Collection<DataField> getDataFields()
    {
        return Collections.unmodifiableCollection(dataMap.values());
    }

    /**
     * Metadata about the structure as a whole.
     *
     * @return non-null map of key-value pairs that define metadata about this structure
     */
    public Map<String, String> getStructureMetadata()
    {
        return Collections.unmodifiableMap(structureMetadataMap);
    }

    /**
     * Adds a key-value pair of metadata to the structure definition.
     *
     * @param key   Non-null metadata key
     * @param value Non-null metadata value to associate with the key
     * @return DataStructureBuilder instance used to continue building the structure
     * @throws IllegalArgumentException if either key or value is {@code null}
     */
    public DataStructureBuilder addStructureMetadata(@Nonnull String key, @Nonnull String value)
    {
        structureMetadataMap.put(key, value);

        return this;
    }

    /**
     * Adds a key-value pair of metadata to the previously defined field within the structure definition.
     *
     * @param fieldName Name of the field to associate the key-value pair with.  Field must be already defined
     * @param key       Non-null metadata key
     * @param value     Non-null metadata value to associate with the key
     * @return DataStructureBuilder instance used to continue building the structure
     * @throws IllegalArgumentException if the field name cannot be found within the structure definition
     */
    public DataStructureBuilder addFieldMetadata(@Nonnull String fieldName, @Nonnull String key,
                                                 @Nonnull String value)
            throws NoSuchFieldException
    {
        getDataField(fieldName).setMetadataValue(key, value);

        return this;
    }

    /**
     * @return The named data field.
     * @throws NoSuchFieldException If no field exists with the given name
     * @since 3.0
     */
    private DataField getDataField(String fieldName) throws NoSuchFieldException
    {
        final DataField field = dataMap.get(fieldName);
        if (field == null) throw new NoSuchFieldException("No such field " + fieldName + " in " + messageType);
        return field;
    }

    /**
     * Add a string field. The string has a default limit of 256 characters.
     *
     * @param fieldName Name of the field.
     * @return This builder.
     * @see #setFieldLength(String, int)
     */
    public DataStructureBuilder addString(String fieldName)
    {
        return addString(fieldName, null);
    }

    public DataStructureBuilder addString(String fieldName, String defaultValue)
    {
        return addString(fieldName, defaultValue, DEFAULT_ORDINAL_POSITION);
    }

    public DataStructureBuilder addString(String fieldName, String defaultValue, int ordinalPosition)
    {
        return addField(fieldName, IDataType.STRING, defaultValue, ordinalPosition);
    }

    /**
     * Add a string array field. The array has a default limit of 10MB of encoded data.
     *
     * @param fieldName Name of the field.
     * @return This builder.
     * @see #setFieldLength(String, int)
     */
    public DataStructureBuilder addStringArray(String fieldName)
    {
        return addStringArray(fieldName, null);
    }

    public DataStructureBuilder addStringArray(String fieldName, String[] defaultValue)
    {
        return addStringArray(fieldName, defaultValue, DEFAULT_ORDINAL_POSITION);
    }

    public DataStructureBuilder addStringArray(String fieldName, String[] defaultValue, int ordinalPosition)
    {
        return addField(fieldName, IDataType.STRING_ARRAY, defaultValue, ordinalPosition);
    }

    public DataStructureBuilder addBoolean(String fieldName)
    {
        return addBoolean(fieldName, null);
    }

    public DataStructureBuilder addBoolean(String fieldName, Boolean defaultValue)
    {
        return addBoolean(fieldName, defaultValue, DEFAULT_ORDINAL_POSITION);
    }

    public DataStructureBuilder addBoolean(String fieldName, Boolean defaultValue, int ordinalPosition)
    {
        return addField(fieldName, IDataType.BOOLEAN, defaultValue, ordinalPosition);
    }

    /**
     * Add a boolean array field. The array has a default limit of 10MB of encoded data.
     *
     * @param fieldName Name of the field.
     * @return This builder.
     * @see #setFieldLength(String, int)
     */
    public DataStructureBuilder addBooleanArray(String fieldName)
    {
        return addBooleanArray(fieldName, null);
    }

    public DataStructureBuilder addBooleanArray(String fieldName, boolean[] defaultValue)
    {
        return addBooleanArray(fieldName, defaultValue, DEFAULT_ORDINAL_POSITION);
    }

    public DataStructureBuilder addBooleanArray(String fieldName, boolean[] defaultValue, int ordinalPosition)
    {
        return addField(fieldName, IDataType.BOOLEAN_ARRAY, defaultValue, ordinalPosition);
    }

    public DataStructureBuilder addShort(String fieldName)
    {
        return addShort(fieldName, null);
    }

    public DataStructureBuilder addShort(String fieldName, Short defaultValue)
    {
        return addShort(fieldName, defaultValue, DEFAULT_ORDINAL_POSITION);
    }

    public DataStructureBuilder addShort(String fieldName, Short defaultValue, int ordinalPosition)
    {
        return addField(fieldName, IDataType.SHORT, defaultValue, ordinalPosition);
    }

    /**
     * Add a short array field. The array has a default limit of 10MB of encoded data.
     *
     * @param fieldName Name of the field.
     * @return This builder.
     * @see #setFieldLength(String, int)
     */
    public DataStructureBuilder addShortArray(String fieldName)
    {
        return addShortArray(fieldName, null);
    }

    public DataStructureBuilder addShortArray(String fieldName, short[] defaultValue)
    {
        return addShortArray(fieldName, defaultValue, DEFAULT_ORDINAL_POSITION);
    }

    public DataStructureBuilder addShortArray(String fieldName, short[] defaultValue, int ordinalPosition)
    {
        return addField(fieldName, IDataType.SHORT_ARRAY, defaultValue, ordinalPosition);
    }

    public DataStructureBuilder addInt(String fieldName)
    {
        return addInt(fieldName, null);
    }

    public DataStructureBuilder addInt(String fieldName, Integer defaultValue)
    {
        return addInt(fieldName, defaultValue, DEFAULT_ORDINAL_POSITION);
    }

    public DataStructureBuilder addInt(String fieldName, Integer defaultValue, int ordinalPosition)
    {
        return addField(fieldName, IDataType.INT, defaultValue, ordinalPosition);
    }

    /**
     * Add a integer array field. The array has a default limit of 10MB of encoded data.
     *
     * @param fieldName Name of the field.
     * @return This builder.
     * @see #setFieldLength(String, int)
     */
    public DataStructureBuilder addIntArray(String fieldName)
    {
        return addIntArray(fieldName, null);
    }

    public DataStructureBuilder addIntArray(String fieldName, int[] defaultValue)
    {
        return addIntArray(fieldName, defaultValue, DEFAULT_ORDINAL_POSITION);
    }

    public DataStructureBuilder addIntArray(String fieldName, int[] defaultValue, int ordinalPosition)
    {
        return addField(fieldName, IDataType.INT_ARRAY, defaultValue, ordinalPosition);
    }

    public DataStructureBuilder addFloat(String fieldName)
    {
        return addFloat(fieldName, null);
    }

    public DataStructureBuilder addFloat(String fieldName, Float defaultValue)
    {
        return addFloat(fieldName, defaultValue, DEFAULT_ORDINAL_POSITION);
    }

    public DataStructureBuilder addFloat(String fieldName, Float defaultValue, int ordinalPosition)
    {
        return addField(fieldName, IDataType.FLOAT, defaultValue, ordinalPosition);
    }

    /**
     * Add a float array field. The array has a default limit of 10MB of encoded data.
     *
     * @param fieldName Name of the field.
     * @return This builder.
     * @see #setFieldLength(String, int)
     */
    public DataStructureBuilder addFloatArray(String fieldName)
    {
        return addFloatArray(fieldName, null);
    }

    public DataStructureBuilder addFloatArray(String fieldName, float[] defaultValue)
    {
        return addFloatArray(fieldName, defaultValue, DEFAULT_ORDINAL_POSITION);
    }

    public DataStructureBuilder addFloatArray(String fieldName, float[] defaultValue, int ordinalPosition)
    {
        return addField(fieldName, IDataType.FLOAT_ARRAY, defaultValue, ordinalPosition);
    }

    public DataStructureBuilder addLong(String fieldName)
    {
        return addLong(fieldName, null);
    }

    public DataStructureBuilder addLong(String fieldName, Long defaultValue)
    {
        return addLong(fieldName, defaultValue, DEFAULT_ORDINAL_POSITION);
    }

    public DataStructureBuilder addLong(String fieldName, Long defaultValue, int ordinalPosition)
    {
        return addField(fieldName, IDataType.LONG, defaultValue, ordinalPosition);
    }

    /**
     * Add a long array field. The array has a default limit of 10MB of encoded data.
     *
     * @param fieldName Name of the field.
     * @return This builder.
     * @see #setFieldLength(String, int)
     */
    public DataStructureBuilder addLongArray(String fieldName)
    {
        return addLongArray(fieldName, null);
    }

    public DataStructureBuilder addLongArray(String fieldName, long[] defaultValue)
    {
        return addLongArray(fieldName, defaultValue, DEFAULT_ORDINAL_POSITION);
    }

    public DataStructureBuilder addLongArray(String fieldName, long[] defaultValue, int ordinalPosition)
    {
        return addField(fieldName, IDataType.LONG_ARRAY, defaultValue, ordinalPosition);
    }

    public DataStructureBuilder addDouble(String fieldName)
    {
        return addDouble(fieldName, null);
    }

    public DataStructureBuilder addDouble(String fieldName, Double defaultValue)
    {
        return addDouble(fieldName, defaultValue, DEFAULT_ORDINAL_POSITION);
    }

    public DataStructureBuilder addDouble(String fieldName, Double defaultValue, int ordinalPosition)
    {
        return addField(fieldName, IDataType.DOUBLE, defaultValue, ordinalPosition);
    }

    /**
     * Add a double array field. The array has a default limit of 10MB of encoded data.
     *
     * @param fieldName Name of the field.
     * @return This builder.
     * @see #setFieldLength(String, int)
     */
    public DataStructureBuilder addDoubleArray(String fieldName)
    {
        return addDoubleArray(fieldName, null);
    }

    public DataStructureBuilder addDoubleArray(String fieldName, double[] defaultValue)
    {
        return addDoubleArray(fieldName, defaultValue, DEFAULT_ORDINAL_POSITION);
    }

    public DataStructureBuilder addDoubleArray(String fieldName, double[] defaultValue, int ordinalPosition)
    {
        return addField(fieldName, IDataType.DOUBLE_ARRAY, defaultValue, ordinalPosition);
    }

    public DataStructureBuilder addDate(String fieldName)
    {
        return addDate(fieldName, null);
    }

    public DataStructureBuilder addDate(String fieldName, Date defaultValue)
    {
        return addDate(fieldName, defaultValue, DEFAULT_ORDINAL_POSITION);
    }

    public DataStructureBuilder addDate(String fieldName, Date defaultValue, int ordinalPosition)
    {
        return addField(fieldName, IDataType.DATE, defaultValue, ordinalPosition);
    }

    /**
     * Add a date array field. The array has a default limit of 10MB of encoded data.
     *
     * @param fieldName Name of the field.
     * @return This builder.
     * @see #setFieldLength(String, int)
     */
    public DataStructureBuilder addDateArray(String fieldName)
    {
        return addDateArray(fieldName, null);
    }

    public DataStructureBuilder addDateArray(String fieldName, Date[] defaultValue)
    {
        return addDateArray(fieldName, defaultValue, DEFAULT_ORDINAL_POSITION);
    }

    public DataStructureBuilder addDateArray(String fieldName, Date[] defaultValue, int ordinalPosition)
    {
        return addField(fieldName, IDataType.DATE_ARRAY, defaultValue, ordinalPosition);
    }

    /**
     * Add a byte array field. The array has a default limit of 10MB.
     *
     * @param fieldName Name of the field.
     * @return This builder.
     * @see #setFieldLength(String, int)
     */
    public DataStructureBuilder addByteArray(String fieldName)
    {
        return addByteArray(fieldName, null);
    }

    public DataStructureBuilder addByteArray(String fieldName, byte[] defaultValue)
    {
        return addByteArray(fieldName, defaultValue, DEFAULT_ORDINAL_POSITION);
    }

    public DataStructureBuilder addByteArray(String fieldName, byte[] defaultValue, int ordinalPosition)
    {
        return addField(fieldName, IDataType.BYTE_ARRAY, defaultValue, ordinalPosition);
    }

    /**
     * Adds a field to the builder.
     *
     * @param fieldName    Name of the field
     * @param type         The data type (should be one of the values in {@link IDataType})
     * @param defaultValue The default value for the field
     * @return This builder
     * @since 3.0
     */
    @Nonnull
    public DataStructureBuilder addField(@Nonnull String fieldName, int type, @Nullable Object defaultValue)
    {
        return addField(fieldName, type, defaultValue, DEFAULT_ORDINAL_POSITION);
    }

    /**
     * Adds a field to the builder.
     *
     * @param fieldName       Name of the field
     * @param type            The data type (should be one of the values in {@link IDataType})
     * @param defaultValue    The default value for the field
     * @param ordinalPosition The ordinal position of the field within the structure
     * @return This builder
     */
    @Nonnull
    private DataStructureBuilder addField(@Nonnull String fieldName, int type, @Nullable Object defaultValue, int ordinalPosition)
    {
        if (messageType.equalsIgnoreCase(LOCATION_MESSAGE_TYPE))
        {
            throw new IllegalStateException("LOCATION_MESSAGE_TYPE structures don't support user-defined fields");
        }

        checkBuilt();

        if (dataMap.containsKey(fieldName))
        {
            throw new IllegalArgumentException("A field with this name has already been added: '" + fieldName + "'");
        }

        // Ensure that the field name doesn't match a core field
        if (CoreField.isCoreFieldName(fieldName))
        {
            throw new IllegalArgumentException("Structures can not contain a field with same name as a core field. "
                    + "Structure: " + messageType + ", field: " + fieldName);
        }

        // Now see if the neutralized form of the field name (the DB column name) conflicts
        final String neutralizedFieldName = fieldName.replaceAll("\\W", "");
        final String conflictingFieldName = neutralizedFieldMap.put(neutralizedFieldName, fieldName);

        if (conflictingFieldName != null)
        {
            final String errorMsg = String.format("An existing field [%s] in structure [%s/%s/%s] conflicts with the new field [%s] because they generate the same database column name [%s]",
                    conflictingFieldName, mapObjectFamily, messageType, version, fieldName, neutralizedFieldName);

            logger.error(errorMsg);
            throw new IllegalArgumentException(errorMsg);
        }

        DataField dataField = new DataField(fieldName, type, defaultValue, ordinalPosition);

        dataMap.put(fieldName, dataField);

        return this;
    }

    /**
     * Add an index to the structure across the fields specified. The index will be built at the time that the
     * structure is created in the database. If multiple fields are specified then the index is created in the order
     * given.<br>
     * <br>
     * Note: The indexing settings are not used to calculate the structure hash.<br>
     *
     * @param unique     Set to true to specify that the index should be unique. A setting of false will allow duplicates.
     * @param fieldNames The list of fields to participate in the index.
     * @return DataStructureBuilder instance used to continue building the structure
     */
    public DataStructureBuilder addIndex(boolean unique, String... fieldNames)
    {
        indexes.add(new IndexDefinition(unique, fieldNames));

        return this;
    }

    /**
     * @return The indices defined on the structure
     */
    public List<IndexDefinition> getIndexes()
    {
        return Collections.unmodifiableList(indexes);
    }

    /**
     * Set the desired length of the field. The length is only valid on String and Array type fields. If a length of
     * {@link Integer#MAX_VALUE} is passed in for a String type field, the column will be created with no length limit.
     * Be sure to add the fields to the structure prior to calling this method. Failure to do so will throw an exception.<br>
     * <br>
     * Note: The length value is not used to calculate the structure hash.<br>
     *
     * @param fieldName The field to set the length on.
     * @param len       The length of the field.
     * @return DataStructureBuilder instance used to continue building the structure
     * @throws NoSuchFieldException If the field cannot be found in the structure.
     */
    public DataStructureBuilder setFieldLength(String fieldName, int len) throws NoSuchFieldException
    {
        getDataField(fieldName).setFieldLength(len);

        return this;
    }

    /**
     * Set a processor for a field as a class name.  Class name is instantiated and added to the prototype so that it
     * is properly added to the structure field when it is built.
     *
     * @param fieldName name of field to add processor for.
     * @param className processor class name as a String.
     * @return DataStructureBuilder instance used to continue building the structure.
     * @throws NoSuchFieldException           If the field cannot be found in the structure.
     * @throws InvalidFieldProcessorException If the processor cannot be instantiated.
     * @since 2.29
     */
    public DataStructureBuilder setFieldDisplayProcessor(@Nonnull String fieldName, @Nonnull String className) throws NoSuchFieldException, InvalidFieldProcessorException
    {
        ClassLoader cl = devicePluginRegistry.getPluginClassLoader(mapObjectFamily, mapObjectType);
        try
        {
            Class<? extends IFieldDisplayProcessor> clazz = Class.forName(className, true, cl).asSubclass(IFieldDisplayProcessor.class);
            IFieldDisplayProcessor classObj = clazz.getConstructor().newInstance();

            getDataField(fieldName).setFieldDisplayProcessor(classObj);

            logger.debug("Added custom field processing class {} for field {}", className, fieldName);
        } catch (ClassCastException e)
        {
            throw new InvalidFieldProcessorException("Error field display processor class " + className +
                    " in the plugin family " + mapObjectFamily + ", type " + mapObjectType + " does not implement IFieldDisplayProcessor");
        } catch (ClassNotFoundException e)
        {
            throw new InvalidFieldProcessorException("Error trying to find field display processor class by name: " + className +
                    " in the plugin family " + mapObjectFamily + ", type " + mapObjectType, e.getCause());
        } catch (InstantiationException | IllegalAccessException | NoSuchMethodException | InvocationTargetException e)
        {
            throw new InvalidFieldProcessorException("Error instantiating field display processor class " + className +
                    " for the plugin family " + mapObjectFamily + ", type " + mapObjectType, e.getCause());
        }

        return this;
    }

    /**
     * Process the internalized data from an XML document describing a structure (JAXB bindings).  Fill out this
     * builder with the data specified in the XML.
     *
     * @param struct StructureType to process
     */
    private void processStructureFromXML(StructureType struct) throws NoSuchFieldException, InvalidFieldProcessorException
    {
        logger.debug("Defining a structure named {} of version {}", messageType, version);

        processStructureMetadata(struct); // Process the structure level metadata

        // Process the fields

        final FieldCollection fields = struct.getFields();

        if (fields != null)
        {
            for (BaseFieldType field : fields.getStringFieldOrBooleanFieldOrIntegerField())
            {
                final Class<? extends BaseFieldType> fieldClass = field.getClass();

                final String fieldName = field.getName();
                final Integer fieldType = XML_TYPE_TO_FIELD_TYPE.get(fieldClass);
                final Object defaultValue = DEFAULT_VALUE_EXTRACTOR.get(fieldClass).apply(field);

                addField(fieldName, fieldType, defaultValue, field.getPosition());

                if (logger.isDebugEnabled())
                {
                    logger.debug("Added field {}, type {}", fieldName, fieldClass.getSimpleName());
                }

                // Handle the fieldLength attribute (common to all fields)
                // Not actually used on primitive types, but this keeps the code consistent.

                final Integer fieldLength = field.getFieldLength();
                if (fieldLength != null)
                {
                    setFieldLength(fieldName, fieldLength);
                }

                processFieldMetadata(field);

                if (field.isIndexed())
                {
                    addIndex(false, fieldName);
                }

                final String fieldProcessor = field.getDisplayProcessor();
                if (fieldProcessor != null)
                {
                    setFieldDisplayProcessor(fieldName, fieldProcessor);
                }
            }
        }

        processIndices(struct);
    }

    /**
     * Process the metadata for the structure as a whole.
     *
     * @param structureType Structure data from XML
     */
    private void processStructureMetadata(StructureType structureType)
    {
        final MetadataType structMetadata = structureType.getMetadata();

        if (null != structMetadata)
        {
            final List<JAXBElement<String>> structKeyValues = structMetadata.getKeyAndValue();
            int size = structKeyValues.size();
            for (int i = 0; i < size; i += 2)
            {
                final String key = structKeyValues.get(i).getValue();
                final String value = structKeyValues.get(i + 1).getValue();
                try
                {
                    addStructureMetadata(key, value);
                } catch (IllegalArgumentException e)
                {
                    logger.warn("Structure metadata with key {} and value {} skipped", key, value, e);
                }
            }
        }
    }

    /**
     * Process the metadata for an individual field.
     *
     * @param field Field data from XML
     */
    private void processFieldMetadata(BaseFieldType field) throws NoSuchFieldException
    {
        final MetadataType fieldMetadata = field.getMetadata();

        if (null != fieldMetadata)
        {
            final String fieldName = field.getName();
            final List<JAXBElement<String>> fieldKeyValues = fieldMetadata.getKeyAndValue();
            final int size = fieldKeyValues.size();

            for (int i = 0; i < size; i += 2)
            {
                String key = fieldKeyValues.get(i).getValue();
                String value = fieldKeyValues.get(i + 1).getValue();

                addFieldMetadata(fieldName, key, value);
            }
        }
    }

    /**
     * Process the indices defined for the structure.
     *
     * @param struct Structure data from XML
     */
    private void processIndices(StructureType struct)
    {
        final IndexCollection indexes = struct.getIndexes();

        if (indexes != null)
        {
            indexes.getIndex().forEach(index -> addIndex(index.isUnique(), index.getField().toArray(new String[0])));
        }
    }
}
