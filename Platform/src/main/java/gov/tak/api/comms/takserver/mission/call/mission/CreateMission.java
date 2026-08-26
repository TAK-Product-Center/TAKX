package gov.tak.api.comms.takserver.mission.call.mission;

import gov.nasa.worldwind.geom.Position;
import gov.tak.api.comms.takserver.IGroupManager;
import gov.tak.api.comms.takserver.mission.call.AbstractEndpointCaller;
import gov.tak.api.comms.takserver.mission.call.IClientAssociatedEndpointCaller;
import gov.tak.api.comms.takserver.mission.call.ParameterInfo;
import gov.tak.api.comms.takserver.mission.data.MapLayer;
import gov.tak.api.comms.takserver.mission.data.Mission;
import gov.tak.api.comms.takserver.mission.data.MissionInvitation;
import gov.tak.api.comms.takserver.mission.data.RoleType;
import gov.tak.api.comms.takserver.mission.feign.FeignMissionApi;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Endpoint caller to create a {@link Mission} instance on the server.
 *
 * @since 3.5
 */
public class CreateMission extends AbstractEndpointCaller<Mission> implements IClientAssociatedEndpointCaller<Mission>
{
    private static final Map<String, ParameterInfo> PARAM_MAP = map(
            paramOptional("creatorUid", ""),
            paramOptional("group", Collections.emptyList()),
            paramOptional("description", ""),
            paramOptional("chatRoom", ""),
            paramOptional("baseLayer", ""),
            paramOptional("bbox", ""),
            paramOptional("boundingPolygon", ""),
            paramOptional("path", ""),
            paramOptional("classification", ""),
            paramOptional("tool", "public"),
            paramOptional("defaultRole", RoleType.MISSION_SUBSCRIBER),
            paramOptional("expiration", -1L),
            paramOptional("inviteOnly", false),
            paramOptional("password", ""));

    /**
     * @param api API reference.
     * @param missionName Name of the mission to fetch.
     * @param missionJson Mission json representation. Can be used as an alternative to query map entries.
     */
    public CreateMission(@Nonnull FeignMissionApi api, @Nonnull String missionName, @Nullable String missionJson)
    {
        super(unpack((headers, params) -> (missionJson != null) ? api.createMission(headers, params, missionName, missionJson) : api.createMission(headers, params, missionName)), PARAM_MAP);
    }

    @Nonnull
    @Override
    public String clientUidKey()
    {
        return "creatorUid";
    }

    /**
     * Sets the name of the group the mission belongs to.
     *
     * @param group Group name the mission will be associated with.
     *
     * @return Self.
     *
     * @see IGroupManager Only names of groups you have access to should be used,
     * though nothing is technically stopping you on the server side from doing otherwise.
     */
    @Nonnull
    public CreateMission withGroup(@Nonnull String group)
    {
        return withGroups(List.of(group));
    }

    /**
     * Sets the names of groups the mission belongs to.
     *
     * @param groups Group names the mission will be associated with.
     *
     * @return Self.
     *
     * @see IGroupManager Only names of groups you have access to should be used,
     * though nothing is technically stopping you on the server side from doing otherwise.
     * @since 5.0
     */
    @Nonnull
    public CreateMission withGroups(@Nonnull Collection<String> groups)
    {
        return setParam("group", groups);
    }

    /**
     * Sets the description of the mission.
     *
     * @param description Mission description.
     *
     * @return Self.
     */
    @Nonnull
    public CreateMission withDescription(@Nonnull String description)
    {
        return setParam("description", description);
    }

    /**
     * Sets the associated XMPP chat room URL of the mission.
     *
     * @param chatRoom XMPP url.
     *
     * @return Self.
     */
    @Nonnull
    public CreateMission withChatRoom(@Nonnull String chatRoom)
    {
        return setParam("chatRoom", chatRoom);
    }

    /**
     * Sets the associated {@link MapLayer} by its {@link MapLayer#uid()} for missions outlining a COP.
     *
     * @param baseLayer A {@link MapLayer#uid() map layer uid}.
     *
     * @return Self.
     */
    @Nonnull
    public CreateMission withBaseLayer(@Nonnull String baseLayer)
    {
        return setParam("baseLayer", baseLayer);
    }

    /**
     * Sets the bounding area of the mission.
     *
     * @param lat1 First corner Y coordinate <i>(latitude)</i>.
     * @param lon1 First corner X coordinate <i>(longitude)</i>.
     * @param lat2 Second corner Y coordinate <i>(latitude)</i>.
     * @param lon2 Second corner X coordinate <i>(longitude)</i>.
     *
     * @return Self.
     */
    @Nonnull
    public CreateMission withBoundingBox(double lat1, double lon1, double lat2, double lon2)
    {
        return withBoundingBox(String.format("%f %f %f %f", lat1, lon1, lat2, lon2));
    }

    /**
     * Sets the bounding area of the mission.
     *
     * @param bbox Bounding box in the format {@code <max lat>, <min lon>, <min lat>, <max lon>}
     * <i>({@code top, left, bottom, right})</i>.
     *
     * @return Self.
     */
    @Nonnull
    public CreateMission withBoundingBox(@Nonnull String bbox)
    {
        return setParam("bbox", bbox);
    }

    /**
     * Sets the bounding area of the mission.
     *
     * @param boundingPolygon List of positions that create a polygon shape.
     *
     * @return Self.
     */
    @Nonnull
    public CreateMission withBoundingPolygon(@Nonnull List<Position> boundingPolygon)
    {
        // TODO TAKX-2131: Move away from allowing single string, enforce point model
        return withBoundingPolygon(boundingPolygon.stream()
                .map(p -> String.format("%f %f", p.getLatitude().getDegrees(), p.getLongitude().getDegrees()))
                .collect(Collectors.joining(", ")));
    }

    /**
     * Sets the bounding area of the mission.
     *
     * @param boundingPolygon Bounding polygon in the format {@code <lat> <lon>, <lat> <lon>, ...} <i>(WKT format)</i>.
     *
     * @return Self.
     */
    @Nonnull
    public CreateMission withBoundingPolygon(@Nonnull String boundingPolygon)
    {
        // TODO TAKX-2131: Move away from allowing single string, enforce point model
        return setParam("boundingPolygon", boundingPolygon);
    }

    /**
     * Sets the path in the COP hierarchy for the mission. Outlines an absolute path like a file system.
     *
     * @param path COP hierarchy path.
     *
     * @return Self.
     */
    @Nonnull
    public CreateMission withPath(@Nonnull String path)
    {
        return setParam("path", path);
    }

    /**
     * Sets the classification level in CAPCO format for the mission.
     *
     * @param classification CAPCO classification level.
     *
     * @return Self.
     */
    @Nonnull
    public CreateMission withClassification(@Nonnull String classification)
    {
        return setParam("classification", classification);
    }

    /**
     * Sets the name of the tool <i>(A client such as ours)</i> that 'created' the mission.
     * By default, no real name is given so the value of {@code "public"} is used.
     *
     * @param tool Name of the tool to mark as responsible for creating the mission.
     *
     * @return Self.
     */
    @Nonnull
    public CreateMission withTool(@Nonnull String tool)
    {
        return setParam("tool", tool);
    }

    /**
     * Sets the default role of users of the mission.
     * By default, users are treated as {@link RoleType#MISSION_SUBSCRIBER subscribers}.
     *
     * @param defaultRole Initial role to assign to users of the mission.
     *
     * @return Self.
     */
    @Nonnull
    public CreateMission withDefaultRole(@Nonnull RoleType defaultRole)
    {
        return setParam("defaultRole", defaultRole);
    }

    /**
     * Sets an expiration timestamp on the mission, after which it will be automatically deleted.
     *
     * @param expiration Timestamp in epoch millis for the mission to delete itself.
     * Using {@code System.currentTimeMillis() + 1000} would specify an expiration time of one second from now.
     *
     * @return Self.
     */
    @Nonnull
    public CreateMission withExpirationMillis(long expiration)
    {
        return setParam("expiration", expiration);
    }

    /**
     * Locks the created mission behind invitations.
     *
     * @param inviteOnly {@code true} to limit access to the mission to those only granted a
     * {@link MissionInvitation}.
     *
     * @return Self.
     */
    @Nonnull
    public CreateMission withInviteOnly(boolean inviteOnly)
    {
        return setParam("inviteOnly", inviteOnly);
    }

    /**
     * Adds a password to the mission. preventing random access to the mission without knowledge of the password.
     * As the creator of the mission, you do not need to provide a password to access the mission though.
     *
     * @param password The password to access the mission.
     *
     * @return Self.
     */
    @Nonnull
    public CreateMission withPassword(@Nonnull String password)
    {
        return setParam("password", password);
    }
}
