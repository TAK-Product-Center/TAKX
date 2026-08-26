package gov.takx.api.shapes;

import gov.takx.api.annotation.NoPluginImplementations;

/**
 * Interface to a mutable ellipse shape. See {@link IEllipse} for more information.
 */
@NoPluginImplementations
public interface IMutableEllipse extends IEllipse, IMutableRingedShape
{
    /**
     * Set the semi-major axis length of the ellipse
     *
     * @param semiMajorAxis Length of major axis
     */
    void setSemiMajorAxis(double semiMajorAxis);

    /**
     * Set the semi-minor axis length of the ellipse
     *
     * @param semiMinorAxis Length of minor axis
     */
    void setSemiMinorAxis(double semiMinorAxis);

    /**
     * Set both axes of the ellipse.
     *
     * @param semiMajorAxis Length of semi-major axis
     * @param semiMinorAxis Length of semi-minor axis
     */
    void setAxes(double semiMajorAxis, double semiMinorAxis);
}
