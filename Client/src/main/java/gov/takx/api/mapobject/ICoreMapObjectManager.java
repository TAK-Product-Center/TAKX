package gov.takx.api.mapobject;

import java.util.List;

/**
 * Core use only methods for IMapObjectManager, not necessary to expose to the (unpulished) API.
 *
 * @since 1.49
 */
public interface ICoreMapObjectManager extends IMapObjectManager
{
    /**
     * The MOM will keep track of track select listeners that want to be notified if a user selected a set of
     * track points from the globe or from the device log.
     *
     * @return the list of track select listeners
     */
    List<ITrackSelectListener> getTrackSelectListeners();

    /**
     * The MOM will keep track of track context menu listeners that want to be notified if a user selected a set of
     * track points from the globe or from the device log using a right click.
     *
     * @return the list of track context menu listeners
     * @since 2.26
     */
    List<ITrackContextMenuListener> getTrackContextMenuListeners();

    /**
     * Add a listener that will be invoked when the user selects a track point.
     *
     * @param listener Listener to add, may not be {@code null}
     * @see #removeTrackSelectListener(ITrackSelectListener)
     */
    void addTrackSelectListener(ITrackSelectListener listener);

    /**
     * Remove a select listener.
     *
     * @param listener Listener to remove
     * @see #addTrackSelectListener(ITrackSelectListener)
     */
    void removeTrackSelectListener(ITrackSelectListener listener);

    /**
     * Add a listener that will be invoked when the user right-clicks a track point.
     *
     * @param listener Listener to add, may not be {@code null}
     * @see #removeTrackContextMenuListener(ITrackContextMenuListener)
     * @since 2.26
     */
    void addTrackContextMenuListener(ITrackContextMenuListener listener);

    /**
     * Remove a track context menu listener.
     *
     * @param listener Listener to remove
     * @see #addTrackContextMenuListener(ITrackContextMenuListener)
     * @since 2.26
     */
    void removeTrackContextMenuListener(ITrackContextMenuListener listener);

    /**
     * Remove the given Map Object from its containing Map Object, if any.
     *
     * @param mapObject Map Object to remove from its container
     * @since 2.37
     */
    void removeFromContainer(IIconMapObject mapObject);
}
