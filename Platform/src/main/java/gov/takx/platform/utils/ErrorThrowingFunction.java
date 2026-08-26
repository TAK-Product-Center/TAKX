package gov.takx.platform.utils;

import gov.tak.api.annotation.DeprecatedApi;

/**
 * A Function-like interface with an {@link #apply(Object)} method that rethrows Exceptions.
 *
 * @param <T> the type of the input into the function
 * @param <U> the type of the output from the function
 * @param <E> the type of Exception thrown by the function
 * @since 5.2
 * @deprecated since 5.8, moved to {@link gov.takx.commons.lang.ErrorThrowingFunction}
 * @see gov.takx.commons.lang.ErrorThrowingFunction
 */
@Deprecated(since = "5.8")
@DeprecatedApi(since = "5.8", forRemoval = true, removeAt = "5.11")
@FunctionalInterface
public interface ErrorThrowingFunction<T, U, E extends Exception>
{
    /**
     * Applies this function to the given argument.
     *
     * @param t the function argument
     * @return the function result
     * @throws E thrown if there is an exception during the operation
     */
    U apply(T t) throws E;
}
