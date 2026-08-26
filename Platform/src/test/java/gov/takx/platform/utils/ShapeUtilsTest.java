package gov.takx.platform.utils;

import com.atakmap.map.EngineLibrary;
import gov.tak.api.engine.map.coords.IGeoPoint;
import gov.tak.api.maps.coords.GeoPoint;
import gov.tak.test.utils.CotTestConstants;
import gov.takx.api.coordinatesystem.ILocation;
import gov.takx.api.coordinatesystem.RaptorLocation;
import gov.takx.dataservices.shapes.Ellipse;
import org.junit.jupiter.api.Test;

import javax.naming.OperationNotSupportedException;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.AssertionsForClassTypes.catchThrowable;
import static org.assertj.core.api.AssertionsForClassTypes.within;

public class ShapeUtilsTest
{
    static
    {
        EngineLibrary.initialize();
    }

    private static final double DELTA = 1e-2;

    @Test
    public void testInterceptEta()
    {
        GeoPoint fromLocation = new GeoPoint(20, -60, 0);
        GeoPoint toLocation = new GeoPoint(34, -50, 0);
        double eta = ShapeUtils.interceptEta(fromLocation, 700d / 1.944, toLocation, 600d / 1.944, 220d);
        assertThat(eta).isCloseTo(2774.231622173239, CotTestConstants.EPSILON);
    }

    @Test
    public void when_RhumbAzimuthOriginAndDestMatch_returnsZero()
    {
        double azimuth = ShapeUtils.rhumbAzimuth(new GeoPoint(0, 0), new GeoPoint(0, 0));
        assertThat(azimuth).isEqualTo(0);
    }

    @Test
    public void when_LatitudeAndLongEqual_returnsRhumbAzimuthOf45()
    {
        // Equator & Prime Meridian is 0,0
        IGeoPoint startPoint = new GeoPoint(0, 0);
        IGeoPoint endPoint = new GeoPoint(1, 1);

        double azimuth = ShapeUtils.rhumbAzimuth(startPoint, endPoint);

        assertThat(azimuth).isCloseTo(45.0, within(DELTA));
    }

    @Test
    public void when_Crossing180thMeridianEastward_returnsRhumbAzimuthOf90()
    {
        IGeoPoint startPoint = new GeoPoint(0, 179);
        IGeoPoint endPoint = new GeoPoint(0, -179);
        double azimuth = ShapeUtils.rhumbAzimuth(startPoint, endPoint);

        assertThat(azimuth).isCloseTo(90.0, within(DELTA));
    }

    @Test
    public void when_GreatCircleAzimuthOriginAndDestMatch_returnsZero()
    {

        IGeoPoint startPoint = new GeoPoint(0, 0);
        IGeoPoint endPoint = new GeoPoint(0, 0);
        double actualAzimuth = ShapeUtils.greatCircleAzimuth(startPoint, endPoint);

        assertThat(actualAzimuth).isEqualTo(0);
    }

    @Test
    public void when_GreatCircleAzimuth_NorthAlongPrimeMeridian_returnsZero()
    {

        IGeoPoint startPoint = new GeoPoint(0, 0);

        // 1 degree North along the Prime Meridian
        IGeoPoint endPoint = new GeoPoint(1, 0);
        double actualAzimuth = ShapeUtils.greatCircleAzimuth(startPoint, endPoint);

        assertThat(actualAzimuth).isCloseTo(0, within(DELTA));
    }

    @Test
    public void when_GreatCircleAzimuthSouthAlongPrimeMeridian_returnsOneEighty()
    {

        IGeoPoint startPoint = new GeoPoint(1, 0);
        IGeoPoint endPoint = new GeoPoint(0, 0);
        double actualAzimuth = ShapeUtils.greatCircleAzimuth(startPoint, endPoint);

        assertThat(actualAzimuth).isCloseTo(180.0, within(DELTA));
    }

    @Test
    public void when_GreatCircleAzimuthEastAlongEquator_returnsNinety()
    {

        IGeoPoint startPoint = new GeoPoint(0, 0);
        IGeoPoint endPoint = new GeoPoint(0, 1);
        double actualAzimuth = ShapeUtils.greatCircleAzimuth(startPoint, endPoint);

        assertThat(actualAzimuth).isCloseTo(90.0, within(DELTA));
    }

    @Test
    public void when_GreatCircleAzimuthWestAlongEquator_returnsNegativeNinety()
    {

        IGeoPoint startPoint = new GeoPoint(0, 1);
        IGeoPoint endPoint = new GeoPoint(0, 0);
        double actualAzimuth = ShapeUtils.greatCircleAzimuth(startPoint, endPoint);

        assertThat(actualAzimuth).isCloseTo(-90, within(DELTA));
    }

    @Test
    public void when_GreatCircleAzimuthNortheast_returnsFortyFive()
    {

        IGeoPoint startPoint = new GeoPoint(0, 0);
        IGeoPoint endPoint = new GeoPoint(1, 1);
        double actualAzimuth = ShapeUtils.greatCircleAzimuth(startPoint, endPoint);

        assertThat(actualAzimuth).isCloseTo(45, within(DELTA));
    }

    @Test
    public void when_RhumbDistanceOriginAndDestinationMatch_returnsZero()
    {

        // Same start and end point
        IGeoPoint startPoint = new GeoPoint(0, 0);
        IGeoPoint endPoint = new GeoPoint(0, 0);
        double actualDistance = ShapeUtils.rhumbDistance(startPoint, endPoint);

        assertThat(actualDistance).isCloseTo(0.0, within(DELTA));
    }

    @Test
    public void when_RhumbDistanceEastAlongEquator_returnsOneDegreeRadian()
    {

        // 1 degree East along the Equator
        IGeoPoint startPoint = new GeoPoint(0, 0);
        IGeoPoint endPoint = new GeoPoint(0, 1);
        double actualDistance = ShapeUtils.rhumbDistance(startPoint, endPoint);

        assertThat(actualDistance).isCloseTo(Math.toRadians(1), within(DELTA));
    }

    @Test
    public void when_RhumbDistanceNorthAlongPrimeMeridian_returnsOneDegreeRadian()
    {
        // 1 degree North along the Prime Meridian
        IGeoPoint startPoint = new GeoPoint(0, 0);
        IGeoPoint endPoint = new GeoPoint(1, 0);
        double actualDistance = ShapeUtils.rhumbDistance(startPoint, endPoint);

        assertThat(actualDistance).isCloseTo(Math.toRadians(1), within(DELTA));
    }

    @Test
    public void when_RhumbDistanceNortheast_returnsExpectedDistance()
    {

        // from Equator & Prime Meridian
        IGeoPoint startPoint = new GeoPoint(0, 0);

        // 1 degree North and 1 degree East
        IGeoPoint endPoint = new GeoPoint(1, 1);
        double actualDistance = ShapeUtils.rhumbDistance(startPoint, endPoint);

        assertThat(actualDistance).isCloseTo(0.0246826, within(DELTA));
    }

    @Test
    public void when_RhumbDistanceAcrossDateLine_returnsTwoDegreesInRadians()
    {

        // Near the Date Line in the Eastern Hemisphere
        IGeoPoint startPoint = new GeoPoint(0, 179);

        // Near the Date Line in the Western Hemisphere
        IGeoPoint endPoint = new GeoPoint(0, -179);

        double actualDistance = ShapeUtils.rhumbDistance(startPoint, endPoint);

        assertThat(actualDistance).isCloseTo(Math.toRadians(2), within(DELTA));
    }

    @Test
    public void when_RhumbEndPositionEastByOneDegree_returnsExpectedEndPoint()
    {

        IGeoPoint startPoint = new GeoPoint(0, 0);
        double rhumbAzimuthDegrees = 90;

        // Due East by 1 degree
        double pathLengthDegrees = 1;

        IGeoPoint expectedEndPoint = new GeoPoint(0, 1);
        IGeoPoint actualEndPoint = ShapeUtils.rhumbEndPosition(startPoint, rhumbAzimuthDegrees, pathLengthDegrees);

        assertThat(actualEndPoint.getLatitude()).isCloseTo(expectedEndPoint.getLatitude(), within(DELTA));
        assertThat(actualEndPoint.getLongitude()).isCloseTo(expectedEndPoint.getLongitude(), within(DELTA));
    }

    @Test
    public void when_RhumbEndPositionNortheastByOneDegree_returnsExpectedEndPoint()
    {
        // Northeast by 1 degree
        IGeoPoint startPoint = new GeoPoint(45, 0); // 45 degrees N, Prime Meridian
        double rhumbAzimuthDegrees = 45;
        double pathLengthDegrees = 1;

        var expectedEndPoint = new GeoPoint(45.707778, 1.0075); // Taken from movable type results
        IGeoPoint actualEndPoint = ShapeUtils.rhumbEndPosition(startPoint, rhumbAzimuthDegrees, pathLengthDegrees);

        assertThat(actualEndPoint.getLatitude()).isCloseTo(expectedEndPoint.getLatitude(), within(DELTA));
        assertThat(actualEndPoint.getLongitude()).isCloseTo(expectedEndPoint.getLongitude(), within(DELTA));
    }

    @Test
    public void when_OriginAndDestinationMatch_greatCircleDistanceReturnsZero()
    {
        IGeoPoint startPoint = new GeoPoint(0, 0);
        IGeoPoint endPoint = new GeoPoint(0, 0);

        assertThat(ShapeUtils.greatCircleDistance(startPoint, endPoint)).isEqualTo(0);
    }

    @Test
    public void when_ShortDistanceBetweenPoints_greatCircleDistanceReturnsExpected()
    {
        IGeoPoint startPoint = new GeoPoint(0, 0);
        IGeoPoint endPoint = new GeoPoint(1, 1);
        double distance = ShapeUtils.greatCircleDistance(startPoint, endPoint);

        assertThat(distance).isCloseTo(0.024682, within(DELTA));
    }

    @Test
    public void when_meridianCrossing_GreatCircleDistanceReturnsExpected()
    {
        IGeoPoint startPoint = new GeoPoint(0, 0);
        IGeoPoint endPoint = new GeoPoint(0, 100);
        double distance = ShapeUtils.greatCircleDistance(startPoint, endPoint);

        assertThat(distance).isCloseTo(1.745329, within(DELTA));
    }

    @Test
    public void when_RectangleSpansDLMeridian_isTrue()
    {
        // vertices provided form a rectangle that does spans the Date Line Meridian.
        GeoPoint vertex1 = new GeoPoint(0, 170);
        GeoPoint vertex2 = new GeoPoint(10, 170);
        GeoPoint vertex3 = new GeoPoint(10, -170);
        GeoPoint vertex4 = new GeoPoint(0, -170);

        assertThat(ShapeUtils.rectangleSpansDateLineMeridian(List.of(vertex1, vertex2, vertex3, vertex4))).isTrue();
    }

    @Test
    public void when_NotSpanningDL_SpansDLMeridian_isFalse()
    {
        // vertices provided do not form a rectangle that spans the Date Line Meridian.
        GeoPoint vertex1 = new GeoPoint(0, -70);
        GeoPoint vertex2 = new GeoPoint(10, -70);
        GeoPoint vertex3 = new GeoPoint(10, 170);
        GeoPoint vertex4 = new GeoPoint(0, 170);

        assertThat(ShapeUtils.rectangleSpansDateLineMeridian(List.of(vertex1, vertex2, vertex3, vertex4))).isFalse();
    }

    @Test
    public void when_InvalidInput_RectangleSpansDLMeridian_IsNull()
    {
        assertThat(ShapeUtils.rectangleSpansDateLineMeridian(null)).isFalse();
    }

    @Test
    public void when_irregularRectangle_RectangleSpansDLMeridian_throwsException()
    {
        GeoPoint vertex1 = new GeoPoint(0, 170);
        GeoPoint vertex2 = new GeoPoint(10, 170);
        GeoPoint vertex3 = new GeoPoint(10, -170);

        // having a point not parallel with sister bottom half will make it not a rectangle, triggering IllegalArgumentException
        GeoPoint vertex4 = new GeoPoint(20, -170);

        assertThat(catchThrowable(() -> {
            ShapeUtils.rectangleSpansDateLineMeridian(List.of(vertex1, vertex2, vertex3, vertex4));
        })).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    public void when_singlePointIntersectPolygon_ReturnsTrue()
    {
        List<ILocation> polygon1 = getGenericPolygonWithSixPoints();

        List<ILocation> locations2 = new ArrayList<>();
        locations2.add(new RaptorLocation(1, 1));
        locations2.add(new RaptorLocation(5, 5));
        locations2.add(new RaptorLocation(11, 11));
        locations2.add(new RaptorLocation(8, 5));
        locations2.add(new RaptorLocation(4, 2));
        locations2.add(new RaptorLocation(1, 1));

        assertThat(ShapeUtils.polygonIntersectsShape(polygon1, locations2)).isTrue();
    }

    @Test
    public void when_singlePointIntersectsPolygon_polygonIntersectsShapeIsTrue()
    {
        List<ILocation> polygon1 = getGenericPolygonWithSixPoints();

        List<ILocation> locations2 = new ArrayList<>();
        locations2.add(new RaptorLocation(5, 5));
        locations2.add(new RaptorLocation(8, 9));
        locations2.add(new RaptorLocation(11, 11));
        locations2.add(new RaptorLocation(4, 2));
        locations2.add(new RaptorLocation(3, 1));
        locations2.add(new RaptorLocation(2, 8));

        assertThat(ShapeUtils.polygonIntersectsShape(polygon1, locations2)).isTrue();
    }

    @Test
    public void when_noPointsIntersectsPolygon_polygonIntersectsShapeIsFalse()
    {
        List<ILocation> polygon1 = getGenericPolygonWithSixPoints();

        List<ILocation> locations2 = new ArrayList<>();
        locations2.add(new RaptorLocation(5, 5));
        locations2.add(new RaptorLocation(8, 9));
        locations2.add(new RaptorLocation(11, 9.9));
        locations2.add(new RaptorLocation(4, 2));
        locations2.add(new RaptorLocation(3, 1));
        locations2.add(new RaptorLocation(2, 8));

        assertThat(ShapeUtils.polygonIntersectsShape(polygon1, locations2)).isFalse();
    }

    @Test
    public void when_pointsOnEdgesAndVertices_polygonContainsPointReturnsFalse()
    {

        // Define the vertices of the polygon (a simple square)
        List<Double> xVertices = Arrays.asList(0.0, 4.0, 4.0, 0.0);
        List<Double> yVertices = Arrays.asList(0.0, 0.0, 4.0, 4.0);

        // Define points on the edge of the polygon, not 'contained'
        double b = 0.0, y2 = 2.0;
        assertThat(ShapeUtils.polygonContainsPoint(xVertices, yVertices, b, y2)).isFalse();

        // Define points exactly at the vertices of the polygon
        double d = 0.0, y4 = 0.0;
        assertThat(ShapeUtils.polygonContainsPoint(xVertices, yVertices, d, y4)).isFalse();

        // Define points outside the polygon
        double c = 5.0, y3 = 5.0;
        assertThat(ShapeUtils.polygonContainsPoint(xVertices, yVertices, c, y3)).isFalse();
    }

    @Test
    public void when_hasToCalculateWithZero_polygonContainsPoint_isCorrect()
    {

        List<Double> xVertices = Arrays.asList(0.0, -4.0, -4.0, 0.0, 0.0);
        List<Double> yVertices = Arrays.asList(0.0, 0.0, 4.0, 4.0, 0.0);

        // this test is setup so that two consecutive vertices share the same Y-coordinate, creating situation where method {@link ShapeUtils#polygonContainsPoint}
        // might divide by zero
        // point that lies inside the polygon
        double xInside = -2.0;
        double yInside = 2.0;
        assertThat(ShapeUtils.polygonContainsPoint(xVertices, yVertices, xInside, yInside)).isTrue();

        // point that lies exactly on the edge (horizontal edge)
        double xOnEdge = -1.0;
        double yOnEdge = 2.0;
        assertThat(ShapeUtils.polygonContainsPoint(xVertices, yVertices, xOnEdge, yOnEdge)).isTrue();

        // point that lies outside the polygon
        double xOutside = 5.0;
        double yOutside = 5.0;
        assertThat(ShapeUtils.polygonContainsPoint(xVertices, yVertices, xOutside, yOutside)).isFalse();

        // point that lies exactly on a vertex
        double xOnVertex = 0.0;
        double yOnVertex = 0.0;
        assertThat(ShapeUtils.polygonContainsPoint(xVertices, yVertices, xOnVertex, yOnVertex)).isFalse();
    }

    @Test
    public void when_polygonContainsEllipse_returnsTrue() throws OperationNotSupportedException
    {
        // Create an ellipse
        Ellipse ellipse = new Ellipse("TestEllipse", new RaptorLocation(5, 5), 0, 2, 1);

        // Define a polygon that contains the ellipse
        List<ILocation> polygonVertices = Arrays.asList(
                new RaptorLocation(0, 0),
                new RaptorLocation(0, 10),
                new RaptorLocation(10, 10),
                new RaptorLocation(10, 0));

        // Test if the ellipse is contained in the polygon
        assertThat(ShapeUtils.polygonContainsShape(polygonVertices, ellipse)).isTrue();
    }

    @Test
    public void when_notPolygonContainsEllipse_returnsFalse()
    {
        // Create an ellipse
        Ellipse ellipse = new Ellipse("TestEllipse", new RaptorLocation(5, 5), 0, 5, 2);

        // Define a polygon that does not contain the ellipse
        List<ILocation> polygon = Arrays.asList(
                new RaptorLocation(0, 0),
                new RaptorLocation(0, 4),
                new RaptorLocation(4, 4),
                new RaptorLocation(4, 0));

        // Check if the ellipse is contained within the polygon
        assertThat(ShapeUtils.polygonContainsEllipse(polygon, ellipse)).isFalse();
    }

    @Test
    public void when_polygonContainsLocation()
    {
        List<ILocation> polygon1 = getGenericPolygonWithSixPoints();
        assertThat(ShapeUtils.polygonContainsLocation(polygon1, new RaptorLocation(15, 15))).isTrue();
        assertThat(ShapeUtils.polygonContainsLocation(polygon1, new RaptorLocation(15, 10))).isTrue();
        assertThat(ShapeUtils.polygonContainsLocation(polygon1, new RaptorLocation(15, 20))).isTrue();
        assertThat(ShapeUtils.polygonContainsLocation(polygon1, new RaptorLocation(12.5, 15))).isTrue();
        assertThat(ShapeUtils.polygonContainsLocation(polygon1, new RaptorLocation(10, 5))).isFalse();
        assertThat(ShapeUtils.polygonContainsLocation(polygon1, new RaptorLocation(20, 5))).isFalse();
        assertThat(ShapeUtils.polygonContainsLocation(polygon1, new RaptorLocation(10, 25))).isFalse();
        assertThat(ShapeUtils.polygonContainsLocation(polygon1, new RaptorLocation(20, 25))).isFalse();
    }

    @Test
    public void when_polygonContainsLocationTouchesEdges()
    {
        List<ILocation> polygon1 = getGenericPolygonWithSixPoints();
        for (ILocation location : polygon1)
        {
            assertThat(ShapeUtils.polygonContainsLocation(polygon1, location)).describedAs("testing location %s ", location).isTrue();
        }
        // Reverse the winding
        ArrayList<ILocation> polygon2 = new ArrayList<>(polygon1);
        Collections.reverse(polygon2);
        for (ILocation location : polygon2)
        {
            assertThat(ShapeUtils.polygonContainsLocation(polygon2, location)).describedAs("testing reverse winding location %s ", location).isTrue();
        }
        assertThat(ShapeUtils.polygonContainsLocation(polygon1, new RaptorLocation(15, 10))).isTrue();
        assertThat(ShapeUtils.polygonContainsLocation(polygon1, new RaptorLocation(15, 10))).isTrue();
        assertThat(ShapeUtils.polygonContainsLocation(polygon1, new RaptorLocation(15, 20))).isTrue();
        assertThat(ShapeUtils.polygonContainsLocation(polygon1, new RaptorLocation(15, 20))).isTrue();
        assertThat(ShapeUtils.polygonContainsLocation(polygon1, new RaptorLocation(7.5, 12.5))).isTrue();
        assertThat(ShapeUtils.polygonContainsLocation(polygon1, new RaptorLocation(22.5, 12.5))).isTrue();
    }

    private static List<ILocation> getGenericPolygonWithSixPoints()
    {
        return List.of(
                new RaptorLocation(10, 10),
                new RaptorLocation(20, 10),
                new RaptorLocation(25, 15),
                new RaptorLocation(20, 20),
                new RaptorLocation(10, 20),
                new RaptorLocation(5, 15));
    }
}
