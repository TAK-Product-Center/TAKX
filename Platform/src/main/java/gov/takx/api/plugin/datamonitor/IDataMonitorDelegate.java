package gov.takx.api.plugin.datamonitor;

import gov.takx.api.filesystem.IVirtualFileSystem;
import gov.takx.api.globe.IFavoritesService;
import gov.takx.api.globe.IStateIconManager;
import gov.takx.api.mapobject.IDataMonitor;
import gov.takx.api.mapobject.IMapObject;
import gov.takx.api.ping.IGeoPingService;
import gov.takx.api.query.IQueryService;
import gov.takx.api.timer.ITimerService;
import gov.takx.commons.constants.State;

import java.net.URL;
import java.util.Date;

/**
 * Provides {@link IDataMonitor}s with core functionality.
 *
 * @since 1.10
 */
public interface IDataMonitorDelegate
{
    /**
     * Set a custom icon on the map object associated with the owner data monitor.
     * Ignored if no data monitor is associated with the owner data monitor.
     *
     * @param imagePathURL The path to the image.
     */
    void setCustomIcon(URL imagePathURL);

    /**
     * Log an alarm-specific event.
     *
     * @param message The message to log.
     * @param details The details to log.
     */
    void logAlarmEvent(String message, String details);

    /**
     * Log a state change event.
     *
     * @param logTime   The time of the event.
     * @param mapObject The map object whose state changed.
     * @param curState  The new state of the map object.
     * @param sDetails  A detailed message about the state change event.
     * @since 1.11
     */
    void logStateChangeEvent(Date logTime, IMapObject mapObject, State curState, String sDetails);

    /**
     * Log a datamonitor state change event.
     *
     * @param dataMonitor the data monitor whose state changed
     * @param curState    the new state of the data monitor
     * @param sDetails    a detailed message describing the data monitor state change event
     * @since 1.12
     */
    void logDataMonitorStateChangeEvent(IDataMonitor dataMonitor, State curState, String sDetails);

    /**
     * Return the service that will allow data monitors to perform
     * queries.
     *
     * @return The service that will allow data monitors to perform queries.
     */
    IQueryService getQueryService();

    /**
     * Return the state icon manager associated with this data monitor.
     * {@code null} if no map object is associated with the owner data monitor.
     */
    IStateIconManager getStateIconManager();

    /**
     * Return an interface that will allow access to the virtual file system.
     *
     * @return An interface that will allow access to the virtual file system.
     */
    IVirtualFileSystem getVirtualFileSystem();

    /**
     * Return the favorites service.
     *
     * @return The favorites service.
     */
    IFavoritesService getFavoritesService();

    /**
     * Return the geo-ping service.
     *
     * @return The geo-ping service.
     */
    IGeoPingService getGeoPingService();

    /**
     * Return a utility class that provides basic timer-related functionality.
     *
     * @return A utility class that provides timer services.
     */
    ITimerService getTimerService();
}
