package gov.takx.api.toolbar;

import gov.takx.api.annotation.NoPluginImplementations;
import gov.takx.api.plugin.IPlugin;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import java.util.List;
import java.util.UUID;

/**
 * Interface representing a tab that can be displayed on the application toolbar. To create a toolbar tab, use
 * {@link IToolbarManager#createTab(IPlugin, String)}.
 *
 * @since 2.16 (renamed and significantly updated in 3.4)
 */
@NoPluginImplementations
public interface IToolbarTab
{
    /**
     * Adds the provided component to this tab. The component must be created using {@link IToolbarButtonBuilder}
     * or {@link IToolbarManager#createSeparator}.
     *
     * @param component The component to add
     * @return {@code true} if the component was added; {@code false} if the component is already in the tab or has been
     * excluded by the user
     * @throws IllegalArgumentException if the component was not created using IToolbarManager or if the component is
     * already associated with a toolbar tab (to avoid this, create a copy of the component using
     * {@link IToolbarComponent#copy()}).
     * @since 3.4
     */
    boolean addComponent(@Nonnull IToolbarComponent component) throws IllegalArgumentException;

    /**
     * Adds the provided component to this tab at the specified index. The component must be created using
     * {@link IToolbarButtonBuilder} or {@link IToolbarManager#createSeparator}.
     *
     * @param component The component to insert
     * @param index The index at which to insert the component
     * @return {@code true} if the component was added; {@code false} if the component is already in the tab or has been
     * excluded by the user
     * @throws IllegalArgumentException if the component was not created using IToolbarManager or if the component is
     * already associated with a toolbar tab (to avoid this, create a copy of the component using
     * {@link IToolbarComponent#copy()}).
     * @since 3.4
     */
    boolean insertComponent(@Nonnull IToolbarComponent component, int index) throws IllegalArgumentException;

    /**
     * Removes the provided component from the tab.
     *
     * @param component The component to remove
     * @throws IllegalArgumentException if the component was not created using IToolbarManager.
     * @since 3.4
     */
    void removeComponent(@Nonnull IToolbarComponent component) throws IllegalArgumentException;

    /**
     * @return The list of components that are in the tab.
     * @since 3.4
     */
    @Nonnull
    List<? extends IToolbarComponent> getComponents();

    /**
     * @return The unique id for the tab.
     * @since 3.4
     */
    @Nonnull
    UUID getId();

    /**
     * @return The unique key of the plugin that created this tab, or {@code null} if created by a user or TAKX.
     * @since 3.4
     */
    @Nullable
    String getPluginUniqueKey();

    /**
     * @return The type of the tab.
     * @since 3.4
     */
    @Nonnull
    TabType getType();

    /**
     * @return True if this tab is visible.
     * @since 3.4
     */
    boolean isVisible();

    /**
     * Updates the visibility of the tab. Note: tabs are visible by default.
     *
     * @param visible Visible state to which to set the tab
     */
    void setVisible(boolean visible);

    /**
     * @return The text to display for the tab.
     * @since 3.4
     */
    @Nonnull
    String getDisplayText();

    /**
     * Sets the display text for the tab.
     *
     * @param displayText The text to display
     * @return this tab instance (to allow for chaining calls)
     * @since 3.4
     */
    @Nonnull
    IToolbarTab setDisplayText(@Nonnull String displayText);
}
