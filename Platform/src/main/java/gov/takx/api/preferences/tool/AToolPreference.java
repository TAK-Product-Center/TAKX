package gov.takx.api.preferences.tool;

import jakarta.annotation.Nonnull;

/**
 * Base class for tool preferences that define a {@link gov.takx.api.preferences.IPreferenceEntry} and its control.
 *
 * @since 5.0
 */
abstract class AToolPreference implements IToolPreference
{
    private final String id;
    private final String title;
    private final String description;
    private final String defaultValue;

    /**
     * @param id The preference id. This id will be combined with the tool uid to form a unique preference entry id.
     *           See {@link IToolPreference#getId()}.
     * @param title The title that will be displayed in the tool preference list
     * @param description The description that will be displayed in the tool preference list
     * @param defaultValue The initial value to use if no value has been stored
     */
    AToolPreference(@Nonnull String id, @Nonnull String title, @Nonnull String description, @Nonnull Object defaultValue)
    {
        this.id = id;
        this.title = title;
        this.description = description;
        this.defaultValue = defaultValue.toString();
    }

    @Nonnull
    @Override
    public String getTitle()
    {
        return title;
    }

    @Nonnull
    @Override
    public String getDescription()
    {
        return description;
    }

    @Nonnull
    @Override
    public String getDefaultValue()
    {
        return defaultValue;
    }

    @Nonnull
    @Override
    public String getId()
    {
        return id;
    }
}