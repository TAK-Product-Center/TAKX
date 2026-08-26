package gov.takx.commons.swing;

import org.jdesktop.swingx.JXTable;
import org.jdesktop.swingx.table.ColumnControlButton;
import org.jdesktop.swingx.table.ColumnControlPopup;

import javax.swing.JPopupMenu;

/**
 * A factory for creating a column control button that provides a scrollable popup menu (in place of the standard
 * popup menu) in case the number of menu entries can grow large enough to overflow the screen height.
 * <p/>
 * Unfortunately, due to the construction sequence of the {@link org.jdesktop.swingx.table.ColumnControlButton}, there
 * is no way to create a direct derived class that supports the specification of the maximum number of visible rows.
 * So, we use a factory pattern instead.
 *
 * @since 1.49
 */
@SuppressWarnings("UnusedDeclaration")
public class ScrollableColumnControlButtonFactory
{
    /**
     * The default number of visible rows in the scrolling popup menu.
     */
    public static final int DEFAULT_MAXIMUM_VISIBLE_ROWS = 15;

    protected final JXTable table;
    protected int maximumVisibleRows;

    /**
     * Construct on the given JXTable with the default number of visible rows.
     *
     * @param table Table on which to this control will operate
     */
    public ScrollableColumnControlButtonFactory(JXTable table)
    {
        this(table, DEFAULT_MAXIMUM_VISIBLE_ROWS);
    }

    /**
     * Construct on the given JXTable with the specified number of visible rows.
     *
     * @param table              Table on which to this control will operate
     * @param maximumVisibleRows max visible row count on the popup menu
     */
    public ScrollableColumnControlButtonFactory(JXTable table, int maximumVisibleRows)
    {
        this.table = table;
        this.maximumVisibleRows = maximumVisibleRows;
    }

    /**
     * @return Configured max visible row count
     */
    public int getMaximumVisibleRows()
    {
        return maximumVisibleRows;
    }

    /**
     * Set the max visible row count of the popup menu.  This will not affect any previously created control buttons,
     * it only affects control buttons created subsequently.
     *
     * @param maximumVisibleRows max row count
     */
    public void setMaximumVisibleRows(int maximumVisibleRows)
    {
        this.maximumVisibleRows = maximumVisibleRows;
    }

    /**
     * Create a new column control button using a scrollable popup menu with the configured max row count.
     *
     * @return newly created control button
     * @see #getMaximumVisibleRows()
     * @see #setMaximumVisibleRows(int)
     */
    public ColumnControlButton create()
    {
        return new ColumnControlButton(table)
        {
            /**
             * {@inheritDoc}
             * <p/>
             * Overridden to create a scrolling popup menu instead of a normal one.
             */
            @Override
            protected ColumnControlPopup createColumnControlPopup()
            {
                return new DefaultColumnControlPopup()
                {
                    private JScrollPopupMenu popupMenu = null;

                    @Override
                    protected JPopupMenu getPopupMenu()
                    {
                        if (popupMenu == null)
                        {
                            popupMenu = new JScrollPopupMenu(maximumVisibleRows);
                        }
                        return popupMenu;
                    }
                };
            }
        };
    }
}
