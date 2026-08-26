package gov.tak.api.icons;

import jakarta.annotation.Nonnull;
import org.apache.commons.text.WordUtils;

import java.awt.Color;
import java.util.List;

/**
 * A collection of colors used in TAKX/TAK products. Can be used to get various specific palettes
 * @since 5.3
 */
public enum TakColor
{
    AQUA("aqua", 0xFF00FFFF),
    BLACK("black", 0xFF000000),
    BLUE("blue", 0xFF0000FF),
    BROWN("brown", 0xFFA0714F),
    CYAN("cyan", 0xFF00FFFF),
    DARK_BLUE("dark blue", 0xFF00007F),
    DARK_GREEN("dark green", 0xFF008000),
    DARK_GRAY("darkgray", 0xFF444444),
    DARK_GREY("darkgrey", 0xFF444444),
    FUCHSIA("fuchsia", 0xFFFF00FF),
    GRAY("gray", 0xFF888888),
    GREEN("green", 0xFF00FF00),
    GREY("grey", 0xFF888888),
    LIGHT_GRAY("lightgray", 0xFFCCCCCC),
    LIGHT_GREY("lightgrey", 0xFFCCCCCC),
    LIME("lime", 0xFF00FF00),
    MAGENTA("magenta", 0xFFFF00FF),
    MAROON("maroon", 0xFF800000),
    NAVY("navy", 0xFF000080),
    OLIVE("olive", 0xFF808000),
    ORANGE("orange", 0xFFFF7700),
    PURPLE("purple", 0xFF800080),
    RAD_SENSOR("rad sensor", 0xFFFFFF00),
    RED("red", 0xFFFF0000),
    SILVER("silver", 0xFFC0C0C0),
    TEAL("teal", 0xFF008080),
    WHITE("white", 0xFFFFFFFF),
    YELLOW("yellow", 0xFFFFFF00);

    /** @since 5.6 **/
    public static final TakColor[] VALUES = values();

    public final String colorName;
    public final int colorValue;

    TakColor(String colorName, int colorValue)
    {
        this.colorName = colorName;
        this.colorValue = colorValue;
    }

    /**
     * @return the {@link Color} associated with this value
     */
    @Nonnull
    public Color getColor()
    {
        return new Color(colorValue);
    }

    /**
     * @return a {@link List} of colors used in marker-specific color selection
     */
    public static List<TakColor> getMarkerPalette()
    {
        return List.of(WHITE, YELLOW, ORANGE, MAGENTA, RED, MAROON, PURPLE, DARK_BLUE, BLUE, CYAN, TEAL, GREEN,
                DARK_GREEN, GRAY, BLACK);
    }

    /**
     * @return a {@link List} of colors used in team-specific color selection
     */
    public static List<TakColor> getTeamPalette()
    {
        return List.of(WHITE, YELLOW, ORANGE, MAGENTA, RED, MAROON, PURPLE, DARK_BLUE, BLUE, CYAN, TEAL, GREEN,
                DARK_GREEN, BROWN);
    }

    /**
     * @return a List of colors used for {@link gov.takx.api.breadcrumb.ITrack tracks}
     */
    public static List<TakColor> getTrackPalette()
    {
        return List.of(RED, ORANGE, YELLOW, GREEN, CYAN, BLUE, PURPLE, WHITE);
    }

    /**
     * @return a {@link List} of color names as Strings that are used for team coloration
     */
    public static List<String> getTeamPaletteNames()
    {
        return getTeamPalette().stream().map(takColor -> WordUtils.capitalize(takColor.colorName)).toList();
    }

    /**
     * @return the color enum matching either the {@link #name()} value or the {@link #colorName} field
     * @throws IllegalArgumentException if the string cannot be parsed
     * @since 5.6
     */
    @Nonnull
    public static TakColor parseColor(@Nonnull String s)
    {
        try
        {
            return valueOf(s);
        } catch (IllegalArgumentException e)
        {
            for (TakColor color : VALUES)
            {
                if (color.colorName.equalsIgnoreCase(s)) return color;
            }
            throw e;
        }
    }
}
