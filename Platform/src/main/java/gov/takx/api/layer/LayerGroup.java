package gov.takx.api.layer;

/**
 * Enum representing layer groups in TAKX.
 * <p/>
 * Moved out of IRaptorLayer to here so that the class can be looked up by name.
 *
 */
public enum LayerGroup
{
    // The order here defines the stacking order on the globe, so be careful where you put things

    EXTRAS("Extra Layers"),                         // stars and anything behind the earth
    IMAGERY("Imagery Layers"),                      // maps and imagery
    /** @since 5.0 */
    MODELS("3D Model Layers"),
    ELEVATION("Elevation Models"),                  // elevation models
    FEATURES("Feature Layers"),                     // features and place names
    SURFACE_IMAGERY("Surface Imagery Layers"),      // surface imagery
    SHAPE_FILES("Shape File Layers"),               // Imported ESRI shape files
    KML_OVERLAY("KML Overlay"),                     // imported KML file ground overlay layers
    UNLISTED_OVERLAY("Unlisted Overlay"),           // Overlay layers which are not to be listed in the Layer Panel

    /**
     * Tools/overlays that want to exclude from the layer list and KML export.
     * @since 3.4
     */
    INTERNAL_TOOLS("Internal Tools"),
    SURFACE_STRUCTURES("Surface Structure Layers"), // Surface Structure layers
    PLACEMARK_OVERLAY("Placemark Overlay"),         // Placemark Overlay (and Search Marker Overlay) layers
    ANALYTIC_SURFACES("Analytic Surface Layers"),   // Analytic surfaces above the terrain
    MAP_DATA("Map Data"),                           // icons, annotations
    TOOLS("Tool Layers"),                           // fixed above the globe
    WSS_SHAPE_GROUPS("WSS Shape Groups"),           // Shape group layers (for interacting with Web Shape Servers)
    TOP("TOP");                                     // special layer that always has to be on top for dragging icons

    private final String name;

    /**
     * Constructor.
     *
     * @param name The display name
     */
    LayerGroup(String name)
    {
        this.name = name;
    }

    /**
     * @return The display name for this group
     */
    public String getName()
    {
        return name;
    }
}
