package gov.takx.api.terrain.experimental;

import com.atakmap.map.elevation.ElevationSource;
import gov.nasa.worldwind.avlist.AVKey;
import gov.nasa.worldwind.geom.Sector;
import gov.nasa.worldwind.globes.ElevationModel;
import gov.nasa.worldwind.util.LevelSet;
import gov.tak.api.annotation.DeprecatedApi;

/**
 * An {@link ElevationModel} that transitions from {@link gov.nasa.worldwind.terrain.BasicElevationModel} to TAK {@link ElevationSource}. Note that WW ElevationModels
 * originally returned elevations in MSL, however all classes derived from ITakElevationModel should return elevations in HAE.
 *
 * @since 5.2
 */
public interface ITakElevationModel extends ElevationModel
{
    /**
     * @return the bounding sector of the coverage area.
     */
    Sector getSector();

    /**
     * Gets a {@link LevelSet} that represents the covered area.
     * @return the level set
     * @deprecated to be removed
     */
    @Deprecated(since = "5.5", forRemoval = true)
    @DeprecatedApi(since = "5.5", forRemoval = true)
    LevelSet getLevels();

    /**
     * @return the {@link ElevationSource} represented by this model
     */
    ElevationSource getElevationSource();

    /**
     * Gets the endianness of the elevation data returned by this model. Defaults to {@link AVKey#LITTLE_ENDIAN}.
     * @return either {@link AVKey#LITTLE_ENDIAN} or {@link AVKey#BIG_ENDIAN}
     */
    default String getElevationDataByteOrder()
    {
        return AVKey.LITTLE_ENDIAN;
    }

    /**
     * Gets the data type of the elevation data returned by this model. Defaults to {@link AVKey#INT16}.
     * @return a data type from {@link AVKey} 
     */
    default String getElevationDataType()
    {
        return AVKey.INT16;
    }
}
