package gov.takx.dataservices.persist;

import gov.takx.api.plugin.IPrePersistMetadata;
import jakarta.annotation.Nonnull;
import jakarta.persistence.Column;
import jakarta.persistence.MappedSuperclass;
import jakarta.persistence.Transient;
import org.hibernate.Length;

import java.util.Set;

/**
 * Abstract base for persistent entities that have metadata. The metadata is stored in the DB as a JSON encoded
 * string which is turned into an internal map on demand.
 *
 * @see JsonMetadata
 * @since 2.37
 */
@MappedSuperclass
public abstract class AEntityWithMetadata implements IPrePersistMetadata
{
    @SuppressWarnings({"FieldCanBeLocal", "FieldMayBeFinal"})
    @Column(length = Length.LONG32)
    private volatile String metadataAsJson = "";

    @Transient
    private volatile JsonMetadata metadata = null;

    protected AEntityWithMetadata()
    {
    }

    @Override
    public void setMetadataValue(@Nonnull String metadataKey, String metadataValue)
    {
        getOrCreateMetadata().setMetadataValue(metadataKey, metadataValue);
        metadataAsJson = metadata.toJson();
    }

    @Nonnull
    @Override
    public Set<String> getMetadataKeys()
    {
        return getOrCreateMetadata().getMetadataKeys();
    }

    @Override
    public String getMetadataValue(String metadataKey)
    {
        return getOrCreateMetadata().getMetadataValue(metadataKey);
    }

    /**
     * @return The JSON form of our metadata (attribute set)
     * @since 3.0
     */
    protected String getMetadataAsJson()
    {
        return getOrCreateMetadata().toJson();
    }

    /**
     * @return Our metadata, created and initialized on demand
     * @since 2.37
     */
    protected synchronized JsonMetadata getOrCreateMetadata()
    {
        if (metadata == null)
        {
            metadata = new JsonMetadata();
            metadata.fromJson(metadataAsJson);
        }
        return metadata;
    }
}
