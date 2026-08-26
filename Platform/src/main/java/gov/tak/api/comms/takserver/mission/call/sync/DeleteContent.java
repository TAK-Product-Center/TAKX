package gov.tak.api.comms.takserver.mission.call.sync;

import gov.tak.api.comms.takserver.mission.call.AbstractEndpointCaller;
import gov.tak.api.comms.takserver.mission.feign.FeignSyncApi;
import jakarta.annotation.Nonnull;

/**
 * Endpoint caller to delete a file from enterprise sync by its SHA26 hash.
 *
 * @since 5.7
 */
public class DeleteContent extends AbstractEndpointCaller<Void>
{
    /**
     * @param api API reference.
     * @param hash SHA256 hash of the file.
     */
    public DeleteContent(@Nonnull FeignSyncApi api, @Nonnull String hash)
    {
        super(devoid((headers, params) -> api.delete(headers, params, hash)));
    }
}
