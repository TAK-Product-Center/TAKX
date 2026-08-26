package gov.takx.core.explorer.common;

import gov.takx.api.explorer.IExplorable;
import gov.takx.api.mapobject.IDataMonitor;
import gov.takx.api.plugin.datamonitor.DataMonitorEvent;
import gov.takx.api.plugin.datamonitor.DataMonitorEventType;
import gov.takx.commons.constants.State;
import jakarta.annotation.Nonnull;

/**
 * Extension of {@link AlarmExplorable} that implements {@link IDataMonitorAlarmExplorable} to represent alarms created by
 * an {@link IDataMonitor}.
 *
 * @since 5.3
 */
public class DataMonitorAlarmExplorable extends AlarmExplorable implements IDataMonitorAlarmExplorable
{
    /**
     * Construct a new instance.
     *
     * @param dataMonitor the data monitor this alarm came from
     * @param explorable the explorable this alarm is for
     * @param alarmState the alarm state
     */
    public DataMonitorAlarmExplorable(@Nonnull IDataMonitor dataMonitor, @Nonnull IExplorable explorable, @Nonnull State alarmState)
    {
        super(dataMonitor, dataMonitor.getUniqueKey() + "-" + explorable.getUniqueKey(), explorable, alarmState);
    }

    @Nonnull
    @Override
    public IDataMonitor getDataMonitor()
    {
        return (IDataMonitor) getOwner();
    }

    @Override
    public void clearAlarm()
    {
        IDataMonitor dataMonitor = getDataMonitor();
        if (dataMonitor.isClearable()) dataMonitor.onDataMonitorEvent(new DataMonitorEvent(this, DataMonitorEventType.AlarmClear));
    }
}