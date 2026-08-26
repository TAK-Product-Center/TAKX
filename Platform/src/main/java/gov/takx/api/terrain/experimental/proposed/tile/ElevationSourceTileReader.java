package gov.takx.api.terrain.experimental.proposed.tile;

import com.atakmap.map.elevation.ElevationManager;
import com.atakmap.map.elevation.ElevationManager.HeightmapStrategy;
import com.atakmap.map.elevation.ElevationSource;
import com.atakmap.map.layer.feature.geometry.Envelope;
import gov.nasa.worldwind.geom.Sector;
import gov.takx.api.annotation.Experimental;

/**
 * An implementation of {@link IElevationTileReader} that queries an {@link ElevationSource} for elevation data.
 *
 * @since 5.2
 */
@Experimental
public class ElevationSourceTileReader implements IElevationTileReader
{
    private final ElevationSource source;
    private final ElevationAttributes attributes;

    public ElevationSourceTileReader(ElevationSource source, ElevationAttributes attributes)
    {
        this.source = source;
        this.attributes = attributes;
    }

    @Override
    public float[] readValues()
    {
        int width = attributes.getWidth();
        int height = attributes.getHeight();
        Sector sector = attributes.getSector();
        double[] values = new double[width * height];
        ElevationManager.HeightmapParams params = new ElevationManager.HeightmapParams();
        params.bounds = new Envelope(sector.getMinLongitude().degrees, sector.getMinLatitude().degrees, 0,
                sector.getMaxLongitude().degrees, sector.getMaxLatitude().degrees, 0);
        params.numPostsLat = height;
        params.numPostsLng = width;
        ElevationManager.createHeightmap(values, source, Double.MIN_VALUE, params, HeightmapStrategy.HighestResolution);

        float[] valuesFloat = new float[width * height];
        for (int a = 0; a < values.length; a++) valuesFloat[a] = (float) values[a];
        return valuesFloat;
    }
}
