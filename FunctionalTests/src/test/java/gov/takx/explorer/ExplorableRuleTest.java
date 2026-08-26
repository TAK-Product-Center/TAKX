package gov.takx.explorer;

import gov.takx.api.coordinatesystem.RaptorLocation;
import gov.takx.api.explorer.IExplorable;
import gov.takx.api.explorer.IGeofenceRule;
import gov.takx.api.explorer.INameRule;
import gov.takx.api.explorer.IProximityRule;
import gov.takx.api.explorer.IRuleGroupController;
import gov.takx.api.mapobject.IMapObject;
import gov.takx.api.mapobject.IMapObjectManager;
import gov.takx.api.mapobject.MapObjectSpec;
import gov.takx.api.shapes.IShape;
import gov.takx.api.shapes.IShapeService;
import gov.takx.api.shapes.IShapeService.ShapeOptions;
import gov.takx.api.shapes.ShapeAlreadyExistsException;
import gov.takx.core.explorer.IExplorer;
import gov.takx.core.explorer.IExplorerManager;
import gov.takx.core.explorer.controller.ExplorerController;
import gov.takx.core.explorer.controller.IExplorerController;
import gov.takx.core.explorer.rules.MapObjectRule;
import gov.takx.core.shapes.ShapeServiceFactory;
import gov.takx.platform.KeyUtils;
import gov.takx.platform.utils.ShapeUtils;
import gov.takx.test.AIntegrationTest;
import gov.takx.test.extension.DependsOn;
import gov.takx.test.extension.DependsOnOrdering;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.SwingUtilities;

import java.lang.invoke.MethodHandles;
import java.lang.reflect.InvocationTargetException;
import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration test for verifying that explorable rules function correctly.
 *
 * @since 2.31
 */
@TestMethodOrder(DependsOnOrdering.class)
public class ExplorableRuleTest extends AIntegrationTest
{
    private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());
    private static final String GEOFENCE_GROUP_NAME = "geofence";
    private static final String PROXIMITY_GROUP_NAME = "proximity";
    private static final String NAME_GROUP_NAME = "name";
    private IExplorer defaultExplorerDelegate;
    private ExplorerController groupsExplorerController;
    private IExplorerController standardExplorerController;
    private IMapObjectManager mapObjectManager;
    private IShapeService shapeService;

    @Override
    protected void postProjectOpen()
    {
        IExplorerManager explorerManager = getBean(IExplorerManager.class);
        defaultExplorerDelegate = explorerManager.getDefaultExplorerDelegate();
        groupsExplorerController = (ExplorerController) defaultExplorerDelegate.getController();
        standardExplorerController = defaultExplorerDelegate.getController();
        mapObjectManager = getBean(IMapObjectManager.class);
        ShapeServiceFactory shapeServiceFactory = getBean(ShapeServiceFactory.class);
        shapeService = shapeServiceFactory.getOrCreateCoreShapeService();
    }

    @AfterEach
    public void cleanup()
    {
        List<IRuleGroupController> ruleGroups = groupsExplorerController.getRuleGroups();
        ruleGroups.forEach(rg -> groupsExplorerController.removeExplorable(rg));

        deleteAllMapObjects();
    }

    /**
     * Verifies that a proximity rule includes explorables within the max distance in the rule group and excludes those
     * outside of the max distance.
     */
    @Test
    public void testProximityRule()
    {
        // Add an icon to use as a reference
        IMapObject blackMapObject = createIconMapObject("Black", "1", 0, 0);

        // Add a proximity rule for objects within 10km of the black icon
        IRuleGroupController ruleGroupController = addProximityRule(10_000, blackMapObject.getKey());

        // Add a green icon that is closer than 10km and a red icon that is further than 10km
        IMapObject greenMapObject = createIconMapObject("Green", "1", 0, 0.01);
        IMapObject redMapObject = createIconMapObject("Red", "1", 0, 0.1);

        assertThat(isExplorableInRuleGroup(greenMapObject.getKey(), ruleGroupController)).isTrue();
        assertThat(isExplorableInRuleGroup(redMapObject.getKey(), ruleGroupController)).isFalse();
    }

    /**
     * Verifies that a proximity rule is correctly updated when map objects change locations. (RAPTORX-365)
     */
    @Test
    @DependsOn("testProximityRule")
    public void testProximityRule_locationChange()
    {
        // Add an icon to use as a reference
        IMapObject blackMapObject = createIconMapObject("Black", "1", 0, 0);

        // Add a proximity rule for objects within 10km of the black icon
        IRuleGroupController ruleGroupController = addProximityRule(10_000, blackMapObject.getKey());

        // Add a green icon that is closer than 10km and a red icon that is further than 10km
        IMapObject greenMapObject = createIconMapObject("Green", "1", 0, 0.01);
        IMapObject redMapObject = createIconMapObject("Red", "1", 0, 0.1);

        assertThat(isExplorableInRuleGroup(greenMapObject.getKey(), ruleGroupController)).isTrue();
        assertThat(isExplorableInRuleGroup(redMapObject.getKey(), ruleGroupController)).isFalse();

        // Move the green icon out of the proximity and the red icon in.
        greenMapObject.setLocation(new RaptorLocation(2, 2), true);
        redMapObject.setLocation(new RaptorLocation(0.01, 0.01), true);

        // Verify group was updated
        assertThat(isExplorableInRuleGroup(greenMapObject.getKey(), ruleGroupController)).isFalse();
        assertThat(isExplorableInRuleGroup(redMapObject.getKey(), ruleGroupController)).isTrue();
    }

    /**
     * Verifies that a proximity rule is correctly restored on project reload. (RAPTORX-418)
     */
    @Test
    @DependsOn("testProximityRule")
    public void testProximityRule_projectReopen() throws Exception
    {
        // Add an icon to use as a reference
        IMapObject blackMapObject = createIconMapObject("Black", "1", 0, 0);

        // Add a proximity rule for objects within 10km of the black icon
        IRuleGroupController ruleGroupController = addProximityRule(10_000, blackMapObject.getKey());

        // Add a green icon that is closer than 10km and a red icon that is further than 10km
        IMapObject greenMapObject = createIconMapObject("Green", "1", 0, 0.01);
        IMapObject redMapObject = createIconMapObject("Red", "1", 0, 0.1);

        assertThat(isExplorableInRuleGroup(greenMapObject.getKey(), ruleGroupController)).isTrue();
        assertThat(isExplorableInRuleGroup(redMapObject.getKey(), ruleGroupController)).isFalse();

        // Save and reopen the project
        shutdown();
        reboot();

        // Explorables that matched the rule should still be in the group (and vice versa)
        ruleGroupController = (IRuleGroupController) standardExplorerController.getExplorable(ruleGroupController.getUniqueKey());
        assertThat(isExplorableInRuleGroup(greenMapObject.getKey(), ruleGroupController)).isTrue();
        assertThat(isExplorableInRuleGroup(redMapObject.getKey(), ruleGroupController)).isFalse();
    }

    /**
     * Verifies that a geofence rule includes explorables within the geofence in the rule group and excludes those
     * outside of the geofence.
     */
    @Test
    public void testGeofenceRule() throws ShapeAlreadyExistsException
    {
        // Add geofence
        final var rectangle = ShapeUtils.calculateRectangleFromMinMaxCorners("Rectangle 1", new RaptorLocation(0, 0), new RaptorLocation(1, 1));
        shapeService.createRectangleMapObject(rectangle, ShapeOptions.PUBLISHED);

        // Add a geofence rule for the geofence
        IRuleGroupController ruleGroupController = addGeofenceRule(rectangle);

        // Add a green icon that is within the shape and a red icon that is outside of the shape
        IMapObject greenMapObject = createIconMapObject("Green", "1", 0.5, 0.5);
        IMapObject redMapObject = createIconMapObject("Red", "1", 1.5, 1.5);

        assertThat(isExplorableInRuleGroup(greenMapObject.getKey(), ruleGroupController)).isTrue();
        assertThat(isExplorableInRuleGroup(redMapObject.getKey(), ruleGroupController)).isFalse();
    }

    /**
     * Verifies that a geofence rule is correctly updated when map objects change locations. (RAPTORX-365)
     */
    @Test
    @DependsOn("testGeofenceRule")
    public void testGeofenceRule_locationChange() throws ShapeAlreadyExistsException
    {
        // Add geofence
        final var rectangle = ShapeUtils.calculateRectangleFromMinMaxCorners("Rectangle 1", new RaptorLocation(0, 0), new RaptorLocation(1, 1));
        shapeService.createRectangleMapObject(rectangle, ShapeOptions.PUBLISHED);

        // Add a geofence rule for the geofence
        IRuleGroupController ruleGroupController = addGeofenceRule(rectangle);

        // Add a green icon that is within the shape and a red icon that is outside of the shape
        IMapObject greenMapObject = createIconMapObject("Green", "1", 0.5, 0.5);
        IMapObject redMapObject = createIconMapObject("Red", "1", 1.5, 1.5);

        assertThat(isExplorableInRuleGroup(greenMapObject.getKey(), ruleGroupController)).isTrue();
        assertThat(isExplorableInRuleGroup(redMapObject.getKey(), ruleGroupController)).isFalse();

        // Move the green icon out of the geofence and the red icon in.
        greenMapObject.setLocation(new RaptorLocation(2, 2), true);
        redMapObject.setLocation(new RaptorLocation(0.1, 0.1), true);

        // Verify group was updated
        assertThat(isExplorableInRuleGroup(greenMapObject.getKey(), ruleGroupController)).isFalse();
        assertThat(isExplorableInRuleGroup(redMapObject.getKey(), ruleGroupController)).isTrue();

        // Move the rectangle so green is back in and red is out
        try
        {
            // TODO: We should not have to do this. There is clearly a threading issue that is causing this problem.
            //       Ideally this setLocation would happen in-line (not in the invokeAndWait). If you are not able
            //       to elicit a test failure on your local system try adding "invocationCount=100" to the @Test.
            //       And just to add a warning, I was able to run successfully on my local system but had failures
            //       on Jenkins, sooooo good luck!
            //       See RAPTORX-1597
            SwingUtilities.invokeAndWait(() -> rectangle.setLocation(new RaptorLocation(2.0, 2.0)));
        } catch (InterruptedException | InvocationTargetException e)
        {
            logger.error("Error: ", e);
        }

        // Verify group was updated
        assertThat(isExplorableInRuleGroup(greenMapObject.getKey(), ruleGroupController)).isTrue();
        assertThat(isExplorableInRuleGroup(redMapObject.getKey(), ruleGroupController)).isFalse();
    }

    /**
     * Verifies that a geofence rule is correctly updated when shapes change rotation. (RAPTORX-1552)
     */
    @Test
    @DependsOn("testGeofenceRule")
    public void testGeofenceRule_rotationChange() throws ShapeAlreadyExistsException
    {
        // Add geofence
        final var rectangle = ShapeUtils.calculateRectangleFromMinMaxCorners("Rectangle 1", new RaptorLocation(0, 0), new RaptorLocation(1, 4));
        shapeService.createRectangleMapObject(rectangle, ShapeOptions.PUBLISHED);

        // Add a geofence rule for the geofence
        IRuleGroupController ruleGroupController = addGeofenceRule(rectangle);

        // Add a green icon that is within the shape and a red icon that is outside of the shape
        IMapObject greenMapObject = createIconMapObject("Green", "1", 0.5, 0.5);
        IMapObject redMapObject = createIconMapObject("Red", "2", 2.0, 2.0);

        assertThat(isExplorableInRuleGroup(greenMapObject.getKey(), ruleGroupController)).isTrue();
        assertThat(isExplorableInRuleGroup(redMapObject.getKey(), ruleGroupController)).isFalse();

        // Rotate the shape so that the green icon is out and the red icon is in
        rectangle.setOrientation(90.0);
        assertThat(isExplorableInRuleGroup(greenMapObject.getKey(), ruleGroupController)).isFalse();
        assertThat(isExplorableInRuleGroup(redMapObject.getKey(), ruleGroupController)).isTrue();

        // Rotate the shape to be upside-down
        rectangle.setOrientation(180.0);
        assertThat(isExplorableInRuleGroup(greenMapObject.getKey(), ruleGroupController)).isTrue();
        assertThat(isExplorableInRuleGroup(redMapObject.getKey(), ruleGroupController)).isFalse();

        // Rotate the shape 1 degree. This causes some calculation discrepancies in the vertices locations which
        // previously caused the "IDL determination" to falsely return true when it should be false.
        rectangle.setOrientation(1.0);
        assertThat(isExplorableInRuleGroup(greenMapObject.getKey(), ruleGroupController)).isTrue();
        assertThat(isExplorableInRuleGroup(redMapObject.getKey(), ruleGroupController)).isFalse();
    }

    /**
     * Verifies that a geofence rule is correctly restored on project reload. (RAPTORX-418)
     */
    @Test
    @DependsOn("testGeofenceRule")
    public void testGeofenceRule_projectReopen() throws Exception
    {
        // Add geofence
        final var rectangle = ShapeUtils.calculateRectangleFromMinMaxCorners("Rectangle 1", new RaptorLocation(0, 0), new RaptorLocation(1, 1));
        shapeService.createRectangleMapObject(rectangle, ShapeOptions.PUBLISHED);

        // Add a geofence rule for the geofence
        IRuleGroupController ruleGroupController = addGeofenceRule(rectangle);

        // Add a green icon that is within the shape and a red icon that is outside of the shape
        IMapObject greenMapObject = createIconMapObject("Green", "1", 0.5, 0.5);
        IMapObject redMapObject = createIconMapObject("Red", "1", 1.5, 1.5);

        assertThat(isExplorableInRuleGroup(greenMapObject.getKey(), ruleGroupController)).isTrue();
        assertThat(isExplorableInRuleGroup(redMapObject.getKey(), ruleGroupController)).isFalse();

        // Save and reopen the project
        shutdown();
        reboot();

        // Explorables that matched the rule should still be in the group (and vice versa)
        ruleGroupController = (IRuleGroupController) standardExplorerController.getExplorable(ruleGroupController.getUniqueKey());
        assertThat(isExplorableInRuleGroup(greenMapObject.getKey(), ruleGroupController)).isTrue();
        assertThat(isExplorableInRuleGroup(redMapObject.getKey(), ruleGroupController)).isFalse();
    }

    /**
     * Verifies that a geofence rule includes explorables within the geofence which spans the International Date Line.
     *
     * @since 2.38
     */
    @Test
    public void testGeofenceRule_acrossIDL() throws ShapeAlreadyExistsException
    {
        // Create map objects for testing containment
        IMapObject rightMapObjectController = createIconMapObject("Right", "1", 0, -179);
        IMapObject leftMapObjectController = createIconMapObject("Left", "2", 0, 179);
        IMapObject eastMapObjectController = createIconMapObject("East", "1", 0, -50);
        IMapObject westMapObjectController = createIconMapObject("West", "2", 0, 50);

        // Add geofence which spans the IDL
        final var rectangle = ShapeUtils.calculateRectangleFromMinMaxCorners("Rectangle 1", new RaptorLocation(-2, 178), new RaptorLocation(2, -178));
        shapeService.createRectangleMapObject(rectangle, ShapeOptions.PUBLISHED);

        // Add a geofence rule for the geofence
        addGeofenceRule(rectangle);

        assertThat(isExplorableInRuleGroup(leftMapObjectController.getKey(), GEOFENCE_GROUP_NAME)).isTrue();
        assertThat(isExplorableInRuleGroup(rightMapObjectController.getKey(), GEOFENCE_GROUP_NAME)).isTrue();

        // test a rotated shape (ensure the min/max corners are still meaningful)
        rectangle.setOrientation(45.0);
        assertThat(isExplorableInRuleGroup(leftMapObjectController.getKey(), GEOFENCE_GROUP_NAME)).isTrue();
        assertThat(isExplorableInRuleGroup(rightMapObjectController.getKey(), GEOFENCE_GROUP_NAME)).isTrue();
        assertThat(isExplorableInRuleGroup(eastMapObjectController.getKey(), GEOFENCE_GROUP_NAME)).isFalse();
        assertThat(isExplorableInRuleGroup(westMapObjectController.getKey(), GEOFENCE_GROUP_NAME)).isFalse();

        // Prior to TAKX-884 the following block would fail
        // When the rectangle is rotated from 180<x<360 the max and min corners effectively flip
        rectangle.setOrientation(181.0);
        assertThat(isExplorableInRuleGroup(leftMapObjectController.getKey(), GEOFENCE_GROUP_NAME)).isTrue();
        assertThat(isExplorableInRuleGroup(rightMapObjectController.getKey(), GEOFENCE_GROUP_NAME)).isTrue();
        assertThat(isExplorableInRuleGroup(eastMapObjectController.getKey(), GEOFENCE_GROUP_NAME)).isFalse();
        assertThat(isExplorableInRuleGroup(westMapObjectController.getKey(), GEOFENCE_GROUP_NAME)).isFalse();

        // test the center being west of the IDL
        rectangle.setLocation(new RaptorLocation(0, 179.9));
        assertThat(isExplorableInRuleGroup(leftMapObjectController.getKey(), GEOFENCE_GROUP_NAME)).isTrue();
        assertThat(isExplorableInRuleGroup(rightMapObjectController.getKey(), GEOFENCE_GROUP_NAME)).isTrue();
        assertThat(isExplorableInRuleGroup(eastMapObjectController.getKey(), GEOFENCE_GROUP_NAME)).isFalse();
        assertThat(isExplorableInRuleGroup(westMapObjectController.getKey(), GEOFENCE_GROUP_NAME)).isFalse();

        // test the center being east of the IDL
        rectangle.setLocation(new RaptorLocation(0, -179.9));
        assertThat(isExplorableInRuleGroup(leftMapObjectController.getKey(), GEOFENCE_GROUP_NAME)).isTrue();
        assertThat(isExplorableInRuleGroup(rightMapObjectController.getKey(), GEOFENCE_GROUP_NAME)).isTrue();
        assertThat(isExplorableInRuleGroup(eastMapObjectController.getKey(), GEOFENCE_GROUP_NAME)).isFalse();
        assertThat(isExplorableInRuleGroup(westMapObjectController.getKey(), GEOFENCE_GROUP_NAME)).isFalse();
    }

    /**
     * Verifies that a name rule includes explorables that contain the name string and excludes those that don't.
     */
    @Test
    public void testNameRule()
    {
        // Add a name rule for "Green"
        IRuleGroupController ruleGroupController = addNameRule("Green");

        // Add a green icon that is closer than 10km and a red icon that is further than 10km
        IMapObject greenMapObject = createIconMapObject("Green", "Green 1", 0, 0.01);
        IMapObject redMapObject = createIconMapObject("Red", "Red 1", 0, 0.1);

        assertThat(isExplorableInRuleGroup(greenMapObject.getKey(), ruleGroupController)).isTrue();
        assertThat(isExplorableInRuleGroup(redMapObject.getKey(), ruleGroupController)).isFalse();
    }

    /**
     * Verifies that a name rule is correctly updated when map objects change names.
     */
    @Test
    @DependsOn("testNameRule")
    public void testNameRule_nameChange()
    {
        // Add a name rule for "Green"
        IRuleGroupController ruleGroupController = addNameRule("Green");

        // Add a green icon that is closer than 10km and a red icon that is further than 10km
        IMapObject greenMapObject = createIconMapObject("Green", "Green 1", 0, 0.01);
        IMapObject redMapObject = createIconMapObject("Red", "Red 1", 0, 0.1);

        assertThat(isExplorableInRuleGroup(greenMapObject.getKey(), ruleGroupController)).isTrue();
        assertThat(isExplorableInRuleGroup(redMapObject.getKey(), ruleGroupController)).isFalse();

        greenMapObject.setName("Red 2");
        redMapObject.setName("Green 2");

        // Verify group was updated
        assertThat(isExplorableInRuleGroup(greenMapObject.getKey(), ruleGroupController)).isFalse();
        assertThat(isExplorableInRuleGroup(redMapObject.getKey(), ruleGroupController)).isTrue();
    }

    /**
     * Verifies that a name rule is correctly restored on project reload.
     */
    @Test
    @DependsOn("testNameRule")
    public void testNameRule_projectReopen() throws Exception
    {
        // Add a name rule for "Green"
        IRuleGroupController ruleGroupController = addNameRule("Green");

        // Add a green icon that is closer than 10km and a red icon that is further than 10km
        IMapObject greenMapObject = createIconMapObject("Green", "Green 1", 0, 0.01);
        IMapObject redMapObject = createIconMapObject("Red", "Red 1", 0, 0.1);

        assertThat(isExplorableInRuleGroup(greenMapObject.getKey(), ruleGroupController)).isTrue();
        assertThat(isExplorableInRuleGroup(redMapObject.getKey(), ruleGroupController)).isFalse();

        // Save and reopen the project
        shutdown();
        reboot();

        // Explorables that matched the rule should still be in the group (and vice versa)
        ruleGroupController = (IRuleGroupController) standardExplorerController.getExplorable(ruleGroupController.getUniqueKey());
        assertThat(isExplorableInRuleGroup(greenMapObject.getKey(), ruleGroupController)).isTrue();
        assertThat(isExplorableInRuleGroup(redMapObject.getKey(), ruleGroupController)).isFalse();
    }

    /**
     * Verifies that a map object rule includes explorables of with specified family/type and excludes those without.
     */
    @Test
    public void testMapObjectRule()
    {
        // Add a map object rule for "Green"
        IRuleGroupController ruleGroupController = addMapObjectRule(KeyUtils.RESOURCE_ICON_FAMILY, "Green", "*");

        // Add a green icon that matches the criteria and a red icon that doesn't
        IMapObject greenMapObject = createIconMapObject("Green", "Green 1", 0, 0.01);
        IMapObject redMapObject = createIconMapObject("Red", "Red 1", 0, 0.1);

        assertThat(isExplorableInRuleGroup(greenMapObject.getKey(), ruleGroupController)).isTrue();
        assertThat(isExplorableInRuleGroup(redMapObject.getKey(), ruleGroupController)).isFalse();
    }

    /**
     * Verifies that a map object rule is correctly restored on project reload.
     */
    @Test
    @DependsOn("testMapObjectRule")
    public void testMapObjectRule_projectReopen() throws Exception
    {
        // Add a map object rule for "Green"
        IRuleGroupController ruleGroupController = addMapObjectRule(KeyUtils.RESOURCE_ICON_FAMILY, "Green", "*");

        // Add a green icon that matches the criteria and a red icon that doesn't
        IMapObject greenMapObject = createIconMapObject("Green", "Green 1", 0, 0.01);
        IMapObject redMapObject = createIconMapObject("Red", "Red 1", 0, 0.1);

        assertThat(isExplorableInRuleGroup(greenMapObject.getKey(), ruleGroupController)).isTrue();
        assertThat(isExplorableInRuleGroup(redMapObject.getKey(), ruleGroupController)).isFalse();

        // Save and reopen the project
        shutdown();
        reboot();

        // Explorables that matched the rule should still be in the group (and vice versa)
        ruleGroupController = (IRuleGroupController) standardExplorerController.getExplorable(ruleGroupController.getUniqueKey());
        assertThat(isExplorableInRuleGroup(greenMapObject.getKey(), ruleGroupController)).isTrue();
        assertThat(isExplorableInRuleGroup(redMapObject.getKey(), ruleGroupController)).isFalse();
    }

    /**
     * Verifies that a rule group with multiple rules set to match any will match based on either rule.
     */
    @Test
    @DependsOn({"testGeofenceRule", "testMapObjectRule"})
    public void testMultipleRules_any() throws ShapeAlreadyExistsException
    {
        // Add geofence
        final var rectangle = ShapeUtils.calculateRectangleFromMinMaxCorners("Rectangle 1", new RaptorLocation(0, 0), new RaptorLocation(1, 1));
        shapeService.createRectangleMapObject(rectangle, ShapeOptions.PUBLISHED);

        // Create a rule group with a geofence and a map object rule for "Red"
        IRuleGroupController ruleGroupController = groupsExplorerController.addRuleGroup("any", null, Collections.emptyList(), null);
        ruleGroupController.setMatchAll(false);
        addGeofenceRule(rectangle, ruleGroupController);
        addMapObjectRule(KeyUtils.RESOURCE_ICON_FAMILY, "Red", "*", ruleGroupController);

        // Add a green icon that is within the shape but doesn't match the map object rule
        // Add a red icon that is outside of the shape but matches the map object rule
        // Add a yellow icon that is outside the shape and doesn't match the map object rule
        IMapObject greenMapObject = createIconMapObject("Green", "1", 0.5, 0.5);
        IMapObject redMapObject = createIconMapObject("Red", "1", 1.5, 1.5);
        IMapObject yellowMapObjectController = createIconMapObject("Yellow", "1", 2, 2);

        // Verify explorables matching either rule are included and those matching neither are not
        assertThat(isExplorableInRuleGroup(greenMapObject.getKey(), ruleGroupController)).isTrue();
        assertThat(isExplorableInRuleGroup(redMapObject.getKey(), ruleGroupController)).isTrue();
        assertThat(isExplorableInRuleGroup(yellowMapObjectController.getKey(), ruleGroupController)).isFalse();
    }

    /**
     * Verifies that a rule group with multiple rules set to match all will match based on all rules.
     */
    @Test
    @DependsOn({"testProximityRule", "testNameRule"})
    public void testMultipleRules_all()
    {
        // Add an icon to use as a reference
        IMapObject blackMapObject = createIconMapObject("Black", "1", 0, 0);

        // Create a rule group with a proximity of 10km around the black icon and a name rule for "Red"
        IRuleGroupController ruleGroupController = groupsExplorerController.addRuleGroup("any", null, Collections.emptyList(), null);
        ruleGroupController.setMatchAll(true);
        addProximityRule(10_000, blackMapObject.getKey(), ruleGroupController);
        addNameRule("Red", ruleGroupController);

        // Add a green icon that is closer than 10km but doesn't match the name rule
        // Add a red icon that is further than 10km but matches the name rule
        // Add a red icon that is closer than 10km and matches the name rule
        IMapObject greenMapObject = createIconMapObject("Green", "1", 0, 0.01);
        IMapObject red1MapObjectController = createIconMapObject("Red", "1", 0, 0.1);
        IMapObject red2MapObjectController = createIconMapObject("Red", "2", 0, 0.01);

        // Verify only explorables matching both rules are included
        assertThat(isExplorableInRuleGroup(greenMapObject.getKey(), ruleGroupController)).isFalse();
        assertThat(isExplorableInRuleGroup(red1MapObjectController.getKey(), ruleGroupController)).isFalse();
        assertThat(isExplorableInRuleGroup(red2MapObjectController.getKey(), ruleGroupController)).isTrue();
    }

    /**
     * Creates a pushpin icon at the specified location
     *
     * @param color     The color, used as the icon type
     * @param unitId    The unitId
     * @param latitude  The latitude for the icon
     * @param longitude The longitude for the icon
     * @return The map object controller for the new map object
     */
    @SuppressWarnings("SameParameterValue")
    private IMapObject createIconMapObject(String color, String unitId, double latitude, double longitude)
    {
        MapObjectSpec mapObjectSpec = MapObjectSpec.builder(KeyUtils.RESOURCE_ICON_FAMILY, color, unitId)
                .location(new RaptorLocation(latitude, longitude))
                .build();
        return mapObjectManager.getOrCreateMapObject(mapObjectSpec);
    }

    /**
     * Create and add a proximity rule with the specified distance relative to the explorable with the provided unique key.
     *
     * @param maxDistance Max distance for rule
     * @param uniqueKey   Unique key of explorable for rule
     * @return The rule group controller for the group containing the rule
     */
    @SuppressWarnings("SameParameterValue")
    private IRuleGroupController addProximityRule(int maxDistance, String uniqueKey)
    {
        return addProximityRule(maxDistance, uniqueKey, null);
    }

    /**
     * Create and add a proximity rule with the specified distance relative to the explorable with the provided unique key.
     *
     * @param maxDistance Max distance for rule
     * @param uniqueKey   Unique key of explorable for rule
     * @param ruleGroup   Existing rule group to which to add the rule (may be {@code null} to force creation of a new group)
     * @return The rule group controller for the group containing the rule
     */
    private IRuleGroupController addProximityRule(int maxDistance, String uniqueKey, IRuleGroupController ruleGroup)
    {
        IExplorable explorable = defaultExplorerDelegate.getExplorable(uniqueKey);

        IRuleGroupController ruleGroupController = ruleGroup;
        if (ruleGroup == null)
        {
            ruleGroupController = groupsExplorerController.addRuleGroup(PROXIMITY_GROUP_NAME, null, Collections.emptyList(), null);
        }

        final IProximityRule proximityRule = defaultExplorerDelegate.createRule(IProximityRule.class);
        proximityRule.setMaxDistanceMeters(maxDistance);
        proximityRule.setSelectedExplorable(explorable);
        ruleGroupController.add(proximityRule);

        return ruleGroupController;
    }

    /**
     * Create and add a geofence rule with the provide shape.
     *
     * @param shape geofence shape for rule
     * @return The rule group controller for the group containing the rule
     */
    private IRuleGroupController addGeofenceRule(IShape shape)
    {
        return addGeofenceRule(shape, null);
    }

    /**
     * Create and add a geofence rule with the provide shape.
     *
     * @param shape     geofence shape for rule
     * @param ruleGroup Existing rule group to which to add the rule (may be {@code null} to force creation of a new group)
     * @return The rule group controller for the group containing the ruleXL
     */
    private IRuleGroupController addGeofenceRule(IShape shape, IRuleGroupController ruleGroup)
    {
        IRuleGroupController ruleGroupController = ruleGroup;
        if (ruleGroup == null)
        {
            ruleGroupController = groupsExplorerController.addRuleGroup(GEOFENCE_GROUP_NAME, null, Collections.emptyList(), null);
        }

        IGeofenceRule geofenceRule = defaultExplorerDelegate.createRule(IGeofenceRule.class);
        geofenceRule.setSelectedShape(shape);
        ruleGroupController.add(geofenceRule);

        return ruleGroupController;
    }

    /**
     * Create and add a name rule with the specified match string.
     *
     * @param matchString String to match for rule
     * @return The rule group controller for the group containing the rule
     */
    private IRuleGroupController addNameRule(String matchString)
    {
        return addNameRule(matchString, null);
    }

    /**
     * Create and add a name rule with the specified match string.
     *
     * @param matchString String to match for rule
     * @param ruleGroup   Existing rule group to which to add the rule (may be {@code null} to force creation of a new group)
     * @return The rule group controller for the group containing the rule
     */
    private IRuleGroupController addNameRule(String matchString, IRuleGroupController ruleGroup)
    {
        IRuleGroupController ruleGroupController = ruleGroup;
        if (ruleGroup == null)
        {
            ruleGroupController = groupsExplorerController.addRuleGroup(NAME_GROUP_NAME, null, Collections.emptyList(), null);
        }

        INameRule nameRule = defaultExplorerDelegate.createRule(INameRule.class);
        nameRule.setMatchString(matchString);
        ruleGroupController.add(nameRule);

        return ruleGroupController;
    }

    /**
     * Create and add a map object rule with the specified family, type, and unitId values.
     *
     * @param family The family to match, or "*" for any
     * @param type   The type to match, or "*" for any
     * @param unitId The unitId to match, or "*" for any
     * @return The rule group controller for the group containing the rule
     */
    private IRuleGroupController addMapObjectRule(String family, String type, String unitId)
    {
        return addMapObjectRule(family, type, unitId, null);
    }

    /**
     * Create and add a map object rule with the specified family, type, and unitId values.
     *
     * @param family    The family to match, or "*" for any
     * @param type      The type to match, or "*" for any
     * @param unitId    The unitId to match, or "*" for any
     * @param ruleGroup Existing rule group to which to add the rule (may be {@code null} to force creation of a new group)
     * @return The rule group controller for the group containing the rule
     */
    private IRuleGroupController addMapObjectRule(String family, String type, String unitId, IRuleGroupController ruleGroup)
    {
        IRuleGroupController ruleGroupController = ruleGroup;
        if (ruleGroup == null)
        {
            ruleGroupController = groupsExplorerController.addRuleGroup("map object", null, Collections.emptyList(), null);
        }

        MapObjectRule mapObjectRule = defaultExplorerDelegate.createRule(MapObjectRule.class);
        mapObjectRule.setMatchParts(family, type, unitId);
        ruleGroupController.add(mapObjectRule);

        return ruleGroupController;
    }

    /**
     * Determines if the explorable with the provided unique key is in the rule group of the provided controller.
     *
     * @param uniqueKey           Unique key of explorable
     * @param ruleGroupController Controller of the rule group
     * @return True if explorable is in rule group.
     */
    private boolean isExplorableInRuleGroup(String uniqueKey, IRuleGroupController ruleGroupController)
    {
        IExplorable explorable = standardExplorerController.getExplorable(uniqueKey);
        return ruleGroupController.getChildren().contains(explorable);
    }

    /**
     * Determines if the explorable with the provided unique key is in the named rule group of the default controller.
     *
     * @param uniqueKey     Unique key of explorable
     * @param ruleGroupName The name of the rule group to look in
     * @return True if explorable is in rule group.
     * @since 2.38
     */
    private boolean isExplorableInRuleGroup(String uniqueKey, String ruleGroupName)
    {
        final List<IExplorable> explorables = defaultExplorerDelegate.getRuleGroupsController(ruleGroupName)
                .map(controller -> controller.getMatchingExplorables(defaultExplorerDelegate.getExplorables()))
                .orElse(Collections.emptyList());

        return explorables.contains(defaultExplorerDelegate.getExplorable(uniqueKey));
    }
}
