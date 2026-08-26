package gov.takx.core.pathsimulation;

import gov.nasa.worldwind.geom.LatLon;
import gov.takx.api.annotation.PseudoBean;
import gov.takx.api.coordinatesystem.ILocation;
import gov.takx.api.dataportal.DefaultRaptorDataListener;
import gov.takx.api.dataportal.IRaptorDataPortal;
import gov.takx.api.dataportal.IRaptorDataPortalController;
import gov.takx.api.globe.IGlobeManager;
import gov.takx.api.mapobject.IMapObject;
import gov.takx.api.messages.IMapObjectOfflineMessage;
import gov.takx.api.messages.IPrePersistRaptorDataMessage;
import gov.takx.api.pathsimulation.IPathSimulationEventListener;
import gov.takx.api.pathsimulation.IPathSimulationService;
import gov.takx.api.pathsimulation.IPathSimulator;
import gov.takx.core.globe.ApplicationGlobe;
import jakarta.annotation.Nonnull;
import jakarta.annotation.PostConstruct;
import jakarta.inject.Inject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.invoke.MethodHandles;
import java.util.ArrayList;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicReference;

/**
 * @since 1.29
 */
@SuppressWarnings("CdiManagedBeanInconsistencyInspection")
@PseudoBean
public class PathSimulator extends DefaultRaptorDataListener implements IPathSimulator
{
    private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    @Inject
    private IRaptorDataPortal dataPortal;

    @Inject
    private IGlobeManager globeManager;

    private static final int ONE_SECOND = 1000;
    private static final String MO_ERROR = "The path simulation map object must not be null.";

    private IMapObject mapObject;
    private final List<ILocation> generatedSimulationPoints;
    private final List<IPathSimulationEventListener> simulationEventListeners = new CopyOnWriteArrayList<>();
    private double playSpeedMultiplier = 1;
    private final AtomicReference<Timer> timer = new AtomicReference<>(null);
    private int currentIndex = -1;

    /**
     * @see IPathSimulationService#createSimulator(java.util.List, IMapObject, double)
     */
    public PathSimulator(List<ILocation> path, IMapObject mapObject, double maxDistanceBetweenSimulationPoints)
    {
        if (mapObject == null)
        {
            throw new IllegalArgumentException(MO_ERROR);
        }
        if (path == null || path.isEmpty())
        {
            throw new IllegalArgumentException("A valid path must be used for simulation.");
        }
        this.mapObject = mapObject;

        List<LatLon> positions = locationsToPositions(path);
        PathMeasurementTool pathAsPositions = new PathMeasurementTool(positions);
        pathAsPositions.setMaxSegmentLength(maxDistanceBetweenSimulationPoints);
        generatedSimulationPoints = pathAsPositions.getPathLocations();
    }

    @PostConstruct
    private void postConstruct()
    {
        dataPortal.addListener(this);
    }

    @Override
    public boolean matchMapObject(IMapObject mapObject)
    {
        return mapObject.equals(this.mapObject);
    }

    @Override
    public void onMapObjectOffline(IMapObjectOfflineMessage msg, IRaptorDataPortalController controller, boolean wipeHistoricalData)
    {
        dataPortal.removeListener(this);
        mapObject = null;
    }

    private void validateMapObject(IMapObject mapObject)
    {
        if (mapObject == null)
        {
            stopTimer();
            throw new IllegalStateException(MO_ERROR);
        }
    }

    private static List<LatLon> locationsToPositions(@Nonnull List<ILocation> path)
    {
        List<LatLon> positions = new ArrayList<>();
        for (ILocation location : path)
        {
            positions.add(LatLon.fromDegrees(location.getLatitude(), location.getLongitude()));
        }
        return positions;
    }

    @Override
    public void play()
    {
        if (!isSimulationRunning())
        {
            if (currentIndex == generatedSimulationPoints.size() - 1)
            {
                currentIndex = -1;
            }

            Timer t = new Timer();
            timer.set(t);
            long period = (long) (ONE_SECOND / playSpeedMultiplier);
            t.schedule(new TimerTask()
            {
                @Override
                public void run()
                {
                    if (currentIndex + 1 < generatedSimulationPoints.size())
                    {
                        currentIndex++;
                        updateLocation(true);
                    } else
                    {
                        pause();
                    }
                }
            }, 0, period);
        }
    }

    /**
     * @param isCancelable if flag is set to true, this method can be canceled (the actual location update and
     *                     postPointTraversal() methods will be skipped).  Cancel is determined by the
     *                     isSimulationRunning() method.
     */
    private void updateLocation(boolean isCancelable)
    {
        validateMapObject(mapObject);

        IPrePersistRaptorDataMessage rdmForLocation = null;

        // Get the location for the current index
        ILocation newLocation = generatedSimulationPoints.get(currentIndex);

        for (IPathSimulationEventListener simulationEventListener : simulationEventListeners)
        {
            // Notify listeners of a location change and use their supplied rdm.
            IPrePersistRaptorDataMessage customRDM = simulationEventListener.prePointTraversal(newLocation, currentIndex);
            // Check to see if we already have an RDM. If so ignore any others and log it.
            if (rdmForLocation != null && customRDM != null)
            {
                logger.warn("An IRaptorDataMessage has already been supplied by a different IPathSimulationEventListener.");
            } else if (customRDM != null)
            {
                // Use the plugin supplied RDM
                rdmForLocation = customRDM;
            }
        }

        /*
         * Determine whether the simulator is currently running before updating the device location and calling
         * pointTraversed() on the listeners.  It is possible for the simulator to be paused/stopped at some point when
         * prePointTraversed() is called on the listeners.
         */
        if (isCancelable && !isSimulationRunning())
        {
            logger.debug("Simulation is not running - canceling update location");
            return;
        }

        sendLocationMessage(rdmForLocation, newLocation);

        for (IPathSimulationEventListener simulationEventListener : simulationEventListeners)
        {
            simulationEventListener.pointTraversed(newLocation, currentIndex);
        }
    }

    private void sendLocationMessage(IPrePersistRaptorDataMessage rdmForLocation, ILocation newLocation)
    {
        if (mapObject == null)
        {
            logger.warn("Can not update the map object's location since it is null. Expected on a map object delete.");
            return;
        }

        if (rdmForLocation == null)
        {
            // No RDM was supplied so just create one for this location
            rdmForLocation = mapObject.createLocationDataMessage();
        }

        // Set the RDMs location for this index
        rdmForLocation.setLocation(newLocation.getLatitude(), newLocation.getLongitude(), newLocation.getElevation());
        // Send the message
        dataPortal.sendToGateway(rdmForLocation);

        // Redraw after every location change if the play speed multiplier is less than the redraw delay. Otherwise, we
        // could miss location changes
        if (playSpeedMultiplier < ApplicationGlobe.REDRAW_DELAY)
        {
            globeManager.requestRedraw("simulation update");
        }
    }

    @Override
    public void stop()
    {
        if (isSimulationRunning())
        {
            stopTimer();
        }
        currentIndex = -1;
        // Don't send a message if the stop was triggered by a map object deletion
        //noinspection VariableNotUsedInsideIf
        if (mapObject == null)
        {
            logger.warn(MO_ERROR);
        } else
        {
            // Stop can be called before the map object reference is removed on a map object deletion.
            if (mapObject.getController() != null)
            {
                sendLocationMessage(null, generatedSimulationPoints.get(0));
            }
        }
    }

    @Override
    public void pause()
    {
        stopTimer();
    }

    private void stopTimer()
    {
        if (isSimulationRunning())
        {
            Timer t = timer.getAndSet(null);
            if (t != null)
            {
                t.cancel();
            }
        }
    }

    @Override
    public void stepForward()
    {
        stopTimer();
        // If we are at the last point loop back to the beginning
        if (currentIndex + 1 > generatedSimulationPoints.size() - 1)
        {
            logger.info("Reached the end of the simulation path.");
        } else
        {
            currentIndex++;
            updateLocation(false);
        }
    }

    @Override
    public void stepBack()
    {
        stopTimer();
        // If we are at the last point loop back to the beginning
        if (currentIndex - 1 < 0)
        {
            logger.info("Reached the beginning of the simulation path.");
        } else
        {
            currentIndex--;
            updateLocation(false);
        }
    }

    @Override
    public void jumpToSimulationPoint(int simulationPointIndex) throws IndexOutOfBoundsException
    {
        if (simulationPointIndex < 0 || simulationPointIndex > generatedSimulationPoints.size() - 1)
        {
            String message = "The simulation point index " + simulationPointIndex + " is outside of the valid range: 0 and " + (generatedSimulationPoints.size() - 1);
            throw new IndexOutOfBoundsException(message);
        } else
        {
            stopTimer();
            currentIndex = simulationPointIndex;
            updateLocation(false);
        }
    }

    @Override
    public void setPlaySpeedMultiplier(double playSpeedMultiplier)
    {
        this.playSpeedMultiplier = playSpeedMultiplier;
        if (isSimulationRunning())
        {
            // Restart the play timer with the new delay but keep the current location index
            stopTimer();
            play();
        }
    }

    @Override
    public boolean isSimulationRunning()
    {
        return timer.get() != null;
    }

    @Override
    public int getCurrentIndex()
    {
        return currentIndex;
    }

    @Override
    public List<ILocation> getSimulationPoints()
    {
        return generatedSimulationPoints;
    }

    @Override
    public void addEventListener(IPathSimulationEventListener simulationEventListener)
    {
        simulationEventListeners.add(simulationEventListener);
    }

    @Override
    public void removeEventListener(IPathSimulationEventListener simulationEventListener)
    {
        simulationEventListeners.remove(simulationEventListener);
    }
}
