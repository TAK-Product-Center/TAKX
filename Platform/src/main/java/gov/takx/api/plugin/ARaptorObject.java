package gov.takx.api.plugin;

import gov.takx.platform.KeyUtils;

/**
 * Base class for all namespace-protected, event-capable objects within the TAKX platform.
 *
 * @since 1.4
 */
public abstract class ARaptorObject extends ARaptorMetadata implements IRaptorObject
{
    /**
     * Identifies the family (namespace) this plugin is associated with.
     */
    protected String family;

    /**
     * Identifies the plugin type.
     */
    protected String type;

    /**
     * Identifies the plugin key.
     */
    protected String key;

    @Override
    public String getFamily()
    {
        return family;
    }

    @Override
    public String getType()
    {
        return type;
    }

    @Override
    public String getUniqueKey()
    {
        if (key == null || key.isEmpty())
        {
            key = KeyUtils.getUniqueKey(family, type);
        }

        return key;
    }
}
