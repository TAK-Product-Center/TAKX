package gov.tak.api.comms.takserver.mission;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;
import gov.tak.api.cot.CotFormatting;

import java.io.IOException;
import java.time.Instant;

/**
 * Serializes fields representing time with TAK Server's {@code COT_DATE_FORMAT_PAD_MILLIS} format.
 *
 * @since 5.0
 */
public class CotDateFormattingSerializer extends JsonSerializer<Instant>
{
    @Override
    public void serialize(Instant value, JsonGenerator gen, SerializerProvider serializers) throws IOException
    {
        gen.writeString(CotFormatting.toCotTime(value.toEpochMilli()));
    }
}
