package gov.takx.core.playback.globe;

import gov.takx.api.annotation.InvokeAndWaitOnEDT;
import gov.takx.api.annotation.InvokeOnEDT;
import gov.takx.api.annotation.PseudoBean;
import gov.takx.api.annotation.RemoveObserver;
import gov.takx.api.event.mapobject.MapObjectRemoved;
import gov.takx.api.event.project.ProjectClosing;
import gov.takx.api.globe.IGlobeDelegate;
import gov.takx.api.globe.IGlobeManager;
import gov.takx.api.mapobject.IMapObject;
import gov.takx.api.mapobject.IMapObjectManager;
import gov.takx.api.playback.IPlaybackManager;
import gov.takx.api.playback.PlaybackEvent;
import gov.takx.api.playback.globe.IDeviceTracksModel;
import gov.takx.api.playback.globe.IGlobeModel;
import gov.takx.api.playback.globe.IGlobePlaybackLane;
import gov.takx.commons.lang.AlphaNumericComparator;
import gov.takx.core.playback.ui.GlobalTimeControl;
import gov.takx.platform.playback.TimeBoundary;
import gov.takx.platform.playback.TimeSpanAdapter;
import gov.takx.platform.playback.ui.TrackPanel;
import jakarta.annotation.PostConstruct;
import jakarta.enterprise.event.Observes;
import jakarta.inject.Inject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.JPanel;

import java.awt.Color;
import java.lang.invoke.MethodHandles;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentSkipListSet;

/**
 * A Globe playback lane represents a set of devices and their track points.  Each lane can display the track point
 * data for an arbitrary number of devices.  The track points for the devices are shown in the time data (track) panel.
 *
 * @since 1.47
 */
@PseudoBean(singletonObserver = true)
public class GlobePlaybackLane implements IGlobePlaybackLane
{
    private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    private static final String LANE_NAME = "Devices";

    @Inject
    private IGlobeManager globeManager;

    @Inject
    private IPlaybackManager playbackManager;

    private boolean projectClosing;

    private final GlobeRepainter globeRepainter = new GlobeRepainter();
    private GlobalTimeControl globalTimeControl = new GlobalTimeControl();
    private GlobeInfoPanel globeInfoPanel;
    private TrackPanel globeTrackPanel;

    private final Set<IMapObject> mapObjects = new ConcurrentSkipListSet<>(new AlphaNumericComparator<>(IMapObject::getName));
    private final Map<IMapObject, GlobeDevicePanel> devicePanelMap = new ConcurrentHashMap<>();

    private boolean isLive = true;

    @PostConstruct
    private void postConstruct()
    {
        globeTrackPanel = new TrackPanel(this, playbackManager.getTimeSpan(), Color.GRAY.brighter());
        globeInfoPanel = new GlobeInfoPanel(this, LANE_NAME, globeTrackPanel);

        playbackManager.getTimeSpan().addListener(globeRepainter);
    }

    @Override
    public JPanel getInfoPanel()
    {
        return globeInfoPanel;
    }

    @Override
    public JPanel getTimeDataPanel()
    {
        return globeTrackPanel;
    }

    /**
     * Get the GlobalTimeControl
     * <p>
     * Note: Intentionally left out of the interface as there is no reason to expose it to the API
     *
     * @return the GlobalTimeControl
     */
    public JPanel getGlobalTimeControl()
    {
        return globalTimeControl;
    }

    @Override
    public TimeBoundary getTimeBoundary()
    {
        // Iterate over all our map objects and determine the spanning time boundary for their track models

        final IGlobeModel globeModel = playbackManager.getGlobeModel();
        final List<TimeBoundary> boundaries = new ArrayList<>(mapObjects.size());

        for (IMapObject mapObject : mapObjects)
        {
            // There exists a race condition where the track model for this device
            // has already been deleted, and we get a NPE. This happens when the time
            // boundary recalculation happens before the map object deletion finishes
            // completely, so we try to get the track model for a map object we're in the
            // process of deleting, thus it is safe to just skip this map object if the
            // device track model returns null.
            final var tracksModel = globeModel.getDeviceTracksModel(mapObject);
            if (tracksModel != null)
            {
                boundaries.add(tracksModel.getTimeBoundary());
            }
        }

        return TimeBoundary.union(boundaries);
    }

    @Override
    public void setBackgroundColor(Color color)
    {
        globeTrackPanel.setBackground(color);
        globalTimeControl.setBackground(color);
    }

    public void setMapObjectList(Collection<IMapObject> mapObjectsToShow)
    {
        handleRemovedMapObjects(mapObjectsToShow);

        handleNewMapObjects(mapObjectsToShow);

        mapObjects.clear();
        mapObjects.addAll(mapObjectsToShow); // Our new set of MOs

        // Now rebuild our internal panels
        globeInfoPanel.setDevices(mapObjects);

        disposeAllDevicePanels();

        buildDevicePanels();
    }

    @Override
    public void addMapObjects(Collection<IMapObject> mapObjectsToAdd)
    {
        final Set<IMapObject> allMOs = new HashSet<>(mapObjects);
        allMOs.addAll(mapObjectsToAdd);
        setMapObjectList(allMOs);
    }

    @Override
    public void removeMapObjects(Collection<IMapObject> mapObjectsToRemove)
    {
        final var allMOs = new ArrayList<>(mapObjects);
        if (allMOs.removeAll(mapObjectsToRemove)) setMapObjectList(allMOs);
    }

    @RemoveObserver
    private void onProjectClosing(@Observes ProjectClosing event)
    {
        // We really just want to avoid processing the clientMapObjectRemoved callbacks as the project is closed.

        projectClosing = true;
    }

    /**
     * Notified by {@link IMapObjectManager} when a map object is removed. If the MO being deleted is in our current
     * set, then remove it.
     *
     * @param event The event providing the removed map object
     * @since 2.38 - formerly clientMapObjectRemoved
     */
    private void onClientMapObjectRemoved(@Observes MapObjectRemoved event)
    {
        if (projectClosing) return; // Bail if the project is being closed

        IMapObject mapObject = event.getMapObject();
        if (mapObjects.contains(mapObject))
        {
            // Remove the deleted map object from our list and rebuild our world
            final List<IMapObject> copy = new ArrayList<>(mapObjects);
            copy.remove(mapObject);

            setMapObjectList(copy);
        } else
        {
            playbackManager.removeDevice(mapObject);
        }
    }

    @Override
    public Collection<IMapObject> getMapObjects()
    {
        return Collections.unmodifiableSet(mapObjects);
    }

    @InvokeOnEDT
    @Override
    public void displayConfigurationEditor()
    {
        final GlobeEditorDialog dialog = new GlobeEditorDialog(LANE_NAME, playbackManager.getGlobeModel(), mapObjects);

        dialog.setVisible(true);

        if (dialog.isOkPressed())
        {
            // Install the new configuration
            setMapObjectList(dialog.getSelectedMapObjects());
        }
    }

    // @InvokeOnEDT is used here to synchronize access of 'globeTrackPanel' to the EDT thread.
    // This will ideally prevent the lock-up issues described in 'buildDevicePanels()'.
    @Override
    @InvokeAndWaitOnEDT
    public void disposeResources()
    {
        logger.trace("GlobePlaybackLane.dispose: " + LANE_NAME);

        playbackManager.getTimeSpan().removeListener(globeRepainter);

        globeInfoPanel.disposeResources();
        globeInfoPanel = null;

        disposeAllDevicePanels();
        globeTrackPanel = null;
        globalTimeControl = null;
    }

    /**
     * Determine which Map Objects are new and inform the playback manager that we want them in playback.
     *
     * @param newMapObjects Updated list of MOs we're handling
     */
    private void handleNewMapObjects(Collection<IMapObject> newMapObjects)
    {
        final Set<IMapObject> newMOs = new HashSet<>(newMapObjects);
        newMOs.removeAll(mapObjects);

        // Inform the playback system that we're interested in these new MOs
        newMOs.forEach(mapObject -> playbackManager.getGlobeModel().addDevice(mapObject));
    }

    /**
     * Determine which Map Objects are no longer to be displayed, and inform the playback manager that we're
     * done with them.
     *
     * @param mapObjects Updated list of MOs we're handling
     */
    private void handleRemovedMapObjects(Collection<IMapObject> mapObjects)
    {
        final var lostMOs = new HashSet<>(this.mapObjects);
        lostMOs.removeAll(mapObjects);

        // Inform the playback system that we're no longer interested in these MOs
        lostMOs.forEach(mapObject -> {
            this.mapObjects.remove(mapObject);
            playbackManager.getGlobeModel().removeDevice(mapObject);
        });
    }

    /**
     * Remove and dispose of all the device track panels.  Clear the panel map.
     */
    private void disposeAllDevicePanels()
    {
        if (globeTrackPanel != null)
        {
            globeTrackPanel.removeAll();

            for (GlobeDevicePanel devicePanel : devicePanelMap.values())
            {
                devicePanel.disposeResources();
            }

            devicePanelMap.clear();
        }
    }

    /**
     * Build device panels for each MO we're currently handling.
     * <br>
     * This was previously using @InvokeAndWaitOnEDT as a fix for 'RAPTORX-41'.
     * The issue was that 'globeTrackPanel' was being set to {@code null} during shutdown when the playback service gets stopped.
     * Using @InvokeAndWaitOnEDT would ensure that this is used before 'globeTrackPanel' is set to {@code null}, but with some side effects.
     * <br>
     * When this method is called as an observer (meaning the InstanceBean is locked) any additional calls to it will wait
     * until the bean is unlocked. A lock-up could occur if an operation from the EDT tries to dispatch an event back to the GlobePlaybackLane.
      */
    @InvokeOnEDT
    private void buildDevicePanels()
    {
        // The globe track panel may already be null during shutdown.
        final var localGlobeTrackPanel = globeTrackPanel;
        if (localGlobeTrackPanel == null) return;

        for (IMapObject mapObject : mapObjects)
        {
            final var devicePanel = new GlobeDevicePanel(playbackManager, mapObject, globeInfoPanel.getFirstRowHeight());

            devicePanelMap.put(mapObject, devicePanel);
            localGlobeTrackPanel.add(devicePanel);
        }
        localGlobeTrackPanel.repaint();
        globeInfoPanel.repaint();
    }

    @Override
    public String toString()
    {
        return "GlobePlaybackLane{" + LANE_NAME + "}";
    }

    /**
     * Handle a playback event, updating our live/stopped state accordingly.
     */
    private void onPlaybackEvent(@Observes PlaybackEvent event)
    {
        switch (event)
        {
            case Live ->
            {
                // remove all devices MO's from playback mode
                playbackManager.getGlobeModel().getAllDevices().forEach(mo -> mo.setPlaybackMode(false));
                isLive = true;
            }
            case Stopped ->
            {
                if (isLive)
                {
                    var allMos = playbackManager.getGlobeModel().getAllDevices();
                    // Add track models for devices not in lanes
                    handleNewMapObjects(allMos);
                    // put all devices MO's into playback mode
                    allMos.forEach(this::setSmartPlaybackMode);
                    isLive = false;
                }
            }
        }
    }

    private void setSmartPlaybackMode(IMapObject mo)
    {
        IDeviceTracksModel model = playbackManager.getGlobeModel().getDeviceTracksModel(mo);
        if (model != null && !model.getTrackPoints().isEmpty())
        {
            mo.setPlaybackMode(true);
        }
    }

    /**
     * Handler to force a redraw on the globe we're working with.  This ensures that the MO playback icons move
     * smoothly with the playback cursor instead of only updating every second.
     */
    private final class GlobeRepainter extends TimeSpanAdapter
    {
        @Override
        public void timePositionChanged()
        {
            // Only update the globe view if we are actually in playback mode, not live mode
            if (!playbackManager.isLive()) globeManager.getAllGlobeDelegates().forEach(IGlobeDelegate::redraw);
        }
    }
}
