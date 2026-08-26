package gov.takx.core.view.gui.toolbar;

import com.formdev.flatlaf.ui.FlatArrowButton;
import com.formdev.flatlaf.ui.FlatStylingSupport;
import com.formdev.flatlaf.ui.FlatUIUtils;
import com.formdev.flatlaf.util.Animator;
import com.formdev.flatlaf.util.CubicBezierEasing;
import com.formdev.flatlaf.util.UIScale;

import javax.swing.AbstractButton;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;
import javax.swing.JViewport;
import javax.swing.SwingUtilities;
import javax.swing.Timer;
import javax.swing.UIManager;
import javax.swing.event.PopupMenuEvent;
import javax.swing.event.PopupMenuListener;
import javax.swing.plaf.ComponentUI;

import java.awt.Color;
import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.EventQueue;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Insets;
import java.awt.LayoutManager;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ComponentEvent;
import java.awt.event.ComponentListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.awt.event.MouseWheelEvent;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.Locale;
import java.util.Map;

import static com.formdev.flatlaf.FlatClientProperties.BUTTON_TYPE;
import static com.formdev.flatlaf.FlatClientProperties.BUTTON_TYPE_TOOLBAR_BUTTON;
import static com.formdev.flatlaf.FlatClientProperties.STYLE;
import static com.formdev.flatlaf.FlatClientProperties.STYLE_CLASS;
import static com.formdev.flatlaf.util.UIScale.scale;
import static gov.takx.core.view.gui.toolbar.ScrollableToolbarProperties.TOOL_BAR_MORE_TOOLS_BUTTON_TOOL_TIP;
import static gov.takx.core.view.gui.toolbar.ScrollableToolbarProperties.TOOL_BAR_PLACEMENT_TRAILING;
import static gov.takx.core.view.gui.toolbar.ScrollableToolbarProperties.TOOL_BAR_POLICY_AS_NEEDED;
import static gov.takx.core.view.gui.toolbar.ScrollableToolbarProperties.TOOL_BAR_POLICY_NEVER;
import static gov.takx.core.view.gui.toolbar.ScrollableToolbarProperties.TOOL_BAR_SCROLL_BUTTONS_PLACEMENT;
import static gov.takx.core.view.gui.toolbar.ScrollableToolbarProperties.TOOL_BAR_SCROLL_BUTTONS_POLICY;
import static gov.takx.core.view.gui.toolbar.ScrollableToolbarProperties.TOOL_BAR_TOOLS_POPUP_POLICY;

/**
 * The UI delegate for {@link JScrollableToolBar} for FlatLaf, which adds support for a drop-down overflow menu. This 
 * was largely copied from {@link com.formdev.flatlaf.ui.FlatTabbedPaneUI} since it implemented a drop-down overflow
 * menu for {@link javax.swing.JTabbedPane}. The dependence on FlatLaf as the LAF is isolated to this file  as much as
 * possible in case we ever change LAFs again or if we want to reuse the {@link JScrollableToolBar} component without
 * requiring FlatLaf. The ONLY dependency on FlatLaf in {@link ScrollableToolBarUI} is extending
 * {@link com.formdev.flatlaf.ui.FlatToolBarUI}. This was done because this class can't extend both UI classes, and it
 * would either need to do something hacky like rely on composition + inheritance or copy all the desired FlatLaf
 * functionality from FlatToolBarUI. A single dependency on the LAF seems like a lesser evil than copying a bunch of
 * code to this class.
 * 
 * @since 3.3
 */
public class FlatScrollableToolBarUI extends ScrollableToolBarUI
{
    // Tools popup policy / scroll arrows policy
    protected static final int NEVER = 0;
    protected static final int AS_NEEDED = 2;
    protected static final int AS_NEEDED_SINGLE = 3;

    // Scroll arrows placement
    protected static final int BOTH = 100;

    // Tool area alignment
    protected static final int FILL = 100;

    protected Color foreground;
    protected Color disabledForeground;

    private int toolsPopupPolicy;
    private int scrollButtonsPolicy;
    private int scrollButtonsPlacement;

    protected String arrowType;
    protected Insets buttonInsets;
    protected int buttonArc;
    protected Color buttonHoverBackground;
    protected Color buttonPressedBackground;

    protected String moreToolsButtonToolTipText;

    protected JViewport toolViewport;
    protected FlatWheelToolScroller wheelToolScroller;

    private JButton moreToolsButton;

    private Dimension scrollBackwardButtonPrefSize;

    private Handler handler;
    private boolean blockRollover;

    private Map<String, Object> oldStyleValues;

    @SuppressWarnings("unused")
    public static ComponentUI createUI(JComponent c)
    {
        return new FlatScrollableToolBarUI();
    }

    @Override
    protected void installDefaults()
    {
        super.installDefaults();

        arrowType = UIManager.getString("Component.arrowType");
        foreground = UIManager.getColor("ToolBar.foreground");
        disabledForeground = UIManager.getColor("TabbedPane.disabledForeground"); // No equivalent toolbar property
        buttonHoverBackground = UIManager.getColor("ToggleButton.toolbar.hoverBackground");
        buttonPressedBackground = UIManager.getColor("ToggleButton.toolbar.pressedBackground");

        toolsPopupPolicy = parseToolsPopupPolicy(UIManager.getString(TOOL_BAR_TOOLS_POPUP_POLICY));
        scrollButtonsPolicy = parseScrollButtonsPolicy(UIManager.getString(TOOL_BAR_SCROLL_BUTTONS_POLICY));
        scrollButtonsPlacement = parseScrollButtonsPlacement(UIManager.getString(TOOL_BAR_SCROLL_BUTTONS_PLACEMENT));

        buttonInsets = UIManager.getInsets("Button.toolbar.spacingInsets");
        buttonArc = UIManager.getInt("Button.arc");

        Locale l = toolBar.getLocale();
        moreToolsButtonToolTipText = UIManager.getString(TOOL_BAR_MORE_TOOLS_BUTTON_TOOL_TIP, l);
    }

    @Override
    protected void uninstallDefaults()
    {
        super.uninstallDefaults();

        foreground = null;
        disabledForeground = null;
        buttonHoverBackground = null;
        buttonPressedBackground = null;

        oldStyleValues = null;
    }

    @Override
    protected void installComponents()
    {
        super.installComponents();

        toolViewport = toolScroller.viewport;

        installHiddenToolsNavigation();
    }

    protected void installHiddenToolsNavigation()
    {
        if (toolViewport == null) return;

        // Create and add "more tools" button
        moreToolsButton = createMoreToolsButton();
        toolBar.add(moreToolsButton);
    }

    @Override
    protected ToolContainer createToolContainer()
    {
        return new FlatToolContainer();
    }

    @Override
    protected void uninstallComponents()
    {
        // Uninstall hidden tools navigation before invoking super.uninstallComponents() for correct uninstallation of
        // ScrollableToolBarUI tool scroller support
        uninstallHiddenToolsNavigation();

        super.uninstallComponents();

        toolViewport = null;
    }

    protected void uninstallHiddenToolsNavigation()
    {
        if (moreToolsButton != null)
        {
            toolBar.remove(moreToolsButton);
            moreToolsButton = null;
        }
    }

    @Override
    protected void installListeners()
    {
        super.installListeners();

        getHandler().installListeners();

        if (toolViewport != null && (wheelToolScroller = createWheelToolScroller()) != null)
        {
            // Ideally we would add the mouse listeners to the viewport, but then the mouse listener of the toolbar 
            // would not receive events while the mouse pointer is over the viewport
            toolBar.addMouseWheelListener(wheelToolScroller);
            toolBar.addMouseMotionListener(wheelToolScroller);
            toolBar.addMouseListener(wheelToolScroller);
        }
    }

    @Override
    protected void uninstallListeners()
    {
        super.uninstallListeners();

        if (handler != null)
        {
            handler.uninstallListeners();
            handler = null;
        }

        if (wheelToolScroller != null)
        {
            wheelToolScroller.uninstall();

            toolBar.removeMouseWheelListener(wheelToolScroller);
            toolBar.removeMouseMotionListener(wheelToolScroller);
            toolBar.removeMouseListener(wheelToolScroller);
            wheelToolScroller = null;
        }
    }

    private Handler getHandler()
    {
        if (handler == null) handler = new Handler();
        return handler;
    }

    protected FlatWheelToolScroller createWheelToolScroller()
    {
        return new FlatWheelToolScroller();
    }

    @Override
    protected PropertyChangeListener createPropertyChangeListener()
    {
        Handler handler = getHandler();
        handler.propertyChangeDelegate = super.createPropertyChangeListener();
        return handler;
    }

    @Override
    protected LayoutManager createLayoutManager()
    {
        return new FlatToolBarScrollLayout();
    }

    protected JButton createMoreToolsButton()
    {
        return new FlatMoreToolsButton();
    }

    @Override
    protected JButton createScrollButton(int direction)
    {
        return new FlatScrollableToolButton(direction);
    }

    @Override
    protected void applyStyle(Object style)
    {
        oldStyleValues = FlatStylingSupport.parseAndApply(oldStyleValues, style, this::applyStyleProperty);

        // update buttons
        for (Component c : toolBar.getComponents())
        {
            if (c instanceof FlatToolAreaButton) ((FlatToolAreaButton) c).updateStyle();
        }

        super.applyStyle(style);
    }

    @Override
    protected Object applyStyleProperty(String key, Object value)
    {
        if (value instanceof String valueString)
        {
            switch (key)
            {
                case "toolsPopupPolicy" -> value = parseToolsPopupPolicy(valueString);
                case "scrollButtonsPolicy" -> value = parseScrollButtonsPolicy(valueString);
                case "scrollButtonsPlacement" -> value = parseScrollButtonsPlacement(valueString);
            }
        }

        return FlatStylingSupport.applyToAnnotatedObjectOrComponent(this, toolBar, key, value);
    }

    protected void setRolloverTool(int x, int y)
    {
        setRolloverTool(toolForCoordinate(toolBar, x, y));
    }

    @Override
    protected void setRolloverTool(int index)
    {
        if (blockRollover) return;

        int oldIndex = getRolloverTool();
        super.setRolloverTool(index);

        if (index == oldIndex) return;

        // repaint old and new hover tools
        repaintTool(oldIndex);
        repaintTool(index);
    }

    private void repaintTool(int toolIndex)
    {
        if (toolIndex < 0 || toolIndex >= toolBar.getToolCount()) return;

        Rectangle r = getToolBounds(toolIndex);
        if (r == null) return;

        toolBar.repaint(r);
    }

    /**
     * Rotates the insets.
     *
     * @param topInsets the top insets
     * @param targetInsets the target insets
     */
    @SuppressWarnings("SuspiciousNameCombination")
    protected void rotateInsets(Insets topInsets, Insets targetInsets)
    {
        if (isHorizontalOrientation())
        {
            targetInsets.top = topInsets.top;
            targetInsets.left = topInsets.left;
            targetInsets.bottom = topInsets.bottom;
            targetInsets.right = topInsets.right;
        } else
        {
            targetInsets.top = topInsets.left;
            targetInsets.left = topInsets.top;
            targetInsets.bottom = topInsets.right;
            targetInsets.right = topInsets.bottom;
        }
    }

    @Override
    public void update(Graphics g, JComponent c)
    {
        Object[] oldRenderingHints = FlatUIUtils.setRenderingHints(g);

        super.update(g, c);

        FlatUIUtils.resetRenderingHints(g, oldRenderingHints);
    }

    @Override
    protected void paintToolArea(Graphics g)
    {
        // need to set rendering hints here too because this method is invoked from
        // ScrollableToolBarUI.ScrollableToolPanel.paintComponent()
        Object[] oldHints = FlatUIUtils.setRenderingHints(g);

        super.paintToolArea(g);

        FlatUIUtils.resetRenderingHints(g, oldHints);
    }

    @Override
    public int toolForCoordinate(JScrollableToolBar toolBar, int x, int y)
    {
        if (moreToolsButton != null)
        {
            // convert x,y from JToolBar coordinate space to ScrollableToolPanel coordinate space
            Point viewPosition = toolViewport.getViewPosition();
            x = x - toolViewport.getX() + viewPosition.x;
            y = y - toolViewport.getY() + viewPosition.y;

            // check whether point is within viewport
            if (!toolViewport.getViewRect().contains(x, y)) return -1;
        }

        return super.toolForCoordinate(toolBar, x, y);
    }

    @Override
    protected Rectangle getToolBounds(int toolIndex, Rectangle dest)
    {
        if (moreToolsButton != null)
        {
            // copy tool bounds to dest
            dest.setBounds(rects[toolIndex]);

            // convert tool bounds to coordinate space of JToolBar
            Point viewPosition = toolViewport.getViewPosition();
            dest.x = dest.x + toolViewport.getX() - viewPosition.x;
            dest.y = dest.y + toolViewport.getY() - viewPosition.y;
            return dest;
        } else
        {
            return super.getToolBounds(toolIndex, dest);
        }
    }

    protected boolean isSmoothScrollingEnabled()
    {
        if (!Animator.useAnimation()) return false;

        // Note: Getting UI value "ScrollPane.smoothScrolling" here to allow
        // applications to turn smooth scrolling on or off at any time
        // (e.g. in application options dialog).
        return UIManager.getBoolean("ScrollPane.smoothScrolling");
    }

    protected int getToolsPopupPolicy()
    {
        Object value = toolBar.getClientProperty(TOOL_BAR_TOOLS_POPUP_POLICY);

        return (value instanceof String)
                ? parseToolsPopupPolicy((String) value)
                : toolsPopupPolicy;
    }

    protected int getScrollButtonsPolicy()
    {
        Object value = toolBar.getClientProperty(TOOL_BAR_SCROLL_BUTTONS_POLICY);

        return (value instanceof String)
                ? parseScrollButtonsPolicy((String) value)
                : scrollButtonsPolicy;
    }

    protected int getScrollButtonsPlacement()
    {
        Object value = toolBar.getClientProperty(TOOL_BAR_SCROLL_BUTTONS_PLACEMENT);

        return (value instanceof String)
                ? parseScrollButtonsPlacement((String) value)
                : scrollButtonsPlacement;
    }

    protected static int parseToolsPopupPolicy(String str)
    {
        return TOOL_BAR_POLICY_NEVER.equals(str) ? NEVER : AS_NEEDED;
    }

    protected static int parseScrollButtonsPolicy(String str)
    {
        if (str == null) return AS_NEEDED_SINGLE;

        return switch (str)
        {
            case TOOL_BAR_POLICY_AS_NEEDED -> AS_NEEDED;
            case TOOL_BAR_POLICY_NEVER -> NEVER;
            default -> AS_NEEDED_SINGLE;
        };
    }

    protected static int parseScrollButtonsPlacement(String str)
    {
        if (str == null) return BOTH;
        return TOOL_BAR_PLACEMENT_TRAILING.equals(str) ? TRAILING : BOTH;
    }

    protected void ensureSelectedToolIsVisibleLater()
    {
        // do nothing if not yet displayable or if not invoked from dispatch thread,
        // which may be the case when creating/modifying in another thread
        if (!toolBar.isDisplayable() || !EventQueue.isDispatchThread()) return;

        EventQueue.invokeLater(this::ensureSelectedToolIsVisible);
    }

    protected void ensureSelectedToolIsVisible()
    {
        if (toolBar == null || toolViewport == null || !toolBar.isDisplayable()) return;

        ensureCurrentLayout();
    }

    private void shiftTools(int sx)
    {
        if (sx == 0) return;

        for (int i = 0; i < rects.length; i++)
        {
            // fix x location in rects
            rects[i].x += sx;
            rects[i].y += 0;

            // fix tool component location
            Component c = toolBar.getToolAtIndex(i);
            if (c != null) c.setLocation(c.getX() + sx, c.getY());
        }
    }

    private int rectsTotalWidth(boolean leftToRight)
    {
        int last = rects.length - 1;
        return leftToRight
                ? (rects[last].x + rects[last].width) - rects[0].x
                : (rects[0].x + rects[0].width) - rects[last].x;
    }

    private int rectsTotalHeight()
    {
        int last = rects.length - 1;
        return (rects[last].y + rects[last].height) - rects[0].y;
    }

    protected class FlatToolAreaButton extends FlatArrowButton
    {
        public FlatToolAreaButton(int direction)
        {
            super(direction, arrowType, FlatScrollableToolBarUI.this.foreground, FlatScrollableToolBarUI.this.disabledForeground,
                    null, buttonHoverBackground, null, buttonPressedBackground);
            setArrowWidth(10);
        }

        protected void updateStyle()
        {
            updateStyle(arrowType, FlatScrollableToolBarUI.this.foreground, FlatScrollableToolBarUI.this.disabledForeground,
                    null, buttonHoverBackground, null, buttonPressedBackground);
        }

        @Override
        protected Color deriveBackground(Color background)
        {
            return FlatUIUtils.deriveColor(background, toolBar.getBackground());
        }

        @Override
        public void paint(Graphics g)
        {
            // fill button background
            if (toolBar.isOpaque())
            {
                g.setColor(toolBar.getBackground());
                g.fillRect(0, 0, getWidth(), getHeight());
            }

            super.paint(g);
        }

        @Override
        protected void paintBackground(Graphics2D g)
        {
            // rotate button insets
            Insets insets = new Insets(0, 0, 0, 0);
            rotateInsets(buttonInsets, insets);

            // use UIScale.scale2() here because this gives smaller insets at 150% and 175%
            int top = UIScale.scale2(insets.top);
            int left = UIScale.scale2(insets.left);
            int bottom = UIScale.scale2(insets.bottom);
            int right = UIScale.scale2(insets.right);

            FlatUIUtils.paintComponentBackground(g, left, top,
                    getWidth() - left - right,
                    getHeight() - top - bottom,
                    0, scale((float) buttonArc));
        }
    }

    protected class FlatMoreToolsButton extends FlatToolAreaButton implements ActionListener, PopupMenuListener
    {
        private boolean popupVisible;

        public FlatMoreToolsButton()
        {
            super(SOUTH);

            updateDirection();
            setToolTipText(moreToolsButtonToolTipText);
            addActionListener(this);
        }

        protected void updateDirection()
        {
            setDirection(isHorizontalOrientation() ? SOUTH : EAST);
        }

        @Override
        public Dimension getPreferredSize()
        {
            Dimension size = super.getPreferredSize();
            boolean horizontal = (direction == SOUTH || direction == NORTH);
            int margin = scale(8);
            return new Dimension(size.width + (horizontal ? margin : 0), size.height + (horizontal ? 0 : margin));
        }

        @Override
        public void paint(Graphics g)
        {
            // paint arrow button near separator line
            if (direction == EAST || direction == WEST)
            {
                int xOffset = Math.max(UIScale.unscale((getWidth() - getHeight()) / 2) - 4, 0);
                setXOffset((direction == EAST) ? xOffset : -xOffset);
            } else
            {
                setXOffset(0);
            }

            super.paint(g);
        }

        @Override
        protected boolean isHover()
        {
            return super.isHover() || popupVisible;
        }

        @Override
        public void actionPerformed(ActionEvent e)
        {
            if (toolViewport == null) return;

            // Detect (partly) hidden tools and build popup menu
            JPopupMenu popupMenu = new JPopupMenu();
            popupMenu.addPopupMenuListener(this);
            Rectangle viewRect = toolViewport.getViewRect();
            int lastToolIndexInMenu = -1;
            boolean separatorIsLast = false;
            for (int i = 0; i < rects.length; i++)
            {
                if (!viewRect.contains(rects[i]))
                {
                    // Add separator between leading and trailing tools in menu
                    if (lastToolIndexInMenu >= 0 && lastToolIndexInMenu + 1 != i && !separatorIsLast)
                    {
                        popupMenu.addSeparator();
                        separatorIsLast = true;
                    }

                    if (toolBar.isSeparatorAtIndex(i))
                    {
                        // Don't add a separator to the menu if the last menu item was a separator or if this would
                        // be the first menu item
                        if (!separatorIsLast && lastToolIndexInMenu >= 0) popupMenu.addSeparator();
                        separatorIsLast = true;
                    } else
                    {
                        // create menu item for tool
                        final JMenuItem menuItem = createToolMenuItem(i);
                        if (menuItem != null) popupMenu.add(menuItem);
                        separatorIsLast = false;
                    }

                    lastToolIndexInMenu = i;
                }
            }

            // compute popup menu location
            int buttonWidth = getWidth();
            int buttonHeight = getHeight();
            Dimension popupSize = popupMenu.getPreferredSize();

            int x = isLeftToRight() ? buttonWidth - popupSize.width : 0;
            int y = buttonHeight - popupSize.height;
            if (isHorizontalOrientation())
            {
                y = buttonHeight;
            } else
            {
                x = buttonWidth;
            }

            // show popup menu
            popupMenu.show(this, x, y);
        }

        protected JMenuItem createToolMenuItem(int toolIndex)
        {
            final JComponent tool = toolBar.getToolAtIndex(toolIndex);
            if (!(tool instanceof AbstractButton button)) return null;

            JMenuItem menuItem = new JMenuItem(button.getText(), button.getIcon());
            menuItem.setDisabledIcon(button.getDisabledIcon());
            menuItem.setToolTipText(button.getToolTipText());
            menuItem.setEnabled(button.isEnabled());
            menuItem.addActionListener(e -> button.doClick());

            return menuItem;
        }

        @Override
        public void popupMenuWillBecomeVisible(PopupMenuEvent e)
        {
            popupVisible = true;
            repaint();
        }

        @Override
        public void popupMenuWillBecomeInvisible(PopupMenuEvent e)
        {
            popupVisible = false;
            repaint();
        }

        @Override
        public void popupMenuCanceled(PopupMenuEvent e)
        {
            popupVisible = false;
            repaint();
        }
    }

    protected class FlatScrollableToolButton extends FlatToolAreaButton implements MouseListener
    {
        private Timer autoRepeatTimer;

        protected FlatScrollableToolButton(int direction)
        {
            super(direction);
            addMouseListener(this);
        }

        @Override
        public void mousePressed(MouseEvent e)
        {
            if (SwingUtilities.isLeftMouseButton(e) && isEnabled())
            {
                if (autoRepeatTimer == null)
                {
                    // Using same delays as in BasicScrollBarUI and BasicSpinnerUI
                    autoRepeatTimer = new Timer(60, e2 -> {
                        if (isEnabled()) doClick();
                    });
                    autoRepeatTimer.setInitialDelay(300);
                }

                autoRepeatTimer.start();
            }
        }

        @Override
        public void mouseReleased(MouseEvent e)
        {
            if (autoRepeatTimer != null) autoRepeatTimer.stop();
        }

        @Override
        public void mouseClicked(MouseEvent e)
        {
        }

        @Override
        public void mouseEntered(MouseEvent e)
        {
            if (autoRepeatTimer != null && isPressed()) autoRepeatTimer.start();
        }

        @Override
        public void mouseExited(MouseEvent e)
        {
            if (autoRepeatTimer != null) autoRepeatTimer.stop();
        }
    }

    protected class FlatWheelToolScroller extends MouseAdapter
    {
        private int lastMouseX;
        private int lastMouseY;

        private boolean inViewport;
        private boolean scrolled;
        private Timer rolloverTimer;
        private Timer exitedTimer;

        private Animator animator;
        private Point startViewPosition;
        private Point targetViewPosition;

        protected void uninstall()
        {
            if (rolloverTimer != null) rolloverTimer.stop();
            if (exitedTimer != null) exitedTimer.stop();
            if (animator != null) animator.cancel();
        }

        @Override
        public void mouseWheelMoved(MouseWheelEvent e)
        {
            // Disable wheel scrolling if application has added its own mouse wheel listener
            if (toolBar.getMouseWheelListeners().length > 1) return;

            // Because this listener receives mouse events for the whole toolbar, we have to check whether the mouse is
            // located over the viewport
            if (!isInViewport(e.getX(), e.getY())) return;

            lastMouseX = e.getX();
            lastMouseY = e.getY();

            double preciseWheelRotation = e.getPreciseWheelRotation();
            boolean isPreciseWheel = (preciseWheelRotation != 0 && preciseWheelRotation != e.getWheelRotation());
            int amount = (int) (maxToolHeight * preciseWheelRotation);

            // Scroll at least one pixel to avoid "hanging"
            if (amount == 0)
            {
                if (preciseWheelRotation > 0)
                {
                    amount = 1;
                } else if (preciseWheelRotation < 0)
                {
                    amount = -1;
                }
            }

            // compute new view position
            Point viewPosition = (targetViewPosition != null)
                    ? targetViewPosition
                    : toolViewport.getViewPosition();
            Dimension viewSize = toolViewport.getViewSize();
            boolean horizontal = isHorizontalOrientation();
            int x = viewPosition.x;
            int y = viewPosition.y;
            if (horizontal)
            {
                x += isLeftToRight() ? amount : -amount;
            } else
            {
                y += amount;
            }

            // In case of having scroll buttons on both sides and hiding disabled buttons,
            // the viewport is moved when the scroll backward button becomes visible
            // or is hidden. For non-precise wheel scrolling (e.g. mouse wheel on Windows),
            // this is no problem because the scroll amount is at least a tool-height.
            // For precise wheel scrolling (e.g. touchpad on Mac), this is a problem
            // because it is possible to scroll by a fraction of a tool-height.
            if (isPreciseWheel &&
                    getScrollButtonsPlacement() == BOTH &&
                    getScrollButtonsPolicy() == AS_NEEDED_SINGLE &&
                    (isLeftToRight() || !horizontal) || // scroll buttons are hidden in right-to-left
                    scrollBackwardButtonPrefSize != null)
            {
                // special cases for scrolling with touchpad or high-resolution wheel:
                //   1. if view is at 0/0 and scrolling right/down, then the scroll backward button
                //      becomes visible, which moves the viewport right/down by the width/height of
                //      the button --> add button width/height to new view position so that
                //      tools seems to stay in place at screen
                //   2. if scrolling left/up to the beginning, then the scroll backward button
                //      becomes hidden, which moves the viewport left/up by the width/height of
                //      the button --> set new view position to 0/0 so that
                //      tools seems to stay in place at screen
                if (horizontal)
                {
                    if (viewPosition.x == 0 && x > 0)
                    {
                        x += scrollBackwardButtonPrefSize.width;
                    } else if (amount < 0 && x <= scrollBackwardButtonPrefSize.width)
                    {
                        x = 0;
                    }
                } else
                {
                    if (viewPosition.y == 0 && y > 0)
                    {
                        y += scrollBackwardButtonPrefSize.height;
                    } else if (amount < 0 && y <= scrollBackwardButtonPrefSize.height)
                    {
                        y = 0;
                    }
                }
            }

            // limit new view position
            if (horizontal)
            {
                x = Math.min(Math.max(x, 0), viewSize.width - toolViewport.getWidth());
            } else
            {
                y = Math.min(Math.max(y, 0), viewSize.height - toolViewport.getHeight());
            }

            // check whether view position has changed
            Point newViewPosition = new Point(x, y);
            if (newViewPosition.equals(viewPosition)) return;

            // update view position
            if (isPreciseWheel)
            {
                // do not use animation for precise scrolling (e.g. with trackpad)

                // stop running animation (if any)
                if (animator != null) animator.stop();

                toolViewport.setViewPosition(newViewPosition);
                updateRolloverDelayed();
            } else
            {
                setViewPositionAnimated(newViewPosition);
            }

            scrolled = true;
        }

        protected void setViewPositionAnimated(Point viewPosition)
        {
            // check whether position is equal to current position
            if (viewPosition.equals(toolViewport.getViewPosition())) return;

            // do not use animation if disabled
            if (!isSmoothScrollingEnabled())
            {
                toolViewport.setViewPosition(viewPosition);
                updateRolloverDelayed();
                return;
            }

            // remember start and target view positions
            startViewPosition = toolViewport.getViewPosition();
            targetViewPosition = viewPosition;

            // create animator
            if (animator == null)
            {
                // using same delays as in FlatScrollBarUI
                int duration = 200;
                int resolution = 10;

                animator = new Animator(duration, fraction -> {
                    if (toolViewport == null || !toolViewport.isShowing())
                    {
                        animator.stop();
                        return;
                    }

                    // update view position
                    int x = startViewPosition.x + Math.round((targetViewPosition.x - startViewPosition.x) * fraction);
                    int y = startViewPosition.y + Math.round((targetViewPosition.y - startViewPosition.y) * fraction);
                    toolViewport.setViewPosition(new Point(x, y));
                }, () -> {
                    startViewPosition = targetViewPosition = null;

                    if (toolBar != null) setRolloverTool(lastMouseX, lastMouseY);
                });

                animator.setResolution(resolution);
                animator.setInterpolator(new CubicBezierEasing(0.5f, 0.5f, 0.5f, 1));
            }

            // restart animator
            animator.restart();
        }

        protected void updateRolloverDelayed()
        {
            blockRollover = true;

            // keep rollover on last tool until it would move to another tool, then clear it
            int oldIndex = getRolloverTool();
            if (oldIndex >= 0)
            {
                int index = toolForCoordinate(toolBar, lastMouseX, lastMouseY);
                if (index >= 0 && index != oldIndex)
                {
                    // clear if moved to another tool
                    blockRollover = false;
                    setRolloverTool(-1);
                    blockRollover = true;
                }
            }

            // create timer
            if (rolloverTimer == null)
            {
                rolloverTimer = new Timer(150, e -> {
                    blockRollover = false;

                    // highlight tool at mouse location
                    if (toolBar != null) setRolloverTool(lastMouseX, lastMouseY);
                });
                rolloverTimer.setRepeats(false);
            }

            // restart timer
            rolloverTimer.restart();
        }

        @Override
        public void mouseMoved(MouseEvent e)
        {
            checkViewportExited(e.getX(), e.getY());
        }

        @Override
        public void mouseExited(MouseEvent e)
        {
            // this event occurs also if mouse is moved to a custom tool component
            // that handles mouse events (e.g. a close button)
            checkViewportExited(e.getX(), e.getY());
        }

        @Override
        public void mousePressed(MouseEvent e)
        {
            // for the case that the tool was only partly visible before the user clicked it
            setRolloverTool(e.getX(), e.getY());
        }

        protected boolean isInViewport(int x, int y)
        {
            return (toolViewport != null && toolViewport.getBounds().contains(x, y));
        }

        protected void checkViewportExited(int x, int y)
        {
            lastMouseX = x;
            lastMouseY = y;

            boolean wasInViewport = inViewport;
            inViewport = isInViewport(x, y);

            if (inViewport != wasInViewport)
            {
                if (!inViewport)
                {
                    viewportExited();
                } else if (exitedTimer != null)
                {
                    exitedTimer.stop();
                }
            }
        }

        protected void viewportExited()
        {
            if (!scrolled) return;

            if (exitedTimer == null)
            {
                exitedTimer = new Timer(500, e -> ensureSelectedToolVisible());
                exitedTimer.setRepeats(false);
            }

            exitedTimer.start();
        }

        protected void ensureSelectedToolVisible()
        {
            // check whether UI delegate was uninstalled because this method is invoked via timer
            if (toolBar == null || toolViewport == null) return;
            if (!scrolled) return;

            scrolled = false;

            // scroll selected tool into visible area
            ensureSelectedToolIsVisible();
        }
    }

    private class Handler implements MouseListener, MouseMotionListener, PropertyChangeListener, ComponentListener
    {
        MouseListener mouseDelegate;
        PropertyChangeListener propertyChangeDelegate;

        void installListeners()
        {
            toolBar.addMouseMotionListener(this);
            toolBar.addComponentListener(this);
        }

        void uninstallListeners()
        {
            toolBar.removeMouseMotionListener(this);
            toolBar.removeComponentListener(this);
        }

        @Override
        public void mouseClicked(MouseEvent e)
        {
            mouseDelegate.mouseClicked(e);
        }

        @Override
        public void mousePressed(MouseEvent e)
        {
            updateRollover(e);
            mouseDelegate.mousePressed(e);
        }

        @Override
        public void mouseReleased(MouseEvent e)
        {
            mouseDelegate.mouseReleased(e);
            updateRollover(e);
        }

        @Override
        public void mouseEntered(MouseEvent e)
        {
            // This is necessary for "more tools" button
            updateRollover(e);
        }

        @Override
        public void mouseExited(MouseEvent e)
        {
            // This event occurs also if mouse is moved to a custom tool component that handles mouse events (e.g. a 
            // close button), so make sure that the tool stays highlighted
            updateRollover(e);
        }

        @Override
        public void mouseDragged(MouseEvent e)
        {
            updateRollover(e);
        }

        @Override
        public void mouseMoved(MouseEvent e)
        {
            updateRollover(e);
        }

        private void updateRollover(MouseEvent e)
        {
            setRolloverTool(e.getX(), e.getY());
        }

        @Override
        public void propertyChange(PropertyChangeEvent e)
        {
            propertyChangeDelegate.propertyChange(e);

            switch (e.getPropertyName())
            {
                case "orientation":
                    if (moreToolsButton instanceof FlatMoreToolsButton flatMoreToolsButton)
                    {
                        flatMoreToolsButton.updateDirection();
                    }
                    break;

                case "componentOrientation":
                    ensureSelectedToolIsVisibleLater();
                    break;

                case TOOL_BAR_TOOLS_POPUP_POLICY:
                case TOOL_BAR_SCROLL_BUTTONS_POLICY:
                case TOOL_BAR_SCROLL_BUTTONS_PLACEMENT:
                    toolBar.revalidate();
                    toolBar.repaint();
                    break;

                case STYLE:
                case STYLE_CLASS:
                    installStyle();
                    toolBar.revalidate();
                    toolBar.repaint();
                    break;
            }
        }

        @Override
        public void componentResized(ComponentEvent e)
        {
            // make sure that selected tool stays visible when component size changed
            ensureSelectedToolIsVisibleLater();
        }

        @Override
        public void componentMoved(ComponentEvent e)
        {
        }

        @Override
        public void componentShown(ComponentEvent e)
        {
        }

        @Override
        public void componentHidden(ComponentEvent e)
        {
        }
    }

    /**
     * Layout manager used for scroll and/or drop-down menu tool layout.
     */
    protected class FlatToolBarScrollLayout extends ToolBarScrollLayout
    {
        private Dimension calculateToolAreaSize()
        {
            // Calculate size of tools
            int width = 0;
            int height = 0;
            int toolCount = toolBar.getToolCount();
            for (int i = 0; i < toolCount; i++)
            {
                if (isHorizontalOrientation())
                {
                    width += calculateToolWidth(i);
                    height = Math.max(height, calculateToolHeight(i));
                } else
                {
                    width = Math.max(width, calculateToolWidth(i));
                    height += calculateToolHeight(i);
                }
            }

            Insets insets = toolBar.getInsets();
            Dimension size = new Dimension(width + insets.left + insets.right, height + insets.bottom + insets.top);

            // limit width/height in scroll layout
            if (isHorizontalOrientation())
            {
                size.width = Math.min(size.width, scale(100));
            } else
            {
                size.height = Math.min(size.height, scale(100));
            }

            return size;
        }

        @Override
        protected Dimension calculateSize(boolean minimum)
        {
            if (isContentEmpty()) return calculateToolAreaSize();
            return super.calculateSize(minimum);
        }

        /**
         * Check whether all content components are either {@code null} or have zero preferred size.
         * <p>
         * If {@code true}, assume that the toolbar is used without any content and
         * use the size of the tool area (single run) as minimum/preferred size.
         */
        protected boolean isContentEmpty()
        {
            int toolCount = toolBar.getToolCount();
            if (toolCount == 0) return true;

            for (int i = 0; i < toolCount; i++)
            {
                Component c = toolBar.getComponentAtIndex(i);
                if (c != null)
                {
                    Dimension cs = c.getPreferredSize();
                    if (cs.width != 0 || cs.height != 0) return false;
                }
            }

            return true;
        }

        @SuppressWarnings("ConstantConditions")
        @Override
        public void layoutContainer(Container parent)
        {
            super.layoutContainer(parent);

            Insets insets = toolBar.getInsets();
            final boolean horizontalOrientation = isHorizontalOrientation();
            if (horizontalOrientation)
            {
                // fix x-locations of tools in right-to-left component orientation
                if (!isLeftToRight()) shiftTools(insets.left);
            }

            int toolsPopupPolicy = getToolsPopupPolicy();
            int scrollButtonsPolicy = getScrollButtonsPolicy();
            int scrollButtonsPlacement = getScrollButtonsPlacement();

            boolean useMoreToolsButton = (toolsPopupPolicy == AS_NEEDED);
            boolean useScrollButtons = (scrollButtonsPolicy == AS_NEEDED || scrollButtonsPolicy == AS_NEEDED_SINGLE);
            boolean hideDisabledScrollButtons = (scrollButtonsPolicy == AS_NEEDED_SINGLE && scrollButtonsPlacement == BOTH);
            boolean trailingScrollButtons = (scrollButtonsPlacement == TRAILING);

            // TODO KMB: Since we own ScrollableToolBarUI, we could probably fix whatever this issue is...
            // for right-to-left always use "more tools" button for horizontal scrolling
            // because methods scrollForward() and scrollBackward() in class
            // BasicTabbedPaneUI.ScrollableTabSupport do not work for right-to-left
            boolean leftToRight = isLeftToRight();
            if (!leftToRight && horizontalOrientation)
            {
                useMoreToolsButton = true;
                useScrollButtons = false;
            }

            JButton backwardButton = toolScroller.scrollBackwardButton;
            JButton forwardButton = toolScroller.scrollForwardButton;
            Rectangle bounds = toolBar.getBounds();
            boolean moreToolsButtonVisible = false;
            boolean backwardButtonVisible = false;
            boolean forwardButtonVisible = false;

            // layout tool area
            if (horizontalOrientation)
            {
                // avoid that tool area "jump" to the right when backward button becomes hidden
                if (useScrollButtons && hideDisabledScrollButtons)
                {
                    Point viewPosition = toolViewport.getViewPosition();
                    if (viewPosition.x <= backwardButton.getPreferredSize().width)
                    {
                        toolViewport.setViewPosition(new Point(0, viewPosition.y));
                    }
                }

                // tool area bounds
                int tx = insets.left;
                int ty = insets.top;
                int tw = bounds.width - insets.left - insets.right;
                int th = maxToolHeight;

                int totalToolWidth = (rects.length > 0) ? rectsTotalWidth(leftToRight) : 0;

                // layout tool viewport and buttons
                if (rects.length > 0)
                {
                    // layout viewport and buttons
                    int x = tx;
                    int w = tw;

                    if (w < totalToolWidth) // available width is too small for all tools --> need buttons
                    {
                        // layout more button on trailing side
                        if (useMoreToolsButton)
                        {
                            int buttonWidth = moreToolsButton.getPreferredSize().width;
                            moreToolsButton.setBounds(leftToRight ? (x + w - buttonWidth) : x, ty, buttonWidth, th);
                            x += leftToRight ? 0 : buttonWidth;
                            w -= buttonWidth;
                            moreToolsButtonVisible = true;
                        }

                        if (useScrollButtons)
                        {
                            // layout forward button on trailing side
                            if (!hideDisabledScrollButtons || forwardButton.isEnabled())
                            {
                                int buttonWidth = forwardButton.getPreferredSize().width;
                                forwardButton.setBounds(leftToRight ? (x + w - buttonWidth) : x, ty, buttonWidth, th);
                                x += leftToRight ? 0 : buttonWidth;
                                w -= buttonWidth;
                                forwardButtonVisible = true;
                            }

                            // layout backward button
                            if (!hideDisabledScrollButtons || backwardButton.isEnabled())
                            {
                                int buttonWidth = backwardButton.getPreferredSize().width;
                                if (trailingScrollButtons)
                                {
                                    // on trailing side
                                    backwardButton.setBounds(leftToRight ? (x + w - buttonWidth) : x, ty, buttonWidth, th);
                                    x += leftToRight ? 0 : buttonWidth;
                                } else
                                {
                                    // on leading side
                                    backwardButton.setBounds(leftToRight ? x : (x + w - buttonWidth), ty, buttonWidth, th);
                                    x += leftToRight ? buttonWidth : 0;
                                }
                                w -= buttonWidth;
                                backwardButtonVisible = true;
                            }
                        }
                    }

                    toolViewport.setBounds(x, ty, w, th);

                    if (!leftToRight)
                    {
                        // layout viewport so that we can get correct view width below
                        toolViewport.doLayout();

                        // fix x-locations of tools so that they are right-aligned in the view
                        shiftTools(toolViewport.getView().getWidth() - (rects[0].x + rects[0].width));
                    }
                }
            } else
            {
                // avoid that tool area "jump" to the top when backward button becomes hidden
                if (useScrollButtons && hideDisabledScrollButtons)
                {
                    Point viewPosition = toolViewport.getViewPosition();
                    if (viewPosition.y <= backwardButton.getPreferredSize().height)
                    {
                        toolViewport.setViewPosition(new Point(viewPosition.x, 0));
                    }
                }

                // tool area bounds
                int tx = insets.left;
                int ty = insets.top;
                int tw = maxToolWidth;
                int th = bounds.height - insets.top - insets.bottom;

                int totalToolHeight = (rects.length > 0) ? rectsTotalHeight() : 0;

                // layout tool viewport and buttons
                if (rects.length > 0)
                {
                    // layout viewport and buttons
                    int y = ty;
                    int h = th;

                    if (h < totalToolHeight) // available height is too small for all tools --> need buttons
                    {
                        // layout more button on bottom side
                        if (useMoreToolsButton)
                        {
                            int buttonHeight = moreToolsButton.getPreferredSize().height;
                            moreToolsButton.setBounds(tx, y + h - buttonHeight, tw, buttonHeight);
                            h -= buttonHeight;
                            moreToolsButtonVisible = true;
                        }
                        if (useScrollButtons)
                        {
                            // layout forward button on bottom side
                            if (!hideDisabledScrollButtons || forwardButton.isEnabled())
                            {
                                int buttonHeight = forwardButton.getPreferredSize().height;
                                forwardButton.setBounds(tx, y + h - buttonHeight, tw, buttonHeight);
                                h -= buttonHeight;
                                forwardButtonVisible = true;
                            }

                            // layout backward button
                            if (!hideDisabledScrollButtons || backwardButton.isEnabled())
                            {
                                int buttonHeight = backwardButton.getPreferredSize().height;
                                if (trailingScrollButtons)
                                {
                                    // on bottom side
                                    backwardButton.setBounds(tx, y + h - buttonHeight, tw, buttonHeight);
                                } else
                                {
                                    // on top side
                                    backwardButton.setBounds(tx, y, tw, buttonHeight);
                                    y += buttonHeight;
                                }
                                h -= buttonHeight;
                                backwardButtonVisible = true;
                            }
                        }
                    }

                    toolViewport.setBounds(tx, y, tw, h);
                }
            }

            // show/hide viewport and buttons
            toolViewport.setVisible(rects.length > 0);
            moreToolsButton.setVisible(moreToolsButtonVisible);
            backwardButton.setVisible(backwardButtonVisible);
            forwardButton.setVisible(forwardButtonVisible);

            scrollBackwardButtonPrefSize = backwardButton.getPreferredSize();
        }
    }

    /**
     * Override to fix styling of buttons for FlatLaf.
     */
    protected class FlatToolContainer extends ToolContainer
    {
        public FlatToolContainer()
        {
            // Once buttons were changed to be toolbar type, this component needed to be opaque to avoid overlapping
            // text being left over when the component was repainted.
            setOpaque(true);
        }

        @Override
        protected void addImpl(Component comp, Object constraints, int index)
        {
            // Overridden to force buttons to be styled as if they were in a toolbar even though they are stored in an
            // intermediary component instead
            if (comp instanceof AbstractButton button) button.putClientProperty(BUTTON_TYPE, BUTTON_TYPE_TOOLBAR_BUTTON);

            super.addImpl(comp, constraints, index);
        }
    }
}
