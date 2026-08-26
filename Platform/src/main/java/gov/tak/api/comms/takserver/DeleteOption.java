package gov.tak.api.comms.takserver;

import jakarta.annotation.Nonnull;

import java.util.Arrays;

/**
 * Deletion options, used when specifying if mission should be deleted to mirror a remote deletion on TAK Server.
 *
 * @since 5.7
 */
public enum DeleteOption
{
    Always("Always delete"),
    Prompt("Prompt to delete"),
    Never("Never delete");

    private final String displayText;

    DeleteOption(@Nonnull String displayText)
    {
        this.displayText = displayText;
    }

    /**
     * @return Display text of the option.
     */
    @Nonnull
    public String getDisplayText()
    {
        return displayText;
    }

    /**
     * @param displayText Display text to map to a deletion option value.
     * @return Associated option value, or {@link #Prompt} if there is no match.
     */
    @Nonnull
    public static DeleteOption getOption(@Nonnull String displayText)
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
