package gov.takx.test.mock.playback;

import gov.takx.api.playback.IPlaybackConfigurationListener;
import gov.takx.api.playback.IPlaybackLane;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Simple listener to track events from the playback configuration.  The members are public so they can be directly
 * accessed.
 *
 * @since 1.48
 */
public class TestPlaybackConfigurationListener implements IPlaybackConfigurationListener
{
    public int speed = 0;
    public List<IPlaybackLane> lanesAdded = new ArrayList<>();
    public List<IPlaybackLane> lanesRemoved = new ArrayList<>();
    public boolean optionsChanged = false;

    /**
     * Reset all out counts and lists.
     */
    public void reset()
    {
        speed = 0;
        lanesAdded.clear();
        lanesRemoved.clear();
        optionsChanged = false;
    }

    @Override
    public void playbackSpeedChanged(int speed)
    {
        this.speed = speed;
    }

    @Override
    public void playbackLanesAdded(IPlaybackLane... playbackLanes)
    {
        lanesAdded.addAll(Arrays.asList(playbackLanes));
    }

    @Override
    public void playbackLanesRemoved(IPlaybackLane... playbackLanes)
    {
        lanesRemoved.addAll(Arrays.asList(playbackLanes));
    }

    @Override
    public void displayOptionsChanged()
    {
        optionsChanged = true;
    }
}
