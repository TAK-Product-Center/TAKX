package gov.takx.core.view.gui.layers.viewmanager;

import gov.takx.api.layer.IRaptorLayer;

import javax.swing.ListSelectionModel;

/**
 */
public class LayerFavoritesPanel extends LayerGroupPanel
{
    public LayerFavoritesPanel(final LayerViewPanel owner, LayerViewManager manager)
    {
        super(LayerViewUtils.FAVORITES, owner, manager, new FavoritesTableModel(manager));
        getGroupTable().setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
    }

    /**
     * Get the favorite layer from the Layer View Manager.
     */
    @Override
    protected IRaptorLayer layerForRow(int row)
    {
        return row < 0 ? null : manager.getFavoriteLayer(row);
    }

    /**
     * An extended version of Layer Table Model which provides Favorite layers from the Layer View Manager.
     */
    private static class FavoritesTableModel extends LayerTableModel
    {
        private final LayerViewManager manager;

        public FavoritesTableModel(LayerViewManager manager)
        {
            super(null);
            this.manager = manager;
        }

        /**
         * Get the favorite layer from the Layer View Manager.
         */
        @Override
        protected IRaptorLayer getRowObject(int rowIndex)
        {
            return manager.getFavoriteLayer(rowIndex);
        }

        /**
         * Get the favorites layer count from the Layer View Manager.
         */
        @Override
        public int getRowCount()
        {
            return manager.getFavoriteLayersCount();
        }
    }
}
