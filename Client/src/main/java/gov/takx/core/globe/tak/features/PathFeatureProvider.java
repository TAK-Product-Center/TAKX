package gov.takx.core.globe.tak.features;

import android.graphics.Color;
import com.atakmap.coremap.maps.coords.NorthReference;
import com.atakmap.map.elevation.ElevationManager;
import com.atakmap.map.layer.feature.Feature.AltitudeMode;
import com.atakmap.map.layer.feature.geometry.Geometry;
import com.atakmap.map.layer.feature.geometry.LineString;
import com.atakmap.map.layer.feature.style.ArrowStrokeStyle;
import com.atakmap.map.layer.feature.style.ArrowStrokeStyle.ArrowHeadMode;
import com.atakmap.map.layer.feature.style.BasicStrokeStyle;
import com.atakmap.map.layer.feature.style.CompositeStyle;
import com.atakmap.map.layer.feature.style.PatternStrokeStyle;
import com.atakmap.map.layer.feature.style.Style;
import gov.nasa.worldwind.WorldWind;
import gov.nasa.worldwind.avlist.AVKey;
import gov.nasa.worldwind.geom.LatLon;
import gov.nasa.worldwind.geom.Position;
import gov.nasa.worldwind.render.AnnotationAttributes;
import gov.nasa.worldwind.render.BasicShapeAttributes;
import gov.nasa.worldwind.render.Material;
import gov.nasa.worldwind.render.Path;
import gov.nasa.worldwind.render.ShapeAttributes;
import gov.tak.api.engine.map.coords.GeoCalculations;
import gov.tak.api.engine.map.coords.IGeoPoint;
import gov.takx.api.annotation.PseudoBean;
import gov.takx.api.preferences.IPreferenceMonitor;
import gov.takx.core.globe.ConnectingLinePath;
import gov.takx.core.globe.RotatableGlobeAnnotation;
import gov.takx.core.globe.tak.FeatureUtility;
import gov.takx.core.globe.tak.features.AnnotationFeatureProvider.AnnotationState;
import gov.takx.dataservices.shapes.Arrow;
import gov.takx.platform.globe.GlobeTextUtils;
import gov.takx.platform.preferences.UserPreferenceEntry;
import gov.takx.platform.utils.WWUtils;
import gov.takx.platform.utils.units.BearingUnits;
import gov.takx.platform.utils.units.RangeUnits;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.Point;
import java.lang.invoke.MethodHandles;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * A {@link IFeatureProvider} for {@link Path}.
 *
 * @since 3.5
 */
@PseudoBean
public final class PathFeatureProvider implements IFeatureProvider<Path, PathFeatureProvider.PathState>
{
    private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    public static final FeatureFactorySpi SPI = new AbstractFeatureFactorySpi(AbstractFeatureFactorySpi.RENDERABLE_PRIORITY, Path.class, PathFeatureProvider::new)
    {
    };

    // Attributes to use if none are supplied by a given surface object
    private static final ShapeAttributes DEFAULT_SHAPE_ATTRIBUTES = new BasicShapeAttributes();
    private static final double[] EMPTY_EXTRUSIONS = {0.0};
    private static final Point ZERO_OFFSET = new Point(0, 0);
    private static final AnnotationFeatureProvider ANNOTATION_FEATURE_PROVIDER = new AnnotationFeatureProvider();

    // Instances of our containing class are created quite frequently, so we need to keep the overhead of that creation
    // as low as possible.  Further, there is no "cleanup" model for provider instances, so no way to dispose of
    // resources like preference monitors. For those reasons, all our required monitors are kept in a single static instance.
    private static final Monitors MONITORS = new Monitors();

    // We can use a single instance here because all usage is single-threaded on the EDT
    private static final RotatableGlobeAnnotation arrowLabelAnnotation = makeLabelAnnotation();

    @Nonnull
    @Override
    public PathState createState(Path path)
    {
        ShapeAttributes shapeAttributes = getShapeAttributes(path);

        ArrayList<Position> positions = new ArrayList<>();
        path.getPositions().forEach(positions::add);

        Material material = shapeAttributes.getOutlineMaterial();
        int opacity = (int) (shapeAttributes.getInteriorOpacity() * 255);
        final java.awt.Color diffuse = material.getDiffuse();
        int diffuseColor = Color.argb(opacity, diffuse.getRed(), diffuse.getGreen(), diffuse.getBlue());
        int stippleFactor = shapeAttributes.getOutlineStippleFactor();
        short pattern = stippleFactor > 0 ? shapeAttributes.getOutlineStipplePattern() : -1;
        boolean drawArrows = path instanceof ConnectingLinePath connectingLinePath && connectingLinePath.isDrawArrowHead();
        float arrowRadius = 0;
        if (drawArrows)
        {
            if (stippleFactor == 0) stippleFactor = 2;
            arrowRadius = (float) (((ConnectingLinePath) path).getMaxScreenSize()) / 1.5f;
        }
        PathArrowState pathArrowState = createPathArrowState(path);
        return new PathState(positions, path.isVisible(), diffuseColor, shapeAttributes.getOutlineWidth(),
                stippleFactor, pattern, drawArrows, arrowRadius, pathArrowState, path.getAltitudeMode());
    }

    @Override
    public int getFeatureCount(@Nonnull PathState state)
    {
        return 1;
    }

    @Override
    public boolean isVisible(@Nonnull PathState state)
    {
        return state.isVisible();
    }

    @SuppressWarnings("AssignmentOrReturnOfFieldWithMutableType")
    @Nonnull
    @Override
    public Geometry[] createGeometry(@Nonnull PathState state)
    {
        Iterable<? extends Position> positions = state.positions;
        if (positions == null) return EMPTY_GEOMETRY;

        // Using the lineString as the geometry tells the kernel to render the text "somewhere along that line",
        // and it will automatically move the annotation if the view would clip it.  Using this instead of a
        // Point geometry brings feature parity with WinTAK and ATAK.

        final LineString lineString = new LineString(3);
        final boolean clampToGround = state.altitudeMode == WorldWind.CLAMP_TO_GROUND;

        final double[] prev = {Double.MAX_VALUE, Double.MAX_VALUE, Double.MAX_VALUE};
        positions.forEach(position -> {
            final double latitude = position.latitude.degrees;
            final double longitude = position.longitude.degrees;
            double hae = clampToGround ? 0 : position.elevation + ElevationManager.getGeoidHeight(latitude, longitude);
            if (latitude != prev[0] || longitude != prev[1] || hae != prev[2]) lineString.addPoint(longitude, latitude, hae);
            prev[0] = latitude;
            prev[1] = longitude;
            prev[2] = hae;
        });

        return lineString.getNumPoints() == 0 ? EMPTY_GEOMETRY : new Geometry[]{lineString};
    }

    @Nonnull
    @Override
    public Style[] createStyle(@Nonnull PathState state)
    {
        Style outlineStyle;
        if (state.drawArrows)
        {
            int stippleFactor = state.stippleFactor == 0 ? 2 : state.stippleFactor;
            outlineStyle = new ArrowStrokeStyle(state.arrowRadius, stippleFactor, state.pattern, state.diffuseColor, (float) state.outlineWidth, BasicStrokeStyle.EXTRUDE_NONE, ArrowHeadMode.PerVertex);
        } else if (state.stippleFactor > 0)
        {
            float r = Color.red(state.diffuseColor) / 255.f;
            float g = Color.green(state.diffuseColor) / 255.f;
            float b = Color.blue(state.diffuseColor) / 255.f;
            float a = Color.alpha(state.diffuseColor) / 255.f;
            outlineStyle = new PatternStrokeStyle(state.stippleFactor, state.pattern, r, g, b, a, (float) state.outlineWidth);
        } else
        {
            outlineStyle = new BasicStrokeStyle(state.diffuseColor, (float) state.outlineWidth);
        }
        outlineStyle = FeatureUtility.makeNoLabelStyle(outlineStyle);

        // Only add a label if this connecting line is handling an Arrow
        final Style style;
        if (state.pathArrowState != null)
        {
            arrowLabelAnnotation.setPosition(state.pathArrowState.position);
            arrowLabelAnnotation.setText(state.pathArrowState.text);
            arrowLabelAnnotation.setTextRotationAngle(state.pathArrowState.rotation);

            AnnotationAttributes attributes = arrowLabelAnnotation.getAttributes();
            int offsetX = attributes.getDrawOffset().x;
            int offsetY = attributes.getDrawOffset().y;
            AnnotationState annotationState = ANNOTATION_FEATURE_PROVIDER.createState(arrowLabelAnnotation);
            Style arrowLabelStyle = ANNOTATION_FEATURE_PROVIDER.createStyle(annotationState, 0, 0, offsetX, offsetY, true, true, true, true)[0];
            style = new CompositeStyle(new Style[]{outlineStyle, arrowLabelStyle});
        } else
        {
            style = outlineStyle;
        }
        return new Style[]{style};
    }

    @Nonnull
    @Override
    public AltitudeMode[] createAltitudeModes(@Nonnull PathState state)
    {
        int featureCount = getFeatureCount(state);
        return FeatureProviderUtility.getAltitudeModeArrayForWWMode(state.altitudeMode, featureCount);
    }

    @SuppressWarnings("AssignmentOrReturnOfFieldWithMutableType")
    @Nonnull
    @Override
    public double[] createExtrusions(@Nonnull PathState state)
    {
        return EMPTY_EXTRUSIONS;
    }

    /**
     * Create a {@link PathArrowState} for the provided path.
     * @since 5.0
     */
    private PathArrowState createPathArrowState(Path path)
    {
        // Only add a label if this connecting line is handling an Arrow
        // TODO: Rework this when we move away from WW renderables and directly generate features.
        if (!(path instanceof ConnectingLinePath && path.getValue(AVKey.TARGET) == Arrow.class
                && UserPreferenceEntry.SHOW_RANGE_BEARING_LABELS.getBooleanValue()))
            return null;

        // Calculate the bearing and range from the first position to the second
        final Iterator<? extends Position> iterator = path.getPositions().iterator();

        if (!iterator.hasNext()) return null;
        final var p1 = WWUtils.positionToRaptorLocation(iterator.next());

        if (!iterator.hasNext()) return null;
        final var p2 = WWUtils.positionToRaptorLocation(iterator.next());

        final var bearing = GeoCalculations.bearing(p1, p2);
        final var range = GeoCalculations.distance(p1, p2);
        final String directionArrow = bearing > 180 && bearing < 360 ? "←" : "→";

        final String rangeLabelText = MONITORS.rangeUnitsOfMeasure.getCurrentValue().format(range, 1);
        final String bearingLabelText = MONITORS.bearingUnits.getCurrentValue().format(p1, range, bearing);
        final String labelText = directionArrow + " " + rangeLabelText + "  " + bearingLabelText;

        // Calculate text rotation, which is measured CCW from horizontal, from bearing which is CW from North.
        // We also need to ensure the text remains "right side up".
        double ccwFromHorizontal = ((360 - bearing) + 90) % 360;
        final double rotation = ccwFromHorizontal > 90 && ccwFromHorizontal < 270
                ? (ccwFromHorizontal + 180) % 360
                : ccwFromHorizontal;

        // This used to call path.getCenterPoint().  However, since this code is now happening on a background thread,
        // it is no longer synchronized with the EDT.  Thus, changes happening to the position list on the EDT can
        // happen at random times relative to this thread.  That can lead to trying to determine the center point
        // of the path when the positions are in an inconsistent state (like having 0 or 1 point instead of 2), which
        // can lead to failures.  So, directly calculate the center point since we have the start, range, and bearing.

        final IGeoPoint centerPosition = GeoCalculations.pointAtDistance(p1, bearing, range / 2.0);
        Position position = new Position(LatLon.fromDegrees(centerPosition.getLatitude(), centerPosition.getLongitude()), 0);
        return new PathArrowState(position, labelText, rotation);
    }

    private static RotatableGlobeAnnotation makeLabelAnnotation()
    {
        RotatableGlobeAnnotation rotatableGlobeAnnotation = new RotatableGlobeAnnotation("", Position.ZERO);
        GlobeTextUtils.setDefaultLabelAnnotations(rotatableGlobeAnnotation);
        rotatableGlobeAnnotation.getAttributes().setDrawOffset(ZERO_OFFSET);
        rotatableGlobeAnnotation.setDragEnabled(false);
        return rotatableGlobeAnnotation;
    }

    /**
     * Get the active shape attributes for the provided {@link Path}.
     * @param path the path
     * @return the shape attributes
     */
    @Nonnull
    private ShapeAttributes getShapeAttributes(Path path)
    {
        final boolean highlighted = path.isHighlighted();
        ShapeAttributes shapeAttributes = highlighted ? path.getHighlightAttributes() : path.getAttributes();

        if (shapeAttributes == null)
        {
            logger.debug("No {} ShapeAttributes defined for {}; using defaults", highlighted ? "highlight" : "normal", path);

            shapeAttributes = DEFAULT_SHAPE_ATTRIBUTES;
            if (highlighted)
            {
                path.setHighlightAttributes(shapeAttributes);
            } else
            {
                path.setAttributes(shapeAttributes);
            }
        }
        return shapeAttributes;
    }

    /**
     * Holds our preference monitors.
     * @since 5.0
     */
    private static class Monitors
    {
        private final IPreferenceMonitor<RangeUnits> rangeUnitsOfMeasure = UserPreferenceEntry.DISPLAY_RANGE_UNITS.newMonitor(RangeUnits.class, v -> {
        });
        private final IPreferenceMonitor<NorthReference> northReference = UserPreferenceEntry.DISPLAY_NORTH_REFERENCE.newMonitor(NorthReference.class, v -> {
        });
        private final IPreferenceMonitor<BearingUnits> bearingUnits = UserPreferenceEntry.DISPLAY_BEARING_UNIT.newMonitor(BearingUnits.class, v -> {
        });
    }

    /**
     * State object for arrow properties in {@link PathState}.
     * @since 5.1
     */
    public record PathArrowState(Position position, String text, double rotation)
    {
    }

    /**
     * State object for {@link PathFeatureProvider}.
     * @since 5.1
     */
    public record PathState(List<Position> positions, boolean isVisible, int diffuseColor, double outlineWidth, int stippleFactor, short pattern, boolean drawArrows, float arrowRadius, @Nullable PathArrowState pathArrowState, int altitudeMode)
    {
    }
}
