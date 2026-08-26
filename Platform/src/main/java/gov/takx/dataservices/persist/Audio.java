package gov.takx.dataservices.persist;

import gov.takx.api.annotation.Internal;
import gov.takx.api.annotation.PseudoBean;
import gov.takx.api.media.AudioBuilder;
import gov.takx.api.media.IAudio;
import gov.takx.api.media.MediaProperties;
import jakarta.annotation.Nonnull;
import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;
import jakarta.persistence.Transient;

/**
 * @since 2.15
 */
@PseudoBean
@Internal
@Entity
@DiscriminatorValue("Audio")
public class Audio extends APlayableMedia<IAudio, AudioBuilder> implements IAudio
{
    protected Audio()
    {
        // JPA
    }

    public Audio(@Nonnull AudioBuilder builder)
    {
        super(builder);
    }

    @Transient
    @Override
    protected String getDurationPropertyName()
    {
        return MediaProperties.Audio.DURATION;
    }

    @Transient
    @Override
    protected String getCaptureLocationPropertyName()
    {
        return MediaProperties.Audio.CAPTURE;
    }
}
