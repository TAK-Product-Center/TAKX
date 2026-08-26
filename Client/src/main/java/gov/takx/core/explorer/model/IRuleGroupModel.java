package gov.takx.core.explorer.model;

import gov.takx.api.explorer.IExplorableRule;
import gov.takx.api.explorer.IExplorerEventSupport;
import gov.takx.api.state.IRestorableMap;
import jakarta.annotation.Nonnull;

import java.util.Collection;
import java.util.List;

/**
 * @since 1.48
 */
public interface IRuleGroupModel extends IExplorerEventSupport
{
    String MATCH_ALL = "Match All";
    String RULE_ADDED = "Rule Added";
    String RULE_REMOVED = "Rule Removed";
    String RESTORED_RULE_STATE_ADDED = "Restored Rule State Added";
    String RESTORED_RULE_STATE_REMOVED = "Restored Rule State Removed";

    /**
     * @return a collection of properties that this rule group model is sensitive to
     * @since 5.8
     */
    @Nonnull
    Collection<String> getSensitiveProperties();

    /**
     * Get this rule group's list of rules.
     *
     * @return the list of rules.
     */
    @Nonnull
    List<IExplorableRule> getRules();

    /**
     * Determine whether match-all mode is enabled.  If match-all is enabled, then a logical AND is applied across all
     * rules during match calculation.  Otherwise, a logical OR is applied during match (aka match-any mode).
     *
     * @return if {@code true}, match-all mode is enabled, otherwise, match-any mode is enabled.
     */
    boolean isMatchAll();

    /**
     * Enable or disable match-all mode.  If match-all is enabled, then a logical AND is applied across all rules
     * during match calculation.  Otherwise, a logical OR is applied during match (aka match-any mode).
     *
     * @param matchAll if {@code true}, match-all mode will be enabled, otherwise, match-any mode will be enabled.
     */
    void setMatchAll(boolean matchAll);

    /**
     * Add an explorable rule to this group.
     *
     * @param rule rule to add.
     * @return {@code true} if add was successful, or {@code false} otherwise.
     */
    boolean add(@Nonnull IExplorableRule rule);

    /**
     * Remove an explorable rule to this group.
     *
     * @param rule rule to remove.
     * @return {@code true} if remove was successful, or {@code false} otherwise.
     */
    boolean remove(@Nonnull IExplorableRule rule);

    /**
     * Remove an explorable from this group.
     *
     * @param index index of rule to remove.
     * @return the removed explorable
     * @throws IndexOutOfBoundsException if the index was out of bounds
     */
    @Nonnull
    IExplorableRule remove(int index);

    /**
     * Get the number of rules in this group.
     *
     * @return the number of rules in this group.
     */
    int getRuleCount();

    /**
     * @return A list of all the enabled rules on this group
     * @since 2.36
     */
    @Nonnull
    List<IExplorableRule> getEnabledRules();

    /**
     * Add restored state for a rule that can not currently be instantiated.  If a factory for the missing rule type
     * appears at some point in the future, a rule of that type will be instantiated, and the restore state will be
     * applied to it.
     *
     * @param type      type of rule.
     * @param ruleState restored state to apply to rule if it becomes available.
     */
    void addRestoredRuleState(@Nonnull String type, @Nonnull IRestorableMap ruleState);

    /**
     * Get restored state for a rule that has not been instantiated.  See
     * {@link #addRestoredRuleState(String, IRestorableMap)} for more details.  This method will be
     * called when a new explorable rule factory becomes available.
     *
     * @param type   type of rule.
     * @param remove if {@code true}, the returned state will also be removed from the group.
     * @return restored state for an explorable rule.
     */
    @Nonnull
    List<IRestorableMap> getRestoredRuleState(@Nonnull String type, boolean remove);
}
