package gov.takx.commons.swing;

import com.formdev.flatlaf.ui.FlatUIUtils;

import javax.swing.Action;
import javax.swing.JButton;
import javax.swing.JPopupMenu;
import javax.swing.JToolBar;
import javax.swing.UIManager;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Insets;
import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

import static gov.takx.commons.swing.KeyActionHandler.ESCAPE_KEY;

/**
 * Swing button with a drop-down menu for additional buttons. The drop-down menu can either be a static menu defined by
 * {@link Action}s or a dynamic menu provided by an {@link IPopupMenuProvider}.
 *
 * @see <a href="https://stackoverflow.com/a/66303093">Code from this Stack Overflow answer</a>
 * @since 3.3
 */
public class SplitButton extends JButton
{
    private static final int ARROW_SIZE = 8;
    private static final int ARROW_THICKNESS = 1;
    private static final int SPLIT_SIZE = 22;

    private static final String POPUP_MENU_CLOSE_ACTION_KEY = "PopupMenu.close";

    private boolean onSplit; // Whether the mouse is on the arrow portion of the button
    private Rectangle splitRectangle;
    private boolean alwaysDropDown;
    private MouseHandler mouseHandler;
    private JPopupMenu jpopupMenu;
    private IPopupMenuProvider popupMenuProvider;
    private boolean chevron; // Whether to draw a triangle or chevron for the arrow
    private Color backgroundColor;
    private Color shadowColor;
    private Color foregroundColor;
    private Color disabledForegroundColor;
    private ArrowLocation arrowLocation = ArrowLocation.RIGHT;

    /**
     * Constructor for a split button with no main action.
     */
    public SplitButton()
    {
        addMouseMotionListener(getMouseHandler());
        addMouseListener(getMouseHandler());
        setAlwaysDropDown(true); // If there is no main action, always show the drop-down.

        new KeyActionHandler(this, POPUP_MENU_CLOSE_ACTION_KEY, ESCAPE_KEY, WHEN_FOCUSED, () -> getPopupMenu().setVisible(false))
                .install();
    }

    /**
     * Overridden so relevant UI defaults can be captured if the skin is ever changed. Ideally, that would be handled in
     * a separate SplitButtonUI class, but the use of the split rectangle for both painting and determining if the 
     * dropdown should be shown makes it challenging to pull out that functionality right now.
     */
    @Override
    public void updateUI()
    {
        super.updateUI();
        String arrowType = UIManager.getString("Component.arrowType");
        chevron = FlatUIUtils.isChevron(arrowType);
        backgroundColor = UIManager.getLookAndFeelDefaults().getColor("Button.background");
        shadowColor = UIManager.getLookAndFeelDefaults().getColor("Button.shadow");
        foregroundColor = UIManager.getColor("Button.foreground");
        disabledForegroundColor = UIManager.getColor("Button.disabledText");
    }

    @Override
    public void addActionListener(ActionListener listener)
    {
        if (listener != null)
        {
            setAlwaysDropDown(false);
            super.addActionListener(listener);
        }

    }

    @Override
    public void setAction(Action mainAction)
    {
        super.setAction(mainAction);
        if (mainAction != null)
        {
            setAlwaysDropDown(false);
        }
    }

    /**
     * Adds an action at the specified index of the popup menu.
     * <p/>
     * Note: to include a separator in the popup menu, include {@link gov.takx.commons.swing.ButtonAction#SEPARATOR}.
     * 
     * @param action The action to add
     * @param index The index at which to add the action
     */
    public void addActionAt(Action action, int index)
    {
        final JPopupMenu popupMenu = getPopupMenu();
        if (action.equals(ButtonAction.SEPARATOR))
        {
            popupMenu.insert(new JToolBar.Separator(), index);
        } else
        {
            popupMenu.insert(action, index);
        }
    }

    /**
     * Adds an action at the end of the popup menu.
     * <p/>
     * Note: to include a separator in the popup menu, include {@link gov.takx.commons.swing.ButtonAction#SEPARATOR}.
     * 
     * @param action The action to add
     */
    public void addAction(Action action)
    {
        final JPopupMenu popupMenu = getPopupMenu();
        addActionAt(action, popupMenu.getComponentCount());
    }

    /**
     * Sets the location for the drop-down menu arrow. The default is right.
     *
     * @param arrowLocation The location of the arrow
     */
    public void setArrowLocation(ArrowLocation arrowLocation)
    {
        this.arrowLocation = arrowLocation;
    }

    /**
     * Sets a static popup menu that will be used each time the button is clicked. To use a dynamic popup menu that can
     * be updated on each button click, call {@link #setPopupMenuProvider(IPopupMenuProvider)} instead.
     * 
     * @param popupMenu The static popup menu
     */
    @SuppressWarnings("unused")
    public void setPopupMenu(JPopupMenu popupMenu)
    {
        jpopupMenu = popupMenu;
        popupMenuProvider = null; // Only allow the popup menu to be set statically or provided dynamically, not both
        setComponentPopupMenu(popupMenu);
    }

    /**
     * Sets a provider for a dynamic popup menu that can be updated each time the button is clicked. To use a static
     * popup menu, call {@link #setPopupMenu(JPopupMenu)} instead.
     * 
     * @param popupMenuProvider The provider for a dynamic popup menu
     */
    public void setPopupMenuProvider(IPopupMenuProvider popupMenuProvider)
    {
        this.popupMenuProvider = popupMenuProvider;
        jpopupMenu = null; // Only allow the popup menu to be set statically or provided dynamically, not both

        // Even though the menu will be provided dynamically each time, go ahead and set it now due to the side effect in
        // the parent of enabling various events.
        setComponentPopupMenu(popupMenuProvider.getPopupMenu());
    }

    /**
     * @return the button's popup menu.
     */
    public JPopupMenu getPopupMenu()
    {
        // If there is a provider, always get the popup menu dynamically
        if (popupMenuProvider != null) return popupMenuProvider.getPopupMenu();

        if (jpopupMenu == null)
        {
            jpopupMenu = new JPopupMenu();
        }

        return jpopupMenu;
    }

    protected MouseHandler getMouseHandler()
    {
        if (mouseHandler == null)
        {
            mouseHandler = new MouseHandler();
        }

        return mouseHandler;
    }

    /**
     * @return The number of items in the drop-down menu.
     */
    protected int getOptionsCount()
    {
        return getPopupMenu().getComponentCount();
    }

    @Override
    public Insets getInsets()
    {
        Insets insets = (Insets) super.getInsets().clone();
        if (arrowLocation == ArrowLocation.RIGHT)
        {
            insets.right += SPLIT_SIZE;
        } else
        {
            insets.bottom += SPLIT_SIZE;
        }

        return insets;
    }

    @Override
    public Insets getInsets(Insets insets)
    {
        Insets insets1 = getInsets();
        insets.left = insets1.left;
        insets.right = insets1.right;
        insets.bottom = insets1.bottom;
        insets.top = insets1.top;
        return insets1;
    }

    protected void showPopupMenu()
    {
        if (getOptionsCount() > 0)
        {
            final JPopupMenu popupMenu = getPopupMenu();
            popupMenu.setVisible(true); // Necessary to calculate pop-up menu width the first time it's displayed.

            if (arrowLocation == ArrowLocation.RIGHT)
            {
                // This aligns the right edge of the menu to the right edge of the button so it appears directly below
                // the arrow portion of the button
                popupMenu.show(this, (getWidth() - popupMenu.getWidth()), getHeight());
            } else
            {
                // When the arrow is on the bottom, it makes more sense to align the left edge of the menu to the left
                // edge of the button
                popupMenu.show(this, 0, getHeight());
            }
        }
    }

    /**
     * Set whether the dropdown menu should always be shown, even if the non-arrow portion of the button is clicked.
     * <p/>
     * If {@code true}, this will prevent the button from raising any actionPerformed events for itself.
     *
     * @param alwaysDropDown {@code true} to show the attached dropdown even if the main button part is clicked, {@code false} otherwise
     */
    public void setAlwaysDropDown(boolean alwaysDropDown)
    {
        this.alwaysDropDown = alwaysDropDown;
    }

    @Override
    protected void paintComponent(Graphics g)
    {
        super.paintComponent(g);
        Color oldColor = g.getColor();

        // Determine the area of the drop-down button
        if (arrowLocation == ArrowLocation.RIGHT)
        {
            splitRectangle = new Rectangle(getWidth() - SPLIT_SIZE, 0, SPLIT_SIZE, getHeight());
        } else
        {
            splitRectangle = new Rectangle(0, getHeight() - SPLIT_SIZE, getWidth(), SPLIT_SIZE);
        }

        g.translate(splitRectangle.x, splitRectangle.y); // Update the graphics to be relative to this area

        // Paint arrow
        g.setColor(getArrowColor());
        if (arrowLocation == ArrowLocation.RIGHT)
        {
            FlatUIUtils.paintArrow((Graphics2D) g, 0, 0, SPLIT_SIZE, getHeight(), SOUTH, chevron,
                    ARROW_SIZE, ARROW_THICKNESS, 0, 0);
        } else
        {
            FlatUIUtils.paintArrow((Graphics2D) g, 0, 0, getWidth(), SPLIT_SIZE, SOUTH, chevron,
                    ARROW_SIZE, ARROW_THICKNESS, 0, 0);
        }

        if (!alwaysDropDown)
        {
            if (getModel().isRollover() || isFocusable())
            {
                g.setColor(backgroundColor);
                int separatorSpacing = 4;
                if (arrowLocation == ArrowLocation.RIGHT)
                {
                    g.drawLine(1, separatorSpacing + 2, 1, getHeight() - separatorSpacing - 2);
                    g.setColor(shadowColor);
                    g.drawLine(2, separatorSpacing + 2, 2, getHeight() - separatorSpacing - 2);
                } else
                {
                    g.drawLine(separatorSpacing + 2, 1, getWidth() - separatorSpacing - 2, 1);
                    g.setColor(shadowColor);
                    g.drawLine(separatorSpacing + 2, 2, getWidth() - separatorSpacing - 2, 2);
                }
            }
        }

        // Put the graphics back the way it was
        g.setColor(oldColor);
        g.translate(-splitRectangle.x, -splitRectangle.y);
    }

    /**
     * @return the color used to paint the arrow.
     */
    protected Color getArrowColor()
    {
        return isEnabled() ? foregroundColor : disabledForegroundColor;
    }

    @Override
    protected void fireActionPerformed(ActionEvent event)
    {
        // This is a little bit of a nasty trick.  Basically this is where we try and decide if the buttons "main" 
        // action should be fired or not.  We don't want it firing if the button has no main action (and always
        // displays the drop-down menu) or the user clicked on the drop-down arrow.
        if (onSplit || alwaysDropDown)
        {
            showPopupMenu();
        } else
        {
            super.fireActionPerformed(event);
        }
    }

    /**
     * Mouse handler that is used to track whether the mouse is over the arrow portion of the button.
     */
    protected class MouseHandler extends MouseAdapter
    {
        @Override
        public void mouseExited(MouseEvent e)
        {
            if (splitRectangle != null)
            {
                onSplit = false;
                repaint(splitRectangle);
            }
        }

        @Override
        public void mouseMoved(MouseEvent e)
        {
            if (splitRectangle != null)
            {
                onSplit = splitRectangle.contains(e.getPoint());
                repaint(splitRectangle);
            }
        }
    }

    /**
     * Represents the possible locations for the drop-down menu arrow.
     */
    public enum ArrowLocation
    {
        RIGHT,
        BOTTOM
    }
}
