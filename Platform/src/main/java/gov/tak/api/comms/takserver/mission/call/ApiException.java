package gov.tak.api.comms.takserver.mission.call;

import feign.FeignException;
import gov.tak.api.comms.takserver.mission.MissionApiAuthenticationException;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import org.apache.commons.lang3.StringUtils;

import java.net.ConnectException;

/**
 * An encompassing exception for Mission API errors.
 * <p/>
 * It will often wrap a {@link FeignException} to force users of the Mission API to handle errors accordingly, as {@link FeignException}
 * is a {@link RuntimeException} and thus is not required to be handled.
 *
 * @since 3.5
 */
public class ApiException extends Exception
{
    /**
     * @see ApiException#isUntrackedServer()
     * @since 5.0
     */
    public static final String MESSAGE_UNTRACKED_SERVER = "Server not tracked";

    /**
     * @param cause Original cause exception.
     *
     * @since 5.0 Loosened type to accept any cause.
     */
    public ApiException(@Nonnull Throwable cause)
    {
        this(cause.getMessage(), cause);
    }

    /**
     * @param message Details of the exception.
     *
     * @since 5.0
     */
    public ApiException(@Nonnull String message)
    {
        this(message, null);
    }

    /**
     * @param message Details of the exception.
     * @param cause Original cause exception.
     * @since 5.3
     */
    public ApiException(@Nullable String message, @Nullable Throwable cause)
    {
        super(message, cause);
    }

    /**
     * The inability to authenticate with TAK Server can occur in a few cases, the main culprits being:
     * <ul>
     *     <li>Invalid user credentials provided in {@link gov.tak.api.comms.takserver.ITakServerConnection}</li>
     *     <li>Loss of internet connection <i>(Though in most cases this will fall under {@link #isConnectionError()})</i></li>
     * </ul>
     *
     * @return {@code true} when the cause of the API exception was due to the inability to authenticate with TAK Server.
     *
     * @since 5.0
     */
    public boolean isAuthenticationError()
    {
        return getCause() instanceof MissionApiAuthenticationException;
    }

    /**
     * @return {@code true} when the cause of the API exception was due to connectivity problems.
     *
     * @since 5.0
     */
    public boolean isConnectionError()
    {
        return getCause() instanceof ConnectException;
    }

    /**
     * @return {@code true} when the cause of the API exception was due to the request/action targeting a connection not handled by internals.
     *
     * @since 5.0
     */
    public boolean isUntrackedServer()
    {
        return StringUtils.contains(getMessage(), MESSAGE_UNTRACKED_SERVER);
    }
}
