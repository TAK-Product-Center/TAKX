package gov.takx.core.mapobject;

import gov.tak.api.maps.PointMapItem;
import gov.tak.core.location.LocationTracker;
import gov.takx.api.annotation.UiEventBus;
import gov.takx.api.annotation.UseClientClassLoader;
import gov.takx.api.annotation.VFSManagerScope;
import gov.takx.api.coordinatesystem.ILocation;
import gov.takx.api.dataportal.IRaptorDataPortal;
import gov.takx.api.dataportal.IRaptorDataPortalController;
import gov.takx.api.event.StartPlugins;
import gov.takx.api.event.StartProjectServices;
import gov.takx.api.event.StopProjectServices;
import gov.takx.api.event.mapobject.MapObjectAdded;
import gov.takx.api.event.mapobject.MapObjectPartialDelete;
import gov.takx.api.event.mapobject.MapObjectRemoved;
import gov.takx.api.event.project.ProjectSaving;
import gov.takx.api.filesystem.IVirtualFileSystemManager;
import gov.takx.api.filesystem.VFSScope;
import gov.takx.api.globe.IExtendedGlobeManager;
import gov.takx.api.mapobject.CreationFailedException;
import gov.takx.api.mapobject.IAnnotationMapObject;
import gov.takx.api.mapobject.ICoreMapObjectManager;
import gov.takx.api.mapobject.IDeletedMapObject;
import gov.takx.api.mapobject.IFileMapObject;
import gov.takx.api.mapobject.IFlatAnnotationMapObject;
import gov.takx.api.mapobject.IIconMapObject;
import gov.takx.api.mapobject.IMapObject;
import gov.takx.api.mapobject.IMapObjectController;
import gov.takx.api.mapobject.IMapObjectManagerListener;
import gov.takx.api.mapobject.IMarkerMapObject;
import gov.takx.api.mapobject.IMultiPolylineMapObject;
import gov.takx.api.mapobject.IShapeMapObject;
import gov.takx.api.mapobject.ITrackContextMenuListener;
import gov.takx.api.mapobject.ITrackSelectListener;
import gov.takx.api.mapobject.MapObjectSpec;
import gov.takx.api.mapobject.MapToolType;
import gov.takx.api.mapobject.TrackSelectEvent;
import gov.takx.api.messages.IMapEntity;
import gov.takx.api.messages.IRaptorDataMessage;
import gov.takx.api.messages.IRaptorDataStructure;
import gov.takx.api.persist.IBackgroundDeleteManager;
import gov.takx.api.persist.IMapObjectPersist;
import gov.takx.api.persist.IPersistService;
import gov.takx.api.persist.IPersistServiceProvider;
import gov.takx.api.persist.IPluginStatePersist;
import gov.takx.api.platform.IProgressMonitor;
import gov.takx.api.plugin.IPluginDefinition;
import gov.takx.api.plugin.IRaptorPlugin;
import gov.takx.api.plugin.device.IDevicePlugin;
import gov.takx.api.plugin.device.IDevicePluginDefinition;
import gov.takx.api.registry.IAppPluginRegistry;
import gov.takx.api.registry.IDevicePluginRegistry;
import gov.takx.api.registry.ISyntheticPluginRegistry;
import gov.takx.api.shapes.ShapeType;
import gov.takx.api.tracks.IConnectingLineDefinition;
import gov.takx.api.tracks.IExtendedSmartTrackManager;
import gov.takx.api.tracks.ITrackStyleDefinition;
import gov.takx.api.ui.event.MapObjectSelectedEvent;
import gov.takx.commons.lang.ExceptionUtils;
import gov.takx.commons.utils.UUIDUtils;
import gov.takx.core.plugin.synthetic.SyntheticPlugin;
import gov.takx.core.project.ServiceOrdering;
import gov.takx.dataservices.persist.AShapeMapObject;
import gov.takx.dataservices.persist.AnnotationMapObject;
import gov.takx.dataservices.persist.DeletedMapObject;
import gov.takx.dataservices.persist.FileMapObject;
import gov.takx.dataservices.persist.FlatAnnotationMapObject;
import gov.takx.dataservices.persist.IconMapObject;
import gov.takx.dataservices.persist.MarkerMapObject;
import gov.takx.dataservices.persist.MultiPolylineMapObject;
import gov.takx.dataservices.persist.RaptorDataStructure;
import gov.takx.module.PluginDefinitionFactory;
import gov.takx.platform.KeyUtils;
import gov.takx.platform.utils.UnitIdHelper;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import jakarta.annotation.Priority;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Event;
import jakarta.enterprise.event.Observes;
import jakarta.inject.Inject;
import org.greenrobot.eventbus.EventBus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.JFrame;
import javax.swing.SwingUtilities;

import java.awt.Component;
import java.awt.MouseInfo;
import java.awt.Point;
import java.lang.invoke.MethodHandles;
import java.lang.reflect.Constructor;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;

/**
 */
@SuppressWarnings("FieldAccessedSynchronizedAndUnsynchronized")
@ApplicationScoped
public class MapObjectManager implements ICoreMapObjectManager
{
    private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    // To simplify the MO creation code (and make it require less maintenance), we locate all the constructors
    // up front and then just invoke the appropriate constructor when a MO is being created.

    private static final Map<String, Constructor<? extends IMapObject>> MAP_OBJECT_CONSTRUCTORS = new HashMap<>();
    private static final Class<?>[] CONSTRUCTOR_ARG_TYPES = {MapObjectSpec.class, IPluginDefinition.class};
    private static final Constructor<? extends IMapObject> DEFAULT_CONSTRUCTOR;

    static
    {
        // Load in all the MO constructors
        addConstructor(KeyUtils.TOOL_FAMILY, IAnnotationMapObject.ANNOTATION_TYPE, AnnotationMapObject.class);
        addConstructor(KeyUtils.TOOL_FAMILY, IFlatAnnotationMapObject.FLAT_ANNOTATION_TYPE, FlatAnnotationMapObject.class);

        addConstructor(KeyUtils.FILE_FAMILY, IFileMapObject.FILE_TYPE, FileMapObject.class);

        addConstructor(IMarkerMapObject.MARKER_FAMILY, IMarkerMapObject.MARKER_TYPE, MarkerMapObject.class);
        addConstructor(KeyUtils.COMPOUND_FAMILY, IMultiPolylineMapObject.MULTI_POLYLINE_TYPE, MultiPolylineMapObject.class);

        // And all the shapes/tools
        Arrays.stream(MapToolType.values())
                .filter(mtt -> mtt.getShapeType() != ShapeType.NONE)
                .forEach(mtt -> addConstructor(KeyUtils.TOOL_FAMILY, mtt.getShapeTypeName(), mtt.getMapObjectClass()));

        // For all other types, we use an IconMapObject
        DEFAULT_CONSTRUCTOR = findConstructor(IconMapObject.class);
    }

    /**
     * Find the constructor for the given {@code mapObjectClass} and add it to the constructor map using
     * the family and type for a key.
     *
     * @param family         Map Object family
     * @param type           Map Object type
     * @param mapObjectClass Map Object class on which to find the constructor
     * @since 3.0
     */
    private static void addConstructor(String family, String type, Class<? extends IMapObject> mapObjectClass)
    {
        MAP_OBJECT_CONSTRUCTORS.put(KeyUtils.getUniqueKey(family, type), findConstructor(mapObjectClass));
    }

    /**
     * Locate the constructor for a given MapObject subclass.
     *
     * @param mapObjectClass The MO subclass
     * @since 2.29
     */
    private static Constructor<? extends IMapObject> findConstructor(Class<? extends IMapObject> mapObjectClass)
    {
        try
        {
            return mapObjectClass.getConstructor(CONSTRUCTOR_ARG_TYPES);
        } catch (NoSuchMethodException e)
        {
            logger.error("Failed to find necessary constructor for {}", mapObjectClass.getSimpleName(), e);
            return null;
        }
    }

    private static final int MAP_OBJECT_START_ID = 1;

    private final Map<String, IMapObjectController> currentMapObjectControllers = new ConcurrentHashMap<>();
    private final Map<Integer, IMapObjectController> moId2mocMap = new ConcurrentHashMap<>();
    private final Map<String, Boolean> forcedVisibilityMap = new ConcurrentHashMap<>();

    private final Map<String, IMapObject> persistedMapObjects = new ConcurrentHashMap<>();

    private IMapObjectController selectedMapObject;
    private boolean loaded;

    private final List<IMapObjectManagerListener> managerListeners = new CopyOnWriteArrayList<>();

    // List of registered track select listeners (apps only, so far).  Includes listening for track select
    // events on the globe OR the device log.
    private final List<ITrackSelectListener> trackSelectListeners = new CopyOnWriteArrayList<>();

    // List of registered context menu listeners (apps only, so far).  Includes listening for right click events on
    // tracks on the globe OR the device log.
    private final List<ITrackContextMenuListener> trackContextMenuListeners = new CopyOnWriteArrayList<>();

    // Map from MapEntity key to instances of device plugins we manufactured (no actual plugin exists for it)
    private final Map<String, IDevicePlugin> manufacturedPlugins = new HashMap<>();

    private final UnitIdHelper unitIdHelper;
    private final IMapObjectPersist mapObjectPersist;
    private final IExtendedSmartTrackManager smartTrackManager;
    private final IPluginStatePersist pluginStatePersist;
    private final EventBus uiEventBus;
    private final IAppPluginRegistry appPluginRegistry;
    private final IDevicePluginRegistry devicePluginRegistry;
    private final ISyntheticPluginRegistry syntheticPluginRegistry;
    private final PluginDefinitionFactory pluginDefinitionFactory;
    private final IPersistServiceProvider persistServiceProvider;
    private final IRaptorDataPortal dataPortal;
    private final IBackgroundDeleteManager backgroundDeleteManager;
    private final IExtendedGlobeManager globeManager;
    private final IVirtualFileSystemManager projectVFSManager;
    private final LocationTracker locationTracker;
    private final Event<MapObjectAdded> mapObjectAddedEvent;
    private final Event<MapObjectRemoved> mapObjectRemovedEvent;
    private final Event<MapObjectPartialDelete> mapObjectPartialDeleteEvent;

    @Inject
    MapObjectManager(UnitIdHelper unitIdHelper, IMapObjectPersist mapObjectPersist, IExtendedSmartTrackManager smartTrackManager,
                     IPluginStatePersist pluginStatePersist, @UiEventBus EventBus uiEventBus, IAppPluginRegistry appPluginRegistry,
                     IDevicePluginRegistry devicePluginRegistry, ISyntheticPluginRegistry syntheticPluginRegistry,
                     PluginDefinitionFactory pluginDefinitionFactory, IPersistServiceProvider persistServiceProvider,
                     IRaptorDataPortal dataPortal, IBackgroundDeleteManager backgroundDeleteManager, IExtendedGlobeManager globeManager,
                     @VFSManagerScope(VFSScope.Project) IVirtualFileSystemManager projectVFSManager, LocationTracker locationTracker,
                     Event<MapObjectAdded> mapObjectAddedEvent, Event<MapObjectRemoved> mapObjectRemovedEvent,
                     Event<MapObjectPartialDelete> mapObjectPartialDeleteEvent)
    {
        this.unitIdHelper = unitIdHelper;
        this.mapObjectPersist = mapObjectPersist;
        this.smartTrackManager = smartTrackManager;
        this.pluginStatePersist = pluginStatePersist;
        this.uiEventBus = uiEventBus;
        this.appPluginRegistry = appPluginRegistry;
        this.devicePluginRegistry = devicePluginRegistry;
        this.syntheticPluginRegistry = syntheticPluginRegistry;
        this.pluginDefinitionFactory = pluginDefinitionFactory;
        this.persistServiceProvider = persistServiceProvider;
        this.dataPortal = dataPortal;
        this.backgroundDeleteManager = backgroundDeleteManager;
        this.globeManager = globeManager;
        this.projectVFSManager = projectVFSManager;
        this.locationTracker = locationTracker;
        this.mapObjectAddedEvent = mapObjectAddedEvent;
        this.mapObjectRemovedEvent = mapObjectRemovedEvent;
        this.mapObjectPartialDeleteEvent = mapObjectPartialDeleteEvent;
    }

    @Override
    public PointMapItem getSelfMarker()
    {
        return locationTracker.getMarkerMapObject();
    }

    @Override
    public boolean keyExists(String key)
    {
        return currentMapObjectControllers.containsKey(key);
    }

    @Override
    public boolean nameExists(String name)
    {
        return currentMapObjectControllers.values().stream().anyMatch(e -> e.getName().equals(name));
    }

    @Override
    public String generateUniqueName(String baseName)
    {
        int counter = 1;
        String name = baseName + " " + counter;
        while (nameExists(name))
        {
            name = baseName + " " + ++counter;
        }

        return name;
    }

    @Override
    public boolean createDevice(IDevicePluginDefinition definition, ILocation location)
    {
        final String family = definition.getFamily();
        final String type = definition.getType();

        final String unitId;
        final String name;

        if (definition.isAutoGenerateUnitId())
        {
            unitId = UUIDUtils.generateType1UUID().toString();

            // Set a friendly unit id as the map object name, so the user doesn't actually see the UUID.
            name = type + " " + unitIdHelper.getNextFriendlyUnitId(family, type);
        } else
        {
            unitId = unitIdHelper.getUserEnteredUnitId(definition);
            name = type + " " + unitId;
        }

        if (unitId != null)
        {
            final MapObjectSpec mapObjectSpec = MapObjectSpec.builder(getOrCreateMapEntity(family, type), unitId)
                    .name(name)
                    .location(location)
                    .build();

            final IMapObjectController controller = getOrCreateMapObject(mapObjectSpec).getController();

            controller.setDefaultPreferencesTab(definition.getDefaultPreferencesTab());

            return true;
        }
        return false;
    }

    @Override
    @UseClientClassLoader
    @Nonnull
    public synchronized IMapObject getOrCreateMapObject(@Nonnull MapObjectSpec mapObjectSpec) throws CreationFailedException
    {
        return internalCreateMapObject(mapObjectSpec);
    }

    @Nonnull
    @Override
    public synchronized IMapEntity getOrCreateMapEntity(@Nonnull String family, @Nonnull String type)
    {
        return getOrCreateMapEntity(family, type, Collections.emptyList());
    }

    @Override
    public synchronized IMapEntity getOrCreateMapEntity(@Nonnull String family, @Nonnull String type, @Nonnull List<IRaptorDataStructure> structures)
    {
        final var structureList = RaptorDataStructure.convertToStructureCollection(structures);

        return mapObjectPersist.getOrCreateMapEntity(family, type, mapEntity -> {
            mapEntity.addStructures(structureList);
            dataPortal.onMapObjectDefinition(mapEntity);
        }, mapEntity -> mapEntity.addStructures(structureList));
    }

    @Override
    public synchronized void deleteMapObject(@Nonnull IMapObject mapObject)
    {
        if (mapObject.isSelfMarker()) return; // Don't allow deletion of the self marker

        final String mapObjectKey = mapObject.getKey();

        logger.debug("delete MapObject: {}", mapObject);

        final var controller = currentMapObjectControllers.get(mapObjectKey);
        if (controller == null)
        {
            logger.info("MO {} has already been deleted; ignoring delete request", mapObjectKey);
            return;
        }

        cleanupMapObject(mapObject); // Remove the MOC and cleanup data
        controller.delete();

        // Do the same for any owned map objects that were also deleted
        for (String ownedKey : mapObjectPersist.deleteMapObject(mapObject))
        {
            final var ownedController = currentMapObjectControllers.get(ownedKey);
            cleanupMapObject(ownedController.getMapObject());
            ownedController.delete();
        }

        persistServiceProvider.cleanupPersistService();  // Be tidy as we exit
    }

    @Override
    public synchronized void partialDeleteMapObject(IMapObject mapObject, long timeOfDeletion)
    {
        // Make a DMO to record this operation
        final IDeletedMapObject dmo = new DeletedMapObject(mapObject, timeOfDeletion, true);
        persistServiceProvider.getPersistService().persistEntity(dmo); // Get it saved

        backgroundDeleteManager.add(dmo); // And submit it for work

        // Fire event first to give core priority so everything can be setup before notifying plugins.
        mapObjectPartialDeleteEvent.fire(new MapObjectPartialDelete(mapObject, timeOfDeletion));

        // And notify all listeners
        ExceptionUtils.safeDoOperation(managerListeners, l -> l.mapObjectPartialDelete(mapObject, timeOfDeletion));
    }

    private void cleanupMapObject(IMapObject mapObject)
    {
        // Notify deletion handlers before we actually tear things down so plugins can access the current MO state
        mapObject.notifyDeletionHandlers();

        globeManager.stopTracking(mapObject); // turn off any tracking associated with this map object
        removeContainmentOnMapObject(mapObject); // Undo any containment this MO is involved in

        final String key = mapObject.getKey();

        dataPortal.deleteMapObject(key);

        final var moc = currentMapObjectControllers.get(key);
        if (moc != null)
        {
            deleteMapObjectController(moc, false);
            projectVFSManager.deleteVirtualFileSystemForDevice(mapObject); // Remove all VFS records
        } else
        {
            logger.error("No MapObjectController found for key: {}", key);
        }
    }

    /**
     * Remove the given MapObject from any containment relationships it may participate in.
     *
     * @param mapObject Map object to process
     */
    private void removeContainmentOnMapObject(IMapObject mapObject)
    {
        if (mapObject instanceof IIconMapObject imo)
        {
            // Use a copy to avoid a ConcurrentModificationException when imo.removeContainedMapObject()
            // deletes the object from the original list.

            for (IIconMapObject containedMO : List.copyOf(imo.getContainedMapObjects()))
            {
                imo.removeContainedMapObject(containedMO);
                mapObjectPersist.persistMapObject(containedMO);
            }

            // If the icon is contained in another, remove it from its parent before deleting.
            IIconMapObject parent = imo.getParentContainer();
            if (parent != null)
            {
                parent.removeContainedMapObject(imo);
                mapObjectPersist.persistMapObject(parent);
            }
        }
    }

    @Override
    public void removeFromContainer(IIconMapObject mapObject)
    {
        final IIconMapObject container = mapObject.getParentContainer();
        if (container != null)
        {
            container.removeContainedMapObject(mapObject);

            mapObjectPersist.persistMapObject(container);
            mapObjectPersist.persistMapObject(mapObject);
        }
    }

    /**
     * Crank up our service on the project.  Load our state from the DB.
     */
    private void startService(@Observes @Priority(ServiceOrdering.START_MapObjectManager) StartProjectServices event)
    {
        final IProgressMonitor progressMonitor = event.getProgressMonitor();

        progressMonitor.setText("Loading map objects...");

        loaded = true;

        // Load all of our existing MOs

        mapObjectPersist.findAllMapObjects().forEach(mapObject -> {
            mapObject.initializeIcons();
            persistedMapObjects.put(mapObject.getKey(), mapObject);
        });

        trackSelectListeners.add(new TrackSelectListener());

        logger.info("Loaded map objects");
    }

    /**
     * Find and restore all the persisted Map Objects (when we are reopening a project).
     *
     * @since 2.29, moved here from DataPortal
     */
    private void restoreMapObjects(@Observes @Priority(ServiceOrdering.START_PLUGINS_MapObjectManager) StartPlugins event)
    {
        logger.debug("Starting MapObject data restore...");

        final IProgressMonitor progressMonitor = event.getProgressMonitor();
        progressMonitor.setText("Restoring Map Objects...");

        try
        {
            // Make a copy to avoid CMEs if something deletes a MO while we're restoring them
            final var mapObjects = List.copyOf(persistedMapObjects.values());

            // Bring all the shapes online first
            mapObjects.stream()
                    .filter(mo -> mo instanceof IShapeMapObject)
                    .forEach(mo -> restoreMapObject(progressMonitor, mo));

            // After the shapes are online, we will bring up the non-shapes.
            // This allows the non-shapes to re-attach their shapes to the shape service.
            mapObjects.stream()
                    .filter(mo -> !(mo instanceof IShapeMapObject))
                    .forEach(mo -> restoreMapObject(progressMonitor, mo));
        } catch (Exception e)
        {
            logger.error("Failed to bring existing MapObjects online", e);
        }
    }

    /**
     * Restore a Map Object by bringing it back online.
     *
     * @param progressMonitor To update
     * @param mapObject       Map Object being restored
     * @since 2.29
     */
    private void restoreMapObject(IProgressMonitor progressMonitor, IMapObject mapObject)
            throws CreationFailedException
    {
        logger.debug("Restoring {}", mapObject);
        progressMonitor.setSubstatus("Restoring " + mapObject.getKey());

        internalCreateMapObject(MapObjectSpec.from(mapObject));
    }

    /**
     * Stop our service.
     */
    private void stopService(@Observes @Priority(ServiceOrdering.STOP_MapObjectManager) StopProjectServices event)
    {
        if (loaded)
        {
            // Use a temp map since the deleteMOC call modifies the map
            new ArrayList<>(currentMapObjectControllers.values())
                    .forEach(moc -> deleteMapObjectController(moc, true));
        }

        // Always clear out our state and listeners, needed by unit tests
        forcedVisibilityMap.clear();
        currentMapObjectControllers.clear();
        moId2mocMap.clear();
        persistedMapObjects.clear();
        selectedMapObject = null;

        managerListeners.clear();
        trackSelectListeners.clear();
        trackContextMenuListeners.clear();
    }

    @Override
    public void shutdownPluginControllers(IPluginDefinition<?> pluginDefinition)
    {
        // Create the key prefix that all MO controllers for this plugin will have
        final String keyPrefix = pluginDefinition.getUniqueKey() + KeyUtils.KEY_SEP;

        currentMapObjectControllers.values().stream()
                .filter(moc -> moc.getKey().startsWith(keyPrefix))
                .forEach(moc -> {
                    if (selectedMapObject == moc) setSelected(null);
                    moc.shutdownPlugin();
                });
    }

    @Override
    public void setSelected(String mapObjectKey)
    {
        IMapObjectController newSelection = null;
        if (mapObjectKey != null)
        {
            newSelection = currentMapObjectControllers.get(mapObjectKey);

            if (Objects.equals(selectedMapObject, newSelection))
            {
                // MapObject is already selected, or we already have no selected map objects
                return;
            }
        }

        if (selectedMapObject != newSelection)
        {
            selectedMapObject = newSelection;
            notifySelectionListeners(newSelection);
        }
    }

    /**
     * Handle the saving event, save plugin state for Map Objects.
     *
     * @since 2.37
     */
    private void handleSaving(@Observes ProjectSaving event)
    {
        // All MapObjects are automatically persisted when a property changes, via MapObjectChangeMonitor.
        // So, all we need to do here is to ensure that any associated plugin state gets persisted

        for (IMapObjectController moc : currentMapObjectControllers.values())
        {
            final IMapObject mo = moc.getMapObject();

            if (mo instanceof IIconMapObject && mo.canHavePluginState())
            {
                pluginStatePersist.saveOrUpdateState(moc.getKey(), moc.getPluginController());
            }
        }
    }

    @Override
    public void addMapObjectManagerListener(IMapObjectManagerListener listener)
    {
        managerListeners.add(listener);
    }

    @Override
    public void removeMapObjectManagerListener(IMapObjectManagerListener listener)
    {
        managerListeners.remove(listener);
    }

    @Override
    public String getNextUnitID(String family, String mapObjectType)
    {
        return unitIdHelper.getNextUnitID(family, mapObjectType, MAP_OBJECT_START_ID);
    }

    @Override
    public IMapObject getSelectedMapObject()
    {
        return selectedMapObject != null ? selectedMapObject.getMapObject() : null;
    }

    @Override
    public Optional<? extends IPluginDefinition<? extends IRaptorPlugin>> getPluginDefinition(@Nonnull String family,
                                                                                              @Nonnull String type)
    {
        Optional<? extends IPluginDefinition<? extends IRaptorPlugin>> definition = devicePluginRegistry.getPluginDefinition(family, type);
        if (definition.isPresent()) return definition;

        definition = appPluginRegistry.getPluginDefinition(family, type);
        if (definition.isPresent()) return definition;

        return syntheticPluginRegistry.getPluginDefinition(family, type);
    }

    @Override
    public IMapObjectController getMapObjectController(@Nonnull String key)
    {
        return currentMapObjectControllers.get(key);
    }

    @Override
    public IMapObjectController getMapObjectController(Integer moId)
    {
        return moId != null ? moId2mocMap.get(moId) : null;
    }

    @Nonnull
    @Override
    public Collection<IMapObjectController> getMapObjectControllers()
    {
        return Collections.unmodifiableCollection(currentMapObjectControllers.values());
    }

    @Nullable
    @Override
    public IMapObject getMapObject(@Nonnull String key)
    {
        final IMapObjectController controller = currentMapObjectControllers.get(key);
        return controller == null ? null : controller.getMapObject();
    }

    @Nonnull
    @Override
    public Collection<? extends IMapObject> getMapObjects()
    {
        return currentMapObjectControllers.values().stream().map(IMapObjectController::getMapObject).toList();
    }

    @Override
    public List<ITrackSelectListener> getTrackSelectListeners()
    {
        return Collections.unmodifiableList(trackSelectListeners);
    }

    @Override
    public List<ITrackContextMenuListener> getTrackContextMenuListeners()
    {
        return Collections.unmodifiableList(trackContextMenuListeners);
    }

    @Override
    public void addTrackSelectListener(ITrackSelectListener listener)
    {
        trackSelectListeners.add(listener);
    }

    @Override
    public void removeTrackSelectListener(ITrackSelectListener listener)
    {
        trackSelectListeners.remove(listener);
    }

    @Override
    public void addTrackContextMenuListener(ITrackContextMenuListener listener)
    {
        trackContextMenuListeners.add(listener);
    }

    @Override
    public void removeTrackContextMenuListener(ITrackContextMenuListener listener)
    {
        trackContextMenuListeners.remove(listener);
    }

    private void notifySelectionListeners(IMapObjectController moc)
    {
        uiEventBus.post(new MapObjectSelectedEvent(moc));
    }

    @UseClientClassLoader
    @Nonnull
    private IMapObject createMapObject(String key, MapObjectSpec mapObjectSpec, IPluginDefinition<?> pluginDefinition)
            throws CreationFailedException
    {
        logger.debug("getOrCreateMapObject request for key: {}", key);

        IMapObject mapObject = mapObjectPersist.findMapObject(key);

        if (mapObject != null)
        {
            if (logger.isDebugEnabled()) logger.debug("   from DB: {}", mapObject.getMapObjectOptions());
            return mapObject;
        }

        logger.debug("   create from scratch: {}", mapObjectSpec);

        // If this is a device and the MO Options have not already been set, then set them from the plugin definition
        if (pluginDefinition instanceof IDevicePluginDefinition && mapObjectSpec.mapObjectOptions() == null)
        {
            mapObjectSpec = mapObjectSpec.toBuilder()
                    .mapObjectOptions(((IDevicePluginDefinition) pluginDefinition).getMapObjectOptions())
                    .build();
        }

        // On the off chance that this is a MO that is being recreated immediately after being deleted, we need
        // to handle the case where the BackgroundDeleteManager has not yet finished processing the deletion of
        // all the MOs associated data.  If we don't, then the following code can throw exceptions due to
        // uniqueness constraints on the database.

        backgroundDeleteManager.waitForDeletion(key);

        // Finally, actually construct the Map Object instance
        mapObject = constructMapObject(mapObjectSpec, pluginDefinition);

        // If this MO can have styled tracks, then create the TSD and CLD for it
        if (mapObject.canHaveStyledTrackData()) createTsdAndCld((IIconMapObject) mapObject);

        return mapObject;
    }

    /**
     * Call the constructor for the appropriate type of Map Object.
     *
     * @param mapObjectSpec    Map Object specification to use to construct the MO
     * @param pluginDefinition Definition controlling the construction
     * @return newly minted object
     * @since 2.29
     * @throws CreationFailedException If an exception occurs during construction of the MO
     */
    @Nonnull
    private IMapObject constructMapObject(MapObjectSpec mapObjectSpec, IPluginDefinition<?> pluginDefinition)
            throws CreationFailedException
    {
        final String key = pluginDefinition.getUniqueKey();
        final var constructor = MAP_OBJECT_CONSTRUCTORS.getOrDefault(key, DEFAULT_CONSTRUCTOR);

        try
        {
            final IMapObject mapObject = constructor.newInstance(mapObjectSpec, pluginDefinition);

            // Execute any supplied initializer now that we've created it.
            //
            // Note that this can not be done within the MapObject constructor since the initializer
            // might be operating on properties of a derived MO class, which will not have been fully
            // constructed at the time the base MO constructor is executing.

            final Consumer<IMapObject> initializer = mapObjectSpec.initializer();
            if (initializer != null) initializer.accept(mapObject);

            return mapObject;
        } catch (Exception e)
        {
            final String msg = "Failed to create MapObject: " + mapObjectSpec.getKey() + " of type "
                    + constructor.getDeclaringClass().getSimpleName();
            logger.error(msg, e);
            throw new CreationFailedException(msg, e);
        }
    }

    /**
     * Create the deferring TSD and CLD for the given Map Object.
     *
     * @param mapObject Map Object to process
     * @since 2.29
     */
    private void createTsdAndCld(IIconMapObject mapObject)
    {
        final IPersistService ps = persistServiceProvider.getPersistService();

        final ITrackStyleDefinition tsd = smartTrackManager.createDeferringTSD(mapObject);
        ps.persistEntity(tsd);
        mapObject.setTrackStyleDefinition(tsd);

        final IConnectingLineDefinition cld = smartTrackManager.createDeferringCLD(mapObject);
        ps.persistEntity(cld);
        mapObject.setConnectingLineDefinition(cld);
    }

    /**
     * Create a Map Object according to the given specification.
     * If a Map Object already exists, it is returned.
     *
     * @param mapObjectSpec The spec for the new map object
     * @return MapObject
     * @since 2.29
     * @throws CreationFailedException If an exception occurs during construction of the MO
     */
    @Nonnull
    private synchronized IMapObject internalCreateMapObject(@Nonnull MapObjectSpec mapObjectSpec) throws CreationFailedException
    {
        final String key = mapObjectSpec.getKey();

        logger.debug("internalCreateMapObject Key: {}", key);

        final IMapObjectController existingMoc = currentMapObjectControllers.get(key);

        if (existingMoc != null)
        {
            return existingMoc.getMapObject();
        } else
        {
            try
            {
                final IMapEntity mapEntity = mapObjectSpec.mapEntity();
                final String pluginFamily = mapEntity.getFamily();
                final String pluginType = mapEntity.getType();

                final var specDefinition = mapObjectSpec.pluginDefinition();
                final var pluginDefinition = specDefinition != null
                        ? specDefinition
                        : getOrCreatePluginDefinition(pluginFamily, pluginType);

                final IMapObject mapObject = createMapObject(key, mapObjectSpec, pluginDefinition);

                logger.debug("Created: {}", key);

                // Create the DPC.  The propagation handler will take care of completing the MO framework
                // if it is going to be propagated.

                dataPortal.createController(mapObject, dpc -> {
                    logger.debug("Init for propagate: {}", key);

                    // Don't persist the MO if we are in the process of restoring it from the DB
                    if (!mapObject.isRestoring()) mapObjectPersist.persistMapObject(mapObject);

                    createAndInitController(mapObject, pluginDefinition, dpc);

                    // Fire event first to give core priority so everything can be setup before notifying plugins.
                    mapObjectAddedEvent.fire(new MapObjectAdded(mapObject));

                    // And notify all the listeners
                    ExceptionUtils.safeDoOperation(managerListeners, l -> l.clientMapObjectAdded(mapObject));
                });

                if (!currentMapObjectControllers.containsKey(key))
                {
                    // For some reason we didn't create a controller
                    throw new CreationFailedException("Failed to create controller for " + key, null);
                } else
                {
                    return currentMapObjectControllers.get(key).getMapObject();
                }
            } catch (CreationFailedException e)
            {
                throw e;
            } catch (Exception e)
            {
                throw new CreationFailedException("Failed to create MapObject: " + mapObjectSpec.getKey(), e);
            }
        }
    }

    /**
     * Create the MapObjectController for the given Map Object and get it and the plugin controller initialized.
     *
     * @param mapObject        The Map Object for which to create a controller
     * @param pluginDefinition Plugin defining the MO
     * @param dpc              The DPC for the MO
     * @since 2.29
     */
    private void createAndInitController(IMapObject mapObject, IPluginDefinition<?> pluginDefinition, IRaptorDataPortalController dpc)
    {
        final String key = mapObject.getKey();

        IDevicePlugin mapObjectPlugin = pluginDefinition instanceof IDevicePluginDefinition devicePluginDefinition
                ? devicePluginDefinition.getInstance()
                : null;

        if (mapObjectPlugin == null) mapObjectPlugin = createPlugin(mapObject);

        final MapObjectController moc = createMapObjectController(dpc, mapObjectPlugin, mapObject);

        currentMapObjectControllers.put(key, moc);
        moId2mocMap.put(mapObject.getId(), moc);

        moc.initPluginController(); // Get the plugin controller initialized
    }

    /**
     * Get the plugin definition for the given family and type using {@link #getPluginDefinition(String, String)}.  If
     * one is not found, then generate a synthetic plugin definition.
     *
     * @return registered plugin definition or generated synthetic definition
     * @since 2.28
     */
    @Nonnull
    private IPluginDefinition<?> getOrCreatePluginDefinition(@Nonnull String family, @Nonnull String type)
    {
        var optionalDefinition = getPluginDefinition(family, type);

        if (optionalDefinition.isEmpty())
        {
            // In this case, we don't have a definition of any kind for the new MO.  So, we need to manufacture one.
            // This deals with cases where we get MO data from a remote system, and we don't have the plugin for
            // it locally.  It also handles our test code which likes to manufacture MOs with no associated plugin.
            //
            // Further, the default for anything unknown is to make it a Device.

            logger.debug("synthetic definition for {}/{}", family, type);

            final var pluginDefinition = pluginDefinitionFactory.syntheticDeviceDefinitionFor(family, type);

            // To avoid creating a new definition every time this family/type is used, add it to the registry
            syntheticPluginRegistry.addPlugin(pluginDefinition);

            return pluginDefinition;
        } else
        {
            return optionalDefinition.get();
        }
    }

    /**
     * Create the appropriate Map Object Controller for the given plugin type.
     *
     * @param dpc             Data portal controller
     * @param mapObjectPlugin Plugin defining the map object
     * @param mapObject       Map object being constructed
     * @return map object controller of appropriate type
     * @since 2.28
     */
    private MapObjectController createMapObjectController(IRaptorDataPortalController dpc, IDevicePlugin mapObjectPlugin, IMapObject mapObject)
    {
        return mapObject instanceof IFileMapObject
                ? new FileMapObjectController(mapObjectPlugin, (IFileMapObject) mapObject, dpc)
                : new MapObjectController(mapObjectPlugin, mapObject, dpc);
    }

    /**
     * Create the plugin instance for types unknown to the core.
     *
     * @param mapObject Map Object for which to create a plugin
     * @return plugin instance
     * @since 2.28
     */
    private IDevicePlugin createPlugin(IMapObject mapObject)
    {
        return manufacturedPlugins.computeIfAbsent(mapObject.getMapEntity().getUniqueKey(),
                k -> new SyntheticPlugin(mapObject.getFamily(), mapObject.getType()));
    }

    /**
     * Delete the given MOC, notifying listeners unless told not to.  Cleanup all state associated with the MOC.
     *
     * @param controller     MOC to delete
     * @param suppressEvents {@code true} if MapObjectRemoved and clientMapObjectRemoved events should not be fired
     */
    private void deleteMapObjectController(IMapObjectController controller, boolean suppressEvents)
    {
        if (controller != null)
        {
            final String key = controller.getKey();
            final IMapObject mo = controller.getMapObject();

            if (!suppressEvents)
            {
                // Fire event first to give core priority so everything can be setup before notifying plugins.
                mapObjectRemovedEvent.fire(new MapObjectRemoved(mo));

                ExceptionUtils.safeDoOperation(managerListeners, l -> l.clientMapObjectRemoved(mo));
            }

            if (selectedMapObject != null && selectedMapObject.getMapObject().equals(mo))
            {
                setSelected(null);  // Clear selection since we just deleted the selected map object
            }

            controller.shutdown();

            // Don't delete the entry from the map until we're all done since code invoked in the listeners
            // may call back into here looking for the controller.

            moId2mocMap.remove(mo.getId());
            currentMapObjectControllers.remove(key);
            persistedMapObjects.remove(key);
        }
    }

    @Override
    public boolean isVisibilityLocked(String key)
    {
        return forcedVisibilityMap.getOrDefault(key, false);
    }

    @Override
    public boolean setVisible(String key, boolean vis)
    {
        boolean wasPermittedAndSuccessful = true;

        if (!isVisibilityLocked(key))
        {
            reconfigureVisibility(key, vis);
        } else
        {
            //
            // State is locked and cannot be changed
            logger.info("Visibility state is now locked by the device plugin controller and cannot change by the user");
            wasPermittedAndSuccessful = false;
        }

        return wasPermittedAndSuccessful;
    }

    /**
     * Reconfigure the visibility on a map object and associated shapes.
     *
     * @param key     MOC key
     * @param visible visibility flag
     */
    private void reconfigureVisibility(String key, boolean visible)
    {
        final var mapObject = getMapObject(key);
        if (mapObject != null) mapObject.setVisible(visible);

        final String shapeKey = "_" + key + "_";

        currentMapObjectControllers.values().stream()
                .map(IMapObjectController::getMapObject)
                .filter(mo -> mo instanceof AShapeMapObject && mo.getUnitID().contains(shapeKey))
                .forEach(mo -> mo.setVisible(visible));
    }

    @Override
    public void setVisible(String key, boolean vis, boolean force)
    {
        reconfigureVisibility(key, vis);
        forcedVisibilityMap.put(key, force);
    }

    /**
     * The actual select listener for handling context menus on tracks
     *
     * @since 2.26
     */
    private static final class TrackSelectListener implements ITrackSelectListener
    {
        @Override
        public void onSelect(TrackSelectEvent event, List<IRaptorDataMessage> rdms)
        {
            if (TrackSelectEvent.TrackSelectAction.RIGHT_CLICK == event.getAction())
            {
                TrackPopupMenu menu = new TrackPopupMenu(rdms);
                final Component c = JFrame.getFrames()[0];
                final Point point = MouseInfo.getPointerInfo().getLocation();
                SwingUtilities.convertPointFromScreen(point, c);
                menu.show(c, point.x, point.y);
            }
        }
    }
}
