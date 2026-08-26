package gov.takx.api.ui;

import gov.takx.api.annotation.InvokeOnEDT;
import jakarta.annotation.Nonnull;

import javax.swing.Icon;
import javax.swing.JButton;
import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;

import java.awt.Component;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.util.function.Consumer;

import static gov.takx.api.ui.APopupButton.ButtonDecoration.Icon_Only;
import static gov.takx.api.ui.APopupButton.ButtonDecoration.Text_And_Icon;
import static gov.takx.api.ui.APopupButton.ButtonDecoration.Text_Only;

/**
 * Abstract pop-up button.
 *
 */
public abstract class APopupButton extends JButton implements MouseListener
{
    private final JPopupMenu menu = new JPopupMenu();
    private ButtonDecoration buttonDecoration = Text_And_Icon;
    private MenuOrientation menuOrientation = MenuOrientation.Up;
    private JMenuItem selectedMenuItem;
    private boolean dirty;

    /**
     * Button decorations.
     */
    public enum ButtonDecoration
    {
        Text_Only,
        Icon_Only,
        Text_And_Icon
    }

    /**
     * Pop-up menu orientations.
     */
    public enum MenuOrientation
    {
        Up,
        Down
    }

    /**
     * APopupButton constructor.
     *
     * @param toolTip tooltip to display when this button is moused over.
     */
    public APopupButton(String toolTip)
    {
        addMouseListener(this);
        setToolTipText(toolTip);
    }

    @InvokeOnEDT
    protected final void addMenuItem(final JMenuItem menuItem)
    {
        // Add an action listener to each menu item that gets added, so that we can properly set the decorations
        // for this button to make it display the menu item that was clicked.
        menuItem.addActionListener(e -> setSelectedMenuItem(menuItem));
        menu.add(menuItem);
    }

    public boolean isDirty()
    {
        return dirty;
    }

    /**
     * Show the pop-up menu.
     *
     * @param e MouseEvent
     */
    @Override
    public final void mouseClicked(MouseEvent e)
    {
        dirty = true;
        menu.setVisible(true);
        menu.show(this, 0, getMenuYPosition());
    }

    /**
     * Set the selected menu item.
     *
     * @param menuItem menu item.
     */
    public final void setSelectedMenuItem(@Nonnull JMenuItem menuItem)
    {
        selectedMenuItem = menuItem;
        setDecorationsFrom(menuItem);
    }

    /**
     * Set the currently selected menu item. Use the itemName to look up the component name.
     *
     * @param itemName name of the component to set selected
     */
    public final void setSelectedMenuItem(String itemName)
    {
        processMenuItem(itemName, this::setSelectedMenuItem);
    }

    /**
     * Process a menu item located by the given name.
     *
     * @param itemName Name of menu item to process
     * @param handler  Handler to apply to located menu item
     * @since 2.35
     */
    private void processMenuItem(@Nonnull String itemName, @Nonnull Consumer<JMenuItem> handler)
    {
        for (Component c : menu.getComponents())
        {
            if (c.getName().equals(itemName) && c instanceof JMenuItem)
            {
                handler.accept((JMenuItem) c);
                break;
            }
        }
    }

    /**
     * Set an icon on a JMenuItem component.
     *
     * @param itemName the component name to set the icon for, must not be {@code null}.
     * @param icon     icon to set
     */
    public final void setIcon(@Nonnull String itemName, Icon icon)
    {
        processMenuItem(itemName, menuItem -> menuItem.setIcon(icon));
    }

    /**
     * Set a tooltip on JMenuItem component.
     *
     * @param itemName the component name to set the icon for, must not be {@code null}.
     * @param tooltip  tooltip
     */
    public final void setToolTipText(@Nonnull String itemName, String tooltip)
    {
        processMenuItem(itemName, menuItem -> menuItem.setToolTipText(tooltip));
    }

    /**
     * Set the pop-up menu orientation. See {@link MenuOrientation} for orientations.
     *
     * @param menuOrientation menu orientation
     */
    public final void setMenuOrientation(MenuOrientation menuOrientation)
    {
        this.menuOrientation = menuOrientation;
    }

    /**
     * Set the button decoration. See {@link ButtonDecoration} for decorations.
     *
     * @param buttonDecoration button decoration
     */
    public final void setButtonDecoration(ButtonDecoration buttonDecoration)
    {
        this.buttonDecoration = buttonDecoration;

        if (selectedMenuItem != null) setDecorationsFrom(selectedMenuItem);
    }

    /**
     * Get the currently selected menu item. Can be {@code null}.
     *
     * @return JMenuItem
     */
    public final JMenuItem getSelectedMenuItem()
    {
        return selectedMenuItem;
    }

    /**
     * Get the Y position for the pop-up menu associated with this button.
     *
     * @return integer representing the pop-ups y location on the screen in relation to this button.
     */
    private int getMenuYPosition()
    {
        switch (menuOrientation)
        {
            case Down:
                return getHeight();
            case Up:
            default:
                return -menu.getHeight();
        }
    }

    /**
     * Set the decoration of this button. It will be decorated according to JMenuItem that was selected, and can
     * either have text and icon, only text or only an icon.
     *
     * @param menuItem JMenuItem to decorate this button from.
     */
    private void setDecorationsFrom(JMenuItem menuItem)
    {
        final String text = (buttonDecoration == Text_Only || buttonDecoration == Text_And_Icon)
                ? menuItem.getText()
                : null;

        final Icon icon = (buttonDecoration == Icon_Only || buttonDecoration == Text_And_Icon)
                ? menuItem.getIcon()
                : null;

        setIcon(icon);
        super.setText(text);
    }

    /**
     * Add an action listener to all of the JMenuItem components
     *
     * @param listener action listener to add.
     */
    public final void addMenuItemsActionListener(ActionListener listener)
    {
        for (Component c : menu.getComponents())
        {
            if (c instanceof JMenuItem)
            {
                ((JMenuItem) c).addActionListener(listener);
            }
        }
    }

    /**
     * Remove all of the menu items from the pop-up menu.
     */
    @InvokeOnEDT
    public final void removeAllMenuItems()
    {
        menu.removeAll();
    }

    @Override
    public final void setText(String text)
    {
        // don't let extending classes set the text, we want the text to either be nothing, or the text of the menu item
        // that was clicked.
    }

    @Override
    public final void mousePressed(MouseEvent e)
    {
    }

    @Override
    public final void mouseReleased(MouseEvent e)
    {
    }

    @Override
    public final void mouseEntered(MouseEvent e)
    {
    }

    @Override
    public final void mouseExited(MouseEvent e)
    {
    }
}
