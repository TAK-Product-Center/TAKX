package gov.takx.api.terrain.experimental.proposed.tile;

import gov.nasa.worldwind.geom.LatLon;
import gov.nasa.worldwind.geom.Sector;
import gov.nasa.worldwind.globes.ElevationModel;
import gov.nasa.worldwind.terrain.Terrain;
import gov.takx.api.annotation.Experimental;
import gov.takx.api.terrain.experimental.ITakElevationModel;
import gov.takx.api.terrain.experimental.proposed.TerrainDataUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.invoke.MethodHandles;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;

/**
 */
@Experimental
public class TileManager implements IElevationProvider
{
    private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());
    private final List<TileCollection> tileCollections;
    private final Terrain backupDataSource;
    private double highestResolution = Double.MAX_VALUE;
    private int cachedCount = 0;
    private int backupCount = 0;
    private Timer flushTimer = null;
    private long flushTimerDelay = 15 * 60 * 1000;
    private long lastTimerUpdate = -1;

    private double calculatedHighestResolution = Double.MAX_VALUE;

    public TileManager(List<ITakElevationModel> modelsOfInterest, Terrain backupDataSource)
    {
        tileCollections = new ArrayList<>();
        this.backupDataSource = backupDataSource;
        setTileCollections(TerrainDataUtils.createTileCollections(modelsOfInterest));
    }

    @Override
    public double getHighestResolution()
    {
        return highestResolution;
    }

    @Override
    public boolean contains(double latitude, double longitude)
    {
        for (TileCollection tileCollection : tileCollections)
        {
            if (tileCollection.getAttributes().getSector().containsDegrees(latitude, longitude))
            {
                return true;
            }
        }
        return false;
    }

    /**
     * @since 5.2
     */
    public void setElevationModelEnabled(ITakElevationModel elevationModel, boolean enabled)
    {
        for (TileCollection tileCollection : tileCollections)
        {
            ITakElevationModel em = tileCollection.getAttributes().getElevationModel();
            if (em.equals(elevationModel))
            {
                tileCollection.setEnabled(enabled);
            }
        }
    }

    public void setElevationModels(Collection<ITakElevationModel> elevationModels)
    {
        List<TileCollection> newTileCollections = new ArrayList<>();
        for (ITakElevationModel elevationModel : elevationModels)
        {
            TileCollection tiles = getCollectionForModel(elevationModel);
            newTileCollections.add(tiles != null ? tiles : TerrainDataUtils.createTileCollection(elevationModel));
        }

        for (TileCollection tiles : tileCollections)
        {
            if (!newTileCollections.contains(tiles))
            {
                tiles.flushAll();
            }
        }

        this.tileCollections.clear();
        this.tileCollections.addAll(newTileCollections);
    }

    @Override
    public float getElevation(double latitude, double longitude)
    {
        ElevationValue value = getBestElevationValue(latitude, longitude);
        updateRootFlushTimer();
        if (value != null && value.isValid() && value.isHighestResolution())
        {
            cachedCount++;
            if (value.getResolution() < calculatedHighestResolution)
            {
                calculatedHighestResolution = value.getResolution();
            }
            return value.getValue();
        } else
        {
            backupCount++;
            Double e = backupDataSource.getElevation(LatLon.fromDegrees(latitude, longitude));
            update(latitude, longitude);
            return e != null ? e.floatValue() : TerrainDataUtils.NOT_LOCAL_SIGNAL;
        }
    }

    public void resetCalculatedHighestResolution()
    {
        calculatedHighestResolution = Double.MAX_VALUE;
    }

    public double getCalculatedHighestResolution()
    {
        return calculatedHighestResolution;
    }

    public void setTileCollections(List<TileCollection> tileCollections)
    {
        this.tileCollections.clear();
        this.tileCollections.addAll(tileCollections);
        updateHighestResolution();
    }

    public TileCollection getCollectionForModel(ElevationModel elevationModel)
    {
        for (TileCollection tileCollection : tileCollections)
        {
            if (elevationModel.equals(tileCollection.getAttributes().getElevationModel()))
            {
                return tileCollection;
            }
        }
        return null;
    }

    public void sortByResolution(Sector sector)
    {
        Map<ITakElevationModel, TileCollection> tileCollectionMap = new HashMap<>();
        List<ITakElevationModel> elevationModels = new ArrayList<>();
        for (TileCollection tileCollection : tileCollections)
        {
            ITakElevationModel elevationModel = tileCollection.getAttributes().getElevationModel();
            tileCollectionMap.put(elevationModel, tileCollection);
            elevationModels.add(elevationModel);
        }

        elevationModels.sort((o1, o2) -> -Double.compare(o1.getBestResolution(sector), o2.getBestResolution(sector)));

        tileCollections.clear();

        // Re-add the sorted elevation models in reverse order.  This is to favor the highest/prioritized elevation
        // models, as the list is sorted in ascending order of quality.
        for (int i = elevationModels.size() - 1; i >= 0; i--)
        {
            tileCollections.add(tileCollectionMap.get(elevationModels.get(i)));
        }
    }

    public double getHighestResolution(double latitude, double longitude)
    {
        if (tileCollections.size() <= 0)
        {
            return Double.MAX_VALUE;
        }
        double highestResolution = tileCollections.get(0).getHighestResolution(latitude, longitude);
        for (int i = 1; i < tileCollections.size(); i++)
        {
            double r = tileCollections.get(i).getHighestResolution(latitude, longitude);
            if (r < highestResolution)
            {
                highestResolution = r;
            }
        }
        return highestResolution;
    }

    public IElevationRaster createElevationRaster(Sector sector)
    {
        double highestResolution = getHighestResolution();

        int width = (int) Math.ceil(sector.getDeltaLonRadians() / highestResolution);
        int height = (int) Math.ceil(sector.getDeltaLatRadians() / highestResolution);

        double maxX = width - 1;
        double maxY = height - 1;

        double minLongitude = sector.getMinLongitude().degrees;
        double maxLatitude = sector.getMaxLatitude().degrees;

        double deltaLat = sector.getDeltaLatDegrees();
        double deltaLon = sector.getDeltaLonDegrees();

        double latitude;
        double longitude;
        float[] values = new float[width * height];

        int yOffset;
        for (int y = 0; y < height; y++)
        {
            yOffset = y * width;
            latitude = maxLatitude - (y / maxY) * deltaLat;
            for (int x = 0; x < width; x++)
            {
                longitude = minLongitude + (x / maxX) * deltaLon;
                values[yOffset + x] = getElevation(latitude, longitude);
            }
        }
        return new ElevationRaster(sector, values, width, height, highestResolution);
    }

    public void update(double latitude, double longitude)
    {
        for (TileCollection tileCollection : tileCollections)
        {
            tileCollection.update(latitude, longitude);
        }
    }

    public void resetCounts()
    {
        cachedCount = backupCount = 0;
    }

    public int getCachedCount()
    {
        return cachedCount;
    }

    public int getBackupCount()
    {
        return backupCount;
    }

    public int size()
    {
        int size = 0;
        for (TileCollection tileManager : tileCollections)
        {
            size += tileManager.size();
        }
        return size;
    }

    public List<ElevationTile> getTiles(double latitude, double longitude)
    {
        List<ElevationTile> tiles = new ArrayList<>();
        for (TileCollection tileManager : tileCollections)
        {
            if (tileManager.getAttributes().getHighestResolution() <= highestResolution)
            {
                ElevationTile tile = tileManager.getElevationTile(latitude, longitude);
                if (tile != null)
                {
                    tiles.add(tile);
                }
            }
        }
        return tiles;
    }

    public ElevationValue getBestElevationValue(double latitude, double longitude)
    {
        double highestResolution = getHighestResolution(latitude, longitude);
        if (highestResolution >= Double.MAX_VALUE)
        {
            return null;
        }
        for (TileCollection tileCollection : tileCollections)
        {
            if (tileCollection.isEnabled())
            {
                ElevationAttributes attributes = tileCollection.getAttributes();
                if (attributes.getHighestResolution(latitude, longitude) <= highestResolution)
                {
                    ElevationValue v = tileCollection.getElevationValue(latitude, longitude);
                    if (v != null && v.isValid() && v.getResolution() <= highestResolution)
                    {
                        return v;
                    }
                }
            }
        }
        return null;
    }

    @Override
    public String toString()
    {
        return "TileManager{" +
                "tileCollections=" + tileCollections +
                ", highestResolution=" + highestResolution +
                '}';
    }

    public void flushAll()
    {
        for (TileCollection tileCollection : tileCollections)
        {
            tileCollection.flushAll();
        }
    }

    public void setFlushDelay(long secondsDelay)
    {
        synchronized (TileManager.class)
        {
            flushTimerDelay = secondsDelay * 1000;
            updateFlushTimer();
        }
    }

    public void updateFlushTimer()
    {
        synchronized (TileManager.class)
        {
            clearFlushTimer();
            lastTimerUpdate = System.currentTimeMillis();
            flushTimer = new Timer("TileManager Timer");
            flushTimer.schedule(new TimerTask()
            {
                @Override
                public void run()
                {
                    synchronized (TileManager.class)
                    {
                        logger.debug("Flushing cache");
                        flushAll();
                        clearFlushTimer();
                    }
                }
            }, flushTimerDelay);

            logger.debug("Updated flush timer");
        }
    }

    public void checkAndUpdateFlushTimer()
    {
        synchronized (TileManager.class)
        {
            long diff = System.currentTimeMillis() - lastTimerUpdate;
            if (diff >= (flushTimerDelay / 2))
            {
                updateFlushTimer();
            }
        }
    }

    private void updateRootFlushTimer()
    {
        checkAndUpdateFlushTimer();
    }

    private void clearFlushTimer()
    {
        synchronized (TileManager.class)
        {
            if (flushTimer != null)
            {
                flushTimer.cancel();
                flushTimer = null;
            }
        }
    }

    private void updateHighestResolution()
    {
        highestResolution = Double.MAX_VALUE;
        for (TileCollection tileManager : tileCollections)
        {
            double v = tileManager.getAttributes().getHighestResolution();
            if (v < highestResolution)
            {
                highestResolution = v;
            }
        }
        updateFlushTimer();
    }
}
