package gov.takx.platform.utils.json;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;
import gov.tak.api.annotation.IncubatingApi;
import gov.tak.api.maps.coords.GeoPoint;
import gov.takx.api.coordinatesystem.ILocation;
import gov.takx.api.coordinatesystem.RaptorLocation;
import gov.takx.commons.utils.ParseUtils;

import java.io.IOException;

/**
 * Deserializer to handle {@link ILocation}, producing {@link RaptorLocation} or {@link GeoPoint} instances.
 *
 * @see RaptorLocationSerializer
 * @see IGeoPointSerializer
 * @since 5.8
 */
@IncubatingApi(since = "5.8")
public class LocationDeserializer extends StdDeserializer<ILocation>
{
    public LocationDeserializer()
    {
        super((Class<?>) null);
    }

    @Override
    public ILocation deserialize(JsonParser p, DeserializationContext context) throws IOException
    {
        final JsonNode node = p.readValueAsTree();

        // Determine which concrete type to produce based on the presence of the encodedXxx attribute

        final JsonNode encodedPoint = node.get(IGeoPointSerializer.ENCODED_POINT);
        if (encodedPoint != null)
        {
            return GeoPoint.parseGeoPoint(encodedPoint.asText());
        } else
        {
            final String encodedLocation = node.get(RaptorLocationSerializer.ENCODED_LOCATION).asText();
            final String[] parts = encodedLocation.split(",");

            final double latitude = ParseUtils.parseDouble(parts[0], Double.NaN);
            final double longitude = ParseUtils.parseDouble(parts[1], Double.NaN);
            final double elevation = ParseUtils.parseDouble(parts[2], Double.NaN);

            return new RaptorLocation(latitude, longitude, !Double.isNaN(elevation), elevation);
        }
    }
}
