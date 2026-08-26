package gov.takx.core.globe;

import gov.nasa.worldwind.layers.Layer;
import gov.nasa.worldwind.layers.LayerList;
import gov.nasa.worldwind.terrain.CompoundElevationModel;
import gov.takx.api.annotation.PseudoBean;
import gov.takx.api.globe.IApplicationGlobe;
import gov.takx.api.globe.IGlobeLayerManager;
import gov.takx.api.layer.IRaptorLayer;
import gov.takx.api.layer.IRaptorLayerManagerListener;
import gov.takx.api.layer.LayerGroup;
import gov.takx.api.platform.IProgressMonitor;
import gov.takx.api.platform.IRaptorShutdownListener;
import gov.takx.core.globe.layers.GlobeLayer;
import gov.takx.core.globe.layers.IGlobeLayer;
import gov.takx.core.globe.layers.WWGlobeLayer;
import gov.takx.core.globe.tak.TakLayerManager;
import gov.takx.core.layer.RaptorLayerManager;
import gov.takx.platform.logging.NullProgressMonitor;
import jakarta.annotation.PostConstruct;
import jakarta.inject.Inject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.lang.invoke.MethodHandles;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Manage globe layers.
 */
@SuppressWarnings("CdiManagedBeanInconsistencyInspection")
@PseudoBean
public class GlobeLayerManager
        extends AGlobeLayerManager
        implements PropertyChangeListener, IRaptorLayerManagerListener
{
    private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    private static final Map<IRaptorLayer, IGlobeLayer> sharedLayers = new HashMap<>();
    private static final Map<IRaptorLayer, Integer> layerUseCount = new HashMap<>();

    @Inject
    private TakLayerManager takLayerManager;

    /**
     * Construct the globe layer manager.
     *
     * @param globe The globe we are the layer manager for
     */
    public GlobeLayerManager(IApplicationGlobe globe)
    {
        this(globe, null);
    }

    /**
     * Calling the constructor with a non-null primaryManager will only happen for secondary globes. There is
     * probably a cleaner way to manage the pending layers, but for now I am going to elect to have one single
     * collection that is shared by all globes. If we are going to have dedicated collections then we need
     * to ensure that when the pendingLayers is modified in the primary that it is also modified in all the secondaries.
     *
     * @param globe The globe we are the layer manager for
     * @param primaryManager Primary GLM with which to share layer lists, if non-null
     */
    public GlobeLayerManager(IApplicationGlobe globe, IGlobeLayerManager primaryManager)
    {
        super(globe, primaryManager == null);
    }

    @PostConstruct
    private void postConstruct()
    {
        layerManager.addRaptorLayerManagerListener(this);

        initGlobeLayers();
    }

    private void initGlobeLayers()
    {
        // get a list of all the current raptor layers, sorted by unified index
        final List<IRaptorLayer> raptorLayers = getSortedRaptorLayers();

        // Attempt to add the layers in order of the sorted list
        for (IRaptorLayer raptorLayer : raptorLayers)
        {
            // Only process sharable layers
            if (!raptorLayer.isSharable()) continue;

            logger.debug("Restoring: {}", raptorLayer);

            addGlobeLayer(raptorLayer);
            raptorLayer.addPropertyChangeListener(this); // Listen for visibility changes
        }

        restoreTerrainService();
    }

    @Override
    public void onShutdown(IProgressMonitor progressMonitor)
    {
        raptorLayersRemoved();

        layerManager.removeRaptorLayerManagerListener(this);

        super.onShutdown(progressMonitor);
    }

    @Override
    public void raptorLayerAdded(IRaptorLayer raptorLayer)
    {
        addGlobeLayer(raptorLayer);
        raptorLayer.addPropertyChangeListener(this);
    }

    @Override
    public void raptorModelAdded(IRaptorLayer raptorLayer)
    {
        raptorLayerAdded(raptorLayer);
    }

    @Override
    public void raptorLayerRemoved(IRaptorLayer raptorLayer)
    {
        removeGlobeLayer(raptorLayer);
        raptorLayer.removePropertyChangeListener(this);
    }

    @Override
    public void raptorModelRemoved(IRaptorLayer raptorLayer)
    {
        raptorLayerRemoved(raptorLayer);
    }

    @Override
    public void raptorLayerMoved(IRaptorLayer raptorLayer)
    {
        final IGlobeLayer globeLayer = getGlobeLayer(raptorLayer);

        if (globeLayer != null)
        {
            if (globeLayer instanceof GlobeLayer applicationGlobeLayer)
            {
                LayerGroup layerGroup = raptorLayer.getLayerGroup();
                int index = getSortedRaptorLayers().stream()
                        .filter(l -> getGlobeLayer(l) instanceof GlobeLayer && l.getLayerGroup() == layerGroup)
                        .toList().indexOf(raptorLayer);
                takLayerManager.setLayerPosition(applicationGlobeLayer, layerGroup, index);
            } else
            {
                int index = getSortedRaptorLayers().stream()
                        .filter(l -> !(getGlobeLayer(l) instanceof GlobeLayer))
                        .toList().indexOf(raptorLayer);
                wwd.getModel().getLayers().remove(globeLayer);
                if (index >= 0)
                {
                    if (index > wwd.getModel().getLayers().size())
                    {
                        wwd.getModel().getLayers().add(globeLayer);
                    } else
                    {
                        wwd.getModel().getLayers().add(index, globeLayer);
                    }
                }
            }

            logLayerOrder();
        }
    }

    /**
     * Debug log of {@link RaptorLayerManager} layer order compared to the {@link GlobeLayerManager} layer order.
     */
    private void logLayerOrder()
    {
        if (!logger.isDebugEnabled()) return;

        List<IRaptorLayer> raptorLayers = getSortedRaptorLayers();

        logger.debug("** layer order: **");
        int i = 0;
        for (IRaptorLayer raptorLayer : raptorLayers)
        {
            int wwIndex = getWWLayerIndex(raptorLayer);

            String prefix = (wwIndex != i) ? "=>" : ""; // highlight an out-of-order layer with an arrow

            logger.debug("{}", String.format("%3s group: %24s  |  raptorIndex: %2d, wwIndex: %2d   |   name: %s",
                    prefix, raptorLayer.getLayerGroup().getName(), i, wwIndex, raptorLayer.getDisplayName()));

            i += 1;
        }
    }

    /**
     * Get a list of all layers in {@link RaptorLayerManager}, sorted by unified index.
     *
     * @return sorted list of {@link IRaptorLayer}s.
     */
    private List<IRaptorLayer> getSortedRaptorLayers()
    {
        List<IRaptorLayer> raptorLayers = new ArrayList<>();
        raptorLayers.addAll(layerManager.getLayers());
        raptorLayers.addAll(layerManager.getModelLayers());

        //pre-calculate each layer's actual unified index for sorting
        final Map<IRaptorLayer, Integer> layerToUnifiedIndexMap = new HashMap<>();
        for (IRaptorLayer layer : raptorLayers)
        {
            layerToUnifiedIndexMap.put(layer, getUnifiedLayerIndex(layer));
        }

        //sort the layer list using the unified indexes
        raptorLayers.sort(Comparator.comparingInt(layerToUnifiedIndexMap::get));

        return raptorLayers;
    }

    /**
     * Given an {@link IRaptorLayer}, find the ordering index of its associated Globe Layer in WorldWind
     *
     * @param raptorLayer a Raptor Layer
     * @return the Globe Layer index associated with the provided {@link IRaptorLayer}.
     */
    private int getWWLayerIndex(IRaptorLayer raptorLayer)
    {
        LayerList wwLayers = wwd.getModel().getLayers();
        for (int i = 0; i < wwLayers.size(); i++)
        {
            Layer layer = wwLayers.get(i);
            if (layer instanceof WWGlobeLayer globeLayer && globeLayer.getRaptorLayer().equals(raptorLayer)) return i;
        }
        return -1;
    }

    // TODO:  This is being called twice during a clear state...

    @Override
    public void raptorLayersRemoved()
    {
        getSortedRaptorLayers().forEach(this::raptorLayerRemoved);

        wwd.getModel().getLayers().clear(); // And clear them out

        // If we are the primary globe's layer manager, then perform a clean shutdown on the layers
        if (primaryGlobe)
        {
            sharedLayers.clear();
            layerUseCount.clear();
        }

        // And clear all the elevation models
        CompoundElevationModel compoundModel = (CompoundElevationModel) wwd.getModel().getGlobe().getElevationModel();

        compoundModel.getElevationModels().forEach(compoundModel::removeElevationModel);
    }

    /**
     * Handle a change in the properties of the layers that we are monitoring.
     *
     * @param evt Change event
     */
    @Override
    public void propertyChange(PropertyChangeEvent evt)
    {
        final String propName = evt.getPropertyName();

        if (propName.equals(IRaptorLayer.VISIBLE)) wwd.repaint();  // Make the change visible immediately
    }

    /**
     * Remove our globe layer (if any) representing the given {@link IRaptorLayer} from the WW context.
     * If this is the last reference to the layer (on any globe), then clean up after it.
     *
     * @param raptorLayer The layer to remove
     */
    private void removeGlobeLayer(IRaptorLayer raptorLayer)
    {
        final IGlobeLayer globeLayer = getGlobeLayer(raptorLayer);

        if (globeLayer != null)
        {
            decrementAndCleanupForLayer(raptorLayer, globeLayer);
            if (getLayerUseCount(raptorLayer) == 0)
            {
                if (globeLayer instanceof GlobeLayer applicationGlobeLayer)
                {
                    takLayerManager.removeLayer(applicationGlobeLayer, raptorLayer.getLayerGroup());
                } else
                {
                    wwd.getModel().getLayers().remove(globeLayer); // Remove it from our WW context
                }
            }
        }
    }

    /**
     * Decrement the layer usage count and if it is now zero, cleanly shut it down.
     *
     * @param raptorLayer RaptorLayer represented by the globe layer
     * @param globeLayer  GlobeLayer to process
     */
    private void decrementAndCleanupForLayer(IRaptorLayer raptorLayer, IGlobeLayer globeLayer)
    {
        if (decrementUseCount(raptorLayer) == 0)
        {
            globeLayer.setEnabled(false);

            // If the globe layer implements the shutdown listener interface, then tell it to shut down.  We do
            // this for all GlobeLayers without them needing to actually register as a listener.

            if (globeLayer instanceof IRaptorShutdownListener shutdownListener)
            {
                logger.debug("GLM-{} shutdown: {}", instance, raptorLayer);

                shutdownListener.onShutdown(new NullProgressMonitor());
            }
        }
    }

    /**
     * Add the globe layer (creating it if needed) for the given {@link IRaptorLayer} to this globe's model.
     *
     * @param raptorLayer The raptor layer being added
     */
    private void addGlobeLayer(IRaptorLayer raptorLayer)
    {
        final String name = raptorLayer.getName();

        if (logger.isDebugEnabled())
        {
            logger.debug("GLM-{} add raptorLayer: {} globe={}", instance, name, raptorLayer.getSpecificGlobeId());
        }

        // If this raptor layer is aimed at a specific globe, and it's not ours, then bail
        if (!raptorLayer.isSharable() && raptorLayer.getSpecificGlobeId() != globe.getGlobeId())
        {
            logger.debug("GLM-{} Skip non-sharable layer", instance);
            return;
        }

        IGlobeLayer globeLayer = sharedLayers.get(raptorLayer);

        // If we found a shared layer for this RaptorLayer, then we just use that layer here.  If not, then
        // we need to construct the underlying WW layer from either a class name or a config file name.

        if (globeLayer != null)
        {
            if (primaryGlobe) logger.debug("Found existing shared layer on primary globe: {}", globeLayer.getName());

            logger.debug("GLM-{} got shared globeLayer: {}", instance, globeLayer.getName());
        } else
        {
            if (logger.isDebugEnabled())
            {
                logger.debug("GLM-{} create: n={} cn={} xml={}", instance, name, raptorLayer.getClassName(), raptorLayer.getConfigFileName());
            }
            try
            {
                globeLayer = globeLayerFactory.constructGlobeLayer(raptorLayer);
            } catch (Exception e)
            {
                logger.error("Failed to construct Globe Layer {}", name, e);
                return;
            }
        }

        if (globeLayer == null)
        {
            logger.error("Unable to create layer: {}", name);
            layerManager.removeLayer(raptorLayer, false);
            return;
        }

        // We have a use of the layer, so bump the count
        incrementUseCount(raptorLayer);

        // If we are the primary GLM, then do some bookkeeping
        if (primaryGlobe)
        {
            // If this layer is shareable, then save it for later
            if (raptorLayer.isSharable() && globeLayer.isSharable())
            {
                if (logger.isDebugEnabled()) logger.debug("GLM-{} Add to shared: n={} cn={}", instance, name, globeLayer.getClass());
                sharedLayers.put(raptorLayer, globeLayer);
            }
        }

        // It turned out that when displaying secondary globes that we were adding a bunch of duplicate layers
        // Adding the "equals" and checking for "contains" fixes that problem.
        //
        // NOTE: This should no longer be true, but we'll leave in the check and add a warning message

        if (!getGlobeLayers().contains(globeLayer))
        {
            if (globeLayer instanceof GlobeLayer applicationGlobeLayer)
            {
                final var layerGroup = raptorLayer.getLayerGroup();
                final var layerList = getSortedRaptorLayers().stream()
                        .filter(l -> l.getLayerGroup() == layerGroup)
                        .filter(l -> l == raptorLayer || getGlobeLayer(l) != null)
                        .toList();
                int index = layerList.indexOf(raptorLayer);
                if (index < 0) index = layerList.size(); // Add to the end if not found
                takLayerManager.addLayer(applicationGlobeLayer, layerGroup, index);
            } else
            {
                final LayerList wwLayers = wwd.getModel().getLayers();
                int index = getSortedRaptorLayers().stream().filter(l -> !(getGlobeLayer(l) instanceof GlobeLayer) || l == raptorLayer).toList().indexOf(raptorLayer);
                if (index >= wwLayers.size())
                {
                    wwLayers.add(globeLayer);
                } else
                {
                    wwLayers.add(index, globeLayer);
                }
            }
        }

        logLayerResolution(globeLayer);
    }

    @Override
    public int getLayerUseCount(IRaptorLayer raptorLayer)
    {
        return layerUseCount.getOrDefault(raptorLayer, 0);
    }

    /**
     * Increment the usage count on a given layer.
     *
     * @param raptorLayer The layer being referenced
     */
    private void incrementUseCount(IRaptorLayer raptorLayer)
    {
        layerUseCount.merge(raptorLayer, 1, (count, ignored) -> count + 1);
    }

    /**
     * Decrement the usage count on a given layer.  If the count reaches zero, remove the entry from the map.
     *
     * @param raptorLayer The layer being referenced
     * @return The new count after the decrement
     */
    private int decrementUseCount(IRaptorLayer raptorLayer)
    {
        final Integer newCount = layerUseCount.merge(raptorLayer, 0, (count, ignored) -> --count == 0 ? null : count);
        return newCount == null ? 0 : newCount;
    }

    private int getUnifiedLayerIndex(IRaptorLayer raptorLayer)
    {
        final LayerGroup raptorLayerLayerGroup = raptorLayer.getLayerGroup();

        int index = layerManager.getIndex(raptorLayer);

        for (LayerGroup layerGroup : LayerGroup.values())
        {
            // We can stop iterating once we get to the group that contains the raptorLayer
            if (raptorLayerLayerGroup == layerGroup) break;

            index += layerManager.getLayerCount(layerGroup);
        }
        return index;
    }
}
