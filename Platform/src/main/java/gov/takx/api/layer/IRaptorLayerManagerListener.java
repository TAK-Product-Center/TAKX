package gov.takx.api.layer;

import gov.tak.api.annotation.DeprecatedApi;

/**
 * Listener for changes made to the layer manager, including addition and removal of layers.
 *
 * @since 1.41 (default implementations added in 3.4)
 */
public interface IRaptorLayerManagerListener
{
    /**
     * Notification that a layer has been added.
     *
     * @param raptorLayer the added layer
     */
    default void raptorLayerAdded(IRaptorLayer raptorLayer)
    {
    }

    /**
     * Notification that an elevation layer has been added.
     *
     * @param raptorLayer the added elevation layer
     * @deprecated use {@link #raptorLayerAdded(IRaptorLayer)} and check {@link IRaptorLayer#getLayerGroup()} for {@link LayerGroup#ELEVATION}.
     */
    @DeprecatedApi(since = "5.6", forRemoval = true, removeAt = "5.9")
    @Deprecated(since = "5.6", forRemoval = true)
    default void raptorModelAdded(IRaptorLayer raptorLayer)
    {
    }

    /**
     * Notification that a layer has been removed.
     *
     * @param raptorLayer the removed layer
     */
    default void raptorLayerRemoved(IRaptorLayer raptorLayer)
    {
    }

    /**
     * Notification that an elevation layer has been removed.
     *
     * @param raptorLayer the removed elevation layer
     * @deprecated use {@link #raptorLayerRemoved(IRaptorLayer)} and check {@link IRaptorLayer#getLayerGroup()} for {@link LayerGroup#ELEVATION}.
     */
    @DeprecatedApi(since = "5.6", forRemoval = true, removeAt = "5.9")
    @Deprecated(since = "5.6", forRemoval = true)
    default void raptorModelRemoved(IRaptorLayer raptorLayer)
    {
    }

    /**
     * Notification that all layers have been removed.
     */
    default void raptorLayersRemoved()
    {
    }

    /**
     * Notification that a layer has been moved within its group.
     *
     * @param raptorLayer the moved layer
     */
    default void raptorLayerMoved(IRaptorLayer raptorLayer)
    {
    }

    /**
     * Indicate that a layer has been selected.
     *
     * @param raptorLayer layer affected, {@code null} if the selection has been cleared
     */
    default void raptorLayerSelected(IRaptorLayer raptorLayer)
    {
    }
}
