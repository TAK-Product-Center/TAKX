package gov.takx.core.view.gui.layers.viewmanager;

import gov.takx.api.annotation.PseudoBean;
import gov.takx.api.layer.IRaptorLayer;
import gov.takx.api.layer.IRaptorLayerManager;
import gov.takx.api.layer.LayerGroup;
import gov.takx.core.globe.LayerBoundsManager;
import gov.takx.core.layer.RaptorLayerManager;
import jakarta.inject.Inject;

import java.util.List;

/**
 * A Layer Table Model for a {@link LayerGroup}, which is directly backed by the {@link RaptorLayerManager}. Changes made in the
 * {@link RaptorLayerManager} will immediately be reflected in this model.
 *
 */
@SuppressWarnings("CdiManagedBeanInconsistencyInspection")
@PseudoBean
public class LayerTableModel extends AMapGroupPanel.AGroupTableModel<IRaptorLayer>
{
    @Inject
    protected IRaptorLayerManager layerManager;

    @Inject
    protected LayerViewManager layerViewManager;

    @Inject
    private LayerBoundsManager layerBoundsManager;

    private final LayerGroup group;

    public LayerTableModel(String groupName)
    {
        group = LayerViewUtils.nameToGroup(groupName);
    }

    /**
     * Get the layer group's layer count from the {@link RaptorLayerManager}.
     */
    @Override
    public int getRowCount()
    {
        return layerManager.getLayerCount(group);
    }

    /**
     * Get the layer from the {@link RaptorLayerManager} specified by the row index, and return the appropriate value.
     */
    @Override
    public Object getValueAt(int rowIndex, int columnIndex)
    {
        final IRaptorLayer layer = getRowObject(rowIndex);

        return switch (LayerViewUtils.Column.VALUES[columnIndex])
        {
            case VISIBLE -> layer.isVisible();
            case TITLE -> layer.getDisplayName();
            case STATUS -> layer.isLayerRendered() ? LayerViewUtils.IN_VIEW_ICON : null;
            case BOUNDING_BOX ->
            {
                if (!layerBoundsManager.canShowBoundingBox(layer)) yield null;
                yield layerBoundsManager.isShowingBoundingBox(layer) ? LayerViewUtils.SHOWING_BOUNDING_BOX_ICON : LayerViewUtils.HIDING_BOUNDING_BOX_ICON;
            }
            case FAVORITE -> layerViewManager.isFavorite(layer) ? LayerViewUtils.FAVORITE_ICON : LayerViewUtils.NOT_FAVORITE_ICON;
            case OBJECT -> layer;
        };
    }

    @Override
    protected void setEntryVisible(int rowIndex, boolean visible)
    {
        getRowObject(rowIndex).setVisible(visible);
    }

    /**
     * Get the layer for a table row index. Layers are displayed in reverse order on the table (i.e. 0th row is at the
     * top and represents the last layer in the layer list), so modify the row index to represent this reverse order
     * and get the corresponding layer from the {@link RaptorLayerManager}.
     */
    @Override
    protected IRaptorLayer getRowObject(int rowIndex)
    {
        List<IRaptorLayer> layerList = layerManager.getLayerList(group);

        // Reverse the layer order
        return layerList.get(layerList.size() - 1 - rowIndex);
    }
}
