package gov.takx.dataservices.persist;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.PropertyAccessor;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;
import com.fasterxml.jackson.databind.module.SimpleModule;
import gov.tak.api.engine.map.coords.IGeoPoint;
import gov.tak.api.maps.coords.GeoPoint;
import gov.tak.api.util.AttributeSet;
import gov.takx.api.coordinatesystem.ILocation;
import gov.takx.api.coordinatesystem.RaptorLocation;
import gov.takx.api.shapes.IMutablePolygon;
import gov.takx.api.shapes.IMutableShape;
import gov.takx.dataservices.shapes.Polygon;
import gov.takx.platform.utils.json.AttributeSetDeserializer;
import gov.takx.platform.utils.json.AttributeSetSerializer;
import gov.takx.platform.utils.json.ColorDeserializer;
import gov.takx.platform.utils.json.ColorSerializer;
import gov.takx.platform.utils.json.GeoPointDeserializer;
import gov.takx.platform.utils.json.IGeoPointDeserializer;
import gov.takx.platform.utils.json.IGeoPointSerializer;
import gov.takx.platform.utils.json.LocationDeserializer;
import gov.takx.platform.utils.json.RaptorLocationSerializer;
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

    static
    {
        final SimpleModule module = new SimpleModule();
        module.addSerializer(RaptorLocation.class, new RaptorLocationSerializer());
        module.addDeserializer(ILocation.class, new LocationDeserializer());

        module.addSerializer(GeoPoint.class, new IGeoPointSerializer());
        module.addDeserializer(GeoPoint.class, new GeoPointDeserializer());
        module.addSerializer(IGeoPoint.class, new IGeoPointSerializer());
        module.addDeserializer(IGeoPoint.class, new IGeoPointDeserializer());

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
}
