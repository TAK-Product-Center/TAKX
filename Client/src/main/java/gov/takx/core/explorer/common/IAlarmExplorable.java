package gov.takx.core.explorer.common;

import gov.takx.api.explorer.IExplorable;
import gov.takx.api.explorer.IExplorableAlarm;

/**
 * Interface for alarms that are visible in the explorer tree.
 * <br>
 * TODO DCW TAKX-3282 Should probably consider moving to the platform so plugins can make use of alarms.
 *
 * @since 5.3
 */
public interface IAlarmExplorable extends IExplorableAlarm, IExplorable, INonGroupableExplorable
{
    @Override
    default boolean supportsVisibility()
    {
        return false;
    }
}