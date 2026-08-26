package gov.takx.core.structures;

import gov.nasa.worldwind.geom.LatLon;
import gov.nasa.worldwind.geom.Sector;
import gov.nasa.worldwind.layers.CachedRenderableLayer;
import gov.nasa.worldwind.render.DrawContext;
import gov.nasa.worldwind.render.Renderable;
import gov.takx.api.structures.ISurfaceStructure;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.invoke.MethodHandles;
import java.util.Collection;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

/**
 * Extension of World Wind's cached renderable layer to specifically handle our surface structures and to
 * override {@link #isLayerInView(DrawContext)} to minimize rendering overhead when the structure bounding
 * sector is not in the current view.
 *
 */
public class CachedSurfaceStructureLayer extends CachedRenderableLayer implements IStructureLayer
{
    private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    protected Sector originalSector;
    protected Sector boundingSector = null;

    /**
     * CachedSurfaceStructureLayer constructor. Creates a cached surface structure layer with a given sector of
     * geographic coverage. Any renderables added to this layer must be within the bounds of this sector to be rendered.
     *
     * @param sector bounds of this layer
     */
    public CachedSurfaceStructureLayer(Sector sector)
    {
        super(sector);
        originalSector = sector;
    }

    @Override
    public void add(ISurfaceStructure structure)
    {
        // Add the "real" renderable to the layer using the UUID as the name
        super.add(structure.getRenderable(), structure.getUUID().toString());

        // Update our bounding sector
        final Sector structureSector = structure.getSector();

        boundingSector = boundingSector == null
                ? structureSector
                : Sector.union(boundingSector, structureSector);
    }

    /**
     * Perform any processing or cleanup after a "batch" (typically a file) of structure additions is complete.
     * This base class does nothing with this notification.  It is here to unify handling with sub-classes.
     *
     * @since 3.0
     */
    public void batchAddDone()
    {
    }

    @Override
    public Collection<? extends ISurfaceStructure> getStructures(Sector extent)
    {
        return renderableStreamToStructures(getRenderables(extent).stream());
    }

    @Override
    public Collection<? extends ISurfaceStructure> getAllStructures()
    {
        return renderableStreamToStructures(StreamSupport.stream(getAllRenderables().spliterator(), false));
    }

    @Override
    public Collection<? extends ISurfaceStructure> getStructures(LatLon location)
    {
        return renderableStreamToStructures(getRenderables(location).stream());
    }

    @Override
    public ISurfaceStructure getStructureByName(String name)
    {
        final Renderable renderable = getByName(name);

        return renderable != null
                ? ((ISurfaceStructureRenderable) renderable).getSurfaceStructure()
                : null;
    }

    /**
     * Remove the given structure's renderable from the layer.
     *
     * @param structure Structure to process
     * @since 3.0
     */
    public void removeStructure(ISurfaceStructure structure)
    {
        removeByName(structure.getUUID().toString());
    }

    /**
     * Convert a Stream of renderables to their containing surface structure.  All renderables will be instances
     * of {@link ISurfaceStructureRenderable} due to the logic in {@link #add(ISurfaceStructure)}.
     *
     * @param stream Stream to process
     * @return collection of containing structures
     * @since 3.0
     */
    private Collection<? extends ISurfaceStructure> renderableStreamToStructures(Stream<? extends Renderable> stream)
    {
        return stream
                .map(r -> ((ISurfaceStructureRenderable) r).getSurfaceStructure())
                .collect(Collectors.toList());
    }

    /**
     * Override our base class' implementation to prevent raw renderables from being added.
     */
    @Override
    public void add(Renderable renderable, String name)
    {
        logger.error("addition of Renderable is not supported: {}", renderable.getClass(), new RuntimeException());
    }

    @Override
    public boolean isLayerInView(DrawContext dc)
    {
        final Sector visibleSector = dc.getVisibleSector();

        return visibleSector == null || getSector().intersects(visibleSector);
    }

    /**
     * @return geographic area covered by this layers renderables
     */
    @Override
    public Sector getSector()
    {
        return boundingSector != null ? boundingSector : originalSector;
    }
}
