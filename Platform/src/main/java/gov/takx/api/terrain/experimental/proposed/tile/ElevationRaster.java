package gov.takx.api.terrain.experimental.proposed.tile;

import gov.nasa.worldwind.geom.Sector;
import gov.takx.api.annotation.Experimental;
import gov.takx.api.terrain.experimental.proposed.TerrainDataUtils;

/**
 */
@Experimental
public class ElevationRaster implements IElevationRaster
{
    private final Sector sector;
    private final double highestResolution;
    private final double sectorDeltaLat;
    private final double sectorDeltaLon;
    private final double maxLatitude;
    private final double minLongitude;

    private final float[] values;
    private final int width;
    private final int height;

    public ElevationRaster(Sector sector, float[] values, int width, int height, double highestResolution)
    {
        this.sector = sector;
        this.highestResolution = highestResolution;
        sectorDeltaLat = sector.getDeltaLat().degrees;
        sectorDeltaLon = sector.getDeltaLon().degrees;
        maxLatitude = sector.getMaxLatitude().degrees;
        minLongitude = sector.getMinLongitude().degrees;
        this.values = values;
        this.width = width;
        this.height = height;
    }

    @Override
    public Sector getSector()
    {
        return sector;
    }

    @Override
    public boolean contains(double latitude, double longitude)
    {
        return sector.containsDegrees(latitude, longitude);
    }

    @Override
    public double getHighestResolution()
    {
        return highestResolution;
    }

    @Override
    public float getElevation(double latitude, double longitude)
    {
        final double dLat = maxLatitude - latitude;
        final double dLon = longitude - minLongitude;
        final double sLat = dLat / sectorDeltaLat;
        final double sLon = dLon / sectorDeltaLon;

        int j = (int) ((height - 1) * sLat);
        int i = (int) ((width - 1) * sLon);
        int k = j * width + i;

        float eLeft = values[k];
        float eRight = i < (width - 1) ? values[k + 1] : eLeft;

        if (TerrainDataUtils.isMissingDataSignal(eLeft, eRight))
        {
            return TerrainDataUtils.MISSING_DATA_SIGNAL;
        } else if (TerrainDataUtils.isNotLocalSignal(eLeft, eRight))
        {
            return TerrainDataUtils.NOT_LOCAL_SIGNAL;
        }

        double dw = sectorDeltaLon / (width - 1);
        double dh = sectorDeltaLat / (height - 1);
        double ssLon = (dLon - i * dw) / dw;
        double ssLat = (dLat - j * dh) / dh;

        double eTop = eLeft + ssLon * (eRight - eLeft);

        if (j < height - 1 && i < width - 1)
        {
            eLeft = values[k + width];
            eRight = values[k + width + 1];

            if (TerrainDataUtils.isMissingDataSignal(eLeft, eRight))
            {
                return TerrainDataUtils.MISSING_DATA_SIGNAL;
            } else if (TerrainDataUtils.isNotLocalSignal(eLeft, eRight))
            {
                return TerrainDataUtils.NOT_LOCAL_SIGNAL;
            }
        }

        double eBot = eLeft + ssLon * (eRight - eLeft);
        return (float) (eTop + ssLat * (eBot - eTop));
    }

    @Override
    public float getRawElevation(double latitude, double longitude)
    {
        final double dLat = maxLatitude - latitude;
        final double dLon = longitude - minLongitude;

        final double sLat = dLat / sectorDeltaLat;
        final double sLon = dLon / sectorDeltaLon;

        int maxX = width;
        int maxY = height;

        int y = Math.min((int) (maxY * sLat), height - 1);
        int x = Math.min((int) (maxX * sLon), width - 1);

        return getValue(x, y);
    }

    @Override
    public float[] getValues()
    {
        return values;
    }

    @Override
    public float getValue(int x, int y)
    {
        return values[y * width + x];
    }

    @Override
    public int getWidth()
    {
        return width;
    }

    @Override
    public int getHeight()
    {
        return height;
    }
}
