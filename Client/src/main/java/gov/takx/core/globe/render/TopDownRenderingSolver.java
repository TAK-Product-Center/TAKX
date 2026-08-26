package gov.takx.core.globe.render;

import gov.nasa.worldwind.geom.Sector;
import gov.takx.core.globe.utils.CellData;
import gov.takx.core.globe.utils.IVisibilityHelper;
import gov.takx.core.globe.utils.RenderingSolution;
import gov.takx.core.globe.utils.SummaryQuadTree;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.invoke.MethodHandles;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * This class is responsible for finding the best rendering solution for the current WW viewport view as defined
 * by a bounding sector and a level number in the top-level quad tree in the rendering model (which correlates to
 * an eye altitude).
 *
 * @since 1.14
 */
public class TopDownRenderingSolver
{
    private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    // An empty solution for use when there is no data to render
    private static final RenderingSolution EMPTY_SOLUTION = new RenderingSolution();

    private final RenderingModel model;
    private final IVisibilityHelper visibilityHelper;
    private int renderingGoal;

    /**
     * Construct on a given model.
     *
     * @param model            The rendering model to operate upon
     * @param renderingGoal    The target number of elements to render
     * @param visibilityHelper Provider of visibility callbacks
     */
    public TopDownRenderingSolver(RenderingModel model, int renderingGoal, IVisibilityHelper visibilityHelper)
    {
        this.model = model;
        this.renderingGoal = renderingGoal;
        this.visibilityHelper = visibilityHelper;
    }

    /**
     * Find the optimal rendering solution given a bounding sector (of the view) and the eye level in the top tree.
     *
     * @param boundingSector Bounding sector of view to render
     * @param topLevel       Level in the top tree that matches the current view
     * @return The rendering solution
     * @see RenderingSolution
     */
    public RenderingSolution findRenderingSolution(Sector boundingSector, int topLevel) throws InterruptedException
    {
        testInterrupted();

        // We need to find all the detail trees that hold data intersecting the visible bounding sector
        final List<SummaryQuadTree> detailTrees = model.findDetailTrees(boundingSector);

        if (detailTrees.isEmpty())
        {
            if (logger.isDebugEnabled()) logger.debug("Nothing to render");
            return EMPTY_SOLUTION;
        }

        // Determine the proper level in the detail trees at which to operate
        int detailLevel = detailTrees.get(0).findMatchingLevel(boundingSector);

        // This odd calculation is an attempt to control the depth of rendering when at large elevations.
        // If we're still at detailLevel 0 (high elevation), then we only want to descend a few levels
        // into the detail tree to determine what to render.  It's just not useful to render 1000 things
        // all piled up on top of each other when the user's eye altitude is still large.
        // Also, as we descend more deeply into the detail tree, we want to try an render more detail.
        // At lower eye altitudes, you want to see more detail.

        final SummaryQuadTree topQuadrants = model.getTopTree();

        int maxIterations = (detailLevel == 0)
                ? 6 - ((topQuadrants.getNumLevels() - 1) - topLevel)
                : (detailLevel <= 3) ? 7 : 10;

        return findOptimumLevel(detailTrees, detailLevel, boundingSector, maxIterations);
    }

    /**
     * Find the optimum level in the quad trees at which to render the scene.  This is a fairly simple search down the
     * tree levels until we find one that has the right number of elements to render.
     *
     * @param detailTrees    The list of quad trees to process
     * @param level          The starting level for the search
     * @param boundingSector The bounding sector for the view we are trying to render
     * @param maxIterations  maximum number of iterations (level descent) to make.  This value may be overridden in
     *                       some cases if the algorithm detects a more optimal rendering solution. The primary use of
     *                       this parameter is to prevent high level views (ones with large eye altitudes) from showing
     *                       too many elements.
     * @return The rendering solution
     */
    public RenderingSolution findOptimumLevel(List<SummaryQuadTree> detailTrees, int level, Sector boundingSector, int maxIterations) throws InterruptedException
    {
        final int startLevel = level;
        final int numLevels = detailTrees.get(0).getNumLevels();
        int maxLevel = numLevels - 1;
        level -= 1; // So we start at the right level

        final int detailTreeCount = detailTrees.size();

        int[] solutionCounts = new int[detailTreeCount];
        List<List<CellData>> solutionCellLists = null;
        int[][] solutionTreeCellCounts = new int[detailTreeCount][];
        int solutionLevel = 0;
        int solutionVisibleCount = 0;
        int solutionCellCount = 0;

        // treeCellCounts is used to hold the visible track count in each cell processed.  We capture it during
        // this loop so we don't have to re-calculate the visible counts down below after we find the solution level.
        int[][] treeCellCounts = new int[detailTreeCount][];

        List<List<CellData>> cellLists = new ArrayList<>(detailTreeCount);
        int groupCount; // Count of visible groups across all cells (the number of group markers we might create)
        int[] counts = new int[2];
        boolean keepGoing;

        do
        {
            int count = 0;
            int cellCount = 0;
            int max = 0;
            int min = Integer.MAX_VALUE;

            level += 1;
            cellLists.clear(); // New level, new data
            Arrays.fill(treeCellCounts, null);
            groupCount = 0;

            for (int treeNum = 0; treeNum < detailTreeCount; treeNum++)
            {
                SummaryQuadTree sqt = detailTrees.get(treeNum);
                int treeCount = 0;
                int treeGroupCount = 0;
                int treeCellCount = 0;

                List<CellData> cellList = sqt.findItemsInRegion(boundingSector, level);
                cellLists.add(cellList); // Keep track these lists so we don't have to recompute at the end
                treeCellCounts[treeNum] = new int[cellList.size()];

                for (CellData cd : cellList)
                {
                    testInterrupted();

                    cellCount += 1;
                    treeCellCount += 1;

                    visibilityHelper.getVisibleCountData(cd.getSummaryCounter(), counts);
                    int cellGroupCount = counts[IVisibilityHelper.GROUP_COUNT];
                    int visibleCount = counts[IVisibilityHelper.TOTAL_COUNT];

                    treeCount += visibleCount;
                    count += visibleCount;

                    if (visibleCount > 0)
                    {
                        groupCount += cellGroupCount;
                        treeGroupCount += cellGroupCount;

                        if (visibleCount > max) max = visibleCount;
                        if (visibleCount < min) min = visibleCount;
                    }

                    treeCellCounts[treeNum][treeCellCount - 1] = visibleCount;
                }

                solutionCounts[treeNum] = treeCount; // Keep track of the count per tree

                if (logger.isTraceEnabled())
                {
                    logger.trace("   Tree {} [{}/{}={}]", treeNum, cellList.size(), treeGroupCount, treeCount);
                }
            }

            // Don't include the count in the test below because we want to show as many group indicators as possible
            // and if we break out because the total count is too high, then we'll be bailing too early

            if (groupCount < renderingGoal || solutionCellLists == null)
            {
                solutionCellLists = new ArrayList<>(cellLists); // Copy it
                System.arraycopy(treeCellCounts, 0, solutionTreeCellCounts, 0, treeCellCounts.length);
                solutionLevel = level;
                solutionVisibleCount = count;
                solutionCellCount = cellCount;
            }

            // We may go past the maxIterations limit if the current level has a summary count within 50% of
            // the total count.  This basically allows to render a lower level when we show more detail
            // without too much rendering cost.  In fact, we can warp to the detail level if this case holds
            // since the ratio of cells to total count can never increase as we descend levels.

            keepGoing = groupCount > 0 && groupCount < renderingGoal && count < renderingGoal && ((count / groupCount) < 1.5);

            if (logger.isTraceEnabled())
            {
                logger.trace("SQT iterate {} [{}/{}={} @ {}], range [{}, {}] mi={} keepGoing={}",
                        (level < maxLevel ? "SUMMARY" : " DETAIL"), cellLists.size(), groupCount, count, level,
                        min, max, maxIterations, keepGoing);
            }

            if (keepGoing && level < maxLevel - 1)
            {
                level = maxLevel - 1;
                if (logger.isTraceEnabled()) logger.trace("WARP TO DETAIL");
            }

            // If we ever have a group count of 0, then we can bail immediately since there can't be any data at
            // deeper levels in the tree.

        } while (groupCount > 0 && groupCount < renderingGoal && level < maxLevel && (--maxIterations > 0 || keepGoing));

        testInterrupted();

        // Construct the rendering solution

        RenderingSolution solution = new RenderingSolution();

        // If we are at the detail level and we have a total count that exceeds our rendering goal, try to find
        // the rendering limit that will allow us to show as many detail points as possible.  We're essentially
        // finding the cell count threshold above which we render as groups and below which we render individuals.

        int threshold = Integer.MAX_VALUE;

        if (solutionCellCount == 1 && solutionLevel == maxLevel && solutionVisibleCount > renderingGoal)
        {
            // If we have a single cell and it has too much data, then set the threshold to the rendering goal
            solution.setDetailThreshold(renderingGoal);
        } else if (solutionCellCount > 1 && solutionLevel == maxLevel && solutionVisibleCount > renderingGoal)
        {
            int cellCounts[] = new int[solutionCellCount];
            int countIndex = 0;

            // First we need a sorted list of the cell visible counts.  Combine all the cell count arrays
            // into one larger array.

            for (int treeNum = 0; treeNum < detailTreeCount; treeNum++)
            {
                final int[] treeCounts = solutionTreeCellCounts[treeNum];
                final int length = treeCounts.length;

                System.arraycopy(treeCounts, 0, cellCounts, countIndex, length);
                countIndex += length;
            }

            Arrays.sort(cellCounts); // get it sorted

            if (logger.isTraceEnabled())
            {
                final int avg = solutionCellCount > 0 ? solutionVisibleCount / solutionCellCount : 0;
                logger.trace("Cell Count: {} total={} avg = {}", solutionCellCount, solutionVisibleCount, avg);

                StringBuilder sb = new StringBuilder();
                sb.append("top cell counts: ");

                for (int i = 1; i <= 20 & i <= solutionCellCount; i++)
                {
                    sb.append(cellCounts[solutionCellCount - i]).append(", ");
                }

                logger.trace(sb.toString());
            }

            // Scan down the counts and set the threshold to each subsequent count until we get to a total count
            // that is renderable.

            int currentTotal = solutionVisibleCount;
            countIndex = solutionCellCount - 1;

            while (countIndex >= 0 && currentTotal > renderingGoal)
            {
                testInterrupted();

                int cellCount = cellCounts[countIndex];

                currentTotal -= cellCount;
                threshold = cellCount;
                countIndex -= 1;
                if (logger.isTraceEnabled())
                {
                    logger.trace("CT={} threshold={} CI={}", currentTotal, threshold, countIndex);
                }
            }

            // Threshold is the number we want, install it into the solution

            if (logger.isDebugEnabled()) logger.debug("FINAL threshold={} new total={}", threshold, currentTotal);
            solution.setDetailThreshold(threshold);
        }

        // Now add all the quadrant solution entries to the total solution

        for (int treeNum = 0; treeNum < detailTreeCount; treeNum++)
        {
            testInterrupted();
            solution.addQuadrantSolution(detailTrees.get(treeNum), solutionCellLists.get(treeNum), solutionLevel, solutionCounts[treeNum]);
        }

        if (logger.isDebugEnabled())
        {
            logger.debug("SQT OPTIMUM [{}/{}={} @{}] threshold={} sl={}", solution.getTotalCellCount(), groupCount,
                    solution.getTotalDetailCount(), solutionLevel, threshold, startLevel);
        }

        return solution;
    }

    /**
     * Test to see if this thread has been interrupted, and, if ow, throw an InterruptedException.
     */
    private void testInterrupted() throws InterruptedException
    {
        if (Thread.interrupted()) throw new InterruptedException();
    }
}
