package gov.takx.core.explorer;

import gov.takx.api.annotation.Headless;
import gov.takx.api.annotation.InvokeAndWaitOnEDT;
import gov.takx.api.annotation.InvokeOnEDT;
import gov.takx.api.annotation.PseudoBean;
import gov.takx.api.annotation.RemoveObserver;
import gov.takx.api.explorer.IExplorable;
import gov.takx.api.explorer.IExplorablePathProvider;
import gov.takx.api.explorer.IExplorableRule;
import gov.takx.api.explorer.IExplorableRuleFactory;
import gov.takx.api.explorer.IExplorerDelegateListener;
import gov.takx.api.explorer.IExplorerDelegateListener.SelectionState;
import gov.takx.api.explorer.IGeofenceRule;
import gov.takx.api.explorer.IManualExplorableGroup;
import gov.takx.api.explorer.INameRule;
import gov.takx.api.explorer.IProximityRule;
import gov.takx.api.explorer.IRuleGroupController;
import gov.takx.api.explorer.ITreeNodeComparator;
import gov.takx.api.platform.IProgressMonitor;
import gov.takx.api.state.IRestorableMap;
import gov.takx.api.state.JSONMapUtils;
import gov.takx.commons.lang.ExceptionUtils;
import gov.takx.core.explorer.common.ExplorerStateKeys;
import gov.takx.core.explorer.common.events.ExplorableLifecycleEvent;
import gov.takx.core.explorer.controller.ExplorerController;
import gov.takx.core.explorer.controller.IExplorerController;
import gov.takx.core.explorer.controller.path.AnnotationMapObjectPathProvider;
import gov.takx.core.explorer.controller.path.AppExplorablePathProvider;
import gov.takx.core.explorer.controller.path.ControllerGroupPathProvider;
import gov.takx.core.explorer.controller.path.MarkerMapObjectPathProvider;
import gov.takx.core.explorer.controller.path.ShapeMapObjectPathProvider;
import gov.takx.core.explorer.model.ExplorerModel;
import gov.takx.core.explorer.model.IExplorerModel;
import gov.takx.core.explorer.rules.GeofenceRule;
import gov.takx.core.explorer.rules.MapObjectRule;
import gov.takx.core.explorer.rules.NameRule;
import gov.takx.core.explorer.rules.ProximityRule;
import gov.takx.core.view.gui.explorer.view.ExplorerPanel;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import jakarta.enterprise.event.Observes;
import jakarta.inject.Inject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.Component;
import java.awt.image.BufferedImage;
import java.io.File;
import java.lang.invoke.MethodHandles;
import java.util.Collection;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Predicate;

/**
 */
@SuppressWarnings("CdiManagedBeanInconsistencyInspection")
@PseudoBean
public final class Explorer implements IExplorer
{
    private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    /**
     * UUID for this Explorer.
     */
    private UUID uuid;

    @Inject
    @Headless
    private boolean headless;

    private final ExplorerModel explorerModel = new ExplorerModel();

    /**
     * The explorer controller manages explorables.
     */
    private final ExplorerController controller;

    /**
     * Set of (typically plugin-contributed) explorer delegate listeners.
     */
    private final Set<IExplorerDelegateListener> explorerDelegateListeners = ConcurrentHashMap.newKeySet();

    /**
     * The UI that represents this Explorer.
     */
    private ExplorerPanel explorerPanel;

    /**
     * The title that will be displayed when there are multiple explorers.
     */
    private String title;

    /**
     * The directory where icons are stored for this Explorer.
     */
    private File iconsDir;

    // "API only" explorer that can be only be contributed through the API (see below)
    private final boolean apiOnly;

    /**
     * Construct an Explorer either as an API type, or as the default TAKX type.  The "API only" type can only
     * be created through the plugin API and will result in an Explorer configured such that only Alerts, Groups and
     * additional nodes added through the API will be present.
     * 
     * @param apiOnly {@code true} to create an "API only" Explorer, {@code false} otherwise.  TAKX Core Explorer
     *                defaults to {@code false}.
     * @since 5.4 (previously no apiOnly option)
     */
    public Explorer(boolean apiOnly)
    {
        this.apiOnly = apiOnly;
        controller = new ExplorerController(this, explorerModel);
        if (!apiOnly)
        {
            addPathProvider(new AppExplorablePathProvider());
            addPathProvider(new ShapeMapObjectPathProvider());
            addPathProvider(new MarkerMapObjectPathProvider());
            addPathProvider(new AnnotationMapObjectPathProvider());
            addPathProvider(new ControllerGroupPathProvider());
        }
    }

    @Override
    public void addPathProvider(@Nonnull IExplorablePathProvider pathProvider)
    {
        controller.addPathProvider(pathProvider);
    }

    @Override
    public void init(String title, UUID uuid, File iconsDir)
    {
        this.title = title;
        this.uuid = uuid;
        this.iconsDir = new File(iconsDir, uuid.toString());

        addExplorableRuleFactory(INameRule.class, NameRule.getFactory());
        addExplorableRuleFactory(IGeofenceRule.class, GeofenceRule.getFactory());
        addExplorableRuleFactory(IProximityRule.class, ProximityRule.getFactory());
        addExplorableRuleFactory(MapObjectRule.class, MapObjectRule.getFactory());
    }

    @Override
    @InvokeAndWaitOnEDT
    public void initUI()
    {
        if (explorerPanel == null)
        {
            explorerPanel = new ExplorerPanel(explorerModel, controller, !apiOnly, !apiOnly, !apiOnly, !apiOnly, !apiOnly); // Hide root and core paths in an apiOnly Explorer
        }
    }

    @Override
    public String getTitle()
    {
        return title;
    }

    @Override
    public UUID getUUID()
    {
        return uuid;
    }

    @Override
    public void start(IProgressMonitor progressMonitor, IRestorableMap restorableMap)
    {
        controller.onLoadState(progressMonitor);

        if (restorableMap != null) restoreFromMap(restorableMap);

        // Send event for both non-group and group trees.
        notifyDelegateListeners(IExplorerDelegateListener.LOAD_STATE_EVENT, null, null, null);
        notifyDelegateListeners(IExplorerDelegateListener.LOAD_STATE_EVENT, null, null, null);
    }

    @RemoveObserver
    @Override
    public void stop(IProgressMonitor progressMonitor)
    {
        // Send event for both non-group and group trees.
        notifyDelegateListeners(IExplorerDelegateListener.CLEAR_STATE_EVENT, null, null, null);
        notifyDelegateListeners(IExplorerDelegateListener.CLEAR_STATE_EVENT, null, null, null);

        controller.onClearState(progressMonitor);

        // This is project specific.
        iconsDir = null;
    }

    @Override
    public void handleVisibilityChange(IExplorable explorable, boolean visible)
    {
        notifyDelegateListeners(IExplorerDelegateListener.VISIBILITY_EVENT, explorable, !visible, visible);
    }

    @Override
    public void handleSelectionChange(List<IExplorable> explorables, List<IExplorerDelegateListener.SelectionState> states)
    {
        for (int i = 0, size = explorables.size(); i < size; i++)
        {
            final IExplorable explorable = explorables.get(i);
            final SelectionState newState = states.get(i);
            final SelectionState oldState = newState == SelectionState.SELECTED ? SelectionState.UNSELECTED : SelectionState.SELECTED;

            notifyDelegateListeners(IExplorerDelegateListener.SELECTION_CHANGED_EVENT, explorable, oldState, newState);
        }
    }

    @Override
    public boolean isApiOnlyExplorer()
    {
        return apiOnly;
    }

    private void onExplorableLifecycleEvent(@Observes ExplorableLifecycleEvent event)
    {
        // Only fire the event if the life cycle event occurred on this explorer
        if (controller.equals(event.source()))
        {
            logger.debug("{}", event);
            if (explorerDelegateListeners.isEmpty()) return;
            IExplorable explorable = event.explorable();
            if (event.added())
            {
                notifyDelegateListeners(IExplorerDelegateListener.EXPLORABLE_ADDED_EVENT, explorable, null, explorable);
            } else
            {
                notifyDelegateListeners(IExplorerDelegateListener.EXPLORABLE_REMOVED_EVENT, explorable, explorable, null);
            }
        }
    }

    @Override
    public void handleAlarmStatusChange()
    {
        notifyDelegateListeners(IExplorerDelegateListener.ALARMS_EVENT, null, null, null);
    }

    @Nonnull
    @Override
    public IExplorerController getController()
    {
        return controller;
    }

    @Nonnull
    @Override
    public IExplorerModel getModel()
    {
        return explorerModel;
    }

    @Override
    public IExplorable getOrAddExplorableGroup(String displayName, List<String> parentPath, BufferedImage iconImage)
    {
        return controller.getOrCreateManualGroup(displayName, iconImage, true);
    }

    @Override
    public boolean addExplorable(IExplorable explorable)
    {
        if (logger.isDebugEnabled()) logger.debug("Adding IExplorable {}", explorable.getDisplayName());

        return controller.addExplorable(explorable);
    }

    @Override
    public boolean removeExplorable(IExplorable explorable, boolean removeEmptyParents)
    {
        if (logger.isDebugEnabled()) logger.debug("Removing IExplorable {}{}", explorable.getDisplayName(), removeEmptyParents ? " and empty parents" : "");

        return controller.removeExplorable(explorable, removeEmptyParents);
    }

    @Override
    public void expandTreeToNode(IExplorable node)
    {
        if (explorerPanel != null) explorerPanel.expandTreeToNode(node);
    }

    @Override
    public void setSinglePane(boolean singlePane)
    {
        if (explorerPanel != null) explorerPanel.setSinglePane(singlePane);
    }

    @Override
    public boolean isSinglePane()
    {
        return explorerPanel != null && explorerPanel.isSinglePane();
    }

    @Override
    public void setTreeSelected(@Nullable IExplorable explorable)
    {
        if (explorerPanel != null) explorerPanel.setTreeSelected(explorable);
    }

    @Override
    public void setAutoExpand(String nodeName, boolean autoExpand)
    {
        // TODO DCW the view should do this instead of the controller
        //standardController.setAutoExpand(nodeName, autoExpand);
    }

    @Override
    public void registerComparator(String key, ITreeNodeComparator comparator)
    {
        // TODO DCW TAKX-3283 the view should do this instead of the controller
        //standardController.registerComparator(key, comparator);
    }

    @Override
    public void registerExplorableComparator(IExplorable explorable, Comparator<Object> comparator)
    {
        explorerPanel.registerExplorableComparator(explorable, comparator::compare);
    }

    @Override
    public void registerExplorableComparator(@Nonnull Predicate<IExplorable> checkFunction, @Nonnull Comparator<IExplorable> comparator)
    {
        explorerPanel.registerExplorableComparator(checkFunction, comparator);
    }

    @Override
    public IExplorable getExplorable(String uniqueKey)
    {
        return controller.getExplorable(uniqueKey);
    }

    @Nullable
    @Override
    public IExplorable getExplorableByPath(@Nonnull String path)
    {
        return controller.getExplorableByPath(path);
    }

    @Override
    public boolean hasExplorable(String uniqueKey)
    {
        return controller.hasExplorable(uniqueKey);
    }

    @Override
    public IExplorable getParent(IExplorable explorable)
    {
        return explorable.getParent();
    }

    @Override
    public List<IExplorable> getAncestors(IExplorable explorable)
    {
        return explorable.getAncestors();
    }

    @Override
    public Set<IExplorable> getChildren(IExplorable explorable)
    {
        return new LinkedHashSet<>(explorable.getChildren());
    }

    @Override
    public int getChildCount(IExplorable explorable)
    {
        return explorable.getChildrenCount();
    }

    @Override
    public void addExplorerDelegateListener(IExplorerDelegateListener listener)
    {
        explorerDelegateListeners.add(listener);
    }

    @Override
    public void removeExplorerDelegateListener(IExplorerDelegateListener listener)
    {
        explorerDelegateListeners.remove(listener);
    }

    @Override
    public IRestorableMap getRestorableMap()
    {
        IRestorableMap state = JSONMapUtils.createNewRestorableMap();
        IRestorableMap groupsControllerState = JSONMapUtils.createNewRestorableMap();

        state.setString(ExplorerStateKeys.UUID_KEY, uuid.toString());
        state.setString(ExplorerStateKeys.TITLE_KEY, title);

        String iconsDirectory = iconsDir.getPath();
        groupsControllerState.setTransientValue(ExplorerStateKeys.ICON_FOLDER_LOCATION_KEY, iconsDirectory);

        controller.store(groupsControllerState);
        state.setRestorableMap(ExplorerStateKeys.CONTROLLER_STATE_KEY, groupsControllerState);

        return state;
    }

    @Override
    public void restoreFromMap(IRestorableMap state)
    {
        title = state.getString(ExplorerStateKeys.TITLE_KEY, "Unknown");
        String iconsDirectory = iconsDir.getPath();

        // Check for groups state
        IRestorableMap groupsControllerState = state.getRestorableMap(ExplorerStateKeys.CONTROLLER_STATE_KEY, null);
        if (groupsControllerState != null)
        {
            groupsControllerState.setTransientValue(ExplorerStateKeys.ICON_FOLDER_LOCATION_KEY, iconsDirectory);
            controller.restore(groupsControllerState);
        }
    }

    @Override
    public Collection<IExplorable> getExplorables()
    {
        return controller.getExplorables();
    }

    @Override
    public ExplorerPanel getExplorerPanel()
    {
        return explorerPanel;
    }

    @Override
    public <T extends IExplorableRule> void addExplorableRuleFactory(Class<T> ruleClass, IExplorableRuleFactory<T> factory)
    {
        controller.addRuleFactory(ruleClass, factory);
    }

    @Override
    public void removeExplorableRuleFactory(IExplorableRuleFactory<? extends IExplorableRule> factory)
    {
        controller.removeRuleFactory(factory);
    }

    @Override
    public void setRulesEnabled(boolean enabled)
    {
        controller.setRulesEnabled(enabled);
    }

    @Override
    public boolean areRulesEnabled()
    {
        return controller.areRulesEnabled();
    }

    @Override
    @InvokeOnEDT
    public void requestGroupsRefresh()
    {
        controller.requestGroupsRefresh();
    }

    @Override
    @InvokeOnEDT
    public void setAlarmsViewVisible(boolean visible)
    {
        controller.getAlarmsController().getAlertsRoot().setVisibleInExplorer(visible);
    }

    @Override
    @InvokeOnEDT
    public void setGroupsViewVisible(boolean visible)
    {
        controller.getGroupsRoot().setVisibleInExplorer(visible);
    }

    private void notifyDelegateListeners(String eventName, IExplorable explorable, Object oldValue, Object newValue)
    {
        if (explorerDelegateListeners.isEmpty()) return;

        ExceptionUtils.safeDoOperation(explorerDelegateListeners,
                listener -> listener.onExplorerEvent(eventName, explorable, oldValue, newValue, false));
    }

    @Override
    public void loadExplorables()
    {
        controller.onLoadState(null);
    }

    @Override
    public <T extends IExplorableRule> T createRule(Class<T> ruleClass)
    {
        IExplorableRuleFactory<T> factory = controller.getRuleFactory(ruleClass);
        return factory != null ? factory.createRule() : null;
    }

    @Override
    public Collection<IRuleGroupController> getRuleGroupsControllers()
    {
        return controller.getRuleGroups();
    }

    @Override
    public IRuleGroupController createRuleGroupsController(String name, boolean rulesBased)
    {
        return createRuleGroup(name);
    }

    @Override
    public Optional<IRuleGroupController> getRuleGroupsController(String name)
    {
        return controller.getRuleGroups().stream().filter(c -> c.getDisplayName().equals(name)).findFirst();
    }

    @Override
    public void removeRuleGroupsController(IRuleGroupController controller)
    {
        this.controller.removeExplorable(controller);
    }

    @Nonnull
    @Override
    public IRuleGroupController createRuleGroup(@Nonnull String name)
    {
        return (IRuleGroupController) controller.addGroup(name, true, null);
    }

    @Nonnull
    @Override
    public IManualExplorableGroup createManualGroup(@Nonnull String name)
    {
        return (IManualExplorableGroup) controller.addGroup(name, false, null);
    }

    @Override
    public void addComponentToBorder(Component component, String borderLayoutLocation)
    {
        explorerPanel.addComponentToBorder(component, borderLayoutLocation);
    }

    @Override
    public Component getExplorerUIComponent()
    {
        if (headless) throw new IllegalStateException("Cannot call getExplorerUIComponent in headless environment");

        initUI();
        return explorerPanel;
    }

    @Override
    public void addViewFilter(@Nonnull Predicate<IExplorable> filter)
    {
        explorerPanel.addViewFilter(filter);
    }

    @Override
    public void addViewFilter(@Nonnull Predicate<IExplorable> parentFilter, @Nonnull Predicate<IExplorable> filter)
    {
        explorerPanel.addViewFilter(parentFilter, filter);
    }

    @Override
    public void removeViewFilter(@Nonnull Predicate<IExplorable> filter)
    {
        explorerPanel.removeViewFilter(filter);
    }

    @Override
    public String toString()
    {
        return "Explorer{uuid=" + uuid + '}';
    }
}
