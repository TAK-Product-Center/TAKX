package gov.takx.platform.playback;

import gov.takx.api.playback.IMarker;
import gov.takx.api.playback.IPlaybackLane;
import gov.takx.api.playback.ITimeSpan;
import gov.takx.test.mock.playback.TestPlaybackLane;
import gov.takx.test.mock.playback.TestTimeSpanListener;
import gov.takx.test.utils.PlaybackTestUtils;
import gov.takx.test.utils.SwingTestUtils;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

/**
 * @since 1.48
 */
public class TimeSpanTest
{
    @Test
    public void testTimes()
    {
        final ITimeSpan timeSpan = new TimeSpan();

        assertThat(timeSpan.getTimeBoundary()).isNotNull();

        // Ensure that minimum time span is upheld
        timeSpan.setTimes(10, 30);
        assertThat(timeSpan.getTotalTime()).isEqualTo(TimelineUtils.SECOND);
        long delta = (TimelineUtils.SECOND - (30 - 10)) / 2;
        assertThat(timeSpan.getStartTime()).isEqualTo(10 - delta);
        assertThat(timeSpan.getEndTime()).isEqualTo(30 + delta);

        // Once it's at min, a new small span should be ignored
        timeSpan.setTimes(100, 130);
        assertThat(timeSpan.getStartTime()).isEqualTo(10 - delta);
        assertThat(timeSpan.getEndTime()).isEqualTo(30 + delta);

        timeSpan.setTimes(10, TimelineUtils.FIVE_YEARS * 2);
        assertThat(timeSpan.getStartTime()).isEqualTo(10);
        assertThat(timeSpan.getEndTime()).isEqualTo(TimelineUtils.FIVE_YEARS + 10);

        // Once it's at max, a new larger span should be ignored
        timeSpan.setTimes(4000, TimelineUtils.FIVE_YEARS * 2);
        assertThat(timeSpan.getStartTime()).isEqualTo(10);
        assertThat(timeSpan.getEndTime()).isEqualTo(TimelineUtils.FIVE_YEARS + 10);

        timeSpan.setTimes(TimelineUtils.SECOND, TimelineUtils.SECOND * 5);
        assertThat(timeSpan.getTotalTime()).isEqualTo(TimelineUtils.SECOND * 4);

        // Ensure trying to set start time so span is less than 1 second is properly bounded
        timeSpan.setStartTime(TimelineUtils.SECOND * 4 + 100);
        delta = 100 / 2;
        assertThat(timeSpan.getStartTime()).isEqualTo((TimelineUtils.SECOND * 4) + delta);
        assertThat(timeSpan.getTotalTime()).isEqualTo(TimelineUtils.SECOND);

        timeSpan.setStartTime(TimelineUtils.SECOND);
        assertThat(timeSpan.getStartTime()).isEqualTo(TimelineUtils.SECOND);

        // Ensure trying to set start time so span is > 5 years is properly bounded
        timeSpan.setEndTime(TimelineUtils.YEAR * 6);
        assertThat(timeSpan.getEndTime()).isEqualTo(TimelineUtils.FIVE_YEARS + TimelineUtils.SECOND);
        assertThat(timeSpan.getTotalTime()).isEqualTo(TimelineUtils.FIVE_YEARS);

        timeSpan.setTimes(10, TimelineUtils.SECOND);
        delta = 10 / 2;
        timeSpan.zoomTime(TimelineUtils.DAY);

        // Time span should now be [5,DAY+1000]

        assertThat(timeSpan.getStartTime()).isEqualTo(5);
        assertThat(timeSpan.getTotalTime()).isEqualTo(TimelineUtils.DAY + 1000);

        assertThat(timeSpan.containsTime(10)).isTrue();
        assertThat(timeSpan.containsTime(100)).isTrue();
        assertThat(timeSpan.containsTime(TimelineUtils.SECOND)).isTrue();
        assertThat(timeSpan.containsTime(2)).isFalse();
        assertThat(timeSpan.containsTime(TimelineUtils.YEAR)).isFalse();

        assertThat(timeSpan.intersects(5, TimelineUtils.SECOND)).isTrue();
        assertThat(timeSpan.intersects(0, 6)).isTrue();
        assertThat(timeSpan.intersects(5, 6)).isTrue();
        assertThat(timeSpan.intersects(0, TimelineUtils.MINUTE)).isTrue();
        assertThat(timeSpan.intersects(0, 4)).isFalse();
        assertThat(timeSpan.intersects(TimelineUtils.DAY + 1006, TimelineUtils.DAY * 2)).isFalse();
    }

    @Test
    public void testPositions() throws Throwable
    {
        final int width = 100;
        final ITimeSpan timeSpan = PlaybackTestUtils.makeTimeSpanWithComponent(width, TimelineUtils.SECOND, TimelineUtils.SECOND * 11);

        assertThat(timeSpan.timeToX(TimelineUtils.SECOND)).isEqualTo(0.0);
        assertThat(timeSpan.timeToX(TimelineUtils.SECOND * 6)).isEqualTo(50.0);
        assertThat(timeSpan.timeToX(TimelineUtils.SECOND * 11)).isEqualTo(100.0);

        assertThat(timeSpan.xToTime(0)).isEqualTo(TimelineUtils.SECOND);
        assertThat(timeSpan.xToTime(50)).isEqualTo(TimelineUtils.SECOND * 6);
        assertThat(timeSpan.xToTime(100)).isEqualTo(TimelineUtils.SECOND * 11);

        // Ensure set position obeys time boundaries
        timeSpan.setPosition(0);
        assertThat(timeSpan.getPosition()).isEqualTo(TimelineUtils.SECOND);
        timeSpan.setPosition(TimelineUtils.MINUTE);
        assertThat(timeSpan.getPosition()).isEqualTo(TimelineUtils.SECOND * 11);

        timeSpan.setPosition(0);
        assertThat(timeSpan.getPosition()).isEqualTo(TimelineUtils.SECOND);
        assertThat(timeSpan.getXPosition()).isEqualTo(0);
        assertThat(timeSpan.getDeltaPosition()).isEqualTo(0);

        timeSpan.setPosition(TimelineUtils.SECOND * 6);
        assertThat(timeSpan.getPosition()).isEqualTo(TimelineUtils.SECOND * 6);
        assertThat(timeSpan.getXPosition()).isEqualTo(50);
        assertThat(timeSpan.getDeltaPosition()).isEqualTo(TimelineUtils.SECOND * 5);

        timeSpan.setPosition(TimelineUtils.SECOND * 11);
        assertThat(timeSpan.getPosition()).isEqualTo(TimelineUtils.SECOND * 11);
        assertThat(timeSpan.getXPosition()).isEqualTo(100);
        assertThat(timeSpan.getDeltaPosition()).isEqualTo(TimelineUtils.SECOND * 10);

        timeSpan.setXPosition(0);
        assertThat(timeSpan.getPosition()).isEqualTo(TimelineUtils.SECOND);
        assertThat(timeSpan.getXPosition()).isEqualTo(0);
        assertThat(timeSpan.getDeltaPosition()).isEqualTo(0);

        timeSpan.setXPosition(50);
        assertThat(timeSpan.getPosition()).isEqualTo(TimelineUtils.SECOND * 6);
        assertThat(timeSpan.getXPosition()).isEqualTo(50);
        assertThat(timeSpan.getDeltaPosition()).isEqualTo(TimelineUtils.SECOND * 5);

        timeSpan.setXPosition(100);
        assertThat(timeSpan.getPosition()).isEqualTo(TimelineUtils.SECOND * 11);
        assertThat(timeSpan.getXPosition()).isEqualTo(100);
        assertThat(timeSpan.getDeltaPosition()).isEqualTo(TimelineUtils.SECOND * 10);

        timeSpan.panTimeByXOffset(10); // Shift by 10% of X, so 1 second
        assertThat(timeSpan.getStartTime()).isEqualTo(TimelineUtils.SECOND * 2);
        assertThat(timeSpan.getEndTime()).isEqualTo(TimelineUtils.SECOND * 12);

        timeSpan.panTimeByXOffset(-10); // Shift by 10% of X, so 1 second
        assertThat(timeSpan.getStartTime()).isEqualTo(TimelineUtils.SECOND);
        assertThat(timeSpan.getEndTime()).isEqualTo(TimelineUtils.SECOND * 11);

        // Verify that changing the component width updated the X position
        timeSpan.setTimes(TimelineUtils.SECOND, TimelineUtils.SECOND * 11);
        timeSpan.setPosition(TimelineUtils.SECOND * 6);
        SwingTestUtils.invokeAndWaitOnEDT(() -> timeSpan.getVisualComponent().setBounds(0, 0, 50, 10));

        // I don't know why this is needed since we already wait on the call above, but without it, the test fails
        SwingTestUtils.waitForEdtEvents();

        // Now we can test for the corresponding X position change due to the width change
        assertThat(timeSpan.getXPosition()).isEqualTo(25);
    }

    @Test
    public void testZoom()
    {
        final int width = 100;
        final ITimeSpan timeSpan = PlaybackTestUtils.makeTimeSpanWithComponent(width, TimelineUtils.SECOND, TimelineUtils.SECOND * 11);

        timeSpan.setTimes(TimelineUtils.HOUR - TimelineUtils.SECOND, TimelineUtils.HOUR + TimelineUtils.SECOND);

        final long origStart = timeSpan.getStartTime();
        final long origEnd = timeSpan.getEndTime();
        final long origSpan = timeSpan.getTotalTime();

        timeSpan.scaleTimeOut(ITimeSpan.DEFAULT_ZOOM_FACTOR, width / 2); // Scale out by 1.5 around the middle
        assertThat(timeSpan.getTotalTime()).isEqualTo((long) (origSpan * ITimeSpan.DEFAULT_ZOOM_FACTOR));
        assertThat(timeSpan.getStartTime()).isEqualTo(origStart - (TimelineUtils.SECOND / 2));
        assertThat(timeSpan.getEndTime()).isEqualTo(origEnd + (TimelineUtils.SECOND / 2));

        // Zoom back in should undo it
        timeSpan.scaleTimeIn(ITimeSpan.DEFAULT_ZOOM_FACTOR, width / 2);
        assertThat(timeSpan.getTotalTime()).isEqualTo(origSpan);
        assertThat(timeSpan.getStartTime()).isEqualTo(origStart);
        assertThat(timeSpan.getEndTime()).isEqualTo(origEnd);

        timeSpan.scaleTimeOut(ITimeSpan.DEFAULT_ZOOM_FACTOR, width); // Scale out by 1.5 around the end
        assertThat(timeSpan.getTotalTime()).isEqualTo((long) (origSpan * ITimeSpan.DEFAULT_ZOOM_FACTOR));
        assertThat(timeSpan.getStartTime()).isEqualTo(origStart - TimelineUtils.SECOND);
        assertThat(timeSpan.getEndTime()).isEqualTo(origEnd);

        timeSpan.setTimes(TimelineUtils.HOUR - TimelineUtils.SECOND, TimelineUtils.HOUR + TimelineUtils.SECOND);
        timeSpan.scaleTimeOut(ITimeSpan.DEFAULT_ZOOM_FACTOR, 0); // Scale out by 1.5 around the start
        assertThat(timeSpan.getTotalTime()).isEqualTo((long) (origSpan * ITimeSpan.DEFAULT_ZOOM_FACTOR));
        assertThat(timeSpan.getStartTime()).isEqualTo(origStart);
        assertThat(timeSpan.getEndTime()).isEqualTo(origEnd + TimelineUtils.SECOND);
    }

    @Test
    public void testListeners()
    {
        final ITimeSpan timeSpan = PlaybackTestUtils.makeTimeSpanWithComponent(100, TimelineUtils.SECOND, TimelineUtils.SECOND * 11);

        final TestTimeSpanListener listener = new TestTimeSpanListener();
        timeSpan.addListener(listener);

        int expectedSpanChangeCount = 0;
        int expectedPosChangeCount = 0;

        timeSpan.setStartTime(TimelineUtils.SECOND); // No actual change
        assertThat(listener.getSpanChangeCount()).isEqualTo(expectedSpanChangeCount);
        timeSpan.setEndTime(TimelineUtils.SECOND * 11);
        assertThat(listener.getSpanChangeCount()).isEqualTo(expectedSpanChangeCount);
        timeSpan.setTimes(TimelineUtils.SECOND, TimelineUtils.SECOND * 11);
        assertThat(listener.getSpanChangeCount()).isEqualTo(expectedSpanChangeCount);

        timeSpan.setStartTime(0);
        assertThat(listener.getSpanChangeCount()).isEqualTo(++expectedSpanChangeCount);

        timeSpan.setStartTime(TimelineUtils.SECOND);
        assertThat(listener.getSpanChangeCount()).isEqualTo(++expectedSpanChangeCount);
        assertThat(listener.getPosChangeCount()).isEqualTo(expectedPosChangeCount); // Subsumed by span change event

        timeSpan.setTimes(TimelineUtils.SECOND, TimelineUtils.HOUR);
        assertThat(listener.getSpanChangeCount()).isEqualTo(++expectedSpanChangeCount);

        timeSpan.zoomTime(TimelineUtils.HOUR);
        assertThat(listener.getSpanChangeCount()).isEqualTo(++expectedSpanChangeCount);

        timeSpan.scaleTimeIn(1.2, 0);
        assertThat(listener.getSpanChangeCount()).isEqualTo(++expectedSpanChangeCount);

        timeSpan.scaleTimeOut(1.2, 0);
        assertThat(listener.getSpanChangeCount()).isEqualTo(++expectedSpanChangeCount);

        timeSpan.panTimeByXOffset(10);
        assertThat(listener.getSpanChangeCount()).isEqualTo(++expectedSpanChangeCount);

        timeSpan.panTimeByXOffset(-10);
        assertThat(listener.getSpanChangeCount()).isEqualTo(++expectedSpanChangeCount);

        timeSpan.setPosition(TimelineUtils.SECOND * 2);
        assertThat(listener.getPosChangeCount()).isEqualTo(++expectedPosChangeCount);

        timeSpan.setXPosition(10);
        assertThat(listener.getPosChangeCount()).isEqualTo(++expectedPosChangeCount);

        final DefaultMarker marker = new DefaultMarker();
        timeSpan.addMarker(marker);
        assertThat(listener.getAddedCount()).isEqualTo(1);
        timeSpan.addMarker(marker);
        assertThat(listener.getAddedCount()).isEqualTo(2);

        timeSpan.removeMarker(marker);
        assertThat(listener.getRemovedCount()).isEqualTo(1);
        timeSpan.removeMarker(marker);
        assertThat(listener.getRemovedCount()).isEqualTo(2);
        timeSpan.removeMarker(marker); // A third time should be a noop
        assertThat(listener.getRemovedCount()).isEqualTo(2);

        timeSpan.removeListener(listener);

        // No more changes should occur
        timeSpan.setTimes(0, TimelineUtils.MINUTE);
        assertThat(listener.getSpanChangeCount()).isEqualTo(expectedSpanChangeCount);
        timeSpan.setPosition(TimelineUtils.SECOND * 2);
        assertThat(listener.getPosChangeCount()).isEqualTo(expectedPosChangeCount);
        timeSpan.addMarker(marker);
        assertThat(listener.getAddedCount()).isEqualTo(2);
        timeSpan.removeMarker(marker);
        assertThat(listener.getRemovedCount()).isEqualTo(2);
    }

    @Test
    public void testMarkers()
    {
        final ITimeSpan timeSpan = PlaybackTestUtils.makeTimeSpanWithComponent(100, TimelineUtils.SECOND, TimelineUtils.SECOND * 11);

        final IPlaybackLane lane1 = new TestPlaybackLane();
        final IPlaybackLane lane2 = new TestPlaybackLane();

        final IMarker marker1 = timeSpan.createMomentMarker("M1", TimelineUtils.SECOND * 3, lane1);
        final IMarker marker2 = timeSpan.createMomentMarker("M2", TimelineUtils.SECOND * 4, lane2);

        final IMarker marker3 = new DefaultMarker();
        marker3.setMoment(TimelineUtils.SECOND * 5);

        assertThat(marker1.isTargetLane(lane1)).isTrue();
        assertThat(marker1.isTargetLane(lane2)).isFalse();

        assertThat(marker2.isTargetLane(lane1)).isFalse();
        assertThat(marker2.isTargetLane(lane2)).isTrue();

        timeSpan.addMarker(marker3);

        timeSpan.setTimes(0, TimelineUtils.SECOND * 10);

        assertThat(marker1.getRulerCursor().getXLocation()).isEqualTo(30);
        assertThat(marker2.getRulerCursor().getXLocation()).isEqualTo(40);
        assertThat(marker3.getRulerCursor().getXLocation()).isEqualTo(50);

        List<IMarker> markers = timeSpan.getMarkers();

        assertThat(markers).hasSize(3);
        assertThat(markers.containsAll(List.of(marker1, marker2, marker3))).isTrue();

        timeSpan.removeMarker(marker1);
        markers = timeSpan.getMarkers();
        assertThat(markers).hasSize(2);
        assertThat(markers.containsAll(List.of(marker2, marker3))).isTrue();

        timeSpan.removeMarker(marker2);
        markers = timeSpan.getMarkers();
        assertThat(markers).hasSize(1);
        assertThat(markers.contains(marker3)).isTrue();

        timeSpan.removeMarker(marker2); // Removing again should have no effect
        markers = timeSpan.getMarkers();
        assertThat(markers).hasSize(1);
        assertThat(markers.contains(marker3)).isTrue();

        timeSpan.removeMarker(marker3);
        markers = timeSpan.getMarkers();
        assertThat(markers).isEmpty();
    }

    @Test
    public void testMarkerListImmutable()
    {
        final ITimeSpan timeSpan = PlaybackTestUtils.makeTimeSpanWithComponent(100, TimelineUtils.SECOND, TimelineUtils.SECOND * 11);
        timeSpan.createMomentMarker("M1", TimelineUtils.SECOND * 3);

        List<IMarker> markers = timeSpan.getMarkers();

        // Should throw exception, collection should be immutable
        assertThatExceptionOfType(UnsupportedOperationException.class)
                .isThrownBy(() -> markers.remove(0));
    }
}
