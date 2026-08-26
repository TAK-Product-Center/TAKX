package gov.takx.commons.swing;

import javax.swing.JPopupMenu;
import javax.swing.JScrollBar;

import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Insets;
import java.awt.LayoutManager;
import java.awt.event.AdjustmentEvent;
import java.awt.event.AdjustmentListener;
import java.awt.event.MouseWheelEvent;
import java.awt.event.MouseWheelListener;

/**
 * Drop in replacement for any {@link JPopupMenu} that adds a vertical scrollbar when the number
 * of items exceeds a configured maximum, which defaults to {@link #DEFAULT_MAX_ROW_COUNT}.
 * <p/>
 * Mostly taken from a post on stack overflow, here. http://stackoverflow.com/questions/9288350/adding-vertical-scroll-to-a-jpopupmenu
 * A few fixes have been made to the original.
 *
 * @see #JScrollPopupMenu(int)
 * @see #setMaximumVisibleRows(int)
 * @since 1.49
 */
@SuppressWarnings("UnusedDeclaration")
public class JScrollPopupMenu extends JPopupMenu
{
    public static final int DEFAULT_MAX_ROW_COUNT = 10;

    protected int maximumVisibleRows = DEFAULT_MAX_ROW_COUNT;
    protected JScrollBar popupScrollBar;

    /**
     * Constructs a {@code JScrollPopupMenu} with no title and the default max rows of {@link #DEFAULT_MAX_ROW_COUNT}.
     */
    public JScrollPopupMenu()
    {
        this(null);
    }

    /**
     * Constructs a {@code JScrollPopupMenu} with no title and the specified max row count.
     *
     * @param maximumVisibleRows Max number of rows to display before showing the scroll bar
     */
    public JScrollPopupMenu(int maximumVisibleRows)
    {
        this(null, maximumVisibleRows);
    }

    /**
     * Constructs a {@code JScrollPopupMenu} with the specified title and the default max rows of {@link #DEFAULT_MAX_ROW_COUNT}.
     *
     * @param label the string that a UI may use to display as a title for the popup menu.
     */
    public JScrollPopupMenu(String label)
    {
        this(label, DEFAULT_MAX_ROW_COUNT);
    }

    /**
     * Constructs a {@code JScrollPopupMenu} with the specified title and max row count.
     *
     * @param label              the string that a UI may use to display as a title for the popup menu.
     * @param maximumVisibleRows Max number of rows to display before showing the scroll bar
     */
    public JScrollPopupMenu(String label, int maximumVisibleRows)
    {
        super(label);

        this.maximumVisibleRows = maximumVisibleRows;

        setLayout(new ScrollPopupMenuLayout());

        super.add(getScrollBar());
        addMouseWheelListener(new MouseWheelListener()
        {
            @Override
            public void mouseWheelMoved(MouseWheelEvent event)
            {
                JScrollBar scrollBar = getScrollBar();
                int amount = (event.getScrollType() == MouseWheelEvent.WHEEL_UNIT_SCROLL)
                        ? event.getUnitsToScroll() * scrollBar.getUnitIncrement()
                        : (event.getWheelRotation() < 0 ? -1 : 1) * scrollBar.getBlockIncrement();

                scrollBar.setValue(scrollBar.getValue() + amount);
                event.consume();
            }
        });
    }

    /**
     * @return Get the vertical scroll bar, lazily created
     */
    protected JScrollBar getScrollBar()
    {
        if (popupScrollBar == null)
        {
            popupScrollBar = new JScrollBar(JScrollBar.VERTICAL);
            popupScrollBar.addAdjustmentListener(new AdjustmentListener()
            {
                @Override
                public void adjustmentValueChanged(AdjustmentEvent e)
                {
                    doLayout();
                    repaint();
                }
            });

            popupScrollBar.setVisible(false);
        }

        return popupScrollBar;
    }

    /**
     * @return Configured max visible row count
     */
    public int getMaximumVisibleRows()
    {
        return maximumVisibleRows;
    }

    /**
     * Set the max visible row count.  If there are more items added to the menu than this limit, a vertical scroll
     * bar will be displayed.
     *
     * @param maximumVisibleRows max row count
     */
    public void setMaximumVisibleRows(int maximumVisibleRows)
    {
        this.maximumVisibleRows = maximumVisibleRows;
    }

    @Override
    public void paintChildren(Graphics g)
    {
        Insets insets = getInsets();
        g.clipRect(insets.left, insets.top, getWidth(), getHeight() - insets.top - insets.bottom);
        super.paintChildren(g);
    }

    @Override
    protected void addImpl(Component comp, Object constraints, int index)
    {
        super.addImpl(comp, constraints, index);

        if (maximumVisibleRows < getComponentCount() - 1)
        {
            getScrollBar().setVisible(true);
        }
    }

    /**
     * {@inheritDoc}
     * <p/>
     * Overridden to account for special 0th component (our scrollbar).
     */
    @Override
    public void remove(int index)
    {
        ++index; // can't remove the scrollbar

        super.remove(index);

        if (maximumVisibleRows >= getComponentCount() - 1)
        {
            getScrollBar().setVisible(false);
        }
    }

    /**
     * {@inheritDoc}
     * <p/>
     * Overridden to replace the scrollbar after removing all other items.
     */
    @Override
    public void removeAll()
    {
        super.removeAll();
        add(getScrollBar()); // reinstate the scroll bar

        getScrollBar().setVisible(false);
    }

    @Override
    public void show(Component invoker, int x, int y)
    {
        JScrollBar scrollBar = getScrollBar();
        if (scrollBar.isVisible())
        {
            int extent = 0;
            int max = 0;
            int i = 0;
            int unit = -1;
            int width = 0;
            for (Component comp : getComponents())
            {
                if (!(comp instanceof JScrollBar))
                {
                    Dimension preferredSize = comp.getPreferredSize();
                    width = Math.max(width, preferredSize.width);
                    if (unit < 0)
                    {
                        unit = preferredSize.height;
                    }
                    if (i++ < maximumVisibleRows)
                    {
                        extent += preferredSize.height;
                    }
                    max += preferredSize.height;
                }
            }

            Insets insets = getInsets();
            int widthMargin = insets.left + insets.right;
            int heightMargin = insets.top + insets.bottom;
            scrollBar.setUnitIncrement(unit);
            scrollBar.setBlockIncrement(extent);
            scrollBar.setValues(0, heightMargin + extent, 0, heightMargin + max);

            width += scrollBar.getPreferredSize().width + widthMargin;
            int height = heightMargin + extent;

            setPopupSize(new Dimension(width, height));
        }

        super.show(invoker, x, y);
    }

    /**
     * Inner class to handle the layout of the scrollbar and added components.
     */
    protected static class ScrollPopupMenuLayout implements LayoutManager
    {
        @Override
        public void addLayoutComponent(String name, Component comp)
        {
        }

        @Override
        public void removeLayoutComponent(Component comp)
        {
        }

        @Override
        public Dimension preferredLayoutSize(Container parent)
        {
            int visibleAmount = Integer.MAX_VALUE;
            Dimension dim = new Dimension();
            for (Component comp : parent.getComponents())
            {
                if (comp.isVisible())
                {
                    if (comp instanceof JScrollBar)
                    {
                        JScrollBar scrollBar = (JScrollBar) comp;
                        visibleAmount = scrollBar.getVisibleAmount();
                    } else
                    {
                        Dimension pref = comp.getPreferredSize();
                        dim.width = Math.max(dim.width, pref.width);
                        dim.height += pref.height;
                    }
                }
            }

            Insets insets = parent.getInsets();
            dim.height = Math.min(dim.height + insets.top + insets.bottom, visibleAmount);

            return dim;
        }

        @Override
        public Dimension minimumLayoutSize(Container parent)
        {
            int visibleAmount = Integer.MAX_VALUE;
            Dimension dim = new Dimension();
            for (Component comp : parent.getComponents())
            {
                if (comp.isVisible())
                {
                    if (comp instanceof JScrollBar)
                    {
                        JScrollBar scrollBar = (JScrollBar) comp;
                        visibleAmount = scrollBar.getVisibleAmount();
                    } else
                    {
                        Dimension min = comp.getMinimumSize();
                        dim.width = Math.max(dim.width, min.width);
                        dim.height += min.height;
                    }
                }
            }

            Insets insets = parent.getInsets();
            dim.height = Math.min(dim.height + insets.top + insets.bottom, visibleAmount);

            return dim;
        }

        @Override
        public void layoutContainer(Container parent)
        {
            Insets insets = parent.getInsets();

            int width = parent.getWidth() - insets.left - insets.right;
            int height = parent.getHeight() - insets.top - insets.bottom;

            int x = insets.left;
            int y = insets.top;
            int position = 0;

            for (Component comp : parent.getComponents())
            {
                if ((comp instanceof JScrollBar) && comp.isVisible())
                {
                    JScrollBar scrollBar = (JScrollBar) comp;
                    Dimension dim = scrollBar.getPreferredSize();
                    scrollBar.setBounds(x + width - dim.width, y, dim.width, height);
                    width -= dim.width;
                    position = scrollBar.getValue();
                    break;
                }
            }

            y -= position;
            for (Component comp : parent.getComponents())
            {
                if (!(comp instanceof JScrollBar) && comp.isVisible())
                {
                    Dimension pref = comp.getPreferredSize();
                    comp.setBounds(x, y, width, pref.height);
                    y += pref.height;
                }
            }
        }
    }
}
