package gov.takx.platform.utils.json;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;
import gov.tak.api.annotation.IncubatingApi;
import gov.tak.api.maps.coords.GeoPoint;

import java.io.IOException;

/**
 * Deserializer to handle {@link GeoPoint} instances.
 * @since 5.8
 */
@IncubatingApi(since = "5.8")
public class GeoPointDeserializer extends StdDeserializer<GeoPoint>
{
    public GeoPointDeserializer()
    {
        super((Class<GeoPoint>) null);
    }

    @Override
    public GeoPoint deserialize(JsonParser parser, DeserializationContext context) throws IOException
    {
        final JsonNode node = parser.readValueAsTree();
        return GeoPoint.parseGeoPoint(node.get(IGeoPointSerializer.ENCODED_POINT).asText());
    }
}
