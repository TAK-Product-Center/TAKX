package gov.takx.core.playback;

import gov.takx.api.playback.IPlaybackConfiguration;
import gov.takx.api.playback.IPlaybackEventListener;
import gov.takx.api.playback.IPlaybackLane;
import gov.takx.api.playback.IPlaybackSystem;
import gov.takx.api.playback.ITimeSpan;
import gov.takx.api.playback.PlaybackEvent;
import gov.takx.api.playback.globe.IGlobeModel;
import gov.takx.core.playback.globe.GlobePlaybackLane;
import gov.takx.platform.playback.TimeBoundary;
import gov.takx.test.AContainerTest;
import gov.takx.test.mock.playback.TestPlaybackConfigurationListener;
import gov.takx.test.mock.playback.TestPlaybackLane;
import gov.takx.test.mock.playback.TestTimeSpanListener;
import gov.takx.test.utils.PlaybackTestUtils;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.invoke.MethodHandles;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;

/**
 * @since 1.48
 */
public class PlaybackSystemTest extends AContainerTest
{
    private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    @BeforeEach
    public void startService()
    {
        startService(IPlaybackSystem.class);
    }

    @AfterEach
    public void stopService()
    {
        stopService(IPlaybackSystem.class);
    }

    @Test
    public void testForwardingMethods()
    {
        final PlaybackSystem ps = getBean(PlaybackSystem.class);
        final IPlaybackConfiguration configuration = ps.getPlaybackConfiguration();
        final ITimeSpan span = ps.getTimeSpan();
        final IGlobeModel globeModel = ps.getGlobeModel();

        final GlobePlaybackLane globePlaybackLane = ps.getGlobePlaybackLane();

        assertThat(configuration).isNotNull();
        assertThat(span).isNotNull();
        assertThat(globeModel).isNotNull();
        assertThat(ps.getPlaybackLanes()).hasSize(1); // Should have globe lane

        final TestPlaybackConfigurationListener listener = new TestPlaybackConfigurationListener();
        configuration.addPlaybackConfigurationListener(listener);

        final TestPlaybackLane lane1 = new TestPlaybackLane();
        final TestPlaybackLane lane2 = new TestPlaybackLane();
        final TestPlaybackLane lane3 = new TestPlaybackLane();
        final TestPlaybackLane lane4 = new TestPlaybackLane();

        ps.addPlaybackLanes(lane2, lane4);

        List<IPlaybackLane> playbackLanes = ps.getPlaybackLanes();

        assertThat(playbackLanes).hasSize(3);
        assertThat(playbackLanes).containsExactly(globePlaybackLane, lane2, lane4);
        assertThat(playbackLanes).isEqualTo(configuration.getPlaybackLanes());
        assertThat(listener.lanesAdded.size()).isEqualTo(2);
        assertThat(listener.lanesRemoved.size()).isEqualTo(0);

        listener.reset();

        ps.removePlaybackLanes(lane2);
        playbackLanes = ps.getPlaybackLanes();

        assertThat(playbackLanes).hasSize(2);
        assertThat(listener.lanesAdded.size()).isEqualTo(0);
        assertThat(listener.lanesRemoved.size()).isEqualTo(1);
        assertThat(playbackLanes).containsExactly(globePlaybackLane, lane4);
        assertThat(playbackLanes).isEqualTo(configuration.getPlaybackLanes());

        listener.reset();
        ps.addPlaybackLanes(lane1);
        ps.replacePlaybackLane(lane4, lane3);
        playbackLanes = ps.getPlaybackLanes();

        assertThat(playbackLanes).hasSize(3);
        assertThat(listener.lanesAdded.size()).isEqualTo(2);
        assertThat(listener.lanesAdded).containsExactly(lane1, lane3);
        assertThat(listener.lanesRemoved.size()).isEqualTo(1);
        assertThat(listener.lanesRemoved).containsExactly(lane4);
        assertThat(playbackLanes).containsExactly(globePlaybackLane, lane3, lane1);
        assertThat(playbackLanes).isEqualTo(configuration.getPlaybackLanes());

        ps.setPlaybackSpeed(2);
        assertThat(ps.getPlaybackSpeed()).isEqualTo(2);
        assertThat(configuration.getPlaybackSpeed()).isEqualTo(2);
        assertThat(listener.speed).isEqualTo(2);
    }

    @Test
    public void testTimeSpan()
    {
        final PlaybackSystem ps = getBean(PlaybackSystem.class);
        final ITimeSpan span = ps.getTimeSpan();
        final Listener eventListener = new Listener();
        final TestTimeSpanListener spanListener = new TestTimeSpanListener();
        final TestPlaybackLane lane1 = new TestPlaybackLane();
        final TestPlaybackLane lane2 = new TestPlaybackLane();

        ps.stopLive(); // Ensure we're not live
        ps.addPlaybackEventListener(eventListener);
        ps.addPlaybackLanes(lane1, lane2);
        span.addListener(spanListener);

        assertThat(eventListener.events).isEmpty();

        // Update the playback time span and verify events
        lane1.setTimeBoundary(new TimeBoundary(20, 2000));
        lane2.setTimeBoundary(new TimeBoundary(1000, 3000));

        ps.fitTimeSpanToLanes();

        assertThat(spanListener.getSpanChangeCount()).isEqualTo(1);
        matchEvents(eventListener, new EventRecord(PlaybackEvent.TimeSpanChanged));
        assertThat(new TimeBoundary(ps.getTimeSpan().getTimeBoundary())).isEqualTo(new TimeBoundary(20, 3000));

        // Doing it again will have no impact and fire no events
        ps.fitTimeSpanToLanes();

        assertThat(spanListener.getSpanChangeCount()).isEqualTo(1);
        matchEvents(eventListener, new EventRecord(PlaybackEvent.TimeSpanChanged));
        assertThat(new TimeBoundary(ps.getTimeSpan().getTimeBoundary())).isEqualTo(new TimeBoundary(20, 3000));

        ps.removePlaybackLanes(lane2);
        ps.fitTimeSpanToLanes();

        assertThat(spanListener.getSpanChangeCount()).isEqualTo(2);
        assertThat(eventListener.events).hasSize(1);
        matchEvents(eventListener, new EventRecord(PlaybackEvent.TimeSpanChanged, 2));
        assertThat(new TimeBoundary(ps.getTimeSpan().getTimeBoundary())).isEqualTo(new TimeBoundary(20, 2000));

        spanListener.reset();
        eventListener.clear();

        // Playback position
        assertThat(ps.getCurrentPlaybackPosition()).isEqualTo(20);

        ps.setCurrentPlaybackPosition(1000);

        assertThat(spanListener.getSpanChangeCount()).isEqualTo(0);
        assertThat(spanListener.getPosChangeCount()).isEqualTo(1);
        matchEvents(eventListener, new EventRecord(PlaybackEvent.ScrubPositionChanged));

        ps.setCurrentPlaybackPosition(1000);
        matchEvents(eventListener, new EventRecord(PlaybackEvent.ScrubPositionChanged));

        ps.setCurrentPlaybackPosition(2000);
        matchEvents(eventListener, new EventRecord(PlaybackEvent.ScrubPositionChanged, 2));

        spanListener.reset();
        eventListener.clear();

        // Playback speed
        assertThat(ps.getPlaybackSpeed()).isEqualTo(1);

        ps.setPlaybackSpeed(4);
        assertThat(spanListener.getSpanChangeCount()).isEqualTo(0);
        assertThat(spanListener.getSpanChangeCount()).isEqualTo(0);
        matchEvents(eventListener, new EventRecord(PlaybackEvent.PlaybackSpeedChanged));

        ps.setPlaybackSpeed(1);
        assertThat(spanListener.getSpanChangeCount()).isEqualTo(0);
        assertThat(spanListener.getSpanChangeCount()).isEqualTo(0);
        matchEvents(eventListener, new EventRecord(PlaybackEvent.PlaybackSpeedChanged, 2));

        ps.setPlaybackSpeed(1); // Should emit no event
        matchEvents(eventListener, new EventRecord(PlaybackEvent.PlaybackSpeedChanged, 2));
    }

    @Test
    public void testOperations() throws InterruptedException
    {
        final PlaybackSystem ps = getBean(PlaybackSystem.class);
        PlaybackTestUtils.addComponentToTimeSpan(ps.getTimeSpan(), 100); // Make sure it has a width
        final Listener eventListener = new Listener();

        ps.getTimeSpan().setTimes(1000, 5000);
        ps.setCurrentPlaybackPosition(2000);
        ps.addPlaybackEventListener(eventListener);

        ps.stop();
        matchEvents(eventListener, new EventRecord(PlaybackEvent.ScrubPositionChanged));
        eventListener.clear();

        ps.stop(); // No event this time
        matchEvents(eventListener);

        ps.pause(); // Noop if not playing
        matchEvents(eventListener);

        // Now play for a fraction of a second and then stop
        ps.play();

        assertThat(ps.isPlaying()).isTrue();

        Thread.sleep(100);
        ps.stop(); // Back to start
        assertThat(ps.isPlaying()).isFalse();

        matchEvents(eventListener,
                new EventRecord(PlaybackEvent.Playing),
                new EventRecord(PlaybackEvent.PlayPositionChanged, 0),
                new EventRecord(PlaybackEvent.Stopped),
                new EventRecord(PlaybackEvent.ScrubPositionChanged));

        eventListener.clear();

        // Now change play position during playback
        ps.play();

        Thread.sleep(100);
        ps.setCurrentPlaybackPosition(4000);

        Thread.sleep(100);
        ps.stop(); // Back to start

        matchEvents(eventListener,
                new EventRecord(PlaybackEvent.Playing),
                new EventRecord(PlaybackEvent.PlayPositionChanged, 0),
                new EventRecord(PlaybackEvent.Paused),
                new EventRecord(PlaybackEvent.ScrubPositionChanged),
                new EventRecord(PlaybackEvent.Playing),
                new EventRecord(PlaybackEvent.PlayPositionChanged, 0),
                new EventRecord(PlaybackEvent.Stopped),
                new EventRecord(PlaybackEvent.ScrubPositionChanged));

        eventListener.clear();

        // Now pause during playback
        ps.play();

        Thread.sleep(100);

        ps.pause();
        assertThat(ps.isPlaying()).isFalse();
        ps.play();
        assertThat(ps.isPlaying()).isTrue();

        Thread.sleep(100);
        ps.stop(); // Back to start

        matchEvents(eventListener,
                new EventRecord(PlaybackEvent.Playing),
                new EventRecord(PlaybackEvent.PlayPositionChanged, 0),
                new EventRecord(PlaybackEvent.Paused),
                new EventRecord(PlaybackEvent.Playing),
                new EventRecord(PlaybackEvent.PlayPositionChanged, 0),
                new EventRecord(PlaybackEvent.Stopped),
                new EventRecord(PlaybackEvent.ScrubPositionChanged));

        eventListener.clear();

        // Now end with a pause
        ps.play();

        Thread.sleep(100);
        ps.pause();

        assertThat(ps.getCurrentPlaybackPosition()).isGreaterThan(ps.getTimeSpan().getStartTime() + 1); // Must have ticked at least once

        matchEvents(eventListener,
                new EventRecord(PlaybackEvent.Playing),
                new EventRecord(PlaybackEvent.PlayPositionChanged, 0),
                new EventRecord(PlaybackEvent.Paused));

        eventListener.clear();
    }

    /**
     * Match the given list of event records against those recorded in the given event listener.
     * Fail the test if any mismatch is found.
     *
     * @param listener     Listener with recorded event stream
     * @param eventRecords Event records to match against recorded events
     */
    private void matchEvents(Listener listener, EventRecord... eventRecords)
    {
        if (listener.events.size() != eventRecords.length)
        {
            logger.warn("{} != {}", listener.events, Arrays.toString(eventRecords));
            fail(String.format("Event size mismatch (%d != %d)", listener.events.size(), eventRecords.length));
        }

        int i = 0;
        for (EventRecord match : eventRecords)
        {
            final EventRecord eventRecord = listener.events.get(i++);

            if (eventRecord.event != match.event)
            {
                logger.warn("{} != {}", listener.events, Arrays.toString(eventRecords));
                fail(String.format("Event type mismatch at index %d (%s != %s)", i - 1, eventRecord.event, match.event));
            }

            // Matching on counts, 0 = match any count, >= 0 exact match
            if (match.count != 0 && eventRecord.count != match.count)
            {
                logger.warn("{} != {}", listener.events, Arrays.toString(eventRecords));
                fail(String.format("Event count mismatch at index %d on %s (%d != %d)", i - 1,
                        eventRecord.event, eventRecord.count, match.count));
            }
        }
    }

    /**
     * Event listener to record event stream.
     */
    private static final class Listener implements IPlaybackEventListener
    {
        final List<EventRecord> events = new ArrayList<>();

        @Override
        public void onPlaybackEvent(PlaybackEvent event)
        {
            // Add the event to the recorded list.  If it is the same as the last event, then just increment the count.
            final EventRecord lastEventRecord = getLastEventRecord();

            if (lastEventRecord == null || lastEventRecord.event != event)
            {
                events.add(new EventRecord(event));
            } else
            {
                lastEventRecord.count += 1;
            }
        }

        /**
         * @return The last recorded event, or {@code null} of none
         */
        EventRecord getLastEventRecord()
        {
            final int size = events.size();
            return size > 0 ? events.get(size - 1) : null;
        }

        void clear()
        {
            events.clear();
        }
    }

    private static final class EventRecord
    {
        final PlaybackEvent event;
        int count;

        EventRecord(PlaybackEvent event)
        {
            this(event, 1);
        }

        EventRecord(PlaybackEvent event, int count)
        {
            this.event = event;
            this.count = count;
        }

        @Override
        public String toString()
        {
            return "EventRecord{" +
                    "event=" + event +
                    ", count=" + count +
                    '}';
        }
    }
}
