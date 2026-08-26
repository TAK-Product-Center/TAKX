package gov.takx.platform.utils.json;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;
import gov.tak.api.annotation.IncubatingApi;

import java.awt.Color;
import java.io.IOException;

/**
 * Deserializer to handle {@link Color} instances.
 * @since 5.8
 */
@IncubatingApi(since = "5.8")
public class ColorDeserializer extends StdDeserializer<Color>
{
    public ColorDeserializer()
    {
        super((Class<?>) null);
    }

    @Override
    public Color deserialize(JsonParser parser, DeserializationContext context) throws IOException
    {
        final JsonNode node = parser.readValueAsTree();
        return new Color(node.get(ColorSerializer.ENCODED_COLOR).asInt(), true);
    }
}