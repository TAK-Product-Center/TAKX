package gov.takx.api.terrain.experimental.proposed.tile;

import gov.takx.api.annotation.Experimental;

/**
 * Interface for reading elevation tile data.
 *
 * @since 1.43
 */
@Experimental
public interface IElevationTileReader
{
    /**
     * Get a float array with elevation tile data.
     *
     * @return float array buffer to fill with elevation tile data or {@code null} if the reader was unable to
     * read the elevation data.
     */
    float[] readValues();
}
