package gov.takx.core.eventlog;

import gov.takx.api.annotation.InvokeOnEDT;
import gov.takx.api.annotation.ProgramPreference;
import gov.takx.api.event.project.ProjectClosing;
import gov.takx.api.event.project.ProjectOpened;
import gov.takx.api.eventlog.EventLogType;
import gov.takx.api.eventlog.IRaptorEventLog;
import gov.takx.api.eventlog.IRaptorEventLogListener;
import gov.takx.api.eventlog.IRaptorEventLogManager;
import gov.takx.api.mapobject.IMapObject;
import gov.takx.api.mapobject.IMapObjectManager;
import gov.takx.api.persist.IEventLogPersist;
import gov.takx.api.platform.ICoreExecutorService;
import gov.takx.commons.lang.CircularArray;
import gov.takx.commons.utils.StringUtils;
import gov.takx.platform.KeyUtils;
import gov.takx.platform.preferences.PreferenceEntry;
import gov.takx.platform.utils.StandardTimeFormatting;
import jakarta.annotation.PostConstruct;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import jakarta.inject.Inject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.table.AbstractTableModel;

import java.lang.invoke.MethodHandles;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 */
@ApplicationScoped
public class EventLogTableModel extends AbstractTableModel implements IRaptorEventLogListener
{
    private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    private static final boolean alwaysUseNewestName = true;

    @Inject
    private IRaptorEventLogManager eventLogManager;

    @Inject
    private IEventLogPersist eventLogPersist;

    @Inject
    private ICoreExecutorService executorService;

    @Inject
    private StandardTimeFormatting timeFormatting;

    @Inject
    private IMapObjectManager mapObjectManager;

    @Inject
    @ProgramPreference(PreferenceEntry.ENABLE_EVENT_LOG)
    private boolean enabled;

    @Inject
    @ProgramPreference(PreferenceEntry.MAX_EVENT_LOG_TABLE_ROWS)
    private int numElements;

    private CircularArray<IRaptorEventLog> logValues;

    public enum ColumnNames
    {
        SOURCE("Device"),
        MAP_OBJECT_FAMILY("Family"),
        MAP_OBJECT_TYPE("Device Type"),
        MAP_OBJECT_UNIT_ID("Unit ID"),
        MESSAGE("Message"),
        DATE("Date"),
        TYPE("Type"),
        DETAILS("Details"),
        TARGET("Target"),
        MAP_OBJECT_NAME("Name"),
        STATE("State");

        private final String name;

        ColumnNames(String name)
        {
            this.name = name;
        }

        @Override
        public String toString()
        {
            return name;
        }
    }

    @PostConstruct
    private void pc()
    {
        logValues = new CircularArray<>(numElements);
    }

    @Override
    @InvokeOnEDT
    public void logAdded(IRaptorEventLog newLog)
    {
        if (logger.isDebugEnabled()) logger.debug("Log Entry Added: {}", newLog);
        logValues.add(newLog);
        fireTableDataChanged();
    }

    @Override
    @InvokeOnEDT
    public void wipeData(final IMapObject mapObject)
    {

        final String family = mapObject.getFamily();
        final String type = mapObject.getType();
        final String unitId = mapObject.getUnitID();

        Set<IRaptorEventLog> logsToDelete = new HashSet<>();

        // Copy the list retaining matching entries
        int size = logValues.size();
        for (int i = 0; i < size; i++)
        {
            IRaptorEventLog logEntry = logValues.get(i);
            if (logEntry.getMapObjectFamily().equals(family)
                    && logEntry.getMapObjectType().equals(type)
                    && logEntry.getMapObjectUnitId().equals(unitId))
            {
                logsToDelete.add(logEntry);
            }
        }

        if (logger.isDebugEnabled()) logger.debug("Removing log entry data for {}", mapObject.getKey());

        logValues.removeAll(logsToDelete);
        fireTableDataChanged();
    }

    private void handleProjectClosing(@SuppressWarnings("UnusedParameters") @Observes ProjectClosing event)
    {
        eventLogManager.removeEventLogListener(this);
        logValues.clear();
        fireTableDataChanged();
    }

    private void handleProjectOpened(@SuppressWarnings("UnusedParameters") @Observes ProjectOpened event)
    {
        if (!enabled)
        {
            if (logger.isDebugEnabled()) logger.debug("disabled");
            return;
        }

        if (logger.isDebugEnabled()) logger.debug("Registering EventLogTableModel as an event log listener");

        eventLogManager.addEventLogListener(this);

        // Load any EventLog objects we have in the DB
        // Do this in the background
        executorService.execute(() -> {
            Thread.currentThread().setName("EventLog Loader");
            logValues.addAll(eventLogPersist.searchDate(null, null));
            fireTableDataChanged();

            if (logger.isDebugEnabled()) logger.debug("Event Log Records Loaded from DB: {}", logValues.size());
        });
    }

    @Override
    public Class<?> getColumnClass(int columnIndex)
    {
        if (!logValues.isEmpty())
        {
            if (getValueAt(0, columnIndex) != null)
            {
                return getValueAt(0, columnIndex).getClass();
            }
        }
        return String.class;
    }

    @Override
    public String getColumnName(int column)
    {
        return (ColumnNames.values()[column]).toString();
    }

    @Override
    public int getColumnCount()
    {
        return ColumnNames.values().length;
    }

    @Override
    public int getRowCount()
    {
        return logValues.size();
    }

    @Override
    public Object getValueAt(int rowIndex, int columnIndex)
    {
        return getValueAt(rowIndex, ColumnNames.values()[columnIndex]);
    }

    public Object getValueAt(int rowIndex, ColumnNames column)
    {
        if (rowIndex < logValues.size())
        {
            IRaptorEventLog logValue = logValues.get(rowIndex);
            return switch (column)
            {
                case SOURCE -> logValue.getSource();
                case MAP_OBJECT_FAMILY -> logValue.getMapObjectFamily();
                case MAP_OBJECT_TYPE -> logValue.getMapObjectType();
                case MAP_OBJECT_UNIT_ID -> logValue.getMapObjectUnitId();
                case MESSAGE -> logValue.getMessage();
                case DATE -> timeFormatting.formatWithDefault(logValue.getDate().getTime());
                case TYPE -> EventLogType.values()[logValue.getType()];
                case DETAILS -> logValue.getDetails();
                case TARGET -> logValue.getTarget();
                case MAP_OBJECT_NAME -> getEventLogObjectName(logValue);
                case STATE -> logValue.getState() == null ? "null" : logValue.getState().toString();
            };
        }
        return null;
    }

    /**
     * Get the name of the "source" object that generated the event log entry.
     * <p>
     * First, check for a name in the log entry.
     * If there isn't one, try to find the map object and get its name.
     * If all else fails, make one up using the recorded type and unit ID.
     *
     * @param logEntry The log entry whose source should be named.
     * @return the name of the log entry's "source" object
     */
    private String getEventLogObjectName(IRaptorEventLog logEntry)
    {
        String name = alwaysUseNewestName ? null : logEntry.getName();

        // fallback: if name is not cached, try to look up object name
        if (!StringUtils.isEmpty(name)) return name;

        final var mapObject = mapObjectManager.getMapObject(
                KeyUtils.getUniqueKey(
                        logEntry.getMapObjectFamily(),
                        logEntry.getMapObjectType(),
                        logEntry.getMapObjectUnitId()));

        if (mapObject != null) return mapObject.getName();

        // Final fallback: if a map object can't be found then use type + unitId
        // for old event log entries that did not store the map object name.
        return logEntry.getMapObjectType() + " " + logEntry.getMapObjectUnitId();
    }

    public IRaptorEventLog[] getLogEntries()
    {
        List<IRaptorEventLog> allLogs = eventLogPersist.searchDate(null, null);

        if (logger.isDebugEnabled()) logger.debug("Event Log Records retrieved from DB: {}", allLogs.size());

        return allLogs.toArray(new IRaptorEventLog[0]);
    }
}
