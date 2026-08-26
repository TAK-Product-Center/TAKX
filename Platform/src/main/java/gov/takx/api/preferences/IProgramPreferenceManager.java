package gov.takx.api.preferences;

import gov.takx.api.annotation.NoPluginImplementations;

/**
 * Extension specific to Program Properties, supporting additional mechanisms for resolving preference values.
 *
 * @since 2.35
 */
@NoPluginImplementations
public interface IProgramPreferenceManager extends IPreferenceManager
{
    /**
     * Get the preference value given the IPreferenceEntry defining it.  The following locations will be
     * attempted, in sequence, when looking for a value:
     * <ol>
     * <li>System properties (defined via -D), accessed with {@link System#getProperty(String)}</li>
     * <li>Environment variables, accessed with {@link System#getenv(String)}</li>
     * <li>Program Properties</li>
     * </ol>
     * <p>
     * When accessing the system properties or the environment variables, any slash character ('/') will be
     * converted to a period ('.') in the key prior to lookup.  So, for example, the key "{@code apps/configuration}"
     * would be converted to "{@code apps.configuration}" when calling {@link System#getProperty(String)} and
     * {@link System#getenv(String)}.
     *
     * @param entry IPreferenceEntry to lookup
     * @return the preference value or the defaultValue if not found
     */
    @Override
    String getStringValue(IPreferenceEntry entry);
}
