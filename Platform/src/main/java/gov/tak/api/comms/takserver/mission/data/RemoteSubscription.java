package gov.tak.api.comms.takserver.mission.data;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

// TODO TAKX-1832: Document what each remaining property is used for.
//  - Though, this has been described as an "internal" class, even though it is exposed in their public API

/**
 * Messaging client subscription information, where each subscription refers to a specific TAK client denoted by their client UID.
 *
 * @param clientUid The {@link ClientEndpoint#uid()} of the user the subscription is for.
 * @param callsign The {@link ClientEndpoint#callsign()} of the user.
 * @param team The team name of the user.
 * @param role
 * @param takv The version of TAK client of the user.
 * @param filterGroups
 * @param notes
 *
 * @since 3.5
 */
public record RemoteSubscription(@JsonProperty("uid") String clientUid,
        String callsign,
        String team,
        String role,
        String takv,
        List<String> filterGroups,
        String notes)
{
}
