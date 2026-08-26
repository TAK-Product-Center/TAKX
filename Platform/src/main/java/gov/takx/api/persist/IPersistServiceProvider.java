package gov.takx.api.persist;

import gov.takx.api.annotation.NoPluginImplementations;

/**
 * Contract for providing a persist service to injection targets.  Note, this does not "produce" them in the CDI
 * sense.  It is a manager object that offers access to appropriate instance for the calling thread.
 *
 * @since 2.0.0
 */
@NoPluginImplementations
public interface IPersistServiceProvider
{
    /**
     * @return The persist service for the calling thread
     */
    IPersistService getPersistService();

    /**
     * Cleanup, close and remove the persist service instance for the calling thread.
     */
    void cleanupPersistService();

    /**
     * Cleanup and close the session for the given thread, if there is a PS recorded for it.
     *
     * @param t The thread to clean up
     * @since 2.37
     */
    void cleanupForThread(Thread t);

    /**
     * @return {@code true} if the persist service has been shut down, {@code false} otherwise
     * @since 5.7
     * @implNote This method will only return {@code true} between the times that the persist service provider is
     *           started and when it is stopped.
     */
    boolean isShutdown();
}
