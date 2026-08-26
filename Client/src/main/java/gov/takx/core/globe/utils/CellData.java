package gov.takx.core.globe.utils;

import gov.takx.core.mapdata.TrackPoint;

import java.util.List;

/**
 * Simple container to hold information about a cell in a {@link }}SummaryQuadTree.  It holds the cell Id for that
 * cell (the caller must keep track of the level in the tree, it's not tracked here), the summary counter for that
 * cell, and, if the cell is a detail cell (lowest cell in the tree), the list of track points located in that cell.
 *
 * @see SummaryQuadTree
 * @since 1.13
 */
public final class CellData
{
    private final int cellId;
    private final SummaryCounter summaryCounter;
    private final List<TrackPoint> points;

    /**
     * @param cellId         Id of the cell containing this data
     * @param summaryCounter Summary counts at that cell
     */
    public CellData(int cellId, SummaryCounter summaryCounter)
    {
        this.cellId = cellId;
        this.summaryCounter = summaryCounter;
        this.points = null;
    }

    /**
     * @param cellId         Id of the cell containing this data
     * @param summaryCounter Summary counts at that cell
     * @param points         The list of points at that cell
     */
    public CellData(int cellId, SummaryCounter summaryCounter, List<TrackPoint> points)
    {
        this.cellId = cellId;
        this.points = points;
        this.summaryCounter = summaryCounter;
    }

    /**
     * @return The Id of the cell
     */
    public int getCellId()
    {
        return cellId;
    }

    /**
     * @return {@code true} if this cell is a "summary" cell, i.e., not the lowest cell in the tree
     */
    public boolean isSummary()
    {
        return points == null;
    }

    /**
     * @return The {@link SummaryCounter} associated with the cell
     */
    public SummaryCounter getSummaryCounter()
    {
        return summaryCounter;
    }

    /**
     * @return The points contained in the cell, {@code null} if {@link #isSummary()} returns {@code true}
     */
    public List<TrackPoint> getPoints()
    {
        return points;
    }

    @Override
    public boolean equals(Object o)
    {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        return cellId == ((CellData) o).cellId;
    }

    @Override
    public int hashCode()
    {
        return cellId;
    }
}
