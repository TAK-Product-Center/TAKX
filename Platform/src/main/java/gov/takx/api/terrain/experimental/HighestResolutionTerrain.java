package gov.takx.api.terrain.experimental;

import gov.nasa.worldwind.exception.WWRuntimeException;
import gov.nasa.worldwind.geom.Extent;
import gov.nasa.worldwind.geom.LatLon;
import gov.nasa.worldwind.geom.Line;
import gov.nasa.worldwind.geom.Position;
import gov.nasa.worldwind.geom.Sector;
import gov.nasa.worldwind.geom.Vec4;
import gov.nasa.worldwind.globes.Earth;
import gov.nasa.worldwind.globes.ElevationModel;
import gov.nasa.worldwind.globes.Globe;
import gov.nasa.worldwind.terrain.BasicElevationModel;
import gov.nasa.worldwind.terrain.HighResolutionTerrain;
import gov.tak.api.annotation.DeprecatedApi;
import gov.takx.api.annotation.Experimental;
import gov.takx.api.coordinatesystem.ILocation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.invoke.MethodHandles;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Extends HighResolutionTerrain to enable the use of best available terrain resolution when the target resolution
 * is not available.
 *
 * @since 1.35
 * @deprecated to be removed
 */
@Experimental
@Deprecated(since = "5.5", forRemoval = true)
@DeprecatedApi(since = "5.5", forRemoval = true, removeAt = "5.8")
public class HighestResolutionTerrain extends HighResolutionTerrain implements IHighestResolutionWWTerrain
{
    public static final String MISSING_SECTOR_ELEVATION = "Unable to retrieve elevation for sector";
    private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());
    private static final double HEIGHT_OF_MT_EVEREST = Earth.ELEVATION_MAX;
    private static final double DEPTH_OF_MARIANAS_TRENCH = Earth.ELEVATION_MIN;
    private static final int LOWEST_USABLE_RESOLUTION = 40;

    private final Set<ITerrainWWListener> terrainListeners;
    private final TerrainHelper elevationHelper;
    private final double globeRadius;
    private final Map<Long, Boolean> tileHashes = new ConcurrentHashMap<>();
    private boolean useBestLocallyAvailableResolution = false;
    private List<BasicElevationModel> elevationModelsOfInterest = null;

    // LocalDataBasicElevationModel only manages local elevation data!
    private List<LocalDataBasicElevationModel> localDataModelsOfInterest = null;

    protected final AtomicLong totalValueCount;
    protected final AtomicLong invalidValueCount;
    protected final Set<Integer> sectorHashes;
    protected final AtomicBoolean dynamicValueReplacementEnabled;
    protected final AtomicBoolean trackInvalidDataEnabled;

    public HighestResolutionTerrain(Globe globe, Double targetResolution)
    {
        super(globe, targetResolution);
        enableUseBestLocallyAvailableResolution(false);
        globeRadius = globe.getRadius();
        elevationHelper = new TerrainHelper();
        terrainListeners = Collections.newSetFromMap(new ConcurrentHashMap<ITerrainWWListener, Boolean>());
        setTimeout(25000L);
        totalValueCount = new AtomicLong(0);
        invalidValueCount = new AtomicLong(0);
        sectorHashes = new HashSet<>();
        dynamicValueReplacementEnabled = new AtomicBoolean(true);
        trackInvalidDataEnabled = new AtomicBoolean(true);
        reset();
    }

    @Override
    public synchronized void addTerrainListener(ITerrainWWListener listener)
    {
        terrainListeners.add(listener);
    }

    @Override
    public synchronized void removeTerrainListener(ITerrainWWListener listener)
    {
        terrainListeners.remove(listener);
    }

    @Override
    public void clearCache()
    {
        logger.debug("clearing Highest Resolution Terrain geometry cache");
        geometryCache.clear();
        System.gc();
    }

    @Override
    public void setGeometryCacheSize(long size)
    {
        size = Math.max(size, (long) 1e6);
        long lowWater = size / 2L;

        logger.debug("Setting Highest Resolution Terrain geometry cache capacity to: {}, and low water value to: {}", size, lowWater);
        geometryCache.setCapacity(size);
        geometryCache.setLowWater(lowWater);
    }

    public void reset()
    {
        setSectorOfInterest(null);
    }

    @Override
    public double getBestResolutionForSector(Sector sector)
    {
        return getGlobe().getElevationModel().getBestResolution(sector) * globeRadius;
    }

    @Override
    public double getElevation(double latitude, double longitude)
    {
        LatLon latLon = LatLon.fromDegrees(latitude, longitude);
        final Double elevation = getElevation(latLon);
        if (elevation == null)
        {
            String information = "Unable to retrieve elevation for location: " + latLon;
            notifyListeners(TerrainMessage.LOCATION_MISSING, information);
            throw new WWRuntimeException(information);
        }
        return elevation;
    }

    @Override
    public double getBestResolutionForArea(ILocation locationA, ILocation locationB)
    {
        final Position positionA = Position.fromDegrees(locationA.getLatitude(), locationA.getLongitude());
        final Position positionB = Position.fromDegrees(locationB.getLatitude(), locationB.getLongitude());
        return getBestResolutionForSector(Sector.boundingSector(positionA, positionB));
    }

    @Override
    public Vec4 getSurfacePoint(Position position)
    {
        Vec4 v = super.getSurfacePoint(position);
        if (v == null)
        {
            String description = "Unable to to retrieve surface point for position: " + position;
            notifyListeners(TerrainMessage.LOCATION_MISSING, description);
            throw new WWRuntimeException(description);
        }
        return v;
    }

    /**
     * Get the total number of elevation values retrieved in terrain lookup.
     *
     * @return total number of elevation values retrieved.
     * @since 1.43
     */
    @Override
    public long getTotalValueCount()
    {
        return totalValueCount.get();
    }

    /**
     * Get the number of invalid elevation values retrieved in terrain lookup.
     *
     * @return number of invalid elevation values retrieved.
     * @since 1.43
     */
    @Override
    public long getInvalidCount()
    {
        return invalidValueCount.get();
    }

    /**
     * Get the ratio of invalid value / total value counts.
     *
     * @return ratio of invalid value / total value counts.
     * @since 1.43
     */
    @Override
    public double getInvalidRatio()
    {
        double total = getTotalValueCount();
        return total > 0 ? getInvalidCount() / total : 0;
    }

    /**
     * Clear counters tracking the number of invalid and total data points used in terrain lookup.
     *
     * @since 1.43
     */
    @Override
    public synchronized void clearValueCounts()
    {
        totalValueCount.set(0);
        invalidValueCount.set(0);
        sectorHashes.clear();
    }

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
    @Override
    public void setDynamicValueReplacementEnabled(boolean enabled)
    {
        dynamicValueReplacementEnabled.set(enabled);
    }

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
    @Override
    public boolean isDynamicValueReplacementEnabled()
    {
        return dynamicValueReplacementEnabled.get();
    }

    /**
     * Enable/disable invalid data tracking.  When enabled, counters track the number of invalid data retrieved from
     * elevation lookups as well as total data retrieved.  The counters may be cleared by calling
     * {@link #clearValueCounts()}.
     *
     * @param enabled enable or disable invalid data tracking.
     * @since 1.43
     */
    @Override
    public void setTrackInvalidDataEnabled(boolean enabled)
    {
        trackInvalidDataEnabled.set(enabled);
    }

    /**
     * Determine invalid data tracking is enabled.  When enabled, counters track the number of invalid data retrieved
     * from elevation lookups as well as total data retrieved.  The counters may be cleared by calling
     * {@link #clearValueCounts()}.
     *
     * @return a flag determining whether invalid data tracking is enabled.
     * @since 1.43
     */
    @Override
    public boolean isTrackInvalidDataEnabled()
    {
        return trackInvalidDataEnabled.get();
    }

    @Override
    public synchronized void setSectorOfInterest(Sector sector)
    {
        double resolution = TerrainUtils.getBestIntersectingResolution(getElevationModelsOfInterest(), sector);
        if (logger.isDebugEnabled())
        {
            logger.debug("Terrain resolution set to: {}", String.format("%3.3f", resolution * globeRadius));
        }

        targetResolution = resolution;
        computeDimensions();

        tileHashes.clear();
        elevationHelper.clearListeners();
        clearValueCounts();
    }

    @Override
    public synchronized void enableUseBestLocallyAvailableResolution(boolean enable)
    {
        if (useBestLocallyAvailableResolution != enable)
        {
            clearCache();
        }
        useBestLocallyAvailableResolution = enable;
    }

    @Override
    public void setTimeout(long timeout)
    {
        super.setTimeout(timeout);
    }

    @Override
    public synchronized boolean isUsingBestLocallyAvailableResolution()
    {
        return useBestLocallyAvailableResolution;
    }

    @Override
    public List<BasicElevationModel> getAllElevationModels()
    {
        return new ArrayList<>(TerrainUtils.splitModels(globe.getElevationModel()));
    }

    public boolean isElevationResolutionUsable(double resolutionInMeters)
    {
        return resolutionInMeters < LOWEST_USABLE_RESOLUTION;
    }

    /**
     * Get a list of the current elevation models of interest.  I.e. those that are being used in the current
     * calculation.  If the member elevationModelsOfInterest is {@code null}, then this method will return all
     * BasicElevationModels available in the globe.
     *
     * @return a list of the elevation models of interest.
     */
    @Override
    public synchronized List<BasicElevationModel> getElevationModelsOfInterest()
    {
        List<BasicElevationModel> emList;
        if (elevationModelsOfInterest == null) // || elevationModelsOfInterest.isEmpty())
        {
            // Get a list of all the elevation models in the globe (by splitting the compound model).
//            emList = getAllElevationModels();
            emList = new ArrayList<>();
        } else
        {
            // Re-create the BasicElevationModels as HighestResolutionTerrainBasicElevationModels
            emList = new ArrayList<>(elevationModelsOfInterest);
        }
        return emList;
    }

    @Override
    public synchronized void setElevationModelsOfInterest(List<BasicElevationModel> elevationModels)
    {
        elevationModelsOfInterest = elevationModels;
        updateLocalDataModelsOfInterest();
        for (ITerrainWWListener listener : terrainListeners)
        {
            listener.elevationModelsChanged(new ArrayList<>(elevationModels));
        }
        logger.debug("elevationModelsOfInterest set to: {}.  (If null, then all models will be used)", elevationModelsOfInterest);
    }

    /**
     * This is the 'main' overridden method from WW's HighResolutionTerrain.  It attempts to guarantee consistent
     * elevations with the best data available.  If the system is network enabled, this method will loop until it gets
     * all of the data it can from disk and the network.  Otherwise, a timeout limit will be reached and an exception
     * will be thrown.
     * <p/>
     * The basic approach is to determine the best resolution layer and gather all elevation data from that layer.  If
     * two or more layers share the same resolution, the 'best' is chosen from the alphabetical ordering of the layer
     * names.  In the case that the best layer is permanently missing some data, such as an elevation cache that does
     * not cover the current, the next-best-available layer will be used instead.  The next time this method is
     * called with a different sector, it will again attempt to use elevation data from the best layer.
     * <p/>
     * Alternatively, if the 'useBestLocallyAvailableResolution' flag is enabled, this method will simply use the best data
     * locally available.  If no new data is downloaded between calculations, the elevations should be consistent
     * between every call to this method.
     *
     * @param sector            the current sector within our region of interest.
     * @param latLons           locations within the sector to get elevation data.
     * @param targetResolutions expected resolution of elevation data.
     * @param elevations        buffer to hold elevation data.
     * @throws InterruptedException
     */
    @Override
    protected void getElevations(Sector sector,
                                 List<LatLon> latLons,
                                 double[] targetResolutions,
                                 double[] elevations)
            throws InterruptedException
    {
        /**
         * If the use best available resolution flag is true, then we will use the best elevation data available on
         * disk.  No attempts to download new data will be made.  This mode is used when the network is not available
         * and the elevation models haven't cached all of their highest resolution data at the provided sector.
         */
        if (isUsingBestLocallyAvailableResolution())
        {
            double bestLocal = getBestLocalElevations(sector, latLons, elevations);
            logger.debug("Using best local available elevation resolution: {}", bestLocal);
            clipToMinimumResolution(elevations);
            return;
        }

        fillBufferWithInvalidSentinel(elevations);

        double targetResolution = Double.MAX_VALUE;
        for (double t : targetResolutions)
        {
            if (t < targetResolution)
            {
                targetResolution = t;
            }
        }

        /**
         * Update target resolution every time this method is called
         * @since 1.43
         */
        targetResolution = getUpdatedTargetResolution(targetResolution, null, sector);

        // Ignore models set is a local set of elevation models that must be ignored in order for this method to
        // complete successfully.  Elevation models are automatically added to this list whenever the any of its tiles
        // under the sector of interest are determined to be absent/unavailable.
        Set<BasicElevationModel> ignoreModels = new HashSet<>();
        int lastIgnoreCount = 0;
        double actualResolution = Double.MAX_VALUE;

        // The 'main' loop for this method, which iterates until the doGetElevations method fills the elevation
        // buffer with satisfactorily high resolution data.
        while (actualResolution > targetResolution)
        {
            actualResolution = doGetElevations(sector, latLons, targetResolution, elevations, ignoreModels);

            // Detect whether an elevation model was added to the ignore list and update the target resolution
            if (lastIgnoreCount != ignoreModels.size())
            {
                lastIgnoreCount = ignoreModels.size();

                // The local subset of elevation models of interest has changed, so update the target resolution.
                double oldResolution = targetResolution;
                targetResolution = getUpdatedTargetResolution(targetResolution, ignoreModels, sector);

                // Notify listeners that a lower-resolution terrain will be used in this
                // calculation.  If this warning dialog has already been displayed during this calculation, it will
                // not be displayed again.
                // Only display this warning if it hasn't been displayed for this calculation already, and the new
                // resolution is lower quality.  (Larger resolution values = lower quality)
                if (targetResolution > oldResolution)
                {
                    notifyListeners(TerrainMessage.LOW_RESOLUTION, "");
                }
            }

            if (actualResolution <= targetResolution)
            {
                break;
            }

            // Give the system a chance to retrieve data from the disk cache or the server. Also catches interrupts
            // and throws interrupt exceptions.
            Thread.sleep(2);

            // Check for timeout before the next iteration.  If the timeout has been reached, throw an exception.
            Long timeout = getTimeout();
            if (startTime.get() != null && timeout != null)
            {
                if (System.currentTimeMillis() - startTime.get() > timeout)
                {
                    String information = "Terrain convergence timed out";
                    notifyListeners(TerrainMessage.RETRIEVAL_TIMEOUT, information);
                    throw new WWRuntimeException(information);
                }
            }
        }

        validateBuffer(sector, elevations);

        trackResolution(actualResolution);
        clipToMinimumResolution(elevations);
    }

    /**
     * todo: it is unclear what will happen if a RectTile sector (the sector that is passed into this method) crosses
     * todo: a boundary between different sector resolution limits in the best-available-model.  it isn't even clear
     * todo: whether that (can occur)/(has actually occurred) in practice.  if it were to happen, i believe that the
     * todo: elevation lookup would timeout while waiting for the lower-res locations to satisfy the resolution
     * todo: criteria (this kind of problem is fairly easy to reproduce when only using the parent class -
     * todo: HighResolutionTerrain).  Once timeout occurs, the 'use best available resolution' mode should still work,
     * todo: but it wouldn't attempt to download any missing data from the elevation server.
     * <p/>
     * Largely recreates the code found in WW's CompoundElevationModel.doGetElevations method.  The difference is that
     * the layers are first sorted (in ascending order) by resolution at the given sector.  If two layers have the
     * same resolution, then the tie will be broken by comparing their String names; which is an attempt to keep
     * the method deterministic (though it can still be non-deterministic if two layers share the same resolution and
     * the same name).
     *
     * @param sector           the sector in question.
     * @param latLons          the locations to return elevations for. If a location is {@code null}, the output buffer for that
     *                         location is not modified.
     * @param targetResolution the desired horizontal resolution, in radians, of the raster or other elevation sample
     *                         from which elevations are drawn. (To compute radians from a distance, divide the distance
     *                         by the radius of the globe, ensuring that both the distance and the radius are in the
     *                         same units.)
     * @param buffer           an array in which to place the returned elevations. The array must be pre-allocated and
     *                         contain at least as many elements as the list of locations.
     * @param ignoredModels    a set of models to ignore due to missing data.  This set is updated by the
     *                         elevationHelper.requestMissingElevations method.
     * @return the resolution achieved, in radians, or {@link Double#MAX_VALUE} if individual elevations cannot be
     * determined for all of the locations.
     */
    private double doGetElevations(Sector sector,
                                   List<LatLon> latLons,
                                   double targetResolution,
                                   double[] buffer,
                                   Set<BasicElevationModel> ignoredModels)
            throws InterruptedException
    {
        // Default resolutionAchieved to the worst possible resolution.
        double resolutionAchieved = Double.MAX_VALUE;

        List<BasicElevationModel> emList = getElevationModelsOfInterest();

        // Remove models on the ignored list
        emList.removeAll(ignoredModels);

        // Sort the models by resolution, from lowest to highest - at the given sector.
        TerrainUtils.sortModelsByResolution(emList, sector);
        if (logger.isTraceEnabled()) logModelResolution(emList, sector);

        // Get the 'best' model from the sorted list of models.  The best model is the first model at the end of the
        // sorted list which contains the sector in question.  This is an optimization to
        BasicElevationModel bestModel = TerrainUtils.getBestContainingModel(sector, emList);
        if (bestModel == null)
        {
            String information = MISSING_SECTOR_ELEVATION + ": " + sector.toString();
            notifyListeners(TerrainMessage.REGION_MISSING, information);
            throw new InterruptedException(information);
        }

        // Use the utility method to request any missing elevations.  If there are missing elevations,
        // then the resolution variable will not be updated (nor will the buffer).
        TerrainHelper.TerrainHelperListener listener = new TerrainHelper.TerrainHelperListener();
        elevationHelper.addListener(listener);
        if (!bestModel.isEnabled())
        {
            logger.debug("Elevation model: {} is not enabled.", bestModel);
            return Double.MAX_VALUE;
        }

        int c = bestModel.intersects(sector);
        if (c < 0) // no intersection
        {
            logger.debug("Elevation model: {} does not intersect sector: {}", bestModel, sector);
            return Double.MAX_VALUE;
        }

        // Determines if any tiles are missing from memory, and if so, requests that WW download/load them.  This method
        // returns true if any tiles were missing, which forces this method to return until all necessary tiles
        // are in memory.
        if (elevationHelper.requestMissingElevations(bestModel, sector, targetResolution, listener, ignoredModels))
        {
            elevationHelper.removeListener(listener);
            return Double.MAX_VALUE;
        }

        // Get the actual elevation data.
        double r = lookUpElevations(bestModel, sector, latLons, targetResolution, buffer);

        // resolutionAchieved can only be updated if the current elevation model has the necessary data in memory.
        if (r < resolutionAchieved)
        {
            resolutionAchieved = r;
        }

        // If the listener has a different size (number of tiles) after filling the buffer, then some tiles were
        // removed from the in-memory cache during the elevation query, which could lead to inconsistent results.
        // In this case, return the worst possible resolution value.
        if (!listener.isValid())
        {
            logger.debug("In-memory cache missing data detected.");
            elevationHelper.removeListener(listener);
            return Double.MAX_VALUE;
        }

        elevationHelper.removeListener(listener);
        return resolutionAchieved;
    }

    // Note that this method (and the methods it calls) do not have timeouts.  Determining the best local resolution
    // for a list of locations may take some time, but the methods will eventually complete.
    protected double getBestLocalElevations(Sector sector,
                                            List<LatLon> latLons,
                                            double[] elevations)
            throws InterruptedException
    {
        fillBufferWithInvalidSentinel(elevations);

        // Extract all individual elevation models from the globe
        List<LocalDataBasicElevationModel> selectedModels = getLocalDataElevationModelsOfInterest();

        TerrainUtils.ElevationKey[] elevationKeys = elevationHelper.getBestLocalResolutions(
                selectedModels,
                sector,
                latLons,
                tileHashes);

        double worst = Double.MIN_VALUE;

        // Ensure that all elevation keys are valid, otherwise throw an exception.
        for (TerrainUtils.ElevationKey key : elevationKeys)
        {
            if (key.model == null || key.key == null)
            {
                String information = MISSING_SECTOR_ELEVATION + ": " + sector.toString();
                notifyListeners(TerrainMessage.REGION_MISSING, information);
                throw new InterruptedException(information);
            }
            double resolution = key.resolution;
            if (resolution > worst)
            {
                worst = resolution;
            }
        }

        /**
         * Determine whether the worst elevation resolution is better than 40m (the minimum acceptable resolution).
         */
        if (!isElevationResolutionUsable(worst * globeRadius))
        {
            notifyListeners(TerrainMessage.UNUSABLE_RESOLUTION, "");
        }

        trackResolutions(elevationKeys);

        /**
         * If all the model keys are identical, we can look up all the elevation values at the same time.
         */
        if (areModelKeysEqual(elevationKeys))
        {
            while (doGetAllBestLocalElevations(sector, latLons, elevations, elevationKeys[0]) >= Double.MAX_VALUE)
            {
                Thread.sleep(2);
            }
        }
        /**
         * The model keys are different, so look up each elevation value individually, according to its associated
         * model key.
         */
        else
        {
            while (doGetIndividualBestLocalElevations(sector, latLons, elevations, elevationKeys) >= Double.MAX_VALUE)
            {
                Thread.sleep(2);
            }
        }

        validateBuffer(sector, elevations);
        return worst;
    }

    /**
     * To be called AFTER an elevation buffer has been filled by an elevation model.  This method checks the buffer for
     * the sentinel value (NaN) which will indicate the elevation model did not have valid data for the location in
     * the buffer.
     * <p/>
     * Note: fillBufferWithInvalidSentinel() should be called BEFORE the buffer has been filled by the elevation model.
     * <p/>
     *
     * @param sector sector of interest.
     * @param buffer buffer of interest.
     * @return {@code true} if the buffer was full of valid values, or {@code false} otherwise.
     */
    protected boolean validateBuffer(Sector sector, double[] buffer)
    {
        /**
         * Keep a running count of the invalid values and total values.
         */
        trackValues(sector, buffer);

        /**
         * Determine whether there are any invalid values in the buffer.  This is handled separately from trackValues()
         * because that method's code may be skipped if we have already seen this Sector before.
         */
        boolean success = isValid(buffer);

        if (!success)
        {
            /**
             * Create the replacement value from the buffer.
             */
            double invalidDataReplacement = createReplacementValue(buffer);

            /**
             * Iterate through the buffer and overwrite invalid values with the replacement.
             */
            for (int i = 0; i < buffer.length; i++)
            {
                double d = buffer[i];
                if (Double.isNaN(d))
                {
                    // Go ahead and replace the invalid data with the replacement value.
                    buffer[i] = invalidDataReplacement;
                }
            }

            String message = "Invalid data found in buffer for sector=" + sector;
            logger.debug(message);
            notifyListeners(TerrainMessage.INVALID_DATA, message);
        }

        return success;
    }

    /**
     * Determine whether all values in the given buffer are valid (i.e. they are not NaNs).
     *
     * @param buffer buffer to validate.
     * @return {@code true} if all values are valid, or {@code false} if one or more values are invalid
     * (NaN).
     * @since 1.43
     */
    protected boolean isValid(double[] buffer)
    {
        for (double d : buffer)
        {
            if (Double.isNaN(d))
            {
                return false;
            }
        }
        return true;
    }

    /**
     * Keep a running count of the number of invalid and total elevation values.
     *
     * @param sector sector of current buffer.  This is used to prevent addition of the same buffer multiple times.
     * @param buffer buffer of elevation values.
     * @since 1.43
     */
    protected void trackValues(Sector sector, double[] buffer)
    {
        /**
         * If invalid data tracking is not enabled, skip this method.
         */
        if (!trackInvalidDataEnabled.get())
        {
            return;
        }

        /**
         * Before doing anything else, use the sector's hash to determine whether we have already seen this region.
         * If we have, skip this method.
         */
        int sectorHash = sector.hashCode();
        synchronized (this)
        {
            if (sectorHashes.contains(sectorHash))
            {
                return;
            }
            sectorHashes.add(sectorHash);
        }

        int invalid = 0;
        for (double d : buffer)
        {
            if (Double.isNaN(d))
            {
                invalid++;
            }
        }

        totalValueCount.getAndAdd(buffer.length);
        invalidValueCount.getAndAdd(invalid);
    }

    /**
     * Create a replacement value for invalid data in this buffer.  The buffer is iterated, and the minimum VALID value
     * is tracked.  If a minimum was found, this is returned.  If no valid values were found, 0 is returned.
     *
     * @param buffer buffer for which to create a replacement value.
     * @return the min valid value of the buffer if valid values exist in it, or 0 otherwise.
     * @since 1.43
     */
    protected double createReplacementValue(double[] buffer)
    {
        /**
         * If dynamic value replacement is disabled, return the default 0 elevation replacement value.
         */
        if (!dynamicValueReplacementEnabled.get())
        {
            return 0d;
        }

        Double replacement = null;
        for (double d : buffer)
        {
            if (!Double.isNaN(d))
            {
                if (replacement == null || d < replacement)
                {
                    replacement = d;
                }
            }
        }

        /**
         * The replacement is the min valid value in the buffer.  If no valid values were found, 0 is the replacement.
         */
        return replacement != null ? replacement : 0d;
    }

    /**
     * Using a single ElevationKey, look up all elevation values for each lat/lon provided at the same time.  This may
     * be used when all of the lat/lon values share the same elevation model and resolution.
     *
     * @param sector       the sector in question
     * @param latLons      a list of lat/lon values
     * @param buffer       array in which to store the elevation values
     * @param elevationKey information on where to find the elevation data
     * @return a value representing the best resolution elevation found
     */
    private double doGetAllBestLocalElevations(Sector sector,
                                               List<LatLon> latLons,
                                               double[] buffer,
                                               TerrainUtils.ElevationKey elevationKey)
            throws InterruptedException
    {
        // Use the utility method to request any missing elevations.  If there are missing elevations,
        // then the resolution variable will not be updated (nor will the buffer).
        TerrainHelper.TerrainHelperListener listener = new TerrainHelper.TerrainHelperListener();
        elevationHelper.addListener(listener);

        /**
         * Instead of asynchronous tile requests, simply load the tile data on this thread.  If unsuccessful, then
         * break out of the thread execution with an interrupted exception.
         */
        if (!elevationKey.model.loadTile(elevationKey.key))
        {
            elevationHelper.removeListener(listener);
            String information = "Unable to retrieve local tile data for: " + elevationKey.key;
            notifyListeners(TerrainMessage.REGION_MISSING, information);
            throw new InterruptedException(information);
        }

        double resolutionAchieved = Double.MAX_VALUE;

        // Fill the buffer with ElevationModel contents from highest resolution elevation model.
        double resolution = elevationKey.resolution;

        double r = lookUpElevations(elevationKey.model, sector, latLons, resolution, buffer);

        if (r < resolutionAchieved)
        {
            resolutionAchieved = r;
        }

        // If the listener has a different size (number of tiles) after filling the buffer, then some tiles were
        // removed from the in-memory cache during the elevation query, which could lead to inconsistent results.
        // In this case, return the worst possible resolution value.
        if (!listener.isValid())
        {
            elevationHelper.removeListener(listener);
            return Double.MAX_VALUE;
        }
        elevationHelper.removeListener(listener);
        return resolutionAchieved;
    }

    /**
     * Using an array of ElevationKeys, look up individual elevation values for each lat/lon provided.  This should be used
     * when different elevation models and resolutions are used with each provided lat/lon value.
     *
     * @param sector        the sector in question
     * @param latLons       a list of lat/lon values
     * @param buffer        array in which to store the elevation values
     * @param elevationKeys array of information on where to find the elevation data
     * @return a value representing the best resolution elevation found
     */
    private double doGetIndividualBestLocalElevations(Sector sector,
                                                      List<LatLon> latLons,
                                                      double[] buffer,
                                                      TerrainUtils.ElevationKey[] elevationKeys)
            throws InterruptedException
    {
        // Use the utility method to request any missing elevations.  If there are missing elevations,
        // then the resolution variable will not be updated (nor will the buffer).
        TerrainHelper.TerrainHelperListener listener = new TerrainHelper.TerrainHelperListener();
        elevationHelper.addListener(listener);

        /**
         * Instead of asynchronous tile requests, simply load the tile data on this thread.  If unsuccessful, then
         * break out of the thread execution with an interrupted exception.
         */
        for (TerrainUtils.ElevationKey elevationKey : elevationKeys)
        {
            if (!elevationKey.model.loadTile(elevationKey.key))
            {
                elevationHelper.removeListener(listener);
                String information = "Unable to retrieve local tile data for: " + elevationKey.key;
                notifyListeners(TerrainMessage.REGION_MISSING, information);
                throw new InterruptedException(information);
            }
        }

        double resolutionAchieved = Double.MAX_VALUE;

        // Fill the buffer with ElevationModel contents from lowest resolution to highest, potentially overwriting
        // values at each step. ElevationModels are expected to leave the buffer untouched for locations outside their
        // coverage area.
        for (int i = 0; i < elevationKeys.length; i++)
        {
            BasicElevationModel em = elevationKeys[i].model;
            double resolution = elevationKeys[i].resolution;
            if (!em.isEnabled())
            {
                continue;
            }

            int c = em.intersects(sector);
            if (c < 0) // no intersection
            {
                continue;
            }

            double[] miniBuffer = new double[1];

            double r = lookUpElevations(em, sector, Arrays.asList(latLons.get(i)), resolution, miniBuffer);

            // resolutionAchieved can only be updated if the current elevation model has the necessary data in memory.
            if (r < resolutionAchieved || resolutionAchieved == 0)
            {
                resolutionAchieved = r;
            }
            buffer[i] = miniBuffer[0];
        }

        // If the listener has a different size (number of tiles) after filling the buffer, then some tiles were
        // removed from the in-memory cache during the elevation query, which could lead to inconsistent results.
        // In this case, return the worst possible resolution value.
        if (!listener.isValid())
        {
            elevationHelper.removeListener(listener);
            return Double.MAX_VALUE;
        }
        elevationHelper.removeListener(listener);
        return resolutionAchieved;
    }

    /**
     * Fill a buffer with values taken from an elevation model.
     *
     * @param elevationModel   elevation model of interest.
     * @param sector           sector of interest.
     * @param latLons          a list of lat/lon locations within the sector.
     * @param targetResolution expected resolution of elevation data.
     * @param buffer           buffer to be filled with elevation data.
     * @return actual elevation data resolution.
     */
    private double lookUpElevations(BasicElevationModel elevationModel,
                                    Sector sector,
                                    List<LatLon> latLons,
                                    double targetResolution,
                                    double[] buffer)
    {
        return elevationModel.getUnmappedElevations(sector, latLons, targetResolution, buffer);
    }

    /**
     * To be called BEFORE an elevation buffer has been filled by an elevation model.  This method fills the buffer
     * with a sentinel value (NaN) which will not be overwritten if the elevation model does not have valid data for
     * the buffer's region.
     * <p/>
     * Note: validateBuffer() should be called AFTER the buffer has been filled by the elevation model.
     *
     * @param buffer buffer of interest.
     */
    private void fillBufferWithInvalidSentinel(double[] buffer)
    {
        Arrays.fill(buffer, Double.NaN);
    }

    private synchronized void trackResolutions(TerrainUtils.ElevationKey[] elevationKeys)
    {
        for (TerrainUtils.ElevationKey elevationKey : elevationKeys)
        {
            trackResolution(elevationKey.resolution);
        }
    }

    private void trackResolution(double resolution)
    {
        resolution *= globeRadius;
        for (ITerrainWWListener listener : terrainListeners)
        {
            listener.trackResolution(resolution);
        }
    }

    /**
     * Clip elevation buffer values to a minimum value of 0.
     *
     * @param elevations elevations buffer to clip to 0
     */
    private void clipToMinimumResolution(double[] elevations)
    {
        for (int i = 0; i < elevations.length; i++)
        {
            if (elevations[i] < 0)
            {
                elevations[i] = 0;
            }
        }
    }

    private synchronized List<LocalDataBasicElevationModel> getLocalDataElevationModelsOfInterest()
    {
        if (localDataModelsOfInterest == null)
        {
            updateLocalDataModelsOfInterest();
        }

        // Return a new list to avoid modification of this list when the models are sorted
        return new ArrayList<>(localDataModelsOfInterest);
    }

    private synchronized void updateLocalDataModelsOfInterest()
    {
        List<BasicElevationModel> modelsOfInterest = getElevationModelsOfInterest();
        localDataModelsOfInterest = new ArrayList<>();
        for (BasicElevationModel model : modelsOfInterest)
        {
            localDataModelsOfInterest.add(new LocalDataBasicElevationModel(model));
        }
    }

    /**
     * Get an updated target resolution which represents the current list of available models, minus ignored models.
     *
     * @param originalTargetResolution the current target resolution.
     * @param ignoreModels             a set of models to remove from the set of available models when determining best resolution.
     * @param sector                   sector of interest.
     * @return a new target resolution.
     */
    private double getUpdatedTargetResolution(double originalTargetResolution,
                                              Set<BasicElevationModel> ignoreModels,
                                              Sector sector)
            throws InterruptedException
    {
        double newTargetResolution = originalTargetResolution;
        List<BasicElevationModel> emList = getElevationModelsOfInterest();
        if (ignoreModels != null)
        {
            emList.removeAll(ignoreModels);
        }
        if (emList.size() > 0)
        {
            // Target resolution is now the best resolution of whatever elevation models are left
            TerrainUtils.sortModelsByResolution(emList, sector);
            BasicElevationModel bestModel = TerrainUtils.getBestContainingModel(sector, emList);
            if (bestModel != null)
            {
                newTargetResolution = TerrainUtils.getBestIntersectingResolution(bestModel, sector);
            } else
            {
                // PLUGIN-2668:
                //
                // In this case, no suitable elevation model has been found for the given sector, so an interrupt
                // exception will be thrown.  However, the exception message can either be an out-of-bounds message
                // or a missing-sector message.
                //
                // The out-of-bounds message indicates that of NONE the elevation models of interest (including ignored
                // models) contain the sector of interest.  No matter how much data is retrieved, none of the elevation
                // models of interest will be able to support this sector.
                //
                // The missing-sector message indicates that AT LEAST ONE elevation model of interest (again, including
                // ignored models) contains the sector of interest.  In this case, if more data were retrieved, it may
                // be possible to support this sector.
                if (TerrainUtils.getBestContainingModel(sector, getElevationModelsOfInterest()) == null)
                {
                    String information = TerrainMessage.REGION_OUT_OF_BOUNDS + ": " + sector.toString();
                    notifyListeners(TerrainMessage.REGION_OUT_OF_BOUNDS, information);
                    throw new InterruptedException(information);
                } else
                {
                    String information = MISSING_SECTOR_ELEVATION + ": " + sector.toString();
                    notifyListeners(TerrainMessage.REGION_MISSING, information);
                    throw new InterruptedException(information);
                }
            }
        }

        if (newTargetResolution != originalTargetResolution)
        {
            logger.trace("Target resolution updated to: {}", newTargetResolution);
        }
        return newTargetResolution;
    }

    private boolean areModelKeysEqual(TerrainUtils.ElevationKey[] elevationKeys)
    {
        for (int i = 0; i < elevationKeys.length - 1; i++)
        {
            if (!elevationKeys[i].isEqualTo(elevationKeys[i + 1]))
            {
                return false;
            }
        }
        return true;
    }

    private synchronized void logModelResolution(List<BasicElevationModel> models, Sector sector)
    {
        if (!logger.isTraceEnabled())
        {
            return;
        }

        logger.trace("\n**** HighestResolutionTerrain.doGetElevations, Thread: {}", Thread.currentThread().getName());
        int size = models.size();
        for (int i = 0; i < size; i++)
        {
            ElevationModel em = models.get(i);
            if (!em.isEnabled())
            {
                continue;
            }

            int c = em.intersects(sector);
            if (c < 0) // no intersection
            {
                continue;
            }
            logger.trace("Elevation Model {}: {}, best resolution: {}, networked: {}", i + 1, em.getName(), String.format("%2.3f", em.getBestResolution(sector) * globeRadius), em.isNetworkRetrievalEnabled());
        }
    }

    protected void notifyListeners(TerrainMessage message, String information)
    {
        for (ITerrainWWListener listener : terrainListeners)
        {
            listener.notify(this, message, information);
        }
    }

    /**
     * Override default implementation to force creation of a sector bounding box with very extreme min/max heights.
     */
    @Override
    protected RectTile createTile(Sector tileSector)
    {
        return new RectTile(computeBoundingBox(tileSector), density, tileSector);
    }

    /**
     * Override default implementation for the same reason as createTile() - the bounding box needs extreme min/max
     * heights.
     */
    @Override
    protected void doGetIntersectingTiles(int r0, int c0, int r1, int c1, Line line, List<RectTile> tiles)
    {
        double minLat = sector.getMinLatitude().degrees + r0 * latTileSize;
        double maxLat = sector.getMinLatitude().degrees + (r1 + 1) * latTileSize;
        double minLon = sector.getMinLongitude().degrees + c0 * lonTileSize;
        double maxLon = sector.getMinLongitude().degrees + (c1 + 1) * lonTileSize;

        // Create a bounding box with extreme min/max elevations.
        Extent extent = computeBoundingBox(Sector.fromDegrees(minLat, maxLat, minLon, maxLon));

        if (!extent.intersects(line))
        {
            return;
        }

        int m = c1 - c0 + 1;
        int n = r1 - r0 + 1;

        if (m == 1 && n == 1)
        {
            tiles.add(createTile(r0, c0));
            return;
        }

        // Subdivide the tile and recursively test for intersection with the line. Order is SW, SE, NW, NE. When there
        // is only one column, the SE subdivision is identical to the SW one and need not be tested. When there is
        // only one row, the NW subdivision is identical to the SW one and need not be tested. In either case (one
        // column or 1 row) the NE subdivision need not be tested.
        doGetIntersectingTiles(r0, c0, r0 + Math.max(0, n / 2 - 1), c0 + Math.max(0, m / 2 - 1), line, tiles); // SW
        if (m != 1)
        {
            doGetIntersectingTiles(r0, c0 + m / 2, r0 + Math.max(0, n / 2 - 1), c1, line, tiles); // SE
        }
        if (n != 1)
        {
            doGetIntersectingTiles(r0 + n / 2, c0, r1, c0 + Math.max(0, m / 2 - 1), line, tiles); // NW
        }
        if (!(m == 1 || n == 1))
        {
            doGetIntersectingTiles(r0 + n / 2, c0 + m / 2, r1, c1, line, tiles); // NE
        }
    }

    /**
     * Create a bounding box extent with extreme min/max elevations (depth of Mariana trench and height of Everest).
     */
    private Extent computeBoundingBox(Sector sector)
    {
        return Sector.computeBoundingBox(
                globe,
                verticalExaggeration,
                sector, DEPTH_OF_MARIANAS_TRENCH,
                HEIGHT_OF_MT_EVEREST);
    }
}
