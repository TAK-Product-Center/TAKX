package gov.takx.test.mock.plugin.device;

import gov.takx.api.coordinatesystem.ILocation;
import gov.takx.api.coordinatesystem.RaptorLocation;
import gov.takx.api.mapobject.MapToolType;
import gov.takx.api.messages.IRaptorDataMessage;
import gov.takx.api.plugin.device.ADevicePluginController;
import gov.takx.api.plugin.device.IDevicePluginDelegate;
import gov.takx.api.shapes.IMutableShape;
import gov.takx.api.shapes.IShapeService;
import gov.takx.api.shapes.IShapeService.ShapeOptions;
import gov.takx.api.shapes.ShapeAlreadyExistsException;
import gov.takx.dataservices.shapes.Circle;
import gov.takx.dataservices.shapes.Ellipse;
import gov.takx.dataservices.shapes.Polygon;
import gov.takx.dataservices.shapes.Polyline;
import gov.takx.dataservices.shapes.Rectangle;
import gov.takx.dataservices.shapes.Square;
import gov.takx.platform.utils.ShapeUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.invoke.MethodHandles;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 */
public class TestDevicePluginController extends ADevicePluginController
{
    private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());
    private IShapeService shapeService;
    private static final String GEOFENCE_NAME = "geoFence";
    private static final String SHAPE_NAME = "shape";
    private RaptorLocation lastKnownLocation = null;
    private final Random random = new Random();

    private boolean deleteCalled = false;

    public TestDevicePluginController(String unitID)
    {
        super(null, null, unitID);
    }

    @Override
    public void setDevicePluginDelegate(IDevicePluginDelegate mapObjectDelegate)
    {
        super.setDevicePluginDelegate(mapObjectDelegate);
        mapObjectDelegate.setDescription("Test Device");
        shapeService = mapObjectDelegate.getShapeService();
    }

    @Override
    public void receiveMessage(IRaptorDataMessage message)
    {
        lastKnownLocation = new RaptorLocation(message.getLatitude(), message.getLongitude(), message.getElevation());
    }

    @Override
    public void delete()
    {
        super.delete();
        deleteCalled = true;
    }

    public boolean isDeleteCalled()
    {
        return deleteCalled;
    }

    /**
     * Create a geofence (a published shape) of the given type.
     *
     * @param shapeType Type of shape to create
     * @return shape
     */
    public IMutableShape createGeofence(MapToolType shapeType)
    {
        return createShape(shapeType, true);
    }

    /**
     * Create a normal (unpublished) shape of the given type.
     *
     * @param shapeType Type of shape to create
     * @return shape
     */
    public IMutableShape createShape(MapToolType shapeType)
    {
        return createShape(shapeType, false);
    }

    /**
     * Create a shape of the given type with the given published setting.
     *
     * @param shapeType Type of shape to create
     * @param published {@code true} to publish the shape, {@code false} otherwise
     * @return shape
     */
    private IMutableShape createShape(MapToolType shapeType, boolean published)
    {
        final String shapeName = (published ? GEOFENCE_NAME : SHAPE_NAME) + "_" + shapeType;
        try
        {
            final ShapeOptions shapeOptions = new ShapeOptions(published);
            return switch (shapeType)
            {
                case DRAWING_CIRCLE -> shapeService.createCircleMapObject(new Circle(shapeName, lastKnownLocation, 150.0), shapeOptions);
                case DRAWING_ELLIPSE -> shapeService.createEllipseMapObject(new Ellipse(shapeName, lastKnownLocation, 0.0, 200.0, 100.0), shapeOptions);
                case DRAWING_PATH -> shapeService.createPolylineMapObject(new Polyline(shapeName, getPolyList()), shapeOptions);
                case DRAWING_POLYGON -> shapeService.createPolygonMapObject(new Polygon(shapeName, getPolyList()), shapeOptions);
                case DRAWING_QUAD -> shapeService.createRectangleMapObject(new Rectangle(shapeName, lastKnownLocation, 0.0, 200.0, 100.0), shapeOptions);
                case DRAWING_SQUARE -> shapeService.createSquareMapObject(new Square(shapeName, lastKnownLocation, 0.0, 150.0), shapeOptions);
                default -> throw new RuntimeException("Unknown geofence type: " + shapeType);
            };
        } catch (ShapeAlreadyExistsException e)
        {
            logger.error("Error", e);
            throw new RuntimeException(e);
        }
    }

    /**
     * Create a rectangle using lat/lon bounds that will throw an exception due to the min corner being greater
     * than the max corner.
     *
     * @throws IllegalArgumentException    on purpose
     * @throws ShapeAlreadyExistsException should never happen
     */
    public void createBogusRectangle() throws IllegalArgumentException, ShapeAlreadyExistsException
    {
        ILocation minCorner = new RaptorLocation(38.0, 38.0);
        ILocation maxCorner = new RaptorLocation(28.0, 28.0);

        shapeService.createRectangleMapObject(ShapeUtils.calculateRectangleFromMinMaxCorners("BOGUS", minCorner, maxCorner));
    }

    /**
     * @return A map of all the shapes created by this plugin
     */
    public List<IMutableShape> getShapeList()
    {
        return shapeService.getMutableShapes();
    }

    private List<ILocation> getPolyList()
    {
        List<ILocation> locs = new ArrayList<>();
        double lat = lastKnownLocation.getLatitude();
        double lon = lastKnownLocation.getLongitude();
        locs.add(new RaptorLocation((lat + (random.nextDouble() * .1)) % 90, (lon + (random.nextDouble() * .1)) % 180));
        locs.add(new RaptorLocation((lat + (random.nextDouble() * .1)) % 90, (lon - (random.nextDouble() * .1)) % 180));
        locs.add(new RaptorLocation((lat - (random.nextDouble() * .1)) % 90, (lon + (random.nextDouble() * .1)) % 180));
        locs.add(new RaptorLocation((lat - (random.nextDouble() * .1)) % 90, (lon - (random.nextDouble() * .1)) % 180));
        return locs;
    }

    /**
     * @return The shape service for this device
     */
    public IShapeService getShapeService()
    {
        return shapeService;
    }
}
