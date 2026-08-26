package gov.takx.core.media.decode;

/**
 * Thrown by decoders to notify the caller of an error in processing a stream
 *
 * @since 2.8
 */
public class DecodingException extends Exception
{
    public DecodingException(String message)
    {
        super(message);
    }

    public DecodingException(String message, Throwable cause)
    {
        super(message, cause);
    }
}
