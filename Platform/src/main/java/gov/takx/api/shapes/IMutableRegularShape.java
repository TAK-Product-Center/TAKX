package gov.takx.api.shapes;

import gov.takx.api.annotation.NoPluginImplementations;

/**
 * Interface to a mutable regular shape. See {@link IRegularShape} for more information.
 */
@NoPluginImplementations
public interface IMutableRegularShape extends IRegularShape, IMutableShape
{
    /**
     * Set the orientation (azimuth angle of rotation around the center point) of the shape.
     *
     * @param orientation orientation angle
     */
    void setOrientation(double orientation);
}
