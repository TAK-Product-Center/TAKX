package gov.tak.api.comms.takserver.mission.call.mission;

import feign.Response;
import gov.tak.api.comms.takserver.mission.call.AbstractEndpointCaller;
import gov.tak.api.comms.takserver.mission.data.Mission;
import gov.tak.api.comms.takserver.mission.feign.FeignMissionApi;
import jakarta.annotation.Nonnull;

import java.io.IOException;
import java.io.InputStream;

/**
 * Endpoint caller to get an archived copy of a {@link Mission}. The {@code byte[]} yielded by this endpoint represents
 * a ZIP file with the given structure:
 * <ul>
 *     <li>{@code MANIFEST/manifest.xml} - Outlines the properties of the {@link Mission}</li>
 *     <li>{@code contents/...} - Contains copies of {@link Mission#contents() referenced files}</li>
 *     <li>{@code cot/...} - Contains XML contents of {@link Mission#uids() map items} associated with the {@link Mission}</li>
 * </ul>
 *
 * @since 3.5
 */
public class GetMissionArchive extends AbstractEndpointCaller<byte[]>
{
    /**
     * @param api API reference.
     * @param missionName Name of the mission to get.
     */
    public GetMissionArchive(@Nonnull FeignMissionApi api, @Nonnull String missionName)
    {
        super((headers, params) -> {
            Response response = api.getMissionArchive(headers, params, missionName);
            try
            {
                // Read body and check if it starts with 'PK' ZIP file header
                InputStream content = response.body().asInputStream();
                byte[] bytes = content.readAllBytes();
                if (bytes.length > 4 && bytes[0] == 'P' && bytes[1] == 'K')
                {
                    return bytes;
                }

                // Body did not match ZIP file pattern
                throw new IllegalStateException("Response did not contain ZIP file");
            } catch (IOException ex)
            {
                throw new IllegalStateException("Failed to read response body for mission archive", ex);
            }
        });
    }
}
