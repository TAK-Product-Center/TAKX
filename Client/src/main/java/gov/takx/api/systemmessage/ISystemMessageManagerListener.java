package gov.takx.api.systemmessage;

/**
 * Contract for components that want notification of {@link ISystemMessageManager} events.
 *
 * @since 1.43
 */
public interface ISystemMessageManagerListener
{
    /**
     * A new system message has been posted.
     *
     * @param systemMessage System message just posted
     * @since 3.4 (added default implementation)
     */
    default void newSystemMessage(SystemMessage systemMessage)
    {
    }

    /**
     * Called whenever the number of outstanding messages changes, either by the arrival of a new message
     * or when the outstanding count is reset.
     * @since 3.4 (added default implementation)
     */
    default void outstandingCountChanged()
    {
    }
}
