package gov.takx.api.query;

import gov.takx.api.annotation.Internal;
import gov.takx.platform.query.SqlState;
import jakarta.annotation.Nonnull;

import java.util.HashSet;
import java.util.Set;

/**
 * An AggregateFieldConstraint is a ComparisonConstraint on an {@link AggregateFieldSelector}.
 * The constraints are only of the form (aggregate comparator value); for example, {@code MAX(elevation) < 150}.
 * <p/>
 * AggregateFieldConstraints are only valid in the HAVING clause of a GROUP BY expression.
 *
 * @see IQueryConstraints#addGroupConstraints(IConstraint...)
 * @since 1.49
 */
public class AggregateFieldConstraint extends ComparisonConstraint
{
    protected final AggregateFieldSelector aggregate;

    /**
     * Construct an AggregateFieldConstraint based on the provided arguments.
     *
     * @param aggregate  the aggregate
     * @param comparator the comparator
     * @param value      the value
     */
    public AggregateFieldConstraint(@Nonnull AggregateFieldSelector aggregate, ComparisonType comparator, Object value)
    {
        super(comparator, value);

        this.aggregate = aggregate;
    }

    /**
     * The Aggregate for this AggregateFieldConstraint.  A FieldConstraint is 'field OP value'; we use
     * aggregate to replace 'field' with something like 'FUNC(column)'.  An arguably better design would
     * not have AggregateFieldConstraint inherit from FieldConstraint but from, perhaps ComparisonConstraint.
     *
     * @return the aggregate field selector
     */
    public AggregateFieldSelector getAggregate()
    {
        return aggregate;
    }

    @Internal
    @Override
    public void generateSQL(SqlState state)
    {
        aggregate.generateSQL(state); // Get the field selector emitted

        state.sql.append(' ').append(comparator.getSymbol());

        if (comparator.usesValue())
        {
            state.generateAndBindParameter(getAlias(), getValue());
        }
    }

    /**
     * @return The alias (wither specified or default generated) for this aggregate
     */
    public String getAlias()
    {
        return aggregate.getAlias();
    }

    /**
     * @return The name of the field referenced by the aggregate
     */
    public String getFieldName()
    {
        return aggregate.getField().getFieldName();
    }

    @Override
    public boolean usesExtendedFields()
    {
        return !aggregate.isCoreField();
    }

    @Override
    public Set<String> getReferencedStructures()
    {
        final Set<String> structures = new HashSet<>();

        final String structureName = aggregate.getField().getStructureName();
        if (structureName != null) structures.add(structureName);

        return structures;
    }

    @Override
    public boolean equals(Object o)
    {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        if (!super.equals(o)) return false;

        AggregateFieldConstraint that = (AggregateFieldConstraint) o;

        return aggregate.equals(that.aggregate);
    }

    @Override
    public int hashCode()
    {
        int result = super.hashCode();
        result = 31 * result + aggregate.hashCode();
        return result;
    }
}
