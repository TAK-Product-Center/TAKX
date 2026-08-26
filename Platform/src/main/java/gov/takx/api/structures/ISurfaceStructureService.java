package gov.takx.api.structures;

import gov.nasa.worldwind.geom.LatLon;
import gov.nasa.worldwind.geom.Sector;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import java.io.File;
import java.util.Collection;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

/**
 * This interface defines the operations for accessing 3D structure models (normally representing buildings).
 * Methods exist for finding structures that contain a lat/lon point, contained in or intersecting a lat/lon region,
 * or by unique id.
 *
 * @since 1.5
 */
@SuppressWarnings({"unused"})
public interface ISurfaceStructureService
{
    /**
     * Event name for surface structure selected events.
     *
     * @since 1.42
     */
    String SURFACE_STRUCTURE_SELECTED = "surfaceStructureSelected";

    /**
     * Event name for surface structures added events.
     *
     * @since 1.42
     */
    String SURFACE_STRUCTURES_ADDED = "surfaceStructuresAdded";

    /**
     * Event name for surface structures removed events.
     *
     * @since 1.42
     */
    String SURFACE_STRUCTURES_REMOVED = "surfaceStructuresRemoved";

    /**
     * Returns all structures that contain a specified location.
     *
     * @param location the location of interest.
     * @return Newly allocated collection of structures at the specified location.
     * @see #getAllStructures()
     */
    Collection<ISurfaceStructure> getStructures(@Nonnull LatLon location);

    /**
     * Returns all Renderables in the layer.
     *
     * @return the Collection of Renderables in the layer.
     */
    Collection<ISurfaceStructure> getAllStructures();

    /**
     * Returns all renderables, from all surface structure layers, that are within a given bounds. Passing in a
     * {@code null} sector throws an illegal argument exception.
     *
     * @param sector the bounds to limit the returned renderables from
     * @return the Collection of Renderables in the layer.
     * @since 1.11
     */
    Collection<ISurfaceStructure> getStructures(@Nonnull Sector sector);

    /**
     * Returns all currently selected structures.
     *
     * @return the Collection of Selected Renderables in this service.
     * @since 1.24
     */
    Collection<ISurfaceStructure> getSelectedStructures();

    /**
     * Get the structure with the given UUID.  If no such structure exists in the model, then {@code null} is returned.
     *
     * @param uuid UUID of the structure to return
     * @return structure or {@code null} if not found
     */
    ISurfaceStructure getStructure(UUID uuid);

    /**
     * Load the structures defined in the given shape file.
     *
     * @param shapeFile File to read for structure definitions
     */
    CompletableFuture<Integer> loadShapeFile(@Nonnull File shapeFile);

    /**
     * Load the structures defined in the given shape file into a specific surface structure layer. The available surface
     * structure layer names can be obtained by calling {@link #getLayerList}. Attempting to load a shape
     * file into a layer not contained within that list will create a new surface structure layer with that name, and
     * then insert the rendered shape file data into that layer.
     * <p/>
     * Note: It is safe to provide a {@code null} for either layerName or callback.
     * <p/>
     *
     * @param shapeFile File to read for structure definitions
     * @param layerName name of the surface structure layer to insert the rendered shape file data into
     * @param callback  Provide a ISurfaceStructureCallback so that you will be notified when the load is complete.
     * @return Future to obtain the number of structures created
     * @since 1.11
     */
    CompletableFuture<Integer> loadShapeFile(@Nonnull File shapeFile, @Nonnull String layerName,
                                             @Nullable ISurfaceStructureCallback callback);

    /**
     * Add a surface structure service listener.  These listeners are notified of all events originated by or
     * propagating through the surface structure service.
     *
     * @param listener listener to add.
     * @since 1.42
     */
    void addSurfaceStructureServiceListener(@Nonnull ISurfaceStructureServiceListener listener);

    /**
     * Remove a surface structure service listener.
     *
     * @param listener listener to remove.
     * @since 1.42
     */
    void removeSurfaceStructureServiceListener(@Nonnull ISurfaceStructureServiceListener listener);

    /**
     * A list containing all of the names of the layers belonging to the surface structures layer group.
     *
     * @return list of surface structure layers.
     * @since 1.11
     */
    List<String> getLayerList();

    /**
     * Persist the structure. This is useful if any properties of the structure have changed. In many cases the
     * persistence will happen automatically, but the best practice is to go ahead and persist the structure
     * whenever it is modified.
     *
     * @param struct Structure to persist
     */
    void persistStructure(@Nonnull ISurfaceStructure struct);

    /**
     * This adds the ability for a plugin to register a meta data key of interest.  This will then allow a user to
     * easily enter a value for the key in each structure.  This meta data key and value will be persisted for future
     * lookup if the user enters a value for the key.
     *
     * @param metaDataKey The key of the meta data of interest
     */
    void addMetaDataOfInterest(@Nonnull String metaDataKey);

    /**
     * Get a list of registered meta data keys of interest.
     *
     * @return list of registered meta data keys of interest.
     */
    List<String> getMetaDataOfInterest();

    /**
     * Get a list of available shapes (contained in {@link IIntersectableShape} objects).
     *
     * @return a list of available shapes.
     */
    List<IIntersectableShape> getShapes();
}
