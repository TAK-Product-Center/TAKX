package gov.takx.api.explorer;

import gov.takx.api.annotation.Experimental;
import gov.takx.api.annotation.NoPluginImplementations;
import gov.takx.api.shapes.IShape;

/**
 * Explorable rule for matching explorables contained or not contained within a specific geofence.
 *
 * @since 2.38
 */
@NoPluginImplementations
@Experimental
public interface IGeofenceRule extends IExplorableRule
{
    /**
     * Get the shape associated with this rule.
     *
     * @return the IShape
     */
    IShape getSelectedShape();

    /**
     * Set the shape for this rule.
     *
     * @param shape the shape
     */
    void setSelectedShape(IShape shape);

    /**
     * Set the behavior of the filter. Setting this to true will match on map objects which are inside the shape
     * specified.
     *
     * @param insideMode true if the matching should be inside the shape
     */
    void setInsideMode(boolean insideMode);

    /**
     * Is the rule set for inside mode.
     *
     * @return true if inside mode is set
     */
    boolean isInsideMode();
}
