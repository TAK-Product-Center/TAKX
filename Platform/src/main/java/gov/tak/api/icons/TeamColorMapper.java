package gov.tak.api.icons;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.invoke.MethodHandles;
import java.util.HashMap;
import java.util.Map;

/**
 * Provides static method to convert a team color name to an equivalent RGB value.
 *
 * @since 3.0
 */
public final class TeamColorMapper
{
    private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());
    private static final Map<String, Integer> COLOR_NAME_MAP = new HashMap<>();

    static
    {
        for (TakColor value : TakColor.values()) COLOR_NAME_MAP.put(value.colorName, value.colorValue);
    }

    /**
     * Convert a team color name to an ARGB (actually RGB since all colors have an alpha value of 255).
     *
     * @param teamColorName Color name to map
     * @return Color RGB value for the named color
     */
    public static int teamToColor(String teamColorName)
    {
        return COLOR_NAME_MAP.computeIfAbsent(teamColorName.toLowerCase(), k -> {
            // Try decoding
            try
            {
                // Decoding as an integer will fail for values with an alpha of 0xFF, so decode as a long and cast
                return Long.decode(k).intValue();
            } catch (NumberFormatException e)
            {
                logger.warn("Can't parse color '{}'", k);
                return TakColor.WHITE.colorValue;
            }
        });
    }

    private TeamColorMapper()
    {
    }
}
