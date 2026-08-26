package gov.takx.core.globe;

import com.atakmap.map.elevation.ElevationManager;
import gov.nasa.worldwind.Movable;
import gov.nasa.worldwind.avlist.AVKey;
import gov.nasa.worldwind.avlist.AVList;
import gov.nasa.worldwind.drag.Draggable;
import gov.nasa.worldwind.event.SelectEvent;
import gov.nasa.worldwind.event.SelectListener;
import gov.nasa.worldwind.geom.LatLon;
import gov.nasa.worldwind.geom.Position;
import gov.nasa.worldwind.geom.Sector;
import gov.nasa.worldwind.pick.PickedObject;
import gov.nasa.worldwind.pick.PickedObjectList;
import gov.nasa.worldwind.render.AbstractShape;
import gov.nasa.worldwind.render.PointPlacemark;
import gov.nasa.worldwind.util.BasicDragger;
import gov.tak.api.maps.coords.GeoPoint;
import gov.tak.api.util.StandardAttributeNames;
import gov.takx.api.annotation.PseudoBean;
import gov.takx.api.annotation.UiEventBus;
import gov.takx.api.coordinatesystem.ExtendedLocation;
import gov.takx.api.coordinatesystem.RaptorLocation;
import gov.takx.api.globe.IApplicationGlobe;
import gov.takx.api.globe.IExtendedGlobeManager;
import gov.takx.api.globe.IGlobeClickService;
import gov.takx.api.globe.IGlobeMapObject;
import gov.takx.api.globe.IGlobeMarkerMapObject;
import gov.takx.api.globe.IHighlightable;
import gov.takx.api.globe.IWorldWindowGL;
import gov.takx.api.globe.render.IRendererSearchResult;
import gov.takx.api.globe.render.ISearchablePickedObject;
import gov.takx.api.mapobject.ElevationMode;
import gov.takx.api.mapobject.ICoreMapObjectManager;
import gov.takx.api.mapobject.IIconMapObject;
import gov.takx.api.mapobject.IMapObject;
import gov.takx.api.mapobject.IMapObjectController;
import gov.takx.api.mapobject.IMapObjectSelectListener;
import gov.takx.api.mapobject.ITrackSelectListener;
import gov.takx.api.mapobject.MapObjectSelectEvent;
import gov.takx.api.mapobject.TrackSelectEvent;
import gov.takx.api.messages.IRaptorDataMessage;
import gov.takx.api.persist.IMapObjectPersist;
import gov.takx.api.persist.IPersistServiceProvider;
import gov.takx.api.platform.IProgressMonitor;
import gov.takx.api.platform.IRaptorShutdownListener;
import gov.takx.api.plugin.device.IDevicePluginDefinition;
import gov.takx.api.registry.IDevicePluginRegistry;
import gov.takx.api.ui.event.MapObjectSelectedEvent;
import gov.takx.commons.swing.ErrorReportingSwingWorker;
import gov.takx.core.mapdata.MapDataUtils;
import gov.takx.core.mapdata.MapPoint;
import gov.takx.core.mapdata.RaptorDataMessageTableModel;
import gov.takx.core.mapdata.gui.MapDataSummaryDialog;
import gov.takx.core.mapobject.gui.GlobeMapObjectSelectionDialog;
import gov.takx.core.mapobject.gui.MapObjectTrackSelectionDialog;
import gov.takx.platform.preferences.PreferenceEntry;
import gov.takx.platform.utils.WWUtils;
import jakarta.annotation.PostConstruct;
import jakarta.inject.Inject;
import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.JFrame;
import javax.swing.SwingUtilities;

import java.awt.Component;
import java.awt.Cursor;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.lang.invoke.MethodHandles;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 */
@SuppressWarnings("CdiManagedBeanInconsistencyInspection")
@PseudoBean
public class WWSelectListener extends MouseAdapter implements SelectListener, IRaptorShutdownListener
{
    private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    private final IWorldWindowGL wwd;
    private IApplicationGlobe globe;
    private BasicDragger dragger;
    private boolean armed;
    private IGlobeInteractable lastToolTipItem;
    private IHighlightable lastHighlightedObject;

    private Cursor previousCursor; // Saved cursor while mouse is over a movable object

    private boolean mouseDown;
    private MapDataSummaryDialog mapDataSummaryDialog;

    @Inject
    private ICoreMapObjectManager mapObjectManager;

    @Inject
    private IPersistServiceProvider persistServiceProvider;

    @Inject
    private IMapObjectPersist mapObjectPersist;

    @Inject
    private IExtendedGlobeManager globeManager;

    @Inject
    private MapDataUtils mapDataUtils;

    @Inject
    private IDevicePluginRegistry devicePluginRegistry;

    @Inject
    private IGlobeClickService globeClickService;

    @Inject
    @UiEventBus
    private EventBus uiEventBus;

    /**
     * Listeners to map object select events
     */
    private final List<IMapObjectSelectListener> selectListeners = new CopyOnWriteArrayList<>();

    /**
     * Constructor.
     *
     * @param wwd     World window to listen on
     * @param dragger drag handler to use
     */
    public WWSelectListener(IWorldWindowGL wwd, BasicDragger dragger)
    {
        this.wwd = wwd;
        armed = true;
        this.dragger = dragger;
    }

    @PostConstruct
    private void postConstruct()
    {
        globe = globeManager.getGlobeForComponent(wwd);
        uiEventBus.register(this);
    }

    @Override
    public void onShutdown(IProgressMonitor progressMonitor)
    {
        dragger = null;

        if (mapDataSummaryDialog != null)
        {
            mapDataSummaryDialog.dispose();
            mapDataSummaryDialog = null;
        }

        selectListeners.clear();
        uiEventBus.unregister(this);
    }

    /**
     * Handle selection change events that result from a map object being selected.  Since we are already the
     * authoritative source on the selection events that need to be communicated to listeners, it makes sense to
     * handle the notification of an actual selection of a map object here as well.
     * <p>
     * Note that in the case of deselection, an empty list will be sent out.
     *
     * @param event the MapObjectSelectedEvent
     */
    @SuppressWarnings("unused")
    @Subscribe
    public void selectionChanged(MapObjectSelectedEvent event)
    {
        IMapObjectController controller = event.getMapObjectController();
        final List<IMapObject> list = controller != null
                ? Collections.singletonList(controller.getMapObject())
                : Collections.emptyList();

        notifyListeners(new MapObjectSelectEvent(globe.getGlobeId(), MapObjectSelectEvent.SELECTED, list));
    }

    @Override
    public void mousePressed(MouseEvent mouseEvent)
    {
        if (mouseEvent.isConsumed())
        {
            return;
        }

        //if in selection mode AND terrain is the only objects at current position, then deselect all map objects
        if (armed && wwd.getObjectsAtCurrentPosition() != null && wwd.getObjectsAtCurrentPosition().size() < 2)
        {
            mapObjectManager.setSelected(null);
        }
        mouseDown = true;
    }

    @Override
    public void mouseReleased(MouseEvent mouseEvent)
    {
        mouseDown = false;
    }

    /**
     * Set if this select listener will act on events received.  If false, then no click or hover events will be
     * processed.
     *
     * @param armed Pass {@code true} to enable this select listener, {@code false} to disable it
     */
    public void setArmed(boolean armed)
    {
        this.armed = armed;
    }

    /**
     * @return {@code true} if this listener is armed
     * @since 5.4
     */
    public boolean isArmed()
    {
        return armed;
    }

    /**
     * This event handler is called for certain actions on the globe.
     * The events are:
     * LEFT_CLICK, LEFT_DOUBLE_CLICK, RIGHT_CLICK, HOVER, ROLLOVER, DRAG_END, DRAG
     *
     * @param event that was triggered
     */
    @Override
    public void selected(SelectEvent event)
    {
        if (!armed) return;

        if (event.isConsumed()) return;

        MouseEvent mouseEvent = event.getMouseEvent();
        if (mouseEvent != null && mouseEvent.isConsumed())
        {
            return;
        }

        // We don't want random picked objects (supplied by plugins) to occlude the GMMO, so see
        // if there's a GMMO in the list that is marked "on top".
        final PickedObject topGMMO = findPickedIcon(event.getObjects(), true);

        final PickedObject topPickedObject = topGMMO != null ? topGMMO : event.getTopPickedObject();
        final Object topObject = topPickedObject != null ? topPickedObject.getObject() : null;

        final Component wwdComponent = wwd.getComponent();

        //  If something movable selected, show a hand cursor
        if (isMovable(topObject) || (topPickedObject != null && topPickedObject.getValue(AVKey.URL) != null))
        {
            // If we haven't already saved it, grab the current cursor so we can restore it when the mouse moves
            // away from the active object.

            if (previousCursor == null)
            {
                previousCursor = wwdComponent.getCursor();
            }
            wwdComponent.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        } else if (previousCursor != null)
        {
            // Since we previously changed the cursor, we need to restore it now.
            wwdComponent.setCursor(previousCursor);
            previousCursor = null;
        }

        if (event.isLeftPress())
        {
            handleLeftPress(event, topPickedObject);
        } else if (event.isLeftClick())
        {
            handleLeftClick(event, topPickedObject);
        } else if (event.isLeftDoubleClick())
        {
            handleLeftDoubleClick(event, topPickedObject);
        } else if (event.isRightClick())
        {
            handleRightClick(event, topPickedObject);
        } else if (event.isHover())
        {
            handleHover(event);
        } else if (event.isRollover())
        {
            handleRollover(event);
        } else if (event.isDrag() || event.isDragEnd())
        {
            handleDragEvents(event);
        }

        notifyListeners(event);
    }

    public void addListener(IMapObjectSelectListener listener)
    {
        selectListeners.add(listener);
    }

    public void removeListener(IMapObjectSelectListener listener)
    {
        selectListeners.remove(listener);
    }

    /**
     * Notify IMapObjectSelectListeners of top picked map object (shapes excluded), if there is one.  Include the
     * affected map object in the event, unless the event is a rollover or hover OFF of a map object, in which case
     * we include an empty list.
     *
     * @param event WW SelectEvent
     */
    private void notifyListeners(SelectEvent event)
    {
        final Object topObject = event.getTopObject();
        if (topObject instanceof IGlobeMarkerMapObject)
        {
            IMapObject mo = ((IGlobeMarkerMapObject) topObject).getMapObject();
            if (mo != null)
            {
                notifyListeners(new MapObjectSelectEvent(globe.getGlobeId(), event, Collections.singletonList(mo)));
            }
        } else
        {
            notifyListeners(new MapObjectSelectEvent(globe.getGlobeId(), event, Collections.emptyList()));
        }
    }

    /**
     * Notify listeners of the given MapObjectSelectEvent.
     *
     * @param selectEvent Event to fire to listeners
     * @since 2.37
     */
    private void notifyListeners(MapObjectSelectEvent selectEvent)
    {
        for (IMapObjectSelectListener listener : selectListeners)
        {
            try
            {
                listener.selected(selectEvent);
            } catch (Exception e)
            {
                logger.error("PLUGIN EXCEPTION handling a MapObjectSelectEvent: {}", selectEvent.getEventAction(), e);
            }
        }
    }

    private void handleLeftPress(SelectEvent event, PickedObject topPickedObject)
    {
        // We only want to change selection when no modifier keys are also active during the mouse event
        //
        // I think that we are performing a selection on a press as well as a click events so that when a user drags an
        // object it is selected and shows in the properties panel.  However, without this test for modifiers, editing
        // shapes using a Ctrl+click and accidentally clicking on a different shape causes an instant switch to the
        // shape being edited and the addition of a control point.  Which is most likely not what the user intended.

        final MouseEvent mouseEvent = event.getMouseEvent();
        final boolean anyModifierKey = mouseEvent.isAltDown() || mouseEvent.isControlDown() || mouseEvent.isShiftDown();

        if (event.hasObjects() && !anyModifierKey)
        {
            Object selected = topPickedObject.getObject();
            highlight(null);

            // TODO: Work around until we wrap WW objects in a GMO
            // If the object implements AVList but not IGlobeMapObject, attempt to retrieve its associated GMO
            // via the key IGlobeMapObject.class.getName(). Note that it is possible for an object to implement
            // both AVList and IGlobeMapObject. Rx classes which implement and wrap WW classes may encounter
            // this case (for example, see GlobeIconMapObject).
            if (selected instanceof AVList && !(selected instanceof IGlobeMapObject))
            {
                selected = ((AVList) selected).getValue(IGlobeMapObject.class.getName());
            }

            if (selected instanceof IGlobeMapObject globeMapObject)
            {
                final IMapObject mapObject = globeMapObject.getMapObject();
                if (mapObject != null && mapObject.isSelectable())
                {
                    mapObjectManager.setSelected(mapObject.getKey());
                }
            }
        }
    }

    private void handleLeftClick(SelectEvent event, PickedObject topPickedObject)
    {
        if (event.hasObjects())
        {
            final Object pickedObject = topPickedObject != null ? topPickedObject.getObject() : null;

            // Get any selectable map objects
            List<IGlobeMapObject> mapObjectList = event.getObjects().stream()
                    .map(PickedObject::getObject)
                    .filter(object -> object instanceof IGlobeMapObject gmo && gmo.getMapObject() != null)
                    .map(IGlobeMapObject.class::cast)
                    .distinct()
                    .sorted(Comparator.comparing(o -> o.getMapObject().getName())).toList();

            Object selected;
            if (mapObjectList.size() > 1)
            {
                // Filter out unselectable objects
                mapObjectList = mapObjectList.stream().filter(gmo -> gmo.getMapObject().isSelectable()).toList();
                final int count = mapObjectList.size();
                selected = count > 1
                        ? new GlobeMapObjectSelectionDialog<>(mapObjectList).getSelectedObject()
                        : count == 1 ? mapObjectList.get(0) : pickedObject;
            } else
            {
                selected = pickedObject;
            }

            // TODO: Work around until we wrap WW objects in a GMO
            // If the object implements AVList but not IGlobeMapObject, attempt to retrieve its associated GMO
            // via the key IGlobeMapObject.class.getName(). Note that it is possible for an object to implement
            // both AVList and IGlobeMapObject. Rx classes which implement and wrap WW classes may encounter
            // this case (for example, see GlobeIconMapObject).
            if (selected instanceof AVList && !(selected instanceof IGlobeMapObject))
            {
                // Check for null value before overriding.
                Object overrideSelected = ((AVList) selected).getValue(IGlobeMapObject.class.getName());
                selected = overrideSelected == null ? selected : overrideSelected;
            }

            if (selected instanceof IGlobeMapObject globeMapObject)
            {
                final var mapObject = globeMapObject.getMapObject();

                // When initially creating a 3D shape, the shape will not have an associated MO until the user
                // completes the edit (clicks away from the shape).  If they click on the shape before that, then
                // the MO is null.
                if (mapObject != null && mapObject.isSelectable())
                {
                    final var mouseEvent = event.getMouseEvent();

                    // center the globe on the selected object, if configured
                    if (PreferenceEntry.GLOBE_RADIAL_CENTER_ON_CLICK.getBooleanValue())
                    {
                        final var location = globeMapObject.getLocation(); // This accounts for playback location
                        final double latitude = location.getLatitude();
                        final double longitude = location.getLongitude();
                        final var loc = mapObject.isSnapIconToSurface()
                                ? new GeoPoint(latitude, longitude, ElevationManager.getElevation(latitude, longitude, null))
                                : location;
                        globe.goToAndTranslateMouseEvent(loc, mouseEvent);
                    }

                    mapObjectManager.setSelected(mapObject.getKey());
                    mapObject.getController().createPopupMenu(mouseEvent); // show the menu
                }
            } else if (supportsSearch(topPickedObject, selected))
            {
                handleTrackSelectEvent(topPickedObject, selected, TrackSelectEvent.TrackSelectAction.CLICK);
            }
        }
    }

    private void handleLeftDoubleClick(SelectEvent event, PickedObject topPickedObject)
    {
        if (event.hasObjects())
        {
            Object selected = topPickedObject != null ? topPickedObject.getObject() : null;

            // TODO: Work around until we wrap WW objects in a GMO
            // If the object implements AVList but not IGlobeMapObject, attempt to retrieve its associated GMO
            // via the key IGlobeMapObject.class.getName(). Note that it is possible for an object to implement
            // both AVList and IGlobeMapObject. Rx classes which implement and wrap WW classes may encounter
            // this case (for example, see GlobeIconMapObject).

            if (selected instanceof AVList)
            {
                // TODO: I think this replacement of the selected object can go away.  All of our objects seem to have a GMO now.
                // Avoid nuking the "selected" value unless this AVList entry exists and is non-null
                final Object value = ((AVList) selected).getValue(IGlobeMapObject.class.getName());
                if (value != null) selected = value;
            }

            if (selected instanceof IGlobeMapObject globeMapObject)
            {
                final IMapObject mapObject = globeMapObject.getMapObject();
                if (mapObject != null && mapObject.isSelectable())
                {
                    mapObject.getController().showPluginDialog();
                }
            }

            if (!(selected instanceof IGlobeMarkerMapObject) && supportsSearch(topPickedObject, selected))
            {
                handleTrackSelectEvent(topPickedObject, selected, TrackSelectEvent.TrackSelectAction.DOUBLE_CLICK);
            }
        }
    }

    /**
     * Determine if an object supports a mechanism to find a track search area.  This includes any PickedObject
     * that implements {@link ISearchablePickedObject} and PointPlacemarks (if they have an associated map point).
     *
     * @param pickedObject The PickedObject
     * @param o            The PickedObject's object
     * @return {@code true} if the object can provide a search area, {@code false} otherwise
     */
    private boolean supportsSearch(PickedObject pickedObject, Object o)
    {
        return pickedObject instanceof ISearchablePickedObject || o instanceof PointPlacemark;
    }

    /**
     * Get the search sector from the selected object.  Currently only {@link PointPlacemark} objects are supported,
     * and their search sector is defined by the associated {@link MapPoint}, if any.
     *
     * @param searchObject The object that was clicked on, center of the search
     * @return The sector covering the search area
     */
    private Sector getSearchSectorForObject(Object searchObject)
    {
        if (searchObject instanceof PointPlacemark)
        {
            // See if this placemark has an associated marker position, if so use that
            final MapPoint mapPoint = (MapPoint) ((PointPlacemark) searchObject).getValue(MapPoint.AVKEY_ASSOCIATED_MARKER);

            if (mapPoint != null)
            {
                return mapPoint.getSearchSector(wwd.getModel().getGlobe());
            }
        }

        return null;
    }

    private void handleTrackSelectEvent(PickedObject pickedObject, final Object selected, final TrackSelectEvent.TrackSelectAction action)
    {
        // Find the selected track points, and fire calls to the registered listeners
        new ErrorReportingSwingWorker<IRendererSearchResult, Object>("Error getting map data", "Unable to retrieve map data")
        {
            @Override
            protected IRendererSearchResult doInBackground()
            {
                try
                {
                    final ISearchablePickedObject searchablePickedObject = pickedObject instanceof ISearchablePickedObject
                            ? (ISearchablePickedObject) pickedObject
                            : null;

                    IRendererSearchResult mapDataResult = searchablePickedObject != null
                            ? searchablePickedObject.searchForRdms()
                            : mapDataUtils.getViewableMessagesForArea(getSearchSectorForObject(selected));

                    if (mapDataResult == null) return null;

                    for (Map.Entry<IMapObject, List<IRaptorDataMessage>> entry : mapDataResult.getData().entrySet())
                    {
                        IMapObject mapObject = entry.getKey();

                        TrackSelectEvent selectEvent = new TrackSelectEvent(mapObject, action);

                        var trackListeners = mapObject.getController().getSelectListeners();

                        if (!trackListeners.isEmpty())
                        {
                            for (ITrackSelectListener listener : trackListeners)
                            {
                                scheduleTrackListenerCallback(listener, selectEvent, entry.getValue());
                            }
                        }
                    }

                    //noinspection DataFlowIssue - getMapObjectController will always return non-null in this usage
                    final IMapObject mapObject = searchablePickedObject != null
                            ? mapObjectManager.getMapObjectController(searchablePickedObject.getMapObjectId()).getMapObject()
                            : null;

                    if (mapObject != null)
                    {
                        TrackSelectEvent selectEvent = new TrackSelectEvent(mapObject, action);

                        // Merge all the selected results into one list
                        final List<IRaptorDataMessage> allRdms = new ArrayList<>(mapDataResult.getMapPointCount());
                        mapDataResult.getData().values().forEach(allRdms::addAll);

                        for (ITrackSelectListener listener : mapObjectManager.getTrackSelectListeners())
                        {
                            scheduleTrackListenerCallback(listener, selectEvent, allRdms);
                        }
                    }

                    return mapDataResult;
                } finally
                {
                    persistServiceProvider.cleanupPersistService();
                }
            }

            @Override
            protected void onSuccess(IRendererSearchResult searchResult)
            {
                if (action == TrackSelectEvent.TrackSelectAction.DOUBLE_CLICK && searchResult != null)
                {
                    Map<IMapObject, List<IRaptorDataMessage>> mapObjectMessageMap = searchResult.getData();
                    final int mapSize = mapObjectMessageMap.size();
                    IMapObject selection = null;
                    boolean cancelled = false;

                    if (mapSize > 1)
                    {
                        MapObjectTrackSelectionDialog dialog = new MapObjectTrackSelectionDialog(mapObjectMessageMap);
                        dialog.setVisible(true);

                        if (dialog.cancelled())
                        {
                            cancelled = true;
                        } else
                        {
                            selection = dialog.getSelectedMapObject();
                        }
                    } else if (mapSize > 0)
                    {
                        selection = mapObjectMessageMap.keySet().iterator().next();
                    } else
                    {
                        logger.warn("No map data selected");
                    }

                    if (!cancelled)
                    {
                        final Sector searchSector = searchResult.getSearchSector();

                        if (selection != null)
                        {
                            LatLon[] corners = searchSector.getCorners();

                            selection.getController().showTrackLog(
                                    searchResult.getData().get(selection),
                                    new RaptorLocation(corners[0].latitude.degrees, corners[0].longitude.degrees),
                                    new RaptorLocation(corners[2].latitude.degrees, corners[2].longitude.degrees));
                        } else
                        {
                            showMapDataForLocation(searchResult, searchSector);
                        }
                    }
                }
            }
        }.execute();
    }

    /**
     * So a dialog that gives information on the map points in a region. This method must be called on the EDT.
     *
     * @param result Result of a previous map data search.
     * @param bounds Search bounds
     */
    private void showMapDataForLocation(IRendererSearchResult result, Sector bounds)
    {
        var model = new RaptorDataMessageTableModel(result, bounds);

        mapDataSummaryDialog = new MapDataSummaryDialog(JFrame.getFrames()[0], model);
        mapDataSummaryDialog.setVisible(true);
    }

    private void scheduleTrackListenerCallback(final ITrackSelectListener listener,
                                               final TrackSelectEvent event,
                                               final java.util.List<IRaptorDataMessage> messages)
    {
        SwingUtilities.invokeLater(() -> listener.onSelect(event, messages));
    }

    private void handleRightClick(SelectEvent event, PickedObject topPickedObject)
    {
        // Ensure that the globe producing the event has the focus.  A right-click does not move the focus
        // to that window by default.

        wwd.getComponent().requestFocusInWindow();

        if (event.hasObjects())
        {
            Object selected = topPickedObject != null ? topPickedObject.getObject() : null;

            // TODO: Work around until we wrap WW objects in a GMO
            // If the object implements AVList but not IGlobeMapObject, attempt to retrieve its associated GMO
            // via the key IGlobeMapObject.class.getName(). Note that it is possible for an object to implement
            // both AVList and IGlobeMapObject. Rx classes which implement and wrap WW classes may encounter
            // this case (for example, see GlobeIconMapObject).
            if (selected instanceof AVList && !(selected instanceof IGlobeMapObject))
            {
                // Check for null value before overriding.
                Object overrideSelected = ((AVList) selected).getValue(IGlobeMapObject.class.getName());

                if (overrideSelected != null)
                {
                    selected = overrideSelected;
                }
            }

            if (supportsSearch(topPickedObject, selected))
            {
                handleTrackSelectEvent(topPickedObject, selected, TrackSelectEvent.TrackSelectAction.RIGHT_CLICK);
            } else if (topPickedObject != null)
            {
                // Support right-click to invoke a quick move of the map object, but only if it is draggable
                if (topPickedObject.getObject() instanceof IGlobeMapObject globeMapObject)
                {
                    final var mapObject = globeMapObject.getMapObject();

                    // self marker doesn't allow drag but allows click-to-move (unless disabled via user preferences)
                    if (mapObject.isAllowDrag() || (mapObject.isSelfMarker() && PreferenceEntry.SELF_MARKER_QUICK_MOVE.getBooleanValue()))
                    {
                        globeClickService.activate("Click anywhere on the map to move the object to that location", location -> {
                            mapObject.getController().sendLocationChangeMessage(new ExtendedLocation(location, true, false));
                            globeClickService.deactivate();
                        });
                    }
                }
            }
        }
    }

    private void handleRollover(SelectEvent event)
    {
        if (dragger == null)
        {
            return;
        }
        if (!dragger.isDragging())
        {
            final Object topObject = event.getTopObject();
            if (!(topObject instanceof IGlobeInteractable) || !mouseDown)
            {
                highlight(topObject);
            }
        }
    }

    private void handleHover(SelectEvent event)
    {
        if (lastToolTipItem != null)
        {
            lastToolTipItem.setShowToolTip(false);
            lastToolTipItem = null;
            wwd.getComponent().repaint();
        }

        if (event.hasObjects() && !dragger.isDragging())
        {
            final Object topObject = event.getTopObject();
            if (topObject instanceof IGlobeInteractable)
            {
                lastToolTipItem = (IGlobeInteractable) topObject;
                lastToolTipItem.setShowToolTip(true);
                wwd.getComponent().repaint();
            }
        }
    }

    private void handleDragEvents(SelectEvent event)
    {
        final Object topObject = event.getTopObject();

        // Can't drag null objects. Do not drag shapes, shape editors will do that. Do not drag playback markers.
        if (topObject == null || topObject instanceof AbstractShape || topObject instanceof GlobeShapeMapObject
                || topObject instanceof GlobePlaybackIconMapObject)
        {
            return;
        }

        IMapObject mo = mapObjectManager.getSelectedMapObject();

        // If we're dragging an IGlobeMarkerMapObject, then always obtain the MO from that instead of relying on the
        // currently selected object. The self-location marker can't be selected, so the currently selected MO could
        // be previously set, and then the user tries to drag the self-location marker.
        if (topObject instanceof IGlobeMarkerMapObject globeMarkerMapObject) mo = globeMarkerMapObject.getMapObject();

        if (mo != null && mo.isAllowDrag())
        {
            dragger.selected(event); // Delegate dragging computations to a dragger.
            if (topObject instanceof IGlobeMarkerMapObject globeMarkerMapObject)
            {
                globeMarkerMapObject.setAlwaysOnTop(true);
                boolean canBeContained = canBeContained(globeMarkerMapObject);
                IGlobeMarkerMapObject topPickedGMMO = findTopNotDraggingContainer(globeMarkerMapObject, wwd.getObjectsAtCurrentPosition());
                // Highlight the container that we are dragging over or reset the highlight-able object if its null.
                // If the map object we are dragging does not want to be contained, we skip the highlight.
                highlight(canBeContained ? topPickedGMMO : null);
                if (event.isDrag()) globeMarkerMapObject.getMapObject().setMetaBoolean(StandardAttributeNames.DRAGGING, true);
                if (event.isDragEnd())
                {
                    globeMarkerMapObject.getMapObject().setMetaBoolean(StandardAttributeNames.DRAGGING, false);
                    handleDragEnd(globeMarkerMapObject, topPickedGMMO, canBeContained);
                }
            }
        } else if (!(topObject instanceof IGlobeMarkerMapObject)) // Don't arbitrarily drag markers
        {
            // Non-map objects are only draggable if they explicitly allow dragging.
            if (topObject instanceof Draggable draggable)
            {
                if (draggable.isDragEnabled()) dragger.selected(event);
            } else
            {
                dragger.selected(event); // Non-map object selected. Delegate to the dragger.
            }
        }
    }

    private void handleDragEnd(IGlobeMarkerMapObject globeMarkerMapObject, IGlobeMarkerMapObject topPickedGMMO, boolean canBeContained)
    {
        // topPickedGMMO is the container or null if not over a map object or over a map object that doesn't
        // allow containment.  Also check to see if the device being dragged allows itself to be contained.
        if (topPickedGMMO != null && canBeContained)
        {
            // globeMarkerMapObject is going into container (topPickedGMMO)
            final IMapObject topPickedMapObject = topPickedGMMO.getMapObject();

            if (topPickedMapObject instanceof final IIconMapObject container)
            {
                final var topMapObject = (IIconMapObject) globeMarkerMapObject.getMapObject();
                container.addMapObjectToContainer(topMapObject);
                mapObjectPersist.persistMapObject(container);
                mapObjectPersist.persistMapObject(topMapObject); // Save the relationship

                // We want our newly contained map object to have the same location as its parent
                topMapObject.getController().sendLocationChangeMessage(container.getLocation());
            }
        } else
        {
            //null check position, in case user drags off globe area
            Position position = getCurrentIconPosition();
            if (position != null)
            {
                //send the new location to the data portal to be persisted
                final IMapObject mapObject = globeMarkerMapObject.getMapObject();
                if (mapObject.getElevationMode() == ElevationMode.AGL)
                {
                    // convert destination elevation to AGL
                    position = new Position(position, mapObject.getLocation().getElevation());
                }

                mapObject.getController().sendLocationChangeMessage(WWUtils.positionToRaptorLocation(position));
            }
        }
    }

    /**
     * Can an object be moved? The object can be moved if it is an instance of Movable and not representing a connecting line.
     *
     * @param o Object to test.
     * @return True if {@code o} can be moved.
     */
    private boolean isMovable(Object o)
    {
        return o instanceof Movable && !(o instanceof ConnectingLinePath path && path.isConnectingLine());
    }

    private void highlight(Object o)
    {
        if (lastHighlightedObject == o)
        {
            return; // same thing selected
        }

        if (lastHighlightedObject != null)
        {
            lastHighlightedObject.setHighlighted(false);
            lastHighlightedObject = null;
        }

        if (o instanceof IHighlightable)
        {
            lastHighlightedObject = (IHighlightable) o;
            lastHighlightedObject.setHighlighted(true);
        }

        wwd.getComponent().repaint();
    }

    /**
     * Find the top map object that can be a container, and it is not the object being dragged.
     *
     * @param topObject Object being dragged (this is the one to ignore)
     * @param pol       The picked object list to scan
     * @return Map object from picked object list that can be a container, or {@code null} if none
     */
    private IGlobeMarkerMapObject findTopNotDraggingContainer(IGlobeMarkerMapObject topObject, PickedObjectList pol)
    {
        for (PickedObject o : pol)
        {
            final Object mo = o.getObject();
            if (topObject != mo && mo instanceof IGlobeMarkerMapObject && ((IGlobeMarkerMapObject) mo).getMapObject().isAllowDeviceToBeContainer())
            {
                return (IGlobeMarkerMapObject) mo;
            }
        }
        return null;
    }

    /**
     * Check the plugin definition to see if the GMMO allows itself to be contained and if it does, return {@code true}.
     * Likewise, if there is not a plugin definition, it should be contained.
     *
     * @param globeMarkerMapObject the GMMO to query for canBeContained
     * @return result of canBeContained, or {@code true} if the plugin definition doesn't exist, since that is the default.
     * @since 2.7
     */
    private boolean canBeContained(IGlobeMarkerMapObject globeMarkerMapObject)
    {
        IMapObject mapObject = globeMarkerMapObject.getMapObject();
        final Optional<IDevicePluginDefinition> definition = devicePluginRegistry.getPluginDefinition(mapObject.getFamily(), mapObject.getType());
        return definition.isEmpty() || definition.get().canBeContained();
    }

    /**
     * Alternate method to get the current position.  This is actually very close to the implementation in WorldWindImpl
     * to get the current position with the exception that we want to filter the pick list to pick if a GMMO is in
     * there.
     *
     * @return current position if there is a valid PickedObjectList, {@code null} otherwise
     * @since 2.7
     */
    private Position getCurrentIconPosition()
    {
        if (wwd.getSceneController() == null) return null;

        final PickedObjectList pol = wwd.getSceneController().getPickedObjectList();
        final PickedObject top = findPickedIcon(pol, false);

        Position p = null;
        if (top != null && top.hasPosition())
        {
            p = top.getPosition();
        } else if (pol.getTerrainObject() != null)
        {
            p = pol.getTerrainObject().getPosition();
        }

        return p;
    }

    /**
     * Method to scan the PO list for a GMMO. Find the first "top" GMMO in the list, or the first GMMO if none
     * are marked on top.
     *
     * @param pol          PickedObjectList
     * @param requireOnTop Pass {@code true} to only return a GMMO if it was set as "on top"
     * @return located GMMO picked object
     * @see PickedObject#isOnTop()
     * @since reworked in 2.29
     */
    private PickedObject findPickedIcon(PickedObjectList pol, boolean requireOnTop)
    {
        if (pol == null) return null;

        PickedObject found = null;
        for (PickedObject po : pol)
        {
            if (po.getObject() instanceof IGlobeMarkerMapObject)
            {
                if (po.isOnTop())
                {
                    return po;
                } else if (found == null)
                {
                    found = po;
                }
            }
        }
        return requireOnTop ? null : found;
    }
}
