package gov.takx.api.preferences.tool;

import gov.takx.api.annotation.NoPluginImplementations;
import gov.takx.api.preferences.IPreferenceEntry;
import jakarta.annotation.Nonnull;

/**
 * Defines a tool preference that will be used to create an {@link gov.takx.api.preferences.IPreferenceEntry} and a
 * corresponding control.
 *
 * @since 5.0
 */
@NoPluginImplementations
public interface IToolPreference
{
    /**
     * @return The preference title to display to users
     */
    @Nonnull
    String getTitle();

    /**
     * @return A brief description of how the preference will be used by the tool
     */
    @Nonnull
    String getDescription();

    /**
     * @return The initial value to use if one has not already been stored
     */
    @Nonnull
    String getDefaultValue();

    /**
     * The preference id. This id will be combined with the tool uid to form a unique {@link IPreferenceEntry} key in
     * the form [toolUid]/[id]. {@link java.util.prefs.PreferenceChangeEvent}s fired for this preference will include
     * a {@link IPreferenceEntry} that returns this combined key.
     *
     * @return The preference id
     */
    @Nonnull
    String getId();
}