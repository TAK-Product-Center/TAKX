package gov.takx.core.database.exception;

import gov.takx.api.event.ITerminateObserverNotification;

/**
 * Exception thrown to report that a fatal DB exception has been encountered and the project is not usable.
 *
 * @since 2.12
 */
public class FatalDatabaseException extends RuntimeException implements ITerminateObserverNotification
{
    public FatalDatabaseException(String msg, Throwable cause)
    {
        super(msg, cause);
    }
}
