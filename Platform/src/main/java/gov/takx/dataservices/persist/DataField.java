package gov.takx.dataservices.persist;

import gov.takx.api.annotation.Internal;
import gov.takx.api.messages.DataFieldEncoding;
import gov.takx.api.messages.IDataField;
import gov.takx.api.messages.IFieldDisplayProcessor;
import gov.takx.api.messages.IRaptorDataStructure;
import gov.takx.api.messages.IStructure;
import gov.takx.api.persist.IPersistable;
import gov.takx.commons.date.DateTimeParser;
import gov.takx.platform.utils.DatabaseUtils;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PostLoad;
import jakarta.persistence.Transient;
import org.hibernate.annotations.Immutable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.Serializable;
import java.lang.invoke.MethodHandles;
import java.sql.Types;
import java.time.ZoneId;

/**
 * Single strongly-typed field that is part of a collection of fields grouped under the auspices of an
 * {@link IRaptorDataStructure}.  Each field consists of a logical name, a strongly-typed data type, an optional
 * default value, and an ordinal position that relates this field to the other fields grouped by the
 * RaptorDataStructure.
 * <p>
 * If no ordinal position is specified, then a collection of DataField instances will be sorted based on
 * a case-insensitive field name comparison.  When no ordinal position is specified, a default of 999 will
 * be automatically defined by the constructor.  In the event two fields have an identical ordinal position,
 * then sorting is based strictly upon the case-insensitive field name comparison, otherwise sorting is
 * always based upon the ordinal position of the field.
 * <p>
 * Providing an ordinal position via the RaptorDataStructure add*DataType*() methods is strongly encouraged.
 * The value of specifying an ordinal position is embodied by the Device Log Panel which automatically defaults
 * to displaying the fields that comprise a RaptorDataStructure based on their sort order.
 *
 * @see IRaptorDataStructure
 */
@Internal
@Entity
@Immutable
public class DataField extends AEntityWithMetadata implements Serializable, IPersistable, IDataField
{
    private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    private static final String DATE_PATTERN = "yyyy-MM-dd HH:mm:ss.SSSS";
    private static final DateTimeParser DATE_FORMAT = new DateTimeParser(DATE_PATTERN, DateTimeParser.Style.DateTimeWithZone, ZoneId.of("UTC"));

    private static final DateTimeParser DATE_DISPLAY_FORMAT =
            new DateTimeParser(DateTimeParser.ISO_8601_DATE_FORMAT, ZoneId.of("UTC"));

    @SuppressWarnings("unused") // set by hibernate
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    private String fieldName;
    private int fieldType;
    private Integer fieldLength;
    private String defaultValue;
    private int ordinalPosition;

    @ManyToOne(targetEntity = Structure.class)
    @JoinColumn(name = "structure_id")
    private IStructure structure;

    @Transient
    private int sqlType = Types.NULL;

    @Transient
    private String neutralizedFieldName = null;

    @Transient
    private Object usableDefaultValue = null;

    @Transient
    private IFieldDisplayProcessor fieldDisplayProcessor = null;

    protected DataField()
    {
    }

    /**
     * Constructor for use by DataStructureBuilder.
     *
     * @since 3.0
     */
    public DataField(String fieldName, int fieldType, Object defaultValue, int ordinalPosition)
    {
        this.fieldName = fieldName;
        this.fieldType = fieldType;
        this.ordinalPosition = ordinalPosition;

        setDefaultFieldLength();
        setDefaultFieldProcessor();

        usableDefaultValue = defaultValue;

        this.defaultValue = defaultValue == null
                ? null
                : DataFieldEncoding.encodeFieldValue(fieldName, fieldType, defaultValue, DATE_FORMAT);
    }

    /**
     * Set the owning structure for this data field.  Package private, only for use by {@code Structure#addField}.
     *
     * @param structure Owning structure
     * @since 3.0
     */
    void setStructure(IStructure structure)
    {
        // This should never happen in real life, but errant tests can do evil things
        if (this.structure != null) logger.error("Replacing structure: old={} new={}", this.structure, structure);

        this.structure = structure;
    }

    @Override
    public int getOrdinalPosition()
    {
        return ordinalPosition;
    }

    @Override
    public String getFieldName()
    {
        return fieldName;
    }

    @Override
    public int getFieldType()
    {
        return fieldType;
    }

    @Override
    public Object getDefaultValue()
    {
        try
        {
            if (defaultValue != null && usableDefaultValue == null)
            {
                usableDefaultValue = DataFieldEncoding.decodeFieldValue(fieldName, fieldType, defaultValue, DATE_FORMAT);
            }
        } catch (Exception e)
        {
            logger.error("Failed to parse default value of field {} [{}]: {}", fieldName, defaultValue, e.getMessage());
        }
        return usableDefaultValue;
    }

    @Override
    public Integer getId()
    {
        return id;
    }

    @Override
    public IStructure getStructure()
    {
        return structure;
    }

    @Override
    public int hashCode()
    {
        int result = fieldName.hashCode();
        result = 31 * result + fieldType;
        result = 31 * result + ordinalPosition;
        return result;
    }

    @Override
    public boolean equals(Object o)
    {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        DataField dataField = (DataField) o;

        if (fieldType != dataField.fieldType) return false;
        if (ordinalPosition != dataField.ordinalPosition) return false;
        return fieldName.equals(dataField.fieldName);
    }

    @Override
    public String toString()
    {
        return "[" +
                fieldName +
                ": " + getDataTypeName() +
                ": " + ordinalPosition +
                "]";
    }

    /**
     * A more complete toString.
     *
     * @return Complete representation of the data field
     */
    public String fullString()
    {
        return "[" +
                id +
                ": " + fieldName +
                ": " + getDataTypeName() +
                ": " + ordinalPosition +
                ": " + fieldLength +
                ": " + defaultValue +
                "]";
    }

    /**
     * Imposes a sort order based first upon the ordinal position, and then upon
     * the case-insensitive field name.
     *
     * @param df DataField to compare to
     * @return a negative integer, zero, or a positive integer as this object
     * is less than, equal to, or greater than the specified object.
     */
    @Override
    public int compareTo(IDataField df)
    {
        final int result = Integer.compare(ordinalPosition, df.getOrdinalPosition());

        return result != 0
                ? result
                : String.CASE_INSENSITIVE_ORDER.compare(getFieldName(), df.getFieldName()); // defer to field name
    }

    /**
     * Set the length of this field, which defines the underlying database column length.
     * <p>
     * Only to be used by DataStructureBuilder prior to structure creation.
     *
     * @param length Field length
     */
    public void setFieldLength(Integer length)
    {
        fieldLength = length;
    }

    @Override
    public Integer getFieldLength()
    {
        return fieldLength;
    }

    /**
     * Set the processor on the data field, used primarily for processing field objects and displaying them
     * as human readable strings in places like the device log.  Useful, for example, in turning a byte array
     * into a readable string.
     * <p>
     * Only to be used by DataStructureBuilder prior to structure creation.
     *
     * @param fieldDisplayProcessor the field processor
     * @since 2.29
     */
    public void setFieldDisplayProcessor(IFieldDisplayProcessor fieldDisplayProcessor)
    {
        this.fieldDisplayProcessor = fieldDisplayProcessor;
    }

    @Override
    public IFieldDisplayProcessor getFieldDisplayProcessor()
    {
        return fieldDisplayProcessor;
    }

    @Override
    public int getSQLType()
    {
        return sqlType != Types.NULL ? sqlType : (sqlType = DatabaseUtils.getSqlTypeForDataType(fieldType));
    }

    @Override
    public String getNeutralizedFieldName()
    {
        // Don't try to synchronize this method, it's not worth the overhead and the worst case is that
        // the neutralized field name would be generated twice.

        return neutralizedFieldName == null
                ? neutralizedFieldName = "F_" + fieldName.replaceAll("\\W", "")
                : neutralizedFieldName;
    }

    /**
     * Set the default field length for this field. This method only handles String and array type fields.
     *
     * @since 3.0
     */
    private void setDefaultFieldLength()
    {
        if (isArrayType())
        {
            setFieldLength(DEFAULT_ARRAY_LENGTH);
        } else if (fieldType == STRING)
        {
            setFieldLength(DEFAULT_STRING_LENGTH);
        }
    }

    /**
     * Set a default field processor for arrays, which in general require processing anyways.  Non-arrays shouldn't
     * need any default processing since returning the object type is generally desired in places like the
     * device log where sorting is an option.
     *
     * @since 2.29
     */
    private void setDefaultFieldProcessor()
    {
        switch (fieldType)
        {
            case BYTE_ARRAY:
            case STRING_ARRAY:
            case BOOLEAN_ARRAY:
            case INT_ARRAY:
            case SHORT_ARRAY:
            case LONG_ARRAY:
            case FLOAT_ARRAY:
            case DOUBLE_ARRAY:
            case DATE_ARRAY:
                fieldDisplayProcessor = fieldValue -> DataFieldEncoding.encodeFieldValue(fieldName, fieldType, fieldValue, DATE_DISPLAY_FORMAT);
                break;

            default:
                fieldDisplayProcessor = null;
                break;
        }
    }

    /**
     * Called by hibernate after this data field is loaded.  Set the default field processor since it is a transient
     * field.
     *
     * @since 2.29
     */
    @PostLoad
    private void onLoad()
    {
        setDefaultFieldProcessor();
    }
}
