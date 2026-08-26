package gov.takx.commons.swing;

import com.jgoodies.forms.layout.CellConstraints;
import com.jgoodies.forms.layout.FormLayout;
import gov.tak.api.annotation.IncubatingApi;
import gov.takx.platform.ui.ResourceDirectory;
import gov.takx.platform.ui.UIResources;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import org.jdesktop.swingx.JXTable;

import javax.swing.DefaultListModel;
import javax.swing.DefaultListSelectionModel;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.ListModel;
import javax.swing.ListSelectionModel;
import javax.swing.SortOrder;
import javax.swing.SwingConstants;
import javax.swing.event.ListDataEvent;
import javax.swing.event.ListDataListener;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableModel;

import java.awt.Dimension;
import java.util.Comparator;
import java.util.List;
import java.util.function.IntPredicate;
import java.util.stream.IntStream;

/**
 * Swing component that allows management of a List by allowing a user to move items between two tables, representing
 * selected and unselected items. Mimics much of the API from {@link JList}, specifically the usage of {@link ListSelectionModel}
 * and {@link ListSelectionListener}.<p>
 * Usage:<pre>{@code
 * DualList<MyObject> myDualList = new DualList<>();
 * List<MyObject> myObjects = new ArrayList<>();
 * ((DefaultListModel<MyObject>) myDualList.getModel()).addAll(myObjects);
 * ...
 * List<MyObject> selected = myDualList.getSelectedItems();
 * }</pre>
 *
 * @since 5.6
 */
@IncubatingApi(since = "5.6")
public class DualList<T> extends JPanel
{
    private static final ImageIcon ARROW_FORWARD_ICON = UIResources.getStandardSizedIcon(ResourceDirectory.PLATFORM_ICONS, "arrow_right.png");
    private static final ImageIcon ARROW_BACK_ICON = UIResources.getStandardSizedIcon(ResourceDirectory.PLATFORM_ICONS, "arrow_left.png");

    private final DefaultTableModel unselectedTableModel;
    private final DefaultTableModel selectedTableModel;
    private final DualListSelectionListener selectionListener = new DualListSelectionListener();
    private final DualListDataListener dataListener = new DualListDataListener();
    private int rowHeight;
    private ListModel<T> model;
    private ListSelectionModel selectionModel;

    /**
     * Construct a new component with empty tables.
     */
    public DualList()
    {
        initComponents();

        addButton.setIcon(ARROW_FORWARD_ICON);
        removeButton.setIcon(ARROW_BACK_ICON);

        unselectedTable.setTableHeader(null);
        selectedTable.setTableHeader(null);

        selectedTableModel = (DefaultTableModel) selectedTable.getModel();
        selectedTableModel.setColumnCount(1);
        unselectedTableModel = (DefaultTableModel) unselectedTable.getModel();
        unselectedTableModel.setColumnCount(1);

        setModel(new DefaultListModel<>());
        setSelectionModel(new DefaultListSelectionModel());
    }

    /**
     * @param comparator the comparator to use to sort items in the selected and unselected tables
     */
    public void setComparator(@Nullable Comparator<T> comparator)
    {
        unselectedTable.getColumnExt(0).setComparator(comparator);
        unselectedTable.setSortOrder(0, SortOrder.ASCENDING);

        selectedTable.getColumnExt(0).setComparator(comparator);
        selectedTable.setSortOrder(0, SortOrder.ASCENDING);
    }

    /**
     * @return the selection model this component is using. Defaults to {@link DefaultListSelectionModel}.
     */
    @Nonnull
    public ListSelectionModel getSelectionModel()
    {
        return selectionModel;
    }

    /**
     * @param selectionModel the selection model this component should use
     */
    public void setSelectionModel(@Nonnull ListSelectionModel selectionModel)
    {
        if (this.selectionModel != null)
        {
            this.selectionModel.removeListSelectionListener(selectionListener);
        }
        this.selectionModel = selectionModel;
        selectionModel.addListSelectionListener(selectionListener);
    }

    /**
     * @return the list model this component is using. Defaults to {@link DefaultListModel}.
     */
    @Nonnull
    public ListModel<T> getModel()
    {
        return model;
    }

    /**
     * @param model the list model to use for providing data to this component
     */
    public void setModel(@Nonnull ListModel<T> model)
    {
        if (this.model != null) this.model.removeListDataListener(dataListener);
        this.model = model;
        IntStream.range(0, model.getSize()).forEach(index -> unselectedTableModel.insertRow(index, new Object[]{model.getElementAt(index)}));
        model.addListDataListener(dataListener);
    }

    /**
     * @param height the row height to use in both tables
     */
    public void setRowHeight(int height)
    {
        rowHeight = height;
        selectedTable.setRowHeight(height);
        unselectedTable.setRowHeight(height);
    }

    /**
     * @return the height of table rows
     */
    public int getRowHeight()
    {
        return rowHeight;
    }

    /**
     * Sets a table cell renderer on the first column for both tables.
     *
     * @param cellRenderer the cell renderer
     */
    public void setCellRenderer(@Nonnull TableCellRenderer cellRenderer)
    {
        selectedTable.getColumnModel().getColumn(0).setCellRenderer(cellRenderer);
        unselectedTable.getColumnModel().getColumn(0).setCellRenderer(cellRenderer);
    }

    /**
     * @param selectionListener the listener to add to the selection model
     */
    public void addListSelectionListener(@Nonnull ListSelectionListener selectionListener)
    {
        getSelectionModel().addListSelectionListener(selectionListener);
    }

    /**
     * @return the List of selected items
     */
    @Nonnull
    public List<T> getSelectedItems()
    {
        return IntStream.of(selectionModel.getSelectedIndices()).mapToObj(model::getElementAt).toList();
    }

    /**
     * @param selectedItems the items to be shown in the selected table (must exist in the list model)
     */
    public void setSelectedItems(@Nonnull List<T> selectedItems)
    {
        selectionModel.clearSelection();
        selectedItems.forEach(item -> {
            int modelIndex = indexOf(item);
            if (modelIndex != -1) selectionModel.addSelectionInterval(modelIndex, modelIndex);
        });
    }

    /**
     * @return the list of unselected items
     */
    @Nonnull
    public List<T> getUnselectedItems()
    {
        return IntStream.range(0, model.getSize()).filter(((IntPredicate) (selectionModel::isSelectedIndex)).negate()).mapToObj(model::getElementAt).toList();
    }

    /**
     * @return the JXTable that holds selected rows
     */
    @Nonnull
    public JXTable getSelectedTable()
    {
        return selectedTable;
    }

    /**
     * @return the JXTable that holds unselected rows
     */
    @Nonnull
    public JXTable getUnselectedTable()
    {
        return unselectedTable;
    }

    /**
     * @return the unselected label text
     */
    @Nullable
    public String getUnselectedText()
    {
        return unselectedLabel.getText();
    }

    /**
     * @param text the unselected label text
     */
    public void setUnselectedText(@Nullable String text)
    {
        unselectedLabel.setText(text);
    }

    /**
     * @return the selected label text
     */
    @Nullable
    public String getSelectedText()
    {
        return selectedLabel.getText();
    }

    /**
     * @param text the selected label text
     */
    public void setSelectedText(@Nullable String text)
    {
        selectedLabel.setText(text);
    }

    private void transfer(DefaultTableModel source, DefaultTableModel destination, Object value)
    {
        var sourceIndex = indexOf(value, source);
        var destinationIndex = indexOf(value, destination);
        if (sourceIndex != -1)
        {
            source.removeRow(sourceIndex);
        }
        if (destinationIndex == -1)
        {
            destination.addRow(new Object[]{value});
        }
    }

    private int indexOf(Object modelValue)
    {
        for (var i = 0; i < model.getSize(); i++)
        {
            if (model.getElementAt(i) == modelValue) return i;
        }
        return -1;
    }

    private int indexOf(Object modelValue, TableModel tableModel)
    {
        for (var i = 0; i < tableModel.getRowCount(); i++)
        {
            if (tableModel.getValueAt(i, 0) == modelValue) return i;
        }
        return -1;
    }

    private void addButtonActionPerformed()
    {
        // Convert selected indices into list model indices and add to selection model
        IntStream.of(unselectedTable.getSelectedRows())
                .map(unselectedTable::convertRowIndexToModel)
                .boxed()
                .sorted(Comparator.reverseOrder()) // Removals from the table model need to be done in descending order
                .forEach(modelIndex -> {
                    var listModelIndex = indexOf(unselectedTableModel.getValueAt(modelIndex, 0));
                    selectionModel.addSelectionInterval(listModelIndex, listModelIndex);
                });
    }

    private void removeButtonActionPerformed()
    {
        IntStream.of(selectedTable.getSelectedRows())
                .map(selectedTable::convertRowIndexToModel)
                .boxed()
                .sorted(Comparator.reverseOrder())
                .forEach(modelIndex -> {
                    var listModelIndex = indexOf(selectedTableModel.getValueAt(modelIndex, 0));
                    selectionModel.removeSelectionInterval(listModelIndex, listModelIndex);
                });
    }

    private class DualListSelectionListener implements ListSelectionListener
    {
        @Override
        public void valueChanged(ListSelectionEvent e)
        {
            for (var i = e.getFirstIndex(); i <= e.getLastIndex(); i++)
            {
                var value = model.getElementAt(i);
                var selected = selectionModel.isSelectedIndex(i);
                transfer(selected ? unselectedTableModel : selectedTableModel, selected ? selectedTableModel : unselectedTableModel, value);
            }
        }
    }

    private class DualListDataListener implements ListDataListener
    {
        @Override
        public void intervalAdded(ListDataEvent e)
        {
            IntStream.range(e.getIndex0(), e.getIndex1() + 1).forEach(index -> unselectedTableModel.insertRow(index, new Object[]{model.getElementAt(index)}));
        }

        @Override
        public void intervalRemoved(ListDataEvent e)
        {
            IntStream.range(e.getIndex0(), e.getIndex1() + 1).forEach(row -> {
                T value = model.getElementAt(row);
                unselectedTableModel.removeRow(indexOf(value, unselectedTableModel));
                selectedTableModel.removeRow(indexOf(value, selectedTableModel));
            });
        }

        @Override
        public void contentsChanged(ListDataEvent e)
        {
            IntStream.range(e.getIndex0(), e.getIndex1() + 1).forEach(row -> {
                T value = model.getElementAt(row);
                unselectedTableModel.setValueAt(value, row, 0);
            });
        }
    }

    private void initComponents()
    {
        // JFormDesigner - Component initialization - DO NOT MODIFY  //GEN-BEGIN:initComponents
        unselectedLabel = new JLabel();
        selectedLabel = new JLabel();
        var unselectedScrollPane = new JScrollPane();
        unselectedTable = new JXTable();
        var buttonPanel = new JPanel();
        addButton = new JButton();
        removeButton = new JButton();
        var selectedScrollPane = new JScrollPane();
        selectedTable = new JXTable();
        CellConstraints cc = new CellConstraints();

        //======== this ========
        setName("this");
        setLayout(new FormLayout(
                "default:grow, $lcgap, default, $lcgap, default:grow",
                "fill:default, $rgap, default:grow, $lgap"));

        //---- unselectedLabel ----
        unselectedLabel.setText("Unselected:");
        unselectedLabel.setName("unselectedLabel");
        add(unselectedLabel, cc.xy(1, 1));

        //---- selectedLabel ----
        selectedLabel.setText("Selected:");
        selectedLabel.setName("selectedLabel");
        add(selectedLabel, cc.xy(5, 1));

        //======== unselectedScrollPane ========
        {
            unselectedScrollPane.setName("unselectedScrollPane");

            //---- unselectedTable ----
            unselectedTable.setPreferredScrollableViewportSize(new Dimension(100, 200));
            unselectedTable.setShowHorizontalLines(false);
            unselectedTable.setShowVerticalLines(false);
            unselectedTable.setAutoResizeMode(JTable.AUTO_RESIZE_ALL_COLUMNS);
            unselectedTable.setName("unselectedTable");
            unselectedScrollPane.setViewportView(unselectedTable);
        }
        add(unselectedScrollPane, cc.xy(1, 3, CellConstraints.DEFAULT, CellConstraints.FILL));

        //======== buttonPanel ========
        {
            buttonPanel.setName("buttonPanel");
            buttonPanel.setLayout(new FormLayout(
                    "default",
                    "default, $lgap, default"));

            //---- addButton ----
            addButton.setText("Add");
            addButton.setHorizontalTextPosition(SwingConstants.CENTER);
            addButton.setVerticalTextPosition(SwingConstants.TOP);
            addButton.setName("addButton");
            addButton.addActionListener(e -> addButtonActionPerformed());
            buttonPanel.add(addButton, cc.xy(1, 1));

            //---- removeButton ----
            removeButton.setText("Remove");
            removeButton.setHorizontalTextPosition(SwingConstants.CENTER);
            removeButton.setVerticalTextPosition(SwingConstants.TOP);
            removeButton.setName("removeButton");
            removeButton.addActionListener(e -> removeButtonActionPerformed());
            buttonPanel.add(removeButton, cc.xy(1, 3));
        }
        add(buttonPanel, cc.xy(3, 3));

        //======== selectedScrollPane ========
        {
            selectedScrollPane.setName("selectedScrollPane");

            //---- selectedTable ----
            selectedTable.setPreferredScrollableViewportSize(new Dimension(100, 200));
            selectedTable.setShowHorizontalLines(false);
            selectedTable.setShowVerticalLines(false);
            selectedTable.setAutoResizeMode(JTable.AUTO_RESIZE_ALL_COLUMNS);
            selectedTable.setName("selectedTable");
            selectedScrollPane.setViewportView(selectedTable);
        }
        add(selectedScrollPane, cc.xy(5, 3, CellConstraints.DEFAULT, CellConstraints.FILL));
        // JFormDesigner - End of component initialization  //GEN-END:initComponents
    }

    // JFormDesigner - Variables declaration - DO NOT MODIFY  //GEN-BEGIN:variables
    protected JLabel unselectedLabel;
    protected JLabel selectedLabel;
    protected JXTable unselectedTable;
    protected JButton addButton;
    protected JButton removeButton;
    protected JXTable selectedTable;
    // JFormDesigner - End of variables declaration  //GEN-END:variables
}
