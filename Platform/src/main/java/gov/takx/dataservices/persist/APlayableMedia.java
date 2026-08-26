package gov.takx.dataservices.persist;

import gov.takx.api.annotation.Internal;
import gov.takx.api.annotation.PseudoBean;
import gov.takx.api.media.APlayableMediaBuilder;
import gov.takx.api.media.IMedia;
import gov.takx.api.media.IPlayable;
import gov.takx.core.media.MediaLengthAnalyzer;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import jakarta.annotation.PostConstruct;
import jakarta.inject.Inject;
import jakarta.persistence.Entity;
import jakarta.persistence.Transient;

import java.net.URI;

/**
 * Abstract base for media types that are playable (i.e., have a duration).
 * @since 5.0
 */
@PseudoBean
@Internal
@Entity
public abstract class APlayableMedia<M extends IMedia & IPlayable, B extends APlayableMediaBuilder<M, B>> extends AMedia<M, B> implements IPlayable
{
    @Inject
    @Transient
    private MediaLengthAnalyzer mediaLengthAnalyzer;

    private Long duration;

    private String captureLocation;

    protected APlayableMedia()
    {
        // JPA
    }

    protected APlayableMedia(@Nonnull B builder)
    {
        super(builder);
    }

    @PostConstruct
    private void postConstruct()
    {
        final B mediaBuilder = getMediaBuilder();

        if (mediaBuilder == null) return;

        // Extract duration from the local file, or default to builder values
        updateDuration(mediaBuilder.getDuration());
    }

    /**
     * Update the duration from a local file if our source is a file URI.
     *
     * @param defaultDuration Duration to set if not local file or duration can't be extracted from the local file
     */
    private void updateDuration(Long defaultDuration)
    {
        final String localFile = getLocalFilePath();
        final Long newDuration = localFile != null
                ? mediaLengthAnalyzer.analyzeDuration(localFile).orElse(defaultDuration)
                : defaultDuration;
        setDuration(newDuration);
    }

    @Override
    public void setSourceUri(@Nonnull URI sourceUri)
    {
        super.setSourceUri(sourceUri);
        updateDuration(null);
    }

    @Override
    public void setDuration(@Nullable Long duration)
    {
        propertyChangeSupport.updateAndFire(getDurationPropertyName(), this.duration, duration, () -> this.duration = duration);
    }

    @Override
    @Nullable
    public Long getDuration()
    {
        return duration;
    }

    @Transient
    protected abstract String getDurationPropertyName();

    @Override
    public void setCaptureLocation(@Nullable String captureLocation)
    {
        propertyChangeSupport.updateAndFire(getCaptureLocationPropertyName(), this.captureLocation, captureLocation,
                () -> this.captureLocation = captureLocation);
    }

    @Override
    @Nullable
    public String getCaptureLocation()
    {
        return captureLocation;
    }

    /**
     * Get the property name for the capture location property.
     *
     * @return The property name for the capture location.
     * @since 5.1.0
     */
    @Transient
    protected abstract String getCaptureLocationPropertyName();
}
