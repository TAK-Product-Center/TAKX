package gov.takx.core.surfaces;

import gov.nasa.worldwind.render.ScreenImage;
import gov.nasa.worldwind.util.WWMath;
import gov.nasa.worldwind.util.WWUtil;
import gov.takx.api.surfaces.IAnalyticSurfaceLegend;
import gov.takx.platform.globe.GlobeTextUtils;
import jakarta.annotation.Nonnull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.Color;
import java.awt.Font;
import java.awt.Point;
import java.awt.geom.Point2D;
import java.lang.invoke.MethodHandles;
import java.text.Format;
import java.util.ArrayList;

/**
 * Simple facade around the WW implementation to unify it with our interface.  We have to re-implement all the static
 * factory methods in order to generate our derived types.
 *
 */
public class AnalyticSurfaceLegend extends gov.nasa.worldwindx.examples.analytics.AnalyticSurfaceLegend implements IAnalyticSurfaceLegend
{

    private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    /**
     * Construct an {@link IAnalyticSurfaceLegend} with Color computed from the min/max values and hues.
     * The color hue is computed by mapping value's in the range {@code [minValue, maxValue]} to the range of
     * color hues {@code [minHue, maxHue]}.  The legend will have a default height of 256, width of 32, and a
     * border color of white.
     *
     * @param minValue   the minimum value.
     * @param maxValue   the maximum value.
     * @param minHue     the minimum color hue, corresponding to the minimum value.
     * @param maxHue     the maximum color hue, corresponding to the maximum value.
     * @param labels     set of labels to apply at even spacing along the legend
     * @param titleLabel The label to use for the title of the legend
     * @return new legend
     */
    public static AnalyticSurfaceLegend fromColorGradient(double minValue, double maxValue, double minHue,
                                                          double maxHue,
                                                          Iterable<? extends IAnalyticSurfaceLegend.LabelAttributes> labels,
                                                          IAnalyticSurfaceLegend.LabelAttributes titleLabel)
    {
        return fromColorGradient(DEFAULT_WIDTH, DEFAULT_HEIGHT, minValue, maxValue, minHue, maxHue, DEFAULT_COLOR,
                labels, titleLabel);
    }

    /**
     * Construct an {@link IAnalyticSurfaceLegend} with Color computed from the min/max values and hues.
     * The color hue is computed by mapping value's in the range {@code [minValue, maxValue]} to the range of
     * color hues {@code [minHue, maxHue]}.  The legend will have the specified height and width (in pixels).
     *
     * @param width       The height of the legend
     * @param height      The width of the legend
     * @param minValue    the minimum value.
     * @param maxValue    the maximum value.
     * @param minHue      the minimum color hue, corresponding to the minimum value.
     * @param maxHue      the maximum color hue, corresponding to the maximum value.
     * @param borderColor Color of the border to draw around the legend
     * @param labels      set of labels to apply at even spacing along the legend
     * @param titleLabel  The label to use for the title of the legend
     * @return new legend
     */
    public static AnalyticSurfaceLegend fromColorGradient(int width, int height, double minValue, double maxValue,
                                                          double minHue, double maxHue, Color borderColor,
                                                          Iterable<? extends IAnalyticSurfaceLegend.LabelAttributes> labels,
                                                          IAnalyticSurfaceLegend.LabelAttributes titleLabel)
    {
        AnalyticSurfaceLegend legend = new AnalyticSurfaceLegend();
        legend.screenImage = new ScreenImage();
        legend.screenImage.setImageSource(legend.createColorGradientLegendImage(width, height, minHue, maxHue,
                borderColor));
        legend.labels = legend.createColorGradientLegendLabels(width, height, minValue, maxValue, labels, titleLabel);

        return legend;
    }

    /**
     * Create a set of default labels for use in a legend that span the range {@code [minValue, maxValue]}.  The set
     * will contain 5 labels formatted according to the format provided.  The labels will use an Arial-bold-12 font
     * and a white color.
     *
     * @param minValue Min data value
     * @param maxValue Max data value
     * @param format   Format to apply to the interpolation value to generate the label text
     * @return set of labels
     */
    public static Iterable<? extends IAnalyticSurfaceLegend.LabelAttributes> createDefaultColorGradientLabels(double minValue,
                                                                                                              double maxValue,
                                                                                                              @Nonnull Format format)
    {
        ArrayList<IAnalyticSurfaceLegend.LabelAttributes> labels = new ArrayList<>();

        int numLabels = 5;
        for (int i = 0; i < numLabels; i++)
        {
            double value = WWMath.mix(i / (double) (numLabels - 1), minValue, maxValue);

            String text = format.format(value);
            if (!WWUtil.isEmpty(text))
            {
                labels.add(createLegendLabelAttributes(value, text, GlobeTextUtils.ANALYTIC_SURFACE_LEGEND_FONT, Color.WHITE, 5d, 0d));
            }
        }

        return labels;
    }

    /**
     * Construct a label attribute using the given text with default font, color and offset.  The label will use an
     * Arial-bold-16 font and a white color.
     *
     * @param text The text to use in the label
     * @return {@link IAnalyticSurfaceLegend.LabelAttributes} instance
     */
    public static IAnalyticSurfaceLegend.LabelAttributes createDefaultTitle(@Nonnull String text)
    {
        return createLegendLabelAttributes(0d, text, GlobeTextUtils.ANALYTIC_SURFACE_LEGEND_TITLE_FONT, Color.WHITE, 0d, -20d);
    }

    /**
     * Construct a label attribute using the provided values.
     *
     * @param value   Associated data value
     * @param text    Text of the label
     * @param font    Font to use for rendering text
     * @param color   Color of the text
     * @param xOffset X offset from the edge of the legend
     * @param yOffset Y offset from the top of the legend
     * @return {@link IAnalyticSurfaceLegend.LabelAttributes} instance
     */
    public static IAnalyticSurfaceLegend.LabelAttributes createLegendLabelAttributes(final double value,
                                                                                     final String text, final Font font, final Color color, final double xOffset, final double yOffset)
    {
        return new IAnalyticSurfaceLegend.LabelAttributes()
        {
            @Override
            public double getValue()
            {
                return value;
            }

            @Override
            public String getText()
            {
                return text;
            }

            @Override
            public Font getFont()
            {
                return font;
            }

            @Override
            public Color getColor()
            {
                return color;
            }

            @Override
            public Point2D getOffset()
            {
                return new Point2D.Double(xOffset, yOffset);
            }
        };
    }

    @Override
    public Point getScreenLocation()
    {
        return screenImage.getScreenLocation();
    }

    private AnalyticSurfaceLegend()
    {
    }
}
