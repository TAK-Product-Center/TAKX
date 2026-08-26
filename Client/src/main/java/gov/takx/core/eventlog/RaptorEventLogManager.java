package gov.takx.core.eventlog;

import gov.takx.api.event.StartProjectServices;
import gov.takx.api.event.StopProjectServices;
import gov.takx.api.event.mapobject.MapObjectAdded;
import gov.takx.api.event.mapobject.MapObjectRemoved;
import gov.takx.api.eventlog.EventLogType;
import gov.takx.api.eventlog.IRaptorEventLog;
import gov.takx.api.eventlog.IRaptorEventLogListener;
import gov.takx.api.eventlog.IRaptorEventLogManager;
import gov.takx.api.mapobject.IDataMonitor;
import gov.takx.api.mapobject.IMapObject;
import gov.takx.api.persist.IEventLogPersist;
import gov.takx.commons.constants.State;
import gov.takx.core.project.ServiceOrdering;
import gov.takx.dataservices.persist.RaptorEventLog;
import gov.takx.platform.preferences.PreferenceEntry;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Priority;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import jakarta.inject.Inject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.lang.invoke.MethodHandles;
import java.util.Date;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import static gov.takx.api.eventlog.EventLogType.ALARM_STATE_CHANGE;
import static gov.takx.api.eventlog.EventLogType.APP_EVENT;
import static gov.takx.api.eventlog.EventLogType.MAP_OBJECT_NAME_CHANGE;
import static gov.takx.api.eventlog.EventLogType.MAP_OBJECT_ONLINE;
import static gov.takx.api.eventlog.EventLogType.PLUGIN_EVENT;
import static gov.takx.api.eventlog.EventLogType.STATE_CHANGE;
import static gov.takx.api.eventlog.EventLogType.USER_EVENT;

/**
 */
@SuppressWarnings("UseOfObsoleteDateTimeApi")
@ApplicationScoped
public class RaptorEventLogManager implements IRaptorEventLogManager, PropertyChangeListener
{
    private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    @Inject
    private IEventLogPersist eventLogPersist;

    @Inject
    private EventLogTableModel eventLogTableModel;

    private final List<IRaptorEventLogListener> eventLogListeners = new CopyOnWriteArrayList<>();
    private boolean enabled;
    private boolean suppressEvents = true;

    RaptorEventLogManager()
    {
    }

    @Override
    public void logContainment(@Nonnull Date logTime, @Nonnull IMapObject mapObject, String container, boolean added, String details)
    {
        if (shouldNotLogMapObjectEvent(mapObject)) return;

        final String message = added ? "Contained by " + container : "Uncontained from " + container;

        addEventLog(new RaptorEventLog(logTime, mapObject, message, details, EventLogType.CONTAINMENT, container));
    }

    @Override
    public void logStateChange(@Nonnull Date logTime, @Nonnull IMapObject mapObject, State curState, String details)
    {
        if (shouldNotLogMapObjectEvent(mapObject)) return;

        String message = mapObject.getName() + " " + curState;

        addEventLog(new RaptorEventLog(logTime, mapObject, message, details, STATE_CHANGE, null, curState));
    }

    @Override
    public void logMapObjectOnline(@Nonnull Date logTime, @Nonnull IMapObject mapObject, String details)
    {
        if (shouldNotLogMapObjectEvent(mapObject)) return;

        addEventLog(new RaptorEventLog(logTime, mapObject, "Added", details, MAP_OBJECT_ONLINE));
    }

    @Override
    public void logMapObjectNameChanged(@Nonnull Date logTime, @Nonnull IMapObject mapObject, String details)
    {
        if (shouldNotLogMapObjectEvent(mapObject)) return;

        addEventLog(new RaptorEventLog(logTime, mapObject, "Name changed", details, MAP_OBJECT_NAME_CHANGE));
    }

    @Override
    public void logMapObjectOffline(@Nonnull Date logTime, @Nonnull IMapObject mapObject, String details)
    {
        if (shouldNotLogMapObjectEvent(mapObject)) return;

        logger.debug("MapObject Offline {}", mapObject);

        // Notify listeners that this data is going away
        eventLogListeners.forEach(listener -> listener.wipeData(mapObject));
    }

    @Override
    public void logUserEvent(@Nonnull Date logTime, String family, String type, String unitId, String message, String details)
    {
        if (!enabled || suppressEvents) return;

        addEventLog(new RaptorEventLog(logTime, family, type, unitId, message, details, USER_EVENT, null, "User Entry"));
    }

    @Override
    public void logPluginEvent(@Nonnull Date logTime, @Nonnull IMapObject mapObject, String message, String details)
    {
        logPluginEvent(logTime, mapObject, message, details, null);
    }

    @Override
    public void logPluginEvent(@Nonnull Date logTime, @Nonnull String family, @Nonnull String type, String unitId, String message, String details)
    {
        if (!enabled || suppressEvents) return;

        addEventLog(new RaptorEventLog(logTime, family, type, unitId, message, details, PLUGIN_EVENT, null, type));
    }

    @Override
    public void logPluginEvent(@Nonnull Date logTime, @Nonnull IMapObject mapObject, String message, String details, State state)
    {
        if (shouldNotLogMapObjectEvent(mapObject)) return;

        addEventLog(new RaptorEventLog(logTime, mapObject, message, details, PLUGIN_EVENT, null, state));
    }

    @Override
    public void logAppEvent(@Nonnull Date logTime, @Nonnull IMapObject mapObject, String message, String details)
    {
        if (shouldNotLogMapObjectEvent(mapObject)) return;

        addEventLog(new RaptorEventLog(logTime, mapObject, message, details, APP_EVENT));
    }

    @Override
    public IRaptorEventLog[] getLogEntries()
    {
        return eventLogTableModel.getLogEntries();
    }

    @Override
    public void addEventLogListener(@Nonnull IRaptorEventLogListener logListener)
    {
        if (logger.isDebugEnabled()) logger.debug("Adding Event Log Listener: {}", logListener.getClass().getName());

        eventLogListeners.add(logListener);
    }

    @Override
    public void removeEventLogListener(@Nonnull IRaptorEventLogListener eventLogListener)
    {
        eventLogListeners.remove(eventLogListener);
    }

    @Override
    public void logDataMonitorStateChange(@Nonnull Date logTime, @Nonnull IMapObject mapObject, IDataMonitor dataMonitor, State state, String details)
    {
        if (shouldNotLogMapObjectEvent(mapObject)) return;

        String displayName = "[" + dataMonitor.getDisplayName() + "] ";

        addEventLog(new RaptorEventLog(logTime, mapObject, displayName, details, ALARM_STATE_CHANGE, null, state));
    }

    @Override
    public void logDataMonitorStateChange(@Nonnull Date logTime, String family, String type, String id, IDataMonitor dataMonitor, State state, String details)
    {
        if (!enabled || suppressEvents) return;

        String dataMonitorDisplayName = "[" + dataMonitor.getDisplayName() + "] ";
        addEventLog(new RaptorEventLog(logTime, family, type, id, dataMonitorDisplayName, details, ALARM_STATE_CHANGE, null, id));
    }

    /**
     * Determine if an event log entry should NOT be created for the given Map object.  This includes testing for
     * the current global {@code enabled} and {@code suppressEvents} state.
     *
     * @param mapObject Map Object of interest
     * @return {@code true} to NOT record an event
     * @since 2.29
     */
    private boolean shouldNotLogMapObjectEvent(@Nonnull IMapObject mapObject)
    {
        return !enabled || suppressEvents || !mapObject.shouldCreateEventLogEntries();
    }

    private void addEventLog(IRaptorEventLog rel)
    {
        logger.debug("Add: {}", rel);

        eventLogPersist.logAdded(rel); // Persist it
        eventLogListeners.forEach(listener -> listener.logAdded(rel));
    }

    private void onMapObjectAdded(@Observes MapObjectAdded event)
    {
        final IMapObject mapObject = event.getMapObject();

        if (mapObject.isDevice() && mapObject.shouldCreateEventLogEntries())
        {
            mapObject.addPropertyChangeListener(IMapObject.NAME, this);

            // Don't generate an online event when restoring
            if (!mapObject.isRestoring()) logMapObjectOnline(new Date(), mapObject, "");
        }
    }

    private void onMapObjectRemoved(@Observes MapObjectRemoved event)
    {
        final IMapObject mapObject = event.getMapObject();

        mapObject.removePropertyChangeListener(IMapObject.NAME, this);

        logMapObjectOffline(new Date(), mapObject, null);
    }

    /**
     * Monitor for Map Object name changes.
     *
     * @since 3.3
     */
    @Override
    public void propertyChange(PropertyChangeEvent event)
    {
        final Object source = event.getSource();

        if (event.getPropertyName().equals(IMapObject.NAME) && source instanceof IMapObject mapObject)
        {
            logMapObjectNameChanged(new Date(), mapObject, event.getOldValue() + " -> " + event.getNewValue());
        }
    }

    /**
     * Handle the opening of a new project. Prepare for events.
     */
    private void startService(@Observes @Priority(ServiceOrdering.START_GenericCoreService) StartProjectServices ignored)
    {
        enabled = PreferenceEntry.ENABLE_EVENT_LOG.getBooleanValue();
        suppressEvents = false; // Allow events once we start

        logger.debug("event logging enabled: {}", enabled);
    }

    /**
     * Stop servicing.
     */
    private void stopService(@Observes @Priority(ServiceOrdering.STOP_GenericCoreService) StopProjectServices ignored)
    {
        suppressEvents = true; // Don't allow any more events once the project is closing
        eventLogListeners.clear();
    }
}
