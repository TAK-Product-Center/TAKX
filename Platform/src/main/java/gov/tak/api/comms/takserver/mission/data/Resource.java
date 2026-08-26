package gov.tak.api.comms.takserver.mission.data;

import com.fasterxml.jackson.annotation.JsonProperty;
import gov.tak.api.comms.takserver.IResource;
import gov.tak.api.comms.takserver.mission.call.sync.SearchResources;

import java.time.Instant;
import java.util.List;

/**
 * @param uid Unique identifier of the file.
 * @param hash SHA256 hash of the file.
 * @param name Name of the resource, as known to the server.
 * Not to be confused with the file name, even though they typically match.
 * @param fileName Name of the actual file.
 * @param submissionTime Time the file was uploaded.
 * @param submitter The {@link ClientEndpoint#username() name of the contact} responsible for uploading the resource.
 * @param creatorUid The {@link ClientEndpoint#uid() UID of the contact} responsible for uploading the resource.
 * @param keywords Keywords associated with the file, which can be used in
 * {@link SearchResources enterprise sync look-ups}.
 * Some keywords hold special value and change how files are treated on the server's end, such as {@code missionpackage}.
 * @param mimeType File type, for instance {@code image/jpeg} and {@code application/octet-stream}.
 * @param size Size in bytes of the resource.
 * @param expiration Timestamp in epoch millis for the resource to delete itself.
 *
 * @since 3.5
 */
public record Resource(String uid,
        String hash,
        String name,
        @JsonProperty("filename") String fileName,
        Instant submissionTime,
        String submitter,
        String creatorUid,
        List<String> keywords,
        String mimeType,
        long size,
        long expiration) implements IResource
{
}