package gov.takx.api.structures;

import gov.nasa.worldwind.Disposable;
import gov.nasa.worldwind.Exportable;
import gov.nasa.worldwind.geom.Sector;
import gov.nasa.worldwind.render.Renderable;
import gov.nasa.worldwind.render.ShapeAttributes;
import gov.takx.api.coordinatesystem.ILocation;
import gov.takx.api.layer.IRaptorLayer;

import java.util.UUID;

/**
 * This interface define the operations available on a 3D surface structure.  Surface structures are immutable since
 * they are defined externally to TAKX.  Surface structures are a simple 3D polygon tied to the surface of the globe.
 * The height of the building is relative to the ground elevation at one of the points in the polygon, as
 * specified via the <em>reference index</em>.
 *
 * @since 1.5
 */
public interface ISurfaceStructure extends IIntersectable, Disposable, Exportable
{
    /**
     * Attribute style for application to individual surface structures.
     *
     * @since 3.0
     */
    enum AttributeStyle
    {
        /**
         * Default/initial state.
         */
        DEFAULT,

        /**
         * Indicates that the mouse is over the structure.
         */
        HIGHLIGHTED,

        /**
         * Indicates that the structure has been "selected", typically by clicking on it with the mouse.
         */
        SELECTED,
    }

    /**
     * @return The underlying renderable
     * @since 3.0
     */
    Renderable getRenderable();

    /**
     * @return The current attributes for this shape
     * @since 3.0
     */
    ShapeAttributes getAttributes();

    /**
     * Reset the current shape attributes.  If the shape is currently selected, then the selected attributes
     * will be installed.  If not, then the default attributes will be restored.
     *
     * @since 3.0
     */
    void resetAttributes();

    /**
     * Set the attributes that control the rendering of the this surface structure.
     *
     * @param attributeStyle new attribute state to apply
     * @since 3.0
     */
    void setStructureAttributes(AttributeStyle attributeStyle);

    /**
     * Get the UUID for the shape. This attribute is immutable.
     *
     * @return The universally unique ID for this structure
     */
    UUID getUUID();

    /**
     * Get the persistable object for this shape.
     *
     * @return the persistable object
     */
    IPersistableSurfaceStructure getPersistable();

    /**
     * Set the layer that this shape belongs to. This should only be called once at the beginning of the shapes
     * life, and should be considered immutable after that.
     *
     * @param raptorLayer The layer holding/rendering the shape
     */
    void setLayer(IRaptorLayer raptorLayer);

    /**
     * @param name Set the name of this shape
     */
    void setName(String name);

    /**
     * @param description Set the description for this shape
     */
    void setDescription(String description);

    /**
     * Get the URL for this shape. The URL will be available in TAKX for the user to navigate to with a web browser.
     *
     * @return the URL
     */
    String getUrl();

    /**
     * Get the name of this shape.
     *
     * @return the name
     */
    String getName();

    /**
     * Get the description for this shape.
     *
     * @return the description
     */
    String getDescription();

    /**
     * Get the layer that this shape belongs to
     *
     * @return the {@link IRaptorLayer}
     */
    IRaptorLayer getLayer();

    /**
     * Get the location for this shape.
     *
     * @return the {@link ILocation}
     */
    ILocation getLocation();

    /**
     * @param url Set the URL for this shape
     */
    void setUrl(String url);

    /**
     * Set the selection state for this state. This attribute is completely transient. This is a "rendering" state for
     * the shape only, and will not be persisted or remembered in any way.
     *
     * @param selected selected state
     */
    void setSelected(boolean selected);

    /**
     * Returns the object's geographic extent.
     *
     * @return the object's geographic extent.
     */
    Sector getSector();
}
