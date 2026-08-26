package gov.takx.api.globe;

import gov.takx.api.coordinatesystem.ILocation;
import gov.takx.api.mapobject.IMapObject;
import jakarta.annotation.Nonnull;

import java.io.Serializable;

/**
 * Interface for representing a map object on the globe.
 *
 */
public interface IGlobeMapObject extends Serializable
{
    IMapObject getMapObject();

    void setHighlighted(boolean highlighted);

    boolean isHighlighted();

    void setSelected(boolean selected);

    boolean isSelected();

    void setMapObject(IMapObject cmo);

    /**
     * Get the position of the globe map object. The interpretation of this is implementation-specific.
     *
     * @return the globe map object position
     * @since 5.5
     */
    @Nonnull
    ILocation getLocation();

    /**
     * Set the location of the globe map object.
     *
     * @param location the new globe map object location
     */
    void setLocation(ILocation location);

    /**
     * Return if the object should be viewable, meaning that according to all the system and
     * object settings it should be rendered on the globe.  This includes checks such as
     * isVisible and whether the security filter passes.
     *
     * @return true if the object should be viewable on the globe
     */
    boolean isViewable();

    /**
     * Called when this globe map object's peer map object is removed. This is when listeners should
     * be deregistered, resources released, etc.
     */
    void peerRemoved();
}
