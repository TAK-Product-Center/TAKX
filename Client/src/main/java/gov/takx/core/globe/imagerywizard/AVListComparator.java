/* Copyright (C) 2001, 2008 United States Government as represented by
the Administrator of the National Aeronautics and Space Administration.
All Rights Reserved.
*/
package gov.takx.core.globe.imagerywizard;

import gov.nasa.worldwind.avlist.AVList;

import java.util.Arrays;
import java.util.Comparator;

/**
 */
public class AVListComparator implements Comparator<AVList>
{
    public enum Order
    {
        ASCENDING, DESCENDING;

        public static Order defaultOrder()
        {
            return ASCENDING;
        }
    }

    private int numKeys;
    private String[] keys;
    private Order[] order;

    public AVListComparator(int numKeys, String[] keys, Order[] order)
    {
        this.numKeys = numKeys;
        this.keys = new String[this.numKeys];
        this.order = new Order[this.numKeys];
        System.arraycopy(keys, 0, this.keys, 0, this.numKeys);

        if (order != null)
        {
            System.arraycopy(order, 0, this.order, 0, this.numKeys);
        } else
        {
            Arrays.fill(this.order, Order.defaultOrder());
        }
    }

    public AVListComparator(String[] keys)
    {
        this(keys.length, keys, null);
    }

    public int compare(AVList avList1, AVList avList2)
    {
        int i = this.compareNull(avList2, avList2);
        if (i != Integer.MAX_VALUE) return i;

        // Find the first key that is different, and return either -1 or 1.
        for (int index = 0; index < numKeys; index++)
        {
            i = this.compareValues(avList1.getValue(this.keys[index]), avList2.getValue(this.keys[index]), this.order[index]);
            if (i != 0) return i;
        }

        // The values for each specified key are equivalent.
        return 0;
    }

    private int compareValues(Object o1, Object o2, Order order)
    {
        int i = this.compareNull(o1, o2);
        if (i != Integer.MAX_VALUE) return fixOrder(i, order);

        //noinspection unchecked, rawtypes
        i = o1 instanceof Comparable c ? c.compareTo(o2) : o1.toString().compareToIgnoreCase(o2.toString());
        return fixOrder(i, order);
    }

    private int compareNull(Object o1, Object o2)
    {
        // Return 0 if o1 and o2 are null.
        // Return 1 if only o1 is null.
        // Return -1 if only o2 is null.
        if (o1 == null && o2 == null)
        {
            return 0;
        } else if (o1 == null)
        {
            return 1;
        } else if (o2 == null)
        {
            return -1;
        }
        return Integer.MAX_VALUE;
    }

    private int fixOrder(int val, Order order)
    {
        return order != Order.ASCENDING && val != 0 ? ((val == -1) ? 1 : -1) : val;
    }
}
