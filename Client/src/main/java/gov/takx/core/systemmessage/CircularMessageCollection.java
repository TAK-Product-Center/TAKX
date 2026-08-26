package gov.takx.core.systemmessage;

import gov.takx.api.systemmessage.SystemMessage;
import gov.takx.commons.lang.CircularArray;

import java.util.AbstractCollection;
import java.util.Collection;
import java.util.Iterator;

/**
 * A {@link CircularArray} that feels like a collection.
 *
 * @since 2.30
 */
class CircularMessageCollection extends AbstractCollection<SystemMessage>
{
    private final CircularArray<SystemMessage> messages;

    CircularMessageCollection(int size)
    {
        messages = new CircularArray<>(size);
    }

    @Override
    public boolean add(SystemMessage message)
    {
        return messages.add(message) == null;
    }

    @Override
    public boolean addAll(Collection<? extends SystemMessage> c)
    {
        return messages.addAll(c);
    }

    @SuppressWarnings("NullableProblems")
    @Override
    public Iterator<SystemMessage> iterator()
    {
        return messages.iterator();
    }

    @Override
    public int size()
    {
        return messages.size();
    }

    /**
     * Overridden to directly clear {@code messages} since the base implementation uses an iterator to remove
     * all the elements of the collection, and the {@code CircularArrayIterator} doesn't support removal (which
     * leads to an {@code UnsupportedOperationException}.
     *
     * @since 2.31
     */
    @Override
    public void clear()
    {
        messages.clear();
    }
}
