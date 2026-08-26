package gov.takx.test.mock.mapobject;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * PCL for listening for notifications from a map object. Employs a map of events to track the received events.
 *
 * @since 3.0 (was previously an inner class on MapObjectTest)
 */
public class TestMapObjectPropertyChangeListener implements PropertyChangeListener
{
    private final Map<String, List<PropertyChangeEvent>> eventMap = new HashMap<>();

    @Override
    public void propertyChange(PropertyChangeEvent event)
    {
        eventMap.computeIfAbsent(event.getPropertyName(), k -> new ArrayList<>()).add(event);
    }

    /**
     * Gets the number of events received for a specified event type.
     *
     * @param eventPropertyName The property name for the event
     * @return The count of received events
     */
    public int getCountForEventType(String eventPropertyName)
    {
        return getEventsForEventType(eventPropertyName).size();
    }

    /**
     * Gets the events received for a specified event type.
     *
     * @param eventPropertyName The property name for the event
     * @return The received events
     */
    public List<PropertyChangeEvent> getEventsForEventType(String eventPropertyName)
    {
        return eventMap.getOrDefault(eventPropertyName, Collections.emptyList());
    }

    /**
     * Gets the type of events that were received.
     *
     * @return The set of event types for which events were received.
     */
    public Set<String> getReceivedEventTypes()
    {
        return eventMap.keySet();
    }

    /**
     * Clears the event counters as well as the set of received event types.
     */
    public void clearEvents()
    {
        eventMap.clear();
    }
}
