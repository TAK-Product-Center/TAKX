package gov.takx.api.query.param;

import gov.takx.api.annotation.Internal;
import org.hibernate.type.Type;

/**
 * Scalar binding, typically used to add a "count" operation to an HQL query.
 *
 */
@Internal
public class SQLScalar extends HQLParameter
{
    public Type value = null;  // In this derived type, the value is always a Hibernate Type

    public SQLScalar(String name, Type value)
    {
        super(name, value);
        this.value = value;
    }
}
