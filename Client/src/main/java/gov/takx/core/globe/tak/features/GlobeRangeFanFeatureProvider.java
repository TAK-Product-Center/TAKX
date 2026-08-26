package gov.takx.core.globe.tak.features;

import com.atakmap.map.layer.feature.Feature.AltitudeMode;
import com.atakmap.map.layer.feature.geometry.Geometry;
import com.atakmap.map.layer.feature.geometry.GeometryCollection;
import com.atakmap.map.layer.feature.geometry.LineString;
import com.atakmap.map.layer.feature.style.BasicStrokeStyle;
import com.atakmap.map.layer.feature.style.CompositeStyle;
import com.atakmap.map.layer.feature.style.LabelPointStyle;
import com.atakmap.map.layer.feature.style.LabelPointStyle.ScrollMode;
import com.atakmap.map.layer.feature.style.Style;
import gov.tak.api.engine.map.coords.GeoCalculations;
import gov.tak.api.engine.map.coords.IGeoPoint;
import gov.tak.api.maps.coords.GeoPoint;
import gov.takx.api.coordinatesystem.ILocation;
import gov.takx.api.globe.IGlobeRangeFanMapObject;
import gov.takx.api.preferences.IPreferenceMonitor;
import gov.takx.commons.utils.StringUtils;
import gov.takx.dataservices.shapes.RangeFan;
import gov.takx.platform.globe.GlobeTextUtils;
import gov.takx.platform.preferences.UserPreferenceEntry;
import gov.takx.platform.utils.units.RangeUnits;
import jakarta.annotation.Nonnull;

import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumSet;
import java.util.List;
import java.util.stream.IntStream;

/**
 * A {@link IFeatureProvider} for {@link IGlobeRangeFanMapObject}.
 *
 * @since 5.5
 */
public final class GlobeRangeFanFeatureProvider implements IFeatureProvider<IGlobeRangeFanMapObject, GlobeRangeFanFeatureProvider.GlobeRangeFanState>
{
    private static final EnumSet<LabelPointStyle.Style> RANGE_FAN_LABEL_FONT_STYLE = EnumSet.noneOf(LabelPointStyle.Style.class);
    private static final BasicStrokeStyle BLACK_STROKE_STYLE = new BasicStrokeStyle(-1, 3);

    // How many points to use to estimate the circle with a polygon.
    private static final int NUM_POINTS_IN_CIRCLE = 72;
    private static final Monitors MONITORS = new Monitors();

    @Nonnull
    @Override
    public GlobeRangeFanState createState(IGlobeRangeFanMapObject globeRangeFanMapObject)
    {
        RangeFan rangeFan = globeRangeFanMapObject.getShapeModel();
        List<RangeRingState> rings = new ArrayList<>();
        List<Double> rangeDistances = rangeFan.getRangeDistances() != null ? rangeFan.getRangeDistances() : generateRangeDistances(rangeFan);
        final var rangeUnits = MONITORS.rangeUnitsMonitor.getCurrentValue();

        int index = 0;
        for (double rangeDistance : rangeDistances)
        {
            int color = rangeFan.getRangeColors().get(index % rangeFan.getRangeColors().size()).getRGB();
            String label = rangeFan.getRangeLabels() != null && index < rangeFan.getRangeLabels().size() ? rangeFan.getRangeLabels().get(index) : null;
            if (label == null) label = rangeUnits.format(rangeDistance, 1);

            List<ILocation> followLocations = rangeFan.getRangeType() == RangeFan.FOLLOW_TERRAIN && rangeFan.getFollowTerrainRingLocations() != null && index < rangeFan.getFollowTerrainRingLocations().size()
                    ? rangeFan.getFollowTerrainRingLocations().get(index)
                    : null;
            rings.add(new RangeRingState(followLocations, rangeDistance, color, label));
            index++;
        }
        return new GlobeRangeFanState(rangeFan.getLocation(), rangeFan.isVisible(), rangeFan.getOrientation(), rangeFan.getRangeType() == RangeFan.FOLLOW_TERRAIN, rangeFan.getSweep(), rangeFan.getSampleDistance(), rangeFan.isShowEdges(), rangeFan.isShowOrientation(), rangeFan.isShowRangeValues(), rings);
    }

    @Override
    public int getFeatureCount(@Nonnull GlobeRangeFanState state)
    {
        int ringCount = state.rings.size();
        int edgeLineCount = state.showEdges && state.sweep < 360 ? 1 : 0;
        int orientationLineCount = state.showOrientation ? 1 : 0;
        return ringCount + edgeLineCount + orientationLineCount;
    }

    @Override
    public boolean isVisible(@Nonnull GlobeRangeFanState state)
    {
        return state.isVisible;
    }

    @Nonnull
    @Override
    public Geometry[] createGeometry(@Nonnull GlobeRangeFanState state)
    {
        int featureCount = getFeatureCount(state);
        Geometry[] geometries = new Geometry[featureCount];
        int index = 0;
        for (RangeRingState ring : state.rings)
        {
            List<ILocation> ringLocations = ring.followLocations != null ? ring.followLocations : assemblePositions(state, ring.range);
            LineString lineString = new LineString(3);
            ringLocations.forEach(l -> lineString.addPoint(l.getLongitude(), l.getLatitude(), l.getAltitude()));
            geometries[index++] = lineString;
        }
        if (state.showEdges && state.sweep < 360) geometries[index++] = createEdgeLines(state);
        if (state.showOrientation) geometries[index++] = createOrientationLine(state);
        return geometries;
    }

    @Nonnull
    @Override
    public Style[] createStyle(@Nonnull GlobeRangeFanState state)
    {
        int featureCount = getFeatureCount(state);
        Style[] styles = new Style[featureCount];
        int index = 0;
        List<Style> styleList = new ArrayList<>();
        for (RangeRingState ring : state.rings)
        {
            styleList.clear();
            styleList.add(new BasicStrokeStyle(ring.color, 3));
            if (state.showRangeValues && !StringUtils.isEmpty(ring.labeltext))
            {
                styleList.add(new LabelPointStyle(ring.labeltext, -1, ring.color, ScrollMode.DEFAULT,
                        GlobeTextUtils.RANGE_FAN_LABEL_FONT.getName(), GlobeTextUtils.RANGE_FAN_LABEL_FONT.getSize(), RANGE_FAN_LABEL_FONT_STYLE,
                        0, 0, 0, 0, 0, true, 14d, 1));
            }
            styles[index++] = new CompositeStyle(styleList.toArray(Style[]::new));
        }
        if (state.showEdges && state.sweep < 360) styles[index++] = BLACK_STROKE_STYLE;
        if (state.showOrientation) styles[index++] = BLACK_STROKE_STYLE;
        return styles;
    }

    @Nonnull
    @Override
    public AltitudeMode[] createAltitudeModes(@Nonnull GlobeRangeFanState state)
    {
        int featureCount = getFeatureCount(state);
        return FeatureProviderUtility.getAltitudeModeArray(AltitudeMode.ClampToGround, featureCount);
    }

    private List<Double> generateRangeDistances(RangeFan rangeFan)
    {
        double distance = rangeFan.getMaxDistance() - rangeFan.getMinDistance();
        if (distance <= 0) return Collections.emptyList();
        int numValues = (int) ((distance) / rangeFan.getInterval());
        return numValues > 1
                ? IntStream.range(0, numValues).mapToObj(i -> rangeFan.getMinDistance() + i * (distance) / (numValues - 1)).toList()
                : List.of(distance);
    }

    /**
     * Calculate the positions in one range ring.
     *
     * @param state    State data for this range fan.
     * @param radius  Radius of the ring in meters.
     * @return The positions that make up the ring.
     */
    private List<ILocation> assemblePositions(GlobeRangeFanState state, double radius)
    {
        double orientation = state.heading;
        double sweep = state.sweep;

        double portion = sweep / 360.0;
        int numLocations = 1 + (int) (NUM_POINTS_IN_CIRCLE * portion); // points to use in this portion
        double startAngleRadians = Math.toRadians(orientation - (sweep / 2.0));
        double da = (portion * (2 * Math.PI)) / (numLocations - 1); // delta angle through sweep radians

        List<ILocation> locations = new ArrayList<>(numLocations);

        for (int i = 0; i < numLocations; i++)
        {
            double azimuthRadians = startAngleRadians + (da * i);

            ILocation ll = new GeoPoint(GeoCalculations.pointAtDistance(state.location, Math.toDegrees(azimuthRadians), radius));
            locations.add(ll);
        }

        return locations;
    }

    /**
     * Create the edge line geometries at the start and stop edges of the fan.
     */
    private Geometry createEdgeLines(GlobeRangeFanState state)
    {
        double orientation = state.heading;
        double halfSweep = state.sweep / 2.0;
        double startAngle = (orientation - halfSweep);
        double endAngle = (orientation + halfSweep);

        // Determine the max distance to draw the line.  This will be maxDistance or the last configured rangeDistance.
        final double distance = state.rings.get(state.rings.size() - 1).range;

        IGeoPoint endLocation1 = GeoCalculations.pointAtDistance(state.location, startAngle, distance);
        IGeoPoint endLocation2 = GeoCalculations.pointAtDistance(state.location, endAngle, distance);

        // Create the edge lines
        GeometryCollection geometryCollection = new GeometryCollection(3);
        LineString ls1 = new LineString(3);
        ls1.addPoint(state.location.getLongitude(), state.location.getLatitude());
        ls1.addPoint(endLocation1.getLongitude(), endLocation1.getLatitude());
        LineString ls2 = new LineString(3);
        ls2.addPoint(state.location.getLongitude(), state.location.getLatitude());
        ls2.addPoint(endLocation2.getLongitude(), endLocation2.getLatitude());
        geometryCollection.addGeometry(ls1);
        geometryCollection.addGeometry(ls2);
        return geometryCollection;
    }

    /**
     * Create the geometry that represents the orientation LOB.
     */
    private Geometry createOrientationLine(GlobeRangeFanState state)
    {
        double orientation = state.heading;

        // Determine the max distance to draw the line.  This will be maxDistance or the last configured rangeDistance.
        final double distance = state.rings.get(state.rings.size() - 1).range;

        IGeoPoint endLocation = GeoCalculations.pointAtDistance(state.location, orientation, distance);

        // Create the orientation line
        LineString lineString = new LineString(3);
        lineString.addPoint(state.location.getLongitude(), state.location.getLatitude());
        lineString.addPoint(endLocation.getLongitude(), endLocation.getLatitude());
        return lineString;
    }

    /**
     * State object for range rings
     */
    record RangeRingState(List<ILocation> followLocations, double range, int color, String labeltext)
    {
    }

    /**
     * State object for {@link GlobeRangeFanFeatureProvider}.
     */
    public record GlobeRangeFanState(ILocation location, boolean isVisible, double heading, boolean followTerrain, double sweep, double sampleDistance, boolean showEdges, boolean showOrientation, boolean showRangeValues, List<RangeRingState> rings)
    {
    }

    /**
     * Holds preference monitors to prevent repeated calls to {@link gov.takx.api.preferences.ITypedPreferenceValue#getValueAs(Class)}
     * and the subsequent String -> Object conversions.
     *
     * @since 5.5
     */
    private static class Monitors
    {
        private final IPreferenceMonitor<RangeUnits> rangeUnitsMonitor = UserPreferenceEntry.DISPLAY_RANGE_UNITS.newMonitor(RangeUnits.class, v -> {
        });
    }
}
