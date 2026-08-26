package gov.takx.platform.utils;

import gov.tak.api.annotation.DeprecatedApi;

/**
 * A BiConsumer-like functional interface with an {@link #accept(Object, Object)} method that rethrows exceptions.
 *
 * @param <T> the type of the first argument
 * @param <U> the type of the second argument
 * @param <E> the type of Exception thrown by the consumer
 * @since 5.2
 * @deprecated since 5.8, moved to {@link gov.takx.commons.lang.ErrorThrowingBiConsumer}
 * @see gov.takx.commons.lang.ErrorThrowingBiConsumer
 */
@Deprecated(since = "5.8")
@DeprecatedApi(since = "5.8", forRemoval = true, removeAt = "5.11")
@FunctionalInterface
public interface ErrorThrowingBiConsumer<T, U, E extends Exception>
{
    /**
     * Performs this operation on the given arguments
     *
     * @param t the first input argument
     * @param u the second input argument
     * @throws E thrown if there is an exception during the operation
     */
    void accept(T t, U u) throws E;
}
