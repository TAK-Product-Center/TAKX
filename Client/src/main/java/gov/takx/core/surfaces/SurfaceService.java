package gov.takx.core.surfaces;

import gov.nasa.worldwind.geom.LatLon;
import gov.nasa.worldwind.layers.Layer;
import gov.nasa.worldwind.layers.RenderableLayer;
import gov.takx.api.annotation.PseudoBean;
import gov.takx.api.layer.IRaptorLayer;
import gov.takx.api.layer.IRaptorLayerManager;
import gov.takx.api.layer.LayerGroup;
import gov.takx.api.surfaces.IAnalyticSurface;
import gov.takx.api.surfaces.IAnalyticSurfaceService;
import gov.takx.api.surfaces.IAnalyticSurfaceUtils;
import jakarta.inject.Inject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.invoke.MethodHandles;

/**
 * @since 1.4
 */
@SuppressWarnings("CdiManagedBeanInconsistencyInspection")
@PseudoBean
public class SurfaceService implements IAnalyticSurfaceService
{
    private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    private static final String LAYER_CLASS = RenderableLayer.class.getName();
    private static final String ICON_PATH = "resources/globe.png";

    @Inject
    private IRaptorLayerManager raptorLayerManager;

    private final String key;
    private int layerNumber = 1;    // For generating unique layer names

    /**
     * Construct a surface service using the given key to generate unique layer names.
     *
     * @param key Unique key to use for generating layer names.
     */
    public SurfaceService(String key)
    {
        this.key = key;
    }

    @Override
    public IAnalyticSurfaceUtils getAnalyticSurfaceUtils()
    {
        return new AnalyticSurfaceUtils();
    }

    @Override
    public IAnalyticSurface createAnalyticSurface(String displayName, LatLon minCorner, LatLon maxCorner,
                                                  double altitude, int width, int height,
                                                  Iterable<? extends IAnalyticSurface.GridPointAttributes> values,
                                                  boolean useDisplayList)
    {
        if ((width * height) > 100000)
        {
            logger.warn("Analytic Surface was designed to comfortably handle 100,000 data points.  A grid of {} x {} surpasses this value- be prepared for a radical slowdown in Globe performance!", width, height);
        }

        final AnalyticSurface surface = new AnalyticSurface(displayName, minCorner, maxCorner, altitude, width, height,
                values, useDisplayList);

        createLayersForSurface(surface);
        return surface;
    }

    @Override
    public IAnalyticSurface createAnalyticSurface(String displayName, LatLon minCorner, LatLon maxCorner,
                                                  double altitude, int width, int height,
                                                  Iterable<? extends IAnalyticSurface.GridPointAttributes> values)
    {
        return createAnalyticSurface(displayName, minCorner, maxCorner, altitude, width, height, values, false);
    }

    @Override
    public IAnalyticSurface createAnalyticSurface(String displayName, LatLon minCorner, LatLon maxCorner, double altitude, int width, int height)
    {
        return createAnalyticSurface(displayName, minCorner, maxCorner, altitude, width, height, null, false);
    }

    @Override
    public boolean deleteAnalyticSurface(IAnalyticSurface surface)
    {
        // Remove the raptor layer associated with the WW layer for this surface
        final IRaptorLayer raptorLayer = findRaptorLayer(((AnalyticSurface) surface).getClientLayer());
        if (raptorLayer == null)
        {
            return false;   // Don't know about this layer, so must already have been deleted
        }

        raptorLayerManager.removeLayer(raptorLayer, false);

        surface.deleteSurface();

        return true;
    }

    /**
     * Create the raptor/globe/WW layers for use by the analytic surface and then set the client layer on the surface
     * to the created WW layer.
     *
     * @param surface The analytic surface to process
     */
    private void createLayersForSurface(AnalyticSurface surface)
    {
        final String layerKey = key + "/" + layerNumber++;

        final IRaptorLayer raptorLayer = raptorLayerManager.addLayer(layerKey, surface.getDisplayName(),
                LAYER_CLASS, true, false, ICON_PATH, LayerGroup.ANALYTIC_SURFACES, null, null, false);

        // Get the WW layer and install that into the surface
        final Layer wwLayer = raptorLayer.getLayer();

        surface.setClientLayer(wwLayer);
        ((RenderableLayer) wwLayer).addRenderable(surface);
    }

    /**
     * Find the {@link IRaptorLayer} associated with the given WW layer.
     *
     * @param wwLayer WW Layer to search for
     * @return associated {@link IRaptorLayer}, or {@code null} if not found
     * @since 2.12
     */
    private IRaptorLayer findRaptorLayer(Layer wwLayer)
    {
        return raptorLayerManager.getLayers().stream()
                .filter(rl -> rl.getLayer() == wwLayer)
                .findFirst().orElse(null);
    }
}
