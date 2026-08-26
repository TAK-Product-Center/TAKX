package gov.takx.core.view.gui.maps.exporters.dataset;

import gov.takx.api.layer.IRaptorLayer;
import gov.takx.api.layer.IRaptorLayerManager;
import gov.takx.api.layer.LayerGroup;
import gov.takx.core.globe.layers.GlobeLayer;
import gov.takx.core.globe.tak.ImageryLayerManager;
import gov.takx.core.globe.tak.elevation.ElevationLayerManager;
import gov.takx.platform.cdi.InjectableContainerSupport;
import jakarta.annotation.Nonnull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.invoke.MethodHandles;
import java.util.List;
import java.util.Objects;

/**
 * Utilities for computing various percentages and strings for cache builder download use and for creating
 * cache wrappers for the various types of {@link IRaptorLayer}s.
 *
 * @since 1.43
 */
public class MapBuilderUtils
{
    private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    private static final InjectableContainerSupport BEAN_ACCESSOR = new InjectableContainerSupport();

    private MapBuilderUtils()
    {
    }

    /**
     * @return List of wrappers for all Raptor imagery layers
     * @since 2.37, existed previously, but method arguments changed
     */
    @Nonnull
    public static List<MapDataWrapper> getImageryWrappers()
    {
        return BEAN_ACCESSOR.getBean(ImageryLayerManager.class).getLayer().getLayers()
                .stream()
                .filter(l -> l instanceof GlobeLayer)
                .map(l -> (GlobeLayer) l)
                .map(MapDataWrapper::forImageGlobeLayer)
                .toList();
    }

    /**
     * @return List of wrappers for all elevation models
     * @since 2.37, existed previously, but method arguments changed
     */
    @Nonnull
    public static List<MapDataWrapper> getElevationWrappers()
    {
        // Get the elevation models from the Raptor layer manager
        IRaptorLayerManager raptorLayerManager = BEAN_ACCESSOR.getBean(IRaptorLayerManager.class);
        ElevationLayerManager elevationLayerManager = BEAN_ACCESSOR.getBean(ElevationLayerManager.class);
        return raptorLayerManager
                .getLayerList(LayerGroup.ELEVATION)
                .stream()
                .map(elevationLayerManager::getElevationSource)
                .filter(Objects::nonNull)
                .map(MapDataWrapper::forElevationSource)
                .toList();
    }

    public static String getByteString(long bytes)
    {
        int unit = 1024;
        if (bytes < unit) return "";

        int exp = (int) (Math.log(bytes) / Math.log(unit));
        final char prefix = "KMGTPE".charAt(exp - 1);
        return String.format("<html>%.1f %cB</html>", bytes / Math.pow(unit, exp), prefix);
    }
}
