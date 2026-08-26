package gov.takx.api.query.param;

import gov.takx.api.annotation.Internal;

/**
 * Specifies a named parameter binding.
 *
 */
@Internal
public class HQLParameter
{
    public String name = null;
    public Object value = null;

    public HQLParameter(String name, Object value)
    {
        this.name = name;
        this.value = value;
    }

    public String toString()
    {
        return "[HQLParameter: name=" + name + " value=" + value + "]";
    }
}
