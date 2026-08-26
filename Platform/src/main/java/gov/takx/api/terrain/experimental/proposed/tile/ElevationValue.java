package gov.takx.api.terrain.experimental.proposed.tile;

import gov.takx.api.annotation.Experimental;
import gov.takx.api.terrain.experimental.proposed.TerrainDataUtils;

/**
 */
@Experimental
public class ElevationValue
{
    public final float value;
    public final ElevationTile elevationTile;
    public final double latitude;
    public final double longitude;

    public ElevationValue(float value, ElevationTile tile, double latitude, double longitude)
    {
        this.value = value;
        this.elevationTile = tile;
        this.latitude = latitude;
        this.longitude = longitude;
    }

    public float getValue()
    {
        return value;
    }

    public boolean isValid()
    {
        return !TerrainDataUtils.isMissingOrNotLocalSignal(value) &&
                value != elevationTile.getAttributes().getLocalMissingDataSignal();
    }

    public boolean isNotLocalSignal()
    {
        return TerrainDataUtils.isNotLocalSignal(value);
    }

    public boolean isMissingDataSignal()
    {
        return TerrainDataUtils.isMissingDataSignal(value);
    }

    public double getResolution()
    {
        return elevationTile.getAttributes().getResolution(elevationTile.getLevel());
    }

    public boolean isHighestResolution()
    {
        return elevationTile.getLevel() >= (elevationTile.getAttributes().getHighestLevel(latitude, longitude));
    }

    @Override
    public String toString()
    {
        return "ElevationValue{" +
                "longitude=" + longitude +
                ", latitude=" + latitude +
                ", elevationTile=" + elevationTile +
                ", value=" + value +
                '}';
    }
}
