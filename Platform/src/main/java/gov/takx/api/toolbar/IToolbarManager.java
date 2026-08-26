package gov.takx.api.toolbar;

import gov.takx.api.annotation.InjectableService;
import gov.takx.api.annotation.NoPluginImplementations;
import gov.takx.api.plugin.IPlugin;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * Supports customization of the application toolbar, including the creation of plugin-defined toolbar tabs, buttons, 
 * and separators.
 * <p>
 * The general flow for adding a custom tab for a plugin is:
 * <ol>
 *     <li>Create a {@link IToolbarButtonBuilder} by calling {@link #createButtonBuilder(IPlugin, String)} and use the 
 *     builder to create a button with the desired characteristics (e.g. togglable).</li>
 *     <li>Retrieve any desired core buttons using {@link #getSingularButton(ButtonType)} or {@link #getButtons(ButtonType)}.</li>
 *     <li>Create separators as desired using {@link #createSeparator(IPlugin, String)}.</li>
 *     <li>Create a new custom tab using {@link #createTab(IPlugin, String)}.</li>
 *     <li>Add the buttons and separators to the returned tab using {@link IToolbarTab#addComponent(IToolbarComponent)}.</li>
 *     <li>Add the populated tab to the toolbar using {@link #addTab(IToolbarTab)}.</li>
 * </ol>
 * <p/>
 * In addition to creating new custom tabs, plugins may also modify the existing core tabs:
 * <ol>
 *     <li>Retrieve the desired core tab using {@link #getSingularTab(TabType)}.</li>
 *     <li>Use the methods available on {@link IToolbarTab} to modify the tab as desired, including adding/removing
 *     components, setting visibility, and changing the display text.</li>
 * </ol>
 * <p/>
 * The workflow for drawing tool buttons is a little different due to the extra interaction that is necessary with the 
 * other drawing tool buttons on the toolbar:
 * <ol>
 *     <li>Create a {@link IToolbarButtonBuilder} by calling {@link #createDrawingToolButtonBuilder(IPlugin, String)}
 *     and use the builder to create a button with the desired characteristics.</li>
 *     <li>Add the drawing tool button to the toolbar using {@link #addDrawingToolButton(IToolbarButton)}. The button 
 *     will automatically be added to the {@link TabType#DRAW tab}.</li>
 *     <li>If desired, add the button to additional toolbar tabs as described above.</li>
 * </ol>
 * @since 1.14 (renamed and significantly updated in 3.4)
 */
@InjectableService
@NoPluginImplementations
public interface IToolbarManager
{
    /**
     * Creates a new {@link IToolbarButtonBuilder} to be used to define and create a toolbar button for a plugin.
     *
     * @param plugin Plugin creating the button
     * @param displayText The text to display for the button
     * @return The toolbar button builder for a plugin button.
     * @see IToolbarTab#addComponent(IToolbarComponent) 
     * @since 3.4
     */
    @Nonnull
    IToolbarButtonBuilder createButtonBuilder(@Nonnull IPlugin plugin, @Nonnull String displayText);

    /**
     * Creates a new {@link IToolbarButtonBuilder} to be used to define and create a button for controlling a drawing
     * tool provided by a plugin.
     *
     * @param plugin Plugin creating the button
     * @param displayText The text to display for the button
     * @return The toolbar button builder for a drawing tool button.
     * @see IToolbarTab#addComponent(IToolbarComponent)
     * @since 3.4
     */
    @Nonnull
    IToolbarButtonBuilder createDrawingToolButtonBuilder(@Nonnull IPlugin plugin, @Nonnull String displayText);

    /**
     * Adds the provided button to the {@link TabType#DRAW} tab. (Plugins may add the button to any other tabs as
     * desired.) This button will be a toggle button that is part of the drawing tools button group so that only one
     * drawing tool is enabled at a time.
     *
     * @param button The drawing tool button to add
     * @throws IllegalArgumentException if the button wasn't created using {@link IToolbarButtonBuilder} or if this is
     * not a drawing tool button.
     * @since 3.4
     */
    void addDrawingToolButton(@Nonnull IToolbarButton button) throws IllegalArgumentException;

    /**
     * Creates a separator that can be added to a toolbar tab.
     *
     * @param plugin Plugin creating the separator
     * @param uniqueName A unique name for the separator, such as the display text of the parent tab plus the
     *                   separator's initial index within the tab, e.g. "Tools:1"
     * @return The toolbar separator.
     * @see IToolbarTab#addComponent(IToolbarComponent)
     * @since 5.2 (modified to add unique name param, which is necessary to create a reproducible unique id)
     */
    @Nonnull
    IToolbarSeparator createSeparator(@Nonnull IPlugin plugin, @Nonnull String uniqueName);

    /**
     * Defines an empty toolbar tab with the assigned display text.
     *
     * @param plugin Plugin creating the tab
     * @param displayText The text to display for the tab
     * @return Toolbar tab that can be added to the toolbar using {@link #addTab(IToolbarTab)}.
     * @see #addTab(IToolbarTab) 
     * @since 2.16
     */
    @Nonnull
    IToolbarTab createTab(@Nonnull IPlugin plugin, @Nonnull String displayText);

    /**
     * Adds the provided tab to the toolbar.
     *
     * @param tab The tab to add
     * @throws IllegalArgumentException if the tab was not created using {@link IToolbarManager} or if the tab has a
     * singular type, since those tabs have already been added by the core and cannot be added again.
     * @since 2.16
     */
    void addTab(@Nonnull IToolbarTab tab) throws IllegalArgumentException;

    /**
     * Adds the provided tab to the toolbar at the specified index.
     *
     * @param tab The tab to insert
     * @param index The index at which to insert the tab
     * @throws IllegalArgumentException if the tab was not created using {@link IToolbarManager}.
     * @since 3.4
     */
    void insertTab(@Nonnull IToolbarTab tab, int index) throws IllegalArgumentException;

    /**
     * Removes the provided tab from the toolbar.
     *
     * @param tab The tab to remove
     * @throws IllegalArgumentException if the tab was not created using {@link IToolbarManager} or if the tab has a 
     * singular type, since those cannot be removed, only hidden.
     * @since 3.4
     */
    void removeTab(@Nonnull IToolbarTab tab) throws IllegalArgumentException;

    /**
     * @return The id for the currently selected tab (or {@code null} if none selected).
     * @since 3.4
     */
    @Nullable
    UUID getSelectedTabId();

    /**
    * Selects a tab with a singular type. This can be used for all tab types except {@link TabType#PLUGIN_CREATED} and
    * {@link TabType#USER_CREATED}. This is a convenience method equivalent to calling {@link #selectTab(UUID)} with the
    * id returned by calling {@link TabType#getSingularId()} on a singular tab type.
    *
    * @param tabType The tab type to select
    * @return {@code true} if the tab was selected, {@code false} if no tab was present on the toolbar with the
    * provided id (likely because it was set to not visible).
    * @throws IllegalArgumentException if the provided tab type is not singular (see {@link TabType#isSingular()}) or if
    * no tab exists with the provided id.
    * @since 3.4
    */
    boolean selectSingularTab(@Nonnull TabType tabType) throws IllegalArgumentException;

    /**
     * Selects a tab with the provided id. This can be used for all tabs and is the only way to select tabs created by
     * plugins or the user.
     *
     * @param tabId The id of the tab to select
     * @throws IllegalArgumentException if no tab exists with the provided id.
     * @return {@code true} if the tab was selected, {@code false} if no tab was present on the toolbar with the 
     * provided id (likely because it was set to not visible).
     * @see IToolbarTab#getId()
     * @see TabType#getSingularId()
     * @since 3.4
     */
    boolean selectTab(@Nonnull UUID tabId) throws IllegalArgumentException;

    /**
     * Programmatically "clicks" the button with the provided id.
     *
     * @param buttonId The id of the button to click
     * @throws IllegalArgumentException if no button exists with the provided id.
     * @since 3.4
     */
    void clickButton(@Nonnull UUID buttonId) throws IllegalArgumentException;

    /**
     * Gets the toggle state of the toggle button with the provided id.
     *
     * @param buttonId The id of the toggle button
     * @return the current toggle state of the button.
     * @throws IllegalArgumentException if no button exists with the provided id or if the button with the provided id
     * is not a toggle button.
     * @since 3.4
     */
    boolean getToggleButtonState(@Nonnull UUID buttonId) throws IllegalArgumentException;

    /**
     * Sets the toggle state of the toggle button with the provided id.
     * <p/>
     * Note: this method does not perform an action event on the button; it simply changes the toggled state. To perform
     * an action, call {@link #clickButton(UUID)}.
     *
     * @param buttonId The id of the toggle button
     * @param selected {@code true} to select the toggle button, {@code false} to deselect it
     * @throws IllegalArgumentException if no button exists with the provided id or if the button with the provided id
     * is not a toggle button.
     * @since 3.4
     */
    void setToggleButtonState(@Nonnull UUID buttonId, boolean selected) throws IllegalArgumentException;

    /**
     * Gets the core tab with the provided singular type. This is a convenience method equivalent to calling
     * {@link #getTab(UUID)} with the id returned by calling {@link TabType#getSingularId()} on a singular tab type.
     * 
     * @param tabType The type of tab to get 
     * @return The toolbar tab with the provided singular type (may be {@code null} if running headless or under test).
     * @throws IllegalArgumentException If the provided tab type is not singular.
     * @see TabType#isSingular() 
     * @since 3.4
     */
    @Nullable
    IToolbarTab getSingularTab(@Nonnull TabType tabType) throws IllegalArgumentException;

    /**
     * Gets the tab with the provided id.
     * 
     * @param tabId The id of the tab to retrieve
     * @return The tab with the provided id or {@code null} if no tab with that id exists.
     * @see TabType#getSingularId()
     * @since 3.4
     */
    @Nullable
    IToolbarTab getTab(@Nonnull UUID tabId);

    /**
     * Gets all tabs with the specified type. This can be used for non-singular tab types like
     * {@link TabType#PLUGIN_CREATED} or {@link TabType#USER_CREATED} in order to access the tabs of that type.
     * 
     * @param tabType The type of tab to get 
     * @return List of tabs with the specified type.
     * @since 3.4
     */
    @Nonnull
    List<? extends IToolbarTab> getTabs(@Nonnull TabType tabType);

    /**
     * Gets all tabs that are part of the toolbar and able to be configured by plugins. This returns all
     * programmatically-created tabs, whether created by the core or plugins. It does not include any tabs created by
     * the user.
     *
     * @return List of all programmatically-created tabs.
     * @since 3.4
     */
    @Nonnull
    List<? extends IToolbarTab> getTabs();

    /**
     * Gets all tabs that are part of the active toolbar configuration, whether created by the core, plugins or
     * the user.
     *
     * @return List of all tabs associated with the active toolbar configuration
     * @since 5.1
     */
    @Nonnull
    List<? extends IToolbarTab> getActiveTabs();

    /**
     * Gets the core button with the provided singular type. This is a convenience method equivalent to calling
     * {@link #getButton(UUID)} with the id returned by calling {@link ButtonType#getSingularId()} on a singular button
     * type.
     *
     * @param buttonType The type of button to get
     * @return The toolbar button with the provided singular type (may be {@code null} if running headless or under test).
     * @throws IllegalArgumentException If the provided button type is not singular.
     * @see ButtonType#isSingular()
     * @since 3.4
     */
    @Nullable
    IToolbarButton getSingularButton(@Nonnull ButtonType buttonType) throws IllegalArgumentException;

    /**
     * Gets the button with the provided id.
     *
     * @param buttonId The id of the button to retrieve
     * @return The button with the provided id or {@code null} if no button with that id exists.
     * @see ButtonType#getSingularId()
     * @since 3.4
     */
    @Nullable
    IToolbarButton getButton(@Nonnull UUID buttonId);

    /**
     * Gets all buttons with the specified type. This can be used for non-singular button types like
     * {@link ButtonType#CONNECTION_PLUGIN} or {@link ButtonType#PLUGIN_CREATED} in order to access the buttons of that
     * type.
     *
     * @param buttonType The type of button to get
     * @return List of buttons with the specified type.
     * @since 3.4
     */
    @Nonnull
    List<? extends IToolbarButton> getButtons(@Nonnull ButtonType buttonType);

    /**
     * Gets all buttons that are part of the toolbar.
     *
     * @return Set of all buttons.
     * @since 3.4
     */
    @Nonnull
    Set<? extends IToolbarButton> getButtons();
}
