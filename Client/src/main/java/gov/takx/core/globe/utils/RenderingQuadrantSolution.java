package gov.takx.core.globe.utils;

import java.util.List;

/**
 * This simple data holder represents the rendering solution parameters for a single summary quad tree.
 *
 * @since 1.14
 */
public class RenderingQuadrantSolution
{
    public final SummaryQuadTree tree;
    public final List<CellData> cellList;
    public final int level;
    public final int detailCount;

    /**
     * Construct a solution with the given parameters.
     *
     * @param tree        The summary tree in which this is a rendering solution
     * @param cellList    The list of cells to be rendered
     * @param level       The level in the tree at which to render
     * @param detailCount The number of detail elements in the scene in this tree
     */
    public RenderingQuadrantSolution(SummaryQuadTree tree, List<CellData> cellList, int level, int detailCount)
    {
        this.tree = tree;
        this.cellList = cellList;
        this.level = level;
        this.detailCount = detailCount;
    }
}
