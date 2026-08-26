package gov.takx.processmanager;

/**
 * Interface for a stand-alone application that will monitor the main application and detect crashes (i.e. when
 * the PID disappears from the process list unexpectedly) and will attempt to stop all sub-processes started
 * by the main application that would otherwise be orphaned after a crash.
 * <p/>
 * todo: Allow PIDs to be unregistered.
 */
public interface IRemoteProcessManager
{
    /**
     * Add the PID of a sub-process that should be stopped if the main application crashes.
     *
     * @param pid The sub-process' PID.
     */
    void addPID(long pid);

    /**
     * Cancel the process manager and shut it down.
     */
    void shutdown();
}
