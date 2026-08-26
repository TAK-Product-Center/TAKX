package gov.takx.core.media;

import gov.takx.api.media.AudioBuilder;
import gov.takx.api.media.IAudio;
import gov.takx.api.media.IImage;
import gov.takx.api.media.IVideo;
import gov.takx.api.media.ImageBuilder;
import gov.takx.api.media.VideoBuilder;
import gov.takx.api.media.factory.IMediaFactory;
import gov.takx.dataservices.persist.Audio;
import gov.takx.dataservices.persist.Image;
import gov.takx.dataservices.persist.Video;
import jakarta.enterprise.context.ApplicationScoped;

/**
 * @since 2.15
 */
@ApplicationScoped
public class MediaFactory implements IMediaFactory
{
    @Override
    public IAudio build(AudioBuilder audioBuilder)
    {
        return new Audio(audioBuilder);
    }

    @Override
    public IVideo build(VideoBuilder videoBuilder)
    {
        return new Video(videoBuilder);
    }

    @Override
    public IImage build(ImageBuilder imageBuilder)
    {
        return new Image(imageBuilder);
    }
}
