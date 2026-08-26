package gov.takx.core.playback.globe;

import gov.takx.api.annotation.PseudoBean;
import gov.takx.api.annotation.RemoveObserver;
import gov.takx.api.event.mapobject.MapObjectRemoved;
import gov.takx.api.mapobject.IMapObject;
import gov.takx.api.mapobject.IMapObjectManager;
import gov.takx.api.messages.DataStructureBuilder;
import gov.takx.api.messages.IStructure;
import gov.takx.api.playback.ITimeLocation;
import gov.takx.api.playback.ITimeSpan;
import gov.takx.api.playback.globe.IDeviceTracksModel;
import gov.takx.api.playback.globe.IGlobeModel;
import jakarta.annotation.Nonnull;
import jakarta.enterprise.event.Observes;
import jakarta.inject.Inject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.invoke.MethodHandles;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Predicate;
import java.util.stream.Collectors;

/**
 * This model handles all the devices that have been added for display in any globe lane.  This model is shared
 * among all instances of globe lanes.  Each device has an associated {@link DeviceTracksModel Model} that can
 * be queried.  That model maintains all the track point data for that device.
 *
 * @since 1.47
 */
@SuppressWarnings("CdiManagedBeanInconsistencyInspection")
@PseudoBean
public class GlobeModel implements IGlobeModel
{
    private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    // Predicate to match a renderable structure that is NOT the predefined LOCATION message structure
    private static final Predicate<IStructure> USABLE_STRUCTURE =
            structure -> structure.isRenderable() && !structure.getMessageType().equals(DataStructureBuilder.LOCATION_MESSAGE_TYPE);

    @Inject
    private IMapObjectManager mom;

    private final ITimeSpan timeSpan;
    private final Map<IMapObject, IDeviceTracksModel> deviceModels = new ConcurrentHashMap<>();

    public GlobeModel(ITimeSpan timeSpan)
    {
        this.timeSpan = timeSpan;
    }

    @Override
    public List<IMapObject> getAllDevices()
    {
        return mom.getMapObjects().stream().filter(this::eligibleForPlayback).collect(Collectors.toList());
    }

    @Override
    public boolean eligibleForPlayback(@Nonnull IMapObject mapObject)
    {
        return mapObject.canHaveStyledTrackData() && mapObject.getAllDataStructures().stream().anyMatch(USABLE_STRUCTURE);
    }

    @Override
    public IDeviceTracksModel addDevice(IMapObject device)
    {
        return addDevice(device, true);
    }

    @Override
    public IDeviceTracksModel addDevice(IMapObject device, boolean setDevicePlayback)
    {
        IDeviceTracksModel model = deviceModels.get(device);

        if (model == null)
        {
            model = new DeviceTracksModel(timeSpan, device);
            deviceModels.put(device, model);
            logger.debug("Adding {}", device);
        }

        return model;
    }

    @Override
    public boolean removeDevice(IMapObject device)
    {
        final var tracksModel = deviceModels.remove(device);
        if (tracksModel != null) tracksModel.disposeResources();
        return true;
    }

    @Override
    public List<IMapObject> getDevicesList()
    {
        return new ArrayList<>(deviceModels.keySet());
    }

    @Override
    public boolean isDeviceInPlayback(IMapObject device)
    {
        return deviceModels.containsKey(device);
    }

    @Override
    public IDeviceTracksModel getDeviceTracksModel(IMapObject device)
    {
        return deviceModels.get(device);
    }

    @Override
    public ITimeLocation getPreviousTrackPointInPlayback(long time)
    {
        ITimeLocation prev = null;

        for (IDeviceTracksModel tracksModel : deviceModels.values())
        {
            final ITimeLocation tp = tracksModel.getPreviousTrackPointInPlayback(time);

            if (tp == null) continue;

            if (prev == null || tp.getTime() > prev.getTime())
            {
                prev = tp;
            }
        }

        return prev;
    }

    @Override
    public ITimeLocation getNextTrackPointInPlayback(long playbackTime)
    {
        ITimeLocation next = null;

        for (IDeviceTracksModel tracksModel : deviceModels.values())
        {
            final ITimeLocation tp = tracksModel.getNextTrackPointInPlayback(playbackTime);

            if (tp == null) continue;

            if (next == null || tp.getTime() < next.getTime())
            {
                next = tp;
            }
        }

        return next;
    }

    /**
     * Notified by {@link IMapObjectManager} when a map object is removed.
     *
     * @param event The event providing the removed map object
     * @since 2.38 - formerly clientMapObjectRemoved
     */
    private void onClientMapObjectRemoved(@Observes MapObjectRemoved event)
    {
        IMapObject mapObject = event.getMapObject();
        final IDeviceTracksModel tracksModel = deviceModels.remove(mapObject);

        if (tracksModel != null)
        {
            logger.debug("Removing {}", mapObject);
            tracksModel.disposeResources();
        }
    }

    /**
     * Clear out all our data.
     */
    @RemoveObserver
    @Override
    public void disposeResources()
    {
        if (logger.isTraceEnabled()) logger.trace("GlobeModel.dispose");

        deviceModels.values().forEach(IDeviceTracksModel::disposeResources);

        deviceModels.clear();
        if (logger.isDebugEnabled()) logger.debug("Clearing deviceModels");
    }
}
