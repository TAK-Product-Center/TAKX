package gov.takx.api.preferences;

import gov.tak.api.annotation.DeprecatedApi;
import gov.takx.api.annotation.NoPluginImplementations;
import gov.takx.api.coordinatesystem.CoordinateSystem;
import gov.takx.commons.media.AudioAlert;
import gov.takx.platform.preferences.UserPreferenceEntry;
import gov.takx.platform.utils.UnitsFormat;

import java.util.function.Consumer;

/**
 * Contract for service offering access to the user preferences.
 *
 * @since 2.2
 */
@NoPluginImplementations
public interface IUserPreferenceManager extends IPreferenceManager
{
    /**
     * Create a new preference monitor on the {@link UserPreferenceEntry#AUDIO_ALERT}.
     * This is a convenience method and is equivalent to the following code:
     * <pre>
     * {@code UserPreferenceEntry.AUDIO_ALERT.newMonitor(AudioAlert.class, changeHandler)}
     * </pre>
     *
     * @return new preference monitor
     */
    IPreferenceMonitor<AudioAlert> newAudioAlertMonitor(Consumer<AudioAlert> changeHandler);

    /**
     * Create a new preference monitor on the {@link UserPreferenceEntry#COORDINATE_SYSTEM}.
     * This is a convenience method and is equivalent to the following code:
     * <pre>
     * {@code UserPreferenceEntry.COORDINATE_SYSTEM.newMonitor(CoordinateSystem.class, changeHandler)}
     * </pre>
     *
     * @return new preference monitor
     */
    IPreferenceMonitor<CoordinateSystem> newCoordinateSystemMonitor(Consumer<CoordinateSystem> changeHandler);

    /**
     * Create a new preference monitor on the {@link UserPreferenceEntry#DISPLAY_UNIT_OF_MEASURE}.
     * This is a convenience method and is equivalent to the following code:
     * <pre>
     * {@code UserPreferenceEntry.DISPLAY_UNIT_OF_MEASURE.newMonitor(UnitsFormat.class, changeHandler)}
     * </pre>
     *
     * @return new preference monitor
     * @deprecated Use {@link IPreferenceEntry#newMonitor(Class, Consumer)} on
     * {@link UserPreferenceEntry#DISPLAY_ALTITUDE_UNITS}, {@link UserPreferenceEntry#DISPLAY_RANGE_UNITS} and
     * {@link UserPreferenceEntry#DISPLAY_SPEED_UNITS} instead.
     */
    @Deprecated(since = "5.5", forRemoval = true)
    @DeprecatedApi(since = "5.5", forRemoval = true, removeAt = "5.8")
    IPreferenceMonitor<UnitsFormat> newUnitsFormatMonitor(Consumer<UnitsFormat> changeHandler);

    /**
     * Reload the user preferences from disk.  Used when a new configuration is imported.
     *
     * @since 2.35
     */
    void reloadUserPreferences();
}
