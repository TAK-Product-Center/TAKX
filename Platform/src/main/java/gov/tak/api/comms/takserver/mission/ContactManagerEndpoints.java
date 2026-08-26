package gov.tak.api.comms.takserver.mission;

import gov.tak.api.comms.takserver.ITakServerConnection;
import gov.tak.api.comms.takserver.mission.call.contacts.GetClientEndpoints;
import gov.tak.api.comms.takserver.mission.feign.FeignContactManagerApi;
import jakarta.annotation.Nonnull;

/**
 * Convenience utility for referencing and calling API endpoints through builder patterns.
 * <p>
 * Wraps {@link FeignContactManagerApi}.
 *
 * @since 3.5
 */
public class ContactManagerEndpoints
{
    private final FeignContactManagerApi contactsApi;

    /**
     * @param apiProvider Provider of client instances.
     * @param connection TAK server connection to use to initialize an API client with.
     */
    public ContactManagerEndpoints(@Nonnull ApiProvider apiProvider,
                                   @Nonnull ITakServerConnection connection)
    {
        contactsApi = apiProvider.getContactManagerApi(connection);
    }

    /**
     * @return Endpoint to get connected clients.
     */
    @Nonnull
    public GetClientEndpoints getClientEndpoints()
    {
        return new GetClientEndpoints(contactsApi);
    }
}
