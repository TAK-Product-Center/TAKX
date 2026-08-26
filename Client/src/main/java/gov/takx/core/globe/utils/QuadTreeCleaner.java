package gov.takx.core.globe.utils;

import com.koloboke.collect.IntCursor;
import com.koloboke.collect.set.IntSet;
import com.koloboke.collect.set.hash.HashIntSets;
import gov.takx.core.mapdata.TrackPoint;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.invoke.MethodHandles;
import java.util.Iterator;
import java.util.List;

/**
 * Instances of this class handle the deletion of MapObject data (track points) from a given quad tree.
 *
 * @see SummaryQuadTree
 * @since 1.34
 */
public class QuadTreeCleaner
{
    private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    private static final int BATCH_SZ = 25;

    private final SummaryQuadTree tree;
    private final int moId;
    private final long timeOfDeletion;

    /**
     * Construct a cleaner on the given tree for the MapObject and deletion time.
     *
     * @param tree           quad tree to process
     * @param moId           Id of MapObject being deleted
     * @param timeOfDeletion Cutoff time for partial deletes (use {@link Long#MAX_VALUE} to delete all data for the MO
     */
    public QuadTreeCleaner(SummaryQuadTree tree, int moId, long timeOfDeletion)
    {
        this.tree = tree;
        this.moId = moId;
        this.timeOfDeletion = timeOfDeletion;
    }

    /**
     * Perform the tree update.
     */
    public void execute()
    {
        final CellGathererForDeletion gatherer = new CellGathererForDeletion(moId);

        tree.applyVisitor(gatherer);

        // Now batch up the affected cell list into reasonable chunks
        final IntSet affectedCells = gatherer.getAffectedCells();
        final int cellCount = affectedCells.size();
        final IntCursor iterator = affectedCells.cursor();

        int i = 0;

        while (i < cellCount)
        {
            int batchEnd = Math.min((i + BATCH_SZ), cellCount);

            if (logger.isDebugEnabled()) logger.debug("Process cells {}-{}", i, batchEnd);

            final IntSet subList = HashIntSets.newUpdatableSet(BATCH_SZ);

            while (i < batchEnd)
            {
                iterator.moveNext();
                subList.add(iterator.elem());
                i++;
            }

            final BatchDeleter deleter = new BatchDeleter(subList, moId, timeOfDeletion);

            // This is the level of synchronization.  Each call here will lock the tree, but hopefully for
            // a very short period since we're only processing a few cells at a time.

            tree.applyBatchUpdate(deleter);

            i = batchEnd;
        }
    }

    /**
     * Tree visitor to collect the ids of all the cells that will potentially be affected by the deletion
     * of the given map object's data.
     */
    private static final class CellGathererForDeletion implements ITreeVisitor
    {
        private final int moId;
        private final IntSet affectedCells = HashIntSets.newUpdatableSet(1000);

        CellGathererForDeletion(int moId)
        {
            this.moId = moId;
        }

        @Override
        public void visitCell(int cellId, SummaryCounter counter, List<TrackPoint> points)
        {
            // If this cell has any counts associated with this MO, then gather it
            if (counter.hasCountForMapObject(moId))
            {
                affectedCells.add(cellId);
            }
        }

        /**
         * @return The list of cells that were determined to be affected by the delete
         */
        @SuppressWarnings("AssignmentOrReturnOfFieldWithMutableType")
        IntSet getAffectedCells()
        {
            return affectedCells;
        }
    }

    /**
     * Delete data associated with a given MapObject according to the time of deletion.  Process a given set of
     * tree cells.
     */
    private static final class BatchDeleter implements IBatchTreeUpdater
    {
        private final int moId;
        private final IntSet cellsToProcess;
        private final long timeOfDeletion;

        /**
         * @param cellsToProcess List of cells to process
         * @param moId           ID of map object to process
         * @param timeOfDeletion Cutoff time for partial deletes (use {@link Long#MAX_VALUE} to delete all data for the MO
         */
        BatchDeleter(IntSet cellsToProcess, int moId, long timeOfDeletion)
        {
            this.cellsToProcess = cellsToProcess;
            this.moId = moId;
            this.timeOfDeletion = timeOfDeletion;
        }

        @Override
        public boolean isCounterAffected(SummaryCounter counter)
        {
            return true; // we know it is
        }

        @Override
        public SummaryCounter processCellData(SummaryQuadTree tree, SummaryCounter originalCounter, List<TrackPoint> points)
        {
            if (points.isEmpty()) return null; // No data here

            int removedCount = 0;
            final Iterator<TrackPoint> iterator = points.iterator();

            while (iterator.hasNext())
            {
                TrackPoint trackPoint = iterator.next();
                if (trackPoint.moId == moId && trackPoint.time <= timeOfDeletion)
                {
                    if (logger.isTraceEnabled()) logger.trace("Remove: {}", trackPoint);

                    iterator.remove();
                    removedCount += 1;
                }
            }

            // If we actually removed any track points, then do a complete recount
            if (removedCount > 0)
            {
                if (logger.isDebugEnabled()) logger.debug("Removed {} points", removedCount);

                final SummaryCounter newCounter = new SummaryCounter();

                for (TrackPoint point : points)
                {
                    tree.bumpCounter(newCounter, point); // Let the tree do the counter bump
                }

                return newCounter;
            } else
            {
                return null; // Didn't change anything, so no counts updated
            }
        }

        @SuppressWarnings("AssignmentOrReturnOfFieldWithMutableType")
        @Override
        public IntSet getCellsToProcess()
        {
            return cellsToProcess;
        }
    }
}
