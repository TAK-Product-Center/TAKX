package gov.takx.core.ping;

import gov.takx.api.coordinatesystem.ILocation;
import gov.takx.api.event.geo.GeoPing;
import gov.takx.api.event.project.ProjectClosing;
import gov.takx.api.ping.IGeoPingService;
import gov.takx.api.ping.IPingEvent;
import gov.takx.api.ping.IPingListener;
import gov.takx.commons.swing.GraphicsUtils;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Event;
import jakarta.enterprise.event.Observes;
import jakarta.inject.Inject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.Color;
import java.lang.invoke.MethodHandles;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Implementation of Ping service.
 *
 */
@ApplicationScoped
public class GeoPingService implements IGeoPingService
{
    private final static Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    @Inject
    private Event<GeoPing> geoPingEvent;

    private final List<IPingListener> listeners = new CopyOnWriteArrayList<>();

    @Override
    public void ping(ILocation location)
    {
        ping(location.getLatitude(), location.getLongitude(), DEFAULT_COLOR, DEFAULT_DURATION);
    }

    @Override
    public void ping(double lat, double lon)
    {
        ping(lat, lon, DEFAULT_COLOR, DEFAULT_DURATION);
    }

    @Override
    public void ping(ILocation location, Color color, int duration)
    {
        ping(location.getLatitude(), location.getLongitude(), color, duration);
    }

    @Override
    public void ping(double lat, double lon, Color color, int duration)
    {
        ping(new PingEvent(lat, lon, color, duration));
    }

    @Override
    public void userPing(double lat, double lon, Color color, int duration)
    {
        ping(new PingEvent(lat, lon, color, duration, false));
    }

    @Override
    public void userPing(double lat, double lon, Color color, int duration, boolean playAudio, GraphicsUtils.Shape shape, boolean verticalFlip)
    {
        ping(new PingEvent(lat, lon, color, duration, playAudio, shape, verticalFlip, false, false));
    }

    @Override
    public void ping(IPingEvent event)
    {
        if (logger.isDebugEnabled()) logger.debug("Fire Ping: {}", event);

        // Fire it as a CDI event
        geoPingEvent.fire(new GeoPing(event));

        // Direct callback to listeners (for backward compatibility)
        for (IPingListener listener : listeners)
        {
            try
            {
                listener.onPing(event);
            } catch (Exception e)
            {
                logger.warn("Exception notifying listener of ping", e);
            }
        }
    }

    @Override
    public void addListener(IPingListener listener)
    {
        listeners.add(listener);
    }

    @Override
    public void removeListener(IPingListener listener)
    {
        listeners.remove(listener);
    }

    /**
     * Clear our listener list when the project is closing.
     */
    private void handleProjectClosing(@SuppressWarnings("UnusedParameters") @Observes ProjectClosing event)
    {
        listeners.clear();
    }
}
