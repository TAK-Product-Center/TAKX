package gov.takx.commons.lang;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;

/**
 * Extremely simple circular array implementation.
 *
 * @since 1.3
 */
@SuppressWarnings("unchecked")
public class CircularArray<T> implements Iterable<T>
{
    private final Object[] data;
    private volatile int count = 0;
    private int takeIndex = 0;
    private int putIndex = 0;

    public CircularArray(int size)
    {
        if (size <= 0) throw new IllegalArgumentException("'size' must be greater than 0");

        data = new Object[size];
    }

    public int size()
    {
        return count;
    }

    /**
     * @return {@code true} if there are not elements in the array, {@code false} otherwise
     * @since 2.37
     */
    public boolean isEmpty()
    {
        return count == 0;
    }

    public synchronized T add(T value)
    {
        Object oldValue = data[putIndex];

        data[putIndex] = value;

        if (count < data.length)
        {
            count += 1;
            putIndex = inc(putIndex);
        } else
        {
            // We've outgrown our buffer, so we need to update both the put and take indices
            putIndex = inc(putIndex);
            takeIndex = inc(takeIndex);
        }
        return (T) oldValue;
    }

    public synchronized boolean addAll(Collection<? extends T> c)
    {
        boolean modified = false;

        for (T e : c)
        {
            if (add(e) != null)
            {
                modified = true;
            }
        }
        return modified;
    }

    public synchronized T get(int position)
    {
        if (position >= count)
        {
            throw new IllegalArgumentException("Position exceeds number of elements in circular array.");
        }

        return (T) data[(takeIndex + position) % data.length];
    }

    public synchronized void clear()
    {
        count = takeIndex = putIndex = 0;
        Arrays.fill(data, null);
    }

    /**
     * Removes from this array all of its elements that are contained in the
     * specified collection.
     *
     * @param toRemove collection containing elements to be removed from this array
     * @return <tt>true</tt> if this list changed as a result of the call
     * @since 2.37
     */
    public synchronized boolean removeAll(Collection<T> toRemove)
    {
        final List<T> elementsToKeep = new ArrayList<>();
        boolean anyRemoved = false;

        for (int i = 0; i < count; i++)
        {
            final T element = get(i);
            if (element != null)
            {
                if (!toRemove.contains(element))
                {
                    elementsToKeep.add(element);
                } else
                {
                    anyRemoved = true;
                }
            }
        }
        clear();
        addAll(elementsToKeep);

        return anyRemoved;
    }

    private int inc(int i)
    {
        return (++i == data.length) ? 0 : i;
    }

    /**
     * Create an iterator that will return the current set of elements in the order they were added to the array.
     *
     * @return Iterator that can walk the circular array once and only once
     */
    @SuppressWarnings("NullableProblems")
    @Override
    public synchronized Iterator<T> iterator()
    {
        return new CircularArrayIterator<>(this);
    }

    /**
     * Private inner class to provide an iterator on the elements of the circular array.
     */
    @SuppressWarnings("unchecked")
    private static class CircularArrayIterator<T> implements Iterator<T>
    {
        private int position = 0;
        private final Object[] clonedData;

        /**
         * Construct an iterator on the given CircularArray.  Clone the data so subsequent modifications to it
         * don't affect this iterator.
         *
         * @param array CircularArray on which to iterate
         */
        public CircularArrayIterator(CircularArray<T> array)
        {
            clonedData = new Object[array.count];

            for (int i = 0; i < array.count; i++)
            {
                clonedData[i] = array.get(i);
            }
        }

        @Override
        public boolean hasNext()
        {
            return position < clonedData.length;
        }

        @Override
        public T next()
        {
            if (position >= clonedData.length) throw new NoSuchElementException();

            return (T) clonedData[position++];
        }

        @Override
        public void remove()
        {
            throw new UnsupportedOperationException("CircularArrayIterator doesn't support remove()");
        }
    }
}
