package gov.takx.dataservices.shapes;

import com.atakmap.map.elevation.ElevationManager;
import gov.nasa.worldwind.geom.Extent;
import gov.nasa.worldwind.geom.ExtentHolder;
import gov.nasa.worldwind.globes.Globe;
import gov.tak.api.engine.map.coords.GeoCalculations;
import gov.tak.api.engine.map.coords.IGeoPoint;
import gov.tak.api.maps.coords.GeoPoint;
import gov.takx.api.coordinatesystem.ILocation;
import gov.takx.api.coordinatesystem.RaptorLocation;
import gov.takx.api.shapes.IMutableRectangle;
import gov.takx.api.shapes.IShape;
import gov.takx.api.shapes.ShapeProperties;
import gov.takx.platform.utils.ShapeUtils;
import jakarta.annotation.Nonnull;

import javax.naming.OperationNotSupportedException;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Implementation of a rectangle shape.
 *
 */
public class Rectangle extends ARegularShape implements IMutableRectangle, Serializable, ExtentHolder
{
    private static final double ROUND_PLACES = 1e5;
    private static final double MAX_DELTA_LON_DEGREES = 45;

    // Legacy Ordering: LL, LR, UR, UL
    private static final int SW = 0;
    private static final int SE = 1;
    private static final int NE = 2;
    private static final int NW = 3;

    // CoT/Kernel Ordering: UL, LL, LR, UR. 
    private static final int UL = NW;
    private static final int LL = SW;
    private static final int LR = SE;
    private static final int UR = NE;

    private List<ILocation> vertices;
    private double width;
    private double height;

    /**
     * Default constructor.
     * @since 3.3
     */
    public Rectangle()
    {
    }

    /**
     * Create a new rectangle instance with the given name, center point, orientation, height, and width.
     *
     * @param name             display name for shape
     * @param location         Location of center point
     * @param orientation      Orientation angle
     * @param height           The height (Y-axis) of the rectangle
     * @param width            The width (X-axis) of the rectangle
     */
    public Rectangle(@Nonnull String name, @Nonnull ILocation location, double orientation, double height, double width)
    {
        setName(name);
        setLocation(location);
        internalSetSize(width, height);
        setOrientation(orientation);
    }

    /**
     * Create a new rectangle instance with the given name, and minimum and maximum corners.
     *
     * @param name             display name for shape
     * @param minCorner        the minimum corner for the square
     * @param maxCorner        the maximum corner for the square
     * @since 5.7
     */
    public Rectangle(@Nonnull String name, @Nonnull ILocation minCorner, @Nonnull ILocation maxCorner)
    {
        this(name, List.of(minCorner, new GeoPoint(minCorner.getLatitude(), maxCorner.getLongitude()), maxCorner, new GeoPoint(maxCorner.getLatitude(), minCorner.getLongitude())));
    }

    /**
     * Create a new rectangle instance with the given name, and vertices in the cardinal order: SW, SE, NE, NW.
     *
     * @param name display name for shape
     * @param vertices vertices for the rectangle
     * @since 5.7
     */
    public Rectangle(@Nonnull String name, @Nonnull List<ILocation> vertices)
    {
        setName(name);
        internalSetVertices(vertices);
    }

    /**
     * When opening an older project (pre 5.7), the 'vertices' list did not exist, so we need to reconstruct it from
     * the persisted width/height in that case.
     * @since 5.7.1
     */
    @Override
    public void onDeserialized()
    {
        if (vertices == null && width != 0 && height != 0) internalSetSize(width, height);
    }

    @Override
    public List<ILocation> getVertices(boolean takOrder)
    {
        if (vertices == null) return null;
        return !takOrder ? new ArrayList<>(vertices) : Arrays.asList(vertices.get(UL), vertices.get(LL), vertices.get(LR), vertices.get(UR));
    }

    @Override
    public void setVertices(@Nonnull List<ILocation> vertices)
    {
        internalSetVertices(vertices);
    }

    private void internalSetVertices(@Nonnull List<ILocation> vertices)
    {
        if (vertices.size() != 4) throw new IllegalArgumentException("Vertex count must be 4, was " + vertices.size());
        propertyChangeSupport.updateAndFire(ShapeProperties.LOCATIONS, this.vertices, vertices, () -> {
            this.vertices = vertices;
            IGeoPoint centroid = GeoCalculations.computeAverage(vertices.toArray(IGeoPoint[]::new), true);
            propertyChangeSupport.updateAndFire(ShapeProperties.WIDTH, getWidth(), calculateWidth(vertices), () -> width = calculateWidth(vertices));
            propertyChangeSupport.updateAndFire(ShapeProperties.HEIGHT, getHeight(), calculateHeight(vertices), () -> height = calculateHeight(vertices));
            super.setOrientation(calculateOrientation(vertices));
            double altitude = getLocation().hasElevation() ? getLocation().getAltitude() : ElevationManager.getElevation(centroid.getLatitude(), centroid.getLongitude(), null);
            super.setLocation(new GeoPoint(centroid.getLatitude(), centroid.getLongitude(), altitude));
        });
    }

    @Override
    public void setOrientation(double orientationDegrees)
    {
        internalSetOrientation(orientationDegrees);
    }

    private void internalSetOrientation(double orientationDegrees)
    {
        double delta = orientationDegrees - getOrientation();
        super.setOrientation(orientationDegrees);
        if (delta != 0 && hasValidVertices(vertices))
        {
            List<ILocation> rotatedPoints = ShapeUtils.rotatePoints(getLocation(), getVertices(), delta, getAltitudeMode()).stream().map(ILocation.class::cast).toList();
            propertyChangeSupport.updateAndFire(ShapeProperties.LOCATIONS, this.vertices, rotatedPoints, () -> this.vertices = rotatedPoints);
        }
    }

    @Override
    public double getHeight()
    {
        return height;
    }

    @Override
    public void setHeight(double height)
    {
        internalSetSize(getWidth(), height);
    }

    @Override
    public double getWidth()
    {
        return width;
    }

    @Override
    public void setWidth(double width)
    {
        internalSetSize(width, getHeight());
    }

    @Override
    public void setSize(double width, double height)
    {
        internalSetSize(width, height);
    }

    @Override
    public void setLocation(@Nonnull ILocation location)
    {
        ILocation origLocation = getLocation();
        if (!location.equals(origLocation))
        {
            if (hasValidVertices(vertices))
            {
                List<ILocation> movedVertices = moveVertices(vertices, origLocation, location);
                propertyChangeSupport.updateAndFire(ShapeProperties.LOCATIONS, this.vertices, movedVertices, () -> vertices = movedVertices);
            }
            super.setLocation(location);
        }
    }

    private void internalSetSize(double width, double height)
    {
        if (width <= 0.0 || height <= 0.0) throw new IllegalArgumentException("Width and height must be greater than 0.");

        double halfWidth = width / 2;
        double halfHeight = height / 2;
        double orientation = getOrientation();
        ILocation location = getLocation();

        ILocation northCenter = ShapeUtils.rhumbEndPositionMeters(location, orientation, halfHeight);
        ILocation northWest = ShapeUtils.rhumbEndPositionMeters(northCenter, orientation - 90, halfWidth);
        ILocation northEast = ShapeUtils.rhumbEndPositionMeters(northCenter, orientation + 90, halfWidth);
        ILocation southCenter = ShapeUtils.rhumbEndPositionMeters(location, orientation + 180, halfHeight);
        ILocation southWest = ShapeUtils.rhumbEndPositionMeters(southCenter, orientation - 90, halfWidth);
        ILocation southEast = ShapeUtils.rhumbEndPositionMeters(southCenter, orientation + 90, halfWidth);

        final List<ILocation> vertices = new ArrayList<>();

        double elevation = location.getElevation();
        vertices.add(new RaptorLocation(southWest.getLatitude(), southWest.getLongitude(), elevation));
        vertices.add(new RaptorLocation(southEast.getLatitude(), southEast.getLongitude(), elevation));
        vertices.add(new RaptorLocation(northEast.getLatitude(), northEast.getLongitude(), elevation));
        vertices.add(new RaptorLocation(northWest.getLatitude(), northWest.getLongitude(), elevation));

        propertyChangeSupport.updateAndFire(ShapeProperties.LOCATIONS, this.vertices, vertices, () -> this.vertices = vertices);
        propertyChangeSupport.updateAndFire(ShapeProperties.WIDTH, getWidth(), width, () -> this.width = width);
        propertyChangeSupport.updateAndFire(ShapeProperties.HEIGHT, getHeight(), height, () -> this.height = height);
    }

    private static boolean hasValidVertices(List<ILocation> vertices)
    {
        return vertices != null && vertices.size() == 4;
    }

    @Override
    public boolean contains(ILocation location)
    {
        if (spansIDL())
        {
            /* This rectangle spans the International Date Line
            
               Note: I would really have liked to put this logic inside ShapeUtils.polygonContainsLocation, but I did
                     not see a reasonable way to detect that a polygon is actually across the IDL. The polygon does
                     not have a min/max point (AFAIK) which prevents us from knowing if it is spanning the IDL.
            */
            final List<ILocation> vertices = new ArrayList<>();
            // Just move the comparison to be NOT over the IDL
            getVertices().forEach(loc -> vertices.add(new RaptorLocation(loc.getLatitude(), adjustLongitudeForIDL(loc.getLongitude()))));
            final ILocation compareLoc = new RaptorLocation(location.getLatitude(), adjustLongitudeForIDL(location.getLongitude()));
            return ShapeUtils.polygonContainsLocation(vertices, compareLoc);
        } else
        {
            // Since this code path is by far the most common we need to keep this as efficient as possible!
            return ShapeUtils.polygonContainsLocation(getVertices(), location);
        }
    }

    /**
     * Determine if a rectangle spans the IDL. Do this by taking the average of the longitude values in the vertices
     * and compare that to the longitude of the center location. If those values are not equal then the rectangle
     * spans the IDL. This works because the IDL represents a mathematical discontinuity.
     * <p>
     * Note: When the rectangle is being rotated the calculation of the vertices becomes more inaccurate. If the
     * rectangle spans the IDL the delta between the center and the average should always be between absolute 90
     * and 180. When the rectangle does not span the IDL then the delta should be exactly zero but in reality
     * seems to drift up to about 4 degrees due to geospatial calculation errors. So I am going to use a value of
     * 45 for the delta boundary used in determining if the rect spans the IDL or not.
     *
     * @return true if the rectangle spans the IDL
     */
    private boolean spansIDL()
    {
        final List<ILocation> vertices = getVertices();
        final double lonSum = vertices.stream().limit(4).mapToDouble(ILocation::getLongitude).sum();
        final double average = lonSum / 4.0;
        final double delta = average - getLocation().getLongitude();

        return Math.abs(delta) > MAX_DELTA_LON_DEGREES;
    }

    /**
     * Adjust the longitude value to remove the IDL from future calculations. The IDL presents a discontinuity
     * for containment calculation when a rectangle spans it. This adjustment essentially shifts the meridian
     * around to the location of the IDL so that the containment calculations can use the current boundary
     * comparison algorithm.
     *
     * @param originalLongitude The original longitude value
     * @return the adjusted longitude
     * @since 2.38
     */
    private double adjustLongitudeForIDL(double originalLongitude)
    {
        double lon = originalLongitude - 180.0;
        if (lon < -180.0) lon += 360.0;
        return lon;
    }

    @Override
    public boolean contains(IShape shape) throws OperationNotSupportedException
    {
        return ShapeUtils.polygonContainsShape(getVertices(), shape);
    }

    @Override
    public boolean intersects(IShape shape) throws OperationNotSupportedException
    {
        return ShapeUtils.polygonIntersectsShape(getVertices(), shape);
    }

    @Override
    public ILocation getMinCorner()
    {
        final List<ILocation> locations = getVertices();
        return locations == null ? null : locations.get(MIN_CORNER);
    }

    @Override
    public ILocation getMaxCorner()
    {
        final List<ILocation> locations = getVertices();
        return locations == null ? null : locations.get(MAX_CORNER);
    }

    @Override
    public Extent getExtent(Globe globe, double verticalExaggeration)
    {
        return ExtentFactory.createExtent(globe, getVertices());
    }

    private static double calculateWidth(List<ILocation> vertices)
    {
        return Math.max(GeoCalculations.distance(vertices.get(SW), vertices.get(SE)),
                GeoCalculations.distance(vertices.get(NW), vertices.get(NE)));
    }

    private static double calculateHeight(List<ILocation> vertices)
    {
        return Math.max(GeoCalculations.distance(vertices.get(SW), vertices.get(NW)),
                GeoCalculations.distance(vertices.get(SE), vertices.get(NE)));
    }

    private static double calculateOrientation(List<ILocation> vertices)
    {
        double orientation = ShapeUtils.rhumbAzimuth(vertices.get(NW), vertices.get(NE)) - 90;
        // Round small numerical precision errors
        return Math.round(orientation * ROUND_PLACES) / ROUND_PLACES;
    }

    private static List<ILocation> moveVertices(List<ILocation> locations, ILocation anchorLocation, ILocation moveLocation)
    {
        List<ILocation> result = new ArrayList<>();
        double dx = moveLocation.getLongitude() - anchorLocation.getLongitude();
        double dy = moveLocation.getLatitude() - anchorLocation.getLatitude();
        for (ILocation location : locations)
        {
            result.add(new GeoPoint(location.getLatitude() + dy, location.getLongitude() + dx, location.getAltitude()));
        }
        return result;
    }
}
