package gov.takx.api.playback;

import gov.takx.api.coordinatesystem.ILocation;

/**
 * Collection of analytical boundaries, including a chronological boundary, a geographical boundary, a singular
 * geographic point, and a singular chronological position. It is possible to combine these features together. For
 * example, a boundary could be defined to represent a period of time within a certain geographic area, or a
 * specific activity at a specific point at a specific time, or perhaps across a period of time.
 * <p/>
 * <b>NOTE:</b> Developers must rely on {@link #hasChronologicalBoundary()}, {@link #hasGeographicBoundary()},
 * {@link #hasMoment()}, and {@link #hasPoint()} to determine if the corresponding getter methods contain valid data!
 *
 * @since 1.47
 */
@SuppressWarnings("UnusedDeclaration")
public interface IMarkerBoundary
{
    /**
     * @return {@code true} if this boundary includes chronological end points.
     * @see #setChronologicalBoundary(long, long)
     * @see #clearChronologicalBoundary()
     */
    boolean hasChronologicalBoundary();

    /**
     * Set the start and end time of the chronological boundary.
     *
     * @param startTime Start of time boundary
     * @param endTime   End of time boundary
     */
    void setChronologicalBoundary(long startTime, long endTime);

    /**
     * Clear any time region boundary.  After calling this method, {@link #hasChronologicalBoundary()} will return
     * {@code false}.
     */
    void clearChronologicalBoundary();

    /**
     * @return The Start time of the time boundary, may be {@code null} if not set
     */
    Long getStartTime();

    /**
     * @return The End time of the time boundary, may be {@code null} if not set
     */
    Long getEndTime();

    /**
     * Identifies if this boundary includes a rectangular geographical area.
     *
     * @return true, if {@link #getGeographicBoundary()} contains valid data
     * @see #setGeographicBoundary(ILocation[])
     */
    boolean hasGeographicBoundary();

    /**
     * Geographical area (rectangular) represented by this boundary. Data is only valid when
     * {@link #hasGeographicBoundary()}  is {@code true}.
     *
     * @return Top-right location in position 0, bottom left location in position 1
     */
    ILocation[] getGeographicBoundary();

    /**
     * Defines a geographical area (rectangular) for this boundary, and ensures {@link #hasGeographicBoundary()} returns
     * {@code true}.  If the specified boundary is {@code null}, then future calls to {@link #hasGeographicBoundary()}}
     * will return {@code false}.
     *
     * @param boundary Top-right location in position 0, bottom left location in position 1, {@code null} to clear
     *                 geographic boundary
     */
    void setGeographicBoundary(ILocation[] boundary);

    /**
     * Identifies if this boundary includes a specific geographical point of interest.
     *
     * @return true, if {@link #getPoint()} contains valid data
     */
    boolean hasPoint();

    /**
     * Specific geographic location identified by this boundary. Data is only valid when {@link #hasPoint()} is
     * {@code true}.
     *
     * @return Specific geographic point
     */
    ILocation getPoint();

    /**
     * Defines a specific geographical point of interest for this boundary, and ensures {@link #hasPoint()} returns
     * {@code true}.  If the specified point is {@code null}, then future calls to {@link #hasPoint()}} will return
     * {@code false}.
     *
     * @param point Reference point, {@code null} to clear point
     */
    void setPoint(ILocation point);

    /**
     * Identifies if this boundary includes a specific chronological point of interest.
     *
     * @return true, if {@link #getMoment()} contains valid data
     * @see #setMoment(long)
     * @see #clearMoment()
     */
    boolean hasMoment();

    /**
     * Specific point in time defined by this boundary. Data is only valid when {@link #hasMoment()} is {@code true}.
     *
     * @return Specific point in time
     */
    Long getMoment();

    /**
     * Defines a specific point in time for this boundary, and ensures {@link #hasMoment()} returns {@code true}.
     *
     * @param moment Point in time in ms
     */
    void setMoment(long moment);

    /**
     * Remove any previously set time moment from this boundary.  After calling this method {@link #hasMoment()} will
     * return {@code false}.
     */
    void clearMoment();
}
