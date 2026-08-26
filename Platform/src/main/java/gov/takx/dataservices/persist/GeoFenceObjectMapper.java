package gov.takx.dataservices.persist;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.PropertyAccessor;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import gov.takx.api.geofence.GeoFence;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.invoke.MethodHandles;

/**
 * Support methods for serializing and deserializing {@link GeoFence} instances in JSON.
 *
 * @since 5.3
 */
public class GeoFenceObjectMapper
{
    private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    private static final ObjectMapper MAPPER = new ObjectMapper();

    static
    {
        // Do property discovery by field, not by accessor methods
        MAPPER.setVisibility(PropertyAccessor.ALL, JsonAutoDetect.Visibility.NONE);
        MAPPER.setVisibility(PropertyAccessor.FIELD, JsonAutoDetect.Visibility.ANY);
    }

    /**
     * Serialize the given geoFence to JSON.
     */
    public static String toJson(GeoFence geoFence)
    {
        try
        {
            return MAPPER.writeValueAsString(geoFence);
        } catch (JsonProcessingException e)
        {
            logger.error("Failed to create JSON for {}", geoFence, e);
            return null;
        }
    }

    /**
     * Deserialize the given JSON into a {@link GeoFence} instance.
     *
     * @param json JSON to deserialize
     * @return new GeoFence instance
     */
    public static GeoFence fromJson(String json)
    {
        try
        {
            return MAPPER.readValue(json, GeoFence.class);
        } catch (JsonProcessingException e)
        {
            logger.error("Failed to create GeoFence from {}", json, e);
            return null;
        }
    }
}
