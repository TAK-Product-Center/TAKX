package gov.takx.core.dataportal;

import jakarta.annotation.Nonnull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.invoke.MethodHandles;
import java.util.function.Consumer;

/**
 * Value class to hold an object and an associated priority.  Instances are comparable, based on priority.
 * Higher priorities will be ordered first (i.e., reverse of the normal integer ordering).  There is no arbitrary
 * restriction on the priority value, it can be any integer value.
 * <p>
 * Member access is direct, no getters, since these are final values.
 *
 * @since 3.0
 */
public class PrioritizedEntry<T> implements Comparable<PrioritizedEntry<T>>
{
    private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    /**
     * The delegate object with the assigned priority.
     */
    public final T delegate;

    /**
     * The priority of this entry, higher priorities sort earlier.
     */
    public final int priority;

    /**
     * @param delegate Delegate object
     * @param priority Priority of object, higher priorities sort earlier. There is no arbitrary restriction
     *                 on the priority value, it can be any integer value.
     */
    public PrioritizedEntry(@Nonnull T delegate, int priority)
    {
        this.delegate = delegate;
        this.priority = priority;
    }

    /**
     * Apply the given action to our delegate, trapping and reporting any exceptions.
     *
     * @param action      Action to perform on our delegate
     * @param description Action description for error message
     */
    public void safeApply(@Nonnull Consumer<T> action, @Nonnull String description)
    {
        try
        {
            action.accept(delegate);
        } catch (Exception e)
        {
            logger.error("Exception notifying delegate of {}", description, e);
        }
    }

    /**
     * Order is highest priority first.
     * <p>
     * {@inheritDoc}
     */
    @Override
    public int compareTo(@Nonnull PrioritizedEntry<T> that)
    {
        // Negate result so higher priorities sort lower
        final int cmp = -Integer.compare(priority, that.priority);

        // Must not return 0 unless our delegate is the same or some collection types (like ConcurrentSkipListSet)
        // will fail to add entries of equal priority on different delegates.
        //
        // We will return 1 in that case, which generally causes entries added at the same priority to iterate in the
        // order they were added.

        return cmp != 0
                ? cmp
                : delegate == that.delegate ? 0 : 1;
    }

    @Override
    public boolean equals(Object o)
    {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        PrioritizedEntry<?> that = (PrioritizedEntry<?>) o;

        return priority == that.priority && delegate == that.delegate;
    }

    @Override
    public int hashCode()
    {
        return 31 * priority + System.identityHashCode(delegate);
    }

    @Override
    public String toString()
    {
        return "PrioritizedEntry{" +
                "priority=" + priority +
                ", delegate=" + delegate +
                '}';
    }
}
