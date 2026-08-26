package gov.takx.core.mapobject;

import gov.takx.api.mapobject.IAudioManager;
import gov.takx.api.platform.IDataMonitorServices;

import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.UnsupportedAudioFileException;

import java.io.File;
import java.io.IOException;

/**
 * @since 2.33
 */
public interface ICoreAudioManager extends IAudioManager
{
    /**
     * Updates the set of tracks that should be playing for a particular data monitor service.
     *
     * @param dataMonitorServices the data monitor service
     */
    void processDataMonitors(IDataMonitorServices dataMonitorServices);

    /**
     * Clears all audio for a data monitor service.  Removes all tracks associated with the service from both
     * the playlist and the alarm map
     *
     * @param dataMonitorServices The service whose audio is to be silenced
     */
    void clearAudioAlarms(IDataMonitorServices dataMonitorServices);

    /**
     * Play the given audio file.
     *
     * @param audioFile File to play
     * @throws UnsupportedAudioFileException
     * @throws IOException
     * @throws LineUnavailableException
     */
    void playAudio(File audioFile)
            throws UnsupportedAudioFileException, IOException, LineUnavailableException;
}
