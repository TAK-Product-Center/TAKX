package gov.takx.platform.utils.json;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;
import gov.tak.api.annotation.IncubatingApi;
import gov.takx.api.coordinatesystem.RaptorLocation;

import java.io.IOException;

/**
 * Serializer to handle {@link RaptorLocation} instances. Encoded as a compact "lat,lon,elev" string form.
 * @since 5.8
 */
@IncubatingApi(since = "5.8")
public class RaptorLocationSerializer extends StdSerializer<RaptorLocation>
{
    protected static final String ENCODED_LOCATION = "encodedLocation";

    public RaptorLocationSerializer()
    {
        super((Class<RaptorLocation>) null);
    }

    @Override
    public void serialize(RaptorLocation location, JsonGenerator gen, SerializerProvider provider) throws IOException
    {
        // Produce a compact form using a comma-delimited string
        final String encoded = String.valueOf(location.getLatitude()) +
                ',' + location.getLongitude() +
                ',' + (location.hasElevation() ? location.getElevation() : Double.NaN);

        gen.writeStartObject();
        gen.writeStringField(ENCODED_LOCATION, encoded);
        gen.writeEndObject();
    }
}