package gov.takx.api.plugin;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import java.util.Set;

/**
 * Simple key-value, String based immutable metadata interface.
 *
 * @since 1.1
 */
public interface IMetadata
{
    /**
     * Identifies an optional list of plugin developer defined metadata field key names.  <b>NOTE:</b> The namespace
     * {@code gov.takx} is strictly reserved and must not be used.
     *
     * @return Set of metadata keys. This set may not be modified.
     */
    @Nonnull
    Set<String> getMetadataKeys();

    /**
     * Retrieves the value of a plugin developer defined metadata field associated with the key.
     *
     * @param key Metadata key to lookup
     * @return Metadata field value associated with the key, or {@code null} if the key could not be found
     */
    @Nullable
    String getMetadataValue(String key);
}
