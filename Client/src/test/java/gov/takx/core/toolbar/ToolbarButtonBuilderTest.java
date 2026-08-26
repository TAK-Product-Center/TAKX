package gov.takx.core.toolbar;

import gov.takx.api.toolbar.ButtonType;
import gov.takx.api.toolbar.IToolbarButton;
import gov.takx.commons.swing.IPopupMenuProvider;
import gov.takx.commons.swing.ImageUtils;
import gov.takx.commons.utils.UUIDUtils;
import gov.takx.core.view.gui.menus.DrawToolbarTabWrapper;
import gov.takx.test.mock.TestActionListener;
import gov.takx.test.mock.TestMouseListener;
import gov.takx.test.mock.plugin.device.TestDevicePlugin;
import org.junit.jupiter.api.Test;

import java.awt.image.BufferedImage;
import java.util.UUID;
import java.util.function.Consumer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalStateException;

/**
 * Unit tests for {@link ToolbarButtonBuilder}.
 *
 * @since 3.4
 */
public class ToolbarButtonBuilderTest
{
    private final TestDevicePlugin testDevicePlugin = new TestDevicePlugin();

    @Test
    public void testCreateCoreButtonBuilder_forSingularButton()
    {
        final ToolbarButtonBuilder builder = ToolbarButtonBuilder.createCoreButtonBuilder(ButtonType.ABOUT, "About button");
        assertThat(builder).isNotNull().extracting("id", "type", "displayText", "plugin")
                .containsExactly(ButtonType.ABOUT.requireSingularId(), ButtonType.ABOUT, "About button", null);

        // For core buttons, image should be set automatically from the type
        assertThat(builder).extracting("image").isNotNull();
    }

    @Test
    public void testCreateCoreButtonBuilder_forNonSingularButton()
    {
        // For non-singular buttons, id should be generated from type and display text
        final UUID id = UUIDUtils.generateType3UUID(ButtonType.DEVICE_PLUGIN.name() + ":" + "Device button");
        final ToolbarButtonBuilder builder = ToolbarButtonBuilder.createCoreButtonBuilder(ButtonType.DEVICE_PLUGIN, "Device button");
        assertThat(builder).isNotNull().extracting("id", "type", "displayText", "plugin")
                .containsExactly(id, ButtonType.DEVICE_PLUGIN, "Device button", null);

        // For core buttons, image should be set automatically from the type
        assertThat(builder).extracting("image").isNotNull();
    }

    @Test
    public void testCreatePluginButtonBuilder()
    {
        final UUID id = UUIDUtils.generateType3UUID(testDevicePlugin.getUniqueKey() + ":" + "My button");
        final ToolbarButtonBuilder builder = ToolbarButtonBuilder.createPluginButtonBuilder(testDevicePlugin, "My button");
        assertThat(builder).isNotNull().extracting("id", "type", "displayText", "plugin")
                .containsExactly(id, ButtonType.PLUGIN_CREATED, "My button", testDevicePlugin);
    }

    @Test
    public void testCreatePluginDrawToolButtonBuilder()
    {
        // For plugin draw tools, the type should be PLUGIN_DRAW_TOOL, the button should be togglable, and it should be
        // in the draw group.
        final UUID id = UUIDUtils.generateType3UUID(testDevicePlugin.getUniqueKey() + ":" + "My button");
        final ToolbarButtonBuilder builder = ToolbarButtonBuilder.createPluginDrawToolButtonBuilder(testDevicePlugin, "My button");
        assertThat(builder).isNotNull().extracting("id", "type", "displayText", "plugin", "togglable", "toggleButtonGroupName")
                .containsExactly(id, ButtonType.PLUGIN_DRAW_TOOL, "My button", testDevicePlugin, true, DrawToolbarTabWrapper.DRAW_BUTTON_GROUP_NAME);
    }

    @Test
    public void testBuild_coreButton_withDefaultValues()
    {
        final ToolbarButton button = ToolbarButtonBuilder.createCoreButtonBuilder(ButtonType.APPS, "My button")
                .build();

        // All fields in the created button should be set to default values unless provided
        assertThat(button).isNotNull()
                .hasFieldOrPropertyWithValue("id", ButtonType.APPS.requireSingularId())
                .hasFieldOrPropertyWithValue("pluginName", null)
                .hasFieldOrPropertyWithValue("pluginUniqueKey", null)
                .hasFieldOrPropertyWithValue("type", ButtonType.APPS)
                .hasFieldOrPropertyWithValue("displayText", "My button")
                .satisfies(b -> assertThat(b.getImage()).isNotNull())
                .hasFieldOrPropertyWithValue("togglable", false)
                .hasFieldOrPropertyWithValue("toggleButtonGroupName", null)
                .hasFieldOrPropertyWithValue("toggleDeselectListener", null)
                .hasFieldOrPropertyWithValue("popupMenuProvider", null)
                .hasFieldOrPropertyWithValue("componentDescriptorId", null)
                .hasFieldOrPropertyWithValue("enabled", true)
                .hasFieldOrPropertyWithValue("tooltipText", null)
                .satisfies(b -> assertThat(b.getActionListeners()).isEmpty())
                .satisfies(b -> assertThat(b.getMouseListeners()).isEmpty());
    }

    @Test
    public void testBuild_pluginButton_withDefaultValues()
    {
        final UUID id = UUIDUtils.generateType3UUID(testDevicePlugin.getUniqueKey() + ":" + "My button");
        final BufferedImage image = ImageUtils.getDefaultImage();
        final ToolbarButton button = ToolbarButtonBuilder.createPluginButtonBuilder(testDevicePlugin, "My button")
                .setImage(image)
                .build();

        // All fields in the created button should be set to default values unless provided
        assertThat(button).isNotNull()
                .hasFieldOrPropertyWithValue("id", id)
                .hasFieldOrPropertyWithValue("pluginName", testDevicePlugin.getName())
                .hasFieldOrPropertyWithValue("pluginUniqueKey", testDevicePlugin.getUniqueKey())
                .hasFieldOrPropertyWithValue("type", ButtonType.PLUGIN_CREATED)
                .hasFieldOrPropertyWithValue("displayText", "My button")
                .hasFieldOrPropertyWithValue("image", image)
                .hasFieldOrPropertyWithValue("togglable", false)
                .hasFieldOrPropertyWithValue("toggleButtonGroupName", null)
                .hasFieldOrPropertyWithValue("toggleDeselectListener", null)
                .hasFieldOrPropertyWithValue("popupMenuProvider", null)
                .hasFieldOrPropertyWithValue("componentDescriptorId", null)
                .hasFieldOrPropertyWithValue("enabled", true)
                .hasFieldOrPropertyWithValue("tooltipText", null)
                .satisfies(b -> assertThat(b.getActionListeners()).isEmpty())
                .satisfies(b -> assertThat(b.getMouseListeners()).isEmpty());
    }

    @Test
    public void testBuild_withAllValues()
    {
        final UUID id = UUIDUtils.generateType3UUID(testDevicePlugin.getUniqueKey() + ":" + "My button");
        final TestActionListener actionListener = new TestActionListener();
        final TestMouseListener mouseListener = new TestMouseListener();
        final BufferedImage image = ImageUtils.getDefaultImage();
        final IPopupMenuProvider popupMenuProvider = () -> null;
        final Consumer<IToolbarButton> deselectListener = b -> {
        };
        final ToolbarButton button = ToolbarButtonBuilder.createPluginButtonBuilder(testDevicePlugin, "My button")
                .addActionListener(actionListener)
                .addMouseListener(mouseListener)
                .setComponentDescriptorId("component id")
                .setEnabled(false)
                .setImage(image)
                .setPopupMenuProvider(popupMenuProvider)
                .setTogglable(true)
                .setToggleButtonGroupName("group")
                .setToggleDeselectListener(deselectListener)
                .setTooltipText("tooltip")
                .build();

        // All fields in the created button should be set with the provided values
        assertThat(button).isNotNull()
                .hasFieldOrPropertyWithValue("id", id)
                .hasFieldOrPropertyWithValue("pluginName", testDevicePlugin.getName())
                .hasFieldOrPropertyWithValue("pluginUniqueKey", testDevicePlugin.getUniqueKey())
                .hasFieldOrPropertyWithValue("type", ButtonType.PLUGIN_CREATED)
                .hasFieldOrPropertyWithValue("displayText", "My button")
                .hasFieldOrPropertyWithValue("image", image)
                .hasFieldOrPropertyWithValue("togglable", true)
                .hasFieldOrPropertyWithValue("toggleButtonGroupName", "group")
                .hasFieldOrPropertyWithValue("toggleDeselectListener", deselectListener)
                .hasFieldOrPropertyWithValue("popupMenuProvider", popupMenuProvider)
                .hasFieldOrPropertyWithValue("componentDescriptorId", "component id")
                .hasFieldOrPropertyWithValue("enabled", false)
                .hasFieldOrPropertyWithValue("tooltipText", "tooltip")
                .satisfies(b -> assertThat(b.getActionListeners()).containsExactly(actionListener))
                .satisfies(b -> assertThat(b.getMouseListeners()).containsExactly(mouseListener));
    }

    @Test
    public void testBuild_withoutImage()
    {
        assertThatIllegalStateException().isThrownBy(
                () -> ToolbarButtonBuilder.createPluginButtonBuilder(testDevicePlugin, "My button").build());
    }

    @Test
    public void testBuild_pluginDrawToolButton_withoutDeselectListener()
    {
        assertThatIllegalStateException().isThrownBy(
                () -> ToolbarButtonBuilder.createPluginDrawToolButtonBuilder(testDevicePlugin, "My button")
                        .setImage(ImageUtils.getDefaultImage())
                        .build());
    }
}