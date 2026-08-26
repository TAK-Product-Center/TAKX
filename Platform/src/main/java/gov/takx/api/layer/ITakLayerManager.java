package gov.takx.api.layer;

import com.atakmap.map.layer.Layer;
import gov.tak.api.engine.map.ILayer;
import gov.takx.api.annotation.InjectableService;
import gov.takx.api.annotation.NoPluginImplementations;
import gov.takx.api.globe.IGlobeDelegate;

/**
 * An interface that allows adding TAK layers to the running globes.
 * @since 5.1
 */
@InjectableService
@NoPluginImplementations
public interface ITakLayerManager
{
    /**
     * Add a {@link Layer} to the provided {@link IGlobeDelegate}.
     * @param globe the globe to add the layer to
     * @param layer the layer to add
     * @since 5.3
     */
    void addLayer(IGlobeDelegate globe, Layer layer);

    /**
     * Add an {@link ILayer} to the provided {@link IGlobeDelegate}.
     * @param globe the globe to add the layer to
     * @param layer the layer to add
     * @since 5.3
     */
    void addLayer(IGlobeDelegate globe, ILayer layer);

    /**
     * Add a shared {@link Layer} that will be added to all globes.
     * @param layer the shared layer to add
     */
    void addLayer(Layer layer);

    /**
     * Add a shared {@link ILayer} that will be added to all globes.
     * @param layer the shared layer to add
     */
    void addLayer(ILayer layer);

    /**
     * Remove a {@link Layer} from the provided {@link IGlobeDelegate}.
     * @param globe the globe to remove the layer from
     * @param layer the layer to remove
     * @since 5.3
     */
    void removeLayer(IGlobeDelegate globe, Layer layer);

    /**
     * Remove an {@link ILayer} from the provided {@link IGlobeDelegate}.
     * @param globe the globe to remove the layer from
     * @param layer the layer to remove
     * @since 5.3
     */
    void removeLayer(IGlobeDelegate globe, ILayer layer);

    /**
     * Remove a shared {@link Layer} from all globes.
     * @param layer the shared layer to remove
     */
    void removeLayer(Layer layer);

    /**
     * Remove a shared {@link ILayer} from all globes.
     * @param layer the shared layer to remove
     */
    void removeLayer(ILayer layer);
}
