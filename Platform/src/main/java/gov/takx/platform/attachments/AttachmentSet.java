package gov.takx.platform.attachments;

import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;
import java.util.Spliterator;

/**
 * An attachment that implements a set.  Forwards all methods declared in {@link Set} to an internal
 * {@link HashSet} delegate.
 *
 * @since 2.25
 */
@SuppressWarnings("NullableProblems")
public class AttachmentSet<T> implements Set<T>
{
    private final Class<T> valueClass;
    private final Set<T> delegate;

    public AttachmentSet(final Class<T> valueClass)
    {
        delegate = Collections.checkedSet(new HashSet<>(), valueClass);
        this.valueClass = valueClass;
    }

    public Class<T> getValueClass()
    {
        return valueClass;
    }

    @Override
    public int size()
    {
        return delegate.size();
    }

    @Override
    public boolean isEmpty()
    {
        return delegate.isEmpty();
    }

    @Override
    public boolean contains(Object o)
    {
        return delegate.contains(o);
    }

    @Override
    public Iterator<T> iterator()
    {
        return delegate.iterator();
    }

    @Override
    public Object[] toArray()
    {
        return delegate.toArray();
    }

    @SuppressWarnings("SuspiciousToArrayCall")
    @Override
    public <T1> T1[] toArray(T1[] a)
    {
        return delegate.toArray(a);
    }

    @Override
    public boolean add(T t)
    {
        return delegate.add(t);
    }

    @Override
    public boolean remove(Object o)
    {
        return delegate.remove(o);
    }

    @Override
    public boolean containsAll(Collection<?> c)
    {
        return delegate.containsAll(c);
    }

    @Override
    public boolean addAll(Collection<? extends T> c)
    {
        return delegate.addAll(c);
    }

    @Override
    public boolean retainAll(Collection<?> c)
    {
        return delegate.retainAll(c);
    }

    @Override
    public boolean removeAll(Collection<?> c)
    {
        return delegate.removeAll(c);
    }

    @Override
    public void clear()
    {
        delegate.clear();
    }

    @SuppressWarnings("EqualsWhichDoesntCheckParameterClass")
    @Override
    public boolean equals(Object o)
    {
        return delegate.equals(o);
    }

    @Override
    public int hashCode()
    {
        return delegate.hashCode();
    }

    @Override
    public Spliterator<T> spliterator()
    {
        return delegate.spliterator();
    }

    @Override
    public String toString()
    {
        return delegate.toString();
    }
}
