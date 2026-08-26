package gov.takx.api.platform;

import gov.tak.api.annotation.DeprecatedApi;
import gov.takx.api.terrain.experimental.ITerrainService;

/**
 * Provides access to experimental services.  These services are not guaranteed to be stable, the API might change at
 * any time, and the service might go away at any time.  Use at your own risk.
 *
 * @since 1.35
 * @deprecated Inject services directly
 */
@Deprecated(since = "5.5", forRemoval = true)
@DeprecatedApi(since = "5.5", forRemoval = true, removeAt = "5.8")
public interface IExperimentalServices
{
    /**
     * Provides access to core terrain services such as the core IHighestResolutionTerrain and terrain calculations.
     *
     * @return ITerrainService
     * @deprecated Inject {@link ITerrainService} directly
     */
    ITerrainService getTerrainService();
}
