package gov.takx.api.explorer;

import gov.takx.api.annotation.InjectableService;
import gov.takx.api.annotation.NoPluginImplementations;

import java.util.List;
import java.util.UUID;

/**
 * Provides plugins with access to the Explorer Manager's features. Allows plugins to create, access and remove
 * specific Explorers.
 *
 * @since 2.4
 */
@InjectableService
@NoPluginImplementations
public interface IExplorerManagerDelegate
{
    /**
     * Create a new, empty Explorer.
     *
     * @param title The panel's title.
     * @return The explorer's delegate.
     * @see IExplorerDelegate#loadExplorables()
     */
    IExplorerDelegate createExplorer(String title);

    /**
     * Create a new, empty Explorer.
     *
     * @param title The panel's title.
     * @param index The index at which the tab should be added. If the value is out of range, the tab will be
     *              added to the beginning or end of the list.
     * @return The explorer's delegate.
     * @see IExplorerDelegate#loadExplorables()
     */
    IExplorerDelegate createExplorer(String title, int index);

    /**
     * Create a new, empty Explorer, along with the option to specify an "API only" Explorer.
     *
     * @param title   The panel's title
     * @param index   The index at which the tab should be added. If the value is out of range, the tab will be
     *                added to the beginning or end of the list
     * @param apiOnly Set to {@code true} for an API only type Explorer, differing from the TAKX core Explorer in
     *                that only Alerts, Groups and nodes explicitly added through the API are included
     * @return The new explorer's delegate.
     * @see IExplorerDelegate#loadExplorables()
     * @since 5.4
     */
    IExplorerDelegate createExplorer(String title, int index, boolean apiOnly);

    /**
     * Create a new, empty Explorer without adding it to the user interface.
     * <p>
     * This method should be used by plugins that wish to embed Explorers within their own panels. Plugins can
     * retrieve the Explorer UI component using {@link IExplorerDelegate#getExplorerUIComponent()} after creating the
     * Explorer with this method.
     *
     * @param title The explorer's title
     * @return The explorer's delegate
     * @since 2.39
     */
    IExplorerDelegate createEmbeddableExplorer(String title);

    /**
     * Show a specific explorer.
     *
     * @param uuid The Explorer's UUID. If no Explorer is associated with the id, the call will be a no-op.
     */
    void showExplorer(UUID uuid);

    /**
     * Get the UUID of the Explorer that is currently being displayed.
     *
     * @return The UUID of the Explorer that is currently being displayed.
     */
    UUID getVisibleExplorerId();

    /**
     * Delete an Explorer. The default Explorer can be hidden but cannot be deleted.
     *
     * @param uuid The Explorer's UUID.
     * @return {@code true} iff the Explorer was successfully deleted.
     */

    boolean deleteExplorer(UUID uuid);

    /**
     * Get the delegate associated with a specific explorer.
     *
     * @param uuid The Explorer's unique id.
     * @return The Explorer's delegate, or {@code null} if no Explorer with the specified id exists.
     */
    IExplorerDelegate getExplorerDelegate(UUID uuid);

    /**
     * Get all Explorer delegates, including the delegate for the default Explorer.
     *
     * @return A list of all Explorer delegates.
     */
    List<IExplorerDelegate> getExplorerDelegates();

    /**
     * Get the default Explorer's delegate. This is the delegate for the Explorer that is created by the application
     * at startup and that cannot be deleted.
     *
     * @return The default Explorer's delegate.
     */
    IExplorerDelegate getDefaultExplorerDelegate();

    /**
     * Toggle the visibility of the default Explorer which cannot be removed.
     *
     * @param visible {@code true} to make the default Explorer visible, {@code false} to hide it.
     */
    void setDefaultExplorerVisible(boolean visible);

    /**
     * Update the setting for the option that will display a tab for a single panel.
     *
     * @param showTabForSingleExplorer {@code true} to show a tab for a single panel, {@code false} to show
     *                                 a single panel directly in the main explorer panel.
     */
    void setShowTabForSingleExplorer(boolean showTabForSingleExplorer);
}
