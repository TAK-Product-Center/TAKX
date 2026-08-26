package gov.takx.api.explorer;

import gov.tak.api.annotation.DeprecatedApi;
import gov.takx.api.annotation.NoPluginImplementations;
import gov.takx.api.plugin.device.ADevicePluginController;
import gov.takx.api.state.IRestorableMap;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import java.awt.Component;
import java.awt.image.BufferedImage;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.function.Predicate;

/**
 * Delegate interface for plugin developers to access functionality in the explorer.
 *
 * @author Proprietary information subject to the terms of a Non-Disclosure Agreement2
 * @since 1.48
 */
@NoPluginImplementations
public interface IExplorerDelegate
{
    /**
     * Return this Explorer's unique id.
     *
     * @return The Explorer's unique id.
     */
    UUID getUUID();

    /**
     * Create an explorable group with an icon image and add it to the explorer.  If an explorable group with an
     * identical path and display name combination has already been added, it will be updated with the icon image.
     *
     * @param displayName display name for new explorable group.
     * @param parentPath  parent path for new explorable group.
     * @param iconImage   icon image to use for the explorable group in the explorer.
     * @return explorable group created (or retrieved) from the explorer.
     */
    IExplorable getOrAddExplorableGroup(String displayName, List<String> parentPath, BufferedImage iconImage);

    /**
     * Add an explorable to the explorer.  If an explorable with the same unique key already exists, this method will
     * fail and the new explorable will NOT replace the original.
     * <p>
     * If you wish to replace an explorable, the original must first be removed from this controller.  See
     * {@link #removeExplorable(IExplorable, boolean)} for more information.
     * <p>
     * If you wish to add an explorable as a child to a Map Object (such as a Device), it is necessary to create the
     * explorable and pass in the Map Object Unique ID (obtained via {@link ADevicePluginController#getUniqueKey()}
     * for a Device Map Object) as the parent path instead of the actual parent path.
     * <p>
     * For example, the following code can be called from the Device Plugin Controller to add a child node to the device
     * <p>
     * {@code IExplorable explorableChildNode = new Explorable(displayName, getUniqueKey());}
     * <p>
     * {@code devicePluginDelegate.getExplorerManagerDelegate().getDefaultExplorerDelegate().addExplorable(explorableChildNode);}
     *
     * @param explorable explorable to add.
     * @return {@code true} if the add was successful, or {@code false} if an explorable with the same unique
     * key already exists.
     */
    boolean addExplorable(IExplorable explorable);

    /**
     * Remove an explorable from the explorer.  Once removed, the explorable's
     * {@link IExplorable#dispose()} method will be called. Note that if the explorable has
     * children, they will also be removed and disposed.
     *
     * @param explorable         explorable to remove.
     * @param removeEmptyParents if {@code true} then this action will cascade to also remove any parents made
     *                           empty by removing the explorable.
     * @return {@code true} if the explorable was found and removed, or {@code false} if it did not exist in
     * the explorer.
     */
    boolean removeExplorable(IExplorable explorable, boolean removeEmptyParents);

    /**
     * Get an explorable object from the explorer.
     *
     * @param uniqueKey unique key identifier of the explorable to retrieve from the explorer.
     * @return a retrieved explorable object matching the unique key identifier, or {@code null} if one was not found.
     */
    IExplorable getExplorable(String uniqueKey);

    /**
     * Get an explorable using its flat string path.  E.g., {@code "Devices/TX/TX 1"}.
     *
     * @param path Path of explorable to locate
     * @return explorable with that path, or {@code null} if not found
     * @since 5.3
     */
    @Nullable
    IExplorable getExplorableByPath(@Nonnull String path);

    /**
     * Determine whether an explorable exists in this model.
     *
     * @param uniqueKey unique key of explorable of interest.
     * @return {@code true} if the explorable exists in this model, or {@code false} otherwise.
     */
    boolean hasExplorable(String uniqueKey);

    /**
     * Get an explorable's parent. Note that this will return {@code null} if no parent exists for the explorable.
     *
     * @param explorable explorable of interest.
     * @return the explorable's parent, or {@code null} if none exists.
     */
    IExplorable getParent(IExplorable explorable);

    /**
     * Get a list of an explorable's ancestors.  The list of ancestor explorables should match (in length and order)
     * the explorable's parent path list.
     * <p>
     * Note that this will return an empty list if no ancestors exists for the explorable.
     *
     * @param explorable explorable of interest.
     * @return a list of the explorable's ancestors, or an empty list if none exist.
     */
    List<IExplorable> getAncestors(IExplorable explorable);

    /**
     * Get an explorable's set of children.
     *
     * @param explorable explorable of interest.
     * @return set of explorable's children.
     */
    Set<IExplorable> getChildren(IExplorable explorable);

    /**
     * Get an explorable's number of children.
     *
     * @param explorable an explorable's number of children.
     * @return number of explorable's children.
     */
    int getChildCount(IExplorable explorable);

    /**
     * Add a path provider to provide default paths for explorables within this explorer.
     *
     * @param pathProvider the path provider
     * @since 5.3
     */
    void addPathProvider(@Nonnull IExplorablePathProvider pathProvider);

    /**
     * Register a comparator for children of the named node
     *
     * @param key        The unique key of the targeted node
     * @param comparator The comparator used to sort children of the named parent node
     * @since 2.29
     * @deprecated use {@link #registerExplorableComparator(IExplorable, Comparator)}
     */
    @Deprecated
    @DeprecatedApi(since = "5.7", forRemoval = true, removeAt = "5.10")
    void registerComparator(String key, ITreeNodeComparator comparator);

    /**
     * Register a comparator for the children of the explorable node.
     * 
     * @param explorable the explorable node
     * @param comparator the comparator for the children of the explorable
     * @since 5.4
     */
    void registerExplorableComparator(IExplorable explorable, Comparator<Object> comparator);

    /**
     * Register a comparator using a predicate function to determine where to apply it.
     *
     * @param checkFunction a Predicate to test if the comparator applies
     * @param comparator the Comparator to use to sort the nodes
     * @since 5.7.1
     */
    void registerExplorableComparator(@Nonnull Predicate<IExplorable> checkFunction, @Nonnull Comparator<IExplorable> comparator);

    /**
     * Add an explorable rule factory.
     *
     * @param factory factory to add.
     * @since 2.38
     */
    <T extends IExplorableRule> void addExplorableRuleFactory(Class<T> ruleType, IExplorableRuleFactory<T> factory);

    /**
     * Remove an explorable rule factory.
     *
     * @param factory factory to remove.
     */
    void removeExplorableRuleFactory(IExplorableRuleFactory<? extends IExplorableRule> factory);

    /**
     * Enable or disable rule group processing.  Rule groups are, by default, enabled.  However, some projects or users
     * may not be interested in the extra overhead this feature incurs.
     *
     * @param enabled if {@code true}, rule groups processing will be enabled, otherwise it will be disabled.
     */
    void setRulesEnabled(boolean enabled);

    /**
     * Determine whether rule group processing is enabled.
     *
     * @return {@code true} if rule group processing is enabled, or {@code false} if is not.
     */
    boolean areRulesEnabled();

    /**
     * Request a full refresh of all rule groups.
     */
    void requestGroupsRefresh();

    /**
     * Add an explorer delegate event listener.
     * <p/>
     * Please note that explorer delegate listeners are never automatically removed during a TAKX session. It is the
     * responsibility of all callers to this method to also manually call the associated
     * {@link #removeExplorerDelegateListener(IExplorerDelegateListener)} method when appropriate.
     *
     * @param listener listener to add.
     * @since 1.49
     */
    void addExplorerDelegateListener(IExplorerDelegateListener listener);

    /**
     * Remove an explorer delegate event listener.
     *
     * @param listener listener to remove.
     * @since 1.49
     */
    void removeExplorerDelegateListener(IExplorerDelegateListener listener);

    /**
     * Get the configuration string for the explorer delegate.
     *
     * @return A IRestorableMap representing the configuration of the explorer delegate.
     * @since 1.49
     */
    IRestorableMap getRestorableMap();

    /**
     * Given a configuration string, attempt to restore the explorer delegate.
     *
     * @param configuration A IRestorableMap representation of the explorer delegate state.
     * @since 1.49
     */
    void restoreFromMap(IRestorableMap configuration);

    /**
     * Enable / Disable the auto-expanding of specific node names.  Note that the default is to auto expand.
     * Example usage would be to call setAutoExpand("Devices", false) at which point any children being added to the
     * "Devices" parent node would not cause the "Devices" node to auto expand.
     *
     * @param nodeName   name of node to exclude from auto-expand
     * @param autoExpand true / false auto expand
     * @since 1.49
     */
    void setAutoExpand(String nodeName, boolean autoExpand);

    /**
     * Expands the explorer tree so that a given node and its ancestors are visible.
     *
     * @param explorable the explorable to expand to in the tree
     * @since 5.3
     */
    void expandTreeToNode(IExplorable explorable);

    /**
     * Controls whether the explorer view has one or two panes. The single pane view is a tree which will display all explorables.
     * The two pane view will display a tree on the left and a table on the right. The table will display children of the currently
     * selected tree node, and will hide all leaf nodes from the tree.
     *
     * @param singlePane {@code true} if the explorer should be in single-pane mode, or {@code false} for dual-pane mode
     * @since 5.3
     */
    void setSinglePane(boolean singlePane);

    /**
     * @return {@code true} if the panel for this explorer is in single-pane mode, otherwise {@code false}
     * @since 5.3
     */
    boolean isSinglePane();

    /**
     * Opens the selected explorable in the tree and selects it.
     *
     * @param explorable the explorable, or {@code null} to clear tree selection
     * @since 5.3
     */
    void setTreeSelected(@Nullable IExplorable explorable);

    /**
     * Get a collection of the current {@link IExplorable}s.
     *
     * @return Collection of the current explorables.
     * @since 2.0.0
     */
    Collection<IExplorable> getExplorables();

    /**
     * Sets the visibility of the Alerts node in the Explorer UI.
     * <p>
     * If the UI has not been initialized, this method does nothing.
     *
     * @param visible if {@code true}, alarms view is set visible, otherwise it is set invisible.
     */
    void setAlarmsViewVisible(boolean visible);

    /**
     * Sets the visibility of the Groups node in the Explorer UI.
     * <p>
     * If the UI has not been initialized, this method does nothing.
     *
     * @param visible if {@code true}, groups view is set visible, otherwise it is set invisible.
     */
    void setGroupsViewVisible(boolean visible);

    /**
     * Load the explorables from the main explorer tree. Note that this will also cause any future
     * explorables to be added (or removed).
     *
     * @since 2.25
     */
    void loadExplorables();

    /**
     * Create a rule of the given type. This method depends on the factory having already been installed prior to this
     * call. See {@link #addExplorableRuleFactory(Class, IExplorableRuleFactory)}
     *
     * @param ruleClass the class of the rule to be created
     * @return the rule created
     * @since 2.38
     */
    <T extends IExplorableRule> T createRule(Class<T> ruleClass);

    /**
     * Get a collection of the current RuleGroupControllers in this group. Each controller represents a node
     * within the Groups area of this explorer tab.
     *
     * @return the collection of rule group controllers for this explorer tab
     * @since 2.38
     */
    Collection<IRuleGroupController> getRuleGroupsControllers();

    /**
     * Create a RuleGroupsController with the given name for this explorer tab.
     *
     * @param name       the name of the controller
     * @param rulesBased true if this controller should be rules based
     * @return the rule group controller created by this request
     * @deprecated use {@link #createRuleGroup(String)}
     * @since 2.38
     */
    @Deprecated
    @DeprecatedApi(since = "5.7", forRemoval = true, removeAt = "5.10")
    IRuleGroupController createRuleGroupsController(String name, boolean rulesBased);

    /**
     * Get the controller with the given name if it exists.
     *
     * @param name the name for the new controller
     * @return the rule group controller of interest
     * @since 2.38
     */
    Optional<IRuleGroupController> getRuleGroupsController(String name);

    /**
     * Remove the controller from the group
     *
     * @param controller the controller to remove
     * @since 2.38
     */
    void removeRuleGroupsController(IRuleGroupController controller);

    /**
     * Create an {@link IRuleGroupController} with the given name for this explorer tab.
     *
     * @param name the name of the group
     * @return the rule group created by this request
     * @since 5.7
     */
    @Nonnull
    IRuleGroupController createRuleGroup(@Nonnull String name);

    /**
     * Create an {@link IManualExplorableGroup} with the given name for this explorer tab.
     *
     * @param name the name of the group
     * @return the rule group created by this request
     * @since 5.7
     */
    @Nonnull
    IManualExplorableGroup createManualGroup(@Nonnull String name);

    /**
     * Add a component to the border of the explorer tree.
     * <p>
     * This method allows plugins to inject UI components (e.g., {@code JPanel}s or {@code JToolBar}s) into
     * border locations surrounding the explorer tree.
     *
     * @param component            the component to be set for this explorer. May be {@code null}, in which case no component
     *                             will be displayed in the given location.
     * @param borderLayoutLocation A string describing the location of the component. Must be one of {@code BorderLayout.NORTH},
     *                             {@code BorderLayout.SOUTH}, {@code BorderLayout.EAST}, or {@code BorderLayout.WEST}.
     * @throws IllegalArgumentException if {@code borderLayoutComponent} is not one of the listed values
     * @since 2.39
     */
    void addComponentToBorder(Component component, String borderLayoutLocation);

    /**
     * Retrieve the Explorer UI component from this Explorer, initializing it first if necessary.
     * <p>
     * This method allows plugins to embed an Explorer within their own panels. This should only be called if
     * the Explorer was created using {@link IExplorerManagerDelegate#createEmbeddableExplorer(String)}.
     * <p>
     * This method relies on TAKX's UI framework having been initialized and, therefore, must not be called in a
     * headless environment.
     *
     * @return the panel associated with this Explorer
     * @throws IllegalStateException if this method is called in a headless environment
     * @since 2.39
     */
    Component getExplorerUIComponent();

    /**
     * Add a filter to the view component for this explorer. This filter will affect which explorables are visible in the
     * user interface, but will not affect the underlying explorable model.
     *
     * @param filter the Predicate to filter the view by
     * @since 5.4.1
     */
    void addViewFilter(@Nonnull Predicate<IExplorable> filter);

    /**
     * Add a filter to the view component for this explorer. This filter will affect which explorables are visible in the
     * user interface, but will not affect the underlying explorable model.
     *
     * @param parentFilter the Predicate to determine which parent nodes the filter applies to
     * @param filter the Predicate to filter the view by
     * @since 5.4.1
     */
    void addViewFilter(@Nonnull Predicate<IExplorable> parentFilter, @Nonnull Predicate<IExplorable> filter);

    /**
     * Remove a filter from the view component for this explorer.
     *
     * @param filter the Predicate filter to remove
     * @since 5.4.1
     */
    void removeViewFilter(@Nonnull Predicate<IExplorable> filter);
}
