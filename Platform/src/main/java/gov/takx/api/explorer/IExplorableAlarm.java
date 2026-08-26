package gov.takx.api.explorer;

import gov.takx.commons.constants.State;

import javax.swing.Icon;

/**
 * Interface for alarms associated with explorables.
 *
 * @since 1.48
 */
public interface IExplorableAlarm
{
    /**
     * Property name for alarm state change events.
     *
     * @since 5.3
     */
    String ALARM_STATE = "Alarm State";

    /**
     * Get the alarm's originating explorable.
     *
     * @return the alarm's originating explorable.
     */
    IExplorable getExplorable();

    /**
     * Get the name of the alarm.
     *
     * @return the name of the alarm.
     */
    String getAlarmName();

    /**
     * Get the level or severity of the alarm.<br/>
     * TODO DCW TAKX-3282 Should we rename this field to alarmState to match with other references?
     *
     * @return the level of the alarm.
     */
    State getAlarmLevel();

    /**
     * Get the icon for the alarm's originating explorable.
     *
     * @return icon for the alarm's originating explorable.
     */
    Icon getExplorableIcon();

    /**
     * Request to clear the alarm.
     */
    default void clearAlarm()
    {
    }
}
