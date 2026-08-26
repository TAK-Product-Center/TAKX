package gov.takx.api.platform;

import jakarta.annotation.Nonnull;

import java.util.concurrent.Callable;

/**
 * Abstract base to support a callable where we change the name of the thread executing the callable and then
 * ensure that it is changed back to its original name when completed.
 *
 * @since 5.7
 */
public abstract class NameFixingCallable<T> implements Callable<T>
{
    private final String name;

    /**
     * @param name Name to set when called
     */
    public NameFixingCallable(@Nonnull String name)
    {
        this.name = name;
    }

    @Override
    public T call()
    {
        final var thread = Thread.currentThread();
        final var oldName = thread.getName();

        thread.setName(name);

        try
        {
            return doCall();
        } finally
        {
            thread.setName(oldName);
        }
    }

    /**
     * Implement the actual logic to be performed.
     */
    protected abstract T doCall();
}
