package gov.takx.api.globe;

/**
 * Methods called when important events occur during the globe lifecycle.
 *
 * @since 1.44
 */
public interface IGlobeLifeCycleListener
{
    /**
     * Called when the globe has been initialized and is ready for use.
     *
     * @param globe the globe delegate
     */
    default void globeCreated(IGlobeDelegate globe)
    {
    }

    /**
     * Called when the globe is about to shutdown.  All services are still available.  This is a good place
     * to add code that should clean up any globe resources (like layers) you may have created.
     *
     * @param globe the globe delegate
     */
    default void globeAboutToBeRemoved(IGlobeDelegate globe)
    {
    }

    /**
     * Called after the globe is fully shutdown.  The globe is not in a usable state at this point.
     *
     * @param globe the globe delegate
     */
    default void globeRemoved(IGlobeDelegate globe)
    {
    }
}
