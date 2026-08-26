package gov.takx.api.messages;

/**
 * Exception in the specification of a field processor class for a data structure field.  Thrown when a plugin
 * contributed field display processor class does not properly implement {@link IFieldDisplayProcessor}, or when
 * TAKX is unable to instantiate the class because it either doesn't exist or was specified incorrectly.
 *
 * @see IFieldDisplayProcessor
 * @since 2.29
 */
public class InvalidFieldProcessorException extends Exception
{
    /**
     * Construct the exception with just a message.
     *
     * @param message the exception message.
     */
    InvalidFieldProcessorException(String message)
    {
        super(message);
    }

    /**
     * Construct the exception with a message and the cause.
     *
     * @param message the exception message.
     * @param cause   the throwable cause.
     */
    InvalidFieldProcessorException(String message, Throwable cause)
    {
        super(message, cause);
    }
}
