package gov.takx.api.layer;

import gov.takx.api.annotation.InjectableService;
import gov.takx.api.annotation.NoPluginImplementations;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

/**
 * Contract for the service that allows users to add layers to the globe from layer configurations. A {@link LayerConfiguration} can be constructed
 * from various builders in the {@link gov.takx.api.layer.builder} package.
 *
 * @since 5.4
 */
@InjectableService
@NoPluginImplementations
public interface ILayerService
{
    /**
     * Add a layer to the maps described by the provided {@link LayerConfiguration} and return an {@link IRaptorLayer} to
     * control the added layer.
     * @param configuration the layer configuration
     * @return the raptor layer that represents the added layer
     */
    @Nonnull
    IRaptorLayer addLayer(@Nonnull LayerConfiguration configuration);

    /**
     * @param layerName Name/key of layer to retrieve
     * @return The layer with the given name (unique key), or {@code null} if no such named layer exists
     * @since 5.7
     */
    @Nullable
    IRaptorLayer getLayer(@Nonnull String layerName);

    /**
     * Remove the layer with the given name/key.  If there is no such layer, nothing is done.
     *
     * @param layerName Name/key of layer to remove
     * @since 5.7
     */
    void removeLayer(@Nonnull String layerName);

    /**
     * Remove the given layer.
     *
     * @param layer Layer to remove
     * @since 5.7
     */
    void removeLayer(@Nonnull IRaptorLayer layer);
}
