package gov.takx.core.toolbar;

import gov.takx.api.plugin.IPlugin;
import gov.takx.api.toolbar.ButtonType;
import gov.takx.api.toolbar.IToolbarButton;
import gov.takx.api.toolbar.IToolbarButtonBuilder;
import gov.takx.api.toolbar.TabType;
import gov.takx.commons.swing.IPopupMenuProvider;
import gov.takx.commons.utils.UUIDUtils;
import gov.takx.core.view.gui.menus.DrawToolbarTabWrapper;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import java.awt.event.ActionListener;
import java.awt.event.MouseListener;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.function.Consumer;

/**
 * Builder for creating {@link IToolbarButton} instances.
 *
 * @since 3.4
 */
public class ToolbarButtonBuilder implements IToolbarButtonBuilder
{
    private final UUID id;
    private final ButtonType type;
    private final String displayText;
    private final IPlugin plugin;
    private final List<ActionListener> actionListeners = new ArrayList<>();
    private final List<MouseListener> mouseListeners = new ArrayList<>();
    private String componentDescriptorId;
    private boolean enabled = true;
    private BufferedImage image;
    private IPopupMenuProvider popupMenuProvider;
    private boolean togglable = false;
    private String toggleButtonGroupName;
    private Consumer<IToolbarButton> toggleDeselectListener;
    private String tooltipText;

    /**
     * Creates a builder for a core (i.e. non-plugin) button with the provided type and display text. The image is
     * defaulted to the value provided by the type.
     *
     * @param type The button type
     * @param displayText The text to display for the button
     * @return The button builder.
     */
    public static ToolbarButtonBuilder createCoreButtonBuilder(@Nonnull ButtonType type, @Nonnull String displayText)
    {
        return new ToolbarButtonBuilder(type, displayText)
                .setImage(type.getImage());
    }

    /**
     * Creates a builder for a {@link ButtonType#PLUGIN_CREATED} button with the provided display text.
     *
     * @param plugin Plugin creating the button
     * @param displayText The text to display for the button
     * @return The button builder.
     */
    public static ToolbarButtonBuilder createPluginButtonBuilder(@Nonnull IPlugin plugin, @Nonnull String displayText)
    {
        return new ToolbarButtonBuilder(ButtonType.PLUGIN_CREATED, displayText, plugin);
    }

    /**
     * Creates a builder for a {@link ButtonType#PLUGIN_DRAW_TOOL} button with the provided display text. The button is
     * set to togglable and added to the appropriate toggle button group for drawing tools.
     * <p/>
     * This method is necessary so custom draw tool buttons will have the correct {@link ButtonType} to be added
     * to the {@link TabType#DRAW} tab automatically. No other plugin-created buttons have this behavior.
     *
     * @param plugin Plugin creating the button
     * @param displayText The display text for the button
     * @return The button builder.
     */
    public static ToolbarButtonBuilder createPluginDrawToolButtonBuilder(@Nonnull IPlugin plugin, @Nonnull String displayText)
    {
        return new ToolbarButtonBuilder(ButtonType.PLUGIN_DRAW_TOOL, displayText, plugin)
                .setTogglable(true)
                .setToggleButtonGroupName(DrawToolbarTabWrapper.DRAW_BUTTON_GROUP_NAME);
    }

    /**
     * Constructor for a core button builder. For singular button types, the singular id is used as the button id. For
     * non-singular buttons, the id is determined from the type and the provided display text so it will be the same
     * across application restarts.
     *
     * @param type The type for the button
     * @param displayText The text to display for the button
     */
    private ToolbarButtonBuilder(@Nonnull ButtonType type, @Nonnull String displayText)
    {
        id = type.isSingular() ? type.requireSingularId() : UUIDUtils.generateType3UUID(type.name() + ":" + displayText);
        this.type = type;
        this.displayText = displayText;
        plugin = null;
    }

    /**
     * Constructor for a plugin button builder. The button id is generated from the plugin's unique key and the provided
     * display text so it will be the same across application restarts.
     *
     * @param type The type for the button
     * @param displayText The display text for the button
     * @param plugin Plugin creating the button
     */
    private ToolbarButtonBuilder(@Nonnull ButtonType type, @Nonnull String displayText, @Nonnull IPlugin plugin)
    {
        id = UUIDUtils.generateType3UUID(plugin.getUniqueKey() + ":" + displayText);
        this.type = type;
        this.displayText = displayText;
        this.plugin = plugin;
    }

    @Override
    @Nonnull
    public ToolbarButtonBuilder addActionListener(@Nonnull ActionListener listener)
    {
        actionListeners.add(listener);
        return this;
    }

    @Nonnull
    @Override
    public ToolbarButtonBuilder addMouseListener(@Nonnull MouseListener listener)
    {
        mouseListeners.add(listener);
        return this;
    }

    @Nonnull
    public ToolbarButtonBuilder setComponentDescriptorId(@Nullable String componentDescriptorId)
    {
        this.componentDescriptorId = componentDescriptorId;
        return this;
    }

    @Override
    @Nonnull
    public ToolbarButtonBuilder setEnabled(boolean enabled)
    {
        this.enabled = enabled;
        return this;
    }

    @Override
    @Nonnull
    public ToolbarButtonBuilder setImage(@Nonnull BufferedImage image)
    {
        this.image = image;
        return this;
    }

    @Override
    @Nonnull
    public ToolbarButtonBuilder setPopupMenuProvider(@Nullable IPopupMenuProvider popupMenuProvider)
    {
        this.popupMenuProvider = popupMenuProvider;
        return this;
    }

    @Override
    @Nonnull
    public ToolbarButtonBuilder setTogglable(boolean togglable)
    {
        this.togglable = togglable;
        return this;
    }

    @Nonnull
    public ToolbarButtonBuilder setToggleButtonGroupName(@Nullable String toggleButtonGroupName)
    {
        this.toggleButtonGroupName = toggleButtonGroupName;
        return this;
    }

    @Override
    @Nonnull
    public ToolbarButtonBuilder setToggleDeselectListener(@Nullable Consumer<IToolbarButton> toggleDeselectListener)
    {
        this.toggleDeselectListener = toggleDeselectListener;
        return this;
    }

    @Override
    @Nonnull
    public ToolbarButtonBuilder setTooltipText(@Nullable String tooltipText)
    {
        this.tooltipText = tooltipText;
        return this;
    }

    @Override
    @Nonnull
    public ToolbarButton build()
    {
        if (image == null) throw new IllegalStateException("The image field must be set for all buttons.");

        if (type == ButtonType.PLUGIN_DRAW_TOOL && toggleDeselectListener == null)
        {
            throw new IllegalStateException("The toggleDeselectListener field must be set for plugin draw tool buttons.");
        }

        return new ToolbarButton(type, id, displayText, image, actionListeners, mouseListeners, togglable,
                toggleButtonGroupName, toggleDeselectListener, popupMenuProvider, plugin)
                .setComponentDescriptorId(componentDescriptorId)
                .setEnabled(enabled)
                .setTooltipText(tooltipText);
    }
}
