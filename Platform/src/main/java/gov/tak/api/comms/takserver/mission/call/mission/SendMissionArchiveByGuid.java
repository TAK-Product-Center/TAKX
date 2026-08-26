package gov.tak.api.comms.takserver.mission.call.mission;

import gov.tak.api.comms.takserver.mission.call.AbstractEndpointCaller;
import gov.tak.api.comms.takserver.mission.data.ClientEndpoint;
import gov.tak.api.comms.takserver.mission.data.Mission;
import gov.tak.api.comms.takserver.mission.feign.FeignMissionApi;
import jakarta.annotation.Nonnull;

import java.util.Collection;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.Map;
import java.util.Set;

/**
 * Endpoint caller to send an archived copy of a {@link Mission} to a number of recipients.
 *
 * @since 5.7
 */
public class SendMissionArchiveByGuid extends AbstractEndpointCaller<Mission>
{
    private static final char[] CONTACTS_CHARS = "contacts".toCharArray();
    private final Set<String> recipientUids = new HashSet<>();

    /**
     * @param api API reference.
     * @param missionGuid Guid of the mission to update.
     */
    public SendMissionArchiveByGuid(@Nonnull FeignMissionApi api, @Nonnull String missionGuid)
    {
        super(unpack((headers, params) -> api.sendMissionArchiveByGuid(headers, params, missionGuid)));
    }

    /**
     * Adds an entry to CoT message generated on the server, adding the given user as a contact for the message.
     *
     * @param uid {@link ClientEndpoint#uid()} of user to send the archive to.
     *
     * @return Self.
     */
    @Nonnull
    public SendMissionArchiveByGuid addRecipientUid(@Nonnull String uid)
    {
        recipientUids.add(uid);
        return this;
    }

    /**
     * Adds entries to CoT message generated on the server, adding the given users as contacts for the message.
     *
     * @param uids Multiple {@link ClientEndpoint#uid()} values of users to send the archive to.
     *
     * @return Self.
     */
    @Nonnull
    public SendMissionArchiveByGuid addRecipientUids(@Nonnull Collection<String> uids)
    {
        recipientUids.addAll(uids);
        return this;
    }

    @Nonnull
    @Override
    protected Map<String, Object> buildParameterMap()
    {
        // This endpoint is an edge-case. We need to support multiple of the same key in order to do:
        // POST /send?contacts=uid1&contacts=uid2
        //
        // To facilitate this, we use an identity map, with multiple equal keys with different identities.
        Map<String, Object> paramMap = new IdentityHashMap<>();
        for (String recipientUid : recipientUids)
        {
            // Each key must have a unique identity, so we make duplicate 'contacts' string instances.
            // Since they are not string constants, they will not be interned and thus have unique identities.
            paramMap.put(new String(CONTACTS_CHARS), recipientUid);
        }
        return paramMap;
    }
}
