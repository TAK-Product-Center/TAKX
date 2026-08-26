package gov.takx.core.platform;

import gov.takx.api.platform.IExperimentalServices;
import gov.takx.api.terrain.experimental.ITerrainService;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

/**
 */
@ApplicationScoped
public class ExperimentalServices implements IExperimentalServices
{
    @Inject
    private ITerrainService terrainService;

    @Override
    public ITerrainService getTerrainService()
    {
        return terrainService;
    }
}
