package gov.takx.api.shapes;

import gov.takx.api.annotation.NoPluginImplementations;

/**
 * Interface to an immutable square shape.
 *
 */
@NoPluginImplementations
public interface ISquare extends IRectangle
{
    /**
     * Get the side length of the square.
     *
     * @return side length
     */
    double getSideLength();
}
