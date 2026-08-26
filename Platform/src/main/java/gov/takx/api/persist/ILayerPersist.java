package gov.takx.api.persist;

import gov.takx.api.layer.IRaptorLayer;
import gov.takx.api.layer.LayerGroup;

import java.util.List;

/**
 */
public interface ILayerPersist
{

    void persistLayer(IRaptorLayer o);

    /**
     * Query for the layers in the given group, this includes deleted layers.
     *
     * @param group Layer group to query.
     * @return List of layers in group
     */
    List<IRaptorLayer> queryAll(LayerGroup group);

    /**
     * Query for the layers in a group that have the given deleted state.
     *
     * @param group   Layer group
     * @param deleted Deleted state
     * @return List of matching layers
     */
    List<IRaptorLayer> query(LayerGroup group, boolean deleted);

    /**
     * Mark a layer as deleted and persist that change.  Note that we don't actually deleted layers, we just
     * set the deleted flag and update the row in the table.
     *
     * @param layer The layer to "delete"
     */
    void deleteLayer(IRaptorLayer layer);
}
