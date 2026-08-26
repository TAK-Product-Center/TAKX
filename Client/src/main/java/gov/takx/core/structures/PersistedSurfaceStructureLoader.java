package gov.takx.core.structures;

import gov.takx.api.globe.IExtendedGlobeManager;
import gov.takx.api.layer.IRaptorLayer;
import gov.takx.api.layer.IRaptorLayerManager;
import gov.takx.api.layer.LayerGroup;
import gov.takx.api.persist.IPersistServiceProvider;
import gov.takx.api.platform.IProgressMonitor;
import gov.takx.api.query.param.HQLParameter;
import gov.takx.dataservices.persist.PersistableSurfaceStructure;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.invoke.MethodHandles;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * A persisted surface structure loader class. Most of this code was previously in {@code SurfaceStructureService},
 * but it was cluttering the class, so it's now separate class. Also, the structure loading now updates the TAKX project
 * progress monitor for visual feedback of its status.
 *
 */
@ApplicationScoped
public class PersistedSurfaceStructureLoader
{
    private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    private final IExtendedGlobeManager globeManager;
    private final IPersistServiceProvider persistServiceProvider;
    private final IRaptorLayerManager layerManager;

    @Inject
    PersistedSurfaceStructureLoader(IExtendedGlobeManager globeManager, IRaptorLayerManager layerManager, IPersistServiceProvider persistServiceProvider)
    {
        this.globeManager = globeManager;
        this.layerManager = layerManager;
        this.persistServiceProvider = persistServiceProvider;
    }

    /**
     * Loads persisted surface structures.  When the globe is ready, the {@code finalizeLayers} method
     * will be called to do the work of actually adding the surface structures to the layers.
     *
     * @param progressMonitor project startup progress monitor
     */
    public void loadPersistedSurfaceStructures(IProgressMonitor progressMonitor)
    {
        progressMonitor.setSubstatusMaximum(IProgressMonitor.INDETERMINATE);

        final long start = System.currentTimeMillis();
        int shapeCount = 0;

        logger.debug("Starting structure persistable restore");

        final List<IRaptorLayer> layers = layerManager.getLayerList(LayerGroup.SURFACE_STRUCTURES);
        final List<PersistableSurfaceStructure> persistableStructures = new ArrayList<>();

        for (IRaptorLayer layer : layers)
        {
            shapeCount += loadPersistableEntities(layer, persistableStructures, progressMonitor);
        }

        if (logger.isDebugEnabled())
        {
            logger.debug("{} layer(s) and {} shape(s) restored in {} milliseconds",
                    layers.size(), shapeCount, System.currentTimeMillis() - start);
        }

        // Finalize the layers when the globe is ready.
        globeManager.getPrimaryGlobeDelegate().doWhenReady(() -> finalizeLayers(persistableStructures));
    }

    /**
     * Load the persisted entities for a layer from the specified table.
     *
     * @param layer                 Layer for which to load entities
     * @param persistableStructures List to which to add loaded entities
     * @param progressMonitor       Progress monitor to update
     * @return count of entities loaded
     * @since 3.0
     */
    private int loadPersistableEntities(IRaptorLayer layer,
                                        final List<PersistableSurfaceStructure> persistableStructures,
                                        IProgressMonitor progressMonitor)
    {
        int shapeCount = 0;

        try
        {
            // Exclude PersistableShapefileShape in this query since it has its own restoration logic
            // in LargeShapefileLayer.

            final String hql = "from PersistableSurfaceStructure pss where" +
                    " type(pss) != PersistableShapefileShape and layer = :layer";

            final List<PersistableSurfaceStructure> results = persistServiceProvider.getPersistService()
                    .query(hql, new HQLParameter("layer", layer));

            for (PersistableSurfaceStructure persistable : results)
            {
                // This is a little subtle, but we need to replace the layer reference in the persistable
                // since it is a reference to a layer that was pulled into the session due to the query
                // and is not the same instance as the one here (the one from the layer manager).
                // If we don't do this, then when the globe is ready, and creates the underlying WW layer
                // for the Raptor layer, it will be updating the layer handled by the layer manager, and not
                // the one referenced by this persistable, which would remain null.

                persistable.setLayer(layer);
                persistableStructures.add(persistable);

                if (shapeCount++ % 50 == 0) progressMonitor.setSubstatus("Structures loaded: " + shapeCount);
            }
        } catch (Exception e)
        {
            logger.error("Error loading surface structures", e);
        }

        return shapeCount;
    }

    /**
     * Once the globe has been initialized we add the structures to their associated layers.
     */
    private void finalizeLayers(List<PersistableSurfaceStructure> persistableStructures)
    {
        final Set<CachedSurfaceStructureLayer> layersAffected = new HashSet<>();

        for (PersistableSurfaceStructure persistable : persistableStructures)
        {
            final CachedSurfaceStructureLayer structureLayer = (CachedSurfaceStructureLayer) persistable.getLayer().getLayer();

            layersAffected.add(structureLayer);

            // Reconstitute the surface structure and add it to the layer
            structureLayer.add(SurfaceStructureFactory.createFromPersistable(persistable));
        }

        // Inform the layers that we're done
        layersAffected.forEach(CachedSurfaceStructureLayer::batchAddDone);

        if (logger.isDebugEnabled()) logger.debug("total structures:{}", persistableStructures.size());
    }
}
