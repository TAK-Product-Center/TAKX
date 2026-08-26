package gov.takx.platform.utils.json;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;
import gov.tak.api.annotation.IncubatingApi;

import java.awt.Color;
import java.io.IOException;

/**
 * Serializer to handle {@link Color} instances. Encoded as a compact ARGB integer form.
 * @since 5.8
 */
@IncubatingApi(since = "5.8")
public class ColorSerializer extends StdSerializer<Color>
{
    protected static final String ENCODED_COLOR = "encodedARGBColor";

    public ColorSerializer()
    {
        super((Class<Color>) null);
    }

    @Override
    public void serialize(Color color, JsonGenerator generator, SerializerProvider provider) throws IOException
    {
        generator.writeStartObject();
        generator.writeNumberField(ENCODED_COLOR, color.getRGB());
        generator.writeEndObject();
    }
}
