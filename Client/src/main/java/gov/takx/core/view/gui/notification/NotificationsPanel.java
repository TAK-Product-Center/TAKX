package gov.takx.core.view.gui.notification;

import com.jgoodies.forms.factories.Borders;
import com.jgoodies.forms.layout.CellConstraints;
import com.jgoodies.forms.layout.FormLayout;
import gov.takx.api.annotation.InvokeOnEDT;
import gov.takx.api.toolbar.ButtonType;
import gov.takx.core.view.gui.ATitledPanel;
import jakarta.annotation.Nonnull;

import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.ScrollPaneConstants;
import javax.swing.SwingUtilities;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.DefaultTableCellRenderer;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.util.ArrayList;
import java.util.List;

/**
 * Displays a list of notifications in a table.
 *
 * @since 5.2
 */
public class NotificationsPanel extends ATitledPanel
{
    private final NotificationTableModel model = new NotificationTableModel();
    private final NotificationEntryRenderer renderer = new NotificationEntryRenderer();

    public NotificationsPanel()
    {
        super(ButtonType.NOTIFICATIONS.getDisplayText());

        initComponents();

        notificationTable.setTableHeader(null);
        notificationTable.setModel(model);
        notificationTable.getColumnModel().getColumn(0).setResizable(false);
        notificationTable.setDefaultRenderer(NotificationEntry.class, renderer);

        // Required to get JTextAreas to shrink
        addComponentListener(new ComponentAdapter()
        {
            @Override
            public void componentResized(ComponentEvent e)
            {
                resetNotificationPanel();
            }
        });
    }

    /**
     * @param entry The entry to add
     */
    @InvokeOnEDT
    void addEntry(@Nonnull NotificationEntry entry)
    {
        model.add(entry);
        revalidate();
    }

    /**
     * Resets the size of the panel used to display notifications.
     */
    private void resetNotificationPanel()
    {
        renderer.notificationPanel.resetSize();
        SwingUtilities.invokeLater(this::repaint); // delay to fix unpinned panel repaint issue
    }

    /**
     * Clear all notifications.
     */
    private void clear()
    {
        model.clear();
    }

    /**
     * Renders {@link NotificationEntry}s in a {@link NotificationPanel}.
     */
    private static class NotificationEntryRenderer extends DefaultTableCellRenderer
    {
        private final NotificationPanel notificationPanel = new NotificationPanel();

        @Override
        public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column)
        {
            final NotificationEntry notificationEntry = (NotificationEntry) value;
            notificationPanel.setNotification(notificationEntry.icon(), notificationEntry.message(), notificationEntry.instant());

            int preferredHeight = notificationPanel.getPreferredSize().height;
            if (table.getRowHeight(row) != preferredHeight) table.setRowHeight(row, preferredHeight);

            return notificationPanel;
        }
    }

    /**
     * The single-colum table model used to store {@link NotificationEntry}s.
     */
    private static class NotificationTableModel extends AbstractTableModel
    {
        private final List<NotificationEntry> messages = new ArrayList<>();

        @Override
        public int getRowCount()
        {
            return messages.size();
        }

        @Override
        public int getColumnCount()
        {
            return 1;
        }

        @Override
        public Class<?> getColumnClass(int columnIndex)
        {
            return NotificationEntry.class;
        }

        @Override
        public Object getValueAt(int rowIndex, int columnIndex)
        {
            return messages.get(rowIndex);
        }

        /**
         * @param entry The entry to add
         */
        public void add(@Nonnull NotificationEntry entry)
        {
            messages.add(0, entry); // Add to top of table
            fireTableRowsInserted(0, 0);
        }

        /**
         * Removes all data from the table.
         */
        public void clear()
        {
            messages.clear();
            fireTableDataChanged();
        }
    }

    private void initComponents()
    {
        // JFormDesigner - Component initialization - DO NOT MODIFY  //GEN-BEGIN:initComponents  @formatter:off
        var headerPanel = new JPanel();
        var headerLabel = new JLabel();
        var clearButton = new JButton();
        var notificationScrollPane = new JScrollPane();
        notificationTable = new JTable();
        CellConstraints cc = new CellConstraints();

        //======== this ========
        setName("this");
        setLayout(new BorderLayout());

        //======== headerPanel ========
        {
            headerPanel.setBorder(Borders.DLU4_BORDER);
            headerPanel.setName("headerPanel");
            headerPanel.setLayout(new FormLayout(
                    "default:grow, $lcgap, default",
                    "default"));

            //---- headerLabel ----
            headerLabel.setText("Timeline");
            headerLabel.setName("headerLabel");
            headerPanel.add(headerLabel, cc.xy(1, 1));

            //---- clearButton ----
            clearButton.setText("Clear all");
            clearButton.setName("clearButton");
            clearButton.addActionListener(e -> clear());
            headerPanel.add(clearButton, cc.xy(3, 1));
        }
        add(headerPanel, BorderLayout.NORTH);

        //======== notificationScrollPane ========
        {
            notificationScrollPane.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
            notificationScrollPane.setName("notificationScrollPane");

            //---- notificationTable ----
            notificationTable.setRowSelectionAllowed(false);
            notificationTable.setName("notificationTable");
            notificationScrollPane.setViewportView(notificationTable);
        }
        add(notificationScrollPane, BorderLayout.CENTER);
        // JFormDesigner - End of component initialization  //GEN-END:initComponents  @formatter:on
    }

    // JFormDesigner - Variables declaration - DO NOT MODIFY  //GEN-BEGIN:variables  @formatter:off
    private JTable notificationTable;
    // JFormDesigner - End of variables declaration  //GEN-END:variables  @formatter:on
}
