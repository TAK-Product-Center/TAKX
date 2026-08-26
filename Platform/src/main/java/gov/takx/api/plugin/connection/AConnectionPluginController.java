package gov.takx.api.plugin.connection;

import gov.takx.api.annotation.PseudoBean;
import gov.takx.api.plugin.BasicPluginStateManager;
import gov.takx.api.ui.IUserProxy;
import gov.takx.commons.utils.UUIDUtils;
import jakarta.annotation.Nonnull;
import jakarta.inject.Inject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.invoke.MethodHandles;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * Abstract plugin connection controller.  Most plugin connection controller implementations will simply
 * extend this base class and provide the family and type parameters via a super() constructor call.
 * <p>
 *
 * @since 2.0.0
 */
@SuppressWarnings({"CdiManagedBeanInconsistencyInspection", "unused"})
@PseudoBean
public abstract class AConnectionPluginController extends BasicPluginStateManager implements IConnectionPluginController
{
    private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    @Inject
    protected IUserProxy userProxy;

    @Inject
    protected ConfigurationMapSupport configurationMapSupport;

    protected final Map<String, String> metadata = new HashMap<>();

    protected final String family;
    protected final String type;
    protected final String uniqueKey = UUIDUtils.generateType1UUID().toString();

    protected boolean editable = false;

    protected IConnectionPluginDelegate delegate;
    protected String configuredFormat = null;
    protected String configuredName = null;

    /**
     * Plugin controller for specified family and type.  Generates a unique key for identification among multiple
     * controllers of the same family and type.
     *
     * @param family Connection family name
     * @param type   Connection type name
     */
    protected AConnectionPluginController(String family, String type)
    {
        this.family = family;
        this.type = type;
    }

    @Nonnull
    @Override
    public Set<String> getMetadataKeys()
    {
        return metadata.keySet();
    }

    @Override
    public String getMetadataValue(String key)
    {
        return metadata.get(key);
    }

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
        return uniqueKey;
    }

    @Override
    public void setDelegate(IConnectionPluginDelegate delegate)
    {
        this.delegate = delegate;
        onDelegateInjected();
    }

    /**
     * Event triggered immediately after the IConnectionPluginDelegate has been injected into the connection controller.
     * This event can be used by classes to perform any setup once the delegate has been injected.
     */
    protected void onDelegateInjected()
    {
        // Default to empty
    }

    /**
     * Base class version returns {@code null} as a default, basically saying the writing the connection out to a template
     * is not supported.  Override this method and return a parameters map (can be empty) to signify support for
     * writing the connection to a template file when an operator selects to export connections.
     *
     * @return {@code null}
     */
    @Override
    public Map<String, String> getRestorableParametersMap()
    {
        return null;
    }

    @Override
    public Map<String, Object> applyConfiguration(Map<String, Object> configuration, Map<String, Class> types)
    {
        configuredName = String.valueOf(configuration.get(ConnectionConfigurationConstants.NAME));
        configuredFormat = String.valueOf(configuration.get(ConnectionConfigurationConstants.FORMAT));

        // Now normalize the configuration map to ensure that the types are correct
        return configurationMapSupport.normalizeConfiguration(configuration, types);
    }

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
    @Override
    public String getConfiguredName(String defaultName)
    {
        return configuredName != null ? configuredName : defaultName;
    }

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
    @Override
    public String getConfiguredFormat(String defaultFormat)
    {
        return configuredFormat != null ? configuredFormat : defaultFormat;
    }

    /**
     * Base class version is a no-op other than a log message stating that a plugin that receives this notification
     * should have provided an override to handle opening a connection with an existing properties map.
     * <p>
     * {@inheritDoc}
     */
    @Override
    public void onOpenConnection(Map<String, Object> configuration, boolean autoStart)
    {
        logger.warn("Open connection with configuration map not implemented for {}, {} connection plugin", family, type);
    }

    @Override
    public void onAutoReconnect()
    {
        // do nothing.  Base class should override to provide the capability.
    }

    /**
     * Base class version is a no-op other than a debug log message stating that a plugin that receives this message
     * has specified that it is editable and therefore could have provided an override for this method
     * <p>
     * {@inheritDoc}
     */
    @Override
    public void onEditConnection()
    {
        logger.debug("Edit connection not implemented by {}, {} connection plugin", family, type);
    }

    /**
     * Base class version is a no-op other than a warning log message stating that a plugin that receives this message
     * has specified that it is editable and therefore SHOULD have provided an override for this method
     * <p>
     * {@inheritDoc}
     */
    @Override
    public void onUpdateConnection()
    {
        logger.warn("Update connection not properly implemented by {}, {} connection plugin", family, type);
    }

    /**
     * Base class version is a no-op other than a warning log message stating that a plugin that receives this message
     * has specified that it is editable and therefore SHOULD have provided an override for this method
     * <p>
     * {@inheritDoc}
     */
    @Override
    public void onUpdateConfiguredConnection(Map<String, Object> configuration)
    {
        logger.warn("Update connection with configuration map not properly implemented by {}, {} connection plugin", family, type);
    }

    /**
     * Set the display name for this connection to the configured name with given {@code commPath} appended.
     * This supports the typical pattern of using the connection type plus the comm path as the display name.
     * If no name has been configured, then the connection TYPE is used as the display name prefix.
     *
     * @param commPath Comm path value to append to the name
     * @since 2.20
     */
    protected void updateDisplayNameWithCommPath(String commPath)
    {
        delegate.setConnectionDisplayName(getConfiguredName(getType()) + " " + commPath);
    }
}
