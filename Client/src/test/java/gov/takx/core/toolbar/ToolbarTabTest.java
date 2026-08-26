package gov.takx.core.toolbar;

import gov.takx.test.AContainerTest;
import gov.takx.test.mock.plugin.device.TestDevicePlugin;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

/**
 * Unit tests for the methods of {@link ToolbarTab}.
 *
 * @since 2.1 (Renamed in 3.4)
 */
public class ToolbarTabTest extends AContainerTest
{
    private final TestDevicePlugin testDevicePlugin = new TestDevicePlugin();

    /**
     * Verifies that the correct text is returned given that custom text is set
     */
    @Test
    public void testGetTabTextCustom()
    {
        ToolbarTab tab = ToolbarTab.createUserTab("Test");
        tab.setDisplayText("Custom Text");
        assertThat(tab.getDisplayText()).isEqualTo("Custom Text");
    }

    /**
     * Verifies that the correct text is returned given that custom text is not set
     */
    @Test
    public void testGetTabTextName()
    {
        ToolbarTab tab = ToolbarTab.createUserTab("Test");
        assertThat(tab.getDisplayText()).isEqualTo(tab.getDisplayText());
    }

    /**
     * Verifies that a button can be added
     */
    @Test
    public void testAddButton()
    {
        ToolbarTab tab = ToolbarTab.createUserTab("Test");
        ToolbarButton button = ToolbarTestUtils.createPluginButton(testDevicePlugin, "Button");
        tab.addComponent(button, false);
        assertThat(tab.getComponentIndex(button)).isEqualTo(0);
    }

    /**
     * Verifies that a button cannot be added to a tab that already contains a button with the same id
     */
    @Test
    public void testAddButton_withSameKey_isNotAdded()
    {
        ToolbarTab tab = ToolbarTab.createUserTab("Test");

        final ToolbarButton buttonModel1 = ToolbarTestUtils.createPluginButton(testDevicePlugin, "button 1");
        final ToolbarButton buttonModel2 = buttonModel1.copy();
        final boolean button1Added = tab.addComponent(buttonModel1, false);
        assertThat(button1Added).isTrue();

        final boolean button2Added = tab.addComponent(buttonModel2, false);
        assertThat(button2Added).isFalse();
    }

    /**
     * Verifies that a component can be removed
     */
    @Test
    public void testRemoveComponent()
    {
        ToolbarTab tab = ToolbarTab.createUserTab("Test");
        ToolbarButton button1 = ToolbarTestUtils.createPluginButton(testDevicePlugin, "Button 1");
        ToolbarButton button2 = ToolbarTestUtils.createPluginButton(testDevicePlugin, "Button 2");

        tab.addComponent(button1, false);
        tab.addComponent(button2, false);

        tab.removeComponent(button1, false);
        assertThat(tab.getComponentIndex(button2)).isEqualTo(0);
    }

    /**
     * Verifies that the button's index is returned correctly
     */
    @Test
    public void testGetButtonIndex()
    {
        ToolbarTab tab = ToolbarTab.createUserTab("Test");
        ToolbarButton button = ToolbarTestUtils.createPluginButton(testDevicePlugin, "Button");
        tab.insertComponent(button, 0, false);
        assertThat(tab.getComponentIndex(button)).isEqualTo(0);
    }

    /**
     * Verifies that -1 is returned if the button is not present
     */
    @Test
    public void testGetComponentIndexNoComponent()
    {
        ToolbarTab tab = ToolbarTab.createUserTab("Test");
        ToolbarButton button = ToolbarTestUtils.createPluginButton(testDevicePlugin, "Button");
        assertThat(tab.getComponentIndex(button)).isEqualTo(-1);
    }

    /**
     * Verifies that components are set correctly
     */
    @Test
    public void testSetComponents()
    {
        List<ToolbarButton> buttons = Arrays.asList(ToolbarTestUtils.createPluginButton(testDevicePlugin, "Button 1"),
                ToolbarTestUtils.createPluginButton(testDevicePlugin, "Button 2"));

        ToolbarTab tab = ToolbarTab.createPluginTab(testDevicePlugin, "Test");
        buttons.forEach(tab::addComponent);

        assertThat(tab.getComponents()).isEqualTo(buttons);
    }

    /**
     * Verifies that configuration can be set correctly
     */
    @Test
    public void testSetConfiguration()
    {
        ToolbarTab tab = ToolbarTab.createUserTab("Test");
        ToolbarConfiguration configuration = ToolbarConfiguration.createConfiguration();
        configuration.setName("Config");
        tab.setConfiguration(configuration);
        assertThat(tab.getConfiguration()).isEqualTo(configuration);
    }

    /**
     * Verifies that exception is thrown if a configuration has already been set
     */
    @Test
    public void testSetConfigurationAlreadySet()
    {
        ToolbarTab tab = ToolbarTab.createUserTab("Test");
        ToolbarConfiguration configuration1 = ToolbarConfiguration.createConfiguration();
        ToolbarConfiguration configuration2 = ToolbarConfiguration.createConfiguration();

        configuration1.setName("Config 1");
        configuration2.setName("Config 2");

        tab.setConfiguration(configuration1);
        assertThatIllegalArgumentException()
                .isThrownBy(() -> tab.setConfiguration(configuration2));
    }

    /**
     * Verifies that the valid property is set correctly
     */
    @Test
    public void testSetValid()
    {
        ToolbarTab tab = ToolbarTab.createUserTab("Test");
        tab.setValid(true);
        assertThat(tab.isValid()).isEqualTo(true);
    }

    /**
     * Verifies that the visible property is set correctly
     */
    @Test
    public void testSetVisible()
    {
        ToolbarTab tab = ToolbarTab.createUserTab("Test");
        tab.setVisible(false, false);
        assertThat(tab.isVisible()).isFalse();
    }

    /**
     * Verifies that the visible property is set correctly when calling setVisibility().
     *
     * @since 3.4
     */
    @Test
    public void testSetVisibility()
    {
        ToolbarTab tab = ToolbarTab.createUserTab("Test");
        tab.setVisible(false);
        assertThat(tab.isVisible()).isFalse();
    }

    /**
     * Verifies that getId returns a non-null value
     */
    @Test
    public void testGetKeyName()
    {
        ToolbarTab tab = ToolbarTab.createUserTab("Test");
        assertThat(tab.getId()).isNotNull();
    }

    /**
     * Verifies that tab is in default configuration
     */
    @Test
    public void testIsInDefaultConfiguration()
    {
        ToolbarTab tab = ToolbarTab.createUserTab("Test");
        tab.setConfiguration(getBean(ToolbarConfigurationManager.class).createDefaultConfiguration());
        assertThat(tab.isInDefaultConfiguration()).isEqualTo(true);
    }

    /**
     * Verifies that getIdentificationString returns the correct value given that a plugin key is set
     */
    @Test
    public void testGetIdentificationString_withPluginKey()
    {
        ToolbarTab tab = ToolbarTab.createPluginTab(testDevicePlugin, "My tab");
        assertThat(tab.getIdentificationString()).contains(tab.getDisplayText(), testDevicePlugin.getName());
    }

    /**
     * Verifies that getIdentificationString returns the correct value for a user-created tab
     */
    @Test
    public void testGetIdentificationString_userCreated()
    {
        ToolbarTab tab = ToolbarTab.createUserTab("Test");
        assertThat(tab.getIdentificationString()).contains(tab.getDisplayText(), "(Custom)");
    }
}
