package gov.takx.api.shapes;

import gov.takx.api.annotation.NoPluginImplementations;

/**
 * Interface to an immutable circle shape.
 *
 */
@NoPluginImplementations
public interface ICircle extends IRingedShape
{
    /**
     * @return the radius of the circle
     */
    double getRadius();
}
