package gov.takx.commons.swing;

import android.graphics.Color;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.Dimension;
import java.awt.image.BufferedImage;
import java.lang.invoke.MethodHandles;

import static org.assertj.core.api.Assertions.assertThat;

/**
 */
public class ImageUtilsTest
{
    private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    @Test
    public void imageResizeRespectingAspectRatioTest()
    {
        logger.info("Image resize respecting aspect ratio test");

        final int width1 = 60;
        final int height1 = 180;
        Dimension d1 = ImageUtils.getScaledRespectingAspectRatio(width1, height1, width1 / 3, height1);
        assertThat(d1).isNotNull();
        assertThat(d1.width).isEqualTo(width1 / 3);
        assertThat(d1.height).isEqualTo(height1 / 3);

        final int width2 = 1000;
        final int height2 = 10;
        Dimension d2 = ImageUtils.getScaledRespectingAspectRatio(width2, height2, width2 / 2, height2 / 10);
        assertThat(d2).isNotNull();
        assertThat(d2.width).isEqualTo(width2 / 10);
        assertThat(d2.height).isEqualTo(height2 / 10);

        final int width3 = 400;
        final int height3 = 100;

        BufferedImage img1 = new BufferedImage(width3, height3, BufferedImage.TYPE_INT_RGB);
        BufferedImage img2 = ImageUtils.getScaledInstanceRespectingAspectRatio(img1, width3 / 4, height3);

        assertThat(img2).isNotNull();
        assertThat(img2.getWidth()).isEqualTo(width3 / 4);
        assertThat(img2.getHeight()).isEqualTo(height3 / 4);

        final int width4 = 200;
        final int height4 = 800;
        img1 = new BufferedImage(width4, height4, BufferedImage.TYPE_INT_RGB);
        img2 = ImageUtils.getScaledInstanceRespectingAspectRatio(img1, width4, height4 / 2);
        assertThat(img2).isNotNull();
        assertThat(img2.getWidth()).isEqualTo(width4 / 2);
        assertThat(img2.getHeight()).isEqualTo(height4 / 2);
    }

    @Test
    public void getPredominantColor_returnsExpectedColor()
    {
        final var defaultColor = Color.BLUE;

        final var image = new BufferedImage(20, 20, BufferedImage.TYPE_INT_ARGB);

        // 100% red, alpha = 255
        colorImage(image, Color.RED, 100, defaultColor);

        int result = ImageUtils.getPredominantColor(image, defaultColor);
        assertThat(result).isEqualTo(Color.RED);

        result = ImageUtils.getPredominantColor(image, defaultColor, 0);
        assertThat(result).isEqualTo(Color.RED);

        result = ImageUtils.getPredominantColor(image, defaultColor, 255);
        assertThat(result).isEqualTo(Color.RED);

        // 60% red, alpha = 1
        final var paleRed = Color.RED & 0x01FFFFFF;
        colorImage(image, paleRed, 60, defaultColor);

        result = ImageUtils.getPredominantColor(image, defaultColor);
        assertThat(result).isEqualTo(paleRed);

        result = ImageUtils.getPredominantColor(image, defaultColor, 1);
        assertThat(result).isEqualTo(paleRed);

        result = ImageUtils.getPredominantColor(image, defaultColor, 2);
        assertThat(result).isEqualTo(defaultColor);

        // 49% red, alpha = 255
        colorImage(image, Color.RED, 49, defaultColor);

        result = ImageUtils.getPredominantColor(image, defaultColor);
        assertThat(result).isEqualTo(defaultColor);
    }

    /**
     * Sets the pixels in an image to the colors to a test color, switching to a default color once the specified
     * percentage of pixels have been colored with the test color.
     * 
     * @param image The image to update
     */
    private void colorImage(BufferedImage image, int testColor, int percentLimit, int defaultColor)
    {
        final var width = image.getWidth();
        final var height = image.getHeight();
        final var limit = width * height * percentLimit * 0.01;

        int count = 0;

        for (int y = 0; y < width; y++)
        {
            for (int x = 0; x < height; x++) image.setRGB(x, y, count++ < limit ? testColor : defaultColor);
        }
    }
}
