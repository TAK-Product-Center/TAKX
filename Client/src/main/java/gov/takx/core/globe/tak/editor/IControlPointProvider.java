package gov.takx.core.globe.tak.editor;

import gov.takx.api.coordinatesystem.ILocation;
import gov.takx.core.globe.tak.editor.IShapeEditorSource.ControlPointDefinition;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import java.util.List;
import java.util.Map;

/**
 * An interface to provide a set of control point definitions and behaviors for a source.
 *
 * @since 5.8.1 (backported from 5.9)
 */
public interface IControlPointProvider
{
    /**
     * Set the shape editor for this controller.
     * @param shapeEditor the shape editor
     */
    void setShapeEditor(@Nonnull IShapeEditor shapeEditor);

    /**
     * Create a list of {@link ControlPointDefinition} that represent editable control points for a shape.
     * @return a list of control point definitions
     */
    @Nonnull
    List<ControlPointDefinition> createControlPointDefinitions();

    /**
     * Indicates the {@link IControlPointController} is requesting a control point should be added at the provided location.
     * @param location the location for the control point
     * @param metadata metadata about the control point
     * @param onSourceUpdated a runnable that will let the caller know that the source object has been updated
     * @see IControlPointController.ControlPointEventListener#onControlPointAdded(ILocation, Map)
     */
    void onControlPointAdded(@Nonnull ILocation location, @Nonnull Map<String, Object> metadata, @Nonnull Runnable onSourceUpdated);

    /**
     * Indicates the {@link IControlPointController} is requesting a control point should be removed.
     * @param controlPoint the control point to be removed
     * @param onSourceUpdated a runnable that will let the caller know that the source object has been updated
     * @see IControlPointController.ControlPointEventListener#onControlPointRemoved(IControlPoint)
     */
    void onControlPointRemoved(@Nonnull IControlPoint controlPoint, @Nonnull Runnable onSourceUpdated);

    /**
     * Indicates the {@link IControlPointController} is requesting the control point with the provided control point type should be moved.
     * @param controlPointType the control point type to be moved
     * @param location the location to move the control point
     * @param onSourceUpdated a runnable that will let the caller know that the source object has been updated
     * @see IControlPointController.ControlPointEventListener#onControlPointDragged(String, ILocation)
     */
    void onControlPointDragged(@Nonnull String controlPointType, @Nonnull ILocation location, @Nonnull Runnable onSourceUpdated);

    /**
     * Get the text to be displayed for the provided control point.
     * @param controlPoint the control point
     * @return the text for the control point, or {@code null} if no text should be displayed
     * @see IControlPointTextProvider#getControlPointText(IControlPoint)
     */
    @Nullable
    String getControlPointText(@Nonnull IControlPoint controlPoint);
}