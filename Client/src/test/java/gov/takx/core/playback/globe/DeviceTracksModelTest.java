package gov.takx.core.playback.globe;

import gov.takx.api.mapobject.IMapObject;
import gov.takx.api.playback.ITimeLocation;
import gov.takx.api.playback.globe.IDeviceTracksModelListener;
import gov.takx.core.playback.APlaybackIntegrationTest;
import gov.takx.platform.playback.TimeBoundary;
import org.junit.jupiter.api.Test;

import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @since 1.48
 */
public class DeviceTracksModelTest extends APlaybackIntegrationTest
{
    /**
     * Construct with a registered plugin so we can use it to send messages.
     */
    public DeviceTracksModelTest()
    {
    }

    @Test
    public void testBasicData() throws Exception
    {
        final DeviceTracksModel model = new DeviceTracksModel(timeSpan, mapObject1);

        model.waitForInitialization(5, TimeUnit.SECONDS);

        assertThat(model.getTrackPoints()).hasSize(10);
        assertThat(model.getTrackPointsInPlayback()).hasSize(5);

        // We have track points with times from 1000 - 10000

        assertThat(model.getTrackPoints(1000, 4000)).hasSize(4);
        assertThat(model.getTrackPoints(1000, 4000).first().getTime()).isEqualTo(1000);
        assertThat(model.getTrackPoints(1000, 4000).last().getTime()).isEqualTo(4000);
        assertThat(model.getTrackPoints(1000, 6000)).hasSize(6);
        assertThat(model.getTrackPoints(1000, 10000)).hasSize(10);

        // Find the lowest and highest times
        long minTime = Long.MAX_VALUE;
        long maxTime = 0;
        for (ITimeLocation timeLocation : model.getTrackPoints())
        {
            final long time = timeLocation.getTime();
            minTime = Math.min(minTime, time);
            maxTime = Math.max(maxTime, time);
        }

        assertThat(model.getTimeBoundary()).isEqualTo(new TimeBoundary(minTime, maxTime));

        assertThat(model.getFirstTrackPoint().getTime()).isEqualTo(minTime);
        assertThat(model.getLastTrackPoint().getTime()).isEqualTo(maxTime);

        assertThat(model.getClosestTrackPointInPlayback(1900).getTime()).isEqualTo(1000);
        assertThat(model.getClosestTrackPointInPlayback(1100).getTime()).isEqualTo(1000);
        assertThat(model.getClosestTrackPointInPlayback(1000).getTime()).isEqualTo(1000);

        assertThat(model.getNextTrackPointInPlayback(0).getTime()).isEqualTo(1000);
        assertThat(model.getNextTrackPointInPlayback(1000).getTime()).isEqualTo(2000);
        assertThat(model.getNextTrackPointInPlayback(6000)).isNull();

        assertThat(model.getPreviousTrackPointInPlayback(2000).getTime()).isEqualTo(1000);
        assertThat(model.getPreviousTrackPointInPlayback(1900).getTime()).isEqualTo(1000);
        assertThat(model.getPreviousTrackPointInPlayback(2100).getTime()).isEqualTo(2000);
        assertThat(model.getPreviousTrackPointInPlayback(1000)).isNull();

        // Change the timespan to include just 4 of the points (from 5000 - 8000)
        timeSpan.setTimes(5000, 8000);

        assertThat(model.getTimeBoundary()).isEqualTo(new TimeBoundary(minTime, maxTime));

        assertThat(model.getTrackPointsInPlayback()).hasSize(4);
        assertThat(model.getTrackPointsInPlayback().first().getTime()).isEqualTo(5000);
        assertThat(model.getTrackPointsInPlayback().last().getTime()).isEqualTo(8000);

        assertThat(model.getFirstTrackPoint().getTime()).isEqualTo(minTime);
        assertThat(model.getLastTrackPoint().getTime()).isEqualTo(maxTime);

        assertThat(model.getClosestTrackPointInPlayback(5900).getTime()).isEqualTo(5000);

        assertThat(model.getNextTrackPointInPlayback(0).getTime()).isEqualTo(5000);
        assertThat(model.getNextTrackPointInPlayback(5000).getTime()).isEqualTo(6000);
        assertThat(model.getNextTrackPointInPlayback(8000)).isNull();

        assertThat(model.getPreviousTrackPointInPlayback(6000).getTime()).isEqualTo(5000);
        assertThat(model.getPreviousTrackPointInPlayback(5900).getTime()).isEqualTo(5000);
        assertThat(model.getPreviousTrackPointInPlayback(5000)).isNull();

        model.disposeResources();
    }

    @Test
    public void testListeners() throws Exception
    {
        timeSpan.setTimes(10000, 19000); // This includes all 10 points (exactly)

        final DeviceTracksModel model = new DeviceTracksModel(timeSpan, mapObject2);

        model.waitForInitialization(5, TimeUnit.SECONDS);

        assertThat(model.getTrackPoints()).hasSize(10);
        assertThat(model.getTrackPointsInPlayback()).hasSize(10);

        final long lastTrackTime = model.getLastTrackPoint().getTime();
        assertThat(lastTrackTime).isEqualTo(19000);

        final Listener listener = new Listener();

        model.addListener(listener);

        // Add a message within the timespan and test event count
        sendMessage(UNIT_ID_2, lastTrackTime - 500);

        assertThat(listener.addedCount).isEqualTo(1);
        assertThat(listener.changedCount).isEqualTo(0);

        // Add a message outside device time range and test event count
        sendMessage(UNIT_ID_2, lastTrackTime + 500);

        assertThat(listener.addedCount).isEqualTo(1);
        assertThat(listener.changedCount).isEqualTo(1);

        assertThat(model.getTimeBoundary()).isEqualTo(new TimeBoundary(10000, lastTrackTime + 500));

        // Add a message outside timespan and withindevice time boundary and ensure no events fire
        sendMessage(UNIT_ID_2, lastTrackTime + 100);

        assertThat(listener.addedCount).isEqualTo(1);
        assertThat(listener.changedCount).isEqualTo(1);

        assertThat(model.getTimeBoundary()).isEqualTo(new TimeBoundary(10000, lastTrackTime + 500));

        model.disposeResources();
    }

    /**
     * Simple listener to count events
     */
    private static final class Listener implements IDeviceTracksModelListener
    {
        private int addedCount = 0;
        private int changedCount = 0;

        @Override
        public void trackPointAdded(IMapObject device, ITimeLocation trackPoint)
        {
            addedCount += 1;
        }

        @Override
        public void deviceTimesChanged(IMapObject device, TimeBoundary boundary)
        {
            changedCount += 1;
        }
    }
}
