package gov.takx.commons.lang;

import gov.tak.api.annotation.IncubatingApi;

/**
 * A BiConsumer-like functional interface with an {@link #accept(Object, Object)} method that rethrows exceptions.
 *
 * @param <T> the type of the first argument
 * @param <U> the type of the second argument
 * @param <E> the type of Exception thrown by the consumer
 * @since 5.8, moved from {@link gov.takx.platform.utils.ErrorThrowingBiConsumer}
 */
@IncubatingApi(since = "5.8")
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
