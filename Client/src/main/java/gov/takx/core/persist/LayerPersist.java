package gov.takx.core.persist;

import gov.takx.api.layer.IRaptorLayer;
import gov.takx.api.layer.LayerGroup;
import gov.takx.api.persist.ILayerPersist;
import gov.takx.api.persist.IPersistServiceProvider;
import gov.takx.api.query.param.HQLParameter;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import java.util.List;

/**
 * Layer persistence services
 */
@ApplicationScoped
public class LayerPersist implements ILayerPersist
{
    @Inject
    private IPersistServiceProvider persistServiceProvider;

    @Override
    public void persistLayer(IRaptorLayer o)
    {
        persistServiceProvider.getPersistService().persistEntity(o);
    }

    @Override
    public List<IRaptorLayer> queryAll(LayerGroup group)
    {
        return internalQuery(group, null);
    }

    @Override
    public List<IRaptorLayer> query(LayerGroup group, boolean deleted)
    {
        return internalQuery(group, deleted);
    }

    /**
     * Internal method to query for layers that match the given criteria.
     *
     * @param group   Layer group
     * @param deleted If non-null, the deleted state
     * @return List of matching layers
     */
    private List<IRaptorLayer> internalQuery(LayerGroup group, Boolean deleted)
    {
        String hql = "from ClientLayer where layerGroup=:group";

        //noinspection VariableNotUsedInsideIf
        if (deleted != null)
        {
            hql += " and deleted=:deleted";
        }
        hql += " order by orderkey";

        return persistServiceProvider.getPersistService().query(hql,
                new HQLParameter("group", group),
                new HQLParameter("deleted", deleted));
    }

    @Override
    public void deleteLayer(IRaptorLayer layer)
    {
        layer.setDeleted(true);
        persistLayer(layer);
    }
}
