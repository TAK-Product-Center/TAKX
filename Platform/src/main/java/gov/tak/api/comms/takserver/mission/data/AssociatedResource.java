package gov.tak.api.comms.takserver.mission.data;

import gov.tak.api.comms.takserver.IResource;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import java.util.Objects;

/**
 * An {@link IResource} associated with an uploader via their callsign name.
 *
 * @since 3.5
 */
public class AssociatedResource extends DelegatingResource
{
    private final String uploaderCallsign;

    /**
     * @param resource Wrapped resource.
     * @param uploaderCallsign The resolved callsign of the resource's {@link Resource#creatorUid() uploader}.
     */
    public AssociatedResource(@Nonnull IResource resource, @Nullable String uploaderCallsign)
    {
        super(resource);
        this.uploaderCallsign = uploaderCallsign == null ? resource.creatorUid() : uploaderCallsign;
    }

    /**
     * @return {@code true} when the callsign has been resolved properly.
     * {@code false} means we do not know the callsign associated with the {@link #creatorUid()}.
     */
    public boolean isCallsignResolved()
    {
        return !Objects.equals(uploaderCallsign, creatorUid());
    }

    /**
     * @return The callsign of the resource's {@link Resource#creatorUid() uploader} if known. If not known, mirrors {@link #creatorUid()}.
     */
    @Nonnull
    public String getUploaderCallsign()
    {
        return uploaderCallsign;
    }

    @Nonnull
    public String getDisplayName()
    {
        return isCallsignResolved() ? getUploaderCallsign() : submitter();
    }
}
