package gov.takx.platform.playback.ui;

import gov.takx.api.playback.IMarker;
import gov.takx.api.playback.IPlaybackLane;
import gov.takx.api.playback.ITimeSpan;
import gov.takx.platform.playback.TimelineUtils;
import gov.takx.test.mock.playback.TestPlaybackLane;
import gov.takx.test.utils.PlaybackTestUtils;
import org.junit.jupiter.api.Test;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @since 1.48
 */
public class TrackPanelTest
{
    @Test
    public void testMarkers()
    {
        final MyTrackPanel trackPanel = new MyTrackPanel();
        final ITimeSpan timeSpan = trackPanel.getTimeSpan();
        final IPlaybackLane lane = trackPanel.getPlaybackLane();

        final IMarker marker1 = timeSpan.createMomentMarker("foo", TimelineUtils.SECOND, lane);
        trackPanel.forceRepaint();

        assertThat(trackPanel.cursorRendered).isInstanceOf(TimeCursorMarkerDiamond.class);
        assertThat(trackPanel.cursorRendered).isEqualTo(marker1.getLaneCursor(lane));
        timeSpan.removeMarker(marker1);

        final IMarker marker2 = timeSpan.createMomentMarker("bar", TimelineUtils.SECOND * 5);
        trackPanel.forceRepaint();

        assertThat(trackPanel.cursorRendered).isInstanceOf(TimeCursorLine.class);
        assertThat(trackPanel.cursorRendered).isEqualTo(marker2.getLaneCursor(lane));
        timeSpan.removeMarker(marker2);
    }

    private static final class MyTrackPanel extends TrackPanel
    {
        ATimeCursor cursorRendered = null;

        MyTrackPanel()
        {
            super(new PlaybackLane(), PlaybackTestUtils.makeTimeSpanWithComponent(100, 0, TimelineUtils.MINUTE), Color.RED);
        }

        IPlaybackLane getPlaybackLane()
        {
            return playbackLane;
        }

        @Override
        protected void renderCursor(ATimeCursor cursor, Graphics2D g)
        {
            super.renderCursor(cursor, g);
            cursorRendered = cursor;
        }

        void forceRepaint()
        {
            BufferedImage image = new BufferedImage(100, 100, BufferedImage.TYPE_INT_ARGB);
            paint(image.getGraphics());
        }
    }

    private static final class PlaybackLane extends TestPlaybackLane
    {

    }
}
