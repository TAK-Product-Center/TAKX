package gov.takx.core.globe.utils;

import com.koloboke.collect.set.IntSet;
import gov.takx.core.mapdata.TrackPoint;

import java.util.List;

/**
 * Batch updater to install a collection of time thresholds into a quad tree.
 *
 * @since 1.25
 */
public class TimeThresholdUpdater implements IBatchTreeUpdater
{
    /**
     * Construct an updater to update the tree with a set of time thresholds.  This updater doesn't have to do much
     * since the quad tree already knows about time thresholds.  We just perform the recount on every cell.
     */
    public TimeThresholdUpdater()
    {
    }

    /**
     * {@inheritDoc}
     * <p>
     * When we're removing, or changing the threshold on a cell that currently has a 0 count for any of the
     * keys we're operating upon, the key may not be on the counter (since 0 counts are removed), we pretty
     * much have to state that this update affects every cell.
     *
     * @return true
     */
    @Override
    public boolean isCounterAffected(SummaryCounter counter)
    {
        return true;
    }

    @Override
    public SummaryCounter processCellData(SummaryQuadTree tree, SummaryCounter originalCounter, List<TrackPoint> points)
    {
        // Scan the list of points and do a full recount.  This works because the tree's bumpCounter method
        // takes the already installed time thresholds into account.  We will end up with a counter that
        // contains what our new count should be.

        final SummaryCounter newCounter = new SummaryCounter();

        for (TrackPoint point : points)
        {
            tree.bumpCounter(newCounter, point); // Let the tree do the counter bump
        }

        return newCounter;
    }

    @Override
    public IntSet getCellsToProcess()
    {
        return null; // Process all cells that have data
    }
}
