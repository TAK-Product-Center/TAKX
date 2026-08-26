package gov.tak.api.comms.takserver.mission.data;

import gov.tak.api.comms.takserver.IResource;
import jakarta.annotation.Nonnull;

import java.time.Instant;
import java.util.List;

/**
 * A {@link IResource} implementation that delegates all values to another provided {@link IResource}.
 *
 * @since 3.5
 */
public class DelegatingResource implements IResource
{
    private final IResource resource;

    /**
     * @param resource Wrapped resource.
     */
    public DelegatingResource(@Nonnull IResource resource)
    {
        this.resource = resource;
    }

    @Override
    public String uid()
    {
        return resource.uid();
    }

    @Override
    public String hash()
    {
        return resource.hash();
    }

    @Override
    public String name()
    {
        return resource.name();
    }

    @Override
    public String fileName()
    {
        return resource.fileName();
    }

    @Override
    public Instant submissionTime()
    {
        return resource.submissionTime();
    }

    @Override
    public String submitter()
    {
        return resource.submitter();
    }

    @Override
    public String creatorUid()
    {
        return resource.creatorUid();
    }

    @Override
    public List<String> keywords()
    {
        return resource.keywords();
    }

    @Override
    public String mimeType()
    {
        return resource.mimeType();
    }

    @Override
    public long size()
    {
        return resource.size();
    }

    @Override
    public long expiration()
    {
        return resource.expiration();
    }

    @Override
    public boolean equals(Object o)
    {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        DelegatingResource that = (DelegatingResource) o;
        return resource.equals(that.resource);
    }

    @Override
    public int hashCode()
    {
        return resource.hashCode();
    }

    @Override
    public String toString()
    {
        return resource.toString();
    }
}
