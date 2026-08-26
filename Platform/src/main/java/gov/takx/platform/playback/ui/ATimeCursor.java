package gov.takx.platform.playback.ui;

import javax.swing.JComponent;

/**
 * Abstract base class for a Time cursor.  A time cursor is intended to represent a location along a time line
 * and is typically rendered on a {@link TimePanel}.
 *
 * @since 1.47
 */
public abstract class ATimeCursor extends JComponent
{
    protected int x = -1;
    protected int height = -1;

    /**
     * Set the center x-location for this cursor.  Note that this is the <b>center</b> location for the cursor.  For
     * time cursors only a pixel or two wide (i.e. lines), this isn't really a concern.  However, this is important when
     * rendering a cursor with a shape or graphic.
     *
     * @param x the center x-location for this cursor.
     */
    public void setXLocation(int x)
    {
        if (this.x != x)
        {
            this.x = x;
            refresh();
        }
    }

    /**
     * @return the center x-location of this marker
     */
    public int getXLocation()
    {
        return x;
    }

    /**
     * Set this cursor's height based on the height of its owner.  The owner is the Component on which this cursor is
     * being painted.
     *
     * @param height the height of this cursor's owner.
     */
    public void setHeight(int height)
    {
        if (this.height != height)
        {
            this.height = height;
            refresh();
        }
    }

    /**
     * Refresh any data/configuration that relies on the X position and/or the height.  Default implementation does
     * nothing.
     */
    protected void refresh()
    {
    }
}
