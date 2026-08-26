package gov.takx.commons.swing;

import javax.swing.JList;
import javax.swing.JTable;
import javax.swing.JTree;
import javax.swing.ListCellRenderer;
import javax.swing.table.TableCellRenderer;
import javax.swing.tree.TreeCellRenderer;

import java.awt.Component;

/**
 * Used to turn a JList, JTable, or JTree into a Check List. The model must contain instances of ICheckBoxItem.
 * <p/>
 * <pre>
 * jlist.setCellRenderer(new CheckBoxCellRenderer());
 * jlist.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
 * <p/>
 * jlist.addMouseListener(new MouseAdapter()
 * {
 *      public void mouseClicked(MouseEvent event)
 *      {
 *           JList list = (JList) event.getSource();
 * <p/>
 *           // Get index of item clicked
 *           int index = list.locationToIndex(event.getPoint());
 *           CheckBoxItem item = (CheckBoxItem) list.getModel().getElementAt(index);
 * <p/>
 *           // Toggle the item's selected state
 *           item.setSelected(!item.isSelected());
 * <p/>
 *           // Repaint the changed cell
 *           list.repaint(list.getCellBounds(index, index));
 *      }
 * });
 * </pre>
 *
 * @param <E> The type of item stored in the list/table/tree
 * @see CheckBoxItem
 * @since 1.4 (renamed and updated to use generics in 3.0)
 */
public class CheckBoxCellRenderer<E extends ICheckBoxItem> extends CheckBoxIconPanel implements ListCellRenderer<E>,
        TableCellRenderer, TreeCellRenderer
{
    @Override
    public Component getListCellRendererComponent(JList<? extends E> list, E value, int index, boolean isSelected,
                                                  boolean hasFocus)
    {
        updateRendererComponentForValue(list, value);
        return this;
    }

    /**
     * {@inheritDoc}
     *
     * @since 3.0
     */
    @Override
    public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus,
                                                   int row, int column)
    {
        //noinspection unchecked
        updateRendererComponentForValue(table, (E) value);
        return this;
    }

    /**
     * {@inheritDoc}
     *
     * @since 3.0
     */
    @Override
    public Component getTreeCellRendererComponent(JTree tree, Object value, boolean selected, boolean expanded,
                                                  boolean leaf, int row, boolean hasFocus)
    {
        //noinspection unchecked
        updateRendererComponentForValue(tree, (E) value);
        return this;
    }

    protected void updateRendererComponentForValue(Component parent, E value)
    {
        setComponentOrientation(parent.getComponentOrientation());
        setEnabled(parent.isEnabled());
        setSelected(value.isSelected());
        setFont(parent.getFont());
        setBackground(parent.getBackground());
        setForeground(parent.getForeground());
        setText(value.toString());
    }
}
