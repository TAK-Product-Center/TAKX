package gov.takx.core.toolbar;

import gov.takx.api.event.FinalizeToolbars;
import gov.takx.api.event.project.ProjectClosing;
import gov.takx.api.toolbar.ButtonType;
import gov.takx.api.toolbar.IToolbarButton;
import gov.takx.api.toolbar.IToolbarButtonBuilder;
import gov.takx.api.toolbar.IToolbarComponent;
import gov.takx.api.toolbar.IToolbarSeparator;
import gov.takx.api.toolbar.IToolbarTab;
import gov.takx.api.toolbar.TabType;
import gov.takx.commons.swing.ImageUtils;
import gov.takx.platform.cdi.NoopInvocationHandler;
import gov.takx.test.AContainerTest;
import gov.takx.test.mock.plugin.device.TestDevicePlugin;
import gov.takx.test.utils.CdiTestUtils;
import gov.takx.test.utils.SwingTestUtils;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.assertj.core.api.InstanceOfAssertFactories.collection;
import static org.assertj.core.api.InstanceOfAssertFactories.list;
import static org.assertj.core.api.InstanceOfAssertFactories.type;

/**
 * Unit tests for {@link ToolbarManager}.
 *
 * @since 3.4
 */
public class ToolbarManagerTest extends AContainerTest
{
    private final TestDevicePlugin testDevicePlugin = new TestDevicePlugin();
    private ToolbarConfigurationManager configurationManager;
    private ToolbarManager toolbarManager;

    @BeforeEach
    public void setup()
    {
        configurationManager = getBean(ToolbarConfigurationManager.class);
        toolbarManager = getBean(ToolbarManager.class);

        // The default configuration will always exist, so go ahead and create it for more accurate tests
        toolbarManager.createStandardTabs();

        container.event().select(FinalizeToolbars.class).fire(new FinalizeToolbars());
    }

    @AfterEach
    public void cleanup()
    {
        // Since we are deleting all of the configurations (which doesn't happen normally), we need to make sure none
        // are active or it will get stuck trying to activate a backup config when the last one is deleted.
        CdiTestUtils.setField(configurationManager, "activeConfiguration", null);

        // Clear any configurations, tabs, or buttons left over from the tests
        configurationManager.getConfigurations().forEach(c -> configurationManager.removeConfiguration(c));
        configurationManager.getReferenceButtons().forEach(b -> configurationManager.removeReferenceButton(b, false));
        configurationManager.getReferenceTabs().forEach(t -> configurationManager.removeReferenceTab(t));

        CdiTestUtils.invoke(toolbarManager, "cleanup", new ProjectClosing("Test", null));
    }

    @Test
    public void testCreateButtonBuilder()
    {
        final IToolbarButtonBuilder buttonBuilder = toolbarManager.createButtonBuilder(testDevicePlugin, "My button");
        assertThat(buttonBuilder).isNotNull().extracting("type").isEqualTo(ButtonType.PLUGIN_CREATED);
    }

    @Test
    public void testCreateDrawingToolButtonBuilder()
    {
        final IToolbarButtonBuilder buttonBuilder = toolbarManager.createDrawingToolButtonBuilder(testDevicePlugin, "My button");
        assertThat(buttonBuilder).isNotNull().extracting("type").isEqualTo(ButtonType.PLUGIN_DRAW_TOOL);
    }

    @Test
    public void testHandleChangeEvent()
    {
        final IToolbarTab tab = toolbarManager.createTab(testDevicePlugin, "My tab");
        toolbarManager.addTab(tab);

        // Add a plugin button to a tab after it has been added to the toolbar
        final IToolbarButton button = toolbarManager.createButtonBuilder(testDevicePlugin, "My button")
                .setImage(ImageUtils.getDefaultImage())
                .build();
        tab.addComponent(button);

        // The new button should be added to the plugin button map
        final Map<String, List<ToolbarButton>> pluginButtonMap = CdiTestUtils.getField(toolbarManager, "pluginButtonMap");
        assertThat(pluginButtonMap.get(testDevicePlugin.getUniqueKey())).extracting(AToolbarComponent::getId).contains(button.getId());
    }

    @Test
    public void testAddDrawingToolButton()
    {
        final IToolbarButton button = toolbarManager.createDrawingToolButtonBuilder(testDevicePlugin, "My button")
                .setToggleDeselectListener(b -> {
                })
                .setImage(ImageUtils.getDefaultImage()).build();

        toolbarManager.addDrawingToolButton(button);

        // An action listener should be added by the DrawTabToolbarWrapper
        assertThat(button).asInstanceOf(type(ToolbarButton.class)).satisfies(b -> assertThat(b.getActionListeners()).hasSize(1));

        // The button should be added as a reference
        assertThat(toolbarManager.getButton(button.getId())).isNotNull();

        // The button should be in the plugin button map
        final Map<String, List<ToolbarButton>> pluginButtonMap = CdiTestUtils.getField(toolbarManager, "pluginButtonMap");
        assertThat(pluginButtonMap.get(testDevicePlugin.getUniqueKey())).extracting(AToolbarComponent::getId).contains(button.getId());
    }

    @Test
    public void testAddDrawingToolButton_notToolbarButton()
    {
        final IToolbarButton iToolbarButton = NoopInvocationHandler.forType(IToolbarButton.class);
        assertThatIllegalArgumentException().isThrownBy(() -> toolbarManager.addDrawingToolButton(iToolbarButton));
    }

    @Test
    public void testAddDrawingToolButton_notDrawingTool()
    {
        final IToolbarButton button = toolbarManager.createButtonBuilder(testDevicePlugin, "My button")
                .setImage(ImageUtils.getDefaultImage()).build();
        assertThatIllegalArgumentException().isThrownBy(() -> toolbarManager.addDrawingToolButton(button));
    }

    @Test
    public void testCreateSeparator()
    {
        final IToolbarSeparator separator = toolbarManager.createSeparator(testDevicePlugin, "My tab:0");
        assertThat(separator).isNotNull().extracting(IToolbarComponent::getPluginUniqueKey)
                .isEqualTo(testDevicePlugin.getUniqueKey());
    }

    @Test
    public void testCreateTab()
    {
        final IToolbarTab tab = toolbarManager.createTab(testDevicePlugin, "My tab");
        assertThat(tab).isNotNull().extracting(IToolbarTab::getPluginUniqueKey, IToolbarTab::getDisplayText)
                .containsExactly(testDevicePlugin.getUniqueKey(), "My tab");
    }

    @Test
    public void testAddTab()
    {
        final IToolbarTab tab = toolbarManager.createTab(testDevicePlugin, "My tab");
        final IToolbarButton button = toolbarManager.createButtonBuilder(testDevicePlugin, "My button")
                .setImage(ImageUtils.getDefaultImage()).build();
        tab.addComponent(button);
        toolbarManager.addTab(tab);
        SwingTestUtils.waitForEdtEvents();

        // The tab should be added as a reference tab (don't need to test that it was added to all configs since that is
        // tested in ToolbarConfigurationManagerTest)
        assertThat(configurationManager.getReferenceTab(tab.getId())).isNotNull();

        // The tab should be tracked in the plugin tab map
        Map<String, List<ToolbarTab>> pluginTabMap = CdiTestUtils.getField(toolbarManager, "pluginTabMap");
        assertThat(pluginTabMap.get(testDevicePlugin.getUniqueKey())).isNotNull().contains((ToolbarTab) tab);

        // Any contained buttons should be added to the plugin button map
        Map<String, List<ToolbarButton>> pluginButtonMap = CdiTestUtils.getField(toolbarManager, "pluginButtonMap");
        assertThat(pluginButtonMap.get(testDevicePlugin.getUniqueKey())).isNotNull().contains((ToolbarButton) button);
    }

    @Test
    public void testAddTab_notToolbarTab()
    {
        final IToolbarTab iToolbarTab = NoopInvocationHandler.forType(IToolbarTab.class);
        assertThatIllegalArgumentException().isThrownBy(
                () -> SwingTestUtils.invokeAndWaitOnEDT(() -> toolbarManager.addTab(iToolbarTab)));
    }

    @Test
    public void testInsertTab()
    {
        final IToolbarTab tab = toolbarManager.createTab(testDevicePlugin, "My tab");
        final IToolbarButton button = toolbarManager.createButtonBuilder(testDevicePlugin, "My button")
                .setImage(ImageUtils.getDefaultImage()).build();
        tab.addComponent(button);
        toolbarManager.insertTab(tab, 2);
        SwingTestUtils.waitForEdtEvents();

        // The tab should be added as a reference tab in the correct index (don't need to test that it was added to all
        // configs at the correct index since that is tested in ToolbarConfigurationManagerTest)
        assertThat(configurationManager.getReferenceTabs()).element(2).isNotNull().isEqualTo(tab);

        // The tab should be tracked in the plugin tab map
        Map<String, List<ToolbarTab>> pluginTabMap = CdiTestUtils.getField(toolbarManager, "pluginTabMap");
        assertThat(pluginTabMap.get(testDevicePlugin.getUniqueKey())).isNotNull().contains((ToolbarTab) tab);

        // Any contained buttons should be added to the plugin button map
        Map<String, List<ToolbarButton>> pluginButtonMap = CdiTestUtils.getField(toolbarManager, "pluginButtonMap");
        assertThat(pluginButtonMap.get(testDevicePlugin.getUniqueKey())).isNotNull().contains((ToolbarButton) button);
    }

    @Test
    public void testInsertTab_notToolbarTab()
    {
        final IToolbarTab iToolbarTab = NoopInvocationHandler.forType(IToolbarTab.class);
        assertThatIllegalArgumentException().isThrownBy(
                () -> SwingTestUtils.invokeAndWaitOnEDT(() -> toolbarManager.insertTab(iToolbarTab, 0)));
    }

    @Test
    public void testInsertTab_singularTab()
    {
        final IToolbarTab homeTab = toolbarManager.getSingularTab(TabType.HOME);
        assertThat(homeTab).isNotNull();
        assertThatIllegalArgumentException().isThrownBy(
                () -> SwingTestUtils.invokeAndWaitOnEDT(() -> toolbarManager.insertTab(homeTab, 0)));
    }

    @Test
    public void testRemoveTab()
    {
        final IToolbarTab tab = toolbarManager.createTab(testDevicePlugin, "My tab");
        toolbarManager.addTab(tab);
        SwingTestUtils.waitForEdtEvents();

        final UUID tabId = tab.getId();
        assertThat(toolbarManager.getTab(tabId)).isNotNull();

        toolbarManager.removeTab(tab);

        // The tab should be removed as a reference tab
        assertThat(configurationManager.getReferenceTabs()).extracting(ToolbarTab::getId).doesNotContain(tabId);

        // The tab should be removed from the plugin tab map
        Map<String, List<ToolbarTab>> pluginTabMap = CdiTestUtils.getField(toolbarManager, "pluginTabMap");
        assertThat(pluginTabMap.get(testDevicePlugin.getUniqueKey())).isNotNull().doesNotContain((ToolbarTab) tab);
    }

    @Test
    public void testRemoveTab_notToolbarTab()
    {
        final IToolbarTab iToolbarTab = NoopInvocationHandler.forType(IToolbarTab.class);
        assertThatIllegalArgumentException().isThrownBy(() -> toolbarManager.removeTab(iToolbarTab));
    }

    @Test
    public void testRemoveTab_singularTab()
    {
        final IToolbarTab homeTab = toolbarManager.getSingularTab(TabType.HOME);
        assertThat(homeTab).isNotNull();
        assertThatIllegalArgumentException().isThrownBy(() -> toolbarManager.removeTab(homeTab));
    }

    @Test
    public void testGetSelectedTabId()
    {
        // We need the real Toolbar and the EDT to actually test this, so just make sure it doesn't blow up and returns
        // null as expected.
        assertThat(toolbarManager.getSelectedTabId()).isNull();
    }

    // Note: we can't test some of the methods like selectTab(), clickButton(), or setToggleButtonState() since they
    // require the EDT and the real Toolbar, so just test the error cases.
    @Test
    public void testSelectSingularTab_notSingularType()
    {
        assertThatIllegalArgumentException().isThrownBy(() -> toolbarManager.selectSingularTab(TabType.USER_CREATED));
    }

    @Test
    public void testSelectTab_noTabForId()
    {
        assertThatIllegalArgumentException().isThrownBy(() -> toolbarManager.selectTab(UUID.randomUUID()));
    }

    @Test
    public void testClickButton_noButtonForId()
    {
        assertThatIllegalArgumentException().isThrownBy(() -> toolbarManager.clickButton(UUID.randomUUID()));
    }

    @Test
    public void testGetToggleButtonState_noButtonForId()
    {
        assertThatIllegalArgumentException().isThrownBy(() -> toolbarManager.getToggleButtonState(UUID.randomUUID()));
    }

    @Test
    public void testGetToggleButtonState_isNotTogglable()
    {
        final IToolbarButton button = toolbarManager.createButtonBuilder(testDevicePlugin, "My button")
                .setImage(ImageUtils.getDefaultImage()).build();
        final IToolbarTab homeTab = toolbarManager.getSingularTab(TabType.HOME);
        assertThat(homeTab).isNotNull();
        homeTab.addComponent(button);

        assertThatIllegalArgumentException().isThrownBy(() -> toolbarManager.getToggleButtonState(button.getId()));
    }

    @Test
    public void testSetToggleButtonState_noButtonForId()
    {
        assertThatIllegalArgumentException().isThrownBy(() -> toolbarManager.setToggleButtonState(UUID.randomUUID(), false));
    }

    @Test
    public void testSetToggleButtonState_isNotTogglable()
    {
        final IToolbarButton button = toolbarManager.createButtonBuilder(testDevicePlugin, "My button")
                .setImage(ImageUtils.getDefaultImage()).build();
        final IToolbarTab homeTab = toolbarManager.getSingularTab(TabType.HOME);
        assertThat(homeTab).isNotNull();
        homeTab.addComponent(button);

        assertThatIllegalArgumentException().isThrownBy(() -> toolbarManager.setToggleButtonState(button.getId(), false));
    }

    @Test
    public void testGetSingularTab()
    {
        // Singular tabs should be returned by type
        assertThat(toolbarManager.getSingularTab(TabType.ATTACHMENTS)).isNotNull();
    }

    @Test
    public void testGetSingularTab_notSingularType()
    {
        assertThatIllegalArgumentException().isThrownBy(() -> toolbarManager.getSingularTab(TabType.USER_CREATED));
    }

    @Test
    public void testGetTab()
    {
        // Singular tabs should be returned by id
        assertThat(toolbarManager.getTab(TabType.DATA.requireSingularId())).isNotNull();

        // Nothing should be returned if the id doesn't exist
        assertThat(toolbarManager.getTab(UUID.randomUUID())).isNull();
    }

    @Test
    public void testGetTabs()
    {
        // All reference tabs should be returned
        assertThat(toolbarManager.getTabs()).asInstanceOf(list(ToolbarTab.class))
                .containsAll(configurationManager.getReferenceTabs());

        // List should have one tab for singular types
        assertThat(toolbarManager.getTabs(TabType.HOME)).hasSize(1).first().isNotNull();

        // List should be empty if no tabs exist for type
        assertThat(toolbarManager.getTabs(TabType.PLUGIN_CREATED)).isEmpty();

        // Add tab for a type; list should contain tab
        final ToolbarTab tab = ToolbarTab.createPluginTab(testDevicePlugin, "Test tab");
        configurationManager.addReferenceTab(tab);
        assertThat(toolbarManager.getTabs(TabType.PLUGIN_CREATED))
                .asInstanceOf(list(ToolbarTab.class))
                .containsOnly(tab);
    }

    @Test
    public void testGetSingularButton()
    {
        // Singular buttons should be returned by type
        assertThat(toolbarManager.getSingularButton(ButtonType.FILE)).isNotNull();
    }

    @Test
    public void testGetSingularButton_notSingularType()
    {
        assertThatIllegalArgumentException().isThrownBy(() -> toolbarManager.getSingularButton(ButtonType.CONNECTION_PLUGIN));
    }

    @Test
    public void testGetButton()
    {
        // Singular buttons should be returned by id
        assertThat(toolbarManager.getButton(ButtonType.FILE.requireSingularId())).isNotNull();

        // Nothing should be returned if the id doesn't exist
        assertThat(toolbarManager.getButton(UUID.randomUUID())).isNull();
    }

    @Test
    public void testGetButtons()
    {
        // All reference buttons should be returned
        assertThat(toolbarManager.getButtons()).asInstanceOf(collection(ToolbarButton.class))
                .containsAll(configurationManager.getReferenceButtons());

        // List should have one button for singular types
        assertThat(toolbarManager.getButtons(ButtonType.CONNECTIONS)).hasSize(1).first().isNotNull();

        // List should be empty if no buttons exist for type
        assertThat(toolbarManager.getButtons(ButtonType.CONNECTION_PLUGIN)).isEmpty();

        // Add two buttons for a type; list should contain both buttons
        final ToolbarButton connectionButton1 = ToolbarTestUtils.createCoreButton(ButtonType.CONNECTION_PLUGIN, "Dummy connection 1");
        final ToolbarButton connectionButton2 = ToolbarTestUtils.createCoreButton(ButtonType.CONNECTION_PLUGIN, "Dummy connection 2");
        configurationManager.addReferenceButton(connectionButton1);
        configurationManager.addReferenceButton(connectionButton2);
        assertThat(toolbarManager.getButtons(ButtonType.CONNECTION_PLUGIN))
                .asInstanceOf(list(ToolbarButton.class))
                .containsOnly(connectionButton1, connectionButton2);
    }

    /**
     * Test that the method to get the active tabs returns the tabs from the active configuration.
     *
     * @since 5.1.0
     */
    @Test
    public void newConfiguration_addToManager_getTabsFromConfiguration()
    {
        ToolbarConfiguration configuration = ToolbarConfiguration.createConfiguration();
        configuration.addTab(ToolbarTab.createUserTab("TestTab"), false);
        configurationManager.addConfiguration(configuration);
        configurationManager.activateConfiguration(configuration);
        int size = toolbarManager.getActiveTabs().size();
        assertThat(toolbarManager.getActiveTabs().get(size - 1).getDisplayText()).isEqualTo("TestTab");

        configurationManager.activateConfiguration(configurationManager.getDefaultConfiguration());
        assertThat(toolbarManager.getActiveTabs().size()).isEqualTo(size - 1);
    }
}