package gov.takx.core.tracks;

import gov.takx.api.configuration.IProgramEnvironment;
import gov.takx.api.event.StartProjectServices;
import gov.takx.api.event.StopProjectServices;
import gov.takx.api.event.mapobject.MapObjectRemoved;
import gov.takx.api.mapobject.IIconMapObject;
import gov.takx.api.mapobject.IMapObject;
import gov.takx.api.mapobject.IMapObjectController;
import gov.takx.api.mapobject.IMapObjectManager;
import gov.takx.api.persist.ISmartTrackPersist;
import gov.takx.api.tracks.IConnectingLineDefinition;
import gov.takx.api.tracks.IExtendedSmartTrackManager;
import gov.takx.api.tracks.ISmartTrackDefinitionListener;
import gov.takx.api.tracks.ITrackStyleDefinition;
import gov.takx.api.tracks.ITrackStylingContext;
import gov.takx.commons.utils.UUIDUtils;
import gov.takx.core.project.ServiceOrdering;
import gov.takx.dataservices.persist.TrackStyleDefinition;
import gov.takx.platform.tracks.ConnectingLineDefinitionBuilder;
import gov.takx.platform.tracks.DefinitionEvent;
import gov.takx.platform.tracks.DefinitionEvent.DefinitionType;
import gov.takx.platform.tracks.TrackStyleDefinitionBuilder;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Priority;
import jakarta.enterprise.event.Observes;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import jakarta.xml.bind.JAXBException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.lang.invoke.MethodHandles;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 */
@Singleton
public class SmartTrackManager implements IExtendedSmartTrackManager
{
    private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    private final Map<String, ITrackStyleDefinition> trackStyleDefinitionsMap = new HashMap<>();
    private final Map<Integer, ITrackStyleDefinition> trackStyleDefinitionsByIdMap = new HashMap<>();
    private final Map<String, IConnectingLineDefinition> connectingLineDefinitionsMap = new HashMap<>();

    private final List<ISmartTrackDefinitionListener> eventListeners = new CopyOnWriteArrayList<>();

    private ITrackStyleDefinition globalTrackStyleDefinition;
    private IConnectingLineDefinition globalConnectingLineDefinition;

    private boolean globalsLoaded = false; // Tracks if our global TSD/CLD have been loaded/created

    // Map from map object ID to styling context to use for that map object
    private final Map<Integer, ITrackStylingContext> mapObjectStylingContexts = new ConcurrentHashMap<>();

    private final File userFilesDir;
    private final ISmartTrackPersist smartTrackPersist;
    private final IMapObjectManager mapObjectManager;
    private final TrackStylingContextFactory trackStylingContextFactory;

    @Inject
    SmartTrackManager(IProgramEnvironment programEnvironment, ISmartTrackPersist smartTrackPersist,
                      IMapObjectManager mapObjectManager,
                      TrackStylingContextFactory trackStylingContextFactory)
    {
        userFilesDir = programEnvironment.getUserFilesDir();

        this.smartTrackPersist = smartTrackPersist;
        this.mapObjectManager = mapObjectManager;
        this.trackStylingContextFactory = trackStylingContextFactory;
    }

    @Override
    public ITrackStyleDefinition createDeferringTSD(@Nonnull IMapObject mapObject)
    {
        // The builder is actually a lot of overhead, so do this directly
        TrackStyleDefinition definition =
                new TrackStyleDefinition(mapObject.getFamily(), mapObject.getType(), mapObject.getKey(), false);

        definition.setDisplayName("TSD for " + mapObject);
        return definition;
    }

    @Override
    public IConnectingLineDefinition createDeferringCLD(@Nonnull IMapObject mapObject)
    {
        return new ConnectingLineDefinitionBuilder(mapObject.getFamily(), mapObject.getType(), UUIDUtils.generateType1UUID().toString(), true)
                .setDisplayName("CLD for : " + mapObject)
                .setParent(getGlobalConnectingLineDefinition())
                .build();
    }

    @Override
    public void registerTrackStyle(@Nonnull ITrackStyleDefinition trackStyleDefinition)
    {
        smartTrackPersist.persistTrackStyleDefinition(trackStyleDefinition);

        internalRegisterTSD(trackStyleDefinition);
    }

    /**
     * Internal method to register (add it to the map) a TSD and fire the notification event.
     *
     * @param trackStyleDefinition The TSD to register
     */
    private void internalRegisterTSD(ITrackStyleDefinition trackStyleDefinition)
    {
        trackStyleDefinitionsMap.put(trackStyleDefinition.getUniqueKey(), trackStyleDefinition);
        trackStyleDefinitionsByIdMap.put(trackStyleDefinition.getId(), trackStyleDefinition);

        final DefinitionEvent event = new DefinitionEvent(this, trackStyleDefinition, DefinitionType.TrackStyle);

        eventListeners.forEach(l -> l.onDefinitionRegistered(event));
    }

    @Override
    public ITrackStyleDefinition getGlobalTrackStyleDefinition()
    {
        return globalTrackStyleDefinition;
    }

    @Override
    public IConnectingLineDefinition getGlobalConnectingLineDefinition()
    {
        return globalConnectingLineDefinition;
    }

    @Override
    public ITrackStyleDefinition unregisterTrackStyle(ITrackStyleDefinition trackStyleDefinition)
    {
        ITrackStyleDefinition removedDefinition = null;

        if (null != trackStyleDefinition)
        {
            removedDefinition = trackStyleDefinitionsMap.remove(trackStyleDefinition.getUniqueKey());
            trackStyleDefinitionsByIdMap.remove(trackStyleDefinition.getId());

            final DefinitionEvent event = new DefinitionEvent(this, trackStyleDefinition, DefinitionType.TrackStyle);

            eventListeners.forEach(l -> l.onDefinitionUnregistered(event));
        }

        // Now delete it from the database
        smartTrackPersist.deleteTrackStyleDefinition(trackStyleDefinition);

        return removedDefinition;
    }

    @Override
    public boolean isTrackStyleAssigned(ITrackStyleDefinition trackStyleDefinition)
    {
        return null != trackStyleDefinition && trackStyleDefinitionsMap.containsKey(trackStyleDefinition.getUniqueKey());
    }

    @Override
    public ITrackStyleDefinition getTrackStyleDefinition(String key)
    {
        return trackStyleDefinitionsMap.get(key);
    }

    @Override
    public Collection<ITrackStyleDefinition> getTrackStyleDefinitions()
    {
        // Don't want developers to be able to delete TSDs, so return a new collection.
        return new ArrayList<>(trackStyleDefinitionsMap.values());
    }

    @Override
    public ITrackStyleDefinition getTrackStyleDefinitionByID(Integer id)
    {
        return trackStyleDefinitionsByIdMap.get(id);
    }

    @Override
    public void registerConnectingLine(@Nonnull IConnectingLineDefinition connectingLineDefinition)
    {
        // If a CLD that is not the GLOBAL CLD is registered, then ensure that it's parent reference is the
        // GLOBAL CLD.  This is to make it easy for plugin developers to create CLDs and not have to worry about
        // wiring up the parent link.
        if (!connectingLineDefinition.getUniqueKey().equals(GLOBAL_CONNECTING_LINE_UUID))
        {
            if (connectingLineDefinition.getParent() == null)
            {
                connectingLineDefinition.setParent(globalConnectingLineDefinition);
            }
        }

        smartTrackPersist.persistConnectingLineDefinition(connectingLineDefinition);

        internalRegisterCLD(connectingLineDefinition);
    }

    /**
     * Internal method to register (add it to the map) a CLD and fire the notification event.
     *
     * @param connectingLineDefinition The CLD to register
     */
    private void internalRegisterCLD(IConnectingLineDefinition connectingLineDefinition)
    {
        connectingLineDefinitionsMap.put(connectingLineDefinition.getUniqueKey(), connectingLineDefinition);

        final DefinitionEvent event = new DefinitionEvent(this, connectingLineDefinition, DefinitionType.ConnectingLine);

        eventListeners.forEach(l -> l.onDefinitionRegistered(event));
    }

    @Override
    public IConnectingLineDefinition unregisterConnectingLine(IConnectingLineDefinition connectingLineDefinition)
    {
        IConnectingLineDefinition removedDefinition = null;

        if (null != connectingLineDefinition)
        {
            removedDefinition = connectingLineDefinitionsMap.remove(connectingLineDefinition.getUniqueKey());

            final DefinitionEvent event = new DefinitionEvent(this, connectingLineDefinition, DefinitionType.ConnectingLine);

            eventListeners.forEach(l -> l.onDefinitionUnregistered(event));
        }

        // Now delete it from the database
        smartTrackPersist.deleteConnectingLineDefinition(connectingLineDefinition);

        return removedDefinition;
    }

    @Override
    public boolean isConnectingLineAssigned(IConnectingLineDefinition connectingLineDefinition)
    {
        return null != connectingLineDefinition && connectingLineDefinitionsMap.containsKey(connectingLineDefinition.getUniqueKey());
    }

    /**
     * Get a track styling context for the given map object.  An instance will be created on demand as needed.
     * The returned context will refer to the TSD assigned to the map object and will reach back to the global
     * TSD for any values not specified on the map object level TSD.  The context will respond to property
     * changes on the map object and properly update itself if the TSD on the map object is replaced.
     *
     * @param mapObject The map object of interest
     * @return track styling context
     * @see IIconMapObject#setTrackStyleDefinition(ITrackStyleDefinition)
     */
    @Override
    public ITrackStylingContext getTrackStylingContext(@Nonnull IMapObject mapObject)
    {
        return mapObjectStylingContexts.computeIfAbsent(mapObject.getId(), k -> trackStylingContextFactory.makeTrackStylingContext(mapObject));
    }

    @Override
    public ITrackStylingContext getTrackStylingContext(Integer moId)
    {
        return mapObjectStylingContexts.computeIfAbsent(moId, k -> {
            IMapObjectController controller = mapObjectManager.getMapObjectController(moId);
            if (controller == null) throw new IllegalArgumentException("No map object for id: " + moId);
            final IMapObject mapObject = controller.getMapObject();
            return trackStylingContextFactory.makeTrackStylingContext(mapObject);
        });
    }

    /**
     * Stop our service.
     */
    private void stopService(@SuppressWarnings("UnusedParameters") @Observes @Priority(ServiceOrdering.STOP_SmartTrackManager) StopProjectServices event)
    {
        globalTrackStyleDefinition = null;
        globalConnectingLineDefinition = null;
        globalsLoaded = false;

        trackStyleDefinitionsMap.clear();
        trackStyleDefinitionsByIdMap.clear();
        connectingLineDefinitionsMap.clear();
        mapObjectStylingContexts.clear();
        eventListeners.clear();
    }

    /**
     * Load our state when the services are started.
     */
    private void startService(@SuppressWarnings("UnusedParameters") @Observes @Priority(ServiceOrdering.START_SmartTrackManager) StartProjectServices event)
    {
        doInitialLoad();
    }

    /**
     * Load our state.  Note that the invocation of {@link #startService(StartProjectServices)} may come too late when
     * reopening an existing project that has Map Objects with TSD/CLD instances associated with them.  So, this
     * method is also called directly by {@link gov.takx.core.persist.MapObjectPersist} when it loads its initial data.
     *
     * @since 3.2
     */
    public void doInitialLoad()
    {
        if (globalsLoaded && globalConnectingLineDefinition != null && globalTrackStyleDefinition != null)
        {
            logger.debug("Globals already loaded");
            return;
        }

        logger.debug("Load TSD/CLD state");

        // See if we already have a global TSD in persistence, if not, create it
        globalTrackStyleDefinition = smartTrackPersist.queryTrackStyleByUniqueKey(GLOBAL_TRACK_STYLE_UUID);

        if (null == globalTrackStyleDefinition) createGlobalTSD();

        // Load and register all the remaining TSDs
        smartTrackPersist.queryAllTrackStyleDefinitions().forEach(this::internalRegisterTSD);

        // See if we already have a global CLD, if not, create it

        // NOTE: We MUST do this before loading all the other CLDs since the onLoad handler for a CLD will try
        // to wire it up to the global CLD if need be.  So, we have to have the global CLD processed first.

        globalConnectingLineDefinition = smartTrackPersist.queryConnectingLineByUniqueKey(GLOBAL_CONNECTING_LINE_UUID);

        if (null == globalConnectingLineDefinition) createGlobalCLD();

        // Load and register all the remaining CLDs
        smartTrackPersist.queryAllConnectingLineDefinitions().forEach(this::internalRegisterCLD);

        globalsLoaded = true;
    }

    /**
     * Create the global Track Style Definition instance and persist it out.
     */
    private void createGlobalTSD()
    {
        final File xmlFile = new File(userFilesDir, "TrackStyle.xml");
        if (xmlFile.exists())
        {
            // Read in user defined preferences
            try
            {
                globalTrackStyleDefinition = TrackStyleDefinitionBuilder.buildFromXML(xmlFile.toURI().toURL())
                        .get(GLOBAL_TRACK_STYLE_UUID);
            } catch (JAXBException | IOException e)
            {
                logger.error("Failed to load Global TrackStyleDefinition from: {}", xmlFile, e);
            }
        }

        if (null == globalTrackStyleDefinition)
        {
            // There was no Global scope definition in the track file; Create using default values
            globalTrackStyleDefinition = new TrackStyleDefinitionBuilder(GLOBAL_SMART_TRACK_FAMILY,
                    GLOBAL_TRACK_STYLE_TYPE, GLOBAL_TRACK_STYLE_UUID)
                    .setDisplayName("Global TSD")
                    .build();
        }
        // Persist the global track style definition
        smartTrackPersist.persistTrackStyleDefinition(globalTrackStyleDefinition);
    }

    /**
     * Create the global Connecting Line Definition instance and persist it out.
     */
    private void createGlobalCLD()
    {
        final File xmlFile = new File(userFilesDir, "ConnectingLine.xml");
        if (xmlFile.exists())
        {
            // Read in user defined preferences
            try
            {
                globalConnectingLineDefinition = ConnectingLineDefinitionBuilder.buildFromXML(xmlFile.toURI().toURL())
                        .get(GLOBAL_CONNECTING_LINE_UUID);
            } catch (JAXBException | IOException e)
            {
                logger.error("Failed to load Global ConnectingLineDefinition from: {}", xmlFile, e);
            }
        }

        if (null == globalConnectingLineDefinition)
        {
            // There was no Global scope definition in the track file; Create default values
            globalConnectingLineDefinition = new ConnectingLineDefinitionBuilder(GLOBAL_SMART_TRACK_FAMILY,
                    GLOBAL_CONNECTING_LINE_TYPE, GLOBAL_CONNECTING_LINE_UUID)
                    .setDisplayName("Global CLD")
                    .build();
        }

        // Persist the global track style definition
        smartTrackPersist.persistConnectingLineDefinition(globalConnectingLineDefinition);
    }

    @Override
    public void addDefinitionListener(@Nonnull ISmartTrackDefinitionListener listener)
    {
        eventListeners.add(listener);
    }

    @Override
    public void removeDefinitionListener(@Nonnull ISmartTrackDefinitionListener listener)
    {
        eventListeners.remove(listener);
    }

    /**
     * Notified by {@link IMapObjectManager} when a map object is removed so we can cleanup to keep memory from bloating.
     *
     * @param event The event providing the removed map object
     * @since 2.38 - formerly clientMapObjectRemoved
     */
    private void onClientMapObjectRemoved(@Observes MapObjectRemoved event)
    {
        mapObjectStylingContexts.remove(event.getMapObject().getId());
    }
}
