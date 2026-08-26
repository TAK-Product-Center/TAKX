package gov.takx.api.plugin.datamonitor;

import gov.takx.api.mapobject.FieldType;
import gov.takx.api.messages.IRaptorDataMessage;
import gov.takx.commons.constants.State;
import gov.takx.platform.ui.UIResources;
import jakarta.annotation.Nonnull;

import java.util.TimerTask;
import java.util.concurrent.TimeUnit;

import static gov.takx.commons.constants.State.Active;
import static gov.takx.commons.constants.State.Inactive;
import static gov.takx.commons.constants.State.Unknown;
import static gov.takx.platform.ui.ResourceDirectory.PLATFORM_ICONS;

/**
 * A pre-defined communications data monitor. The display name and icon cannot be customized by the user.
 * No actual message field is analyzed to determine the state, it is based on the time between receipt of messages.
 * Any message received will set the state to Active.  After going Active, a timer task will analyze the delta from
 * the last message receipt every 5 seconds, comparing that to the configured Thresholds to determine a new state.
 * Threshold values must be in integer milliseconds (not longs).
 * <p>
 * Note: as constructed this data monitor has no thresholds defined.  You must add appropriate thresholds for your
 * time limits and alarm states.
 * <p>
 * Here's an example using a builder to construct and configure an instance:
 * <pre>
 * {@code
 * IDataMonitor monitor = new DataMonitorBuilder(family, type, StandardDataMonitorType.Comms)
 *     .setDefaultState(State.Critical)
 *     .setClearable(true)
 *     .addThreshold(20_000, State.Active, IDataMonitor.INCLUSIVE)
 *     .addThreshold(40_000, State.Warning, IDataMonitor.INCLUSIVE)
 *     .build();
 * }
 * </pre>
 *
 * @since 2.18
 * @see DataMonitorBuilder
 */
public class CommsDataMonitor extends AStockDataMonitor
{
    private static final long FIVE_SECS_MS = TimeUnit.SECONDS.toMillis(5);

    /**
     * We use a single Timer (thread) to handle all our tasks.  Otherwise, we end up with
     * one thread for every device instance - not good.
     */
    private static final String COMMS_TIMER_NAME = "CommsDataMonitor Timer";

    private TimerTask checkCommsTask;
    private long lastMessageTime = 0;

    public CommsDataMonitor(String family, String type, boolean showValueOnDisplay)
    {
        this(family, type, showValueOnDisplay, Unknown);
    }

    @SuppressWarnings("UnusedDeclaration")
    public CommsDataMonitor(String family, String type, boolean showValueOnDisplay, State defaultState)
    {
        super(family, type, "Comms", UIResources.getUrl(PLATFORM_ICONS, "data_black.png"));

        setFieldName("Comms"); // Actually unused
        setFieldType(FieldType.INTEGER);
        setClearable(false);
        setShowValueOnDisplay(showValueOnDisplay);
        setDefaultState(defaultState);
        setSuppressValueLogging(true);
    }

    /**
     * Override validate to ensure timer is started on first message
     *
     * @param rdm RaptorDataMessage to validate
     */
    @Override
    public void analyze(@Nonnull IRaptorDataMessage rdm)
    {
        if (!rdm.isManuallyGeneratedLocation() && !rdm.isImported()) // Do not trigger if a manual move or was imported
        {
            lastMessageTime = System.currentTimeMillis();

            if (Active != currentState)
            {
                delegate.logDataMonitorStateChangeEvent(this, Active, fieldName + " established");
            }

            setState(Active);

            if (checkCommsTask == null) startTimerTask();
            if (valueFieldName != null) updateValue(rdm);
        }
    }

    @Override
    public void onDataMonitorEvent(@Nonnull DataMonitorEvent event)
    {
        State oldState = getCurrentState();
        switch (event.getEventType())
        {
            case AlarmClear, DoubleClick ->
            {
                // Don't think we want to allow a clear of an active comms data monitor
                if (clearable && (oldState != Unknown || getUnclearedAlarmState() != Unknown) && oldState != Active)
                {
                    stopTimerTask();
                    clear(Unknown);
                }
            }
            case Deactivate, Shutdown ->
            {
                stopTimerTask();
                deactivate();
            }
            default -> super.onDataMonitorEvent(event);
        }
    }

    /**
     * Cancel the current timer task.
     */
    private void stopTimerTask()
    {
        if (checkCommsTask != null)
        {
            checkCommsTask.cancel();
            checkCommsTask = null;
        }
    }

    /**
     * Starts a timer task that checks every 5 seconds for the elapsed time since the last message received.
     * It then validates the thresholds.
     */
    private void startTimerTask()
    {
        checkCommsTask = new CheckCommsTask();
        delegate.getTimerService().scheduleAtFixedRate(COMMS_TIMER_NAME, checkCommsTask, 0, FIVE_SECS_MS);
    }

    /**
     * Timer task that checks the comm status on a regular basis.
     */
    private class CheckCommsTask extends TimerTask
    {
        @Override
        public void run()
        {
            final int deltaT = (int) (System.currentTimeMillis() - lastMessageTime);
            final State oldState = currentState;

            analyzeValue(deltaT);

            if (currentState != oldState && currentState != Inactive)
            {
                delegate.logDataMonitorStateChangeEvent(CommsDataMonitor.this, currentState,
                        "No comms for over " + (deltaT / 1000) + " seconds");
            }
        }
    }
}
