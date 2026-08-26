package gov.takx.api.explorer;

import gov.takx.api.state.IRestorableMapSupport;
import jakarta.annotation.Nonnull;

/**
 * Interface for explorer rules.  Explorer rules provide an isMatch() method to determine whether a particular
 * explorer object meets the rule's internal logical constraints. Note that the isMatch will only be called internally
 * when explorables are created or deleted. If the matching should occur at other times then the plugin should capture
 * those times and call {@link IExplorerDelegate#requestGroupsRefresh()} manually.
 *
 * @since 1.48
 */
public interface IExplorableRule extends IExplorerEventSupport, IRestorableMapSupport
{
    /**
     * Property name for explorable rule change events.
     */
    String RULE_CHANGED = "Explorable Rule Changed";

    /**
     * Property name for explorable rule name change events.
     */
    String NAME = "Explorable Rule Display Name";

    /**
     * Property name for explorable rule enabled change events.
     */
    String ENABLED = "Explorable Rule Enabled";

    /**
     * Property name for explorable rule type change events.
     */
    String TYPE = "Explorable Rule Type";

    /**
     * Determines whether an explorer object matches this rule's logical constraints.
     * <p/>
     * Implementation Note: this method may be called on a background thread, so it should be written with
     * thread-safety in mind.  In particular, any members that are mutated by the view component will be modified on
     * the EDT, so access to those members that are also used in this method should be protected by some kind of
     * thread-safe mechanism.
     *
     * @param explorable map object of interest.
     * @return {@code true} if the explorer object matches, or {@code false} otherwise.
     */
    boolean isMatch(IExplorable explorable);

    /**
     * Get this rule's name.
     *
     * @return this rule's name.
     */
    String getType();

    /**
     * Get the display name for this rule.
     *
     * @return the display name for this rule.
     */
    String getDisplayName();

    /**
     * Set the display name ofr this rule.
     *
     * @param name the display name for this rule.
     */
    void setDisplayName(String name);

    /**
     * Each rule is responsible for providing their own user interface to adjust this rule's parameters.
     */
    @Nonnull
    IExplorableRuleViewComponent createViewComponent();

    /**
     * Determine whether this rule is enabled.
     *
     * @return {@code true} if this rule is enabled, or {@code false} otherwise.
     */
    boolean isEnabled();

    /**
     * Enable or disable this rule.
     *
     * @param enabled {@code true} if this rule is to be enabled, or {@code false} otherwise.
     */
    void setEnabled(boolean enabled);
}
