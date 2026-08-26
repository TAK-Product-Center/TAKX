package gov.takx.api.shapes;

import gov.takx.api.annotation.Internal;
import gov.takx.api.annotation.NoPluginImplementations;
import gov.takx.api.coordinatesystem.ILocation;
import gov.takx.api.mapobject.IShapeMapObject;
import gov.takx.dataservices.shapes.AShape.TextureFillParams;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import java.awt.Color;

/**
 * Interface to a mutable shape. See {@link IShape} for more information.
 */
@NoPluginImplementations
public interface IMutableShape extends IShape
{
    /**
     * Set the Shape Map Object associated with this shape.
     * @since 3.3
     */
    void setShapeMapObject(IShapeMapObject<?> shapeMapObject);

    /**
     * Set the unique key for this shape.
     * @since 3.3
     */
    void setKey(String key);

    /**
     * Set the location of the shape.  This will move the shape on the globe surface to the specified location.
     * For regular shapes, the center point is moved.  For polygons, the first vertex is moved to the specified
     * location and the rest of the vertices are translated accordingly.
     *
     * @param location the new location of the object
     */
    void setLocation(ILocation location);

    /**
     * Set the border color of the shape.
     *
     * @param color border color
     */
    void setLineColor(Color color);

    /**
     * Set the width of the border of the shape.
     *
     * @param width Width of the border
     */
    void setLineWidth(double width);

    /**
     * Set the line stipple factor. The factor value specifies a multiplier for each bit in the line stipple pattern.
     * If factor is 3, for example, each bit in the pattern is used three times before the next bit in the pattern is used.
     *
     * @param stippleFactor the line stipple factor
     * @since 5.5
     */
    void setLineStippleFactor(int stippleFactor);

    /**
     * Set the line stipple pattern. The pattern value specifies a 16-bit integer whose bit pattern determines which fragments
     * of a line will be drawn when the line is rasterized. Bit zero is used first.
     *
     * @param stipplePattern the line stipple pattern
     * @since 5.5
     */
    void setLineStipplePattern(short stipplePattern);

    /**
     * Set the fill color of the shape.
     *
     * @param color fill color
     */
    void setFillColor(Color color);

    /**
     * Set the texture fill parameters to fill the shape with an optionally repeating image.
     *
     * @param params the parameters for the texture fill, or {@code null} to clear the texture fill
     * @since 5.5.1
     */
    @Nullable
    void setTextureFillParams(TextureFillParams params);

    /**
     * Set the opacity of the colors used to draw the shape.  The value must be between 0 and 100, and it
     * represents the level of opaqueness of the color, where 0 = totally transparent and 100 = totally
     * opaque.  If this value is not set specifically after creating a shape, the default value is 70.
     *
     * @param opacity Opacity value
     * @throws IllegalArgumentException if the value is less than 0 or greater than 100.
     */
    void setOpacity(int opacity) throws IllegalArgumentException;

    /**
     * Whether the shape is orientable, or rotatable by the user. By default, shapes are orientable, however,
     * changing the orientation also requires Editable to be true.
     *
     * @param orientable A boolean true to allow orientation change by the user.
     * @since 1.50
     */
    void setOrientable(boolean orientable);

    /**
     * Set if the shape is pickable/editable by the user.  By default, any shape created by the shape service
     * is <em>not</em> editable by the user.  Generally, the user shouldn't be modifying a shape under the control
     * of a plugin.  If you want the shape to be modifiable by the user, then call the method with a value of
     * {@code true}.
     *
     * @param editable editable setting
     * @since 1.2.4
     */
    void setEditable(boolean editable);

    /**
     * Set whether this shape can be dragged.
     *
     * @param allowDrag true to allow dragging
     * @since 3.3
     */
    void setAllowDrag(boolean allowDrag);

    /**
     * Sets the display name of the IShapeMapObject in the map object tree.
     *
     * @param name to display in the map object tree
     * @since 1.4
     */
    void setName(@Nonnull String name);

    /**
     * Sets the description (tooltip) of the IShape in the map object tree.
     *
     * @param description description (tooltip) in the map object tree
     * @since 1.11
     */
    void setDescription(@Nonnull String description);

    /**
     * Sets the shape to be published or not. Setting a shape to published lets other plugins obtain an immutable reference
     * to this shape. This allows them to access information about it, but not edit it programmatically. Setting a shape
     * as published will also change the look of the shape slightly. Published shapes will have a similar color scheme
     * as when it was unpublished, however, the border will not be dashed instead of solid, and the fill will have
     * a hatching pattern to it. When a shape is set as not published, this will revert the look back to normal.
     *
     * @param published whether the shape is published.
     * @since 1.4
     */
    void setPublished(boolean published);

    /**
     * Sets the shape to be visible on the globe or not.
     *
     * @param visible whether the shape should be visible.
     * @since 1.4
     */
    void setVisible(boolean visible);

    /**
     * Set the vertical height (in meters) on this shape.
     *
     * @param verticalHeight the vertical height (in meters).
     * @since 1.29
     */
    void setVerticalHeight(double verticalHeight);

    /**
     * Enable or disable the vertical height on this shape.
     *
     * @param enable {@code true} to enable height on this shape, or {@code false} to disable vertical height.
     * @since 1.29
     */
    void setVerticalHeightEnabled(boolean enable);

    /**
     * Set the altitude mode for the shape vertices.
     *
     * @param altitudeMode the altitude mode for this shape
     * @since 5.6
     */
    void setAltitudeMode(@Nonnull AltitudeMode altitudeMode);

    /**
     * Sets the remarks on the Shape
     * @since 3.5
     */
    void setRemarks(@Nonnull String remarks);

    /**
     * Called immediately after the shape has been deserialized.  Primarily used to handle cross-version issues
     * when the serialized form of a shape changes.
     *
     * @apiNote FOR INTERNAL USE ONLY.
     * @since 5.7.1
     */
    @Internal
    default void onDeserialized()
    {
    }
}
