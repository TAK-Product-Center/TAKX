package gov.tak.api.cot.route;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import java.util.HashMap;
import java.util.Map;

/**
 * Indicates whether a route is the primary or secondary route.
 *
 * @since 3.4
 */
public enum RouteType
{
    PRIMARY("Primary"),
    SECONDARY("Secondary");

    private static final Map<String, RouteType> textMap = new HashMap<>();

    static
    {
        for (RouteType precedence : values())
        {
            textMap.put(precedence.text, precedence);
        }
    }

    private final String text;

    RouteType(String text)
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
    public static RouteType fromText(String text)
    {
        return textMap.get(text);
    }
}
