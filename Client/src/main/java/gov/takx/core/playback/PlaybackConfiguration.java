package gov.takx.core.playback;

import gov.takx.api.playback.IPlaybackConfiguration;
import gov.takx.api.playback.IPlaybackConfigurationListener;
import gov.takx.api.playback.IPlaybackLane;
import gov.takx.api.playback.IPlaybackStrategy;
import gov.takx.core.playback.globe.GlobePlaybackLane;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.invoke.MethodHandles;
import java.util.ArrayList;
import java.util.List;

/**
 * Concrete implementation of the playback configuration.  This configuration manages high-level elements like
 * the playback lanes, whether to hide or show future tracks/icons, and the fixed/dynamic setting of the time span.
 *
 * @since 1.47
 */
public class PlaybackConfiguration implements IPlaybackConfiguration
{
    private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    private int playbackSpeed = 1;
    private final List<IPlaybackLane> playbackLanes = new ArrayList<>();
    private final List<IPlaybackConfigurationListener> configurationListeners = new ArrayList<>();

    IPlaybackStrategy playbackStrategy;
    private boolean autofitTimeline = true;

    /**
     * Construct the configuration.
     */
    public PlaybackConfiguration()
    {
    }

    @Override
    public void disposeResources()
    {
        if (logger.isTraceEnabled()) logger.trace("PlaybackConfiguration.dispose");

        for (IPlaybackLane playbackLane : playbackLanes)
        {
            playbackLane.disposeResources();
        }

        playbackLanes.clear();
        configurationListeners.clear();
    }

    @Override
    public void setPlaybackSpeed(int speed)
    {
        if (playbackSpeed == speed) return; // No change

        playbackSpeed = speed;
        firePlaybackSpeedChanged(speed);
    }

    @Override
    public int getPlaybackSpeed()
    {
        return playbackSpeed;
    }

    @Override
    public void addPlaybackLanes(IPlaybackLane... playbackLanes)
    {
        for (IPlaybackLane playbackLane : playbackLanes)
        {
            // Never add a lane more then once
            if (!this.playbackLanes.contains(playbackLane))
            {
                this.playbackLanes.add(playbackLane);
            }
        }

        fireLanesAdded(playbackLanes);
    }

    @Override
    public void removePlaybackLanes(IPlaybackLane... playbackLanes)
    {
        boolean aLaneRemoved = false;
        for (IPlaybackLane playbackLane : playbackLanes)
        {
            if (!(playbackLane instanceof GlobePlaybackLane) && this.playbackLanes.remove(playbackLane))
            {
                aLaneRemoved = true;
            }
        }

        // Only perform the cleanup if the lane was in our current configuration
        if (aLaneRemoved)
        {
            fireLanesRemoved(playbackLanes);
        }
    }

    @Override
    public void replacePlaybackLane(IPlaybackLane oldLane, IPlaybackLane newLane)
    {
        final int index = playbackLanes.indexOf(oldLane);

        if (index >= 0)
        {
            if (oldLane instanceof GlobePlaybackLane)
            {
                logger.error("Unable to replace the Globe Playback Lane");
            } else
            {
                playbackLanes.remove(index);
                fireLanesRemoved(oldLane);

                playbackLanes.add(index, newLane);
                fireLanesAdded(newLane);
            }
        } else
        {
            logger.error("Unable to replace PlaybackLane, old lane not found: {}", oldLane);
        }
    }

    @Override
    public List<IPlaybackLane> getPlaybackLanes()
    {
        return new ArrayList<>(playbackLanes);
    }

    @Override
    public void addPlaybackConfigurationListener(IPlaybackConfigurationListener listener)
    {
        synchronized (configurationListeners)
        {
            configurationListeners.add(listener);
        }
    }

    @Override
    public void removePlaybackConfigurationListener(IPlaybackConfigurationListener listener)
    {
        synchronized (configurationListeners)
        {
            configurationListeners.remove(listener);
        }
    }

    /**
     * Fire a playback speed changed event.
     *
     * @param speed The new playback speed
     */
    private void firePlaybackSpeedChanged(int speed)
    {
        synchronized (configurationListeners)
        {
            for (IPlaybackConfigurationListener listener : configurationListeners)
            {
                listener.playbackSpeedChanged(speed);
            }
        }
    }

    /**
     * Fire a playback lanes added event.
     *
     * @param playbackLanes The lanes that were added
     */
    private void fireLanesAdded(IPlaybackLane... playbackLanes)
    {
        synchronized (configurationListeners)
        {
            for (IPlaybackConfigurationListener listener : configurationListeners)
            {
                listener.playbackLanesAdded(playbackLanes);
            }
        }
    }

    /**
     * Fire a playback lanes removed event.
     *
     * @param playbackLanes The lanes that were removed
     */
    private void fireLanesRemoved(IPlaybackLane... playbackLanes)
    {
        synchronized (configurationListeners)
        {
            for (IPlaybackConfigurationListener listener : configurationListeners)
            {
                listener.playbackLanesRemoved(playbackLanes);
            }
        }
    }

    @Override
    public void setPlaybackStrategy(IPlaybackStrategy playbackStrategy)
    {
        this.playbackStrategy = playbackStrategy;
    }

    @Override
    public IPlaybackStrategy getPlaybackStrategy()
    {
        return playbackStrategy;
    }

    @Override
    public void setAutofitTimeline(boolean autofitTimeline)
    {
        this.autofitTimeline = autofitTimeline;
    }

    @Override
    public boolean isAutofitTimeline()
    {
        return autofitTimeline;
    }
}
