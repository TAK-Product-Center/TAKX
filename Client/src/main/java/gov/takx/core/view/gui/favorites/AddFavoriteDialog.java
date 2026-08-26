package gov.takx.core.view.gui.favorites;

import com.atakmap.map.elevation.ElevationManager;
import gov.nasa.worldwind.WorldWindow;
import gov.nasa.worldwind.avlist.AVKey;
import gov.nasa.worldwind.geom.Position;
import gov.nasa.worldwind.view.BasicView;
import gov.nasa.worldwind.view.orbit.OrbitView;
import gov.takx.api.annotation.PseudoBean;
import jakarta.annotation.PostConstruct;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;

/**
 */
@PseudoBean
public class AddFavoriteDialog extends FavoriteDialog implements PropertyChangeListener
{
    private WorldWindow wwd;
    private BasicView view;

    public AddFavoriteDialog()
    {
        super(true, "favorite_add.png",
                "<html>Add the current view as a Favorite. Project favorites will be stored with the project. " +
                        "User favorites will be available in all projects opened on this computer and will not be " +
                        "stored with the project.</html>");
    }

    @PostConstruct
    private void postConstruct()
    {
        wwd = globeManager.getLastFocusedGlobe().getWorldWindow();
        view = (BasicView) wwd.getView();
        wwd.getSceneController().addPropertyChangeListener(AVKey.VIEW, this);

        setCurrentLocation();
    }

    @Override
    protected boolean saveFavorite()
    {
        // Create a new favorite based on the current location.
        final var favorite = globeManager.getLastFocusedGlobe().getViewBookmark();
        final String favoriteName = getFavoriteName();
        final var manager = userSelected() ? userViewBookmarkManager : projectViewBookmarkManager;

        if (checkAndReportDuplicateDescription(favoriteName, manager))
        {
            favorite.setDescription(favoriteName);
            manager.add(favorite);
            return true;
        } else
        {
            return false;
        }
    }

    @Override
    public void okButtonActionPerformed()
    {
        wwd.getSceneController().removePropertyChangeListener(AVKey.VIEW, this);
        super.okButtonActionPerformed();
    }

    @Override
    public void propertyChange(PropertyChangeEvent evt)
    {
        setCurrentLocation();
    }

    private void setCurrentLocation()
    {
        // Get zoom if we have an OrbitView, otherwise use eye position elevation
        double zoomOrElevation = view instanceof OrbitView orbitView
                ? orbitView.getZoom()
                : view.getEyePosition().elevation;

        Position currentPosition = view.getCenterPosition();
        double latitude = currentPosition.getLatitude().getDegrees();
        double longitude = currentPosition.getLongitude().getDegrees();

        updateCurrentView(
                latitude,
                longitude,
                ElevationManager.getElevation(latitude, longitude, null),
                view.getHeading().getDegrees(),
                view.getPitch().getDegrees(),
                zoomOrElevation);
    }
}
