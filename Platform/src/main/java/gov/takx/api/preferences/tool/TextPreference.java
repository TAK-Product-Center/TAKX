package gov.takx.api.preferences.tool;

import jakarta.annotation.Nonnull;

/**
 * Defines a String {@link gov.takx.api.preferences.IPreferenceEntry} that will be controlled by a JTextField. The
 * preference entry's String value will correspond to the value displayed in the text field.
 *
 * @since 5.0
 */
public class TextPreference extends AToolPreference
{
    /**
     * @param id The preference id. This id will be combined with the tool uid to form a unique preference entry id.
     *           See {@link IToolPreference#getId()}.
     * @param title The title that will be displayed in the tool preference list
     * @param description The description that will be displayed in the tool preference list
     * @param defaultValue The initial text value to use if no value has been stored
     */
    public TextPreference(@Nonnull String id, @Nonnull String title, @Nonnull String description,
                          @Nonnull String defaultValue)
    {
        super(id, title, description, defaultValue);
    }
}