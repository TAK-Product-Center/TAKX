package gov.takx.api.shapes;

import gov.takx.api.mapobject.IMapObject;

/**
 * Constants defining the shape types and property names.
 *
 * @since 3.3
 */
public class ShapeProperties
{
    public static final String ALLOW_DRAG = IMapObject.ALLOW_DRAG;
    /** @since 5.6 */
    public static final String ALTITUDE_MODE = "ALTITUDE_MODE";
    /** @since 5.1 */
    public static final String ANCHORED = "ANCHORED";
    public static final String ANGLE = "ANGLE";
    public static final String BEARING = "BEARING";
    public static final String CENTER_ANCHOR = "CENTER_ANCHOR";
    public static final String EDITABLE = "EDITABLE";   // Is the shape editable property
    public static final String FILL_COLOR = "FILL_COLOR";
    /** @since 5.3 */
    public static final String GEOFENCE = "GEOFENCE";
    public static final String GROUND_REFERENCE = "GROUND_REFERENCE";
    public static final String HEIGHT = "HEIGHT";
    public static final String INTERVAL = "INTERVAL";
    public static final String LINE_COLOR = "LINE_COLOR";
    public static final String LINE_WIDTH = "LINE_WIDTH";
    /** @since 5.5 */
    public static final String LINE_STIPPLE_FACTOR = "LINE_STIPPLE_FACTOR";
    /** @since 5.5 */
    public static final String LINE_STIPPLE_PATTERN = "LINE_STIPPLE_PATTERN";
    public static final String LOCATION = IMapObject.LOCATION;
    public static final String LOCATIONS = "LOCATIONS";
    public static final String MAX_DISTANCE = "MAX_DISTANCE";
    public static final String MIN_DISTANCE = "MIN_DISTANCE";
    public static final String NAME = IMapObject.NAME;
    public static final String OPACITY = "OPACITY";
    public static final String ORIENTABLE = "ORIENTABLE"; // Whether to allow orientation edits to the object
    public static final String ORIENTATION = "orientation";
    public static final String PUBLISHED = "PUBLISHED"; // Is the shape published (a geofence)
    public static final String RADIUS = "RADIUS";
    public static final String RADIUS_ANCHOR = "RADIUS_ANCHOR";
    public static final String RANGE = "RANGE";
    public static final String RANGE_ANCHOR = "RANGE_ANCHOR";
    /** @since 3.4 */
    public static final String RANGE_COLORS = "RANGE_COLORS";
    public static final String RANGE_DISTANCES = "RANGE_DISTANCES";
    public static final String RANGE_LABELS = "RANGE_LABELS";
    public static final String RANGE_TYPE = "RANGE_TYPE";
    /** @since 3.5 */
    public static final String REMARKS = IMapObject.REMARKS;
    public static final String RING_COUNT = "RING_COUNT"; // Number of concentric rings for circles/ellipses
    public static final String SAMPLE_DISTANCE = "SAMPLE_DISTANCE";
    public static final String SEMI_MAJOR_AXIS = "SEMI_MAJOR_AXIS";
    public static final String SEMI_MINOR_AXIS = "SEMI_MINOR_AXIS";
    public static final String SHOW_EDGES = "SHOW_EDGES";
    public static final String SHOW_ORIENTATION = "SHOW_ORIENTATION";
    public static final String SHOW_RANGE_VALUES = "SHOW_RANGE_VALUES";
    public static final String START_ANCHOR = "START_ANCHOR";
    public static final String SWEEP = "SWEEP";
    /** @since 5.5.1 */
    public static final String TEXTURE_FILL_PARAMS = "TEXTURE_FILL_PARAMS";
    public static final String VERTICAL_HEIGHT = "VERTICAL_HEIGHT";
    public static final String VERTICAL_HEIGHT_ENABLED = "VERTICAL_HEIGHT_ENABLED";
    public static final String VISIBILITY = IMapObject.VISIBILITY;
    public static final String WIDTH = "WIDTH";
    public static final String HOLE_ADDED = "HOLE_ADDED";
    public static final String HOLE_REMOVED = "HOLE_REMOVED";

    private ShapeProperties()
    {
    }
}
