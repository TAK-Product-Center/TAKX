package gov.takx.api.terrain.experimental;

import gov.nasa.worldwind.globes.ElevationModel;
import gov.takx.api.annotation.Experimental;

import java.util.List;

/**
 */
@Experimental
public interface ITerrainListener
{
    void notify(IImmutableHighestResolutionTerrain terrain, TerrainMessage message, String information);

    void trackResolution(double resolutionInMeters);

    void elevationModelsChanged(List<ElevationModel> newModels);
}
