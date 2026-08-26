package gov.takx.core.explorer;

import gov.takx.api.explorer.IExplorable;
import gov.takx.api.explorer.IExplorerDelegate;
import gov.takx.api.explorer.IExplorerDelegateListener;
import gov.takx.api.platform.IProgressMonitor;
import gov.takx.api.state.IRestorableMap;
import gov.takx.core.explorer.controller.IExplorerController;
import gov.takx.core.explorer.model.IExplorerModel;
import gov.takx.core.view.gui.explorer.view.ExplorerPanel;
import jakarta.annotation.Nonnull;

import java.io.File;
import java.util.List;
import java.util.UUID;

/**
 * Manages a single explorer view.
 *
 * @since 2.4
 */
public interface IExplorer extends IExplorerDelegate
{
    /**
     * @return the explorer controller for this explorer instance
     * @since 5.3
     */
    @Nonnull
    IExplorerController getController();

    /**
     * @return the explorer model for this explorer instance
     * @since 5.3
     */
    @Nonnull
    IExplorerModel getModel();

    /**
     * Get the explorer panel for this explorer.
     *
     * @return the explorer panel for this explorer.
     */
    ExplorerPanel getExplorerPanel();

    /**
     * General initialization of the explorer. This happens immediately after construction of the object, as opposed to
     * {@link #start(IProgressMonitor, IRestorableMap)}  which is called when the service is started.
     *
     * @param title    The title that will be displayed if there are multiple explorers.
     * @param uuid     The unique identifier for this Explorer.
     * @param iconsDir The icons directory.
     */
    void init(String title, UUID uuid, File iconsDir);

    /**
     * Initialize the UI for this explorer, wiring up the controllers to the views. This method should only be called
     * if the application is not running in headless mode.
     */
    void initUI();

    /**
     * Start the object, initializing any resourcing and restoring state when necessary.
     *
     * @param progressMonitor Used to provide progress feedback.
     * @param restorableMap   The state to restore. Will be {@code null} if there is no state to restore.
     */
    void start(IProgressMonitor progressMonitor, IRestorableMap restorableMap);

    /**
     * Shut down the explorer.
     *
     * @param progressMonitor Used to provide progress feedback.
     */
    void stop(IProgressMonitor progressMonitor);

    /**
     * Get the title that will be used in the UI if there are multiple Explorers.
     *
     * @return The Explorer's title.
     */
    String getTitle();

    /**
     * Notification that the visibility of an explorable has changed.
     *
     * @param explorable Explorable that changed
     * @param visible new visibility state
     * @since 5.3
     */
    void handleVisibilityChange(IExplorable explorable, boolean visible);

    /**
     * @return {@code true} if this Explorer is of a type that can only be created through the API, {@code false}
     * otherwise.  The "API only" Explorer will consist only of Alerts, Groups and nodes explicitly added through
     * the API.
     *
     * @since 5.4
     */
    boolean isApiOnlyExplorer();

    /**
     * Notification that the explorer selection has changed.
     *
     * @param explorables Explorables whose selection state has changed
     * @param states Selection state for each explorable in {@code explorables}
     * @since 5.3
     */
    void handleSelectionChange(List<IExplorable> explorables, List<IExplorerDelegateListener.SelectionState> states);

    /**
     * Notification that the state of an alarm has changed.
     * @since 5.3
     */
    void handleAlarmStatusChange();
}
