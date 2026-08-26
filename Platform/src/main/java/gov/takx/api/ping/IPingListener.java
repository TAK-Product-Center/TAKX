package gov.takx.api.ping;

/**
 * Listener for ping events. Ping events are intended to temporarily draw the user's attention to
 * a geographic location. They are not map objects, and are not persisted.
 *
 */
public interface IPingListener
{
    /**
     * Called when a ping occurs.
     *
     * @param ping Information about the ping event.
     */
    public void onPing(IPingEvent ping);
}
