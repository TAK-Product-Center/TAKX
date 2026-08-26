package gov.takx.api.query;

import jakarta.annotation.Nonnull;

import java.util.Objects;

/**
 * Abstract class for constraints that perform a comparison function against a value.
 *
 * @see ComparisonType
 * @since 1.49
 */
public abstract class ComparisonConstraint implements IConstraint
{
    protected ComparisonType comparator;

    // All the actual types we can contain
    protected Object value;

    /**
     * Construct a ComparisonConstraint based on the provided arguments.  The {@code value} parameter is required
     * to be non-null if the comparison type requires a value, as determined by {@link ComparisonType#usesValue()}.
     *
     * @param comparator the comparator
     * @param value      the value
     */
    protected ComparisonConstraint(@Nonnull ComparisonType comparator, Object value)
    {
        if (value == null && comparator.usesValue())
        {
            throw new IllegalArgumentException("value must be non-null with comparator " + comparator);
        }

        this.comparator = comparator;
        this.value = value;
    }

    /**
     * @return the comparator
     */
    public ComparisonType getComparator()
    {
        return comparator;
    }

    /**
     * @return the constraint value
     */
    public Object getValue()
    {
        return value;
    }

    @Override
    public boolean equals(Object o)
    {
        if (this == o) return true;
        if (!(o instanceof ComparisonConstraint that)) return false;

        return comparator == that.comparator && Objects.equals(value, that.value);
    }

    @Override
    public int hashCode()
    {
        int result = comparator.hashCode();
        result = 31 * result + (value != null ? value.hashCode() : 0);
        return result;
    }
}
