package gov.takx.api.toolbar;

import gov.takx.api.annotation.NoPluginImplementations;
import gov.takx.commons.swing.IPopupMenuProvider;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import java.awt.event.ActionListener;
import java.awt.event.MouseListener;
import java.awt.image.BufferedImage;

/**
 * Interface representing a button that can be added to an {@link IToolbarTab} for display on the application
 * toolbar. To create a toolbar button, use {@link IToolbarButtonBuilder}. An instance of the builder can be obtained by
 * calling {@link IToolbarManager#createButtonBuilder} (or {@link IToolbarManager#createDrawingToolButtonBuilder} for a 
 * button to access a drawing tool).
 *
 * @since 3.4
 */
@NoPluginImplementations
public interface IToolbarButton extends IToolbarComponent
{
    /**
     * @return The type of the button.
     */
    @Nonnull
    ButtonType getType();

    /**
     * Adds an action listener to be notified of a button click.
     *
     * @param actionListener The listener to add
     * @return this button instance (to allow for chaining calls)
     */
    @Nonnull
    IToolbarButton addActionListener(@Nonnull ActionListener actionListener);

    /**
     * Removes an action listener from the button.
     *
     * @param actionListener The listener to remove
     * @return this button instance (to allow for chaining calls)
     */
    @Nonnull
    IToolbarButton removeActionListener(@Nonnull ActionListener actionListener);

    /**
     * Adds a mouse listener to be notified of a mouse events.
     *
     * @param mouseListener The listener to add
     * @return this button instance (to allow for chaining calls)
     * @since 5.0
     */
    @Nonnull
    IToolbarButton addMouseListener(@Nonnull MouseListener mouseListener);

    /**
     * Removes a mouse listener from the button.
     *
     * @param mouseListener The listener to remove
     * @return this button instance (to allow for chaining calls)
     * @since 5.0
     */
    @Nonnull
    IToolbarButton removeMouseListener(@Nonnull MouseListener mouseListener);

    /**
     * @return The text to display for the button.
     */
    @Nonnull
    String getDisplayText();

    /**
     * Sets the display text for the button.
     *
     * @param displayText The text to display
     * @return this button instance (to allow for chaining calls)
     */
    @Nonnull
    IToolbarButton setDisplayText(@Nonnull String displayText);

    /**
     * @return The tooltip text for the button.
     */
    @Nullable
    String getTooltipText();

    /**
     * Sets the tooltip text for the button.
     *
     * @param tooltipText The tooltip text to display
     * @return this button instance (to allow for chaining calls)
     */
    @Nonnull
    IToolbarButton setTooltipText(@Nullable String tooltipText);

    /**
     * @return The image to display as the button's icon.
     */
    @Nullable
    BufferedImage getImage();

    /**
     * Sets the image to display on the toolbar for the button (and updates the sized icon created from the image).
     *
     * @param image The image for the button
     * @return this button instance (to allow for chaining calls)
     */
    @Nonnull
    IToolbarButton setImage(@Nonnull BufferedImage image);

    /**
     * @return Whether the button is enabled.
     */
    boolean isEnabled();

    /**
     * Sets the enabled state of the button.
     *
     * @param enabled Whether the button should be enabled
     * @return this button instance (to allow for chaining calls)
     */
    @Nonnull
    IToolbarButton setEnabled(boolean enabled);

    /**
     * @return {@code true} if this is a toggle button.
     */
    boolean isTogglable();

    /**
     * @return The popup menu provider for the drop-down menu or {@code null} if this is not a drop-down button.
     */
    @Nullable
    IPopupMenuProvider getPopupMenuProvider();
}
