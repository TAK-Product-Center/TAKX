package gov.takx.core.persist;

import gov.takx.api.event.StartProjectServices;
import gov.takx.api.event.StopProjectServices;
import gov.takx.api.persist.IPersistService;
import gov.takx.api.persist.IPersistServiceProvider;
import gov.takx.api.platform.IProgressMonitor;
import gov.takx.core.project.ServiceOrdering;
import jakarta.annotation.Priority;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;

/**
 * This provider can be injected and then use {@link #getPersistService()} to obtain the persist service for the
 * current thread.
 *
 * @since 2.0.0
 */
@ApplicationScoped
public class PersistServiceProvider implements IPersistServiceProvider
{
    private boolean shutdown = true;

    @Override
    public IPersistService getPersistService()
    {
        return PersistService.forCurrentThread();
    }

    @Override
    public void cleanupPersistService()
    {
        cleanupForThread(Thread.currentThread());
    }

    @Override
    public void cleanupForThread(Thread t)
    {
        PersistService.cleanupForThread(t);
    }

    /**
     * Start the service, initialize the persist service.
     */
    private void startService(@Observes @Priority(ServiceOrdering.START_PersistServiceProvider) StartProjectServices event)
    {
        final var progressMonitor = event.getProgressMonitor();
        progressMonitor.setText("Preparing database...");
        progressMonitor.setSubstatusMaximum(IProgressMonitor.INDETERMINATE);

        PersistService.forCurrentThread().initialize(progressMonitor);
        shutdown = false;
    }

    /**
     * Stop our service.
     */
    private void stopService(@Observes @Priority(ServiceOrdering.STOP_PersistServiceProvider) StopProjectServices event)
    {
        final var progressMonitor = event.getProgressMonitor();
        progressMonitor.setMaximum(IProgressMonitor.INDETERMINATE);
        progressMonitor.setText("Closing database...");

        shutdown = true;
        PersistService.shutdown();
    }

    @Override
    public boolean isShutdown()
    {
        return shutdown;
    }
}
