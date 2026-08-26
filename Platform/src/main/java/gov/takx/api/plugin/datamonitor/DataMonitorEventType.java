package gov.takx.api.plugin.datamonitor;

/**
 * Specifies the type of event represented by a {@link DataMonitorEvent}.
 * <p>
 * In the case that the {@link #Command} event type is used, the user <b>must</b> identify the custom
 * event type by providing the {@link DataMonitorEvent} with a corresponding custom name.
 *
 * @since 1.10
 */
public enum DataMonitorEventType
{
    Click,
    DoubleClick,
    RightClick,
    AlarmClear,
    Deactivate,
    AlarmAcknowledge,
    ToggleMute,
    Shutdown,
    Command
}
