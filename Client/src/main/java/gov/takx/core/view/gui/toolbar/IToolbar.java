package gov.takx.core.view.gui.toolbar;

import gov.takx.api.toolbar.IToolbarButton;
import gov.takx.core.toolbar.AToolbarComponent;
import gov.takx.core.toolbar.ToolbarButton;
import gov.takx.core.toolbar.ToolbarTab;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import java.awt.Point;
import java.util.UUID;

/**
 * Interface describing the functionality of the toolbar. The toolbar is composed to tabs, buttons, and separators.
 *
 * @since 3.3
 */
public interface IToolbar
{
    /**
     * Sets whether the toolbar should be minimized (showing only the tabs and no toolbar components).
     *
     * @param minimized True if the toolbar should be minimized
     */
    void setMinimized(boolean minimized);

    /**
     * Clears all tabs/components from the toolbar.
     */
    void clear();

    /**
     * Adds a tab at the end of the toolbar.
     *
     * @param tabModel The model representing the tab to add
     * @return True if the tab was added or false if the tab not added (e.g., due to being invalid or invisible)
     * @since 3.4
     */
    boolean addTab(@Nonnull ToolbarTab tabModel);

    /**
     * Adds a tab to the toolbar at the specified index.
     *
     * @param tabModel The model representing the tab to add
     * @param tabIndex The index at which to add the tab
     * @return True if the tab was added or false if the tab not added (e.g., due to being invalid or invisible)
     * @since 3.4
     */
    boolean insertTab(@Nonnull ToolbarTab tabModel, int tabIndex);

    /**
     * Removes the provided tab from the toolbar.
     *
     * @param tabModel The model representing the tab to remove
     */
    void removeTab(@Nonnull ToolbarTab tabModel);

    /**
     * @return The id for the currently selected tab (or {@code null} if none selected).
     * @since 3.4
     */
    @Nullable
    UUID getSelectedTabId();

    /**
     * Selects the tab with the provided id.
     *
     * @param tabId The id of the tab to select.
     * @return {@code true} if the tab was selected; {@code false} if no tab was found with the provided id.
     * @since 3.4 (parameter changed to UUID)
     */
    boolean selectTab(@Nonnull UUID tabId);

    /**
     * Updates the display text for the tab that corresponds to the provided model.
     *
     * @param tabModel The model representing the tab to update
     * @since 3.4
     */
    void updateTabText(@Nonnull ToolbarTab tabModel);

    /**
     * Adds a component at the end of a toolbar tab, assuming the tab model is valid and visible.
     *
     * @param componentModel The model representing the component to add
     * @param tabModel The model of the tab to which to add the component
     * @throws IllegalArgumentException if the component is not one of the supported types or if no tab exists on the
     * toolbar for the tab model associated with the component
     * @since 3.4
     */
    void addComponent(@Nonnull AToolbarComponent componentModel, @Nonnull ToolbarTab tabModel);

    /**
     * Adds a component to a toolbar tab at the specified index, assuming the tab model is valid and visible.
     *
     * @param componentModel The model representing the component to add
     * @param tabModel The model of the tab to which to add the component
     * @param componentIndex The index at which to add the component within its tab
     * @throws IllegalArgumentException if the component is not one of the supported types or if no tab exists on the
     * toolbar for the tab model associated with the component
     * @since 3.4
     */
    void insertComponent(@Nonnull AToolbarComponent componentModel, @Nonnull ToolbarTab tabModel, int componentIndex);

    /**
     * Removes a component from the specified tab, assuming the tab model is valid and visible.
     *
     * @param componentModel The model representing the component to remove
     * @param tabModel The model representing the tab from which to remove the component
     * @throws IllegalArgumentException if no tab exists on the toolbar for the provided model
     * @since 3.4
     */
    void removeComponent(@Nonnull AToolbarComponent componentModel, @Nonnull ToolbarTab tabModel);

    /**
     * Updates the attributes of all buttons with the same id as the model with the attributes from the model, including
     * icon, text, state, and tooltip.
     *
     * @param buttonModel The model representing the button
     */
    void updateButton(@Nonnull ToolbarButton buttonModel);

    /**
     * @param groupName The name of the button group
     * @return True if any button in the named group is selected.
     * @throws IllegalArgumentException if no group exists with the provided name.
     * @since 3.4
     */
    boolean isAnyButtonSelectedInGroup(@Nonnull String groupName);

    /**
     * Selects all buttons with the specified id in the named group (or deselects all if id is {@code null}).
     *
     * @param buttonId The id of the button to select (or {@code null} to deselect all)
     * @param groupName The name of the button group
     */
    void setSelectedButtonInGroup(@Nullable UUID buttonId, @Nonnull String groupName);

    /**
     * Gets the selected state for the toggle button with the specified id.
     *
     * @param buttonId The key of the button for which to get the toggle state
     * @since 3.4
     */
    boolean getToggleButtonState(@Nonnull UUID buttonId);

    /**
     * Sets the selected state for all toggle buttons with the specified id.
     *
     * @param buttonId The key of the button to update
     * @param selected The new selected state for the button.
     */
    void setToggleButtonState(@Nonnull UUID buttonId, boolean selected);

    /**
     * Programmatically "clicks" the specified button.
     *
     * @param buttonId The id of the button to click
     */
    void doButtonClick(@Nonnull UUID buttonId);

    /**
     * Determines if the provided point is on the specified button.
     *
     * @param buttonModel The model representing the button
     * @param point The point to check
     * @return True if the point is within the bounds of the button.
     */
    boolean isPointOnButton(@Nonnull IToolbarButton buttonModel, @Nonnull Point point);
}
