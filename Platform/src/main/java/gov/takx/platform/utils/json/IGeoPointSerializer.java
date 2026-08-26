package gov.takx.platform.utils.json;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;
import gov.tak.api.annotation.IncubatingApi;
import gov.tak.api.engine.map.coords.IGeoPoint;
import gov.tak.api.maps.coords.GeoPoint;

import java.io.IOException;

/**
 * Serializer to handle {@link IGeoPoint} and {@link GeoPoint} instances. Encoded as a compact string in the form:
 * <p/>
 * {@code "latitude,longitude,altitude,altitudeReference,ce90,le90"}
 * @since 5.8
 */
@IncubatingApi(since = "5.8")
public class IGeoPointSerializer extends StdSerializer<IGeoPoint>
{
    protected static final String ENCODED_POINT = "encodedPoint";

    public IGeoPointSerializer()
    {
        super((Class<IGeoPoint>) null);
    }

    @Override
    public void serialize(IGeoPoint point, JsonGenerator generator, SerializerProvider provider) throws IOException
    {
        generator.writeStartObject();
        generator.writeStringField(ENCODED_POINT, new GeoPoint(point).toStringRepresentation());
        generator.writeEndObject();
    }
}
