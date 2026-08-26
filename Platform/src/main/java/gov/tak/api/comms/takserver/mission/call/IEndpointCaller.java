package gov.tak.api.comms.takserver.mission.call;

import gov.takx.api.annotation.NoPluginImplementations;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;

/**
 * Outlines the capability of invoking a REST endpoint.
 *
 * @param <T> Endpoint return value type.
 *
 * @since 3.5
 */
@NoPluginImplementations
public interface IEndpointCaller<T>
{
    /**
     * Calls the endpoint, yielding the mapped value of the response to the expected data type.
     *
     * @return Result of endpoint invocation, if any.
     *
     * @throws ApiException When the underlying endpoint call encounters an error.
     * Each endpoint may define its own error codes for different kinds of problems.
     * Most of the time they follow a pattern, for instance here are some examples:
     * <ul>
     *     <li><b>403</b>: You are not authorized to do a specific action, or request some resource.</li>
     *     <li><b>404</b>: Something referred to does not exist, like a mission name.</li>
     *     <li><b>410</b>: Something referred to was deleted and is now gone.</li>
     * </ul>
     */
    @Nullable
    T get() throws ApiException;

    /**
     * Calls the endpoint, yielding the mapped value of the response to the expected data type.
     *
     * @return Completable future wrapping the result of {@link #get()}.
     *
     * @see #get() Wrapped synchronous call.
     * @see #asyncGet(ExecutorService) Same call, but with the option to use a specific executor service.
     */
    @Nonnull
    CompletableFuture<T> asyncGet();

    /**
     * Calls the endpoint, yielding the mapped value of the response to the expected data type.
     *
     * @param executorService Executor service to use for the completable future supplier.
     *
     * @return Completable future wrapping the result of {@link #get()}.
     *
     * @see #get() Wrapped synchronous call.
     * @see #asyncGet() Same call, but with an anonymous executor service.
     */
    @Nonnull
    CompletableFuture<T> asyncGet(@Nonnull ExecutorService executorService);
}
