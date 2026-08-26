package gov.tak.api.comms.takserver.mission.data;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import gov.tak.api.comms.takserver.mission.CotDateFormattingSerializer;

import java.time.Instant;
import java.util.Set;

/**
 * @param content Text content of the log entry.
 * @param creatorUid The {@link ClientEndpoint#uid()} of the user responsible for writing the entry.
 * @param entryUid UID of the associated CoT track.
 * @param dtg Time of log creation by the local client.
 * @param missionNames Missions associated with this log entry.
 * @param contentHashes Hashes of enterprise sync files to associate with this log entry.
 * @param keywords Optional keywords describing the log entry.
 *
 * @since 3.5
 */
public record LogEntry(String content,
        String creatorUid,
        String entryUid,
        @JsonSerialize(using = CotDateFormattingSerializer.class) Instant dtg,
        Set<String> missionNames,
        Set<String> contentHashes,
        Set<String> keywords)
{
}
