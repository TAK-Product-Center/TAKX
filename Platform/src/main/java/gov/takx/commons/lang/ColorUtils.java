package gov.takx.commons.lang;

import com.google.common.annotations.VisibleForTesting;
import gov.tak.api.annotation.IncubatingApi;
import jakarta.annotation.Nonnull;

import java.awt.Color;
import java.util.HashMap;
import java.util.Map;

/**
 * Utilities for working with {@link Color} instances.
 * 
 * @since 5.7
 */
@IncubatingApi(since = "5.7")
public final class ColorUtils
{
    // Two maps are used because there is an element of the Color object that we are filtering (alpha) but is included in the Color.equals() comparison
    @VisibleForTesting
    static final Map<Color, String> COLOR_HEX_RGB_MAP = new HashMap<>();
    @VisibleForTesting
    static final Map<String, Color> HEX_RGB_COLOR_MAP = new HashMap<>();

    private ColorUtils()
    {
    }

    /**
     * @implNote the {@link Color} constructor used here ignores the leftmost 8-bit block, which often represents alpha
     * @param hexString a hex string where the three rightmost 8-bit blocks represent RGB color components
     * @return the color object
     * @throws NumberFormatException if the string cannot be parsed as an integer
     */
    @Nonnull
    public static Color hexRGBToColor(@Nonnull String hexString)
    {
        return HEX_RGB_COLOR_MAP.computeIfAbsent(hexString, key -> new Color(Integer.parseUnsignedInt(hexString, 16)));
    }

    /**
     * @implNote the {@link Color} constructor used here ignores the leftmost 8-bit block, which often represents alpha
     * @param hexString a hex string where the three rightmost 8-bit blocks represent RGB color components
     * @param defaultColor a default Color
     * @return the color object or {@code defaultColor} if the hex String cannot be converted
     */
    @Nonnull
    public static Color hexRGBToColor(@Nonnull String hexString, @Nonnull Color defaultColor)
    {
        try
        {
            return hexRGBToColor(hexString);
        } catch (NumberFormatException e)
        {
            return defaultColor;
        }
    }

    /**
     * @param color the color object to convert
     * @return an uppercase hex string of the RGB components (no alpha) of the color
     */
    @Nonnull
    public static String colorToHexRGB(@Nonnull Color color)
    {
        return COLOR_HEX_RGB_MAP.computeIfAbsent(color, c -> String.format("%06X", color.getRGB() & 0xFFFFFF));
    }
}
