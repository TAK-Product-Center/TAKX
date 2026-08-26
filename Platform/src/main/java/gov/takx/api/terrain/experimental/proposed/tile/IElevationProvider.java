package gov.takx.api.terrain.experimental.proposed.tile;

import gov.takx.api.annotation.Experimental;

/**
 */
@Experimental
public interface IElevationProvider
{
    /**
     * Get the elevation for a latitude/longitude location (in degrees).  Since lat/lon locations exist in a continuous
     * 2D space and the elevation raster is a non-continuous 2D grid of points, the returned elevation is actually an
     * interpolation of the points nearest the given location.
     *
     * @param latitude  latitude in degrees
     * @param longitude longitude in degrees
     * @return elevation for location
     */
    float getElevation(double latitude, double longitude);

    /**
     * Get the highest resolution for the data available in this elevation provider.
     *
     * @return highest resolution available
     */
    double getHighestResolution();

    /**
     * Determine whether this elevation raster contains a latitude/longitude location (in degrees).
     *
     * @param latitude  latitude in degrees
     * @param longitude longitude in degrees
     * @return {@code true} if this elevation raster contains the location, or {@code false} otherwise.
     */
    boolean contains(double latitude, double longitude);
}
