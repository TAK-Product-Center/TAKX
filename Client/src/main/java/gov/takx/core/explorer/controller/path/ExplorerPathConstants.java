package gov.takx.core.explorer.controller.path;

import java.util.List;

/**
 * Constants for explorable paths within an explorer model.
 * @since 5.3
 */
public class ExplorerPathConstants
{
    private static final String ALERTS_STRING = "Alerts";
    private static final String ANNOTATIONS_STRING = "Annotations";
    private static final String APPS_STRING = "Apps";
    private static final String COT2525_STRING = "CoT 2525";
    private static final String CONTACTS_STRING = "Contacts";
    private static final String DATA_MONITORS_STRING = "Data Monitors";
    private static final String DEVICES_STRING = "Devices";
    private static final String DRAWING_OBJECTS_STRING = "Drawing Objects";
    private static final String EMERGENCY_STRING = "Emergency";
    private static final String FRIENDLY_STRING = "Friendly";
    private static final String FILE_OVERLAYS_STRING = "File Overlays";
    private static final String GEO_FENCES_STRING = "Geo Fences";
    private static final String GROUPS_STRING = "Groups";
    /** @since 5.8 */
    private static final String HASHTAGS_STRING = "Hashtags";
    private static final String HOSTILE_STRING = "Hostile";
    private static final String ICONS_STRING = "Icons";
    private static final String MARKERS_STRING = "Markers";
    private static final String MEDIA_STRING = "Media";
    private static final String NAVIGATION_STRING = "Navigation";
    private static final String NEUTRAL_STRING = "Neutral";
    private static final String OTHER_STRING = "Other";
    private static final String RANGE_AND_BEARING_STRING = "Range & Bearing";
    private static final String ROUTES_STRING = "Routes";
    private static final String SHAPES_STRING = "Shapes";
    private static final String TEAM_STRING = "Team";
    private static final String UNKNOWN_STRING = "Unknown";
    private static final String VEHICLE_MODELS_STRING = "Vehicle Models";

    /** Path for all alert categories. */
    public static final List<String> ALERTS_PATH = List.of(ALERTS_STRING);
    /** Path for data monitor alerts (also referred to as "alarms"). */
    public static final List<String> ALERTS_DATA_MONITORS_PATH = List.of(ALERTS_STRING, DATA_MONITORS_STRING);
    /** Path for geofence alerts. */
    public static final List<String> ALERTS_EMERGENCY_PATH = List.of(ALERTS_STRING, EMERGENCY_STRING);
    /** Path for geofence alerts. */
    public static final List<String> ALERTS_GEO_FENCES_PATH = List.of(ALERTS_STRING, GEO_FENCES_STRING);
    /** Path for all annotations. */
    public static final List<String> ANNOTATIONS_PATH = List.of(ANNOTATIONS_STRING);
    /** Path for instances of {@link gov.takx.api.explorer.IAppExplorable}. */
    public static final List<String> APPS_PATH = List.of(APPS_STRING);
    /**
     * Path for 2525C Friendly markers.
     * @since 5.3 Renamed from COT2525C_FRIENDLY_PATH in 5.8
     * */
    public static final List<String> COT2525_FRIENDLY_PATH = List.of(MARKERS_STRING, COT2525_STRING, FRIENDLY_STRING);
    /**
     * Path for 2525C Hostile markers.
     * @since 5.3 Renamed from COT2525_HOSTILE_PATH in 5.8
     * */
    public static final List<String> COT2525_HOSTILE_PATH = List.of(MARKERS_STRING, COT2525_STRING, HOSTILE_STRING);
    /**
     * Path for 2525C Neutral markers.
     * @since 5.3 Renamed from COT2525C_NEUTRAL_PATH in 5.8
     * */
    public static final List<String> COT2525_NEUTRAL_PATH = List.of(MARKERS_STRING, COT2525_STRING, NEUTRAL_STRING);
    /**
     * Path for 2525C Unknown markers.
     * @since 5.3 Renamed from COT2525C_UNKNOWN_PATH in 5.8
     * */
    public static final List<String> COT2525_UNKNOWN_PATH = List.of(MARKERS_STRING, COT2525_STRING, UNKNOWN_STRING);
    /**
     * Path for 2525C markers of other types.
     * @since 5.3 Renamed from COT2525C_OTHER_PATH in 5.8
     * */
    public static final List<String> COT2525_OTHER_PATH = List.of(MARKERS_STRING, COT2525_STRING, OTHER_STRING);
    /** Path for all Devices. */
    public static final List<String> DEVICES_PATH = List.of(DEVICES_STRING);
    /**
     * Path for File Overlays.
     * @since 5.5
     */
    public static final List<String> FILE_OVERLAYS_PATH = List.of(FILE_OVERLAYS_STRING);
    /** Path for all group types. */
    public static final List<String> GROUPS_PATH = List.of(GROUPS_STRING);
    /**
     * Path for all hashtag groups.
     * @since 5.8
     */
    public static final List<String> HASHTAGS_PATH = List.of(HASHTAGS_STRING);
    /** Path for all Contacts. */
    public static final List<String> MARKERS_CONTACTS_PATH = List.of(MARKERS_STRING, CONTACTS_STRING);
    /** Path for Icon type markers. */
    public static final List<String> MARKERS_ICONS_PATH = List.of(MARKERS_STRING, ICONS_STRING);
    /** Path for all other Marker types. */
    public static final List<String> MARKERS_OTHER_PATH = List.of(MARKERS_STRING, OTHER_STRING);
    /**
     * Path for all Markers.
     * @since 5.7
     */
    public static final List<String> MARKERS_PATH = List.of(MARKERS_STRING);
    /**
     * Path for all Media objects.
     * @since 5.7
     */
    public static final List<String> MEDIA_PATH = List.of(MEDIA_STRING);
    /**
     * Path for all Navigation objects.
     * @since 5.7
     */
    public static final List<String> NAVIGATION_PATH = List.of(NAVIGATION_STRING);
    /** Path for range and bearing shape objects. */
    public static final List<String> RANGE_AND_BEARING_PATH = List.of(SHAPES_STRING, RANGE_AND_BEARING_STRING);
    /** Path for route objects. */
    public static final List<String> ROUTES_PATH = List.of(NAVIGATION_STRING, ROUTES_STRING);
    /** Path for all shapes. */
    public static final List<String> SHAPES_PATH = List.of(SHAPES_STRING);
    /** Path for drawing objects. */
    public static final List<String> SHAPES_DRAWING_OBJECTS_PATH = List.of(SHAPES_STRING, DRAWING_OBJECTS_STRING);
    /** Path for all Team nodes. */
    public static final List<String> TEAM_PATH = List.of(TEAM_STRING);

    /**
     * Path for all Vehicle Models.
     * @since 5.8
     */
    public static final List<String> VEHICLE_MODELS_PATH = List.of(MARKERS_STRING, VEHICLE_MODELS_STRING);
}