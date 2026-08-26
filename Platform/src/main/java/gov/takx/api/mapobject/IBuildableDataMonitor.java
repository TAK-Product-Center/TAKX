package gov.takx.api.mapobject;

import gov.takx.commons.constants.State;
import gov.takx.commons.utils.Threshold;
import jakarta.annotation.Nonnull;

import java.net.URL;

/**
 * Contract for building/configuring a data monitor.
 *
 * @since 3.2
 * @see gov.takx.api.plugin.datamonitor.DataMonitorBuilder
 */
public interface IBuildableDataMonitor extends IDataMonitor
{
    /**
     * Specify whether this data monitor will produce an audible alert and produce a colored background on
     * the owning Map Object's icon when alarming.
     *
     * @param affectsAudioAndIcons Pass {@code true} to produce an audible and visual indicator when alarming
     */
    void setAffectsAudioAndIcons(boolean affectsAudioAndIcons);

    /**
     * Set the name of the field that this data monitor will inspect in the RDMs that flow through.
     */
    void setFieldName(@Nonnull String name);

    /**
     * Set the type of field that this data monitor will inspect in the RDMs that flow through.
     */
    void setFieldType(@Nonnull FieldType fieldType);

    /**
     * Optionally set an alternate field to be displayed on screen (instead of the field being inspected).
     */
    void setValueFieldName(@Nonnull String name);

    /**
     * Set whether the value of the field being inspected (or the value field, if set) should be displayed on screen.
     */
    void setShowValueOnDisplay(boolean showValueOnDisplay);

    /**
     * Set the default state of this data monitor. The data monitor will start in the default state and revert
     * to the default state when cleared.
     */
    void setDefaultState(@Nonnull State defaultState);

    /**
     * Add a threshold, declaring the max value that this data monitor can have for the specified state.
     *
     * @param maxThreshold The max value that the field can have while staying in the state specified in {@code state}.
     * @param state        The state whose max threshold is being defined.
     * @param inclusive    Indicates if {@code maxThreshold} is inclusive or exclusive.
     */
    void addThreshold(@Nonnull Comparable<?> maxThreshold, @Nonnull State state, boolean inclusive);

    /**
     * Add a pre-defined threshold.
     */
    void addThreshold(@Nonnull Threshold threshold);

    /**
     * Set whether this data monitor can be manually cleared when in an alarming state.
     */
    void setClearable(boolean clearable);

    /**
     * Set if this data monitor's audio alarms can be muted.
     */
    void setCanBeMuted(boolean canBeMuted);

    /**
     * Set the name that will be displayed for this data monitor.
     */
    void setDisplayName(@Nonnull String displayName);

    /**
     * Set the parent of this data monitor.
     */
    void setParent(@Nonnull IDataMonitor parent);

    /**
     * Set URL of the icon that will be displayed instead of the standard icon.
     */
    void setCustomIcon(@Nonnull URL url);

    /**
     * If the given value is {@code true}, then the highest state seen will be tracked until the user acknowledges
     * a prompt displayed when the data monitor triggers an alarm. The visual representation in the map object dialog
     * will be maintained until the user acknowledges the prompt. This DOES NOT affect calls to
     * {@link #getCurrentState()}, which will continue to return the state from the last value analyzed.  Thus, this
     * has no effect on the audio alarm or the Map Object's globe icon.
     * <p>
     * If the given value is {@code false}, map object dialog indicator and the prompt will be displayed for the
     * user to acknowledge but will reset when the data monitor drops below its alarm threshold.
     *
     * @see #getCurrentState()
     * @see #getUnclearedAlarmState()
     */
    void setTrackHighestAlarmState(boolean trackHighestAlarmState);

    /**
     * If the given value is {@code true}, then the state of the data monitor will not decrease until it is
     * acknowledged by the user. This affects calls to {@link #getCurrentState()}. If {@code false}, then the
     * state of the alarm tracks that determined by the last value analyzed.
     *
     * @see #getUnclearedAlarmState()
     */
    void setHoldHighestAlarmState(boolean holdHighestAlarmState);
}
