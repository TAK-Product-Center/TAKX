package gov.takx.api.pathsimulation;

import gov.takx.api.coordinatesystem.ILocation;

import java.util.List;

/**
 * This interface is used to configure and control the path simulation.
 *
 * @see IPathSimulationService
 * @since 1.29
 */
public interface IPathSimulator
{
    /**
     * Starts the map object traversal along the simulation points
     */
    void play();

    /**
     * Stops the simulation and moves the map object back to the original position
     */
    void stop();

    /**
     * Leaves the map object at its current simulation point until play is again invoked.
     */
    void pause();

    /**
     * Moves the map object forward to the next simulation point
     */
    void stepForward();

    /**
     * Moves the map object to its previous simulation point
     */
    void stepBack();

    /**
     * Moves the map object (skipping any previous points) to the given simulation point index in the list of simulation points
     *
     * @param simulationPointIndex The index of the simulation point to jump to
     */
    void jumpToSimulationPoint(int simulationPointIndex);

    /**
     * Used to increase or decrease the simulation speed of the map object
     *
     * @param multiplier The multiplier used to determine simulation point traversal speed
     */
    void setPlaySpeedMultiplier(double multiplier);

    /**
     * Gets the simulation points that were generated along the defined path
     *
     * @return The generated simulation points
     */
    List<ILocation> getSimulationPoints();

    /**
     * Registers a listener that is interested in simulation events
     *
     * @param simulationEventListener The listener of simulation events
     */
    void addEventListener(IPathSimulationEventListener simulationEventListener);

    /**
     * Removes the simulation event listener
     *
     * @param simulationEventListener The listener to be removed
     */
    void removeEventListener(IPathSimulationEventListener simulationEventListener);

    /**
     * Used to determine if the simulation is currently "Playing"
     *
     * @return True if the simulation is "Playing" and false if it is not
     */
    boolean isSimulationRunning();

    /**
     * Gets the index of the simulation point that the map object is currently at
     *
     * @return The simulation point index of the map object
     */
    int getCurrentIndex();
}
