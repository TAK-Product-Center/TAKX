package gov.takx.api.plugin;

import jakarta.annotation.Nonnull;

/**
 * Provides a mutable metadata interface for manipulating String based key-value pairs.
 *
 */
public interface IPrePersistMetadata extends IMetadata
{
    /**
     * Sets the value of a plugin developer defined metadata field associated with a key.
     *
     * @param key   Metadata key to set.
     * @param value Value to associate with key.
     */
    void setMetadataValue(@Nonnull String key, String value);
}
