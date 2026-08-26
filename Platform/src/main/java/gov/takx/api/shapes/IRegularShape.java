package gov.takx.api.shapes;

import gov.takx.api.annotation.NoPluginImplementations;

/**
 * Interface to an immutable regular shape.  This includes all shapes that are defined by a center point and a set of
 * characteristics (like radius, width, etc.).  Regular shapes also support the notion of an orientation
 * (azimuth) angle.
 *
 */
@NoPluginImplementations
public interface IRegularShape extends IShape
{
    /**
     * Get the orientation (azimuth angle of rotation around the center point) of the shape.
     *
     * @return orientation of shape
     */
    double getOrientation();
}
