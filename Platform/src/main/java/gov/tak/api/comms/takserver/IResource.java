package gov.tak.api.comms.takserver;

import gov.tak.api.comms.takserver.mission.data.AssociatedResource;
import gov.tak.api.comms.takserver.mission.data.ClientEndpoint;
import jakarta.annotation.Nonnull;

import java.time.Instant;
import java.util.List;

/**
 * Outline of a TAK Server resource.
 *
 * @see gov.tak.api.comms.takserver.mission.data.Resource
 * @see AssociatedResource
 * @since 3.5
 */
public interface IResource
{
    /**
     * @return Unique identifier of the file.
     */
    String uid();

    /**
     * @return SHA256 hash of the file.
     */
    String hash();

    /**
     * @return Name of the resource, as known to the server.
     * Not to be confused with the file name, even though they typically match.
     */
    String name();

    /**
     * @return Name of the actual file.
     */
    String fileName();

    /**
     * @return Name of the file, or {@code "Unknown"} if not known.
     * Never {@code null}, even if the resource model does not specify a file name.
     */
    @Nonnull
    default String safeFileName()
    {
        String name = fileName();
        if (name == null)
        {
            name = name();
            if (name == null) return "Unknown";
        }
        return name;
    }

    /**
     * @return Time the file was uploaded.
     */
    Instant submissionTime();

    /**
     * @return The {@link ClientEndpoint#username() name of the contact} responsible for uploading the resource.
     */
    String submitter();

    /**
     * @return The {@link ClientEndpoint#uid() UID of the contact} responsible for uploading the resource.
     */
    String creatorUid();

    /**
     * @return Keywords associated with the file, which can be used in
     * {@link gov.tak.api.comms.takserver.mission.call.sync.SearchResources enterprise sync look-ups}.
     * Some keywords hold special value and change how files are treated on the server's end, such as {@code missionpackage}.
     */
    List<String> keywords();

    /**
     * @return File type, for instance {@code image/jpeg} and {@code application/octet-stream}.
     */
    String mimeType();

    /**
     * @return Size in bytes of the resource.
     */
    long size();

    /**
     * @return Timestamp in epoch millis for the resource to delete itself.
     */
    long expiration();
}
