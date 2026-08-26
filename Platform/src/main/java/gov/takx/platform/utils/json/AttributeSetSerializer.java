package gov.takx.platform.utils.json;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;
import gov.tak.api.annotation.IncubatingApi;
import gov.tak.api.util.AttributeSet;

import java.io.IOException;

/**
 * Serializer to handle {@link AttributeSet} instances. Encoded as a string via {@link AttributeSet} methods.
 * @since 5.8
 */
@IncubatingApi(since = "5.8")
public class AttributeSetSerializer extends StdSerializer<AttributeSet>
{
    public AttributeSetSerializer()
    {
        super((Class<AttributeSet>) null);
    }

    @Override
    public void serialize(AttributeSet attributeSet, JsonGenerator gen, SerializerProvider provider) throws IOException
    {
        gen.writeRawValue(attributeSet.toJson()); // Let AttributeSet produce the JSON
    }
}