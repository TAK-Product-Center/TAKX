package gov.tak.api.comms.takserver.mission.call;

import gov.tak.api.annotation.DeprecatedApi;
import gov.tak.api.comms.takserver.IDataSyncManager;
import gov.tak.api.comms.takserver.ITakServerConnection;
import gov.tak.api.comms.takserver.mission.call.mission.CreateMissionSubscription;
import gov.takx.api.annotation.NoPluginImplementations;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import java.util.Map;

/**
 * Outlines the capability of a configurable call to a REST endpoint.
 *
 * @param <T> Endpoint return value type.
 *
 * @since 3.5
 */
@NoPluginImplementations
public interface IConfigurableEndpointCaller<T> extends IEndpointCaller<T>
{
    /**
     * The key name for the header required for interacting with password-protected content.
     *
     * @since 5.1
     */
    String HEADER_AUTHORIZATION_KEY = "MissionAuthorization";

    /**
     * @return Map of known query parameters, their default values, and if they are required.
     */
    @Nonnull
    Map<String, ParameterInfo> getParameters();

    /**
     * Sets the {@code MissionAuthorization} header to the given token.
     * Interacting with password protected content requires this header be set.
     *
     * @param token Authorization token.
     * @param <S> Self type.
     *
     * @return Self, as to follow the builder pattern.
     * @see CreateMissionSubscription#get() For fetching a token.
     * @see IDataSyncManager#getMissionSubscriptionToken(ITakServerConnection, String) For retrieving an existing token,
     * @see #withAuthorizationTokenIfAvailable(IDataSyncManager, ITakServerConnection, String) For automatically adding the token if a token is tracked by the data-sync manager.
     * @since 5.1
     */
    default <S extends IConfigurableEndpointCaller<T>> S setAuthorizationToken(@Nullable String token)
    {
        return token == null ? unsetHeader(HEADER_AUTHORIZATION_KEY) : setHeader(HEADER_AUTHORIZATION_KEY, "Bearer " + token);
    }

    /**
     * Applies a stored authorization token to this request if one is associated with the given mission from a TAK Server.
     *
     * @param dataSyncManager Data sync manager to pull mission data from.
     * @param connection Connection defining the mission.
     * @param missionName Name of mission to get the authorization token of.
     *
     * @return Self, as to follow the builder pattern.
     * @param <S> Self type.
     * @since 5.2
     */
    <S extends IConfigurableEndpointCaller<T>> S withAuthorizationTokenIfAvailable(@Nonnull IDataSyncManager dataSyncManager,
                                                                                   @Nonnull ITakServerConnection connection,
                                                                                   @Nonnull String missionName);

    /**
     * Set a header value.
     *
     * @param key Header name.
     * @param value Header value.
     *              Can be {@code null} to delegate to {@link #unsetParam(String)}.
     * @param <S> Self type.
     *
     * @return Self, as to follow the builder pattern.
     * @since 5.1
     * @deprecated
     * @see #setHeader(String, Object)
     */
    @Nonnull
    @Deprecated(since = "5.7", forRemoval = true)
    @DeprecatedApi(since = "5.7", forRemoval = true, removeAt = "5.10")
    default <S extends IConfigurableEndpointCaller<T>> S setHeader(@Nonnull String key, @Nullable String value)
    {
        return setHeader(key, (Object) value);
    }

    /**
     * Set a header value.
     *
     * @param key Header name.
     * @param value Header value.
     *              Can be {@code null} to delegate to {@link #unsetParam(String)}.
     * @param <S> Self type.
     *
     * @return Self, as to follow the builder pattern.
     * @since 5.7
     */
    <S extends IConfigurableEndpointCaller<T>> S setHeader(@Nonnull String key, @Nullable Object value);

    /**
     * Unset a header value.
     *
     * @param key Header name.
     * @param <S> Self type.
     *
     * @return Self, as to follow the builder pattern.
     * @since 5.1
     */
    @Nonnull
    <S extends IConfigurableEndpointCaller<T>> S unsetHeader(@Nonnull String key);

    /**
     * Set a query parameter's value.
     *
     * @param name Query parameter name.
     * @param value Query parameter value.
     *              Must be able to be represented as a string <i>(Any type that defines a {@link Object#toString()})</i>.
     *              Can be {@code null} to delegate to {@link #unsetParam(String)}.
     * @param <S> Self type.
     *
     * @return Self, as to follow the builder pattern.
     */
    <S extends IConfigurableEndpointCaller<T>> S setParam(@Nonnull String name, @Nullable Object value);

    /**
     * Unsets a query parameter's value.
     *
     * @param name Query parameter name.
     * @param <S> Self type.
     *
     * @return Self, as to follow the builder pattern.
     */
    <S extends IConfigurableEndpointCaller<T>> S unsetParam(@Nonnull String name);
}
