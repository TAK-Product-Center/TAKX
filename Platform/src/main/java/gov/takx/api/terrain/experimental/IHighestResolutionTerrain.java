package gov.takx.api.terrain.experimental;

import gov.takx.api.annotation.Experimental;

import java.util.List;

/**
 * This interface defines a terrain object that uses the best possible data available
 * for calculations.  Configurable options include: which elevation models to include and
 * whether to use the best data available locally or online
 *
 * @since 1.35
 */
@Experimental
public interface IHighestResolutionTerrain extends IImmutableHighestResolutionTerrain
{
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
    void setElevationModelsOfInterest(List<ITakElevationModel> elevationModels);

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
     * @since 1.43
     */
    long getTotalValueCount();

    /**
     * Get the number of invalid elevation values retrieved in terrain lookup.
     *
     * @return number of invalid elevation values retrieved.
     * @since 1.43
     */
    long getInvalidCount();

    /**
     * Get the ratio of invalid value / total value counts.
     *
     * @return ratio of invalid value / total value counts.
     * @since 1.43
     */
    double getInvalidRatio();

    /**
     * Clear counters tracking the number of invalid and total data points used in terrain lookup.
     *
     * @since 1.43
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
     * @since 1.43
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
     * @since 1.43
     */
    boolean isDynamicValueReplacementEnabled();

    /**
     * Enable/disable invalid data tracking.  When enabled, counters track the number of invalid data retrieved from
     * elevation lookups as well as total data retrieved.  The counters may be cleared by calling
     * {@link #clearValueCounts()}.
     *
     * @param enabled enable or disable invalid data tracking.
     * @since 1.43
     */
    void setTrackInvalidDataEnabled(boolean enabled);

    /**
     * Determine invalid data tracking is enabled.  When enabled, counters track the number of invalid data retrieved
     * from elevation lookups as well as total data retrieved.  The counters may be cleared by calling
     * {@link #clearValueCounts()}.
     *
     * @return a flag determining whether invalid data tracking is enabled.
     * @since 1.43
     */
    boolean isTrackInvalidDataEnabled();
}
