package gov.takx.core.globe.utils;

import com.koloboke.collect.set.IntSet;
import gov.takx.core.mapdata.TrackPoint;

import java.util.List;

/**
 * Interface defining operations that need to update a {@link SummaryQuadTree} in a batch mode.  This is typically
 * done when updating counts for changes like time thresholds or the assignment of RDM-level TSDs.
 *
 * @since 1.25
 */
public interface IBatchTreeUpdater
{
    /**
     * Determine if the given summary counter is affected by the batch operation we're performing.
     *
     * @param counter The counter to test
     * @return {@code true} if the counter would be affected by this operation
     */
    boolean isCounterAffected(SummaryCounter counter);

    /**
     * Process the list of points and summary counter for a cell in the tree.  This should make any modifications
     * necessary to the points in the list and produce a new summary counter to be applied to this cell and all
     * its parent cells.
     * <p/>
     * If our batch operation doesn't affect this cell's points, then return {@code null}.
     *
     * @param tree            The quad tree we are operating upon, it is used to perform counter increments in a "smart" way
     * @param originalCounter The original (current) counter associated with the given point list
     * @param points          The list of points in the cell
     * @return A new summary counter to be applied up the tree, {@code null} if the cell is not affected
     */
    SummaryCounter processCellData(SummaryQuadTree tree, SummaryCounter originalCounter, List<TrackPoint> points);

    /**
     * If the updater has first determined the affected cells, then it can specify which cells to process during
     * the update.  If not, it should just return {@code null} here.
     *
     * @return Iterable of cell Ids to process, or {@code null} to let the tree process all cells that contain data
     */
    IntSet getCellsToProcess();
}
