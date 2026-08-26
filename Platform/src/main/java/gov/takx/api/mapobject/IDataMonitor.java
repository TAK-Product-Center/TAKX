package gov.takx.api.mapobject;

import gov.takx.api.annotation.Internal;
import gov.takx.api.messages.IRaptorCommandMessage;
import gov.takx.api.messages.IRaptorDataMessage;
import gov.takx.api.plugin.IRaptorObject;
import gov.takx.api.plugin.datamonitor.DataMonitorBuilder;
import gov.takx.api.plugin.datamonitor.DataMonitorEvent;
import gov.takx.api.plugin.datamonitor.IDataMonitorDelegate;
import gov.takx.commons.constants.State;
import gov.takx.commons.utils.Threshold;
import jakarta.annotation.Nonnull;

import java.beans.PropertyChangeListener;
import java.net.URL;
import java.util.List;

/**
 * Interface for a data monitor. Data Monitors are used to process {@link IRaptorDataMessage}s and
 * {@link IRaptorCommandMessage}s and update the state of the map object for a specific field in those messages.
 * Thresholds, display names, and state changes can all be customized.
 *
 */
public interface IDataMonitor extends IRaptorObject, Comparable<IDataMonitor>
{
    boolean INCLUSIVE = true;
    boolean EXCLUSIVE = false;

    String STATE_CHANGED = "state";
    String VALUE_CHANGED = "value";

    String MUTE = "mute";
    String MOUSE_EVENT = "MouseEvent";

    /**
     * Add a property change listener.
     * @since 3.4
     */
    void addPropertyChangeListener(@Nonnull PropertyChangeListener listener);

    /**
     * Remove a property change listener.
     * @since 3.4
     */
    void removePropertyChangeListener(@Nonnull PropertyChangeListener listener);

    /**
     * @return {@code true} if this data monitor affects a Map Object's audible and visible alarm.
     */
    boolean isAffectsAudioAndIcons();

    /**
     * Analyze the next RDM being sent to the plugin.
     *
     * @param rdm The {@link IRaptorDataMessage} to analyze.
     */
    void analyze(@Nonnull IRaptorDataMessage rdm);

    /**
     * Return the name of the field in the RDM that is used for validation.
     *
     * @return String field name
     */
    String getFieldName();

    /**
     * Return the name used in the status panel display for this data monitor
     *
     * @return String display name
     */
    String getDisplayName();

    /**
     * Return last value of data monitor
     *
     * @return String display name
     */
    Object getValue();

    /**
     * Return current state of data monitor.  If {@link #isHoldHighestAlarmState()} is {@code true},
     * then this method will return the highest state seen to this point, until the user acknowledges the alarm.
     *
     * @return state value
     * @see State
     * @see #isHoldHighestAlarmState()
     * @see #getUnclearedAlarmState()
     */
    @Nonnull
    State getCurrentState();

    /**
     * Return the highest unacknowledged state set on this data monitor. This may differ
     * from the current state if {@link #isTrackHighestAlarmState} returns true.
     *
     * @return state value
     * @see State
     */
    @Nonnull
    State getUnclearedAlarmState();

    /**
     * @return The URL of the custom icon for this data monitor that will be overlayed on the standard icon.
     */
    URL getCustomIcon();

    /**
     * @return whether the value of the data monitor should be displayed on the status panel
     */
    boolean getShowValueOnDisplay();

    /**
     * Return a list of all active thresholds. The list is a copy and will not affect the
     * actual threshold list if modified.
     *
     * @return A list of all active thresholds being used by this data monitor.
     */
    @Nonnull
    List<Threshold> thresholds();

    /**
     * Set the delegate that this data monitor will use to get access to
     * core services.
     *
     * @param delegate The delegate that this data monitor will use to get access
     *                 core services.
     */
    @Internal
    void setDelegate(@Nonnull IDataMonitorDelegate delegate);

    /**
     * Return a list of all the children being observed by this data monitor.
     *
     * @return A list of all the children being monitored by this data monitor. {@code null}
     * if this data monitor currently has no children.
     */
    @Nonnull
    List<IDataMonitor> getChildren();

    /**
     * Return the parent data monitor that is observing this data monitor.
     *
     * @return The parent data monitor that is observing this data monitor. {@code null}
     * if this data monitor does not currently have a parent.
     */
    IDataMonitor getParent();

    /**
     * Notifies this data monitor of an event.
     *
     * @param event The event that this data monitor should be aware of.
     */
    void onDataMonitorEvent(@Nonnull DataMonitorEvent event);

    /**
     * Add a child to this data monitor. This method is not meant to establish initial hierarchy. Users should use
     * {@link DataMonitorBuilder#setParent(IDataMonitor)} to define the hierarchy.
     * This method provides a way for the child to notify the parent that it has a new child data monitor.
     *
     * @param dataMonitor The data monitor that has been added as a child of {@code this} data monitor.
     */
    @Internal
    void onChildAdded(@Nonnull IDataMonitor dataMonitor);

    /**
     * This method relates <i>only</i> to the visual map object dialog indicator and the prompt a user receives when
     * the data monitor reaches its alarm state. (If the value of
     * {@link #isHoldHighestAlarmState()} is {@code true}, the value of this method
     * will always be {@code true}).
     * <p/>
     * If the return value is {@code true}, the user must <i>always</i> acknowledge a prompt displayed when
     * the data monitor triggers an alarm. The visual representation in the map object dialog will be maintained until
     * the user acknowledges the prompt.
     * <p/>
     * If the return value is {@code false}, map object dialog indicator and the prompt will be displayed for the
     * user to acknowledge but will reset when the data monitor drops below its alarm threshold.
     *
     * @return Boolean indicating if the device's alarm can only be cleared manually.
     */
    boolean isTrackHighestAlarmState();

    /**
     * This method relates to the audio, on-globe visual and map object dialog indicators and the {@link State}
     * of the map object represented by this data monitor.
     * <p>
     * If the value is {@code true}, the audio and visual indicators and the {@link State} of an <i>alarming</i>
     * map object will be maintained until a prompt is acknowledged by the user.
     * <p>
     * If false, the map object's audio and visual indicators and the {@link State} will automatically follow the
     * most current analysis.
     *
     * @return Boolean indicating if an alarming device's audio/visual indicators and {@link State} can only
     * be cleared manually.
     */
    boolean isHoldHighestAlarmState();

    /**
     * Return a boolean indicating if the data monitor is manually clearable.
     *
     * @return Boolean indicating if the data monitor is manually clearable.
     */
    boolean isClearable();

    /**
     * Overriding this method allows the data monitor to decline the logging to the event log of the state change
     * transition from the previous state to the current state of the map object.  Of course, the data monitor
     * already knows what it's current state is
     *
     * @param previousState previous state of the map object
     * @param currentState  current state of the map object
     * @return true to log the state change, false otherwise
     * @since 1.14.20
     */
    boolean logStateChangeEvent(@Nonnull State previousState, @Nonnull State currentState);

    /**
     * Result indicates if this data monitor's audible alerts can be muted.
     *
     * @return Boolean indicating if this data monitor's audible alerts can be muted.
     */
    boolean canBeMuted();

    /**
     * Result indicates if this data monitor is currently muted.
     *
     * @return Boolean indicating if this data monitor is currently muted.
     */
    boolean isMuted();

    /**
     * Result indicates if this data monitor is currently alarming.
     *
     * @return Boolean indicating if the data monitor is currently alarming.
     */
    boolean isAlarming();
}
