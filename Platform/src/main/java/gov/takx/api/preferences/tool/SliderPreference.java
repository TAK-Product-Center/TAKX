package gov.takx.api.preferences.tool;

import jakarta.annotation.Nonnull;

/**
 * Defines an integer {@link gov.takx.api.preferences.IPreferenceEntry} that will be controlled by a JSlider. The 
 * preference entry's integer value will correspond to the slider value.
 * 
 * @since 5.0
 */
public class SliderPreference extends AToolPreference
{
    private final int min;
    private final int max;

    /**
     * @param id The preference id. This id will be combined with the tool uid to form a unique preference entry id.
     *           See {@link IToolPreference#getId()}.
     * @param title The title that will be displayed in the tool preference list
     * @param description The description that will be displayed in the tool preference list
     * @param defaultValue The initial slider value to use if no value has been stored
     * @param min The minimum slider value
     * @param max The maximum slider value
     */
    public SliderPreference(@Nonnull String id, @Nonnull String title, @Nonnull String description, int defaultValue,
                            int min, int max)
    {
        super(id, title, description, defaultValue);
        this.min = min;
        this.max = max;
    }

    /**
     * @return The min slider value
     */
    public int getMin()
    {
        return min;
    }

    /**
     * @return The max slider value
     */
    public int getMax()
    {
        return max;
    }
}