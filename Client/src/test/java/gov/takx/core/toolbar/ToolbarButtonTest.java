package gov.takx.core.toolbar;

import gov.takx.api.toolbar.ButtonType;
import gov.takx.test.AContainerTest;
import gov.takx.test.mock.plugin.device.TestDevicePlugin;
import gov.takx.test.mock.plugin.device.TestDevicePlugin2;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseListener;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

/**
 * Unit tests for the methods of {@link ToolbarButton}.
 *
 * @since 2.1 (Renamed in 3.4)
 */
public class ToolbarButtonTest extends AContainerTest
{
    /**
     * Tab which contains the toolbar button.
     */
    private ToolbarTab tab;

    /**
     * A button configured as if it came from a plugin.
     */
    private ToolbarButton pluginButton;

    /**
     * A toggle button used in a tab.
     */
    private ToolbarButton toolbarButton;

    private final TestDevicePlugin testDevicePlugin = new TestDevicePlugin();
    private boolean testSetupDone = false;

    /**
     * Create two buttons to use in the tests, one toggle button and one plugin button.
     * <p>
     * Note: Due to a limitation in JMockit (which I consider a bug), Mocked fields are not initialized in a
     * BeforeClass or BeforeTest annotated method.  So, we have to use a BeforeMethod, and then use a flag to ensure
     * that we only perform our initial setup one time.
     */
    @BeforeEach
    public void setup()
    {
        if (!testSetupDone)
        {
            // Create a toggle button with custom text
            final ButtonType buttonType = ButtonType.CITY_SEARCH;
            toolbarButton = ToolbarTestUtils.createCoreButtonBuilder(buttonType, buttonType.requireDisplayText())
                    .setTooltipText("Searches for stuff")
                    .setTogglable(true)
                    .build();

            // Add the button to a tab so that its tab field will be set
            tab = ToolbarTab.createUserTab("Test");
            tab.addComponent(toolbarButton, false);

            // Create a button to appear as if it was created by a plugin
            pluginButton = ToolbarTestUtils.createPluginButton(testDevicePlugin, "My plugin button");
            testSetupDone = true;
        }
    }

    /**
     * Verifies that all expected fields match when the copy constructor is used for a toolbar button.
     */
    @Test
    public void testCopyConstructor()
    {
        ToolbarButton newButton = toolbarButton.copy();

        compareCopyConstructedButtons(newButton, toolbarButton);
    }

    /**
     * Verifies that all expected fields match when the copy constructor is used for a plugin button.
     */
    @Test
    public void testCopyConstructorForPluginButton()
    {
        ToolbarButton newButton = pluginButton.copy();

        compareCopyConstructedButtons(newButton, pluginButton);
    }

    /**
     * Verifies that the parent tab is set correctly if no tab has been set.
     */
    @Test
    public void testSetTab()
    {
        ToolbarButton button = ToolbarTestUtils.createPluginButton(testDevicePlugin, "test");
        button.setTab(tab);
        assertThat(button.getTab()).isEqualTo(tab);
    }

    /**
     * Verifies that an exception is thrown if a tab has already been set when attempting to set a tab.
     */
    @Test
    public void testSetTab_AlreadyInTab()
    {
        ToolbarTab newTab = ToolbarTab.createUserTab("Test");
        assertThatIllegalArgumentException()
                .isThrownBy(() -> toolbarButton.setTab(newTab));
    }

    /**
     * Verifies that the action listener is added correctly
     */
    @Test
    public void testAddActionListener()
    {
        ToolbarButton button = ToolbarTestUtils.createPluginButton(testDevicePlugin, "test");
        ActionListener actionListener = e -> {
        };
        button.addActionListener(actionListener);
        assertThat(button.getActionListeners().get(0)).isEqualTo(actionListener);
    }

    /**
     * Verifies that the mouse listener is added correctly
     *
     * @since 5.0
     */
    @Test
    public void testAddMouseListener()
    {
        final ToolbarButton button = ToolbarTestUtils.createPluginButton(testDevicePlugin, "test");
        final MouseListener mouseListener = new MouseAdapter()
        {
        };
        button.addMouseListener(mouseListener);
        assertThat(button.getMouseListeners().get(0)).isEqualTo(mouseListener);
    }

    /**
     * Verifies that tool tip text is set correctly
     */
    @Test
    public void testSetActionToolTipText()
    {
        ToolbarButton button = ToolbarTestUtils.createPluginButton(testDevicePlugin, "test");
        String toolTipText = "Tool Tip Text";
        button.setTooltipText(toolTipText);
        assertThat(button.getTooltipText()).isEqualTo(toolTipText);
    }

    /**
     * Verifies that you can set the enabled property
     */
    @Test
    public void testSetEnabled()
    {
        ToolbarButton button = ToolbarTestUtils.createPluginButton(testDevicePlugin, "test");
        button.setEnabled(true);
        assertThat(button.isEnabled()).isEqualTo(true);
    }

    /**
     * Verifies that you can set the valid property
     */
    @Test
    public void testSetValid()
    {
        ToolbarButton button = ToolbarTestUtils.createPluginButton(testDevicePlugin, "test");
        button.setValid(true);
        assertThat(button.isValid()).isEqualTo(true);
    }

    /**
     * Verifies that display text is set correctly
     */
    @Test
    public void setDisplayText()
    {
        ToolbarButton button = ToolbarTestUtils.createPluginButton(testDevicePlugin, "test");
        String customText = "Custom Text";
        button.setDisplayText(customText);
        assertThat(button.getDisplayText()).isEqualTo(customText);
    }

    /**
     * Tests the compareTo function given that the two buttons have different display text values.
     */
    @Test
    public void testCompareTo_withDifferentDisplayText()
    {
        ToolbarButton button1 = ToolbarTestUtils.createPluginButton(testDevicePlugin, "Button 1");
        ToolbarButton button2 = ToolbarTestUtils.createPluginButton(testDevicePlugin, "Button 2");

        assertThat(button1.compareTo(button2)).isEqualTo(-1);
        assertThat(button2.compareTo(button1)).isEqualTo(1);
    }

    /**
     * Tests the compareTo function given that the two buttons have the same display text but different plugins
     */
    @Test
    public void testCompareTo_withDifferentPlugins()
    {
        ToolbarButton button1 = ToolbarTestUtils.createPluginButton(testDevicePlugin, "Button 1");
        ToolbarButton button2 = ToolbarTestUtils.createPluginButton(new TestDevicePlugin2(), "Button 1");

        assertThat(button1.compareTo(button2)).isLessThanOrEqualTo(-1);
        assertThat(button2.compareTo(button1)).isGreaterThanOrEqualTo(1);
    }

    /**
     * Tests the compareTo function given that the two buttons have the same id
     */
    @Test
    public void testCompareTo_withSameId()
    {
        final ButtonType buttonType = ButtonType.NEW_PROJECT;
        ToolbarButton button1 = ToolbarTestUtils.createCoreButton(buttonType, buttonType.requireDisplayText());
        final ToolbarButton button2 = button1.copy();
        button2.setDisplayText("button 2");

        assertThat(button1.compareTo(button2)).isEqualTo(0);
    }

    /**
     * Verifies that createIcon returns an icon given that the icon path is set
     */
    @Test
    public void testCreateIconFromIconName()
    {
        final ButtonType buttonType = ButtonType.PROPERTIES;
        ToolbarButton button = ToolbarTestUtils.createCoreButton(buttonType, buttonType.requireDisplayText());
        assertThat(button.getIcon()).isNotNull();
    }

    /**
     * Test getIdentificationString given that a unique plugin key is set
     */
    @Test
    public void testGetIdentificationString_withNoPluginOrCategory()
    {
        final ButtonType buttonType = ButtonType.PROPERTIES;
        ToolbarButton button = ToolbarTestUtils.createCoreButton(buttonType, buttonType.requireDisplayText());
        assertThat(button.getIdentificationString()).contains(button.getDisplayText());
    }

    /**
     * Test getIdentificationString given that a unique plugin key is set
     */
    @Test
    public void testGetIdentificationString_withPluginKey()
    {
        ToolbarButton button = ToolbarTestUtils.createPluginButton(testDevicePlugin, "Button");
        assertThat(button.getIdentificationString()).contains(button.getDisplayText(), testDevicePlugin.getName());
    }

    /**
     * Test getIdentificationString given that a button type is set and has a category
     */
    @Test
    public void testGetIdentificationString_withCategory()
    {
        ToolbarButton button = ToolbarTestUtils.createCoreButton(ButtonType.CONNECTION_PLUGIN, "My connection");
        assertThat(button.getIdentificationString()).contains(button.getDisplayText(), ButtonType.CONNECTION_PLUGIN.getCategory());
    }

    /**
     * Helper method for comparing two buttons after using the copy constructor to create one from the other.
     *
     * @param newButton      The newly created button
     * @param originalButton The original button which was copied
     */
    private void compareCopyConstructedButtons(ToolbarButton newButton, ToolbarButton originalButton)
    {
        assertThat(newButton).isNotSameAs(originalButton);
        assertThat(newButton.getDisplayText()).isEqualTo(originalButton.getDisplayText());
        assertThat(newButton.getType()).isEqualTo(originalButton.getType());
        assertThat(newButton.getTooltipText()).isEqualTo(originalButton.getTooltipText());
        assertThat(newButton.getActionListeners()).isEqualTo(originalButton.getActionListeners());
        assertThat(newButton.getMouseListeners()).isEqualTo(originalButton.getMouseListeners());
        assertThat(newButton.isEnabled()).isEqualTo(originalButton.isEnabled());
        assertThat(newButton.isValid()).isEqualTo(originalButton.isValid());
        assertThat(newButton.isTogglable()).isEqualTo(originalButton.isTogglable());
        assertThat(newButton.getImage()).isEqualTo(originalButton.getImage());
        assertThat(newButton.getIcon()).isEqualTo(originalButton.getIcon());
        assertThat(newButton.getPluginUniqueKey()).isEqualTo(originalButton.getPluginUniqueKey());
        assertThat(newButton.getToggleButtonGroupName()).isEqualTo(originalButton.getToggleButtonGroupName());
        assertThat(newButton.getTab()).isNull();
        assertThat(newButton.getId()).isEqualTo(originalButton.getId());
    }
}
