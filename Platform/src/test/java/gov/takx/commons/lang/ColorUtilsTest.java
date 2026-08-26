package gov.takx.commons.lang;

import org.junit.jupiter.api.Test;

import java.awt.Color;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @since 5.7
 */
public class ColorUtilsTest
{
    /**
     * Test cache and conversions.
     */
    @Test
    void testBasic()
    {
        var redHex = "FF0000";
        var red = ColorUtils.hexRGBToColor(redHex);
        assertThat(red).isEqualTo(Color.RED);
        var hex = ColorUtils.colorToHexRGB(Color.RED);
        assertThat(hex).isEqualTo(redHex);

        assertThat(ColorUtils.COLOR_HEX_RGB_MAP.size()).isEqualTo(1);
        assertThat(ColorUtils.HEX_RGB_COLOR_MAP.size()).isEqualTo(1);

        var def = ColorUtils.hexRGBToColor("ZZZZZZ", Color.MAGENTA);
        assertThat(def).isEqualTo(Color.MAGENTA);
        assertThat(ColorUtils.HEX_RGB_COLOR_MAP.size()).isEqualTo(1);

        ColorUtils.colorToHexRGB(Color.YELLOW);
        ColorUtils.colorToHexRGB(Color.BLUE);
        assertThat(ColorUtils.COLOR_HEX_RGB_MAP.size()).isEqualTo(3);
    }
}
