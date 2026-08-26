package gov.takx.core.shapes;

import gov.tak.api.util.StandardAttributeNames;
import gov.tak.platform.symbology.milstd2525.MilSymStandardAttributes;
import gov.tak.test.utils.CotTestConstants;
import gov.takx.api.coordinatesystem.RaptorLocation;
import gov.takx.api.gateway.IDeviceGateway;
import gov.takx.api.mapobject.IMapObject;
import gov.takx.api.mapobject.IMapObjectController;
import gov.takx.api.mapobject.IMapObjectManager;
import gov.takx.api.mapobject.IShapeCircleMapObject;
import gov.takx.api.mapobject.IShapeMapObject;
import gov.takx.api.mapobject.IShapeSquareMapObject;
import gov.takx.api.mapobject.MapToolType;
import gov.takx.api.milsym.MilStd2525Configuration;
import gov.takx.api.persist.IMapObjectPersist;
import gov.takx.api.plugin.gateway.IGatewayController;
import gov.takx.api.shapes.IIrregularShape;
import gov.takx.api.shapes.IMutableRectangle;
import gov.takx.api.shapes.IMutableShape;
import gov.takx.api.shapes.IMutableSquare;
import gov.takx.api.shapes.IRectangle;
import gov.takx.api.shapes.IShape;
import gov.takx.api.shapes.IShapeFilter;
import gov.takx.api.shapes.IShapeFilterBuilder;
import gov.takx.api.shapes.IShapeListener;
import gov.takx.api.shapes.IShapeService;
import gov.takx.api.shapes.IShapeService.ShapeOptions;
import gov.takx.api.shapes.IShapeServiceListener;
import gov.takx.api.shapes.InvalidShapeFilterException;
import gov.takx.api.shapes.NoSuchShapeException;
import gov.takx.api.shapes.ShapeAlreadyExistsException;
import gov.takx.api.shapes.ShapeProperties;
import gov.takx.api.shapes.ShapeType;
import gov.takx.dataservices.shapes.Circle;
import gov.takx.dataservices.shapes.ShapeFilterBuilder;
import gov.takx.dataservices.shapes.Square;
import gov.takx.platform.KeyUtils;
import gov.takx.platform.utils.ShapeUtils;
import gov.takx.test.AIntegrationTest;
import gov.takx.test.extension.DependsOn;
import gov.takx.test.extension.DependsOnOrdering;
import gov.takx.test.mock.plugin.device.TestDevicePlugin;
import gov.takx.test.mock.plugin.device.TestDevicePluginController;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.Color;
import java.beans.PropertyChangeEvent;
import java.io.IOException;
import java.lang.invoke.MethodHandles;
import java.util.Arrays;
import java.util.EnumMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

/**
 */
@TestMethodOrder(DependsOnOrdering.class)
public class ShapeServiceTest extends AIntegrationTest
{
    private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    private static final String UNIT_ID_1 = "1";
    private static final String UNIT_ID_2 = "2";

    private static final ShapeType[] ALL_SHAPE_TYPES = {
            ShapeType.CIRCLE, ShapeType.SQUARE, ShapeType.RECTANGLE, ShapeType.ELLIPSE,
            ShapeType.POLYLINE, ShapeType.POLYGON
    };

    private TestDevicePlugin plugin;
    private IGatewayController gc1;
    private IGatewayController gc2;
    private final String mapObjectKey1 = KeyUtils.getUniqueKey(TestDevicePlugin.FAMILY, TestDevicePlugin.DEVICE_NAME, UNIT_ID_1);
    private final String mapObjectKey2 = KeyUtils.getUniqueKey(TestDevicePlugin.FAMILY, TestDevicePlugin.DEVICE_NAME, UNIT_ID_2);
    private IMapObjectManager cmom;
    private IMapObjectController cmoc1;
    private IMapObjectController cmoc2;
    private TestDevicePluginController tpc1;
    private TestDevicePluginController tpc2;
    private IShapeSquareMapObject publishedSquare;
    private IShapeCircleMapObject publishedCircle;
    private final ShapeServiceListener ssl1 = new ShapeServiceListener();
    private final ShapeServiceListener ssl2 = new ShapeServiceListener();
    private final Map<MapToolType, String> ss1ShapeNames = new EnumMap<>(MapToolType.class);

    private int expectedPublishCount = 0;
    private int expectedUnpublishCount = 0;
    private int expectedDefinedCount = 0;
    private int expectedDeletionCount = 0;
    private static final double SQUARE_ORIENTATION = 30.0;
    private static final Color CIRCLE_COLOR = Color.orange;

    protected final ShapeServiceFactory shapeServiceFactory = getBean(ShapeServiceFactory.class);

    public ShapeServiceTest()
    {
        setPluginsToRegister(TestDevicePlugin.class);
    }

    /**
     * Reboot the core - bring our plugin online afterward.
     */
    @Override
    protected void reboot() throws IOException
    {
        super.reboot();
        bringPluginOnline();
    }

    /**
     * Send newly generated RDM using both plugins.  The plugin's Gateway Controller must have been initialized before
     * calling this method.
     */
    private void sendMessages()
    {
        gc1.processParserMessage(plugin.generateMessage(UNIT_ID_1));
        gc2.processParserMessage(plugin.generateMessage(UNIT_ID_2));
    }

    /**
     * Initialize the plugin instance (bring it online).
     */
    @Test
    public void bringPluginOnline()
    {
        if (logger.isDebugEnabled()) logger.debug("testing bringPluginOnline");

        plugin = getPluginInstance(TestDevicePlugin.FAMILY, TestDevicePlugin.DEVICE_NAME);

        cmom = getBean(IMapObjectManager.class);
        final IDeviceGateway gateway = getBean(IDeviceGateway.class);

        // Create some published shapes before the plugin map objects are created so we can verify that they are
        // available via getShapes (this requires the "reattach" functionality to work properly)

        final ShapeService coreShapeService = shapeServiceFactory.getOrCreateCoreShapeService();

        RaptorLocation location = new RaptorLocation(34.0, -119.0);

        try
        {
            publishedCircle = coreShapeService.createCircleMapObject(new Circle("published circle", location, 10.0), ShapeOptions.PUBLISHED);
            publishedCircle.setLineColor(CIRCLE_COLOR);

            publishedSquare = coreShapeService.createSquareMapObject(new Square("published_square", location, SQUARE_ORIENTATION, 10.0), ShapeOptions.PUBLISHED);
        } catch (ShapeAlreadyExistsException e)
        {
            throw new RuntimeException("Failed to create shape", e);
        }

        gc1 = gateway.getController(plugin.getFamily(), plugin.getType(), UNIT_ID_1);
        gc2 = gateway.getController(plugin.getFamily(), plugin.getType(), UNIT_ID_2);
    }

    @Test
    @DependsOn("bringPluginOnline")
    public void sendInitialMessages()
    {
        logger.debug("testing sendInitialMessages");

        sendMessages();

        // Now validate that the system has the proper state after plugin comes online
        // 4 = 2 shapes + 2 devices
        assertThat(cmom.getMapObjectControllers()).hasSize(4);

        cmoc1 = cmom.getMapObjectController(mapObjectKey1);
        assertThat(cmoc1).isNotNull();
        assertThat(cmoc1.getMapObject().getUnitID()).isEqualTo(UNIT_ID_1);
        tpc1 = (TestDevicePluginController) cmoc1.getPluginController();
        tpc1.getShapeService().addShapeServiceListener(ssl1);

        cmoc2 = cmom.getMapObjectController(mapObjectKey2);
        assertThat(cmoc2).isNotNull();
        assertThat(cmoc2.getMapObject().getUnitID()).isEqualTo(UNIT_ID_2);
        tpc2 = (TestDevicePluginController) cmoc2.getPluginController();
        tpc2.getShapeService().addShapeServiceListener(ssl2);
    }

    public Iterable<MapToolType> provideShapeTypes()
    {
        // Provide each of the shape types to be tested
        return Arrays.asList(
                MapToolType.DRAWING_CIRCLE,
                MapToolType.DRAWING_SQUARE,
                MapToolType.DRAWING_QUAD,
                MapToolType.DRAWING_ELLIPSE,
                MapToolType.DRAWING_PATH,
                MapToolType.DRAWING_POLYGON);
    }

    @ParameterizedTest
    @DependsOn("sendInitialMessages")
    @MethodSource("provideShapeTypes")
    public void createShapes(MapToolType toolType)
    {
        sendMessages();

        IMutableShape shape = tpc1.createGeofence(toolType);
        checkShapeServiceListenerValues(1, 1, 0, 0);    // Should have one more published
        testBasicShapeListener(shape);

        // Test shapes coming from a second shape service
        tpc2.createGeofence(toolType);
        checkShapeServiceListenerValues(1, 1, 0, 0);    // Should have one more published

        // Create un-published shapes as well
        shape = tpc1.createShape(toolType);
        checkShapeServiceListenerValues(0, 0, 0, 0);    // Should be no change
        ss1ShapeNames.put(toolType, shape.getName());

        tpc2.createShape(toolType);
        checkShapeServiceListenerValues(0, 0, 0, 0);    // Should be no change

        // Ensure that the shape is owned by the feature and not the device
        assertThat(shape).isNotNull();
    }

    /**
     * Create a bogus rectangle and ensure that the proper exception is thrown.
     */
    @Test
    @DependsOn("sendInitialMessages")
    public void createBogusRectangle()
    {
        assertThatIllegalArgumentException()
                .isThrownBy(() -> tpc1.createBogusRectangle());
    }

    @Test
    void create_usingMil2525Configuration_properlyCreatesShape() throws NoSuchShapeException
    {
        final Circle circle = new Circle("unused", CotTestConstants.ZERO_LOC, 100);
        final String name = "my circle";

        // This code that requires a circle
        final String symbolCode = "G*F*ACVC--****X";
        final var configuration = MilStd2525Configuration.builder(symbolCode)
                .name(name)
                .shape(circle)
                .withModifier("T", "T value")
                .withModifier("W", "W value")
                .initializer(smo -> smo.setMetaBoolean("I ran", true))
                .build();

        final ShapeService coreShapeService = shapeServiceFactory.getOrCreateCoreShapeService();

        final Circle mil2525Graphic = coreShapeService.createMil2525Graphic(configuration);
        final IShapeMapObject<?> shapeMapObject = mil2525Graphic.getShapeMapObject();

        assertThat(mil2525Graphic.getName()).isEqualTo("my circle");
        assertThat(mil2525Graphic.getLocation()).isEqualTo(CotTestConstants.ZERO_LOC);
        assertThat(mil2525Graphic.getRadius()).isEqualTo(100);
        assertThat(shapeMapObject.getMetaString(StandardAttributeNames.MIL_SYM, "")).isEqualTo(symbolCode);
        assertThat(shapeMapObject.getMetaString(MilSymStandardAttributes.MILSYM_MODIFIER_PREFIX + "T", "")).isEqualTo("T value");
        assertThat(shapeMapObject.getMetaString(MilSymStandardAttributes.MILSYM_MODIFIER_PREFIX + "W", "")).isEqualTo("W value");
        assertThat(shapeMapObject.getMetaBoolean("I ran", false)).isTrue();

        coreShapeService.deleteShape(name);
    }

    @Test
    public void test_shape_filter_invalid_shape_type_throws()
    {
        final ShapeFilterBuilder fb = new ShapeFilterBuilder();
        assertThatExceptionOfType(InvalidShapeFilterException.class)
                .isThrownBy(() -> fb.defineFilter(IShapeFilter.SHAPE_TYPE, "bogus"));
    }

    @Test
    @DependsOn({"createShapes", "createBogusRectangle"})
    public void countShapes() throws InvalidShapeFilterException
    {
        final int typeCount = ALL_SHAPE_TYPES.length;
        final int doubleCount = typeCount * 2;

        assertThat(tpc1.getShapeList().size()).isEqualTo(doubleCount);
        assertThat(tpc2.getShapeList().size()).isEqualTo(doubleCount);

        IMapObjectPersist p = getBean(IMapObjectPersist.class);
        List<IMapObject> list = p.findAllMapObjects();
        // 4 directly created shapes, and doubleCount shapes via 2 device plugins
        assertThat(list).hasSize(4 + 2 * doubleCount);

        // Ensure that the published and unpublished sets are correct
        // We should have a total of 20 published (2 + doubleCount) and typeCount unpublished
        countShapesByPublishedSetting(tpc1.getShapeService(), 2 + doubleCount, typeCount);
        countShapesByPublishedSetting(tpc2.getShapeService(), 2 + doubleCount, typeCount);
    }

    /**
     * Count and validate the number of shapes in two buckets - published and unpublished.
     *
     * @param shapeService The shape service to use
     * @param pubCount     Number of published shapes expected
     * @param unpubCount   Number of unpublished shapes expected
     * @throws InvalidShapeFilterException only if something is broken
     */
    private void countShapesByPublishedSetting(IShapeService shapeService, int pubCount, int unpubCount)
            throws InvalidShapeFilterException
    {
        IShapeFilterBuilder fb = shapeService.createShapeFilterBuilder();
        fb.defineFilter(IShapeFilter.PUBLISHED_FLAG, true);
        List<IShape> shapes = shapeService.getShapes(fb.build());
        assertThat(shapes).isNotNull();
        assertThat(shapes).hasSize(pubCount);

        // And 9 unpublished
        fb = shapeService.createShapeFilterBuilder();
        fb.defineFilter(IShapeFilter.PUBLISHED_FLAG, false);
        shapes = shapeService.getShapes(fb.build());
        assertThat(shapes).isNotNull();
        assertThat(shapes).hasSize(unpubCount);
    }

    @Test
    @DependsOn("countShapes")
    public void playWithShapes() throws InvalidShapeFilterException
    {
        // Try to locate shapes using various filters
        final IShapeService ss1 = tpc1.getShapeService();
        final IShapeService ss2 = tpc2.getShapeService();

        // There should be 3 published squares and 4 published circles

        // Ask SS1 about the squares
        IShapeFilterBuilder fb = ss1.createShapeFilterBuilder();
        fb.defineFilter(IShapeFilter.SHAPE_TYPE, IShapeFilter.SQUARE);
        fb.defineFilter(IShapeFilter.PUBLISHED_FLAG, true);
        List<IShape> shapes = ss1.getShapes(fb.build());
        assertThat(shapes).isNotNull()
                .hasSize(3)
                .extracting(IShape::getType).containsOnly(IShapeFilter.SQUARE.getShapeType());

        // Ask SS2 about the circles (it shouldn't matter which one we query for published shapes)
        fb = ss2.createShapeFilterBuilder();
        fb.defineFilter(IShapeFilter.SHAPE_TYPE, IShapeFilter.CIRCLE);
        fb.defineFilter(IShapeFilter.PUBLISHED_FLAG, true);
        shapes = ss2.getShapes(fb.build());
        assertThat(shapes).isNotNull()
                .hasSize(3)
                .extracting(IShape::getType).containsOnly(IShapeFilter.CIRCLE.getShapeType());

        // Asking SS2 for MUTABLE shapes with this same filter should only give us 1 - the one directly created on it
        List<IMutableShape> mutableShapes = ss2.getMutableShapes(fb.build());
        assertThat(mutableShapes).isNotNull()
                .hasSize(1)
                .extracting(IShape::getType).containsOnly(IShapeFilter.CIRCLE.getShapeType());

        // Now lets go after unpublished shapes
        for (ShapeType shapeType : ALL_SHAPE_TYPES)
        {
            fb = ss2.createShapeFilterBuilder();
            fb.defineFilter(IShapeFilter.SHAPE_TYPE, shapeType);
            mutableShapes = ss2.getMutableShapes(fb.build());
            assertThat(mutableShapes).isNotNull().hasSize(2);
            assertThat(mutableShapes).extracting(IShape::getType).containsOnly(shapeType.getShapeType());
            assertThat(mutableShapes.stream().anyMatch(IShape::isPublished)).isTrue();
            assertThat(mutableShapes.stream().anyMatch(shape -> !shape.isPublished())).isTrue();
        }

        // Let's rename some shapes and ensure that no change is made to the shape service keys
        IMutableShape rectangle = ss1.getShape(ss1ShapeNames.get(MapToolType.DRAWING_QUAD));
        rectangle.setName("New Rectangle Name");

        IMutableShape rectangle2 = ss1.getShape(ss1ShapeNames.get(MapToolType.DRAWING_QUAD));
        assertThat(rectangle).isSameAs(rectangle2);

        // Delete the global shapes we created at the start
        cmom.deleteMapObject(publishedCircle);
        checkShapeServiceListenerValues(0, 0, 0, 1);    // Should have one more deleted

        cmom.deleteMapObject(publishedSquare);
        checkShapeServiceListenerValues(0, 0, 0, 1);    // Should have one more deleted

        // Delete map object 2 (the second device)
        cmom.deleteMapObject(cmoc2.getMapObject());
    }

    /**
     * Restart the core and verify that the shapes created earlier are reattached to the
     * shape service for our map object.
     *
     * @throws IOException                 if the reboot fails
     * @throws InvalidShapeFilterException only on code failure
     */
    @Test
    @DependsOn("playWithShapes")
    public void reattachTest() throws Exception
    {
        shutdown();
        reboot();

        final int typeCount = ALL_SHAPE_TYPES.length;
        final int doubleCount = typeCount * 2;

        // Should have 1 device MO + 2 global shapes + doubleCount shape service shape MOs
        assertThat(cmom.getMapObjectControllers()).hasSize(1 + 2 + doubleCount);

        // Get new references to all our controllers
        cmoc1 = cmom.getMapObjectController(mapObjectKey1);
        assertThat(cmoc1).isNotNull();
        assertThat(cmoc1.getMapObject().getUnitID()).isEqualTo(UNIT_ID_1);
        tpc1 = (TestDevicePluginController) cmoc1.getPluginController();

        cmoc2 = cmom.getMapObjectController(mapObjectKey2);
        assertThat(cmoc2).isNull();

        // Now see if we have our shapes back
        assertThat(tpc1.getShapeList().size()).isEqualTo(doubleCount);

        // Ensure that the published and unpublished sets are correct
        // We should have a total of 2 + typeCount (those from tpc2 have been deleted) and typeCount unpublished
        countShapesByPublishedSetting(tpc1.getShapeService(), 2 + typeCount, typeCount);
    }

    @Test
    @DependsOn("reattachTest")
    public void deleteMO()
    {
        cmom.deleteMapObject(cmoc1.getMapObject());

        assertThat(cmom.keyExists(mapObjectKey1)).as("MapObject %s should not exist", mapObjectKey1).isFalse();
        assertThat(cmom.keyExists(mapObjectKey2)).as("MapObject %s should not exist", mapObjectKey2).isFalse();

        // All we should have left are the 2 shapes created in bringPluginsOnline()
        IMapObjectPersist p = getBean(IMapObjectPersist.class);
        List<IMapObject> list = p.findAllMapObjects();
        assertThat(list).hasSize(2);
    }

    /**
     * Verifies that a rectangle can be created for a geofence crossing the equator.  Prior to RAPTORX-1599 this test would have failed.
     *
     * @since 2.39
     */
    @Test
    public void testGeofenceRuleAcrossEquator() throws ShapeAlreadyExistsException, NoSuchShapeException
    {
        ShapeServiceFactory shapeServiceFactory = getBean(ShapeServiceFactory.class);
        IShapeService shapeService = shapeServiceFactory.getOrCreateCoreShapeService();
        final var rectangle = ShapeUtils.calculateRectangleFromMinMaxCorners("Rectangle across equator", new RaptorLocation(-10, 0), new RaptorLocation(5, 1));
        shapeService.createRectangleMapObject(rectangle, ShapeOptions.PUBLISHED);
        shapeService.deleteShape(rectangle.getName());
    }

    /**
     * Verifies that a rectangle creation fails with exception if min is greater than max.
     *
     * @since 2.39
     */
    @Test
    public void testGeofenceRuleFailsIfMinGreaterThanMax()
    {
        var shapeService = getBean(ShapeServiceFactory.class).getOrCreateCoreShapeService();
        assertThatIllegalArgumentException()
                .isThrownBy(() -> {
                    final var rectangle = ShapeUtils.calculateRectangleFromMinMaxCorners("Rectangle min greater than max", new RaptorLocation(10, 0), new RaptorLocation(5, 1));
                    shapeService.createRectangleMapObject(rectangle, ShapeOptions.PUBLISHED);
                });
    }

    /**
     * Increment the expected counts as indicated and then assert that the shape service listeners have the expected
     * values.
     *
     * @param definedIncr     Amount to increment expected defined count
     * @param publishedIncr   Amount to increment expected published count
     * @param unpublishedIncr Amount to increment expected unpublished count
     * @param deletedIncr     Amount to increment expected deleted count
     */
    private synchronized void checkShapeServiceListenerValues(int definedIncr, int publishedIncr, int unpublishedIncr, int deletedIncr)
    {
        expectedDefinedCount += definedIncr;
        assertThat(ssl1.getShapeDefinedCount()).isEqualTo(expectedDefinedCount);
        assertThat(ssl2.getShapeDefinedCount()).isEqualTo(expectedDefinedCount);

        expectedPublishCount += publishedIncr;
        assertThat(ssl1.getShapePublishedCount()).isEqualTo(expectedPublishCount);
        assertThat(ssl2.getShapePublishedCount()).isEqualTo(expectedPublishCount);

        expectedUnpublishCount += unpublishedIncr;
        assertThat(ssl1.getShapeUnpublishedCount()).isEqualTo(expectedUnpublishCount);
        assertThat(ssl2.getShapeUnpublishedCount()).isEqualTo(expectedUnpublishCount);

        expectedDeletionCount += deletedIncr;
        assertThat(ssl1.getShapeDeletionCount()).isEqualTo(expectedDeletionCount);
        assertThat(ssl2.getShapeDeletionCount()).isEqualTo(expectedDeletionCount);
    }

    /**
     * Run a series of basic tests on the shape listener for a shape.
     *
     * @param shape Shape to test
     */
    private void testBasicShapeListener(IMutableShape shape)
    {
        ShapeListener sl1 = new ShapeListener();
        shape.addShapeListener(sl1);

        int expectedSl1Count = 0;
        int expectedSl2Count = 0;

        // -- test updating location

        var loc = shape.getLocation();
        shape.setLocation(loc);
        assertThat(sl1.getPropertyChangeCount()).isEqualTo(0);

        RaptorLocation newLoc = new RaptorLocation(10.0, 10.0);
        shape.setLocation(newLoc);
        expectedSl1Count += 1;

        // Irregular shapes will produce 2 PCEs (LOCATIONS, and LOCATION)
        if (shape instanceof IIrregularShape) expectedSl1Count += 1;

        // Rectangular shapes will produce 2 PCEs (LOCATIONS, and LOCATION)
        if (shape instanceof IRectangle) expectedSl1Count += 1;

        checkShapeListener(sl1, expectedSl1Count, ShapeProperties.LOCATION, newLoc);

        // -- test line width

        double w = shape.getLineWidth();

        shape.setLineWidth(w);  // Setting to current value should not result in a PCE
        assertThat(sl1.getPropertyChangeCount()).isEqualTo(expectedSl1Count);

        shape.setLineWidth(w + .5);  // Should result in a change
        expectedSl1Count += 1;
        checkShapeListener(sl1, expectedSl1Count, ShapeProperties.LINE_WIDTH, w + .5);

        // Now add a second listener and ensure that both listeners are notified
        ShapeListener sl2 = new ShapeListener();
        shape.addShapeListener(sl2);

        shape.setLineColor(Color.black);

        // Test new value - note COLOR change events send an Integer, not a Color
        expectedSl1Count += 1;
        expectedSl2Count += 1;
        checkShapeListener(sl1, expectedSl1Count, ShapeProperties.LINE_COLOR, Color.black.getRGB());
        checkShapeListener(sl2, expectedSl2Count, ShapeProperties.LINE_COLOR, Color.black.getRGB());

        // If the shape is published, set it to unpublished and verify that the shape service listeners get notified
        if (shape.isPublished())
        {
            shape.setPublished(false);

            // validate shape listener events (4: published, stipple pattern, stipple factor, texture fill parameters)
            expectedSl1Count += 4;
            expectedSl2Count += 4;
            checkShapeListener(sl1, expectedSl1Count, ShapeProperties.PUBLISHED, Boolean.FALSE);
            checkShapeListener(sl2, expectedSl2Count, ShapeProperties.PUBLISHED, Boolean.FALSE);

            // Validate shape service listener events
            checkShapeServiceListenerValues(0, 0, 1, 0); // Expect an unpublish event

            // Republish the shape and test again (4: published, stipple pattern, stipple factor, texture fill parameters)
            shape.setPublished(true);
            expectedSl1Count += 4;
            expectedSl2Count += 4;
            checkShapeListener(sl1, expectedSl1Count, ShapeProperties.PUBLISHED, Boolean.TRUE);
            checkShapeListener(sl2, expectedSl2Count, ShapeProperties.PUBLISHED, Boolean.TRUE);

            checkShapeServiceListenerValues(0, 1, 0, 0); // Expect a publish event
        }

        // If it is a rectangle, then test height/width changes (regression test)
        if (shape instanceof IMutableRectangle rectangle && !(shape instanceof IMutableSquare))
        {
            final double newValue = rectangle.getWidth() * 4;
            rectangle.setWidth(newValue);

            expectedSl1Count += 2;
            expectedSl2Count += 2;
            checkShapeListener(sl1, expectedSl1Count, ShapeProperties.WIDTH, newValue);
            checkShapeListener(sl2, expectedSl2Count, ShapeProperties.WIDTH, newValue);
        }

        // Now remove one of the listeners and ensure it no longer receives events
        sl1.resetPropertyChanges();
        sl2.resetPropertyChanges();
        expectedSl1Count = expectedSl2Count = 0;

        shape.removeShapeListener(sl2);

        shape.setLineColor(Color.red);

        expectedSl1Count += 1;
        checkShapeListener(sl1, expectedSl1Count, ShapeProperties.LINE_COLOR, Color.red.getRGB());

        assertThat(sl2.getPropertyChangeCount()).isEqualTo(expectedSl2Count);
        assertThat(sl2.propertyWasChanged(ShapeProperties.LINE_COLOR)).isFalse();

        shape.removeShapeListener(sl1);
    }

    /**
     * Check the status of a shape listener.
     *
     * @param sl            The shape listener to test
     * @param expectedCount The expected change count
     * @param propertyName  The name of the last property that should have changed
     * @param newValue      The new value of that property
     */
    private void checkShapeListener(ShapeListener sl, int expectedCount, String propertyName, Object newValue)
    {
        assertThat(sl.getPropertyChangeCount()).isEqualTo(expectedCount);
        assertThat(sl.propertyWasChanged(propertyName)).as("%s property changed", propertyName).isTrue();
        assertThat(sl.getLastShapeChangeEvent().getNewValue()).as("%s new value is correct", propertyName).isEqualTo(newValue);
    }

    /**
     * IShapeListener implementation so we can test shape listeners.
     */
    private static class ShapeListener implements IShapeListener
    {
        private final Set<String> propertiesChanged = new HashSet<>();
        private int propertyChangeCount = 0;
        private PropertyChangeEvent lastChangeEvent = null;

        /**
         * @return The number of property change events since the last reset
         */
        public int getPropertyChangeCount()
        {
            return propertyChangeCount;
        }

        /**
         * Test if a specific property has been seen in a change event since the last reset.
         *
         * @param propertyName Name of property to test
         * @return {@code true} if the a property change event for this property has been seen
         */
        public boolean propertyWasChanged(String propertyName)
        {
            return propertiesChanged.contains(propertyName);
        }

        /**
         * @return The last ShapeChangeEvent received since a reset.
         */
        public PropertyChangeEvent getLastShapeChangeEvent()
        {
            return lastChangeEvent;
        }

        /**
         * Reset the property change tracking data.
         */
        public void resetPropertyChanges()
        {
            propertyChangeCount = 0;
            propertiesChanged.clear();
            lastChangeEvent = null;
        }

        @Override
        public void propertyChange(PropertyChangeEvent evt)
        {
            propertyChangeCount += 1;
            propertiesChanged.add(evt.getPropertyName());
            lastChangeEvent = evt;
        }
    }

    /**
     * ShapeServiceListener test implementation.
     */
    private static class ShapeServiceListener implements IShapeServiceListener
    {
        private int shapePublishedCount = 0;
        private int shapeUnpublishedCount = 0;
        private int shapeDefinedCount = 0;
        private int shapeDeletionCount = 0;

        public int getShapePublishedCount()
        {
            return shapePublishedCount;
        }

        public int getShapeUnpublishedCount()
        {
            return shapeUnpublishedCount;
        }

        public int getShapeDefinedCount()
        {
            return shapeDefinedCount;
        }

        public int getShapeDeletionCount()
        {
            return shapeDeletionCount;
        }

        @Override
        public void onShapePublished(IShape shape)
        {
            assertThat(shape).isNotNull();
            shapePublishedCount += 1;
        }

        @Override
        public void onShapeUnpublished(IShape shape)
        {
            assertThat(shape).isNotNull();
            shapeUnpublishedCount += 1;
        }

        @Override
        public void onShapeDefined(IShape shape)
        {
            assertThat(shape).isNotNull();
            shapeDefinedCount += 1;
        }

        @Override
        public void onShapeDeletion(IShape shape)
        {
            assertThat(shape).isNotNull();
            shapeDeletionCount += 1;
        }
    }
}
