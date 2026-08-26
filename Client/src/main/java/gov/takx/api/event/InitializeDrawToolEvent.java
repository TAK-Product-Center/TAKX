package gov.takx.api.event;

/**
 * This event is fired when a new draw tool is created and needs to be initialized with
 * the properties from the map tools properties panel, which means it only occurs when the user
 * manually creates a new shape or the draw operation is invoked via {@link gov.takx.api.shapes.IShapeDrawingService}.
 *
 * @since 3.3 (renamed in 3.5)
 */
public record InitializeDrawToolEvent(Object mapObject)
{
}
