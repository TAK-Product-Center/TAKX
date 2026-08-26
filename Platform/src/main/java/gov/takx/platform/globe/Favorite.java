package gov.takx.platform.globe;

import gov.takx.api.globe.ICameraEye;
import gov.takx.api.globe.IFavorite;
import jakarta.annotation.Nonnull;

import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;

/**
 */
public class Favorite implements IFavorite
{
    public static final String USER_OWNER = "User";

    private final PropertyChangeSupport changeSupport = new PropertyChangeSupport(this);

    private final String owner;

    private String description;
    private double latitude;
    private double longitude;
    private double elevation;
    private double heading;
    private double pitch;
    private double zoom;

    private Favorite(String owner, String description, double latitude, double longitude, double elevation,
                     double heading, double pitch, double zoom)
    {
        this.owner = owner;
        this.description = description;
        this.latitude = latitude;
        this.longitude = longitude;
        this.elevation = elevation;
        this.heading = heading;
        this.pitch = pitch;
        this.zoom = zoom;
    }

    public Favorite(Favorite bookmark)
    {
        owner = bookmark.owner;
        description = bookmark.description;
        latitude = bookmark.latitude;
        longitude = bookmark.longitude;
        elevation = bookmark.elevation;
        heading = bookmark.heading;
        pitch = bookmark.pitch;
        zoom = bookmark.zoom;
    }

    public static IFavorite createViewBookmark(String owner, String description, double latitude, double longitude, double elevation,
                                               double heading, double pitch, double zoom)
    {
        if (null == owner) owner = USER_OWNER;
        if (null == description) description = "<Empty Description>";

        return new Favorite(owner, description, latitude, longitude, elevation, heading, pitch, zoom);
    }

    public static IFavorite createViewBookmarkAsUser(String description, double latitude, double longitude, double elevation,
                                                     double heading, double pitch, double zoom)
    {
        return createViewBookmark(USER_OWNER, description, latitude, longitude, elevation, heading, pitch, zoom);
    }

    public static IFavorite createViewBookmark(String owner, String description, @Nonnull ICameraEye cameraEye)
    {
        return createViewBookmark(owner, description, cameraEye.getLatitude(), cameraEye.getLongitude(),
                cameraEye.getElevation(), cameraEye.getHeading(), cameraEye.getPitch(), cameraEye.getZoom());
    }

    public static IFavorite createViewBookmark(String description, @Nonnull ICameraEye cameraEye)
    {
        return createViewBookmark(USER_OWNER, description, cameraEye.getLatitude(), cameraEye.getLongitude(),
                cameraEye.getElevation(), cameraEye.getHeading(), cameraEye.getPitch(), cameraEye.getZoom());
    }

    @Override
    public String getOwner()
    {
        return owner;
    }

    @Override
    public String getDescription()
    {
        return description;
    }

    @Override
    public double getLatitude()
    {
        return latitude;
    }

    @Override
    public double getLongitude()
    {
        return longitude;
    }

    @Override
    public double getHeading()
    {
        return heading;
    }

    @Override
    public double getPitch()
    {
        return pitch;
    }

    @Override
    public double getZoom()
    {
        return zoom;
    }

    @Override
    public double getElevation()
    {
        return elevation;
    }

    @Override
    public boolean hasElevation()
    {
        return false;
    }

    @Override
    public void setDescription(String description)
    {
        String old = this.description;
        this.description = description;
        changeSupport.firePropertyChange(DESCRIPTION_PROPERTY, old, description);
    }

    @Override
    public void setLatitude(double lat)
    {
        double old = latitude;
        latitude = lat;
        changeSupport.firePropertyChange(LATITUDE_PROPERTY, old, latitude);
    }

    @Override
    public void setLongitude(double lon)
    {
        double old = longitude;
        longitude = lon;
        changeSupport.firePropertyChange(LONGITUDE_PROPERTY, old, longitude);
    }

    @Override
    public void setHeading(double heading)
    {
        double old = this.heading;
        this.heading = heading;
        changeSupport.firePropertyChange(HEADING_PROPERTY, old, heading);
    }

    @Override
    public void setPitch(double pitch)
    {
        double old = this.pitch;
        this.pitch = pitch;
        changeSupport.firePropertyChange(PITCH_PROPERTY, old, pitch);
    }

    @Override
    public void setZoom(double zoom)
    {
        double old = this.zoom;
        this.zoom = zoom;
        changeSupport.firePropertyChange(ZOOM_PROPERTY, old, zoom);
    }

    @Override
    public void setElevation(double elevation)
    {
        double old = this.elevation;
        this.elevation = elevation;
        changeSupport.firePropertyChange(ELEVATION_PROPERTY, old, elevation);
    }

    @Override
    public String toString()
    {
        return "Description: " + description + "\nLatitude: " + latitude + "\nLongitude: " + longitude + "\nElevation: " + elevation + " Heading: " + heading + "\nPitch: " + pitch + "\nZoom: " + zoom;
    }

    @Override
    public void addPropertyChangeListener(@Nonnull PropertyChangeListener listener)
    {
        changeSupport.addPropertyChangeListener(listener);
    }

    @Override
    public void removePropertyChangeListener(@Nonnull PropertyChangeListener listener)
    {
        changeSupport.removePropertyChangeListener(listener);
    }
}
