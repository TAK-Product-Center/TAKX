package gov.takx.core.globe.utils;

/**
 * Helper callback methods to determine the visibility of styling context and visible counts.
 *
 * @see StylingContextKey
 * @see SummaryCounter
 */
public interface IVisibilityHelper
{
    /**
     * The index into the visible count data array holding the number of visible groups.
     */
    int GROUP_COUNT = 0;

    /**
     * The index into the visible count data array holding the total count of all visible map points.
     */
    int TOTAL_COUNT = 1;

    /**
     * Get the count of visible track points on the given summary counter instance.
     *
     * @param summaryCounter Counter to process
     * @return Number of visible track points represented in the counter
     */
    int getVisibleCount(SummaryCounter summaryCounter);

    /**
     * Given a SummaryCounter, fill an array of two counts: the number of entries (style groups) that are visible
     * and the total count of all visible map points.
     * <p>
     * During the calculation of the visible counts, the last visible styling context key is tracked.  This is
     * needed during scene calculations when trying to locate a singleton to render in a given cell.
     * <p/>
     * NOTE: The array is passed in to avoid allocation of numerous arrays during scene generation.
     *
     * @param summaryCounter SummaryCounter instance to process
     * @param countData      array of two counts: the number of entries (style groups) that are visible and the total
     *                       count of all visible map points in those visible groups
     * @see #GROUP_COUNT
     * @see #TOTAL_COUNT
     */
    void getVisibleCountData(SummaryCounter summaryCounter, int[] countData);

    /**
     * Determine if the elements controlled by the given context key are visible.
     *
     * @param scKey Styling context key to test
     * @return {@code true} if the elements are visible, {@code false} otherwise
     */
    boolean isStylingContextVisible(StylingContextKey scKey);

    /**
     * Determine if the elements controlled by the given context's map key are visible.
     *
     * @param scMapKey Styling context key to test
     * @return {@code true} if the elements are visible, {@code false} otherwise
     */
    boolean isStylingContextVisible(long scMapKey);

    /**
     * Return the last SC key that was found to be visible within the last call to {@link #getVisibleCount(SummaryCounter)} or {@link #getVisibleCountData(SummaryCounter, int[])}.
     *
     * @return Last visible styling context key
     * @since 2.8
     */
    long getLastVisibleStylingContextKey();
}
