package gov.takx.api.layer;

import gov.tak.api.annotation.DeprecatedApi;
import gov.takx.api.annotation.InjectableService;
import gov.takx.api.annotation.NoPluginImplementations;
import jakarta.annotation.Nonnull;

import java.util.List;

/**
 * Delegate to provide plugins the ability to access available layers and to register themselves as listeners for
 * notifications of changes made to the layer manager.
 * <p/>
 * Plugins which add listeners should ensure that they properly unregister as listeners, or they risk creating bugs.
 * <p/>
 * Note that the lifecycle of this delegate is tied to the TAKX project lifecycle, so plugins accessing this delegate
 * must also ensure that they do not hold references to layers beyond the scope of a project session.
 *
 * @since 1.41
 */
@InjectableService
@NoPluginImplementations
public interface ILayerManagerDelegate
{
    /**
     * Get an {@link IRaptorLayer} given the layer key.
     *
     * @param key Key of layer to retrieve
     * @return Layer for that key
     * @since 1.41
     */
    IRaptorLayer getLayer(String key);

    /**
     * Get the count of all the layers that are in the given layer group.
     *
     * @param layerGroup Layer group of interest
     * @return count of all the layers in that group
     * @since 1.41
     */
    int getLayerCount(LayerGroup layerGroup);

    /**
     * Get all the layers that are in the given layer group.
     *
     * @param layerGroup Layer group of interest
     * @return List of all layers in that group
     * @since 1.41
     */
    @Nonnull
    List<IRaptorLayer> getLayerList(LayerGroup layerGroup);

    /**
     * Get all (non-elevation model) layers.
     *
     * @return all (non-elevation model) layers
     * @since 1.41
     */
    List<IRaptorLayer> getLayers();

    /**
     * Get all the elevation model layers.
     *
     * @return all the elevation model layers
     * @since 1.41
     * @deprecated use {@link #getLayers()} and check {@link IRaptorLayer#getLayerGroup()} for {@link LayerGroup#ELEVATION}.
     */
    @DeprecatedApi(since = "5.6", forRemoval = true, removeAt = "5.9")
    @Deprecated(since = "5.6", forRemoval = true)
    List<IRaptorLayer> getModelLayers();

    /**
     * Add a layer manager listener.
     *
     * @param raptorLayerManagerListener the layer manager listener to add
     * @since 1.41
     */
    void addRaptorLayerManagerListener(IRaptorLayerManagerListener raptorLayerManagerListener);

    /**
     * Remove a layer manager listener.
     *
     * @param raptorLayerManagerListener the layer manager listener to remove
     * @since 1.41
     */
    void removeRaptorLayerManagerListener(IRaptorLayerManagerListener raptorLayerManagerListener);
}
