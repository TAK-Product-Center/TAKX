package gov.takx.api.pathsimulation;

import gov.takx.api.annotation.InjectableService;
import gov.takx.api.annotation.NoPluginImplementations;
import gov.takx.api.coordinatesystem.ILocation;
import gov.takx.api.mapobject.IMapObject;

import java.util.List;

/**
 * The Path Simulation Service provides a way for plugins to simulate location changes for a map object along a user or
 * plugin defined set of locations. Currently, one path and map object are associated with each simulation.
 *
 * @since 1.29
 */
@InjectableService
@NoPluginImplementations
public interface IPathSimulationService
{
    /**
     * Used to get a {@link IPathSimulator} instance which is used to manage the simulation associated with the given
     * path and map object. The given path will be used to generate the actual simulation points. This is due to the
     * fact that the points that the map object would traverse changes depending on the distance between simulation
     * points.
     *
     * @param path                     The path that will be used to generate the actual simulation points
     * @param mapObject                The map object that will be used in the simulation
     * @param maxDistanceBetweenPoints The maximum distance in <b>meters</b> between generated simulation points
     * @return The simulator object used to manage the simulation.
     */
    IPathSimulator createSimulator(List<ILocation> path, IMapObject mapObject, double maxDistanceBetweenPoints);
}
