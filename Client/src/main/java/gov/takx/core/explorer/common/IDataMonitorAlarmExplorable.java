package gov.takx.core.explorer.common;

import gov.takx.api.explorer.IExplorableAlarm;
import gov.takx.api.mapobject.IDataMonitor;
import gov.takx.commons.constants.State;
import jakarta.annotation.Nonnull;

/**
 * Extension of {@link IExplorableAlarm} for alarms produce by an {@link IDataMonitor}.
 *
 * @since 5.3
 */
public interface IDataMonitorAlarmExplorable extends IAlarmExplorable
{

    /**
     * @return the Data Monitor associated with this alarm
     */
    @Nonnull
    IDataMonitor getDataMonitor();

    /**
     * Sets the current alarm state.
     *
     * @param currentState the alarm state
     */
    void setAlarmState(@Nonnull State currentState);

}