package gov.takx.core.view.gui.metadata;

import com.jgoodies.forms.layout.CellConstraints;
import com.jgoodies.forms.layout.FormLayout;
import gov.tak.api.util.AttributeSet;
import gov.takx.api.annotation.InvokeOnEDT;

import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.ListSelectionModel;
import javax.swing.border.TitledBorder;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableColumn;

import java.awt.Dimension;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * UI Panel to display and allow editing of shape/structure metadata.
 *
 * @since 3.3 (nearly a complete rewrite)
 */
public class MetadataTablePanel extends JPanel
{
    private List<String> metadataOfInterest = new ArrayList<>();
    private AttributeSet originalMetadata = new AttributeSet();
    private AttributeSet metadata = new AttributeSet();
    private final PropertyChangeListener changeListener;
    private DefaultTableModel tableModel = new MetadataTableModel();

    public MetadataTablePanel(PropertyChangeListener changeListener)
    {
        this.changeListener = changeListener;

        initComponents();

        metadataTable.setModel(tableModel);
        metadataTable.setTableHeader(null);

        final TableColumn keyColumn = metadataTable.getColumnModel().getColumn(0);
        keyColumn.setPreferredWidth(80);
        keyColumn.setMaxWidth(200);
        keyColumn.setWidth(80);
    }

    public void setMetadata(AttributeSet metadata)
    {
        originalMetadata = metadata == null ? new AttributeSet() : new AttributeSet(metadata);
        this.metadata = metadata == null ? new AttributeSet() : new AttributeSet(metadata);
        rebuildTableModel();
    }

    public void setMetadataOfInterest(List<String> metadataOfInterest)
    {
        this.metadataOfInterest = new ArrayList<>(metadataOfInterest);
        rebuildTableModel();
    }

    public void setEditable(boolean editable)
    {
        metadataTable.setEnabled(editable);
    }

    /**
     * @return Original metadata, as last set by a call to {@link #setMetadata}
     */
    public AttributeSet getOriginalMetadata()
    {
        return originalMetadata;
    }

    /**
     * @return Current metadata
     */
    public AttributeSet getMetadata()
    {
        return metadata;
    }

    /**
     * @return Set of the metadata keys that had their values changed
     */
    public Set<String> getChangedMetadataKeys()
    {
        return metadataOfInterest.stream()
                .filter(key -> {
                    final boolean originalHasValue = originalMetadata.containsAttribute(key);
                    final boolean currentHasValue = metadata.containsAttribute(key);
                    return originalHasValue != currentHasValue
                            || (originalHasValue && !originalMetadata.getStringAttribute(key).equals(metadata.getStringAttribute(key)));
                })
                .collect(Collectors.toSet());
    }

    /**
     * Commit any active edit within the table.
     */
    public void commit()
    {
        if (metadataTable.isEditing()) metadataTable.getCellEditor().stopCellEditing();
    }

    @InvokeOnEDT
    private void rebuildTableModel()
    {
        tableModel = new MetadataTableModel();

        for (String key : metadataOfInterest)
        {
            tableModel.addRow(new Object[]{key, metadata.getStringAttribute(key, "")});
        }

        metadataTable.setModel(tableModel);
    }

    private class MetadataTableModel extends DefaultTableModel
    {
        public MetadataTableModel()
        {
            super(new Object[]{"Key", "Value"}, 0);
        }

        @Override
        public Class<?> getColumnClass(int column)
        {
            return String.class;
        }

        @Override
        public boolean isCellEditable(int row, int col)
        {
            return col == 1; // value is editable
        }

        @Override
        public void setValueAt(Object value, int row, int column)
        {
            super.setValueAt(value, row, column);

            final String key = (String) getValueAt(row, 0);
            final String oldValue = metadata.getStringAttribute(key, "");
            final String newValue = (String) value;

            metadata.setAttribute(key, newValue);

            changeListener.propertyChange(new PropertyChangeEvent(this, key, oldValue, newValue));
        }
    }

    private void initComponents()
    {
        // JFormDesigner - Component initialization - DO NOT MODIFY  //GEN-BEGIN:initComponents
        var metadataPane = new JScrollPane();
        metadataTable = new JTable();
        CellConstraints cc = new CellConstraints();

        //======== this ========
        setName("panelMetadataTable");
        setBorder(new TitledBorder("Metadata"));
        setLayout(new FormLayout(
                "default:grow",
                "default"));

        //======== metadataPane ========
        {
            metadataPane.setName("metadataPane");

            //---- metadataTable ----
            metadataTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
            metadataTable.setEnabled(false);
            metadataTable.setPreferredScrollableViewportSize(new Dimension(200, 100));
            metadataTable.setName("metadataTable");
            metadataPane.setViewportView(metadataTable);
        }
        add(metadataPane, cc.xy(1, 1));
        // JFormDesigner - End of component initialization  //GEN-END:initComponents
    }

    // JFormDesigner - Variables declaration - DO NOT MODIFY  //GEN-BEGIN:variables
    private JTable metadataTable;
    // JFormDesigner - End of variables declaration  //GEN-END:variables
}
