package gov.takx.platform.utils.json;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;
import gov.tak.api.annotation.IncubatingApi;
import gov.tak.api.engine.map.coords.IGeoPoint;
import gov.tak.api.maps.coords.GeoPoint;

import java.io.IOException;

/**
 * Deserializer to handle {@link IGeoPoint} instances.
 * @since 5.8
 */
@IncubatingApi(since = "5.8")
public class IGeoPointDeserializer extends StdDeserializer<IGeoPoint>
{
    public IGeoPointDeserializer()
    {
        super((Class<?>) null);
    }

    @Override
    public IGeoPoint deserialize(JsonParser p, DeserializationContext context) throws IOException
    {
        final JsonNode node = p.readValueAsTree();
        return GeoPoint.parseGeoPoint(node.get(IGeoPointSerializer.ENCODED_POINT).asText());
    }
}
