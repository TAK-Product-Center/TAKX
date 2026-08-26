package gov.takx.api.event;

import gov.takx.api.shapes.IMutableShape;

/**
 * Event to be fired when a shape is created interactively by the user.  This is used by the
 * {@link gov.takx.core.shapes.ShapeDrawingService} to detect when the user is done drawing.
 *
 * @param shape The shape that was drawn
 *
 * @since 3.5
 */
public record UserShapeCreatedEvent(IMutableShape shape)
{
}
