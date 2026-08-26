package gov.takx.core.gateway;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.ListMultimap;
import gov.takx.api.annotation.PlatformUnderTest;
import gov.takx.api.commservices.ICommService;
import gov.takx.api.dataportal.DefaultRaptorDataListener;
import gov.takx.api.dataportal.IRaptorDataPortal;
import gov.takx.api.dataportal.IRaptorDataPortalController;
import gov.takx.api.event.PluginAdded;
import gov.takx.api.event.ShuttingDown;
import gov.takx.api.event.StartProjectServices;
import gov.takx.api.event.StopProjectServices;
import gov.takx.api.event.connection.ConnectionAdded;
import gov.takx.api.event.connection.ConnectionRemoved;
import gov.takx.api.gateway.IDeviceGateway;
import gov.takx.api.mapobject.IMapObject;
import gov.takx.api.mapobject.IMapObjectManager;
import gov.takx.api.mapobject.MapObjectSpec;
import gov.takx.api.messages.IMapEntity;
import gov.takx.api.messages.IMapObjectOfflineMessage;
import gov.takx.api.messages.IRaptorDataStructure;
import gov.takx.api.messages.IStructure;
import gov.takx.api.persist.IMapObjectPersist;
import gov.takx.api.platform.IParser;
import gov.takx.api.platform.IProgressMonitor;
import gov.takx.api.platform.NameFixingRunnable;
import gov.takx.api.plugin.PluginCategory;
import gov.takx.api.plugin.PluginConstants;
import gov.takx.api.plugin.connection.ICommServiceDelegate;
import gov.takx.api.plugin.device.IDataStructureFactory;
import gov.takx.api.plugin.device.IDevicePlugin;
import gov.takx.api.plugin.device.IDevicePluginDefinition;
import gov.takx.api.plugin.gateway.GenericGatewayController;
import gov.takx.api.plugin.gateway.IGatewayController;
import gov.takx.api.plugin.gateway.IGatewayControllerDelegate;
import gov.takx.api.plugin.gateway.IGatewayFramer;
import gov.takx.api.plugin.gateway.IGatewayFramerDelegate;
import gov.takx.api.plugin.gateway.IGatewayParser;
import gov.takx.api.plugin.gateway.IGatewayParserDelegate;
import gov.takx.api.pointdropper.CotIcon;
import gov.takx.api.registry.IDevicePluginRegistry;
import gov.takx.commons.constants.CommServiceConstants;
import gov.takx.commons.constants.CommServiceType;
import gov.takx.core.connection.SimulatorConnectionManager;
import gov.takx.core.project.ServiceOrdering;
import gov.takx.dataservices.persist.RaptorDataStructure;
import gov.takx.platform.KeyUtils;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import jakarta.annotation.Priority;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import jakarta.inject.Inject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.invoke.MethodHandles;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * Main Device Gateway class, responsible for managing delegates, wiring parsers/framers to comm ports
 * and creating controllers and delegates.
 *
 */
@ApplicationScoped
public class DeviceGateway implements IDeviceGateway
{
    private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    private final Map<String, List<IRaptorDataStructure>> dataStructures = new ConcurrentHashMap<>();

    private final ListMultimap<CommServiceKey, IGatewayParser> parserMap = ArrayListMultimap.create();
    private final ListMultimap<CommServiceKey, IGatewayFramer> framerMap = ArrayListMultimap.create();
    private final Map<String, IGatewayController> controllerMap = new ConcurrentHashMap<>();
    private final Map<String, GatewayControllerDelegate> controllerDelegateMap = new ConcurrentHashMap<>();
    private final Set<String> emittedMissingPluginWarnings = new HashSet<>();

    private final SimulatorConnectionManager simulatorConnectionManager;
    private final boolean platformUnderTest;
    private final IDevicePluginRegistry devicePluginRegistry;
    private final IMapObjectPersist mapObjectPersist;
    private final IRaptorDataPortal dataPortal;
    private final IMapObjectManager mapObjectManager;
    private final ICommServiceDelegate commServiceDelegate;

    @Inject
    public DeviceGateway(@PlatformUnderTest boolean platformUnderTest, IDevicePluginRegistry devicePluginRegistry,
                         IMapObjectPersist mapObjectPersist, IRaptorDataPortal dataPortal,
                         IMapObjectManager mapObjectManager, SimulatorConnectionManager simulatorConnectionManager,
                         ICommServiceDelegate commServiceDelegate)
    {
        this.platformUnderTest = platformUnderTest;
        this.devicePluginRegistry = devicePluginRegistry;
        this.mapObjectPersist = mapObjectPersist;
        this.dataPortal = dataPortal;
        this.mapObjectManager = mapObjectManager;
        this.simulatorConnectionManager = simulatorConnectionManager;
        this.commServiceDelegate = commServiceDelegate;
    }

    @Override
    public Set<String> getParserKeys()
    {
        return parserMap.entries().stream()
                .map(e -> String.format("%s/%s/%s", e.getKey().sourceType, e.getKey().sourceName, e.getValue().getDelegate().getDeviceUniqueKey()))
                .collect(Collectors.toSet());
    }

    @Override
    public Set<String> getFramerKeys()
    {
        return framerMap.entries().stream()
                .map(e -> String.format("%s/%s/%s", e.getKey().sourceType, e.getKey().sourceName, e.getValue().getDelegate().getDeviceUniqueKey()))
                .collect(Collectors.toSet());
    }

    private void registerDataStructures(IDevicePlugin plugin)
    {
        final String deviceType = plugin.getUniqueKey();
        if (logger.isDebugEnabled()) logger.debug("Registering DataStructures for plugin {}", deviceType);

        final IDataStructureFactory factory = plugin.getDataStructureFactory();
        if (null != factory)
        {
            factory.resetStructures();

            final List<IRaptorDataStructure> pluginDataStructures = factory.getPersistableDataStructures();

            if (!dataStructures.containsKey(deviceType) && null != pluginDataStructures)
            {
                dataStructures.put(deviceType, pluginDataStructures);

                if (logger.isDebugEnabled()) logger.debug("DS registered: {} {}", deviceType, plugin.getVersion());

                IMapEntity mapEntity = createDefinitionMessage(plugin.getFamily(), plugin.getType(), pluginDataStructures);

                // Add ME to each RCM
                final List<IRaptorDataStructure> commandStructures = factory.getCommandDataStructures();
                if (commandStructures != null)
                {
                    commandStructures.forEach(rds -> rds.getStructure().addDefinition(mapEntity));
                }

                dataPortal.onMapObjectDefinition(mapEntity);

                // Since we're running on an arbitrary background thread, cleanup
                mapObjectPersist.getPersistService().cleanupAndClose();
            }
        } else
        {
            logger.warn("Plugin {} (Version: {}) did not define an IDataStructureFactory implementation.", plugin.getType(), plugin.getVersion());
        }
    }

    /**
     * Creates a MapEntity from a given list of {@link IRaptorDataStructure}s.
     *
     * @param family      String family name (namespace)
     * @param type        map object type
     * @param dataStructs List of data structures
     * @return MapObjectDefinitionMessage
     */
    private IMapEntity createDefinitionMessage(String family, String type, List<IRaptorDataStructure> dataStructs)
    {
        if (logger.isDebugEnabled()) logger.debug("Creating MapEntity for {}", type);

        final IMapEntity mapEntity = mapObjectPersist.getOrCreateMapEntity(family, type);

        final List<IStructure> structs = RaptorDataStructure.convertToStructureCollection(dataStructs);

        mapEntity.addStructures(structs);

        return mapEntity;
    }

    /**
     * Start servicing this project.
     */
    private void startService(@Observes @Priority(ServiceOrdering.START_DeviceGateway) StartProjectServices event)
    {
        final IProgressMonitor progressMonitor = event.getProgressMonitor();

        simulatorConnectionManager.startListening();

        logger.info("Loading plugins");
        progressMonitor.setText("Loading Device plugins...");

        final var devicePlugins = devicePluginRegistry.getPlugins();
        final int pluginCount = devicePlugins.size();
        progressMonitor.setSubstatusMaximum(pluginCount);
        progressMonitor.setSubstatus("Registering structures for plugins");

        final long start = System.currentTimeMillis();

        // Launch all the plugin structure discovery in parallel
        //
        // Note: We can't use our normal executor service here due to some oddity in the ForkJoinTask
        // internals.  If we use our executor, and have more tasks than threads in the service's pool,
        // we'll lock up because completed tasks are not properly releasing their thread.  So, we use
        // the fork/join pool (the default) and use a NameFixingRunnable to keep thread names tidy.

        final var futureList = devicePlugins.stream()
                .map(plugin -> submitStructureLoad(plugin, progressMonitor))
                .toList();

        try
        {
            // Wait for them all to complete
            CompletableFuture.allOf(futureList.toArray(new CompletableFuture[0])).join();
        } catch (Exception e)
        {
            logger.error("Structure discovery failed", e);
        }

        if (logger.isDebugEnabled()) logger.debug("All registered in {} ms", System.currentTimeMillis() - start);

        logger.info("Starting Device Gateway");
        progressMonitor.setText("Starting Device Gateway");

        // Elevate the priority of the device gateway.  It is essential that the Device Gateway make the
        // connection between its controller delegate (IGatewayControllerDelegate) and the IDataPortalController
        // when a map object is stood up before anything else.  Which doesn't quite happen in the correct order
        // when you restore an old project, i.e. the MapObjectManager goes first. CORE-4024
        dataPortal.addListener(new DefaultRaptorDataListener()
        {
            @Override
            public void onMapObjectAdded(IMapObject mapObject, IRaptorDataPortalController dpc)
            {
                createController(mapObject.getKey(), false);
            }

            @Override
            public void onMapObjectOffline(IMapObjectOfflineMessage msg, IRaptorDataPortalController controller, boolean wipeHistoricalData)
            {
                destroyController(msg.getFamily(), msg.getType(), msg.getUnitID());
            }
        }, IRaptorDataPortalController.MEDIUM_PRIORITY + 1);
    }

    /**
     * @return Future for an async task that registers the data structures for a given device plugin
     * @since 5.7
     */
    @Nonnull
    private CompletableFuture<Void> submitStructureLoad(@Nonnull IDevicePlugin plugin, @Nullable IProgressMonitor progressMonitor)
    {
        return CompletableFuture.runAsync(
                new NameFixingRunnable("Load Structures for: " + plugin.getUniqueKey())
                {
                    @Override
                    protected void doRun()
                    {
                        registerDataStructures(plugin);
                        if (progressMonitor != null) progressMonitor.incrementSubstatus();
                    }
                });
    }

    /**
     * Register the data structures for any device plugins added.
     * @since 5.7
     */
    private void onPluginAdded(@Observes PluginAdded pluginAdded)
    {
        if (pluginAdded.getPluginCategory() == PluginCategory.DevicePlugin)
        {
            logger.debug("onPluginAdded: {}, registering data structures", pluginAdded);

            final var plugin = (IDevicePlugin) pluginAdded.pluginDefinition().getInstance();
            final var future = submitStructureLoad(plugin, null);

            // When under test, we need to wait for this operation to finish
            if (platformUnderTest) future.join();
        }
    }

    /**
     * Stop servicing this project.
     */
    private void stopService(@Observes @Priority(ServiceOrdering.STOP_DeviceGateway) StopProjectServices event)
    {
        final IProgressMonitor progressMonitor = event.getProgressMonitor();

        progressMonitor.setMaximum(IProgressMonitor.INDETERMINATE);
        progressMonitor.setText("Stopping Device Gateway");

        stopDeviceGateway();
    }

    private void shutdown(@SuppressWarnings("UnusedParameters") @Observes ShuttingDown event)
    {
        stopDeviceGateway();

        parserMap.values().forEach(IParser::shutdown);

        parserMap.clear();
        framerMap.clear();

        dataStructures.clear();
        controllerMap.clear();
        controllerDelegateMap.clear();
    }

    @Override
    public IGatewayController getController(String family, String type, String unitId)
    {
        return createController(KeyUtils.getUniqueKey(family, type, unitId), false);
    }

    @Override
    public IGatewayController getController(String uniqueKey)
    {
        return controllerMap.get(uniqueKey); // It must already exist, we will not create it
    }

    @Override
    public IGatewayController getController(String uniqueKey, boolean createIfNotFound)
    {
        return createController(uniqueKey, createIfNotFound);
    }

    @Override
    public ICommServiceDelegate getCommServiceDelegate()
    {
        return commServiceDelegate;
    }

    @Override
    public IGatewayFramer getFramer(CommServiceType sourceType, String sourceName, String family, String type)
    {
        final String key = KeyUtils.getUniqueKey(family, type);

        return framerMap.get(new CommServiceKey(sourceType, sourceName)).stream()
                .filter(f -> f.getDelegate().getDeviceUniqueKey().equals(key))
                .findFirst()
                .orElse(null);
    }

    @Override
    public Map<CommServiceType, Collection<String>> getFramedCommPaths(String family, String type)
    {
        final String key = KeyUtils.getUniqueKey(family, type);
        final ListMultimap<CommServiceType, String> commPathNameMap = ArrayListMultimap.create();

        framerMap.entries().stream()
                .filter(e -> e.getValue().getDelegate().getDeviceUniqueKey().equals(key)) // Filter on device's unique key
                .forEach(e -> commPathNameMap.put(e.getKey().sourceType, e.getKey().sourceName));

        return commPathNameMap.asMap();
    }

    /**
     * Notification that a connection has been added.  We know that the ICommManager can be cast to an ICommService,
     * which we do to set the parsers for the comm service.
     *
     * @param event connection added event
     * @since 2.0.0
     */
    private void connectionAdded(@Observes ConnectionAdded event)
    {
        ICommService commService = (ICommService) event.getCommManager();
        if (logger.isDebugEnabled())
        {
            logger.debug("Comm service added: {}", commService);
        }
        if (commService.isCommOpen())
        {
            createParsersAndFramers(commService);
            commService.setParsers(getParsers(commService.getType(), commService.getName()));
        }
    }

    /**
     * Notification that a connection has been removed.  We know that the ICommManager can be cast to an ICommService,
     * which we do to get the type and name of connection for removing parsers and framers.
     *
     * @param event the connection removed event
     * @since 2.0.0
     */
    private void connectionRemoved(@Observes ConnectionRemoved event)
    {
        ICommService commService = (ICommService) event.getCommManager();
        if (commService != null)
        {
            if (logger.isDebugEnabled())
            {
                logger.debug("Comm service removed: {}", commService);
            }
            destroyParsersAndFramers(commService.getType(), commService.getName());
        } else
        {
            logger.warn("Cannot remove commService: null");
        }
    }

    /**
     * Called when a comm service is added so that all the parsers can be attached to it.  Comm service
     * will then feed incoming data to the parsers.
     *
     * @param sourceType String port type "ETHERNET", "SERIAL"
     * @param sourceName name of port "COM1", "127.0.0.1:1024"
     * @return list of gateway parsers that support the port type and name.
     */
    private List<IGatewayParser> getParsers(CommServiceType sourceType, String sourceName)
    {
        final CommServiceKey commServiceKey = new CommServiceKey(sourceType, sourceName);
        if (logger.isDebugEnabled()) logger.debug("Getting parsers to attach to comm port:  {}", commServiceKey);

        return parserMap.get(commServiceKey);
    }

    // NOTE:  This doesn't appear to need synchronization with the destroyController and createController methods
    // since it appears that it will always be referring to a different map object than what you would
    // be working on with either of those 2 methods.  However, it does cause a reach back into the
    // controllerDelegateMap, which should be synced as a ConcurrentHashMap.
    @Override
    public void putDeviceOnline(String family, String deviceType, String unitID)
    {
        // Notify core of new controller creation

        if (logger.isDebugEnabled()) logger.debug("Creating MO for {} ID {}", deviceType, unitID);

        final IMapEntity mapEntity = mapObjectPersist.findMapEntity(family, deviceType);

        if (mapEntity == null)
        {
            logger.error("Missing MapEntity: {}", Arrays.asList(family, deviceType), new RuntimeException());
            return;
        }

        mapObjectManager.getOrCreateMapObject(MapObjectSpec.builder(mapEntity, unitID).build()); // Create the new MO
    }

    /**
     * Called by the messenger when a device goes offline and is deleted.  Remove the GatewayController and
     * GatewayControllerDelegate so they can be garbage collected
     *
     * @param family     String family name (namespace)
     * @param deviceType String description of the device (ie "Bloodhound")
     * @param unitID     String unitID of the device instance (ie "3")
     */
    private void destroyController(String family, String deviceType, String unitID)
    {
        String expectedControllerKey = KeyUtils.getUniqueKey(family, deviceType, unitID);
        IGatewayController controller = controllerMap.remove(expectedControllerKey);

        if (controller != null)
        {
            controller.shutdown();
        }

        controllerDelegateMap.remove(expectedControllerKey);
    }

    /**
     * Creates gateway controller (if not already created) and returns it.  This is in response to
     * a controller delegate or parser delegate request.  A controller delegate will ask for a controller
     * when sending an RCM downstream, and a parser delegate will request a controller when sending an RDM
     * upstream.
     * Also called in response to the DG receiving a MapObjectOnline, creates a gateway controller if necessary,
     * and puts it into a map of controllers.  Also creates a controller delegate that is injected into the controller.
     * This method of creating a controller is for gateway synchronization in the case of a user dropping an icon
     * on the map or file import.
     *
     * @param controllerKey    Unique key of controller to locate/create
     * @param createIfNotFound boolean to create a GenericGatewayController if the plugin is not found.
     * @return controller, if it was created.
     */
    private IGatewayController createController(String controllerKey, boolean createIfNotFound)
    {
        final boolean debugEnabled = logger.isDebugEnabled();

        // Start with a simple get, which dos no internal locking, making the happy path lock-free
        IGatewayController controller = controllerMap.get(controllerKey);

        if (controller != null)             // Found controller, return it
        {
            if (debugEnabled) logger.debug("IGatewayController exists for {}... returning it", controllerKey);
        } else
        {
            // Controller does not yet exist, create it
            // Now use an atomic operation to create the new controller
            controller = controllerMap.computeIfAbsent(controllerKey, k -> internalCreateController(controllerKey, createIfNotFound));
        }

        if (debugEnabled && controller == null)
        {
            logger.debug("Could not find or create an IGatewayController for key:  {}", controllerKey);
        }

        return controller;
    }

    /**
     * Create a new gateway controller.
     *
     * @param controllerKey    Unique key of controller to locate/create
     * @param createIfNotFound boolean to create a GenericGatewayController if the plugin is not found.
     * @return controller, if it was created
     * @since 2.14
     */
    private IGatewayController internalCreateController(String controllerKey, boolean createIfNotFound)
    {
        logger.debug("Create an IGatewayController for key: {}", controllerKey);

        IGatewayController newController = null;

        try
        {
            final String[] keyParts = controllerKey.split(KeyUtils.KEY_SEP);

            // Check the RDM key here.  It might seem intuitive to check it when an RDM is created or sent, but I
            // am hesitant to add any more overhead there.  This check will only be performed on a new controller.
            if (keyParts.length < 3)
            {
                throw new IllegalArgumentException("Illegal RaptorDataMessage key = [" + controllerKey + "].  Verify that family, type and unitId were properly set on the message");
            }
            final String family = keyParts[0];
            final String deviceType = keyParts[1];
            final String unitID = keyParts[2];

            IDevicePlugin plugin = devicePluginRegistry.getPlugin(family, deviceType);
            if (plugin != null)
            {
                GatewayControllerDelegate delegate = new GatewayControllerDelegate(family, deviceType, unitID);

                logger.debug("Created IGatewayController for key:  {}", controllerKey);

                newController = plugin.createGatewayController(unitID, delegate);

                if (newController != null)
                {
                    controllerDelegateMap.put(newController.getUniqueKey(), delegate);
                }
            } else
            {
                // Skip the WARN message if the MOM has a known configuration for this family/type.  This
                // indicates that it is an icon provided by a plugin.

                final var pluginDefinition = mapObjectManager.getPluginDefinition(family, deviceType);

                // If the family comes back as GENERIC, then we don't actually have a configuration for it.
                // Issue a warning unless it is not a device, or we are running the unit tests.

                final boolean isNonDevice = KeyUtils.isNonDevice(family);

                if (pluginDefinition.isEmpty() && isNonDevice && !platformUnderTest && !CotIcon.TYPE.equals(deviceType))
                {
                    final String key = KeyUtils.getUniqueKey(family, deviceType);
                    if (emittedMissingPluginWarnings.add(key))
                    {
                        logger.warn("Attempt to create a controller for an unknown type of {}", key);
                    }
                }

                if (!isNonDevice && createIfNotFound)
                {
                    logger.debug("Creating GenericGatewayController for: {}", controllerKey);

                    GatewayControllerDelegate delegate = new GatewayControllerDelegate(family, deviceType, unitID);
                    controllerDelegateMap.put(controllerKey, delegate);
                    newController = new GenericGatewayController(family, deviceType, unitID, delegate);
                }
            }
            return newController;
        } catch (Exception e)
        {
            logger.warn(PluginConstants.PLUGIN_EXCEPTION + ": Exception accessing plugins in DeviceGateway ", e);
            return null;
        }
    }

    /**
     * Creates parsers and framers for a new comm service that has been added to the system.  Also creates
     * delegates for both.  Framers and parsers are put into a map and the parser's data structures are
     * registered with the DataPortal.
     *
     * @param commService ICommService to create parsers and framers
     */
    private void createParsersAndFramers(ICommService commService)
    {
        final String portFormat = (String) commService.getProperties().get(CommServiceConstants.PORT_FORMAT.toString());

        if (!CommServiceConstants.NO_PARSERS_FRAMERS.toString().equals(portFormat))
        {
            if (logger.isDebugEnabled()) logger.debug("Create parser/framer for port name:  {}", commService.getName());

            //try catch(throwable) and log as error.  Get name of exception and put in logger.
            try
            {
                for (IDevicePlugin plugin : devicePluginRegistry.getPlugins())
                {
                    createParserForCommService(commService, portFormat, plugin);
                    createFramerForCommService(commService, portFormat, plugin);
                }
            } catch (Exception e)
            {
                logger.warn(PluginConstants.PLUGIN_EXCEPTION + ": Exception accessing plugins in DeviceGateway ", e);
            }
        }
    }

    /**
     * Create and configure a parser for the given comm service and plugin.  The plugin will be queried to determine
     * if it wants to supply a parser for this given comm service or not using
     * {@link IDevicePlugin#createGatewayParser(CommServiceType, String, String, IGatewayParserDelegate)}.
     *
     * @param commService Comm service being processed
     * @param portFormat  Format name (if any) for this comm service
     * @param plugin      Plugin to process
     */
    private void createParserForCommService(ICommService commService, String portFormat, IDevicePlugin plugin)
    {
        final CommServiceType sourceType = commService.getType();
        final String sourceName = commService.getName();
        final String devType = plugin.getType();
        final String devFamily = plugin.getFamily();

        final IGatewayParserDelegate parserDelegate = new ParserDelegate(devFamily, devType, sourceName);

        IGatewayParser parser = null;
        try
        {
            parser = plugin.createGatewayParser(sourceType, sourceName, portFormat, parserDelegate);
        } catch (NullPointerException npe)
        {
            logger.warn(PluginConstants.PLUGIN_EXCEPTION + "parameter 'format' may be null; plugin didn't handle this situation properly.  No parser was registered.", npe);
        }

        if (null != parser)
        {
            final String deviceKey = KeyUtils.getUniqueKey(devFamily, devType);
            final CommServiceKey commServiceKey = new CommServiceKey(sourceType, sourceName);

            if (logger.isDebugEnabled()) logger.debug("parser for:  {}  key:  {}", deviceKey, commServiceKey);

            parser.setDataStructureFactory(plugin.getDataStructureFactory());
            parserMap.put(commServiceKey, parser);
        } else
        {
            if (logger.isDebugEnabled()) logger.debug("No parser created for plugin: {}", plugin.getUniqueKey());
        }
    }

    /**
     * Create and configure a framer for the given comm service and plugin.  The plugin will be queried to determine
     * if it wants to supply a framer for this given comm service or not using
     * {@link IDevicePlugin#createGatewayFramer(CommServiceType, String, String, IGatewayFramerDelegate)}.
     *
     * @param commService Comm service being processed
     * @param portFormat  Format name (if any) for this comm service
     * @param plugin      Plugin to process
     */
    private void createFramerForCommService(ICommService commService, String portFormat, IDevicePlugin plugin)
    {
        final CommServiceType sourceType = commService.getType();
        final String sourceName = commService.getName();
        final String devType = plugin.getType();
        final String devFamily = plugin.getFamily();
        final String deviceKey = KeyUtils.getUniqueKey(devFamily, devType);

        final IGatewayFramerDelegate framerDelegate = new FramerDelegate(sourceName, commService, deviceKey);

        IGatewayFramer framer = null;
        try
        {
            framer = plugin.createGatewayFramer(sourceType, sourceName, portFormat, framerDelegate);
        } catch (NullPointerException npe)
        {
            logger.warn(PluginConstants.PLUGIN_EXCEPTION + "parameter 'format' may be null; plugin didn't handle this situation properly.  No framer was registered.", npe);
        }

        if (null != framer)
        {
            final CommServiceKey commServiceKey = new CommServiceKey(sourceType, sourceName);

            if (logger.isDebugEnabled()) logger.debug("Framer for:  {}  key:  {}", deviceKey, commServiceKey);

            framer.setDataStructureFactory(plugin.getDataStructureFactory());
            framerMap.put(commServiceKey, framer);
        } else
        {
            if (logger.isDebugEnabled()) logger.debug("No framer created for plugin: {}", plugin.getUniqueKey());
        }
    }

    /**
     * Remove the parsers and framers for a specific sourceType and sourceName.  This method is
     * called when a comm service goes away.
     *
     * @param sourceType type of comm service going away
     * @param sourceName name of comm service going away.
     * @see #connectionRemoved(ConnectionRemoved)
     */
    private void destroyParsersAndFramers(CommServiceType sourceType, String sourceName)
    {
        final CommServiceKey commServiceKey = new CommServiceKey(sourceType, sourceName);
        if (logger.isDebugEnabled()) logger.debug("Removing parsers and framers for: {}", commServiceKey);

        parserMap.removeAll(commServiceKey)
                .forEach(IParser::shutdown);

        framerMap.removeAll(commServiceKey)
                .forEach(IGatewayFramer::shutdown);
    }

    private void unregisterAllControllers()
    {
        for (IGatewayController controller : controllerMap.values())
        {
            if (logger.isTraceEnabled()) logger.trace("Shutting down controller {}", controller);
            controller.shutdown();
        }
        controllerMap.clear();

        controllerDelegateMap.values().forEach(IGatewayControllerDelegate::shutdown);
        controllerDelegateMap.clear();

        if (logger.isDebugEnabled()) logger.debug("Unregistered all controllers");
    }

    private void stopDeviceGateway()
    {
        simulatorConnectionManager.stopListening();

        for (IDevicePluginDefinition pluginDefinition : devicePluginRegistry.getPluginDefinitions())
        {
            dataStructures.remove(pluginDefinition.getUniqueKey());
        }

        logger.info("Stopping Gateway");
        unregisterAllControllers();

        if (logger.isTraceEnabled()) logger.trace("Completed shutting down DG");
    }

    /**
     * Simple class holding comm service info (source type and name) to be used as a key into our managed
     * parser/framer collections.
     */
    private record CommServiceKey(CommServiceType sourceType, String sourceName)
    {
    }
}
