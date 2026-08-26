package gov.takx.core.playback.globe;

import com.jgoodies.forms.layout.CellConstraints;
import com.jgoodies.forms.layout.FormLayout;
import gov.takx.api.mapobject.IMapObject;
import gov.takx.api.playback.globe.IGlobeModel;
import gov.takx.commons.swing.SelectAllTableSupport;

import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;
import javax.swing.border.TitledBorder;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.TableColumnModel;

import java.awt.Component;
import java.awt.Dimension;
import java.util.Collection;
import java.util.List;

/**
 * UI to allow the user to select which Map Objects are to be included in the globe playback lane and to indicate
 * which one, if any, should be "centered on" during playback.
 *
 * @since 1.47
 */
public class GlobeEditorPanel extends JPanel
{
    private final GlobeMapObjectTableModel tableModel;

    public GlobeEditorPanel(IGlobeModel globeModel, Collection<IMapObject> currentMapObjects)
    {
        initComponents();

        tableModel = new GlobeMapObjectTableModel(globeModel, currentMapObjects);
        globeMOTable.setModel(tableModel);

        TableColumnModel columnModel = globeMOTable.getColumnModel();

        final int selectedOrdinal = GlobeMapObjectTableModel.Columns.SELECTED.ordinal();
        SelectAllTableSupport.configureSelectAllColumn(globeMOTable, tableModel, selectedOrdinal, false, 50);

        columnModel.getColumn(GlobeMapObjectTableModel.Columns.NAME.ordinal()).setCellRenderer(new IconNameRenderer());
    }

    public List<IMapObject> getSelectedMapObjects()
    {
        return tableModel.getSelectedMapObjects();
    }

    private void filterFieldCaretUpdate()
    {
        tableModel.updateVisibleVector(filterField.getText());
    }

    private void clearFilterButtonActionPerformed()
    {
        filterField.setText("");
    }

    /**
     * Simple cell renderer to show the MO icon and name.
     */
    private class IconNameRenderer extends DefaultTableCellRenderer
    {
        @Override
        public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected,
                                                       boolean hasFocus, int row, int column)
        {
            if (value != null)
            {
                setText(value.toString());

                final ImageIcon image = tableModel.getMapObjectIcon(row);
                if (image != null)
                {
                    setIcon(image);
                }
            }
            return this;
        }
    }

    private void initComponents()
    {
        // JFormDesigner - Component initialization - DO NOT MODIFY  //GEN-BEGIN:initComponents
        var editorPanel = new JPanel();
        var scrollPane1 = new JScrollPane();
        globeMOTable = new JTable();
        var filterPanel = new JPanel();
        var filterLabel = new JLabel();
        filterField = new JTextField();
        var clearFilterButton = new JButton();
        CellConstraints cc = new CellConstraints();

        //======== this ========
        setBorder(new TitledBorder("Globe Map Objects"));
        setName("this");
        setLayout(new FormLayout(
                "default:grow",
                "fill:default:grow, $lgap, default"));

        //======== editorPanel ========
        {
            editorPanel.setName("editorPanel");
            editorPanel.setLayout(new FormLayout(
                    "default:grow",
                    "default, $lgap, fill:default:grow, $lgap, default"));

            //======== scrollPane1 ========
            {
                scrollPane1.setName("scrollPane1");

                //---- globeMOTable ----
                globeMOTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
                globeMOTable.setPreferredScrollableViewportSize(new Dimension(200, 100));
                globeMOTable.setName("globeMOTable");
                scrollPane1.setViewportView(globeMOTable);
            }
            editorPanel.add(scrollPane1, cc.xy(1, 3));

            //======== filterPanel ========
            {
                filterPanel.setBorder(new TitledBorder("Map Object List Filter"));
                filterPanel.setName("filterPanel");
                filterPanel.setLayout(new FormLayout(
                        "default, $lcgap, default:grow, $lcgap, default",
                        "default"));

                //---- filterLabel ----
                filterLabel.setText("Filter Text: ");
                filterLabel.setName("filterLabel");
                filterPanel.add(filterLabel, cc.xy(1, 1));

                //---- filterField ----
                filterField.setName("filterField");
                filterField.addCaretListener(e -> filterFieldCaretUpdate());
                filterPanel.add(filterField, cc.xy(3, 1));

                //---- clearFilterButton ----
                clearFilterButton.setText("Clear Filter");
                clearFilterButton.setName("clearFilterButton");
                clearFilterButton.addActionListener(e -> clearFilterButtonActionPerformed());
                filterPanel.add(clearFilterButton, cc.xy(5, 1));
            }
            editorPanel.add(filterPanel, cc.xy(1, 5));
        }
        add(editorPanel, cc.xy(1, 1));
        // JFormDesigner - End of component initialization  //GEN-END:initComponents
    }

    // JFormDesigner - Variables declaration - DO NOT MODIFY  //GEN-BEGIN:variables
    private JTable globeMOTable;
    private JTextField filterField;
    // JFormDesigner - End of variables declaration  //GEN-END:variables
}
