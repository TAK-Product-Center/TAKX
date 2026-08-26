package gov.takx.api.ui.event;

import gov.takx.api.mapobject.IMapObjectController;

/**
 * Event fired when the map object selection is changed, either to a new map object or {@code null} (to clear
 * the selection).
 */
public class MapObjectSelectedEvent
{
    private final IMapObjectController cmoc;

    /**
     * Construct selection event.
     *
     * @param cmoc Controller for map object selected, {@code null} for no selection
     */
    public MapObjectSelectedEvent(IMapObjectController cmoc)
    {
        this.cmoc = cmoc;
    }

    /**
     * @return Controller for map object selected, {@code null} for no selection
     */
    public IMapObjectController getMapObjectController()
    {
        return cmoc;
    }
}
