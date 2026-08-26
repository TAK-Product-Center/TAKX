package gov.takx.core.view.gui.toolbar;

import gov.takx.api.annotation.SuppressLogging;
import gov.takx.commons.swing.ImageUtils;
import gov.takx.core.toolbar.AToolbarComponent;
import gov.takx.core.toolbar.ToolbarButton;
import gov.takx.core.toolbar.ToolbarSeparator;
import gov.takx.core.toolbar.ToolbarTab;
import gov.takx.core.toolbar.ToolbarTestUtils;
import gov.takx.platform.preferences.UserPreferenceEntry;
import gov.takx.platform.toolbar.ToolbarButtonLabelLocation;
import gov.takx.test.AContainerTest;
import gov.takx.test.mock.TestActionListener;
import gov.takx.test.mock.TestMouseListener;
import gov.takx.test.mock.plugin.device.TestDevicePlugin;
import gov.takx.test.utils.SwingTestUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import javax.swing.AbstractButton;
import javax.swing.Icon;
import javax.swing.JComponent;
import javax.swing.JSeparator;
import javax.swing.JToolBar;
import javax.swing.SwingConstants;

import java.awt.Component;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIndexOutOfBoundsException;

/**
 * Unit tests for {@link Toolbar}.
 *
 * @since 3.4
 */
public class ToolbarTest extends AContainerTest
{
    private final TestDevicePlugin testDevicePlugin = new TestDevicePlugin();
    private Toolbar toolbar;

    @BeforeEach
    public void setupBeforeMethod()
    {
        toolbar = new Toolbar();

        // Make sure we start off with the default label location
        UserPreferenceEntry.TOOLBAR_BUTTON_LABEL_LOCATION.putAndSave(ToolbarButtonLabelLocation.BOTTOM.name());
    }

    @Test
    public void testSetMinimized()
    {
        toolbar.setMinimized(true);
        // TODO KMB: Not implemented yet so nothing to verify... just shouldn't blow up (TAKX-1380)
    }

    @Test
    public void testClear_removesAllTabs()
    {
        // Setup toolbar with one tab and one button
        setupToolbarTab("tab 1", createButtons(List.of("button 1")));

        // Clear should remove the tab (including its associated toolbar)
        toolbar.clear();
        SwingTestUtils.waitForEdtEvents();
        assertThat(toolbar.getTabCount()).isEqualTo(0);
    }

    @Test
    public void testInsertTab_withValidIndices_areAdded()
    {
        assertThat(toolbar.getTabCount()).isEqualTo(0);

        // Add the first tab to an empty toolbar
        final ToolbarTab tabModel1 = ToolbarTab.createUserTab("Test");
        tabModel1.setDisplayText("tab 1");
        assertThat(toolbar.insertTab(tabModel1, 0)).isTrue();
        assertThat(toolbar.getTabCount()).isEqualTo(1);
        assertThat(toolbar.indexOfTab("tab 1")).isEqualTo(0);
        assertThat(toolbar.getComponentAt(0)).isInstanceOf(JToolBar.class);

        // Add a new tab at position 0; should shift the tab that was at 0
        final ToolbarTab tabModel2 = ToolbarTab.createUserTab("Test");
        tabModel2.setDisplayText("tab 2");
        assertThat(toolbar.insertTab(tabModel2, 0)).isTrue();
        assertThat(toolbar.getTabCount()).isEqualTo(2);
        assertThat(toolbar.indexOfTab("tab 2")).isEqualTo(0);
        assertThat(toolbar.indexOfTab("tab 1")).isEqualTo(1);

        // Add a new tab at position 1; should leave the tab at 0 and shift the tab that was at 1
        final ToolbarTab tabModel3 = ToolbarTab.createUserTab("Test");
        tabModel3.setDisplayText("tab 3");
        assertThat(toolbar.insertTab(tabModel3, 1)).isTrue();
        assertThat(toolbar.getTabCount()).isEqualTo(3);
        assertThat(toolbar.indexOfTab("tab 2")).isEqualTo(0);
        assertThat(toolbar.indexOfTab("tab 3")).isEqualTo(1);
        assertThat(toolbar.indexOfTab("tab 1")).isEqualTo(2);
    }

    @Test
    public void testInsertTab_withInvalidIndex_isNotAdded()
    {
        // Adding a tab with a negative index should throw an exception (and the tab should not be added)
        final ToolbarTab tabModel = ToolbarTab.createUserTab("Test");
        assertThatIndexOutOfBoundsException().isThrownBy(() -> SwingTestUtils.invokeAndWaitOnEDT(() -> toolbar.insertTab(tabModel, -2)));
        assertThat(toolbar.getTabCount()).isEqualTo(0);

        // Adding a tab with a too large index should throw an exception (and the tab should not be added)
        assertThatIndexOutOfBoundsException().isThrownBy(() -> SwingTestUtils.invokeAndWaitOnEDT(() -> toolbar.insertTab(tabModel, 1)));
        assertThat(toolbar.getTabCount()).isEqualTo(0);
    }

    @Test
    public void testAddTab_withValidTab_isAdded()
    {
        // Verify that a valid tab is added
        final ToolbarTab tabModel = ToolbarTab.createUserTab("Test");
        assertThat(toolbar.addTab(tabModel)).isTrue();
        assertThat(toolbar.getTabCount()).isEqualTo(1);
    }

    @Test
    public void testAddTab_withInvalidTab_isNotAdded()
    {
        // Verify that an invalid tab is not added
        final ToolbarTab tabModel = ToolbarTab.createUserTab("Test");
        tabModel.setValid(false);
        assertThat(toolbar.addTab(tabModel)).isFalse();
        assertThat(toolbar.getTabCount()).isEqualTo(0);
    }

    @Test
    public void testAddTab_withInvisibleTab_isNotAdded()
    {
        // Verify that an invisible tab is not added
        final ToolbarTab tabModel = ToolbarTab.createUserTab("Test");
        tabModel.setVisible(false);
        assertThat(toolbar.addTab(tabModel)).isFalse();
        assertThat(toolbar.getTabCount()).isEqualTo(0);
    }

    @Test
    public void testRemoveTab_withValidTab_removesTab()
    {
        final ToolbarTab tabModel = setupToolbarTab("tab 1", Collections.emptyList());

        toolbar.removeTab(tabModel);
        SwingTestUtils.waitForEdtEvents();
        assertThat(toolbar.getTabCount()).isEqualTo(0);
    }

    @SuppressLogging(Toolbar.class)
    @Test
    public void testRemoveTab_withNonExistentTab_doesNothing()
    {
        setupToolbarTab("tab 1");

        // Removing a tab that isn't present should do nothing
        toolbar.removeTab(ToolbarTab.createUserTab("Test"));
        SwingTestUtils.waitForEdtEvents();
        assertThat(toolbar.getTabCount()).isEqualTo(1);
    }

    @SuppressLogging(Toolbar.class)
    @Test
    public void testSelectTab()
    {
        final ToolbarTab tabModel1 = setupToolbarTab("tab 1");
        final ToolbarTab tabModel2 = setupToolbarTab("tab 2");

        final UUID tabModel1Id = tabModel1.getId();
        boolean selected = toolbar.selectTab(tabModel1Id);
        SwingTestUtils.waitForEdtEvents();
        assertThat(selected).isTrue();
        assertThat(toolbar.getSelectedIndex()).isEqualTo(0);
        assertThat(toolbar.getSelectedTabId()).isEqualTo(tabModel1Id);

        final UUID tabModel2Id = tabModel2.getId();
        selected = toolbar.selectTab(tabModel2Id);
        SwingTestUtils.waitForEdtEvents();
        assertThat(selected).isTrue();
        assertThat(toolbar.getSelectedIndex()).isEqualTo(1);
        assertThat(toolbar.getSelectedTabId()).isEqualTo(tabModel2Id);

        selected = toolbar.selectTab(UUID.randomUUID());
        SwingTestUtils.waitForEdtEvents();
        assertThat(selected).isFalse();
        assertThat(toolbar.getSelectedIndex()).isEqualTo(1);
        assertThat(toolbar.getSelectedTabId()).isEqualTo(tabModel2Id);
    }

    @Test
    public void testUpdateTabText()
    {
        final ToolbarTab tabModel = setupToolbarTab("tab 1");
        toolbar.selectTab(tabModel.getId());
        SwingTestUtils.waitForEdtEvents();

        tabModel.setDisplayText("new tab text");
        toolbar.updateTabText(tabModel);
        SwingTestUtils.waitForEdtEvents();

        assertThat(toolbar.getTitleAt(0)).isEqualTo("new tab text");
        assertThat(toolbar.getSelectedIndex()).isEqualTo(0);
    }

    @Test
    public void testAddButton()
    {
        final ToolbarTab tabModel = setupToolbarTab("tab 1");

        final JScrollableToolBar jToolBar = getToolbarForTab(0);
        assertThat(jToolBar.getToolCount()).isEqualTo(0);

        final ToolbarButton buttonModel = ToolbarTestUtils.createPluginButton(testDevicePlugin, "button 1");
        tabModel.addComponent(buttonModel, false);

        toolbar.addComponent(buttonModel, tabModel);
        SwingTestUtils.waitForEdtEvents();
        assertThat(jToolBar.getToolCount()).isEqualTo(1);
        final AbstractButton button = getButtonAtIndex("tab 1", 0);
        assertThat(button.getText()).isEqualTo("button 1");
    }

    @Test
    public void testInsertButton()
    {
        final ToolbarTab tabModel = setupToolbarTab("tab 1");

        final JScrollableToolBar jToolBar = getToolbarForTab(0);
        assertThat(jToolBar.getToolCount()).isEqualTo(0);

        final ToolbarButton buttonModel = ToolbarTestUtils.createPluginButton(testDevicePlugin, "button 1");
        tabModel.addComponent(buttonModel, false);

        toolbar.insertComponent(buttonModel, tabModel, 0);
        SwingTestUtils.waitForEdtEvents();
        assertThat(jToolBar.getToolCount()).isEqualTo(1);
        final AbstractButton button = getButtonAtIndex("tab 1", 0);
        assertThat(button.getText()).isEqualTo("button 1");
    }

    @Test
    public void testAddSeparator()
    {
        final ToolbarTab tabModel = setupToolbarTab("tab 1");

        final JScrollableToolBar jToolBar = getToolbarForTab(0);
        assertThat(jToolBar.getToolCount()).isEqualTo(0);

        final ToolbarSeparator separatorModel = new ToolbarSeparator("tab 1", 0);
        tabModel.addComponent(separatorModel);

        toolbar.addComponent(separatorModel, tabModel);
        SwingTestUtils.waitForEdtEvents();
        assertThat(jToolBar.getToolCount()).isEqualTo(1);
        final JSeparator separator = getSeparatorAtIndex("tab 1", 0);
        assertThat(separator.getName()).isEqualTo(separatorModel.getId().toString());
    }

    @Test
    public void testInsertSeparator()
    {
        final ToolbarTab tabModel = setupToolbarTab("tab 1");

        final JScrollableToolBar jToolBar = getToolbarForTab(0);
        assertThat(jToolBar.getToolCount()).isEqualTo(0);

        final ToolbarSeparator separatorModel = new ToolbarSeparator("tab 1", 0);
        tabModel.addComponent(separatorModel);

        toolbar.insertComponent(separatorModel, tabModel, 0);
        SwingTestUtils.waitForEdtEvents();
        assertThat(jToolBar.getToolCount()).isEqualTo(1);
        final JSeparator separator = getSeparatorAtIndex("tab 1", 0);
        assertThat(separator.getName()).isEqualTo(separatorModel.getId().toString());
    }

    @Test
    public void testRemoveButton()
    {
        final ToolbarTab tabModel = setupToolbarTab("tab 1", createButtons(List.of("button 1")));
        final AToolbarComponent componentModel = tabModel.getComponents().get(0);

        toolbar.removeComponent(componentModel, tabModel);
        SwingTestUtils.waitForEdtEvents();
        final JScrollableToolBar jToolBar = getToolbarForTab(0);
        assertThat(jToolBar.getToolCount()).isEqualTo(0);
    }

    @Test
    public void testUpdateButton_withNewText()
    {
        final List<ToolbarButton> buttonModels = createButtons(List.of("button 1"));
        setupToolbarTab("tab 1", buttonModels);
        ToolbarButton buttonModel = buttonModels.get(0);

        final AbstractButton button = getButtonAtIndex("tab 1", 0);
        assertThat(button.getText()).isEqualTo("button 1");

        buttonModel.setDisplayText("custom 1");
        toolbar.updateButton(buttonModel);
        SwingTestUtils.waitForEdtEvents();

        assertThat(button.getText()).isEqualTo("custom 1");
    }

    @SuppressWarnings("ConstantConditions")
    @Test
    public void testUpdateButton_withNewImage()
    {
        final List<ToolbarButton> buttonModels = createButtons(List.of("button 1"));
        setupToolbarTab("tab 1", buttonModels);
        ToolbarButton buttonModel = buttonModels.get(0);

        final AbstractButton button = getButtonAtIndex("tab 1", 0);
        final Icon oldIcon = buttonModel.getIcon();
        assertThat(button.getIcon()).isEqualTo(oldIcon);

        buttonModel.setImage(ImageUtils.getImage(getClass().getClassLoader().getResource("icons/truck-96.png"), false));
        toolbar.updateButton(buttonModel);
        SwingTestUtils.waitForEdtEvents();

        assertThat(button.getIcon()).isEqualTo(buttonModel.getIcon()).isNotEqualTo(oldIcon);
    }

    @Test
    public void testUpdateButton_withNewListener()
    {
        final List<ToolbarButton> buttonModels = createButtons(List.of("button 1"));
        ToolbarButton buttonModel = buttonModels.get(0);
        final TestActionListener actionListener1 = new TestActionListener();
        final TestMouseListener mouseListener1 = new TestMouseListener();
        buttonModel.addActionListener(actionListener1);
        buttonModel.addMouseListener(mouseListener1);
        setupToolbarTab("tab 1", buttonModels);

        final TestActionListener actionListener2 = new TestActionListener();
        buttonModel.removeActionListener(actionListener1);
        buttonModel.addActionListener(actionListener2);
        final TestMouseListener mouseListener2 = new TestMouseListener();
        buttonModel.removeMouseListener(mouseListener1);
        buttonModel.addMouseListener(mouseListener2);
        toolbar.updateButton(buttonModel);
        SwingTestUtils.waitForEdtEvents();

        // Trigger a click to verify the correct listeners were present (should be listener2 only)
        toolbar.doButtonClick(buttonModel.getId());
        SwingTestUtils.waitForEdtEvents();
        assertThat(actionListener1.getCallCount()).isEqualTo(0);
        assertThat(actionListener2.getCallCount()).isEqualTo(1);
        assertThat(mouseListener1.getCallCount()).isEqualTo(0);
        assertThat(mouseListener2.getCallCount()).isEqualTo(1);
    }

    @Test
    public void testUpdateButton_withNewState()
    {
        final List<ToolbarButton> buttonModels = createButtons(List.of("button 1"));
        setupToolbarTab("tab 1", buttonModels);
        ToolbarButton buttonModel = buttonModels.get(0);

        final AbstractButton button = getButtonAtIndex("tab 1", 0);
        assertThat(button.isEnabled()).isEqualTo(true);

        buttonModel.setEnabled(false);
        toolbar.updateButton(buttonModel);
        SwingTestUtils.waitForEdtEvents();

        assertThat(button.isEnabled()).isEqualTo(false);
    }

    @Test
    public void testUpdateButton_withNewTooltip()
    {
        final List<ToolbarButton> buttonModels = createButtons(List.of("button 1"));
        ToolbarButton buttonModel = buttonModels.get(0);
        buttonModel.setTooltipText("tooltip 1");
        setupToolbarTab("tab 1", buttonModels);

        final AbstractButton button = getButtonAtIndex("tab 1", 0);
        assertThat(button.getToolTipText()).isEqualTo("tooltip 1");

        buttonModel.setTooltipText("tooltip 2");
        toolbar.updateButton(buttonModel);
        SwingTestUtils.waitForEdtEvents();

        assertThat(button.getToolTipText()).isEqualTo("tooltip 2");
    }

    @Test
    public void testGetAndSetSelectedButtonInGroup()
    {
        final String groupName = "toggle group";

        final ToolbarTab tabModel = setupToolbarTab("tab 1");
        final ToolbarButton buttonModel1 = ToolbarTestUtils.createPluginButtonBuilder(testDevicePlugin, "button 1")
                .setTogglable(true)
                .setToggleButtonGroupName(groupName)
                .build();
        tabModel.addComponent(buttonModel1, false);

        final ToolbarButton buttonModel2 = ToolbarTestUtils.createPluginButtonBuilder(testDevicePlugin, "button 2")
                .setTogglable(true)
                .setToggleButtonGroupName(groupName)
                .build();
        tabModel.addComponent(buttonModel2, false);

        // Create a second tab to add a button copy (since button copies can't exist in the same tab)
        final ToolbarTab tabModel2 = setupToolbarTab("tab 2");
        final ToolbarButton buttonModel3 = buttonModel2.copy();
        buttonModel3.setDisplayText("button 2 copy");
        tabModel2.addComponent(buttonModel3, false);

        toolbar.addComponent(buttonModel1, tabModel);
        toolbar.addComponent(buttonModel2, tabModel);
        toolbar.addComponent(buttonModel3, tabModel2);
        SwingTestUtils.waitForEdtEvents();

        final AbstractButton button1 = getButtonAtIndex("tab 1", 0);
        final AbstractButton button2 = getButtonAtIndex("tab 1", 1);
        final AbstractButton button3 = getButtonAtIndex("tab 2", 0);

        assertThat(toolbar.isAnyButtonSelectedInGroup(groupName)).isFalse();

        toolbar.setSelectedButtonInGroup(buttonModel1.getId(), groupName);
        assertThat(toolbar.isAnyButtonSelectedInGroup(groupName)).isTrue();
        assertThat(button1.isSelected()).isTrue();
        assertThat(button2.isSelected()).isFalse();
        assertThat(button3.isSelected()).isFalse();

        toolbar.setSelectedButtonInGroup(buttonModel2.getId(), groupName);
        assertThat(toolbar.isAnyButtonSelectedInGroup(groupName)).isTrue();
        assertThat(button1.isSelected()).isFalse();
        assertThat(button2.isSelected()).isTrue();
        assertThat(button3.isSelected()).isTrue();

        toolbar.setSelectedButtonInGroup(null, groupName);
        assertThat(toolbar.isAnyButtonSelectedInGroup(groupName)).isFalse();
        assertThat(button1.isSelected()).isFalse();
        assertThat(button2.isSelected()).isFalse();
        assertThat(button3.isSelected()).isFalse();
    }

    @Test
    public void testSetToggleButtonSelected()
    {
        final ToolbarTab tabModel = setupToolbarTab("tab 1");
        final ToolbarButton buttonModel = ToolbarTestUtils.createPluginButtonBuilder(testDevicePlugin, "button 1")
                .setTogglable(true)
                .build();
        tabModel.addComponent(buttonModel, false);

        // Create a second tab to add a button copy (since button copies can't exist in the same tab)
        final ToolbarTab tabModel2 = setupToolbarTab("tab 2");
        final ToolbarButton buttonModel2 = buttonModel.copy();
        buttonModel2.setDisplayText("button 1 copy");
        tabModel2.addComponent(buttonModel2, false);

        toolbar.addComponent(buttonModel, tabModel);
        toolbar.addComponent(buttonModel2, tabModel2);
        SwingTestUtils.waitForEdtEvents();

        final AbstractButton button = getButtonAtIndex("tab 1", 0);
        final AbstractButton button2 = getButtonAtIndex("tab 2", 0);

        final UUID buttonId = buttonModel.getId();
        toolbar.setToggleButtonState(buttonId, true);
        SwingTestUtils.waitForEdtEvents();
        assertThat(button.isSelected()).isTrue();
        assertThat(button2.isSelected()).isTrue();
        assertThat(toolbar.getToggleButtonState(buttonId)).isTrue();

        toolbar.setToggleButtonState(buttonId, false);
        SwingTestUtils.waitForEdtEvents();
        assertThat(button.isSelected()).isFalse();
        assertThat(button2.isSelected()).isFalse();
        assertThat(toolbar.getToggleButtonState(buttonId)).isFalse();
    }

    @Test
    public void testDoButtonClick()
    {
        final ToolbarTab tabModel = setupToolbarTab("tab 1");
        final ToolbarButton buttonModel = ToolbarTestUtils.createPluginButton(testDevicePlugin, "button 1");
        final TestActionListener listener = new TestActionListener();
        buttonModel.addActionListener(listener);
        tabModel.addComponent(buttonModel, false);

        toolbar.addComponent(buttonModel, tabModel);
        SwingTestUtils.waitForEdtEvents();

        toolbar.doButtonClick(buttonModel.getId());
        SwingTestUtils.waitForEdtEvents();
        assertThat(listener.getCallCount()).isEqualTo(1);
    }

    @Test
    public void testButtonLabelLocationChange()
    {
        final ToolbarTab tabModel = setupToolbarTab("tab 1", createButtons(List.of("button 1")));
        final AbstractButton button = getButtonAtIndex("tab 1", 0);

        UserPreferenceEntry.TOOLBAR_BUTTON_LABEL_LOCATION.putAndSave(ToolbarButtonLabelLocation.NONE.name());
        SwingTestUtils.waitForEdtEvents();
        assertThat(button.getText()).isEqualTo("");

        UserPreferenceEntry.TOOLBAR_BUTTON_LABEL_LOCATION.putAndSave(ToolbarButtonLabelLocation.RIGHT.name());
        SwingTestUtils.waitForEdtEvents();
        assertThat(button.getText()).isEqualTo("button 1");
        assertThat(button.getHorizontalTextPosition()).isEqualTo(SwingConstants.RIGHT);
        assertThat(button.getVerticalTextPosition()).isEqualTo(SwingConstants.CENTER);

        UserPreferenceEntry.TOOLBAR_BUTTON_LABEL_LOCATION.putAndSave(ToolbarButtonLabelLocation.BOTTOM.name());
        SwingTestUtils.waitForEdtEvents();
        assertThat(button.getText()).isEqualTo("button 1");
        assertThat(button.getHorizontalTextPosition()).isEqualTo(SwingConstants.CENTER);
        assertThat(button.getVerticalTextPosition()).isEqualTo(SwingConstants.BOTTOM);

        // Set preference and verify newly added button conforms
        UserPreferenceEntry.TOOLBAR_BUTTON_LABEL_LOCATION.putAndSave(ToolbarButtonLabelLocation.NONE.name());

        final ToolbarButton buttonModel2 = ToolbarTestUtils.createPluginButton(testDevicePlugin, "button 2");

        tabModel.addComponent(buttonModel2, false);
        toolbar.addComponent(buttonModel2, tabModel);
        SwingTestUtils.waitForEdtEvents();

        final AbstractButton button2 = getButtonAtIndex("tab 1", 1);
        assertThat(button2.getText()).isEmpty();
    }

    /**
     * Setup the toolbar with an empty tab added at the end of the toolbar.
     *
     * @param tabDisplayText The text to display for the tab
     * @return The model for the tab
     */
    private ToolbarTab setupToolbarTab(String tabDisplayText)
    {
        return setupToolbarTab(tabDisplayText, Collections.emptyList());
    }

    /**
     * Create a list of buttons with the supplied names.
     *
     * @param buttonNames The names of buttons to create
     * @return The list of buttons
     * @since 3.4
     */
    private List<ToolbarButton> createButtons(List<String> buttonNames)
    {
        List<ToolbarButton> buttons = new ArrayList<>();
        for (String buttonName : buttonNames)
        {
            buttons.add(ToolbarTestUtils.createPluginButton(testDevicePlugin, buttonName));
        }

        return buttons;
    }

    /**
     * Setup the toolbar with a tab added at the end of the toolbar, containing the supplied buttons.
     *
     * @param tabDisplayText The text to display for the tab
     * @param buttons The  buttons to add to the tab (may be empty to have an empty tab)
     * @return The model for the tab
     */
    private ToolbarTab setupToolbarTab(String tabDisplayText, List<ToolbarButton> buttons)
    {
        final int previousTabCount = toolbar.getTabCount();
        final ToolbarTab tabModel = ToolbarTab.createUserTab("Test");
        tabModel.setDisplayText(tabDisplayText);

        buttons.forEach(b -> tabModel.addComponent(b, false));

        toolbar.addTab(tabModel);
        assertThat(toolbar.getTabCount()).isEqualTo(previousTabCount + 1);

        final JScrollableToolBar jToolBar = getToolbarForTab(tabDisplayText);
        assertThat(jToolBar.getToolCount()).isEqualTo(buttons.size());

        return tabModel;
    }

    /**
     * Helper method to get the JToolBar containing buttons for the specified tab.
     *
     * @param tabName The name of the tab
     * @return The Swing JToolBar associated with the tab.
     */
    private JScrollableToolBar getToolbarForTab(String tabName)
    {
        return getToolbarForTab(toolbar.indexOfTab(tabName));
    }

    /**
     * Helper method to get the JToolBar containing buttons for the specified tab.
     *
     * @param tabIndex The index of the tab
     * @return The Swing JToolBar associated with the tab.
     */
    private JScrollableToolBar getToolbarForTab(int tabIndex)
    {
        final Component component = toolbar.getComponentAt(tabIndex);
        assertThat(component).isInstanceOf(JScrollableToolBar.class);
        return (JScrollableToolBar) component;
    }

    /**
     * Helper method to get the button at the specified index within the named tab.
     * @param tabName The name of the tab
     * @param index The index of the button
     * @return The AbstractButton at the specified index on the tab.
     */
    private AbstractButton getButtonAtIndex(String tabName, int index)
    {
        return getComponentAtIndex(tabName, index, AbstractButton.class);
    }

    /**
     * Helper method to get the component at the specified index within the named tab.
     * @param tabName The name of the tab
     * @param index The index of the component
     * @return The JComponent at the specified index on the tab.
     */
    @SuppressWarnings("unchecked")
    private <T> T getComponentAtIndex(String tabName, int index, Class<T> componentClass)
    {
        final JScrollableToolBar jToolBar = getToolbarForTab(tabName);
        assertThat(jToolBar.getToolCount()).isGreaterThan(index);
        final JComponent component = jToolBar.getToolAtIndex(index);
        assertThat(component).isInstanceOf(componentClass).isNotNull();
        return (T) component;
    }

    /**
     * Helper method to get the separator at the specified index within the named tab.
     * @param tabName The name of the tab
     * @param index The index of the separator
     * @return The JSeparator at the specified index on the tab.
     */
    private JSeparator getSeparatorAtIndex(String tabName, int index)
    {
        return getComponentAtIndex(tabName, index, JSeparator.class);
    }
}