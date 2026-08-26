package gov.takx.core.globe.widgets;

import com.atakmap.coremap.conversions.Span;
import com.atakmap.map.elevation.ElevationManager;
import com.atakmap.map.layer.LegacyAdapters;
import com.atakmap.map.layer.feature.DataStoreException;
import com.atakmap.map.layer.feature.Feature;
import com.atakmap.map.layer.feature.FeatureDataStore2;
import com.atakmap.map.layer.feature.FeatureLayer3;
import com.atakmap.map.layer.feature.FeatureSet;
import com.atakmap.map.layer.feature.control.FeatureEditControl;
import com.atakmap.map.layer.feature.geometry.LineString;
import com.atakmap.map.layer.feature.geometry.Point;
import com.atakmap.map.layer.feature.style.BasicPointStyle;
import com.atakmap.map.layer.feature.style.BasicStrokeStyle;
import com.atakmap.map.layer.feature.style.Style;
import gov.nasa.worldwind.View;
import gov.nasa.worldwind.WorldWind;
import gov.nasa.worldwind.avlist.AVKey;
import gov.tak.api.commons.graphics.Bitmap;
import gov.tak.api.commons.graphics.BitmapDrawable;
import gov.tak.api.engine.map.ILayer;
import gov.tak.api.engine.map.IMapRendererEnums.DisplayOrigin;
import gov.tak.api.engine.map.MapRenderer;
import gov.tak.api.engine.map.MapSceneModel;
import gov.tak.api.engine.map.coords.GeoCalculations;
import gov.tak.api.engine.map.coords.GeoPoint;
import gov.tak.api.engine.map.coords.IGeoPoint;
import gov.tak.api.widgets.IMapWidget;
import gov.tak.core.map.layer.feature.datastore.memory.InMemoryFeatureDataStore;
import gov.tak.platform.graphics.PointF;
import gov.tak.platform.marshal.MarshalManager;
import gov.tak.platform.ui.MotionEvent;
import gov.tak.platform.util.LimitingThread;
import gov.tak.platform.widgets.DrawableWidget;
import gov.takx.api.annotation.InvokeOnEDT;
import gov.takx.api.annotation.PseudoBean;
import gov.takx.api.coordinatesystem.ILocation;
import gov.takx.api.globe.AWidget;
import gov.takx.api.globe.CoreWidgetNames;
import gov.takx.api.globe.IGlobeDelegate;
import gov.takx.api.globe.IWorldWindowGL;
import gov.takx.api.layer.LayerGroup;
import gov.takx.api.preferences.IPreferenceMonitor;
import gov.takx.core.globe.ApplicationGlobe;
import gov.takx.core.globe.tak.TakLayerManager;
import gov.takx.core.globe.tak.ww.ApplicationOrbitView;
import gov.takx.platform.preferences.UserPreferenceEntry;
import gov.takx.platform.utils.ShapeUtils;
import gov.takx.platform.utils.UnitsOfMeasureUtils;
import gov.takx.platform.utils.units.AltitudeUnits;
import gov.takx.platform.utils.units.SpanConversionUtils;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import jakarta.annotation.PostConstruct;
import jakarta.inject.Inject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.font.GlyphVector;
import java.awt.geom.Path2D;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.beans.PropertyChangeListener;
import java.lang.invoke.MethodHandles;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

/**
 * A TME widget that displays a terrain profile graph in a screen corner. Provides elevation based on given positions.
 * <p/>
 * Defaults to a line across the screen in {@link FollowBehavior#FOLLOW_VIEW} mode, but can be a given path if
 * {@link #setFollowPath(List)} is called (thereby setting the follow behavior to {@link FollowBehavior#FOLLOW_PATH}).
 *
 * @since 5.0
 */
@SuppressWarnings("CdiManagedBeanInconsistencyInspection")
@PseudoBean
public class TerrainProfilerWidget extends AWidget
{
    private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    private static final Font DEFAULT_FONT = Font.decode("Arial-PLAIN-12");
    private static final int WIDGET_SPACING = 20;
    private static final int LABEL_SIZE = 15;
    private static final int BUTTON_SIZE = 15;
    private static final int BUTTON_SPACING = 5;

    private static final Color DEFAULT_COLOR = new Color(255, 255, 255, 255);
    private static final Color LABEL_BACKGROUND_COLOR = new Color(0, 0, 0, 150);
    private static final Color GRAPH_FILL_COLOR = new Color(255, 255, 255, 100);
    private static final Color CROSSHAIR_COLOR = new Color(255, 255, 128, 204);
    private static final Color SELECTED_POINT_COLOR = new Color(255, 128, 128, 204);
    private static final Color HIGHLIGHT_COLOR = new Color(255, 255, 255, 128);
    private static final Color SEA_LEVEL_COLOR = new Color(178, 178, 255, 128);
    private static final Color BACKGROUND_COLOR = new Color(0, 0, 0, 90);
    private static final Color NO_DTED_FOUND = Color.RED;
    private static final BasicStroke GRAPH_STROKE = new BasicStroke(1f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND);

    private static final EnumMap<Size, Dimension> SIZES = new EnumMap<>(Size.class);

    private static BufferedImage backgroundImage;
    private static BufferedImage increaseImage;
    private static BufferedImage increaseHighlightImage;
    private static BufferedImage decreaseImage;
    private static BufferedImage decreaseHighlightImage;

    @Inject
    private TakLayerManager takLayerManager;

    private final IPreferenceMonitor<AltitudeUnits> altitudeUnits =
            UserPreferenceEntry.DISPLAY_ALTITUDE_UNITS.newMonitor(AltitudeUnits.class, v -> unitChanged());
    private final PropertyChangeListener viewListener = e -> updateAll();
    private final IGlobeDelegate globe;
    private final LimitingThread updateTaskThread = new LimitingThread("TerrainProfilerWidget.ComputeProfile", this::updateAllNoSync);
    private final AtomicReference<TerrainProfilerResult> terrainProfilerResult = new AtomicReference<>();

    private final FeatureLayer3 layer;
    private final InMemoryFeatureDataStore dataStore;
    private final Style selectionLineStyle = new BasicStrokeStyle(CROSSHAIR_COLOR.getRGB(), 1f);
    private final Style selectionPointStyle = new BasicPointStyle(SELECTED_POINT_COLOR.getRGB(), 10f);
    private final Style selectionPointHiddenStyle = new BasicPointStyle(0, 0f);

    private long selectionLineFeatureId;
    private long selectionPointFeatureId;
    private boolean featureEditing;

    private Size currentSize = Size.STANDARD;
    private DrawableWidget profilerWidget;

    private int samples = 250;
    private int unit = Span.METRIC;
    private int pickedSample = -1;           // Picked sample number if not -1
    private List<IGeoPoint> pathPositions;    // Path position list if FOLLOW_PATH
    private FollowBehavior follow = FollowBehavior.FOLLOW_VIEW;     // Profile position follow behavior

    private boolean decreaseHighlight;
    private boolean increaseHighlight;

    protected boolean zeroBased = true;        // Pad graph elevation scale to include sea level if true
    protected double profileLengthFactor = 1;  // Applied to default profile length (zoom on profile)
    protected int pathType = WorldWind.GREAT_CIRCLE;

    public TerrainProfilerWidget(@Nonnull IGlobeDelegate globe)
    {
        this.globe = globe;
        setOrientation(VERTICAL);
        setMargins(WIDGET_SPACING, WIDGET_SPACING, WIDGET_SPACING, WIDGET_SPACING);
        setName(CoreWidgetNames.TERRAIN_PROFILER);

        SIZES.put(Size.MINIMIZED, new Dimension(32, 32));
        SIZES.put(Size.STANDARD, new Dimension(450, 140));
        SIZES.put(Size.MAXIMIZED, new Dimension(840, 280));

        dataStore = new InMemoryFeatureDataStore();
        layer = new FeatureLayer3("Terrain Profiler Layer", dataStore);
        generateButtons();
    }

    @PostConstruct
    private void postConstruct()
    {
        populateFeaturesDatabase();
        takLayerManager.addLayer(layer, LayerGroup.UNLISTED_OVERLAY);

        globe.getWorldWindow().getView().addPropertyChangeListener(AVKey.VIEW, viewListener);
        unitChanged();
        setVisible(false);
        updateAll();
    }

    private void unitChanged()
    {
        int oldUnit = unit;
        unit = SpanConversionUtils.toSpanSystem(altitudeUnits.getCurrentValue());

        if (oldUnit != unit) updateAll();
    }

    /**
     * Adds the screen line and picked point follow dot features to the in-memory feature database
     */
    private void populateFeaturesDatabase()
    {
        try
        {
            dataStore.acquireModifyLock(true);
            FeatureSet featureSet = new FeatureSet("TerrainProfilerWidget", "TerrainProfilerWidget", "TerrainProfilerWidget", Double.MAX_VALUE, 0);
            long featureSetId = dataStore.insertFeatureSet(featureSet);

            Feature selectionLineFeature = new Feature(featureSetId, null, null, selectionLineStyle, null, Feature.AltitudeMode.ClampToGround, 0.0);
            selectionLineFeatureId = dataStore.insertFeature(selectionLineFeature);

            Feature selectionPointFeature = new Feature(featureSetId, null, null, selectionPointStyle, null, Feature.AltitudeMode.Absolute, 0.0);
            selectionPointFeatureId = dataStore.insertFeature(selectionPointFeature);
        } catch (InterruptedException e)
        {
            logger.warn("Interrupted while acquiring modify lock on TerrainProfilerWidget feature database.", e);
        } catch (DataStoreException e)
        {
            logger.error("Error inserting feature into TerrainProfilerWidget datastore.", e);
        } finally
        {
            dataStore.releaseModifyLock();
        }
    }

    /**
     * "Asynchronous" update method, thread-safe
     */
    private void updateAll()
    {
        if (isVisible()) updateTaskThread.exec();
    }

    /**
     * Synchronous update method, calling is not thread-safe. Should not be called directly, use {@link #updateAll()} instead
     */
    private void updateAllNoSync()
    {
        if (!isVisible()) return;
        SIZES.get(Size.MAXIMIZED).setSize((int) (globe.getWorldWindow().getSurfaceWidth() * .85), (int) (globe.getWorldWindow().getSurfaceHeight() * .50));
        TerrainProfileContext context = new TerrainProfileContext((ApplicationGlobe) globe, pathType, follow, samples, unit, zeroBased, currentSize, profileLengthFactor, decreaseHighlight, increaseHighlight, pickedSample, pathPositions);
        PathCalculationResults pathCalculationResults = computeProfile(context);
        BufferedImage backgroundImage = generateBackground(context);
        BufferedImage graphImage = generateGraph(context, pathCalculationResults);
        BufferedImage profilerImage = stitchProfiler(context, pathCalculationResults, backgroundImage, graphImage);
        TerrainProfilerResult result = new TerrainProfilerResult(profilerImage, pathCalculationResults);
        terrainProfilerResult.set(result);
        updateSelectionLine(context, pathCalculationResults);
        onTerrainProfilerResult();
    }

    @InvokeOnEDT
    private void onTerrainProfilerResult()
    {
        TerrainProfilerResult results = terrainProfilerResult.getAndSet(null);
        if (results != null) updateWidget(results.profilerImage);
    }

    private void updateWidget(@Nonnull BufferedImage profilerImage)
    {
        BitmapDrawable profilerBitmap = new BitmapDrawable(MarshalManager.marshal(profilerImage, BufferedImage.class, Bitmap.class));
        Dimension currentDimension = SIZES.get(currentSize);

        if (!getChildren().isEmpty()) // just update the existing widget
        {
            profilerWidget.setSize(currentDimension.width, currentDimension.height + LABEL_SIZE);
            profilerWidget.setDrawable(profilerBitmap);
        } else // there's no inner widget, initialize a new one
        {
            profilerWidget = new DrawableWidget(profilerBitmap);
            profilerWidget.setSize(currentDimension.width, currentDimension.height + LABEL_SIZE);
            TerrainProfilerListener listener = new TerrainProfilerListener();
            profilerWidget.addOnHoverListener(listener);
            profilerWidget.addOnClickListener(listener);
            addChildWidgetAt(0, profilerWidget);
        }
    }

    /**
     * Updates the selection line and the picked sample indicator if the user is hovering over the widget
     *
     * @param context     The current context of the update
     * @param pathResults the current calculation results, based on the context
     */
    private void updateSelectionLine(@Nonnull TerrainProfileContext context, @Nullable PathCalculationResults pathResults)
    {
        if (pathResults == null) return;
        var lineString = new LineString(2);
        pathResults.positions.forEach(point -> lineString.addPoint(point.getLongitude(), point.getLatitude()));
        var mapRenderer = context.globe.getPortableMapRenderer();

        ILayer adaptedLayer = LegacyAdapters.adapt2(layer);
        if (!featureEditing)
        {
            featureEditing = mapRenderer.visitControl(adaptedLayer, control -> {
                control.startEditing(selectionLineFeatureId);
                control.startEditing(selectionPointFeatureId);
            }, FeatureEditControl.class);
        }
        if (!featureEditing) return;

        mapRenderer.visitControl(adaptedLayer, control -> control.updateFeature(selectionLineFeatureId, FeatureDataStore2.PROPERTY_FEATURE_GEOMETRY, null, lineString, selectionLineStyle, Feature.AltitudeMode.ClampToGround, FeatureDataStore2.UPDATE_ATTRIBUTES_ADD_OR_REPLACE), FeatureEditControl.class);
        if (context.pickedSample != -1 && !pathResults.positions.isEmpty())
        {
            IGeoPoint pickedPosition = pathResults.positions.get(context.pickedSample);
            Point pickedPoint = new Point(pickedPosition.getLongitude(), pickedPosition.getLatitude(), Math.max(50, pathResults.length * .05));
            mapRenderer.visitControl(adaptedLayer, control -> control.updateFeature(selectionPointFeatureId, FeatureDataStore2.PROPERTY_FEATURE_GEOMETRY | FeatureDataStore2.PROPERTY_FEATURE_STYLE, null, pickedPoint, selectionPointStyle, Feature.AltitudeMode.Absolute, FeatureDataStore2.UPDATE_ATTRIBUTES_ADD_OR_REPLACE), FeatureEditControl.class);
        } else
        {
            mapRenderer.visitControl(adaptedLayer, control -> control.updateFeature(selectionPointFeatureId, FeatureDataStore2.PROPERTY_FEATURE_STYLE, null, null, selectionPointHiddenStyle, Feature.AltitudeMode.Absolute, FeatureDataStore2.UPDATE_ATTRIBUTES_ADD_OR_REPLACE), FeatureEditControl.class);
        }
    }

    /**
     * Calculate the profile based on the current context variables
     *
     * @param context The current context
     * @return The calculated results
     */
    @Nullable
    private static PathCalculationResults computeProfile(@Nonnull TerrainProfileContext context)
    {
        PathCalculationResults results = null;
        try
        {
            results = context.follow == FollowBehavior.FOLLOW_VIEW ? computeMirroredPositions(context) : computePathPositions(context);
        } catch (Exception ignored)
        {
        }
        return results;
    }

    /**
     * Return the positions based on the center viewpoint; positions are to the left and right of the center a given distance
     *
     * @param context The current update context
     * @return The calculated results
     */
    @Nullable
    private static PathCalculationResults computeMirroredPositions(@Nonnull TerrainProfileContext context)
    {
        IWorldWindowGL wwd = context.globe.getWorldWindow();
        View view = wwd.getView();
        double wwdGlobeRadiusRadians = ShapeUtils.RADIUS_OF_EARTH * Math.PI;

        MapRenderer<?> mapRenderer = context.globe.getPortableMapRenderer();
        MapSceneModel mapSceneModel = mapRenderer.getMapSceneModel(true, DisplayOrigin.UpperLeft);
        PointF point = new PointF(mapSceneModel.width / 2.0f, mapSceneModel.height / 2.0f);
        GeoPoint inverseGeoPoint = GeoPoint.createMutable();
        if (!mapSceneModel.inverse(point, inverseGeoPoint)) return null;
        IGeoPoint centerGeoPoint = createGeoPoint(inverseGeoPoint.getLatitude(), inverseGeoPoint.getLongitude());

        // Compute profile length
        double length = view instanceof ApplicationOrbitView applicationOrbitView
                ? Math.min(applicationOrbitView.getZoom() * .8 * context.profileLengthFactor, wwdGlobeRadiusRadians)
                : Math.min(Math.abs(view.getEyePosition().getElevation()) * .8 * context.profileLengthFactor, wwdGlobeRadiusRadians);

        IGeoPoint west = GeoCalculations.pointAtDistance(centerGeoPoint, -90, length / 2.0);
        IGeoPoint east = GeoCalculations.pointAtDistance(centerGeoPoint, 90, length / 2.0);

        List<IGeoPoint> positions = new ArrayList<>(context.samples);
        for (int i = 0; i < context.samples; i++)
        {
            IGeoPoint pathPoint = GeoCalculations.pointAtDistance(west, east, (double) i / context.samples);
            positions.add(createGeoPoint(pathPoint.getLatitude(), pathPoint.getLongitude()));
        }
        return getPathCalculationResults(positions, length);
    }

    /**
     * Return the positions based on the provided path points (via {@link #setFollowPath(List)})
     *
     * @param context The current update context
     * @return The calculated results
     */
    @Nullable
    private static PathCalculationResults computePathPositions(@Nonnull TerrainProfileContext context)
    {
        if (context.pathPositions.size() < 2) return null;

        double length = computePathLength(context);
        double lengthRadians = length / ShapeUtils.RADIUS_OF_EARTH;
        double step = lengthRadians / (context.samples - 1);
        int segmentIndex = 0;
        IGeoPoint geoPoint = context.pathPositions.get(segmentIndex);
        IGeoPoint nextGeoPoint = context.pathPositions.get(segmentIndex + 1);
        List<IGeoPoint> positions = new ArrayList<>(context.pathPositions.size());
        positions.add(geoPoint); // Set first sample at path start
        // Compute in between samples
        double distance;
        double azimuth;
        for (int i = 1; i < context.samples - 1; i++)
        {
            double stepToGo = step;
            while (stepToGo > 0)
            {
                distance = context.pathType == WorldWind.RHUMB_LINE
                        ? ShapeUtils.rhumbDistance(geoPoint, nextGeoPoint)
                        : ShapeUtils.greatCircleDistance(geoPoint, nextGeoPoint);
                if (distance >= stepToGo)
                {
                    if (context.pathType == WorldWind.RHUMB_LINE)
                    {
                        azimuth = ShapeUtils.rhumbAzimuth(geoPoint, nextGeoPoint);
                        geoPoint = ShapeUtils.rhumbEndPosition(geoPoint, azimuth, stepToGo);
                    } else
                    {
                        azimuth = ShapeUtils.greatCircleAzimuth(geoPoint, nextGeoPoint);
                        geoPoint = ShapeUtils.greatCircleEndPosition((ILocation) geoPoint, stepToGo * ShapeUtils.RADIUS_OF_EARTH, azimuth);
                    }
                    positions.add(geoPoint);
                    stepToGo = 0;
                } else
                {
                    segmentIndex++;
                    geoPoint = context.pathPositions.get(segmentIndex);
                    nextGeoPoint = context.pathPositions.get(segmentIndex + 1);
                    stepToGo -= distance;
                }
            }
        }
        // Set last sample at path end
        positions.add(context.pathPositions.get(context.pathPositions.size() - 1));
        return getPathCalculationResults(positions, length);
    }

    private static PathCalculationResults getPathCalculationResults(List<IGeoPoint> positions, double length)
    {
        double[] elevations = getElevations(positions);
        double minElevation = Double.MAX_VALUE;
        double maxElevation = -Double.MAX_VALUE;
        for (int i = 0; i < positions.size(); i++)
        {
            double elevation = elevations[i];
            positions.get(i).setAltitude(elevation);
            // leave geopoint elevation as NaN so it can be detected later, set to 0 for min/max only
            if (Double.isNaN(elevation)) elevation = 0;
            minElevation = Math.min(elevation, minElevation);
            maxElevation = Math.max(elevation, maxElevation);
        }
        return new PathCalculationResults(length, minElevation, maxElevation, positions);
    }

    private static double computePathLength(@Nonnull TerrainProfileContext context)
    {
        double pathLength = 0;
        IGeoPoint geoPoint1 = null;
        for (IGeoPoint geoPoint2 : context.pathPositions)
        {
            if (geoPoint1 != null) pathLength += GeoCalculations.distance(geoPoint1, geoPoint2);
            geoPoint1 = geoPoint2;
        }
        return pathLength;
    }

    /**
     * Generate the buttons for the profiler, both increase and decrease buttons highlighted and not
     */
    private static void generateButtons()
    {
        increaseImage = generateButton(BACKGROUND_COLOR, DEFAULT_COLOR, true);
        increaseHighlightImage = generateButton(HIGHLIGHT_COLOR, DEFAULT_COLOR, true);
        decreaseImage = generateButton(BACKGROUND_COLOR, DEFAULT_COLOR, false);
        decreaseHighlightImage = generateButton(HIGHLIGHT_COLOR, DEFAULT_COLOR, false);
    }

    /**
     * Generate a BufferedImage button based on the parameters
     *
     * @param backgroundColor The background color of the button
     * @param foregroundColor The color of the elements drawn on the button
     * @param isIncrease      If the button is an increase button or not
     * @return A button of the specified type
     */
    @Nonnull
    private static BufferedImage generateButton(@Nonnull Color backgroundColor, @Nonnull Color foregroundColor, boolean isIncrease)
    {
        int buttonHalfPos = BUTTON_SIZE / 2;
        int minButtonPos = (int) (BUTTON_SIZE * .25); // draw 25% of button size away from one edge...
        int maxButtonPos = (int) (BUTTON_SIZE * .75); // to 25% away from the other edge

        BufferedImage button = new BufferedImage(BUTTON_SIZE, BUTTON_SIZE, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = button.createGraphics();
        g.setColor(backgroundColor);
        g.fillRect(0, 0, BUTTON_SIZE, BUTTON_SIZE);
        g.setColor(foregroundColor);
        g.drawLine(minButtonPos, buttonHalfPos, maxButtonPos, buttonHalfPos);
        if (isIncrease) g.drawLine(buttonHalfPos, minButtonPos, buttonHalfPos, maxButtonPos);
        g.dispose();
        return button;
    }

    /**
     * Generate the background for the Profiler
     */
    @Nonnull
    private static BufferedImage generateBackground(@Nonnull TerrainProfileContext context)
    {
        Dimension currentDimension = SIZES.get(context.currentSize);
        // if the background is initialized & hasn't changed size, don't need to redraw
        if (backgroundImage != null && backgroundImage.getWidth() == currentDimension.width && backgroundImage.getHeight() == currentDimension.height) return backgroundImage;

        backgroundImage = new BufferedImage(currentDimension.width, currentDimension.height, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = backgroundImage.createGraphics();
        // Background color
        g.setColor(BACKGROUND_COLOR);
        g.fillRect(0, 0, currentDimension.width, currentDimension.height);

        // Grid - minimal
        g.setColor(DEFAULT_COLOR);
        g.drawLine(0, currentDimension.height - 1, currentDimension.width - 1, currentDimension.height - 1); // bottom horizontal line
        g.drawLine(0, 0, 0, currentDimension.height - 1); // left vertical line
        g.drawLine(currentDimension.width - 1, 0, currentDimension.width - 1, currentDimension.height - 1); // right vertical line
        g.setColor(new Color(DEFAULT_COLOR.getRed(), DEFAULT_COLOR.getBlue(), DEFAULT_COLOR.getGreen(), (int) (DEFAULT_COLOR.getAlpha() * .25)));
        g.drawLine(currentDimension.width / 2, 0, currentDimension.width / 2, currentDimension.height - 1); // middle vertical line
        g.dispose();
        return backgroundImage;
    }

    /**
     * Generate the graph for the Profiler. Includes the crosshair & min/max lines
     */
    @Nonnull
    private static BufferedImage generateGraph(@Nonnull TerrainProfileContext context, @Nullable PathCalculationResults pathResults)
    {
        Dimension currentDimension = SIZES.get(context.currentSize);
        BufferedImage graphImage = new BufferedImage(currentDimension.width, currentDimension.height, BufferedImage.TYPE_INT_ARGB);
        if (pathResults == null || pathResults.positions.isEmpty()) return graphImage;
        Graphics2D g = graphImage.createGraphics();

        // Adjust min/max elevation for the graph
        double min = pathResults.minElevation;
        double max = pathResults.maxElevation;
        if (context.zeroBased)
        {
            if (min > 0) min = 0;
            if (max < 0) max = 0;
        }
        double stepX = currentDimension.width / pathResults.length;
        double stepY = max == min ? 1 : currentDimension.height / (max - min);
        double lengthStep = pathResults.length / (context.samples - 1);
        double x;
        double y;
        g.setColor(NO_DTED_FOUND);

        Path2D graphPath = new Path2D.Double();
        graphPath.moveTo(0, currentDimension.height);
        for (int i = 0; i < context.samples; i++)
        {
            x = i * lengthStep * stepX;
            GeoPoint position = (GeoPoint) pathResults.positions.get(i);
            double elevation = position.getAltitude();
            if (Double.isNaN(elevation)) // no dted detected
            {
                y = currentDimension.height - ((-min) * stepY);
                g.drawLine((int) x, (int) y - 1, (int) (x + 1), (int) y - 1);
            } else
            {
                y = currentDimension.height - ((elevation - min) * stepY);
            }
            graphPath.lineTo(x, y);
        }
        graphPath.lineTo(currentDimension.width, currentDimension.height);
        graphPath.closePath();
        g.setColor(GRAPH_FILL_COLOR);
        g.fill(graphPath);
        g.setColor(DEFAULT_COLOR);
        g.setStroke(GRAPH_STROKE);
        g.draw(graphPath);

        // Selected/picked vertical and horizontal lines (Crosshair)
        if (context.pickedSample != -1 && context.currentSize != Size.MINIMIZED && !pathResults.positions.isEmpty())
        {
            double pickedX = context.pickedSample * lengthStep * stepX;
            double pickedY = currentDimension.height - (pathResults.positions.get(context.pickedSample).getAltitude() - min) * stepY;
            g.setColor(CROSSHAIR_COLOR);
            g.drawLine((int) pickedX, 0, (int) pickedX, currentDimension.height); // vertical
            g.drawLine(0, (int) pickedY, currentDimension.width, (int) pickedY); // horizontal
        }
        // Min elevation horizontal line
        if (pathResults.minElevation != min)
        {
            y = currentDimension.height - ((pathResults.minElevation - min) * stepY);
            g.setColor(HIGHLIGHT_COLOR);
            g.drawLine(0, (int) y, currentDimension.width, (int) y);
        }
        // Max elevation horizontal line
        if (pathResults.maxElevation != max)
        {
            y = currentDimension.height - ((pathResults.maxElevation - min) * stepY);
            g.setColor(HIGHLIGHT_COLOR);
            g.drawLine(0, (int) y, currentDimension.width, (int) y);
        }
        // Sea level in between positive elevations only (not across land)
        if (min < 0 && max >= 0)
        {
            g.setColor(SEA_LEVEL_COLOR);
            y = currentDimension.height - (-pathResults.minElevation * stepY);
            double previousX = -1;
            for (int i = 0; i < context.samples; i++)
            {
                x = i * lengthStep * stepX;
                if (pathResults.positions.get(i).getAltitude() > 0 || i == context.samples - 1)
                {
                    if (previousX >= 0)
                    {
                        g.drawLine((int) previousX, (int) y, (int) x, (int) y);
                        previousX = -1;
                    }
                } else
                {
                    previousX = previousX < 0 ? x : previousX;
                }
            }
        }
        g.dispose();
        return graphImage;
    }

    @Nonnull
    private static BufferedImage stitchProfiler(@Nonnull TerrainProfileContext context, @Nullable PathCalculationResults pathResults, @Nonnull BufferedImage backgroundImage, @Nonnull BufferedImage graphImage)
    {
        Dimension currentDimension = SIZES.get(context.currentSize);
        BufferedImage profilerImage = new BufferedImage(currentDimension.width, currentDimension.height + LABEL_SIZE, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = profilerImage.createGraphics();
        g.drawImage(backgroundImage, null, 0, 0);
        g.drawImage(graphImage, null, 0, 0);
        if (context.currentSize == Size.STANDARD)
        {
            g.drawImage(context.decreaseHighlight ? decreaseHighlightImage : decreaseImage, null, currentDimension.width - 2 * BUTTON_SIZE - 2 * BUTTON_SPACING, BUTTON_SIZE / 2);
            g.drawImage(context.increaseHighlight ? increaseHighlightImage : increaseImage, null, currentDimension.width - BUTTON_SIZE - BUTTON_SPACING, BUTTON_SIZE / 2);
        } else if (context.currentSize == Size.MAXIMIZED)
        {
            g.drawImage(context.decreaseHighlight ? decreaseHighlightImage : decreaseImage, null, currentDimension.width - BUTTON_SIZE - BUTTON_SPACING, BUTTON_SIZE / 2);
        }
        g.dispose();

        if (pathResults == null) return profilerImage;

        // min/max label @ bottom left
        String label = context.unit == Span.ENGLISH
                ? String.format("min %.0fft   max %.0fft", UnitsOfMeasureUtils.convertMetersToFeet(pathResults.minElevation), UnitsOfMeasureUtils.convertMetersToFeet(pathResults.maxElevation))
                : String.format("min %.0fm   max %.0fm", pathResults.minElevation, pathResults.maxElevation);
        renderLabel(context, profilerImage, label, -1, DEFAULT_COLOR);

        // current cursor pos (over widget) elevation @ bottom right
        if (context.pickedSample != -1 && !pathResults.positions.isEmpty())
        {
            double pickedElevation = pathResults.positions.get(context.pickedSample).getAltitude();
            boolean validElevation = !Double.isNaN(pickedElevation);
            if (!validElevation)
            {
                label = "No DTED currently available";
            } else
            {
                label = context.unit == Span.ENGLISH
                        ? String.format("%.0fft", UnitsOfMeasureUtils.convertMetersToFeet(pickedElevation))
                        : String.format("%.0fm", pickedElevation);
            }
            renderLabel(context, profilerImage, label, 1, validElevation ? DEFAULT_COLOR : NO_DTED_FOUND);
        }

        return profilerImage;
    }

    private static void renderLabel(@Nonnull TerrainProfileContext context, @Nonnull BufferedImage profilerImage, @Nonnull String label, int align, @Nonnull Color textColor)
    {
        if (context.currentSize == Size.MINIMIZED) return;
        Dimension currentDimension = SIZES.get(context.currentSize);
        Graphics2D g = profilerImage.createGraphics();
        g.setFont(DEFAULT_FONT);
        GlyphVector labelGlyphVector = DEFAULT_FONT.createGlyphVector(g.getFontRenderContext(), label);
        Rectangle2D labelBounds = labelGlyphVector.getVisualBounds();
        int xPos;
        if (align < 0) //left align
        {
            xPos = 0;
        } else if (align == 0) //centered
        {
            xPos = (int) (currentDimension.width - labelBounds.getWidth() - 2) / 2; // - 2 to allow shadow to be accounted for
        } else //right align
        {
            xPos = (int) (currentDimension.width - labelBounds.getWidth()) - 2; // - 2 to allow shadow to be accounted for
        }
        int yPos = (int) (currentDimension.height + labelBounds.getHeight() * 1.25); // offset because the yPos is the _bottom_ of where the text starts drawing
        g.setColor(LABEL_BACKGROUND_COLOR);
        // variation off pos/height/width is to ensure the label is fully encapsulated by the background
        g.fillRect(xPos, (int) (yPos - labelBounds.getHeight()), (int) (labelBounds.getWidth() + 4), (int) (labelBounds.getHeight() * 1.25));
        g.setColor(Color.BLACK);
        g.drawString(label, xPos + 1, yPos + 1);
        g.setColor(textColor);
        g.drawString(label, xPos, yPos);
        g.dispose();
    }

    /**
     * Using the {@link ElevationManager} get the elevation data at the points provided;
     * Emulates WW behavior by setting 0 when no data is available
     *
     * @param points A list of points to retrieve elevation data about
     * @return an array of elevations relative to the passed in list
     */
    private static double[] getElevations(@Nonnull List<IGeoPoint> points)
    {
        double[] elevations = new double[points.size()];
        ElevationManager.getElevation(points.stream().map(point -> new com.atakmap.coremap.maps.coords.GeoPoint(point.getLatitude(), point.getLongitude())).iterator(),
                elevations, null);
        return elevations;
    }

    private void setSize(@Nonnull Size newSize)
    {
        currentSize = newSize;
    }

    /**
     * Set the graph center point placement behavior to {@link FollowBehavior#FOLLOW_VIEW}.
     *
     * @see #setFollowPath(List)
     */
    @SuppressWarnings({"UnusedDeclaration"})
    public void setFollowView()
    {
        follow = FollowBehavior.FOLLOW_VIEW;
    }

    /**
     * Overwrites the current positions list and sets the {@link FollowBehavior} to {@link FollowBehavior#FOLLOW_VIEW}.
     *
     * @param pathPositions The new positions to overwrite the existing positions with
     * @see #setFollowView()
     */
    @SuppressWarnings({"UnusedDeclaration"})
    public void setFollowPath(@Nonnull List<IGeoPoint> pathPositions)
    {
        this.pathPositions = new ArrayList<>(pathPositions.stream().map(geoPoint -> createGeoPoint(geoPoint.getLatitude(), geoPoint.getLongitude())).toList());
        setSamples(pathPositions.size());
        follow = FollowBehavior.FOLLOW_PATH;
    }

    public void setSamples(int number)
    {
        samples = Math.abs(number);
    }

    @Override
    public boolean setVisible(boolean visible)
    {
        if (super.setVisible(visible))
        {
            layer.setVisible(visible);
            if (visible) updateAll();
            return true;
        }
        return false;
    }

    @Override
    public void releaseResources()
    {
        takLayerManager.removeLayer(layer, LayerGroup.UNLISTED_OVERLAY);
        if (globe != null && globe.getWorldWindow() != null) globe.getWorldWindow().getView().removePropertyChangeListener(AVKey.VIEW, viewListener);
        updateTaskThread.dispose();
        altitudeUnits.dispose();
    }

    private static IGeoPoint createGeoPoint(double latitude, double longitude)
    {
        return new GeoPoint(latitude, longitude, IGeoPoint.UNKNOWN, IGeoPoint.AltitudeReference.HAE, IGeoPoint.UNKNOWN, IGeoPoint.UNKNOWN, GeoPoint.Access.READ_WRITE);
    }

    private class TerrainProfilerListener implements IMapWidget.OnHoverListener, IMapWidget.OnClickListener
    {
        private Rectangle2D standardIncreaseRect;
        private Rectangle2D standardDecreaseRect;
        private Rectangle2D maximizedDecreaseRect;

        public TerrainProfilerListener()
        {
            updateRectangles();
        }

        /**
         * Update the selection bounding boxes based on the current size; only needs calling on size update
         */
        private void updateRectangles()
        {
            Dimension currentDimension = SIZES.get(currentSize);
            standardDecreaseRect = new Rectangle(currentDimension.width - 2 * BUTTON_SIZE - 2 * BUTTON_SPACING, BUTTON_SIZE / 2, BUTTON_SIZE, BUTTON_SIZE);
            standardIncreaseRect = new Rectangle(currentDimension.width - BUTTON_SIZE - BUTTON_SPACING, BUTTON_SIZE / 2, BUTTON_SIZE, BUTTON_SIZE);
            maximizedDecreaseRect = new Rectangle(currentDimension.width - BUTTON_SIZE - BUTTON_SPACING, BUTTON_SIZE / 2, BUTTON_SIZE, BUTTON_SIZE);
        }

        @Override
        public void onMapWidgetHover(IMapWidget widget, MotionEvent event)
        {
            if (event.getAction() == MotionEvent.ACTION_HOVER_EXIT)
            {
                pickedSample = -1;
                increaseHighlight = false;
                decreaseHighlight = false;
            } else
            {
                Dimension currentDimension = SIZES.get(currentSize);
                PointF widgetPos = widget.getAbsoluteWidgetPosition();
                pickedSample = (int) (((double) (event.getX() - widgetPos.x) / currentDimension.width) * samples);
                float hoverXPos = event.getX() - widgetPos.x;
                float hoverYPos = event.getY() - widgetPos.y;
                switch (currentSize)
                {
                    case MINIMIZED ->
                    {
                        increaseHighlight = false;
                        decreaseHighlight = false;
                    }
                    case STANDARD ->
                    {
                        decreaseHighlight = standardDecreaseRect.contains(hoverXPos, hoverYPos);
                        increaseHighlight = standardIncreaseRect.contains(hoverXPos, hoverYPos);
                    }
                    case MAXIMIZED ->
                    {
                        increaseHighlight = false;
                        decreaseHighlight = maximizedDecreaseRect.contains(hoverXPos, hoverYPos);
                    }
                }
            }
            if (pickedSample > samples) pickedSample = -1;
            updateAll();
        }

        @Override
        public void onMapWidgetClick(IMapWidget widget, MotionEvent event)
        {
            PointF widgetPos = widget.getAbsoluteWidgetPosition();
            float clickXPos = event.getX() - widgetPos.x;
            float clickYPos = event.getY() - widgetPos.y;
            switch (currentSize)
            {
                case MINIMIZED -> setSize(Size.STANDARD);
                case STANDARD ->
                {
                    if (standardDecreaseRect.contains(clickXPos, clickYPos))
                    {
                        setSize(Size.MINIMIZED);
                    } else if (standardIncreaseRect.contains(clickXPos, clickYPos))
                    {
                        setSize(Size.MAXIMIZED);
                    }
                }
                case MAXIMIZED ->
                {
                    if (maximizedDecreaseRect.contains(clickXPos, clickYPos)) setSize(Size.STANDARD);
                }
            }
            updateRectangles();
            increaseHighlight = false;
            decreaseHighlight = false;
            pickedSample = -1;
            updateAll();
        }
    }

    private enum Size
    {
        MINIMIZED,
        STANDARD,
        MAXIMIZED
    }

    private enum FollowBehavior
    {
        FOLLOW_VIEW,
        FOLLOW_PATH
    }

    private record TerrainProfileContext(@Nonnull ApplicationGlobe globe, int pathType, @Nonnull FollowBehavior follow, int samples, int unit, boolean zeroBased, @Nonnull Size currentSize, double profileLengthFactor,
            boolean decreaseHighlight, boolean increaseHighlight, int pickedSample, @Nonnull List<IGeoPoint> pathPositions)
    {
    }

    private record PathCalculationResults(double length, double minElevation, double maxElevation, @Nonnull List<IGeoPoint> positions)
    {
    }

    private record TerrainProfilerResult(@Nonnull BufferedImage profilerImage, @Nullable PathCalculationResults pathResults)
    {
    }
}
