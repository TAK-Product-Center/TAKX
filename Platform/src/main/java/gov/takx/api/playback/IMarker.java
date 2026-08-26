package gov.takx.api.playback;

import gov.takx.platform.playback.ui.ATimeCursor;
import jakarta.annotation.Nonnull;

import java.awt.Color;
import java.awt.event.MouseEvent;
import java.util.Collection;

/**
 * Identifies <i>something</i> of interest within a timeline. The following capabilities are supported:
 * <ul>
 * <li>Geographic Point</li>
 * <li>Geographic Rectangular Area, specified by top-left/bottom-right coordinates</li>
 * <li>Single moment in time</li>
 * <li>Single period of time</li>
 * </ul>
 * <p/>
 * There is nothing in the design that precludes the combination of these items. For example, a marker might identify
 * a specific geographic point at a specific moment in time, or it might identify a geographic area across a period
 * of time.
 *
 * @since 1.47
 */
public interface IMarker extends IMarkerBoundary
{
    /**
     * @return The color used to render the marker on the ruler and in registered playback lanes
     */
    Color getMarkerColor();

    /**
     * Set the color used to render the marker on the ruler and in registered playback lanes.
     *
     * @param markerColor new Marker color
     */
    void setMarkerColor(@Nonnull Color markerColor);

    /**
     * Get this marker's ruler cursor.  This cursor is special in that it is rendered on the ruler panel as a circular,
     * colored pin head.
     *
     * @return this marker's ruler cursor.
     */
    ATimeCursor getRulerCursor();

    /**
     * Update the cursor to be positioned at the given X offset.
     *
     * @param x New position
     */
    void updateCursorPosition(int x);

    /**
     * Specify whether this marker is rendered or not.
     *
     * @param visible Pass {@code true} to render marker, {@code false} to not
     */
    void setVisible(boolean visible);

    /**
     * @return Is this marker visible (rendered) or not
     */
    boolean isVisible();

    /**
     * @return The label (used in the tooltip text) of this marker
     */
    String getLabel();

    /**
     * Set the label for this marker. The label is used in the default tooltip text.
     *
     * @param label Label for marker
     */
    void setLabel(String label);

    /**
     * Obtain the list of {@link IPlaybackLane} instances that should render the marker.
     * <p/>
     * For example, if there are 9 playback lanes, and the marker should only apply to four
     * of the nine {@link IPlaybackLane} instances, then only include those four via calls to
     * {@link #addTargetLane(IPlaybackLane)}.
     *
     * @return Collection of {@link IPlaybackLane} instances that should render the marker.
     */
    Collection<IPlaybackLane> getTargetLanes();

    /**
     * Adds a non-null {@link IPlaybackLane} to the list of instances that should render the marker.
     *
     * @param lane Specific {@link IPlaybackLane} that should render the marker
     * @throws IllegalArgumentException if the referenced {@link IPlaybackLane} does not exist within the current set
     */
    void addTargetLane(@Nonnull IPlaybackLane lane);

    /**
     * Removes a previously registered {@link IPlaybackLane} from the list of lanes that should render the marker.
     *
     * @param lane Specific {@link IPlaybackLane} that should be removed from the list
     */
    void removeTargetLane(@Nonnull IPlaybackLane lane);

    /**
     * Determine if the given lane has been added as a target lane for this marker.
     *
     * @param lane Lane of interest
     * @return {@code true} if the lane as been added to this marker using {@link #addTargetLane(IPlaybackLane)}
     */
    boolean isTargetLane(@Nonnull IPlaybackLane lane);

    /**
     * Get the cursor to use to render this marker on a given playback lane.  Lanes registered as "target" lanes,
     * using {@link #addTargetLane(IPlaybackLane)}, will generally render the marker position differently than
     * an unregistered lane.
     *
     * @param lane The lane of interest
     * @return The cursor to use to render the marker on that lane
     */
    ATimeCursor getLaneCursor(@Nonnull IPlaybackLane lane);

    /**
     * Handle a double click on this marker.
     *
     * @implNote default implementation does nothing
     * @since 5.7
     */
    default void handleDoubleClick()
    {
    }

    /**
     * Handle a double click on this marker.
     *
     * @param e The mouse event that indicated the double click
     * @implNote default implementation calls {@link #handleDoubleClick()} for backward compatibility
     * @since 5.7
     */
    default void handleDoubleClick(MouseEvent e)
    {
        handleDoubleClick();
    }
}
