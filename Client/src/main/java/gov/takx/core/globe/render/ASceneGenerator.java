package gov.takx.core.globe.render;

import gov.nasa.worldwind.geom.Sector;
import gov.takx.core.globe.utils.IVisibilityHelper;

import java.util.ArrayList;
import java.util.BitSet;
import java.util.List;

/**
 * Abstract base class for scene generators.  This base handles:
 * <ul>
 * <li>tests for, and tracking of, thread interrupt</li>
 * <li>storage for a list of detail tree sectors included in the scene</li>
 * <li>the frame number</li>
 * </ul>
 * tests for, and tracking of, thread interrupt.
 *
 * @since 2.8
 */
public abstract class ASceneGenerator implements ISceneGenerator
{
    protected final int frame;
    protected final RenderingContext renderingContext;
    protected final int renderingGoal;
    protected final IVisibilityHelper visibilityHelper;
    protected final BitSet mapObjectVisible; // High level visibility control for map objects
    protected final RenderingStyleContextManager scManager;

    protected boolean interrupted = false;
    protected final List<Sector> detailTreeSectors = new ArrayList<>();

    /**
     * Construct the scene generator.
     *
     * @param frame            The frame being calculated
     * @param renderingGoal    The target number of renderable objects
     * @param renderingContext The current rendering context
     * @param mapObjectVisible A set of flags indicating which Map Objects are visible
     */
    ASceneGenerator(int frame, RenderingContext renderingContext, int renderingGoal, IVisibilityHelper visibilityHelper, BitSet mapObjectVisible)
    {
        this.frame = frame;
        this.renderingContext = renderingContext;
        this.renderingGoal = renderingGoal;
        this.visibilityHelper = visibilityHelper;
        this.mapObjectVisible = mapObjectVisible;

        scManager = renderingContext.getStyleContextManager();
    }

    @Override
    public int getFrame()
    {
        return frame;
    }

    @Override
    public boolean isInterrupted()
    {
        return interrupted;
    }

    @Override
    public List<Sector> getDetailTreeSectors()
    {
        return detailTreeSectors;
    }

    /**
     * Test to see if this thread has been interrupted, and, if so, throw an InterruptedException.
     */
    protected void testInterrupted() throws InterruptedException
    {
        if (Thread.interrupted())
        {
            interrupted = true;
            throw new InterruptedException();
        }
    }
}
