package gov.tak.api.cot.route;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import java.util.HashMap;
import java.util.Map;

/**
 * The order in which waypoints are named for a CoT route.
 *
 * @since 3.4
 */
public enum RouteOrder
{
    ASCENDING("Ascending Check Points"),
    DESCENDING("Descending Check Points");

    private static final Map<String, RouteOrder> textMap = new HashMap<>();

    static
    {
        for (RouteOrder order : values())
        {
            textMap.put(order.text, order);
        }
    }

    private final String text;

    RouteOrder(String text)
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
    public static RouteOrder fromText(String text)
    {
        return textMap.get(text);
    }
}
