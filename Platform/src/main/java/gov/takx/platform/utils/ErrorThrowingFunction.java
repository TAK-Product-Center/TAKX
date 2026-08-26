package gov.takx.platform.utils;

/**
 * A Function-like interface with an {@link #apply(Object)} method that rethrows Exceptions.
 *
 * @param <T> the type of the input into the function
 * @param <U> the type of the output from the function
 * @param <E> the type of Exception thrown by the function
 * @since 5.2
 */
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
