package gov.takx.api.structures;

import gov.nasa.worldwind.geom.LatLon;
import gov.tak.api.util.AttributeSet;
import gov.takx.api.coordinatesystem.ILocation;
import gov.takx.api.coordinatesystem.RaptorLocation;
import gov.takx.api.layer.IRaptorLayer;
import gov.takx.api.persist.IPersistable;

import java.util.UUID;

/**
 * This interface defines the operations available on a 3D surface structure.  Surface structures are immutable since
 * they are defined externally to TAKX.  Surface structures are a simple 3D polygon tied to the surface of the globe.
 * The height of the building is relative to the ground elevation at one of the points in the polygon, as
 * specified via the <em>reference index</em>.
 *
 * @since 1.5
 */
public interface IPersistableSurfaceStructure extends IPersistable
{
    /**
     * @return The universally unique ID for this structure
     */
    UUID getUUID();

    /**
     * @return The structure "go to" location
     */
    ILocation getLocation();

    /**
     * @return The set of vertices (lat/lon coordinates) describing the polygon
     */
    Iterable<? extends LatLon> getVertices();

    /**
     * @return The structure description
     */
    String getDescription();

    /**
     * set the description
     */
    void setDescription(String description);

    /**
     * @return The structure name
     */
    String getName();

    /**
     * set the name
     */
    void setName(String name);

    /**
     * @return The structure URL
     */
    String getUrl();

    /**
     * set the URL
     */
    void setUrl(String url);

    /**
     * @return the layer that "owns" this structure.
     */
    IRaptorLayer getLayer();

    /**
     * Set the layer that this shape is associated with.
     */
    void setLayer(IRaptorLayer layer);

    /**
     * Set the location of the shape.
     *
     * @param location "go to" location
     */
    void setLocation(RaptorLocation location);

    /**
     * @return the values stored with this surface structure
     */
    AttributeSet getMetadata();
}
