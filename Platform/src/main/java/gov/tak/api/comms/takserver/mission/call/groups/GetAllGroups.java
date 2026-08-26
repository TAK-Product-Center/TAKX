package gov.tak.api.comms.takserver.mission.call.groups;

import gov.tak.api.comms.takserver.mission.call.AbstractEndpointCaller;
import gov.tak.api.comms.takserver.mission.call.ParameterInfo;
import gov.tak.api.comms.takserver.mission.data.Group;
import gov.tak.api.comms.takserver.mission.data.GroupDirection;
import gov.tak.api.comms.takserver.mission.feign.FeignGroupsApi;
import jakarta.annotation.Nonnull;

import java.util.List;
import java.util.Map;

/**
 * Endpoint caller to query groups of a TAK Server.
 *
 * @since 5.0
 */
public class GetAllGroups extends AbstractEndpointCaller<List<Group>>
{
    private static final Map<String, ParameterInfo> PARAM_MAP = map(
            param("useCache", true),
            paramOptional("sendLatestSA", false));

    /**
     * @param api API reference.
     */
    public GetAllGroups(@Nonnull FeignGroupsApi api)
    {
        super(api::getAllGroups, PARAM_MAP);
    }

    /**
     * From the confluence page on <a href="=Channels">Channels
     * (The user-facing name for groups)</a>:
     * <p/>
     * The option {@code useCache} will result in this endpoint yielding {@link Group} values where the {@link Group#direction()} can by
     * either possible value. This will look like a list containing duplicate {@link Group} entries, where the two copies only have a
     * different {@link Group#direction()}.
     * <p/>
     * The option {@code useCache} also according to the confluence page should always be {@code true} for the reason:
     * <i>"When useCache is true, TAK Server returns the users cached group selection vs the groups that came directly from the auth backend".</i>
     *
     * @param useCache {@code true} to yield groups of {@link GroupDirection#OUT} and {@link GroupDirection#IN} <i>(Read and write respectively)</i>.
     * {@code false} <i>(default)</i> to yield only {@link GroupDirection#OUT} variants.
     *
     * @return Self.
     */
    @Nonnull
    public GetAllGroups withUseCache(boolean useCache)
    {
        return setParam("useCache", useCache);
    }

    /**
     * @param sendLatestSA {@code true} to have the server send SA updates back upon the requests completion.
     *
     * @return Self.
     */
    @Nonnull
    public GetAllGroups withSendLatestSA(boolean sendLatestSA)
    {
        return setParam("sendLatestSA", sendLatestSA);
    }
}
