package gov.takx.api.terrain.experimental;

import gov.nasa.worldwind.geom.Sector;
import gov.nasa.worldwind.globes.ElevationModel;
import gov.nasa.worldwind.terrain.BasicElevationModel;
import gov.tak.api.annotation.DeprecatedApi;
import gov.takx.api.annotation.Experimental;
import gov.takx.api.coordinatesystem.ILocation;

import java.util.List;

/**
 * This interface is a placeholder until we remove {@link HighestResolutionTerrain}.
 *
 * @since 5.2
 * @deprecated to be removed with HighestResolutionTerrain
 */
@Experimental
@Deprecated(since = "5.5", forRemoval = true)
@DeprecatedApi(since = "5.5", forRemoval = true, removeAt = "5.8")
public interface IHighestResolutionWWTerrain
{
    interface ITerrainWWListener
    {
        void notify(IHighestResolutionWWTerrain terrain, TerrainMessage message, String information);

        void trackResolution(double resolutionInMeters);

        void elevationModelsChanged(List<ElevationModel> newModels);
    }

    /**
     * Set the cache size for the internal terrain geometry cache, in bytes.
     *
     * @param size size of cache in bytes.
     */
    void setGeometryCacheSize(long size);

    /**
     * Set the elevation models of interest.
     *
     * @param elevationModels a list of elevation models to be used in terrain calculations. If {@code null} is provided,
     *                        all available elevation models will be used in terrain calculations.
     */
    void setElevationModelsOfInterest(List<BasicElevationModel> elevationModels);

    /**
     * Enables the use of best (non-optimal) locally available terrain data in calculations in the case that TAKX
     * is unable to retrieve the true highest resolution terrain data.
     *
     * @param enable if {@code true} then best local terrain will be used when necessary or if {@code false},
     *               calculations will fail when unable to retrieve the highest quality terrain data.
     */
    void enableUseBestLocallyAvailableResolution(boolean enable);

    /**
     * Set the timeout (in milliseconds) for terrain data retrieval.
     *
     * @param timeout timeout in milliseconds for terrain data retrieval.
     */
    void setTimeout(long timeout);

    /**
     * Get the total number of elevation values retrieved in terrain lookup.
     *
     * @return total number of elevation values retrieved.
     */
    long getTotalValueCount();

    /**
     * Get the number of invalid elevation values retrieved in terrain lookup.
     *
     * @return number of invalid elevation values retrieved.
     */
    long getInvalidCount();

    /**
     * Get the ratio of invalid value / total value counts.
     *
     * @return ratio of invalid value / total value counts.
     */
    double getInvalidRatio();

    /**
     * Clear counters tracking the number of invalid and total data points used in terrain lookup.
     *
     */
    void clearValueCounts();

    /**
     * Enable/disable dynamic value replacement when invalid data is encountered.  When enabled and an invalid
     * elevation value is retrieved, it is replaced by the minimum value found in nearby (valid) locations.  If none of
     * the nearby locations are valid, 0 is used as the replacement.
     * <p/>
     * When this is disabled, 0 is always uses as the invalid value replacement.
     *
     * @param enabled enable or disable dynamic value replacement.
     */
    void setDynamicValueReplacementEnabled(boolean enabled);

    /**
     * Determine whether dynamic value replacement is enabled.  When enabled and an invalid elevation value is
     * retrieved, it is replaced by the minimum value found in nearby (valid) locations.  If none of
     * the nearby locations are valid, 0 is used as the replacement.
     * <p/>
     * When disabled, 0 is always uses as the invalid value replacement.
     *
     * @return flag determining whether dynamic value replacement is enabled.
     */
    boolean isDynamicValueReplacementEnabled();

    /**
     * Enable/disable invalid data tracking.  When enabled, counters track the number of invalid data retrieved from
     * elevation lookups as well as total data retrieved.  The counters may be cleared by calling
     * {@link #clearValueCounts()}.
     *
     * @param enabled enable or disable invalid data tracking.
     */
    void setTrackInvalidDataEnabled(boolean enabled);

    /**
     * Determine invalid data tracking is enabled.  When enabled, counters track the number of invalid data retrieved
     * from elevation lookups as well as total data retrieved.  The counters may be cleared by calling
     * {@link #clearValueCounts()}.
     *
     * @return a flag determining whether invalid data tracking is enabled.
     */
    boolean isTrackInvalidDataEnabled();

    /**
     * Set the sector of interest for a terrain-based calculation.
     *
     * @param sector the sector of interest.
     */
    void setSectorOfInterest(Sector sector);

    /**
     * Clear cached (geometry and tile) elevation data used in this terrain object.
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
    List<BasicElevationModel> getElevationModelsOfInterest();

    /**
     * Get a list of all available elevation models.
     *
     * @return a list of all available elevation models.
     */
    List<BasicElevationModel> getAllElevationModels();

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
     * Add a terrain listener.
     * <p/>
     * Note: this is an experimental feature and may be removed in the near future.
     *
     * @param listener terrain listener to add
     */
    void addTerrainListener(ITerrainWWListener listener);

    /**
     * Remove a terrain listener.
     * <p/>
     * Note: this is an experimental feature and may be removed in the near future.
     *
     * @param listener terrain listener to remove
     */
    void removeTerrainListener(ITerrainWWListener listener);
}
