package gov.tak.api.comms.takserver.mission;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;
import gov.tak.api.comms.takserver.mission.data.MissionContentKeys;

import java.io.IOException;
import java.util.List;

/**
 * Serializes {@link gov.tak.api.comms.takserver.mission.data.MissionContentKeys} to only include one field, even
 * if both are specified.
 *
 * @since 5.3
 */
public class MissionContentKeysSerializer extends JsonSerializer<MissionContentKeys>
{
    @Override
    public void serialize(MissionContentKeys value, JsonGenerator gen, SerializerProvider serializers) throws IOException
    {
        List<String> hashes = value.hashes();
        List<String> uids = value.uids();

        gen.writeStartObject();
        if (hashes != null && !hashes.isEmpty())
        {
            gen.writeArrayFieldStart("hashes");
            for (String hash : hashes) gen.writeString(hash);
            gen.writeEndArray();
        } else if (uids != null && !uids.isEmpty())
        {
            gen.writeArrayFieldStart("uids");
            for (String uid : uids) gen.writeString(uid);
            gen.writeEndArray();
        } else
        {
            throw new IllegalStateException("Mission content must specify at least one value in 'hashes' or 'uids'");
        }

        gen.writeEndObject();
    }
}
