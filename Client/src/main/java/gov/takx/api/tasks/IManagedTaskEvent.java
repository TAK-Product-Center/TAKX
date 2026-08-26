package gov.takx.api.tasks;

/**
 * Contract for managed task events.
 *
 * @see ManagedTaskEvents
 * @since 2.37
 */
public interface IManagedTaskEvent
{
    /**
     * @return Task involved in the event
     */
    IManagedTask getTask();
}
