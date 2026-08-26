package gov.tak.api.comms.takserver.mission.data;

import java.time.Instant;

/**
 * Outline of a connected TAK client, which can be thought of as a {@link gov.tak.api.contact.IContact}.
 *
 * @param callsign The display name of the client.
 * @param uid The unique ID of the client.
 * @param username The login name of the client.
 * @param lastEventTime The last time the client changed status.
 * @param lastStatus The last known client status.
 *
 * @since 3.5
 */
public record ClientEndpoint(String callsign,
        String uid,
        String username,
        Instant lastEventTime,
        String lastStatus)
{
}