package gov.takx.api.globe.render;

import gov.takx.api.globe.IRenderingEngine;

/**
 * Constants relating to track rendering.
 * <p>
 * All constants from {@link IRenderingEngine} were moved here as of 2.29.
 *
 * @since 2.29
 */
public class RendererConstants
{
    /**
     * Value of one of the rendering mode selecting the Top Down renderer.
     */
    public static final String RENDER_MODE_TOP_DOWN = "TopDown";

    /**
     * Value of one of the rendering modes selecting the Spatial (3D) renderer.
     */
    public static final String RENDER_MODE_SPATIAL = "Spatial";

    /**
     * Value of a rendering mode where a empty placeholder renderer should be used (default for the secondary).
     * Should not be used for the Primary renderer at this time.
     */
    public static final String RENDER_MODE_NONE = "None";

    /**
     * Constant indicating that a high number of tracks should be displayed
     * before applying grouping policies. The specific number of tracks allowed
     * at this level is specified in the ProgramProperties TrackDetailLevels node.
     */
    public static final String HIGH_TRACK_DETAIL_LEVEL = "high";

    /**
     * Constant indicating that a mid-range number of tracks should be displayed
     * before applying grouping policies. The specific number of tracks allowed
     * at this level is specified in the ProgramProperties TrackDetailLevels node.
     */
    public static final String MEDIUM_TRACK_DETAIL_LEVEL = "medium";

    /**
     * Constant indicating that a low number of tracks should be displayed
     * before applying grouping policies. The specific number of tracks allowed
     * at this level is specified in the ProgramProperties TrackDetailLevels node.
     */
    public static final String LOW_TRACK_DETAIL_LEVEL = "low";

    /**
     * Maximum number of tracks to be displayed before applying grouping logic.
     * Fallback value in case there are problems reading the program preference
     * "low" value.
     */
    public static final int DEFAULT_HIGH_DETAIL_VISIBLE_TRACKS = 40000;

    /**
     * Maximum number of tracks to be displayed before applying grouping logic.
     * Fallback value in case there are problems reading the program preference
     * "low" value.
     */
    public static final int DEFAULT_MEDIUM_DETAIL_VISIBLE_TRACKS = 20000;

    /**
     * Maximum number of tracks to be displayed before applying grouping logic.
     * Fallback value in case there are problems reading the program preference
     * "low" value.
     */
    public static final int DEFAULT_LOW_DETAIL_VISIBLE_TRACKS = 2000;

    /**
     * Constant to use for an "unspecified" Z-order for map object rendering.
     */
    public static final int NO_Z_ORDER = -1;
}
