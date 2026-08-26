package gov.takx.core.globe.tak.features;

import com.atakmap.coremap.conversions.Angle;
import com.atakmap.coremap.conversions.Span;
import com.atakmap.coremap.conversions.SpanUtilities;
import com.atakmap.coremap.maps.coords.NorthReference;
import com.atakmap.map.layer.feature.Feature;
import com.atakmap.map.layer.feature.geometry.Geometry;
import com.atakmap.map.layer.feature.geometry.LineString;
import com.atakmap.map.layer.feature.geometry.Point;
import com.atakmap.map.layer.feature.style.ArrowStrokeStyle;
import com.atakmap.map.layer.feature.style.BasicStrokeStyle;
import com.atakmap.map.layer.feature.style.LabelPointStyle;
import com.atakmap.map.layer.feature.style.Style;
import gov.tak.api.engine.map.coords.GeoCalculations;
import gov.tak.api.engine.map.coords.IGeoPoint;
import gov.tak.api.maps.coords.GeoPoint;
import gov.takx.api.coordinatesystem.ILocation;
import gov.takx.api.globe.IGlobeShapeMapObject;
import gov.takx.dataservices.shapes.Bullseye;
import jakarta.annotation.Nonnull;

import java.awt.Color;
import java.awt.Font;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;

import static gov.takx.platform.globe.GlobeTextUtils.DEFAULT_GLOBE_FONT;

/**
 * A {@link IFeatureProvider} for {@link IGlobeShapeMapObject} that represents an {@link Bullseye} shape.
 *
 * @since 5.5
 */
public class GlobeBullseyeFeatureProvider implements IFeatureProvider<IGlobeShapeMapObject, GlobeBullseyeFeatureProvider.BullseyeState>
{
    private static final BasicStrokeStyle GREEN_STYLE = new BasicStrokeStyle(Color.GREEN.getRGB(), 3);
    private static final BasicStrokeStyle RED_STYLE = new BasicStrokeStyle(Color.RED.getRGB(), 3);
    private static final EnumSet<LabelPointStyle.Style> BULLSEYE_LABEL_FONT_STYLE = EnumSet.of(LabelPointStyle.Style.BOLD);
    private static final Font BULLSEYE_LABEL_FONT = DEFAULT_GLOBE_FONT.deriveFont(DEFAULT_GLOBE_FONT.getSize() * 1.25f);

    private static final int NUM_SPOKES = 12;

    // How many points to use to estimate the circle with a polygon.
    private static final int NUM_POINTS_IN_CIRCLE = 72;

    @Nonnull
    @Override
    public BullseyeState createState(@Nonnull IGlobeShapeMapObject gmoBullseye)
    {
        Bullseye bullseye = gmoBullseye.requireShapeModel();
        return new BullseyeState(bullseye.getLocation(), bullseye.isVisible(), bullseye.isShowRangeRings(),
                bullseye.isPointingEdgeToCenter(), bullseye.getRadius(), bullseye.getRingRadius(), bullseye.getRingCount(),
                bullseye.getNorthReference(), bullseye.getBearingUnits(), bullseye.getSpanUnits());
    }

    @Override
    public int getFeatureCount(@Nonnull BullseyeState state)
    {
        if (state.radius == 0 || state.location() == GeoPoint.UNKNOWN_POINT) return 0; // if not complete, there are no features

        // one for the arrow, one for each spoke; if showing rings: one for each ring, one for each ring label, and one for each spoke annotation
        return 1 + NUM_SPOKES + (state.isShowRangeRings ? (state.ringCount * 2 + NUM_SPOKES) : 0);
    }

    @Override
    public boolean isVisible(@Nonnull BullseyeState state)
    {
        return state.isVisible();
    }

    @Nonnull
    @Override
    public Geometry[] createGeometry(@Nonnull BullseyeState state)
    {
        int featureCount = getFeatureCount(state);
        if (featureCount == 0) return new Geometry[]{};
        Geometry[] geometries = new Geometry[featureCount];
        int index = 0;

        double northOffset = switch (state.northReference)
        {
            case TRUE -> 0.0;
            case MAGNETIC -> GeoCalculations.magneticDeclination(state.location);
            case GRID -> GeoCalculations.computeGridConvergence(state.location, 0, state.radius);
        };

        // northArrow
        LineString northArrow = new LineString(3);
        IGeoPoint threeQuarters = GeoCalculations.pointAtDistance(state.location, 0 + northOffset, state.radius * .75);
        IGeoPoint edge = GeoCalculations.pointAtDistance(state.location, 0 + northOffset, state.radius);
        final var startArrow = state.pointingEdgeToCenter ? edge : threeQuarters;
        final var endArrow = state.pointingEdgeToCenter ? threeQuarters : edge;

        northArrow.addPoint(startArrow.getLongitude(), startArrow.getLatitude(), startArrow.getAltitude());
        northArrow.addPoint(endArrow.getLongitude(), endArrow.getLatitude(), endArrow.getAltitude());

        geometries[index++] = northArrow;

        // spokes and annotations
        final int deltaDegrees = 360 / NUM_SPOKES;
        double degrees = deltaDegrees + (state.pointingEdgeToCenter ? 180 : 0);

        double centerLon = state.location.getLongitude();
        double centerLat = state.location.getLatitude();
        double centerAlt = state.location.getAltitude();
        for (int i = 1; i <= NUM_SPOKES; i++)
        {
            IGeoPoint end = GeoCalculations.pointAtDistance(state.location, degrees + northOffset, state.radius);
            if (state.isShowRangeRings) geometries[index++] = new Point(end.getLongitude(), end.getLatitude(), end.getAltitude());
            LineString linestring = new LineString(3);
            linestring.addPoint(centerLon, centerLat, centerAlt);
            linestring.addPoint(end.getLongitude(), end.getLatitude(), end.getAltitude());
            geometries[index++] = linestring;
            degrees += deltaDegrees;
        }

        // rings + labels
        if (state.isShowRangeRings)
        {
            for (int i = 1; i <= state.ringCount; i++)
            {
                List<ILocation> ringLocations = assembleRingPositions(state, state.ringRadius * i);
                LineString lineString = new LineString(3);
                ringLocations.forEach(l -> lineString.addPoint(l.getLongitude(), l.getLatitude(), l.getAltitude()));
                geometries[index++] = lineString;
                IGeoPoint annotationPoint = GeoCalculations.pointAtDistance(state.location, 180 + northOffset, state.ringRadius * i);
                geometries[index++] = new Point(annotationPoint.getLongitude(), annotationPoint.getLatitude(), annotationPoint.getAltitude());
            }
        }
        return geometries;
    }

    @Nonnull
    @Override
    public Style[] createStyle(@Nonnull BullseyeState state)
    {
        int featureCount = getFeatureCount(state);
        if (featureCount == 0) return new Style[]{};
        Style[] styles = new Style[featureCount];
        int index = 0;

        BasicStrokeStyle directionStyle = state.pointingEdgeToCenter ? RED_STYLE : GREEN_STYLE;
        // north arrow
        styles[index++] = new ArrowStrokeStyle(13f, 2, (short) -1, directionStyle.getColor(), 3, 0);

        // spokes and annotations
        boolean isDegrees = state.bearingUnits == Angle.DEGREE;
        double perSpoke = (isDegrees ? 360.0 : 6400.0) / NUM_SPOKES;
        int whiteRGB = Color.WHITE.getRGB();
        String fontName = BULLSEYE_LABEL_FONT.getName();
        int fontSize = BULLSEYE_LABEL_FONT.getSize();
        String northSpokeAbbrev = (isDegrees ? "" : "mils") + state.northReference.getAbbrev();

        for (int i = 1; i <= NUM_SPOKES; i++)
        {
            if (state.isShowRangeRings)
            {
                String annotation = Math.round(i * perSpoke) + (i == NUM_SPOKES ? northSpokeAbbrev : "");
                styles[index++] = new LabelPointStyle(annotation, whiteRGB, 0, LabelPointStyle.ScrollMode.DEFAULT,
                        fontName, fontSize, BULLSEYE_LABEL_FONT_STYLE, 0, 0, 0, 0,
                        0, true, 14d, 1);
            }
            styles[index++] = directionStyle;
        }

        // rings + labels
        if (state.isShowRangeRings)
        {
            for (int i = 1; i <= state.ringCount; i++)
            {
                styles[index++] = directionStyle;
                String formattedRadius = SpanUtilities.formatType(state.distanceUnits.getType(), state.ringRadius * i, Span.METER);
                styles[index++] = new LabelPointStyle(formattedRadius, Color.WHITE.getRGB(), 0, LabelPointStyle.ScrollMode.DEFAULT,
                        BULLSEYE_LABEL_FONT.getName(), BULLSEYE_LABEL_FONT.getSize(), BULLSEYE_LABEL_FONT_STYLE,
                        0, 0, 0, 0, 0, false, 14d, 1);
            }
        }
        return styles;
    }

    @Nonnull
    @Override
    public Feature.AltitudeMode[] createAltitudeModes(@Nonnull BullseyeState state)
    {
        return FeatureProviderUtility.getAltitudeModeArray(Feature.AltitudeMode.ClampToGround, getFeatureCount(state));
    }

    /**
     * @param state   State data for this {@link Bullseye}.
     * @param radius  Radius of this ring in {@link Span#METER Span.METER}.
     * @return The {@link ILocation ILocations} that make up the ring.
     */
    private List<ILocation> assembleRingPositions(@Nonnull BullseyeState state, double radius)
    {
        final int numLocations = 1 + NUM_POINTS_IN_CIRCLE;
        final double da = 360.0 / (numLocations - 1);
        List<ILocation> locations = new ArrayList<>(numLocations);

        for (int i = 0; i < numLocations; i++) locations.add(new GeoPoint(GeoCalculations.pointAtDistance(state.location, da * i, radius)));
        return locations;
    }

    /**
     * State object for {@link GlobeBullseyeFeatureProvider}.
     */
    public record BullseyeState(ILocation location, boolean isVisible, boolean isShowRangeRings,
            boolean pointingEdgeToCenter, double radius, double ringRadius, int ringCount,
            NorthReference northReference, Angle bearingUnits, Span distanceUnits)
    {
    }
}
