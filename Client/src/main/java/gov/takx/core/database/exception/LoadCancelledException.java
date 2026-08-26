package gov.takx.core.database.exception;

import gov.takx.api.event.ITerminateObserverNotification;

/**
 * Exception that is thrown if project load was cancelled by user.
 */
public class LoadCancelledException extends RuntimeException implements ITerminateObserverNotification
{
    public LoadCancelledException(String m)
    {
        super(m);
    }

    public LoadCancelledException()
    {
        super();
    }
}
