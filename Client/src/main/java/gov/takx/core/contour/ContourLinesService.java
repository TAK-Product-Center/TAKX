package gov.takx.core.contour;

import com.atakmap.coremap.maps.coords.GeoPoint;
import com.atakmap.map.layer.feature.geometry.Envelope;
import com.atakmap.map.layer.raster.DatasetDescriptor;
import gov.tak.api.contour.ContourFunction;
import gov.tak.api.contour.ContourLayerFactory;
import gov.tak.api.contour.ContourLayerParameters;
import gov.tak.api.contour.ContourLine;
import gov.tak.api.contour.ContourLineFactory;
import gov.tak.api.contour.ContourLineLayer;
import gov.tak.api.contour.ContourProgressListener;
import gov.tak.api.contour.ContourStyle;
import gov.tak.api.contour.DefaultContourLayerFactory;
import gov.tak.api.contour.DefaultContourLineFactory;
import gov.tak.api.contour.InvalidContourException;
import gov.tak.platform.contour.ContourGroupListener;
import gov.tak.platform.contour.ContourLineGroup;
import gov.tak.platform.contour.IContourLinesService;
import gov.takx.api.coordinatesystem.ILocation;
import gov.takx.api.globe.IApplicationGlobe;
import gov.takx.api.globe.IExtendedGlobeManager;
import gov.takx.api.layer.IRaptorLayer;
import gov.takx.api.layer.IRaptorLayerManager;
import gov.takx.api.layer.LayerGroup;
import gov.takx.api.platform.ICoreExecutorService;
import gov.takx.api.shapes.IRectangle;
import gov.takx.api.shapes.IShapeDrawingService;
import gov.takx.api.shapes.ShapeType;
import gov.takx.commons.lang.ExceptionUtils;
import gov.takx.core.view.gui.maptools.elevationtools.contour.ContourListPanel;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.invoke.MethodHandles;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Service implementation of {@link IContourLinesService}.
 *
 * @since 5.8
 */
@ApplicationScoped
public class ContourLinesService implements IContourLinesService
{
    private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    private static final int TIMEOUT_SEC = 120;

    private static final ContourLineFactory lineFactory = new DefaultContourLineFactory();
    private static final ContourLayerFactory layerFactory = new DefaultContourLayerFactory();

    private final List<ContourGroupListener> listeners = new CopyOnWriteArrayList<>();
    private final List<ContourLineGroup> contourLineGroups = new CopyOnWriteArrayList<>();
    private final ICoreExecutorService executorService;
    private final IShapeDrawingService shapeDrawingService;
    private final IExtendedGlobeManager globeManager;
    private final IRaptorLayerManager layerManager;

    @Inject
    public ContourLinesService(ICoreExecutorService executorService,
                               IShapeDrawingService shapeDrawingService,
                               IExtendedGlobeManager globeManager,
                               IRaptorLayerManager layerManager)
    {
        this.executorService = executorService;
        this.shapeDrawingService = shapeDrawingService;
        this.globeManager = globeManager;
        this.layerManager = layerManager;
    }

    @Nonnull
    @Override
    public List<ContourLineGroup> getContourGroups()
    {
        return Collections.unmodifiableList(contourLineGroups);
    }

    @Override
    public void removeContourGroup(@Nonnull ContourLineGroup group)
    {
        if (contourLineGroups.remove(group))
        {
            layerManager.removeLayer(group.getLayer(), false);
            ExceptionUtils.safeDoOperation(listeners, l -> l.onContourGroupRemoved(group));
        }
    }

    @Override
    public void addContourGroupListener(@Nonnull ContourGroupListener listener)
    {
        listeners.add(listener);
    }

    @Override
    public void removeContourGroupListener(@Nonnull ContourGroupListener listener)
    {
        listeners.remove(listener);
    }

    @Nonnull
    @Override
    public CompletableFuture<ContourLineGroup> initiateContourDrawing(@Nonnull ContourLineFactory.Generation generationParams,
                                                                      @Nonnull ContourLayerParameters layerParams,
                                                                      @Nullable AtomicBoolean cancellation,
                                                                      @Nullable ContourProgressListener contourGenerationListener,
                                                                      @Nullable ContourProgressListener contourLayerListener)
    {
        CompletableFuture<ContourLineGroup> result = new CompletableFuture<>();

        // Get a globe to draw on.
        IApplicationGlobe globe = globeManager.getLastFocusedGlobe();
        if (globe == null) globe = globeManager.getPrimaryGlobeDelegate();

        // Clear any previous draw task.
        shapeDrawingService.cancelDraw();

        // Draw an area and build contours filling that space.
        shapeDrawingService.requestDraw(globe, ShapeType.RECTANGLE, "Create a contour area").whenComplete((shape, error) -> {
            if (error != null)
            {
                logger.debug("Skipping contour creation, an error occurred drawing the area");
                result.completeExceptionally(new InvalidContourException(error, "Error drawing area"));
                return;
            } else if (shape == null)
            {
                logger.debug("Skipping contour creation, user cancelled area selection");
                result.completeExceptionally(new InvalidContourException(null, "User cancelled area selection"));
                return;
            }

            shapeDrawingService.removeDrawnShape(shape);
            if (shape instanceof IRectangle rectangle)
            {
                result.completeAsync(() -> buildFromAreaImpl(generationParams, layerParams, cancellation, contourGenerationListener, contourLayerListener, rectangle), executorService)
                        .orTimeout(TIMEOUT_SEC, TimeUnit.SECONDS);
            } else
            {
                result.completeExceptionally(new InvalidContourException(null, "Invalid shape provided"));
            }
        });

        // Sanity check
        return result.orTimeout(TIMEOUT_SEC, TimeUnit.SECONDS);
    }

    @Nonnull
    @Override
    public CompletableFuture<ContourLineGroup> buildFromArea(@Nonnull ContourLineFactory.Generation generationParams,
                                                             @Nonnull ContourLayerParameters layerParams,
                                                             @Nullable AtomicBoolean cancellation,
                                                             @Nullable ContourProgressListener contourGenerationListener,
                                                             @Nullable ContourProgressListener contourLayerListener,
                                                             @Nonnull IRectangle area)
    {
        return CompletableFuture.supplyAsync(() -> buildFromAreaImpl(generationParams, layerParams, cancellation, contourGenerationListener, contourLayerListener, area), executorService);
    }

    @Nonnull
    private ExtendedContourLineGroup buildFromAreaImpl(@Nonnull ContourLineFactory.Generation generationParams,
                                                       @Nonnull ContourLayerParameters layerParams,
                                                       @Nullable AtomicBoolean cancellation,
                                                       @Nullable ContourProgressListener contourGenerationListener,
                                                       @Nullable ContourProgressListener contourLayerListener,
                                                       @Nonnull IRectangle area)
    {
        Thread.currentThread().setName("Contour Lines Area Building");

        try
        {
            // Copy the parameters in case the user re-uses the parameters object between multiple calls.
            // This can lead to some style properties being shared between layers.
            layerParams = layerParams.copy();

            // In our copy, mutate the area to fill the given area before building the actual contours.
            fitArea(generationParams, area);
            return build(generationParams, layerParams, cancellation, contourGenerationListener, contourLayerListener);
        } catch (InvalidContourException ex)
        {
            // Calling context is CompletableFuture.supplyAsync so we wrap in the expected type
            // for a clean exceptional completion for any failures.
            throw new CompletionException(ex);
        }
    }

    /**
     * @param generationParams Contour generation parameters.
     * @param layerParams Contour layer style parameters.
     * @param cancellation Optional cancellation handle.
     * @param contourGenerationListener Optional progress listener for the contour generation stage.
     * @param contourLayerListener Optional progress listener for the contour layer creation stage.
     * @return Contour data wrapper.
     * @throws InvalidContourException When contour generation fails, or layer creation fails.
     */
    @Nonnull
    private ExtendedContourLineGroup build(@Nonnull ContourLineFactory.Generation generationParams,
                                           @Nonnull ContourLayerParameters layerParams,
                                           @Nullable AtomicBoolean cancellation,
                                           @Nullable ContourProgressListener contourGenerationListener,
                                           @Nullable ContourProgressListener contourLayerListener)
            throws InvalidContourException
    {
        // Build the contour lines.
        List<ContourLine> contourLines = lineFactory.buildContours(generationParams, cancellation, contourGenerationListener);
        if (cancellation != null && cancellation.get()) throw new InvalidContourException(null, "Contour line generation was cancelled");

        // Wrap the lines into a TAK layer, and validate we get a result back.
        layerParams.setContourLines(contourLines);
        ContourLineLayer layer = layerFactory.wrap(generationParams, layerParams, cancellation, contourLayerListener);
        if (cancellation != null && cancellation.get()) throw new InvalidContourException(null, "Contour line generation was cancelled");
        if (layer == null) throw new InvalidContourException(null, "Failed to create layer from contours");

        // Wrap the TAK layer into a TAKX layer and we're done.
        IRaptorLayer raptorLayer = layerManager.addLayer(layer, LayerGroup.UNLISTED_OVERLAY);
        raptorLayer.setVisible(true);

        // Package up into the group model and notify listeners
        ExtendedContourLineGroup group = new ExtendedContourLineGroup(contourLines, layer, raptorLayer);
        contourLineGroups.add(group);
        ExceptionUtils.safeDoOperation(listeners, l -> l.onContourGroupCreated(group));
        return group;
    }

    /**
     * Updates {@link ContourLineFactory.Generation#getElevationQueryParams()} to wrap the given area.
     * @param generation Contour generation parameters to update.
     * @param area Area to fit.
     */
    public static void fitArea(@Nonnull ContourLineFactory.Generation generation, @Nonnull IRectangle area)
    {
        ILocation minCorner = area.getMinCorner();
        ILocation maxCorner = area.getMaxCorner();

        double minLat = minCorner.getLatitude();
        double minLon = minCorner.getLongitude();
        double maxLat = maxCorner.getLatitude();
        double maxLon = maxCorner.getLongitude();

        GeoPoint ul = new GeoPoint(maxLat, minLon);
        GeoPoint ur = new GeoPoint(maxLat, maxLon);
        GeoPoint lr = new GeoPoint(minLat, maxLon);
        GeoPoint ll = new GeoPoint(minLat, minLon);

        generation.getElevationQueryParams().spatialFilter = DatasetDescriptor.createSimpleCoverage(ul, ur, lr, ll);
        generation.setStrictSpatialFilter(true);
    }

    /**
     * Implementation and extension of the contour group that exposes color scheme information.
     * This is directly tied to our UI in {@link ContourListPanel}
     * hence this only being accessible within the client module.
     */
    public static class ExtendedContourLineGroup implements ContourLineGroup
    {
        public static final int COLOR_SCHEME_FLAT = 0;
        public static final int COLOR_SCHEME_GRAD = 1;

        private final List<ContourLine> contourLines;
        private final ContourLineLayer layer;
        private final IRaptorLayer raptorLayer;
        private int scheme;

        private ExtendedContourLineGroup(@Nonnull List<ContourLine> contourLines,
                                         @Nonnull ContourLineLayer layer,
                                         @Nonnull IRaptorLayer raptorLayer)
        {
            this.contourLines = contourLines;
            this.layer = layer;
            this.raptorLayer = raptorLayer;

            scheme = determineColorScheme();
        }

        /**
         * @return Current color scheme.
         */
        public int getColorScheme()
        {
            return scheme;
        }

        /**
         * @param scheme New color scheme.
         */
        public void setColorScheme(int scheme)
        {
            this.scheme = scheme;
        }

        @Nonnull
        @Override
        public IRaptorLayer getLayer()
        {
            return raptorLayer;
        }

        @Override
        public ContourStyle getStyle()
        {
            return layer.getStyle();
        }

        @Override
        public Envelope getBoundingBox()
        {
            return layer.getBoundingBox();
        }

        @Override
        public double getMinElevationMeters()
        {
            return layer.getMinElevationMeters();
        }

        @Override
        public double getMaxElevationMeters()
        {
            return layer.getMaxElevationMeters();
        }

        @Nonnull
        @Override
        public List<ContourLine> getContourLines()
        {
            return contourLines;
        }

        @Override
        public String toString()
        {
            return raptorLayer.getDisplayName() + ": " + contourLines.size() + " contours";
        }

        /**
         * Estimates the color scheme of the line group, assuming its limited in scope
         * to one of our provided schemes.
         *
         * @return Expected style of the group.
         */
        private int determineColorScheme()
        {
            // Extract two unique major elevations from the contours.
            double elevationMajor1 = Double.NaN;
            double elevationMajor2 = Double.NaN;
            for (ContourLine line : contourLines)
            {
                double elevation = line.getElevationMeters();
                if (line.isMajor())
                {
                    if (Double.isNaN(elevationMajor1))
                    {
                        elevationMajor1 = elevation;
                    } else if (elevationMajor1 != elevation)
                    {
                        elevationMajor2 = elevation;

                        // We have two unique major elevations, so we're done with this loop.
                        break;
                    }
                }
            }

            // Use the color mapper with the extracted elevations to determine if we're using the flat or gradient color schemes.
            // The flat scheme will have both majors use the same color.
            // The gradient scheme won't follow this pattern, so any different is a positive match.
            ContourFunction<Integer> colorMapper = layer.getStyle().getContourColorMapper();
            Integer color1;
            Integer color2;
            if (Double.isNaN(elevationMajor1) || Double.isNaN(elevationMajor2))
            {
                // If there are not enough majors to tell the difference we'll fall back to checking the min/max elevation.
                // These should both be 'minors' given that we found no majors in the prior loop.
                // The same idea still applies, but with minors instead of majors.
                double minElevation = layer.getMinElevationMeters();
                double maxElevation = layer.getMaxElevationMeters();

                // Hack to coerce different inputs for the color mapper if there is only one contour elevation present.
                // PI is a unique enough number for it to reasonably prevent accidentally landing on a major interval (even if we pass major=false).
                if (maxElevation == minElevation) maxElevation += Math.PI;

                // These two colors are based on minor lines.
                color1 = colorMapper.apply(minElevation, false);
                color2 = colorMapper.apply(maxElevation, false);
            } else
            {
                // We have two major elevations, so we can extract an interval between these.
                double elevationMajorInterval = Math.abs(elevationMajor1 - elevationMajor2);
                double maxMajorElevation = Math.max(elevationMajor1, elevationMajor2);

                // These two colors are based on major lines.
                color1 = colorMapper.apply(maxMajorElevation + elevationMajorInterval, true);
                color2 = colorMapper.apply(maxMajorElevation, true);
            }

            // If the colors are the same we're using the flat scheme.
            return Objects.equals(color1, color2) ? COLOR_SCHEME_FLAT : COLOR_SCHEME_GRAD;
        }
    }
}
