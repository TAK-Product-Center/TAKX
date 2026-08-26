package gov.takx.api.playback;

import java.util.List;

/**
 * Contract for playback configuration.
 * <p/>
 * This is the general configuration of playback, not execution details like playing, stopped, position changes, etc.
 * If you need access to those data or events, see {@link IPlaybackEventListener} and
 * {@link IPlaybackManager#addPlaybackEventListener(IPlaybackEventListener)}
 *
 * @see IPlaybackManager#addPlaybackEventListener(IPlaybackEventListener)
 * @see IPlaybackEventListener
 * @since 1.47
 */
@SuppressWarnings("UnusedDeclaration")
public interface IPlaybackConfiguration extends IDisposable
{
    String HIDE_ICONS_DURING_PLAYBACK = "hideIconsDuringPlayback";
    String HIDE_FUTURE_PLAYBACK_TRACKS = "hideFuturePlaybackTracks";

    /**
     * Set the current playback speed.
     *
     * @param speed new playback speed.
     */
    void setPlaybackSpeed(int speed);

    /**
     * @return the current playback speed
     */
    int getPlaybackSpeed();

    /**
     * Add a set of playback lanes to the configuration.
     * <p/>
     * If you want the global time span to be updated to ensure that it contains the time boundary of this new lane,
     * you should call {@link IPlaybackManager#fitTimeSpanToLanes()}.
     *
     * @param playbackLanes The lanes to add
     */
    void addPlaybackLanes(IPlaybackLane... playbackLanes);

    /**
     * Remove a set of playback lanes from the configuration
     * <p/>
     * If you want the global time span to be updated to ensure that it contains the time boundary of this new lane,
     * you should call {@link IPlaybackManager#fitTimeSpanToLanes()}.
     * <p/>
     * It is assumed that the caller of this method will take care of disposing of any resources held by the
     * removed playback lanes.
     *
     * @param playbackLanes The lane to add
     */
    void removePlaybackLanes(IPlaybackLane... playbackLanes);

    /**
     * Replace an existing playback lane with a new one.
     * <p/>
     * If you want the global time span to be updated to ensure that it contains the time boundary of this new lane,
     * you should call {@link IPlaybackManager#fitTimeSpanToLanes()}.
     * <p/>
     * It is assumed that the caller of this method will take care of disposing of any resources held by the
     * old playback lane.
     *
     * @param oldLane The lane to replace
     * @param newLane The replacement lane
     */
    void replacePlaybackLane(IPlaybackLane oldLane, IPlaybackLane newLane);

    /**
     * @return A list of all the playback lanes installed
     */
    List<IPlaybackLane> getPlaybackLanes();

    /**
     * Add a listener to the playback configuration.
     *
     * @param listener Listener to register
     */
    void addPlaybackConfigurationListener(IPlaybackConfigurationListener listener);

    /**
     * Remove a listener from the playback configuration.
     *
     * @param listener Listener to remove
     */
    void removePlaybackConfigurationListener(IPlaybackConfigurationListener listener);

    /**
     * Set a custom Playback Strategy
     *
     * @param playbackStrategy custom implementation of the IPlaybackStrategy interface
     */
    void setPlaybackStrategy(IPlaybackStrategy playbackStrategy);

    /**
     * Get the current Playback Strategy.
     */
    IPlaybackStrategy getPlaybackStrategy();

    boolean isAutofitTimeline();

    void setAutofitTimeline(boolean autofitTimeline);
}
