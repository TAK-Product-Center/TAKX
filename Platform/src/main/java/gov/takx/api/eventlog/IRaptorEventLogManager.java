package gov.takx.api.eventlog;

import gov.takx.api.annotation.InjectableService;
import gov.takx.api.annotation.NoPluginImplementations;
import gov.takx.api.mapobject.IDataMonitor;
import gov.takx.api.mapobject.IMapObject;
import gov.takx.commons.constants.State;
import jakarta.annotation.Nonnull;

import java.util.Date;

/**
 */
@InjectableService
@NoPluginImplementations
public interface IRaptorEventLogManager
{
    /**
     * Value to be used for the family field for user-generated event logs
     */
    String USER_SOURCE = "USER";

    void logContainment(@Nonnull Date logTime, @Nonnull IMapObject mapObject, String sContainer, boolean bAdded, String sDetails);

    void logStateChange(@Nonnull Date logTime, @Nonnull IMapObject mapObject, State curState, String sDetails);

    void logMapObjectOnline(@Nonnull Date logTime, @Nonnull IMapObject mapObject, String sDetails);

    void logMapObjectNameChanged(@Nonnull Date logTime, @Nonnull IMapObject mapObject, String details);

    void logMapObjectOffline(@Nonnull Date logTime, @Nonnull IMapObject mapObject, String sDetails);

    /**
     * Adds a user-generated event
     *
     * @param logTime  The time of the log message
     * @param family   The map object family, or {@link IRaptorEventLogManager#USER_SOURCE} if it is a user message
     * @param type     the map object type, or {@link IRaptorEventLogManager#USER_SOURCE} if it is a user message
     * @param unitId   the map object id, or the user's username
     * @param sMessage The log message
     * @param sDetails The log details
     */
    void logUserEvent(@Nonnull Date logTime, String family, String type, String unitId, String sMessage, String sDetails);

    /**
     * Plugin generated log event to log to the EventLog panel.
     *
     * @param logTime   time of the log message
     * @param mapObject map object to log the message for. Can not be {@code null}
     * @param sMessage  The event log message
     * @param sDetails  A detailed message to log
     */
    void logPluginEvent(@Nonnull Date logTime, @Nonnull IMapObject mapObject, String sMessage, String sDetails);

    /**
     * Log an event from a plugin with the additional details of what state the log event should assume.  State can
     * be {@code null} if the event has no state.
     *
     * @param logTime   time of the log message
     * @param mapObject map object to log the message for
     * @param sMessage  The event log message
     * @param sDetails  A detailed message to log
     * @param state     The state of the log message {@link State}
     */
    void logPluginEvent(@Nonnull Date logTime, @Nonnull IMapObject mapObject, String sMessage, String sDetails, State state);

    /**
     * Log an event from a plugin.
     *
     * @param logTime time of the log message
     * @param family  The family of the plugin. Required
     * @param type    The type of the plugin. Required
     * @param unitId  The unit id of the plugin. Not required
     * @param message The event log message
     * @param details A detailed message to log
     */
    void logPluginEvent(@Nonnull Date logTime, @Nonnull String family, @Nonnull String type, String unitId, String message, String details);

    /**
     * Allows an app plugin to log an event correlating to a specific map object. An app plugin should use
     * {@link IRaptorEventLogManager#logPluginEvent(Date, String, String, String, String, String)} to log an event that
     * does not have a correlating map object.
     *
     * @param logTime   time of the log message
     * @param mapObject The correlating map object. Required
     * @param sMessage  The event log message
     * @param sDetails  A detailed message to log
     */
    void logAppEvent(@Nonnull Date logTime, @Nonnull IMapObject mapObject, String sMessage, String sDetails);

    /**
     * Log a data monitor state change event.
     *
     * @param logTime     time that the state change event occurred
     * @param mapObject   map object that owns the data monitor
     * @param dataMonitor data monitor for which the state change event occurred
     * @param curState    the new state of the data monitor
     * @param sDetails    a detailed message describing the data monitor state change event
     * @since 1.12
     */
    void logDataMonitorStateChange(@Nonnull Date logTime, @Nonnull IMapObject mapObject, IDataMonitor dataMonitor, State curState, String sDetails);

    /**
     * Log a data monitor state change event.
     *
     * @param logTime     time that the state change event occurred
     * @param family      the data monitor owner's family
     * @param type        the data monitor owner's type
     * @param id          the data monitor owner's unique id
     * @param dataMonitor data monitor for which the state change event occurred
     * @param state       the new state of the data monitor
     * @param details     a detailed message describing the data monitor state change event
     * @since 2.16
     */
    void logDataMonitorStateChange(@Nonnull Date logTime, String family, String type, String id, IDataMonitor dataMonitor, State state, String details);

    void addEventLogListener(@Nonnull IRaptorEventLogListener logListener);

    void removeEventLogListener(@Nonnull IRaptorEventLogListener eventLogListener);

    /**
     * Return the array of log entries
     *
     * @return array of log entries
     */
    IRaptorEventLog[] getLogEntries();
}
