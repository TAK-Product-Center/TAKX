package gov.takx.api.shapes;

import gov.takx.api.annotation.NoPluginImplementations;

/**
 * Interface to a mutable circle shape. See {@link ICircle} for more information.
 */
@NoPluginImplementations
public interface IMutableCircle extends ICircle, IMutableRingedShape
{
    /**
     * Set the radius of the circle
     *
     * @param radius radius of circle
     */
    void setRadius(double radius);
}
