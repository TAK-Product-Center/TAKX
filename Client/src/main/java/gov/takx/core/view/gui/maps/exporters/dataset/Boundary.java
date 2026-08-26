package gov.takx.core.view.gui.maps.exporters.dataset;

import gov.nasa.worldwind.geom.Angle;
import gov.nasa.worldwind.geom.Sector;
import gov.tak.api.maps.coords.GeoPoint;
import gov.takx.api.coordinatesystem.ILocation;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Stream;

/**
 * Boundary definition for creating a cache of tiles
 */
public class Boundary
{

    private final Sector sector;
    private final List<ILocation> vertices;
    private final boolean closed;
    private final double bufferWidthMeters;

    public Boundary(Sector sector, List<ILocation> vertices, boolean closed, double bufferWidthMeters)
    {
        this.sector = sector;
        this.vertices = vertices;
        this.closed = closed;
        this.bufferWidthMeters = bufferWidthMeters;
    }

    public Boundary(List<ILocation> vertices, boolean closed, double bufferWidthMeters)
    {
        this.vertices = vertices;
        this.closed = closed;
        this.bufferWidthMeters = bufferWidthMeters;
        this.sector = Boundary.boundingSector(vertices);
    }

    public Boundary(Sector sector, List<ILocation> vertices, boolean closed)
    {
        this.sector = sector;
        this.vertices = vertices;
        this.closed = closed;
        this.bufferWidthMeters = 0;
    }

    /**
     * @param sector a sector that defines the boundary
     * @param bufferWidthMeters the buffer width in meters
     * @since 5.0
     */
    public Boundary(Sector sector, double bufferWidthMeters)
    {
        this.sector = sector;
        this.vertices = sector.asList().stream().map(ll -> (ILocation) new GeoPoint(ll.latitude.degrees, ll.longitude.degrees)).toList();
        this.closed = true;
        this.bufferWidthMeters = bufferWidthMeters;
    }

    /**
     * @return the sector
     */
    public Sector getSector()
    {
        return sector;
    }

    /**
     * @return the vertices
     */
    public List<ILocation> getVertices()
    {
        return vertices;
    }

    /**
     * @return the closed
     */
    public boolean isClosed()
    {
        return closed;
    }

    /**
     * @return the buffer width in meters
     */
    public double getBufferWidthMeters()
    {
        return bufferWidthMeters;
    }

    public double getArea()
    {
        int n = vertices.size();
        double area = 0;
        for (int i = 0; i < n; i++)
        {
            area += vertices.get(i).getLongitude()
                    * (vertices.get((i + 1) % n).getLatitude() - vertices.get((i + n - 1) % n).getLatitude());
        }
        return Math.abs(area * .5);
    }

    public static Sector boundingSector(List<ILocation> locations)
    {
        if (locations == null || locations.isEmpty())
        {
            return Sector.EMPTY_SECTOR; // TODO: should be returning null
        }

        double minLat = Angle.POS90.getDegrees();
        double minLon = Angle.POS180.getDegrees();
        double maxLat = Angle.NEG90.getDegrees();
        double maxLon = Angle.NEG180.getDegrees();

        for (ILocation p : locations)
        {
            double lat = p.getLatitude();
            if (lat < minLat)
            {
                minLat = lat;
            }
            if (lat > maxLat)
            {
                maxLat = lat;
            }

            double lon = p.getLongitude();
            if (lon < minLon)
            {
                minLon = lon;
            }
            if (lon > maxLon)
            {
                maxLon = lon;
            }
        }

        return Sector.fromDegrees(minLat, maxLat, minLon, maxLon);
    }

    /**
     * Returns a closed array of vertices from an array of vertices that may or may not already be closed.
     * @param vertices an array of vertices
     * @return an array of closed vertices, or null if {@code vertices} is null
     * @since 5.3
     */
    public static ILocation[] closeVertices(ILocation[] vertices)
    {
        if (vertices == null || vertices.length <= 1 ||
                (vertices[0].getLatitude() == vertices[vertices.length - 1].getLatitude() &&
                        vertices[0].getLongitude() == vertices[vertices.length - 1].getLongitude()))
            return vertices;
        return Stream.concat(Arrays.stream(vertices), Stream.of(vertices[0])).toArray(ILocation[]::new);
    }
}
