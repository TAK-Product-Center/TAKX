package gov.takx.core.explorer.controller;

import com.google.common.base.Predicates;
import gov.takx.api.annotation.PseudoBean;
import gov.takx.api.annotation.RemoveObserver;
import gov.takx.api.apps.IAppInstance;
import gov.takx.api.apps.ICoreAppManager;
import gov.takx.api.event.mapobject.MapObjectAdded;
import gov.takx.api.event.mapobject.MapObjectRemoved;
import gov.takx.api.explorer.ExplorerUtils;
import gov.takx.api.explorer.IAppExplorable;
import gov.takx.api.explorer.IExplorable;
import gov.takx.api.explorer.IExplorableGroup;
import gov.takx.api.explorer.IExplorablePathProvider;
import gov.takx.api.explorer.IExplorableRule;
import gov.takx.api.explorer.IExplorableRuleFactory;
import gov.takx.api.explorer.IExplorerDelegateListener;
import gov.takx.api.explorer.IManualExplorableGroup;
import gov.takx.api.explorer.IMapObjectExplorable;
import gov.takx.api.explorer.IRuleGroupController;
import gov.takx.api.explorer.IUserExplorableGroup;
import gov.takx.api.mapobject.IDataMonitor;
import gov.takx.api.mapobject.IMapObject;
import gov.takx.api.mapobject.IMapObjectManager;
import gov.takx.api.platform.ICoreExecutorService;
import gov.takx.api.platform.IDataMonitorServices;
import gov.takx.api.platform.IProgressMonitor;
import gov.takx.api.plugin.app.IAppListener;
import gov.takx.api.plugin.app.IImmutableAppInstance;
import gov.takx.api.shapes.ShapeProperties;
import gov.takx.api.state.IRestorableMap;
import gov.takx.api.state.IRestorableMapSupport;
import gov.takx.commons.swing.AlwaysOnTopJOptionPane;
import gov.takx.core.explorer.IExplorer;
import gov.takx.core.explorer.common.AppExplorable;
import gov.takx.core.explorer.common.IControllerExplorable;
import gov.takx.core.explorer.common.INonGroupableExplorable;
import gov.takx.core.explorer.common.IPathExplorableGroup;
import gov.takx.core.explorer.common.ManualExplorableGroup;
import gov.takx.core.explorer.common.MapObjectExplorable;
import gov.takx.core.explorer.common.PathExplorableGroup;
import gov.takx.core.explorer.common.events.ExplorableLifecycleEvent;
import gov.takx.core.explorer.controller.path.DefaultPathProvider;
import gov.takx.core.explorer.model.IExplorablePersistor;
import gov.takx.core.explorer.model.IExplorerModel;
import gov.takx.platform.KeyUtils;
import gov.takx.platform.ui.UIResources;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import jakarta.enterprise.event.Event;
import jakarta.enterprise.event.Observes;
import jakarta.inject.Inject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.JFrame;
import javax.swing.JOptionPane;

import java.awt.image.BufferedImage;
import java.beans.PropertyChangeEvent;
import java.lang.invoke.MethodHandles;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import static gov.takx.core.explorer.controller.path.ExplorerPathConstants.APPS_PATH;
import static gov.takx.core.explorer.controller.path.ExplorerPathConstants.GROUPS_PATH;
import static gov.takx.platform.ui.ResourceDirectory.PLATFORM_ICONS;

/**
 * Implementation of {@link IExplorerController} for managing all explorables and rules-based and user-created groups.
 *
 * @since 1.48
 */
@SuppressWarnings({"CdiManagedBeanInconsistencyInspection", "removal"})
@PseudoBean
public class ExplorerController implements IExplorerController, IAppListener, IRestorableMapSupport
{
    private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    /**
     * Default icon for Rule based groups.
     */
    private static final BufferedImage RULE_GROUP_ICON = UIResources.getImage(PLATFORM_ICONS, "folder_rule.png");

    /**
     * Default Icon for standard groups.
     */
    static final BufferedImage GROUP_ICON = UIResources.getImage(PLATFORM_ICONS, "folder.png");

    private static final String GROUP_KEY_FAMILY = "gov.takx.explorer.GROUP";
    private static final String RULE_GROUP_KEY_TYPE = "Rule Based";
    private static final String STANDARD_GROUP_KEY_TYPE = "Standard";

    private static final String EXPLORER_ROOT_KEY = new UUID(0, 0).toString();
    private static final String EXPLORABLES_RESTORE_KEY = "Explorables";

    @Inject
    private ICoreExecutorService executorService;
    @Inject
    private IMapObjectManager mapObjectManager;
    @Inject
    private ICoreAppManager appManager;
    @Inject
    private Event<ExplorableLifecycleEvent> lifecycleEvent;

    private final IExplorer explorer;
    private final IExplorerModel model;
    private final List<IExplorablePathProvider> explorablePathProviders = new ArrayList<>();
    private final Collection<IExplorablePersistor> persistors = new LinkedHashSet<>();
    private final Map<String, IExplorableRuleFactory<? extends IExplorableRule>> ruleFactoriesByType = new ConcurrentHashMap<>();
    private final Map<Class<? extends IExplorableRule>, IExplorableRuleFactory<? extends IExplorableRule>> ruleFactoriesByRuleClass = new ConcurrentHashMap<>();
    private final IPathExplorableGroup rootGroup = new PathExplorableGroup(this, null, "Home", EXPLORER_ROOT_KEY);
    private final IPathExplorableGroup controllerGroupsRoot;
    private final IPathExplorableGroup appsRoot;
    private final AtomicBoolean rulesEnabled = new AtomicBoolean(true);
    private final ManualExplorableGroupPersistor basicGroupPersistor;
    private final IExplorerAlarmsController alarmsController;

    // The predicate that determines if an explorable can be added to an IUserExplorableGroup
    private final Predicate<IExplorable> groupablePredicate = Predicate.<IExplorable>not(IExplorableGroup.class::isInstance).and(Predicate.not(INonGroupableExplorable.class::isInstance));

    private boolean loaded;

    /**
     * @param model the model to use for this controller
     * @since 5.3
     */
    public ExplorerController(@Nonnull IExplorer explorer, @Nonnull IExplorerModel model)
    {
        this.explorer = explorer;
        this.model = model;

        // have to set the root from here since we need a controller reference
        model.setRootGroup(rootGroup);

        // Make sure we create external controllers before adding listeners that depend on them
        alarmsController = new ExplorerAlarmsController(this, explorer);

        model.addPropertyChangeListener(this::modelPropertyChange);

        // Ensure certain groups always exist
        controllerGroupsRoot = rootGroup.getOrCreatePath(GROUPS_PATH);

        appsRoot = rootGroup.getOrCreatePath(APPS_PATH);
        appsRoot.setSupportsVisibility(false);

        // create model persistors
        basicGroupPersistor = new ManualExplorableGroupPersistor(this);
        addExplorablePersistor(basicGroupPersistor);
        addExplorablePersistor(new RuleExplorableGroupPersistor(this));

        // default path provider when all others return null
        addPathProvider(new DefaultPathProvider());
    }

    @Override
    @Nonnull
    public IExplorerModel getModel()
    {
        return model;
    }

    private void modelPropertyChange(PropertyChangeEvent event)
    {
        switch (event.getPropertyName())
        {
            case IMapObject.LOCATION, ShapeProperties.LOCATIONS, ShapeProperties.ORIENTATION -> refreshGroups(getRuleGroups(), RefreshReason.LOCATION);
            case IMapObject.NAME -> refreshGroups(getRuleGroups(), RefreshReason.NAME);
            // TODO DCW TAKX-3282 this can be made more efficient if we can get the reference to the actual rule / group
            case IExplorableRule.RULE_CHANGED -> refreshGroups(getRuleGroups(), RefreshReason.OTHER);
            // The Explorable may have some pre-existing alarms. Request update so they aren't missed.
            case IDataMonitorServices.DATA_MONITOR_ADDED,
                    IDataMonitorServices.DATA_MONITOR_REMOVED,
                    IDataMonitor.STATE_CHANGED,
                    IExplorerModel.EXPLORABLE_ADDED,
                    IExplorable.CHILD,
                    IExplorable.HIGHEST_ALARM_STATE_KEY ->
                alarmsController.update();
            case IExplorable.VISIBLE -> explorer.handleVisibilityChange((IExplorable) event.getSource(), (Boolean) event.getNewValue());
            default ->
            {
                // ignore
            }
        }
    }

    @Override
    public void onLoadState(IProgressMonitor progressMonitor)
    {
        if (!loaded)
        {
            mapObjectManager.getMapObjects().forEach(this::initMapObjectExplorable); // Add explorables for all map objects
            appManager.addListener(this);
            loaded = true;
        }
    }

    @RemoveObserver
    @Override
    public void onClearState(IProgressMonitor progressMonitor)
    {
        appManager.removeListener(this);
        model.dispose();
        loaded = false;
    }

    @Override
    public void requestSetSelected(@Nonnull List<IExplorable> explorables, @Nonnull List<IExplorerDelegateListener.SelectionState> states)
    {
        explorer.handleSelectionChange(explorables, states);
    }

    @Override
    public void requestDelete(@Nonnull IExplorable explorable)
    {
        explorable.requestDelete();
    }

    @Override
    public boolean addExplorable(@Nonnull IExplorable explorable)
    {
        if (model.hasExplorable(explorable.getUniqueKey())) return false;

        List<String> path = explorablePathProviders
                .stream()
                .map(provider -> provider.createDefaultPath(explorable))
                .filter(Predicates.notNull())
                .findFirst()
                .orElse(Collections.emptyList());

        IExplorable parent;
        String first = !path.isEmpty() ? path.get(0) : null;
        if (first != null && ExplorerUtils.isUUID(first)) // If the first path element was a UUID then find that element directly
        {
            parent = model.getExplorable(first);
            if (parent != null && path.size() > 1)
            {
                parent = IPathExplorableGroup.getOrCreatePath(parent, path.subList(1, path.size()), this, true, false); // Ensure sub paths exists
            }
        } else
        {
            parent = rootGroup.getOrCreatePath(path, false);
        }
        if (parent != null && parent.addChild(explorable))
        {
            // TODO DCW TAKX-4213 need to move this to a more general location, probably ExplorerModel#explorableAdded
            alarmsController.update();
            lifecycleEvent.fire(new ExplorableLifecycleEvent(this, explorable, true));
            matchExplorableToRuleGroups(explorable);
            basicGroupPersistor.restoreExplorableToGroup(explorable);
            // TODO DCW TAKX-3282 find a more general way of doing this
            if (explorable instanceof IRuleGroupController ruleGroup) refreshGroup(ruleGroup);
            return true;
        }
        return false;
    }

    @Override
    public boolean removeExplorable(@Nonnull IExplorable explorable)
    {
        return removeExplorable(explorable, false);
    }

    // TODO DCW TAKX-3282 the removeEmptyParents flag should be adjusted to change some sort of "hidden" flag on the explorable, which can then be used to filter it out in the UI
    @Override
    public boolean removeExplorable(@Nonnull IExplorable explorable, boolean removeEmptyParents)
    {
        if (model.removeExplorable(explorable))
        {
            lifecycleEvent.fire(new ExplorableLifecycleEvent(this, explorable, false));
            getAlarmsController().update();
            return true;
        } else
        {
            return false;
        }
    }

    @Override
    @Nonnull
    public Collection<IExplorable> getExplorables()
    {
        return model.getExplorables();
    }

    @Override
    @Nullable
    public IExplorable getExplorable(@Nonnull String uniqueKey)
    {
        var explorable = model.getExplorable(uniqueKey);

        // Backwards compatibility to support the explorable's path being its unique key
        if (explorable == null)
        {
            explorable = getExplorableByPath(uniqueKey);
            if (explorable != null)
            {
                logger.warn("Found explorable by path '{}'; this is a deprecated feature and will be removed in a future release.", uniqueKey);
                logger.warn("Use the unique key when calling hasExplorable/getExplorable instead of its path. See also getExplorableByPath(path).");
            }
        }

        return explorable;
    }

    @Nullable
    @Override
    public IExplorable getExplorableByPath(@Nonnull String path)
    {
        return rootGroup.getOrCreatePath(Arrays.asList(path.split(KeyUtils.KEY_SEP)), false, false);
    }

    @Override
    public boolean hasExplorable(@Nonnull String uniqueKey)
    {
        return model.hasExplorable(uniqueKey);
    }

    @Nonnull
    @Override
    public IUserExplorableGroup addGroup(@Nonnull String name, boolean isRuleGroup, @Nullable BufferedImage groupIcon)
    {
        return isRuleGroup ? addEmptyRuleGroup(name) : createManualGroup(name, null, groupIcon);
    }

    @Override
    public void updateGroup(@Nonnull IRuleGroupController delegate, @Nonnull List<IExplorableRule> rules, @Nonnull Map<IExplorableRule, Boolean> enabledMap, @Nonnull Map<IExplorableRule, String> displayNameMap, boolean matchAll)
    {
        if (!rules.equals(delegate.getRules()))
        {
            delegate.clearRules();
            for (IExplorableRule rule : rules) delegate.add(rule);
        }
        enabledMap.forEach(IExplorableRule::setEnabled);
        displayNameMap.forEach(IExplorableRule::setDisplayName);
        delegate.setMatchAll(matchAll);
        ruleGroupUpdated(delegate);
    }

    @Override
    @Nullable
    public IManualExplorableGroup getOrCreateManualGroup(@Nonnull String displayName, @Nullable BufferedImage iconImage, boolean create)
    {
        return getGroupsRoot()
                .getDescendantsStream()
                .filter(IManualExplorableGroup.class::isInstance)
                .map(IManualExplorableGroup.class::cast)
                .filter(e -> displayName.equals(e.getDisplayName()))
                .findFirst()
                .orElseGet(() -> create ? createManualGroup(displayName, null, iconImage) : null);
    }

    @Override
    public void requestSetChildren(@Nonnull IExplorableGroup group, @Nonnull Collection<IExplorable> children)
    {
        group.setChildren(children);
    }

    @Override
    public void addPathProvider(@Nonnull IExplorablePathProvider pathProvider)
    {
        // Path providers are ordered from highest priority to lowest, so always add to the top of the list
        explorablePathProviders.add(0, pathProvider);
    }

    @Override
    public <T extends IExplorableRule> void addRuleFactory(@Nonnull Class<T> ruleClass, @Nonnull IExplorableRuleFactory<T> factory)
    {
        ruleFactoriesByType.put(factory.getType(), factory);
        ruleFactoriesByRuleClass.put(ruleClass, factory);
        for (IRuleGroupController ruleGroup : getRuleGroups()) ruleGroup.ruleFactoryAdded(factory);
    }

    @Override
    public void removeRuleFactory(@Nonnull IExplorableRuleFactory<? extends IExplorableRule> ruleFactory)
    {
        final var factoryType = ruleFactory.getType();
        ruleFactoriesByRuleClass.entrySet().removeIf(entry -> factoryType.equals(entry.getValue().getType()));
        ruleFactoriesByType.remove(factoryType);
    }

    @SuppressWarnings("unchecked")
    @Override
    @Nullable
    public <T extends IExplorableRule> IExplorableRuleFactory<T> getRuleFactory(@Nonnull Class<T> ruleClass)
    {
        return (IExplorableRuleFactory<T>) ruleFactoriesByRuleClass.get(ruleClass);
    }

    @Override
    public IExplorableRuleFactory<? extends IExplorableRule> getRuleFactory(@Nonnull String type)
    {
        return ruleFactoriesByType.get(type);
    }

    @Nonnull
    @Override
    public List<IExplorableRuleFactory<? extends IExplorableRule>> getRuleFactories()
    {
        return new ArrayList<>(ruleFactoriesByType.values());
    }

    @Override
    public void ruleGroupUpdated(@Nonnull IRuleGroupController ruleGroup)
    {
        if (areRulesEnabled()) refreshGroup(ruleGroup);
    }

    @Override
    public void setRulesEnabled(boolean enabled)
    {
        rulesEnabled.getAndSet(enabled);
    }

    @Override
    public boolean areRulesEnabled()
    {
        return rulesEnabled.get();
    }

    @Override
    @Nonnull
    public IManualExplorableGroup createManualGroup(@Nonnull String displayName, @Nullable String uniqueKey, @Nullable BufferedImage iconImage)
    {
        final String key = uniqueKey != null ? uniqueKey : KeyUtils.getUniqueKey(GROUP_KEY_FAMILY, STANDARD_GROUP_KEY_TYPE, UUID.randomUUID().toString());
        ManualExplorableGroup group = new ManualExplorableGroup(this, displayName, key, iconImage != null ? iconImage : GROUP_ICON);
        addExplorable(group);
        return group;
    }

    /**
     * Add a rules-based group.
     *
     * @param groupName name of group to add.
     * @return a rules-based group if one was successfully created, or already existed. This method may return {@code null} if
     * the name is already in use by non-rule-group explorable.
     */
    @Nonnull
    private IRuleGroupController addEmptyRuleGroup(String groupName)
    {
        return addRuleGroup(groupName, null, Collections.emptyList(), null);
    }

    /**
     * Add a rules-based group.
     *
     * @param groupName  name of group to add.
     * @param uniqueKey  Unique key for the group
     * @param parentPath custom parent path for rule group.
     * @param iconImage  custom icon image for rule group.
     * @return a rules-based group if one was successfully created, or already existed. This method may return {@code null} if
     * the name is already in use by non-rule-group explorable.
     */
    @Override
    public IRuleGroupController addRuleGroup(@Nonnull String groupName, @Nullable String uniqueKey, @Nonnull List<String> parentPath, @Nullable BufferedImage iconImage)
    {
        IExplorable explorable = null;
        if (uniqueKey != null) explorable = model.getExplorable(uniqueKey);

        // check if an explorable on the path with the same name exists
        if (explorable != null)
        {
            // An explorable does exist at the path, so now we check whether it is a rule group.
            if (explorable instanceof IRuleGroupController group)
            {
                // Return the rule group.
                group.setIconImage(iconImage);
                return group;
            }

            // If the explorable at this path is not a rule group, we won't replace it, so return null.  If replacement
            // is desired, it is necessary to first remove the original explorable before adding a new one at the same
            // path.
            logger.warn("Unable to add rule group.  Explorable with path={} already exists", ExplorerUtils.createDefaultKey(groupName, parentPath));
            return null;
        }

        // Although the icon for rule groups can be customized, the default smart group icon will be used if one is not
        // provided.
        if (iconImage == null) iconImage = RULE_GROUP_ICON;

        // Create a new rule group, add it and return it.
        final String key = uniqueKey != null ? uniqueKey : KeyUtils.getUniqueKey(GROUP_KEY_FAMILY, RULE_GROUP_KEY_TYPE, UUID.randomUUID().toString());
        IRuleGroupController group = new RuleGroupController(this, groupName, key, parentPath, iconImage);
        addExplorable(group);
        return group;

    }

    private void matchExplorableToRuleGroups(IExplorable explorable)
    {
        if (areRulesEnabled() && groupablePredicate.test(explorable))
        {
            for (IRuleGroupController ruleGroup : getRuleGroups())
            {
                if (ruleGroup.isMatch(explorable))
                {
                    // It's a match, so add this explorable (if one doesn't already exist) to the rule group.
                    ruleGroup.addChild(explorable);
                } else
                {
                    // It's not a match, so remove this explorable (if one exists) from the rule group.
                    ruleGroup.removeChild(explorable);
                }
            }
        }
    }

    @Override
    @Nonnull
    public Set<String> getGroupNames()
    {
        return model.getExplorables().stream().filter(IUserExplorableGroup.class::isInstance).map(IExplorable::getDisplayName).collect(Collectors.toSet());
    }

    @Override
    @Nonnull
    public List<IRuleGroupController> getRuleGroups()
    {
        return model.getRuleGroupControllers();
    }

    @Override
    public void requestGroupsRefresh()
    {
        if (areRulesEnabled()) refreshGroups(getRuleGroups(), RefreshReason.OTHER);
    }

    private void refreshGroup(IRuleGroupController ruleGroup)
    {
        refreshGroups(Collections.singletonList(ruleGroup), RefreshReason.OTHER);
    }

    /**
     * Determine if this rule group should be processed based on the refresh reason.
     *
     * @param ruleGroup     Rule group to process
     * @param refreshReason Reason for performing the refresh
     * @return {@code true} if the rule group should be refreshed based on the reason
     * @since 2.36
     */
    private boolean shouldRefreshGroup(IRuleGroupController ruleGroup, RefreshReason refreshReason)
    {
        return switch (refreshReason)
        {
            case NAME -> ruleGroup.hasNameSensitiveRules();
            case LOCATION -> ruleGroup.hasLocationSensitiveRules();
            case OTHER -> true;
        };
    }

    private void refreshGroups(List<IRuleGroupController> ruleGroups, RefreshReason refreshReason)
    {
        if (!areRulesEnabled()) return;
        logger.debug("Refreshing Rule Groups={}", ruleGroups);
        List<IExplorable> explorables = null; // delay loading these until we need them
        for (IRuleGroupController ruleGroup : ruleGroups)
        {
            if (!shouldRefreshGroup(ruleGroup, refreshReason)) continue;
            if (explorables == null) explorables = getGroupableExplorables();
            // Match all explorables against the group's rules.  This results in a set of all the
            // explorables that need to be in the group.  Note that these are ordinary explorables, not proxies.
            Set<IExplorable> newChildren = new HashSet<>(ruleGroup.getMatchingExplorables(explorables));
            ruleGroup.setChildren(newChildren);
        }
    }

    /**
     * Returns all explorables that can be added to standard groups.
     * <br>
     * Note:If we ever want to allow group nesting, this is one spot where we would control that.
     *
     * @return the List of explorables that can be added to standard groups
     */
    @Override
    @Nonnull
    public List<IExplorable> getGroupableExplorables()
    {
        return getExplorables().stream().filter(groupablePredicate).toList();
    }

    @Override
    @Nonnull
    public Predicate<IExplorable> getGroupablePredicate()
    {
        return groupablePredicate;
    }

    @Override
    @Nonnull
    public IExplorableGroup getGroupsRoot()
    {
        return controllerGroupsRoot;
    }

    @Override
    @Nonnull
    public IPathExplorableGroup getRoot()
    {
        return rootGroup;
    }

    @Override
    @Nonnull
    public IExplorableGroup getAppsRoot()
    {
        return appsRoot;
    }

    @Override
    @Nonnull
    public IExplorerAlarmsController getAlarmsController()
    {
        return alarmsController;
    }

    @Override
    public void removeExplorable(@Nonnull IControllerExplorable explorable, boolean confirm)
    {
        if (!confirm || showRemoveConfirmation(explorable) == JOptionPane.YES_OPTION)
        {
            removeExplorable(explorable);
        }
    }

    private int showRemoveConfirmation(IControllerExplorable selectedGroup)
    {
        final String groupDisplayName = selectedGroup.getDisplayName();
        String message = "<html><center>Are you sure you want to delete group <b>" + groupDisplayName + "</b>" + "?<br>" + "<font color=red>This action cannot be undone.</font></center></html>";
        String title = "Confirm Delete Group - " + groupDisplayName;
        return AlwaysOnTopJOptionPane.showConfirmDialog(JFrame.getFrames()[0], message, title, JOptionPane.YES_NO_OPTION);
    }

    /**
     * Notified by {@link IMapObjectManager} when a new map object is added.
     *
     * @param event The event providing the added map object
     * @since 2.38 - formerly clientMapObjectAdded
     */
    @SuppressWarnings("unused")
    private void onClientMapObjectAdded(@Observes MapObjectAdded event)
    {
        initMapObjectExplorable(event.getMapObject());
    }

    /**
     * Notified by {@link IMapObjectManager} when a map object is removed.
     *
     * @param event The event providing the removed map object
     * @since 2.38 - formerly clientMapObjectRemoved
     */
    @SuppressWarnings("unused")
    private void onMapObjectRemoved(@Observes MapObjectRemoved event)
    {
        IMapObjectExplorable meo = getMapExplorerObject(event.getMapObject().getKey());
        if (meo != null) removeExplorable(meo, true);
    }

    @Override
    public void onAppInstalled(IImmutableAppInstance app)
    {
        // Do nothing.
    }

    @Override
    public void onAppUninstalled(IImmutableAppInstance app)
    {
        // Do nothing.
    }

    @Override
    public void onAppStarted(IImmutableAppInstance app)
    {
        initAppExplorable((IAppInstance) app);
    }

    @Override
    public void onAppStopped(IImmutableAppInstance app)
    {
        IExplorable explorable = getAppExplorableObject(((IAppInstance) app).getUniqueKey());
        if (explorable != null) removeExplorable(explorable, true);
    }

    /**
     * Convenience method to get a map explorer object associated with a key (if one exists).
     *
     * @param uniqueKey key of map explorer object.
     * @return map explorer object associated with the key (if one exists).
     */
    private IMapObjectExplorable getMapExplorerObject(String uniqueKey)
    {
        return model.getExplorable(uniqueKey) instanceof IMapObjectExplorable o ? o : null;
    }

    /**
     * Convenience method to get an app explorer object associated with a key (if one exists).
     *
     * @param uniqueKey key of app explorer object.
     * @return app object associated with the key (if one exists).
     */
    private IAppExplorable getAppExplorableObject(String uniqueKey)
    {
        return model.getExplorable(uniqueKey) instanceof IAppExplorable o ? o : null;
    }

    /**
     * Initialize an explorer object associated with a map object and contained within this manager.  If this manager
     * does not contain an explorer object for the map object, one will be created and added to the manager.
     *
     * @param mapObject map object of interest.
     */
    private void initMapObjectExplorable(IMapObject mapObject)
    {
        if (mapObject == null)
        {
            logger.warn("Unable to create IMapExplorerObject for null IMapObject");
            return;
        }

        // Don't add map object explorables to an API only explorer
        if (explorer.isApiOnlyExplorer()) return;

        // If the MO should not be shown in the explorer, then just bail.
        if (!mapObject.shouldShowInExplorer()) return;

        // Check whether an explorable already exists with the same key.
        IExplorable explorable = getExplorable(mapObject.getKey());

        // If the original explorable is an IMapObjectExplorable, nothing needs to be done.
        if (explorable instanceof IMapObjectExplorable)
        {
            return;
        } else if (explorable != null)
        {
            // Otherwise, if the explorable is not null, then the map object takes precedence over the original
            // explorable and the original explorable is removed.
            removeExplorable(explorable, false);
        }

        // Create a new map explorable create and add it.
        addExplorable(new MapObjectExplorable(mapObject));
    }

    /**
     * Initialize an explorer object associated with an app and contained within this manager.  If this manager
     * does not contain an explorer object for the app, one will be created and added to the manager.
     *
     * @param app The app
     */
    private void initAppExplorable(IAppInstance app)
    {
        if (app == null)
        {
            logger.warn("Unable to create IAppExplorable for null IImmutableAppInstance");
            return;
        }

        // Check whether an explorable already exists with the same key.
        IExplorable explorable = getExplorable(app.getUniqueKey());

        // If the original explorable is an IAppExplorable, nothing needs to be done.
        if (explorable instanceof IAppExplorable)
        {
            return;
        } else if (explorable != null)
        {
            // Otherwise, if the explorable is not null, then the app takes precedence over the original
            // explorable and the original explorable is removed.
            removeExplorable(explorable, false);
        }

        // Create a new map explorable create and add one.
        addExplorable(new AppExplorable(app));
    }

    @Override
    public void addExplorablePersistor(@Nonnull IExplorablePersistor persistor)
    {
        persistors.add(persistor);
    }

    @Override
    public void restore(@Nonnull IRestorableMap map)
    {
        // Give each persistor a chance to bulk restore
        persistors.forEach(p -> p.restore(map));

        // Now try to restore individual explorables
        List<IRestorableMap> restoreMaps = map.getRestorableMapList(EXPLORABLES_RESTORE_KEY);
        for (IRestorableMap explorableMap : restoreMaps)
        {
            for (IExplorablePersistor persistor : persistors)
            {
                // Give persistor a chance to restore a specific explorable
                IExplorable explorable = persistor.restoreExplorable(explorableMap);
                if (explorable != null)
                {
                    // TODO DCW TAKX-3282 need to move the store/restore logic back to the controller since it depends on path providers
                    addExplorable(explorable);
                    // break so we don't restore the same explorable twice
                    break;
                }
            }
        }
    }

    @Override
    public void store(@Nonnull IRestorableMap map)
    {
        // Give each persistor a chance to bulk store
        persistors.forEach(p -> p.store(map));

        // Try to store individual explorables
        List<IRestorableMap> explorableMapList = new ArrayList<>();
        for (IExplorable explorable : getExplorables())
        {
            for (IExplorablePersistor persistor : persistors)
            {
                IRestorableMap explorableMap = persistor.storeExplorable(explorable, map);
                if (explorableMap != null)
                {
                    explorableMapList.add(explorableMap);
                    break;
                }
            }
        }
        map.setRestorableMapList(EXPLORABLES_RESTORE_KEY, explorableMapList);
    }

    @Override
    public String toString()
    {
        return "ExplorerController=" + hashCode();
    }

    /**
     * Indicates the reason for performing a rule group refresh.
     *
     * @since 2.36
     */
    private enum RefreshReason
    {
        LOCATION, // Only affects rule groups containing rules based on location
        NAME,     // Only affects rule groups containing rules based on explorable name
        OTHER     // All other updates, will refresh ALL rule groups
    }
}
