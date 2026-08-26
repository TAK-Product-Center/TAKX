package gov.tak.api.comms.takserver.mission.data;

import java.time.Instant;

/**
 * @param missionName The name of the mission the invite is for.
 * @param invitee The identifier of who is invited. Content depends on the {@link InvitationType} provided.
 * @param type The type of invite, which determines what should be filled in the {@code invitee} field.
 * @param creatorUid The {@link ClientEndpoint#uid()} of the user responsible for creating the invite.
 * @param createTime The timestamp of the invite creation.
 * @param token Generated token unique to each invite.
 * @param role The role granted to users accepting the invitation.
 *
 * @since 3.5
 */
public record MissionInvitation(String missionName,
        String invitee,
        InvitationType type,
        String creatorUid,
        Instant createTime,
        String token,
        MissionRole role)
{
}
