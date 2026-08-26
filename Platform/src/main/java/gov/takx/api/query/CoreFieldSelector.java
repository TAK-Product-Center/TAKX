package gov.takx.api.query;

import gov.takx.api.annotation.Internal;
import gov.takx.api.persist.CompositeType;
import gov.takx.platform.query.SqlState;

import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Specifies a Core field in an RDM for selection or constraint handling in the query service.  You can construct
 * these instances directly or use the CoreField convenience member {@link CoreField#selector}.  Thus, the
 * following 2 idioms are equivalent:
 * <pre>
 *     new CoreFieldSelector(CoreField.elevation)
 *     CoreField.elevation.selector
 * </pre>
 *
 * @see CoreField
 * @see CoreField#selector
 * @see FieldConstraint
 * @since 1.49
 */
public class CoreFieldSelector extends ASimpleFieldSelector
{
    private final CoreField coreField;

    /**
     * Construct a selector on the given core field.
     *
     * @param coreField Core field enum
     * @see CoreField#selector
     */
    public CoreFieldSelector(CoreField coreField)
    {
        this(coreField, null);
    }

    /**
     * Construct a selector on the given core field using the given alias in generated sql.
     *
     * @param coreField Core field enum
     * @param alias     Alias to use in generated SQL
     * @see CoreField#selector
     */
    public CoreFieldSelector(CoreField coreField, String alias)
    {
        super(coreField.name(), alias);
        this.coreField = coreField;
    }

    /**
     * {@inheritDoc}
     *
     * @return true
     */
    @Override
    public boolean isCoreField()
    {
        return true;
    }

    /**
     * @return The CoreField referenced in this selector
     */
    public CoreField getCoreField()
    {
        return coreField;
    }

    @Override
    public CompositeType getCompositeType()
    {
        return coreField.getCompositeType();
    }

    @Override
    public boolean isCompositeType()
    {
        return coreField.isCompositeType();
    }

    @Internal
    @Override
    public void generateSQL(SqlState state)
    {
        final SqlState.SqlClause sqlClause = state.getSqlClause();

        // If we're generating a group by clause, then just emit our alias
        if (sqlClause == SqlState.SqlClause.GroupBy)
        {
            state.sql.append('"').append(getAlias()).append('"');
        } else
        {
            // Otherwise, emit the field selector and, optionally, the alias
            // Need to handle composite fields specially (by listing all the components)

            if (coreField.isCompositeType())
            {
                final CompositeType compositeType = coreField.getCompositeType();

                final String clause = Stream.of(compositeType.getColumnNames())
                        .map(cn -> "rdm." + cn + " as \"" + cn + "\"")
                        .collect(Collectors.joining(", "));

                state.sql.append(clause);
            } else
            {
                appendSQLFieldName(state);
                if (sqlClause == SqlState.SqlClause.Select)
                {
                    state.sql.append(" as ").append('"').append(getAlias()).append('"');
                }
            }
        }
    }

    @Internal
    @Override
    public void appendSQLFieldName(SqlState state)
    {
        state.sql.append("rdm.").append(fieldName);
    }

    @Override
    public boolean equals(Object o)
    {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        CoreFieldSelector that = (CoreFieldSelector) o;

        return coreField == that.coreField && alias.equals(that.alias);
    }

    @Override
    public int hashCode()
    {
        int result = coreField.hashCode();
        result = 31 * result + alias.hashCode();
        return result;
    }
}
