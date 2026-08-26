package gov.takx.api.globe;

/**
 * Constants for the names of some standard TAKX layers. Plugins should not create a layer with the same name as
 * one of the reserved layers.
 *
 * @since 1.3
 */
@SuppressWarnings("NewClassNamingConvention")
public interface ReservedLayers
{
    String COMPASS = "Compass";
    String NAVIGATION = "Navigation";
    String TERRAIN_PROFILER = "Terrain Profiler";
    String ICONS = "Icons";
    String PLAYBACK_ICONS = "Playback Icons";
    String ANNOTATIONS = "Annotations";
    String SHAPES = "Shapes";

    /**
     * @since 3.4
     */
    String TRACKS = "Tracks";
    String TRACK_COLOR_SCALE = "Track Color Scale";
    String PROJECT_LABEL = "Project Label";
    String LAYER_BOUNDS = "Layer Bounds";
    String ALARMS = "Alarms";
    String LAT_LON_GRID = "Lat Lon Grid";
    String FLAT_ANNOTATIONS = "Flat Annotations";
    String WIDGETS = "Widgets";

    /**
     * @since 5.2
     */
    String MGRS_GRID = "MGRS Grid";

    /** @since 5.6 */
    String GARS_GRID = "GARS Grid";
}
