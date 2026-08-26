package gov.takx.commons.swing;

import com.jgoodies.forms.layout.CellConstraints;
import com.jgoodies.forms.layout.FormLayout;
import gov.takx.platform.ui.UIResources;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import javax.swing.Action;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.JSeparator;
import javax.swing.JSplitPane;
import javax.swing.JTable;
import javax.swing.ListSelectionModel;
import javax.swing.SwingConstants;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableColumn;
import javax.swing.table.TableColumnModel;

import java.awt.CardLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Point;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * A panel that renders a master table on the left and a details panel on the right. The content of the detail panel
 * is determined from the selected row in the master table.
 * <p/>
 * Each row in the table has a checkbox to enable/disable that entry, the text display name of the entry, and a button
 * to remove the entry from the table.
 *
 * @param <E> Type of object in the table model entries
 * @param <V> Validation handler type for entries
 *
 * @since 3.4
 */
public abstract class AListAndDetailPanel<E, V extends IValidateAction & IPanelStateListenerReceiver> extends JPanel implements IValidateAction, IPanelStateListener
{
    private static final int ROW_HEIGHT = 34;
    private static final int ICON_COLUMN_WIDTH = 22;
    private static final String NULL_PANEL_NAME = "NullPanel";

    protected final List<TableRow> rows = new ArrayList<>();

    private final TableModel tableModel = new TableModel();
    private final CardLayout detailCardLayout;

    private IPanelStateListener panelStateListener;
    private int lastSelectedRow = -1;

    /**
     * @param panelStateListener State listener for clean/dirty updates
     */
    protected AListAndDetailPanel(IPanelStateListener panelStateListener)
    {
        this.panelStateListener = panelStateListener;

        initComponents();
        configureTable();

        addButton.setIcon(UIResources.ADD_ICON);

        detailCardLayout = (CardLayout) detailPanel.getLayout();
        detailPanel.add(new JPanel(), NULL_PANEL_NAME);

        updateForSelection();
    }

    /**
     * Construct a new instance without a {@link IPanelStateListener}.
     *
     * @since 5.6
     */
    protected AListAndDetailPanel()
    {
        this(null);
    }

    /**
     * Set the name to be used in UI text to refer to the entries in the table.
     *
     * @param singular Name to use for UI text, e.g., {@code "Selected <singular>"} and {@code "Add a new <singular>"}
     * @param plural Name to use as the header of the entry table (the plural form of the entry type name, like "Rules")
     */
    protected void setEntryTypeNames(@Nonnull String singular, @Nonnull String plural)
    {
        entriesLabel.setText(plural);
        selectedEntryLabel.setText("Selected " + singular);
        addButton.setToolTipText("Add a new " + singular);
    }

    /**
     * Create a proxy to map our operations onto target table entries.
     *
     * @param entry Entry for which to create a proxy
     * @return Proxy for that entry
     * @see EntryProxy
     */
    protected abstract EntryProxy createEntryProxy(@Nonnull E entry);

    /**
     * Apply all modifications to all entries.
     */
    public abstract void applyActions();

    /**
     * Respond to the user clicking the "Add" button.  Implementations will likely then call {@link #showAddActions(List)}
     * to present the user with options for what to add.
     */
    protected abstract void addButtonActionPerformed();

    /**
     * Add the given component below the master table.
     *
     * @param component Component to add
     */
    protected void addTableFooter(@Nonnull JComponent component)
    {
        masterPanel.add(component, new CellConstraints().xy(1, 5));
    }

    /**
     * {@inheritDoc}
     * <p/>
     * @ImplNote This implementation delegates the call to an internal field if not {@code null}.
     */
    @Override
    public void setPanelDirty()
    {
        if (panelStateListener != null) panelStateListener.setPanelDirty();
    }

    /**
     * {@inheritDoc}
     * <p/>
     * @ImplNote This implementation delegates the call to an internal field if not {@code null}.
     * @since 5.6
     */
    @Override
    public void setPanelClean()
    {
        if (panelStateListener != null) panelStateListener.setPanelClean();
    }

    /**
     * @return the current panel state listener
     * @since 5.6
     */
    @SuppressWarnings("unused")
    public IPanelStateListener getPanelStateListener()
    {
        return panelStateListener;
    }

    /**
     * @param panelStateListener the new panel state listener
     * @since 5.6
     */
    public void setPanelStateListener(IPanelStateListener panelStateListener)
    {
        this.panelStateListener = panelStateListener;
    }

    /**
     * Initialize the table with the given list of entries.  The first entry will be initially selected.
     * The model will be marked as clean after calling this method.
     *
     * @param entries List of entries to add to the table
     * @see #addEntry
     */
    public void initialize(@Nonnull List<E> entries)
    {
        clear();
        entries.forEach(this::addRow);

        tableModel.fireTableDataChanged();

        selectRow(0);
        updateForSelection();
    }

    /**
     * Clear the model of all entries and associated data.  The model will be marked as clean after calling this method.
     */
    public void clear()
    {
        rows.clear();
        detailPanel.removeAll();
        tableModel.fireTableDataChanged();
        setPanelClean();
        revalidate();
    }

    /**
     * Add the given entry to the master table.  A proxy will be created to wrap the given entry.
     *
     * @param entry Entry to add
     * @see #createEntryProxy
     */
    public void addEntry(@Nonnull E entry)
    {
        addRow(entry);
        setPanelDirty();
        tableModel.fireTableDataChanged();
        selectRow(rows.size() - 1);
    }

    /**
     * Add a row for a given entry to our table.  A proxy will be created to represent the actual entry.
     *
     * @see #createEntryProxy
     */
    protected void addRow(@Nonnull E entry)
    {
        final EntryProxy entryProxy = createEntryProxy(entry);
        final TableRow tableRow = new TableRow(entryProxy);
        rows.add(tableRow);

        final JComponent viewComponent = tableRow.viewComponent;
        ((IPanelStateListenerReceiver) viewComponent).setPanelListener(this);

        detailPanel.add(viewComponent, entryProxy.getKey());
    }

    /**
     * Select the given {@code row} in the master table.  This will cause the associated detail panel to be displayed.
     */
    protected void selectRow(int row)
    {
        if (row >= 0 && row < rows.size()) masterTable.getSelectionModel().setSelectionInterval(row, row);
    }

    /**
     * @return A list of all the entries (actual, not proxies) in the master table. Never {@code null}, may be empty.
     */
    @Nonnull
    protected List<E> getEntries()
    {
        return rows.stream().map(r -> r.entryProxy.getEntry()).toList();
    }

    /**
     * Show a menu of provided actions as a popup menu below the add button.
     *
     * @param addActions Actions to place in the popup menu
     */
    public void showAddActions(@Nonnull List<Action> addActions)
    {
        if (!addActions.isEmpty())
        {
            final JPopupMenu popupMenu = new JPopupMenu();
            addActions.forEach(popupMenu::add);
            popupMenu.show(this, addButton.getX(), (addButton.getY() + addButton.getHeight()));
        }
    }

    /**
     * Remove the specified row from our model. The model is marked as dirty after this operation.
     */
    protected void removeRow(int rowIndex)
    {
        if (rowIndex >= 0 && rowIndex < rows.size())
        {
            final JComponent viewComponent = rows.remove(rowIndex).viewComponent;
            detailCardLayout.removeLayoutComponent(viewComponent);
            detailPanel.remove(viewComponent);
            detailPanel.revalidate();
            tableModel.fireTableDataChanged();
            setPanelDirty();
        }
    }

    /**
     * Update text visuals (labels) and show the detail panel associated with the selected table entry.
     */
    private void updateForSelection()
    {
        final EntryProxy selectedProxy = getSelectedEntryProxy();
        final boolean haveSelection = selectedProxy != null;

        detailCardLayout.show(detailPanel, haveSelection ? selectedProxy.getKey() : NULL_PANEL_NAME);
        selectedEntryNameLabel.setText(haveSelection ? selectedProxy.getDisplayName() : "None");
        selectedEntryNameLabel.setToolTipText(selectedEntryNameLabel.getText());
    }

    /**
     * @return The currently selected entry proxy, or {@code null} if none.
     */
    @Nullable
    private EntryProxy getSelectedEntryProxy()
    {
        int row = masterTable.getSelectedRow();
        return row >= 0 && row < rows.size()
                ? rows.get(row).entryProxy
                : null;
    }

    /**
     * Commit changes to all entries in the master table.
     */
    protected void commit()
    {
        rows.forEach(TableRow::commit);
    }

    /**
     * Configure the table header, rows, and columns.
     */
    private void configureTable()
    {
        masterTable.setModel(tableModel);
        masterTable.setRowHeight(ROW_HEIGHT);
        masterTable.setTableHeader(null);

        TableColumnModel columnModel = masterTable.getColumnModel();

        TableColumn enabledColumn = columnModel.getColumn(Column.ENABLED.ordinal());
        setColumnWidth(enabledColumn, ICON_COLUMN_WIDTH);
        enabledColumn.setCellRenderer(new BooleanRenderer());

        columnModel.getColumn(Column.NAME.ordinal()).setCellRenderer(new NameRenderer());

        TableColumn removeColumn = columnModel.getColumn(Column.REMOVE.ordinal());
        removeColumn.setCellRenderer(new RemoveIconRenderer());
        setColumnWidth(removeColumn, ICON_COLUMN_WIDTH);

        masterTable.addMouseListener(new RemoveButtonMouseAdapter());
        masterTable.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting())
            {
                lastSelectedRow = masterTable.getSelectedRow();
                updateForSelection();
            }
        });
    }

    private void setColumnWidth(TableColumn column, int width)
    {
        column.setMinWidth(width);
        column.setWidth(width);
        column.setPreferredWidth(width);
        column.setMaxWidth(width);
    }

    /**
     * The columns in our table.
     */
    protected enum Column
    {
        ENABLED(Boolean.class), NAME(String.class), REMOVE(Object.class);

        static final Column[] VALUES = values();

        final Class<?> type;

        Column(Class<?> type)
        {
            this.type = type;
        }
    }

    /**
     * Model for the master table.
     */
    protected class TableModel extends AbstractTableModel
    {
        @Override
        public int getRowCount()
        {
            return rows.size();
        }

        @Override
        public int getColumnCount()
        {
            return Column.VALUES.length;
        }

        @Override
        public boolean isCellEditable(int row, int column)
        {
            return column == Column.ENABLED.ordinal(); // Only the "enable" checkbox is editable
        }

        @Override
        public Class<?> getColumnClass(int column)
        {
            return Column.VALUES[column].type;
        }

        @Override
        public Object getValueAt(int row, int column)
        {
            return switch (Column.VALUES[column])
            {
                case ENABLED -> rows.get(row).enabled;
                case NAME -> rows.get(row).displayName;
                default -> null;
            };
        }

        @Override
        public void setValueAt(Object value, int row, int column)
        {
            if (column == Column.ENABLED.ordinal())
            {
                rows.get(row).enabled = (Boolean) value;
                setPanelDirty();
                fireTableCellUpdated(row, column);
            }
        }
    }

    /**
     * A proxy to mediate local operations with operations on our target entry types.  Those types are not required to
     * implement any specific interface, so this proxy defines that contract and subclasses are responsible for
     * implementing a proxy that performs the appropriate translation from this contract to available operations on
     * the target entry.
     */
    protected abstract class EntryProxy
    {
        /**
         * @return The actual target entry
         */
        @Nonnull
        public abstract E getEntry();

        /**
         * @return A unique key for an entry in the master table
         */
        @Nonnull
        protected abstract String getKey();

        /**
         * @return {@code true} if the entry is enabled, {@code false} if not
         */
        protected abstract boolean isEnabled();

        /**
         * Set the entry enabled state.
         *
         * @param enabled New enabled state
         */
        protected abstract void setEnabled(boolean enabled);

        /**
         * @return Display name to show in the master table for this entry
         */
        @Nonnull
        protected abstract String getDisplayName();

        /**
         * @return Component that will handle validation and dirty state tracking for the target entry
         */
        @Nonnull
        protected abstract V getValidationHandler();

        /**
         * @return The panel to display in the detail area for this entry
         */
        @Nonnull
        protected abstract JPanel getDetailPanel();

        /**
         * Apply any changes from the UI (detail panel) to the target entry.
         */
        protected abstract void applyAction();
    }

    /**
     * Master table model entry.
     */
    protected class TableRow
    {
        public final EntryProxy entryProxy;
        public final JPanel viewComponent;
        public final V validationHandler;

        public boolean enabled;
        public String displayName;

        protected TableRow(EntryProxy entryProxy)
        {
            this.entryProxy = entryProxy;
            enabled = entryProxy.isEnabled();
            displayName = entryProxy.getDisplayName();
            validationHandler = Objects.requireNonNull(entryProxy.getValidationHandler(), "'entryProxy.getValidationHandler()' must not return null");
            viewComponent = Objects.requireNonNull(entryProxy.getDetailPanel(), "'entryProxy.getDetailPanel()' must not return null");
        }

        /**
         * Commit any changes from the UI to the model target entry, via the proxy.
         */
        protected void commit()
        {
            entryProxy.applyAction();
            entryProxy.setEnabled(enabled);
            displayName = entryProxy.getDisplayName(); // In case the "applyAction" modified the display name
        }
    }

    /**
     * Custom version of the boolean renderer to avoid highlighting the background.
     */
    private static class BooleanRenderer extends JCheckBox implements TableCellRenderer
    {
        public BooleanRenderer()
        {
            setOpaque(true);
            setHorizontalAlignment(JLabel.CENTER);
        }

        @Override
        public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus,
                                                       int row, int column)
        {
            setSelected((value != null && (Boolean) value));
            return this;
        }

        @Override
        public String getToolTipText()
        {
            return isSelected() ? "Disable" : "Enable";
        }
    }

    /**
     * Renderer for the "remove row" table column.  The remove icon is only displayed if the row is selected.
     */
    private static class RemoveIconRenderer extends DefaultTableCellRenderer
    {
        public RemoveIconRenderer()
        {
            setToolTipText("Remove Entry");
        }

        @Override
        public Component getTableCellRendererComponent(JTable table, Object value, boolean selected, boolean focused,
                                                       int row, int column)
        {
            super.getTableCellRendererComponent(table, value, selected, false, row, column);
            setIcon(selected ? UIResources.REMOVE_ICON : null);
            return this;
        }
    }

    /**
     * Renderer for the name column of the table, and prevent focus border from rendering.
     */
    private static class NameRenderer extends DefaultTableCellRenderer
    {
        @Override
        public Component getTableCellRendererComponent(JTable table, Object value, boolean selected, boolean focused,
                                                       int row, int column)
        {
            // Pass hasFocus as false to avoid focus border
            return super.getTableCellRendererComponent(table, value, selected, false, row, column);
        }
    }

    /**
     * Mouse listener to handle the user clicking on the "remove" icon in the selected table row.
     */
    private class RemoveButtonMouseAdapter extends MouseAdapter
    {
        private boolean enabled = false; // True IFF the user clicks in the "remove" column of the currently selected row

        /**
         * Have to determine if we are enabled during the pressed event because the clicked event comes AFTER the
         * table selection has changed to the row clicked.
         */
        @Override
        public void mousePressed(MouseEvent e)
        {
            final Point mousePoint = e.getPoint();

            enabled = e.getButton() == MouseEvent.BUTTON1
                    && Column.REMOVE.ordinal() == masterTable.columnAtPoint(mousePoint)
                    && lastSelectedRow == masterTable.rowAtPoint(mousePoint);
        }

        @Override
        public void mouseClicked(MouseEvent e)
        {
            final Point mousePoint = e.getPoint();

            if (e.getButton() == MouseEvent.BUTTON1 && Column.REMOVE.ordinal() == masterTable.columnAtPoint(mousePoint))
            {
                final int rowAtPoint = masterTable.rowAtPoint(mousePoint);
                if (enabled && lastSelectedRow == rowAtPoint) removeRow(rowAtPoint);
            }
        }
    }

    private void initComponents()
    {
        // JFormDesigner - Component initialization - DO NOT MODIFY  //GEN-BEGIN:initComponents
        var splitPane = new JSplitPane();
        var masterPane = new JPanel();
        masterPanel = new JPanel();
        var headerPanel = new JPanel();
        entriesLabel = new JLabel();
        addButton = new JButton();
        var scrollPanel = new JScrollPane();
        masterTable = new JTable();
        var separator2 = new JSeparator();
        var detailPane = new JPanel();
        var detailHeaderPanel = new JPanel();
        selectedEntryLabel = new JLabel();
        selectedEntryNameLabel = new JLabel();
        var separator3 = new JSeparator();
        detailPanel = new JPanel();
        CellConstraints cc = new CellConstraints();

        //======== this ========
        setName("this");
        setLayout(new FormLayout(
                "default:grow",
                "fill:default:grow"));

        //======== splitPane ========
        {
            splitPane.setDividerSize(6);
            splitPane.setResizeWeight(0.25);
            splitPane.setName("splitPane");

            //======== masterPane ========
            {
                masterPane.setName("masterPane");
                masterPane.setLayout(new FormLayout(
                        "default:grow, default",
                        "fill:default:grow"));

                //======== masterPanel ========
                {
                    masterPanel.setName("masterPanel");
                    masterPanel.setLayout(new FormLayout(
                            "default:grow",
                            "default, $lgap, fill:default:grow, $lgap, default"));

                    //======== headerPanel ========
                    {
                        headerPanel.setName("headerPanel");
                        headerPanel.setLayout(new FormLayout(
                                "default:grow, default",
                                "default"));

                        //---- entriesLabel ----
                        entriesLabel.setText("Entries:");
                        entriesLabel.setFont(entriesLabel.getFont().deriveFont(entriesLabel.getFont().getStyle() | Font.BOLD, entriesLabel.getFont().getSize() + 1f));
                        entriesLabel.setName("entriesLabel");
                        headerPanel.add(entriesLabel, cc.xy(1, 1, CellConstraints.CENTER, CellConstraints.DEFAULT));

                        //---- addButton ----
                        addButton.setToolTipText("Add an entry");
                        addButton.setRequestFocusEnabled(false);
                        addButton.setFocusable(false);
                        addButton.setName("addButton");
                        addButton.addActionListener(e -> addButtonActionPerformed());
                        headerPanel.add(addButton, cc.xy(2, 1));
                    }
                    masterPanel.add(headerPanel, cc.xy(1, 1));

                    //======== scrollPanel ========
                    {
                        scrollPanel.setName("scrollPanel");

                        //---- masterTable ----
                        masterTable.setShowVerticalLines(false);
                        masterTable.setShowHorizontalLines(false);
                        masterTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
                        masterTable.setPreferredScrollableViewportSize(new Dimension(100, 200));
                        masterTable.setName("masterTable");
                        scrollPanel.setViewportView(masterTable);
                    }
                    masterPanel.add(scrollPanel, cc.xy(1, 3));
                }
                masterPane.add(masterPanel, cc.xy(1, 1));

                //---- separator2 ----
                separator2.setOrientation(SwingConstants.VERTICAL);
                separator2.setName("separator2");
                masterPane.add(separator2, cc.xy(2, 1));
            }
            splitPane.setLeftComponent(masterPane);

            //======== detailPane ========
            {
                detailPane.setName("detailPane");
                detailPane.setLayout(new FormLayout(
                        "default:grow",
                        "2*(default), fill:default:grow"));

                //======== detailHeaderPanel ========
                {
                    detailHeaderPanel.setFont(detailHeaderPanel.getFont().deriveFont(detailHeaderPanel.getFont().getSize() + 1f));
                    detailHeaderPanel.setName("detailHeaderPanel");
                    detailHeaderPanel.setLayout(new FormLayout(
                            "default:grow",
                            "default, $lgap, default"));

                    //---- selectedEntryLabel ----
                    selectedEntryLabel.setText("Selected Entry:");
                    selectedEntryLabel.setHorizontalAlignment(SwingConstants.CENTER);
                    selectedEntryLabel.setFont(selectedEntryLabel.getFont().deriveFont(selectedEntryLabel.getFont().getStyle() | Font.BOLD, selectedEntryLabel.getFont().getSize() + 1f));
                    selectedEntryLabel.setName("selectedEntryLabel");
                    detailHeaderPanel.add(selectedEntryLabel, cc.xy(1, 1));

                    //---- selectedEntryNameLabel ----
                    selectedEntryNameLabel.setText("None");
                    selectedEntryNameLabel.setFont(selectedEntryNameLabel.getFont().deriveFont(selectedEntryNameLabel.getFont().getSize() + 1f));
                    selectedEntryNameLabel.setName("selectedEntryNameLabel");
                    detailHeaderPanel.add(selectedEntryNameLabel, cc.xy(1, 3, CellConstraints.CENTER, CellConstraints.DEFAULT));
                }
                detailPane.add(detailHeaderPanel, cc.xy(1, 1));

                //---- separator3 ----
                separator3.setName("separator3");
                detailPane.add(separator3, cc.xy(1, 2));

                //======== detailPanel ========
                {
                    detailPanel.setName("detailPanel");
                    detailPanel.setLayout(new CardLayout());
                }
                detailPane.add(detailPanel, cc.xy(1, 3));
            }
            splitPane.setRightComponent(detailPane);
        }
        add(splitPane, cc.xy(1, 1, CellConstraints.FILL, CellConstraints.FILL));
        // JFormDesigner - End of component initialization  //GEN-END:initComponents
    }

    // JFormDesigner - Variables declaration - DO NOT MODIFY  //GEN-BEGIN:variables
    private JPanel masterPanel;
    private JLabel entriesLabel;
    private JButton addButton;
    private JTable masterTable;
    private JLabel selectedEntryLabel;
    private JLabel selectedEntryNameLabel;
    private JPanel detailPanel;
    // JFormDesigner - End of variables declaration  //GEN-END:variables
}
