package gov.takx.api.query;

import gov.takx.api.annotation.Internal;
import gov.takx.platform.query.SqlState;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * LogicConstraint that limits a query to a LogicType applied to subConstraints
 *
 * @see FieldConstraint
 * @see IQueryConstraints
 * @since 1.49
 */
public class LogicConstraint implements IConstraint
{
    protected LogicType type;
    protected List<IConstraint> subconstraints;

    /**
     * Construct a LogicConstraint of the given type on the set of sub-constraints.
     *
     * @param type           The LogicType to apply
     * @param subconstraints The subconstraints
     */
    public LogicConstraint(LogicType type, List<IConstraint> subconstraints)
    {
        if (LogicType.not == type && 1 != subconstraints.size())
        {
            throw new IllegalArgumentException("LogicType.NOT expects exactly one subconstraint");
        }

        this.type = type;
        this.subconstraints = subconstraints;
    }

    /**
     * Construct a LogicConstraint based on the provided values
     *
     * @param type           The LogicType to apply
     * @param subconstraints The IConstraint subconstraints
     */
    public LogicConstraint(LogicType type, IConstraint... subconstraints)
    {
        this(type, Arrays.asList(subconstraints));
    }

    /**
     * @return the logic type of this constraint
     */
    public LogicType getLogicType()
    {
        return type;
    }

    /**
     * @return The subconstraints used by this logic constraint
     */
    public List<IConstraint> getSubconstraints()
    {
        return subconstraints;
    }

    /**
     * Determine if this logic constraint, and all subconstraints, use/reference any extended structure fields.
     *
     * @return {@code true} if a structure field is referenced, {@code false} if only core fields are referenced
     */
    @Override
    public boolean usesExtendedFields()
    {
        for (IConstraint subconstraint : subconstraints)
        {
            if (subconstraint.usesExtendedFields()) return true;
        }

        return false;
    }

    @Override
    public Set<String> getReferencedStructures()
    {
        final Set<String> structures = new HashSet<>();

        for (IConstraint subconstraint : subconstraints)
        {
            structures.addAll(subconstraint.getReferencedStructures());
        }

        return structures;
    }

    @Internal
    @Override
    public void generateSQL(SqlState state)
    {
        // We are going to explicitly group every logic constraint with either:
        //   a) NOT ( ... )
        //   b) ( ... AND/OR ... )

        if (LogicType.not == type) state.sql.append(type);

        state.sql.append('(');

        // We will recurse on 'constraints', a list, even though NOT has only one, surely.
        // The connector argument will be 'AND' or 'OR' or an ignored 'NOT'
        state.getGenerator().generateSqlForLogicSubconstraints(this);

        state.sql.append(')'); // Close off SQL fragment
    }

    @Override
    public boolean equals(Object o)
    {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        LogicConstraint that = (LogicConstraint) o;

        return type == that.type && subconstraints.equals(that.subconstraints);
    }

    @Override
    public int hashCode()
    {
        int result = type.hashCode();
        result = 31 * result + subconstraints.hashCode();
        return result;
    }

    @Override
    public String toString()
    {
        final StringBuilder sb = new StringBuilder();
        sb.append("LogicConstraint: ").append(type).append(" (");

        if (type == LogicType.not)
        {
            sb.append(subconstraints.get(0));
        } else
        {
            subconstraints.forEach(sc -> sb.append(sc).append(",\n"));
        }
        sb.append(")");

        return sb.toString();
    }
}
