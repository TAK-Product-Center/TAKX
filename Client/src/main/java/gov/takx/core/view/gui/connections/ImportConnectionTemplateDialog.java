package gov.takx.core.view.gui.connections;

import com.jgoodies.forms.factories.Borders;
import com.jgoodies.forms.layout.CellConstraints;
import com.jgoodies.forms.layout.FormLayout;
import gov.takx.api.annotation.PseudoBean;
import gov.takx.api.plugin.connection.IConnectionPlugin;
import gov.takx.api.registry.IConnectionPluginRegistry;
import gov.takx.core.view.gui.DialogHeaderPanel;
import gov.takx.platform.schema.connection.ConnectionDefinitionType;
import gov.takx.platform.schema.connection.ConnectionDefinitionsType;
import gov.takx.platform.schema.connection.ObjectFactory;
import gov.takx.platform.ui.AStandardDialog;
import jakarta.annotation.PostConstruct;
import jakarta.inject.Inject;
import org.jdesktop.swingx.JXTable;

import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.ListSelectionModel;
import javax.swing.SwingConstants;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.TableColumn;
import javax.swing.table.TableColumnModel;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dialog;
import java.awt.Dimension;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Dialog that let's the operator choose which connections from a template file to actually import.  There is an
 * additional column to choose whether or not to have the connection auto-start when it is imported (as opposed to
 * just adding it to the panel and requiring a manual re-connect).
 *
 */
@SuppressWarnings("CdiManagedBeanInconsistencyInspection")
@PseudoBean
public class ImportConnectionTemplateDialog extends AStandardDialog
{
    @Inject
    private IConnectionPluginRegistry connectionPluginRegistry;

    private final ConnectionTableModel model;
    private final ConnectionDefinitionsType definitions;
    private final Map<String, String> typeToNameMap = new HashMap<>();

    private boolean okay = false;

    private static final String DESCRIPTION = "Select form the list of importable connections.  Note there are " +
            "check-boxes for both selecting the connection to import and for choosing to have the connection " + "" +
            "auto-connect on import.";

    public enum ColumnNames
    {
        IMPORT("Import"), TYPE("Type"), NAME("Name"), AUTOSTART("Auto Start");

        private final String name;

        ColumnNames(String name)
        {
            this.name = name;
        }

        @Override
        public String toString()
        {
            return name;
        }
    }

    public ImportConnectionTemplateDialog(ConnectionDefinitionsType connections)
    {
        initComponents();

        dialogHeaderPanel.setDescription(DESCRIPTION);
        dialogHeaderPanel.setIcon("connection.png");

        model = new ConnectionTableModel(connections.getConnectionDefinition());
        connectionTable.setModel(model);

        final TableColumnModel columnModel = connectionTable.getColumnModel();

        // Set a smaller width on the two checkbox columns
        CheckBoxCellRenderer checkBoxCellRenderer = new CheckBoxCellRenderer();
        checkBoxCellRenderer.setHorizontalAlignment(SwingConstants.CENTER);
        TableColumn checkColumn = columnModel.getColumn(ColumnNames.IMPORT.ordinal());
        checkColumn.setPreferredWidth(50);
        checkColumn.setMinWidth(50);
        checkColumn.setMaxWidth(50);
        checkColumn.setCellRenderer(checkBoxCellRenderer);
        checkColumn = columnModel.getColumn(ColumnNames.AUTOSTART.ordinal());
        checkColumn.setPreferredWidth(75);
        checkColumn.setMinWidth(75);
        checkColumn.setMaxWidth(75);
        checkColumn.setCellRenderer(checkBoxCellRenderer);

        // Name should be the largest...
        checkColumn = columnModel.getColumn(ColumnNames.NAME.ordinal());
        checkColumn.setPreferredWidth(200);
        checkColumn.setMinWidth(200);

        definitions = connections;
    }

    @PostConstruct
    private void postConstruct()
    {
        for (ConnectionDefinitionType type : definitions.getConnectionDefinition())
        {
            IConnectionPlugin plugin = connectionPluginRegistry.getPlugin(type.getFamily(), type.getType());
            typeToNameMap.put(plugin.getType(), plugin.getName());
        }
    }

    public ConnectionDefinitionsType getDefinitions()
    {
        ObjectFactory factory = new ObjectFactory();
        ConnectionDefinitionsType definitionsType = factory.createConnectionDefinitionsType();
        int rows = model.getRowCount();
        for (int i = 0; i < rows; i++)
        {
            boolean selected = (Boolean) model.getValueAt(i, ColumnNames.IMPORT.ordinal());
            if (selected)
            {
                boolean autostart = (Boolean) model.getValueAt(i, ColumnNames.AUTOSTART.ordinal());
                ConnectionDefinitionType definition = model.dataVector.get(i);
                definition.setAutoConnect(autostart);
                definitionsType.getConnectionDefinition().add(definition);
            }
        }
        return definitionsType;
    }

    public boolean isOkay()
    {
        return okay;
    }

    private void okButtonActionPerformed()
    {
        okay = true;
        dispose();
    }

    private void cancelButtonActionPerformed()
    {
        dispose();
    }

    private class ConnectionTableModel extends AbstractTableModel
    {
        private final List<ConnectionDefinitionType> dataVector;
        private final List<Integer> selectedList = new ArrayList<>();

        ConnectionTableModel(List<ConnectionDefinitionType> list)
        {
            dataVector = list;
            for (int i = 0; i < dataVector.size(); i++)
            {
                selectedList.add(i);
            }
        }

        @Override
        public int getRowCount()
        {
            return dataVector != null ? dataVector.size() : 0;
        }

        @Override
        public int getColumnCount()
        {
            return ColumnNames.values().length;
        }

        @Override
        public String getColumnName(int column)
        {
            return ColumnNames.values()[column].toString();
        }

        @Override
        public Class<?> getColumnClass(int columnIndex)
        {
            return switch (ColumnNames.values()[columnIndex])
            {
                case IMPORT, AUTOSTART -> Boolean.class;
                default -> String.class;
            };
        }

        @Override
        public boolean isCellEditable(int row, int col)
        {
            return switch (ColumnNames.values()[col])
            {
                case IMPORT, AUTOSTART -> true;
                default -> false;
            };
        }

        @Override
        public Object getValueAt(int rowIndex, int columnIndex)
        {
            if (dataVector != null && dataVector.size() > rowIndex)
            {
                ConnectionDefinitionType connection = dataVector.get(rowIndex);
                return switch (ColumnNames.values()[columnIndex])
                {
                    case IMPORT -> selectedList.contains(rowIndex);
                    case TYPE -> typeToNameMap.get(connection.getType());
                    case NAME -> connection.getName();
                    case AUTOSTART -> connection.isAutoConnect();
                };
            }
            return "";
        }

        @Override
        public void setValueAt(Object aValue, int rowIndex, int columnIndex)
        {
            if (dataVector != null && dataVector.size() > rowIndex)
            {
                ConnectionDefinitionType connection = dataVector.get(rowIndex);
                switch (ColumnNames.values()[columnIndex])
                {
                    case TYPE:
                    case NAME:
                        break;
                    case AUTOSTART:
                        connection.setAutoConnect((Boolean) aValue);
                        break;
                    case IMPORT:
                        if ((Boolean) aValue && !selectedList.contains(rowIndex))
                        {
                            selectedList.add(rowIndex);
                        } else if (!(Boolean) aValue && selectedList.contains(rowIndex))
                        {
                            selectedList.remove((Integer) rowIndex);
                        }
                        break;
                }
            }
        }
    }

    /**
     * Render a boolean as a checkbox.
     *
     * @since 3.0
     */
    private static class CheckBoxCellRenderer extends DefaultTableCellRenderer
    {
        private final JCheckBox checkedCheckBox = new JCheckBox();
        private final JCheckBox unCheckedCheckBox = new JCheckBox();

        public CheckBoxCellRenderer()
        {
            checkedCheckBox.setSelected(true);
        }

        @Override
        public void setHorizontalAlignment(int alignment)
        {
            super.setHorizontalAlignment(alignment);
            if (checkedCheckBox != null)
            {
                checkedCheckBox.setHorizontalAlignment(alignment);
                unCheckedCheckBox.setHorizontalAlignment(alignment);
            }
        }

        @Override
        public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column)
        {
            if (value == null)
            {
                return unCheckedCheckBox;
            } else if ((Boolean) value)
            {
                return checkedCheckBox;
            } else
            {
                return unCheckedCheckBox;
            }
        }
    }

    private void initComponents()
    {
        // JFormDesigner - Component initialization - DO NOT MODIFY  //GEN-BEGIN:initComponents
        dialogPane = new JPanel();
        dialogHeaderPanel = new DialogHeaderPanel();
        contentPanel = new JPanel();
        scrollPane = new JScrollPane();
        connectionTable = new JXTable();
        buttonBar = new JPanel();
        okButton = new JButton();
        cancelButton = new JButton();
        CellConstraints cc = new CellConstraints();

        //======== this ========
        setModalityType(Dialog.ModalityType.APPLICATION_MODAL);
        setTitle("Select Connections to Import");
        setName("this");
        var contentPane = getContentPane();
        contentPane.setLayout(new BorderLayout());

        //======== dialogPane ========
        {
            dialogPane.setBorder(Borders.DIALOG_BORDER);
            dialogPane.setPreferredSize(new Dimension(500, 400));
            dialogPane.setName("dialogPane");
            dialogPane.setLayout(new BorderLayout());

            //---- dialogHeaderPanel ----
            dialogHeaderPanel.setName("dialogHeaderPanel");
            dialogPane.add(dialogHeaderPanel, BorderLayout.NORTH);

            //======== contentPanel ========
            {
                contentPanel.setName("contentPanel");
                contentPanel.setLayout(new FormLayout(
                        "default:grow",
                        "fill:default:grow"));

                //======== scrollPane ========
                {
                    scrollPane.setName("scrollPane");

                    //---- connectionTable ----
                    connectionTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
                    connectionTable.setPreferredScrollableViewportSize(new Dimension(200, 100));
                    connectionTable.setName("connectionTable");
                    scrollPane.setViewportView(connectionTable);
                }
                contentPanel.add(scrollPane, cc.xy(1, 1));
            }
            dialogPane.add(contentPanel, BorderLayout.CENTER);

            //======== buttonBar ========
            {
                buttonBar.setBorder(Borders.BUTTON_BAR_GAP_BORDER);
                buttonBar.setName("buttonBar");
                buttonBar.setLayout(new FormLayout(
                        "$glue, $button, $rgap, $button",
                        "pref"));

                //---- okButton ----
                okButton.setText("OK");
                okButton.setName("okButton");
                okButton.addActionListener(e -> okButtonActionPerformed());
                buttonBar.add(okButton, cc.xy(2, 1));

                //---- cancelButton ----
                cancelButton.setText("Cancel");
                cancelButton.setName("cancelButton");
                cancelButton.addActionListener(e -> cancelButtonActionPerformed());
                buttonBar.add(cancelButton, cc.xy(4, 1));
            }
            dialogPane.add(buttonBar, BorderLayout.SOUTH);
        }
        contentPane.add(dialogPane, BorderLayout.CENTER);
        pack();
        setLocationRelativeTo(getOwner());
        // JFormDesigner - End of component initialization  //GEN-END:initComponents
    }

    // JFormDesigner - Variables declaration - DO NOT MODIFY  //GEN-BEGIN:variables
    private JPanel dialogPane;
    private DialogHeaderPanel dialogHeaderPanel;
    private JPanel contentPanel;
    private JScrollPane scrollPane;
    private JXTable connectionTable;
    private JPanel buttonBar;
    private JButton okButton;
    private JButton cancelButton;
    // JFormDesigner - End of variables declaration  //GEN-END:variables
}
