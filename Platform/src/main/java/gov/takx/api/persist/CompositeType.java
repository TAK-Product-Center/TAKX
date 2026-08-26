package gov.takx.api.persist;

import gov.takx.api.query.ComparisonType;

/**
 * Similar to Hibernate's composite user type, but specific to handling composite types in Core Fields (like UUID).
 * We couldn't use {@link org.hibernate.usertype.CompositeUserType} directly, since it doesn't offer a sufficient
 * API to reifying an instance of the mapped type from a tuple of property values.
 *
 * @since 2.0.0
 */
public interface CompositeType
{
    /**
     * @return The number of properties used to represent this type
     */
    int getPropertyCount();

    /**
     * Get the "property names" that may be used in a query.
     *
     * @return an array of "property names"
     */
    String[] getPropertyNames();

    /**
     * Get the column names that are used in the database.
     *
     * @return an array of column names
     */
    String[] getColumnNames();

    /**
     * Get the name of the column for the indexed property.
     *
     * @param property Property index
     * @return the property value
     */
    String getColumnName(int property);

    /**
     * Get the value of a property.
     *
     * @param component an instance of class mapped by this "type"
     * @param property  Property index
     * @return the property value
     */
    Object getPropertyValue(Object component, int property);

    /**
     * Get the comparator symbol to use for the given comparison type.  A composite type may need to handle
     * comparisons like "NOT NULL" in a unique manner (like comparing against 0 instead of NULL).
     *
     * @param comparisonType Comparison type to interpret
     * @return Proper comparator symbol to use
     * @see ComparisonType#getSymbol()
     * @since 2.14
     */
    String getComparatorSymbol(ComparisonType comparisonType);

    /**
     * Reify an instance of the mapped type from the data in an Object array (the results of a query).
     * Implementations of this method need to be {@code null} safe on the tuple values.
     *
     * @param tuple  Tuple containing the data
     * @param offset Stating offset at which to retrieve the data with which to construct our mapped type
     * @return Instance of the mapped type
     */
    Object reify(Object[] tuple, int offset);
}
