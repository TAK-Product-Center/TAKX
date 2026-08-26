package gov.takx.api.query;

import gov.takx.api.annotation.Internal;
import gov.takx.api.persist.CompositeType;
import gov.takx.platform.query.SqlState;
import jakarta.annotation.Nonnull;

/**
 * Abstract base for "simple" selectors.  This includes any field/column that can be selected without causing
 * a structural change in the query results.  For example, a core or extended column selection is "simple,"
 * but an aggregate selector is not since it causes the results to be grouped.
 *
 * @since 1.49
 */
public abstract class ASimpleFieldSelector extends AFieldSelector
{
    protected String fieldName;

    /**
     * Construct a selector for the given field name.
     *
     * @param fieldName Field to select
     */
    protected ASimpleFieldSelector(String fieldName)
    {
        this(fieldName, null);
    }

    /**
     * Construct a field selector with the given alias.
     *
     * @param fieldName Field to select
     * @param alias     Alias to use in SQL generation
     */
    protected ASimpleFieldSelector(@Nonnull String fieldName, String alias)
    {
        super(alias == null ? fieldName : alias);

        this.fieldName = fieldName;
    }

    /**
     * @return Field name
     */
    @Override
    public String getFieldName()
    {
        return fieldName;
    }

    /**
     * Generate the fully-qualified field name (e.g. for use in an ORDER BY clause) and append to the state.
     *
     * @param state Sql generation state for mapping field names and parameter binding. Will be modified by this method
     * @since 2.20
     */
    @Internal
    public abstract void appendSQLFieldName(SqlState state);

    /**
     * @return {@code true} if this core field uses a composite data type
     */
    public abstract boolean isCompositeType();

    /**
     * @return The composite type for this field
     */
    public abstract CompositeType getCompositeType();

    @Override
    public String toString()
    {
        return alias != null ? (fieldName + " as " + alias) : fieldName;
    }
}
