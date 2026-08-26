package gov.tak.api.comms.takserver.mission;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;
import gov.tak.api.comms.takserver.mission.data.Mission;
import gov.tak.api.comms.takserver.mission.data.MissionRole;
import gov.tak.api.comms.takserver.mission.data.RoleType;

import java.io.IOException;
import java.util.Collections;

/**
 * Deserializer for {@link MissionRole}.
 *
 * @since 5.0
 */
public abstract class AMissionRoleDeserializer extends StdDeserializer<MissionRole>
{
    private AMissionRoleDeserializer()
    {
        super(MissionRole.class);
    }

    /**
     * Sub-type for {@link Mission#defaultRole()}.
     */
    public static class Default extends AMissionRoleDeserializer
    {
        private static final MissionRole DEFAULT_VALUE = new MissionRole(RoleType.MISSION_SUBSCRIBER, Collections.emptySet());

        @Override
        public MissionRole deserialize(JsonParser p, DeserializationContext ctxt) throws IOException
        {
            return p.readValueAs(MissionRole.class);
        }

        @Override
        public MissionRole getNullValue(DeserializationContext ctxt)
        {
            return DEFAULT_VALUE;
        }
    }

    /**
     * Sub-type for {@link Mission#ownerRole()}.
     */
    public static class Owner extends AMissionRoleDeserializer
    {
        private static final MissionRole DEFAULT_VALUE = new MissionRole(RoleType.MISSION_OWNER, Collections.emptySet());

        @Override
        public MissionRole deserialize(JsonParser p, DeserializationContext ctxt) throws IOException
        {
            return p.readValueAs(MissionRole.class);
        }

        @Override
        public MissionRole getNullValue(DeserializationContext ctxt)
        {
            return DEFAULT_VALUE;
        }
    }
}
