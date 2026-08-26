package gov.takx.shapes;

import gov.takx.api.coordinatesystem.ILocation;
import gov.takx.api.coordinatesystem.RaptorLocation;
import gov.takx.api.shapes.IMutableCircle;
import gov.takx.api.shapes.IMutablePolygon;
import gov.takx.api.shapes.ShapeAlreadyExistsException;
import gov.takx.core.shapes.ShapeService;
import gov.takx.core.shapes.ShapeServiceFactory;
import gov.takx.dataservices.shapes.Circle;
import gov.takx.dataservices.shapes.Polygon;
import gov.takx.dataservices.shapes.Square;
import gov.takx.platform.utils.ShapeUtils;
import gov.takx.test.AIntegrationTest;
import gov.takx.test.mock.plugin.device.TestDevicePlugin;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import javax.naming.OperationNotSupportedException;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 */
public class ShapeContainmentTest extends AIntegrationTest
{
    // Put the location right on the equator and prime meridian to make things interesting
    private static final ILocation SQUARE_LOCATION = new RaptorLocation(0, 0);
    private static final ILocation MIN_RECTANGLE_LOCATION = new RaptorLocation(-20, -20);
    private static final ILocation MAX_RECTANGLE_LOCATION = new RaptorLocation(20, 20);

    private ShapeService shapeService;

    public ShapeContainmentTest()
    {
        setPluginsToRegister(TestDevicePlugin.class);
    }

    @Override
    protected void postProjectOpen()
    {
        ShapeServiceFactory shapeServiceFactory = getBean(ShapeServiceFactory.class);
        shapeService = shapeServiceFactory.getOrCreateCoreShapeService();
    }

    @AfterEach
    public void cleanupAfterMethod()
    {
        deleteAllMapObjects();
    }

    @Test
    public void testLocationContainment() throws OperationNotSupportedException, ShapeAlreadyExistsException
    {
        var square = shapeService.createSquareMapObject(new Square("Square 1", SQUARE_LOCATION, 0, 1000));
        var rectangle = shapeService.createRectangleMapObject(ShapeUtils.calculateRectangleFromMinMaxCorners("Rectangle 1", MIN_RECTANGLE_LOCATION, MAX_RECTANGLE_LOCATION));

        List<ILocation> locations = new ArrayList<>();
        locations.add(new RaptorLocation(20, 20));
        locations.add(new RaptorLocation(10, -20));
        locations.add(new RaptorLocation(-20, -20));
        locations.add(new RaptorLocation(-50, 0));
        locations.add(new RaptorLocation(-30, 40));
        IMutablePolygon polygon = shapeService.createPolygonMapObject(new Polygon("Polygon 1", locations));
        IMutableCircle circle = shapeService.createCircleMapObject(new Circle("Circle 1", new RaptorLocation(0, 0), 100));

        assertThat(square.contains(SQUARE_LOCATION)).isTrue();

        assertThat(rectangle.contains(new RaptorLocation(30, 30))).isFalse();
        // Check a mid-point
        assertThat(rectangle.contains(new RaptorLocation(10, 10))).isTrue();
        // Ensure that all the vertices are contained
        for (ILocation vertex : square.getVertices()) assertThat(rectangle.contains(vertex)).isTrue();

        assertThat(polygon.contains(new RaptorLocation(10, 10))).isTrue();

        assertThat(circle.contains(new RaptorLocation(0, 0))).isTrue();
        assertThat(circle.contains(new RaptorLocation(5, 5))).isFalse();
    }

    /*
     IShapeService shapeService = devicePluginDelegate.getShapeService();
    
     try
     {
     ILocation squareLocation = new RaptorLocation(0, 0);
    
     List<ILocation> poly1Locations = new ArrayList<>();
     poly1Locations.add(new RaptorLocation(0.00385, 0.00385));
     poly1Locations.add(new RaptorLocation(0.1, 0.1));
     poly1Locations.add(new RaptorLocation(0.00385, 0.1));
    
     List<ILocation> poly2Locations = new ArrayList<>();
     poly2Locations.add(new RaptorLocation(0.004, 0.004));
     poly2Locations.add(new RaptorLocation(0.09, 0.09));
     poly2Locations.add(new RaptorLocation(0.004, 0.09));
    
     List<ILocation> polyline1Locations = new ArrayList<>();
     polyline1Locations.add(new RaptorLocation(0.004, 0.004));
     polyline1Locations.add(new RaptorLocation(-0.09, -0.09));
    
     IShape square = shapeService.createSquare("Square", squareLocation, 0d, 1000d);
     IShape poly1 = shapeService.createPolygon("Poly1", poly1Locations);
     IShape poly2 = shapeService.createPolygon("Poly2", poly2Locations);
     IShape line1 = shapeService.createPolyline("Polyline1", polyline1Locations);
    
     assertTrue(square.contains(new RaptorLocation(0.00385, 0.00385)));
    
     assertTrue(poly1.intersects(square));
     assertTrue(square.intersects(poly1));
     assertTrue(poly1.intersects(poly2));
    
     assertFalse(poly1.contains(square));
     assertFalse(square.contains(poly1));
     assertTrue(poly1.contains(poly2));
     assertFalse(poly2.contains(poly1));
    
     assertTrue(square.intersects(line1));
     assertFalse(square.contains(line1));
    
     } catch (Exception e)
     {
     logger.error("ERROR", e);
     }
     */
}
