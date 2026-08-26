package gov.tak.api.comms.takserver.mission;

import jakarta.annotation.Nonnull;

/**
 * Thrown by {@link ApiProvider} when authentication fails on connections that require it.
 *
 * @since 5.0
 */
public class MissionApiAuthenticationException extends RuntimeException
{
    /**
     * @param message Additional details about the authentication issue.
     * @param cause Cause of authentication to fail.
     */
    public MissionApiAuthenticationException(@Nonnull String message, @Nonnull Throwable cause)
    {
        super(message, cause);
    }
}
