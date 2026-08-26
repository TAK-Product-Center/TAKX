package gov.takx.core.explorer.controller;

import gov.takx.api.explorer.IExplorable;
import gov.takx.api.explorer.IExplorableGroup;
import gov.takx.api.explorer.IExplorablePathProvider;
import gov.takx.api.explorer.IExplorableRule;
import gov.takx.api.explorer.IExplorableRuleFactory;
import gov.takx.api.explorer.IExplorerDelegateListener;
import gov.takx.api.explorer.IManualExplorableGroup;
import gov.takx.api.explorer.IRuleGroupController;
import gov.takx.api.explorer.IUserExplorableGroup;
import gov.takx.api.platform.IRaptorLifecycleListener;
import gov.takx.core.explorer.common.IControllerExplorable;
import gov.takx.core.explorer.common.IPathExplorableGroup;
import gov.takx.core.explorer.model.IExplorablePersistor;
import gov.takx.core.explorer.model.IExplorerModel;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import java.awt.image.BufferedImage;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Predicate;

/**
 * Interface for the explorer controller.  This provides essential access the explorer's functionality.
 *
 * @since 1.48
 */
@SuppressWarnings("removal")
public interface IExplorerController extends IRaptorLifecycleListener
{
    /**
     * Add a path provider to provide default paths for explorables within this controller.
     *
     * @param pathProvider the path provider
     * @since 5.3
     */
    void addPathProvider(@Nonnull IExplorablePathProvider pathProvider);

    /**
     * Find an existing {@link IManualExplorableGroup} by display name, or (optionally) create a new one with an icon image and add it to 
     * the explorer.
     *
     * @param displayName display name for new explorable group.
     * @param iconImage icon image to use for the explorable group in the explorer.
     * @param create {@code true} if a new group should be created when one is not found, otherwise {@code false}
     * @return explorable group created (or retrieved) from the explorer.
     * @since 5.3
     */
    @Nullable
    IManualExplorableGroup getOrCreateManualGroup(@Nonnull String displayName, @Nullable BufferedImage iconImage, boolean create);

    /**
     * Create an {@link IManualExplorableGroup} with an icon image and add it to the explorer.
     *
     * @param displayName display name for new explorable group.
     * @param iconImage icon image to use for the explorable group in the explorer.
     * @return explorable group created (or retrieved) from the explorer.
     * @since 5.3
     */
    @Nonnull
    IManualExplorableGroup createManualGroup(@Nonnull String displayName, @Nullable String uniqueKey, @Nullable BufferedImage iconImage);

    /**
     * Add an explorable to the explorer.  If an explorable with the same unique key already exists, this method will
     * fail and the new explorable will NOT replace the original.
     * <p>
     * If you wish to replace an explorable, the original must first be removed from this controller.  See
     * {@link #removeExplorable(IExplorable, boolean)} for more information.
     *
     * @param explorable explorable to add.
     * @return {@code true} if the add was successful, or {@code false} if an explorable with the same unique
     * key already exists.
     */
    boolean addExplorable(@Nonnull IExplorable explorable);

    /**
     * Remove an explorable from this controllers model.
     * 
     * @param explorable the explorable to remove
     * @return {@code true} if the explorable was successfully removed, otherwise {@code false}
     * @since 5.3
     */
    boolean removeExplorable(@Nonnull IExplorable explorable);

    /**
     * @return the root Explorable for the controller
     */
    @Nonnull
    IPathExplorableGroup getRoot();

    /**
     * Remove a {@link IControllerExplorable} and optionally confirm.
     *
     * @param explorable the explorable to remove
     * @param confirm {@code true} to ask for confirmation, otherwise {@code false}
     * @since 5.3 (Removed cascade parameter in 5.7)
     */
    void removeExplorable(@Nonnull IControllerExplorable explorable, boolean confirm);

    /**
     * Remove an explorable from the explorer.  Once removed, the explorable's
     * {@link IExplorable#dispose()} method will be called.  Note that if the explorable has
     * children, they will also be removed and disposed.
     *
     * @param explorable         explorable to remove.
     * @param removeEmptyParents if {@code true} then this action will cascade to also remove any parents made
     *                           empty by removing the explorable.
     * @return {@code true} if the explorable was found and removed, or {@code false} if it did not exist in
     * the explorer.
     */
    boolean removeExplorable(@Nonnull IExplorable explorable, boolean removeEmptyParents);

    /**
     * Get a collection of all explorables known to this controller.
     *
     * @return collection of all explorables known to this controller.
     */
    @Nonnull
    Collection<IExplorable> getExplorables();

    /**
     * Get an explorable from the explorer.
     *
     * @param uniqueKey unique key identifier of the explorable to retrieve from the explorer.
     * @return a retrieved explorable matching the unique key identifier, or {@code null} if one was not found.
     */
    @Nullable
    IExplorable getExplorable(@Nonnull String uniqueKey);

    /**
     * Get an explorable using its flat string path.
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
    boolean hasExplorable(@Nonnull String uniqueKey);

    /**
     * Add a rules-based group.
     *
     * @param groupName  name of group to add.
     * @param uniqueKey  Unique key for the group
     * @param iconImage  custom icon image for rule group.
     * @return a rules-based group if one was successfully created, or already existed. This method may return {@code null} if
     * the name is already in use by non-rule-group explorable.
     * @since 5.3
     */
    IRuleGroupController addRuleGroup(@Nonnull String groupName, @Nullable String uniqueKey, @Nonnull List<String> path, @Nullable BufferedImage iconImage);

    /**
     * @return the model used by this controller
     * @since 5.3
     */
    @Nonnull
    IExplorerModel getModel();

    /**
     * @return the root group for instances of {@link gov.takx.api.explorer.IAppExplorable}.
     * @since 5.3
     */
    @Nonnull
    IExplorableGroup getAppsRoot();

    /**
     * TODO TAKX-3282 decouple this
     * @return the internal AlarmsController for this ExplorerController instance
     * @since 5.3
     */
    @Nonnull
    IExplorerAlarmsController getAlarmsController();

    /**
     * @return The predicate that determines if an explorable can be added to an {@link IUserExplorableGroup}.
     * @since 5.3
     */
    @Nonnull
    Predicate<IExplorable> getGroupablePredicate();

    /**
     * @return the explorable that is the parent of all groups
     * @since 5.3
     */
    @Nonnull
    IExplorableGroup getGroupsRoot();

    /**
     * @return the Set of the display names of all instances of {@link IUserExplorableGroup}
     * @since 5.3
     */
    @Nonnull
    Set<String> getGroupNames();

    /**
     * Add an explorable rule factory.
     *
     * @param ruleClass the class of the return type from the factory.
     * @param factory   factory to add.
     * @since 5.3
     */
    <T extends IExplorableRule> void addRuleFactory(@Nonnull Class<T> ruleClass, @Nonnull IExplorableRuleFactory<T> factory);

    /**
     * Remove an explorable rule factory.
     *
     * @param factory factory to remove.
     * @since 5.3
     */
    void removeRuleFactory(@Nonnull IExplorableRuleFactory<? extends IExplorableRule> factory);

    /**
     * Enable or disable rule group logic.
     *
     * @param enabled if {@code true}, rule group logic will be enabled.  If {@code false}, it will be disabled.
     * @since 5.3
     */
    void setRulesEnabled(boolean enabled);

    /**
     * Determine whether rule group logic is enabled.
     *
     * @return if {@code true}, rule group logic is enabled, otherwise it is disabled.
     * @since 5.3
     */
    boolean areRulesEnabled();

    /**
     * Update the given group with the new properties.
     *
     * @param delegate the rule group
     * @param rules the new list of rules
     * @param enabledMap the map of enabled rules
     * @param displayNameMap the map of rule display names
     * @param matchAll the rule matchAll flag, see {@link IRuleGroupController#isMatchAll}
     * @since 5.3
     */
    void updateGroup(@Nonnull IRuleGroupController delegate, @Nonnull List<IExplorableRule> rules, @Nonnull Map<IExplorableRule, Boolean> enabledMap, @Nonnull Map<IExplorableRule, String> displayNameMap, boolean matchAll);

    /**
     * Request a refresh of all rule-based groups.
     * @since 5.3
     */
    void requestGroupsRefresh();

    /**
     * Get the current list of RuleGroupControllers.
     *
     * @return the controller list
     * @since 5.3
     */
    @Nonnull
    List<IRuleGroupController> getRuleGroups();

    /**
     * Get an explorable rule factory for a given rule class.
     *
     * @param ruleClass the Class of the rule that is created by the factory desired.
     * @return matching explorable rule factory, or {@code null} if no matching factory exists.
     * @since 5.3
     */
    @Nullable
    <T extends IExplorableRule> IExplorableRuleFactory<T> getRuleFactory(@Nonnull Class<T> ruleClass);

    /**
     * Get an explorable rule factory for a given type.
     *
     * @param type type of explorable rule factory of interest.
     * @return matching explorable rule factory, or {@code null} if no matching factory exists.
     * @since 5.3
     */
    @Nullable
    IExplorableRuleFactory<? extends IExplorableRule> getRuleFactory(@Nonnull String type);

    /**
     * Get a list of all available explorable rule factories.
     *
     * @return list of all available explorable rule factories.
     * @since 5.3
     */
    @Nonnull
    List<IExplorableRuleFactory<? extends IExplorableRule>> getRuleFactories();

    /**
     * Notification method that a rule group's settings have been updated.  The delegate should refresh the group's
     * contents.
     *
     * @param ruleGroupController rule group with updated settings.
     * @since 5.3
     */
    void ruleGroupUpdated(@Nonnull IRuleGroupController ruleGroupController);

    /**
     * @return all explorables that are eligible to be added to a group.
     * @since 5.3
     */
    @Nonnull
    List<IExplorable> getGroupableExplorables();

    /**
     * A set of nodes being selected or deselected, along with their selection state.
     *
     * @param explorables The set of explorables
     * @param states      The Selected or Unselected state of the explorable.
     * @since 2.29
     */
    void requestSetSelected(@Nonnull List<IExplorable> explorables, @Nonnull List<IExplorerDelegateListener.SelectionState> states);

    /**
     * Request creation of a group.
     *
     * @param name         display name of group to create.
     * @param isRulesGroup if {@code true}, the group should be rules-based.  If {@code false}, it should be
     *                     a standard group.
     */
    @Nonnull
    IUserExplorableGroup addGroup(@Nonnull String name, boolean isRulesGroup, @Nullable BufferedImage icon);

    /**
     * Request setting a group's children.
     *
     * @param group    group of interest.
     * @param children children to set.
     */
    void requestSetChildren(@Nonnull IExplorableGroup group, @Nonnull Collection<IExplorable> children);

    /**
     * Request deletion of an explorable.
     *
     * @param explorable explorable of interest.
     */
    void requestDelete(@Nonnull IExplorable explorable);

    /**
     * Adds an explorable persistor to this model, which will be used to
     * store and restore explorables from a {@link gov.takx.api.state.IRestorableMap}.
     *
     * @param persistor the persistor
     * @since 5.3
     */
    void addExplorablePersistor(@Nonnull IExplorablePersistor persistor);
}
