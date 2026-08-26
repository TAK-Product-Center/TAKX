package gov.takx.api.query;

import gov.takx.api.annotation.Internal;
import gov.takx.api.persist.CompositeType;
import gov.takx.platform.query.SqlState;
import jakarta.annotation.Nonnull;

import java.util.HashSet;
import java.util.Set;

/**
 * A FieldConstraint limits the selection to rows that satisfy a specified comparison against a value.
 * For example, {@code speed < 10}.  The type of comparison to perform is specified using a {@link ComparisonType}.
 * <p>
 * The value constraint is applied to {@link ASimpleFieldSelector} instances, so basically any core or extended structure
 * field, but not aggregates.
 *
 * @see AFieldSelector
 * @since 1.49
 */
public class FieldConstraint extends ComparisonConstraint
{
    private final ASimpleFieldSelector field;

    /**
     * Construct a field constraint with the given values.
     *
     * @param field      Name of field to compare against
     * @param comparator Comparison type
     * @param value      Value to compare against
     */
    public FieldConstraint(@Nonnull ASimpleFieldSelector field, ComparisonType comparator, Object value)
    {
        super(comparator, value);

        this.field = field;
    }

    /**
     * @return the field selector
     */
    public ASimpleFieldSelector getField()
    {
        return field;
    }

    /**
     * @return The name of the field referenced by this constraint
     */
    public String getFieldName()
    {
        return getField().getFieldName();
    }

    @Override
    public boolean usesExtendedFields()
    {
        return !field.isCoreField();
    }

    @Override
    public Set<String> getReferencedStructures()
    {
        final Set<String> structures = new HashSet<>();

        final String structureName = field.getStructureName();
        if (structureName != null) structures.add(structureName);

        return structures;
    }

    @Internal
    @Override
    public void generateSQL(SqlState state)
    {
        // Handle composite fields types
        if (field.isCompositeType())
        {
            final CompositeType type = field.getCompositeType();
            final String[] propertyNames = type.getColumnNames();
            final int length = propertyNames.length;

            state.sql.append('('); // Ensure proper logic precedence

            for (int i = 0; i < length; i++)
            {
                final String fullPropertyName = type.getColumnName(i);
                state.sql.append("rdm.").append(fullPropertyName);

                state.sql.append(' ').append(type.getComparatorSymbol(comparator));

                if (comparator.usesValue())
                {
                    // No aliases supported
                    state.generateAndBindParameter(fullPropertyName, type.getPropertyValue(getValue(), i));
                }

                if (i < length - 1)
                {
                    // We only really handle EQ and NEQ
                    final String junction = comparator == ComparisonType.EQ ? " and " : " or ";
                    state.sql.append(junction);
                }
            }
            state.sql.append(')');
        } else
        {
            field.generateSQL(state); // Get the field selector emitted

            state.sql.append(' ').append(comparator.getSymbol());

            if (comparator.usesValue())
            {
                state.generateAndBindParameter(field.getAlias(), getValue());
            }
        }
    }

    @Override
    public String toString()
    {
        final StringBuilder s = new StringBuilder("[FieldConstraint: ");
        s.append(field).append(' ').append(comparator.getSymbol());

        if (comparator.usesValue())
        {
            s.append(' ').append(value);
        }
        s.append(']');

        return s.toString();
    }

    @Override
    public boolean equals(Object o)
    {
        if (this == o) return true;
        if (!(o instanceof FieldConstraint that)) return false;
        if (!super.equals(o)) return false;

        return field.equals(that.field);
    }

    @Override
    public int hashCode()
    {
        int result = super.hashCode();
        result = 31 * result + field.hashCode();
        return result;
    }
}
