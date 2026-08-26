package gov.takx.core.mapobject;

import gov.takx.api.annotation.Headless;
import gov.takx.api.annotation.UseClientClassLoader;
import gov.takx.api.event.project.ProjectClosing;
import gov.takx.api.mapobject.IDataMonitor;
import gov.takx.api.mapobject.IMapObjectController;
import gov.takx.api.mapobject.IMapObjectManager;
import gov.takx.api.platform.IDataMonitorServices;
import gov.takx.commons.constants.State;
import gov.takx.commons.media.AudioAlert;
import gov.takx.platform.preferences.UserPreferenceEntry;
import jakarta.enterprise.event.Observes;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.DataLine;
import javax.sound.sampled.Line;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.SourceDataLine;
import javax.sound.sampled.UnsupportedAudioFileException;

import java.io.File;
import java.io.IOException;
import java.lang.invoke.MethodHandles;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Audio manager; few responsibilities, but very critical.  Some design decisions were made that sacrifice
 * performance for the sake of reliability, but performance should still be adequate for the frequency this
 * code is expected to run.
 * <p>
 * There is an audio player thread which starts on demand and never (intentionally) exits.
 * There is a watchdog timer which periodically destroys and restarts the audio player if it fails to check in.
 * <p>
 * Its interactions with the outside world are limited to the following:
 * <p>
 * - MapObjectController sometimes asks this class to play audio for its map object's data monitors.  This class
 * currently does too much of that work (the MOC should probably be responsible for inspecting the monitors).
 * - MapObjectController sometimes asks this class to stop playing audio for any of its monitors.
 * - The GlobeMapObjectManager will ask this class at shutdown to stop playing altogether.
 * - This class periodically plays an alarm sound from its playlist.  Each map object that has active alarms gets
 * one spot in the playlist, and that map object's most-serious alarm should sound.  Only map objects whose MOC
 * reported the alarm to this class will be processed.
 * <p>
 * Other things to keep in mind when editing this code:
 * - Make sure that synchronized blocks don't call other methods that synchronize on different objects.
 * If they must, make sure that objects are always locked in the same order in all possible code paths.
 * If they aren't, the code will almost certainly deadlock once in a while, with no explanation why.
 */
@Singleton
public class AudioManager implements ICoreAudioManager
{
    private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    public static final long WATCHDOG_INTERVAL = 60000L; // 1 minute

    public static final int INTER_ALARM_DELAY = 2500; // milliseconds
    private static final File WARNING_FILE = new File("resources" + File.separator + "audio" + File.separator + "ding.wav");
    private static final File CRITICAL_FILE = new File("resources" + File.separator + "audio" + File.separator + "CritAlarm.wav");
    private static final int EXTERNAL_BUFFER_SIZE = 128000;

    // if the audio system doesn't support the kind of audio we want to play, we only log it once -
    // otherwise we'll flood the user's logs with things they can't do anything about.
    private boolean loggedOnce = false;

    // Controllers of map objects (indexed by map object key) which were playing alarms last we checked.
    // They may or may not still be in alarm states.
    // All access to the playlist and the playlistChanged flag is synchronized on the playlist object, and
    // the playlist's monitor is used to notify the player thread when the playlist changes
    // (implemented in the waitForPlaylistChange and notifyPlaylistChanged functions)
    private final Map<String, IDataMonitorServices> playlist = new HashMap<>();
    private volatile boolean playlistChanged = false;

    private final Object playerThreadLock = new Object();
    private Thread playerThread = null;

    // If the player doesn't set this to "true" often enough, the watchdog will reboot the player.
    // Currently, "often enough" means at least once every 60 sec.
    private final AtomicBoolean stillAlive = new AtomicBoolean();

    @Inject
    @Headless
    private boolean headless;

    @Inject
    private IMapObjectManager mapObjectManager;

    AudioManager()
    {
        final Timer watchdog = new Timer("Audio Manager Watchdog", true);
        watchdog.schedule(new Watchdog(), WATCHDOG_INTERVAL, WATCHDOG_INTERVAL);
    }

    @Override
    public File getAlertFile(final State state)
    {
        if (state == null) return null;

        return switch (state)
        {
            case Warning, Serious -> WARNING_FILE; // Use same audio for Serious as for Warning
            case Critical -> CRITICAL_FILE;
            default -> null;
        };
    }

    @Override
    public void processDataMonitors(final IDataMonitorServices dataMonitorServices)
    {
        if (dataMonitorServices.getHighestState().compareTo(State.Warning) >= 0)
        {
            synchronized (playlist)
            {
                if (playlist.put(dataMonitorServices.getKey(), dataMonitorServices) == null)
                {
                    notifyPlaylistChange();
                }
            }

            startThreadIfNeeded();
        }
    }

    @Override
    public void clearAudioAlarms(IDataMonitorServices dataMonitorServices)
    {
        synchronized (playlist)
        {
            playlist.remove(dataMonitorServices.getKey());

            notifyPlaylistChange();
        }
    }

    private void clearAllAudioAlarms(@SuppressWarnings("unused") @Observes ProjectClosing event)
    {
        synchronized (playlist)
        {
            playlist.clear();

            notifyPlaylistChange();
        }
    }

    private void startThreadIfNeeded()
    {
        if (headless) return; // Don't play audio when we're running headless

        synchronized (playlist)
        {
            if (playlist.isEmpty())
            {
                return; // don't need a player if there's nothing to play
            }
        }

        synchronized (playerThreadLock)
        {
            if (playerThread == null)
            {
                playerThread = new Thread(new PlayerThread(), "Audio Manager");
                playerThread.setDaemon(true);
                playerThread.start();

                // first ones a freebie (don't want thread to get killed between now and run()
                stillAlive.set(true);
            }
        }
    }

    /**
     * Get the highest currently-audible alarm state in the playlist
     */
    private State getHighestAudibleState()
    {
        State highestAudibleState = State.Unknown;

        synchronized (playlist)
        {
            final List<String> dataMonitorServicesToRemove = new LinkedList<>();

            for (final IDataMonitorServices dataMonitorServices : playlist.values())
            {
                if (dataMonitorServices.getHighestState().compareTo(State.Warning) < 0)
                {
                    // map object is no longer alarming at all, drop it.
                    // (but don't drop it now, drop it after we're done iterating)
                    dataMonitorServicesToRemove.add(dataMonitorServices.getKey());
                } else
                {
                    final State audibleState = getHighestAudibleState(dataMonitorServices);
                    if (audibleState.compareTo(highestAudibleState) > 0)
                    {
                        highestAudibleState = audibleState;

                        // short-circuit if we know we've hit the max level.
                        // we'll miss out on pruning more controllers, but since we won't be iterating through
                        // them anywhere else I don't think we care.
                        if (highestAudibleState == State.Critical) break;
                    }
                }
            }

            // not signalling 'playlistChanged' here because we are about to deliver the latest state.
            dataMonitorServicesToRemove.forEach(playlist::remove);

            playlistChanged = false;
        }

        return highestAudibleState;
    }

    /**
     * Look up the highest playable alert state for a map object.  This is not always the same as
     * mapObject.getHighestState.  If no audio should play, returns State.Unknown.
     *
     * @return The alert level that should be played (State.Unknown if none should)
     */
    private State getHighestAudibleState(IDataMonitorServices dataMonitorServices)
    {
        final AudioAlert alertLevel = UserPreferenceEntry.AUDIO_ALERT.getValueAs(AudioAlert.class);

        IMapObjectController controller = mapObjectManager.getMapObjectController(dataMonitorServices.getKey());

        // If there's a device associated with the data monitor services, check the device's security level and mute settings.
        if (controller != null)
        {
            // Mute check - skip if controller is muted
            if (!controller.isAudioAlarmPlaybackEnabled())
            {
                logger.debug("Skipping map object because its controller has audio muted");
                return State.Unknown;
            }
        }

        // User preference check
        final State state = getHighestAudibleMonitorState(dataMonitorServices);
        if (stateShouldPlayAtAlertLevel(state, alertLevel))
        {
            return state;
        } else
        {
            logger.debug("Skipping audio due to alert level preference: {}", alertLevel);
            return State.Unknown;
        }
    }

    /**
     * I think IMapObject.getHighestState sometimes reports a state higher than the audio that should play
     * (specifically, when there is a critical-but-muted data monitor).
     * <p>
     * this finds the highest state that should actually play.
     */
    private State getHighestAudibleMonitorState(IDataMonitorServices controller)
    {
        return controller.getDataMonitors()
                .stream()
                .filter(dm -> dm.isAffectsAudioAndIcons() && dm.isAlarming() && !dm.isMuted())
                .map(IDataMonitor::getCurrentState)
                .max(Comparator.naturalOrder())
                .orElse(State.Unknown);
    }

    /**
     * Wait a specified number of milliseconds, but stop waiting if the playlist changes
     * (or has changed since the last call to {@link #getHighestAudibleState()}.
     *
     * @param millis The number of milliseconds to wait
     * @return true if the playlist changed.
     * @throws InterruptedException if the thread is interrupted.
     */
    private boolean waitForPlaylistChange(long millis) throws InterruptedException
    {
        synchronized (playlist)
        {
            if (!playlistChanged)
            {
                playlist.wait(millis);
            }

            return playlistChanged;
        }
    }

    /**
     * Notify callers of 'waitForPlaylistChange' that the playlist has changed.
     */
    private void notifyPlaylistChange()
    {
        synchronized (playlist)
        {
            playlistChanged = true;
            playlist.notifyAll();
        }
    }

    /**
     * Check whether the system alert level allows audio from an alarm state to play.
     *
     * @param alarmState the state of the alarm
     * @param alertLevel the system alert level setting
     * @return whether the alarm audio should be played
     */
    private static boolean stateShouldPlayAtAlertLevel(final State alarmState, final AudioAlert alertLevel)
    {
        // This could be one big boolean expression, but I find this quite a bit more readable.
        return switch (alertLevel)
        {
            case ALERTS_ON -> true;
            case CRITICAL_ONLY -> alarmState == State.Critical;
            default -> false;
        };
    }

    /**
     * Play a specified audio file.
     *
     * @param audioFile The File containing the audio to play
     */
    @Override
    @UseClientClassLoader // Ensure we can access Client resources
    public void playAudio(File audioFile)
            throws UnsupportedAudioFileException, IOException, LineUnavailableException
    {
        logger.trace("Attempting to open up audio stream");
        final AudioInputStream audioInputStream = AudioSystem.getAudioInputStream(audioFile);
        final AudioFormat audioFormat = audioInputStream.getFormat();
        final DataLine.Info info = new DataLine.Info(SourceDataLine.class, audioFormat);

        try
        {
            // one or the other of these next 2 lines sometimes causes the thread to
            // either hang or exit (I think it's silently exiting)...
            logger.trace("Attempting to get audio line");
            final Line line = AudioSystem.getLine(info);

            if (line == null)
            {
                logger.warn("AudioSystem.getLine returned null!");
                return;
            }

            if (!(line instanceof final SourceDataLine sourceDataLine))
            {
                logger.warn("AudioSystem.getLine returned {}, we need a SourceDataLine", line.getClass().getName());
                return;
            }

            logger.trace("Got audio line, attempting to open it");
            sourceDataLine.open(audioFormat);

            logger.trace("Starting the audio line");
            sourceDataLine.start();

            int nBytesRead = 0;
            final byte[] abData = new byte[EXTERNAL_BUFFER_SIZE];

            logger.trace("Pumping audio data into the line");
            while (nBytesRead != -1)
            {
                try
                {
                    nBytesRead = audioInputStream.read(abData, 0, abData.length);
                } catch (IOException e)
                {
                    logger.error("I/O exception reading audio input stream", e);
                }
                if (nBytesRead >= 0)
                {
                    sourceDataLine.write(abData, 0, nBytesRead);
                }
            }

            logger.trace("Draining the line");
            sourceDataLine.drain();

            logger.trace("Closing the line");
            sourceDataLine.close();

            try
            {
                audioInputStream.close();
            } catch (IOException e)
            {
                logger.warn("Unable to close audio input stream due to an I/O exception", e);
            }
        } catch (IllegalArgumentException e)
        {
            // TODO: find out what actually throws this exception and only catch that.
            if (!loggedOnce)
            {
                loggedOnce = true;
                logger.error("No line matching interface SourceDataLine supporting format PCM_SIGNED 44100.0 Hz, 16 " +
                        "bit, stereo, 4 bytes/frame, little-endian is supported.  Since there is no audio on this " +
                        "machine, no audio alarms will be played.");
            }
        }
    }

    private class PlayerThread implements Runnable
    {
        @Override
        public void run()
        {
            try
            {
                //noinspection InfiniteLoopStatement
                while (true)
                {
                    stillAlive.set(true);

                    final State state = getHighestAudibleState();
                    final File alertFile = getAlertFile(state);

                    // null is expected here any time an alert shouldn't play (because getAlertFile will return
                    // null for states that are below 'Warning'.
                    if (alertFile != null)
                    {
                        try
                        {
                            playAlarm(alertFile);
                        } catch (UnsupportedAudioFileException e)
                        {
                            logger.error("Failed to play audio clip {}: Unsupported audio format", alertFile);
                        } catch (IOException e)
                        {
                            logger.error("Failed to play audio clip {}: IO Exception", alertFile, e);
                        } catch (LineUnavailableException e)
                        {
                            logger.error("Failed to play audio clip {}: Audio line unavailable", alertFile);
                        }
                    }

                    // Wait up to 2.5 sec (less if a new alarm pops up) before playing the next track.
                    stillAlive.set(true);
                    if (waitForPlaylistChange(INTER_ALARM_DELAY))
                    {
                        logger.trace("Inter-alarm delay interrupted by new alarm");
                    }
                }
            } catch (InterruptedException e)
            {
                logger.warn("Audio player thread interrupted, probably by Audio Manager watchdog");
            } catch (Throwable t)
            {
                // I'm not convinced at this point that the uncaught-exception handler is working, so here's a catch-all.
                logger.error("Audio player thread killed by uncaught exception", t);
            } finally
            {
                logger.debug("Audio Manager thread dying");
            }
        }

        private void playAlarm(File alarm) throws UnsupportedAudioFileException, IOException, LineUnavailableException
        {
            // todo: make sure watchdog gets poked during audio playing
            // or maybe make watchdog interval scale to leave headroom for longest audio files
            playAudio(alarm);
        }
    }

    private class Watchdog extends TimerTask
    {
        @Override
        public void run()
        {
            boolean restartPlayer = false;
            synchronized (playerThreadLock)
            {
                if (playerThread == null) return;

                if (!stillAlive.getAndSet(false))
                {
                    logger.warn("Watchdog hasn't heard from player thread in last " + WATCHDOG_INTERVAL / 1e3 + " seconds, killing it.");
                    if (logger.isInfoEnabled())
                    {
                        logger.info("Player thread currently reports it is {}", playerThread.isAlive() ? "alive" : "dead");
                    }
                    if (playerThread.isAlive())
                    {
                        // TODO: add to a zombie list
                        if (logger.isDebugEnabled())
                        {
                            logger.debug("Player thread appears to be hung at: ");
                            for (StackTraceElement frame : playerThread.getStackTrace())
                            {
                                logger.debug("  {}", frame);
                            }
                        }
                    }

                    // stop the old one
                    playerThread.interrupt();

                    // force a new one to start
                    playerThread = null;
                    restartPlayer = true;
                } else if (!playerThread.isAlive())
                {
                    logger.warn("Audio player thread appears to have died, cleaning up and restarting it.");

                    playerThread = null;
                    restartPlayer = true;
                }
            }

            if (restartPlayer)

            {
                startThreadIfNeeded();
            }
        }
    }
}
