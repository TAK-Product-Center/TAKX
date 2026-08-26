package gov.tak.api.comms.takserver.mission;

import gov.tak.api.comms.takserver.ITakServerConnection;
import gov.tak.api.comms.takserver.mission.call.cot.GetCotItem;
import gov.tak.api.comms.takserver.mission.feign.FeignCotApi;
import jakarta.annotation.Nonnull;

/**
 * Convenience utility for referencing and calling API endpoints through builder patterns.
 * <p>
 * Wraps {@link FeignCotApi}.
 *
 * @since 5.0
 */
public class CotEndpoints
{
    private final FeignCotApi cotApi;

    /**
     * @param apiProvider Provider of client instances.
     * @param connection TAK server connection to use to initialize an API client with.
     */
    public CotEndpoints(@Nonnull ApiProvider apiProvider,
                        @Nonnull ITakServerConnection connection)
    {
        cotApi = apiProvider.getCotApi(connection);
    }

    /**
     * @param uid UID of the map item to get the CoT XML representation of.
     *
     * @return Endpoint to get the CoT XML of a map item, denoted by a UID.
     */
    @Nonnull
    public GetCotItem getCotItem(@Nonnull String uid)
    {
        return new GetCotItem(cotApi, uid);
    }
}
