package gov.takx.core.view.gui.maptools.elevationtools.contour;

import com.jgoodies.forms.layout.CellConstraints;
import com.jgoodies.forms.layout.FormLayout;
import com.jgoodies.forms.layout.RowSpec;
import gov.tak.api.annotation.IncubatingApi;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import javax.swing.JPanel;
import javax.swing.JScrollPane;

import java.awt.BorderLayout;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.function.Supplier;

/**
 * Effectively a {@code ListView<T>} where {@code T} is mapped to a {@link JPanel} via a function.
 * This differs from {@link javax.swing.JList} since the mapped panels are fully realized rather than painted ghosts.
 * <p>
 * This is not in {@code gov.takx.commons.swing} due to the current capability gap outlined in {@link #refresh()}.
 *
 * @param <T> Cell item type.
 *
 * @since 5.8
 */
@IncubatingApi(since = "5.8")
class VerticalCellListPanel<T> extends JPanel
{
    private static final RowSpec ROW_SPEC = RowSpec.decode("fill:pref");
    private static final String COLUMN_SPEC = "default:grow";

    private final JScrollPane scrollPane;
    private final JPanel contentPanel;
    private final Function<T, JPanel> itemMapper;
    private final List<T> items = new ArrayList<>();
    private final Map<T, JPanel> panelCache;
    private final CellConstraints cc = new CellConstraints();

    /**
     * @param itemMapper Mapping function to create panels for items.
     * @param mapSupplier Backing map provider for caching panels.
     */
    public VerticalCellListPanel(@Nonnull Function<T, JPanel> itemMapper, @Nonnull Supplier<Map<T, JPanel>> mapSupplier)
    {
        this.itemMapper = itemMapper;

        panelCache = mapSupplier.get();

        contentPanel = new JPanel(new FormLayout(COLUMN_SPEC, ""));
        scrollPane = new JScrollPane(contentPanel);

        setLayout(new BorderLayout());
        add(scrollPane, BorderLayout.CENTER);
    }

    /**
     * @return Scrollable viewport for cell items.
     */
    @Nonnull
    public JScrollPane getScrollPane()
    {
        return scrollPane;
    }

    /**
     * @return {@code true} when cells should register on-click 'selection' notifications.
     * @see #onSelect(JPanel, Object) 
     */
    protected boolean supportsSelection()
    {
        return false;
    }

    /**
     * Called when a cell is pressed. Intended to be overridden to support optional on-selection capabilities.
     * You will also need to override {@link #supportsSelection()} to return {@code true}.
     * @param panel Selected panel.
     * @param item Associated item.
     */
    protected void onSelect(@Nonnull JPanel panel, @Nonnull T item)
    {
        // no-op
    }

    /**
     * @param item Item to add.
     */
    public void addItem(@Nonnull T item)
    {
        items.add(item);
        refresh();
    }

    /**
     * @param item Item to add.
     * @param index Index to add the item at.
     * @throws IndexOutOfBoundsException When the index is {@code < 0 || >=} the current size.
     */
    public void addItem(@Nonnull T item, int index)
    {
        items.add(index, item);
        refresh();
    }

    /**
     * @param toAdd Items to add.
     */
    public void addItems(@Nonnull Collection<? extends T> toAdd)
    {
        items.addAll(toAdd);
        refresh();
    }

    /**
     * @param item Item to remove.
     */
    public void removeItem(@Nonnull T item)
    {
        if (items.remove(item))
        {
            panelCache.remove(item);
            refresh();
        }
    }

    /**
     * @param toRemove Items to remove.
     */
    public void removeItems(@Nonnull Collection<? extends T> toRemove)
    {
        if (items.removeAll(toRemove))
        {
            panelCache.keySet().removeAll(toRemove);
            refresh();
        }
    }

    /**
     * Clears all items.
     */
    public void clear()
    {
        items.clear();
        panelCache.clear();
        refresh();
    }

    /**
     * @param newItems Collection of new items to set, replacing all prior ones.
     */
    public void setItems(@Nonnull Collection<? extends T> newItems)
    {
        items.clear();
        items.addAll(newItems);
        panelCache.keySet().retainAll(newItems);

        refresh();
    }

    /**
     * @return Items in this cell list.
     */
    @Nonnull
    public List<T> getItems()
    {
        return Collections.unmodifiableList(items);
    }

    /**
     * @param item Item in this cell list.
     * @return Panel representation of the item.
     */
    @Nullable
    public JPanel getPanel(T item)
    {
        int i = items.indexOf(item);
        return (i < 0) ? null : getPanels().get(i);
    }

    /**
     * @return List of panels for all items in this cell list.
     */
    @Nonnull
    public List<JPanel> getPanels()
    {
        return Arrays.stream(contentPanel.getComponents())
                .filter(t -> t instanceof JPanel)
                .map(t -> (JPanel) t)
                .toList();
    }

    /**
     * Change the order of items in the cell list.
     * @param comparator Comparator for sorting.
     */
    public void sort(@Nonnull Comparator<? super T> comparator)
    {
        items.sort(comparator);
        refresh();
    }

    /**
     * Regenerate the UI.
     */
    protected void refresh()
    {
        contentPanel.removeAll();

        // Reset layout rows.
        FormLayout layout = (FormLayout) contentPanel.getLayout();
        while (layout.getRowCount() > 0) layout.removeRow(1);

        // Append rows for all items, recycling existing mapped panels when possible.
        for (T item : items)
        {
            JPanel itemPanel = panelCache.computeIfAbsent(item, key -> {
                JPanel p = itemMapper.apply(key);
                // TODO: If a child of 'p' has a mouse listener, it will interrupt the handling
                //  and prevent this selection listener from functioning.
                //   - GlassPane is a global hack
                //   - JLayeredPane with a invisible pane on top for recieving listeners should work...
                //     Have the JLayeredPane use borderlayout + a custom extension to fix the 'add' contract problem with that
                //     However, no matter what I've tried I cannot get the overlay on the layered pane to not be properly transparent
                if (supportsSelection()) p.addMouseListener(new MouseAdapter()
                {
                    @Override
                    public void mousePressed(MouseEvent e)
                    {
                        onSelect(p, key);
                    }
                });
                return p;
            });
            layout.appendRow(ROW_SPEC);
            contentPanel.add(itemPanel, cc.xy(1, layout.getRowCount()));
        }

        // Revalidate and repaint
        contentPanel.revalidate();
        contentPanel.repaint();
    }
}