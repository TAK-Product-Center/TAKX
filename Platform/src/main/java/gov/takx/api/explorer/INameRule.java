package gov.takx.api.explorer;

import gov.takx.api.annotation.Experimental;
import gov.takx.api.annotation.NoPluginImplementations;

/**
 * Explorable rule for matching explorables based on their name. The rule will match any explorables whose name contains
 * the specified match string. Matching is non-case-sensitive
 *
 * @since 2.38
 */
@NoPluginImplementations
@Experimental
public interface INameRule extends IExplorableRule
{
    /**
     * Set the string to match against.
     *
     * @param matchString the string
     */
    void setMatchString(String matchString);

    /**
     * Get the match string.
     *
     * @return the string
     */
    String getMatchString();
}
