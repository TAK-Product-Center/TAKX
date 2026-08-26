package gov.takx.core.connection;

import gov.takx.api.annotation.InvokeOnEDT;
import gov.takx.api.annotation.UiEventBus;
import gov.takx.api.configuration.IProgramEnvironment;
import gov.takx.api.connection.IComposedConnectionController;
import gov.takx.api.connection.IConnectionManager;
import gov.takx.api.connection.IConnectionManagerListener;
import gov.takx.api.event.StartPlugins;
import gov.takx.api.event.StartProjectServices;
import gov.takx.api.event.StopProjectServices;
import gov.takx.api.event.project.ProjectSaving;
import gov.takx.api.messages.IPluginState;
import gov.takx.api.persist.IConnectionPersist;
import gov.takx.api.persist.IPluginStatePersist;
import gov.takx.api.plugin.IRaptorObject;
import gov.takx.api.plugin.PluginCategory;
import gov.takx.api.plugin.connection.ConnectionState;
import gov.takx.api.plugin.connection.IConnectionPlugin;
import gov.takx.api.plugin.connection.IConnectionPluginController;
import gov.takx.api.plugin.connection.IConnectionPluginDefinition;
import gov.takx.api.plugin.connection.IDelegatedConnectionAuthentication;
import gov.takx.api.plugin.connection.NoSuchConnectionControllerException;
import gov.takx.api.plugin.connection.TooManyConnectionsException;
import gov.takx.api.project.IProject;
import gov.takx.api.project.IProjectManager;
import gov.takx.api.registry.IConnectionPluginRegistry;
import gov.takx.api.registry.NoSuchPluginException;
import gov.takx.api.ui.IConnectionSwingUIConsumer;
import gov.takx.commons.swing.AlwaysOnTopJOptionPane;
import gov.takx.commons.swing.AuthenticationDialog;
import gov.takx.core.connection.gui.EditConnectionDialog;
import gov.takx.core.project.ServiceOrdering;
import gov.takx.core.uibridge.SwingUIBinder;
import gov.takx.core.view.gui.connections.ImportConnectionTemplateDialog;
import gov.takx.core.view.gui.event.ProxyButtonEvent;
import gov.takx.dataservices.persist.Connection;
import gov.takx.platform.KeyUtils;
import gov.takx.platform.schema.connection.ConnectionDefinitionType;
import gov.takx.platform.schema.connection.ConnectionDefinitionsType;
import gov.takx.platform.schema.connection.PropertiesType;
import gov.takx.platform.schema.connection.PropertyType;
import gov.takx.platform.utils.ConnectionTemplateManager;
import gov.takx.platform.utils.RaptorObjectFactory;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Priority;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import jakarta.inject.Inject;
import jakarta.xml.bind.JAXBException;
import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;

import java.awt.event.ActionEvent;
import java.io.File;
import java.io.IOException;
import java.lang.invoke.MethodHandles;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.stream.Collectors;

/**
 * Client connection manager, manages the controllers(delegates) associated with IPluginConnectionControllers,
 * and notifies listeners, such as the ConnectionPanel that the state of a connection has changed.  Also
 * keeps a list of open "EditConnection" dialogs in case an already open one is requested.
 *
 */
@SuppressWarnings({"CdiInjectionPointsInspection", "SpringAutowiredFieldsWarningInspection"})
@ApplicationScoped
public class ConnectionManager implements IConnectionManager
{
    private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    @Inject
    private IProgramEnvironment programEnvironment;

    @Inject
    private IProjectManager projectManager;

    @Inject
    private IConnectionPluginRegistry connectionPluginRegistry;

    @Inject
    private IConnectionPersist connectionPersist;

    @Inject
    private IPluginStatePersist pluginStatePersist;

    @Inject
    private SwingUIBinder swingUIBinder;

    @Inject
    private AutoReconnectionManager autoReconnectionManager;

    @Inject
    @UiEventBus
    private EventBus uiEventBus;

    /**
     * Tracks the number of active connections based on the Family/Type of the connection controller.  This is used to
     * indirectly ensure that the Connection Wizard only displays connections that have not exceeded their max
     * connection count.
     */
    private final Map<String, Integer> numConnectionsMap = new ConcurrentHashMap<>();

    /**
     * Connection manager state listeners include objects like the ConnectionPanel which needs to update its GUI when
     * the connection state changes.
     */
    private final List<IConnectionManagerListener> listeners = new CopyOnWriteArrayList<>();

    /**
     * Map for translating from an IConnectionPluginController.getUniqueKey() into its IComposedConnectionController.
     */
    private final Map<String, IComposedConnectionController> conxPluginControllerToController = new ConcurrentHashMap<>();

    /**
     * Map for translating from an IConnection.getUniqueKey() into its IComposedConnectionController.
     */
    private final Map<String, IComposedConnectionController> conxToController = new ConcurrentHashMap<>();

    /**
     * Map the currently open connection dialogs that are being edited.  The Connection Wizard is modal; these are
     * the dialogs that the user accessed by double-clicking on a connection entry in the Connections Panel.
     */
    private final Map<String, EditConnectionDialog> conxDialogsMap = new HashMap<>();

    /**
     * Map from connection id to physical connections.
     *
     * @since 1.14
     */
    private final Map<String, Connection> activeConnections = new HashMap<>();

    /**
     * Map from proxy button event ids to event.
     */
    private final Map<String, ProxyButtonEvent> proxyConnections = new HashMap<>();

    ConnectionManager()
    {
    }

    @Override
    public Collection<IComposedConnectionController> getConnectionControllers()
    {
        return conxPluginControllerToController.values();
    }

    @Override
    public Collection<IConnectionPluginController> getConnectionPluginControllers()
    {
        return getConnectionControllers().stream().map(IComposedConnectionController::getPluginConnectionController).collect(Collectors.toList());
    }

    @Override
    public String getDisplayName(IConnectionPluginController controller)
    {
        IComposedConnectionController composedController = conxPluginControllerToController.get(controller.getUniqueKey());
        return composedController != null ? composedController.getConnectionDisplayName() : null;
    }

    @Override
    public void addConnectionManagerListener(@Nonnull IConnectionManagerListener listener)
    {
        if (logger.isTraceEnabled())
        {
            logger.trace("Added client connection listener");
        }

        listeners.add(listener);

        // Update the listener with the current state from all registered connection controllers
        for (IComposedConnectionController connectionController : conxPluginControllerToController.values())
        {
            IRaptorObject clonedRO = RaptorObjectFactory.clone(connectionController);

            listener.onConnectionAdded(clonedRO, connectionController.getConnectionDisplayName(), connectionController.getState());

            connectionController.addConnectionStateListener(listener);
        }
    }

    @Override
    public void removeConnectionManagerListener(@Nonnull IConnectionManagerListener listener)
    {
        listeners.remove(listener);

        for (IComposedConnectionController controller : conxPluginControllerToController.values())
        {
            controller.removeConnectionStateListener(listener);
        }
    }

    @Override
    public boolean isManagedConnection(String controllerKey)
    {
        return activeConnections.containsKey(controllerKey);
    }

    @Override
    public IComposedConnectionController createConnectionController(String connectionFamily, String connectionType) throws NoSuchPluginException, TooManyConnectionsException
    {
        IComposedConnectionController controller;

        if (connectionPluginRegistry.exists(connectionFamily, connectionType))
        {
            IConnectionPlugin plugin = connectionPluginRegistry.getPlugin(connectionFamily, connectionType);
            int maxConnections = plugin.getMaxSupportedConnections();
            int currentConnections = getConnectionCount(connectionFamily, connectionType);
            if (currentConnections >= maxConnections)
            {
                throw new TooManyConnectionsException(connectionFamily, connectionType, "Max number of connections reached", maxConnections);
            }

            IConnectionPluginController pluginController = plugin.createConnectionController();

            controller = new ComposedConnectionController(pluginController, plugin.getName());
            conxPluginControllerToController.put(pluginController.getUniqueKey(), controller);

            pluginController.setDelegate(controller);

            if (logger.isDebugEnabled())
            {
                logger.debug("Connection Plugin Controller Key: {}", pluginController.getUniqueKey());
            }

            // Increment the number of connections for this family/type.  Each connection plugin controller
            // defines a maximum number of permitted connections, and this is the logic to track that.
            // This count has nothing to do with the specific controller.
            incrementCount(controller.getFamily(), controller.getType());
        } else
        {
            logger.error("A connection of type [{}/{}] that existed during bootstrapping is now missing", connectionFamily, connectionType);
            throw new NoSuchPluginException(PluginCategory.ConnectionPlugin, connectionFamily, connectionType);
        }

        return controller;
    }

    /**
     * {@inheritDoc}
     * <p>
     * Adds the new connection in a state of "Disconnected", and makes sends the notification that
     * a connection should be initiated).  Generally the result of an operator filling out the connection
     * parameters in a UI and clicking "Connect".
     */
    @Override
    public void addActiveConnection(@Nonnull IComposedConnectionController controller)
    {
        IRaptorObject clonedRO = RaptorObjectFactory.clone(controller);

        for (IConnectionManagerListener listener : listeners)
        {
            controller.addConnectionStateListener(listener);
            listener.onConnectionAdded(clonedRO, controller.getConnectionDisplayName(), ConnectionState.Disconnected);
        }

        openConnection(controller);

        // Persist the connection and add it to the list of active connections.
        persistConnection(controller);
    }

    @Override
    public void addPreconfiguredConnection(@Nonnull IComposedConnectionController controller, Map<String, Object> configuration)
    {
        IRaptorObject clonedRO = RaptorObjectFactory.clone(controller);

        for (IConnectionManagerListener listener : listeners)
        {
            controller.addConnectionStateListener(listener);
            listener.onConnectionAdded(clonedRO, controller.getConnectionDisplayName(), ConnectionState.Disconnected);
        }

        // Install the configuration and open the connection
        controller.getPluginConnectionController().onOpenConnection(configuration, true);

        // Persist the connection and add it to the list of active connections.
        persistConnection(controller);
    }

    /**
     * {@inheritDoc}
     * <p>
     * Basically a touch point for creating a connection with a list of configuration parameters, perhaps for
     * doing something like an auto-connect on startup.
     */
    @Override
    public void createAndAddConfiguredConnection(String family, String type, Map<String, Object> configuration, boolean autoStart) throws TooManyConnectionsException
    {
        IComposedConnectionController controller = createConnectionController(family, type);
        openConfiguredConnection(controller, configuration, autoStart);

        // Persist the connection and add it to the list of active connections.
        persistConnection(controller);
    }

    @Override
    public void openConfiguredConnection(String uniqueKey, Map<String, Object> configuration, boolean autoStart) throws NoSuchConnectionControllerException
    {
        IComposedConnectionController controller = getControllerFromConnectionKey(uniqueKey);
        openConfiguredConnection(controller, configuration, autoStart);
    }

    @Override
    public void updateConfiguredConnection(String uniqueKey, Map<String, Object> configuration) throws NoSuchConnectionControllerException
    {
        IComposedConnectionController controller = getControllerFromConnectionKey(uniqueKey);

        IConnectionPluginController baseConnectionPluginController = controller.getPluginConnectionController();
        baseConnectionPluginController.onUpdateConfiguredConnection(configuration);
    }

    /**
     * Performs the common logic necessary for opening a configured connection.
     *
     * @param controller    The composed connection controller that represents the connection.
     * @param configuration Map of key/value configuration parameters used to initialize the connection before
     *                      it attempts to actually create the connection
     * @param autoStart     whether to auto-start this connection when it is added.
     * @since 2.18
     */
    private void openConfiguredConnection(IComposedConnectionController controller, Map<String, Object> configuration,
                                          boolean autoStart)
    {
        IRaptorObject clonedRO = RaptorObjectFactory.clone(controller);

        for (IConnectionManagerListener listener : listeners)
        {
            controller.addConnectionStateListener(listener);
            listener.onConnectionAdded(clonedRO, controller.getConnectionDisplayName(), ConnectionState.Disconnected);
        }

        IConnectionPluginController baseConnectionPluginController = controller.getPluginConnectionController();
        baseConnectionPluginController.onOpenConnection(configuration, autoStart);

        // Restore the UI for the UI consumer, if implemented.
        if (baseConnectionPluginController instanceof IConnectionSwingUIConsumer uiConsumer)
        {
            SwingUtilities.invokeLater(() -> uiConsumer.setConnectionPanel(controller.getConnectionPanel()));
        }
    }

    @Override
    @Nonnull
    public IComposedConnectionController getControllerFromConnectionKey(String uniqueKey) throws NoSuchConnectionControllerException
    {
        IComposedConnectionController controller = conxToController.get(uniqueKey);
        if (controller == null)
        {
            controller = conxPluginControllerToController.get(uniqueKey);
        }

        if (controller == null)
        {
            throw new NoSuchConnectionControllerException(uniqueKey);
        }

        return controller;
    }

    /**
     * Check to see if a key exists for a known or proxy connection
     *
     * @param key key entry to check
     * @return true if connection exists
     */
    @Override
    public boolean exists(String key)
    {
        return proxyConnections.containsKey(key) || conxPluginControllerToController.containsKey(key);
    }

    /**
     * Notify listeners that a proxy connection has been added / updated
     *
     * @param id              id/key of proxy connection
     * @param connectionState status of proxy connection
     */
    @Override
    public void displayProxyConnection(@Nonnull String id, @Nonnull ConnectionState connectionState)
    {
        getProxyConnections().stream()
                .filter(pbe -> id.equals(pbe.getId()))
                .forEach(pbe -> {
                    for (IConnectionManagerListener listener : listeners)
                    {
                        listener.onConnectionAdded(pbe.getId(), pbe.getName(), pbe.getIconPath(), connectionState, pbe.getAction());
                    }
                });
    }

    /**
     * Notify listeners that a proxy connection has been removed.
     *
     * @param id id/key of proxy connection
     */
    @Override
    public void removeProxyConnection(String id)
    {
        for (IConnectionManagerListener listener : listeners)
        {
            listener.onConnectionRemoved(id);
        }
    }

    @Override
    public Collection<ProxyButtonEvent> getProxyConnections()
    {
        return proxyConnections.values();
    }

    @SuppressWarnings("UnusedDeclaration")
    @Subscribe
    public void onProxyConnectionEvent(ProxyButtonEvent event)
    {
        if (event.isAddButton())
        {
            proxyConnections.put(event.getId(), event);
        } else
        {
            proxyConnections.remove(event.getId());
        }
    }

    @Override
    public int getConnectionCount(String connectionFamily, String connectionType)
    {
        String index = KeyUtils.getUniqueKey(connectionFamily, connectionType);
        Integer count = numConnectionsMap.get(index);
        return count == null ? 0 : count;
    }

    /**
     * Display editor dialog for a connection.  Normally will be looked up by the connection's plugin controller,
     * except in the case of a proxy connection which will just fire the action event registered by the proxy
     * connection
     *
     * @param key connection key
     */
    @Override
    public void displayEditDialog(String key)
    {
        if (logger.isTraceEnabled())
        {
            logger.trace("enableEditMode: Looking for connection dialog for key = {}", key);
        }

        // If this is a proxy connection, just fire an event on the action that was registered for it
        for (ProxyButtonEvent event : getProxyConnections())
        {
            if (key.equals(event.getId()))
            {
                event.getAction().actionPerformed(new ActionEvent(this, 0, "Edit"));
                return;
            }
        }

        IComposedConnectionController controller = conxPluginControllerToController.get(key);

        if (null != controller)
        {
            boolean supportsEdit = false;
            Optional<IConnectionPluginDefinition> def = connectionPluginRegistry.getPluginDefinition(controller.getFamily(), controller.getType());
            if (def.isPresent())
            {
                supportsEdit = def.get().isEditSupported();
            }
            if (supportsEdit)
            {
                controller.getPluginConnectionController().onEditConnection();
            }

            // Before trying to create the edit dialog, make sure a Swing UI provider is present so the user can be
            // provided with an informative error message
            boolean uiProviderPresent = swingUIBinder.isUIProviderPresent(controller.getPluginConnectionController().getClass());

            if (uiProviderPresent)
            {
                // Now, physically display the dialog if it isn't already visible
                EditConnectionDialog ecDialog = conxDialogsMap.computeIfAbsent(key, k -> new EditConnectionDialog(controller));

                ecDialog.setVisible(true);
            } else
            {
                swingUIBinder.showNoSwingUIDialog(controller.getType() + " connections");
            }
        } else
        {
            logger.warn("Unable to find a connection controller with key {}: unable to edit connection dialog", key);
        }
    }

    @Override
    public void editDialogClosed(String uniqueKey)
    {
        conxDialogsMap.remove(uniqueKey);
    }

    /**
     * Remove a connection from the list.  In the case of a proxy connection, just fire an event on the action
     * that was registered for the proxy connection.  Otherwise, provide the proper notification to let the connection
     * classes know that the connection has been deleted.
     * <p>
     * Note:  Key will be the same if it comes from {@link IConnectionPluginController} or from
     * {@link IComposedConnectionController}.
     *
     * @param key                connection key
     * @param isActiveConnection boolean indicating if the connection being removed is an active connection
     * @see ComposedConnectionController#getUniqueKey()
     */
    @Override
    public void removeConnection(@Nonnull String key, boolean isActiveConnection)
    {
        if (logger.isDebugEnabled()) logger.debug("Remove Connection {}, isActive = {}", key, isActiveConnection);

        // Check to see if this is a proxy connection, and if so, fire an event on the Action and notify the
        // listeners.
        for (ProxyButtonEvent event : getProxyConnections())
        {
            if (key.equals(event.getId()))
            {
                // TODO:  Some static string to replace "Removed"
                event.getAction().actionPerformed(new ActionEvent(this, 0, "Removed"));
                for (IConnectionManagerListener listener : listeners)
                {
                    listener.onConnectionRemoved(key);
                }
                return;
            }
        }

        IComposedConnectionController controller = conxPluginControllerToController.remove(key);

        // Step 1.  Close the Edit Connection Dialog (if open)
        // Step 2.  Shutdown the connection controller, purging the IConnectionPluginController and IConnection
        // Step 3.  Decrement the num connections count on the connection Family/Type (think IConnectionPlugin.getMaxSupportedConnections() in API)
        // Step 4.  Notify all connection manager listeners that the connection has been removed

        if (null != controller)
        {
            // Step 1 - Dispose of an existing edit dialog
            EditConnectionDialog ecDialog = conxDialogsMap.get(key);
            if (ecDialog != null)
            {
                ecDialog.setVisible(false);
                ecDialog.shutdown();
            }

            // Step 2 - Call shutdown on the controllers
            controller.shutdown();

            // Step 3 - Decrement the count
            decrementCount(controller.getFamily(), controller.getType());

            // Step 4 - Fire off the connection removed event for listeners (ConnectionPanel for one)
            for (IConnectionManagerListener listener : listeners)
            {
                IRaptorObject clonedRO = RaptorObjectFactory.clone(controller);
                listener.onConnectionRemoved(clonedRO);
            }

            String selectedKey = null;

            if (isActiveConnection)
            {
                try
                {
                    selectedKey = controller.getUniqueKey();
                    Connection connection = activeConnections.get(selectedKey);
                    if (null != connection.getId())
                    {
                        connectionPersist.deleteConnection(connection);
                        if (logger.isDebugEnabled()) logger.debug("Deleting connection for {}", controller.getType());
                    }
                } catch (NullPointerException e)
                {
                    logger.error("Connection not found for key '{}' in map.  Connection not removed.  Does the plugin change its name improperly?", selectedKey);
                    AlwaysOnTopJOptionPane.showErrorDialogLater("Unable to find a connection with the name '" + selectedKey + "' in the registry.", "Connection Panel");
                }

                activeConnections.remove(controller.getUniqueKey());
            }
        } else
        {
            logger.warn("No connection controller associated with key {}: removal silently failed", key);
        }

        conxDialogsMap.remove(key);
    }

    /**
     * This is basically the initiation of a connection from the plugin's point of view.
     *
     * @param connectionController the "composed" connection controller.
     */
    @Override
    public void openConnection(@Nonnull IComposedConnectionController connectionController)
    {
        IConnectionPluginController pluginConnectionController = connectionController.getPluginConnectionController();

        if (pluginConnectionController instanceof IDelegatedConnectionAuthentication)
        {
            AuthenticationDialog dialog = new AuthenticationDialog((IDelegatedConnectionAuthentication) pluginConnectionController);

            dialog.setVisible(true);
            if (!dialog.isOkay())
            {
                return;
            }
        }

        connectionController.getPluginConnectionController().onNewConnection();
    }

    @Override
    @InvokeOnEDT
    public void loadConnectionTemplate(@Nonnull File template)
    {
        try
        {
            ConnectionDefinitionsType connections = ConnectionTemplateManager.loadConnectionTemplate(template.toURI().toURL());

            ImportConnectionTemplateDialog dialog = new ImportConnectionTemplateDialog(connections);
            dialog.setVisible(true);
            if (dialog.isOkay())
            {
                ConnectionDefinitionsType selectedConnections = dialog.getDefinitions();
                if (!selectedConnections.getConnectionDefinition().isEmpty())
                {
                    importConnections(selectedConnections);
                } else
                {
                    AlwaysOnTopJOptionPane.showMessageDialog(JFrame.getFrames()[0], "No Connections Selected From Template", "Connection Template", JOptionPane.WARNING_MESSAGE);
                }
            }
        } catch (IOException | JAXBException e)
        {
            logger.error("Error opening connection template {}", template.getAbsolutePath(), e);
            AlwaysOnTopJOptionPane.showErrorDialog("Error opening connection template " + template.getAbsolutePath(), "File Error");
        }
    }

    /**
     * Crank up our service on the project.  Load our state from the DB.
     */
    // TODO:  This method is in need of re-factoring to separate the persistence logic from the auto-start logic
    private void startService(@Observes @Priority(ServiceOrdering.START_ConnectionManager) StartProjectServices ignored)
    {
        uiEventBus.register(this);
    }

    private void startConnections(@Observes @Priority(ServiceOrdering.START_PLUGINS_ConnectionManager) StartPlugins ignored)
    {
        final IProject project = projectManager.getCurrentOpenProject();
        if (project.isNew())
        {
            final File template = new File(programEnvironment.getProjectDataDirectory(), "ConnectionTemplate.xml");

            if (template.exists()) loadConnectionTemplate(template);
        } else
        {
            restoreConnections();
            executeAutoReconnect();
        }
    }

    /**
     * Stop our service.
     */
    private void stopService(@Observes @Priority(ServiceOrdering.STOP_ConnectionManager) StopProjectServices ignored)
    {
        if (uiEventBus.isRegistered(this)) uiEventBus.unregister(this);

        if (logger.isDebugEnabled()) logger.debug("Clear State");

        for (IComposedConnectionController connectionController : conxPluginControllerToController.values())
        {
            IConnectionPluginController pluginController = connectionController.getPluginConnectionController();
            if (null != pluginController)
            {
                // Setting isActiveConnection to false here (even though connection is really active) to prevent
                // deletion from the database. (This parameter is only used for deciding whether to remove the
                // connection from the database.)
                removeConnection(pluginController.getUniqueKey(), false);
            }
        }

        // These should be clear, but we'll make sure!
        conxPluginControllerToController.clear();
        conxToController.clear();

        if (!numConnectionsMap.isEmpty())
        {
            logger.error("Invalid State Detected in Connection Manager - numConnectionsMap should be empty!  Enable DEBUG logging for more information");

            if (logger.isDebugEnabled()) logger.debug("numConnectionsMap still contains: {}", numConnectionsMap);
        }
        activeConnections.clear();
        numConnectionsMap.clear();
        proxyConnections.clear();
    }

    private void onProjectSaving(@SuppressWarnings("UnusedParameters") @Observes ProjectSaving event)
    {
        conxPluginControllerToController.values().forEach(IComposedConnectionController::onProjectSaving);
    }

    private void persistConnection(IComposedConnectionController controller)
    {
        Connection conx = new Connection(
                controller.getFamily(),
                controller.getType(),
                controller.getUniqueKey(),
                controller.getConnectionDisplayName());

        connectionPersist.persistConnection(conx);
        activeConnections.put(controller.getUniqueKey(), conx);
    }

    private int decrementCount(String connectionFamily, String connectionType)
    {
        int finalCount = -1;

        String index = KeyUtils.getUniqueKey(connectionFamily, connectionType);
        Integer count = numConnectionsMap.get(index);

        if (null == count)
        {
            logger.error("Illegal state detected in ConnectionManager- attempt to decrement the count of a connection controller that has no existing count: {}/{}", connectionFamily, connectionType);
        } else
        {
            count--;
            finalCount = count;

            if (0 == count)
            {
                numConnectionsMap.remove(index);
            } else
            {
                numConnectionsMap.put(index, count);
            }
        }

        return finalCount;
    }

    private int incrementCount(String connectionFamily, String connectionType)
    {
        String index = KeyUtils.getUniqueKey(connectionFamily, connectionType);
        Integer count = numConnectionsMap.get(index);

        if (null == count)
        {
            count = 1;
        } else
        {
            count++;
        }

        numConnectionsMap.put(index, count);

        return count;
    }

    /**
     * Restore the connections. Gets the persistable connection objects from the DB and turns them into TAKX connections.
     */
    private void restoreConnections()
    {
        // On loading the state of the connections panel, query the database to see what connections existed
        // at the time the project closed.  By default, we repopulate this list only, we don't auto-reconnect.
        // In order to auto-reconnect, the ProgramProperties.xml must be configured properly.  See the comments
        // in DefaultProgramProperties.xml for further explanation

        List<Connection> connections = connectionPersist.queryAll();

        if (logger.isDebugEnabled()) logger.debug("Found {} persisted connections", connections.size());

        if (!connections.isEmpty())
        {
            for (Connection conx : connections)
            {
                try
                {
                    IComposedConnectionController conxController = createConnectionController(conx.getFamily(), conx.getType());
                    IConnectionPluginController baseConnectionPluginController = conxController.getPluginConnectionController();

                    String uniqueKey = conx.getUniqueKey();
                    IPluginState dbState = pluginStatePersist.query(uniqueKey);

                    if (null != dbState)
                    {
                        baseConnectionPluginController.restore(dbState.getState());

                        // Delete the old persisted state, which is associated with the unique key of the original
                        // connection.  That connection is being replaced with a newly created connection (with a new
                        // unique key) so there is no reason to keep the old state.
                        pluginStatePersist.deleteState(dbState);
                    }

                    // Restore the UI for the UI consumer, if implemented.
                    if (baseConnectionPluginController instanceof IConnectionSwingUIConsumer uiConsumer)
                    {
                        SwingUtilities.invokeLater(() -> uiConsumer.setConnectionPanel(conxController.getConnectionPanel()));
                    }

                    listeners.forEach(conxController::addConnectionStateListener);

                    // Remove the old connection from the DB and persist the new connection which has replaced it
                    persistConnection(conxController);
                    connectionPersist.deleteConnection(conx);
                } catch (TooManyConnectionsException e)
                {
                    logger.error("Error restoring connection of family: {}, type: {}, name: {}.  Max number of connections exceeded.  Extra connection will be removed from the database.", conx.getFamily(), conx.getType(), conx.getName());
                    connectionPersist.deleteConnection(conx);
                } catch (NoSuchPluginException e)
                {
                    logger.error("Can't find plugin for connection family/type [{}/{}], named '{}' to do a " +
                            "restore from the project.  Unknown connection will be removed from the database.",
                            conx.getFamily(), conx.getType(), conx.getName());
                    connectionPersist.deleteConnection(conx);
                }
            }
        }
    }

    /**
     * Execute any auto connection code (should be called on startup of the project).
     * Support for auto-connection is specified on the connection plugin class (in an annotation).
     *
     * @see IConnectionPluginDefinition#isAutoReconnectSupported()
     */
    private void executeAutoReconnect()
    {
        for (IComposedConnectionController conxController : getConnectionControllers())
        {
            String connectionKey = KeyUtils.getUniqueKey(conxController.getFamily(), conxController.getType());

            // Check with the AutoReconnectionManager to see if auto-reconnection is enabled
            if (autoReconnectionManager.isAutoReconnectionEnabled() && autoReconnectionManager.hasConfiguration(connectionKey))
            {
                logger.info("Attempting to auto-reconnect connection with key {}", connectionKey);

                boolean supportsAutoReconnect = false;

                Optional<IConnectionPluginDefinition> def = connectionPluginRegistry.getPluginDefinition(conxController.getFamily(), conxController.getType());

                if (def.isPresent())
                {
                    supportsAutoReconnect = def.get().isAutoReconnectSupported();
                }

                if (supportsAutoReconnect)
                {
                    logger.debug("Auto-reconnect supported by plugin, initiating auto-reconnect now.");

                    IConnectionPluginController controller = conxController.getPluginConnectionController();
                    controller.onAutoReconnect();
                }
            } else
            {
                if (logger.isInfoEnabled())
                {
                    logger.info("Auto-Reconnect not supported by metadata declaration in connection plugin {} / {}", conxController.getFamily(), conxController.getType());
                }
            }
        }
    }

    /**
     * Import a list of connections that were imported / selected from a connection template.
     *
     * @param connections The connections to import
     */
    private void importConnections(ConnectionDefinitionsType connections)
    {
        List<ConnectionDefinitionType> connectionList = connections.getConnectionDefinition();
        for (ConnectionDefinitionType connection : connectionList)
        {
            Map<String, Object> configMap = new HashMap<>();
            PropertiesType propertiesType = connection.getProperties();
            for (PropertyType type : propertiesType.getProperty())
            {
                configMap.put(type.getName(), type.getValue());
            }
            try
            {
                createAndAddConfiguredConnection(connection.getFamily(), connection.getType(), configMap, connection.isAutoConnect());
            } catch (TooManyConnectionsException e)
            {
                logger.error("Max number of connections already exceeded for {}, {}", connection.getFamily(), connection.getType());
            }
        }
    }
}
