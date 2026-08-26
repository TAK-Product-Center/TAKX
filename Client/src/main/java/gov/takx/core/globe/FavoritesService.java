package gov.takx.core.globe;

import gov.takx.api.annotation.PseudoBean;
import gov.takx.api.globe.ICameraEye;
import gov.takx.api.globe.IExtendedGlobeManager;
import gov.takx.api.globe.IFavorite;
import gov.takx.api.globe.IFavoritesService;
import gov.takx.api.globe.IProjectFavoritesManager;
import gov.takx.platform.globe.Favorite;
import jakarta.inject.Inject;

import java.util.List;
import java.util.stream.Collectors;

/**
 * @since 1.3
 */
@SuppressWarnings("CdiManagedBeanInconsistencyInspection")
@PseudoBean
public class FavoritesService implements IFavoritesService
{
    private String owner;

    @Inject
    private IProjectFavoritesManager bookmarkManager;

    @Inject
    private IExtendedGlobeManager globeManager;

    public FavoritesService(String uniqueKey)
    {
        owner = uniqueKey;
    }

    @Override
    public IFavorite add()
    {
        final IFavorite bookmark = globeManager.getLastFocusedGlobe().getViewBookmark();
        bookmarkManager.add(bookmark);
        return bookmark;
    }

    @Override
    public IFavorite add(String description)
    {
        final IFavorite bookmark = globeManager.getLastFocusedGlobe().getViewBookmark();
        bookmark.setDescription(description);
        bookmarkManager.add(bookmark);
        return bookmark;
    }

    @Override
    public IFavorite add(String description, double latitude, double longitude, double elevation, double heading,
                         double pitch, double zoom)
    {
        final IFavorite bookmark = Favorite.createViewBookmark(owner, description, latitude, longitude, elevation, heading, pitch, zoom);
        bookmarkManager.add(bookmark);
        return bookmark;
    }

    @Override
    public IFavorite add(String description, ICameraEye cameraEye)
    {
        final IFavorite bookmark = Favorite.createViewBookmark(owner, description, cameraEye);
        bookmarkManager.add(bookmark);
        return bookmark;
    }

    @Override
    public void remove(IFavorite bookmark)
    {
        bookmarkManager.remove(bookmark);
    }

    @Override
    public List<IFavorite> getFavorites()
    {
        return bookmarkManager.getBookmarks().stream()
                .filter(bookmark -> bookmark.getOwner().equals(owner))
                .collect(Collectors.toList());
    }
}
