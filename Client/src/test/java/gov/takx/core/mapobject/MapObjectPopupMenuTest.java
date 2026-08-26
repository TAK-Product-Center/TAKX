package gov.takx.core.mapobject;

import gov.tak.api.cot.event.CotEvent;
import gov.tak.api.cot.importer.ImportResult;
import gov.tak.core.comms.cot.CotImporterManager;
import gov.takx.api.coordinatesystem.ILocation;
import gov.takx.api.coordinatesystem.RaptorLocation;
import gov.takx.api.mapobject.IMapObject;
import gov.takx.api.mapobject.IMapObjectController;
import gov.takx.api.mapobject.IMapObjectManager;
import gov.takx.api.mapobject.MapObjectSpec;
import gov.takx.api.persist.IMapObjectPersist;
import gov.takx.api.ui.IDeviceMenuSpaceManager;
import gov.takx.api.ui.IMenuSpaceManager;
import gov.takx.commons.utils.UUIDUtils;
import gov.takx.core.mapobject.gui.MapObjectPopupMenu;
import gov.takx.test.AIntegrationTest;
import gov.takx.test.mock.plugin.device.TestDevicePlugin;
import gov.takx.test.utils.CdiTestUtils;
import org.assertj.core.api.InstanceOfAssertFactories;
import org.junit.jupiter.api.Test;

import javax.swing.AbstractAction;
import javax.swing.JLabel;
import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;

import java.awt.Component;
import java.awt.event.MouseEvent;
import java.io.IOException;
import java.util.List;
import java.util.Map;

import static gov.takx.api.ui.IMenuSpaceManager.ContextMenuEnum.CENTER_ON;
import static gov.takx.api.ui.IMenuSpaceManager.ContextMenuEnum.INSPECT_COT;
import static gov.takx.api.ui.IMenuSpaceManager.ContextMenuEnum.KEEP_IN_VIEW;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * Test a few features of the IMenuSpaceManager and hence MapObjectPopupMenu
 *
 * @since 2.5
 */
public class MapObjectPopupMenuTest extends AIntegrationTest
{
    private static final String MARKER_UID = "21c57f66-d135-4180-a771-f2ede16629dc";
    private final CotImporterManager importerManager = getBean(CotImporterManager.class);
    private final IMapObjectManager mapObjectManager = getBean(IMapObjectManager.class);
    private final IMapObjectPersist mapObjectPersist = getBean(IMapObjectPersist.class);
    private TestDevicePlugin plugin;

    public MapObjectPopupMenuTest()
    {
        setPluginsToRegister(TestDevicePlugin.class);
    }

    @Override
    protected void preLoadState()
    {
        plugin = getPluginInstance(TestDevicePlugin.FAMILY, TestDevicePlugin.DEVICE_NAME);
    }

    /**
     * Test the ordering of the user contributed menus.  See API-271
     */
    @Test
    public void testMenuOrdering()
    {
        IMapObjectManager mapObjectManager = getBean(IMapObjectManager.class);

        final String unitId = UUIDUtils.generateType1UUID().toString();
        final ILocation location = new RaptorLocation(20, 30, false, 0);

        final TestDevicePlugin plugin = getPluginInstance(TestDevicePlugin.FAMILY, TestDevicePlugin.DEVICE_NAME);

        final MapObjectSpec spec = MapObjectSpec.builder(plugin.getFamily(), plugin.getType(), unitId)
                .location(location).build();
        final IMapObjectController controller = mapObjectManager.getOrCreateMapObject(spec).getController();

        IMenuSpaceManager menuSpaceManager = controller.getMenuSpaceManager();
        menuSpaceManager.addDevicePopupMenuComponent(new JMenuItem("Test 1"), 200);
        menuSpaceManager.addDevicePopupMenuComponent(new JMenuItem("Test 2"), 201);
        menuSpaceManager.addDevicePopupMenuComponent(new JMenuItem("Test 3"), 199);

        List<IDeviceMenuSpaceManager.IIndexedComponent> menuList = ((DeviceMenuSpaceManager) menuSpaceManager).getAdditionalMenuItems();
        assertThat(((JMenuItem) menuList.get(0).getComponent()).getText()).isEqualTo("Test 3");
        assertThat(((JMenuItem) menuList.get(1).getComponent()).getText()).isEqualTo("Test 1");
        assertThat(((JMenuItem) menuList.get(2).getComponent()).getText()).isEqualTo("Test 2");

        menuSpaceManager.addDevicePopupMenuComponent(new JMenuItem("Test 4"));
        assertThat(((JMenuItem) menuList.get(3).getComponent()).getText()).isEqualTo("Test 4");

        menuSpaceManager.addDevicePopupMenuComponent(new JPopupMenu.Separator());
        menuSpaceManager.addDevicePopupMenuSeparator(198);
        assertThat(menuList.get(5).getComponent() instanceof JPopupMenu.Separator).isEqualTo(true);
        assertThat(menuList.get(0).getComponent() instanceof JPopupMenu.Separator).isEqualTo(true);
    }

    /**
     * Test that a context menu for a map object with a valid location will have the CENTER_ON and KEEP_IN_VIEW
     * actions enabled.
     *
     * @since 2.40.4
     */
    @Test
    public void moMenu_withLocation_centerOnAndKeepInViewEnabled()
    {
        Map<IMenuSpaceManager.ContextMenuEnum, AbstractAction> actionMap = getMapObjectActionMap(true);

        JMenuItem menuItem = CdiTestUtils.invoke(actionMap.get(CENTER_ON), "createMenuItem");
        assertThat(menuItem.isEnabled()).isTrue();
        menuItem = CdiTestUtils.invoke(actionMap.get(KEEP_IN_VIEW), "createMenuItem");
        assertThat(menuItem.isEnabled()).isTrue();
    }

    /**
     * Test that a context menu for a map object without a location will have the CENTER_ON and KEEP_IN_VIEW
     * actions disabled.
     *
     * @since 2.40.4
     */
    @Test
    public void moMenu_withoutLocation_centerOnAndKeepInViewDisabled()
    {
        Map<IMenuSpaceManager.ContextMenuEnum, AbstractAction> actionMap = getMapObjectActionMap(false);

        JMenuItem menuItem = CdiTestUtils.invoke(actionMap.get(CENTER_ON), "createMenuItem");
        assertThat(menuItem.isEnabled()).isFalse();
        menuItem = CdiTestUtils.invoke(actionMap.get(KEEP_IN_VIEW), "createMenuItem");
        assertThat(menuItem.isEnabled()).isFalse();
    }

    /**
     * Test that a single CoT inspection action is present (no sub-menu) for a map object that doesn't have any received CoT events.
     *
     * @since 5.0 (reworked semantics in 5.3)
     */
    @Test
    public void test_inspectCotMenu_forMapObjectWithoutReceivedCotEvent()
    {
        var actionMap = getMapObjectActionMap(false);

        JMenuItem menuItem = CdiTestUtils.invoke(actionMap.get(INSPECT_COT), "createMenuItem");
        assertThat(menuItem.isEnabled()).isTrue();
        assertThat(menuItem).isNotInstanceOf(JMenu.class);
    }

    /**
     * Test that both CoT inspection sub-menu items are present and both are enabled for a map object created from a
     * CoT event.
     *
     * @since 5.0
     */
    @Test
    public void test_inspectCotMenu_forMapObjectWithReceivedCotEvent() throws IOException
    {
        // Create the map object from an imported CoT event
        final CotEvent event = CotEvent.parse(getResourceAsString("/cot/marker.cot"));
        assertThat(importerManager.importEvent(event)).isEqualTo(ImportResult.SUCCESS);
        final IMapObject mapObject = mapObjectPersist.findMapObjectByUid(MARKER_UID);
        assertThat(mapObject).isNotNull();

        Map<IMenuSpaceManager.ContextMenuEnum, AbstractAction> actionMap = getMapObjectActionMap(mapObject);

        JMenuItem menuItem = CdiTestUtils.invoke(actionMap.get(INSPECT_COT), "createMenuItem");
        assertThat(menuItem.isEnabled()).isTrue();
        assertThat(menuItem).isInstanceOf(JMenu.class).asInstanceOf(InstanceOfAssertFactories.type(JMenu.class))
                .satisfies(jMenu -> assertThat(jMenu.getMenuComponents()).hasSize(2).extracting(Component::isEnabled)
                        .containsExactlyInAnyOrder(true, true));
    }

    /**
     * Create a test map object with or without a valid location, specified by the {@code useLocation} field and
     * then return the Action map for its popup menu.
     *
     * @param useLocation whether to give the map object a valid location
     * @return the action map for the map object popup menu
     * @since 3.5.0
     */
    private Map<IMenuSpaceManager.ContextMenuEnum, AbstractAction> getMapObjectActionMap(boolean useLocation)
    {
        final String unitId = UUIDUtils.generateType1UUID().toString();
        final MapObjectSpec.Builder builder = MapObjectSpec.builder(plugin.getFamily(), plugin.getType(), unitId);
        final MapObjectSpec spec = useLocation ? builder.location(new RaptorLocation(20, 30, false, 0)).build() : builder.build();
        final IMapObject mapObject = mapObjectManager.getOrCreateMapObject(spec);

        return getMapObjectActionMap(mapObject);
    }

    /**
     * Return the Action map for the popup menu of the provided map object.
     *
     * @param mapObject The map object for which to get the action map
     * @return the action map for the map object popup menu
     * @since 5.0
     */
    private Map<IMenuSpaceManager.ContextMenuEnum, AbstractAction> getMapObjectActionMap(IMapObject mapObject)
    {
        MouseEvent mouseEvent = new MouseEvent(new JLabel(), 0, 0, 0, 0, 0, 0, true, 0);
        final MapObjectPopupMenu popupMenu = new MapObjectPopupMenu(mouseEvent, mapObject.getController());
        return CdiTestUtils.getField(popupMenu, "actionMap");
    }
}
