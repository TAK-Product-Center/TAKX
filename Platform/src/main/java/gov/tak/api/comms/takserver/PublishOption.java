package gov.tak.api.comms.takserver;

import jakarta.annotation.Nonnull;

import java.util.Arrays;

/**
 * Publishing options, used when specifying if timeline data should be published.
 *
 * @since 5.3 (Moved to platform)
 */
public enum PublishOption
{
    Always("Always publish"),
    Prompt("Prompt to publish"),
    Never("Never publish");

    public final String displayText;

    PublishOption(@Nonnull String displayText)
    {
        this.displayText = displayText;
    }

    /**
     * @return Display text of the option.
     * @since 5.3
     */
    @Nonnull
    public String getDisplayText()
    {
        return displayText;
    }

    /**
     * @param displayText Display text to map to a publish option value.
     * @return Associated option value, or {@link #Prompt} if there is no match.
     * @since 5.3
     */
    @Nonnull
    public static PublishOption getOption(@Nonnull String displayText)
    {
        return Arrays.stream(values())
                .filter(st -> st.displayText.equals(displayText))
                .findFirst()
                .orElse(Prompt);
    }

    @Override
    public String toString()
    {
        return displayText;
    }
}
