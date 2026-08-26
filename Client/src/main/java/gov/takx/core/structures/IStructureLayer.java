package gov.takx.core.structures;

import gov.nasa.worldwind.geom.LatLon;
import gov.nasa.worldwind.geom.Sector;
import gov.nasa.worldwind.render.GeographicExtent;
import gov.takx.api.structures.ISurfaceStructure;

import java.util.Collection;

/**
 * Contract for layers managed by ISurfaceStructureService.  Provides various means of accessing the
 * structures within the layer.
 *
 * @since 3.0 (existed previously, but reworked in 3.0)
 */
public interface IStructureLayer extends GeographicExtent
{
    /**
     * Add a surface structure to the layer.
     *
     * @param structure Structure to add
     */
    void add(ISurfaceStructure structure);

    /**
     * Get all surface structures in this layer.
     *
     * @return iterable of surface structures in this layer.
     */
    Collection<? extends ISurfaceStructure> getAllStructures();

    /**
     * Get all surface structures within a Sector in this layer.
     *
     * @param extent Sector of interest.
     * @return collection of surface structures within the Sector.
     */
    Collection<? extends ISurfaceStructure> getStructures(Sector extent);

    /**
     * Get all surface structures under a location.
     *
     * @param location location of interest.
     * @return collection of surface structures under a location.
     */
    Collection<? extends ISurfaceStructure> getStructures(LatLon location);

    /**
     * Access a surface structure by its name.
     *
     * @param name name of surface structure.
     * @return Surface structure with the given name, {@code null} otherwise
     */
    ISurfaceStructure getStructureByName(String name);

    /**
     * Notify is instance that it has been removed and should dispose of any resources it holds.
     */
    void dispose();
}
