package gov.takx.api.explorer;

import gov.takx.api.mapobject.IDataMonitor;
import gov.takx.api.mapobject.IMapObject;
import gov.takx.api.mapobject.IMapObjectController;

import java.util.List;

/**
 * Interface for explorer objects based upon map objects.
 *
 * @since 1.48
 */
public interface IMapObjectExplorable extends IExplorable
{
    String FAMILY = "Family";
    String TYPE = "Type";
    String LAST_COMMUNICATION_TIME = "Last Communication Time";

    /**
     * Get the map object associated with the implementation.
     *
     * @return the map object associated with the implementation.
     */
    IMapObject getMapObject();

    /**
     * Convenience method for getting the controller associated with the implementation's map object.
     *
     * @return the map object controller associated with the map object.
     */
    IMapObjectController getMapObjectController();

    /**
     * Get the list of data monitors associated with this map explorer object.
     *
     * @return list of data monitors associated with this map explorer object.
     */
    List<IDataMonitor> getDataMonitors();
}
