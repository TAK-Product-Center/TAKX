package gov.takx.core.explorer.controller;

import gov.takx.api.explorer.IExplorable;
import gov.takx.api.explorer.IExplorableGroup;
import gov.takx.api.mapobject.IDataMonitor;
import gov.takx.commons.constants.State;
import gov.takx.core.explorer.common.IDataMonitorAlarmExplorable;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import java.util.List;

/**
 * Interface for the explorer alarms controller.  This controller tracks and manages the current alarm status for all
 * explorables accessible via IExplorerController.
 *
 * @since 1.48
 */
public interface IExplorerAlarmsController
{
    /**
     * @return the explorable that is the parent of all alerts
     * @since 5.5
     */
    @SuppressWarnings("removal")
    @Nonnull
    IExplorableGroup getAlertsRoot();

    /**
     * Get the current highest alarm.
     *
     * @return the current highest alarm.
     */
    @Nullable
    State getHighestAlarmState();

    /**
     * Get an ordered list of the current alarms.
     *
     * @return ordered list of the current alarms.
     */
    @Nonnull
    List<IDataMonitorAlarmExplorable> getAlarms();

    /**
     * Returns a List of alarms for the given explorable within this controller.
     *
     * @param explorable the explorable
     * @return the List of alarms for the given explorable.
     * @since 5.3
     */
    @Nonnull
    List<IDataMonitorAlarmExplorable> getAlarms(IExplorable explorable);

    /**
     * Returns the highest alarm state for the given explorable.
     *
     * @param explorable the explorable to filter on
     * @return the highest alarm state for the explorable, if there are any, otherwise {@code null}
     * @since 5.3
     */
    @Nonnull
    State getHighestAlarmState(IExplorable explorable);

    /**
     * @param explorable The explorable to test
     * @return The highest alarm state for the explorable, if it is greater than the minimum visible property, otherwise {@code null}
     * @since 5.7
     */
    @Nullable
    State getHighestVisibleAlarmState(@Nonnull IExplorable explorable);

    /**
     * Get an ordered list of the current alarms of a state greater than or equal to a minimum alarm state.
     *
     * @param minState minimum alarm state.
     * @return ordered list of current alarms of a state greater than or equal to the minimum alarm state.
     */
    @Nonnull
    List<IDataMonitorAlarmExplorable> getAlarms(State minState);

    /**
     * Clear all alarms.
     */
    void clear();

    /**
     * Update state with current alarm status.
     */
    void update();

    /**
     * Get the list of {@link IDataMonitor}s for the given explorable.
     *
     * @param explorable the Explorable
     * @return the list of {@link IDataMonitor}s for the given explorable.
     * @since 5.3
     */
    List<IDataMonitor> getDataMonitors(IExplorable explorable);
}
