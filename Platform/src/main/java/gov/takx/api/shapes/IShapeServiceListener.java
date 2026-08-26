package gov.takx.api.shapes;

/**
 * This interface defines a listener for being notified when an IShape goes from being published to not being published,
 * or visa versa. It also notifies when published shapes are first defined and when they are deleted. Notifications are
 * also sent when shapes that the plugin has created/own are deleted but are not published.
 *
 * @since 1.4
 */
public interface IShapeServiceListener
{
    /**
     * Called from the IShapeService instance who had the IShapeServiceListener reference registered with it. Notification
     * for when an IShape has been published.
     *
     * @param shape the immutable IShape reference to the shape that is now published.
     */
    default void onShapePublished(IShape shape)
    {
    }

    /**
     * Called from the IShapeService instance who had the IShapeServiceListener reference registered with it. Notification
     * for when an IShape is no longer published.
     *
     * @param shape the immutable IShape reference to the shape that is no longer published.
     */
    default void onShapeUnpublished(IShape shape)
    {
    }

    /**
     * Called from the IShapeService instance who had the IShapeServiceListener reference registered with it. Notification
     * for when a new IShape has been defined in the project session. This notification occurs only when a shape is
     * created with it's published flag set to true.
     *
     * @param shape the immutable IShape reference of the newly defined shape.
     */
    default void onShapeDefined(IShape shape)
    {
    }

    /**
     * Called from the IShapeService instance who had the IShapeServiceListener reference registered with it. Notification
     * for when an existing shape has been deleted. Reference is no longer valid once the method returns. This notification
     * occurs both when a shape with it's published flag set to true (and that we may or may not own) is deleted and
     * when a shape that we created/own with is published flag set to false is deleted.
     *
     * @param shape the immutable IShape reference of the shape about to be deleted.
     */
    default void onShapeDeletion(IShape shape)
    {
    }
}
