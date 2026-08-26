package gov.takx.api.terrain.experimental.proposed.tile;

import gov.nasa.worldwind.geom.LatLon;
import gov.nasa.worldwind.geom.Sector;
import gov.nasa.worldwind.util.Level;
import gov.nasa.worldwind.util.LevelSet;
import gov.takx.api.annotation.Experimental;
import gov.takx.api.terrain.experimental.ITakElevationModel;

import java.awt.Dimension;

/**
 */
@Experimental
public class ElevationAttributes
{
    private final String type;
    private final String byteOrder;
    private final Sector sector;
    private final Dimension tileDimension;
    private final LatLon zeroTileDelta;
    private final LatLon tileOrigin;
    private final Integer levels;
    private final LatLon[] levelDeltas;
    private float localMissingDataSignal;
    private double[] texelSizes;
    private final SectorResolutionLimit[] sectorResolutionLimits;
    private final ITakElevationModel elevationModel;

    /**
     * @since 5.2
     */
    public ElevationAttributes(ITakElevationModel model)
    {
        this.elevationModel = model;
        type = model.getElevationDataType();
        byteOrder = model.getElevationDataByteOrder();
        LevelSet levelSet = model.getLevels();
        sector = levelSet.getSector();
        Level level = levelSet.getFirstLevel();
        tileDimension = new Dimension(level.getTileWidth(), level.getTileHeight());
        zeroTileDelta = levelSet.getLevelZeroTileDelta();
        tileOrigin = levelSet.getTileOrigin();
        levels = levelSet.getNumLevels();
        levelDeltas = createLevelDeltas(zeroTileDelta, levels);
        localMissingDataSignal = (float) model.getMissingDataSignal();
        texelSizes = createTexelSizes(levels, levelDeltas, tileDimension.height);

        LevelSet.SectorResolution[] sectorResolutions = model.getLevels().getSectorLevelLimits();
        if (sectorResolutions != null && sectorResolutions.length > 0)
        {
            sectorResolutionLimits = new SectorResolutionLimit[sectorResolutions.length];
            for (int i = 0; i < sectorResolutionLimits.length; i++)
            {
                LevelSet.SectorResolution sectorResolution = sectorResolutions[i];
                sectorResolutionLimits[i] = new SectorResolutionLimit(
                        sectorResolution.getSector(),
                        sectorResolution.getLevelNumber());
            }
        } else
        {
            sectorResolutionLimits = null;
        }
    }

    public String getType()
    {
        return type;
    }

    public String getByteOrder()
    {
        return byteOrder;
    }

    public Sector getSector()
    {
        return sector;
    }

    public Dimension getTileDimension()
    {
        return tileDimension;
    }

    public LatLon getZeroTileDelta()
    {
        return zeroTileDelta;
    }

    public LatLon getTileOrigin()
    {
        return tileOrigin;
    }

    public Integer getLevels()
    {
        return levels;
    }

    public LatLon getLevelDelta(int level)
    {
        if (levelDeltas == null)
        {
            return null;
        }
        return levelDeltas[level];
    }

    public float getLocalMissingDataSignal()
    {
        return localMissingDataSignal;
    }

    public double getResolution(int level)
    {
        return texelSizes[level];
    }

    public int getHighestLevel()
    {
        return levels - 1;
    }

    public int getHighestLevel(double latitude, double longitude)
    {
        if (sectorResolutionLimits == null)
        {
            return getHighestLevel();
        }
        for (SectorResolutionLimit limit : sectorResolutionLimits)
        {
            if (limit.contains(latitude, longitude))
            {
                return limit.level;
            }
        }
        return getHighestLevel();
    }

    public double getHighestResolution()
    {
        return texelSizes[texelSizes.length - 1];
    }

    public double getHighestResolution(double latitude, double longitude)
    {
        if (!sector.containsDegrees(latitude, longitude))
        {
            return Double.MAX_VALUE;
        }
        return getResolution(getHighestLevel(latitude, longitude));
    }

    public int getHighestLevel(Sector sector)
    {
        if (sectorResolutionLimits == null)
        {
            return getHighestLevel();
        }
        for (SectorResolutionLimit limit : sectorResolutionLimits)
        {
            if (limit.intersects(sector))
            {
                return limit.level;
            }
        }
        return getHighestLevel();
    }

    public boolean isHighestResolution(ElevationTile elevationTile)
    {
        // Note that it must be EQUAL to the highest level and not equal to or greater than.
        // This is because some tiles of higher resolution than defined by the sector resolution limits can sometimes
        // sneak into a cache.  We don't want those, because they don't represent what users expect.
        return elevationTile.getLevel() == getHighestLevel(elevationTile.getSector());
    }

    public ITakElevationModel getElevationModel()
    {
        return elevationModel;
    }

    public int getWidth()
    {
        return tileDimension.width;
    }

    public int getHeight()
    {
        return tileDimension.height;
    }

    @Override
    public String toString()
    {
        return "ElevationAttributes{" +
                ", type='" + type + '\'' +
                ", byteOrder='" + byteOrder + '\'' +
                ", sector=" + sector +
                ", levels=" + levels +
                ", tileDimension=" + tileDimension +
                ", zeroTileDelta=" + zeroTileDelta +
                ", tileOrigin=" + tileOrigin +
                ", localMissingDataSignal=" + localMissingDataSignal +
                '}';
    }

    public static class SectorResolutionLimit
    {
        public final Sector sector;
        public final int level;

        public SectorResolutionLimit(Sector sector, int level)
        {
            this.sector = Sector.boundingSector(sector);
            this.level = level;
        }

        public boolean contains(double latitude, double longitude)
        {
            return sector.containsDegrees(latitude, longitude);
        }

        public boolean intersects(Sector sector)
        {
            return sector.intersectsInterior(this.sector);
        }
    }

    private static LatLon[] createLevelDeltas(LatLon zeroTileDelta, int levels)
    {
        LatLon[] levelDeltas = new LatLon[levels];
        double latDelta = zeroTileDelta.getLatitude().degrees;
        double lonDelta = zeroTileDelta.getLongitude().degrees;
        for (int i = 0; i < levels; i++)
        {
            levelDeltas[i] = LatLon.fromDegrees(latDelta, lonDelta);
            latDelta /= 2;
            lonDelta /= 2;
        }
        return levelDeltas;
    }

    private static double[] createTexelSizes(int levels, LatLon[] levelDeltas, int height)
    {
        double[] texelSizes = new double[levels];
        for (int i = 0; i < texelSizes.length; i++)
        {
            texelSizes[i] = levelDeltas[i].getLatitude().getRadians() / height;
        }
        return texelSizes;
    }
}
