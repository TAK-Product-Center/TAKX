package gov.takx.api.plugin;

/**
 * Collection of keys and logging prefix/suffix constants.
 *
 */
@SuppressWarnings("unused")
public interface PluginConstants
{
    /**
     * Used for TRACE level logging to identify a plugin touch point is about to be made.
     */
    String BEGIN_PLUGIN_CALL = "BEGIN_PLUGIN_CALL";

    /**
     * Used for TRACE level logging to identify a plugin touch point has returned.
     */
    String RETURN_PLUGIN_CALL = "RETURN_PLUGIN_CALL";

    /**
     * Used for WARN level logging to identify that a plugin threw an unexpected touch point and did not return.
     */
    String PLUGIN_EXCEPTION = "PLUGIN_EXCEPTION";
}
