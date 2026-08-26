package gov.takx.core.layer;

import gov.takx.api.layer.IRaptorLayer;
import gov.takx.api.layer.IRaptorLayerManager;
import gov.takx.api.layer.IRaptorLayerManagerListener;
import gov.takx.api.layer.LayerGroup;

import java.util.HashSet;
import java.util.Set;

/**
 * Intended to be registered as an {@link IRaptorLayerManagerListener} with the {@link IRaptorLayerManager}, to track
 * notifications of layer addition or removal.
 *
 * @since 5.0
 */
public class TestRaptorLayerManagerListener implements IRaptorLayerManagerListener
{
    private final Set<String> layerNamesAdded = new HashSet<>();
    private final Set<String> layerNamesRemoved = new HashSet<>();
    private final boolean imageryGroupOnly;

    public TestRaptorLayerManagerListener(boolean imageryGroupOnly)
    {
        this.imageryGroupOnly = imageryGroupOnly;
    }

    @Override
    public void raptorLayerAdded(IRaptorLayer raptorLayer)
    {
        if (imageryGroupOnly && raptorLayer.getLayerGroup() != LayerGroup.IMAGERY) return;

        layerNamesAdded.add(raptorLayer.getName());
    }

    @Override
    public void raptorLayerRemoved(IRaptorLayer raptorLayer)
    {
        if (imageryGroupOnly && raptorLayer.getLayerGroup() != LayerGroup.IMAGERY) return;

        layerNamesRemoved.add(raptorLayer.getName());
    }

    /**
     * @return The set of names of layers added
     * @since 5.6
     */
    public Set<String> getLayerNamesAdded()
    {
        return layerNamesAdded;
    }

    /**
     * @return The set of names of layers removed
     * @since 5.6
     */

    public Set<String> getLayerNamesRemoved()
    {
        return layerNamesRemoved;
    }
}
