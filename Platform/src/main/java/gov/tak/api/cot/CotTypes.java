package gov.tak.api.cot;

import gov.tak.api.comms.takserver.mission.data.Mission;
import jakarta.annotation.Nonnull;

/**
 * Constants and utility methods relating to Cursor on Target type strings.
 *
 * @since 3.0
 */
public class CotTypes
{
    /**
     * The CoT type designating an empty/unknown type.
     */
    public static final String EMPTY = "no-defined-type";

    /**
     * The CoT type designating our "self" marker.
     */
    public static final String SELF = "self";

    /**
     * Type prefix for an atom.
     *
     * @since 5.1
     */
    public static final String PREFIX_ATOM = "a-";

    /**
     * Type prefix indicating a unit with affiliation: FRIENDLY
     */
    public static final String PREFIX_FRIENDLY = "a-f";

    /**
     * Type prefix indicating a unit with affiliation: ASSUMED FRIEND
     */
    public static final String PREFIX_ASSUMED_FRIENDLY = "a-a";

    /**
     * Type prefix indicating a unit with affiliation: NEUTRAL
     */
    public static final String PREFIX_NEUTRAL = "a-n";

    /**
     * Type prefix indicating a unit with affiliation: UNKNOWN
     */
    public static final String PREFIX_UNKNOWN = "a-u";

    /**
     * Type prefix indicating a unit with affiliation: PENDING
     */
    public static final String PREFIX_PENDING = "a-p";

    /**
     * Type prefix indicating a unit with affiliation: HOSTILE
     */
    public static final String PREFIX_HOSTILE = "a-h";

    /**
     * Type prefix indicating a unit with affiliation: JOKER
     */
    public static final String PREFIX_JOKER = "a-j";

    /**
     * Type prefix indicating a unit with affiliation: FAKER
     */
    public static final String PREFIX_FAKER = "a-k";

    /**
     * Type prefix indicating a unit with affiliation: SUSPECT
     */
    public static final String PREFIX_SUSPECT = "a-s";

    /**
     * Type prefix indicating a file transfer
     */
    public static final String PREFIX_FILE_TRANSFER = "b-f-t";

    /**
     * Type prefix indicating a Chat or free text
     */
    public static final String PREFIX_FREE_TEXT = "b-t-f";

    /**
     * Type indicating delivered Chat or free text
     *
     * @since 3.4
     */
    public static final String FREE_TEXT_DELIVERED = PREFIX_FREE_TEXT + "-d";

    /**
     * Type indicating read Chat or free text
     *
     * @since 3.4
     */
    public static final String FREE_TEXT_READ = PREFIX_FREE_TEXT + "-r";

    /**
     * Type prefix indicating a "Detection"
     */
    public static final String PREFIX_DETECT = "b-d";

    /**
     * Type prefix indicating an "Emergency"
     */
    public static final String PREFIX_EMERGENCY = "b-a";

    /**
     * Type suffix indicating a generic ground unit
     */
    public static final String SUFFIX_GROUND = "-G";

    /**
     * Type suffix indicating a generic ground combat unit
     */
    public static final String SUFFIX_GROUND_COMBAT_UNIT = "-G-U-C";

    /**
     * Type indicating a control point (e.g. a vertex on a route).
     */
    public static final String CONTROL_POINT = "b-m-p-c";

    /**
     * Type indicating a file transfer request.
     */
    public static final String FILE_TRANSFER_REQUEST = "b-f-t-r";

    /**
     * Type indicating to cancel an emergency alert.
     * @since 5.7
     */
    public static final String EMERGENCY_CANCEL = PREFIX_EMERGENCY + "-o-can";

    /**
     * Type indicating an evacuation point (helo LZ).
     * @since 3.1
     */
    public static final String EVAC_POINT = "b-r-f-h-c";

    /**
     * Common prefix for mission related CoT events.
     * @since 5.7
     */
    public static final String MISSION_PREFIX = "t-x-m-";
    /**
     * Type indicating a mission import request.
     * @since 5.0
     */
    public static final String MISSION = MISSION_PREFIX + "i";
    /**
     * Type indicating a mission was created.
     * @since 5.7
     */
    public static final String MISSION_CREATED = MISSION_PREFIX + "n";
    /**
     * Type indicating a mission was deleted.
     * @since 5.7
     */
    public static final String MISSION_DELETED = MISSION_PREFIX + "d";
    /**
     * Type indicating a user's role within a mission was changed.
     * @since 5.7
     */
    public static final String MISSION_ROLE_CHANGED = MISSION_PREFIX + "r";
    /**
     * Type indicating a changed value in a mission's {@link Mission#uids()} or {@link Mission#contents()}.
     * @since 5.7
     */
    public static final String MISSION_CHANGED_CONTENT = MISSION_PREFIX + "c";
    /**
     * Type indicating a new entry in a mission's {@link Mission#logs()}.
     * @since 5.7
     */
    public static final String MISSION_CHANGED_LOG = MISSION_CHANGED_CONTENT + "-l";
    /**
     * Type indicating changed value in a mission's {@link Mission#keywords()}.
     * @since 5.7
     */
    public static final String MISSION_CHANGED_KEYWORD = MISSION_CHANGED_CONTENT + "-k";
    /**
     * Unsupported.
     * @since 5.7
     */
    public static final String MISSION_CHANGED_KEYWORD_ON_UID = MISSION_CHANGED_CONTENT + "-k-u";
    /**
     * Unsupported.
     * @since 5.7
     */
    public static final String MISSION_CHANGED_KEYWORD_ON_RESOURCE = MISSION_CHANGED_CONTENT + "-k-c";
    /**
     * Type indicating a changed value for a mission's {@link Mission#description()}.
     * @since 5.7
     */
    public static final String MISSION_CHANGED_METADATA = MISSION_CHANGED_CONTENT + "-m";
    /**
     * Type indicating a changed value in a mission's {@link Mission#externalData()}.
     * @since 5.7
     */
    public static final String MISSION_CHANGED_EXTERNAL_DATA = MISSION_CHANGED_CONTENT + "-e";
    /**
     * Type indicating a changed value in a mission's {@link Mission#mapLayers()}.
     * @since 5.7
     */
    public static final String MISSION_CHANGED_MISSION_LAYER = MISSION_CHANGED_CONTENT + "-h";

    /**
     * Type indicating a notification that a user's TAK Server group changed.
     * @since 5.0
     */
    public static final String GROUP_CHANGE = "t-x-g-c";

    /**
     * Type indicating a Route.
     */
    public static final String ROUTE = "b-m-r";

    /**
     * Type indicating this is a video/ConnectionEntry
     * @since 5.3
     */
    public static final String VIDEO = "b-i-v";

    /**
     * Type indicating an SPI.
     * @since 5.0
     */
    public static final String SENSOR_POINT_OF_INTEREST = "b-m-p-s-p-i";

    /**
     * Type indicating manually placed sensor (via point dropper).
     * @since 5.2
     */
    public static final String MANUALLY_PLACED_SENSOR = "b-m-p-s-m";

    /**
     * Type indicating a range and bearing arrow shape.
     */
    public static final String SHAPE_ARROW = "u-rb-a";

    /**
     * Type indicating a Rectangle shape.
     */
    public static final String SHAPE_RECTANGLE = "u-d-r";

    /**
     * Type indicating a multi-polyline shape.
     */
    public static final String SHAPE_MULTI_POLYLINE = "u-d-f-m";

    /**
     * Type indicating a Polyline/Polygon shape.
     */
    public static final String SHAPE_POLYSHAPE = "u-d-f";

    /**
     * Type indicating an Ellipse shape (including concentric ellipses).
     */
    public static final String SHAPE_ELLIPSE = "u-d-c-e";

    /**
     * Type indicating a Circle shape (including concentric circles).
     */
    public static final String SHAPE_CIRCLE = "u-d-c-c";

    /**
     * Type indicating a range and bearing shape (i.e. range rings).
     */
    public static final String SHAPE_RANGE_CIRCLE = "u-r-b-c-c";

    /**
     * Type indicating a range and bearing Bullseye
     * @since 5.5
     */
    public static final String SHAPE_BULLSEYE = "u-r-b-bullseye";

    /**
     * Type indicating a Spot Marker.
     * @since 3.1
     */
    public static final String SPOT_MARKER = "b-m-p-s-m";

    /**
     * Type indicating a task/request to delete an item.
     */
    public static final String TASK_DELETE = "t-x-d-d";

    /**
     * Type indicating a task/request to destroy a target.
     */
    public static final String TASK_DESTROY = "t-k-d";

    /**
     * Type indicating a Vehicle Model.
     * @since 5.8
     */
    public static final String VEHICLE_MODEL = "u-d-v-m";

    /**
     * Viewshed Marker type.
     * @since 5.8
     */
    public static final String VIEWSHED_MARKER_TYPE = "vsd-marker";

    /**
     * Type indicating a waypoint (e.g. a named vertex on a route).
     */
    public static final String WAYPOINT = "b-m-p-w";

    /**
     * Type indicating an unknown ground item.
     */
    public static final String UNKNOWN_GROUND = PREFIX_UNKNOWN + SUFFIX_GROUND;

    /**
     * Type indicating a friendly ground combat unit.
     */
    public static final String FRIENDLY_GROUND_COMBAT_UNIT = PREFIX_FRIENDLY + SUFFIX_GROUND_COMBAT_UNIT;

    /**
     * Determine if a CoT type string indicates a unit with a friendly affiliation.
     *
     * @param type Type to test
     * @return {@code true} if type is a friendly unit
     * @see #PREFIX_FRIENDLY
     * @see #PREFIX_ASSUMED_FRIENDLY
     */
    public static boolean isFriendly(@Nonnull String type)
    {
        return type.startsWith(PREFIX_FRIENDLY) || type.startsWith(PREFIX_ASSUMED_FRIENDLY);
    }

    /**
     * Determine if a CoT type string indicates a unit with a hostile affiliation.
     *
     * @param type Type to test
     * @return {@code true} if type is a hostile unit
     * @see #PREFIX_HOSTILE
     * @see #PREFIX_FAKER
     * @see #PREFIX_JOKER
     * @see #PREFIX_SUSPECT
     */
    public static boolean isHostile(@Nonnull String type)
    {
        return type.startsWith(PREFIX_HOSTILE) || type.startsWith(PREFIX_FAKER)
                || type.startsWith(PREFIX_JOKER) || type.startsWith(PREFIX_SUSPECT);
    }

    /**
     * Determine if a CoT type string indicates a unit with a neutral affiliation.
     *
     * @param type Type to test
     * @return {@code true} if type is a neutral unit
     * @see #PREFIX_NEUTRAL
     */
    public static boolean isNeutral(@Nonnull String type)
    {
        return type.startsWith(PREFIX_NEUTRAL);
    }

    /**
     * Determine if a CoT type string indicates a unit with an unknown affiliation.
     *
     * @param type Type to test
     * @return {@code true} if type is an unknown unit
     * @see #PREFIX_UNKNOWN
     * @see #PREFIX_PENDING
     */
    public static boolean isUnknown(@Nonnull String type)
    {
        return type.startsWith(PREFIX_UNKNOWN) || type.startsWith(PREFIX_PENDING);
    }

    private CotTypes()
    {
        /* no instances */
    }
}
