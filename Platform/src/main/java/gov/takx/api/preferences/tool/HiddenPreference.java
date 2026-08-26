package gov.takx.api.preferences.tool;

import jakarta.annotation.Nonnull;

/**
 * Defines a {@link IToolPreference} that will NOT be displayed in the tool preferences page. This supports plugins
 * that need to store preference data, but not allow the user to see or edit the value.
 *
 * @since 5.5.1
 */
public class HiddenPreference extends AToolPreference
{
    /**
     * @param id The preference id. This id will be combined with the tool uid to form a unique preference entry id.
     *           See {@link IToolPreference#getId()}.
     * @param defaultValue The initial value to use if no value has been stored
     */
    public HiddenPreference(@Nonnull String id, @Nonnull String defaultValue)
    {
        super(id, "untitled", "no description", defaultValue);
    }
}