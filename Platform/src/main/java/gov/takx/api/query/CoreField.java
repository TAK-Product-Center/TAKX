package gov.takx.api.query;

import gov.takx.api.messages.IDataType;
import gov.takx.api.messages.IRaptorDataMessage;
import gov.takx.api.persist.CompositeType;
import gov.takx.api.persist.UUIDCompositeType;
import gov.takx.commons.constants.CommServiceType;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Enum indicating a <em>core</em> field in an {@link IRaptorDataMessage} for use in a query constraint. Core fields are
 * direct attributes of the message proper, as opposed to structure fields that are defined by the plugin/device
 * developer. The javadoc for each field indicates the underlying database type. When passing in comparison values to
 * the {@link IQueryConstraints}, you must use an appropriately typed object.
 * <p>
 * <strong>Note: these enum names must match the column names used in persistence.</strong>
 *
 * @see IQueryConstraints#addFieldConstraint(CoreField, ComparisonType, Object)
 */
public enum CoreField
{
    /**
     * Latitude in decimal degrees.
     * <p>
     * DB type: double
     */
    latitude(IDataType.DOUBLE),

    /**
     * Longitude in decimal degrees.
     * <p>
     * DB type: double
     */
    longitude(IDataType.DOUBLE),

    /**
     * Elevation in meters.
     * <p>
     * DB type: double
     */
    elevation(IDataType.DOUBLE),

    /**
     * Device time in milliseconds since epoch.
     * <p>
     * DB type: long (millis since epoch)
     */
    time(IDataType.LONG),

    /**
     * Time the message is received in TAKX in milliseconds since epoch.
     * <p>
     * DB type: long (millis since epoch)
     */
    receiveTime(IDataType.LONG),

    /**
     * Relevant time (device time if set; otherwise receive time) in milliseconds since epoch.
     * <p>
     * DB type: Date; when returned in a field level query, type: long (millis since epoch)
     */
    relevantTime(IDataType.LONG),

    /**
     * The heading in degrees, with values from 0 to 360, where 0 is north.
     * <p>
     * DB type: double
     */
    heading(IDataType.DOUBLE),

    /**
     * The pitch in degrees, with values from -180 to 180, where positive values indicate up.
     * <p>
     * DB type: double
     */
    pitch(IDataType.DOUBLE),

    /**
     * The roll in degrees, with values from -180 to 180, where positive values indicate right wing down.
     * <p>
     * DB type: double
     */
    roll(IDataType.DOUBLE),

    /**
     * Speed in meters/sec.
     * <p>
     * DB type: double
     */
    speed(IDataType.DOUBLE),

    /**
     * DB type: String
     */
    sourceName(IDataType.STRING),

    /**
     * DB type: {@link CommServiceType}, in DB field-level queries an Integer
     */
    sourceType(IDataType.INT),

    /**
     * DB type: boolean
     */
    hasMotion(IDataType.BOOLEAN),

    /**
     * DB type: String
     * <p>
     * NOTE: You can not use this field in a field-level query or constraint, it is not a direct field on the RDM
     */
    family(IDataType.STRING),

    /**
     * DB type: String
     * <p>
     * NOTE: You can not use this field in a field-level query or constraint, it is not a direct field on the RDM
     */
    type(IDataType.STRING),

    /**
     * DB type: String
     * <p>
     * NOTE: You can not use this field in a field-level query or constraint, it is not a direct field on the RDM
     */
    unitId(IDataType.STRING),

    /**
     * RDM Id, primary key, DB type: Integer
     */
    id(IDataType.INT),

    /**
     * DB type: boolean
     *
     * @since 1.46
     */
    hasLocation(IDataType.BOOLEAN),

    /**
     * DB type: boolean
     *
     * @since 1.46
     */
    hasElevation(IDataType.BOOLEAN),

    /**
     * DB type: boolean
     *
     * @since 1.46
     */
    hasHeading(IDataType.BOOLEAN),

    /**
     * DB type: boolean
     *
     * @since 1.46
     */
    has3dOrientation(IDataType.BOOLEAN),

    /**
     * DB type: boolean
     *
     * @since 1.46
     */
    hasSpeed(IDataType.BOOLEAN),

    /**
     * DB type: boolean
     *
     * @since 1.46
     */
    hasTime(IDataType.BOOLEAN),

    /**
     * DB type: boolean
     *
     * @since 1.46
     */
    isManualLocation(IDataType.BOOLEAN),

    /**
     * DB type: boolean
     *
     * @since 1.46
     */
    isRenderable(IDataType.BOOLEAN),

    /**
     * DB type: Integer (may be {@code null})
     *
     * @since 1.46
     */
    trackStyleId(IDataType.INT),

    /**
     * DB type: Integer (may be {@code null})
     *
     * @since 1.46
     */
    overrideTrackStyleId(IDataType.INT),

    /**
     * DB type: Integer (may be {@code null})
     *
     * @since 2.0
     */
    uuid(IDataType.UUID, new UUIDCompositeType("uuid")),

    /**
     * DB type: integer (not {@code null})
     *
     * @since 3.0
     */
    mapObject_id(IDataType.INT),

    /**
     * DB type: boolean
     *
     * @since 2.22
     */
    isImported(IDataType.BOOLEAN),

    /**
     * DB type: integer (not {@code null})
     *
     * @since 2.36
     */
    structure_id(IDataType.INT),

    // end of list
    ;

    /**
     * The {@link CoreFieldSelector} for this CoreField enum.
     */
    public final CoreFieldSelector selector = new CoreFieldSelector(this);

    private final int fieldType;
    private final CompositeType compositeType;

    /**
     * Simple constructor.
     */
    CoreField(int fieldType)
    {
        this(fieldType, null);
    }

    /**
     * Constructor for composite types.
     */
    CoreField(int fieldType, CompositeType compositeType)
    {
        this.fieldType = fieldType;
        this.compositeType = compositeType;
    }

    /**
     * @return The data type (defined in {@link IDataType}) for this field
     * @since 1.49
     */
    public int getFieldType()
    {
        return fieldType;
    }

    /**
     * @return {@code true} if this core field uses a composite data type
     */
    public boolean isCompositeType()
    {
        return compositeType != null;
    }

    /**
     * @return The composite type for this field, only valid if {@link #isCompositeType()} returns {@code true}
     */
    public CompositeType getCompositeType()
    {
        return compositeType;
    }

    /**
     * An array containing all the fields that can legally be used in a field constraint or a
     * field-level query with the QueryService.
     */
    public static final CoreField[] ALL_SELECTABLE_FIELDS = {
            latitude, longitude, elevation, time, receiveTime, relevantTime, heading, pitch, roll, speed,
            sourceName, sourceType, hasMotion, id,
            hasLocation, hasElevation, hasHeading, has3dOrientation, hasSpeed, hasTime, isManualLocation, isRenderable,
            trackStyleId, overrideTrackStyleId, uuid, mapObject_id, isImported
    };

    /**
     * A list of all the core field names mapped to lower case.
     *
     * @since 3.0
     */
    public static final List<String> CANONICAL_CORE_FIELD_NAMES = Arrays.stream(values())
            .map(cf -> cf.name().toLowerCase())
            .collect(Collectors.toList());

    /**
     * Get the CoreField for the given name, returning {@code null} if it is not a core field name.
     *
     * @param name Name of field to find
     * @return CoreField or {@code null} if not found
     * @since 3.0
     */
    public static CoreField forName(String name)
    {
        try
        {
            return valueOf(name);
        } catch (IllegalArgumentException e)
        {
            return null;
        }
    }

    /**
     * Determine if a given field name is equivalent to any core field name, ignoring case.
     *
     * @param fieldName Field name to test
     * @return {@code true} if the given field is equal to a core field name ignoring case
     * @since 3.0
     */
    public static boolean isCoreFieldName(String fieldName)
    {
        return CANONICAL_CORE_FIELD_NAMES.contains(fieldName.toLowerCase());
    }
}
