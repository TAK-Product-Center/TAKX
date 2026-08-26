package gov.takx.platform.utils;

import com.atakmap.map.elevation.ElevationManager;
import com.atakmap.map.projection.ECEFProjection;
import com.atakmap.math.Vector3D;
import gov.nasa.worldwind.geom.Angle;
import gov.nasa.worldwind.geom.LatLon;
import gov.nasa.worldwind.geom.Sector;
import gov.nasa.worldwind.globes.Earth;
import gov.tak.api.annotation.DeprecatedApi;
import gov.tak.api.engine.map.coords.GeoCalculations;
import gov.tak.api.engine.map.coords.IGeoPoint;
import gov.tak.api.engine.map.coords.IProjection;
import gov.tak.api.engine.math.PointD;
import gov.tak.api.maps.coords.GeoPoint;
import gov.tak.platform.engine.map.coords.ProjectionFactory;
import gov.takx.api.coordinatesystem.ILocation;
import gov.takx.api.coordinatesystem.RaptorLocation;
import gov.takx.api.shapes.IEllipse;
import gov.takx.api.shapes.IIrregularShape;
import gov.takx.api.shapes.IPolyline;
import gov.takx.api.shapes.IRectangle;
import gov.takx.api.shapes.IShape;
import gov.takx.dataservices.shapes.Rectangle;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.naming.OperationNotSupportedException;

import java.lang.invoke.MethodHandles;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 */
@SuppressWarnings("UnusedDeclaration")
public class ShapeUtils
{
    private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    /**
     * The ECEF projection, used for intercept approximations.
     */
    private static final IProjection ECEF = ProjectionFactory.getProjection(ECEFProjection.INSTANCE.getSpatialReferenceID());

    /**
     * Index of the "min" (lower left) corner in the array returned from {@link Sector#getCorners()}.
     * @since 5.2
     */
    public static final int MIN_CORNER = 0;

    /**
     * Index of the "max" (upper right) corner in the array returned from {@link Sector#getCorners()}.
     * @since 5.2
     */
    public static final int MAX_CORNER = 2;

    /**
     * ellipsoid equatorial radius, in meters.
     */
    public static final double RADIUS_OF_EARTH = Earth.WGS84_EQUATORIAL_RADIUS;

    // Number of intervals to use to calculate ellipse locations.
    private static final int ELLIPSE_INTERVALS = 32;

    /**
     * A near zero threshold used in some of the rhumb line calculations where floating point calculations cause errors.
     * @since 5.0
     */
    private final static double NEAR_ZERO_THRESHOLD = 1e-15;

    /**
     * Calculate the distance from one location to another.
     *
     * @param p1 the starting point
     * @param p2 the point to measure to
     * @return the distance in meters
     * @since 3.0
     */
    @SuppressWarnings("DuplicatedCode")
    public static double distanceTo(ILocation p1, ILocation p2)
    {
        return GeoCalculations.distance(p1, p2);
    }

    /**
     * Convenience method to calculate a bearing from one location to another.
     *
     * @param p1 the starting point
     * @param p2 the point to measure to
     * @return the bearing from p1 to p2
     * @since 3.0
     */
    @SuppressWarnings("DuplicatedCode")
    public static double bearingTo(ILocation p1, ILocation p2)
    {
        return GeoCalculations.bearing(p1, p2);
    }

    /**
     * Computes the location on a great circle arc with the given starting location, heading, and distance.
     * This method uses a spherical model, not elliptical.
     *
     * @param startingPoint Initial location
     * @param distance      Arc distance to travel, in meters
     * @param heading       Heading in degrees (clockwise from North)
     * @return Ending location on the great circle arc
     * @see LatLon#greatCircleEndPosition(LatLon, double, double)
     * @since 3.0
     */
    public static ILocation greatCircleEndPosition(ILocation startingPoint, double distance, double heading)
    {
        final var point = GeoCalculations.pointAtDistance(startingPoint, heading, distance);
        return new RaptorLocation(point.getLatitude(), point.getLongitude());
    }

    /**
     * Computes the great circle <b>angular</b> distance (in radians) between two locations. The return value gives the distance as the
     * angle between the two positions on the pi radius circle. This angle is also the arc length of the
     * segment between the two positions on that circle. To compute a distance in meters from this value, multiply it by
     * the radius of the globe.
     * This method uses a spherical model, not elliptical.
     *
     * @param startPoint the starting coordinate
     * @param endPoint   the ending coordinate
     *
     * @return the angular distance (in radians) between the two locations - this value is the arc length on the radius pi
     * circle.
     *
     * @since 5.0
     */
    public static double greatCircleDistance(@Nonnull IGeoPoint startPoint, @Nonnull IGeoPoint endPoint)
    {
        double lat1 = Math.toRadians(startPoint.getLatitude());
        double lon1 = Math.toRadians(startPoint.getLongitude());
        double lat2 = Math.toRadians(endPoint.getLatitude());
        double lon2 = Math.toRadians(endPoint.getLongitude());

        if (lat1 == lat2 && lon1 == lon2) return 0;

        // "Haversine formula," taken from http://en.wikipedia.org/wiki/Great-circle_distance#Formul.C3.A6
        double a = Math.sin((lat2 - lat1) / 2.0);
        double b = Math.sin((lon2 - lon1) / 2.0);
        double c = a * a + Math.cos(lat1) * Math.cos(lat2) * b * b;
        double distanceRadians = 2.0 * Math.asin(Math.sqrt(c));

        return Double.isNaN(distanceRadians) ? 0 : distanceRadians;
    }

    /**
     * Computes the azimuth angle (clockwise from North, in degrees) that points from the first location to the second location.
     * This angle can be used as the starting azimuth for a great circle arc that begins at the first location, and
     * passes through the second location.
     * This method uses a spherical model, not elliptical.
     *
     * @param startPoint the starting coordinate
     * @param endPoint   the ending coordinate
     *
     * @return an angle (in degrees) that points from the first location to the second location.
     *
     * @since 5.0
     */
    public static double greatCircleAzimuth(@Nonnull IGeoPoint startPoint, @Nonnull IGeoPoint endPoint)
    {
        double lat1 = Math.toRadians(startPoint.getLatitude());
        double lon1 = Math.toRadians(startPoint.getLongitude());
        double lat2 = Math.toRadians(endPoint.getLatitude());
        double lon2 = Math.toRadians(endPoint.getLongitude());

        if (lat1 == lat2 && lon1 == lon2) return 0;
        if (lon1 == lon2) return lat1 > lat2 ? 180 : 0;

        // Taken from "Map Projections - A Working Manual", page 30, equation 5-4b.
        // The atan2() function is used in place of the traditional atan(y/x) to simplify the case when x==0.
        double y = Math.cos(lat2) * Math.sin(lon2 - lon1);
        double x = Math.cos(lat1) * Math.sin(lat2) - Math.sin(lat1) * Math.cos(lat2) * Math.cos(lon2 - lon1);
        double azimuthRadians = Math.atan2(y, x);

        return Double.isNaN(azimuthRadians) ? 0 : Math.toDegrees(azimuthRadians);
    }

    /**
     * Calculate the midpoint between two coordinates.
     *
     * @param startPoint the starting coordinate
     * @param endPoint   the ending coordinate
     * @return the midpoint
     * @since 3.0
     */
    public static ILocation calculateCenterPoint(ILocation startPoint, ILocation endPoint)
    {
        final var point = GeoCalculations.midpoint(startPoint, endPoint);
        return new RaptorLocation(point.getLatitude(), point.getLongitude());
    }

    /**
     * Calculate the midpoint between two coordinates.
     *
     * @param startPoint the starting coordinate
     * @param endPoint   the ending coordinate
     * @return the midpoint
     * @since 3.0
     */
    public static LatLon calculateCenterPoint(LatLon startPoint, LatLon endPoint)
    {
        final Angle distance = LatLon.greatCircleDistance(startPoint, endPoint);
        final Angle azimuth = LatLon.greatCircleAzimuth(startPoint, endPoint);

        return LatLon.greatCircleEndPosition(startPoint, azimuth, distance.divide(2));
    }

    /**
     * Calculate the height and width between two coordinates in meters.
     *
     * @param cornerPoint the corner coordinate
     * @param centerPoint the center coordinate
     * @return an array containing {@code [width, height]}
     * @since 3.0
     */
    public static double[] calculateDimensions(ILocation cornerPoint, ILocation centerPoint)
    {
        return calculateDimensions(WWUtils.locationToLatLon(cornerPoint), WWUtils.locationToLatLon(centerPoint));
    }

    /**
     * Calculate the height and width between two coordinates in meters.
     *
     * @param cornerPoint the corner coordinate
     * @param centerPoint the center coordinate
     * @return an array containing {@code [width, height]}
     * @since 3.0
     */
    public static double[] calculateDimensions(LatLon cornerPoint, LatLon centerPoint)
    {
        final Angle distance = LatLon.greatCircleDistance(centerPoint, cornerPoint);
        final Angle azimuth = LatLon.greatCircleAzimuth(centerPoint, cornerPoint);
        final double diagonal = distance.getRadians() * getRadiusAt(centerPoint.latitude.degrees) * 2;

        return new double[]{diagonal * Math.abs(azimuth.sin()), diagonal * Math.abs(azimuth.cos())};
    }

    /**
     * Given a list of vertices (ordered UL, LL, LR, UR), return a rectangle with the appropriate dimensions and orientation.
     * Note that this ordering is different from that provided by {@link #getLocations(IRectangle)}, which returns LL, LR, UR, UL.
     * The returned rectangle will have a blank name.
     *
     * @param vertices Corner vertices of the rectangle
     * @return Rectangle with center, height, width, and orientation calculated from the point geometry
     * @since 3.5
     * @deprecated since 5.7, removeAt 5.10
     */
    @Deprecated(since = "5.7", forRemoval = true)
    @DeprecatedApi(since = "5.7", forRemoval = true, removeAt = "5.10")
    public static Rectangle calculateRectangleFromCotPoints(List<? extends ILocation> vertices)
    {
        return calculateRectangleFromCotPoints("", vertices);
    }

    /**
     * Given a list of vertices (ordered UL, LL, LR, UR), return a rectangle with the appropriate dimensions and orientation.
     * Note that this ordering is different from that provided by {@link #getLocations(IRectangle)}, which returns LL, LR, UR, UL.
     *
     * @param name Rectangle name
     * @param vertices Corner vertices of the rectangle
     * @return Rectangle with center, height, width, and orientation calculated from the point geometry
     * @since 5.2
     * @deprecated since 5.7, removeAt 5.10
     */
    @Deprecated(since = "5.7", forRemoval = true)
    @DeprecatedApi(since = "5.7", forRemoval = true, removeAt = "5.10")
    public static Rectangle calculateRectangleFromCotPoints(@Nonnull String name, @Nonnull List<? extends ILocation> vertices)
    {
        final ILocation ul = vertices.get(0);
        final ILocation ll = vertices.get(1);
        final ILocation lr = vertices.get(2);
        final ILocation ur = vertices.get(3);

        final var centerL = GeoCalculations.midpoint(lr, ll);
        final var centerU = GeoCalculations.midpoint(ur, ul);
        final var centerGP = GeoCalculations.midpoint(centerL, centerU);
        final var center = new GeoPoint(centerGP.getLatitude(), centerGP.getLongitude());
        final double orientation = GeoCalculations.bearing(center, centerU);
        final double height = GeoCalculations.distance(centerL, centerU);
        final double width = GeoCalculations.distance(ll, lr);

        return new Rectangle(name, center, orientation, height, width);
    }

    /**
     * Given a min and max corner return a rectangle with the appropriate dimensions and a 0 orientation.
     * The returned rectangle will have a blank name.
     *
     * @param minCorner        Location of the minimum corner
     * @param maxCorner        Location of the maximum corner
     * @return Rectangle with center, height, width calculated from the point geometry
     * @since 5.2
     */
    public static Rectangle calculateRectangleFromMinMaxCorners(@Nonnull ILocation minCorner, @Nonnull ILocation maxCorner)
    {
        return calculateRectangleFromMinMaxCorners("", minCorner, maxCorner);
    }

    /**
     * Given a min and max corner return a rectangle with the appropriate dimensions and a 0 orientation.
     *
     * @param name Rectangle name
     * @param minCorner Location of the minimum corner
     * @param maxCorner Location of the maximum corner
     * @return Rectangle with center, height, width calculated from the point geometry
     * @since 5.2
     */
    public static Rectangle calculateRectangleFromMinMaxCorners(@Nonnull String name, @Nonnull ILocation minCorner, @Nonnull ILocation maxCorner)
    {
        // Determine the centroid, height, and width of the corresponding rectangle
        final Sector sector = Sector.fromDegrees(minCorner.getLatitude(), maxCorner.getLatitude(),
                minCorner.getLongitude(), maxCorner.getLongitude());
        final double sectorLengthRadians = sector.getDeltaLatRadians();

        // Ensure that the bounds are valid
        // The longitude can only be checked for general boundaries due to the fact that across the International
        // Date Line the minimum longitude will actually be larger than the maximum.
        if (sectorLengthRadians <= 0 || minCorner.getLongitude() <= -180.0 || maxCorner.getLongitude() >= 180.0)
        {
            throw new IllegalArgumentException("rectangle min corner must be less than max corner");
        }

        return new Rectangle(name, List.of(minCorner, new GeoPoint(minCorner.getLatitude(), maxCorner.getLongitude()), maxCorner, new GeoPoint(maxCorner.getLatitude(), minCorner.getLongitude())));
    }

    /**
     * Given a {@code bearingDelta}, calculate a new list of points rotating each of the given {@code points} around the {@code centerPoint}.
     *
     * @param centerPoint The center point
     * @param points Points to rotate
     * @param bearingDelta The change in bearing to apply to the points
     * @param altitudeMode The {@link IShape.AltitudeMode} to use for the points
     * @return new list of points rotated according to the bearing delta
     * @since 5.7
     */
    public static List<? extends ILocation> rotatePoints(@Nonnull ILocation centerPoint, @Nonnull List<? extends ILocation> points, double bearingDelta, @Nonnull IShape.AltitudeMode altitudeMode)
    {
        if (points.isEmpty()) return points;

        return points.stream()
                .map(l -> {
                    double bearing = GeoCalculations.bearing(centerPoint, l);
                    double distance = GeoCalculations.distance(centerPoint, l);
                    IGeoPoint newVertex = GeoCalculations.pointAtDistance(centerPoint, bearing + bearingDelta, distance);
                    // Check for IDL crossing
                    double longitude = newVertex.getLongitude();
                    longitude = longitude < -180d ? longitude + 360 : longitude;
                    longitude = longitude > 180d ? longitude - 360 : longitude;
                    return switch (altitudeMode)
                    {
                        case ClampToGround -> new GeoPoint(newVertex.getLatitude(), longitude, ElevationManager.getElevation(newVertex.getLatitude(), longitude, null));
                        case Absolute, Relative -> new GeoPoint(newVertex.getLatitude(), longitude, l.getAltitude());
                    };
                }).toList();
    }

    /**
     * Given a new starting location, calculate a new list of points moving each point in the given {@code points}
     * along a bearing and distance calculated from the old start point to the new one. Assumes an altitude mode of {@link IShape.AltitudeMode#Absolute}.
     *
     * @param newLocation New starting location for the points
     * @param points Points to move
     * @return new list of points moved according to the range and bearing difference from the first point in {@code points} to {@code refEndPoint}
     * @see #movePoints(ILocation, List, IShape.AltitudeMode)
     * @since 5.5
     * @deprecated since 5.7, removeAt 5.10
     */
    @Deprecated(since = "5.7", forRemoval = true)
    @DeprecatedApi(since = "5.7", removeAt = "5.10", forRemoval = true)
    public static List<? extends ILocation> movePoints(@Nonnull ILocation newLocation, @Nonnull List<? extends ILocation> points)
    {
        return movePoints(newLocation, points, IShape.AltitudeMode.Absolute);
    }

    /**
     * Given a new starting location, calculate a new list of points moving each point in the given {@code points}
     * along a bearing and distance calculated from the old start point to the new one.
     *
     * @param newLocation New starting location for the points
     * @param points Points to move
     * @param altitudeMode The {@link IShape.AltitudeMode} to use for the points
     * @return new list of points moved according to the range and bearing difference from the first point in {@code points} to {@code refEndPoint}
     * @since 5.7
     */
    public static List<? extends ILocation> movePoints(@Nonnull ILocation newLocation, @Nonnull List<? extends ILocation> points, @Nonnull IShape.AltitudeMode altitudeMode)
    {
        if (points.isEmpty()) return points;

        final var startPoint = points.get(0);
        final double bearing = GeoCalculations.bearing(startPoint, newLocation);
        final double distance = GeoCalculations.distance(startPoint, newLocation);

        return points.stream()
                .map(l -> {
                    IGeoPoint geoPoint = l.equals(startPoint) ? newLocation : GeoCalculations.pointAtDistance(l, bearing, distance);
                    double endLat = geoPoint.getLatitude();
                    double endLon = geoPoint.getLongitude();
                    return switch (altitudeMode)
                    {
                        case ClampToGround -> new GeoPoint(endLat, endLon, ElevationManager.getElevation(endLat, endLon, null));
                        case Absolute, Relative -> new GeoPoint(endLat, endLon, l.getAltitude());
                    };
                }).toList();
    }

    /**
     * Calculate the radius of the earth at a given latitude.
     * Stolen from WW to avoid needing a reference to a Globe instance.
     *
     * @param latitude in degrees
     * @return radius in meters
     * @since 3.0
     */
    public static double getRadiusAt(double latitude)
    {
        // The radius for an ellipsoidal globe is a function of its latitude. The following solution was derived by
        // observing that the length of the ellipsoidal point at the specified latitude and longitude indicates the
        // radius at that location. The formula for the length of the ellipsoidal point was then converted into the
        // simplified form below.

        final double es = Earth.WGS84_ES;
        final double sinLat = Math.sin(Math.toRadians(latitude));
        final double sinLatSquared = sinLat * sinLat;
        final double rpm = Earth.WGS84_EQUATORIAL_RADIUS / Math.sqrt(1.0 - es * sinLatSquared);

        return rpm * Math.sqrt(1.0 + (es * es - 2.0 * es) * sinLatSquared);
    }

    /**
     * Compute the locations of the corners of a rectangle.
     *
     * @return List of corner locations, or {@code null} if no size has been set on the shape
     * @since 3.3
     * @deprecated since 5.7, removeAt 5.10 use {@link IRectangle#getVertices()}
     */
    @Nullable
    @Deprecated(since = "5.7", forRemoval = true)
    @DeprecatedApi(since = "5.7", forRemoval = true, removeAt = "5.10")
    public static List<ILocation> getLocations(@Nonnull IRectangle rectangle)
    {
        double halfWidth = rectangle.getWidth() / 2;
        double halfHeight = rectangle.getHeight() / 2;
        if (halfWidth == 0 && halfHeight == 0) return null;

        final ILocation location = rectangle.getLocation();
        final double orientation = rectangle.getOrientation();

        IGeoPoint northCenter = GeoCalculations.pointAtDistance(location, orientation, halfHeight);
        IGeoPoint northWest = GeoCalculations.pointAtDistance(northCenter, orientation - 90, halfWidth);
        IGeoPoint northEast = GeoCalculations.pointAtDistance(northCenter, orientation + 90, halfWidth);
        IGeoPoint southCenter = GeoCalculations.pointAtDistance(location, orientation + 180, halfHeight);
        IGeoPoint southWest = GeoCalculations.pointAtDistance(southCenter, orientation - 90, halfWidth);
        IGeoPoint southEast = GeoCalculations.pointAtDistance(southCenter, orientation + 90, halfWidth);

        final List<ILocation> locations = new ArrayList<>();

        double elevation = location.getElevation();
        locations.add(new RaptorLocation(southWest.getLatitude(), southWest.getLongitude(), elevation));
        locations.add(new RaptorLocation(southEast.getLatitude(), southEast.getLongitude(), elevation));
        locations.add(new RaptorLocation(northEast.getLatitude(), northEast.getLongitude(), elevation));
        locations.add(new RaptorLocation(northWest.getLatitude(), northWest.getLongitude(), elevation));

        return locations;
    }

    /**
     * Compute the locations along the boundary of an ellipse.  The outline is approximated using 32 points.
     *
     * @return List of locations, or {@code null} if no size has been set on the shape
     * @since 3.3
     */
    @Nullable
    public static List<ILocation> getLocations(@Nonnull IEllipse ellipse)
    {
        final double majorRadius = ellipse.getSemiMajorAxis();
        final double minorRadius = ellipse.getSemiMinorAxis();
        if (majorRadius == 0 && minorRadius == 0) return null;

        return getEllipseLocations(ellipse.getLocation(), majorRadius, minorRadius, ellipse.getOrientation());
    }

    /**
     * Compute the locations along the boundary of an ellipse.  The outline is approximated using 32 points.
     *
     * @return List of locations
     * @since 5.5
     */
    @Nonnull
    public static List<ILocation> getEllipseLocations(ILocation centerLocation, double majorRadius, double minorRadius, double orientation)
    {
        final int numLocations = 1 + ELLIPSE_INTERVALS;
        final double da = (2 * Math.PI) / (numLocations - 1);
        final double globeRadius = getRadiusAt(centerLocation.getLatitude());
        final double heading = Math.toRadians(orientation);
        final List<ILocation> locations = new ArrayList<>();

        for (int i = 0; i < numLocations; i++)
        {
            final double angle = (i != numLocations - 1) ? i * da : 0;
            final double xLength = majorRadius * Math.cos(angle);
            final double yLength = minorRadius * Math.sin(angle);
            final double distance = Math.sqrt(xLength * xLength + yLength * yLength);
            final double pathLengthRadians = distance / globeRadius;

            // azimuth runs positive clockwise from north and through 360 degrees.
            final double azimuth = (Math.PI / 2.0) - (Math.acos(xLength / distance) * Math.signum(yLength) - heading);

            ILocation ringLocation = new GeoPoint(GeoCalculations.pointAtDistance(centerLocation, Math.toDegrees(azimuth), distance));
            locations.add(ringLocation);
        }

        return locations;
    }

    /**
     * Used to determine the number to shift all latitudes by in order to make sure that they are all positive numbers.
     *
     * @param polygonVertices The locations containing the latitudes to shift.
     * @return The shift amount needed to make the latitudes positive
     */
    public static double getLatShiftAmountForPolygon(List<ILocation> polygonVertices)
    {
        double lowestLat = Double.MAX_VALUE;

        for (ILocation polygonVertex : polygonVertices)
        {
            final double latitude = polygonVertex.getLatitude();
            if (latitude < 0 && latitude < lowestLat)
            {
                lowestLat = latitude;
            }
        }

        return lowestLat < 0 ? Math.abs(lowestLat) : 0;
    }

    /**
     * Used to determine the number to shift all longitudes by in order to make sure that they are all positive numbers.
     *
     * @param polygonVertices The locations containing the longitudes to shift.
     * @return The shift amount needed to make the longitudes positive
     */
    public static double getLonShiftAmountForPolygon(List<ILocation> polygonVertices)
    {
        double lowestLon = Double.MAX_VALUE;

        for (ILocation polygonVertex : polygonVertices)
        {
            final double longitude = polygonVertex.getLongitude();
            if (longitude < 0 && longitude < lowestLon)
            {
                lowestLon = longitude;
            }
        }

        return lowestLon < 0 ? Math.abs(lowestLon) : 0;
    }

    /**
     * Returns true if the rectangle spans the International Date Line
     * This assumes that the vertices are ordered clockwise starting at the lower left corner.
     * This is limited to rectangles because we have no way to know the ordering of other polygons vertices from left to
     * right, which means we can not determine if it spans the IDL.
     *
     * @param rectangleVertices The vertices of the rectangle
     * @return true if it spans the IDL false otherwise
     */
    public static boolean rectangleSpansDateLineMeridian(List<ILocation> rectangleVertices)
    {
        if (rectangleVertices == null || rectangleVertices.size() != 4)
        {
            return false;
        }

        // Make sure the 4 sided polygon has the same longitudes for the left and right sides and the same lats for the
        // top and bottom sides
        final ILocation firstVertex = rectangleVertices.get(0);
        final ILocation thirdVertex = rectangleVertices.get(3);
        if (((firstVertex.getLongitude() != rectangleVertices.get(1).getLongitude()) || (rectangleVertices.get(2).getLongitude() != thirdVertex.getLongitude())))
        {
            return false;
        }
        if ((firstVertex.getLatitude() != thirdVertex.getLatitude()) || (rectangleVertices.get(1).getLatitude() != rectangleVertices.get(2).getLatitude()))
        {
            throw new IllegalArgumentException("This 4 sided polygon is not a rectangle. The vertices longitudes don't match:" + rectangleVertices);
        }
        // Return true if it's a rectangle and its right longitude is less than its left longitude
        {
            return thirdVertex.getLongitude() < firstVertex.getLongitude();
        }
    }

    /**
     * Check to see if one polygon contains another, i.e. all the contained polygon's point exist within the bounds
     * of the container polygon.
     *
     * @param containerPolygonVertices The container polygon's vertices.
     * @param shape                    The contained shape.
     * @return {@code true} iff the polygon represented by {@code containerPolygonVertices} completely
     * contains the polygon represented by {@code containedPolygonVertices}.
     */
    public static boolean polygonContainsShape(List<ILocation> containerPolygonVertices, IShape shape)
            throws OperationNotSupportedException
    {
        if (containerPolygonVertices == null) return false;

        if (shape instanceof IIrregularShape irregularShape)
        {
            return polygonContainsShape(containerPolygonVertices, irregularShape.getVertices());
        } else if (shape instanceof IRectangle rectangle)
        {
            ILocation minCorner = rectangle.getMinCorner();
            ILocation maxCorner = rectangle.getMaxCorner();

            List<ILocation> corners;
            if (minCorner == null || maxCorner == null)
            {
                final ILocation location = shape.getLocation();
                corners = Arrays.asList(location, location, location, location);
            } else
            {
                ILocation otherCorner1 = new RaptorLocation(minCorner.getLatitude(), maxCorner.getLongitude());
                ILocation otherCorner2 = new RaptorLocation(maxCorner.getLatitude(), minCorner.getLongitude());
                corners = Arrays.asList(minCorner, otherCorner1, maxCorner, otherCorner2);
            }

            return polygonContainsShape(containerPolygonVertices, corners);
        } else if (shape instanceof IEllipse ellipse)
        {
            return polygonContainsEllipse(containerPolygonVertices, ellipse);
        } else
        {
            throw new OperationNotSupportedException(shape.getType() + " is not supported");
        }
    }

    /**
     * Check to see if one polygon contains another, i.e. all the contained polygon's points exist within the bounds
     * of the container polygon.
     *
     * @param containerPolygonVertices The container polygon's vertices.
     * @param containedShapeExtremes   The contained shape's extremes. For a polygon, this will be its vertices. For an
     *                                 ellipse, it will be the endpoints of its major and minor axes.
     * @return {@code true} iff the polygon represented by {@code containerPolygonVertices} completely
     * contains the polygon represented by {@code containedPolygonVertices}.
     */
    public static boolean polygonContainsShape(List<ILocation> containerPolygonVertices, List<ILocation> containedShapeExtremes)
    {
        if (containerPolygonVertices == null || containedShapeExtremes == null) return false;

        for (ILocation location : containedShapeExtremes)
        {
            if (!polygonContainsLocation(containerPolygonVertices, location))
            {
                return false;
            }
        }

        return true;
    }

    /**
     * Check to see if a polygon contains an ellipse.
     *
     * @param containerPolygonVertices The container polygon's vertices.
     * @param ellipse                  The ellipse.
     * @return {@code true} iff the polygon represented by {@code containerPolygonVertices} completely
     * contains the ellipse.
     */
    public static boolean polygonContainsEllipse(List<ILocation> containerPolygonVertices, IEllipse ellipse)
    {
        if (containerPolygonVertices == null) return false;

        final ILocation ellipseLocation = ellipse.getLocation();

        if (!polygonContainsLocation(containerPolygonVertices, ellipseLocation))
        {
            return false;
        }

        double minorAxis = ellipse.getSemiMinorAxis() * 2;
        double majorAxis = ellipse.getSemiMajorAxis() * 2;
        double angle = ellipse.getOrientation();

        if (minorAxis > majorAxis)
        {
            double temp = minorAxis;
            minorAxis = majorAxis;
            majorAxis = temp;
        } else
        {
            angle = (angle + 90) % 360;
        }

        for (int i = 0; i < containerPolygonVertices.size() - 1; i++)
        {
            if (ellipseIntersectsLine(ellipseLocation, minorAxis, majorAxis, angle, containerPolygonVertices.get(i), containerPolygonVertices.get(i + 1)))
            {
                return false;
            }
        }

        return true;
    }

    /**
     * A shape is considered a hole if all of its points are located within the polygon but none of the polygon's
     * vertices are contained within the shape.
     *
     * @param containerPolygonVertices The container polygon's vertices.
     * @param potentialHole            The shape that will be tested to be a hole
     * @return True if the shape is an eligible hole
     */
    public static boolean isEligibleHole(List<ILocation> containerPolygonVertices, List<ILocation> potentialHole)
    {
        if (containerPolygonVertices == null || potentialHole == null || containerPolygonVertices.size() < 3 || potentialHole.size() < 3)
        {
            return false;
        }
        // Make sure that the hole does not contain any of the polygon's vertices
        for (ILocation location : containerPolygonVertices)
        {
            if (polygonContainsLocation(potentialHole, location))
            {
                return false;
            }
        }
        return polygonContainsShape(containerPolygonVertices, potentialHole);
    }

    /**
     * Check to see if a polygons and a shape intersect, i.e. at least 1 vertex from one polygon exists in the bounds of the
     * other shape, or in the case that the shape is a polyline, that the polyline intersects the polygon.
     *
     * @param polygonVertices The polygon's vertices.
     * @param shape           The shape.
     * @return {@code true} iff the two polygons intersect.
     */
    public static boolean polygonIntersectsShape(List<ILocation> polygonVertices, IShape shape)
            throws OperationNotSupportedException
    {
        if (shape instanceof IIrregularShape)
        {
            if (shape instanceof IPolyline)
            {
                return polygonIntersectsPolyline(polygonVertices, ((IPolyline) shape).getVertices());
            } else
            {
                return polygonIntersectsShape(polygonVertices, ((IIrregularShape) shape).getVertices());
            }
        } else if (shape instanceof IRectangle)
        {
            return polygonIntersectsShape(polygonVertices, ((IRectangle) shape).getVertices());
        } else if (shape instanceof IEllipse)
        {
            return polygonIntersectsEllipse(polygonVertices, (IEllipse) shape);
        } else
        {
            throw new OperationNotSupportedException(shape.getType() + " is not supported");
        }
    }

    /**
     * Check to see if a polygons and a shape intersect, i.e. at least 1 vertices from one polygon exists in the bounds of the
     * other shape.
     * <p/>
     * todo: Make this more efficient.
     *
     * @param polygonVertices The polygon's vertices.
     * @param shapeVertices   The shape's vertices.
     * @return {@code true} iff the two polygons intersect.
     */
    public static boolean polygonIntersectsShape(List<ILocation> polygonVertices, List<ILocation> shapeVertices)
    {
        if (polygonVertices == null || shapeVertices == null || polygonVertices.size() < 3)
        {
            return false;
        }

        for (ILocation location : shapeVertices)
        {
            if (polygonContainsLocation(polygonVertices, location))
            {
                return true;
            }
        }

        for (ILocation location : polygonVertices)
        {
            if (polygonContainsLocation(shapeVertices, location))
            {
                return true;
            }
        }

        return false;
    }

    /**
     * Check to see if a polygon intersects a polyline.  Method assumes first parameter contains the polygon vertices
     * all unique (last point should not equal first point) and the second parameter contains the polyline vertices
     *
     * @param polygonVertices  polygon vertices
     * @param polylineVertices polyline vertices
     * @return {@code true} on intersection or {@code false} on no intersection.
     * @since 1.49
     */
    public static boolean polygonIntersectsPolyline(@Nonnull List<ILocation> polygonVertices,
                                                    @Nonnull List<ILocation> polylineVertices)
    {
        final int size = polygonVertices.size();

        if (size < 3 || polylineVertices.size() < 2) return false;

        for (int v = 0, end = size - 1; v < end; v++)
        {
            if (linesIntersect(polygonVertices.get(v), polygonVertices.get(v + 1), polylineVertices)) return true;
        }

        // Test the "closing" segment
        return linesIntersect(polygonVertices.get(size - 1), polygonVertices.get(0), polylineVertices);
    }

    /**
     * Determine if any segment of a polyline intersects a line from p1 to p2.
     *
     * @param p1               Starting point of line segment to test against
     * @param p2               Ending point of line segment to test against
     * @param polylineVertices Vertices of the polyline to test against p1,p2
     * @return {@code true} if any segment of the polyline intersect p1,p2
     * @since 3.0
     */
    private static boolean linesIntersect(ILocation p1, ILocation p2, List<ILocation> polylineVertices)
    {
        for (int v = 0, end = polylineVertices.size() - 1; v < end; v++)
        {
            if (intersection(p1, p2, polylineVertices.get(v), polylineVertices.get(v + 1)) != null) return true;
        }

        return false;
    }

    /**
     * Check to see if a location exists within or on the edge a polygon, as defined by its vertices.
     *
     * @param polygonVertices The locations of the polygon's vertices.
     * @param point           The location to check.
     * @return {@code true} iff the point is in or on the polygon, {@code false} otherwise
     */
    public static boolean polygonContainsLocation(List<ILocation> polygonVertices, ILocation point)
    {
        if (polygonVertices == null || point == null || polygonVertices.size() < 3)
        {
            return false;
        }

        ILocation first = polygonVertices.get(0);
        ILocation last = polygonVertices.get(polygonVertices.size() - 1);

        if (first.getLatitude() != last.getLatitude() || first.getLongitude() != last.getLongitude())
        {
            // Copy because we're going to modify the list.
            polygonVertices = new ArrayList<>(polygonVertices);

            // Make it a closed polygon.
            polygonVertices.add(polygonVertices.get(0));
        }

        ILocation[] polygonVerticesArray = new ILocation[polygonVertices.size()];
        polygonVerticesArray = polygonVertices.toArray(polygonVerticesArray);

        // Count the number of crossings in and out of the polygon.
        int windingNumberCounter = 0;

        // Loop through all the edges.
        for (int i = 0; i < polygonVertices.size() - 1; i++)
        {
            // If we are on an edge, we are contained
            if (onLineSegment(polygonVerticesArray[i], polygonVerticesArray[i + 1], point)) return true;

            // edge from V[i] to V[i+1]
            if (polygonVerticesArray[i].getLatitude() <= point.getLatitude())
            {
                // start y <= P.y
                // an upward crossing
                if (polygonVerticesArray[i + 1].getLatitude() > point.getLatitude())
                {
                    // P left of edge
                    if (isLeft(polygonVerticesArray[i], polygonVerticesArray[i + 1], point) > 0)
                    {
                        // have a valid up intersect
                        ++windingNumberCounter;
                    }
                }
            } else
            {
                // start y > P.y (no test needed)
                // a downward crossing
                if (polygonVerticesArray[i + 1].getLatitude() <= point.getLatitude())
                {
                    // P right of edge
                    if (isLeft(polygonVerticesArray[i], polygonVerticesArray[i + 1], point) < 0)
                    {
                        // have a valid down intersect
                        --windingNumberCounter;
                    }
                }
            }
        }

        return windingNumberCounter != 0;
    }

    /**
     * Determine whether a point is in an ellipse. This method does take into account the spherical earth.
     *
     * @param ellipseCenter The center of the ellipse.
     * @param minorDiameter The major axis diameter.
     * @param majorDiameter The minor axis diameter.
     * @param angle         The angle of rotation, clockwise relative to the <b>major axis</b>.
     * @param point         The point's coordinates.
     * @return {@code true} iff the point lies within the ellipse.
     */
    public static boolean ellipseContainsPoint(ILocation ellipseCenter, double minorDiameter, double majorDiameter, double angle, ILocation point)
    {
        double fociDistance = getDistanceToFoci(majorDiameter / 2d, minorDiameter / 2d);
        ILocation[] foci = getFoci(ellipseCenter, angle, fociDistance);

        return ellipseContainsPoint(fociDistance, foci, minorDiameter, majorDiameter, angle, point);
    }

    /**
     * Determine whether a point is in an ellipse. This method does take into account the spherical earth.
     *
     * @param minorDiameter The major axis diameter.
     * @param majorDiameter The minor axis diameter.
     * @param angle         The angle of rotation, clockwise relative to the <b>major axis</b>.
     * @param point         The point's coordinates.
     * @return {@code true} iff the point lies within the ellipse.
     */
    private static boolean ellipseContainsPoint(double fociDistance, ILocation[] foci, double minorDiameter, double majorDiameter, double angle, ILocation point)
    {
        double distance = 0;

        for (ILocation focus : foci)
        {
            distance += distanceTo(focus, point);
        }

        // Calculate the distance from the foci to the end of the minor axis using the simple a^2 + b^2 = c^2 formula.
        // Any point further away than twice that distance is outside the ellipse.
        return distance < Math.sqrt(Math.pow(fociDistance, 2) + Math.pow(minorDiameter / 2d, 2)) * 2;
    }

    /**
     * Determine whether a point is within a circle. This method does take into account the spherical earth.
     *
     * @param circleCenter Location of circle center
     * @param radius       radius of circle
     * @param point        point to test
     * @return {@code true} iff the point lies within (or on) the circle
     */
    public static boolean circleContainsPoint(ILocation circleCenter, double radius, ILocation point)
    {
        return distanceTo(circleCenter, point) <= radius;
    }

    private static boolean polygonIntersectsEllipse(List<ILocation> polygonVertices, IEllipse shape)
    {
        if (polygonVertices == null || polygonVertices.size() < 3)
        {
            return false;
        }

        ILocation location = shape.getLocation();

        // Ellipse center is inside the polygon.
        if (polygonContainsLocation(polygonVertices, location))
        {
            return true;
        }

        double minor = shape.getSemiMinorAxis() * 2;
        double major = shape.getSemiMajorAxis() * 2;
        double angle = shape.getOrientation();

        if (minor > major)
        {
            double temp = minor;
            minor = major;
            major = temp;
        } else
        {
            angle += 90;
        }

        // Check each side of the polygon to see if it intersects the ellipse.
        for (int i = 0; i < polygonVertices.size() - 1; i++)
        {
            if (ellipseIntersectsLine(location, minor, major, angle, polygonVertices.get(i), polygonVertices.get(i + 1)))
            {
                return true;
            }
        }

        return false;
    }

    public static boolean ellipseIntersectsLine(ILocation ellipseCenter, double minorDiameter, double majorDiameter,
                                                double angle, ILocation pointA, ILocation pointB)
    {
        final double semiMajor = 0.5 * majorDiameter;
        final double semiMinor = 0.5 * minorDiameter;

        double fociDistance = getDistanceToFoci(semiMajor, semiMinor);
        ILocation[] foci = getFoci(ellipseCenter, angle, fociDistance);
        double d = 2 * Math.hypot(fociDistance, semiMinor);

        return ellipseIntersectsLine(foci[0], foci[1], pointA, pointB, d);
    }

    /**
     * Ellipse-line intersection on the surface of a sphere.
     * <p>
     * This algorithm is based on solving a constrained minimization problem on a portion of the sphere.
     * <p>
     * the objective function is:
     * f(X) = dist(F0, X) + dist(F1, X)
     * <p>
     * and there are 3 constraints:
     * 1) X must lie on great circle connecting A and B
     * 2) X must lie on A or "toward B" from A
     * 3) X must lie on B or "toward A" from B
     * <p>
     * The objective value at the solution (there is only one, by a shortest-path argument) determines the size
     * of the smallest ellipse with foci F0 and F1 which touches the line - if it is less than or equal to d, then
     * the line and ellipse intersect.  Obviously, we need not actually find the minimum - once we find any point
     * X such that f(X) <= d, we know there's an intersection and can return immediately.
     * <p>
     * Inequality constraints 2 and 3 can be removed by pre-checking the endpoints.  That leaves one equality
     * constraint.  The gradient of f at X is the sum of equal-length direction vectors from X toward F0 and F1,
     * and this sum must be perpendicular to the great circle at X.
     * <p>
     * This reduces to a 1d root-finding problem on the great circle, finding a root of "ellipseLineLagrangian",
     * which computes the projection of the gradient of the minimization problem's Lagrange function onto the great
     * circle arc from pA to pB.  This implementation is only valid on that arc.  This function is strictly monotonic
     * on the arc as long as the arc does not pass between the foci.  In that case it can also be extended to a
     * monotonic function on most of the great circle (all but a region on the opposite side of the globe),
     * but it's not necessary here.
     * <p>
     * (I haven't proven it, but I believe that region is the segment bounded by the projections of the antipodes
     * of the foci onto the great circle, and that therefore this entire method will converge to the correct answer
     * as long as the segment does not have an endpoint within that region).
     * TODO: prove this, or at least handle that case by clipping the segment to the ellipse's bounding box first
     */
    private static double ellipseLineLagrangian(ILocation f0, ILocation f1, ILocation pA, ILocation pB, ILocation pX)
    {
        // TODO: simplify
        // this can probably be greatly simplified along the following lines:
        // b0 = bearingTo(pX, f0)
        // b1 = bearingTo(pX, f1)
        // b = bisection(b0, b1) // (angle halfway between, on the side with the sub-180 angle
        // return b - 90; // or so

        // the current implementation is based on measuring the angles A->X->F0 and F1->X->B
        // and computing their difference.  The current version is a little convoluted but I don't
        // want to change it right now because I can't currently run TAKX to test it.
        final double b0 = pX.equals(pA) ? 180.0 - angle(f0, pX, pB) : angle(pA, pX, f0);
        final double b1 = pX.equals(pB) ? 180.0 - angle(f1, pX, pA) : angle(pB, pX, f1);
        double t = b0 + b1;
        t %= 360.0;
        if (t < 0.0) t += 360.0;

        final double result = t > 180.0 ? t - 360.0 : t;

        if (logger.isTraceEnabled())
        {
            logger.trace("[f]: {}, {}, {}, {}", b0, b1, t, result);
        }

        return result;
    }

    /**
     * Measure the directed angle A->B->C.  Positive rotation is counter-clockwise,
     * opposite to the direction of positive bearing.
     */
    private static double angle(ILocation a, ILocation b, ILocation c)
    {
        final double ba = bearingTo(b, a);
        final double bc = bearingTo(b, c);

        return (ba + (360.0 - bc)) % 360.0;
    }

    // actually perform a line-ellipse intersection test as described above (in the comments by
    // "ellipseLineLagrangian").  This could be fairly easily extended to compute the actual location of the hit,
    // if needed, and from there it'd be a matter of bisection on the objective function to locate the boundary
    // of the intersection.
    private static boolean ellipseIntersectsLine(ILocation f0, ILocation f1, ILocation pA, ILocation pB, final double d)
    {
        if (logger.isTraceEnabled())
        {
            logger.trace("ellipseIntersectsLine: {}, {}, {}, {}, {}", f0, f1, pA, pB, d);
        }

        double distA = distanceTo(f0, pA) + distanceTo(f1, pA);

        if (distA <= d)
        {
            logger.trace("ellipseIntersectsLine: true (pA inside)");
            return true;
        }

        double distB = distanceTo(f0, pB) + distanceTo(f1, pB);
        if (distB <= d)
        {
            logger.trace("ellipseIntersectsLine: true (pB inside)");
            return true;
        }

        // if the line crosses the segment
        if (intersection(f0, f1, pA, pB) != null)
        {
            logger.trace("ellipseIntersectsLine: true (segments cross)");
            return true;
        }

        ILocation low = pA;
        double lowVal = ellipseLineLagrangian(f0, f1, pA, pB, low);

        ILocation high = pB;
        double highVal = ellipseLineLagrangian(f0, f1, pA, pB, high);

        if (lowVal > highVal)
        {
            ILocation tmp = low;
            double tmpVal = lowVal;

            low = high;
            lowVal = highVal;

            high = tmp;
            highVal = tmpVal;
        }

        if (lowVal > 0 || highVal < 0)
        {
            // optimum is at an endpoint, which we already checked.
            // therefore there is no intersection.
            logger.trace("ellipseIntersectsLine: false (same side)");
            return false;
        }

        double length = distanceTo(pA, pB);
        double eps = 1e-7;
        while (length > eps)
        {
            ILocation mid = calculateCenterPoint(low, high);
            double midDist = distanceTo(f0, mid) + distanceTo(f1, mid);

            logger.trace("ellipseIntersectsLine[search]: {}, {}, {}({}), {}, {}", low, lowVal, mid, midDist, high, highVal);

            if (midDist <= d)
            {
                // found a point on the line that's inside the ellipse
                logger.trace("ellipseIntersectsLine: true (found point)");

                return true;
            }

            double midVal = ellipseLineLagrangian(f0, f1, pA, pB, mid);
            if (midVal + eps < 0)
            {
                // new lower bound
                low = mid;
                // TODO: prove the bound always improves, or determine why it doesn't.
                // it appears it can occasionally move back, probably due to curvature of the sphere.
                // in practice it's not frequent, and it probably wouldn't be hard to prove that it's a benign
                // effect as long as your segment is less than half the great circle, or that it can be eliminated
                // entirely by pre-clipping the segment to a smaller region bounding the ellipse.
                if (midVal + eps < lowVal) throw new IllegalStateException((midVal + eps) + " not >= " + lowVal);
                lowVal = midVal;
            } else if (midVal - eps > 0)
            {
                // new upper bound
                high = mid;
                if (midVal - eps > highVal) throw new IllegalStateException((midVal - eps) + " not <= " + highVal);
                highVal = midVal;
            } else
            {
                // found the root and it's not good enough; bail
                break;
            }

            length *= 0.5;
        }

        logger.trace("ellipseIntersectsLine: false (exhausted search)");
        return false;
    }

    /**
     * This method iterates through each polygon vertex to see if the x,y point is within it and the previous vertex.
     * The solution is to compare each side of the polygon to the Y (vertical) coordinate of the test point, and
     * compile a list of nodes, where each node is a point where one side crosses the Y threshold of the test point.
     * A more detailed explanation can be found <a href="http://alienryderflex.com/polygon/">here</a>.
     * <p/>
     * Note that division by zero is avoided because the division is protected
     * by the "if" clause which surrounds it.
     *
     * @param xVertices The x of the vertices of the polygon used to determine if the location is contained within it.
     * @param yVertices The y of the vertices of the polygon used to determine if the location is contained within it.
     * @param x         The x coordinate of the point that will be determined whether it is within the polygon
     * @param y         The y coordinate of the point that will be determined whether it is within the polygon
     * @return The function will return true if the (x,y) point is inside the polygon, or
     * false if it is not.  If the point is exactly on the edge of the polygon,
     * then the function may return true or false.
     */
    public static boolean polygonContainsPoint(List<Double> xVertices, List<Double> yVertices, double x, double y)
    {
        if (xVertices.isEmpty() || yVertices.isEmpty() || (xVertices.size() != yVertices.size())) return false;

        int numVertices = xVertices.size();
        int previousVertex = numVertices - 1;
        boolean oddNodes = false;

        for (int vertexIndex = 0; vertexIndex < numVertices; vertexIndex++)
        {
            double polyX = xVertices.get(vertexIndex);
            double polyY = yVertices.get(vertexIndex);

            double polyXPrev = xVertices.get(previousVertex);
            double polyYPrev = yVertices.get(previousVertex);

            // Is the point within this vertex and the previous vertex
            if ((polyY < y && polyYPrev >= y || polyYPrev < y && polyY >= y) && (polyX <= x || polyXPrev <= x))
            {
                oddNodes ^= (polyX + ((y - polyY) / (polyYPrev - polyY) * (polyXPrev - polyX))) < x;
            }
            previousVertex = vertexIndex;
        }

        return oddNodes;
    }

    /**
     * Calculates the shortest distance from a point to a line.
     *
     * @param linePointA Line point 1.
     * @param linePointB Line point 2.
     * @param point      The outlying point.
     * @return The shortest distance from the point to the line. The sign indicates left/right.
     */
    private static double getCrossTrackDistance(ILocation linePointA, ILocation linePointB, ILocation point)
    {
        double aToPointBearing = bearingTo(linePointA, point);
        double aToBBearing = bearingTo(linePointA, linePointB);

        double aToPointDistance = distanceTo(linePointA, point);

        return Math.asin(Math.sin(aToPointDistance / RADIUS_OF_EARTH) * Math.sin(aToPointBearing - aToBBearing)) * RADIUS_OF_EARTH;
    }

    /**
     * Calculate the foci of an ellipse based on its center, the distance of the foci from the center and the angle
     * at which the major axis is rotated clockwise from north.
     *
     * @param center       The ellipse's center.
     * @param angle        The angle that the major axis is rotated clockwise from north.
     * @param fociDistance The distance of the foci from the center of the ellipse.
     * @return The location of the two foci.
     */
    private static ILocation[] getFoci(ILocation center, double angle, double fociDistance)
    {
        ILocation foci1 = greatCircleEndPosition(center, fociDistance, angle);
        ILocation foci2 = greatCircleEndPosition(center, fociDistance, (angle + 180) % 360.0);

        return new ILocation[]{foci1, foci2};
    }

    /**
     * Calculate the distance from the center of an ellipse to its foci.
     * <p/>
     * Formula:
     * <p/>
     * F = sqrt(j*j - n*n)
     * where: F is the distance to the foci, j is major radius and n is the minor radius
     *
     * @param majorRadius The length of the major axis radius.
     * @param minorRadius The length of the minor axis radius.
     * @return The distance from the center of the ellipse to its foci.
     */
    private static double getDistanceToFoci(double majorRadius, double minorRadius)
    {
        return Math.sqrt(Math.pow(majorRadius, 2) - Math.pow(minorRadius, 2));
    }

    /**
     * Get to see if a point is to the left of a line. This method may need to be more accurate...
     *
     * @param lineEndpoint1 The first line endpoint.
     * @param lineEndpoint2 The other line endpoint.
     * @param pointToCheck  The point to compare relative to the line.
     * @return 1 if the point is to the left, -1 if it's to the right or 0 if it falls on the line.
     */
    private static int isLeft(ILocation lineEndpoint1, ILocation lineEndpoint2, ILocation pointToCheck)
    {
        double calc = ((lineEndpoint2.getLongitude() - lineEndpoint1.getLongitude()) * (pointToCheck.getLatitude() - lineEndpoint1.getLatitude())
                - (pointToCheck.getLongitude() - lineEndpoint1.getLongitude()) * (lineEndpoint2.getLatitude() - lineEndpoint1.getLatitude()));

        if (calc > 0)
        {
            return 1;
        } else if (calc < 0)
        {
            return -1;
        } else
        {
            return 0;
        }
    }

    /**
     * Check to see if a point is on the line segment.
     *
     * @param lineEndpoint1 The first line endpoint.
     * @param lineEndpoint2 The other line endpoint.
     * @param pointToCheck  The point to compare relative to the line.
     * @return {@code true} if the point is on the line segment, {@code false} if not
     * @since 5.7
     */
    private static boolean onLineSegment(ILocation lineEndpoint1, ILocation lineEndpoint2, ILocation pointToCheck)
    {
        // Check if point is collinear
        if (isLeft(lineEndpoint1, lineEndpoint2, pointToCheck) == 0)
        {
            double px = pointToCheck.getLongitude();
            double py = pointToCheck.getLatitude();
            double p1x = lineEndpoint1.getLongitude();
            double p1y = lineEndpoint1.getLatitude();
            double p2x = lineEndpoint2.getLongitude();
            double p2y = lineEndpoint2.getLatitude();
            // Check if P is within the bounding box of the line segment
            return ((px >= Math.min(p1x, p2x) && px <= Math.max(p1x, p2x)) &&
                    (py >= Math.min(p1y, p2y) && py <= Math.max(p1y, p2y)));
        }
        return false;
    }

    /**
     * Returns the point of intersection of two paths defined by point and bearing
     * <p/>
     * @see <a href="http://williams.best.vwh.net/avform.htm#Intersection">Intersection</a>
     */
    private static ILocation intersection(ILocation line1A, ILocation line1B, ILocation line2A, ILocation line2B)
    {
        double bearing1 = bearingTo(line1A, line1B);
        double bearing2 = bearingTo(line2A, line2B);
        double p1Length = distanceTo(line1A, line1B);
        double p2Length = distanceTo(line2A, line2B);

        double lat1 = Math.toRadians(line1A.getLatitude());
        double lon1 = Math.toRadians(line1A.getLongitude());
        double lat2 = Math.toRadians(line2A.getLatitude());
        double lon2 = Math.toRadians(line2A.getLongitude());

        double bearing13 = Math.toRadians(bearing1);
        double bearing23 = Math.toRadians(bearing2);
        double dLat = lat2 - lat1;
        double dLon = lon2 - lon1;

        double dist12 = 2 * Math.asin(Math.sqrt(Math.sin(dLat / 2) * Math.sin(dLat / 2) +
                Math.cos(lat1) * Math.cos(lat2) * Math.sin(dLon / 2) * Math.sin(dLon / 2)));

        if (dist12 == 0) return null;

        // initial/final bearings between points
        double bearingA = Math.acos((Math.sin(lat2) - Math.sin(lat1) * Math.cos(dist12)) /
                (Math.sin(dist12) * Math.cos(lat1)));

        double bearingB = Math.acos((Math.sin(lat1) - Math.sin(lat2) * Math.cos(dist12)) /
                (Math.sin(dist12) * Math.cos(lat2)));

        double bearing12;
        double bearing21;

        if (Math.sin(lon2 - lon1) > 0)
        {
            bearing12 = bearingA;
            bearing21 = 2 * Math.PI - bearingB;
        } else
        {
            bearing12 = 2 * Math.PI - bearingA;
            bearing21 = bearingB;
        }

        double alpha1 = (bearing13 - bearing12 + Math.PI) % (2 * Math.PI) - Math.PI;  // angle 2-1-3
        double alpha2 = (bearing21 - bearing23 + Math.PI) % (2 * Math.PI) - Math.PI;  // angle 1-2-3

        if (Math.sin(alpha1) == 0 && Math.sin(alpha2) == 0) return null;  // infinite intersections
        if (Math.sin(alpha1) * Math.sin(alpha2) < 0) return null;       // ambiguous intersection

        //alpha1 = Math.abs(alpha1);
        //alpha2 = Math.abs(alpha2);
        // ... Ed Williams takes abs of alpha1/alpha2, but seems to break calculation?

        double alpha3 = Math.acos(-Math.cos(alpha1) * Math.cos(alpha2) +
                Math.sin(alpha1) * Math.sin(alpha2) * Math.cos(dist12));
        double dist13 = Math.atan2(Math.sin(dist12) * Math.sin(alpha1) * Math.sin(alpha2),
                Math.cos(alpha2) + Math.cos(alpha1) * Math.cos(alpha3));
        double lat3 = Math.asin(Math.sin(lat1) * Math.cos(dist13) +
                Math.cos(lat1) * Math.sin(dist13) * Math.cos(bearing13));
        double dLon13 = Math.atan2(Math.sin(bearing13) * Math.sin(dist13) * Math.cos(lat1),
                Math.cos(dist13) - Math.sin(lat1) * Math.sin(lat3));
        double lon3 = lon1 + dLon13;
        lon3 = (lon3 + 3 * Math.PI) % (2 * Math.PI) - Math.PI;  // normalise to -180..+180

        ILocation intersect = new RaptorLocation(Math.toDegrees(lat3), Math.toDegrees(lon3));

        if (logger.isDebugEnabled()) logger.debug("A to intersect: {}", distanceTo(line1A, intersect));
        if (logger.isDebugEnabled()) logger.debug("B to intersect: {}", distanceTo(line1B, intersect));

        if (distanceTo(line1A, intersect) > p1Length || distanceTo(line2A, intersect) > p2Length)
        {
            return null;
        } else
        {
            return intersect;
        }
    }

    /**
     * Computes the length of the rhumb line (in radians) between two locations. The return value gives the distance as the angular
     * distance between the two positions on the pi radius circle. This angle is also the arc length of the
     * segment between the two positions on that circle. To compute a distance in meters from this value, multiply it by
     * the radius of the globe.
     * This method uses a spherical model, not elliptical.
     *
     * @param startPoint the starting coordinate
     * @param endPoint   the ending coordinate
     *
     * @return the arc length (in radians) of the rhumb line between the two locations - this value is the arc length on
     * the radius pi circle.
     *
     * @since 5.0
     */
    public static double rhumbDistance(@Nonnull IGeoPoint startPoint, @Nonnull IGeoPoint endPoint)
    {
        double lat1 = Math.toRadians(startPoint.getLatitude());
        double lon1 = Math.toRadians(startPoint.getLongitude());
        double lat2 = Math.toRadians(endPoint.getLatitude());
        double lon2 = Math.toRadians(endPoint.getLongitude());

        if (lat1 == lat2 && lon1 == lon2) return 0;

        // Taken from http://www.movable-type.co.uk/scripts/latlong.html
        double dLat = lat2 - lat1;
        double dLon = lon2 - lon1;

        double q;
        if (Math.abs(dLat) < NEAR_ZERO_THRESHOLD)
        {
            q = Math.cos(lat1);
        } else
        {
            double dPhi = Math.log(Math.tan(lat2 / 2.0 + Math.PI / 4.0) / Math.tan(lat1 / 2.0 + Math.PI / 4.0));
            q = dLat / dPhi;
        }

        // If lonChange over 180 take shorter rhumb across 180 meridian.
        if (Math.abs(dLon) > Math.PI) dLon = dLon > 0 ? -(2 * Math.PI - dLon) : (2 * Math.PI + dLon);

        double distanceRadians = Math.sqrt(dLat * dLat + q * q * dLon * dLon);

        return Double.isNaN(distanceRadians) ? 0 : distanceRadians;
    }

    /**
     * Computes the length of the rhumb line (in meters) between two locations. See {@link #rhumbDistance(IGeoPoint, IGeoPoint)}.
     *
     * @param startPoint the starting coordinate
     * @param endPoint the ending coordinate
     * @return the arc length (in meters)
     * @since 5.6
     */
    public static double rhumbDistanceMeters(@Nonnull ILocation startPoint, @Nonnull ILocation endPoint)
    {
        return rhumbDistance(startPoint, endPoint) * getRadiusAt((startPoint.getLatitude() + endPoint.getLatitude()) / 2);
    }

    /**
     * Computes the azimuth angle (clockwise from North, in degrees) of a rhumb line (a line of constant heading) between two
     * locations.
     * This method uses a spherical model, not elliptical.
     *
     * @param startPoint the starting coordinate
     * @param endPoint   the ending coordinate
     *
     * @return azimuth angle (in degrees) of a rhumb line between the two locations.
     *
     * @since 5.0
     */
    public static double rhumbAzimuth(@Nonnull IGeoPoint startPoint, @Nonnull IGeoPoint endPoint)
    {
        double lat1 = Math.toRadians(startPoint.getLatitude());
        double lon1 = Math.toRadians(startPoint.getLongitude());
        double lat2 = Math.toRadians(endPoint.getLatitude());
        double lon2 = Math.toRadians(endPoint.getLongitude());

        if (lat1 == lat2 && lon1 == lon2) return 0;

        // Taken from http://www.movable-type.co.uk/scripts/latlong.html
        double dLon = lon2 - lon1;
        double dPhi = Math.log(Math.tan(lat2 / 2.0 + Math.PI / 4.0) / Math.tan(lat1 / 2.0 + Math.PI / 4.0));
        // If lonChange over 180 take shorter rhumb across 180 meridian.
        if (Math.abs(dLon) > Math.PI) dLon = dLon > 0 ? -(2 * Math.PI - dLon) : (2 * Math.PI + dLon);
        double azimuthRadians = Math.atan2(dLon, dPhi);

        return Double.isNaN(azimuthRadians) ? 0 : Math.toDegrees(azimuthRadians);
    }

    /**
     *  Computes the location on a rhumb line with the given starting location, rhumb azimuth, and distance along the line
     *  in meters. See {@link #rhumbEndPosition(IGeoPoint, double, double)}.
     *
     * @param startPoint the starting coordinate
     * @param rhumbAzimuthDegrees rhumb azimuth angle (clockwise from North, in degrees)
     * @param pathLengthMeters arc distance to travel (in meters)
     * @return location on the rhumb line.
     * @since 5.6
     */
    public static ILocation rhumbEndPositionMeters(@Nonnull ILocation startPoint, double rhumbAzimuthDegrees, double pathLengthMeters)
    {
        return rhumbEndPosition(startPoint, rhumbAzimuthDegrees, Angle.fromRadians(pathLengthMeters / getRadiusAt(startPoint.getLatitude())).degrees);
    }

    /**
     * Computes the location on a rhumb line with the given starting location, rhumb azimuth, and arc distance along the
     * line. This method uses a spherical model, not elliptical.
     *
     * @param startPoint the starting coordinate
     * @param rhumbAzimuthDegrees rhumb azimuth angle (clockwise from North, in degrees)
     * @param pathLengthDegrees arc distance to travel (in degrees)
     * @return location on the rhumb line.
     * @since 5.6
     */
    public static ILocation rhumbEndPosition(@Nonnull ILocation startPoint, double rhumbAzimuthDegrees, double pathLengthDegrees)
    {
        return (ILocation) rhumbEndPosition((IGeoPoint) startPoint, rhumbAzimuthDegrees, pathLengthDegrees);
    }

    /**
     * Computes the location on a rhumb line with the given starting location, rhumb azimuth, and arc distance along the
     * line.
     * This method uses a spherical model, not elliptical.
     *
     * @param startPoint          the starting coordinate
     * @param rhumbAzimuthDegrees rhumb azimuth angle (clockwise from North, in degrees)
     * @param pathLengthDegrees   arc distance to travel (in degrees)
     *
     * @return location on the rhumb line.
     *
     * @since 5.0
     */
    public static IGeoPoint rhumbEndPosition(@Nonnull IGeoPoint startPoint, double rhumbAzimuthDegrees, double pathLengthDegrees)
    {
        double lat1 = Math.toRadians(startPoint.getLatitude());
        double lon1 = Math.toRadians(startPoint.getLongitude());
        double azimuth = Math.toRadians(rhumbAzimuthDegrees);
        double distance = Math.toRadians(pathLengthDegrees);

        if (distance == 0) return startPoint;

        // Taken from http://www.movable-type.co.uk/scripts/latlong.html
        double dLat = distance * Math.cos(azimuth);
        double lat2 = lat1 + dLat;
        double q;
        if (Math.abs(dLat) < NEAR_ZERO_THRESHOLD)
        {
            q = Math.cos(lat1);
        } else
        {
            double dPhi = Math.log(Math.tan(lat2 / 2.0 + Math.PI / 4.0) / Math.tan(lat1 / 2.0 + Math.PI / 4.0));
            q = (lat2 - lat1) / dPhi;
        }

        double dLon = distance * Math.sin(azimuth) / q;
        // Handle latitude passing over either pole.
        if (Math.abs(lat2) > Math.PI / 2.0) lat2 = lat2 > 0 ? Math.PI - lat2 : -Math.PI - lat2;
        double lon2 = (lon1 + dLon + Math.PI) % (2 * Math.PI) - Math.PI;

        if (Double.isNaN(lat2) || Double.isNaN(lon2)) return startPoint;
        return GeoPoint.createMutable().set(Angle.fromRadians(lat2).normalizedLatitude().degrees, Angle.fromRadians(lon2).normalizedLongitude().degrees);
    }

    /**
     * Get whether the list of locations represents a closed shape where the first point is equal to the last point.
     * @param locations the list of locations
     * @return {@code true} if closed, {@code false} if not
     * @since 5.5
     */
    public static boolean isClosed(List<ILocation> locations)
    {
        return (locations.size() < 2) || (locations.get(0).equals(locations.get(locations.size() - 1)));
    }

    /**
     * Close a list of locations if necessary. The returned locations will have the first point be equal to the last point.
     * @param locations the list of locations
     * @return a closed list of locations
     * @since 5.5
     */
    public static List<ILocation> closeLocations(List<ILocation> locations)
    {
        if (isClosed(locations)) return locations;
        ArrayList<ILocation> result = new ArrayList<>(locations);
        result.add(locations.get(0));
        return result;
    }

    /**
     * Unclose a list of locations if necessary. The returned locations will remove the last point if equal to the first point.
     * @param locations the list of locations
     * @return an unclosed list of locations\
     * @since 5.5
     */
    public static List<ILocation> uncloseLocations(List<ILocation> locations)
    {
        if (!isClosed(locations) || locations.size() <= 2) return locations;
        ArrayList<ILocation> result = new ArrayList<>(locations);
        result.remove(locations.size() - 1);
        return result;
    }

    // TODO TAKX-4097 document what the relative error is between the spherical math and the cartesian approximation
    /**
     * Calculates the nearest intercept time, in seconds, from a moving source to a moving target. The target
     * heading is assumed constant and the necessary source heading can be derived by plotting the
     * target out for the calculated eta duration.
     * <p>Note: This method uses a cartesian projection and a quadratic equation, based on
     * <a href="https://www.codeproject.com/articles/Interception-of-Two-Moving-Objects-in-D-Space">
     * Interception of Two Moving Objects in 2D Space</a>.
     * This will be accurate for small distances, but larger distances will need truly spherical math, as outlined in section 5.B of
     * <a href="https://apps.dtic.mil/sti/tr/pdf/ADA170615.pdf">Tactical Algorithms for Spherical Geometry</a>.
     *
     * @param sourceLocation the source location
     * @param sourceSpeed the source speed in meters per second
     * @param targetLocation the target location
     * @param targetSpeed the target speed in meters per second
     * @param targetHeading the target heading
     * @return the minimum time to intercept, in seconds, or {@link Double#NaN} if no intercept is possible
     * @since 5.6
     */
    public static double interceptEta(@Nonnull IGeoPoint sourceLocation, double sourceSpeed, @Nonnull IGeoPoint targetLocation, double targetSpeed, double targetHeading)
    {
        var eta = Double.NaN;

        // If source point is not moving then we can never intercept
        if (sourceSpeed > 0.0)
        {
            // if target point is not moving or has no heading then we can use the simple calculation
            if (Double.isNaN(targetSpeed) || targetSpeed == 0 || Double.isNaN(targetHeading))
            {
                eta = GeoCalculations.distance(sourceLocation, targetLocation) / sourceSpeed;
            } else
            {
                var tempProjectedPoint = new PointD();
                var endPointNext = GeoCalculations.pointAtDistance(targetLocation, targetHeading, targetSpeed);
                var vectorEndPoint = projectEcef(endPointNext, tempProjectedPoint);
                var toVector = projectEcef(targetLocation, tempProjectedPoint);
                var velocityEndMarker = vectorEndPoint.subtract(toVector);
                var fromVector = projectEcef(sourceLocation, tempProjectedPoint);
                var vectorFromStart = fromVector.subtract(toVector);

                var dist = Math.sqrt((toVector.X - fromVector.X) * (toVector.X - fromVector.X) + (toVector.Y - fromVector.Y) * (toVector.Y - fromVector.Y) + (toVector.Z - fromVector.Z) * (toVector.Z - fromVector.Z));
                var time = Double.NaN;

                var a = sourceSpeed * sourceSpeed - targetSpeed * targetSpeed;
                var b = 2 * vectorFromStart.dot(velocityEndMarker);
                var c = -dist * dist;

                if (a == 0)
                {
                    // If speeds are the same then it becomes a linear problem, and we can skip quadratic function
                    var linear = -c / b;
                    if (linear >= 0) time = linear;
                } else
                {
                    var sqrtpart = (b * b) - (4 * a * c);
                    var answer1 = ((-1) * b + Math.sqrt(sqrtpart)) / (2 * a);
                    var answer2 = ((-1) * b - Math.sqrt(sqrtpart)) / (2 * a);

                    if (answer1 < 0 && answer2 > 0)
                    {
                        time = answer2;
                    } else if (answer1 > 0 && answer2 < 0)
                    {
                        time = answer1;
                    } else if (answer1 > 0 && answer2 > 0)
                    {
                        time = Math.min(answer1, answer2);
                    }
                }
                eta = time;
            }
        }
        return eta;
    }

    @SuppressWarnings("SameParameterValue")
    private static Vector3D projectEcef(IGeoPoint point, PointD projectedPoint)
    {
        var gp = new gov.tak.api.engine.map.coords.GeoPoint(point.getLatitude(), point.getLongitude(), 0);
        ECEF.forward(gp, projectedPoint);
        return new Vector3D(projectedPoint.x, projectedPoint.y, projectedPoint.z);
    }
}
