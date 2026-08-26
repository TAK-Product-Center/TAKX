package gov.takx.api.mapobject;

import gov.takx.api.globe.IGlobeDelegate;

/**
 * Implement this interface to receive notifications of map object selections events
 * {@link MapObjectSelectEvent} relating to mouse selections, HOVER, ROLLOVER, LEFT_CLICK, etc.  Basic pattern
 * follows the WW {@link gov.nasa.worldwind.event.SelectEvent} with the added event SELECTED, which can also
 * be fired when an operator selects a map object from the event log or a plugin programmatically selects
 * a map object through the API.
 * <p/>
 * Affected Map Objects include any type that are represented by an icon on the globe.
 *
 * @see MapObjectSelectEvent
 * @see IGlobeDelegate#addSelectListener(IMapObjectSelectListener)
 * @since 1.40
 */
public interface IMapObjectSelectListener
{
    /**
     * Callback of a select event on a map object.
     *
     * @param event the map object selection event
     * @since 1.40
     */
    void selected(MapObjectSelectEvent event);
}
