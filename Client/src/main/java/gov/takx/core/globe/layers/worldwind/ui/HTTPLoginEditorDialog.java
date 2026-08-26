package gov.takx.core.globe.layers.worldwind.ui;

import com.jgoodies.forms.factories.Borders;
import com.jgoodies.forms.layout.CellConstraints;
import com.jgoodies.forms.layout.FormLayout;
import gov.takx.commons.swing.AlwaysOnTopJOptionPane;
import gov.takx.core.globe.layers.worldwind.HTTPLoginInfo;
import gov.takx.core.globe.layers.worldwind.HTTPPasswordManager;
import gov.takx.platform.ui.AStandardDialog;

import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.SwingConstants;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.TableColumn;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.util.ArrayList;
import java.util.List;

/**
 * @since 1.43
 */
public class HTTPLoginEditorDialog extends AStandardDialog
{
    private enum Column
    {
        URL("URL"), USERNAME("Username");

        private final String name;

        Column(String name)
        {
            this.name = name;
        }

        public String toString()
        {
            return name;
        }
    }

    private final TableModel tableModel;

    public HTTPLoginEditorDialog()
    {
        initComponents();
        tableModel = new TableModel();
        authTable.setModel(tableModel);
        DefaultTableCellRenderer centerRenderer = new DefaultTableCellRenderer();
        centerRenderer.setHorizontalAlignment(JLabel.CENTER);
        authTable.setDefaultRenderer(Object.class, centerRenderer);

        TableColumn column = authTable.getColumnModel().getColumn(Column.URL.ordinal());
        column.setPreferredWidth(200);
        column.setWidth(200);

        refresh();
    }

    private void okButtonActionPerformed()
    {
        dispose();
    }

    private void refresh()
    {
        tableModel.clear();
        for (HTTPLoginInfo loginInfo : HTTPPasswordManager.getInstance().getLoginInfo())
        {
            tableModel.addRow(loginInfo);
        }
    }

    private void removeButtonActionPerformed()
    {
        int ret = AlwaysOnTopJOptionPane.showConfirmDialog(this,
                "<html><center>Are you sure you want to <font color=red>remove the selected login(s)?</font><br>" +
                        "The changes take effect immediately and cannot be undone.<center></html>",
                "Confirm Authentication Removal",
                JOptionPane.OK_CANCEL_OPTION);

        if (ret == JOptionPane.OK_OPTION)
        {
            for (int row : authTable.getSelectedRows())
            {
                HTTPLoginInfo info = tableModel.getRow(row);
                HTTPPasswordManager.getInstance().removeAuthentication(info.url);
            }
            refresh();
        }
    }

    private void addButtonActionPerformed()
    {
        HTTPLoginDialog loginDialog = new HTTPLoginDialog(this);
        loginDialog.setSiteEditable(true);
        loginDialog.setVisible(true);
        refresh();
    }

    private void initComponents()
    {
        // JFormDesigner - Component initialization - DO NOT MODIFY  //GEN-BEGIN:initComponents
        var dialogPane = new JPanel();
        var contentPanel = new JPanel();
        var label1 = new JLabel();
        var scrollPane1 = new JScrollPane();
        authTable = new JTable();
        var buttonBar = new JPanel();
        var addButton = new JButton();
        var removeButton = new JButton();
        var closeButton = new JButton();
        CellConstraints cc = new CellConstraints();

        //======== this ========
        setTitle("Cached HTTP Authorizations");
        setName("this");
        var contentPane = getContentPane();
        contentPane.setLayout(new BorderLayout());

        //======== dialogPane ========
        {
            dialogPane.setBorder(Borders.DIALOG_BORDER);
            dialogPane.setName("dialogPane");
            dialogPane.setLayout(new BorderLayout());

            //======== contentPanel ========
            {
                contentPanel.setName("contentPanel");
                contentPanel.setLayout(new FormLayout(
                        "[pref,200dlu]:grow",
                        "default, $lgap, fill:default:grow"));

                //---- label1 ----
                label1.setText("<html><center><b>Cached HTTP Authorizations</b></center><br>Host, port and usernames are displayed, but passwords are not.  To add an authorization, click on <b>Add Login</b>.  To remove authorizations, select them on the table and click <b>Remove Selected</b>.</html>");
                label1.setHorizontalAlignment(SwingConstants.LEFT);
                label1.setName("label1");
                contentPanel.add(label1, cc.xy(1, 1));

                //======== scrollPane1 ========
                {
                    scrollPane1.setName("scrollPane1");

                    //---- authTable ----
                    authTable.setShowVerticalLines(false);
                    authTable.setPreferredScrollableViewportSize(new Dimension(200, 100));
                    authTable.setName("authTable");
                    scrollPane1.setViewportView(authTable);
                }
                contentPanel.add(scrollPane1, cc.xy(1, 3));
            }
            dialogPane.add(contentPanel, BorderLayout.CENTER);

            //======== buttonBar ========
            {
                buttonBar.setBorder(Borders.BUTTON_BAR_GAP_BORDER);
                buttonBar.setName("buttonBar");
                buttonBar.setLayout(new FormLayout(
                        "$button, $rgap, $button, $glue, $button",
                        "default"));

                //---- addButton ----
                addButton.setText("Add Login");
                addButton.setName("addButton");
                addButton.addActionListener(e -> addButtonActionPerformed());
                buttonBar.add(addButton, cc.xy(1, 1));

                //---- removeButton ----
                removeButton.setText("Remove Selected");
                removeButton.setName("removeButton");
                removeButton.addActionListener(e -> removeButtonActionPerformed());
                buttonBar.add(removeButton, cc.xy(3, 1));

                //---- closeButton ----
                closeButton.setText("Close");
                closeButton.setName("closeButton");
                closeButton.addActionListener(e -> okButtonActionPerformed());
                buttonBar.add(closeButton, cc.xy(5, 1, CellConstraints.DEFAULT, CellConstraints.FILL));
            }
            dialogPane.add(buttonBar, BorderLayout.SOUTH);
        }
        contentPane.add(dialogPane, BorderLayout.CENTER);
        pack();
        setLocationRelativeTo(getOwner());
        // JFormDesigner - End of component initialization  //GEN-END:initComponents
    }

    // JFormDesigner - Variables declaration - DO NOT MODIFY  //GEN-BEGIN:variables
    private JTable authTable;
    // JFormDesigner - End of variables declaration  //GEN-END:variables

    private static class TableModel extends AbstractTableModel
    {
        private final List<HTTPLoginInfo> loginInfo;

        public TableModel()
        {
            loginInfo = new ArrayList<>();
        }

        public void addRow(HTTPLoginInfo info)
        {
            loginInfo.add(info);
            fireTableDataChanged();
        }

        public HTTPLoginInfo getRow(int row)
        {
            return loginInfo.get(row);
        }

        @Override
        public String getColumnName(int columnIndex)
        {
            return Column.values()[columnIndex].toString();
        }

        @Override
        public int getRowCount()
        {
            return loginInfo.size();
        }

        @Override
        public int getColumnCount()
        {
            return Column.values().length;
        }

        @Override
        public Object getValueAt(int rowIndex, int columnIndex)
        {
            if (rowIndex < 0 || rowIndex >= loginInfo.size())
            {
                return null;
            }

            HTTPLoginInfo info = loginInfo.get(rowIndex);
            if (columnIndex == Column.URL.ordinal())
            {
                return info.url;
            } else
            {
                return info.username;
            }
        }

        public void clear()
        {
            loginInfo.clear();
            fireTableDataChanged();
        }
    }
}
