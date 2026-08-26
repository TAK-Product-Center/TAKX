package gov.takx.commons.swing;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;

import javax.swing.ImageIcon;
import javax.swing.JLabel;
import javax.swing.SwingConstants;

import java.awt.Color;
import java.awt.image.BufferedImage;

/**
 * @since 5.8
 */
public class LayeredIconTest
{
    @Test
    public void test()
    {
        var image0 = new BufferedImage(3, 3, BufferedImage.TYPE_INT_ARGB);
        var graphics = image0.getGraphics();
        graphics.setColor(Color.RED);
        graphics.fillRect(0, 0, 3, 3);
        graphics.dispose();

        var image1 = new BufferedImage(1, 1, BufferedImage.TYPE_INT_ARGB);
        graphics = image1.getGraphics();
        graphics.setColor(Color.BLUE);
        graphics.fillRect(0, 0, 1, 1);
        graphics.dispose();

        var image2 = new BufferedImage(1, 1, BufferedImage.TYPE_INT_ARGB);
        graphics = image2.getGraphics();
        graphics.setColor(Color.GREEN);
        graphics.fillRect(0, 0, 1, 1);
        graphics.dispose();

        var icon0 = new ImageIcon(image0);
        var icon1 = new ImageIcon(image1);
        var icon2 = new ImageIcon(image2);

        var component = new JLabel();
        var paintedImage = new BufferedImage(3, 3, BufferedImage.TYPE_INT_ARGB);
        var paintedImageGraphics = paintedImage.getGraphics();

        // start with 3x3 red icon
        var layeredIcon = new LayeredIcon(icon0);
        layeredIcon.paintIcon(component, paintedImageGraphics, 0, 0);
        Assertions.assertThat(paintedImage.getRGB(0, 0)).isEqualTo(Color.RED.getRGB());
        Assertions.assertThat(paintedImage.getRGB(1, 1)).isEqualTo(Color.RED.getRGB());

        // add 1x1 blue icon to center
        layeredIcon.addLayer(icon1, SwingConstants.CENTER, SwingConstants.CENTER);
        layeredIcon.paintIcon(component, paintedImageGraphics, 0, 0);
        Assertions.assertThat(paintedImage.getRGB(0, 0)).isEqualTo(Color.RED.getRGB());
        Assertions.assertThat(paintedImage.getRGB(1, 1)).isEqualTo(Color.BLUE.getRGB());

        // add 1x1 green icon to top left
        layeredIcon.addLayer(icon2, SwingConstants.LEFT, SwingConstants.TOP);
        layeredIcon.paintIcon(component, paintedImageGraphics, 0, 0);
        var priorImage = layeredIcon.image;
        Assertions.assertThat(paintedImage.getRGB(1, 0)).isEqualTo(Color.RED.getRGB());
        Assertions.assertThat(paintedImage.getRGB(0, 0)).isEqualTo(Color.GREEN.getRGB());
        Assertions.assertThat(paintedImage.getRGB(1, 1)).isEqualTo(Color.BLUE.getRGB());

        // add 1x1 green icon to bot right
        layeredIcon.addLayer(icon2, SwingConstants.RIGHT, SwingConstants.BOTTOM);
        layeredIcon.paintIcon(component, paintedImageGraphics, 0, 0);
        var newImage = layeredIcon.image;
        Assertions.assertThat(newImage).isNotSameAs(priorImage);
        Assertions.assertThat(paintedImage.getRGB(1, 0)).isEqualTo(Color.RED.getRGB());
        Assertions.assertThat(paintedImage.getRGB(0, 0)).isEqualTo(Color.GREEN.getRGB());
        Assertions.assertThat(paintedImage.getRGB(2, 2)).isEqualTo(Color.GREEN.getRGB());
        Assertions.assertThat(paintedImage.getRGB(1, 1)).isEqualTo(Color.BLUE.getRGB());

        priorImage = newImage;
        layeredIcon.paintIcon(component, paintedImageGraphics, 0, 0);
        newImage = layeredIcon.image;
        Assertions.assertThat(newImage).isSameAs(priorImage);

        layeredIcon.setCached(false);
        layeredIcon.paintIcon(component, paintedImageGraphics, 0, 0);
        newImage = layeredIcon.image;
        Assertions.assertThat(newImage).isNotSameAs(priorImage);
    }
}
