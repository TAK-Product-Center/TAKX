package gov.tak.api.comms.takserver.mission.call.mission;

import gov.tak.api.comms.takserver.mission.call.AbstractEndpointCaller;
import gov.tak.api.comms.takserver.mission.call.IClientAssociatedEndpointCaller;
import gov.tak.api.comms.takserver.mission.call.ParameterInfo;
import gov.tak.api.comms.takserver.mission.data.ClientEndpoint;
import gov.tak.api.comms.takserver.mission.data.Mission;
import gov.tak.api.comms.takserver.mission.data.MissionFeed;
import gov.tak.api.comms.takserver.mission.feign.FeignMissionApi;
import jakarta.annotation.Nonnull;

import java.util.Map;

/**
 * Endpoint caller to assign a {@link MissionFeed} to a {@link Mission} instance,
 * creating a new {@link MissionFeed} if the provided {@link #withDataFeedUid(String) UID}
 * does not exist on the server.
 *
 * @see RemoveFeed
 * @since 3.5
 */
public class AddFeed extends AbstractEndpointCaller<Void> implements IClientAssociatedEndpointCaller<Void>
{
    private static final Map<String, ParameterInfo> PARAM_MAP = map(
            param("creatorUid", ""),
            param("dataFeedUid", ""),
            paramOptional("filterBbox", ""),
            paramOptional("filterType", ""),
            paramOptional("filterCallsign", ""));

    /**
     * @param api API reference.
     * @param missionName Name of the mission to update.
     */
    public AddFeed(@Nonnull FeignMissionApi api, @Nonnull String missionName)
    {
        super(devoid((headers, params) -> api.addFeed(headers, params, missionName)), PARAM_MAP);
    }

    @Nonnull
    @Override
    public String clientUidKey()
    {
        return "creatorUid";
    }

    /**
     * Sets the UID of the feed to add to the mission.
     * If no such UID exists a new {@link MissionFeed} will be created with this UID.
     *
     * @param dataFeedUid The {@link MissionFeed#uid()} to add to the mission.
     *
     * @return Self.
     */
    @Nonnull
    public AddFeed withDataFeedUid(@Nonnull String dataFeedUid)
    {
        return setParam("dataFeedUid", dataFeedUid);
    }

    /**
     * Sets the {@link MissionFeed#filterBbox()} of a created data feed.
     *
     * @param maxLat Top coordinate of the bounding box.
     * @param minLon Left coordinate of the bounding box.
     * @param minLat Bottom coordinate of the bounding box.
     * @param maxLon Right coordinate of the bounding box.
     *
     * @return Self.
     */
    @Nonnull
    public AddFeed withFilterBbox(double maxLat, double minLon, double minLat, double maxLon)
    {
        return withFilterBbox(String.format("%f, %f, %f, %f", maxLat, minLon, minLat, maxLon));
    }

    /**
     * Sets the {@link MissionFeed#filterBbox()} of a created data feed.
     *
     * @param filterBbox Value of {@link MissionFeed#filterBbox()} to assign to a created {@link MissionFeed}.
     * Bounding box in the format {@code <max lat>, <min lon>, <min lat>, <max lon>}
     * <i>({@code top, left, bottom, right})</i>.
     *
     * @return Self.
     */
    @Nonnull
    public AddFeed withFilterBbox(@Nonnull String filterBbox)
    {
        return setParam("filterBbox", filterBbox);
    }

    /**
     * Sets the {@link MissionFeed#filterType()} of a created data feed.
     *
     * @param filterType Value of {@link MissionFeed#filterType()} to assign to a created {@link MissionFeed}.
     * Matches a type of CoT event by name.
     *
     * @return Self.
     */
    @Nonnull
    public AddFeed withFilterType(@Nonnull String filterType)
    {
        return setParam("filterType", filterType);
    }

    /**
     * Sets the {@link MissionFeed#filterCallsign()} of a created data feed.
     *
     * @param filterCallsign Value of {@link MissionFeed#filterCallsign()} to assign to a created {@link MissionFeed}.
     * Matches the name of a {@link ClientEndpoint#callsign()}.
     *
     * @return Self.
     */
    @Nonnull
    public AddFeed withFilterCallsign(@Nonnull String filterCallsign)
    {
        return setParam("filterCallsign", filterCallsign);
    }
}
