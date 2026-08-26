package gov.tak.api.comms.takserver.mission.data;

import com.fasterxml.jackson.annotation.JsonProperty;
import gov.tak.api.comms.takserver.IResource;

import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * Models the response from {@link gov.tak.api.comms.takserver.mission.feign.FeignSyncApi#upload(Map, Map, byte[])}.
 *
 * @param uid UID of file uploaded.
 * @param submissionTime Server timestamp of upload.
 * @param mimeType File mime type.
 * @param submitter Username of uploader.
 * @param hash SHA256 hash of file uploaded.
 *
 * @since 3.5
 */
public record UploadedResourceResponse(@JsonProperty("UID") String uid,
        @JsonProperty("SubmissionDateTime") Instant submissionTime,
        @JsonProperty("MIMEType") String mimeType,
        @JsonProperty("SubmissionUser") String submitter,
        @JsonProperty("Hash") String hash) implements IResource
{
    @Override
    public String name()
    {
        return "unknown";
    }

    @Override
    public String fileName()
    {
        return "unknown";
    }

    @Override
    public String creatorUid()
    {
        return "unknown";
    }

    @Override
    public List<String> keywords()
    {
        return Collections.emptyList();
    }

    @Override
    public long size()
    {
        return -1;
    }

    @Override
    public long expiration()
    {
        return -1;
    }
}
