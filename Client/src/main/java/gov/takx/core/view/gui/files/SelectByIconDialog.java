package gov.takx.core.view.gui.files;

import com.jgoodies.forms.factories.Borders;
import com.jgoodies.forms.layout.CellConstraints;
import com.jgoodies.forms.layout.FormLayout;
import gov.takx.api.plugin.io.IImportWizard;
import gov.takx.commons.swing.ImageUtils;
import gov.takx.core.view.gui.GUIUtils;
import gov.takx.platform.ui.AStandardDialog;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import javax.swing.Icon;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JRootPane;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.ListSelectionModel;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.event.ListSelectionEvent;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Frame;
import java.awt.Point;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;
import java.net.URL;
import java.util.Arrays;

/**
 * Dialog that allows a user to make a selection by choosing an icon/text pair.
 *
 * @since 1.35
 */
public class SelectByIconDialog extends AStandardDialog
{
    /**
     * The size of the icons used in this dialog. They are square, so only one size is required.
     */
    private static final int ICON_SIZE = ImageUtils.FEATURE_ICON_HEIGHT;

    /**
     * Selection types. {@link #once} indicates that the user would like to use the selection this time only.
     * {@link #always} indicates that the user would like the system to remember the choice and take the same action
     * next time instead of presenting the available options.
     */
    public enum SelectionType
    {
        once,
        always,
        all
    }

    private final IconsTableModel tableModel = new IconsTableModel();
    private IconEntry result = null; // The user's selection. May be null if the user cancelled the dialog.
    private SelectionType selectionType = SelectionType.once;
    private final boolean enableAllOption; // Indicates if the "All" option is allowed.
    private final boolean enableAlwaysOption; // Indicates if the "Always" option is allowed.

    /**
     * Constructor.
     *
     * @param owner              The dialog's owner.
     * @param title              The dialog title.
     * @param enableAllOption    Pass {@code true} to allow the "All" option
     * @param enableAlwaysOption Pass {@code true} to allow the "Always" option
     */
    public SelectByIconDialog(Frame owner, String title, boolean enableAllOption, boolean enableAlwaysOption)
    {
        super(owner);
        getRootPane().setWindowDecorationStyle(JRootPane.NONE);

        initComponents();

        setTitle(title);

        this.enableAllOption = enableAllOption;
        this.enableAlwaysOption = enableAlwaysOption;

        tableScrollPane.setColumnHeaderView(null);

        tableIconEntries.setModel(tableModel);
        tableIconEntries.setDefaultRenderer(Object.class, new IconTableRenderer());
        tableIconEntries.setTableHeader(null);
        tableIconEntries.getSelectionModel().addListSelectionListener(this::checkValidSelection);
        tableIconEntries.getColumnModel().getSelectionModel().addListSelectionListener(this::checkValidSelection);
        tableIconEntries.addMouseListener(new TooltipMouseAdapter(tableIconEntries));
    }

    /**
     * Add an entry to the list.
     *
     * @param uniqueId      A unique identifier that can be used to determine which entry the user selected.
     * @param displayName   The text to display.
     * @param helpMessage   The contextual help message.
     * @param imageURL      The URL for the image to display.
     * @param wizard        The import wizard
     */
    public void addEntry(String uniqueId, String displayName, String helpMessage, URL imageURL, IImportWizard wizard)
    {
        if (imageURL == null) imageURL = ImageUtils.DEFAULT_IMAGE_URL;
        BufferedImage scaledImage = ImageUtils.getScaledImage(imageURL, ICON_SIZE, ICON_SIZE, true);

        //noinspection DataFlowIssue - scaledImge can't be null, the default icon will be returned on any error
        final Icon icon = ImageUtils.getSquareIcon(scaledImage, ICON_SIZE);
        final String tooltip = MultiLineLabel.addLineBreaks(helpMessage, 40, "#303030");
        final IconEntry entry = new IconEntry(uniqueId, displayName, tooltip, imageURL, icon, wizard);

        tableModel.addEntry(entry);

        GUIUtils.updateRowHeights(tableIconEntries, 5);
    }

    /**
     * Check to see if a cell is selected. Enable or disable the buttons accordingly.
     */
    private void checkValidSelection(ListSelectionEvent e)
    {
        if (e.getValueIsAdjusting()) return;

        final int selectedRow = tableIconEntries.getSelectedRow();
        final int selectedColumn = tableIconEntries.getSelectedColumn();
        final boolean haveSelection = selectedRow >= 0 && selectedColumn >= 0;

        buttonAlways.setEnabled(haveSelection && enableAlwaysOption);
        buttonOnce.setEnabled(haveSelection);
        buttonAll.setEnabled(haveSelection && enableAllOption);
    }

    /**
     * Respond to user selecting an option with the "Always" button.
     */
    private void buttonAlwaysActionPerformed()
    {
        handleSelection(SelectionType.always);
    }

    /**
     * Respond to user selecting an option with the "Just Once" button.
     */
    private void buttonOnceActionPerformed()
    {
        handleSelection(SelectionType.once);
    }

    private void buttonAllActionPerformed()
    {
        handleSelection(SelectionType.all);
    }

    /**
     * Handle the user's selection.
     *
     * @param selectionType The selection type.
     */
    private void handleSelection(final SelectionType selectionType)
    {
        dispose();

        if (SelectionType.always == selectionType)
        {
            JOptionPane.showMessageDialog(
                    JFrame.getFrames()[0],
                    "You can clear this selection by going to:\n" +
                            "Settings > File & Directory Preferences > File Associations",
                    "Always Option",
                    JOptionPane.INFORMATION_MESSAGE);
        }

        this.selectionType = selectionType;

        final int selectedRow = tableIconEntries.getSelectedRow();
        final int selectedColumn = tableIconEntries.getSelectedColumn();

        if (selectedRow >= 0 && selectedColumn >= 0) result = tableModel.getValueAt(selectedRow, selectedColumn);
    }

    /**
     * @return The user's selection. May be {@code null} if the user cancelled the dialog.
     */
    @Nullable
    public IconEntry getResult()
    {
        return result;
    }

    /**
     * @return The selection type chosen by the user
     * @since 5.1
     */
    @Nonnull
    public SelectionType getSelectionType()
    {
        return selectionType;
    }

    /**
     * The icon and name to display to the user in the list of options.
     *
     * @param uniqueId      A unique identifier that can be used to determine which entry the user selected.
     * @param displayName   The text that should be displayed with the entry.
     * @param tooltip       Component to use as a tooltip
     * @param iconURL       URL of icon to display
     * @param icon          Icon to display for the entry
     * @param wizard        The import wizard
     * @since 5.2 converted to record
     */
    public record IconEntry(String uniqueId, String displayName, String tooltip, URL iconURL, Icon icon, IImportWizard wizard)
    {
    }

    /**
     * Specialized table model for holding our icon entries.
     * @since 5.1
     */
    private static class IconsTableModel extends DefaultTableModel
    {
        private static final int ENTRIES_PER_ROW = 3;
        private static final String[] HEADER = new String[ENTRIES_PER_ROW];

        private int entryCount = 0;

        static
        {
            Arrays.fill(HEADER, "Entry");
        }

        IconsTableModel()
        {
            super(HEADER, 0);
        }

        @Override
        public boolean isCellEditable(int row, int col)
        {
            return false;
        }

        void addEntry(IconEntry entry)
        {
            final int row = entryCount / ENTRIES_PER_ROW;
            final int col = entryCount % ENTRIES_PER_ROW;

            if (row >= getRowCount()) addRow(new Object[ENTRIES_PER_ROW]);

            setValueAt(entry, row, col);

            entryCount++;
        }

        @Override
        public IconEntry getValueAt(int row, int column)
        {
            return (IconEntry) super.getValueAt(row, column);
        }
    }

    /**
     * Custom renderer for {@link IconEntry} objects. Displays the text centered beneath the icon.
     */
    private static class IconTableRenderer extends DefaultTableCellRenderer
    {
        IconTableRenderer()
        {
            setHorizontalAlignment(JLabel.CENTER);
            setHorizontalTextPosition(JLabel.CENTER);
            setVerticalTextPosition(JLabel.BOTTOM);
        }

        @Override
        public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column)
        {
            // Allow super to handle the foreground/background colors based on the selected/focused state
            super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);

            if (value != null)
            {
                IconEntry entry = (IconEntry) value;
                setIcon(entry.icon);
                setText(entry.displayName);
                setToolTipText(entry.tooltip);
            } else
            {
                setIcon(null);
                setText(null);
                setToolTipText(null);
            }

            return this;
        }
    }

    /**
     * Mouse adapter to handle double-click actions on our table entries.
     */
    private class TooltipMouseAdapter extends MouseAdapter
    {
        private final JTable table;

        /**
         * @param table The table that this adapter will be providing tooltips for.
         */
        TooltipMouseAdapter(JTable table)
        {
            this.table = table;
        }

        @Override
        public void mouseClicked(MouseEvent e)
        {
            // User clicked table. Double-click indicates quick selection of the double-clicked importer.
            if (e.getSource() == table && SwingUtilities.isLeftMouseButton(e) && e.getClickCount() == 2)
            {
                final Point point = e.getPoint();
                final int row = tableIconEntries.rowAtPoint(point);
                final int col = tableIconEntries.columnAtPoint(point);

                if (row >= 0 && col >= 0 && tableModel.getValueAt(row, col) != null) handleSelection(SelectionType.once);
            }
        }
    }

    private void initComponents()
    {
        // JFormDesigner - Component initialization - DO NOT MODIFY  //GEN-BEGIN:initComponents
        var dialogPane = new JPanel();
        var contentPanel = new JPanel();
        var instructionsLabel = new JLabel();
        tableScrollPane = new JScrollPane();
        tableIconEntries = new JTable();
        var buttonBar = new JPanel();
        buttonOnce = new JButton();
        buttonAll = new JButton();
        buttonAlways = new JButton();
        CellConstraints cc = new CellConstraints();

        //======== this ========
        setModal(true);
        setTitle("Complete Action Using");
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
                        "default:grow",
                        "default, $lgap, fill:default:grow"));

                //---- instructionsLabel ----
                instructionsLabel.setText("Hover over importer for details");
                instructionsLabel.setHorizontalAlignment(SwingConstants.CENTER);
                instructionsLabel.setName("instructionsLabel");
                contentPanel.add(instructionsLabel, cc.xy(1, 1));

                //======== tableScrollPane ========
                {
                    tableScrollPane.setName("tableScrollPane");

                    //---- tableIconEntries ----
                    tableIconEntries.setShowVerticalLines(false);
                    tableIconEntries.setFillsViewportHeight(true);
                    tableIconEntries.setPreferredScrollableViewportSize(new Dimension(200, 200));
                    tableIconEntries.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
                    tableIconEntries.setRowHeight(80);
                    tableIconEntries.setShowHorizontalLines(false);
                    tableIconEntries.setCellSelectionEnabled(true);
                    tableIconEntries.setName("tableIconEntries");
                    tableScrollPane.setViewportView(tableIconEntries);
                }
                contentPanel.add(tableScrollPane, cc.xy(1, 3));
            }
            dialogPane.add(contentPanel, BorderLayout.CENTER);

            //======== buttonBar ========
            {
                buttonBar.setBorder(Borders.BUTTON_BAR_GAP_BORDER);
                buttonBar.setName("buttonBar");
                buttonBar.setLayout(new FormLayout(
                        "$glue, $button, $rgap, $button, $lcgap, default",
                        "pref"));

                //---- buttonOnce ----
                buttonOnce.setText("Just Once");
                buttonOnce.setEnabled(false);
                buttonOnce.setToolTipText("Just Use the Importer On This One File");
                buttonOnce.setName("buttonOnce");
                buttonOnce.addActionListener(e -> buttonOnceActionPerformed());
                buttonBar.add(buttonOnce, cc.xy(2, 1, CellConstraints.DEFAULT, CellConstraints.FILL));

                //---- buttonAll ----
                buttonAll.setText("All Similar");
                buttonAll.setEnabled(false);
                buttonAll.setToolTipText("Use the Importer On This and All Similar Files");
                buttonAll.setName("buttonAll");
                buttonAll.addActionListener(e -> buttonAllActionPerformed());
                buttonBar.add(buttonAll, cc.xy(4, 1, CellConstraints.DEFAULT, CellConstraints.FILL));

                //---- buttonAlways ----
                buttonAlways.setText("Always");
                buttonAlways.setEnabled(false);
                buttonAlways.setToolTipText("Always Use the Importer For This Type of File");
                buttonAlways.setName("buttonAlways");
                buttonAlways.addActionListener(e -> buttonAlwaysActionPerformed());
                buttonBar.add(buttonAlways, cc.xy(6, 1));
            }
            dialogPane.add(buttonBar, BorderLayout.SOUTH);
        }
        contentPane.add(dialogPane, BorderLayout.CENTER);
        pack();
        setLocationRelativeTo(getOwner());
        // JFormDesigner - End of component initialization  //GEN-END:initComponents
    }

    // JFormDesigner - Variables declaration - DO NOT MODIFY  //GEN-BEGIN:variables
    private JScrollPane tableScrollPane;
    private JTable tableIconEntries;
    private JButton buttonOnce;
    private JButton buttonAll;
    private JButton buttonAlways;
    // JFormDesigner - End of variables declaration  //GEN-END:variables
}
