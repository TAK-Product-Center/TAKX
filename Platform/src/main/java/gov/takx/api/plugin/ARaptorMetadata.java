package gov.takx.api.plugin;

import jakarta.annotation.Nonnull;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * Abstract implementation of a metadata management class.
 *
 * @since 1.4
 */
public abstract class ARaptorMetadata implements IMetadata
{
    /**
     * Map that manages the plugin's set of key-value metadata pairs used to
     * describe arbitrary features of the plugin.  <b>NOTE:</b> The namespace
     * {@code gov.takx} is strictly reserved and must not be used.
     */
    protected final Map<String, String> metadata = new HashMap<>();

    @Nonnull
    @Override
    public Set<String> getMetadataKeys()
    {
        return metadata.keySet();
    }

    @Override
    public String getMetadataValue(String key)
    {
        return metadata.get(key);
    }
}
