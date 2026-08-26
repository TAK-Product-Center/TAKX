package gov.takx.core.view.gui.mapobjectmanager;

import gov.nasa.worldwind.render.DrawContext;
import gov.nasa.worldwind.util.tree.ScrollFrame;

import java.awt.Color;
import java.awt.Point;

/**
 * Custom implementation of the WorldWind {@link ScrollFrame}.
 * <p/>
 * Custom implementation eliminates the title bar and the ability to drag/minimize the frame.
 * The frame will resize to accommodate the current number of nodes being displayed.
 * Further, it keeps its scroll bar color in line with the scroll bar color currently being
 * used by the LAF.
 *
 * @since 1.11
 */
public class DataMonitorScrollFrame extends ScrollFrame // implements SkinChangeListener // TODO KMB: Fix for FlatLAF (TAKX-739)
{
    /**
     * Constructor.
     */
    public DataMonitorScrollFrame()
    {
        super();

        // No title bar will be displayed, so allocate
        // 0 pixels for it.
        titleBarHeight = 0;

        // No frame border will be displayed, so allocate
        // 0 pixels for it.
        frameBorder = 0;

        // Set the color of the border drawn around the
        // scroll bar.
        verticalScrollBar.setLineColor(Color.black);

        // Match the scroll bar's color to that currently being
        // used by Substance.
//        skinChanged(); // TODO KMB: Fix for FlatLaf (TAKX-739)

        // Listen for changes to the look and feel so that the
        // scroll bar color can be updated accordingly.
//        SubstanceLookAndFeel.registerSkinChangeListener(this); // TODO KMB: Fix for FlatLaf (TAKX-739)
    }

    /**
     * The Substance skin has changed. Update the color of the vertical scroll bar.
     */
    // TODO KMB: Fix for FlatLAF (TAKX-739)
//    @Override
//    public void skinChanged()
//    {
//        SubstanceColorScheme scheme =
//                SubstanceLookAndFeel.getCurrentSkin().getColorScheme(new JScrollBar(), ComponentState.DEFAULT);
//        verticalScrollBar.setKnobColor(scheme.getLightColor(), scheme.getDarkColor());
//    }

    /**
     * By default, WorldWind uses a different color for the scroll bar border when the
     * frame is selected. This is not consistent with the rest of TAKX, so here the color
     * of the border is forced back to what we want it to be.
     *
     * @param highlighted Unused.
     */
    @Override
    public void setHighlighted(boolean highlighted)
    {
        super.setHighlighted(highlighted);

        // force back after the color is changed to the foreground color
        verticalScrollBar.setLineColor(Color.black);
    }

    /**
     * Ignore calls to draw the title bar.
     *
     * @param dc Unused.
     */
    @Override
    protected void drawTitleBar(DrawContext dc)
    {
        // don't draw the title bar
    }

    /**
     * Ignore calls to start dragging.
     *
     * @param point Unused.
     */
    @Override
    protected void beginDrag(Point point)
    {
        // don't allow drag
    }

    /**
     * Ignore calls to drag the frame.
     *
     * @param point Unused.
     */
    @Override
    public void drag(Point point)
    {
        // don't allow drag
    }

    /**
     * Ignore calls to minimize the frame.
     *
     * @param minimized Unused.
     */
    @Override
    public void setMinimized(boolean minimized)
    {
        // don't allow minimization
    }
}
