package gov.takx.api.query;

import gov.takx.commons.utils.StringUtils;

/**
 * A FieldAlias holds an alias name for use in generating SQL fragments for field selectors and
 * aggregate fields.  The configured alias will have all whitespace removed to ensure it can be used as a SQL
 * identifier.
 *
 * @since 1.49
 */
public abstract class FieldAlias
{
    protected String alias;

    /**
     * Constructor.
     *
     * @param alias The field's alias, may not be {@code null}
     */
    protected FieldAlias(String alias)
    {
        this.alias = StringUtils.stripWhitespace(alias);
    }

    /**
     * @return The alias
     */
    public String getAlias()
    {
        return alias;
    }
}
