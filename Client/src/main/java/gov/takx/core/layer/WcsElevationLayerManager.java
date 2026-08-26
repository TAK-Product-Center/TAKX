package gov.takx.core.layer;

import gov.tak.api.annotation.DeprecatedApi;
import gov.takx.api.layer.IRaptorLayer;
import gov.takx.api.layer.IRaptorLayerManager;
import gov.takx.api.layer.IWcsElevationLayerManager;
import gov.takx.api.layer.LayerGroup;
import gov.takx.core.globe.tak.elevation.ElevationLayer;
import gov.takx.core.globe.tak.elevation.WcsElevationLayerSpi;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

/**
 * WCS elevation layer manager service, supporting the creation of layers from WCS configuration XML.
 *
 * @since 5.3
 * @deprecated Use {@link gov.takx.api.layer.builder.WcsElevationLayerBuilder}
 */
@ApplicationScoped
@Deprecated(since = "5.4", forRemoval = true)
@DeprecatedApi(since = "5.4", forRemoval = true, removeAt = "5.7")
public class WcsElevationLayerManager implements IWcsElevationLayerManager
{
    private static final String LAYER_CLASS_NAME = ElevationLayer.class.getName();
    // XML element that identifies a custom map source
    private static final String MAP_SOURCE_ELEMENT = "<" + WcsElevationLayerSpi.CONFIGURATION_ELEMENT + ">";

    private final IRaptorLayerManager layerManager;

    @Inject
    WcsElevationLayerManager(IRaptorLayerManager layerManager)
    {
        this.layerManager = layerManager;
    }

    @Override
    public boolean isMapSourceXml(@Nonnull String string)
    {
        return string.contains(MAP_SOURCE_ELEMENT);
    }

    @Nullable
    @Override
    public IRaptorLayer addLayerFromConfigXml(@Nonnull String layerName, @Nonnull String configXml)
    {
        return layerManager.addLayer(layerName, layerName, LAYER_CLASS_NAME, true, false, null,
                LayerGroup.ELEVATION, null, configXml, false);
    }

    @Override
    public void removeLayer(@Nonnull String layerName)
    {
        IRaptorLayer layer = layerManager.getLayer(layerName);
        boolean isElevationLayer = layer != null && LAYER_CLASS_NAME.equals(layer.getClassName()) && layer.getLayerGroup() == LayerGroup.ELEVATION;
        if (isElevationLayer) layerManager.removeLayer(layer, false);
    }
}
