package gov.takx.core.layer;

import com.atakmap.map.layer.Layer;
import com.atakmap.map.layer.raster.DatasetRasterLayer2;
import gov.takx.api.layer.ILayerService;
import gov.takx.api.layer.IRaptorLayer;
import gov.takx.api.layer.IRaptorLayerManager;
import gov.takx.api.layer.LayerConfiguration;
import gov.takx.core.globe.tak.ImageryLayerManager;
import gov.takx.core.globe.tak.ModelLayerManager;
import gov.takx.core.globe.tak.elevation.ElevationLayer;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

/**
 * An implementation of {@link ILayerService} that translates a {@link LayerConfiguration} into an {@link IRaptorLayer} and adds it to the globe
 * via {@link IRaptorLayerManager}.
 *
 * @since 5.4
 */
@ApplicationScoped
public class LayerService implements ILayerService
{
    private final IRaptorLayerManager raptorLayerManager;

    @Inject
    LayerService(IRaptorLayerManager raptorLayerManager)
    {
        this.raptorLayerManager = raptorLayerManager;
    }

    private String getClassName(LayerConfiguration.LayerClassType layerClassType)
    {
        return switch (layerClassType)
        {
            case Elevation, Wcs -> ElevationLayer.class.getName();
            case GenericLayer -> Layer.class.getName();
            case LocalImagery -> ImageryLayerManager.IMAGERY_LAYER_MANAGER_CLASS;
            case OnlineImagery -> DatasetRasterLayer2.class.getName();
            case Model, LegacyModel -> ModelLayerManager.SCENE_LAYER_CLASS;
        };
    }

    @Override
    @Nonnull
    public IRaptorLayer addLayer(@Nonnull LayerConfiguration configuration)
    {
        if (configuration.layer() != null)
        {
            IRaptorLayer raptorLayer = raptorLayerManager.addLayer(configuration.layer(), configuration.visible(), configuration.pickable(), configuration.iconPath(), configuration.layerGroup(),
                    configuration.type(), configuration.restorableXMLString(), configuration.permanent(), IRaptorLayerManager.ALL_GLOBES);
            if (configuration.displayName() != null) raptorLayer.setDisplayName(configuration.displayName());
            return raptorLayer;
        } else
        {
            return raptorLayerManager.addLayer(configuration.name(), configuration.displayName(), getClassName(configuration.layerClassType()), configuration.visible(),
                    configuration.pickable(), configuration.iconPath(), configuration.layerGroup(), configuration.type(), configuration.restorableXMLString(), configuration.permanent());
        }
    }

    @Nullable
    @Override
    public IRaptorLayer getLayer(@Nonnull String layerName)
    {
        return raptorLayerManager.getLayer(layerName);
    }

    @Override
    public void removeLayer(@Nonnull String layerName)
    {
        final var layer = getLayer(layerName);
        if (layer != null) raptorLayerManager.removeLayer(layer, false);
    }

    @Override
    public void removeLayer(@Nonnull IRaptorLayer layer)
    {
        raptorLayerManager.removeLayer(layer, false);
    }
}
