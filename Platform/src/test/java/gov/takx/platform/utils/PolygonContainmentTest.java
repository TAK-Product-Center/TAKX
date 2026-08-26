package gov.takx.platform.utils;

import com.atakmap.map.EngineLibrary;
import gov.takx.api.coordinatesystem.ILocation;
import gov.takx.api.coordinatesystem.RaptorLocation;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

// TODO KMB: Combine into one ShapeUtilsTest
/**
 * Test polygon containment.
 *
 */
public class PolygonContainmentTest
{
    static
    {
        EngineLibrary.initialize();
    }

    @Test
    public void standardPolygonContainment()
    {
        List<ILocation> vertices = getStandardPolygonVertices();
        checkPolygonPoints(vertices, getPointsInsideStandardPolygon(), true);
        checkPolygonPoints(vertices, getPointsOutsideStandardPolygon(), false);
    }

    @Test
    public void irregularPolygonContainment()
    {
        List<ILocation> vertices = getIrregularPolygonVertices();
        checkPolygonPoints(vertices, getPointsInsideIrregularPolygon(), true);
        checkPolygonPoints(vertices, getPointsOutsideIrregularPolygon(), false);
    }

    @Test
    public void equatorCrossingPolygonContainment()
    {
        List<ILocation> vertices = getEquatorPolygonVertices();
        checkPolygonPoints(vertices, getPointsInsideEquatorPolygon(), true);
        checkPolygonPoints(vertices, getPointsOutsideEquatorPolygon(), false);
    }

    @Test
    public void IDLCrossingPolygonContainment()
    {
        List<ILocation> vertices = getIDLPolygonVertices();
        checkPolygonPoints(vertices, getPointsInsideIDLPolygon(), true);
        checkPolygonPoints(vertices, getPointsOutsideIDLPolygon(), false);
    }

    @Test
    public void standardPolygonIntersection()
    {
        List<ILocation> polygon1 = getStandardPolygonVertices();
        List<ILocation> polygon2;

        polygon2 = getStandardPolygonIntersectVertices();
        assertThat(ShapeUtils.polygonIntersectsShape(polygon1, polygon2)).isTrue();

        polygon2 = getStandardPolygonNoIntersectVertices();
        assertThat(ShapeUtils.polygonIntersectsShape(polygon1, polygon2)).isFalse();
    }

    @Test
    public void equatorialPolygonIntersection()
    {
        List<ILocation> polygon1 = getEquatorPolygonVertices();
        List<ILocation> polygon2;

        polygon2 = getEquatorPolygonIntersectVertices();
        assertThat(ShapeUtils.polygonIntersectsShape(polygon1, polygon2)).isTrue();

        polygon2 = getEquatorPolygonNoIntersectVertices();
        assertThat(ShapeUtils.polygonIntersectsShape(polygon1, polygon2)).isFalse();
    }

    @Test
    public void irregularPolygonIntersection()
    {
        List<ILocation> polygon1 = getIrregularPolygonVertices();
        List<ILocation> polygon2;

        polygon2 = getIrregularPolygonIntersectVertices();
        assertThat(ShapeUtils.polygonIntersectsShape(polygon1, polygon2)).isTrue();

        polygon2 = getIrregularPolygonNoIntersectVertices();
        assertThat(ShapeUtils.polygonIntersectsShape(polygon1, polygon2)).isFalse();
    }

    @Test
    public void IDLPolygonIntersection()
    {
        List<ILocation> polygon1 = getIDLPolygonVertices();
        List<ILocation> polygon2;

        polygon2 = getIDLPolygonIntersectVertices();
        assertThat(ShapeUtils.polygonIntersectsShape(polygon1, polygon2)).isTrue();

        polygon2 = getIDLPolygonNoIntersectVertices();
        assertThat(ShapeUtils.polygonIntersectsShape(polygon1, polygon2)).isFalse();
    }

    @Test
    public void PolygonPolylineIntersection()
    {
        assertThat(ShapeUtils.polygonIntersectsPolyline(getStandardPolygonVertices(), getSpanningPolylineVertices())).isTrue();
        assertThat(ShapeUtils.polygonIntersectsPolyline(getStandardPolygonVertices(), getNonSpanningPolylineVertices())).isFalse();
        assertThat(ShapeUtils.polygonIntersectsPolyline(getStandardPolygonVertices(), getLastSegmentIntersectingPolylineVertices())).isTrue();
        assertThat(ShapeUtils.polygonIntersectsPolyline(getStandardPolygonVertices(), getThreePointIntersectingPolylineVertices())).isTrue();
        assertThat(ShapeUtils.polygonIntersectsPolyline(getStandardPolygonVertices(), getFourPointNonSpanningPolylineVertices())).isFalse();
    }

    /**
     * Check to see if each point in a set of points is inside a polyogon.
     *
     * @param vertices  The polygon's vertices. The first vertex must also be the last vertex.
     * @param locations The locations to check.
     * @param inside    {@code true} iff the points are expected to be inside the polygon.
     */
    private void checkPolygonPoints(List<ILocation> vertices, List<ILocation> locations, boolean inside)
    {
        StringBuilder builder = new StringBuilder();
        List<ILocation> workingVertices = new ArrayList<>(vertices);

        for (int i = 0; i < workingVertices.size(); i++)
        {
            // Circularly rotate the points to try all the different combinations.
            workingVertices.add(workingVertices.remove(0));

            for (ILocation vertex : workingVertices)
            {
                builder.append('[');
                builder.append(vertex.getLatitude());
                builder.append(',');
                builder.append(vertex.getLongitude());
                builder.append(']');
            }

            for (ILocation location : locations)
            {
                assertThat(ShapeUtils.polygonContainsLocation(workingVertices, location)).withFailMessage("Point: " + location.getLatitude() + "/" + location.getLongitude() + " in polygon with vertices: " + builder.toString()).isEqualTo(inside);
            }

            // Reset.
            builder.setLength(0);
        }
    }

    // Simple

    private List<ILocation> getSpanningPolylineVertices()
    {
        List<ILocation> locations = new ArrayList<>();

        locations.add(new RaptorLocation(2, 15));
        locations.add(new RaptorLocation(27, 15));

        return locations;
    }

    private List<ILocation> getNonSpanningPolylineVertices()
    {
        List<ILocation> locations = new ArrayList<>();

        locations.add(new RaptorLocation(2, 25));
        locations.add(new RaptorLocation(27, 25));

        return locations;
    }

    private List<ILocation> getLastSegmentIntersectingPolylineVertices()
    {
        List<ILocation> locations = new ArrayList<>();

        locations.add(new RaptorLocation(5, 10));
        locations.add(new RaptorLocation(10, 15));

        return locations;
    }

    private List<ILocation> getThreePointIntersectingPolylineVertices()
    {
        List<ILocation> locations = new ArrayList<>();

        locations.add(new RaptorLocation(10, 5));
        locations.add(new RaptorLocation(20, 5));
        locations.add(new RaptorLocation(20, 15));

        return locations;
    }

    private List<ILocation> getFourPointNonSpanningPolylineVertices()
    {
        List<ILocation> locations = new ArrayList<>();

        locations.add(new RaptorLocation(10, 5));
        locations.add(new RaptorLocation(30, 5));
        locations.add(new RaptorLocation(30, 30));
        locations.add(new RaptorLocation(5, 30));

        return locations;
    }

    private List<ILocation> getStandardPolygonVertices()
    {
        List<ILocation> locations = new ArrayList<>();

        locations.add(new RaptorLocation(10, 10));
        locations.add(new RaptorLocation(20, 10));
        locations.add(new RaptorLocation(25, 15));
        locations.add(new RaptorLocation(20, 20));
        locations.add(new RaptorLocation(10, 20));
        locations.add(new RaptorLocation(5, 15));

        return locations;
    }

    private List<ILocation> getPointsInsideStandardPolygon()
    {
        List<ILocation> locations = new ArrayList<>();

        locations.add(new RaptorLocation(10.1, 10.1));
        locations.add(new RaptorLocation(11, 11));
        locations.add(new RaptorLocation(15, 15));
        locations.add(new RaptorLocation(24.99, 15));
        locations.add(new RaptorLocation(19.99, 19.99));

        return locations;
    }

    private List<ILocation> getPointsOutsideStandardPolygon()
    {
        List<ILocation> locations = new ArrayList<>();

        locations.add(new RaptorLocation(0, 0));
        locations.add(new RaptorLocation(9, 9));
        locations.add(new RaptorLocation(-15, 15));
        locations.add(new RaptorLocation(21, 20));
        locations.add(new RaptorLocation(25, 14.99));
        locations.add(new RaptorLocation(25, 15.01));

        return locations;
    }

    // Irregular (concave side)

    private List<ILocation> getIrregularPolygonVertices()
    {
        List<ILocation> locations = new ArrayList<>();

        locations.add(new RaptorLocation(10, 10));
        locations.add(new RaptorLocation(20, 10));
        locations.add(new RaptorLocation(25, 15));
        locations.add(new RaptorLocation(20, 20));
        locations.add(new RaptorLocation(15, 15));
        locations.add(new RaptorLocation(10, 20));
        locations.add(new RaptorLocation(5, 15));

        return locations;
    }

    private List<ILocation> getPointsInsideIrregularPolygon()
    {
        List<ILocation> locations = new ArrayList<>();

        locations.add(new RaptorLocation(10.1, 10.1));
        locations.add(new RaptorLocation(11, 11));
        locations.add(new RaptorLocation(15, 14.9));
        locations.add(new RaptorLocation(20, 15));
//        locations.add(new RaptorLocation(20, 19.50));

        return locations;
    }

    private List<ILocation> getPointsOutsideIrregularPolygon()
    {
        List<ILocation> locations = new ArrayList<>();

        locations.add(new RaptorLocation(0, 0));
        locations.add(new RaptorLocation(9, 9));
        locations.add(new RaptorLocation(-15, 15));
        locations.add(new RaptorLocation(15, 15.01));
        locations.add(new RaptorLocation(19, 19.99));
        locations.add(new RaptorLocation(21, 20));
        locations.add(new RaptorLocation(25, 14.99));
        locations.add(new RaptorLocation(25, 15.01));
        locations.add(new RaptorLocation(19.99, 20));

        return locations;
    }

    // Equator crossing

    private List<ILocation> getEquatorPolygonVertices()
    {
        List<ILocation> locations = new ArrayList<>();

        locations.add(new RaptorLocation(-10, 10));
        locations.add(new RaptorLocation(10, 10));
        locations.add(new RaptorLocation(15, 15));
        locations.add(new RaptorLocation(10, 20));
        locations.add(new RaptorLocation(-10, 20));
        locations.add(new RaptorLocation(-15, 15));

        return locations;
    }

    private List<ILocation> getPointsInsideEquatorPolygon()
    {
        List<ILocation> locations = new ArrayList<>();

        locations.add(new RaptorLocation(-10, 11));
        locations.add(new RaptorLocation(10.01, 10.01));
        locations.add(new RaptorLocation(14.99, 15));
        locations.add(new RaptorLocation(0, 19.99));
        locations.add(new RaptorLocation(0, 15));

        return locations;
    }

    private List<ILocation> getPointsOutsideEquatorPolygon()
    {
        List<ILocation> locations = new ArrayList<>();

        locations.add(new RaptorLocation(-11, 10));
        locations.add(new RaptorLocation(9.99, 9.99));
        locations.add(new RaptorLocation(15, 15.01));
        locations.add(new RaptorLocation(10.01, 20));
        locations.add(new RaptorLocation(0, 20.01));

        return locations;
    }

    // International Date Line crossing
    private List<ILocation> getIDLPolygonVertices()
    {
        List<ILocation> locations = new ArrayList<>();

        locations.add(new RaptorLocation(10, -10));
        locations.add(new RaptorLocation(20, -10));
        locations.add(new RaptorLocation(25, 0));
        locations.add(new RaptorLocation(20, 10));
        locations.add(new RaptorLocation(10, 10));
        locations.add(new RaptorLocation(5, 0));

        return locations;
    }

    private List<ILocation> getPointsInsideIDLPolygon()
    {
        List<ILocation> locations = new ArrayList<>();

        locations.add(new RaptorLocation(7, -1));
        locations.add(new RaptorLocation(11, -9.99));
        locations.add(new RaptorLocation(20, 0));
        locations.add(new RaptorLocation(24.99, 0));
        locations.add(new RaptorLocation(10, 9.99));
        locations.add(new RaptorLocation(5.01, 0));
        locations.add(new RaptorLocation(20, -9.99));

        return locations;
    }

    private List<ILocation> getPointsOutsideIDLPolygon()
    {
        List<ILocation> locations = new ArrayList<>();

        locations.add(new RaptorLocation(10, -10.01));
        locations.add(new RaptorLocation(9, -9.99));
        locations.add(new RaptorLocation(4.99, 0));
        locations.add(new RaptorLocation(25.01, 0));
        locations.add(new RaptorLocation(0, 0));
        locations.add(new RaptorLocation(-10, 0));
        locations.add(new RaptorLocation(89.99, 0));

        return locations;
    }

    // Intersection check polygons

    private List<ILocation> getStandardPolygonIntersectVertices()
    {
        List<ILocation> locations = new ArrayList<>();

        locations.add(new RaptorLocation(1, 1));
        locations.add(new RaptorLocation(5, 5));
        locations.add(new RaptorLocation(11, 11));
        locations.add(new RaptorLocation(3, 3));

        return locations;
    }

    private List<ILocation> getStandardPolygonNoIntersectVertices()
    {
        List<ILocation> locations = new ArrayList<>();

        locations.add(new RaptorLocation(1, 1));
        locations.add(new RaptorLocation(5, 5));
        locations.add(new RaptorLocation(9.99, 9.99));
        locations.add(new RaptorLocation(3, 3));

        return locations;
    }

    private List<ILocation> getEquatorPolygonIntersectVertices()
    {
        List<ILocation> locations = new ArrayList<>();

        locations.add(new RaptorLocation(0, 0));
        locations.add(new RaptorLocation(-9, 10));
        locations.add(new RaptorLocation(-10, 0));

        return locations;
    }

    private List<ILocation> getEquatorPolygonNoIntersectVertices()
    {
        List<ILocation> locations = new ArrayList<>();

        locations.add(new RaptorLocation(0, 0));
        locations.add(new RaptorLocation(-11, 10));
        locations.add(new RaptorLocation(-10, 0));

        return locations;
    }

    private List<ILocation> getIrregularPolygonIntersectVertices()
    {
        List<ILocation> locations = new ArrayList<>();

        locations.add(new RaptorLocation(15, 14.99));
        locations.add(new RaptorLocation(20, 25));
        locations.add(new RaptorLocation(10, 25));

        return locations;
    }

    private List<ILocation> getIrregularPolygonNoIntersectVertices()
    {
        List<ILocation> locations = new ArrayList<>();

        locations.add(new RaptorLocation(15, 15.01));
        locations.add(new RaptorLocation(20, 25));
        locations.add(new RaptorLocation(10, 25));

        return locations;
    }

    private List<ILocation> getIDLPolygonIntersectVertices()
    {
        List<ILocation> locations = new ArrayList<>();

        locations.add(new RaptorLocation(0, -10));
        locations.add(new RaptorLocation(7.5, -4.99));
        locations.add(new RaptorLocation(0, -5));

        return locations;
    }

    private List<ILocation> getIDLPolygonNoIntersectVertices()
    {
        List<ILocation> locations = new ArrayList<>();

        locations.add(new RaptorLocation(0, -10));
        locations.add(new RaptorLocation(7.5, -5.01));
        locations.add(new RaptorLocation(0, -5));

        return locations;
    }
}
