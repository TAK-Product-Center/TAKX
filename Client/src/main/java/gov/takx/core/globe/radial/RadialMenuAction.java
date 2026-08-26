package gov.takx.core.globe.radial;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import javax.swing.AbstractAction;
import javax.swing.Icon;
import javax.swing.JCheckBoxMenuItem;
import javax.swing.JMenuItem;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.function.Supplier;

/**
 * Custom {@link javax.swing.Action} specifically for use in radial menus.
 *
 * @since 5.3, extracted from {@link gov.takx.core.mapobject.gui.MapObjectPopupMenu}
 */
public class RadialMenuAction extends AbstractAction
{
    protected final ActionListener actionListener;
    protected final Supplier<Boolean> enabledSupplier;
    protected final Supplier<Boolean> stateSupplier;
    protected final String iconName;
    protected final String name;

    /**
     * Note that {@link Supplier}s are used so that evaluation is delayed until the menu item is actually added to a menu.
     *
     * @param name            Name to use for action
     * @param iconName        The path of the icon image, relative to the classpath
     * @param actionListener  Action listener
     * @param enabledSupplier Supplier determining if the menu item is enabled
     * @param stateSupplier   Supplier determining if the menu item has "state", resulting in a checkbox menu item
     */
    public RadialMenuAction(@Nullable String name, @Nullable String iconName, @Nullable ActionListener actionListener,
                            @Nonnull Supplier<Boolean> enabledSupplier, @Nullable Supplier<Boolean> stateSupplier)
    {
        super(name);

        this.actionListener = actionListener;
        this.enabledSupplier = enabledSupplier;
        this.stateSupplier = stateSupplier;
        this.name = name;
        this.iconName = iconName;

        setEnabled(enabled);
    }

    /**
     * @param icon The icon to display
     */
    public void setIcon(Icon icon)
    {
        putValue(SMALL_ICON, icon);
    }

    /**
     * @return Whether this action is selected using the {@code stateSupplier}
     */
    public boolean isSelected()
    {
        return stateSupplier != null ? stateSupplier.get() : false;
    }

    /**
     * @return Weather this action is enabled using the {@code enabledSupplier}
     */
    @Override
    public boolean isEnabled()
    {
        return enabledSupplier.get();
    }

    @Override
    public void actionPerformed(ActionEvent e)
    {
        if (actionListener != null) actionListener.actionPerformed(e);
    }

    /**
     * @return {@code true} if this action has state.
     */
    public boolean hasState()
    {
        return stateSupplier != null;
    }

    /**
     * Create the {@link JMenuItem} appropriate for this radial menu action.
     * <p/>
     * If the {@code stateSupplier} is non-null this will create a {@link JCheckBoxMenuItem}, otherwise
     * a {@link JMenuItem}. The menu item will be enabled according to the value from {@code enabledSupplier}.
     */
    @Nonnull
    public JMenuItem createMenuItem()
    {
        final JMenuItem menuItem;

        if (stateSupplier != null)
        {
            menuItem = new JCheckBoxMenuItem(this);
            menuItem.setSelected(stateSupplier.get());

            // For checkbox menu items, having an icon results in NOT having a checkmark, so it is difficult
            // for the user to tell which state the menu item is in.  So, we'll remove any icon.
            menuItem.setIcon(null);
        } else
        {
            menuItem = new JMenuItem(this);
        }

        menuItem.setEnabled(enabledSupplier.get());
        return menuItem;
    }

    /**
     * @return Name of an icon, either a URI or the name of an icon in platform.
     */
    @Nullable
    public String getIconName()
    {
        return iconName;
    }

    /**
     * @return Name of this action
     */
    @Nullable
    public String getName()
    {
        return name;
    }

    /**
     * @return {@code true} if this action has an action listener attached
     */
    public boolean hasActionListener()
    {
        return actionListener != null;
    }
}
