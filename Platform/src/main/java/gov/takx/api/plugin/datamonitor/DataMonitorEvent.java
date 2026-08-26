package gov.takx.api.plugin.datamonitor;

import gov.takx.api.mapobject.IDataMonitor;

import java.util.Collections;
import java.util.EventObject;
import java.util.Map;
import java.util.Set;

/**
 * An object used to notify {@link IDataMonitor}s that an event has taken place that
 * may affect their behavior or state.
 * <p>
 * {@link IDataMonitor}s are notified of these events via the method
 * {@link IDataMonitor#onDataMonitorEvent(DataMonitorEvent)}.
 *
 * @see DataMonitorEventType
 * @since 1.10
 */
public class DataMonitorEvent extends EventObject
{
    /**
     * The extensible list of properties associated with this event.
     */
    private final Map<String, Object> properties;

    /**
     * The type of event this is as specified in the constants defined in {@link DataMonitorEventType}.
     */
    private final DataMonitorEventType type;

    /**
     * The name of this event.
     */
    private final String name;

    /**
     * Constructor for sending an event, using the event's name, and with no custom properties.
     *
     * @param source The object that is creating this event
     * @param type   The type of this event
     * @since 2.29
     */
    public DataMonitorEvent(Object source, DataMonitorEventType type)
    {
        this(source, type.name(), type, Collections.emptyMap());
    }

    /**
     * Constructor, defaulting the name to the name of the {@code type} given.
     *
     * @param source     The object that is creating this event.
     * @param type       The type of this event.
     * @param properties A custom set of properties associated with this event.
     * @see DataMonitorEventType#name()
     * @since 3.2
     */
    public DataMonitorEvent(Object source, DataMonitorEventType type, Map<String, Object> properties)
    {
        this(source, type.name(), type, properties);
    }

    /**
     * Constructor.
     *
     * @param source     The object that is creating this event.
     * @param name       The name of this event. In most cases, the name will be the String value returned by
     *                   {@link DataMonitorEventType#name()}. If {@link DataMonitorEventType#Command} is used, the
     *                   user <b>must</b> specify a custom name that will identify the type of command this event
     *                   represents.
     * @param type       The type of this event.
     * @param properties A custom set of properties associated with this event.
     */
    public DataMonitorEvent(Object source, String name, DataMonitorEventType type, Map<String, Object> properties)
    {
        super(source);

        //noinspection AssignmentOrReturnOfFieldWithMutableType
        this.properties = properties;
        this.name = name;
        this.type = type;
    }

    /**
     * Returns the type of event this is.
     *
     * @return The type of event this is.
     */
    public DataMonitorEventType getEventType()
    {
        return type;
    }

    /**
     * Returns the name of this event. Typically, this is identical to the name of the DataMonitorEventType, unless
     * it is a Command. When the type is Command, the event name represents the arbitrary custom event name
     * as defined by the developer.
     *
     * @return The name of this event.
     */
    public String getEventName()
    {
        return name;
    }

    /**
     * Returns the property mapped to the specified key (if it exists).
     *
     * @param key The key mapped to the desired property.
     * @return The property mapped to the key; {@code null} if the property does not exist.
     */
    public Object getProperty(String key)
    {
        return properties.get(key);
    }

    /**
     * @return the set of all the keys in the property map.
     */
    public Set<String> getKeys()
    {
        return properties.keySet();
    }
}
