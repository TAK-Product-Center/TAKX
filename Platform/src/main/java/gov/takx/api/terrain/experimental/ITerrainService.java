package gov.takx.api.terrain.experimental;

import gov.nasa.worldwind.terrain.Terrain;
import gov.takx.api.annotation.InjectableService;
import gov.takx.api.annotation.NoPluginImplementations;

/**
 * This interface defines a service for working with terrain.  This includes
 * Occlusion operations for line of sight calculations, getting specific elevations
 * for a location, and getting information on the available elevation models.
 *
 * @since 1.35
 */
@InjectableService
@NoPluginImplementations
public interface ITerrainService
{
    /**
     * Retrieves the core's highest resolution terrain model.  It is configurable by the user.  The user
     * can choose which elevation models to use, what size cache to maintain, etc.  This terrain is lazily
     * initialized due to it's high memory consumption.  Once it is initialized it is shared by all apps.  It
     * ensures that the best resolution available (online or local) is used in calculations unless the user
     * configures it to use the best resolution locally.
     *
     * @return Returns the core's terrain object for highest resolution calculations
     */
    IImmutableHighestResolutionTerrain getHighestResolutionTerrain();

    /**
     * Provides a calculator to do many terrain related operations such as occlusion and getting elevations.
     * It requires a Terrain object.  If the intent is to use the highest resolution available then use
     * the {@link IHighestResolutionTerrain}.  For quicker but less accurate results the globe's terrain model
     * can be used.  Or, a custom Terrain object can be created to suite specific needs.
     *
     * @param terrain A Terrain object that will be used for the calculations
     * @return ITerrainCalculator
     */
    ITerrainCalculator getTerrainCalculator(Terrain terrain);
}
