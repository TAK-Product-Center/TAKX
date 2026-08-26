package gov.tak.api.comms.takserver;

import gov.tak.api.comms.takserver.mission.call.ApiException;
import gov.takx.api.annotation.NoPluginImplementations;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import java.util.Objects;

/**
 * Outline of the possible erroneous state of a result from async TAK Server API usage.
 *
 * @since 5.0
 */
@NoPluginImplementations
interface IPossiblyErroneous
{
    /**
     * @return The possible error.
     */
    @Nullable
    ApiException error();

    /**
     * Should only be used in contexts where the error is assumed to be non-null.
     *
     * @return {@link #error()} but assumed to be non-null.
     */
    @Nonnull
    default ApiException requireError()
    {
        return Objects.requireNonNull(error());
    }

    /**
     * @return {@code true} if the operation was a success.
     */
    default boolean wasSuccess()
    {
        return error() == null;
    }
}
