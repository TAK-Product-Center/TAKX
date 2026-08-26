package gov.takx.core.globe.utils;

import gov.takx.core.mapdata.TrackPoint;

import java.util.List;

/**
 * Contract for objects that need to visit all the cells in a quad tree that have data in them.  All cells
 * containing data will be visited, but there is no ordering guarantee.
 *
 * @since 1.34
 */
public interface ITreeVisitor
{
    /**
     * Visit a given cell.
     *
     * @param cellId  Id of cell within the tree
     * @param counter The summary counter for that cell
     * @param points  The list of track points for that cell (will be non-empty)
     */
    void visitCell(int cellId, SummaryCounter counter, List<TrackPoint> points);
}
