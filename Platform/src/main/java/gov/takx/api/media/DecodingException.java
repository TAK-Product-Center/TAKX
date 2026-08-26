package gov.takx.api.media;

/**
 * Thrown by decoders to notify the caller of an error in processing a stream
 *
 * @since 2.8 (moved to Platform in 5.8)
 */
public class DecodingException extends Exception
{
    public DecodingException(String message)
    {
        super(message);
    }
}
