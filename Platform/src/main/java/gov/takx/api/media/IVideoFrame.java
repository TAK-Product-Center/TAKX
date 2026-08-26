package gov.takx.api.media;

import gov.takx.api.annotation.Experimental;

import java.awt.image.BufferedImage;

/**
 * Represents a frame of video data
 *
 * @since 2.8
 */
@Experimental
public interface IVideoFrame
{
    /**
     * Access the decoded image frame
     *
     * @return The decoded image frame
     */
    BufferedImage getImage();
}
