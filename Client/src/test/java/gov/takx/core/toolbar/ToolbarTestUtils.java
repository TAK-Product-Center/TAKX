package gov.takx.core.toolbar;

import gov.takx.api.plugin.IPlugin;
import gov.takx.api.toolbar.ButtonType;
import gov.takx.commons.swing.ImageUtils;

/**
 * Utility methods to support testing of the toolbar classes.
 *
 * @since 3.4
 */
public class ToolbarTestUtils
{
    /**
     * Create a plugin button for the provided plugin instance with the specified text, the default image, and a dummy
     * action listener (to satisfy the builder requirement of having at least one action listener).
     * 
     * @param plugin The plugin instance "creating" the button
     * @param displayText The text to display for the button
     * @return The newly created button
     */
    public static ToolbarButton createPluginButton(IPlugin plugin, String displayText)
    {
        return createPluginButtonBuilder(plugin, displayText).build();
    }

    /**
     * Create a plugin button builder for a button from the provided plugin instance with the specified text and the
     * default image.
     *
     * @param plugin The plugin instance "creating" the button
     * @param displayText The text to display for the button
     * @return The newly created button builder (which can be customized further before calling 
     * {@link ToolbarButtonBuilder#build()} to create the button.
     */
    public static ToolbarButtonBuilder createPluginButtonBuilder(IPlugin plugin, String displayText)
    {
        return ToolbarButtonBuilder.createPluginButtonBuilder(plugin, displayText)
                .setImage(ImageUtils.getDefaultImage());
    }

    /**
     * Create a core button of the provided type with the specified text and the default image.
     *
     * @param type The button type
     * @param displayText The text to display for the button
     * @return The newly created button
     */
    public static ToolbarButton createCoreButton(ButtonType type, String displayText)
    {
        return createCoreButtonBuilder(type, displayText).build();
    }

    /**
     * Create a core button builder for a button of the provided type with the specified text and the default image.
     *
     * @param type The button type
     * @param displayText The text to display for the button
     * @return The newly created button builder (which can be customized further before calling 
     * {@link ToolbarButtonBuilder#build()} to create the button.
     */
    public static ToolbarButtonBuilder createCoreButtonBuilder(ButtonType type, String displayText)
    {
        return ToolbarButtonBuilder.createCoreButtonBuilder(type, displayText)
                .setImage(ImageUtils.getDefaultImage());
    }
}
