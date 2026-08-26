package gov.tak.api.comms.takserver.mission.data;

import java.time.Instant;

/**
 * @param uid Unique subscription identifier.
 * @param token The {@link MissionInvitation#token() invitation token} the user used to subscribe to the mission.
 * @param clientUid The {@link ClientEndpoint#uid()} of the user the subscription is for.
 * @param username The {@link ClientEndpoint#username()} of the user the subscription is for.
 * @param createTime The timestamp the subscription was created at.
 * @param role The role of the user in the mission.
 *
 * @since 3.5
 */
public record MissionSubscription(String uid,
        String token,
        String clientUid,
        String username,
        Instant createTime,
        MissionRole role)
{
}
