package gov.takx.commons.swing;

import com.google.common.annotations.VisibleForTesting;
import gov.tak.api.annotation.IncubatingApi;
import jakarta.annotation.Nonnull;

import javax.swing.Icon;
import javax.swing.SwingConstants;

import java.awt.Component;
import java.awt.Graphics;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;

/**
 * Simple {@link Icon} implementation that renders other icons in a layered fashion during the paint cycle and allows setting
 * alignment of layers to handle icons with sizes different from the base. By default, the combined image is cached and
 * re-rendered only when the layer collection is altered. This cache should be disabled if any of the icon layers has dynamic
 * content.
 *
 * @since 5.8
 */
@IncubatingApi(since = "5.8")
public class LayeredIcon implements Icon
{
    private final int width;
    private final int height;
    private final List<IconLayer> layers = new ArrayList<>();
    @VisibleForTesting
    BufferedImage image;
    private boolean cached = true;

    /**
     * Construct a new layered icon with the given icon as the base layer, using its dimensions as this icon's dimensions.
     *
     * @param icons the icons array, where the first element is assumed to be the base icon that will control size 
     * @since 5.8
     */
    public LayeredIcon(@Nonnull Icon... icons)
    {
        this(icons[0].getIconWidth(), icons[0].getIconHeight());
        for (var icon : icons) addLayer(icon);
    }

    /**
     * Construct a new layered icon with the given width and height and no layers.
     *
     * @param width the icon width
     * @param height the icon height
     */
    public LayeredIcon(int width, int height)
    {
        this.width = width;
        this.height = height;
    }

    @Override
    public void paintIcon(Component c, Graphics g, int x, int y)
    {
        g.drawImage(getImage(c), x, y, null);
    }

    @Override
    public int getIconWidth()
    {
        return width;
    }

    @Override
    public int getIconHeight()
    {
        return height;
    }

    /**
     * Update a layer with a new icon using {@link SwingConstants#CENTER} for horizontal and vertical alignment.
     *
     * @param index the layer index
     * @param icon the icon
     */
    public void setLayer(int index, @Nonnull Icon icon)
    {
        setLayer(index, icon, SwingConstants.CENTER, SwingConstants.CENTER);
    }

    /**
     * Update a layer with a new icon and alignment.
     *
     * @param index the layer index
     * @param icon the icon
     * @param horizontalAlignment the horizontal alignment (See {@link SwingConstants})
     * @param verticalAlignment the vertical alignment (See {@link SwingConstants})
     */
    public void setLayer(int index, @Nonnull Icon icon, int horizontalAlignment, int verticalAlignment)
    {
        checkDimension(icon);
        layers.set(index, new IconLayer(icon, padding(horizontalAlignment, getIconWidth(), icon.getIconWidth()), padding(verticalAlignment, getIconHeight(), icon.getIconHeight())));
        clearCache();
    }

    /**
     * Add an icon layer using {@link SwingConstants#CENTER} for horizontal and vertical alignment.
     * 
     * @param icon the icon
     */
    public void addLayer(@Nonnull Icon icon)
    {
        addLayer(icon, SwingConstants.CENTER, SwingConstants.CENTER);
    }

    /**
     * Add an icon layer.
     *
     * @param icon the icon
     * @param horizontalAlignment the horizontal alignment (See {@link SwingConstants})
     * @param verticalAlignment the vertical alignment (See {@link SwingConstants})
     */
    public void addLayer(@Nonnull Icon icon, int horizontalAlignment, int verticalAlignment)
    {
        checkDimension(icon);
        layers.add(new IconLayer(icon, padding(horizontalAlignment, getIconWidth(), icon.getIconWidth()), padding(verticalAlignment, getIconHeight(), icon.getIconHeight())));
        clearCache();
    }

    /**
     * @return {@code true} if the combined image is cached, or {@code false} if it is rendered each paint cycle 
     */
    public boolean isCached()
    {
        return cached;
    }

    /**
     * @param cached {@code true} if the combined image should be cached, or {@code false} if it should be rendered each
     * paint cycle
     */
    public void setCached(boolean cached)
    {
        this.cached = cached;
    }

    @Nonnull
    private BufferedImage getImage(@Nonnull Component c)
    {
        if (image == null || !cached)
        {
            image = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
            var graphics = image.createGraphics();
            for (var layer : layers)
            {
                layer.icon.paintIcon(c, graphics, layer.widthPad, layer.heightPad);
            }
            graphics.dispose();
        }
        return image;
    }

    private void clearCache()
    {
        image = null;
    }

    private void checkDimension(@Nonnull Icon icon)
    {
        if (icon.getIconWidth() > getIconWidth() || icon.getIconHeight() > getIconHeight())
        {
            throw new IllegalArgumentException("Icon dimension " + icon.getIconWidth() + " x " + icon.getIconHeight() + " is larger than base dimension " + getIconWidth() + " x " + getIconHeight());
        }
    }

    /**
     * Calculates the necessary padding based on an alignment flag and two sizes.
     */
    private static int padding(int alignment, int base, int other)
    {
        return switch (alignment)
        {
            case SwingConstants.LEFT, SwingConstants.TOP -> 0;
            case SwingConstants.RIGHT, SwingConstants.BOTTOM -> base - other;
            case SwingConstants.CENTER -> (base - other) / 2;
            default -> throw new IllegalArgumentException("Unexpected alignment: " + alignment);
        };
    }

    private record IconLayer(@Nonnull Icon icon, int widthPad, int heightPad)
    {
    }
}
