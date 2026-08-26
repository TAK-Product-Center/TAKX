package gov.takx.api.shapes;

import gov.takx.api.annotation.NoPluginImplementations;

/**
 * Interface to a mutable square shape. See {@link ISquare} for more information.
 */
@NoPluginImplementations
public interface IMutableSquare extends ISquare, IMutableRectangle
{
    /**
     * Set the length of the sides of the square.
     *
     * @param sideLength Length of the sides
     */
    void setSideLength(double sideLength);
}
