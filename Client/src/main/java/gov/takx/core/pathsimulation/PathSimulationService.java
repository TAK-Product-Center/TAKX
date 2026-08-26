package gov.takx.core.pathsimulation;

import gov.takx.api.coordinatesystem.ILocation;
import gov.takx.api.mapobject.IMapObject;
import gov.takx.api.pathsimulation.IPathSimulationService;
import gov.takx.api.pathsimulation.IPathSimulator;
import jakarta.enterprise.context.ApplicationScoped;

import java.util.List;

/**
 * Concrete implementation of the IPathSimulationService.
 *
 */
@ApplicationScoped
public class PathSimulationService implements IPathSimulationService
{
    PathSimulationService()
    {
    }

    @Override
    public IPathSimulator createSimulator(List<ILocation> path, IMapObject mapObject, double maxDistanceBetweenPoints)
    {
        return new PathSimulator(path, mapObject, maxDistanceBetweenPoints);
    }
}
