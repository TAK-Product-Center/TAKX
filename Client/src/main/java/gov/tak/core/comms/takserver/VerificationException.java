package gov.tak.core.comms.takserver;

/**
 * Exception indicating a failure to verify the downloaded file for size/SHA256 match.
 *
 * @since 3.1
 */
public class VerificationException extends RuntimeException
{
    public VerificationException(String message)
    {
        super(message);
    }
}
