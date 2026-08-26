package gov.takx.api.media.factory;

import gov.takx.api.media.AudioBuilder;
import gov.takx.api.media.IAudio;
import gov.takx.api.media.IImage;
import gov.takx.api.media.IVideo;
import gov.takx.api.media.ImageBuilder;
import gov.takx.api.media.VideoBuilder;

/**
 * @since 2.15
 */
public interface IMediaFactory
{
    IAudio build(AudioBuilder audioBuilder);

    IVideo build(VideoBuilder videoBuilder);

    IImage build(ImageBuilder imageBuilder);
}
