package gov.takx.api.globe;

import gov.takx.api.annotation.Internal;
import gov.takx.api.annotation.NoPluginImplementations;

import java.util.List;

/**
 */
@Internal
@NoPluginImplementations
public interface IFavoritesManager
{
    void add(IFavorite bookmark);

    int count();

    IFavorite get(int index);

    /**
     * Sets the index for the given favorite. Reassigns all indices based on new ordering.
     *
     * @param favorite The favorite whose index will be set
     * @param index The new index of the favorite
     * @since 3.4 (added to the interface)
     */
    void setIndex(IFavorite favorite, int index);

    void remove(int index);

    void remove(IFavorite bookmark);

    List<IFavorite> getBookmarks();

    void addListener(IFavoritesListener listener);

    void removeListener(IFavoritesListener listener);
}
