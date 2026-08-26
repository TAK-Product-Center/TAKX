package gov.takx.core.globe.radial;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import javax.swing.JMenu;
import javax.swing.JMenuItem;

import java.awt.event.ActionListener;
import java.util.List;
import java.util.function.Supplier;
import java.util.stream.Stream;

/**
 * Custom action for radial menus with nested submenus.
 *
 * @since 5.3, extracted from {@link gov.takx.core.mapobject.gui.MapObjectPopupMenu}
 */
public class NestedRadialMenuAction extends RadialMenuAction
{
    private final List<RadialMenuAction> subMenus;

    /**
     * Create a {@link NestedRadialMenuAction} from a given {@link JMenu} source.  If the given source menu has an
     * {@link JMenu#getAction() action} defined, then the constructed action will trigger that action instead of opening
     * the submenu. The submenu can still be opened by a long press event. All the menu items within the given menu will
     * be added as submenu items.
     *
     * @param source Source menu
     * @return new NestedMenuAction
     */
    public static NestedRadialMenuAction nestedMenuActionFromMenu(@Nonnull JMenu source)
    {
        final var subMenus = Stream.of(source.getMenuComponents())
                .filter(JMenuItem.class::isInstance)
                .map(JMenuItem.class::cast)
                .map(menuItem -> menuItem instanceof JMenu menu
                        ? nestedMenuActionFromMenu(menu)
                        : RadialMenuHelper.createRadialMenuAction(menuItem))
                .toList();

        final var menuAction = new NestedRadialMenuAction(source.getText(), null, source.getAction(), source::isEnabled, subMenus);
        menuAction.setIcon(source.getIcon());

        return menuAction;
    }

    /**
     * Note that {@link Supplier}s are used so that evaluation is delayed until the menu item is actually added to a menu.
     *
     * @param name            Name to use for action
     * @param iconName        The path of the icon image, relative to the classpath
     * @param actionListener  Action listener
     * @param enabledSupplier Supplier determining if the menu item is enabled
     * @param subMenus        The submenus that are displayed when this menu action is selected
     */
    public NestedRadialMenuAction(@Nullable String name, @Nullable String iconName, @Nullable ActionListener actionListener,
                                  @Nonnull Supplier<Boolean> enabledSupplier, @Nonnull List<RadialMenuAction> subMenus)
    {
        super(name, iconName, actionListener, enabledSupplier, null);
        this.subMenus = subMenus;
    }

    /**
     * Create the {@link JMenuItem} appropriate for this menu action, in this case a {@link JMenu}, along with its sub-menu items.
     * <p/>
     * The menu item will be enabled according to the value from {@code enabledSupplier}.
     */
    @Nonnull
    @Override
    public JMenuItem createMenuItem()
    {
        final var menu = new JMenu(this);
        menu.setEnabled(enabledSupplier.get());

        subMenus.stream().map(RadialMenuAction::createMenuItem).forEach(menu::add);

        return menu;
    }
}
