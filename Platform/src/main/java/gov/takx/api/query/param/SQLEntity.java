package gov.takx.api.query.param;

import gov.takx.api.annotation.Internal;

/**
 * Specifies an entity type to retrieve via a hibernate query.
 *
 */
@Internal
public class SQLEntity extends HQLParameter
{
    public Class value = null;  // In this derived type, the value is always a Class

    public SQLEntity(String name, Class value)
    {
        super(name, value);
        this.value = value;
    }
}
