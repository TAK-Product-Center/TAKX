package gov.takx.core.globe.radial;

import gov.tak.platform.widgets.AbstractButtonWidget;
import gov.tak.platform.widgets.MapMenuButtonWidget;
import jakarta.annotation.Nonnull;

import javax.swing.Action;
import javax.swing.JCheckBoxMenuItem;
import javax.swing.JMenu;
import javax.swing.JMenuItem;

import java.awt.event.ActionListener;
import java.util.List;
import java.util.function.Supplier;

/**
 * Provides operations useful in creating radial menus, their menu items, and actions.
 * 
 * @since 5.3
 */
public class RadialMenuHelper
{
    /**
     * Create a {@link RadialMenuAction} from a given {@link JMenuItem}.  The constructed action will
     * have an action listener that invokes all the action listeners registered on the source menu item.
     *
     * @param source Source menu item
     * @return new RadialMenuAction
     */
    public static RadialMenuAction createRadialMenuAction(JMenuItem source)
    {
        // Collect all the current action listeners in order to call them when our new action is invoked.
        // This mechanism handles both an Action having been set on the menu item and any additional action
        // listeners it may have.  Note that any action set is also added to the action listeners, so all we need
        // to process is the action listener list. This removes the requirement for the plugin to have called
        // setAction on the menu item.

        final ActionListener mainActionListener = e -> List.of(source.getActionListeners()).forEach(l -> l.actionPerformed(e));
        final ActionListener actionListener;
        final Supplier<Boolean> stateSupplier;

        if (source instanceof JCheckBoxMenuItem)
        {
            stateSupplier = source::isSelected;

            actionListener = e -> {
                mainActionListener.actionPerformed(e);

                // Keep the source menu item's selected state updated as the menu action is invoked.
                //
                // There are two different possible sources for the event depending on whether the
                // radial menu was used or the popup menu from the explorer.

                final Object eventSource = e.getSource();
                if (eventSource instanceof MapMenuButtonWidget menuButtonWidget)
                {
                    source.setSelected((menuButtonWidget.getState() & AbstractButtonWidget.STATE_SELECTED) != 0);
                } else if (eventSource instanceof JCheckBoxMenuItem checkBoxMenuItem)
                {
                    source.setSelected(checkBoxMenuItem.isSelected());
                }
            };
        } else
        {
            stateSupplier = null;
            actionListener = mainActionListener;
        }

        final var menuAction = new RadialMenuAction(source.getText(), null, actionListener, source::isEnabled, stateSupplier);

        menuAction.setIcon(source.getIcon());

        return menuAction;
    }

    /**
     * Create a radial menu item from a given {@link Action}.
     *
     * @param action The action from which to create the menu item
     */
    @Nonnull
    public static JMenuItem createRadialMenuItem(@Nonnull Action action)
    {
        return createRadialMenuItem(new JMenuItem(action));
    }

    /**
     * Create a radial menu item from a given {@code menuItem}. {@link JMenuItem} and {@link JMenu} entries are converted to
     * {@link RadialMenuAction} or {@link NestedRadialMenuAction} respectively, copying data from the source as needed, so that
     * they are handled correctly during radial menu creation.
     *
     * @param menuItem The source menu item
     */
    @Nonnull
    public static JMenuItem createRadialMenuItem(@Nonnull JMenuItem menuItem)
    {
        // Test JMenu first because it extends JMenuItem but is handled differently
        return menuItem instanceof JMenu menu
                ? NestedRadialMenuAction.nestedMenuActionFromMenu(menu).createMenuItem()
                : createRadialMenuAction(menuItem).createMenuItem();
    }
}
