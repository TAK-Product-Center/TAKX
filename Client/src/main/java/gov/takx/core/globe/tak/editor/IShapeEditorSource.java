package gov.takx.core.globe.tak.editor;

import gov.takx.api.coordinatesystem.ILocation;
import gov.takx.api.shapes.IShape.AltitudeMode;
import jakarta.annotation.Nonnull;

import java.util.List;

/**
 * An interface to manage the source object for a shape editor.
 *
 * @since 5.4
 */
public interface IShapeEditorSource<T>
{
    /**
     * Get the shape editor for this source.
     * @return the shape editor
     * @throws NullPointerException if this is called before {@link #setShapeEditor(IShapeEditor)} is called
     */
    @Nonnull
    IShapeEditor getShapeEditor();

    /**
     * Set the shape editor for this source.
     * @param shapeEditor the shape editor
     */
    void setShapeEditor(@Nonnull IShapeEditor shapeEditor);

    /**
     * Set whether this source is enabled. If enabled, the editor will be visible and ready for input.
     * @param enabled {@code true} if enabled, {@code false} if not
     */
    void setEnabled(boolean enabled);

    /**
     * Set whether this source is armed. If armed, the editor will be armed to create a new object.
     * @param armed {@code true} if armed, {@code false} if not
     */
    void setArmed(boolean armed);

    /**
     * Get the source object.
     * @return the source object
     */
    @Nonnull
    T getSource();

    /**
     * Create a list of {@link ControlPointDefinition} that represent editable control points for a shape.
     * @return a list of control point definitions
     */
    @Nonnull
    List<ControlPointDefinition> createControlPointDefinitions();

    /**
     * @return {@code true} if any control points have been added
     * @since 5.7.1
     */
    boolean hasControlPoints();

    /**
     * Apply the control points to the source object.
     * @param controlPoints the control points
     * @throws IllegalArgumentException if the control points are not sufficient to create the shape
     */
    void applyControlPointsToSource(List<IControlPoint> controlPoints);

    void addShapeEditorSourceListener(ShapeEditorSourceListener listener);

    void removeShapeEditorSourceListener(ShapeEditorSourceListener listener);

    /**
     * Listener for {@link IShapeEditorSource} events.
     */
    interface ShapeEditorSourceListener
    {
        /**
         * Event indicating the source has been updated.
         * @param source the source object
         * @param <T> the source object type
         */
        <T> void onSourceUpdated(T source);
    }

    /**
     * A data class to define the properties of a control point.
     * @param location the control point location
     * @param controlPointType the control point type
     * @param draggable {@code true} if draggable, {@code false} if not
     * @param pointSize the control point graphic point size
     * @param color the control point graphic color
     * @param highlightColor the control point graphic highlight color
     * @param altitudeMode the control point altitude mode
     */
    record ControlPointDefinition(@Nonnull ILocation location, @Nonnull String controlPointType, boolean draggable, int pointSize, int color, int highlightColor, AltitudeMode altitudeMode)
    {
        public ControlPointDefinition(@Nonnull ILocation location, @Nonnull String controlPointType, boolean draggable, AltitudeMode altitudeMode)
        {
            this(location, controlPointType, draggable, IControlPoint.DEFAULT_POINT_SIZE, IControlPoint.DEFAULT_COLOR, IControlPoint.DEFAULT_HIGHLIGHT_COLOR, altitudeMode);
        }
    }
}
