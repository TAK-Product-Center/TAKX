package gov.takx.dataservices.persist;

import gov.tak.api.video.ConnectionEntry;
import gov.takx.api.annotation.Internal;
import gov.takx.api.annotation.PseudoBean;
import gov.takx.api.media.IVideo;
import gov.takx.api.media.MediaProperties;
import gov.takx.api.media.VideoBuilder;
import gov.takx.dataservices.utils.ConnectionEntryConverter;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;
import jakarta.persistence.Transient;
import org.hibernate.Length;

/**
 * @since 2.15
 */
@PseudoBean
@Internal
@Entity
@DiscriminatorValue("Video")
public class Video extends APlayableMedia<IVideo, VideoBuilder> implements IVideo
{
    @Convert(converter = ConnectionEntryConverter.class)
    @Column(length = Length.LONG32)
    private ConnectionEntry connectionEntry;

    protected Video()
    {
        // JPA
    }

    public Video(@Nonnull VideoBuilder builder)
    {
        super(builder);
        connectionEntry = builder.getConnectionEntry();
    }

    @Transient
    @Override
    protected String getDurationPropertyName()
    {
        return MediaProperties.Video.DURATION;
    }

    @Transient
    @Override
    protected String getCaptureLocationPropertyName()
    {
        return MediaProperties.Video.CAPTURE;
    }

    @Nullable
    @Override
    public ConnectionEntry getConnectionEntry()
    {
        return connectionEntry;
    }
}
