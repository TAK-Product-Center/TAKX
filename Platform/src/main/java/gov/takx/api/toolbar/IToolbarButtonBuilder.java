package gov.takx.api.toolbar;

import gov.takx.api.annotation.NoPluginImplementations;
import gov.takx.commons.swing.IPopupMenuProvider;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import java.awt.event.ActionListener;
import java.awt.event.MouseListener;
import java.awt.image.BufferedImage;
import java.util.function.Consumer;

/**
 * Builder for creating {@link IToolbarButton} instances. The builder pattern is used since some aspects of a toolbar
 * button can only be set on creation and cannot be modified later, such as whether the button if togglable or the
 * inclusion of a popup menu.
 *
 * @since 3.4
 */
@NoPluginImplementations
public interface IToolbarButtonBuilder
{
    /**
     * Adds an action listener to the button being built. May be called multiple times.
     *
     * @param listener The listener to add
     * @return this builder instance for chaining calls
     */
    @Nonnull
    IToolbarButtonBuilder addActionListener(@Nonnull ActionListener listener);

    /**
     * Adds a mouse listener to the button being built. May be called multiple times.
     *
     * @param listener The listener to add
     * @return this builder instance for chaining calls
     * @since 5.0
     */
    @Nonnull
    IToolbarButtonBuilder addMouseListener(@Nonnull MouseListener listener);

    /**
     * Sets the enabled state of the button being built. The default is {@code true}.
     *
     * @param enabled The enabled state for the button
     * @return this builder instance for chaining calls
     */
    @Nonnull
    IToolbarButtonBuilder setEnabled(boolean enabled);

    /**
     * Sets the image for the button being built.
     * <p/>
     * Note: all buttons are required to have an image.
     *
     * @param image The image for the button
     * @return this builder instance for chaining calls
     */
    @Nonnull
    IToolbarButtonBuilder setImage(@Nonnull BufferedImage image);

    /**
     * Sets the popup menu provider for the drop-down menu activated when the button is clicked. The default is no
     * popup menu.
     *
     * @param popupMenuProvider a provider for the popup menu
     * @return this builder instance for chaining calls
     */
    @Nonnull
    IToolbarButtonBuilder setPopupMenuProvider(@Nullable IPopupMenuProvider popupMenuProvider);

    /**
     * Sets whether the button is togglable. The default is {@code false} unless this is a plugin draw tool button, in
     * which case the default is {@code true}.
     *
     * @param togglable {@code true} if the button is togglable
     * @return this builder instance for chaining calls
     */
    @Nonnull
    IToolbarButtonBuilder setTogglable(boolean togglable);

    /**
     * Sets a listener to be notified when a toggle button is deselected. This field will not be used for buttons that
     * are not togglable.
     * <p/>
     * Note: all plugin draw tool buttons are required to have a toggle deselect listener.
     *
     * @param toggleDeselectListener The listener to be notified when the toggle button is deselected
     * @return this builder instance for chaining calls
     */
    @Nonnull
    IToolbarButtonBuilder setToggleDeselectListener(@Nullable Consumer<IToolbarButton> toggleDeselectListener);

    /**
     * Sets the tooltip text to display for the button on hover.
     *
     * @param tooltipText The tooltip text for the button
     * @return this builder instance for chaining calls
     */
    @Nonnull
    IToolbarButtonBuilder setTooltipText(@Nullable String tooltipText);

    /**
     * @return The newly constructed toolbar button
     * @throws IllegalStateException if a required field is not set: all buttons require an image to be set and plugin
     * draw tool buttons require a deselection callback.
     */
    @Nonnull
    IToolbarButton build();
}
