package gov.takx.api.mapobject;

import gov.nasa.worldwind.event.SelectEvent;
import gov.takx.api.annotation.Internal;
import gov.takx.api.globe.IGlobeDelegate;

import java.awt.event.MouseEvent;
import java.util.Collections;
import java.util.EventObject;
import java.util.List;

/**
 * This event signals that a map object is under the cursor and the operation that caused the signal.
 * If the event is in result of rolling off of a map object, an empty list of map objects will be returned, and
 * {@link #hasMapObjects()} will return false.  In the event {@link #hasMapObjects()} returns true, you can
 * get the affected map objects by calling {@link #getMapObjects()}.<br>
 * <br>
 * For example, to listen for HOVER events on map objects, register as a select listener, and in overriding
 * the {@code selected(MapObjectSelectEvent event)} method, check for a hover event with
 * {@code event.isHoverEvent()}, check for associated IMapObject with {@code event.hasMapObjects()}, and
 * if so, {@code event.getMapObjects().get(0)} to get the IMapObject.<br>
 * <br>
 * Basic pattern follows the WW {@link gov.nasa.worldwind.event.SelectEvent} with the added event SELECTED,
 * which can also be fired when an operator selects a map object from the event log or a plugin programmatically
 * selects a map object through the API.<br>
 * <br>
 * IMapObjectSelectListeners should be registered via
 * {@link IGlobeDelegate#addSelectListener(IMapObjectSelectListener)}.  Be sure to remove
 * select listeners when they are being torn down.
 *
 * @since 1.40
 */
@SuppressWarnings({"StringEquality", "UnusedDeclaration"})
public class MapObjectSelectEvent extends EventObject
{
    /**
     * A map object has been selected, either from a globe click, through the explorer tree, or through the API
     */
    public static final String SELECTED = "MapObjectSelectEvent.Selected";
    /**
     * The user clicked the left mouse button while the cursor was over picked object.
     */
    public static final String LEFT_CLICK = SelectEvent.LEFT_CLICK;
    /**
     * The user double-clicked the left mouse button while the cursor was over picked object.
     */
    public static final String LEFT_DOUBLE_CLICK = SelectEvent.LEFT_DOUBLE_CLICK;
    /**
     * The user clicked the right mouse button while the cursor was over picked object.
     */
    public static final String RIGHT_CLICK = SelectEvent.RIGHT_CLICK;
    /**
     * The user pressed the left mouse button while the cursor was over picked object.
     */
    public static final String LEFT_PRESS = SelectEvent.LEFT_PRESS;
    /**
     * The user pressed the right mouse button while the cursor was over picked object.
     */
    public static final String RIGHT_PRESS = SelectEvent.RIGHT_PRESS;
    /**
     * The cursor has moved over the picked object and become stationary, or has moved off the object of the most recent
     * {@code HOVER} event. In the latter case, the picked object will be {@code null}.
     */
    public static final String HOVER = SelectEvent.HOVER;
    /**
     * The cursor has moved over the object or has moved off the object most recently rolled over. In the latter case
     * the picked object will be {@code null}.
     */
    public static final String ROLLOVER = SelectEvent.ROLLOVER;
    /**
     * The user is attempting to drag the picked object.
     */
    public static final String DRAG = SelectEvent.DRAG;
    /**
     * The user has stopped dragging the picked object.
     */
    public static final String DRAG_END = SelectEvent.DRAG_END;

    private final String eventAction;
    private final List<IMapObject> mapObjects;
    private final MouseEvent mouseEvent;
    private final int globeID;

    /**
     * @param globeID     Id of globe on which the selection took place
     * @param eventAction Event action string from WW
     * @param mapObjects  Map Object being selected, may be {@code null}
     */
    @SuppressWarnings("AssignmentOrReturnOfFieldWithMutableType") // We take ownership of the mapObjects list
    @Internal
    public MapObjectSelectEvent(int globeID, String eventAction, List<IMapObject> mapObjects)
    {
        super(mapObjects);
        this.eventAction = eventAction;
        mouseEvent = null;
        this.mapObjects = mapObjects;
        this.globeID = globeID;
    }

    /**
     * @param globeID     Id of globe on which the selection took place
     * @param sourceEvent Source WW SelectEvent
     * @param mapObjects  Map Object being selected, may be {@code null}
     * @since 2.37
     */
    @SuppressWarnings("AssignmentOrReturnOfFieldWithMutableType") // We take ownership of the mapObjects list
    @Internal
    public MapObjectSelectEvent(int globeID, SelectEvent sourceEvent, List<IMapObject> mapObjects)
    {
        super(mapObjects);
        eventAction = sourceEvent.getEventAction();
        mouseEvent = sourceEvent.getMouseEvent();
        this.mapObjects = mapObjects;
        this.globeID = globeID;
    }

    /**
     * Get the mouse event associated with the selection event.
     *
     * @return the MouseEvent
     */
    public MouseEvent getMouseEvent()
    {
        return mouseEvent;
    }

    /**
     * Get the String representation of the event action.
     *
     * @return the String event action.
     */
    public String getEventAction()
    {
        return eventAction != null ? eventAction : "MapObjectSelectEvent.UnknownEventAction";
    }

    /**
     * Get the list of map objects associated with the selection event.  In the case of "de-selection", this list
     * will be empty.  Currently, only a single map object is supported for selection, and can be dereferenced by
     * means of {@code getMapObjects().get(0)} if not an empty list.
     *
     * @return the list of map objects associated with the selection event.
     * @see #hasMapObjects()
     */
    public List<IMapObject> getMapObjects()
    {
        return Collections.unmodifiableList(mapObjects);
    }

    /**
     * Get the id of the globe that this event occurred on.
     *
     * @return id of the globe event relates to
     * @since 1.49
     */
    public int getGlobeID()
    {
        return globeID;
    }

    /**
     * Check to see if there are any map objects associated with this selection event, and if so, return true.
     * Otherwise, as in the case of a "de-selection" or "roll-off", return false;
     *
     * @return true if there are associated map objects, false if not.
     */
    public boolean hasMapObjects()
    {
        return !mapObjects.isEmpty();
    }

    public boolean isRollover()
    {
        return getEventAction() == ROLLOVER;
    }

    public boolean isHover()
    {
        return getEventAction() == HOVER;
    }

    public boolean isDragEnd()
    {
        return getEventAction() == DRAG_END;
    }

    public boolean isDrag()
    {
        return getEventAction() == DRAG;
    }

    public boolean isRightPress()
    {
        return getEventAction() == RIGHT_PRESS;
    }

    public boolean isRightClick()
    {
        return getEventAction() == RIGHT_CLICK;
    }

    public boolean isLeftDoubleClick()
    {
        return getEventAction() == LEFT_DOUBLE_CLICK;
    }

    public boolean isLeftClick()
    {
        return getEventAction() == LEFT_CLICK;
    }

    public boolean isLeftPress()
    {
        return getEventAction() == LEFT_PRESS;
    }

    public boolean isMapObjectSelected()
    {
        return getEventAction() == SELECTED;
    }
}
