package gov.takx.api.processmanager;

import gov.takx.api.annotation.InjectableService;
import gov.takx.api.annotation.NoPluginImplementations;

/**
 * Contract for the external process manager.  The external process manager can be asked to track external processes,
 * started by TAKX or plugins, and to terminate them if we crash.
 * <p>
 * It is expected that any process registered will be properly shutdown as part of normal operation if we
 * exit normally.  It is up to the registrant to properly shut down any external processes as TAKX is shutdown.
 *
 * @since 2.0.0
 */
@InjectableService
@NoPluginImplementations
public interface IProcessManager
{
    /**
     * Register a child process to be killed if the main process crashes.
     *
     * @param pid The process' id.
     */
    void registerExternalProcess(long pid);
}
