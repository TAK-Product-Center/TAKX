package gov.takx.api.registry;

import gov.takx.api.plugin.PluginCategory;

/**
 * Thrown to indicate that the specified family and type for a plugin operation found no
 * case-sensitive match.
 *
 */
public class NoSuchPluginException extends RuntimeException
{
    public NoSuchPluginException(PluginCategory category, String family, String type)
    {
        super("No plugin of family " + family + "/" + type + " could be found in the " + category + " plugin registry");
    }

    public NoSuchPluginException(String msg)
    {
        super(msg);
    }
}
