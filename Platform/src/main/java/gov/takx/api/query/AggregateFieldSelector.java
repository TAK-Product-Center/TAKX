package gov.takx.api.query;

import gov.takx.api.annotation.Internal;
import gov.takx.platform.query.SqlState;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.invoke.MethodHandles;

/**
 * An AggregateFieldSelector defines the application of an {@link AggregateType} (a 'SQL Aggregate Function') to a
 * {@link ASimpleFieldSelector} (table column). Examples would be {@code MAX(elevation)} or
 * {@code COUNT(SomeStructure.myField)}.
 * <p/>
 * An Aggregate can be applied to a DISTINCT selection of a field, yielding, for example, {@code COUNT(DISTINCT elevation)}.
 * <p/>
 * An alias can be specified for use in SQL generation.  If no alias is specified, then a default alias will be
 * generated using the following form: &lt;type&gt;Of[Distinct]&lt;field&gt;, for example an aggregate type of MAX on
 * the 'elevation' field, would result in a default alias of {@code maxOfElevation}.  When using the query method
 * {@link IQueryService#queryForFieldsAsMap(IQueryConstraints, FieldSelector)}, this alias is the name used as the
 * key in the returned field map.
 * <p/>
 * See {@link AggregateFieldConstraint} for how to apply a value constraint on an aggregate.
 *
 * @see ASimpleFieldSelector
 * @see AggregateFieldConstraint
 * @since 1.49
 */
public class AggregateFieldSelector extends AFieldSelector
{
    private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    private final AggregateType type;
    private final boolean distinct;
    private final ASimpleFieldSelector field;

    /**
     * Construct an aggregate with a default value of {@code false} for distinct, and a default alias.
     *
     * @param type  Aggregate type (function)
     * @param field Selector for field to aggregate
     */
    public AggregateFieldSelector(AggregateType type, ASimpleFieldSelector field)
    {
        this(type, false, field, generateAliasName(type, false, field));
    }

    /**
     * Construct an aggregate with a default alias.
     *
     * @param type     Aggregate type (function)
     * @param distinct Pass {@code true} to aggregate only distinct values of the field
     * @param field    Selector for field to aggregate
     */
    public AggregateFieldSelector(AggregateType type, boolean distinct, ASimpleFieldSelector field)
    {
        this(type, distinct, field, generateAliasName(type, distinct, field));
    }

    /**
     * Construct an aggregate.
     *
     * @param type     Aggregate type (function)
     * @param distinct Pass {@code true} to aggregate only distinct values of the field
     * @param field    Selector for field to aggregate
     * @param alias    Alias to use in generated SQL and returned field maps
     */
    public AggregateFieldSelector(AggregateType type, boolean distinct, ASimpleFieldSelector field, String alias)
    {
        super(alias);

        this.type = type;
        this.distinct = distinct;
        this.field = field;
    }

    /**
     * @return Aggregate type (function)
     */
    public AggregateType getType()
    {
        return type;
    }

    /**
     * @return {@code true} if aggregating distinct values
     */
    public boolean isDistinct()
    {
        return distinct;
    }

    /**
     * @return The field being aggregated
     */
    public ASimpleFieldSelector getField()
    {
        return field;
    }

    /**
     * @return The name of the field referenced by the aggregate
     */
    @Override
    public String getFieldName()
    {
        return getField().getFieldName();
    }

    @Override
    public boolean isCoreField()
    {
        return getField().isCoreField();
    }

    /**
     * {@inheritDoc}
     *
     * @return structure name, will not be {@code null}
     */
    @Override
    public String getStructureName()
    {
        return isCoreField() ? null : getField().getStructureName();
    }

    @Internal
    @Override
    public void generateSQL(SqlState state)
    {
        final SqlState.SqlClause sqlClause = state.getSqlClause();

        // If we're generating a group by clause, then just emit our alias
        if (sqlClause == SqlState.SqlClause.GroupBy)
        {
            logger.error("Attempted to generate aggregate in GROUP BY clause", new RuntimeException("SQL error"));
            return;
        }

        // Otherwise, emit the aggregate function and, optionally, the alias
        final boolean coreField = isCoreField();

        final String table = coreField ? "rdm" : state.getTableAlias();
        final String column = coreField ? getFieldName() : state.mapField(getFieldName());

        if (column == null)
        {
            throw new QueryExecutionException("Invalid aggregate: " + this,
                    new NoSuchFieldException("No such field: " + simpleFieldName()));
        }

        state.sql.append(type.toString()).append('(');
        if (distinct) state.sql.append("DISTINCT ");
        state.sql.append(table).append('.').append(column);
        state.sql.append(')');

        if (sqlClause == SqlState.SqlClause.Select)
        {
            state.sql.append(" as \"").append(alias).append('\"');
        }
    }

    /**
     * Generate the appropriate default alias for this aggregate selector.
     * The form is: &lt;type&gt;Of[Distinct]&lt;field&gt;
     *
     * @param type     Aggregate type
     * @param distinct Distinct or not
     * @param field    field being aggregated
     * @return alias name
     */
    private static String generateAliasName(AggregateType type, boolean distinct, ASimpleFieldSelector field)
    {
        // Determine the field name to use, use the alias if defined
        final String alias = field.getAlias();
        final String name = alias != null ? alias : field.getFieldName();
        final String capName = name.isEmpty() ? name : name.substring(0, 1).toUpperCase() + name.substring(1);

        return type.toString().toLowerCase()
                + "Of"
                + (distinct ? "Distinct" : "")
                + capName;
    }

    @Override
    public String toString()
    {
        return type.toString() + "(" + (distinct ? "distinct " : "") + simpleFieldName() + ")";
    }

    /**
     * @return A simple field name (no alias) for use in toString and exceptions
     */
    private String simpleFieldName()
    {
        final String s = field.getStructureName();
        return (s != null ? s + "." : "") + getFieldName();
    }

    @Override
    public boolean equals(Object o)
    {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        AggregateFieldSelector that = (AggregateFieldSelector) o;

        if (distinct != that.distinct) return false;
        if (!field.equals(that.field)) return false;
        //noinspection RedundantIfStatement
        if (type != that.type) return false;

        return true;
    }

    @Override
    public int hashCode()
    {
        int result = type.hashCode();
        result = 31 * result + (distinct ? 1 : 0);
        result = 31 * result + field.hashCode();
        return result;
    }
}
