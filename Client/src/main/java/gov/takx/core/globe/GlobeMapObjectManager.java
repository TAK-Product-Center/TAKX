package gov.takx.core.globe;

import gov.nasa.worldwind.drag.Draggable;
import gov.takx.api.annotation.InvokeAndWaitOnEDT;
import gov.takx.api.annotation.InvokeOnEDT;
import gov.takx.api.annotation.PseudoBean;
import gov.takx.api.annotation.RemoveObserver;
import gov.takx.api.annotation.UiEventBus;
import gov.takx.api.event.EditorFailedEvent;
import gov.takx.api.event.InitializeDrawToolEvent;
import gov.takx.api.event.ShuttingDown;
import gov.takx.api.event.mapobject.MapObjectAdded;
import gov.takx.api.event.mapobject.MapObjectRemoved;
import gov.takx.api.globe.IExtendedGlobeManager;
import gov.takx.api.globe.IGlobeMapObject;
import gov.takx.api.globe.IGlobeMapObjectChangeListener;
import gov.takx.api.globe.IGlobeMapObjectEditor;
import gov.takx.api.globe.IGlobeMapObjectManager;
import gov.takx.api.globe.IGlobeMarkerMapObject;
import gov.takx.api.globe.IGlobeShapeMapObject;
import gov.takx.api.mapobject.IAnnotationMapObject;
import gov.takx.api.mapobject.IFlatAnnotationMapObject;
import gov.takx.api.mapobject.IIconMapObject;
import gov.takx.api.mapobject.IMapObject;
import gov.takx.api.mapobject.IMapObjectController;
import gov.takx.api.mapobject.IMapObjectManager;
import gov.takx.api.mapobject.IMarkerMapObject;
import gov.takx.api.mapobject.IMultiPolylineMapObject;
import gov.takx.api.mapobject.IRouteMapObject;
import gov.takx.api.mapobject.IShapeBullseyeMapObject;
import gov.takx.api.mapobject.IShapeMapObject;
import gov.takx.api.mapobject.MapToolType;
import gov.takx.api.platform.IProgressMonitor;
import gov.takx.api.preferences.IPreferenceMonitor;
import gov.takx.api.shapes.IShape;
import gov.takx.api.ui.event.MapObjectSelectedEvent;
import gov.takx.core.milsym.MilStd2525Service;
import gov.takx.core.project.ServiceOrdering;
import gov.takx.dataservices.shapes.PublishedShapeTracker;
import gov.takx.platform.KeyUtils;
import gov.takx.platform.preferences.PreferenceEntry;
import gov.takx.platform.preferences.UserPreferenceEntry;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.Priority;
import jakarta.enterprise.event.Observes;
import jakarta.inject.Inject;
import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.Cursor;
import java.beans.PropertyChangeEvent;
import java.lang.invoke.MethodHandles;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

import static jakarta.interceptor.Interceptor.Priority.PLATFORM_BEFORE;

/**
 * Service for creating and managing {@link IGlobeMapObject} instances associated with drawing tools and Map Objects.
 */
@PseudoBean
public class GlobeMapObjectManager implements IGlobeMapObjectManager
{
    private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    private static final List<String> COMPOUND_MO_TYPES = List.of(IMultiPolylineMapObject.MULTI_POLYLINE_TYPE);
    private static final String PLAYBACK_ICONS = "Playback Icons";
    private static final List<String> ALL_PROPERTIES = List.of(
            IMapObject.ALLOW_DRAG, IMapObject.LOCATION, IIconMapObject.CONTAINED, IMapObject.ICON, IMapObject.ICON_SCALE,
            IMapObject.HIGHEST_STATE, IMapObject.PLAYBACK_MODE);

    private final Map<String, Map<String, IGlobeMapObject>> globeMapObjects = new ConcurrentHashMap<>();
    private final Map<String, IGlobeMapObject> globeMapObjectsByKey = new ConcurrentHashMap<>();
    private final Map<String, IGlobeShapeMapObject> shapeGlobeMapObjects = new ConcurrentHashMap<>();
    private StateIconManager stateIconManager = new StateIconManager();
    private final List<IGlobeMapObjectChangeListener> globeMapObjectChangeListeners = new ArrayList<>();
    private final Set<IMapObject> pendingMapObjects = new HashSet<>();

    private IGlobeMapObjectEditor currentEditor;

    private boolean isWorldWindowLoaded;
    private IGlobeMapObject currentSelection;    // The currently selected globe map object

    private final IPreferenceMonitor<Double> iconScaleMonitor = UserPreferenceEntry.GLOBAL_ICON_SCALE.newDoubleMonitor(this::iconScaleChanged);
    private final IPreferenceMonitor<String> showMarkerToolTips = PreferenceEntry.MAP_OBJECT_TOOLTIPS.newStringMonitor(v -> recalculateTooltipStates());

    @Inject
    private IMapObjectManager mapObjectManager;

    @Inject
    private IExtendedGlobeManager globeManager;

    @Inject
    @UiEventBus
    private EventBus uiEventBus;

    private boolean clearingTool;

    @PostConstruct
    private void postConstruct()
    {
        for (IMapObjectController controller : mapObjectManager.getMapObjectControllers())
        {
            addMapObject(controller.getMapObject());
        }

        // We're created as part of the initial globe instantiation, so we don't need to iterate the globes here.
        // We'll get onGlobeCreated calls for every globe, including the primary.

        // Wire up event processing. This class only listens for events on the UI event bus
        uiEventBus.register(this);
    }

    /**
     * When shutting down, mouse usage will have been disabled by the time the {@link #onShutdown(IProgressMonitor)}
     * method below is called, leading to the user being unable to close any potential "insufficient control points"
     * dialog coming from an active editor.  So clear the current editor as soon as we know we're about to shut down.
     *
     * @since 5.8
     */
    private void onShuttingDown(@Observes ShuttingDown ignored)
    {
        clearCurrentEditor(true);
    }

    @RemoveObserver
    @Override
    public void onShutdown(IProgressMonitor progressMonitor)
    {
        clearCurrentEditor(true); // Dispose of any editor

        globeMapObjectChangeListeners.clear();
        stateIconManager = null;

        iconScaleMonitor.dispose();
        showMarkerToolTips.dispose();
        uiEventBus.unregister(this);

        mapObjectManager.getMapObjects().forEach(mapObject -> mapObject.removePropertyChangeListener(ALL_PROPERTIES, this));

        globeMapObjects.values().forEach(typeMap -> typeMap.values().forEach(IGlobeMapObject::peerRemoved));
        globeMapObjects.clear();
        globeMapObjectsByKey.clear();
        shapeGlobeMapObjects.clear();
    }

    /**
     * Notified by {@link IMapObjectManager} when a new map object is added.
     * <p/>
     * This class must be notified of new map objects before classes like {@link PublishedShapeTracker} are notified.
     * This is enforced by using a high priority for this observer. This higher priority can be adjusted slightly
     * if other services need to intercept the map object before we do here, such as {@link MilStd2525Service}
     * which is responsible for updating icons of newly created map objects if they have the appropriate MIL-SYM attributes.
     * <p/>
     * If more nuanced control is needed, we can create a set of priorities similar to {@link ServiceOrdering}.
     *
     * @param event The event providing the added map object
     * @since 2.38
     */
    private void onMapObjectAdded(@Observes @Priority(PLATFORM_BEFORE + 1) MapObjectAdded event)
    {
        addMapObject(event.getMapObject());
    }

    /**
     * Add a new map object to our context.
     *
     * @param mapObject The new map object
     * @since 2.38 - formerly clientMapObjectAdded
     */
    private void addMapObject(final IMapObject mapObject)
    {
        final String key = mapObject.getKey();

        logger.debug("Add MO: {}", mapObject);

        mapObject.addPropertyChangeListener(IMapObject.ALLOW_DRAG, this);

        final String family = mapObject.getFamily();
        final String type = mapObject.getType();

        if (isToolType(mapObject, IAnnotationMapObject.ANNOTATION_TYPE))
        {
            addGlobeMapObject(type, key, new GlobeAnnotationMapObject((IAnnotationMapObject) mapObject));
        } else if (isToolType(mapObject, IFlatAnnotationMapObject.FLAT_ANNOTATION_TYPE))
        {
            addGlobeMapObject(type, key, new GlobeFlatAnnotationMapObject((IFlatAnnotationMapObject) mapObject));
        } else if (KeyUtils.TOOL_FAMILY.equals(family) && MapToolType.isMapToolType(type))
        {
            addShapeMapObject((IShapeMapObject<?>) mapObject);
        }
        // Exclude compound MOs; nothing to render for them right now
        else if (!KeyUtils.COMPOUND_FAMILY.equals(family))
        {
            addGlobeMapObject(IIconMapObject.ICON_TYPE, key, new GlobeIconMapObject(mapObject));

            mapObject.addPropertyChangeListener(List.of(IIconMapObject.CONTAINED, IMapObject.ICON, IMapObject.ICON_SCALE,
                    IMapObject.HIGHEST_STATE, IMapObject.PLAYBACK_MODE), this);
        }
    }

    /**
     * Test if the given MapObject has family of {@link KeyUtils#TOOL_FAMILY} and the given {@code type}.
     *
     * @param mapObject Map Object to test
     * @param type      Required type
     * @return {@code true} if family and type are the given values
     * @since 3.0
     */
    private boolean isToolType(IMapObject mapObject, String type)
    {
        return mapObject.getFamily().equals(KeyUtils.TOOL_FAMILY) && mapObject.getType().equals(type);
    }

    private void addShapeMapObject(IShapeMapObject<?> mapObject)
    {
        if (mapObject.isRestoring())
        {
            logger.debug("Restoring shape: {}", mapObject);

            if (isWorldWindowLoaded)
            {
                addGlobeMapObject(mapObject.getType(), mapObject.getKey(), createGlobeShapeMapObject(mapObject));
            } else
            {
                pendingMapObjects.add(mapObject); // Add to pending list until WorldWindow is loaded
            }
        } else // New map tool from Shapes API or user
        {
            final IGlobeMapObject globeMapObject = getGlobeMapObject(mapObject.getKey());

            if (globeMapObject == null)
            {
                logger.debug("Creating shape from MO from API");

                addGlobeMapObject(mapObject.getType(), mapObject.getKey(), createGlobeShapeMapObject(mapObject));
            } else
            {
                logger.debug("Creating new shape from Tool");

                getCurrentMapTool().setMapObject(mapObject);
            }
        }
    }

    private IGlobeMapObject createGlobeShapeMapObject(IMapObject mapObject)
    {
        String mapToolType = mapObject.getType();
        if (MapToolType.DRAWING_RANGE_FAN.getShapeTypeName().equals(mapToolType))
        {
            return new GlobeRangeFanMapObject(mapObject);
        } else if (MapToolType.DRAWING_ROUTE.getShapeTypeName().equals(mapToolType))
        {
            return new GlobeRouteMapObject((IRouteMapObject) mapObject);
        } else if (MapToolType.DRAWING_BULLSEYE.getShapeTypeName().equals(mapToolType))
        {
            return new GlobeBullseyeMapObject((IShapeBullseyeMapObject) mapObject);
        } else
        {
            return new GlobeShapeMapObject(mapObject);
        }
    }

    /**
     * Handle a change to the global marker tooltip preference.
     * @since 5.7
     */
    private void recalculateTooltipStates()
    {
        applyMarkerUpdate(IIconMapObject.ICON_TYPE, IGlobeMarkerMapObject::recalculateTooltipState);
        applyMarkerUpdate(PLAYBACK_ICONS, IGlobeMarkerMapObject::recalculateTooltipState);
    }

    /**
     * Handle a change to global icon scale.
     */
    @InvokeOnEDT
    private void iconScaleChanged(double ignored)
    {
        if (!globeMapObjects.isEmpty())
        {
            applyMarkerUpdate(IIconMapObject.ICON_TYPE, IGlobeMarkerMapObject::refreshIcons);
            applyMarkerUpdate(PLAYBACK_ICONS, IGlobeMarkerMapObject::refreshIcons);
        }

        globeManager.repaintAllGlobes();
    }

    /**
     * Apply a given operation to all GMOs of a given type. This should only be called for types that implement
     * IGlobeMarkerMapObject (that is, map object markers and icons).
     *
     * @param type The map object type, or in the case of playback {@link #PLAYBACK_ICONS}
     * @since 5.7
     */
    private void applyMarkerUpdate(String type, Consumer<IGlobeMarkerMapObject> consumer)
    {
        final var gmoMap = globeMapObjects.get(type);
        if (gmoMap != null)
        {
            // This cast is safe since the only types that call this method use a GMO that implements IGlobeMarkerMapObject
            gmoMap.values().forEach(gmo -> consumer.accept((IGlobeMarkerMapObject) gmo));
        }
    }

    /**
     * Listen for changes on map objects and on the shape editor.
     *
     * @param propertyChangeEvent Event to handle
     */
    @Override
    public void propertyChange(@Priority(PLATFORM_BEFORE) PropertyChangeEvent propertyChangeEvent)
    {
        final String propertyName = propertyChangeEvent.getPropertyName();
        final Object source = propertyChangeEvent.getSource();

        final IMapObject mapObject = (IMapObject) source;
        final String key = mapObject.getKey();

        //noinspection EnhancedSwitchMigration
        switch (propertyName)
        {
            case IMapObject.ICON, IMapObject.ICON_SCALE:
            {
                var gmo = (IGlobeMarkerMapObject) getGlobeMapObject(key);
                if (gmo != null) gmo.refreshIcons();
                break;
            }
            case IMapObject.HIGHEST_STATE:
            {
                if (getGlobeMapObject(key) instanceof IGlobeMarkerMapObject globeMarkerMapObject)
                {
                    stateIconManager.changeState(globeMarkerMapObject, mapObject.getHighestState());
                }
                break;
            }
            case IMapObject.PLAYBACK_MODE:
                setupPlaybackMode(mapObject, mapObject.inPlaybackMode());
                break;
            case IMapObject.ALLOW_DRAG:
                if (getGlobeMapObject(key) instanceof Draggable draggable) draggable.setDragEnabled(mapObject.isAllowDrag());
                break;
            default:
                break; // Ignore others
        }

        globeManager.repaintAllGlobes(); // Get any visual change drawn immediately
    }

    /**
     * Notified by {@link IMapObjectManager} when a map object is removed.
     *
     * @param event The event providing the removed map object
     * @since 2.38 - formerly clientMapObjectRemoved
     */
    private void onMapObjectRemoved(@Observes MapObjectRemoved event)
    {
        IMapObject mapObject = event.getMapObject();
        String mapObjectType = mapObject.getType();
        final String mapObjectKey = mapObject.getKey();

        if (MapToolType.isMapToolType(mapObjectType))
        {
            // Ensure the shape is not being edited.
            if (isCurrentMapTool(mapObjectKey))
            {
                clearCurrentEditor(false);
            }
        } else if (mapObject instanceof IMarkerMapObject)
        {
            // All devices and markers use the same "group" type of ICON, not the specific device type
            mapObjectType = IIconMapObject.ICON_TYPE;
        }

        removeGlobeMapObject(mapObjectType, mapObjectKey);
    }

    public void addMapObjectChangeListener(IGlobeMapObjectChangeListener globeMapObjectChangeListener)
    {
        globeMapObjectChangeListeners.add(globeMapObjectChangeListener);
    }

    public void removeMapObjectChangeListener(IGlobeMapObjectChangeListener globeMapObjectChangeListener)
    {
        globeMapObjectChangeListeners.remove(globeMapObjectChangeListener);
    }

    @Override
    public void removeGlobeMapObjectForShape(@Nonnull IShape shape)
    {
        removeGlobeMapObject(shape.getType(), shape.getKey());
    }

    private void removeGlobeMapObject(@Nonnull String type, @Nonnull String key)
    {
        logger.debug("Remove GMO: type={} key={}", type, key);

        shapeGlobeMapObjects.remove(key);

        final boolean nonPlaybackType = !type.equals(PLAYBACK_ICONS);

        if (nonPlaybackType) globeMapObjectsByKey.remove(key);

        // TODO KMB: make sure this is still true when a custom editor is added
        // Nothing to show for the actual compound MO, so a globe MO was never added
        if (COMPOUND_MO_TYPES.contains(type)) return;

        final var globeMapObjectsOfType = globeMapObjects.get(type);

        // During normal operation, this should never be null.  But errors earlier on can result in a map never
        // being created.  So, a little defensive programming to avoid NPEs during shutdown.
        if (globeMapObjectsOfType != null)
        {
            var globeMapObject = globeMapObjectsOfType.remove(key);
            if (globeMapObject != null)
            {
                notifyGlobeMapObjectChangeListeners(globeMapObject, IGlobeMapObjectChangeListener.MapObjectChangeEvent.REMOVED);

                if (currentSelection == globeMapObject) currentSelection = null;    // Just deleted it

                globeMapObject.peerRemoved();   // Let it know so it can clean up
            } else
            {
                logger.debug("NULL globeMapObject for key: {}", key);
            }
        } else
        {
            // Only an error for non-playback types
            if (nonPlaybackType) logger.error("NULL globeMapObjectsOfType for type: {}, key={}", type, key);
        }
    }

    private void addGlobeMapObject(String type, String mapObjectKey, IGlobeMapObject globeMapObject)
    {
        logger.debug("Add GMO type={} key={}", type, mapObjectKey);

        if (globeMapObject instanceof IGlobeShapeMapObject globeShapeMapObject)
        {
            shapeGlobeMapObjects.put(mapObjectKey, globeShapeMapObject);
        }

        globeMapObjects.computeIfAbsent(type, k -> new ConcurrentHashMap<>()).put(mapObjectKey, globeMapObject);

        if (!type.equals(PLAYBACK_ICONS)) globeMapObjectsByKey.put(mapObjectKey, globeMapObject);

        notifyGlobeMapObjectChangeListeners(globeMapObject, IGlobeMapObjectChangeListener.MapObjectChangeEvent.ADDED);
    }

    @Nullable
    @Override
    public IGlobeMapObject getGlobeMapObject(@Nonnull String key, boolean forPlayback)
    {
        logger.trace("looking for globe map object for {} forPlayback {}", key, forPlayback);

        if (forPlayback)
        {
            final var objectMap = globeMapObjects.get(PLAYBACK_ICONS);
            return objectMap != null ? objectMap.get(key) : null;
        } else
        {
            return globeMapObjectsByKey.get(key);
        }
    }

    @Override
    @Nonnull
    public Collection<IGlobeShapeMapObject> getShapeMapObjects()
    {
        return Collections.unmodifiableCollection(shapeGlobeMapObjects.values());
    }

    private boolean isCurrentMapTool(String key)
    {
        return currentEditor != null && currentEditor.isArmed() && currentEditor.isGlobeMapObject(key);
    }

    private IGlobeShapeMapObject getCurrentMapTool()
    {
        final IGlobeMapObject editorObject = currentEditor != null ? currentEditor.getGlobeMapObject() : null;
        return (editorObject instanceof IGlobeShapeMapObject globeShapeMapObject)
                ? globeShapeMapObject : null;
    }

    @Override
    public IGlobeMapObjectEditor getCurrentEditor()
    {
        return currentEditor;
    }

    @Override
    @InvokeOnEDT
    public void worldWindowLoaded()
    {
        isWorldWindowLoaded = true;

        // finish processing our pending objects
        pendingMapObjects.forEach(mo -> addGlobeMapObject(mo.getType(), mo.getKey(), createGlobeShapeMapObject(mo)));

        pendingMapObjects.clear();
    }

    /**
     * Handle a map object selection event.
     *
     * @param event Event to process
     */
    @SuppressWarnings("UnusedDeclaration")
    @Subscribe
    public void selectionChanged(MapObjectSelectedEvent event)
    {
        // There is a race condition where we might get an event callback from the EDT after the project has essentially
        // been shutdown.  So, catch that case and avoid NPEs.

        if (globeMapObjects.isEmpty()) return;

        IMapObjectController mapObjectController = event.getMapObjectController();
        IGlobeMapObject globeMapObject = null;

        logger.debug("SelectionChanged: MOC= {}", mapObjectController);

        if (mapObjectController != null)
        {
            final IMapObject mapObject = mapObjectController.getMapObject();
            final String type = mapObject.getType();

            globeMapObject = globeMapObjectsByKey.get(mapObject.getKey());

            try
            {
                if (type.equals(IAnnotationMapObject.ANNOTATION_TYPE))
                {
                    clearCurrentEditor(false);
                    if (globeMapObject.isViewable())
                    {
                        installEditor(new GlobeAnnotationEditor(), globeMapObject, false);
                    }
                } else if (type.equals(IFlatAnnotationMapObject.FLAT_ANNOTATION_TYPE))
                {
                    clearCurrentEditor(false);
                    if (globeMapObject.isViewable())
                    {
                        installEditor(new GlobeFlatAnnotationEditor(), globeMapObject, false);
                    }
                } else if (MapToolType.isMapToolType(type))
                {
                    if (logger.isTraceEnabled()) logger.trace("CurrentMapTool: {}", getCurrentMapTool());

                    // Check if this is different map tool than currently selected
                    if (!isCurrentMapTool(mapObject.getKey()))
                    {
                        clearCurrentEditor(false);

                        IGlobeShapeMapObject globeShapeMapObject = (IGlobeShapeMapObject) globeMapObject;
                        MapToolType mapToolType = globeShapeMapObject.getMapToolType();

                        // Target this editor at the last clicked globe since the focus may not have transferred yet
                        final IGlobeMapObjectEditor editor = makeEditorForMapTool(mapToolType, false);
                        installEditor(editor, globeShapeMapObject, false); // Shape is not new
                    }
                } else
                {
                    clearCurrentEditor(false);
                }

                // globeMapObject can be null during certain shutdown sequences (depending on timing)
                if (globeMapObject != null)
                {
                    globeMapObject.setSelected(true);
                }
            } catch (NullPointerException e)
            {
                logger.error("Attempt to access a globe map object with a null key", e);
            }
        } else
        {
            clearCurrentEditor(false);
        }

        setSelected(globeMapObject);
    }

    @Override
    public void cancelMapTool()
    {
        logger.debug("cancelMapTool");
        clearCurrentEditor(true);
        globeManager.setCursorOnAllGlobes(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
    }

    @Override
    public IGlobeMapObject setMapTool(@Nonnull MapToolType mapToolType, boolean drawOnly)
    {
        logger.debug("SetMapTool called for type {}", mapToolType == MapToolType.NONE ? "NONE" : mapToolType);

        // stop recursive calls to this method that can happen when clearCurrentEditor() calls disarm()
        if (clearingTool) return null;

        clearingTool = mapToolType == MapToolType.NONE;

        clearCurrentEditor(false);

        IGlobeMapObject globeMapObject = null;

        if (!isWorldWindowLoaded)
        {
            logger.error("WorldWindow not loaded yet!");
        } else if (clearingTool) // If we are clearing the tool, then reset the cursor
        {
            globeManager.setCursorOnAllGlobes(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
        } else
        {
            // Now construct the globe MO and hand it off to the editor
            globeMapObject = switch (mapToolType)
            {
                case DRAWING_RANGE_FAN -> new GlobeRangeFanMapObject();
                case DRAWING_BULLSEYE -> new GlobeBullseyeMapObject();
                case DRAWING_ROUTE -> new GlobeRouteMapObject();
                case DRAWING_ANNOTATION -> new GlobeAnnotationMapObject(null);
                case DRAWING_FLAT_ANNOTATION -> new GlobeFlatAnnotationMapObject(null);
                default -> new GlobeShapeMapObject(mapToolType);
            };

            globeMapObject.setSelected(true);
            installEditor(makeEditorForMapTool(mapToolType, drawOnly), globeMapObject, true);
        }

        clearingTool = false;

        return globeMapObject;
    }

    /**
     * Construct the appropriate editor for the given tool type.
     *
     * @param mapToolType Tool type for which to instantiate the editor
     * @param drawOnly Pass {@code true} if the shape should be drawn only, and not create a Map Object
     * @return editor for the given type
     * @since 3.5
     */
    private IGlobeMapObjectEditor makeEditorForMapTool(MapToolType mapToolType, boolean drawOnly)
    {
        if (mapToolType == MapToolType.DRAWING_ROUTE)
        {
            return new GlobeRouteMapObjectEditor();
        } else if (mapToolType == MapToolType.DRAWING_ANNOTATION)
        {
            return new GlobeAnnotationEditor();
        } else if (mapToolType == MapToolType.DRAWING_FLAT_ANNOTATION)
        {
            return new GlobeFlatAnnotationEditor();
        } else
        {
            return new GlobeShapeMapObjectEditor(drawOnly);
        }
    }

    /**
     * Install, configure, and arm the given editor as the current editor.
     *
     * @param editor The editor to install
     * @param gmo The globe shape MO to give to the editor
     * @param isNew Pass {@code true} if this is a newly created globe map object
     */
    private void installEditor(IGlobeMapObjectEditor editor, IGlobeMapObject gmo, boolean isNew)
    {
        currentEditor = editor;
        currentEditor.setGlobeMapObject(gmo, isNew);
        currentEditor.arm();
        if (gmo instanceof IGlobeShapeMapObject gsmo)
        {
            if (isNew)
            {
                final String mapToolString = gsmo.getMapToolType().toString();
                addGlobeMapObject(mapToolString, gsmo.requireShapeModel().getKey(), gsmo);
                uiEventBus.post(new InitializeDrawToolEvent(gsmo.getShapeModel()));
            }
        }
    }

    /**
     * Disarm and dispose of the current editor, if any.
     *
     * @param noUpdate Pass {@code true} to suppress updates when the editor is disarmed
     */
    @InvokeAndWaitOnEDT
    private void clearCurrentEditor(boolean noUpdate)
    {
        logger.trace("Clearing editor, noUpdate {}, currentEditor {}", noUpdate, currentEditor);

        if (currentEditor != null)
        {
            if (currentEditor.isArmed()) currentEditor.disarm(noUpdate);
            currentEditor = null;
        }
    }

    /**
     * Set the playback mode for a given MO.  If playbackMode is true, then we add the playback icon.  If it is
     * false, we remove the playback icon.
     * <p>
     * All our work needs to be done on the EDT since we interact heavily with the UI
     *
     * @param mapObject    The MO of interest
     * @param playbackMode Pass {@code true} if the device is in playback mode, false otherwise
     */
    @InvokeOnEDT
    private void setupPlaybackMode(IMapObject mapObject, boolean playbackMode)
    {
        final String key = mapObject.getKey();

        if (playbackMode)
        {
            // device is in playback mode, start displaying it on the globe
            addGlobeMapObject(PLAYBACK_ICONS, key, new GlobePlaybackIconMapObject(mapObject));
            logger.debug("Playback Map Object added: {}", mapObject);
        } else
        {
            removeGlobeMapObject(PLAYBACK_ICONS, key);
            logger.debug("Playback Map Object Removed: {}", mapObject);
        }
    }

    /**
     * Set the currently selected globe map object.  Deselect any currently selected object.
     *
     * @param selectedGlobeMapObject GlobeMapObject to be selected, {@code null} to clear selection
     */

    private void setSelected(IGlobeMapObject selectedGlobeMapObject)
    {
        if (selectedGlobeMapObject != currentSelection)
        {
            if (currentSelection != null)
            {
                currentSelection.setSelected(false);  // Deselect it
            }

            if (selectedGlobeMapObject != null)
            {
                selectedGlobeMapObject.setSelected(true);
            }

            currentSelection = selectedGlobeMapObject;

            globeManager.repaintAllGlobes(); // Always repaint when the selection changes
        }
    }

    private void notifyGlobeMapObjectChangeListeners(IGlobeMapObject globeMapObject, IGlobeMapObjectChangeListener.MapObjectChangeEvent mapObjectChangeEvent)
    {
        for (IGlobeMapObjectChangeListener globeMapObjectListener : globeMapObjectChangeListeners)
        {
            globeMapObjectListener.globeMapObjectChanged(globeMapObject, mapObjectChangeEvent);
        }
    }

    /**
     *  Receives a {@link EditorFailedEvent}, and removes the corresponding globe map object if there is no controller for the map object.
     *
     * @param failedEvent Event fired by a disarming editor if the creation failed.
     */
    @Subscribe
    @SuppressWarnings("unused")
    public void editorFailedEvent(EditorFailedEvent failedEvent)
    {
        String mapObjectKey = failedEvent.mapObjectKey();
        if (mapObjectKey != null)
        {
            logger.trace("Received editor failed event from editor for map object : {}", mapObjectKey);
            // If there is no controller for the object, remove it from the globe map objects
            if (mapObjectManager.getMapObjectController(mapObjectKey) == null)
            {
                removeGlobeMapObject(KeyUtils.getTypeFromKey(mapObjectKey), mapObjectKey);
            }
        }
    }
}
