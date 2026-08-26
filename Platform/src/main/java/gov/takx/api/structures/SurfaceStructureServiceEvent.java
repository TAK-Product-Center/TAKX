package gov.takx.api.structures;

/**
 * Event class for events originating from or propagating through
 * {@link ISurfaceStructureService}.
 *
 * @since 1.42
 */
public class SurfaceStructureServiceEvent
{
    private final Object source;
    private final SurfaceStructureServiceEventType eventType;
    private final String eventName;
    private final Object oldValue;
    private final Object newValue;

    public SurfaceStructureServiceEvent(Object source,
                                        SurfaceStructureServiceEventType eventType,
                                        String eventName,
                                        Object oldValue,
                                        Object newValue)
    {
        this.source = source;
        this.eventType = eventType;
        this.eventName = eventName;
        this.oldValue = oldValue;
        this.newValue = newValue;
    }

    /**
     * Get the source object for this event.  The source object type is dependent upon the event type. If this is a
     * metadata change event, the object may either be a surface structure or an intersectable shape.  If the event
     * type is a property change, then the source is an intersectable shape.  Other event types may provide different
     * source object types.
     *
     * @return the source object for this event.
     * @since 1.42
     */
    public Object getSource()
    {
        return source;
    }

    /**
     * Get the event type string.  The event type will determine whether this is a general service structure service
     * event type, a property change event type, or a metadata change event type.
     *
     * @return the string event type determining whether this is a general event type, property change event type or
     * metadata change event type.
     * @since 1.42
     */
    public SurfaceStructureServiceEventType getEventType()
    {
        return eventType;
    }

    /**
     * Get event name.  Depending on the event, this may also be a property name or metadata key.
     *
     * @return event name.
     * @since 1.42
     */
    public String getEventName()
    {
        return eventName;
    }

    /**
     * Get the value in-place prior to the event.
     *
     * @return the old value.
     * @since 1.42
     */
    public Object getOldValue()
    {
        return oldValue;
    }

    /**
     * Get the new value.
     *
     * @return the new value.
     * @since 1.42
     */
    public Object getNewValue()
    {
        return newValue;
    }

    /**
     * Convenience method to determine whether the source of this event is an intersectable shape.
     *
     * @return {@code true} if the source of this event is an intersectable shape, or {@code false}
     * otherwise.
     * @since 1.42
     */
    public boolean isSourceIntersectableShape()
    {
        return source instanceof IIntersectableShape;
    }

    /**
     * Convenience method to determine whether the source of this event is a surface structure.
     *
     * @return {@code true} if the source of this event is a surface structure, or {@code false} otherwise.
     * @since 1.42
     */
    public boolean isSourceSurfaceStructure()
    {
        return source instanceof ISurfaceStructure;
    }

    @Override
    public String toString()
    {
        return "SurfaceStructureServiceEvent{" +
                "source=" + source +
                ", eventType=" + eventType +
                ", eventName='" + eventName + '\'' +
                ", oldValue=" + oldValue +
                ", newValue=" + newValue +
                '}';
    }
}
