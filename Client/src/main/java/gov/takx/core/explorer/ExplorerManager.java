package gov.takx.core.explorer;

import gov.takx.api.annotation.Headless;
import gov.takx.api.annotation.InvokeAndWaitOnEDT;
import gov.takx.api.annotation.InvokeOnEDT;
import gov.takx.api.annotation.VFSManagerScope;
import gov.takx.api.event.StartProjectServices;
import gov.takx.api.event.StopProjectServices;
import gov.takx.api.event.project.ProjectSaving;
import gov.takx.api.explorer.IExplorable;
import gov.takx.api.explorer.IExplorerDelegate;
import gov.takx.api.filesystem.IVirtualFileSystemManager;
import gov.takx.api.filesystem.VFSScope;
import gov.takx.api.platform.IProgressMonitor;
import gov.takx.api.state.IRestorableMap;
import gov.takx.api.state.JSONMapUtils;
import gov.takx.core.explorer.common.ExplorerStateKeys;
import gov.takx.core.explorer.common.events.ExplorableLifecycleEvent;
import gov.takx.core.project.ServiceOrdering;
import gov.takx.core.view.gui.ATitledPanel;
import gov.takx.core.view.gui.explorer.view.MainExplorerPanel;
import jakarta.annotation.Priority;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import jakarta.inject.Inject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.Component;
import java.io.File;
import java.lang.invoke.MethodHandles;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Manages the set of Explorers.
 *
 * @since 1.48
 */
@ApplicationScoped
public class ExplorerManager implements IExplorerManager
{
    private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());
    private static final String DEFAULT_EXPLORER_TITLE = "Explorer";
    private static final String VFS_NAMESPACE = "ExplorerManager";
    private static final String VFS_STATE_FILENAME = "ExplorerManagerState.json";
    private static final String VFS_ICON_FOLDER = "icons";
    private static final String EXPLORERS_MAP_KEY = "explorers";
    private static final UUID DEFAULT_EXPLORER_UUID = new UUID(0, 0);
    private static final String UNKNOWN_EXPLORER_TITLE = "(unknown)";

    /**
     * Map from each Explorer UUID to the actual Explorer.
     */
    private final Map<UUID, IExplorer> explorerMap = new HashMap<>();

    private final IVirtualFileSystemManager projectVFSManager;
    private final boolean headless;

    /**
     * The main explorer panel. This contains the default Explorer or the tabbed pane if there are multiple Explorers
     * visible.
     *
     * @since 2.3.4
     */
    private MainExplorerPanel mainExplorerPanel;

    @Inject
    ExplorerManager(@VFSManagerScope(VFSScope.Project) IVirtualFileSystemManager projectVFSManager, @Headless boolean headless)
    {
        this.projectVFSManager = projectVFSManager;
        this.headless = headless;
    }

    @Override
    public ATitledPanel getViewComponent()
    {
        if (!headless && mainExplorerPanel == null) mainExplorerPanel = new MainExplorerPanel();

        return mainExplorerPanel;
    }

    @Override
    public IExplorer getDefaultExplorerDelegate()
    {
        return explorerMap.get(DEFAULT_EXPLORER_UUID);
    }

    @Override
    @InvokeOnEDT
    public void setDefaultExplorerVisible(boolean visible)
    {
        if (visible)
        {
            mainExplorerPanel.addExplorer(DEFAULT_EXPLORER_TITLE, explorerMap.get(DEFAULT_EXPLORER_UUID).getExplorerPanel(), 0);
        } else
        {
            mainExplorerPanel.removeExplorer(explorerMap.get(DEFAULT_EXPLORER_UUID).getExplorerPanel());
        }
    }

    @Override
    public void setShowTabForSingleExplorer(boolean showTabForSingleExplorer)
    {
        mainExplorerPanel.setShowTabForSingleExplorer(showTabForSingleExplorer);
    }

    @Override
    public IExplorerDelegate createExplorer(String title)
    {
        return createExplorer(title, -1); // index of -1 adds the tab at the end of the tab list
    }

    @Override
    public IExplorerDelegate createExplorer(String title, int index)
    {
        return createExplorer(title, index, false);
    }

    @Override
    public IExplorerDelegate createExplorer(String title, int index, boolean apiOnly)
    {
        var explorer = createNewExplorer(title, apiOnly);
        if (!headless) addExplorerToUI(explorer, index);
        return explorer;
    }

    @Override
    public IExplorerDelegate createEmbeddableExplorer(String title)
    {
        return createNewExplorer(title);
    }

    @Override
    @InvokeOnEDT
    public void showExplorer(UUID uuid)
    {
        IExplorer explorer = explorerMap.get(uuid);

        if (explorer != null && !headless) mainExplorerPanel.showTab(explorer.getExplorerPanel());
    }

    @Override
    public UUID getVisibleExplorerId()
    {
        Component visibleComponent = mainExplorerPanel.getVisibleComponent();

        for (IExplorer explorer : explorerMap.values())
        {
            if (explorer.getExplorerPanel() == visibleComponent) return explorer.getUUID();
        }

        // Should never be null.
        return null;
    }

    @Override
    public boolean deleteExplorer(UUID uuid)
    {
        logger.debug("Deleting explorer with UUID {}", uuid);

        if (DEFAULT_EXPLORER_UUID.equals(uuid))
        {
            logger.error("Cannot remove the default Explorer. Try hiding it instead.");
            return false;
        }

        var explorer = explorerMap.remove(uuid);

        if (explorer != null)
        {
            explorer.stop(null);
            if (!headless) removeExplorerFromUI(explorer);
            return true;
        }

        return false;
    }

    @Override
    public IExplorerDelegate getExplorerDelegate(UUID uuid)
    {
        return explorerMap.get(uuid);
    }

    @Override
    public List<IExplorerDelegate> getExplorerDelegates()
    {
        return new ArrayList<>(explorerMap.values());
    }

    /**
     * Create a new Explorer, wire it up and track it.
     *
     * @return The new explorer.
     */
    private Explorer createNewExplorer(String title)
    {
        return createNewExplorer(title, UUID.randomUUID(), false);
    }

    /**
     * Create a new Explorer with the additional option to create an "API only" Explorer.  An API only Explorer will be
     * empty of all nodes other than alerts, groups and those explicitly added through the API.
     * 
     * @param title   the title for the explorer
     * @param apiOnly {@code true} to create an API only explorer, {@code false} for a TAKX core type explorer
     * @return The new explorer
     * @since 5.4
     */
    private Explorer createNewExplorer(String title, boolean apiOnly)
    {
        return createNewExplorer(title, UUID.randomUUID(), apiOnly);
    }

    /**
     * Create a new Explorer, wire it up and track it.  Additionally, specify the UUID and whether it should
     * be an API only type Explorer or not.
     *
     * @param title   The title that will be used if there are multiple Explorers.
     * @param uuid    The unique id for the Explorer.
     * @param apiOnly {@code true} to create an API only explorer, {@code false} for a TAKX core type explorer
     * @return The new Explorer.
     */
    private Explorer createNewExplorer(String title, UUID uuid, boolean apiOnly)
    {
        Explorer explorer = new Explorer(apiOnly);

        explorer.setRulesEnabled(true);

        // Initialize.
        explorer.init(title, uuid, getIconsDirectory());

        // Add all the known explorables unless apiOnly
        if (!apiOnly && !DEFAULT_EXPLORER_UUID.equals(uuid) && getDefaultExplorerDelegate() != null)
        {
            getDefaultExplorerDelegate().getExplorables().forEach(explorer::addExplorable);
        }

        // Track it.
        explorerMap.put(explorer.getUUID(), explorer);

        logger.debug("Created explorer {}", explorer);

        return explorer;
    }

    /**
     * Start our service.
     * <p>
     * For now, only the default Explorer's state will be restored.
     */
    private void startService(@Observes @Priority(ServiceOrdering.START_ExplorerManager) StartProjectServices event)
    {
        final IProgressMonitor progressMonitor = event.getProgressMonitor();

        progressMonitor.setText("Starting Explorer Manager");

        // Restore explorers or create the default explorer.
        restorePersistables(progressMonitor);

        // This might be a new project, or the restore may have encountered errors. If so, create the default Explorer.
        if (!explorerMap.containsKey(DEFAULT_EXPLORER_UUID))
        {
            var defaultExplorer = createNewExplorer(DEFAULT_EXPLORER_TITLE, DEFAULT_EXPLORER_UUID, false);

            if (!headless) addExplorerToUI(defaultExplorer, 0);

            defaultExplorer.start(progressMonitor, null);
        }
    }

    /**
     * Stop our service.
     */
    private void stopService(@Observes @Priority(ServiceOrdering.STOP_GenericCoreService) StopProjectServices event)
    {
        final IProgressMonitor progressMonitor = event.getProgressMonitor();

        explorerMap.values().forEach(explorer -> explorer.stop(progressMonitor));
        explorerMap.clear();

        if (!headless && mainExplorerPanel != null) mainExplorerPanel.clear();
        mainExplorerPanel = null;
    }

    /**
     * Process a lifecycle event from an explorable, adding/removing the explorable from the default explorer if
     * necessary.
     *
     * @param event The event to process
     */
    private void onExplorableLifecycleEvent(@Observes ExplorableLifecycleEvent event)
    {
        IExplorable explorable = event.explorable();
        Boolean copyToDefaultExplorer = explorable.getBooleanValue(IExplorable.COPY_TO_DEFAULT_EXPLORER, false);

        // Only need to add/remove the explorable if it has the copy-to-default property
        if (copyToDefaultExplorer)
        {
            IExplorer defaultExplorer = getDefaultExplorerDelegate();

            if (event.added())
            {
                IExplorable copy = explorable.copy();
                if (copy != null) defaultExplorer.addExplorable(copy);
            } else
            {
                IExplorable existing = defaultExplorer.getExplorable(explorable.getUniqueKey());
                if (existing != null) defaultExplorer.removeExplorable(existing, true);
            }
        }
    }

    /**
     * Store our persistables when the project is saving.
     */
    private void handleSaving(@Observes ProjectSaving event)
    {
        storePersistables();
    }

    /**
     * Add a new Explorer at the specified tab index.
     *
     * @param explorer The Explorer to add.
     * @param index    The tab index where the Explorer should be added.
     */
    @InvokeAndWaitOnEDT
    private void addExplorerToUI(Explorer explorer, int index)
    {
        explorer.initUI();
        mainExplorerPanel.addExplorer(explorer.getTitle(), explorer.getExplorerPanel(), index);
    }

    /**
     * Remove an Explorer from the tabbed UI.
     *
     * @param explorer The Explorer to remove
     */
    @InvokeOnEDT
    private void removeExplorerFromUI(IExplorer explorer)
    {
        mainExplorerPanel.removeExplorer(explorer.getExplorerPanel());
    }

    /**
     * Save the explorer manager state and the state of the default Explorer.  Excludes Explorers added by plugins.
     *
     * @see #restorePersistables(IProgressMonitor)
     */
    private void storePersistables()
    {
        // root map
        IRestorableMap rootMap = JSONMapUtils.createNewRestorableMap();

        // map of Explorers
        IRestorableMap explorersMap = JSONMapUtils.createNewRestorableMap();

        // add explorers map to root map
        rootMap.setRestorableMap(EXPLORERS_MAP_KEY, explorersMap);

        // Only store the default Explorer.  Otherwise:
        // 1. Problems opening a project with a build that doesn't contain the plugins
        // 2. Problems if plugin is upgraded and uses a completely different tree structure.
        // 3. Problems with the plugins re-building their models... better to do from a file than from the UI.
        explorerMap.entrySet().stream().filter(explorerEntry -> DEFAULT_EXPLORER_UUID.equals(explorerEntry.getKey())).forEach(entry -> explorersMap.setRestorableMap(entry.getKey().toString(), entry.getValue().getRestorableMap()));

        // write entire map to a file
        rootMap.writeToFile(getStateFile());

        if (logger.isDebugEnabled()) logger.debug("Saved ExplorerManager state:\n{}", rootMap);
    }

    /**
     * Restore persisted Explorers, providing it is the default Explorer.  Does not try to restore state of explorables
     * added by plugins.
     *
     * @param progressMonitor Progress monitor to use to provide progress feedback.
     * @see #storePersistables()
     */
    private void restorePersistables(IProgressMonitor progressMonitor)
    {
        final IRestorableMap rootRestorableMap = getPersistables();
        final IRestorableMap explorersMap = rootRestorableMap != null
                ? rootRestorableMap.getRestorableMap(EXPLORERS_MAP_KEY, null)
                : null;

        if (explorersMap != null)
        {
            Set<String> explorerUUIDs = explorersMap.getKeys();

            // For now, we only restore the default Explorer.  See explanation in store method.
            explorerUUIDs.stream().filter(uuid -> DEFAULT_EXPLORER_UUID.toString().equals(uuid)).forEach(uuid -> {
                IRestorableMap explorerState = explorersMap.getRestorableMap(uuid, null);

                if (explorerState != null)
                {
                    String title = explorerState.getString(ExplorerStateKeys.TITLE_KEY, null);
                    if (title == null) logger.warn("Could not restore Explorer title. Using default.");

                    String uuidString = explorerState.getString(ExplorerStateKeys.UUID_KEY, null);
                    if (uuidString == null) logger.warn("Could not restore Explorer UUID. Generating new UUID.");

                    Explorer explorer = createNewExplorer(
                            title != null ? title : UNKNOWN_EXPLORER_TITLE,
                            uuidString != null ? UUID.fromString(uuidString) : UUID.randomUUID(),
                            false);

                    if (!headless)
                    {
                        addExplorerToUI(explorer, explorer.getUUID().equals(DEFAULT_EXPLORER_UUID) ? 0 : -1);
                    }

                    explorer.start(progressMonitor, explorerState);
                }
            });
        }
    }

    /**
     * Return the entire state map or {@code null} if the file does not exist.
     *
     * @return The state map or {@code null} if the map doesn't exist.
     */
    private IRestorableMap getPersistables()
    {
        IRestorableMap rootMap = null;

        // look for the state file
        File stateFile = getStateFile();

        // if it exists, turn it back into a map
        if (stateFile.exists())
        {
            try
            {
                rootMap = JSONMapUtils.restoreMap(stateFile);
            } catch (Exception e)
            {
                logger.error("Error restoring state.");
            }
        }

        return rootMap;
    }

    /**
     * Find the Explorer Manager state file in the VFS.
     *
     * @return The Explorer Manager state file, or {@code null} if it does not exist.
     */
    private File getStateFile()
    {
        return new File(getStateDirectory(), VFS_STATE_FILENAME);
    }

    /**
     * Find the VFS directory where the Explorer Manager state should be stored.
     *
     * @return The VFS directory where the Explorer Manager state should be stored.
     */
    private File getStateDirectory()
    {
        File dir = projectVFSManager.getVirtualFileSystem(VFS_NAMESPACE).getRootDir();

        return new File(dir.getPath());
    }

    /**
     * Find the VFS directory where Explorers can store their icons.
     *
     * @return The VFS directory where Explorers can store their icons.
     */
    private File getIconsDirectory()
    {
        File iconsDirectory = new File(getStateDirectory(), VFS_ICON_FOLDER);
        if (!iconsDirectory.exists() && !iconsDirectory.mkdirs()) logger.error("Failed to make directory: {}", iconsDirectory.getAbsolutePath());
        return iconsDirectory;
    }
}
