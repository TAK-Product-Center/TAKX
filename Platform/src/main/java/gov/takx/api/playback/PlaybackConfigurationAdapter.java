package gov.takx.api.playback;

/**
 * Simple adapter for configuration listeners.  All methods have empty bodies.  Override just the methods you need.
 *
 * @since 1.47
 */
public class PlaybackConfigurationAdapter implements IPlaybackConfigurationListener
{
    @Override
    public void playbackSpeedChanged(int speed)
    {
    }

    @Override
    public void playbackLanesAdded(IPlaybackLane... playbackLanes)
    {
    }

    @Override
    public void playbackLanesRemoved(IPlaybackLane... playbackLanes)
    {
    }

    @Override
    public void displayOptionsChanged()
    {
    }
}
