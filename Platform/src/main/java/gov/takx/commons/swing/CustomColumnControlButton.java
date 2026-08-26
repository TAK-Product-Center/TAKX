package gov.takx.commons.swing;

import com.formdev.flatlaf.ui.FlatCheckBoxMenuItemUI;
import org.jdesktop.swingx.JXTable;
import org.jdesktop.swingx.action.AbstractActionExt;
import org.jdesktop.swingx.action.ActionContainerFactory;
import org.jdesktop.swingx.table.ColumnControlButton;
import org.jdesktop.swingx.table.ColumnControlPopup;

import javax.swing.JCheckBoxMenuItem;
import javax.swing.JPopupMenu;
import javax.swing.MenuSelectionManager;
import javax.swing.event.PopupMenuListener;

import java.awt.Component;
import java.util.Collections;
import java.util.List;

/**
 * A factory for creating a column control button that provides a scrollable popup menu (in place of the standard
 * popup menu) in case the number of menu entries can grow large enough to overflow the screen height.
 * <p>
 * Unfortunately, due to the construction sequence of the {@link org.jdesktop.swingx.table.ColumnControlButton}, there
 * is no way to create a direct derived class that supports the specification of the maximum number of visible rows.
 * So, we use a factory pattern instead.
 *
 * @since 2.3.1
 */
public class CustomColumnControlButton
{
    /**
     * The default number of visible rows in the scrolling popup menu.
     */
    private static final int DEFAULT_MAXIMUM_VISIBLE_ROWS = 15;

    private final ActionContainerFactory factory = new ActionContainerFactory(null);

    protected final JXTable table;
    private int maximumVisibleRows;

    /**
     * Construct on the given JXTable with the default number of visible rows.
     *
     * @param table Table on which to this control will operate
     */
    public CustomColumnControlButton(JXTable table)
    {
        this(table, DEFAULT_MAXIMUM_VISIBLE_ROWS);
    }

    /**
     * Construct on the given JXTable with the specified number of visible rows.
     *
     * @param table              Table on which to this control will operate
     * @param maximumVisibleRows max visible row count on the popup menu
     */
    public CustomColumnControlButton(JXTable table, int maximumVisibleRows)
    {
        this.table = table;
        this.maximumVisibleRows = maximumVisibleRows;
    }

    /**
     * Create a new column control button using a scrollable popup menu with the configured max row count.
     *
     * @return newly created control button
     */
    public ColumnControlButton create()
    {
        return create(null);
    }

    /**
     * Create a new column control button using a scrollable popup menu with the configured max row count.
     *
     * @param popupMenuListener A popup menu listener that is added to the {@link ColumnControlPopup} {@link JPopupMenu}
     *                          once it has been created.  If {@code null} is provided then no listener is added.
     * @return newly created control button
     * @since 2.29
     */
    public ColumnControlButton create(PopupMenuListener popupMenuListener)
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
                    private JPopupMenu popupMenu = null;
                    private List<? extends AbstractActionExt> actions = Collections.emptyList();
                    private boolean actionsChanged = false;

                    @Override
                    protected JPopupMenu getPopupMenu()
                    {
                        if (popupMenu == null)
                        {
                            // Customize the scrolling popup menu to postpone actual creation of the visibility action
                            // menu items until the menu is shown.  This dramatically improves performance when the
                            // column model is large (> 250 columns)
                            popupMenu = new JScrollPopupMenu(maximumVisibleRows)
                            {
                                @Override
                                public void show(Component invoker, int x, int y)
                                {
                                    // On first use, create and add all our menu items
                                    if (actionsChanged)
                                    {
                                        for (AbstractActionExt action : actions)
                                        {
                                            JCheckBoxMenuItem mi = (JCheckBoxMenuItem) factory.createMenuItem(action);
                                            mi.setUI(new MyCheckBoxMenuItemUI(mi));
                                            add(mi);
                                        }

                                        actionsChanged = false;
                                    }

                                    super.show(invoker, x, y);
                                }
                            };

                            if (popupMenuListener != null)
                            {
                                popupMenu.addPopupMenuListener(popupMenuListener);
                            }
                        }
                        return popupMenu;
                    }

                    /**
                     * Populating the scrolling popup menu with custom check box menu items
                     * that stay open when adding or removing columns
                     * <p/>
                     * Note: this method is always called with the complete list of visibility actions, so we can
                     * just keep a reference to the list instead of copying it.
                     */
                    @Override
                    public void addVisibilityActionItems(List<? extends AbstractActionExt> actions)
                    {
                        this.actions = actions;
                        actionsChanged = true;
                    }

                    @Override
                    public void removeAll()
                    {
                        actions = Collections.emptyList();
                        getPopupMenu().removeAll();
                    }
                };
            }
        };
    }

    /**
     * Custom CheckBoxMenuItemUI that stays open when selecting menuItems
     * and has check boxes to show which columns are visible
     */
    private static class MyCheckBoxMenuItemUI extends FlatCheckBoxMenuItemUI
    {
        private final JCheckBoxMenuItem menuItem;

        private MyCheckBoxMenuItemUI(JCheckBoxMenuItem menuItem)
        {
            this.menuItem = menuItem;
        }

        protected synchronized void doClick(MenuSelectionManager msm)
        {
            menuItem.doClick(0);
        }
    }
}
