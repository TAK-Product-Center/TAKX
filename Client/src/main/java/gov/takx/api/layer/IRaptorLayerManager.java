package gov.takx.api.layer;

import gov.nasa.worldwind.layers.Layer;
import gov.tak.api.annotation.DeprecatedApi;
import gov.takx.core.globe.layers.WWGlobeLayer;
import jakarta.annotation.Nonnull;

import java.util.List;

/**
 * Interface to Layer Manager
 *
 */
public interface IRaptorLayerManager extends ILayerManagerDelegate
{
    /**
     * Globe Id to use if the layer is not specific to a globe instance (i.e. sharable).
     *
     * @see #addLayer(gov.nasa.worldwind.layers.Layer, LayerGroup, int)
     */
    int ALL_GLOBES = IRaptorLayer.ALL_GLOBES;

    /**
     * Get a {@link IRaptorLayer} given the layer key.
     *
     * @param key Key of layer to retrieve
     * @return Layer for that key
     */
    @Override
    IRaptorLayer getLayer(String key);

    /**
     * Add a pre-constructed layer to the given layer group. It defaults to visible, not pickable, {@code null} iconPath,
     * {@code null} type, {@code null} restorable state string, and not permanent. The layer may be shared across globes
     * if it is one of the layer types known to be sharable (like WMS layers, Stars, etc.).
     *
     * @param layer      The WW layer to add
     * @param layerGroup Group to which the layer belongs
     * @return The new layer.
     * @see WWGlobeLayer
     */
    IRaptorLayer addLayer(Layer layer, LayerGroup layerGroup);

    /**
     * Add a pre-constructed layer to the given layer group. It defaults to visible, not pickable, {@code null} iconPath,
     * {@code null} type, {@code null} restorable state string, and not permanent.
     *
     * @param layer      The WW layer to add
     * @param layerGroup Group to which the layer belongs
     * @param globeId    Id of globe if this layer is specific to a globe (not sharable), {@link #ALL_GLOBES} may be
     *                   passed if the layer is sharable (the default)
     * @return The new layer.
     */
    IRaptorLayer addLayer(Layer layer, LayerGroup layerGroup, int globeId);

    /**
     * Add a pre-constructed TAK layer to the given layer group. It defaults to visible, not pickable, {@code null} iconPath,
     * {@code null} type, {@code null} restorable state string, and not permanent. The layer may be shared across globes
     * if it is one of the layer types known to be sharable (like WMS layers, Stars, etc.).
     *
     * @param layer      The TAK layer to add
     * @param layerGroup Group to which the layer belongs
     * @return The new layer.
     * @since 5.2
     */
    default IRaptorLayer addLayer(com.atakmap.map.layer.Layer layer, LayerGroup layerGroup)
    {
        return addLayer(layer, layerGroup, ALL_GLOBES);
    }

    /**
     * Add a pre-constructed TAK layer to the given layer group. It defaults to visible, not pickable, {@code null} iconPath,
     * {@code null} type, {@code null} restorable state string, and not permanent.
     *
     * @param layer      The TAK layer to add
     * @param layerGroup Group to which the layer belongs
     * @param globeId    Id of globe if this layer is specific to a globe (not sharable), {@link #ALL_GLOBES} may be
     *                   passed if the layer is sharable (the default)
     * @return The new layer.
     * @since 5.2
     */
    IRaptorLayer addLayer(com.atakmap.map.layer.Layer layer, LayerGroup layerGroup, int globeId);

    /**
     * Add a pre-constructed TAK layer with the given properties.
     *
     * @param layer               The TAK layer to add
     * @param visible             is the layer visible or not
     * @param pickable            is the layer pickable or not
     * @param iconPath            path to icon for the layer
     * @param layerGroup          group to which the layer belongs
     * @param type                type of layer
     * @param restorableXMLString restorable state XML
     * @param permanent           is the layer permanent or not
     * @param globeId             Id of globe if this layer is specific to a globe (not sharable), {@link #ALL_GLOBES} may be
     *                            passed if the layer is sharable (the default)
     * @return The new layer
     * @since 5.4
     */
    IRaptorLayer addLayer(com.atakmap.map.layer.Layer layer, boolean visible, boolean pickable, String iconPath,
                          LayerGroup layerGroup, String type, String restorableXMLString, boolean permanent, int globeId);

    /**
     * Add a layer with the given name and group. It defaults to visible, not pickable, {@code null} iconPath,
     * {@code null} type, {@code null} xml string, and not permanent.
     *
     * @param name       layer name
     * @param className  Implementation class name
     * @param layerGroup Group to which the layer belongs
     * @return The new layer.
     */
    IRaptorLayer addLayer(String name, String className, LayerGroup layerGroup);

    /**
     * Add a new layer with the given properties.
     *
     * @param name                The layer name
     * @param displayName         The display name (visible name used in the UI)
     * @param className           implementation class name
     * @param visible             is the layer visible or not
     * @param pickable            is the layer pickable or not
     * @param iconPath            path to icon for the layer
     * @param layerGroup          group to which the layer belongs
     * @param type                type of layer
     * @param restorableXMLString restorable state XML
     * @param permanent           is the layer permanent or not
     * @return The new layer
     */
    IRaptorLayer addLayer(String name, String displayName, String className, boolean visible, boolean pickable, String iconPath,
                          LayerGroup layerGroup, String type, String restorableXMLString, boolean permanent);

    /**
     * Add a new layer with the given properties.
     *
     * @param name                The layer name
     * @param displayName         The display name (visible name used in the UI)
     * @param className           implementation class name
     * @param visible             is the layer visible or not
     * @param pickable            is the layer pickable or not
     * @param iconPath            path to icon for the layer
     * @param layerGroup          group to which the layer belongs
     * @param type                type of layer
     * @param restorableXMLString restorable state XML
     * @param permanent           is the layer permanent or not
     * @param splitScale          is used by the globe to determine when to change resolutions
     * @param minActiveAltitude   used by the globe to determine at what min altitude the layer will be active
     * @param maxActiveAltitude   used by the globe to determine at what max altitude the layer will be active
     * @return The new layer
     */
    IRaptorLayer addLayer(String name, String displayName, String className, boolean visible, boolean pickable, String iconPath,
                          LayerGroup layerGroup, String type, String restorableXMLString, boolean permanent, double splitScale,
                          double minActiveAltitude, double maxActiveAltitude);

    /**
     * Add a layer with the given name and group as defined by the given configuration file.
     * It defaults to visible, not pickable, {@code null} iconPath, {@code null} type, {@code null} xml string, and not permanent.
     *
     * @param name           layer name
     * @param configFileName Path to the XML configuration file defining the layer
     * @param layerGroup     Group to which the layer belongs
     * @return The new layer.
     * @since 1.11
     */
    IRaptorLayer addLayerFromXML(String name, String configFileName, LayerGroup layerGroup);

    /**
     * Add a new layer defined by the given XML configuration file with the given properties.
     *
     * @param name                The layer name
     * @param displayName         The display name (visible name used in the UI)
     * @param configFileName      Path to the XML configuration file defining the layer
     * @param visible             is the layer visible or not
     * @param pickable            is the layer pickable or not
     * @param iconPath            path to icon for the layer
     * @param layerGroup          group to which the layer belongs
     * @param type                type of layer
     * @param restorableXMLString restorable state XML
     * @param permanent           is the layer permanent or not
     * @param splitScale          is used by the globe to determine when to change resolutions
     * @param minActiveAltitude   used by the globe to determine at what min altitude the layer will be active
     * @param maxActiveAltitude   used by the globe to determine at what max altitude the layer will be active
     * @return The new layer
     * @since 1.11
     */
    IRaptorLayer addLayerFromXML(String name, String displayName, String configFileName, boolean visible, boolean pickable, String iconPath,
                                 LayerGroup layerGroup, String type, String restorableXMLString, boolean permanent, double splitScale,
                                 double minActiveAltitude, double maxActiveAltitude);

    void removeLayer(IRaptorLayer layer, boolean confirm);

    void moveLayer(String key, int index);

    /**
     * Get all the layers that are in the given layer group.
     *
     * @param layerGroup Layer group of interest
     * @return List of all layers in that group
     */
    @Nonnull
    @Override
    List<IRaptorLayer> getLayerList(LayerGroup layerGroup);

    int getIndex(IRaptorLayer layer);

    @Override
    int getLayerCount(LayerGroup layerGroup);

    @Override
    void addRaptorLayerManagerListener(IRaptorLayerManagerListener raptorLayerManagerListener);

    @Override
    void removeRaptorLayerManagerListener(IRaptorLayerManagerListener raptorLayerManagerListener);

    @Override
    List<IRaptorLayer> getLayers();

    @DeprecatedApi(since = "5.6", forRemoval = true, removeAt = "5.9")
    @Deprecated(since = "5.6", forRemoval = true)
    @Override
    List<IRaptorLayer> getModelLayers();

    /**
     * Indicate that the given layer has been selected, {@code null} means no selection.
     *
     * @param layer layer to select, {@code null} to remove the selection
     */
    void selectLayer(IRaptorLayer layer);

    /**
     * Requests the removal of multiple layers.
     *
     * @param layers  the layers to remove
     * @param confirm whether to prompt the user with a confirm dialog
     * @since 1.14
     */
    void removeLayers(List<IRaptorLayer> layers, boolean confirm);
}
