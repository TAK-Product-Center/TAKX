package gov.takx.api.surfaces;

import gov.nasa.worldwindx.examples.analytics.AnalyticSurfaceLegend;

import java.awt.Color;
import java.awt.Font;
import java.awt.Point;
import java.awt.geom.Point2D;

/**
 * This interface defines the mechanism for manipulating the legend for an analytic surface.  All of the text
 * that is rendered as part of the legend is handled using a {@link IAnalyticSurfaceLegend.LabelAttributes} interface
 * that unifies the specification of the text, font, color, and relative offset from the legend border.
 *
 * @since 1.4
 */
public interface IAnalyticSurfaceLegend
{

    /**
     * Holds the values needed for rendering a piece of text as part of the legend.  Instances are created by
     * factory methods in {@link IAnalyticSurface}.
     *
     * @see IAnalyticSurface#createDefaultColorGradientLabels(double, double, java.text.Format)
     * @see IAnalyticSurface#createDefaultTitle(String)
     * @see IAnalyticSurface#createLegendLabelAttributes(double, String, java.awt.Font, java.awt.Color, double, double)
     */
    public interface LabelAttributes extends AnalyticSurfaceLegend.LabelAttributes
    {
        /**
         * @return the value associated with this label
         */
        double getValue();

        /**
         * @return The text of the label
         */
        String getText();

        /**
         * @return The font used to render the text
         */
        Font getFont();

        /**
         * @return The color used to render the text
         */
        Color getColor();

        /**
         * @return The X and Y offset from the legend border at which to render the text
         */
        Point2D getOffset();
    }

    /**
     * @return If the legend is visible or not
     */
    public boolean isVisible();

    /**
     * Specify if the legend is visible (being rendered) or not.
     *
     * @param visible Pass {@code true} to make the legend visible, {@code false} otherwise
     */
    public void setVisible(boolean visible);

    /**
     * @return The opacity value for the legend (between 0.0 and 1.0)
     */
    public double getOpacity();

    /**
     * Set the opacity of the legend.
     *
     * @param opacity Opacity setting, must be between 0.0 and 1.o, inclusive.
     */
    public void setOpacity(double opacity);

    /**
     * Returns the location of the legend on the screen. The position is relative to the upper-left corner of the World
     * Window.
     *
     * @return the current legend position.
     */
    public Point getScreenLocation();

    /**
     * Specifies the location of the legend on the screen. The position is relative to the upper-left corner of the World
     * Window. The legend is centered on this position.
     *
     * @param point the screen location on which to center the image. May be {@code null}, in which case the image is
     *              not displayed.
     */
    public void setScreenLocation(Point point);
}
