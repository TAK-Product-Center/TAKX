package gov.takx.core.globe.utils;

import com.koloboke.collect.set.IntSet;
import gov.takx.core.mapdata.TrackPoint;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.invoke.MethodHandles;
import java.util.List;

/**
 * Batch updater to install a set of RDM-level TSD assignments into a quad tree.
 *
 * @since 1.25
 */
public class TSDAssignmentUpdater implements IBatchTreeUpdater
{
    private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    private final TSDAssignmentSet assignments;

    /**
     * Construct an updater for the given TSD assignments.
     *
     * @param assignments Assignments to install into the tree
     */
    public TSDAssignmentUpdater(TSDAssignmentSet assignments)
    {
        this.assignments = assignments;
    }

    /**
     * {@inheritDoc}
     *
     * @return true if the counter has any counts affected by the assignments
     */
    @Override
    public boolean isCounterAffected(SummaryCounter counter)
    {
        return counter.hasAffectedCounts(assignments);
    }

    @Override
    public SummaryCounter processCellData(SummaryQuadTree tree, SummaryCounter originalCounter, List<TrackPoint> points)
    {
        final SummaryCounter newCounter = new SummaryCounter();
        boolean pointChanged = false;

        for (TrackPoint point : points)
        {
            final int rdmId = point.rdmId;
            final TSDAssignment tsda = assignments.get(rdmId);

            if (tsda != null)
            {
                // This RDM is in the list of assignments
                point.setRdmTsdId(tsda.newTsdId);
                pointChanged = true;

                if (logger.isDebugEnabled()) logger.debug("Updated with new rdmTsdId: {}", point);
            }

            tree.bumpCounter(newCounter, point); // Let the tree do the counter bump
        }

        // If we didn't change any points, then we don't need to adjust the counts
        return pointChanged ? newCounter : null;
    }

    @Override
    public IntSet getCellsToProcess()
    {
        return null; // Process all cells that have data
    }
}
