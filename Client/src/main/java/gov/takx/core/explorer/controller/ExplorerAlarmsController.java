package gov.takx.core.explorer.controller;

import gov.takx.api.annotation.PseudoBean;
import gov.takx.api.explorer.IExplorable;
import gov.takx.api.explorer.IExplorableAlarm;
import gov.takx.api.explorer.IExplorableGroup;
import gov.takx.api.explorer.IMapObjectExplorable;
import gov.takx.api.mapobject.ADataMonitor;
import gov.takx.api.mapobject.IDataMonitor;
import gov.takx.api.mapobject.IMapObject;
import gov.takx.commons.constants.State;
import gov.takx.core.explorer.IExplorer;
import gov.takx.core.explorer.common.DataMonitorAlarmExplorable;
import gov.takx.core.explorer.common.IAlarmExplorable;
import gov.takx.core.explorer.common.IDataMonitorAlarmExplorable;
import gov.takx.core.explorer.common.IPathExplorableGroup;
import gov.takx.platform.utils.TimeBoundedTask;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import jakarta.inject.Inject;
import org.greenrobot.eventbus.EventBus;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static gov.takx.core.explorer.controller.path.ExplorerPathConstants.ALERTS_DATA_MONITORS_PATH;
import static gov.takx.core.explorer.controller.path.ExplorerPathConstants.ALERTS_PATH;

/**
 * Implementation class of {@link IExplorerAlarmsController}.
 *
 * @since 1.48
 */
@SuppressWarnings("CdiManagedBeanInconsistencyInspection")
@PseudoBean
public class ExplorerAlarmsController implements IExplorerAlarmsController
{
    private static final int MIN_DELAY = 250;
    private static final int MAX_DELAY = 1000;
    private final IExplorerController controller;
    private final IExplorer explorer;
    private final Map<IExplorable, List<IDataMonitorAlarmExplorable>> alarmsMap = new HashMap<>();
    private final State minAlarmState = State.Active;

    @Inject
    private EventBus eventBus;
    // Our "update" task, performed on a timed basis no matter how often requested
    private final TimeBoundedTask updateTask =
            new TimeBoundedTask("Explorer Alarm Updater", MIN_DELAY, MAX_DELAY, this::performUpdate);
    private State highestState;
    private final IPathExplorableGroup rootAlertsGroup;
    private final IPathExplorableGroup dataMonitorAlertsGroup;
    private final IDataMonitorAlarmExplorable alertsRootAlarm;

    ExplorerAlarmsController(@Nonnull IExplorerController controller, @Nonnull IExplorer explorer)
    {
        this.controller = controller;
        this.explorer = explorer;
        highestState = State.Unknown;

        rootAlertsGroup = controller.getRoot().getOrCreatePath(ALERTS_PATH);
        rootAlertsGroup.setSupportsVisibility(false);

        dataMonitorAlertsGroup = controller.getRoot().getOrCreatePath(ALERTS_DATA_MONITORS_PATH);
        dataMonitorAlertsGroup.setSupportsVisibility(false);

        ExplorerAlarmsControllerDataMonitor internalMonitor = new ExplorerAlarmsControllerDataMonitor();
        alertsRootAlarm = new DataMonitorAlarmExplorable(internalMonitor, rootAlertsGroup, highestState);
    }

    @SuppressWarnings("removal")
    @Nonnull
    @Override
    public IExplorableGroup getAlertsRoot()
    {
        return rootAlertsGroup;
    }

    /**
     * Gets the Data Monitor list from the explorable object.
     * TODO DCW TAKX-3282 Why not move this to the IExplorable interface?
     *
     * @param explorerObject the explorable
     * @return the List of DataMonitors
     */
    @SuppressWarnings("unchecked")
    @Nonnull
    public static List<IDataMonitor> getDataMonitorsFromExplorable(@Nonnull IExplorable explorerObject)
    {
        if (!explorerObject.hasKey(IExplorable.DATA_MONITORS_KEY)) return Collections.emptyList();
        return explorerObject.getValue(IExplorable.DATA_MONITORS_KEY) instanceof List<?> o ? (List<IDataMonitor>) o : Collections.emptyList();
    }

    @Override
    @Nonnull
    public State getHighestAlarmState()
    {
        return highestState;
    }

    @Nonnull
    @Override
    public State getHighestAlarmState(@Nonnull IExplorable explorable)
    {
        if (explorable instanceof IExplorableAlarm alarm)
        {
            return alarm.getAlarmLevel();
        } else if (explorable instanceof IMapObjectExplorable mapObjectExplorable)
        {
            return (State) mapObjectExplorable.getValue(IMapObject.HIGHEST_STATE);
        }

        synchronized (alarmsMap)
        {
            return alarmsMap.getOrDefault(explorable, Collections.emptyList())
                    .stream()
                    .map(IExplorableAlarm::getAlarmLevel)
                    .reduce((a, b) -> a.ordinal() > b.ordinal() ? a : b)
                    .orElse(State.Unknown);
        }
    }

    @Nullable
    @Override
    public State getHighestVisibleAlarmState(@Nonnull IExplorable explorable)
    {
        var highest = getHighestAlarmState(explorable);
        return highest.ordinal() >= minAlarmState.ordinal() ? highest : null;
    }

    @Override
    public void clear()
    {
        synchronized (alarmsMap)
        {
            alarmsMap.clear();
            highestState = State.Unknown;
        }
    }

    @Override
    @Nonnull
    public List<IDataMonitorAlarmExplorable> getAlarms()
    {
        synchronized (alarmsMap)
        {
            return alarmsMap.values().stream().flatMap(Collection::stream).toList();
        }
    }

    @Override
    @Nonnull
    public List<IDataMonitorAlarmExplorable> getAlarms(@Nonnull IExplorable explorable)
    {
        synchronized (alarmsMap)
        {
            return alarmsMap.get(explorable).stream().map(IDataMonitorAlarmExplorable.class::cast).toList();
        }
    }

    @Nonnull
    @Override
    public List<IDataMonitorAlarmExplorable> getAlarms(@Nonnull State minState)
    {
        synchronized (alarmsMap)
        {
            return alarmsMap.values().stream().flatMap(Collection::stream).filter(alarm -> alarm.getAlarmLevel().ordinal() >= minState.ordinal()).toList();
        }
    }

    @Override
    public void update()
    {
        updateTask.request(); // Just request an update, gathering multiple requests on a timed basis
    }

    /**
     * Update state with current alarm status.
     */
    private void performUpdate()
    {
        State nextHighestState = State.Unknown;
        for (IExplorable explorable : controller.getExplorables())
        {
            // Don't look for alarms on alarms
            if (explorable instanceof IAlarmExplorable) continue;

            List<IDataMonitorAlarmExplorable> currentAlarms;
            synchronized (alarmsMap)
            {
                currentAlarms = List.copyOf(alarmsMap.getOrDefault(explorable, Collections.emptyList()));
            }
            List<IDataMonitorAlarmExplorable> toRemoveAlarms = new ArrayList<>(currentAlarms);
            List<IDataMonitorAlarmExplorable> toAddAlarms = new ArrayList<>();
            List<IDataMonitorAlarmExplorable> nextAlarms = new ArrayList<>();
            for (IDataMonitor dataMonitor : getDataMonitors(explorable))
            {
                // Don't create alarms for states below the min
                State state = dataMonitor.getCurrentState();
                nextHighestState = state.ordinal() > nextHighestState.ordinal() ? state : nextHighestState;
                IDataMonitorAlarmExplorable alarm = currentAlarms
                        .stream()
                        .filter(a -> a.getDataMonitor().equals(dataMonitor))
                        .findFirst().orElse(null);
                if (alarm == null && state.ordinal() >= minAlarmState.ordinal())
                {
                    alarm = new DataMonitorAlarmExplorable(dataMonitor, explorable, state);
                    toAddAlarms.add(alarm);
                    nextAlarms.add(alarm);
                } else if (alarm != null)
                {
                    alarm.setAlarmState(state);
                    if (state.ordinal() >= minAlarmState.ordinal())
                    { // Only retain alarm explorables above the min sate
                        toRemoveAlarms.remove(alarm);
                        nextAlarms.add(alarm);
                    }
                }
            }

            // Set state
            synchronized (alarmsMap)
            {
                alarmsMap.put(explorable, nextAlarms);
            }

            // Add new alarms to group
            toAddAlarms.forEach(dataMonitorAlertsGroup::addChild);

            // Remove missing alarms from controller
            toRemoveAlarms.forEach(controller::removeExplorable);
        }

        // Roll the alarm state up to the root alerts group
        alertsRootAlarm.setAlarmState(nextHighestState);
        if (nextHighestState.ordinal() >= minAlarmState.ordinal())
        {
            alarmsMap.put(rootAlertsGroup, List.of(alertsRootAlarm));
            alarmsMap.put(dataMonitorAlertsGroup, List.of(alertsRootAlarm));
        } else
        {
            alarmsMap.remove(rootAlertsGroup);
            alarmsMap.remove(dataMonitorAlertsGroup);
        }

        explorer.handleAlarmStatusChange();
    }

    @Override
    @Nonnull
    public List<IDataMonitor> getDataMonitors(@Nonnull IExplorable explorable)
    {
        return getDataMonitorsFromExplorable(explorable);
    }

    /**
     * This class exists to internally satisfy the {@link IDataMonitorAlarmExplorable} constructor.
     */
    private static class ExplorerAlarmsControllerDataMonitor extends ADataMonitor
    {
        protected ExplorerAlarmsControllerDataMonitor()
        {
            super("Explorer Alarms Controller", "Explorer Alarms Controller");
        }
    }
}
