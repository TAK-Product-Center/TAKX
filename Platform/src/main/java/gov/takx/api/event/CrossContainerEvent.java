package gov.takx.api.event;

/**
 * Tag interface for events that can be fired across plugin deployment containers.  Any plugin that wants to fire
 * a CDI event that can be observed in dependent containers must have the event type implement this interface.
 *
 * @since 2.35
 */
public interface CrossContainerEvent
{
}
