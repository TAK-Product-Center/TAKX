package gov.takx.api.query;

import gov.takx.api.annotation.Internal;
import gov.takx.api.persist.CompositeType;
import gov.takx.platform.query.SqlState;
import jakarta.annotation.Nonnull;

/**
 * Specifies a field in an extended structure for selection or constraint handling in the query service.
 *
 * @see FieldConstraint
 * @since 1.49
 */
public class StructureFieldSelector extends ASimpleFieldSelector
{
    protected String structure;

    /**
     * Constructor.
     *
     * @param structure Name of structure defining field
     * @param fieldName Name of field to select
     */
    public StructureFieldSelector(@Nonnull String structure, String fieldName)
    {
        super(fieldName);

        this.structure = structure;
    }

    /**
     * Constructor.
     *
     * @param structure  Name of structure defining field
     * @param fieldName  Name of field to select
     * @param fieldAlias Alias of field to select
     */
    public StructureFieldSelector(@Nonnull String structure, String fieldName, String fieldAlias)
    {
        super(fieldName, fieldAlias);

        this.structure = structure;
    }

    /**
     * {@inheritDoc}
     *
     * @return false
     */
    @Override
    public boolean isCoreField()
    {
        return false;
    }

    /**
     * {@inheritDoc}
     *
     * @return structure name, will not be {@code null}
     */
    @Override
    public String getStructureName()
    {
        return structure;
    }

    /**
     * Composite types are not supported in structure fields, yet.
     *
     * @return {@code null}
     */
    @Override
    public CompositeType getCompositeType()
    {
        return null;
    }

    /**
     * Composite types are not supported in structure fields, yet.
     *
     * @return false
     */
    @Override
    public boolean isCompositeType()
    {
        return false;
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
            appendSQLFieldName(state);

            if (sqlClause == SqlState.SqlClause.Select)
            {
                state.sql.append(" as ").append('"').append(getAlias()).append('"');
            }
        }
    }

    @Internal
    @Override
    public void appendSQLFieldName(SqlState state)
    {
        state.sql.append(state.getTableAlias())
                .append('.')
                .append(state.mapField(fieldName));
    }

    @Override
    public String toString()
    {
        return structure + "." + super.toString();
    }

    @Override
    public boolean equals(Object o)
    {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        StructureFieldSelector that = (StructureFieldSelector) o;

        if (!alias.equals(that.alias)) return false;
        if (!fieldName.equals(that.fieldName)) return false;
        //noinspection RedundantIfStatement
        if (!structure.equals(that.structure)) return false;

        return true;
    }

    @Override
    public int hashCode()
    {
        int result = structure.hashCode();
        result = 31 * result + fieldName.hashCode();
        result = 31 * result + alias.hashCode();
        return result;
    }
}
