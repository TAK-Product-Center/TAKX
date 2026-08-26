package gov.takx.api.preferences.tool;

import jakarta.annotation.Nonnull;

/**
 * Defines a boolean {@link gov.takx.api.preferences.IPreferenceEntry} that will be controlled by a checkbox. The
 * preference entry's boolean value will correspond to the checkbox's selected state.
 * 
 * @since 5.0
 */
public class CheckBoxPreference extends AToolPreference
{
    /**
     * @param id The preference id. This id will be combined with the tool uid to form a unique preference entry id.
     *           See {@link IToolPreference#getId()}.
     * @param title The title that will be displayed in the tool preference list
     * @param description The description that will be displayed in the tool preference list
     * @param defaultValue The initial value to use if no value has been stored
     */
    public CheckBoxPreference(@Nonnull String id, @Nonnull String title, @Nonnull String description,
                              boolean defaultValue)
    {
        super(id, title, description, defaultValue);
    }
}