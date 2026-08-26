package gov.takx.api.eventlog;

import gov.takx.api.mapobject.IMapObject;

/**
 * Listener that wants to be notified whenever a new event log entry is recorded.
 *
 */
public interface IRaptorEventLogListener
{
    /**
     * Record a new event log event.
     *
     * @param newLog new log entry
     */
    void logAdded(IRaptorEventLog newLog);

    /**
     * Permanently wipe all event log entries associated with the given map object.
     *
     * @param mapObject The map object that is being wiped
     */
    void wipeData(IMapObject mapObject);
}
