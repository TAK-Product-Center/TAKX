package gov.takx.api.explorer;

import gov.takx.api.annotation.Experimental;
import gov.takx.api.annotation.NoPluginImplementations;
import gov.takx.api.state.IRestorableMapSupport;
import jakarta.annotation.Nonnull;

import java.util.List;

/**
 * Interface for a dynamic list of explorer objects which match the list of rules associated with this group.
 *
 * @since 2.38 originally created as a core capability in client in 1.48
 */
@NoPluginImplementations
@Experimental
public interface IRuleGroupController extends IUserExplorableGroup, IRestorableMapSupport
{
    /**
     * Update the controller's display name.
     *
     * @param displayName The new display name.
     * @since 2.22
     */
    void setDisplayName(@Nonnull String displayName);

    /**
     * Get the list of all explorable rules associated with this group.
     *
     * @return list of explorable rules.
     */
    @Nonnull
    List<IExplorableRule> getRules();

    /**
     * @return True if the group contains any rules that rely on location information (i.e. proximity and geofence rules)
     * @since 2.31
     */
    boolean hasLocationSensitiveRules();

    /**
     * @return True if the group contains any rules that rely on the name of the explorable
     * @since 2.36
     */
    boolean hasNameSensitiveRules();

    /**
     * Get the match-all flag.
     *
     * @return the match-all flag value.
     */
    boolean isMatchAll();

    /**
     * Set the match-all flag.
     *
     * @param matchAll a match-all value.
     */
    void setMatchAll(boolean matchAll);

    /**
     * Filter an iterable of explorables into a new list containing only those explorables which meet the match
     * constraints defined by this rule group.
     *
     * @param explorables iterable of explorables.
     * @return list of matching explorables.
     */
    @Nonnull
    List<IExplorable> getMatchingExplorables(@Nonnull Iterable<IExplorable> explorables);

    /**
     * Determine whether an explorable meets the match constraints defined by this rule group.
     *
     * @param explorable explorable of interest.
     * @return {@code true} if the explorable matches, or {@code false} otherwise.
     */
    boolean isMatch(@Nonnull IExplorable explorable);

    /**
     * Add an explorable rule to this group.
     *
     * @param rule explorable rule to add.
     */
    boolean add(@Nonnull IExplorableRule rule);

    /**
     * Remove an explorable rule from this group.
     *
     * @param rule explorable rule to remove.
     * @return {@code true} if the rule was associated with this group and was removed, or {@code false}
     * otherwise.
     */
    boolean remove(@Nonnull IExplorableRule rule);

    /**
     * Remove all rules from this group.
     *
     * @since 5.3
     */
    void clearRules();

    /**
     * Remove a rule from this group via its index in the group's rule list.
     *
     * @param index index of rule to remove.
     * @return the removed rule, or {@code false}
     * otherwise.
     */
    @Nonnull
    IExplorableRule remove(int index);

    /**
     * Get the number of explorer rules in this group.
     *
     * @return the number of explorer rules in this group.
     */
    int getRuleCount();

    /**
     * Notification that an explorable rule factory has been added.  This gives the rule group controller the
     * opportunity to restore any rules that are of the factory's type.
     *
     * @param ruleFactory added rule factory.
     */
    void ruleFactoryAdded(@Nonnull IExplorableRuleFactory<? extends IExplorableRule> ruleFactory);
}
