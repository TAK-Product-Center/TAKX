package gov.takx.api.globe;

import gov.nasa.worldwind.geom.Intersection;
import gov.nasa.worldwind.geom.Line;
import gov.nasa.worldwind.terrain.Terrain;
import gov.takx.api.mapobject.MapToolType;
import gov.takx.api.shapes.IMutableShape;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import java.beans.PropertyChangeListener;
import java.util.List;

/**
 */
public interface IGlobeShapeMapObject extends IGlobeMapObject
{
    void setShapeModel(@Nonnull IMutableShape shapeModel);

    /**
     * @param <T> Inferred return type
     * @return the current shape model, may be {@code null}
     * @since 3.3
     */
    @Nullable
    <T extends IMutableShape> T getShapeModel();

    /**
     * @param <T> Inferred return type
     * @return Current shape model
     * @throws NullPointerException If the current model is {@code null}
     * @since 3.3
     */
    @Nonnull
    <T extends IMutableShape> T requireShapeModel();

    /**
     * @return if the shape is editable (pickable)
     */
    boolean isEditable();

    /**
     * Set if the shape can be edited.
     *
     * @param editable pass {@code true} to make the shape editable
     */
    void setEditable(boolean editable);

    /**
     * Get if the shape is being actively edited.
     *
     * @return {@code true} if shape is being edited, {@code false} otherwise
     * @since 5.0
     */
    boolean isEditing();

    /**
     * Set if the shape is being actively edited.
     *
     * @param editing {@code true} if shape is being edited, {@code false} otherwise
     * @since 5.0
     */
    void setEditing(boolean editing);

    @Nonnull
    MapToolType getMapToolType();

    void addPropertyChangeListener(@Nonnull PropertyChangeListener listener);

    /**
     * Add a property change listener on the given list of properties of this object.  To unregister, use
     * {@link #removePropertyChangeListener(PropertyChangeListener, List)}.
     *
     * @param listener   Listener to register
     * @param properties List of properties of interest
     * @since 5.1
     */
    void addPropertyChangeListener(PropertyChangeListener listener, List<String> properties);

    void removePropertyChangeListener(@Nonnull PropertyChangeListener listener);

    /**
     * Remove the listener from the specified properties.
     *
     * @param listener   Listener to remove
     * @param properties list of properties to unregister
     * @see java.beans.PropertyChangeSupport#removePropertyChangeListener(String, java.beans.PropertyChangeListener)
     * @since 5.1
     */
    void removePropertyChangeListener(PropertyChangeListener listener, List<String> properties);

    /**
     * Returns a boolean specifying whether this shape can be used in 3D line intersection calculations.  In general,
     * 3D shapes are intersectable, while 2D shapes are not.
     *
     * @return {@code true} if this shape can be used in line intersection calculations, or {@code false} otherwise.
     * @since 5.5
     */
    default boolean isIntersectable()
    {
        // TODO: TAKX-2148
        return false;
    }

    /**
     * Calculate the intersections between a line and this shape.
     *
     * @param line    the line to intersect with this shape.
     * @param terrain the terrain to use when computing the extruded polygon's geometry.
     * @return a list of intersections between the line and this shape.  If this shape is not intersectable,
     * {@code null} will be returned.
     * @throws InterruptedException if the intersection calculations were interrupted.
     * @since 5.5
     */
    @Nullable
    default List<Intersection> intersect(@Nonnull Line line, @Nonnull Terrain terrain) throws InterruptedException
    {
        // TODO: TAKX-2148
        return null;
    }
}
