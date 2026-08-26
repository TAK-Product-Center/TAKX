package gov.takx.core.view.gui.explorer;

import gov.takx.api.annotation.PseudoBean;
import gov.takx.api.coordinatesystem.RaptorLocation;
import gov.takx.api.explorer.IExplorerDelegate;
import gov.takx.api.explorer.IGeofenceRule;
import gov.takx.api.explorer.IMapObjectExplorable;
import gov.takx.api.explorer.INameRule;
import gov.takx.api.explorer.IProximityRule;
import gov.takx.api.explorer.IRuleGroupController;
import gov.takx.api.mapobject.IMapObject;
import gov.takx.api.mapobject.IMapObjectManager;
import gov.takx.api.mapobject.MapObjectSpec;
import gov.takx.core.explorer.IExplorer;
import gov.takx.core.explorer.IExplorerManager;
import gov.takx.platform.KeyUtils;
import gov.takx.test.AIntegrationTest;
import jakarta.inject.Inject;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

import java.util.Collection;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalStateException;

/**
 * Test the new methods and interfaces available to the API as of 2.38.
 *
 * @author Proprietary information subject to the terms of a EULA
 * @since 2.38
 */
@PseudoBean
public class ExplorerTest extends AIntegrationTest
{
    @Inject
    private IExplorerManager explorerManager;
    @Inject
    private IMapObjectManager mapObjectManager;

    @BeforeEach
    public void beforeMethod()
    {
        IExplorer def = explorerManager.getDefaultExplorerDelegate();
        explorerManager.getExplorerDelegates().stream().filter(d -> !d.equals(def))
                .forEach(e -> explorerManager.deleteExplorer(e.getUUID()));
    }

    @AfterEach
    public void cleanup()
    {
        deleteAllMapObjects();
    }

    @Test
    void testCreateRule()
    {
        IExplorerDelegate testExplorer = explorerManager.createExplorer("Test");
        IGeofenceRule geofenceRule = testExplorer.createRule(IGeofenceRule.class);
        assertThat(geofenceRule).isNotNull();
        IProximityRule proximityRule = testExplorer.createRule(IProximityRule.class);
        assertThat(proximityRule).isNotNull();
        INameRule nameRule = testExplorer.createRule(INameRule.class);
        assertThat(nameRule).isNotNull();
    }

    @Test
    void testRuleGroupsAPI()
    {
        // test createRuleGroupsController
        IExplorerDelegate testExplorer = explorerManager.createExplorer("Test");
        IRuleGroupController controller = testExplorer.createRuleGroupsController("Test", true);
        assertThat(controller).isNotNull();

        // test getRuleGroupsControllers
        Collection<IRuleGroupController> controllers = testExplorer.getRuleGroupsControllers();
        assertThat(controllers).hasSize(1);

        // test getRuleGroupsController
        assertThat(testExplorer.getRuleGroupsController("Test")).isPresent();

        // test removeRuleGroupsController
        IRuleGroupController controller2 = testExplorer.createRuleGroupsController("Test2", true);
        assertThat(testExplorer.getRuleGroupsControllers()).hasSize(2);
        testExplorer.removeRuleGroupsController(controller2);
        assertThat(testExplorer.getRuleGroupsControllers()).hasSize(1);

        IRuleGroupController controller3 = testExplorer.createRuleGroup("Test3");
        assertThat(controller3).isNotNull();
    }

    /**
     * @since 5.7
     */
    @Test
    void testManualGroup()
    {
        // test createRuleGroupsController
        var testExplorer = explorerManager.createExplorer("Test");
        var group = testExplorer.createManualGroup("Test");
        assertThat(group).isNotNull();
    }

    /**
     * Test to verify RAPTORX-1551: Fix multi-rule with same geofence bug.
     * The bug turned out to be a cascading proxy creation event when an explorable matched on rules in multiple
     * explorer delegates. The only success found in trapping the issue was to set a timeout. The behavior in TAKX
     * was to simply lock up the UI since the issue was happening on the EDT.
     *
     * @since 2.39
     */
    @Test
    @Timeout(30)
    void testMultipleDelegates()
    {
        IExplorerDelegate explorer1 = setupDelegate("Test1");
        IExplorerDelegate explorer2 = setupDelegate("Test2");
        createIconMapObject("Test", "Black", "1", 0, 0);
        int size1 = explorer1.getExplorables().size();
        int size2 = explorer2.getExplorables().size();
        assertThat(size1).isEqualTo(size2);
    }

    /**
     * Test to verify RAPTORX-1581: Explorables are not added to new explorer delegates
     * If an explorer delegate is created after MapObjects have been created it doesn't ever seem to have the existing
     * MO's available for matching against the rules.
     *
     * @since 2.39
     */
    @Test
    void testDelegateCreationPostMOCreation()
    {
        IMapObject mapObject = createIconMapObject("Test", "Black", "1", 0, 0);
        IExplorerDelegate explorer = setupDelegate("Test1");
        assertThat(explorer.getExplorables().stream().filter(IMapObjectExplorable.class::isInstance).map(IMapObjectExplorable.class::cast).map(IMapObjectExplorable::getMapObject)).contains(mapObject);
    }

    /**
     * Create an explorer delegate with a "name rule" inside to test for the presence of object names containing "Test".
     *
     * @param name The name of the delegate and the contained rule group.
     * @return The delegate
     */
    private IExplorerDelegate setupDelegate(String name)
    {
        IExplorerDelegate explorer = explorerManager.createExplorer(name);
        IRuleGroupController controller = explorer.createRuleGroupsController(name, true);
        INameRule nameRule = explorer.createRule(INameRule.class);
        nameRule.setMatchString("Test");
        nameRule.setEnabled(true);
        controller.add(nameRule);
        explorer.requestGroupsRefresh();

        return explorer;
    }

    private IMapObject createIconMapObject(String name, String color, String unitId, double latitude, double longitude)
    {
        MapObjectSpec mapObjectSpec = MapObjectSpec.builder(KeyUtils.RESOURCE_ICON_FAMILY, color, unitId)
                .location(new RaptorLocation(latitude, longitude))
                .name(name)
                .build();
        return mapObjectManager.getOrCreateMapObject(mapObjectSpec);
    }

    /**
     * Test for RAPTORX-1624 (embeddable Explorers).
     * <p>
     * This test ensures that the getExplorerPanel method throws an exception when run in a headless environment.
     *
     * @since 2.39
     */
    @Test
    void testGetExplorerUIComponent_ThrowsWhenHeadless()
    {
        IExplorerDelegate explorer = explorerManager.createExplorer("test");
        assertThatIllegalStateException()
                .isThrownBy(explorer::getExplorerUIComponent);
    }
}
