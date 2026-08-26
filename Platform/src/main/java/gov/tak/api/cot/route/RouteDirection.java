package gov.tak.api.cot.route;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import java.util.HashMap;
import java.util.Map;

/**
 * The direction for CoT routes.
 *
 * @since 3.0
 */
public enum RouteDirection
{
    INFILTRATION("Infil"),
    EXFILTRATION("Exfil");

    private static final Map<String, RouteDirection> textMap = new HashMap<>();

    static
    {
        for (RouteDirection direction : values())
        {
            textMap.put(direction.text, direction);
        }
    }

    private final String text;

    RouteDirection(String text)
    {
        this.text = text;
    }

    /**
     * @return The human readable text that should be used in CoT messages and displays.
     */
    @Nonnull
    public String getDisplayText()
    {
        return text;
    }

    /**
     * Gets the enum literal that corresponds to the provided text.
     *
     * @param text The text to convert
     * @return The corresponding enum literal or {@code null} if none was found.
     */
    @Nullable
    public static RouteDirection fromText(String text)
    {
        return textMap.get(text);
    }
}
