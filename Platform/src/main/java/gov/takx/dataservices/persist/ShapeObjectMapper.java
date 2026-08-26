package gov.takx.dataservices.persist;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.PropertyAccessor;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;
import gov.tak.api.maps.coords.GeoPoint;
import gov.tak.api.util.AttributeSet;
import gov.takx.api.coordinatesystem.ILocation;
import gov.takx.api.coordinatesystem.RaptorLocation;
import gov.takx.api.shapes.IMutablePolygon;
import gov.takx.api.shapes.IMutableShape;
import gov.takx.commons.utils.ParseUtils;
import gov.takx.dataservices.shapes.Polygon;
import jakarta.annotation.Nonnull;

import java.awt.Color;
import java.io.IOException;

/**
 * Support methods for serializing and deserializing shape model instances in JSON.
 *
 * @since 3.3
 */
public class ShapeObjectMapper
{
    private static final ObjectMapper MAPPER = new ObjectMapper();

    private static final String ENCODED_POINT = "encodedPoint"; // Attribute name for encoded GeoPoint value
    private static final String ENCODED_LOCATION = "encodedLocation"; // Attribute name for encoded RaptorLocation value
    private static final String COLOR_RGB = "colorRgb"; // Attribute name for encoded Color value

    static
    {
        final SimpleModule module = new SimpleModule();
        module.addSerializer(RaptorLocation.class, new RaptorLocationSerializer());
        module.addDeserializer(ILocation.class, new LocationDeserializer());

        module.addSerializer(GeoPoint.class, new GeoPointSerializer());
        module.addDeserializer(GeoPoint.class, new GeoPointDeserializer());

        module.addSerializer(AttributeSet.class, new AttributeSetSerializer());
        module.addDeserializer(AttributeSet.class, new AttributeSetDeserializer());

        module.addDeserializer(IMutablePolygon.class, new PolygonDeserializer());

        module.addSerializer(Color.class, new ColorSerializer());
        module.addDeserializer(Color.class, new ColorDeserializer());

        MAPPER.registerModule(module);

        // Do property discovery by field, not by accessor methods
        MAPPER.setVisibility(PropertyAccessor.ALL, JsonAutoDetect.Visibility.NONE);
        MAPPER.setVisibility(PropertyAccessor.FIELD, JsonAutoDetect.Visibility.ANY);
    }

    /**
     * Serialize the given shape model to JSON.
     */
    public static String shapeToJson(@Nonnull IMutableShape shape)
    {
        try
        {
            return MAPPER.writeValueAsString(shape);
        } catch (JsonProcessingException e)
        {
            throw new RuntimeException("Failed to create JSON for " + shape, e);
        }
    }

    /**
     * Deserialize the given JSON into a shape model instance.
     *
     * @param shapeClass Class of shape to construct
     * @param json JSON to deserialize
     * @return new model instance
     * @param <T> Inferred model type
     */
    public static <T extends IMutableShape> T jsonToShape(Class<?> shapeClass, String json)
    {
        try
        {
            final T shape = MAPPER.readerFor(shapeClass).readValue(json);
            shape.onDeserialized();// Give it a chance to do any post-deserialization cleanup/migration
            return shape;
        } catch (JsonProcessingException e)
        {
            throw new RuntimeException("Failed to create " + shapeClass.getSimpleName() + " from " + json, e);
        }
    }

    private ShapeObjectMapper()
    {
    }

    /**
     * Serializer to handle {@link RaptorLocation} instances. Encoded as a compact "lat,lon,elev" string form.
     */
    private static final class RaptorLocationSerializer extends StdSerializer<RaptorLocation>
    {
        RaptorLocationSerializer()
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

    /**
     * Deserializer to handle {@link ILocation}, producing {@link RaptorLocation} or {@link GeoPoint} instances.
     *
     * @see RaptorLocationSerializer
     * @see GeoPointSerializer
     */
    private static final class LocationDeserializer extends StdDeserializer<ILocation>
    {
        LocationDeserializer()
        {
            super((Class<?>) null);
        }

        @Override
        public ILocation deserialize(JsonParser p, DeserializationContext context) throws IOException
        {
            final JsonNode node = p.readValueAsTree();

            // Determine which concrete type to produce based on the presence of the encodedXxx attribute

            final JsonNode encodedPoint = node.get(ENCODED_POINT);
            if (encodedPoint != null)
            {
                return GeoPoint.parseGeoPoint(encodedPoint.asText());
            } else
            {
                final String encodedLocation = node.get(ENCODED_LOCATION).asText();
                final String[] parts = encodedLocation.split(",");

                final double latitude = ParseUtils.parseDouble(parts[0], Double.NaN);
                final double longitude = ParseUtils.parseDouble(parts[1], Double.NaN);
                final double elevation = ParseUtils.parseDouble(parts[2], Double.NaN);

                return new RaptorLocation(latitude, longitude, !Double.isNaN(elevation), elevation);
            }
        }
    }

    /**
     * Serializer to handle {@link GeoPoint} instances. Encoded via {@link GeoPoint#toStringRepresentation()}.
     */
    private static final class GeoPointSerializer extends StdSerializer<GeoPoint>
    {
        GeoPointSerializer()
        {
            super((Class<GeoPoint>) null);
        }

        @Override
        public void serialize(GeoPoint geoPoint, JsonGenerator gen, SerializerProvider provider) throws IOException
        {
            gen.writeStartObject();
            gen.writeStringField(ENCODED_POINT, geoPoint.toStringRepresentation());
            gen.writeEndObject();
        }
    }

    /**
     * Deserializer to handle {@link GeoPoint} instances. Parsed via {@link GeoPoint#parseGeoPoint(String)}.
     */
    private static final class GeoPointDeserializer extends StdDeserializer<GeoPoint>
    {
        GeoPointDeserializer()
        {
            super((Class<?>) null);
        }

        @Override
        public GeoPoint deserialize(JsonParser p, DeserializationContext context) throws IOException
        {
            final JsonNode node = p.readValueAsTree();
            return GeoPoint.parseGeoPoint(node.get(ENCODED_POINT).asText());
        }
    }

    /**
     * Serializer to handle {@link Color} instances. Encoded as an integer.
     */
    private static final class ColorSerializer extends StdSerializer<Color>
    {
        ColorSerializer()
        {
            super((Class<Color>) null);
        }

        @Override
        public void serialize(Color color, JsonGenerator gen, SerializerProvider provider) throws IOException
        {
            gen.writeStartObject();
            gen.writeNumberField(COLOR_RGB, color.getRGB());
            gen.writeEndObject();
        }
    }

    /**
     * Deserializer to handle {@link Color} instances. Parsed via {@link Color#Color(int, boolean)}.
     */
    private static final class ColorDeserializer extends StdDeserializer<Color>
    {
        ColorDeserializer()
        {
            super((Class<?>) null);
        }

        @Override
        public Color deserialize(JsonParser p, DeserializationContext context) throws IOException
        {
            final JsonNode node = p.readValueAsTree();
            return new Color(node.get(COLOR_RGB).asInt());
        }
    }

    /**
     * Deserializer to handle {@link IMutablePolygon} instances.  Necessary because the "holes" within a Polygon
     * are stored as a list of {@link IMutablePolygon}, and we need to force a concrete type for deserialization.
     */
    private static final class PolygonDeserializer extends StdDeserializer<IMutablePolygon>
    {
        PolygonDeserializer()
        {
            super((Class<?>) null);
        }

        @Override
        public IMutablePolygon deserialize(JsonParser p, DeserializationContext context) throws IOException
        {
            return MAPPER.readerFor(Polygon.class).readValue(p);
        }
    }

    /**
     * Serializer to handle {@link AttributeSet} instances.  Encoded via {@link AttributeSet#toJson}
     */
    private static final class AttributeSetSerializer extends StdSerializer<AttributeSet>
    {
        AttributeSetSerializer()
        {
            super((Class<AttributeSet>) null);
        }

        @Override
        public void serialize(AttributeSet attributeSet, JsonGenerator gen, SerializerProvider provider) throws IOException
        {
            gen.writeRawValue(attributeSet.toJson()); // Let AttributeSet produce the JSON
        }
    }

    /**
     * Deserializer to handle {@link AttributeSet} instances. Parsed via {@link AttributeSet} constructor.
     */
    private static final class AttributeSetDeserializer extends StdDeserializer<AttributeSet>
    {
        AttributeSetDeserializer()
        {
            super((Class<?>) null);
        }

        @Override
        public AttributeSet deserialize(JsonParser p, DeserializationContext context) throws IOException
        {
            return new AttributeSet(p.readValueAsTree().toString()); // Let AttributeSet parse it
        }
    }
}
