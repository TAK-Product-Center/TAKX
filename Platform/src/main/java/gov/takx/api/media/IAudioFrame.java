package gov.takx.api.media;

import gov.takx.api.annotation.Experimental;

import javax.sound.sampled.AudioFormat;

/**
 * Represents a packet of audio data
 *
 * @since 2.8
 */
@Experimental
public interface IAudioFrame
{
    byte[] getAudioData();

    AudioFormat getAudioFormat();
}
