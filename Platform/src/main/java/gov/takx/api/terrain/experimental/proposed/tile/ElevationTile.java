package gov.takx.api.terrain.experimental.proposed.tile;

import gov.nasa.worldwind.geom.Sector;
import gov.tak.api.annotation.DeprecatedApi;
import gov.takx.api.annotation.Experimental;
import gov.takx.api.terrain.experimental.proposed.TerrainDataUtils;

import java.awt.Dimension;
import java.lang.ref.SoftReference;
import java.util.Objects;

/**
 * An elevation tile
 */
@Experimental
public class ElevationTile implements IElevationProvider
{
    private final ElevationAttributes attributes;
    private final Sector originalSector;
    private final Sector sector;
    private final int level;
    private final int row;
    private final int col;
    private final long key;
    private final int width;
    private final int height;
    private final IElevationTileReader tileReader;
    private SoftReference<IElevationRaster> rasterReference = new SoftReference<>(null);

    /**
     * @since 5.2
     * @deprecated use {@link #ElevationTile(int, int, int, ElevationAttributes)}
     */
    @Deprecated(since = "5.6", forRemoval = true)
    @DeprecatedApi(since = "5.6", forRemoval = true, removeAt = "5.9")
    public ElevationTile(int row, int col, ElevationAttributes attributes)
    {
        this(row, col, 0, attributes);
    }

    /**
     * @since 5.6
     */
    public ElevationTile(int row, int col, int level, ElevationAttributes attributes)
    {
        this.attributes = attributes;
        Dimension dimension = attributes.getTileDimension();
        width = dimension.width;
        height = dimension.height;
        this.level = level;
        this.row = row;
        this.col = col;
        originalSector = TerrainDataUtils.getSector(row, col, level, attributes, false);
        sector = TerrainDataUtils.getSector(row, col, level, attributes, true);
        key = TerrainDataUtils.getKey(row, col, level);
        tileReader = new ElevationSourceTileReader(attributes.getElevationModel().getElevationSource(), attributes);
    }

    public ElevationAttributes getAttributes()
    {
        return attributes;
    }

    /**
     * Get the original Sector defined by this tile's dimensions.  This is the tile's Sector when it is not affected
     * by its owning model's bounding Sector.  For most tiles, this will be identical to the Sector available via
     * getSector().  The difference occurs in tiles extending beyond the model's bounding Sector.  In this case, the
     * original Sector will be larger than the one returned by getSector().
     *
     * @return the Sector determined by this tile's dimensions.  No intersection with its owning model's boundaries.
     */
    public Sector getOriginalSector()
    {
        return originalSector;
    }

    /**
     * Get the logical sector defined by intersecting the original Sector with the owning model's bounding Sector.  See
     * notes for getOriginalSector() for more information.
     *
     * @return the sector defined by intersecting the original sector with the owning model's bounding Sector.
     */
    public Sector getSector()
    {
        return sector;
    }

    @Override
    public boolean contains(double latitude, double longitude)
    {
        return sector.containsDegrees(latitude, longitude);
    }

    public ElevationValue getElevationValue(double latitude, double longitude)
    {
        // todo: handle edge values - ensure the interpolated value uses just as high resolution.
        return new ElevationValue(getElevation(latitude, longitude), this, latitude, longitude);
    }

    @Override
    public float getElevation(double latitude, double longitude)
    {
        if (!contains(latitude, longitude))
        {
            return TerrainDataUtils.NOT_LOCAL_SIGNAL;
        }
        return getRaster().getElevation(latitude, longitude);
    }

    public float getRawElevation(double latitude, double longitude)
    {
        if (!contains(latitude, longitude))
        {
            return TerrainDataUtils.NOT_LOCAL_SIGNAL;
        }
        return getRaster().getRawElevation(latitude, longitude);
    }

    public float getValue(int x, int y)
    {
        return getValues()[y * height + x];
    }

    public int getLevel()
    {
        return level;
    }

    public int getCol()
    {
        return col;
    }

    public int getRow()
    {
        return row;
    }

    public long getKey()
    {
        return key;
    }

    @Override
    public double getHighestResolution()
    {
        return attributes.getResolution(level);
    }

    public synchronized IElevationRaster getRaster()
    {
        IElevationRaster r = null;
        if (rasterReference != null)
        {
            r = rasterReference.get();
        }
        if (r == null)
        {
            float[] values = tileReader.readValues();
            if (values != null)
            {
                r = new ElevationRaster(attributes.getSector(), values, width, height, attributes.getResolution(level));
                rasterReference = new SoftReference<>(r);
            }
        }
        return r;
    }

    public synchronized float[] getValues()
    {
        return getRaster().getValues();
    }

    public synchronized void flushValues()
    {
        rasterReference = null;
    }

    @Override
    public String toString()
    {
        return "ElevationTile{" +
                ", row=" + row +
                ", col=" + col +
                ", sector=" + sector +
                ", level=" + level +
                '}';
    }

    @Override
    public int hashCode()
    {
        return new Long(key).hashCode();
    }

    @Override
    public boolean equals(Object o)
    {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        ElevationTile that = (ElevationTile) o;

        if (col != that.col) return false;
        if (height != that.height) return false;
        if (key != that.key) return false;
        if (level != that.level) return false;
        if (row != that.row) return false;
        if (width != that.width) return false;
        if (!Objects.equals(sector, that.sector)) return false;

        return true;
    }
}
