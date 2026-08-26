package gov.takx.api.surfaces;

import gov.nasa.worldwind.geom.LatLon;
import gov.nasa.worldwindx.examples.analytics.AnalyticSurface;

import java.awt.Color;
import java.awt.Font;
import java.text.Format;

/**
 * AnalyticSurface represents a connected grid of geographic locations, covering a specified Sector (lat/lon box) at a
 * specified base altitude in meters. The number of grid locations is defined by the AnalyticSurface's dimensions. The
 * default dimensions are {@code (10, 10)}. Callers specify the dimensions by using one of the constructors
 * accepting {@code width} and {@code height}, or by invoking {@link #setDimensions(int, int)}. Each grid
 * point has the following set of attributes: <ul> <li>Scalar value : the grid point's height relative to the surface's
 * base altitude, both in meters</li> <li>Color : the grid point's RGB color components</li> <li>Opacity : the grid
 * point's Alpha color component</li> </ul> Callers specify the attributes at each grid point by invoking {@link
 * #setSurfaceValues(Iterable)} with an {@link Iterable} of {@link GridPointAttributes}. Grid points are assigned attributes
 * from this iterable staring at the upper left hand corner, and proceeding in row-first order across the grid. The
 * iterable should contain at least {@code width * height} values, where width and height are the AnalyticSurface's
 * grid dimensions. If the caller does not specify any GridPointAttributes, or the caller specified iterable contains
 * too few values, the unassigned grid points are given default attributes: the default scalar value is 0, the default
 * color is {@link java.awt.Color#BLACK}, and the default opacity is 1 (totally opaque).
 * <p/>
 * <em>
 * It is best to perform all modifications to analytic surfaces (such as setting tbe boundaries, attributes, or values)
 * on the Swing EDT.  Since the underlying analytic surface could be in the middle of being rendered at the time the
 * mutator method is called, making the call on the EDT ensures that it is properly synchronized with the rendering
 * operations.
 * </em>
 *
 * @since 1.4
 */
public interface IAnalyticSurface
{
    void refreshSurface();

    void deleteSurface();

    /**
     * GridPointAttributes defines the properties associated with a single grid point of an AnalyticSurface.
     */
    interface GridPointAttributes extends AnalyticSurface.GridPointAttributes
    {
        /**
         * Returns the scalar value associated with a grid point. By default, AnalyticSurface interprets this value as
         * the grid point's height relative to the AnalyticSurface's base altitude, both in meters.
         *
         * @return the grid point's scalar value.
         */
        double getValue();

        /**
         * Returns the {@link java.awt.Color} associated with a grid point. By default, AnalyticSurface interprets this
         * Color as the RGBA components of a grid point's RGBA color, with the alpha channel controlling opacity.
         *
         * @return the grid point's RGBA color components.
         */
        java.awt.Color getColor();
    }

    /**
     * @return the display name for this surface
     */
    String getDisplayName();

    /**
     * Set the display name for this surface (used in the layer table).
     *
     * @param displayName New display name
     */
    void setDisplayName(String displayName);

    /**
     * Returns true if the surface is visible in the scene, and false otherwise.
     *
     * @return true if the surface is visible in the scene, and false otherwise
     */
    boolean isVisible();

    /**
     * Sets whether or not the surface is visible in the scene.
     *
     * @param visible true to make the surface visible, and false to make it hidden.
     */
    void setVisible(boolean visible);

    /**
     * Returns the {@link LatLon} defining the minimum geographic boundary of this surface.
     *
     * @return this surface's minimum geographic boundary, as a LatLon.
     */
    LatLon getMinCorner();

    /**
     * Sets this surface's minimum geographic boundary, as a {@link LatLon}.
     *
     * @param corner this surface's new minimum geographic boundary, as a LatLon.
     * @throws IllegalArgumentException if the corner is {@code null}.
     */
    void setMinCorner(LatLon corner);

    /**
     * Returns the {@link LatLon} defining the maximum geographic boundary of this surface.
     *
     * @return this surface's maximum geographic boundary, as a LatLon.
     */
    LatLon getMaxCorner();

    /**
     * Sets this surface's maximum geographic boundary, as a {@link LatLon}.
     *
     * @param corner this surface's new maximum geographic boundary, as a LatLon.
     * @throws IllegalArgumentException if the corner is {@code null}.
     */
    void setMaxCorner(LatLon corner);

    /**
     * Returns this surface's base altitude, in meters.
     *
     * @return this surface's base altitude, in meters.
     */
    double getAltitude();

    /**
     * Sets this surface's base altitude, in meters.
     *
     * @param altitude the new base altitude, in meters.
     */
    void setAltitude(double altitude);

    /**
     * Set the surface's altitude type:<br>
     * <br>
     * {@link gov.nasa.worldwind.WorldWind#ABSOLUTE}: The surface's altitude is relative to mean sea level.
     * This is the default behavior and the only option originally available.<br>
     * {@link gov.nasa.worldwind.WorldWind#RELATIVE_TO_GROUND}: The surface's altitude relative to the terrain
     * elevation beneath each grid point.
     * {@link gov.nasa.worldwind.WorldWind#CLAMP_TO_GROUND}: The surface's altitude is ignored; the surface is
     * drawn directly on the terrain.
     *
     * @param mode The desired altitude adjusting behavior for the analytic surface.
     * @since 1.7
     */
    void setAltitudeMode(int mode);

    /**
     * Returns the number of horizontal and vertical points composing this surface as an array with two values. The
     * value at index 0 indicates the grid width, and the value at index 1 indicates the grid height.
     *
     * @return the dimensions of this surface's grid.
     */
    int[] getDimensions();

    /**
     * Sets the number of horizontal and vertical points composing this surface.
     *
     * @param width  the new grid width.
     * @param height the new grid height.
     * @throws IllegalArgumentException if either width or height are less than 1.
     */
    void setDimensions(int width, int height);

    /**
     * Returns the surface's iterable of {@link GridPointAttributes}. See {@link #setSurfaceValues(Iterable)} for details on
     * how this iterable is interpreted by AnalyticSurface.
     *
     * @return this surface's GridPointAttributes.
     */
    Iterable<? extends GridPointAttributes> getSurfaceValues();

    /**
     * Sets this surface's iterable of {@link GridPointAttributes}. Grid points are assigned attributes from this
     * iterable starting at the upper left hand corner, and proceeding in row-first order across the grid. The iterable
     * should contain at least {@code width * height} values, where width and height are the AnalyticSurface's grid
     * dimensions. If the iterable contains too few values, the unassigned grid points are given default attributes: the
     * default scalar value is 0, the default color is {@link java.awt.Color#BLACK}, and the default opacity is 1
     * (totally opaque).
     *
     * @param iterable the new grid point attributes.
     * @throws IllegalArgumentException if the iterable is {@code null}.
     */
    void setSurfaceValues(Iterable<? extends GridPointAttributes> iterable);

    /**
     * Returns the scale applied to the value at each grid point.
     *
     * @return the surface's vertical scale coefficient.
     */
    double getVerticalScale();

    /**
     * Sets the scale applied to the value at each grid point. Before rendering, this value is applied to each grid
     * points scalar value, thus increasing or decreasing it's height relative to the surface's base altitude, both in
     * meters.
     *
     * @param scale the surface's vertical scale coefficient.
     */
    void setVerticalScale(double scale);

    /**
     * Returns a copy of the rendering attributes associated with this surface. Modifying the contents of the returned
     * reference has no effect on this surface. In order to make an attribute change take effect, invoke {@link
     * #setAnalyticSurfaceAttributes(ISurfaceAttributes)} with the modified attributes.
     *
     * @return a copy of this surface's rendering attributes.
     */
    ISurfaceAttributes getAnalyticSurfaceAttributes();

    /**
     * Sets the rendering attributes associated with this surface. The caller cannot assume that modifying the attribute
     * reference after calling setSurfaceAttributes() will have any effect, as the implementation may defensively copy
     * the attribute reference. In order to make an attribute change take effect, invoke
     * setSurfaceAttributes(AnalyticSurfaceAttributes) again with the modified attributes.
     *
     * @param attributes this surface's new rendering attributes.
     * @throws IllegalArgumentException if attributes is {@code null}.
     */
    void setAnalyticSurfaceAttributes(ISurfaceAttributes attributes);

    /**
     * Returns a new instance of {@link GridPointAttributes} with a Color computed from the specified value and value
     * range. The color's RGB components are computed by mapping value's relative position in the range {@code [minValue,
     * maxValue]} to the range of color hues {@code [minHue, maxHue]}. The color's Alpha component is
     * computed by mapping the values' relative position in the range {@code [minValue, minValue + (maxValue - minValue)
     * / 10]} to the range {@code [0, 1]}. This has the effect of interpolating hue and alpha based on the
     * grid point value.
     *
     * @param value    the new GridPointAttributes' value.
     * @param minValue the minimum value.
     * @param maxValue the maximum value.
     * @param minHue   the minimum color hue, corresponding to the minimum value.
     * @param maxHue   the maximum color hue, corresponding to the maximum value.
     * @return a new GridPointAttributes defined by the specified value, value range, and color hue range.
     */
    IAnalyticSurface.GridPointAttributes createColorGradientGridPointAttributes(double value, double minValue,
                                                                                double maxValue, double minHue,
                                                                                double maxHue);

    /**
     * Returns a new instance of {@link GridPointAttributes} with the specified value and color.
     *
     * @param value the new GridPointAttributes' value.
     * @param color the new GridPointAttributes' color.
     * @return a new GridPointAttributes defined by the specified value and color.
     */
    IAnalyticSurface.GridPointAttributes createSurfaceGridPointAttributes(double value, Color color);

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
    IAnalyticSurfaceLegend createLegendFromColorGradient(double minValue, double maxValue, double minHue,
                                                         double maxHue,
                                                         Iterable<? extends IAnalyticSurfaceLegend.LabelAttributes> labels,
                                                         IAnalyticSurfaceLegend.LabelAttributes titleLabel);

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
    IAnalyticSurfaceLegend createLegendFromColorGradient(int width, int height, double minValue, double maxValue,
                                                         double minHue, double maxHue, Color borderColor,
                                                         Iterable<? extends IAnalyticSurfaceLegend.LabelAttributes> labels,
                                                         IAnalyticSurfaceLegend.LabelAttributes titleLabel);

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
    Iterable<? extends IAnalyticSurfaceLegend.LabelAttributes> createDefaultColorGradientLabels(double minValue, double maxValue,
                                                                                                Format format);

    /**
     * Construct a label attribute using the given text with default font, color and offset.  The label will use an
     * Arial-bold-16 font and a white color.
     *
     * @param text The text to use in the label
     * @return {@link IAnalyticSurfaceLegend.LabelAttributes} instance
     */
    IAnalyticSurfaceLegend.LabelAttributes createDefaultTitle(String text);

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
    IAnalyticSurfaceLegend.LabelAttributes createLegendLabelAttributes(double value, String text, Font font,
                                                                       Color color, double xOffset, double yOffset);
}
