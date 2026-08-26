package gov.takx.core.connection;

import gov.takx.api.commservices.ICommServiceManager;
import gov.takx.api.commservices.ISerialPortStatus;
import gov.takx.api.connection.IComposedConnectionController;
import gov.takx.api.connection.IConnectionManager;
import gov.takx.api.connection.IConnectionManagerListener;
import gov.takx.api.filesystem.IVirtualFileSystem;
import gov.takx.api.filesystem.VFSScope;
import gov.takx.api.gateway.IDeviceGateway;
import gov.takx.api.persist.IPersistableState;
import gov.takx.api.persist.IPluginStatePersist;
import gov.takx.api.plugin.IRaptorObject;
import gov.takx.api.plugin.PluginConstants;
import gov.takx.api.plugin.connection.ConnectionState;
import gov.takx.api.plugin.connection.ICommServiceDelegate;
import gov.takx.api.plugin.connection.IConnectionLifecycleListener;
import gov.takx.api.plugin.connection.IConnectionPanel;
import gov.takx.api.plugin.connection.IConnectionPlugin;
import gov.takx.api.plugin.connection.IConnectionPluginController;
import gov.takx.api.plugin.connection.IConnectionPluginDefinition;
import gov.takx.api.registry.IConnectionPluginRegistry;
import gov.takx.api.ui.IConnectionSwingUIConsumer;
import gov.takx.core.filesystem.VirtualFileSystemManagerProvider;
import gov.takx.core.platform.APluginDelegate;
import gov.takx.core.uibridge.SwingUIBinder;
import gov.takx.platform.utils.RaptorObjectFactory;
import jakarta.annotation.Nonnull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.invoke.MethodHandles;
import java.util.Collection;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Composed connection controller acts as a delegate/listener for the IConnectionPluginController.  Maintains a list
 * of IConnectionManagerListener that want to be notified of a state change of a connection.  As a delegate,
 * allows the IConnectionPluginController to send connection events to the IConnection implementation associated
 * with the connection plugin controller.
 *
 */
public class ComposedConnectionController extends APluginDelegate implements IComposedConnectionController
{
    private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    private final IConnectionPluginController conxPluginController;
    private final List<IConnectionManagerListener> connectionStateListeners = new CopyOnWriteArrayList<>();
    private final Map<IConnectionPluginController, Map<VFSScope, IVirtualFileSystem>> fileSystems = new HashMap<>();
    private String displayName;
    private ConnectionState connectionState = ConnectionState.Disconnected;
    // Need to track the messages for the state so that we can send them to RW one someone pulls down the connections
    private final Map<ConnectionState, String> connectionStateMessages = new EnumMap<>(ConnectionState.class);

    /**
     * Constructor.
     *
     * @param controller IConnectionPluginController instance.
     * @since 2.0.0
     */
    public ComposedConnectionController(IConnectionPluginController controller, String name)
    {
        super(controller.getFamily(), controller.getType(), null, controller.getUniqueKey());

        conxPluginController = controller;
        displayName = name;
    }

    @Override
    public Collection<ISerialPortStatus> listSerialPorts()
    {
        return getBean(ICommServiceManager.class).getSerialPortCommStatus();
    }

    /**
     * Add a connection state listener to the list of listeners.  Listeners will be notified by the connection plugin
     * when the connection state changes.
     *
     * @param listener connection state listener
     */
    @Override
    public void addConnectionStateListener(@Nonnull IConnectionManagerListener listener)
    {
        if (!connectionStateListeners.contains(listener))
        {
            connectionStateListeners.add(listener);
        }
    }

    /**
     * Remove a connection state listener from the list of listeners.
     *
     * @param listener IClientConnectionStateListener
     */
    @Override
    public void removeConnectionStateListener(@Nonnull IConnectionManagerListener listener)
    {
        connectionStateListeners.remove(listener);
    }

    @Override
    public void removeConnection()
    {
        // We can't trust that a plugin didn't call this method after the connection was already persisted and added
        // to the connection panel.  Because of that, we need to check with the connection manager to see if
        // this is a managed connection when calling the removeConnection method.
        IConnectionManager connectionManager = getBean(IConnectionManager.class);
        connectionManager.removeConnection(uniqueKey, connectionManager.isManagedConnection(uniqueKey));
    }

    @Override
    public String getConnectionDisplayName()
    {
        return displayName;
    }

    /**
     * Connection is being removed.
     *
     * @see IConnectionLifecycleListener
     */
    @Override
    public void shutdown()
    {
        try
        {
            conxPluginController.onShutdown();
        } catch (Exception e)
        {
            logger.warn(PluginConstants.PLUGIN_EXCEPTION + ": Exception in onShutdown in ConnectionController {}", uniqueKey, e);
        }
    }

    @Override
    public String getMetadataValue(String key, String defaultValue)
    {
        String result = conxPluginController.getMetadataValue(key);
        if (null == result)
        {
            result = defaultValue;
        }

        return result;
    }

    @Override
    public String getConnectionName()
    {
        final Optional<IConnectionPluginDefinition> pluginDefinition = getBean(IConnectionPluginRegistry.class).getPluginDefinition(family, type);
        return pluginDefinition.isPresent() ? pluginDefinition.get().getName() : "Unknown";
    }

    @Override
    public IConnectionPluginController getPluginConnectionController()
    {
        return conxPluginController;
    }

    @Override
    public void onProjectSaving()
    {
        try
        {
            getBean(IPluginStatePersist.class).saveOrUpdateState(uniqueKey, conxPluginController);
        } catch (Exception e)
        {
            logger.warn(PluginConstants.PLUGIN_EXCEPTION + ": Exception in saveOrUpdateState ", e);
        }
    }

    @Override
    public ConnectionState getState()
    {
        return connectionState;
    }

    @Override
    public String getMessage(ConnectionState connectionState)
    {
        return connectionStateMessages.get(connectionState);
    }

    /**
     * {@inheritDoc}
     * <p>
     * Also responsible for into the controller if it declares itself as a consumer of the panel.
     */
    @Override
    public IConnectionPanel getConnectionPanel()
    {
        IConnectionPanel panel = null;
        try
        {
            panel = getBean(SwingUIBinder.class).forConnection(this).getConnectionPanel();
            if (conxPluginController instanceof IConnectionSwingUIConsumer uiConsumer) uiConsumer.setConnectionPanel(panel);
        } catch (Exception e)
        {
            logger.warn(PluginConstants.PLUGIN_EXCEPTION + ": Exception in setConnectionPanel", e);
        }

        return panel;
    }

    @Override
    public ICommServiceDelegate getCommServiceDelegate()
    {
        return getBean(IDeviceGateway.class).getCommServiceDelegate();
    }

    @Override
    public void setConnectionState(ConnectionState connectionState)
    {
        setConnectionState(connectionState, null);
    }

    @Override
    public void setConnectionState(ConnectionState connectionState, String message)
    {
        this.connectionState = connectionState;
        connectionStateMessages.put(connectionState, message);
        notifyListeners(connectionState, message);
    }

    @Override
    public void setConnectionDisplayName(String displayName)
    {
        this.displayName = displayName;

        IRaptorObject clonedRO = RaptorObjectFactory.clone(this);

        for (IConnectionManagerListener listener : connectionStateListeners)
        {
            listener.onConnectionNameChanged(clonedRO, displayName);
        }
    }

    @Override
    public synchronized IVirtualFileSystem getVirtualFileSystem(VFSScope scope)
    {
        IVirtualFileSystem fs = null;
        Map<VFSScope, IVirtualFileSystem> map = fileSystems.get(conxPluginController);
        if (map == null)
        {
            map = new EnumMap<>(VFSScope.class);
            fileSystems.put(conxPluginController, map);
        } else
        {
            fs = map.get(scope);
        }
        if (fs == null)
        {
            IConnectionPlugin plugin = getBean(IConnectionPluginRegistry.class).getPlugin(family, type);
            if (plugin != null)
            {
                fs = getBean(VirtualFileSystemManagerProvider.class).getVirtualFileSystemManager(scope).getVirtualFileSystemForConnection(plugin.getFamily(), plugin.getType());
                map.put(scope, fs);
            } else
            {
                logger.error("Could not find Connection Plugin for family: {} of type: {}", family, type);
            }
        }

        return fs;
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

    @Nonnull
    @Override
    public Set<String> getMetadataKeys()
    {
        return conxPluginController.getMetadataKeys();
    }

    @Override
    public String getMetadataValue(String key)
    {
        return conxPluginController.getMetadataValue(key);
    }

    private void notifyListeners(ConnectionState state, String message)
    {
        IRaptorObject clonedRO = RaptorObjectFactory.clone(this);

        for (IConnectionManagerListener listener : connectionStateListeners)
        {
            listener.onConnectionStateChanged(clonedRO, state, message);
        }
    }

    @Override
    protected IPersistableState getPersistableState()
    {
        return conxPluginController;
    }
}
