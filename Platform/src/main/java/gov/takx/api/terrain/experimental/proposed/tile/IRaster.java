package gov.takx.api.terrain.experimental.proposed.tile;

import gov.takx.api.annotation.Experimental;

/**
 */
@Experimental
public interface IRaster
{
    /**
     * Get the elevation grid values in an array of floats.
     *
     * @return elevation grid values
     */
    float[] getValues();

    /**
     * Get the elevation grid value for an x,y coordinate.
     *
     * @param x x-coordinate
     * @param y y-coordinate
     * @return elevation grid value for coordinate
     */
    float getValue(int x, int y);

    int getWidth();

    int getHeight();
}
