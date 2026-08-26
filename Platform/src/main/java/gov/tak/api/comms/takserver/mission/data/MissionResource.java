package gov.tak.api.comms.takserver.mission.data;

import java.time.Instant;

/**
 * Wraps a {@link Resource} with information on who added it and when to a {@link Mission}.
 *
 * @param data The actual resource.
 * @param timestamp Timestamp when the resource was added to {@link Mission#contents()}.
 * @param creatorUid The {@link ClientEndpoint#uid()} of the user who added the resource to he {@link Mission#contents()}.
 *
 * @since 3.5
 */
public record MissionResource(Resource data, Instant timestamp, String creatorUid)
{
}
