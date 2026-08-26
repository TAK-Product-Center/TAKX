package gov.takx.core.view.gui.favorites;

import gov.takx.api.annotation.PseudoBean;
import gov.takx.api.globe.IFavorite;
import jakarta.annotation.PostConstruct;

/**
 */
@PseudoBean
public class EditFavoriteDialog extends FavoriteDialog
{
    private final IFavorite favorite;
    private final boolean originallyUserFavorite;

    public EditFavoriteDialog(IFavorite favorite, boolean userFavorite)
    {
        super(favorite, userFavorite, "favorite_settings.png",
                "<html>Edit the name of the selected favorite or change between a project or user favorite</html>");

        setTitle("Edit a Favorite");

        this.favorite = favorite;
        originallyUserFavorite = userFavorite;
    }

    @PostConstruct
    private void postConstruct()
    {
        updateCurrentView(
                favorite.getLatitude(),
                favorite.getLongitude(),
                favorite.getElevation(),
                favorite.getHeading(),
                favorite.getPitch(),
                favorite.getZoom());
    }

    @Override
    protected boolean saveFavorite()
    {
        // Check to see if the favorite has changed between user and project.
        final boolean moving = originallyUserFavorite != userSelected();

        final var originalManager = originallyUserFavorite ? userViewBookmarkManager : projectViewBookmarkManager;
        final var newManager = moving
                ? (originallyUserFavorite ? projectViewBookmarkManager : userViewBookmarkManager)
                : originalManager;

        final String favoriteName = getFavoriteName();

        if (checkAndReportDuplicateDescription(favoriteName, newManager))
        {
            favorite.setDescription(favoriteName); // Always update the name.

            if (moving)
            {
                originalManager.remove(favorite);
                newManager.add(favorite);
            }
            return true;
        } else
        {
            return false;
        }
    }
}
