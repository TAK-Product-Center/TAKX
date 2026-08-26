package gov.takx.api.shapes;

import gov.takx.api.annotation.NoPluginImplementations;
import gov.takx.api.coordinatesystem.ILocation;
import gov.takx.api.mapobject.IShapeMapObject;
import gov.takx.commons.lang.IPropertyChangeListenerSupport;
import gov.takx.dataservices.shapes.AShape.TextureFillParams;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import javax.naming.OperationNotSupportedException;

import java.awt.Color;

/**
 * Interface to a base immutable shape object. This interface provides the common operations supported by all shapes.
 *
 */
@NoPluginImplementations
public interface IShape extends IPropertyChangeListenerSupport
{
    /**
     * Specifies how to interpret a shape's altitude
     * @since 5.6
     */
    enum AltitudeMode
    {
        /** All vertices are clamped to the ground */
        ClampToGround,
        /** Vertex altitude is interpreted as an absolute value HAE */
        Absolute,
        /** Vertex altitude is interpreted as relative to ground */
        Relative
    }

    /**
     * @param <T> Inferred type
     * @return The {@link IShapeMapObject Shape MapObject} representing this shape
     */
    <T extends IShapeMapObject<?>> T getShapeMapObject();

    /**
     * Get the location of the shape.  For shapes with a center, this is the center point.  For polygons, it's the
     * location of the first vertex.
     *
     * @return location of the shape
     */
    @Nonnull
    ILocation getLocation();

    /**
     * Get the border color of the shape.
     *
     * @return border color
     */
    Color getLineColor();

    /**
     * Get the width of the border for the shape.
     *
     * @return width of the border
     */
    double getLineWidth();

    /**
     * Get the line stipple factor.
     *
     * @return the line stipple factor
     * @since 5.5
     */
    int getLineStippleFactor();

    /**
     * Get the line stipple pattern.
     *
     * @return the line stipple pattern
     * @since 5.5
     */
    short getLineStipplePattern();

    /**
     * Get the fill color of the shape.
     *
     * @return fill color
     */
    Color getFillColor();

    /**
     * Get the texture fill parameters to fill the shape with an optionally repeating image.
     *
     * @return the parameters for the texture fill, or {@code null} for no texture fill
     * @since 5.5.1
     */
    @Nullable
    TextureFillParams getTextureFillParams();

    /**
     * Get the opacity of the drawing colors for the shape.
     *
     * @return opacity value between 0 and 100
     */
    int getOpacity();

    /**
     * @return If the shape is orientable by the user
     */
    boolean isOrientable();

    /**
     * @return if the shape is editable by the user
     */
    boolean isEditable();

    /**
     * @return {@code true} if this shape allows drag operations
     * @since 3.3
     */
    boolean isAllowDrag();

    /**
     * Get the display name of the IShape as shown in the map object tree.
     *
     * @return The display name of the shape in the map object tree
     */
    String getName();

    /**
     * Get the description (tooltip) of the IShape as shown in the map object tree.
     *
     * @return The description (tooltip) of the shape
     * @since 1.11
     */
    String getDescription();

    /**
     * Checks to see if the shape is published. If a shape is published, it is a "geofence"
     *
     * @return true if the shape is published, false otherwise.
     * @since 1.4
     */
    boolean isPublished();

    /**
     * Get the unique key of the shape
     *
     * @return the unique key associated with the Shape.
     * @since 1.4
     */
    String getKey();

    /**
     * Get the type of the shape
     *
     * @return the type of the Shape.
     * @since 1.4
     */
    String getType();

    /**
     * Checks to see if the shape is visible or not
     *
     * @return whether the shape is visible
     * @since 1.4
     */
    boolean isVisible();

    /**
     * @return The number of plugins monitoring this shape
     * @since 3.3
     */
    int getShapeListenerCount();

    /**
     * Add a ShapeListener to this shape.
     * @since 1.4
     */
    void addShapeListener(@Nonnull IShapeListener shapeListener);

    /**
     * Remove a ShapeListener from this shape.
     * @since 1.4
     */
    void removeShapeListener(@Nonnull IShapeListener shapeListener);

    /**
     * Used to determine if this shape contains the given location.
     *
     * @param location The location to test.
     * @return True if the location is within the IShape
     * @throws UnsupportedOperationException if the shape does not support this method.
     */
    boolean contains(ILocation location) throws OperationNotSupportedException;

    /**
     * Used to determine if this shape completely contains another shape.
     *
     * @param shape The shape to test.
     * @return {@code true} iff the other shape is within this shape.
     * @throws UnsupportedOperationException Thrown if the shape does not support this method.
     */
    boolean contains(IShape shape) throws OperationNotSupportedException;

    /**
     * Used to determine if this shape intersects another shape.
     *
     * @param shape The shape to test.
     * @return {@code true} iff the other shape intersects this shape.
     * @throws UnsupportedOperationException Thrown if the shape does not support this method.
     */
    boolean intersects(IShape shape) throws OperationNotSupportedException;

    /**
     * Determine whether this shape can use vertical height.  This is typically a hard-coded value based upon the
     * extending shape map object type.
     *
     * @return {@code true} if height can be used on this shape, or {@code false} otherwise.
     * @since 1.29
     */
    boolean canUseVerticalHeight();

    /**
     * Get the vertical height (in meters) from this shape.
     *
     * @return the vertical height (in meters).
     * @since 1.29
     */
    double getVerticalHeight();

    /**
     * Determine whether vertical height is enabled on this shape.
     *
     * @return {@code true} if height is enabled on this shape, or {@code false} otherwise.
     * @since 1.29
     */
    boolean isVerticalHeightEnabled();

    /**
     * @return {@code true} if the shape should be extruded, equivalent to:
     * <pre>
     * {@code canUseVerticalHeight() && isVerticalHeightEnabled() && getVerticalHeight() != 0}
     * </pre>
     * @since 5.0
     */
    default boolean isExtruded()
    {
        return canUseVerticalHeight() && isVerticalHeightEnabled() && getVerticalHeight() != 0;
    }

    /**
     * Get the altitude mode for the shape vertices.
     *
     * @return the altitude mode for this shape
     * @since 5.6
     */
    AltitudeMode getAltitudeMode();

    /**
     * Gets the Shape's remarks
     * @since 3.5
     */
    String getRemarks();
}
