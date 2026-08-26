package gov.takx.commons.swing.entity;

import com.google.common.annotations.VisibleForTesting;
import gov.tak.api.annotation.IncubatingApi;
import gov.takx.platform.ui.UIResources;
import jakarta.annotation.Nonnull;
import org.jdesktop.swingx.table.TableColumnExt;

import javax.swing.JButton;
import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;
import javax.swing.SortOrder;
import javax.swing.SwingConstants;
import javax.swing.UIManager;

import java.awt.Component;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.HashMap;
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
        var entityDefinition = table.getEntityDefinition();
        this.table.getRowSorter().addRowSorterListener(e -> update());
        addSortItem("Default", null);
        for (var column : entityDefinition.getColumns())
        {
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

    private void addSortItem(String name, EntityColumn<E, ?> column)
    {
        var menu = new JMenuItem(name);
        menuItemMap.put(column, menu);
        menu.addActionListener(e -> clickedSort(column));
        add(menu);
    }

    @VisibleForTesting
    void clickedSort(EntityColumn<E, ?> column)
    {
        if (column == null)
        {
            table.resetSortOrder();
        } else
        {
            table.toggleSortOrder(column);
        }
        update();
        setVisible(false);
    }

    @VisibleForTesting
    void update()
    {
        var sortColumn = table.getSortedColumn();
        var sortOrder = sortColumn != null ? table.getSortOrder(sortColumn.getIdentifier()) : SortOrder.ASCENDING;
        var icon = sortOrder == SortOrder.ASCENDING ? UIResources.SORT_ASCENDING_ICON : UIResources.SORT_DESCENDING_ICON;
        menuItemMap.forEach((col, item) -> {
            var tableColumn = col == null ? null : table.getColumn(col);
            if (tableColumn != null) item.setVisible(sortable(col, tableColumn));
            if (tableColumn == sortColumn)
            {
                item.setIcon(icon);
                button.setText(item.getText());
            } else
            {
                item.setIcon(null);
            }

        });
        button.setIcon(icon);
    }

    private boolean sortable(EntityColumn<?, ?> column, TableColumnExt tableColumn)
    {
        return !column.name().isEmpty() && column.sortable() && tableColumn.isSortable();
    }

    private void popupButtonMouseClicked()
    {
        var size = getPreferredSize();
        show(button, (button.getWidth() - size.width), button.getHeight());
    }

    private void initComponents()
    {
        // JFormDesigner - Component initialization - DO NOT MODIFY  //GEN-BEGIN:initComponents  @formatter:off
        button = new JButton();

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
                popupButtonMouseClicked();
            }
        });
        // JFormDesigner - End of component initialization  //GEN-END:initComponents  @formatter:on
    }

    // JFormDesigner - Variables declaration - DO NOT MODIFY  //GEN-BEGIN:variables  @formatter:off
    private JButton button;
    // JFormDesigner - End of variables declaration  //GEN-END:variables  @formatter:on
}
