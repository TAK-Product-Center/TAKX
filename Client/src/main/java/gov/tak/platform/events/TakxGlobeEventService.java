package gov.tak.platform.events;

import gov.nasa.worldwind.WorldWindow;
import gov.tak.api.annotation.NonNull;
import gov.tak.api.events.IGlobeEvent;
import jakarta.enterprise.context.ApplicationScoped;

import java.util.concurrent.ConcurrentHashMap;

/**
 * TAKX implementation of IGlobeEventService
 *
 * @since 3.3.0
 */
@ApplicationScoped
public class TakxGlobeEventService extends AbstractGlobeEventService implements NwwGlobeEventsSource.RaiseEventDelegate
{
    private final ConcurrentHashMap<WorldWindow, NwwGlobeEventsSource> eventSources = new ConcurrentHashMap<>();

    /**
     * Add a WorldWindow event source
     *
     * @param nwwEventSource the event source
     * @param globeId the globe id
     */
    public void addEventSource(@NonNull WorldWindow nwwEventSource, int globeId)
    {
        NwwGlobeEventsSource eventSource = eventSources.computeIfAbsent(nwwEventSource, k -> new NwwGlobeEventsSource(nwwEventSource, globeId, this));
        eventSource.start();
    }

    /**
     * Remove an existing WorldWindow event source
     *
     * @param nwwEventSource the event source
     */
    public void removeEventSource(WorldWindow nwwEventSource)
    {
        final NwwGlobeEventsSource removedEventSource = eventSources.remove(nwwEventSource);
        if (removedEventSource != null) removedEventSource.stop();
    }

    @Override
    public void raiseGlobeEvent(IGlobeEvent globeEvent)
    {
        if (globeEvent != null)
        {
            raiseTopLevelGlobeEvent(globeEvent);
        }
    }
}
