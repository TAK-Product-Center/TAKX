package gov.takx.core.globe.utils;

import java.util.ArrayList;
import java.util.List;

/**
 * This class holds the rendering solution for a given viewport view.  It holds a set of quadrant solutions, one
 * per tree that has data to be rendered in the current view.
 *
 * @since 1.14
 */
public class RenderingSolution
{
    private int totalDetailCount = 0;
    private int totalCellCount = 0;
    private boolean detailLevel = false;
    private int detailThreshold = Integer.MAX_VALUE;
    private int level = 0;

    private final List<RenderingQuadrantSolution> quadrantSolutions = new ArrayList<RenderingQuadrantSolution>();

    public RenderingSolution()
    {
    }

    /**
     * Add a quadrant solution to the full solution set.
     *
     * @param tree        The summary tree in which this is a rendering solution
     * @param cellList    The list of cells to be rendered
     * @param level       The level in the tree at which to render
     * @param detailCount The number of detail elements in the scene in this tree
     */
    public void addQuadrantSolution(SummaryQuadTree tree, List<CellData> cellList, int level, int detailCount)
    {
        totalDetailCount += detailCount;
        totalCellCount += cellList.size();
        this.level = level;
        detailLevel = (level == tree.getNumLevels() - 1); // Detail if at max level

        quadrantSolutions.add(new RenderingQuadrantSolution(tree, cellList, level, detailCount));
    }

    /**
     * @return The total detail element count from all the quadrant solutions
     */
    public int getTotalDetailCount()
    {
        return totalDetailCount;
    }

    /**
     * @return Total number of cells in the solution set
     */
    public int getTotalCellCount()
    {
        return totalCellCount;
    }

    /**
     * @return The level being rendered by this solution
     */
    public int getLevel()
    {
        return level;
    }

    /**
     * @return {@code true} if the level in the solution is the max level in the tree (the detail level)
     */
    public boolean isDetailLevel()
    {
        return detailLevel;
    }

    /**
     * @return Detail rendering threshold
     */
    public int getDetailThreshold()
    {
        return detailThreshold;
    }

    /**
     * Set the detail rendering threshold.  Any cell with a count above this threshold should be rendered as a
     * group even if we are rendering at the detail level.
     *
     * @param detailThreshold new detail threshold
     */
    public void setDetailThreshold(int detailThreshold)
    {
        this.detailThreshold = detailThreshold;
    }

    /**
     * @return The list of quadrant solutions
     */
    public List<RenderingQuadrantSolution> getQuadrantSolutions()
    {
        return quadrantSolutions;
    }
}
