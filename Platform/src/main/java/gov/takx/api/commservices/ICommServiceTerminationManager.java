package gov.takx.api.commservices;

import gov.takx.api.annotation.InjectableService;
import gov.takx.api.annotation.NoPluginImplementations;

/**
 * Strictly used by {@link ICommService} implementations to notify the core when the comm service has been terminated.
 *
 * @since 1.14
 */
@InjectableService
@NoPluginImplementations
public interface ICommServiceTerminationManager
{
    /**
     * Notifies the core that the given comm service is terminating, and an indication if the termination was
     * an unexpected.
     *
     * @param commService Comm service to terminate
     */
    void terminateCommService(ICommService commService);

    /**
     * Clears and closes the persistence session associated with the comm service thread. This call is necessary if and
     * only if a custom comm service spawns a thread. Failure to call this method will orphan a database connection,
     * and potentially cause a connection pool leak.
     */
    void flushPersistenceCache();
}
