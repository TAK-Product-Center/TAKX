package gov.takx.commons.lang;

import gov.tak.api.annotation.IncubatingApi;
import jakarta.annotation.Nonnull;
import org.apache.commons.lang3.NotImplementedException;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;

/**
 * A List implementation that has O(1) {@code indexOf} operations by using an internal Map of values to indices. The
 * consequences of this are that this List cannot support duplicate values and that removals and inserts are more costly as
 * they involve remapping indexes past the removal or insertion point. The optimized {@code indexOf} operations are necessary
 * when working with Swing Tables and PropertyChangeEvents, which require large numbers of lookups to map event objects to
 * table indexes.
 *
 * @param <T> the type of objects in this List
 * @since 5.7
 */
@IncubatingApi(since = "5.7")
public class FastIndexList<T> implements List<T>
{
    private final List<T> delegate = new ArrayList<>();
    private final Map<Object, Integer> indexes = new HashMap<>();

    @Nonnull
    @Override
    public Object[] toArray()
    {
        return delegate.toArray();
    }

    @Nonnull
    @Override
    public <T1> T1[] toArray(@Nonnull T1[] a)
    {
        return delegate.toArray(a);
    }

    @Override
    public boolean containsAll(@Nonnull Collection<?> c)
    {
        return indexes.keySet().containsAll(c);
    }

    @Override
    public boolean addAll(@Nonnull Collection<? extends T> c)
    {
        var size = size();
        var i = size;
        for (var o : c)
        {
            var found = indexes.getOrDefault(o, -1);
            if (found == -1)
            {
                indexes.put(o, i++);
                delegate.add(o);
            }
        }
        return i != size;
    }

    @Override
    public boolean addAll(int index, @Nonnull Collection<? extends T> c)
    {
        var size = size();
        var i = index;
        for (var object : c)
        {
            add(i++, object);
        }
        return size != size();
    }

    @Override
    public boolean removeAll(@Nonnull Collection<?> c)
    {
        var changed = false;
        var reindex = Integer.MAX_VALUE;
        // We are doing individual removes here since we need to know the index to begin reindexing on. Alternatively we could 
        //  use delegate.removeAll and then reindex the whole List. As the remove collection grows towards N, the removeAll
        //  call becomes preferable. Because both are still O(n) we are picking one and sticking with it for now.
        for (var object : c)
        {
            var index = indexes.remove(object);
            if (index != null)
            {
                delegate.remove((int) index);
                if (index < reindex) reindex = index;
                changed = true;
            }
        }
        if (changed) reindex(reindex);
        return changed;
    }

    @Override
    public boolean retainAll(@Nonnull Collection<?> c)
    {
        var size = size();
        for (var o : List.copyOf(delegate))
        {
            if (!c.contains(o)) remove(o);
        }
        return size != size();
    }

    @Override
    public void clear()
    {
        delegate.clear();
        indexes.clear();
    }

    @Override
    public T set(int index, T element)
    {
        var current = get(index);
        indexes.remove(current);
        delegate.set(index, element);
        indexes.put(element, index);
        return current;
    }

    @Override
    public T remove(int index)
    {
        var object = delegate.remove(index);
        indexes.remove(object);
        reindex(index);
        return object;
    }

    @Override
    public int lastIndexOf(Object o)
    {
        return indexOf(o);
    }

    /**
     * {@inheritDoc} The returned iterator is not live.
     * @return the iterator
     */
    @Nonnull
    @Override
    public Iterator<T> iterator()
    {
        return List.copyOf(delegate).iterator();
    }

    /**
     * {@inheritDoc} The returned iterator is not live.
     * @return the iterator
     */
    @Nonnull
    @Override
    public ListIterator<T> listIterator()
    {
        return List.copyOf(delegate).listIterator();
    }

    /**
     * {@inheritDoc} The returned iterator is not live.
     * @return the iterator
     */
    @Nonnull
    @Override
    public ListIterator<T> listIterator(int index)
    {
        return List.copyOf(delegate).listIterator(index);
    }

    /**
     * @param object the object to add
     * @return Always {@code true}
     * @throws IllegalArgumentException If an entity that compares equal to the given object is already in the list
     */
    @Override
    public boolean add(T object)
    {
        int i = indexes.getOrDefault(object, -1);
        if (i == -1)
        {
            add(delegate.size(), object);
        } else
        {
            throw new IllegalArgumentException("Duplicate object found at index " + i);
        }
        return true;
    }

    @Override
    public void add(int index, T object)
    {
        int i = indexes.getOrDefault(object, -1);
        if (i == -1)
        {
            delegate.add(index, object);
            indexes.put(object, index);
            reindex(index + 1);
        } else
        {
            throw new IllegalArgumentException("Duplicate object found at index " + i);
        }
    }

    @Override
    public int indexOf(Object object)
    {
        return object != null ? indexes.getOrDefault(object, -1) : -1;
    }

    @Override
    public boolean remove(Object child)
    {
        return indexOfRemove(child) > -1;
    }

    @Override
    public int size()
    {
        return delegate.size();
    }

    @Override
    public T get(int index)
    {
        return delegate.get(index);
    }

    @Override
    public boolean isEmpty()
    {
        return delegate.isEmpty();
    }

    @Override
    public boolean contains(Object object)
    {
        return indexes.containsKey(object);
    }

    /**
     * @param child the child to remove
     * @return the index of the child that was removed
     */
    public int indexOfRemove(Object child)
    {
        var index = indexes.remove(child);
        if (index != null)
        {
            delegate.remove((int) index);
            reindex(index);
            return index;
        }
        return -1;
    }

    @Nonnull
    @Override
    public List<T> subList(int fromIndex, int toIndex)
    {
        throw new NotImplementedException();
    }

    private void reindex(int start)
    {
        var size = delegate.size();
        for (var i = start; i < size; i++)
        {
            indexes.put(delegate.get(i), i);
        }
    }
}
