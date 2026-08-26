package gov.takx.platform.utils.json;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;
import gov.tak.api.annotation.IncubatingApi;
import gov.tak.api.util.AttributeSet;

import java.io.IOException;

/**
 * Deserializer to handle {@link AttributeSet} instances. Parsed via {@link AttributeSet} constructor.
 * @since 5.8
 */
@IncubatingApi(since = "5.8")
public class AttributeSetDeserializer extends StdDeserializer<AttributeSet>
{
    public AttributeSetDeserializer()
    {
        super((Class<?>) null);
    }

    @Override
    public AttributeSet deserialize(JsonParser p, DeserializationContext context) throws IOException
    {
        return new AttributeSet(p.readValueAsTree().toString()); // Let AttributeSet parse it
    }
}
