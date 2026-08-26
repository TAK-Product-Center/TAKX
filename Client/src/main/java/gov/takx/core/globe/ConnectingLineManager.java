package gov.takx.core.globe;

import gov.nasa.worldwind.render.DrawContext;
import gov.takx.api.annotation.Headless;
import gov.takx.api.event.StartProjectServices;
import gov.takx.api.event.StopProjectServices;
import gov.takx.api.event.mapobject.MapObjectAdded;
import gov.takx.api.event.mapobject.MapObjectRemoved;
import gov.takx.api.globe.IConnectingLineManager;
import gov.takx.api.mapobject.IIconMapObject;
import gov.takx.api.mapobject.IMapObject;
import gov.takx.api.mapobject.IMapObjectManager;
import gov.takx.api.playback.IPlaybackEventListener;
import gov.takx.api.playback.IPlaybackManager;
import gov.takx.api.playback.PlaybackEvent;
import gov.takx.core.project.ServiceOrdering;
import gov.takx.platform.utils.TimeBoundedTask;
import jakarta.annotation.Priority;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import jakarta.inject.Inject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.invoke.MethodHandles;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * This class is responsible for managing all the connecting line objects that need to be created for
 * map objects that have connecting lines enabled.
 *
 */
@ApplicationScoped
public class ConnectingLineManager implements IConnectingLineManager, IPlaybackEventListener
{
    private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());
    private static final long INITIAL_UPDATE_DELAY = 100;
    private static final long MAX_UPDATE_DELAY = 300;

    private final Map<Integer, ConnectingLine> connectingLines = new ConcurrentHashMap<>();

    private boolean forceSnapToSurface = false;

    @Inject
    @Headless
    private boolean headless;

    @Inject
    private IPlaybackManager playbackManager;

    private final TimeBoundedTask updateHandler = new TimeBoundedTask("update connecting lines", INITIAL_UPDATE_DELAY, MAX_UPDATE_DELAY, () -> connectingLines.values().forEach(ConnectingLine::updatePositions));

    @Override
    public void render(DrawContext dc)
    {
        for (ConnectingLine cl : connectingLines.values())
        {
            cl.render(dc);
        }
    }

    /**
     * Set the override snap to surface.  This is typically used when the current rendering engine doesn't support
     * full 3D elevations).
     *
     * @param forceSnapToSurface Pass {@code true} to force the connecting lines to render on the surface
     */
    @Override
    public void setForceSnapToSurface(boolean forceSnapToSurface)
    {
        this.forceSnapToSurface = forceSnapToSurface;

        for (ConnectingLine cl : connectingLines.values())
        {
            cl.setForceSnapToSurface(forceSnapToSurface);
        }
    }

    /**
     * Notified by {@link IMapObjectManager} when a new map object is added so we can construct our ConnectingLine.
     *
     * @param event The event providing the added map object
     * @since 2.38 - formerly clientMapObjectAdded
     */
    private void onClientMapObjectAdded(@Observes MapObjectAdded event)
    {
        if (headless) return;

        IMapObject mapObject = event.getMapObject();

        // We only care about map objects of the correct type
        if (mapObject.canHaveStyledTrackData())
        {
            if (logger.isDebugEnabled()) logger.debug("Add MO: {}", mapObject);

            IIconMapObject imo = (IIconMapObject) mapObject;

            final Integer moId = mapObject.getId();
            if (!connectingLines.containsKey(moId))
            {
                // Create the ConnectingLine for this MapObject
                final ConnectingLine connectingLine = new ConnectingLine(imo);
                connectingLine.setForceSnapToSurface(forceSnapToSurface);
                connectingLines.put(moId, connectingLine);
            }
        }
    }

    /**
     * Notified by {@link IMapObjectManager} when a map object is removed so we can remove it from our control maps as well.
     *
     * @param event The event providing the removed map object
     * @since 2.38 - formerly clientMapObjectRemoved
     */
    private void onClientMapObjectRemoved(@Observes MapObjectRemoved event)
    {
        if (headless) return;

        IMapObject mapObject = event.getMapObject();

        // We only care about map objects of the correct type
        if (mapObject.canHaveStyledTrackData())
        {
            if (logger.isDebugEnabled()) logger.debug("Remove MO: {}", mapObject);

            final ConnectingLine cl = connectingLines.remove(mapObject.getId());

            if (cl != null)
            {
                cl.cleanup();
            } else
            {
                logger.warn("No ConnectingLine for MapObject: {}", mapObject);
            }
        }
    }

    /**
     * Start our service.  Only really start if we are running headed and we're not under test.
     */
    private void startService(@Observes @Priority(ServiceOrdering.START_GenericCoreService) StartProjectServices ignored)
    {
        if (headless) return;

        playbackManager.addPlaybackEventListener(this);
    }

    /**
     * Stop our service.
     */
    private void stopService(@Observes @Priority(ServiceOrdering.STOP_GenericCoreService) StopProjectServices ignored)
    {
        if (headless) return;

        playbackManager.removePlaybackEventListener(this);

        // Cleanup all of our connecting lines
        connectingLines.values().forEach(ConnectingLine::cleanupFast);
        connectingLines.clear();
    }

    /**
     * Listen for playback events where the timeline position is changing. Where applicable, adjust the path for the
     * connecting line. The ConnectingLine object will make its own determination if the line is shorter than playback
     * allows and will adjust its "positions" accordingly.
     * <p>
     * We only respond to PlayPositionChanged and ScrubPositionChanged here
     *
     * @param playbackEvent The event fired
     * @since 2.32
     */
    @Override
    public void onPlaybackEvent(PlaybackEvent playbackEvent)
    {
        if (!playbackManager.isLive()
                && (playbackEvent == PlaybackEvent.PlayPositionChanged || playbackEvent == PlaybackEvent.ScrubPositionChanged))
        {
            updateHandler.request();
        }
    }
}
