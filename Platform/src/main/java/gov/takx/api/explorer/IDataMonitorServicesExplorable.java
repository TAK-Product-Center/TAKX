package gov.takx.api.explorer;

import gov.takx.api.mapobject.IDataMonitor;

import java.util.List;

/**
 * Base class for {@link IExplorable} objects that support data monitors.
 *
 * @since 2.16
 */
public interface IDataMonitorServicesExplorable extends IExplorable
{
    /**
     * Return the app's data monitors.
     *
     * @return The app's data monitors.
     */
    List<IDataMonitor> getDataMonitors();
}
