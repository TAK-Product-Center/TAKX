package gov.takx.api.eventlog;

import gov.takx.api.annotation.NoPluginImplementations;
import gov.takx.api.persist.IPersistable;
import gov.takx.commons.constants.State;

import java.util.Date;

/**
 * Represents a TAKX Event Log entry.
 *
 */
@NoPluginImplementations
public interface IRaptorEventLog extends IPersistable
{
    /**
     * Source (key) of the event log entry.
     *
     * @return key
     */
    String getSource();

    /**
     * Map Object Family responsible for the event log entry.
     *
     * @return Map Object Family
     */
    String getMapObjectFamily();

    /**
     * Map Object Type responsible for the event log entry.
     *
     * @return Map Object Type
     */
    String getMapObjectType();

    /**
     * Map Object Unit Id responsible for the event log entry.
     *
     * @return Map Object Unit Id
     */
    String getMapObjectUnitId();

    /**
     * Event type.
     *
     * @return type of event; one of {@link EventLogType}'s ordinals
     */
    int getType();

    /**
     * Event message.
     *
     * @return message
     */
    String getMessage();

    /**
     * Event details.
     *
     * @return details
     */
    String getDetails();

    /**
     * Log date.
     *
     * @return date of the log entry
     */
    Date getDate();

    /**
     * Target.
     *
     * @return target
     */
    String getTarget();

    /**
     * Unit's user-defined name.
     *
     * @return name
     */
    String getName();

    /**
     * @return {@code null} if not a mapobject or data monitor state change event log. {@link State} otherwise.
     * @since 1.12
     */
    State getState();

    /**
     * @return Id of Map Object with the log entry (if any), may be for events not associated with a Map Object
     * @since 3.0
     */
    Integer getMapObjectId();

    /**
     * Arbitrary 'User' data
     *
     * @return String-encoded (think 'JSON') User data
     */
    String getAnnotation();

    /**
     * Set the arbitrary 'User' data
     *
     * @param annotation The User data
     */
    void setAnnotation(String annotation);
}
