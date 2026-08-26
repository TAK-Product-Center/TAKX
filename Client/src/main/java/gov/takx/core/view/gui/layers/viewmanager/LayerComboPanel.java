package gov.takx.core.view.gui.layers.viewmanager;

import gov.takx.api.layer.IRaptorLayer;
import gov.takx.api.layer.LayerGroup;

import java.util.List;

/**
 */
public class LayerComboPanel extends LayerGroupPanel
{
    private final ComboTableModel comboTableModel;

    public LayerComboPanel(String comboGroupName,
                           List<LayerGroup> groupsList,
                           LayerViewPanel owner,
                           LayerViewManager manager)
    {
        super(comboGroupName, owner, manager, new ComboTableModel(comboGroupName, groupsList));
        comboTableModel = (ComboTableModel) tableModel;
    }

    @Override
    protected IRaptorLayer layerForRow(int row)
    {
        return row < 0 ? null : comboTableModel.getRowObject(row);
    }

    /**
     * Table Model for a combination of Layer Groups.  The groups and their order in the model are specified by a list
     * provided upon construction.
     */
    private static class ComboTableModel extends LayerTableModel
    {
        private final List<LayerGroup> groupsList;

        public ComboTableModel(String groupName, List<LayerGroup> groupsList)
        {
            super(groupName);
            this.groupsList = List.copyOf(groupsList);
        }

        /**
         * Iterate through the list of groups and sum their layer counts.
         */
        @Override
        public int getRowCount()
        {
            int count = 0;
            for (LayerGroup group : groupsList)
            {
                count += layerManager.getLayerCount(group);
            }
            return count;
        }

        /**
         * Get a layer for a row index.  Layers are ordered overall by the group list and locally by their position
         * within their group.
         */
        @Override
        public IRaptorLayer getRowObject(int rowIndex)
        {
            /**
             * Iterate through the groups, subtracting group size from the row index until the row index is within
             * the range of a given group.
             */
            for (LayerGroup group : groupsList)
            {
                int groupSize = layerManager.getLayerCount(group);
                if (groupSize > rowIndex && rowIndex >= 0)
                {
                    List<IRaptorLayer> layers = layerManager.getLayerList(group);
                    return layers.get(layers.size() - 1 - rowIndex);
                } else
                {
                    rowIndex -= groupSize;
                }
            }
            return null;
        }
    }
}
