package gov.takx.platform.ui.devicelog;

import gov.takx.api.query.ComparisonType;

/**
 * Simple class to hold the comparison type and optional value as part of a field filter.
 *
 * @see FieldFilter
 * @since 1.49
 */
class FilterComparison
{
    final ComparisonType comparisonType;
    final Object value;

    /**
     * @param comparisonType Comparison type
     * @param value          The value to compare against, optional if comparisonType doesn't require a value
     * @see ComparisonType#usesValue()
     */
    FilterComparison(ComparisonType comparisonType, Object value)
    {
        this.comparisonType = comparisonType;
        this.value = value;
    }

    /**
     * @return {@code true} if the comparison is fully configured, {@code false} if the comparison type requires
     * a value and no value was provided
     */
    boolean isUsable()
    {
        return comparisonType != null && (!comparisonType.usesValue() || value != null);
    }

    @Override
    public String toString()
    {
        return comparisonType.usesValue()
                ? comparisonType + " " + value
                : comparisonType.toString();
    }
}
