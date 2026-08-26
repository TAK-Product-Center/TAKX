package gov.tak.api.comms.takserver.mission.feign;

import feign.HeaderMap;
import feign.QueryMap;
import feign.RequestLine;
import gov.tak.api.comms.takserver.mission.ApiProvider;
import gov.tak.api.comms.takserver.mission.data.Session;
import gov.takx.api.annotation.NoPluginImplementations;
import jakarta.annotation.Nonnull;

import java.util.Collections;
import java.util.Map;

/**
 * REST interface modeling the OAuth API from TAK Server.
 * <p>
 * For internal use in {@link ApiProvider}.
 * @since 3.5
 */
@NoPluginImplementations
public interface FeignSessionApi
{
    /**
     * Convenience call for {@link #login(Map, Map)} with a username/password combination.
     *
     * @param username Account username.
     * @param password Account password.
     *
     * @return Authorized session.
     */
    @Nonnull
    default Session login(@Nonnull String username, @Nonnull String password)
    {
        Map<String, Object> queryMap = Map.of("grant_type", "password",
                "username", username,
                "password", password);
        return login(Collections.emptyMap(), queryMap);
    }

    /**
     * @param headers Additional request headers.
     * @param queryMap Map of query names to values.
     *
     * @return Authorized session.
     */
    @RequestLine("POST /oauth/token")
    Session login(@Nonnull @HeaderMap Map<String, Object> headers,
                  @Nonnull @QueryMap Map<String, Object> queryMap);
}
