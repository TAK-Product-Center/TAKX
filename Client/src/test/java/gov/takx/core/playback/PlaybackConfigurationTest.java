package gov.takx.core.playback;

import gov.takx.api.playback.IPlaybackLane;
import gov.takx.test.AContainerTest;
import gov.takx.test.mock.playback.TestPlaybackConfigurationListener;
import gov.takx.test.mock.playback.TestPlaybackLane;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @since 1.48
 */
public class PlaybackConfigurationTest extends AContainerTest
{
    @Test
    public void testSpeed()
    {
        final PlaybackConfiguration configuration = new PlaybackConfiguration();
        final TestPlaybackConfigurationListener listener = new TestPlaybackConfigurationListener();

        configuration.addPlaybackConfigurationListener(listener);

        configuration.setPlaybackSpeed(2);
        assertThat(configuration.getPlaybackSpeed()).isEqualTo(2);
        assertThat(listener.speed).isEqualTo(2);
    }

    @Test
    public void testLanes()
    {
        final PlaybackConfiguration configuration = new PlaybackConfiguration();
        final TestPlaybackConfigurationListener listener = new TestPlaybackConfigurationListener();

        configuration.addPlaybackConfigurationListener(listener);

        final TestPlaybackLane lane1 = new TestPlaybackLane();
        final TestPlaybackLane lane2 = new TestPlaybackLane();
        final TestPlaybackLane lane3 = new TestPlaybackLane();
        final TestPlaybackLane lane4 = new TestPlaybackLane();

        configuration.addPlaybackLanes(lane2, lane4);
        List<IPlaybackLane> playbackLanes = configuration.getPlaybackLanes();

        assertThat(playbackLanes).hasSize(2);
        assertThat(playbackLanes).containsExactly(lane2, lane4);
        assertThat(listener.lanesAdded.size()).isEqualTo(2);
        assertThat(listener.lanesRemoved.size()).isEqualTo(0);

        listener.reset();
        configuration.removePlaybackLanes(lane2);
        playbackLanes = configuration.getPlaybackLanes();

        assertThat(playbackLanes).hasSize(1);
        assertThat(listener.lanesAdded.size()).isEqualTo(0);
        assertThat(listener.lanesRemoved.size()).isEqualTo(1);
        assertThat(playbackLanes).containsExactly(lane4);

        listener.reset();
        configuration.addPlaybackLanes(lane1);
        configuration.replacePlaybackLane(lane4, lane3);
        playbackLanes = configuration.getPlaybackLanes();

        assertThat(playbackLanes).hasSize(2);
        assertThat(listener.lanesAdded.size()).isEqualTo(2);
        assertThat(listener.lanesAdded).containsExactly(lane1, lane3);
        assertThat(listener.lanesRemoved.size()).isEqualTo(1);
        assertThat(listener.lanesRemoved).containsExactly(lane4);
        assertThat(playbackLanes).containsExactly(lane3, lane1);
    }

    @Test
    public void testPlaybackSettings()
    {
        final PlaybackConfiguration configuration = new PlaybackConfiguration();
        final TestPlaybackConfigurationListener listener = new TestPlaybackConfigurationListener();

        configuration.addPlaybackConfigurationListener(listener);
    }
}
