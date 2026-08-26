package gov.takx.core.view.gui.datapackage;

import com.formdev.flatlaf.FlatClientProperties;
import com.jgoodies.forms.factories.Borders;
import com.jgoodies.forms.layout.CellConstraints;
import com.jgoodies.forms.layout.FormLayout;
import gov.tak.api.comms.takserver.ITakServerConnection;
import gov.tak.api.comms.takserver.mission.data.AssociatedResource;
import gov.takx.api.annotation.InvokeOnEDT;
import gov.takx.commons.file.SizeFormatter;
import gov.takx.commons.swing.ImageUtils;
import gov.takx.commons.swing.PlaceholderTextField;
import gov.takx.core.view.gui.GUIUtils;
import gov.takx.core.view.gui.datapackage.PackageSelectionTableModel.SortOn;
import gov.takx.platform.ui.UIResources;
import jakarta.annotation.Nonnull;
import org.jdesktop.swingx.JXTable;

import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.ListSelectionModel;
import javax.swing.RowFilter;
import javax.swing.SwingConstants;
import javax.swing.border.CompoundBorder;
import javax.swing.border.LineBorder;
import javax.swing.event.DocumentEvent;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableColumn;
import javax.swing.table.TableColumnModel;

import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.image.BufferedImage;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.function.BiConsumer;

/**
 * Panel to allow the user to select packages for download.
 *
 * @since 3.1
 */
public class PackageSelectionPanel extends JPanel
{
    private static final int ROW_PADDING = 5;

    private static final ZoneId UTC = ZoneId.of("UTC");
    private static final DateTimeFormatter DMY_FORMATTER = DateTimeFormatter.ofPattern("dd LLL yyyy").withZone(UTC);

    // File name bold, formatted date below
    private static final String NAME_AND_DATE_FMT = "<html><b>%s</b><br>%s</html>";

    // Size in bold, creator's callsign below, right justified
    private static final String SIZE_AND_USER_FMT = "<html><body style=\"text-align:right;\"><b>%s</b><br>%s</body></html>";

    private final PackageSelectionTableModel model = new PackageSelectionTableModel();

    private final Runnable cancelAction; // Invoked when cancel button is clicked
    private final BiConsumer<ITakServerConnection, List<AssociatedResource>> okAction; // Invoked when Ok button is clicked

    private final SortHandler nameSortHandler;
    private final SortHandler userSortHandler;
    private final SortHandler sizeSortHandler;
    private final SortHandler dateSortHandler;

    private SortHandler currentSortHandler = null;

    /**
     * @param okAction The consumer to invoke with the list of selected file entries when the Ok button is clicked
     * @param cancelAction The action to invoke when the cancel button is clicked
     */
    public PackageSelectionPanel(@Nonnull BiConsumer<ITakServerConnection, List<AssociatedResource>> okAction,
                                 @Nonnull Runnable cancelAction)
    {
        initComponents();

        btnCancel.setIcon(new ImageIcon(ImageUtils.applyColorMask((BufferedImage) UIResources.CANCEL_ICON.getImage(), Color.red)));

        this.okAction = okAction;
        this.cancelAction = cancelAction;

        GUIUtils.addSingleActionDocumentListener(txtSearch, this::applyFilter);

        // Make all the sort buttons appear flat. This is achieved in FlatLAF by making them borderless
        List.of(btnSortName, btnSortDate, btnSortSize, btnSortUser, btnCancel)
                .forEach(btn -> btn.putClientProperty(FlatClientProperties.BUTTON_TYPE, FlatClientProperties.BUTTON_TYPE_BORDERLESS));

        nameSortHandler = new SortHandler(btnSortName, SortOn.Name);
        userSortHandler = new SortHandler(btnSortUser, SortOn.User);
        sizeSortHandler = new SortHandler(btnSortSize, SortOn.Size);
        dateSortHandler = new SortHandler(btnSortDate, SortOn.Date);

        tblPackages.setModel(model);
        tblPackages.getTableHeader().setVisible(false);

        final TableColumnModel cm = tblPackages.getColumnModel();
        final TableColumn selectedColumn = cm.getColumn(PackageSelectionTableModel.Column.SELECTED.ordinal());
        selectedColumn.setResizable(false);
        selectedColumn.setMaxWidth(30); // keep checkbox column narrow

        final TableColumn nameColumn = cm.getColumn(PackageSelectionTableModel.Column.NAME_AND_DATE.ordinal());
        nameColumn.setCellRenderer(new NameAndDateRenderer());

        final TableColumn sizeColumn = cm.getColumn(PackageSelectionTableModel.Column.SIZE_AND_USER.ordinal());
        sizeColumn.setCellRenderer(new SizeAndUserRenderer());
    }

    /**
     * Request that the focus be given to the search text field.
     */
    public void takeFocus()
    {
        txtSearch.requestFocusInWindow();
    }

    /**
     * Add the given list of files to the model.  The model is re-sorted after adding the new files.
     */
    @InvokeOnEDT
    public void addFiles(@Nonnull ITakServerConnection connection, @Nonnull List<AssociatedResource> fileList)
    {
        model.addFileList(connection, fileList);
        sort();
    }

    /**
     * @return {@code true} when the selection panel has at least one package.
     *
     * @since 5.0
     */
    public boolean hasFiles()
    {
        return model.getRowCount() > 0;
    }

    private void okButtonActionPerformed()
    {
        model.getSelectedFileEntriesPerConnection().forEach(okAction);
    }

    private void btnCancelActionPerformed()
    {
        cancelAction.run();
    }

    /**
     * Add a RowFilter using the current search text to the table.  The existing filter is removed if the search text
     * is empty.
     */
    private void applyFilter(DocumentEvent ignored)
    {
        final String searchText = txtSearch.getText();

        if (searchText.isEmpty())
        {
            tblPackages.setRowFilter(null);
        } else
        {
            // There's no way to update a filter, so just create a new one
            tblPackages.setRowFilter(new TextFilter(searchText));
        }
    }

    /**
     * Install the given sort handler as the current one, and re-sort the table.  The sort direction of the current
     * sort handler is reversed after the sort.
     */
    private void updateSort(SortHandler sortHandler)
    {
        currentSortHandler = sortHandler;

        // Reset all the sort button text
        List.of(nameSortHandler, userSortHandler, sizeSortHandler, dateSortHandler).forEach(SortHandler::resetText);

        sortHandler.updateText();
        sort();

        sortHandler.invert(); // And swap for next invocation
    }

    /**
     * Sort the table according to the current sort order.
     */
    @InvokeOnEDT
    private void sort()
    {
        if (currentSortHandler != null) model.sort(currentSortHandler.sortOn, currentSortHandler.ascending);

        GUIUtils.updateRowHeights(tblPackages, ROW_PADDING);
    }

    private void btnSortNameActionPerformed()
    {
        updateSort(nameSortHandler);
    }

    private void btnSortUserActionPerformed()
    {
        updateSort(userSortHandler);
    }

    private void btnSortSizeActionPerformed()
    {
        updateSort(sizeSortHandler);
    }

    private void btnSortDateActionPerformed()
    {
        updateSort(dateSortHandler);
    }

    /**
     * Render the name and date, left aligned.
     */
    private class NameAndDateRenderer extends AHighlightingRenderer
    {
        @Override
        public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column)
        {
            final AssociatedResource fileEntry = (AssociatedResource) value;
            final String date = LocalDate.ofInstant(fileEntry.submissionTime(), UTC).format(DMY_FORMATTER);

            value = String.format(NAME_AND_DATE_FMT, highlight(fileEntry.safeFileName()), date);

            return super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
        }
    }

    /**
     * Renders the file size and callsign, right aligned
     */
    private class SizeAndUserRenderer extends AHighlightingRenderer
    {
        private SizeAndUserRenderer()
        {
            setHorizontalAlignment(SwingConstants.RIGHT);
        }

        @Override
        public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column)
        {
            final AssociatedResource fileEntry = (AssociatedResource) value;
            final String size = SizeFormatter.formatEstimate(fileEntry.size());

            value = String.format(SIZE_AND_USER_FMT, size, highlight(fileEntry.getUploaderCallsign()));

            return super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
        }
    }

    /**
     * Adds html elements to highlight the current search text (if any) in yellow.
     */
    private abstract class AHighlightingRenderer extends DefaultTableCellRenderer
    {
        protected String highlight(String text)
        {
            final String searchText = txtSearch.getText().toLowerCase();

            if (!searchText.isEmpty())
            {
                final String searchableText = text.toLowerCase();
                final int index = searchableText.indexOf(searchText.toLowerCase());

                if (index >= 0)
                {
                    final int endIndex = index + searchText.length();
                    final String start = text.substring(0, index);
                    final String mid = text.substring(index, endIndex);
                    final String end = text.substring(endIndex);

                    return start + "<font color=yellow>" + mid + "</font>" + end;
                }
            }

            return text;
        }
    }

    /**
     * Filter to select rows that contain the search text in either the file name or user callsign.
     */
    private static class TextFilter extends RowFilter<PackageSelectionTableModel, Integer>
    {
        private final String filterText;

        private TextFilter(String filterText)
        {
            this.filterText = filterText.toLowerCase();
        }

        @Override
        public boolean include(Entry<? extends PackageSelectionTableModel, ? extends Integer> entry)
        {
            final AssociatedResource fileResource = entry.getModel().getFileEntry(entry.getIdentifier());

            return fileResource.safeFileName().toLowerCase().contains(filterText)
                    || fileResource.getUploaderCallsign().toLowerCase().contains(filterText);
        }
    }

    /**
     * Encapsulates handling of the sort button state.
     */
    private static class SortHandler
    {
        final JButton button;
        final SortOn sortOn;
        boolean ascending = true;

        /**
         * @param button The button controlling this sort
         * @param sortOn The sort field
         */
        private SortHandler(JButton button, SortOn sortOn)
        {
            this.button = button;
            this.sortOn = sortOn;
        }

        /**
         * Reset the button text to its default (no arrow).
         */
        void resetText()
        {
            button.setText(sortOn.name());
        }

        /**
         * Set our button text to include the appropriate up/down arrow.
         */
        void updateText()
        {
            final String arrow = (ascending ? " \u25b2" : " \u25bc");
            button.setText(sortOn.name() + arrow);
        }

        void invert()
        {
            ascending = !ascending;
        }
    }

    private void initComponents()
    {
        // JFormDesigner - Component initialization - DO NOT MODIFY  //GEN-BEGIN:initComponents
        var panel2 = new JPanel();
        var sortPanel = new JPanel();
        btnSortName = new JButton();
        btnSortUser = new JButton();
        btnSortSize = new JButton();
        btnSortDate = new JButton();
        btnCancel = new JButton();
        txtSearch = new PlaceholderTextField();
        var scrollPane1 = new JScrollPane();
        tblPackages = new JXTable();
        var buttonBar = new JPanel();
        var okButton = new JButton();
        CellConstraints cc = new CellConstraints();

        //======== this ========
        setName("this");
        setLayout(new FormLayout(
                "default:grow",
                "fill:default:grow"));

        //======== panel2 ========
        {
            panel2.setBorder(new CompoundBorder(
                    Borders.DLU2_BORDER,
                    new CompoundBorder(
                            LineBorder.createGrayLineBorder(),
                            Borders.DLU2_BORDER)));
            panel2.setName("panel2");
            panel2.setLayout(new FormLayout(
                    "default:grow",
                    "2*(default, $lgap), fill:default:grow, $lgap, default"));

            //======== sortPanel ========
            {
                sortPanel.setName("sortPanel");
                sortPanel.setLayout(new FormLayout(
                        "4*(default, $lcgap), default:grow, $lcgap, default",
                        "default"));

                //---- btnSortName ----
                btnSortName.setText("Name");
                btnSortName.setFocusable(false);
                btnSortName.setName("btnSortName");
                btnSortName.addActionListener(e -> btnSortNameActionPerformed());
                sortPanel.add(btnSortName, cc.xy(1, 1));

                //---- btnSortUser ----
                btnSortUser.setText("User");
                btnSortUser.setFocusable(false);
                btnSortUser.setName("btnSortUser");
                btnSortUser.addActionListener(e -> btnSortUserActionPerformed());
                sortPanel.add(btnSortUser, cc.xy(3, 1));

                //---- btnSortSize ----
                btnSortSize.setText("Size");
                btnSortSize.setFocusable(false);
                btnSortSize.setName("btnSortSize");
                btnSortSize.addActionListener(e -> btnSortSizeActionPerformed());
                sortPanel.add(btnSortSize, cc.xy(5, 1));

                //---- btnSortDate ----
                btnSortDate.setText("Date");
                btnSortDate.setFocusable(false);
                btnSortDate.setName("btnSortDate");
                btnSortDate.addActionListener(e -> btnSortDateActionPerformed());
                sortPanel.add(btnSortDate, cc.xy(7, 1));

                //---- btnCancel ----
                btnCancel.setIconTextGap(0);
                btnCancel.setFocusable(false);
                btnCancel.setName("btnCancel");
                btnCancel.addActionListener(e -> btnCancelActionPerformed());
                sortPanel.add(btnCancel, cc.xy(11, 1));
            }
            panel2.add(sortPanel, cc.xy(1, 1));

            //---- txtSearch ----
            txtSearch.setPlaceholderText("Search...");
            txtSearch.setName("txtSearch");
            panel2.add(txtSearch, cc.xy(1, 3));

            //======== scrollPane1 ========
            {
                scrollPane1.setName("scrollPane1");

                //---- tblPackages ----
                tblPackages.setModel(new DefaultTableModel(1, 0));
                tblPackages.setRowSelectionAllowed(false);
                tblPackages.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
                tblPackages.setFocusable(false);
                tblPackages.setHorizontalScrollEnabled(true);
                tblPackages.setShowVerticalLines(false);
                tblPackages.setPreferredScrollableViewportSize(new Dimension(200, 100));
                tblPackages.setName("tblPackages");
                scrollPane1.setViewportView(tblPackages);
            }
            panel2.add(scrollPane1, cc.xy(1, 5));

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
                buttonBar.add(okButton, cc.xy(4, 1));
            }
            panel2.add(buttonBar, cc.xy(1, 7));
        }
        add(panel2, cc.xy(1, 1));
        // JFormDesigner - End of component initialization  //GEN-END:initComponents
    }

    // JFormDesigner - Variables declaration - DO NOT MODIFY  //GEN-BEGIN:variables
    private JButton btnSortName;
    private JButton btnSortUser;
    private JButton btnSortSize;
    private JButton btnSortDate;
    private JButton btnCancel;
    private PlaceholderTextField txtSearch;
    private JXTable tblPackages;
    // JFormDesigner - End of variables declaration  //GEN-END:variables
}
