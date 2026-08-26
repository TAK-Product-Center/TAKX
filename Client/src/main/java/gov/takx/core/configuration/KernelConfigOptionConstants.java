package gov.takx.core.configuration;

import com.atakmap.util.ConfigOptions;

/**
 * Constants defining strings to be used with the various{@code ConfigOptions.setOption} methods.
 * 
 * @see ConfigOptions#setOption(String, String)
 * @see ConfigOptions#setOption(String, int)
 * @see ConfigOptions#setOption(String, long)
 * @see ConfigOptions#setOption(String, double)
 * @since 5.4
 */
public class KernelConfigOptionConstants
{
    /**
     * Specifies the font atlas path.
     */
    public static final String DEFAULT_FONT_ATLAS_URI = "default-font-atlas-uri";

    /**
     * Specifies the default font name for text rendering on the globe.
     */
    public static final String DEFAULT_FONT_NAME = "default-font-name";

    /**
     * Specifies the path to the EGM96 data file.
     */
    public static final String EGM96_DATA_FILE = "egm96-file";

    /**
     * Syncs the renderer frame rate to the display refresh, disabling vsync will allow the frame rate to
     * exceed the display refresh. Enable with integer value of 1.
     */
    public static final String ENABLE_VSYNC = "enable-vsync";

    /**
     * Specifies the path to the directory in which to cache imagery tiles.
     */
    public static final String IMAGERY_OFFLINE_CACHE_DIR = "imagery.offline-cache-dir";

    /**
     * Specifies the URI of the icon to display for 3D models.
     */
    public static final String MODEL_ICON_URI = "TAK.Engine.Model.default-icon";

    /**
     * Specifies the path to the directory in which to cache 3D models.
     */
    public static final String MODEL_OFFLINE_CACHE_DIR = "3dtiles.cache-dir";

    /**
     * Enables/disables display of on-screen diagnostics related to the globe. Enable with integer value of 1.
     */
    public static final String SHOW_RENDER_DIAGNOSTICS = "glmapview.render-diagnostics";

    /**
     * Enables/disables suppression of some excessive duplicate logging. Enable with integer value of 1.
     */
    public static final String SUPPRESS_DUPLICATE_LOGS = "logging.suppress-duplicate-logs";

    /**
     * Specifies the path to the World Magnetic Model file.
     */
    public static final String WMM_MODEL_FILE = "world-magnetic-model-file";
}
