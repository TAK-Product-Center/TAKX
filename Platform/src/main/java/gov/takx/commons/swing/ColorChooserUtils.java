package gov.takx.commons.swing;

import gov.takx.commons.lang.ColorUtils;
import net.java.dev.colorchooser.Palette;

import java.awt.Color;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 */
public final class ColorChooserUtils
{
    private static final List<Color> colors = new ArrayList<>();
    private static final List<String> strings = new ArrayList<>();
    private static final List<Palette> palettes = new ArrayList<>();
    private static final Palette[] defaultPalettes = Palette.getDefaultPalettes(true);

    static
    {
        for (int i = 0; i < 24; i++) colors.add(Color.getHSBColor(i / 26f, 1f, 1f));
        colors.add(new Color(118, 56, 27)); // Add brown to the pallet
        colors.add(Color.YELLOW);
        colors.add(Color.WHITE);
        colors.add(Color.BLACK);
        colors.add(Color.DARK_GRAY);
        colors.add(Color.GRAY);
        colors.add(Color.LIGHT_GRAY);

        for (int i = 0; i < colors.size(); i++) strings.add(i, "");

        Palette palette = Palette.createPredefinedPalette("Colors", colors.toArray(new Color[0]), strings.toArray(new String[0]));

        palettes.add(palette);
        palettes.addAll(Arrays.asList(defaultPalettes).subList(0, defaultPalettes.length - 1));
    }

    public static Palette[] getDefaultPalettes()
    {
        return palettes.toArray(new Palette[0]);
    }

    /**
     * @apiNote Prefer using {@link ColorUtils#hexRGBToColor(String)} unless this specific implementation is absolutely required
     * @param colorString a hex string where lowest 3 integer bit ranges represent RGB color components
     * @return the Color for the hex string or {@code Color.YELLOW} if the hex cannot be converted
     */
    public static Color getColorFromString(String colorString)
    {
        return ColorUtils.hexRGBToColor(colorString, Color.YELLOW);
    }

    /**
     * Determines an appropriate text color which will be visible against the provided background color.
     *
     * @param backgroundColor The background color for which to find an appropriate foreground text color
     * @return The foreground text color which will be visible
     * @since 3.3
     * @see <a href="https://stackoverflow.com/a/13030061">Code is copied from this Stack Overflow answer</a>
     */
    public static Color getVisibleTextColor(Color backgroundColor)
    {
        // Convert RGB to YIQ and use the Y value (which represents brightness) to determine whether the foreground
        // color should be white or black
        double y = (299 * backgroundColor.getRed() + 587 * backgroundColor.getGreen() + 114 * backgroundColor.getBlue()) / 1000.0;
        return y >= 128 ? Color.black : Color.white;
    }

    /**
     * No instances.
     */
    private ColorChooserUtils()
    {
    }
}
