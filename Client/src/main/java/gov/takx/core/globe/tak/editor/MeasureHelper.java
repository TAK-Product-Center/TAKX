package gov.takx.core.globe.tak.editor;

import gov.nasa.worldwind.geom.Position;
import gov.nasa.worldwind.globes.Earth;
import gov.nasa.worldwind.globes.Globe;
import gov.nasa.worldwind.util.measure.AreaMeasurer;
import gov.tak.api.engine.map.coords.GeoCalculations;
import gov.tak.api.engine.map.coords.IGeoPoint;
import gov.takx.api.coordinatesystem.CoordinateSystem;
import gov.takx.api.coordinatesystem.ILocation;
import gov.takx.api.preferences.IPreferenceMonitor;
import gov.takx.api.shapes.IShape;
import gov.takx.dataservices.shapes.Arrow;
import gov.takx.dataservices.shapes.Circle;
import gov.takx.dataservices.shapes.Ellipse;
import gov.takx.dataservices.shapes.Polygon;
import gov.takx.dataservices.shapes.Polyline;
import gov.takx.dataservices.shapes.Rectangle;
import gov.takx.dataservices.shapes.Square;
import gov.takx.platform.preferences.UserPreferenceEntry;
import gov.takx.platform.utils.ShapeUtils;
import gov.takx.platform.utils.WWUtils;
import gov.takx.platform.utils.units.AltitudeUnits;
import gov.takx.platform.utils.units.BearingUnits;
import gov.takx.platform.utils.units.RangeUnits;
import jakarta.annotation.Nullable;

import java.util.ArrayList;
import java.util.List;

/**
 * Utility class to produce measurement text.
 *
 * @since 5.4
 */
final class MeasureHelper
{
    private static final Globe EARTH = new Earth();
    private static final Monitors MONITORS = new Monitors();
    private static BearingUnits bearingUnits = MONITORS.bearingUnitsMonitor.getCurrentValue();
    private static final AreaMeasurer areaMeasurer = new AreaMeasurer();

    static
    {
        areaMeasurer.setFollowTerrain(true);
    }

    /**
     * Get the measurement text for a control point that represents the provided {@link IShape}.
     * @param shapeModel the shape model
     * @param controlPoint the control point
     * @return the measurement text, or {@link null} if there is none
     */
    @Nullable
    static String formatGlobeShapeMeasurements(IShape shapeModel, IControlPoint controlPoint)
    {
        final StringBuilder sb = new StringBuilder();
        sb.append(shapeModel.getName()).append("\n");
        if (shapeModel instanceof Circle circle)
        {
            formatBasicMeasurementsBuilder(ShapeUtils.getEllipseLocations(circle.getLocation(), circle.getRadius(), circle.getRadius(), circle.getOrientation()), sb);
            formatCircleMeasurements(circle, controlPoint, sb);
        } else if (shapeModel instanceof Ellipse ellipse)
        {
            formatBasicMeasurementsBuilder(ellipse.getVertices(), sb);
            formatEllipseMeasurements(ellipse, controlPoint, sb);
        } else if (shapeModel instanceof Rectangle rectangle)
        {
            // Rectangle.getVertices() returns four points - we want a closed shape for formatting
            List<ILocation> vertices = ShapeUtils.closeLocations(rectangle.getVertices());
            formatBasicMeasurementsBuilder(vertices, sb);
            formatQuadMeasurements(rectangle, controlPoint, sb);
        } else if (shapeModel instanceof Polygon polygon)
        {
            formatBasicMeasurementsBuilder(polygon.getVertices(), sb);
            formatPolygonMeasurements(polygon, controlPoint, sb);
        } else if (shapeModel instanceof Polyline polyline)
        {
            formatLineMeasurements(polyline.getVertices(), controlPoint, sb);
        } else if (shapeModel instanceof Arrow arrow)
        {
            formatLineMeasurements(arrow.getVertices(), controlPoint, sb);
        }

        return sb.isEmpty() ? null : sb.toString().trim();
    }

    private static void formatCircleMeasurements(Circle circle, IControlPoint controlPoint, StringBuilder sb)
    {
        sb.append("Radius ").append(MONITORS.rangeUnitsMonitor.getCurrentValue().format(circle.getRadius(), 1)).append('\n');

        formatHeight(circle, sb);
        sb.append("Heading ").append(bearingUnits.format(circle.getLocation(), circle.getRadius(), circle.getOrientation()));
        sb.append("\n");
        formatLocation(controlPoint, sb);
    }

    private static void formatEllipseMeasurements(Ellipse ellipse, IControlPoint controlPoint, StringBuilder sb)
    {
        final var rangeUnits = MONITORS.rangeUnitsMonitor.getCurrentValue();
        sb.append("Major ").append(rangeUnits.format(ellipse.getSemiMajorAxis() * 2, 1)).append('\n');
        sb.append("Minor ").append(rangeUnits.format(ellipse.getSemiMinorAxis() * 2, 1)).append('\n');
        formatHeight(ellipse, sb);
        sb.append("Heading ").append(bearingUnits.format(ellipse.getLocation(), ellipse.getSemiMinorAxis(), ellipse.getOrientation()));
        sb.append("\n");
        formatLocation(controlPoint, sb);
    }

    private static void formatQuadMeasurements(Rectangle rectangle, IControlPoint controlPoint, StringBuilder sb)
    {
        final var rangeUnits = MONITORS.rangeUnitsMonitor.getCurrentValue();
        sb.append("Width ").append(rangeUnits.format(rectangle.getWidth(), 1)).append('\n');
        if (!(rectangle instanceof Square)) sb.append("Length ").append(rangeUnits.format(rectangle.getHeight(), 1)).append('\n');
        formatHeight(rectangle, sb);
        sb.append("Heading ").append(bearingUnits.format(rectangle.getLocation(), rectangle.getHeight(), rectangle.getOrientation()));
        sb.append("\n");
        formatLocation(controlPoint, sb);
    }

    private static void formatPolygonMeasurements(Polygon polygon, IControlPoint controlPoint, StringBuilder sb)
    {
        formatHeight(polygon, sb);
        formatLocation(controlPoint, sb);
    }

    private static void formatLineMeasurements(List<ILocation> locations, IControlPoint controlPoint, StringBuilder sb)
    {
        final var rangeUnits = MONITORS.rangeUnitsMonitor.getCurrentValue();
        int index = Integer.parseInt(controlPoint.getControlPointType());
        double length = computeAccumulatedLength(locations);
        sb.append("Length ").append(rangeUnits.format(length, 1)).append('\n');
        double accumLength = computeAccumulatedLength(locations.subList(0, index + 1));
        if (accumLength >= 1 && !lengthsEssentiallyEqual(length, accumLength))
        {
            sb.append("Accumulated ").append(rangeUnits.format(accumLength, 1)).append('\n');
        }

        if (locations.size() > index + 1)
        {
            final IGeoPoint point1 = locations.get(index);
            double bearing = GeoCalculations.bearing(point1, locations.get(index + 1));
            sb.append("Heading ").append(bearingUnits.format(point1, length, bearing)).append('\n');
        }

        formatLocation(controlPoint, sb);
    }

    private static double computeAccumulatedLength(List<ILocation> locations)
    {
        double length = 0;
        ILocation lastLocation = null;
        for (ILocation location : locations)
        {
            if (lastLocation != null)
            {
                length += GeoCalculations.distance(lastLocation, location);
            }
            lastLocation = location;
        }
        return length;
    }

    private static void formatBasicMeasurementsBuilder(List<ILocation> locations, StringBuilder sb)
    {
        ArrayList<Position> positions = new ArrayList<>(locations.stream().map(WWUtils::locationToPosition).toList());
        // Cache repeated requests
        if (areaMeasurer.getPositions() == null || !areaMeasurer.getPositions().equals(positions))
        {
            areaMeasurer.setPositions(positions);
        }
        double area = areaMeasurer.getArea(EARTH);
        double length = areaMeasurer.getLength(EARTH);

        final var rangeUnits = MONITORS.rangeUnitsMonitor.getCurrentValue();
        sb.append("Area ").append(rangeUnits.formatArea(area, 1)).append('\n');
        sb.append("Perimeter ").append(rangeUnits.format(length, 1)).append('\n');
    }

    /**
     * Add the formatted height, if any, to the builder.
     */
    private static void formatHeight(IShape shape, StringBuilder sb)
    {
        if (shape.canUseVerticalHeight())
        {
            sb.append("Height ");
            sb.append(shape.isVerticalHeightEnabled()
                    ? MONITORS.altitudeUnitsMonitor.getCurrentValue().format(shape.getVerticalHeight(), 1)
                    : "N/A");
            sb.append("\n");
        }
    }

    /**
     * Format the given location (lat/lon) according to the current coordinate system preference.  A newline is
     * appended to the formatted string.
     *
     * @param controlPoint the control point
     * @param sb  the StringBuilder to populate with the results
     */
    private static void formatLocation(IControlPoint controlPoint, StringBuilder sb)
    {
        boolean isCenter = IControlPoint.CONTROL_POINT_CENTER.equals(controlPoint.getControlPointType());
        if (isCenter) sb.append("Center").append(' ');

        final var coordinateSystem = MONITORS.coordinateSystemMonitor.getCurrentValue();
        sb.append(CoordinateSystem.getLocationString(controlPoint.getLocation().getLatitude(), controlPoint.getLocation().getLongitude(), coordinateSystem))
                .append('\n');
    }

    private static boolean lengthsEssentiallyEqual(double l1, double l2)
    {
        return Math.abs(l1 - l2) / l1 < 0.001; // equal to within a millimeter
    }

    /**
     * Holds preference monitors to prevent repeated calls to {@link gov.takx.api.preferences.ITypedPreferenceValue#getValueAs(Class)}
     * and the subsequent String -> Object conversions.
     *
     * @since 5.5
     */
    private static class Monitors
    {
        private final IPreferenceMonitor<AltitudeUnits> altitudeUnitsMonitor = UserPreferenceEntry.DISPLAY_ALTITUDE_UNITS.newMonitor(AltitudeUnits.class, u -> {
        });
        private final IPreferenceMonitor<BearingUnits> bearingUnitsMonitor = UserPreferenceEntry.DISPLAY_BEARING_UNIT.newMonitor(BearingUnits.class, u -> bearingUnits = u);
        private final IPreferenceMonitor<CoordinateSystem> coordinateSystemMonitor = UserPreferenceEntry.COORDINATE_SYSTEM.newMonitor(CoordinateSystem.class, u -> {
        });
        private final IPreferenceMonitor<RangeUnits> rangeUnitsMonitor = UserPreferenceEntry.DISPLAY_RANGE_UNITS.newMonitor(RangeUnits.class, u -> {
        });
    }
}
