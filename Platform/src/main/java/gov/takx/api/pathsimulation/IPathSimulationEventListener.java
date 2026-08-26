package gov.takx.api.pathsimulation;

import gov.takx.api.coordinatesystem.ILocation;
import gov.takx.api.messages.IPrePersistRaptorDataMessage;
import gov.takx.api.messages.IRaptorDataMessage;

/**
 * This class should be implemented if a plugin is interested in events that are triggered while a map object traverses
 * the simulation path. The implementing class needs to be registered with the {@link IPathSimulator} instance that will
 * be used during the simulation.
 *
 * @since 1.29
 */
public interface IPathSimulationEventListener
{
    /**
     * This event is fired before a point is traversed. The implementing class can supply an {@link IRaptorDataMessage}
     * as a return type if they would like to change device state as well. The RDM location will be overwritten using the
     * supplied pointToTraverse after the method is returned. If a different event listener already supplied an RDM then
     * this one will be ignored. If a list of RDMs was supplied before the simulation then the current RDM in the list
     * will be replaced by this RDM.
     *
     * @param pointToTraverse      The point that is about to be traversed
     * @param simulationPointIndex The index within the list of points that is about to be traversed. This can be used
     *                             to synchronize plugin state with the current location.
     * @return The plugin supplied RDM if device specific state changes are desired
     */
    public IPrePersistRaptorDataMessage prePointTraversal(ILocation pointToTraverse, int simulationPointIndex);

    /**
     * The event is fired after a point is traversed.
     *
     * @param traversedPoint       The location of the point that was traversed
     * @param simulationPointIndex The index of the point that was traversed. This can be used
     *                             to synchronize plugin state with the current location.
     */
    public void pointTraversed(ILocation traversedPoint, int simulationPointIndex);
}
