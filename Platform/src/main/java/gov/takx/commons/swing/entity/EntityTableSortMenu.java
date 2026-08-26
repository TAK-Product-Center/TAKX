package gov.takx.commons.swing.entity;

import com.google.common.annotations.VisibleForTesting;
import gov.tak.api.annotation.IncubatingApi;
import gov.takx.platform.ui.UIResources;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import org.jdesktop.swingx.table.TableColumnExt;

import javax.swing.JButton;
import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;
import javax.swing.SortOrder;
import javax.swing.SwingConstants;
import javax.swing.UIManager;
import javax.swing.event.RowSorterEvent;

import java.awt.Component;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * A popup menu that lists potential sort columns for an {@link EntityTable}. This class also manages the state of a
 * {@link JButton} that can be used to display the sort state as well as open the popup menu. The sort column list always
 * begins with the item "Default", which resets sorting in the table, allowing the user to see the original order of the
 * underlying table model.
 *
 * @apiNote {@link #setTable(EntityTable)}  must be called before this popup and button can be used.
 * @param <E> the entity type
 * @since 5.7
 */
@IncubatingApi(since = "5.7")
public class EntityTableSortMenu<E> extends JPopupMenu
{
    @VisibleForTesting
    final Map<EntityColumn<E, ?>, JMenuItem> menuItemMap = new HashMap<>();
    private final List<JMenuItem> sortableMenuItems = new ArrayList<>();
    private final EntityColumn<E, ?> defaultSortEntityColumn = EntityColumn.<E, Object>builder().build(); // To avoid null map key
    private boolean defaultSortVisible;
    private EntityTable<E> table;

    /**
     * Construct a new empty popup menu with no column options.
     */
    public EntityTableSortMenu()
    {
        initComponents();
    }

    @Override
    public void show(@Nonnull Component invoker, int x, int y)
    {
        update();
        super.show(invoker, x, y);
    }

    /**
     * Set's the table to derive an entity definition, and trigger sorting.
     *
     * @param table the entity table to sort
     */
    public void setTable(@Nonnull EntityTable<E> table)
    {
        this.table = table;
        addSortItem(defaultSortItem, defaultSortEntityColumn);
        var entityDefinition = table.getEntityDefinition();
        table.getRowSorter().addRowSorterListener(e -> {
            if (e.getType() == RowSorterEvent.Type.SORT_ORDER_CHANGED) update();
        });
        for (var column : entityDefinition.getColumns())
        {
            table.requireColumn(column).addPropertyChangeListener(e -> {
                if (e.getPropertyName().equals("sortable")) update();
            });
            addSortItem(column.name(), column);
        }
        update();
    }

    /**
     * @return A button that displays the sort state and triggers the popup to open
     */
    @Nonnull
    public JButton getButton()
    {
        return button;
    }

    /**
     * @param visible {@code true} if the 'Default' sort option should be visible, otherwise {@code false}. Default is {@code false}.
     * @since 5.8
     */
    public void setDefaultSortVisible(boolean visible)
    {
        defaultSortVisible = visible;
        update();
    }

    /**
     * @return a List of all entity columns that this menu can sort on
     * @since 5.8
     */
    @Nonnull
    public List<JMenuItem> getSortableMenuItems()
    {
        return List.copyOf(sortableMenuItems);
    }

    private void addSortItem(String name, EntityColumn<E, ?> column)
    {
        addSortItem(new JMenuItem(name), column);
    }

    private void addSortItem(JMenuItem menu, EntityColumn<E, ?> column)
    {
        menuItemMap.put(column, menu);
        menu.addActionListener(e -> sort(column));
        add(menu);
    }

    @VisibleForTesting
    void sort(EntityColumn<E, ?> column)
    {
        // Both of these will call update() from our RowSorterListener
        if (column == defaultSortEntityColumn)
        {
            table.resetSortOrder();
        } else
        {
            table.toggleSortOrder(column);
        }
        setVisible(false);
    }

    @VisibleForTesting
    void update()
    {
        var sortColumn = (TableColumnExt) table.getSortedColumn();
        var sortOrder = sortColumn != null ? table.getSortOrder(sortColumn.getIdentifier()) : null;
        var icon = sortOrder == SortOrder.DESCENDING ? UIResources.SORT_DESCENDING_ICON : UIResources.SORT_ASCENDING_ICON;
        button.setText("Sort");
        button.setIcon(icon);
        sortableMenuItems.clear();
        menuItemMap.forEach((entityColumn, menuItem) -> {
            var tableColumn = entityColumn == defaultSortEntityColumn ? null : table.getColumn(entityColumn);
            var sortable = sortable(entityColumn, tableColumn);
            menuItem.setVisible(sortable);
            if (sortable) sortableMenuItems.add(menuItem);
            if (tableColumn == sortColumn && menuItem.isVisible())
            {
                menuItem.setIcon(icon);
                button.setText(menuItem.getText());
            } else
            {
                menuItem.setIcon(null);
            }
        });
    }

    private boolean sortable(@Nonnull EntityColumn<?, ?> column, @Nullable TableColumnExt tableColumn)
    {
        return (column == defaultSortEntityColumn && defaultSortVisible) || tableColumn != null && !column.name().isEmpty() && column.sortable() && tableColumn.isSortable();
    }

    private void popupButtonMouseClicked(MouseEvent e)
    {
        // This is a quick way to tell if we clicked the icon. As long as we don't mess with the internal button UI then it should work fine
        if (e.getX() > button.getWidth() - button.getIcon().getIconWidth() - button.getInsets().right)
        {
            // We clicked the icon, so just toggle the order
            var column = table.getSortedColumn();
            // We can only do this for an actual column. Default sort is not reversible.
            if (column != null) table.toggleSortOrder(column.getIdentifier());
        } else
        {
            var size = getPreferredSize();
            show(button, (button.getWidth() - size.width), button.getHeight());
        }
    }

    private void initComponents()
    {
        // JFormDesigner - Component initialization - DO NOT MODIFY  //GEN-BEGIN:initComponents  @formatter:off
        button = new JButton();
        defaultSortItem = new JMenuItem();

        //======== this ========
        setName("this");

        //---- button ----
        button.setOpaque(false);
        button.setBorderPainted(false);
        button.setBackground(UIManager.getColor("TableHeader.background"));
        button.setToolTipText("Select sort column");
        button.setHorizontalTextPosition(SwingConstants.LEADING);
        button.setName("button");
        button.addMouseListener(new MouseAdapter()
        {
            @Override
            public void mouseClicked(MouseEvent e)
            {
                popupButtonMouseClicked(e);
            }
        });

        //---- defaultSortItem ----
        defaultSortItem.setText("Default");
        defaultSortItem.setVisible(false);
        defaultSortItem.setName("defaultSortItem");
        // JFormDesigner - End of component initialization  //GEN-END:initComponents  @formatter:on
    }

    // JFormDesigner - Variables declaration - DO NOT MODIFY  //GEN-BEGIN:variables  @formatter:off
    private JButton button;
    private JMenuItem defaultSortItem;
    // JFormDesigner - End of variables declaration  //GEN-END:variables  @formatter:on
}
