package gov.takx.commons.lang;

import gov.tak.api.annotation.IncubatingApi;

/**
 * A Runnable-like functional interface that declares it throws a given exception type.
 *
 * @param <E> the type of Exception thrown by the function
 * @since 5.8
 */
@IncubatingApi(since = "5.8")
@FunctionalInterface
public interface ErrorThrowingRunnable<E extends Exception>
{
    void run() throws E;
}
