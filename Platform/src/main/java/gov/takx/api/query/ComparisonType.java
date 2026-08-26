package gov.takx.api.query;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.invoke.MethodHandles;

/**
 * Comparison type to be used with FIELD value constraints.
 *
 * @see IQueryConstraints#addFieldConstraint(String, String, ComparisonType, Object)
 */
public enum ComparisonType
{
    /**
     * Strictly less than.
     */
    LT("<"),
    /**
     * Less than or equal.
     */
    LTE("<="),
    /**
     * Equal.
     */
    EQ("="),
    /**
     * Greater than or equal.
     */
    GTE(">="),
    /**
     * Strictly greater than.
     */
    GT(">"),
    /**
     * Not equal.
     */
    NEQ("!="),
    /**
     * is {@code null}
     */
    ISNULL("is null", false),
    /**
     * is not {@code null}
     */
    ISNOTNULL("is not null", false),
    /**
     * like (wildcard comparator), this type of comparison does not require a value operand
     */
    LIKE("like"),
    /**
     * not like (wildcard not comparator), this type of comparison does not require a value operand
     */
    NOTLIKE("not like");

    private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    private final String symbol;
    private final boolean usesValue;

    /**
     * Constructor for symbols that require a comparison value.
     *
     * @param symbol Comparison symbol (used in SQL generation)
     */
    ComparisonType(String symbol)
    {
        this(symbol, true);
    }

    /**
     * @param symbol    Comparison symbol (used in SQL generation)
     * @param usesValue Pass {@code true} if this comparison type requires a value to compare against
     */
    ComparisonType(String symbol, boolean usesValue)
    {
        this.symbol = symbol;
        this.usesValue = usesValue;
    }

    /**
     * @return The symbol to use for this comparison in generated query strings
     */
    public String getSymbol()
    {
        return symbol;
    }

    /**
     * @return {@code true} if this comparison type requires a value operand, {@code false} if not
     */
    public boolean usesValue()
    {
        return usesValue;
    }

    @Override
    public String toString()
    {
        return symbol;
    }

    /**
     * Interpret a compareTo result against this comparison type.
     *
     * @param compareToResult compareTo result
     * @since 1.49
     */
    public boolean interpretComparison(int compareToResult)
    {
        switch (this)
        {
            case EQ:
                return compareToResult == 0;
            case NEQ:
                return compareToResult != 0;
            case LT:
                return compareToResult < 0;
            case LTE:
                return compareToResult <= 0;
            case GT:
                return compareToResult > 0;
            case GTE:
                return compareToResult >= 0;
        }

        logger.error("Unknown comparator type: {}", this);
        return false; // Assume false in case of error
    }
}
