package gov.takx.api.query;

import gov.takx.api.annotation.Internal;
import gov.takx.platform.query.SqlState;

/**
 * Abstract base class for field selectors (core, structure-based, aggregate, etc.).
 *
 * @since 1.49
 */
public abstract class AFieldSelector extends FieldAlias
{
    /**
     * Construct a selector with the given alias.
     *
     * @param alias Alias to use in SQL generation
     */
    protected AFieldSelector(String alias)
    {
        super(alias);
    }

    /**
     * Return {@code true} if this selector refers to a core field, {@code false} if it refers to a structure field.
     */
    public abstract boolean isCoreField();

    /**
     * Get the name of the structure referenced by this selector. The default is {@code null}. Yes, this is a little
     * broken w.r.t. clean object design, but it greatly simplifies the handling of selectors within the query
     * constraints.
     *
     * @return Name of structure referenced by this selector, default to {@code null}
     */
    public String getStructureName()
    {
        return null;
    }

    /**
     * @return The name of the field referenced in this selector
     */
    public abstract String getFieldName();

    /**
     * Generate SQL for this selector using the provided SQL state.
     *
     * @param state Sql generation state for mapping field names and parameter binding
     */
    @Internal
    public abstract void generateSQL(SqlState state);
}
