package gov.tak.api.comms.takserver.mission.data;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.annotation.Nonnull;

/**
 * Kind of data outlined by {@link MissionInvitation#invitee()}.
 *
 * @see MissionInvitation
 * @since 3.5
 */
public enum InvitationType
{
    /**
     * A {@link ClientEndpoint#uid()}.
     */
    @JsonProperty("clientUid")
    CLIENT_UID("clientUid"),
    /**
     * A callsign, associated with {@link ClientEndpoint#uid()},
     * which can be resolved by {@link gov.tak.api.comms.commo.IContactManager#findContactByUid(String)}.
     */
    @JsonProperty("callsign")
    CALLSIGN("callsign"),
    /**
     * A {@link ClientEndpoint#username()}.
     */
    @JsonProperty("userName")
    USERNAME("userName"),
    /**
     * Members of a group.
     * <p>
     * <b>Note:</b> There is currently no direct way to look up users of a group.
     */
    @JsonProperty("group")
    GROUP("group"),
    /**
     * Members of a team.
     * <p>
     * <b>Note:</b> This is not currently implemented on the server's side.
     */
    @JsonProperty("team")
    TEAM("team");

    private final String apiName;

    /**
     * @param apiName Name used by TAK Server.
     */
    InvitationType(@Nonnull String apiName)
    {
        this.apiName = apiName;
    }

    /**
     * TAK Server's APIs follow a camel-case convention.
     * Any usage of this type should use this value over {@link InvitationType#name()}.
     *
     * @return The text representation to use for TAK Server API usage.
     */
    @Nonnull
    public String getApiName()
    {
        return apiName;
    }

    @Override
    public String toString()
    {
        return apiName;
    }
}
