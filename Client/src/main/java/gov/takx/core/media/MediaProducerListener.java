package gov.takx.core.media;

/**
 * Interface to listen for newly decoded frames
 *
 * @since 2.8
 */
public interface MediaProducerListener
{
    /**
     * Callback indicating a new {@link MediaFrame} has been read
     *
     * @param mediaFrame The media frame
     */
    void onMediaFrame(MediaFrame mediaFrame);
}
