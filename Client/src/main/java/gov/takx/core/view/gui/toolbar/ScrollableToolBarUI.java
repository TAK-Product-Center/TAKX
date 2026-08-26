package gov.takx.core.view.gui.toolbar;

import com.formdev.flatlaf.ui.FlatToolBarUI;

import javax.swing.AbstractButton;
import javax.swing.Action;
import javax.swing.ActionMap;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.JSeparator;
import javax.swing.JToolBar;
import javax.swing.JViewport;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.plaf.ComponentUI;
import javax.swing.plaf.UIResource;
import javax.swing.plaf.basic.BasicArrowButton;

import java.awt.Color;
import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Insets;
import java.awt.LayoutManager;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;

import static gov.takx.core.view.gui.toolbar.JScrollableToolBar.TOOL_ADDED;
import static gov.takx.core.view.gui.toolbar.JScrollableToolBar.TOOL_REMOVED;

/**
 * The UI delegate for {@link JScrollableToolBar}. {@link javax.swing.plaf.basic.BasicTabbedPaneUI} was used as a reference
 * for adding scroll support to a {@link javax.swing.JToolBar} since {@link javax.swing.JTabbedPane} already includes
 * support for scroll buttons.
 *
 * @since 3.3
 */
public class ScrollableToolBarUI extends FlatToolBarUI
{
    protected JScrollableToolBar toolBar;

    protected Rectangle[] rects = new Rectangle[0];
    protected int maxToolHeight;
    protected int maxToolWidth;

    protected PropertyChangeListener propertyChangeListener;

    protected ScrollableToolSupport toolScroller;
    protected ToolContainer toolContainer;
    private Handler handler; // Combined listeners.

    private int rolloverToolIndex; // Index of the tool the mouse is over.
    private boolean calculatedBaseline;
    private int baseline;

    /**
     * @param c a component
     * @return the UI created for the component.
     */
    @SuppressWarnings("unused")
    public static ComponentUI createUI(JComponent c)
    {
        return new ScrollableToolBarUI();
    }

    @Override
    public void installUI(JComponent c)
    {
        toolBar = (JScrollableToolBar) c;
        c.setLayout(createLayoutManager());
        super.installUI(c);

        calculatedBaseline = false;
        rolloverToolIndex = -1;
    }

    @Override
    public void uninstallUI(JComponent c)
    {
        super.uninstallUI(c);
        c.setLayout(null);

        toolBar = null;
    }

    /**
     * Invoked by {@code installUI} to create a layout manager object to manage the {@code JToolBar}.
     *
     * @return a layout manager object
     * @see ToolBarScrollLayout
     */
    protected LayoutManager createLayoutManager()
    {
        return new ToolBarScrollLayout();
    }

    @Override
    protected void installComponents()
    {
        super.installComponents();

        if (toolScroller == null)
        {
            toolScroller = new ScrollableToolSupport();
            toolBar.add(toolScroller.viewport);
        }

        installToolContainer();
    }

    /**
     * Installs a panel to serve as the parent container for the tools so they can be added to a viewport.
     */
    private void installToolContainer()
    {
        for (int i = 0; i < toolBar.getToolCount(); i++)
        {
            Component tool = toolBar.getToolAtIndex(i);
            if (toolContainer == null) toolContainer = createToolContainer();
            toolContainer.add(tool);
        }

        if (toolContainer == null) return;

        toolScroller.toolPanel.add(toolContainer);
    }

    /**
     * Create the panel for actually containing the tools (instead of adding them directly to the toolbar) so they can
     * be included in a viewport.
     * 
     * @return The tool container
     */
    protected ToolContainer createToolContainer()
    {
        return new ToolContainer();
    }

    /**
     * {@inheritDoc}
     * <p>
     * Note: we don't need to override {@link #uninstallKeyboardActions()} because the parent class resets the whole map.
     */
    @Override
    protected void installKeyboardActions()
    {
        super.installKeyboardActions();

        // Get shared action map, used for all toolbars
        ActionMap map = SwingUtilities.getUIActionMap(toolBar);
        if (map != null)
        {
            map.put(Actions.SCROLL_FORWARD, new Actions(Actions.SCROLL_FORWARD));
            map.put(Actions.SCROLL_BACKWARD, new Actions(Actions.SCROLL_BACKWARD));
        }
    }

    /**
     * Creates and returns a JButton that will provide the user with a way to scroll the tools in a particular direction.
     * The returned JButton must be instance of UIResource.
     *
     * @param direction One of the SwingConstants constants: SOUTH, NORTH, EAST or WEST
     * @return Widget for user to scroll toolbar
     * @see javax.swing.SwingConstants
     * @throws IllegalArgumentException if direction is not one of NORTH, SOUTH, EAST or WEST
     */
    protected JButton createScrollButton(int direction)
    {
        if (direction != SOUTH && direction != NORTH && direction != EAST && direction != WEST)
        {
            throw new IllegalArgumentException("Direction must be one of: SOUTH, NORTH, EAST or WEST");
        }

        return new ScrollableToolButton(direction);
    }

    @Override
    protected void uninstallComponents()
    {
        super.uninstallComponents();

        uninstallToolContainer();

        toolBar.remove(toolScroller.viewport);
        toolBar.remove(toolScroller.scrollForwardButton);
        toolBar.remove(toolScroller.scrollBackwardButton);
        toolScroller = null;
    }

    /**
     * Uninstalls the panel used to contain the tools in a viewport.
     */
    private void uninstallToolContainer()
    {
        if (toolContainer == null) return;

        // Remove all the toolComponents, making sure not to notify the toolbar.
        toolContainer.notifyToolBar = false;
        toolContainer.removeAll();
        toolScroller.toolPanel.remove(toolContainer);
        toolContainer = null;
    }

    @Override
    protected void installListeners()
    {
        super.installListeners();

        propertyChangeListener = createPropertyChangeListener();
        if (propertyChangeListener != null) toolBar.addPropertyChangeListener(propertyChangeListener);
    }

    @Override
    protected void uninstallListeners()
    {
        super.uninstallListeners();

        if (propertyChangeListener != null)
        {
            toolBar.removePropertyChangeListener(propertyChangeListener);
            propertyChangeListener = null;
        }

        handler = null;
    }

    /**
     * @return a property change listener
     */
    protected PropertyChangeListener createPropertyChangeListener()
    {
        return getHandler();
    }

    private Handler getHandler()
    {
        if (handler == null) handler = new Handler();
        return handler;
    }

    /**
     * Set the focusable state of all buttons to match the UI setting.
     *
     * @param focusable The desired focusable state
     */
    @Override
    protected void setButtonsFocusable(boolean focusable)
    {
        // Had to override the parent implementation since it loops over toolbar's components, and we didn't add the
        // tools directly to the toolbar.
        final int toolCount = toolBar.getToolCount();
        for (int i = 0; i < toolCount; i++)
        {
            final JComponent tool = toolBar.getToolAtIndex(i);
            setFocusableIfButton(tool, focusable);
        }
    }

    /**
     * Set the focusable state of the provided component to the specified focusable state (if it is a button).
     *
     * @param c The component to update (if it is a button)
     * @param focusable The desired focusable state
     */
    private void setFocusableIfButton(Component c, boolean focusable)
    {
        if (c instanceof AbstractButton && focusable != c.isFocusable()) c.setFocusable(focusable);
    }

    /**
     * Sets the tool the mouse is currently over to {@code index}. The {@code index} will be -1 if the mouse 
     * is no longer over any tool. No checking is done to ensure the passed in index identifies a valid tool.
     *
     * @param index Index of the tool the mouse is over.
     */
    protected void setRolloverTool(int index)
    {
        rolloverToolIndex = index;
    }

    /**
     * @return the tool the mouse is currently over, or {@code -1} if the mouse is no longer over any tool
     */
    protected int getRolloverTool()
    {
        return rolloverToolIndex;
    }

    @Override
    public Dimension getMinimumSize(JComponent c)
    {
        // Default to LayoutManager's minimumLayoutSize
        return null;
    }

    @Override
    public Dimension getMaximumSize(JComponent c)
    {
        // Default to LayoutManager's maximumLayoutSize
        return null;
    }

    @Override
    public int getBaseline(JComponent c, int width, int height)
    {
        super.getBaseline(c, width, height);
        int baseline = calculateBaselineIfNecessary();
        if (baseline != -1)
        {
            Insets insets = toolBar.getInsets();
            baseline += insets.top;
            return baseline;
        }

        return -1;
    }

    private int calculateBaselineIfNecessary()
    {
        if (!calculatedBaseline)
        {
            calculatedBaseline = true;
            baseline = -1;
            if (toolBar.getToolCount() > 0)
            {
                calculateBaseline();
            }
        }
        return baseline;
    }

    private void calculateBaseline()
    {
        int toolCount = toolBar.getToolCount();
        maxToolHeight = calculateMaxToolHeight();
        if (toolCount > 0) baseline = getBaseline(0);

        if (isHorizontalOrientation())
        {
            for (int i = 1; i < toolCount; i++)
            {
                if (getBaseline(i) != baseline)
                {
                    baseline = -1;
                    break;
                }
            }
        } else
        {
            // vertical, tools may be different sizes.
            int height = calculateToolHeight(0);
            for (int i = 1; i < toolCount; i++)
            {
                int newHeight = calculateToolHeight(i);
                if (height != newHeight)
                {
                    // assume different baseline
                    baseline = -1;
                    break;
                }
            }
        }
    }

    /**
     * Returns the baseline for the specified tool.
     *
     * @param toolIndex index of tool for which to get the baseline
     * @return baseline or a value &lt; 0 indicating there is no reasonable baseline
     */
    protected int getBaseline(int toolIndex)
    {
        final Component tool = toolBar.getToolAtIndex(toolIndex);
        Dimension pref = tool.getPreferredSize();
        int cellHeight = maxToolHeight;
        return tool.getBaseline(pref.width, pref.height) + (cellHeight - pref.height) / 2;
    }

    @Override
    public Component.BaselineResizeBehavior getBaselineResizeBehavior(JComponent c)
    {
        super.getBaselineResizeBehavior(c);
        return Component.BaselineResizeBehavior.CONSTANT_ASCENT;
    }

    @Override
    public void paint(Graphics g, JComponent c)
    {
        ensureCurrentLayout();
    }

    /**
     * Paints the tools in the tool area. Invoked by {@link #paint(Graphics, JComponent)}.
     * 
     * @param g the graphics object to use for rendering
     */
    protected void paintToolArea(Graphics g)
    {
        Rectangle clipRect = g.getClipBounds();

        int toolCount = toolBar.getToolCount();
        for (int i = 0; i < toolCount; i++)
        {
            if (rects[i].intersects(clipRect))
            {
                final Component tool = toolBar.getToolAtIndex(i);
                tool.paint(g);
            }
        }
    }

    /**
     * @return True if the toolbar orientation is horizontal.
     */
    protected boolean isHorizontalOrientation()
    {
        return toolBar.getOrientation() == HORIZONTAL;
    }

    /**
     * @return True if the toolbar component orientation is left to right.
     */
    protected boolean isLeftToRight()
    {
        return toolBar.getComponentOrientation().isLeftToRight();
    }

    protected void ensureCurrentLayout()
    {
        if (!toolBar.isValid()) toolBar.validate();

        // If toolBar doesn't have a peer yet, the validate() call will silently fail. We handle that by forcing a 
        // layout if toolBar is still invalid.  See bug 4237677.
        if (!toolBar.isValid())
        {
            ToolBarScrollLayout layout = (ToolBarScrollLayout) toolBar.getLayout();
            layout.calculateLayoutInfo();
        }
    }

    /**
     * Returns the tool index which intersects the specified point in the JToolBar's coordinate space.
     */
    public int toolForCoordinate(JScrollableToolBar toolBar, int x, int y)
    {
        ensureCurrentLayout();

        Point p = new Point(x, y);

        translatePointToToolPanel(x, y, p);
        Rectangle viewRect = toolScroller.viewport.getViewRect();
        if (!viewRect.contains(p)) return -1;

        int toolCount = toolBar.getToolCount();
        for (int i = 0; i < toolCount; i++)
        {
            if (rects[i].contains(p.x, p.y)) return i;
        }

        return -1;
    }

    /**
     * Returns the bounds of the specified tool in the coordinate space of the JToolBar component. This is required 
     * because the tool rects are by default defined in the coordinate space of the component where they are rendered,
     * which could be the JToolBar (for WRAP_TOOL_LAYOUT) or a ScrollableToolPanel (SCROLL_TOOL_LAYOUT). This method 
     * should be used whenever the tool rectangle must be relative to the JToolBar itself and the result should be 
     * placed in a designated Rectangle object (rather than instantiating and returning a new Rectangle each time). The 
     * tool index parameter must be a valid toolbar tool index (0 to tool count - 1, inclusive). The destination 
     * rectangle parameter must be a valid {@code Rectangle} instance. The handling of invalid parameters is 
     * unspecified.
     * 
     * @param toolIndex the index of the tool
     * @param dest      the rectangle where the result should be placed
     * @return the resulting rectangle
     */
    protected Rectangle getToolBounds(int toolIndex, Rectangle dest)
    {
        dest.width = rects[toolIndex].width;
        dest.height = rects[toolIndex].height;

        // Need to translate coordinates based on viewport location & view position
        Point viewportLocation = toolScroller.viewport.getLocation();
        Point viewLocation = toolScroller.viewport.getViewPosition();
        dest.x = rects[toolIndex].x + viewportLocation.x - viewLocation.x;
        dest.y = rects[toolIndex].y + viewportLocation.y - viewLocation.y;

        return dest;
    }

    /**
     * Returns the index of the tool closest to the passed in location, note that the returned tool may not contain the
     * location x,y.
     */
    private int getClosestTool(int x, int y)
    {
        int min = 0;
        int toolCount = Math.min(rects.length, toolBar.getToolCount());
        final boolean horizontalOrientation = isHorizontalOrientation();
        int max = toolCount;
        int want = horizontalOrientation ? x : y;

        while (min != max)
        {
            int current = (max + min) / 2;
            int minLoc = horizontalOrientation ? rects[current].x : rects[current].y;
            int maxLoc = minLoc + (horizontalOrientation ? rects[current].width : rects[current].height);

            if (want < minLoc)
            {
                max = current;
                if (min == max) return Math.max(0, current - 1);
            } else if (want >= maxLoc)
            {
                min = current;
                if (max - min <= 1) return Math.max(current + 1, toolCount - 1);
            } else
            {
                return current;
            }
        }

        return min;
    }

    /**
     * Returns a point which is translated from the specified point in the JToolBar's coordinate space to the coordinate
     * space of the ScrollableToolPanel. This is used for SCROLL_TOOL_LAYOUT ONLY.
     */
    @SuppressWarnings("SameParameterValue")
    private void translatePointToToolPanel(int srcX, int srcY, Point dest)
    {
        Point viewportPosition = toolScroller.viewport.getLocation();
        Point viewPosition = toolScroller.viewport.getViewPosition();
        dest.x = srcX - viewportPosition.x + viewPosition.x;
        dest.y = srcY - viewportPosition.y + viewPosition.y;
    }

    /**
     * Assure the rectangles are created.
     * 
     * @param toolCount the tool count
     */
    protected void assureRectsCreated(int toolCount)
    {
        int rectArrayLen = rects.length;
        if (toolCount != rectArrayLen)
        {
            Rectangle[] tempRectArray = new Rectangle[toolCount];
            System.arraycopy(rects, 0, tempRectArray, 0, Math.min(rectArrayLen, toolCount));
            rects = tempRectArray;
            for (int rectIndex = rectArrayLen; rectIndex < toolCount; rectIndex++)
            {
                rects[rectIndex] = new Rectangle();
            }
        }
    }

    /**
     * Calculates the tool height.
     *
     * @param toolIndex the index of the tool with respect to other tools
     * @return the tool height
     */
    protected int calculateToolHeight(int toolIndex)
    {
        Component tool = toolBar.getToolAtIndex(toolIndex);
        return tool.getPreferredSize().height;
    }

    /**
     * @return the maximum tool height
     */
    protected int calculateMaxToolHeight()
    {
        int toolCount = toolBar.getToolCount();
        int result = 0;
        for (int i = 0; i < toolCount; i++)
        {
            result = Math.max(calculateToolHeight(i), result);
        }

        return result;
    }

    /**
     * Calculates the tool width.
     *
     * @param toolIndex the index of the tool with respect to other tools
     * @return the tool width
     */
    protected int calculateToolWidth(int toolIndex)
    {
        Component tool = toolBar.getToolAtIndex(toolIndex);
        return tool.getPreferredSize().width;
    }

    /**
     * @return the maximum tool width
     */
    protected int calculateMaxToolWidth()
    {
        int toolCount = toolBar.getToolCount();
        int result = 0;
        for (int i = 0; i < toolCount; i++)
        {
            result = Math.max(calculateToolWidth(i), result);
        }

        return result;
    }

    public Rectangle getToolBounds(int i)
    {
        ensureCurrentLayout();
        Rectangle toolRect = new Rectangle();
        return getToolBounds(i, toolRect);
    }

    /**
     * Action implementation for handling the various actions.
     * <p>
     * Implementation copied from UIAction which is not accessible outside of the sun.swing package
     */
    protected record Actions(String name) implements Action
    {
        static final String SCROLL_FORWARD = "scrollToolsForwardAction";
        static final String SCROLL_BACKWARD = "scrollToolsBackwardAction";

        public String getName()
        {
            return name;
        }

        @Override
        public Object getValue(String key)
        {
            return key.equals(NAME) ? name : null;
        }

        @Override
        public void putValue(String key, Object value)
        {
        }

        @Override
        public void setEnabled(boolean b)
        {
        }

        @Override
        public boolean isEnabled()
        {
            return true;
        }

        @Override
        public void addPropertyChangeListener(PropertyChangeListener listener)
        {
        }

        @Override
        public void removePropertyChangeListener(PropertyChangeListener listener)
        {
        }

        @Override
        public void actionPerformed(ActionEvent e)
        {
            String key = getName();
            JScrollableToolBar toolBar = (JScrollableToolBar) e.getSource();
            final ScrollableToolBarUI ui = toolBar.getUI();

            if (ui == null) return;

            //noinspection SwitchStatementWithoutDefaultBranch
            switch (key)
            {
                case SCROLL_FORWARD -> ui.toolScroller.scrollForward();
                case SCROLL_BACKWARD -> ui.toolScroller.scrollBackward();
            }
        }
    }

    /**
     * Layout for a scrollable toolbar
     */
    protected class ToolBarScrollLayout implements LayoutManager
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
            return calculateSize(false);
        }

        @Override
        public Dimension minimumLayoutSize(Container parent)
        {
            return calculateSize(true);
        }

        /**
         * @param minimum True to use the minimum size; false to use the preferred size
         * @return the calculated size
         */
        protected Dimension calculateSize(boolean minimum)
        {
            Insets insets = toolBar.getInsets();

            int height = 0;
            int width = 0;

            // Determine minimum size required to display largest child in each dimension
            for (int i = 0; i < toolBar.getToolCount(); i++)
            {
                Component tool = toolBar.getToolAtIndex(i);
                Dimension size = minimum ? tool.getMinimumSize() : tool.getPreferredSize();

                if (size != null)
                {
                    height = Math.max(size.height, height);
                    width = Math.max(size.width, width);
                }
            }

            return new Dimension(width + insets.left + insets.right, height + insets.bottom + insets.top);
        }

        @Override
        public void layoutContainer(Container parent)
        {
            setRolloverTool(-1);

            int toolCount = toolBar.getToolCount();
            Insets insets = toolBar.getInsets();

            calculateLayoutInfo();

            if (toolBar.getToolCount() == 0)
            {
                toolScroller.scrollForwardButton.setVisible(false);
                toolScroller.scrollBackwardButton.setVisible(false);
                return;
            }

            int tx, ty, tw, th; // tool area bounds
            Rectangle bounds = toolBar.getBounds();
            int numChildren = toolBar.getComponentCount();

            if (numChildren > 0)
            {
                final boolean horizontalOrientation = isHorizontalOrientation();

                // calculate tool area bounds
                tw = horizontalOrientation ? bounds.width - insets.left - insets.right : maxToolWidth;
                th = horizontalOrientation ? maxToolHeight : bounds.height - insets.top - insets.bottom;
                tx = insets.left;
                ty = insets.top;

                final int totalToolWidth = rects[toolCount - 1].x + rects[toolCount - 1].width;
                final int totalToolHeight = rects[toolCount - 1].y + rects[toolCount - 1].height;

                for (int i = 0; i < numChildren; i++)
                {
                    Component child = toolBar.getComponent(i);

                    if (toolScroller != null && child == toolScroller.viewport)
                    {
                        JViewport viewport = (JViewport) child;
                        Rectangle viewRect = viewport.getViewRect();
                        int vw = tw;
                        int vh = th;
                        Dimension scrollButtonSize = toolScroller.scrollForwardButton.getPreferredSize();
                        if (horizontalOrientation)
                        {
                            if (totalToolWidth > tw)
                            {
                                // Need to allow space for scroll buttons
                                vw = (tw > 2 * scrollButtonSize.width) ? tw - 2 * scrollButtonSize.width : 0;
                                if (totalToolWidth - viewRect.x <= vw)
                                {
                                    // Scrolled to the end, so ensure the viewport size is
                                    // such that the scroll offset aligns with a tool
                                    vw = totalToolWidth - viewRect.x;
                                }
                            }
                        } else
                        {
                            if (totalToolHeight > th)
                            {
                                // Allow space for scroll buttons
                                vh = (th > 2 * scrollButtonSize.height) ? th - 2 * scrollButtonSize.height : 0;
                                if (totalToolHeight - viewRect.y <= vh)
                                {
                                    // Scrolled to the end, so ensure the viewport size is
                                    // such that the scroll offset aligns with a tool
                                    vh = totalToolHeight - viewRect.y;
                                }
                            }
                        }
                        child.setBounds(tx, ty, vw, vh);

                    } else if (toolScroller != null &&
                            (child == toolScroller.scrollForwardButton || child == toolScroller.scrollBackwardButton))
                    {
                        Dimension scrollButtonSize = child.getPreferredSize();
                        int bx = 0;
                        int by = 0;
                        int bw = scrollButtonSize.width;
                        int bh = scrollButtonSize.height;
                        boolean visible = false;

                        if (horizontalOrientation)
                        {
                            if (totalToolWidth > tw)
                            {
                                visible = true;
                                bx = (child == toolScroller.scrollForwardButton)
                                        ? bounds.width - insets.left - scrollButtonSize.width
                                        : bounds.width - insets.left - 2 * scrollButtonSize.width;
                                by = ty + th - scrollButtonSize.height;
                            }
                        } else
                        {
                            if (totalToolHeight > th)
                            {
                                visible = true;
                                bx = tx + tw - scrollButtonSize.width;
                                by = (child == toolScroller.scrollForwardButton)
                                        ? bounds.height - insets.bottom - scrollButtonSize.height
                                        : bounds.height - insets.bottom - 2 * scrollButtonSize.height;
                            }
                        }

                        child.setVisible(visible);
                        if (visible) child.setBounds(bx, by, bw, bh);

                    }
                }

                layoutToolComponents();
            }
        }

        public void calculateLayoutInfo()
        {
            int toolCount = toolBar.getToolCount();
            assureRectsCreated(toolCount);
            calculateToolRects(toolCount);
        }

        private void layoutToolComponents()
        {
            if (toolContainer == null) return;

            Rectangle rect = new Rectangle();
            Point delta = new Point(-toolContainer.getX(), -toolContainer.getY());
            translatePointToToolPanel(0, 0, delta);

            for (int i = 0; i < toolBar.getToolCount(); i++)
            {
                Component tool = toolBar.getToolAtIndex(i);

                getToolBounds(i, rect);
                int x = rect.x + delta.x;
                int y = rect.y + delta.y;
                tool.setBounds(x, y, rect.width, rect.height);
            }
        }

        protected void calculateToolRects(int toolCount)
        {
            Dimension size = toolBar.getSize();
            Insets insets = toolBar.getInsets();
            int i;
            boolean leftToRight = isLeftToRight();
            int x = 0;
            int y = 0;
            int totalWidth = 0;
            int totalHeight = 0;

            // Calculate bounds within which a tool row must fit
            final boolean horizontalOrientation = isHorizontalOrientation();
            if (horizontalOrientation)
            {
                maxToolHeight = calculateMaxToolHeight();
            } else
            {
                maxToolWidth = calculateMaxToolWidth();
            }

            if (toolCount == 0) return;

            // Run through tools and lay them out in a single row/column
            Rectangle rect;
            for (i = 0; i < toolCount; i++)
            {
                rect = rects[i];

                if (horizontalOrientation)
                {
                    if (i > 0)
                    {
                        rect.x = rects[i - 1].x + rects[i - 1].width;
                    } else
                    {
                        maxToolWidth = 0;
                        totalHeight += maxToolHeight;
                        rect.x = x;
                    }
                    rect.width = calculateToolWidth(i);
                    totalWidth = rect.x + rect.width;
                    maxToolWidth = Math.max(maxToolWidth, rect.width);

                    rect.y = y;
                    rect.height = maxToolHeight;

                } else
                {
                    if (i > 0)
                    {
                        rect.y = rects[i - 1].y + rects[i - 1].height;
                    } else
                    {
                        maxToolHeight = 0;
                        totalWidth = maxToolWidth;
                        rect.y = y;
                    }
                    rect.height = calculateToolHeight(i);
                    totalHeight = rect.y + rect.height;
                    maxToolHeight = Math.max(maxToolHeight, rect.height);

                    rect.x = x;
                    rect.width = maxToolWidth;

                }
            }

            // if right to left and horizontal orientation, flip x positions and adjust by widths
            if (!leftToRight && horizontalOrientation)
            {
                int rightMargin = size.width - insets.right;
                for (i = 0; i < toolCount; i++)
                {
                    rects[i].x = rightMargin - rects[i].x - rects[i].width;
                }
            }

            toolScroller.toolPanel.setPreferredSize(new Dimension(totalWidth, totalHeight));
            toolScroller.toolPanel.invalidate();
        }
    }

    /**
     * Manager for the scroll functionality which adds the scroll buttons and fires the appropriate action events to
     * update the viewport.
     */
    protected class ScrollableToolSupport implements ActionListener, ChangeListener
    {
        public ScrollableToolViewport viewport;
        public ScrollableToolPanel toolPanel;
        public JButton scrollForwardButton;
        public JButton scrollBackwardButton;
        public int leadingToolIndex;

        private final Point toolViewPosition = new Point(0, 0);

        ScrollableToolSupport()
        {
            viewport = new ScrollableToolViewport();
            toolPanel = new ScrollableToolPanel();
            viewport.setView(toolPanel);
            viewport.addChangeListener(this);
            createButtons();
        }

        /**
         * Recreates the scroll buttons and adds them to the toolBar.
         */
        void createButtons()
        {
            if (scrollForwardButton != null)
            {
                toolBar.remove(scrollForwardButton);
                scrollForwardButton.removeActionListener(this);
                toolBar.remove(scrollBackwardButton);
                scrollBackwardButton.removeActionListener(this);
            }

            final boolean horizontalOrientation = isHorizontalOrientation();
            scrollForwardButton = createScrollButton(horizontalOrientation ? EAST : SOUTH);
            scrollBackwardButton = createScrollButton(horizontalOrientation ? WEST : NORTH);
            scrollForwardButton.addActionListener(this);
            scrollBackwardButton.addActionListener(this);
            toolBar.add(scrollForwardButton);
            toolBar.add(scrollBackwardButton);
        }

        public void scrollForward()
        {
            Dimension viewSize = viewport.getViewSize();
            Rectangle viewRect = viewport.getViewRect();

            if (isHorizontalOrientation())
            {
                if (viewRect.width >= viewSize.width - viewRect.x) return; // no room left to scroll

            } else
            {
                if (viewRect.height >= viewSize.height - viewRect.y) return;
            }

            setLeadingToolIndex(leadingToolIndex + 1);
        }

        public void scrollBackward()
        {
            if (leadingToolIndex == 0) return; // no room left to scroll
            setLeadingToolIndex(leadingToolIndex - 1);
        }

        public void setLeadingToolIndex(int index)
        {
            leadingToolIndex = index;
            Dimension viewSize = viewport.getViewSize();
            Rectangle viewRect = viewport.getViewRect();

            if (isHorizontalOrientation())
            {
                toolViewPosition.x = leadingToolIndex == 0 ? 0 : rects[leadingToolIndex].x;
                if ((viewSize.width - toolViewPosition.x) < viewRect.width)
                {
                    // We've scrolled to the end, so adjust the viewport size
                    // to ensure the view position remains aligned on a tool boundary
                    Dimension extentSize = new Dimension(viewSize.width - toolViewPosition.x, viewRect.height);
                    viewport.setExtentSize(extentSize);
                }
            } else
            {
                toolViewPosition.y = leadingToolIndex == 0 ? 0 : rects[leadingToolIndex].y;
                if ((viewSize.height - toolViewPosition.y) < viewRect.height)
                {
                    // We've scrolled to the end, so adjust the viewport size
                    // to ensure the view position remains aligned on a tool boundary
                    Dimension extentSize = new Dimension(viewRect.width, viewSize.height - toolViewPosition.y);
                    viewport.setExtentSize(extentSize);
                }
            }

            viewport.setViewPosition(toolViewPosition);
        }

        @Override
        public void stateChanged(ChangeEvent e)
        {
            updateView();
        }

        private void updateView()
        {
            int toolCount = toolBar.getToolCount();
            assureRectsCreated(toolCount);
            Rectangle vpRect = viewport.getBounds();
            Dimension viewSize = viewport.getViewSize();
            Rectangle viewRect = viewport.getViewRect();

            leadingToolIndex = getClosestTool(viewRect.x, viewRect.y);

            // If the tool isn't right aligned, adjust it.
            if (leadingToolIndex + 1 < toolCount)
            {
                if (isHorizontalOrientation())
                {
                    if (rects[leadingToolIndex].x < viewRect.x)
                    {
                        leadingToolIndex++;
                    }
                } else
                {
                    if (rects[leadingToolIndex].y < viewRect.y)
                    {
                        leadingToolIndex++;
                    }
                }
            }

            if (isHorizontalOrientation())
            {
                toolBar.repaint(vpRect.x, vpRect.y + vpRect.height, vpRect.width, 0);
                scrollBackwardButton.setEnabled(viewRect.x > 0 && leadingToolIndex > 0);
                scrollForwardButton.setEnabled(leadingToolIndex < toolCount - 1 && viewSize.width - viewRect.x > viewRect.width);
            } else
            {
                toolBar.repaint(vpRect.x + vpRect.width, vpRect.y, 0, vpRect.height);
                scrollBackwardButton.setEnabled(viewRect.y > 0 && leadingToolIndex > 0);
                scrollForwardButton.setEnabled(leadingToolIndex < toolCount - 1 && viewSize.height - viewRect.y > viewRect.height);
            }
        }

        /**
         * ActionListener for the scroll buttons.
         */
        @Override
        public void actionPerformed(ActionEvent e)
        {
            ActionMap map = toolBar.getActionMap();
            if (map != null)
            {
                String actionKey = e.getSource() == scrollForwardButton ? Actions.SCROLL_FORWARD : Actions.SCROLL_BACKWARD;
                Action action = map.get(actionKey);

                if (action != null && action.isEnabled())
                {
                    action.actionPerformed(
                            new ActionEvent(toolBar, ActionEvent.ACTION_PERFORMED, null, e.getWhen(), e.getModifiers()));
                }
            }
        }

        @Override
        public String toString()
        {
            return "viewport.viewSize=" + viewport.getViewSize() + "\n" +
                    "viewport.viewRectangle=" + viewport.getViewRect() + "\n" +
                    "leadingToolIndex=" + leadingToolIndex + "\n" +
                    "toolViewPosition=" + toolViewPosition;
        }
    }

    /**
     * Viewport to manage the visible area of the toolbar.
     */
    protected class ScrollableToolViewport extends JViewport implements UIResource
    {
        public ScrollableToolViewport()
        {
            super();
            setName("ToolBar.scrollableViewport");
            setScrollMode(SIMPLE_SCROLL_MODE);
            setOpaque(toolBar.isOpaque());
            Color bgColor = UIManager.getColor("ToolBar.background");
            if (bgColor == null) bgColor = toolBar.getBackground();
            setBackground(bgColor);
        }
    }

    /**
     * Panel to serve as the view for {@link ScrollableToolViewport}.
     */
    protected class ScrollableToolPanel extends JPanel implements UIResource
    {
        public ScrollableToolPanel()
        {
            super(null);
            setOpaque(toolBar.isOpaque());
            Color bgColor = UIManager.getColor("ToolBar.background");
            if (bgColor == null) bgColor = toolBar.getBackground();
            setBackground(bgColor);
        }

        @Override
        public void paintComponent(Graphics g)
        {
            super.paintComponent(g);
            paintToolArea(g);
        }

        @Override
        public void doLayout()
        {
            if (getComponentCount() > 0)
            {
                // The panel will only ever have one child component: a ToolContainer
                Component child = getComponent(0);
                child.setBounds(0, 0, getWidth(), getHeight());
            }
        }
    }

    /**
     * Button to be used for scrolling the toolbar.
     */
    protected static class ScrollableToolButton extends BasicArrowButton implements UIResource
    {
        public ScrollableToolButton(int direction)
        {
            super(direction,
                    UIManager.getColor("ToolBar.selected"),
                    UIManager.getColor("ToolBar.shadow"),
                    UIManager.getColor("ToolBar.darkShadow"),
                    UIManager.getColor("ToolBar.highlight"));
        }
    }

    /**
     * Handler for any necessary listener interfaces.
     */
    protected class Handler implements PropertyChangeListener
    {
        @Override
        public void propertyChange(PropertyChangeEvent e)
        {
            String propertyName = e.getPropertyName();
            //noinspection SwitchStatementWithoutDefaultBranch
            switch (propertyName)
            {
                case "orientation" ->
                {
                    // Search for JSeparator components and change their orientation to correspond to the toolbar
                    int toolCount = toolBar.getToolCount();
                    final boolean horizontalOrientation = isHorizontalOrientation();

                    for (int i = 0; i < toolCount; i++)
                    {
                        if (toolBar.getToolAtIndex(i) instanceof JToolBar.Separator separator)
                        {
                            separator.setOrientation(horizontalOrientation ? JSeparator.VERTICAL : JSeparator.HORIZONTAL);
                            Dimension size = separator.getSeparatorSize();
                            if (size != null && size.width != size.height)
                            {
                                //noinspection SuspiciousNameCombination
                                Dimension newSize = new Dimension(size.height, size.width);
                                separator.setSeparatorSize(newSize);
                            }
                        }
                    }

                    toolScroller.createButtons();
                    calculatedBaseline = false;
                }
                case TOOL_ADDED ->
                {
                    JComponent c = (JComponent) e.getNewValue();
                    if (toolContainer == null)
                    {
                        installToolContainer();
                    } else
                    {
                        final int indexOfTool = toolBar.getIndexOfTool(c);
                        toolContainer.add(c, indexOfTool);
                    }

                    toolBar.revalidate();
                    toolBar.repaint();
                    calculatedBaseline = false;
                }
                case TOOL_REMOVED ->
                {
                    JComponent c = (JComponent) e.getOldValue();
                    if (toolContainer != null) toolContainer.remove(c);

                    toolBar.revalidate();
                    toolBar.repaint();
                    calculatedBaseline = false;
                }
            }
        }
    }

    /**
     * Panel for containing all the tools so they can be added to a viewport.
     */
    protected class ToolContainer extends JPanel implements UIResource
    {
        private boolean notifyToolBar = true;

        public ToolContainer()
        {
            super(null);
        }

        @Override
        protected void addImpl(Component comp, Object constraints, int index)
        {
            // When a separator is added to a toolbar, the orientation is set based on the orientation of the toolbar.
            // Since we aren't adding the separator directly to a toolbar, we have to do it ourselves here.
            if (comp instanceof JToolBar.Separator separator)
            {
                separator.setOrientation(isHorizontalOrientation() ? JSeparator.VERTICAL : JSeparator.HORIZONTAL);
            }

            if (!focusableButtons) setFocusableIfButton(comp, false);

            super.addImpl(comp, constraints, index);
        }

        /**
         * Remove a tool from the container. The purpose of this overload is to simply remove the tool from the
         * container without trying to remove it from the toolbar itself, since that is the source of the removal.
         * 
         * @param tool The tool to remove
         */
        public void remove(JComponent tool)
        {
            super.remove(tool);
            if (!focusableButtons) setFocusableIfButton(tool, true);
        }

        @Override
        public void remove(Component comp)
        {
            int index = toolBar.getComponentIndex(comp);
            super.remove(comp);
            if (notifyToolBar && index != -1) toolBar.removeTool(index);
        }

        @Override
        public void doLayout()
        {
            // We layout toolComponents in JToolBar's layout manager and use this method as a hook for repainting tools
            // to update tools area e.g. when the size of toolComponent was changed
            toolScroller.toolPanel.repaint();
            toolScroller.updateView();
        }
    }
}
