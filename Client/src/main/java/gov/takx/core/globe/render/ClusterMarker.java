package gov.takx.core.globe.render;

import gov.nasa.worldwind.render.DrawContext;
import gov.nasa.worldwind.render.PointPlacemark;
import gov.nasa.worldwind.render.PointPlacemarkAttributes;
import gov.takx.api.annotation.PseudoBean;
import gov.takx.api.playback.IPlaybackManager;
import gov.takx.core.mapdata.TrackPoint;
import gov.takx.platform.ui.UIResources;
import jakarta.inject.Inject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.invoke.MethodHandles;
import java.util.Collections;
import java.util.Comparator;

import static gov.takx.platform.ui.ResourceDirectory.PLATFORM_ICONS;

/**
 * Specialized point placemark in order to handle playback effects.
 *
 * @since 2.31
 */
@SuppressWarnings("CdiManagedBeanInconsistencyInspection")
@PseudoBean
final class ClusterMarker extends PointPlacemark
{
    private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    // Attributes used to render top-quadrant "cluster" items
    private static final PointPlacemarkAttributes PLACEMARK_ATTRIBUTES = new PointPlacemarkAttributes();

    private boolean lastLive = true;
    private long lastPlaybackPosition;
    private boolean listSorted;

    static
    {
        PLACEMARK_ATTRIBUTES.setImageAddress(UIResources.getPath(PLATFORM_ICONS, "track_point_group.png"));
    }

    @Inject
    private IPlaybackManager playbackManager;

    private final TopDownSceneGenerator.ClusterData clusterData;

    /**
     * Construct a cluster marker.
     *
     * @param clusterData Data defining the cluster
     */
    public ClusterMarker(TopDownSceneGenerator.ClusterData clusterData)
    {
        super(clusterData.position());

        this.clusterData = clusterData;

        setAttributes(PLACEMARK_ATTRIBUTES);
        setCount(clusterData.visibleCount());
    }

    /**
     * Override normal rendering so we can account for playback position (time) in our count.
     */
    @Override
    public void render(DrawContext dc)
    {
        final boolean live = playbackManager.isLive();
        final long playbackPosition = playbackManager.getCurrentPlaybackPosition();

        // If something has changed, then recalculate
        if (lastLive != live || (!live && lastPlaybackPosition != playbackPosition))
        {
            lastLive = live;
            lastPlaybackPosition = playbackPosition;

            if (live)
            {
                setCount(clusterData.visibleCount()); // Restore our initial count
            } else
            {
                updateCountForPlayback(playbackPosition);
            }
        }

        super.render(dc);
    }

    /**
     * Calculate the current number of points in the cluster based on playback state.
     *
     * @param playbackPosition Current playback position (time)
     */
    private void updateCountForPlayback(long playbackPosition)
    {
        final var trackPoints = clusterData.cellData().getPoints();

        // If we have not already sorted out cell's data points, then do so now
        if (!listSorted)
        {
            trackPoints.sort(Comparator.comparingLong(tp -> tp.time));
            listSorted = true;
        }

        // With the list sorted, we can use a binary search to determine how many points are visible
        // based on the current playback position.

        final TrackPoint searchPoint = new TrackPoint(0, null, 0, 0, 0, playbackPosition);

        final int searchIndex = Collections.binarySearch(trackPoints, searchPoint, Comparator.comparingLong(tp -> tp.time));

        // If the time was found in the list, then we need to scan forward to find the last entry of the same time.
        // If not found, then the index can be used directly.

        logger.debug("updateCount, index={}, pos={}", searchIndex, playbackPosition);

        if (searchIndex >= 0)
        {
            final int size = trackPoints.size();
            int pointIndex = searchIndex + 1;
            while (pointIndex < size && trackPoints.get(pointIndex).time == playbackPosition) pointIndex++;

            setCount(pointIndex);
        } else
        {
            setCount(-searchIndex - 1);
        }
    }

    /**
     * Set the label text to the given count and update visibility accordingly.  If the count is 0, then make
     * this marker invisible, otherwise visible.
     *
     * @param count Count to display
     */
    private void setCount(int count)
    {
        setLabelText(Integer.toString(count));
        setVisible(count != 0);
    }
}
