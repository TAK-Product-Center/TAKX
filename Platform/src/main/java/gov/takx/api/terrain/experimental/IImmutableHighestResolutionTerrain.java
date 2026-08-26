package gov.takx.api.terrain.experimental;

import com.atakmap.map.elevation.ElevationManager;
import gov.nasa.worldwind.geom.Intersection;
import gov.nasa.worldwind.geom.LatLon;
import gov.nasa.worldwind.geom.Sector;
import gov.nasa.worldwind.terrain.Terrain;
import gov.takx.api.annotation.Experimental;
import gov.takx.api.coordinatesystem.ILocation;

import java.util.Collection;
import java.util.List;
import java.util.Map;

/**
 * This interface defines a terrain object that uses the best possible data available
 * for calculations.  It is not editable by plugins.
 *
 * @since 1.35
 */
@Experimental
public interface IImmutableHighestResolutionTerrain extends Terrain
{
    /**
     * Produce a heightmap of gridded elevation values.
     *
     * @param result an array of {@link ElevationManager.HeightmapParams#numPostsLat} * {@link ElevationManager.HeightmapParams#numPostsLng} doubles to store the result
     * @param gsd the resolution used to bound the queries per `strategy`
     * @param params the heightmap configuration
     * @param strategy the strategy used for heightmap construction
     * @return {@code true} on success, {@code false} if one or more posts are not resolved
     * @since 5.2
     */
    boolean createHeightmap(double[] result, double gsd, ElevationManager.HeightmapParams params, ElevationManager.HeightmapStrategy strategy);

    /**
     * Produce a heightmap of {@code numPostsLng/numPostsLat} gridded elevation values starting from {@code bounds.minX/bounds.minY} inclusive to {@code bounds.maxX/bounds.maxY} inclusive.
     * Results are returned as an array of LLA triples. Altitude results of {@link Double#NaN} indicate that elevation data was not available.
     *
     * @param result an array of {@code numPostsLat * numPostsLng * 3} doubles to store the result
     * @param gsd the target resolution of the result, use {@link Double#MIN_VALUE} to get the highest resolution available
     * @param params the heightmap configuration
     * @since 5.2
     */
    void createHeightmapLLA(double[] result, double gsd, ElevationManager.HeightmapParams params);

    /**
     * Computes the intersections of a collection of line segments with the terrain. The lines are specified by two positions
     * whose altitude field is interpreted according to the specified altitude mode. The returned map contains all the line segments submitted
     * for evaluation along with an array of zero or more intersections points with respect to the passed limit.
     *
     * @param positionLineSegments a collection of {@link PositionLineSegment PositionLineSegments}
     * @param altitudeMode the altitude mode indicating the reference for the altitudes in the {@link PositionLineSegment PositionLineSegments}
     * @param limit a limit of intersection results per-ray to return, or 0 for no limit
     * @return a map of lines to zero or more {@link Intersection} results
     * @since 5.2
     */
    Map<PositionLineSegment, Intersection[]> intersect(Collection<PositionLineSegment> positionLineSegments, int altitudeMode, int limit);

    /**
     * Set the sector of interest for a terrain-based calculation.
     *
     * @param sector the sector of interest.
     * @since 1.43
     */
    void setSectorOfInterest(Sector sector);

    /**
     * Clear cached (geometry and tile) elevation data used in this terrain object.
     *
     * @since 1.43
     */
    void clearCache();

    /**
     * Determine whether the use of best (non-optimal) locally available terrain data in calculations in the case that
     * TAKX is unable to retrieve the highest resolution terrain data.
     *
     * @return if {@code true} then best local terrain will be used when necessary or if {@code false},
     * calculations will fail when unable to retrieve the highest quality terrain data.
     */
    boolean isUsingBestLocallyAvailableResolution();

    /**
     * Get a list of the elevation models of interest.
     *
     * @return a list of the elevation models of interest.
     */
    List<ITakElevationModel> getElevationModelsOfInterest();

    /**
     * Get a list of all available elevation models.
     *
     * @return a list of all available elevation models.
     */
    List<ITakElevationModel> getAllElevationModels();

    /**
     * Determines the best resolution of data in meters available for the sector
     *
     * @param sector Sector of interest.
     * @return Resolution in meters
     */
    double getBestResolutionForSector(Sector sector);

    /**
     * Determines the highest resolution terrain within a bounding box defined locationA and locationB.
     *
     * @param locationA first corner of area bounding box
     * @param locationB opposite corner of area bounding box
     * @return the best resolution attainable for the specified sector, in radians, or {@link Double#MAX_VALUE} if the
     * area does not contain elevation data.
     */
    double getBestResolutionForArea(ILocation locationA, ILocation locationB);

    /**
     * Determines the elevation above sea level in meters at a given location
     *
     * @param latitude  latitude for location
     * @param longitude longitude for location
     * @return elevation above sea level in meters
     */
    double getElevation(double latitude, double longitude);

    /**
     * Determines the elevations above sea level in meters for the given locations
     *
     * @param sector the bounding area of interest
     * @param latlons the list of locations to retrieve
     * @param buffer a buffer to return the results
     * @since 5.2
     */
    void getElevations(Sector sector, List<? extends LatLon> latlons, double[] buffer);

    /**
     * Add a terrain listener.
     * <p/>
     * Note: this is an experimental feature and may be removed in the near future.
     *
     * @param listener terrain listener to add
     */
    void addTerrainListener(ITerrainListener listener);

    /**
     * Remove a terrain listener.
     * <p/>
     * Note: this is an experimental feature and may be removed in the near future.
     *
     * @param listener terrain listener to remove
     */
    void removeTerrainListener(ITerrainListener listener);
}
