package gov.takx.api.preferences;

import gov.takx.api.annotation.NoPluginImplementations;
import gov.takx.api.coordinatesystem.CoordinateSystem;
import gov.takx.commons.media.AudioAlert;
import gov.takx.platform.preferences.UserPreferenceEntry;

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
     * Reload the user preferences from disk.  Used when a new configuration is imported.
     *
     * @since 2.35
     */
    void reloadUserPreferences();
}
