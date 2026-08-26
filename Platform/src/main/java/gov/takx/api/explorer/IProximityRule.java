package gov.takx.api.explorer;

import gov.takx.api.annotation.Experimental;
import gov.takx.api.annotation.NoPluginImplementations;

/**
 * Explorable rule for proximity matching. A locatable explorable is selected and any other locatable explorables within
 * a given radius are positively matched.
 *
 * @author Proprietary information subject to the terms of a EULA
 * @since 2.38
 */
@NoPluginImplementations
@Experimental
public interface IProximityRule extends IExplorableRule
{
    /**
     * Set the Explorable object to be in proximity of.
     *
     * @param explorable the explorable
     */
    void setSelectedExplorable(IExplorable explorable);

    /**
     * Get the explorable in this rule.
     *
     * @return the explorable
     */
    IExplorable getSelectedExplorable();

    /**
     * Set the max distance from the explorable within which the match will return true.
     *
     * @param maxDistanceMeters the max distance in meters
     */
    void setMaxDistanceMeters(int maxDistanceMeters);

    /**
     * Get the max distance value.
     *
     * @return the max distance value in meters
     */
    int getMaxDistanceMeters();
}
