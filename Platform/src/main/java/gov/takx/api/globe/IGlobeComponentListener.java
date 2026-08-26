package gov.takx.api.globe;

/**
 * Methods called when important events occur during the globe lifecycle.
 *
 */
public interface IGlobeComponentListener
{
    /**
     * Called when the globe has been initialized and is ready for use.
     */
    public void globeInitialized();

    /**
     * Called when the globe is about to shutdown.  All services are still available.  This is a good place
     * to add code that should clean up any globe resources (like layers) you may have created.
     */
    public void globeAboutToShutdown();

    /**
     * Called after the globe is fully shutdown.  The globe is not in a usable state at this point.
     */
    public void globeShutdown();
}
