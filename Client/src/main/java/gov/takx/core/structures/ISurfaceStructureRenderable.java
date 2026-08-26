package gov.takx.core.structures;

import gov.nasa.worldwind.geom.Sector;
import gov.nasa.worldwind.render.GeographicExtent;
import gov.takx.api.structures.ISurfaceStructure;

/**
 * Contract for renderables that represent a surface structure.
 *
 * @since 3.0
 */
public interface ISurfaceStructureRenderable extends GeographicExtent
{
    /**
     * @return The surface structure for which this object is the renderable
     */
    ISurfaceStructure getSurfaceStructure();

    /**
     * Some renderable classes don't implement {@link GeographicExtent}, so we provide a default implementation
     * by reaching back to the containing surface structure.
     *
     * @return The bounding sector for this renderable
     */
    @Override
    default Sector getSector()
    {
        return getSurfaceStructure().getSector();
    }
}
