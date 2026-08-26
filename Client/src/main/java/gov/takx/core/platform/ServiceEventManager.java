package gov.takx.core.platform;

import gov.takx.api.event.CoreServicesStarted;
import gov.takx.api.event.CoreServicesStopping;
import gov.takx.api.event.StartPlugins;
import gov.takx.api.event.StopProjectServices;
import gov.takx.core.project.ServiceOrdering;
import jakarta.annotation.Priority;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Event;
import jakarta.enterprise.event.Observes;
import jakarta.inject.Inject;

/**
 * Service to send appropriate platform events as core services are started/stopped.
 *
 * @since 2.30
 */
@SuppressWarnings("unused")
@ApplicationScoped
public class ServiceEventManager
{
    @Inject
    private Event<CoreServicesStarted> coreServicesStartedEvent;

    @Inject
    private Event<CoreServicesStopping> coreServicesStoppingEvent;

    /**
     * Fire the core services started event to all containers.  Note that this event occurs on the {@code StartPlugins}
     * event, and not the normal {@code StartProjectServices} event, because we need things like the MapObjectManager
     * to have restored all MapObjects prior to firing this event.
     */
    private void fireServicesStarted(@Observes @Priority(ServiceOrdering.START_PLUGINS_ServicesStarted) StartPlugins ignored)
    {
        coreServicesStartedEvent.fire(new CoreServicesStarted());
    }

    /**
     * Fire the core services stopping event to all containers.
     */
    private void fireServicesStopping(@Observes @Priority(ServiceOrdering.STOP_ServiceStopping) StopProjectServices ignored)
    {
        coreServicesStoppingEvent.fire(new CoreServicesStopping());
    }
}
