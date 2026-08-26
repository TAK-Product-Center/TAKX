package gov.takx.commons.swing;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import java.awt.Color;
import java.awt.Paint;
import java.awt.PaintContext;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.geom.AffineTransform;
import java.awt.geom.Rectangle2D;
import java.awt.image.ColorModel;
import java.awt.image.Raster;
import java.awt.image.WritableRaster;

/**
 */
@SuppressWarnings("unused")
public class GlossyPaint implements Paint
{
    private int gradientStrength = 90;
    private int shinePosition = 35;
    private float shineStrength = 0.0f;
    private int shineCurve = 30;
    private final Color color;

    /**
     * @param color color of the background gradient
     */
    public GlossyPaint(@Nonnull Color color)
    {
        this.color = color;
    }

    /**
     * @param color            color of the background gradient
     * @param gradientStrength intensity of the background gradient (must be &gt;= 0)
     * @param shineStrength    intensity of the bright top part (must be between 0 and 1)
     * @param shinePosition    height of the bright top part
     * @param shineCurve       curve of the bright top part (must be &gt;= 1)
     */
    public GlossyPaint(@Nonnull Color color, int gradientStrength, float shineStrength, int shinePosition, int shineCurve)
    {
        if (gradientStrength < 0)
        {
            throw new IllegalArgumentException("Gradient strength must be >= 0");
        }
        if (shineStrength < 0 || shineStrength > 1)
        {
            throw new IllegalArgumentException("Shine strength must be between 0 and 1");
        }
        if (shineCurve < 1)
        {
            throw new IllegalArgumentException("Shine curve must be >= 1");
        }

        this.color = color;
        this.gradientStrength = gradientStrength;
        this.shineStrength = shineStrength;
        this.shinePosition = shinePosition;
        this.shineCurve = shineCurve;
    }

    @Nonnull
    public Color getColor()
    {
        return color;
    }

    public int getGradientStrength()
    {
        return gradientStrength;
    }

    public int getShineCurve()
    {
        return shineCurve;
    }

    public int getShinePosition()
    {
        return shinePosition;
    }

    public float getShineStrength()
    {
        return shineStrength;
    }

    @Nonnull
    @Override
    public PaintContext createContext(@Nullable ColorModel cm, @Nonnull Rectangle deviceBounds, @Nullable Rectangle2D userBounds,
                                      @Nullable AffineTransform xform, @Nullable RenderingHints hints)
    {
        return new GlossyPaintContext(deviceBounds);
    }

    @Override
    public int getTransparency()
    {
        return color.getTransparency();
    }

    private class GlossyPaintContext implements PaintContext
    {
        private final Rectangle2D userBounds;
        private final int c;
        private final int r2;
        private final int r;
        private final int g;
        private final int b;
        private final int a;

        public GlossyPaintContext(@Nonnull Rectangle2D userBounds)
        {
            this.userBounds = userBounds;
            // center of the curve
            c = (int) (userBounds.getY() + shinePosition + ((userBounds.getWidth() / 2.0f) * (userBounds.getWidth() / 2.0f) - (shineCurve * shineCurve)) / (2 * shineCurve));
            // radius squared of the curve
            r2 = (c - shinePosition + shineCurve) * (c - shinePosition + shineCurve);
            r = color.getRed();
            g = color.getGreen();
            b = color.getBlue();
            a = color.getAlpha();
        }

        @Override
        public void dispose()
        {
        }

        @Nonnull
        @Override
        public ColorModel getColorModel()
        {
            return ColorModel.getRGBdefault();
        }

        @Nonnull
        @Override
        public Raster getRaster(int x, int y, int w, int h)
        {
            WritableRaster raster = getColorModel().createCompatibleWritableRaster(w, h);
            int[] data = new int[w * h * 4];

            x = (int) (x - userBounds.getX());
            y = (int) (y - userBounds.getY());

            for (int j = 0; j < h; j++)
            {
                float delta = (float) gradientStrength * (j + y) / (float) userBounds.getHeight(); // for the background gradient
                int valR = (int) (r - delta);
                valR = valR < 0 ? 0 : (Math.min(valR, 255));
                int valG = (int) (g - delta);
                valG = valG < 0 ? 0 : (Math.min(valG, 255));
                int valB = (int) (b - delta);
                valB = valB < 0 ? 0 : (Math.min(valB, 255));
                for (int i = 0; i < w; i++)
                {
                    int base = (j * w + i) * 4;
                    data[base] = valR;
                    data[base + 1] = valG;
                    data[base + 2] = valB;
                    data[base + 3] = a;

                    // if in the bright top part :
                    final double midX = i + x - userBounds.getWidth() / 2.0f;
                    if (j + y <= shinePosition && j + y <= c - Math.sqrt(r2 - midX * midX))
                    {
                        data[base] = (int) (data[base] + (255 - data[base]) * shineStrength);
                        data[base + 1] = (int) (data[base + 1] + (255 - data[base + 1]) * shineStrength);
                        data[base + 2] = (int) (data[base + 2] + (255 - data[base + 2]) * shineStrength);
                    }
                }
            }

            raster.setPixels(0, 0, w, h, data);
            return raster;
        }
    }
}
