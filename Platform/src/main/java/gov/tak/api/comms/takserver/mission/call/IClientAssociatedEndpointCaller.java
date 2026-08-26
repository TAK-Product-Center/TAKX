package gov.tak.api.comms.takserver.mission.call;

import jakarta.annotation.Nonnull;

import java.util.UUID;

/**
 * Serves as a marker type that one of its UID's pertains to our client's ID.
 *
 * @param <T> Endpoint return value type.
 *
 * @since 3.5
 */
public interface IClientAssociatedEndpointCaller<T> extends IConfigurableEndpointCaller<T>
{
    /**
     * Intercepts endpoint instances that can take the {@code clientUid} value and autofills it so users don't have to.
     *
     * @param endpoint Some endpoint.
     * @param clientUid Unique client identifier to assign.
     * @param <T> Endpoint return type. For instance, T can be {@code Mission}.
     * @param <E> Endpoint implementation type. For instance E can be {@code IEndpointCaller<Mission>}.
     *
     * @return The same endpoint, with some common values configured that can be automatically managed.
     */
    @Nonnull
    static <T, E extends IEndpointCaller<T>> E interceptClientUid(@Nonnull E endpoint, @Nonnull UUID clientUid)
    {
        if (endpoint instanceof IClientAssociatedEndpointCaller<?> configurableEndpoint)
        {
            configurableEndpoint.setParam(configurableEndpoint.clientUidKey(), clientUid);
        }
        return endpoint;
    }

    /**
     * Declares which query parameter of an endpoint represents our client uid.
     * <p>
     * Generally, endpoints that manipulate data want to know who is responsible for
     * such actions, so the value is passed by clients like ourselves along with the
     * rest of the request.
     * <p>
     * Some endpoints declare this value as a required query parameter, while some
     * only mark it as being optional.
     *
     * @return Query parameter name that should be assigned to our client's ID.
     */
    @Nonnull
    String clientUidKey();
}
