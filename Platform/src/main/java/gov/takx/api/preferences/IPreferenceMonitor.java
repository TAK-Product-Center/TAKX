package gov.takx.api.preferences;

import gov.takx.api.annotation.NoPluginImplementations;

/**
 * Contract for type-safe monitoring and handling of changes to preference values.
 * <p>
 * Monitors provide access to the current and previous (if a change has taken place) values of the preference.
 * <p>
 * Monitors provide change notification by invoking a registered change handler with the new value of the preference.
 * <p>
 * Monitors can be created on any {@link IPreferenceEntry} with type-safe value retrieval for all basic
 * types (String, Boolean, Integer, Long, Float, and Double).  Monitors on supported non-primitive types can
 * be created using {@link IPreferenceEntry#newMonitor(java.lang.Class, java.util.function.Consumer)}.
 * <p>
 * Monitors can be stopped and started as needed using {@link #stop()} and {@link #start()} respectively.  A monitor
 * is initially started upon creation.
 * <p>
 * <em>Note: When a consumer is finished using a monitor, it should call {@link #dispose()} in order to release our
 * resources and unregister internal listeners.</em>
 *
 * @since 2.35
 */
@NoPluginImplementations
public interface IPreferenceMonitor<T>
{
    /**
     * @return The current (type-specific) preference value
     */
    T getCurrentValue();

    /**
     * @return The previous (type-specific) preference value
     */
    T getPreviousValue();

    /**
     * @return The current String value of the preference
     */
    String getCurrentStringValue();

    /**
     * @return The previous String value of the preference
     */
    String getPreviousStringValue();

    /**
     * Stop monitoring for changes and dispose of any resources.
     */
    void dispose();

    /**
     * Start/resume monitoring the preference value for changes.
     * <p>
     * <em>Note: This method can be called more than once without an intervening call to {@link #stop()}.
     * Calls beyond the first will be ignored.</em>
     */
    void start();

    /**
     * Stop monitoring the preference value for changes.
     */
    void stop();
}
