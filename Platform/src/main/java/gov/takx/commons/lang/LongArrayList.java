package gov.takx.commons.lang;

import java.util.Arrays;

/**
 * Manage a list of primitive long values, similar in functionality to {@link java.util.ArrayList}.
 * <p>
 * This class is NOT thread safe.
 * <p>
 * This class was inspired by Trove4j's TLongArrayList.  It was needed because Koloboke (for some unknown reason)
 * doesn't seem to offer any primitive List style collections.
 *
 * @since 3.0
 */
public class LongArrayList
{
    public static final int DEFAULT_CAPACITY = 10;

    private long[] values;
    private int pos = 0;

    /**
     * Construct with the default initial capacity.
     */
    public LongArrayList()
    {
        this(DEFAULT_CAPACITY);
    }

    /**
     * Construct with the given initial capacity.
     *
     * @param initialCapacity Initial capacity
     */
    public LongArrayList(int initialCapacity)
    {
        values = new long[initialCapacity <= 0 ? DEFAULT_CAPACITY : initialCapacity];
    }

    /**
     * @return true if the list is empty.
     */
    public boolean isEmpty()
    {
        return pos == 0;
    }

    /**
     * @return the number of values in the list.
     */
    public int size()
    {
        return pos;
    }

    /**
     * Returns the value at the specified index.
     *
     * @param index Index of value to retrieve
     * @return value at that index
     */
    public long get(int index)
    {
        if (index >= pos) throw new ArrayIndexOutOfBoundsException(index);

        return values[index];
    }

    /**
     * Sets the value at the specified offset.
     *
     * @param index index of value to set
     * @param value value to set
     */
    public void set(int index, long value)
    {
        if (index >= pos) throw new ArrayIndexOutOfBoundsException(index);

        values[index] = value;
    }

    /**
     * Adds {@code value} to the end of the list, growing as needed.
     *
     * @param value value to add
     */
    public void add(long value)
    {
        if (pos == values.length - 1)
        {
            ensureCapacity(values.length + 1);
        }

        values[pos++] = value;
    }

    /**
     * Adds the values in the array {@code values} to the end of the list.
     *
     * @param values Array of values to add
     */
    public void add(long[] values)
    {
        add(values, 0, values.length);
    }

    /**
     * Adds a slice of the values in the array {@code values} to the end of the list.
     *
     * @param values Array of values to add
     * @param offset the offset at which to start copying (inclusive)
     * @param length the number of values to copy
     */
    public void add(long[] values, int offset, int length)
    {
        ensureCapacity(pos + length);
        System.arraycopy(values, offset, values, pos, length);
        pos += length;
    }

    /**
     * Applies the consumer to each value in the list in ascending order.
     *
     * @param consumer Consumer to receive each value
     */
    public void forEach(ILongConsumer consumer)
    {
        for (int i = 0, end = pos; i < end; i++)
        {
            consumer.accept(values[i]);
        }
    }

    /**
     * Applies the consumer to each value in the list in reverse (back to front) order.
     *
     * @param consumer Consumer to receive each value
     */
    public void forEachReverse(ILongConsumer consumer)
    {
        for (int i = pos; i-- > 0;)
        {
            consumer.accept(values[i]);
        }
    }

    /**
     * Removes the element at the given index from the list.
     *
     * @param index the index to the element to be removed
     * @return the value removed
     */
    public long removeAt(int index)
    {
        final long old = get(index);
        remove(index, 1);
        return old;
    }

    /**
     * Removes {@code length} values from the list, starting at {@code index}.
     *
     * @param index  index of first element to remove (inclusive)
     * @param length number of elements to remove
     */
    public void remove(int index, int length)
    {
        if (length == 0) return;
        if (index < 0 || index >= pos) throw new ArrayIndexOutOfBoundsException(index);

        if (index == 0)
        {
            System.arraycopy(values, length, values, 0, pos - length); // trim the front
        } else
        {
            //noinspection StatementWithEmptyBody
            if (pos - length == index)
            {
                // no copy to make, decrementing pos "deletes" values at the end
            } else
            {
                System.arraycopy(values, index + length, values, index, pos - (index + length));
            }
        }
        pos -= length;
    }

    /**
     * Delete the current list, creating a new, empty one with the default capacity.
     */
    public void clear()
    {
        clear(DEFAULT_CAPACITY);
    }

    /**
     * Delete the current list, creating a new, empty one with the given {@code capacity}.
     *
     * @param capacity the new initial capacity
     */
    public void clear(int capacity)
    {
        values = new long[capacity];
        pos = 0;
    }

    /**
     * Searches the list front to back for the index of value.
     *
     * @param value Value to search for
     * @return the first offset of the value, or -1 if it is not in the list.
     * @see #binarySearch for faster searches on sorted lists
     */
    public int indexOf(long value)
    {
        return indexOf(0, value);
    }

    /**
     * Searches the list front to back for the index of value, starting at {@code offset}.
     *
     * @param offset the offset at which to start the linear search (inclusive)
     * @param value  Value to search for
     * @return the first offset of the value, or -1 if it is not in the list.
     * @see #binarySearch for faster searches on sorted lists
     */
    public int indexOf(int offset, long value)
    {
        for (int i = offset, end = pos; i < end; i++)
        {
            if (values[i] == value) return i;
        }
        return -1;
    }

    /**
     * Searches the list back to front for the last index of value.
     *
     * @param value Value to search for
     * @return the last offset of the value, or -1 if it is not in the list.
     * @see #binarySearch for faster searches on sorted lists
     */
    public int lastIndexOf(long value)
    {
        return lastIndexOf(pos, value);
    }

    /**
     * Searches the list back to front for the last index of value, starting at {@code offset}.
     *
     * @param offset the offset at which to start the linear search (exclusive)
     * @param value  Value to search for
     * @return the last offset of the value, or -1 if it is not in the list.
     * @see #binarySearch for faster searches on sorted lists
     */
    public int lastIndexOf(int offset, long value)
    {
        for (int i = offset; i-- > 0;)
        {
            if (values[i] == value) return i;
        }
        return -1;
    }

    /**
     * Searches the list for value.
     *
     * @param value Value to search for
     * @return true if value is in the list.
     */
    public boolean contains(long value)
    {
        return indexOf(value) >= 0;
    }

    /**
     * Fills every slot in the list with the specified value.
     *
     * @param val the value to set
     */
    public void fill(long val)
    {
        Arrays.fill(values, 0, pos, val);
    }

    /**
     * Fills a range in the list with the specified value.  If the specified {@code toIndex} is greater than
     * the current capacity of the list, it will be expanded as needed.
     *
     * @param fromIndex the offset at which to start filling (inclusive)
     * @param toIndex   the offset at which to stop filling (exclusive)
     * @param val       the value to set
     */
    public void fill(int fromIndex, int toIndex, long val)
    {
        if (toIndex > pos)
        {
            ensureCapacity(toIndex);
            pos = toIndex;
        }
        Arrays.fill(values, fromIndex, toIndex, val);
    }

    /**
     * Sort the values in the list in ascending order.
     *
     * @see java.util.Arrays#sort
     */
    public void sort()
    {
        Arrays.sort(values, 0, pos);
    }

    /**
     * Performs a binary search for {@code value} in the entire list.
     * Note that you <b>must</b> {@link #sort sort} the list before doing a search.
     *
     * @param value the value to search for
     * @return the absolute offset in the list of the value, or its negative insertion point into the sorted list.
     * @see Arrays#binarySearch(long[], int, int, long)
     */
    public int binarySearch(long value)
    {
        return Arrays.binarySearch(values, 0, pos, value);
    }

    /**
     * Copies the contents of the list into a newly allocated native array.
     *
     * @return a newly allocated {@code long[]} with the same values as this list
     */
    public long[] toArray()
    {
        return toArray(0, pos);
    }

    /**
     * Copies a slice of the list into a newly allocated native array.
     *
     * @param offset the offset at which to start copying
     * @param len    the number of values to copy
     * @return a newly allocated {@code long[]} with the same values as the slice of this list
     */
    public long[] toArray(int offset, int len)
    {
        return toArray(new long[len], offset, len);
    }

    /**
     * Copies a slice of the list into a given native array.
     *
     * @param dest   the array to copy into
     * @param offset the offset of the first value to copy
     * @param len    the number of values to copy
     * @return the given {@code dest} array
     */
    public long[] toArray(long[] dest, int offset, int len)
    {
        System.arraycopy(values, 0, dest, offset, len);
        return dest;
    }

    /**
     * Grow the internal array as needed to accommodate the specified number of elements.
     * The size of the array increases by 25% on each resize unless capacity requires more than that.
     *
     * @param capacity new capacity required
     */
    private void ensureCapacity(int capacity)
    {
        if (capacity > values.length)
        {
            final int newCapacity = (int) Math.max(values.length * 1.25, capacity);
            final long[] newValues = new long[newCapacity];
            System.arraycopy(values, 0, newValues, 0, values.length);
            values = newValues;
        }
    }

    /**
     * Compares this list to another list, value by value.
     *
     * @param o the object to compare against
     * @return true if o is a LongArrayList and has exactly the same length and values.
     */
    @Override
    public boolean equals(Object o)
    {
        if (o == this)
        {
            return true;
        } else if (o instanceof LongArrayList)
        {
            final LongArrayList that = (LongArrayList) o;
            if (pos != that.pos)
            {
                return false;
            } else
            {
                for (int i = pos; i-- > 0;)
                {
                    if (values[i] != that.values[i]) return false;
                }
                return true;
            }
        } else
        {
            return false;
        }
    }

    @Override
    public int hashCode()
    {
        int h = 0;
        for (int i = pos; i-- > 0;) h += Long.hashCode(values[i]);
        return h;
    }

    @Override
    public String toString()
    {
        final StringBuilder buf = new StringBuilder("{");

        for (int i = 0, end = pos; i < end; i++)
        {
            if (i > 0) buf.append(", ");
            buf.append(values[i]);
        }
        buf.append("}");

        return buf.toString();
    }

    /**
     * Functional Interface for use with {@link #forEach(ILongConsumer)}, accepting a primitive long value.
     */
    @FunctionalInterface
    public interface ILongConsumer
    {
        void accept(long value);
    }
}
