package gov.takx.core.view.gui.maps.exporters.dataset;

import jakarta.annotation.Nonnull;
import org.jdesktop.swingx.JXTable;

import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.ListSelectionModel;
import javax.swing.RowFilter;

import java.awt.BorderLayout;
import java.awt.Dimension;

/**
 * A list of elevation or imagery layers with a custom model and rendering.
 *
 * @since 1.43 (renamed in 5.7)
 */
public class LayerManagementTable extends JPanel
{
    private final LayerTableModel model = new LayerTableModel();
    private final LayerTableFilter filter = new LayerTableFilter();

    public LayerManagementTable()
    {
        initComponents();
        layerTable.setModel(model);

        final var column = layerTable.getColumn(0);
        column.setCellEditor(new LayerManagementEditor());
        column.setCellRenderer(new LayerManagementRenderer());
    }

    @Override
    public void doLayout()
    {
        layerTable.setRowFilter(null);

        super.doLayout();
        layerTable.packAll();
        adjustRows();

        layerTable.setRowFilter(filter);
    }

    private void adjustRows()
    {
        if (model.getRowCount() > 0)
        {
            final var rowComponent = layerTable.prepareRenderer(layerTable.getCellRenderer(0, 0), 0, 0);
            layerTable.setRowHeight(rowComponent.getPreferredSize().height);
        }
    }

    /**
     * @return The table's model
     */
    @Nonnull
    public LayerTableModel getModel()
    {
        return model;
    }

    private static class LayerTableFilter extends RowFilter<LayerTableModel, Integer>
    {
        @Override
        public boolean include(Entry<? extends LayerTableModel, ? extends Integer> entry)
        {
            var wrapper = entry.getModel().getRow(entry.getIdentifier());
            Long totalTileCount = wrapper.getTotalTileCount();
            return totalTileCount != null && totalTileCount > 0;
        }
    }

    private void initComponents()
    {
        // JFormDesigner - Component initialization - DO NOT MODIFY  //GEN-BEGIN:initComponents
        scrollPane = new JScrollPane();
        layerTable = new JXTable();

        //======== this ========
        setName("this");
        setLayout(new BorderLayout());

        //======== scrollPane ========
        {
            scrollPane.setName("scrollPane");

            //---- layerTable ----
            layerTable.setShowVerticalLines(false);
            layerTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
            layerTable.setRowSelectionAllowed(false);
            layerTable.setPreferredScrollableViewportSize(new Dimension(100, 100));
            layerTable.setShowHorizontalLines(false);
            layerTable.setName("layerTable");
            layerTable.setTableHeader(null);
            scrollPane.setViewportView(layerTable);
        }
        add(scrollPane, BorderLayout.CENTER);
        // JFormDesigner - End of component initialization  //GEN-END:initComponents
    }

    // JFormDesigner - Variables declaration - DO NOT MODIFY  //GEN-BEGIN:variables
    private JScrollPane scrollPane;
    private JXTable layerTable;
    // JFormDesigner - End of variables declaration  //GEN-END:variables
}
