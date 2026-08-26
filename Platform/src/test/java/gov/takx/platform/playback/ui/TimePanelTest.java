package gov.takx.platform.playback.ui;

import gov.takx.api.playback.IMarker;
import gov.takx.api.playback.ITimeSpan;
import gov.takx.platform.playback.DefaultMarker;
import gov.takx.platform.playback.TimelineUtils;
import gov.takx.test.utils.PlaybackTestUtils;
import gov.takx.test.utils.SwingTestUtils;
import org.junit.jupiter.api.Test;

import java.awt.image.BufferedImage;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @since 1.48
 */
public class TimePanelTest
{
    @Test
    public void testEvents()
    {
        final MyTimePanel timePanel = new MyTimePanel();
        final ITimeSpan timeSpan = timePanel.getTimeSpan();

        // Verify that events from the TimeSpan are properly dispatched to the panel
        timeSpan.setTimes(0, TimelineUtils.SECOND * 10);
        assertThat(timePanel.spanChangeCount).isEqualTo(1);

        timeSpan.setPosition(100);
        assertThat(timePanel.posChangeCount).isEqualTo(1);

        final IMarker marker = new DefaultMarker();
        marker.setMoment(TimelineUtils.SECOND * 5);

        timeSpan.addMarker(marker);
        assertThat(timePanel.addedCount).isEqualTo(1);
        timeSpan.removeMarker(marker);
        assertThat(timePanel.removedCount).isEqualTo(1);
    }

    @Test
    public void testCursor()
    {
        final MyTimePanel timePanel = new MyTimePanel();
        final ITimeSpan timeSpan = timePanel.getTimeSpan();

        timeSpan.setTimes(0, TimelineUtils.SECOND * 10);
        timeSpan.setPosition(TimelineUtils.SECOND);

        // Verify that painting the panel updates the cursor

        final ATimeCursor cursor = timePanel.getTimeCursor();

        assertThat(cursor.getXLocation()).isEqualTo(-1);

        timePanel.forceRepaint();

        SwingTestUtils.waitForEdtEvents();
        assertThat(cursor.getXLocation()).isEqualTo(10);
    }

    private final static class MyTimePanel extends TimePanel
    {
        int spanChangeCount = 0;
        int posChangeCount = 0;
        int addedCount = 0;
        int removedCount = 0;

        MyTimePanel()
        {
            super(PlaybackTestUtils.makeTimeSpanWithComponent(100, 0, TimelineUtils.SECOND));
        }

        @Override
        public void timeSpanChanged()
        {
            spanChangeCount += 1;
            super.timeSpanChanged();
        }

        @Override
        public void timePositionChanged()
        {
            posChangeCount += 1;
            super.timePositionChanged();
        }

        @Override
        public void markerAdded(IMarker marker)
        {
            addedCount += 1;
            super.markerAdded(marker);
        }

        @Override
        public void markerRemoved(IMarker marker)
        {
            removedCount += 1;
            super.markerRemoved(marker);
        }

        void forceRepaint()
        {
            BufferedImage image = new BufferedImage(100, 100, BufferedImage.TYPE_INT_ARGB);
            paint(image.getGraphics());
        }
    }
}
