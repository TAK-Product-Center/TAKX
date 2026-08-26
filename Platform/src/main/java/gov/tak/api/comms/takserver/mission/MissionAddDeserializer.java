package gov.tak.api.comms.takserver.mission;

import com.fasterxml.jackson.core.JacksonException;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;
import gov.tak.api.comms.takserver.mission.data.MapLayer;
import gov.tak.api.comms.takserver.mission.data.MissionAdd;
import gov.tak.api.comms.takserver.mission.data.Resource;
import gov.tak.api.comms.takserver.mission.data.UidDetails;

import java.io.IOException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/**
 * Deserializer for {@link MissionAdd}, which can have several kinds of subtypes.
 *
 * @since 3.5
 */
public class MissionAddDeserializer extends StdDeserializer<MissionAdd<?>>
{
    /**
     * New deserializer.
     */
    public MissionAddDeserializer()
    {
        super(MissionAdd.class);
    }

    @Override
    public MissionAdd<?> deserialize(JsonParser jp, DeserializationContext context) throws IOException, JacksonException
    {
        JsonNode node = jp.getCodec().readTree(jp);
        JsonNode dataNode = node.get("data");
        JsonNode pathNode = node.get("path");
        JsonNode afterNode = node.get("after");
        JsonNode detailsNode = node.get("details");
        JsonNode timestampNode = node.get("timestamp");
        UidDetails details = detailsNode == null || detailsNode.isNull() ? null : context.readTreeAsValue(detailsNode, UidDetails.class);
        Instant timestamp = timestampNode.isTextual() ? Instant.parse(timestampNode.asText()) : Instant.ofEpochMilli(timestampNode.asLong());
        String creatorUid = node.get("creatorUid").asText();
        String path = pathNode == null || pathNode.isNull() ? null : pathNode.asText();
        String after = afterNode == null || afterNode.isNull() ? null : afterNode.asText();
        List<String> keywords;
        JsonNode keywordsNode = node.get("keywords");
        if (keywordsNode != null)
        {
            keywords = new ArrayList<>();
            keywordsNode.elements().forEachRemaining(n -> keywords.add(n.asText()));
        } else
        {
            keywords = null;
        }
        if (dataNode.isTextual())
        {
            // Used by most map items, where the data is the map item uid.
            String data = dataNode.asText();
            return new MissionAdd.TString(data, timestamp, creatorUid, path, after, keywords, details);
        } else
        {
            // Resources declare a 'hash' property and layers declare a 'defaultLayer' property.
            // We can use those to figure out what kind of data we have.
            if (dataNode.get("hash") != null)
            {
                Resource data = context.readTreeAsValue(dataNode, Resource.class);
                return new MissionAdd.TResource(data, timestamp, creatorUid, path, after, keywords, details);
            } else if (dataNode.get("defaultLayer") != null)
            {
                MapLayer data = context.readTreeAsValue(dataNode, MapLayer.class);
                return new MissionAdd.TMapLayer(data, timestamp, creatorUid, path, after, keywords, details);
            }
        }

        // Not seen any samples with different data types, and the internal TAK Server source does not
        // indicate any other data types are supported at the moment.
        throw new UnsupportedOperationException("MissionAdd<T> only supports: String, Resource, and MapLayer" +
                "\nFound unknown data: " + dataNode);
    }
}
