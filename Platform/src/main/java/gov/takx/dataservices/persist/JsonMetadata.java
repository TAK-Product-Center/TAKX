package gov.takx.dataservices.persist;

import gov.takx.api.annotation.Internal;
import gov.takx.api.plugin.ARaptorMetadata;
import gov.takx.api.plugin.IPrePersistMetadata;
import gov.takx.commons.utils.StringUtils;
import jakarta.annotation.Nonnull;
import org.json.JSONObject;

import java.util.Iterator;

/**
 * Metadata holder that can marshal to/from JSON.
 *
 * @since 2.37
 */
@Internal
public class JsonMetadata extends ARaptorMetadata implements IPrePersistMetadata
{
    @Override
    public void setMetadataValue(@Nonnull String metadataKey, @Nonnull String metadataValue)
    {
        metadata.put(metadataKey, metadataValue);
    }

    /**
     * Parse the given JSON into our metadata key/values.
     *
     * @param metadataJson JSON string to parse
     */
    public void fromJson(String metadataJson)
    {
        if (!StringUtils.isEmpty(metadataJson, true))
        {
            final var jsonObject = new JSONObject(metadataJson);
            final Iterator<?> it = jsonObject.keys();

            while (it.hasNext())
            {
                final String key = (String) it.next();
                metadata.put(key, jsonObject.getString(key));
            }
        }
    }

    /**
     * @return JSON encoded string representing the metadata key/value pairs
     */
    public String toJson()
    {
        if (metadata.isEmpty())
        {
            return "";
        } else
        {
            final var jsonObject = new JSONObject();
            metadata.forEach(jsonObject::put);
            return jsonObject.toString();
        }
    }
}
