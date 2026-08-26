package gov.takx.api.plugin.connection;

import java.util.Collections;
import java.util.Map;

/**
 * Manages both the UI state of the connection, and the physical connection itself. Recommendation is to
 * override {@link AConnectionPluginController} rather than implementing this interface.
 *
 * @see IConnectionPlugin
 * @since 2.0.0
 */
public interface IConnectionPluginController extends IConnectionLifecycleListener
{
    /**
     * Delegate injected that permits the controller to basic delegate operations including the ability
     * to get the {@code ICommServiceDelegate} to manage connections.
     *
     * @param delegate injected delegate controller.
     * @see ICommServiceDelegate
     */
    void setDelegate(IConnectionPluginDelegate delegate);

    /**
     * Implement this method and return a parameter map to support writing out connection parameters to a template.
     *
     * @return connection parameters
     */
    Map<String, String> getRestorableParametersMap();

    /**
     * Get the configured name if one has been set (typically via {@code applyConfiguration}).  If no
     * name has been configured, then the given {@code defaultName} is returned.
     *
     * @param defaultName Value to return if no name has been configured
     * @return Configured name or {@code defaultName} if one has not been set
     * @see #applyConfiguration(Map)
     * @see #applyConfiguration(Map, Map)
     * @since 2.20
     */
    String getConfiguredName(String defaultName);

    /**
     * Get the configured format if one has been set (typically via {@code applyConfiguration}).  If no
     * format has been configured, then the given {@code defaultFormat} is returned.
     *
     * @param defaultFormat Value to return if no format has been configured
     * @return Configured format or {@code defaultFormat} if one has not been set
     * @see #applyConfiguration(Map)
     * @see #applyConfiguration(Map, Map)
     * @since 2.20
     */
    String getConfiguredFormat(String defaultFormat);

    /**
     * Apply the given configuration to this connection controller.  All configuration values are assumed
     * to be String values (and are coerced to Strings if they are not).
     * <p>
     * If the standard configuration properties of NAME of FORMAT exist, they are extracted and made
     * available using {@link #getConfiguredName(String)} and {@link #getConfiguredFormat(String)}.
     *
     * @param configuration Configuration to apply
     * @return Normalized configuration map with all values as Strings
     * @see ConnectionConfigurationConstants#FORMAT
     * @see ConnectionConfigurationConstants#NAME
     * @see #getConfiguredFormat(String)
     * @see #getConfiguredName(String)
     * @since 2.20
     */
    default Map<String, Object> applyConfiguration(Map<String, Object> configuration)
    {
        return applyConfiguration(configuration, Collections.emptyMap());
    }

    /**
     * Apply the given configuration to this connection controller.  Configuration value types can be specified
     * using the {@code types} map.  Each key in
     * {@code configuration} is looked up in {@code types} to determine the desired type of the property.
     * If there is no entry in {@code types} for a given property key, then a type of {@code String} is assumed.
     * <p>
     * Only {@code String}, {@code Boolean}, and the {@code Number} types are supported as target types.
     * <p>
     * If a value in the given configuration is of a {@code Number} type and the required type is a different
     * {@code Number} type, then a type coercion to the target type will be performed.
     * <p>
     * If the standard configuration properties of NAME of FORMAT exist, they are extracted and made
     * available using {@link #getConfiguredName(String)} and {@link #getConfiguredFormat(String)}.
     *
     * @param configuration Configuration to apply
     * @return Normalized configuration map with all values as Strings
     * @see ConnectionConfigurationConstants#FORMAT
     * @see ConnectionConfigurationConstants#NAME
     * @see #getConfiguredFormat(String)
     * @see #getConfiguredName(String)
     * @see ConfigurationMapSupport#normalizeConfiguration(Map, Map)
     * @since 2.20
     */
    Map<String, Object> applyConfiguration(Map<String, Object> configuration, Map<String, Class> types);
}
