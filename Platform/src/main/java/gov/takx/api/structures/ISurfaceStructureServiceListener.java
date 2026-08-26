package gov.takx.api.structures;

/**
 * An interface for events originated by or propagating through the Surface Structure Service.
 *
 * @since 1.42
 */
public interface ISurfaceStructureServiceListener
{
    /**
     * Notification of a general surface structure service event.
     *
     * @param event event object with affected object, event name, and old and new values.
     * @since 1.42
     */
    void onEvent(SurfaceStructureServiceEvent event);
}
