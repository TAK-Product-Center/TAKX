package gov.takx.core.globe.tak.editor;

import gov.takx.api.coordinatesystem.ILocation;
import jakarta.annotation.Nonnull;

import java.beans.PropertyChangeListener;
import java.util.List;
import java.util.Map;

/**
 * An interface to manage a set of control points for shape editing.
 *
 * @since 5.4
 */
public interface IControlPointController
{
    /**
     * Get the shape editor for this controller.
     * @return the shape editor
     * @throws NullPointerException if this is called before {@link #setShapeEditor(IShapeEditor)} is called
     */
    @Nonnull
    IShapeEditor getShapeEditor();

    /**
     * Set the shape editor for this controller.
     * @param shapeEditor the shape editor
     */
    void setShapeEditor(@Nonnull IShapeEditor shapeEditor);

    /**
     * Set whether this controller is enabled. If enabled, the editor will be visible and ready for input.
     * @param enabled {@code true} if enabled, {@code false} if not
     */
    void setEnabled(boolean enabled);

    /**
     * Set whether this controller is armed. If armed, the editor will be armed to create a new object.
     * @param armed {@code true} if armed, {@code false} if not
     */
    void setArmed(boolean armed);

    /**
     * Get a list of control points for the editor.
     * @return a list of control points
     */
    @Nonnull
    List<IControlPoint> getControlPoints();

    void addControlPointEventListener(@Nonnull ControlPointEventListener listener);

    void removeControlPointEventListener(@Nonnull ControlPointEventListener listener);

    void addPropertyChangeListener(@Nonnull PropertyChangeListener listener);

    void removePropertyChangeListener(@Nonnull PropertyChangeListener listener);

    /**
     * The intent here is to <i>request</i> that listeners react to these events. For instance, source objects
     * should modify themselves in response and report back that they have changed.
     */
    interface ControlPointEventListener
    {
        /**
         * Indicates the {@link IControlPointController} is requesting a control point should be added at the provided location.
         * @param location the location for the control point
         * @param metadata metadata about the control point
         */
        void onControlPointAdded(@Nonnull ILocation location, @Nonnull Map<String, Object> metadata);

        /**
         * Indicates the {@link IControlPointController} is requesting a control point should be removed.
         * @param controlPoint the control point to be removed
         */
        void onControlPointRemoved(@Nonnull IControlPoint controlPoint);

        /**
         * Indicates the {@link IControlPointController} is requesting the control point with the provided control point type should be moved.
         * @param controlPointType the control point type to be moved
         * @param location the location to move the control point
         */
        void onControlPointDragged(@Nonnull String controlPointType, @Nonnull ILocation location);
    }
}
