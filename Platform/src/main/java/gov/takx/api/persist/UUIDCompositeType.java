package gov.takx.api.persist;

import gov.takx.api.query.ComparisonType;

import java.util.UUID;

/**
 * Composite type definition for UUID.
 *
 * @since 2.0.0
 */
public class UUIDCompositeType implements CompositeType
{
    private static final String SEP = "_";

    private static final String[] propertyNames = {"msb", "lsb"};
    private final String[] columnNames = new String[propertyNames.length];

    /**
     * Construct a composite type using the given base column name.
     *
     * @param baseColumnName Base column name onto which our property names are appended to make complete column names
     */
    public UUIDCompositeType(String baseColumnName)
    {
        for (int i = 0; i < propertyNames.length; i++)
        {
            columnNames[i] = baseColumnName + SEP + propertyNames[i];
        }
    }

    @Override
    public int getPropertyCount()
    {
        return propertyNames.length;
    }

    @SuppressWarnings("AssignmentOrReturnOfFieldWithMutableType")
    @Override
    public String[] getPropertyNames()
    {
        return propertyNames;
    }

    @SuppressWarnings("AssignmentOrReturnOfFieldWithMutableType")
    @Override
    public String[] getColumnNames()
    {
        return columnNames;
    }

    @Override
    public String getColumnName(int property)
    {
        return columnNames[property];
    }

    @Override
    public Object getPropertyValue(Object component, int property)
    {
        return property == 0 ? ((UUID) component).getMostSignificantBits()
                : ((UUID) component).getLeastSignificantBits();
    }

    @Override
    public String getComparatorSymbol(ComparisonType comparisonType)
    {
        return switch (comparisonType)
        {
            case ISNOTNULL -> "!= 0";
            case ISNULL -> "= 0";
            default -> comparisonType.getSymbol();
        };
    }

    @Override
    public Object reify(Object[] tuple, int offset)
    {
        if (offset + 1 > tuple.length)
        {
            return null;
        }
        final Object p1 = tuple[offset];
        final Object p2 = tuple[offset + 1];

        if (offset + 1 > tuple.length || p1 == null || p2 == null)
        {
            return null;
        }

        if (p1.getClass() != Long.class || p2.getClass() != Long.class)
        {
            throw new RuntimeException("Illegal component types: " + p1.getClass() + " and " + p2.getClass());
        }

        final long msb = (long) p1;

        return msb != 0 ? new UUID((long) p1, (long) p2) : null;
    }
}
