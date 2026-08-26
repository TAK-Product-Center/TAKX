package gov.takx.api.globe;

import gov.takx.api.annotation.Internal;
import gov.takx.api.annotation.NoPluginImplementations;

/**
 * Listener interface to monitor a ViewBookmarkManager.
 *
 */
@Internal
@NoPluginImplementations
public interface IFavoritesListener
{
    /**
     * Fired when a view bookmark is added.
     *
     * @param bookmark The bookmark that was added
     */
    void onFavoriteAdded(IFavorite bookmark);

    /**
     * Fired when a view bookmark is removed.
     *
     * @param bookmark The bookmark that was removed.
     */
    void onFavoriteRemoved(IFavorite bookmark);

    /**
     * Fired when a bookmark is changed.
     *
     * @param bookmark The bookmark that was changed.
     * @param index    The index of the bookmark in the bookmark list.
     */
    void onFavoriteChanged(IFavorite bookmark, int index);
}
