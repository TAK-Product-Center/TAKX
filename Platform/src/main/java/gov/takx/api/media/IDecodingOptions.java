package gov.takx.api.media;

import gov.takx.api.annotation.Experimental;

/**
 * Options to use when decoding media
 *
 * @since 2.8
 */
@Experimental
public interface IDecodingOptions
{
    /**
     * Check if video frames are being decoded
     *
     * @return True if video frames are being decoded
     */
    boolean isDecodeVideo();

    /**
     * Set flag to decode video frames
     *
     * @param decodeVideo True to decode video frames
     */
    void setDecodeVideo(boolean decodeVideo);

    /**
     * Check if audio data is being decoded
     *
     * @return True if audio data is being decoded
     */
    boolean isDecodeAudio();

    /**
     * Set flag to decode audio data
     *
     * @param decodeAudio True to decode audio frames
     */
    void setDecodeAudio(boolean decodeAudio);

    /**
     * Check if data streams (subtitle, etc.) are being decoded
     *
     * @return True if data streams are being decoded
     */
    boolean isDecodeData();

    /**
     * Set flag to decode data streams
     *
     * @param decodeData True to decode data streams
     */
    void setDecodeData(boolean decodeData);
}
