package gov.takx.api.terrain.experimental.proposed.tile;

import gov.nasa.worldwind.geom.Sector;
import gov.takx.api.annotation.Experimental;

/**
 */
@Experimental
public interface IElevationRaster extends IElevationProvider, IRaster
{
    /**
     * Get this elevation raster's bounding sector.
     *
     * @return the bounding sector for this elevation raster
     */
    Sector getSector();

    /**
     * Determine whether this elevation raster contains a latitude/longitude location (in degrees).
     *
     * @param latitude  latitude in degrees
     * @param longitude longitude in degrees
     * @return {@code true} if this elevation raster contains the location, or {@code false} otherwise.
     */
    boolean contains(double latitude, double longitude);

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
     * Get the elevation for a latitude/longitude location (in degrees).  This method will return the elevation value
     * of the nearest grid point without interpolation.
     *
     * @param latitude  latitude in degrees
     * @param longitude longitude in degrees
     * @return elevation for location
     */
    float getRawElevation(double latitude, double longitude);

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
