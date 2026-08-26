package gov.takx.api.dataportal;

import gov.takx.api.annotation.InjectableService;
import gov.takx.api.annotation.NoPluginImplementations;
import jakarta.annotation.Nonnull;

/**
 * Contract for registering/unregistering to data (RDM) message flows.
 * <p>
 * To access the {@link IDataListenerManager}, you can have it injected by CDI. Here is an example using field injection.
 * <pre>
 * &#64;Inject
 * private IDataListenerManager dataListenerManager;
 * </pre>
 *
 * @since 2.0.0
 */
@InjectableService
@NoPluginImplementations
public interface IDataListenerManager
{
    /**
     * Add a data listener. This listener will be notified of all data messages that flow through the data portal.
     * The listener will be added with {@link IRaptorDataPortalController#MEDIUM_PRIORITY medium} priority.
     *
     * @param listener Listener to add.
     * @see #addListener(IRaptorDataListener, int)
     */
    default void addListener(@Nonnull IRaptorDataListener listener)
    {
        addListener(listener, IRaptorDataPortalController.MEDIUM_PRIORITY);
    }

    /**
     * Add a data listener with the given priority.  Listeners with higher priority will get notified before
     * listeners with lower priority. This listener will be notified of all data messages that flow through the data
     * portal.
     *
     * @param listener Listener to add.
     * @param priority Priority of listener
     */
    void addListener(@Nonnull IRaptorDataListener listener, int priority);

    /**
     * Remove a data listener.
     *
     * @param listener Listener to remove.
     */
    void removeListener(@Nonnull IRaptorDataListener listener);
}
