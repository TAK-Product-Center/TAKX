package gov.tak.api.comms.takserver.mission;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import gov.tak.api.comms.takserver.ITakServerConnection;
import gov.tak.api.comms.takserver.mission.call.IClientAssociatedEndpointCaller;
import gov.tak.api.comms.takserver.mission.call.IEndpointCaller;
import gov.tak.api.comms.takserver.mission.call.mission.AddFeed;
import gov.tak.api.comms.takserver.mission.call.mission.AddMissionContent;
import gov.tak.api.comms.takserver.mission.call.mission.AddMissionContentByGuid;
import gov.tak.api.comms.takserver.mission.call.mission.AddMissionPackage;
import gov.tak.api.comms.takserver.mission.call.mission.ClearMissionKeywords;
import gov.tak.api.comms.takserver.mission.call.mission.ClearMissionResourceKeyword;
import gov.tak.api.comms.takserver.mission.call.mission.CopyMission;
import gov.tak.api.comms.takserver.mission.call.mission.CreateLogEntry;
import gov.tak.api.comms.takserver.mission.call.mission.CreateMapLayer;
import gov.tak.api.comms.takserver.mission.call.mission.CreateMapLayerByGuid;
import gov.tak.api.comms.takserver.mission.call.mission.CreateMission;
import gov.tak.api.comms.takserver.mission.call.mission.CreateMissionLayer;
import gov.tak.api.comms.takserver.mission.call.mission.CreateMissionLayerByGuid;
import gov.tak.api.comms.takserver.mission.call.mission.CreateMissionReadAccessToken;
import gov.tak.api.comms.takserver.mission.call.mission.CreateMissionSubscription;
import gov.tak.api.comms.takserver.mission.call.mission.CreateMissionSubscriptionByGuid;
import gov.tak.api.comms.takserver.mission.call.mission.GetAllLogEntries;
import gov.tak.api.comms.takserver.mission.call.mission.GetAllMissionSubscribers;
import gov.tak.api.comms.takserver.mission.call.mission.GetAllMissions;
import gov.tak.api.comms.takserver.mission.call.mission.GetCurrentMissionSubscription;
import gov.tak.api.comms.takserver.mission.call.mission.GetCurrentMissionSubscriptionByGuid;
import gov.tak.api.comms.takserver.mission.call.mission.GetLatestMissionCotEvents;
import gov.tak.api.comms.takserver.mission.call.mission.GetLatestMissionCotEventsByGuid;
import gov.tak.api.comms.takserver.mission.call.mission.GetLogEntries;
import gov.tak.api.comms.takserver.mission.call.mission.GetLogEntry;
import gov.tak.api.comms.takserver.mission.call.mission.GetMission;
import gov.tak.api.comms.takserver.mission.call.mission.GetMissionArchive;
import gov.tak.api.comms.takserver.mission.call.mission.GetMissionArchiveByGuid;
import gov.tak.api.comms.takserver.mission.call.mission.GetMissionByGuid;
import gov.tak.api.comms.takserver.mission.call.mission.GetMissionChanges;
import gov.tak.api.comms.takserver.mission.call.mission.GetMissionChangesByGuid;
import gov.tak.api.comms.takserver.mission.call.mission.GetMissionChildren;
import gov.tak.api.comms.takserver.mission.call.mission.GetMissionChildrenByGuid;
import gov.tak.api.comms.takserver.mission.call.mission.GetMissionInvitations;
import gov.tak.api.comms.takserver.mission.call.mission.GetMissionInvitationsByGuid;
import gov.tak.api.comms.takserver.mission.call.mission.GetMissionInvitationsForUser;
import gov.tak.api.comms.takserver.mission.call.mission.GetMissionKml;
import gov.tak.api.comms.takserver.mission.call.mission.GetMissionKmlByGuid;
import gov.tak.api.comms.takserver.mission.call.mission.GetMissionLayer;
import gov.tak.api.comms.takserver.mission.call.mission.GetMissionLayerByGuid;
import gov.tak.api.comms.takserver.mission.call.mission.GetMissionLayers;
import gov.tak.api.comms.takserver.mission.call.mission.GetMissionLayersByGuid;
import gov.tak.api.comms.takserver.mission.call.mission.GetMissionParent;
import gov.tak.api.comms.takserver.mission.call.mission.GetMissionParentByGuid;
import gov.tak.api.comms.takserver.mission.call.mission.GetMissionRole;
import gov.tak.api.comms.takserver.mission.call.mission.GetMissionRoleByGuid;
import gov.tak.api.comms.takserver.mission.call.mission.GetMissionSubscribers;
import gov.tak.api.comms.takserver.mission.call.mission.GetMissionSubscribersByGuid;
import gov.tak.api.comms.takserver.mission.call.mission.GetMissionSubscriptions;
import gov.tak.api.comms.takserver.mission.call.mission.GetMissionSubscriptionsByGuid;
import gov.tak.api.comms.takserver.mission.call.mission.GetRemoteSubscriptions;
import gov.tak.api.comms.takserver.mission.call.mission.GetRemoteSubscriptionsByGuid;
import gov.tak.api.comms.takserver.mission.call.mission.NotifyChangedMissionExternalData;
import gov.tak.api.comms.takserver.mission.call.mission.NotifyChangedMissionExternalDataByGuid;
import gov.tak.api.comms.takserver.mission.call.mission.RemoveFeed;
import gov.tak.api.comms.takserver.mission.call.mission.RemoveLogEntry;
import gov.tak.api.comms.takserver.mission.call.mission.RemoveMapLayer;
import gov.tak.api.comms.takserver.mission.call.mission.RemoveMapLayerByGuid;
import gov.tak.api.comms.takserver.mission.call.mission.RemoveMission;
import gov.tak.api.comms.takserver.mission.call.mission.RemoveMissionByGuid;
import gov.tak.api.comms.takserver.mission.call.mission.RemoveMissionContent;
import gov.tak.api.comms.takserver.mission.call.mission.RemoveMissionContentByGuid;
import gov.tak.api.comms.takserver.mission.call.mission.RemoveMissionExternalData;
import gov.tak.api.comms.takserver.mission.call.mission.RemoveMissionExternalDataByGuid;
import gov.tak.api.comms.takserver.mission.call.mission.RemoveMissionInvitation;
import gov.tak.api.comms.takserver.mission.call.mission.RemoveMissionInvitationByGuid;
import gov.tak.api.comms.takserver.mission.call.mission.RemoveMissionKeyword;
import gov.tak.api.comms.takserver.mission.call.mission.RemoveMissionLayer;
import gov.tak.api.comms.takserver.mission.call.mission.RemoveMissionLayerByGuid;
import gov.tak.api.comms.takserver.mission.call.mission.RemoveMissionParent;
import gov.tak.api.comms.takserver.mission.call.mission.RemoveMissionParentByGuid;
import gov.tak.api.comms.takserver.mission.call.mission.RemoveMissionPassword;
import gov.tak.api.comms.takserver.mission.call.mission.RemoveMissionPasswordByGuid;
import gov.tak.api.comms.takserver.mission.call.mission.RemoveMissionSubscription;
import gov.tak.api.comms.takserver.mission.call.mission.RemoveMissionSubscriptionByGuid;
import gov.tak.api.comms.takserver.mission.call.mission.SendMissionArchive;
import gov.tak.api.comms.takserver.mission.call.mission.SendMissionArchiveByGuid;
import gov.tak.api.comms.takserver.mission.call.mission.SendMissionInvitation;
import gov.tak.api.comms.takserver.mission.call.mission.SendMissionInvitationByGuid;
import gov.tak.api.comms.takserver.mission.call.mission.SendMissionInvitations;
import gov.tak.api.comms.takserver.mission.call.mission.SendMissionInvitationsByGuid;
import gov.tak.api.comms.takserver.mission.call.mission.SetMissionExpiration;
import gov.tak.api.comms.takserver.mission.call.mission.SetMissionExpirationByGuid;
import gov.tak.api.comms.takserver.mission.call.mission.SetMissionExternalData;
import gov.tak.api.comms.takserver.mission.call.mission.SetMissionExternalDataByGuid;
import gov.tak.api.comms.takserver.mission.call.mission.SetMissionKeywords;
import gov.tak.api.comms.takserver.mission.call.mission.SetMissionLayerName;
import gov.tak.api.comms.takserver.mission.call.mission.SetMissionLayerNameByGuid;
import gov.tak.api.comms.takserver.mission.call.mission.SetMissionLayerParent;
import gov.tak.api.comms.takserver.mission.call.mission.SetMissionLayerParentByGuid;
import gov.tak.api.comms.takserver.mission.call.mission.SetMissionLayerPosition;
import gov.tak.api.comms.takserver.mission.call.mission.SetMissionLayerPositionByGuid;
import gov.tak.api.comms.takserver.mission.call.mission.SetMissionParent;
import gov.tak.api.comms.takserver.mission.call.mission.SetMissionPassword;
import gov.tak.api.comms.takserver.mission.call.mission.SetMissionPasswordByGuid;
import gov.tak.api.comms.takserver.mission.call.mission.SetMissionResourceKeywords;
import gov.tak.api.comms.takserver.mission.call.mission.SetMissionRole;
import gov.tak.api.comms.takserver.mission.call.mission.SetMissionRoleByGuid;
import gov.tak.api.comms.takserver.mission.call.mission.SetMissionSubscriptionRoles;
import gov.tak.api.comms.takserver.mission.call.mission.SetMissionSubscriptionRolesByGuid;
import gov.tak.api.comms.takserver.mission.call.mission.UpdateLogEntry;
import gov.tak.api.comms.takserver.mission.call.mission.UpdateMapLayer;
import gov.tak.api.comms.takserver.mission.call.mission.UpdateMapLayerByGuid;
import gov.tak.api.comms.takserver.mission.data.ClientEndpoint;
import gov.tak.api.comms.takserver.mission.data.ExternalMissionData;
import gov.tak.api.comms.takserver.mission.data.InvitationType;
import gov.tak.api.comms.takserver.mission.data.LogEntry;
import gov.tak.api.comms.takserver.mission.data.MapLayer;
import gov.tak.api.comms.takserver.mission.data.Mission;
import gov.tak.api.comms.takserver.mission.data.MissionChange;
import gov.tak.api.comms.takserver.mission.data.MissionContentKeys;
import gov.tak.api.comms.takserver.mission.data.MissionFeed;
import gov.tak.api.comms.takserver.mission.data.MissionInvitation;
import gov.tak.api.comms.takserver.mission.data.MissionLayer;
import gov.tak.api.comms.takserver.mission.data.MissionSubscription;
import gov.tak.api.comms.takserver.mission.data.RemoteSubscription;
import gov.tak.api.comms.takserver.mission.data.Resource;
import gov.tak.api.comms.takserver.mission.feign.FeignMissionApi;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import java.util.List;
import java.util.UUID;

/**
 * Convenience utility for referencing and calling API endpoints through builder patterns.
 * <p>
 * Wraps {@link FeignMissionApi}.
 *
 * @since 3.5
 */
@SuppressWarnings("unused")
public class MissionEndpoints
{
    private static final ObjectMapper mapper = new ObjectMapper();
    private final FeignMissionApi missionApi;
    private final UUID clientUid;

    static
    {
        mapper.registerModule(new JavaTimeModule());

        // Override certain features to ensure instants are serialized as longs (of their epoch millis equivalent)
        mapper.enable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        mapper.disable(SerializationFeature.WRITE_DATE_TIMESTAMPS_AS_NANOSECONDS);
    }

    /**
     * @param apiProvider Provider of client instances.
     * @param clientUid Unique client identifier.
     * @param connection TAK server connection to use to initialize an API client with.
     */
    public MissionEndpoints(@Nonnull ApiProvider apiProvider,
                            @Nonnull UUID clientUid,
                            @Nonnull ITakServerConnection connection)
    {
        missionApi = apiProvider.getMissionApi(connection);
        this.clientUid = clientUid;
    }

    /**
     * @return Endpoint to list all available {@link Mission} instances.
     */
    @Nonnull
    public GetAllMissions getAllMissions()
    {
        return interceptClientUid(new GetAllMissions(missionApi));
    }

    /**
     * @param mission Instance of the mission create on the server.
     *
     * @return Endpoint to create the {@link Mission} instance.
     */
    @Nonnull
    public CreateMission createMission(@Nonnull Mission mission)
    {
        return createMission(mission, null);
    }

    /**
     * @param mission Instance of the mission create on the server.
     * @param password Password to assign to the created mission.
     *
     * @return Endpoint to create the {@link Mission} instance.
     */
    @Nonnull
    public CreateMission createMission(@Nonnull Mission mission, @Nullable String password)
    {
        /*
        TODO: The endpoint does not properly take all the values from the serialized mission, like the groups attribute.
              Not being able to have this value set prevents it from showing up in other TAK products like WinTAK since
              it will default to being an anonymous mission group (__ANON__).
              Until that gets fixed, just extract the properties and pass as query parameters.
        try
        {
            return interceptClientUid(new CreateMission(missionApi, mission.name(), mapper.writeValueAsString(mission))).withPassword(password);
        } catch (JsonProcessingException ex)
        {
            // Will never realistically happen.
            throw new IllegalStateException(ex);
        }
         */
        CreateMission create = createMission(mission.name());
        if (mission.groups() != null && !mission.groups().isEmpty()) create.withGroups(mission.groups());
        if (mission.description() != null) create.withDescription(mission.description());
        if (mission.chatRoom() != null) create.withChatRoom(mission.chatRoom());
        if (mission.baseLayer() != null) create.withBaseLayer(mission.baseLayer());
        if (mission.bbox() != null) create.withBoundingBox(mission.bbox());
        if (mission.boundingPolygon() != null) create.withBoundingPolygon(mission.boundingPolygon());
        if (mission.path() != null) create.withPath(mission.path());
        if (mission.classification() != null) create.withClassification(mission.classification());
        if (mission.tool() != null) create.withTool(mission.tool());
        if (mission.defaultRole() != null) create.withDefaultRole(mission.defaultRole().type());
        if (mission.expiration() != null) create.withExpirationMillis(mission.expiration());
        if (mission.inviteOnly()) create.withInviteOnly(true);
        if (password != null && !password.isBlank()) create.withPassword(password);
        return create;
    }

    /**
     * @param missionName Name of the mission to create.
     *
     * @return Endpoint to create the {@link Mission} instance.
     */
    @Nonnull
    public CreateMission createMission(@Nonnull String missionName)
    {
        return interceptClientUid(new CreateMission(missionApi, missionName, null));
    }

    /**
     * @param missionName Name of the mission to fetch.
     *
     * @return Endpoint to get a single {@link Mission} instance.
     */
    @Nonnull
    public GetMission getMission(@Nonnull String missionName)
    {
        return interceptClientUid(new GetMission(missionApi, missionName));
    }

    /**
     * @param missionGuid Guid of the mission to fetch.
     *
     * @return Endpoint to get a single {@link Mission} instance.
     * @since 5.7
     */
    @Nonnull
    public GetMissionByGuid getMissionByGuid(@Nonnull String missionGuid)
    {
        return interceptClientUid(new GetMissionByGuid(missionApi, missionGuid));
    }

    /**
     * @param missionName Name of the mission to fetch.
     *
     * @return Endpoint to get a single {@link Mission} instance in KML format.
     */
    @Nonnull
    public GetMissionKml getMissionKml(@Nonnull String missionName)
    {
        return interceptClientUid(new GetMissionKml(missionApi, missionName));
    }

    /**
     * @param missionGuid Guid of the mission to fetch.
     *
     * @return Endpoint to get a single {@link Mission} instance in KML format.
     * @since 5.7
     */
    @Nonnull
    public GetMissionKmlByGuid getMissionKmlByGuid(@Nonnull String missionGuid)
    {
        return interceptClientUid(new GetMissionKmlByGuid(missionApi, missionGuid));
    }

    /**
     * @param missionName Name of the mission to copy.
     *
     * @return Endpoint to copy a single {@link Mission} instances.
     */
    @Nonnull
    public CopyMission copyMission(@Nonnull String missionName)
    {
        return interceptClientUid(new CopyMission(missionApi, missionName));
    }

    /**
     * @param missionName Name of the mission to delete.
     *
     * @return Endpoint to delete a single {@link Mission} instances.
     */
    @Nonnull
    public RemoveMission removeMission(@Nonnull String missionName)
    {
        return interceptClientUid(new RemoveMission(missionApi, missionName));
    }

    /**
     * @param missionGuid Guid of the mission to delete.
     *
     * @return Endpoint to delete a single {@link Mission} instances.
     * @since 5.7
     */
    @Nonnull
    public RemoveMissionByGuid removeMissionByGuid(@Nonnull String missionGuid)
    {
        return interceptClientUid(new RemoveMissionByGuid(missionApi).withGuid(missionGuid));
    }

    /**
     * @param missionName Name of the mission to look up the parent of.
     *
     * @return Endpoint to get a parent {@link Mission} of another {@link Mission} instances.
     */
    @Nonnull
    public GetMissionParent getMissionParent(@Nonnull String missionName)
    {
        return interceptClientUid(new GetMissionParent(missionApi, missionName));
    }

    /**
     * @param missionGuid Guid of the mission to look up the parent of.
     *
     * @return Endpoint to get a parent {@link Mission} of another {@link Mission} instances.
     * @since 5.7
     */
    @Nonnull
    public GetMissionParentByGuid getMissionParentByGuid(@Nonnull String missionGuid)
    {
        return interceptClientUid(new GetMissionParentByGuid(missionApi, missionGuid));
    }

    /**
     * @param missionName Name of the parent mission to look up the children of.
     *
     * @return Endpoint to get all child {@link Mission} instances linked to a given parent mission.
     */
    @Nonnull
    public GetMissionChildren getMissionChildren(@Nonnull String missionName)
    {
        return interceptClientUid(new GetMissionChildren(missionApi, missionName));
    }

    /**
     * @param missionGuid Guid of the parent mission to look up the children of.
     *
     * @return Endpoint to get all child {@link Mission} instances linked to a given parent mission.
     * @since 5.7
     */
    @Nonnull
    public GetMissionChildrenByGuid getMissionChildrenByGuid(@Nonnull String missionGuid)
    {
        return interceptClientUid(new GetMissionChildrenByGuid(missionApi, missionGuid));
    }

    /**
     * @param missionName Name of the mission to update.
     * @param parentMissionName Name of the mission to link to as a parent.
     *
     * @return Endpoint to get a parent {@link Mission} of another {@link Mission} instances.
     */
    @Nonnull
    public SetMissionParent setMissionParent(@Nonnull String missionName, @Nonnull String parentMissionName)
    {
        return interceptClientUid(new SetMissionParent(missionApi, missionName, parentMissionName));
    }

    /**
     * @param missionGuid Guid of the mission to update.
     * @param parentMissionGuid Guid of the mission to link to as a parent.
     *
     * @return Endpoint to get a parent {@link Mission} of another {@link Mission} instances.
     * @since 5.7
     */
    @Nonnull
    public SetMissionParent setMissionParentByGuid(@Nonnull String missionGuid, @Nonnull String parentMissionGuid)
    {
        return interceptClientUid(new SetMissionParent(missionApi, missionGuid, parentMissionGuid));
    }

    /**
     * @param missionName Name of the mission to update.
     *
     * @return Endpoint to remove parent {@link Mission} link in the given mission.
     */
    @Nonnull
    public RemoveMissionParent removeMissionParent(@Nonnull String missionName)
    {
        return interceptClientUid(new RemoveMissionParent(missionApi, missionName));
    }

    /**
     * @param missionName Name of the mission to update.
     *
     * @return Endpoint to remove parent {@link Mission} link in the given mission.
     * @since 5.7
     */
    @Nonnull
    public RemoveMissionParentByGuid removeMissionParentByGuid(@Nonnull String missionName)
    {
        return interceptClientUid(new RemoveMissionParentByGuid(missionApi, missionName));
    }

    /**
     * @param missionName Name of the mission to update.
     *
     * @return Endpoint to set a password on a {@link Mission} instance.
     */
    @Nonnull
    public SetMissionPassword setMissionPassword(@Nonnull String missionName)
    {
        return interceptClientUid(new SetMissionPassword(missionApi, missionName));
    }

    /**
     * @param missionGuid Guid of the mission to update.
     *
     * @return Endpoint to set a password on a {@link Mission} instance.
     * @since 5.7
     */
    @Nonnull
    public SetMissionPasswordByGuid setMissionPasswordByGuid(@Nonnull String missionGuid)
    {
        return interceptClientUid(new SetMissionPasswordByGuid(missionApi, missionGuid));
    }

    /**
     * @param missionName Name of the mission to update.
     *
     * @return Endpoint to remove a password from a {@link Mission} instance.
     */
    @Nonnull
    public RemoveMissionPassword removeMissionPassword(@Nonnull String missionName)
    {
        return interceptClientUid(new RemoveMissionPassword(missionApi, missionName));
    }

    /**
     * @param missionGuid Guid of the mission to update.
     *
     * @return Endpoint to remove a password from a {@link Mission} instance.
     * @since 5.7
     */
    @Nonnull
    public RemoveMissionPasswordByGuid removeMissionPasswordByGuid(@Nonnull String missionGuid)
    {
        return interceptClientUid(new RemoveMissionPasswordByGuid(missionApi, missionGuid));
    }

    /**
     * @param missionName Name of the mission to update.
     *
     * @return Endpoint to set an expiration time for on a {@link Mission} instance.
     */
    @Nonnull
    public SetMissionExpiration setMissionExpiration(@Nonnull String missionName)
    {
        return interceptClientUid(new SetMissionExpiration(missionApi, missionName));
    }

    /**
     * @param missionGuid Guid of the mission to update.
     *
     * @return Endpoint to set an expiration time for on a {@link Mission} instance.
     * @since 5.7
     */
    @Nonnull
    public SetMissionExpirationByGuid setMissionExpirationByGuid(@Nonnull String missionGuid)
    {
        return interceptClientUid(new SetMissionExpirationByGuid(missionApi, missionGuid));
    }

    /**
     * @param missionName Name of the mission to update.
     * @param data External mission data to associate with the mission.
     *
     * @return Endpoint to associate some {@link ExternalMissionData} with a {@link Mission} instance.
     */
    @Nonnull
    public SetMissionExternalData setMissionExternalData(@Nonnull String missionName, @Nonnull ExternalMissionData data)
    {
        return interceptClientUid(new SetMissionExternalData(missionApi, missionName, data));
    }

    /**
     * @param missionGuid Guid of the mission to update.
     * @param data External mission data to associate with the mission.
     *
     * @return Endpoint to associate some {@link ExternalMissionData} with a {@link Mission} instance.
     * @since 5.7
     */
    @Nonnull
    public SetMissionExternalDataByGuid setMissionExternalDataByGuid(@Nonnull String missionGuid, @Nonnull ExternalMissionData data)
    {
        return interceptClientUid(new SetMissionExternalDataByGuid(missionApi, missionGuid, data));
    }

    /**
     * @param missionName Name of the mission to update.
     * @param id The {@link ExternalMissionData#uid() identifier} of data to remove.
     *
     * @return Endpoint to remove an {@link ExternalMissionData} item from a {@link Mission} instance.
     */
    @Nonnull
    public RemoveMissionExternalData removeMissionExternalData(@Nonnull String missionName, @Nonnull String id)
    {
        return interceptClientUid(new RemoveMissionExternalData(missionApi, missionName, id));
    }

    /**
     * @param missionGuid Guid of the mission to update.
     * @param id The {@link ExternalMissionData#uid() identifier} of data to remove.
     *
     * @return Endpoint to remove an {@link ExternalMissionData} item from a {@link Mission} instance.
     * @since 5.7
     */
    @Nonnull
    public RemoveMissionExternalDataByGuid removeMissionExternalDataByGuid(@Nonnull String missionGuid, @Nonnull String id)
    {
        return interceptClientUid(new RemoveMissionExternalDataByGuid(missionApi, missionGuid, id));
    }

    /**
     * @param missionName Name of the mission to notify.
     * @param id The {@link ExternalMissionData#uid() identifier} of data to notify of an update.
     * @param token Token assigned to the {@link MissionChange} when the remote server
     * creates a change-log entry. Otherwise, seemingly unused.
     *
     * @return Endpoint to notify a {@link Mission} containing {@link ExternalMissionData} that the remote data has updated.
     */
    @Nonnull
    public NotifyChangedMissionExternalData notifyChangedMissionExternalData(@Nonnull String missionName,
                                                                             @Nonnull String id, @Nonnull String token)
    {
        return interceptClientUid(new NotifyChangedMissionExternalData(missionApi, missionName, id, token));
    }

    /**
     * @param missionGuid Guid of the mission to notify.
     * @param id The {@link ExternalMissionData#uid() identifier} of data to notify of an update.
     * @param token Token assigned to the {@link MissionChange} when the remote server
     * creates a change-log entry. Otherwise, seemingly unused.
     *
     * @return Endpoint to notify a {@link Mission} containing {@link ExternalMissionData} that the remote data has updated.
     * @since 5.7
     */
    @Nonnull
    public NotifyChangedMissionExternalDataByGuid notifyChangedMissionExternalDataByGuid(@Nonnull String missionGuid,
                                                                                         @Nonnull String id, @Nonnull String token)
    {
        return interceptClientUid(new NotifyChangedMissionExternalDataByGuid(missionApi, missionGuid, id, token));
    }

    /**
     * @param missionName Name of the mission to update.
     *
     * @return Endpoint to assign a {@link MissionFeed} to a {@link Mission} instance,
     * creating a new {@link MissionFeed} if the provided {@link AddFeed#withDataFeedUid(String) UID}
     * does not exist on the server.
     */
    @Nonnull
    public AddFeed addFeedToMission(@Nonnull String missionName)
    {
        return interceptClientUid(new AddFeed(missionApi, missionName));
    }

    /**
     * @param missionName Name of the mission to update.
     * @param uid The {@link MissionFeed#uid()} of feed to remove.
     *
     * @return Endpoint to remove an {@link MissionFeed} item from a {@link Mission} instance.
     */
    @Nonnull
    public RemoveFeed removeFeedFromMission(@Nonnull String missionName, @Nonnull String uid)
    {
        return interceptClientUid(new RemoveFeed(missionApi, missionName, uid));
    }

    /**
     * @param missionName Name of the mission to get the archive of.
     *
     * @return Endpoint to get an archive of a mission.
     */
    @Nonnull
    public GetMissionArchive getMissionArchive(@Nonnull String missionName)
    {
        return interceptClientUid(new GetMissionArchive(missionApi, missionName));
    }

    /**
     * @param missionGuid Guid of the mission to get the archive of.
     *
     * @return Endpoint to get an archive of a mission.
     * @since 5.7
     */
    @Nonnull
    public GetMissionArchiveByGuid getMissionArchiveByGuid(@Nonnull String missionGuid)
    {
        return interceptClientUid(new GetMissionArchiveByGuid(missionApi, missionGuid));
    }

    /**
     * @param missionName Name of the mission to get the archive of.
     *
     * @return Endpoint to send an archived copy of a {@link Mission} to a number of recipients.
     */
    @Nonnull
    public SendMissionArchive sendMissionArchive(@Nonnull String missionName)
    {
        return interceptClientUid(new SendMissionArchive(missionApi, missionName));
    }

    /**
     * @param missionGuid Guid of the mission to get the archive of.
     *
     * @return Endpoint to send an archived copy of a {@link Mission} to a number of recipients.
     * @since 5.7
     */
    @Nonnull
    public SendMissionArchiveByGuid sendMissionArchiveByGuid(@Nonnull String missionGuid)
    {
        return interceptClientUid(new SendMissionArchiveByGuid(missionApi, missionGuid));
    }

    /**
     * @param missionName Name of the mission to update.
     * @param keys Keys of {@link Resource#uid()} and {@link Resource#hash()} items.
     *
     * @return Endpoint to add one or more {@link Resource} entries to a {@link Mission}.
     */
    @Nonnull
    public AddMissionContent addMissionContent(@Nonnull String missionName, @Nonnull MissionContentKeys keys)
    {
        return interceptClientUid(new AddMissionContent(missionApi, missionName, keys));
    }

    /**
     * @param missionGuid Guid of the mission to update.
     * @param keys Keys of {@link Resource#uid()} and {@link Resource#hash()} items.
     *
     * @return Endpoint to add one or more {@link Resource} entries to a {@link Mission}.
     * @since 5.7
     */
    @Nonnull
    public AddMissionContentByGuid addMissionContentByGuid(@Nonnull String missionGuid, @Nonnull MissionContentKeys keys)
    {
        return interceptClientUid(new AddMissionContentByGuid(missionApi, missionGuid, keys));
    }

    /**
     * @param missionName Name of the mission to update.
     *
     * @return Endpoint to remove a {@link Resource} entry from a {@link Mission}.
     */
    @Nonnull
    public RemoveMissionContent removeMissionContent(@Nonnull String missionName)
    {
        return interceptClientUid(new RemoveMissionContent(missionApi, missionName));
    }

    /**
     * @param missionGuid Guid of the mission to update.
     *
     * @return Endpoint to remove a {@link Resource} entry from a {@link Mission}.
     * @since 5.7
     */
    @Nonnull
    public RemoveMissionContentByGuid removeMissionContentByGuid(@Nonnull String missionGuid)
    {
        return interceptClientUid(new RemoveMissionContentByGuid(missionApi, missionGuid));
    }

    /**
     * @param missionName Name of the mission to update.
     * @param missionPackage Raw contents of mission package ZIP archive.
     *
     * @return Endpoint to upload a mission package <i>(ZIP file)</i> to a {@link Mission}.
     */
    @Nonnull
    public AddMissionPackage addMissionPackage(@Nonnull String missionName, @Nonnull byte[] missionPackage)
    {
        return interceptClientUid(new AddMissionPackage(missionApi, missionName, missionPackage));
    }

    /**
     * @param missionName Name of the mission to get the changelog of.
     *
     * @return Endpoint to get the changelog of a {@link Mission} instance.
     */
    @Nonnull
    public GetMissionChanges getMissionChanges(@Nonnull String missionName)
    {
        return interceptClientUid(new GetMissionChanges(missionApi, missionName));
    }

    /**
     * @param missionGuid Guid of the mission to get the changelog of.
     *
     * @return Endpoint to get the changelog of a {@link Mission} instance.
     * @since 5.7
     */
    @Nonnull
    public GetMissionChangesByGuid getMissionChangesByGuid(@Nonnull String missionGuid)
    {
        return interceptClientUid(new GetMissionChangesByGuid(missionApi, missionGuid));
    }

    /**
     * @param missionName Name of the mission to update.
     * @param keywords List of keywords to associate with the mission.
     *
     * @return Endpoint to set keywords of a {@link Mission} instance.
     */
    @Nonnull
    public SetMissionKeywords setMissionKeywords(@Nonnull String missionName, @Nonnull List<String> keywords)
    {
        return interceptClientUid(new SetMissionKeywords(missionApi, missionName, keywords));
    }

    /**
     * @param missionName Name of the mission to update.
     *
     * @return Endpoint to remove all keywords from a {@link Mission} instance.
     */
    @Nonnull
    public ClearMissionKeywords clearMissionKeywords(@Nonnull String missionName)
    {
        return interceptClientUid(new ClearMissionKeywords(missionApi, missionName));
    }

    /**
     * @param missionName Name of the mission to update.
     * @param keyword Keyword to remove.
     *
     * @return Endpoint to remove one keyword from a {@link Mission} instance.
     */
    @Nonnull
    public RemoveMissionKeyword removeMissionKeyword(@Nonnull String missionName, @Nonnull String keyword)
    {
        return interceptClientUid(new RemoveMissionKeyword(missionApi, missionName, keyword));
    }

    /**
     * @param missionName Name of the mission with a resource to update.
     * @param hash {@link Resource#hash()} in a mission to update.
     * @param keywords Keywords to set.
     *
     * @return Endpoint to set mission-local keywords of a contained {@link Mission#contents() resource}.
     */
    @Nonnull
    public SetMissionResourceKeywords setMissionResourceKeywords(@Nonnull String missionName, @Nonnull String hash,
                                                                 @Nonnull List<String> keywords)
    {
        return interceptClientUid(new SetMissionResourceKeywords(missionApi, missionName, hash, keywords));
    }

    /**
     * @param missionName Name of the mission with a resource to update.
     * @param hash {@link Resource#hash()} in a mission to update.
     *
     * @return Endpoint to clear all mission-local keywords of a contained {@link Mission#contents() resource}.
     */
    @Nonnull
    public ClearMissionResourceKeyword clearMissionResourceKeyword(@Nonnull String missionName, @Nonnull String hash)
    {
        return interceptClientUid(new ClearMissionResourceKeyword(missionApi, missionName, hash));
    }

    /**
     * @param missionName Name of the mission to create a token for.
     *
     * @return Endpoint to create a read-only access token to a {@link Mission}.
     */
    @Nonnull
    public CreateMissionReadAccessToken createMissionReadAccessToken(@Nonnull String missionName)
    {
        return interceptClientUid(new CreateMissionReadAccessToken(missionApi, missionName));
    }

    /**
     * @param missionName Name of the mission to get subscription info for.
     *
     * @return Endpoint to get the {@link MissionSubscription} of a {@link Mission} for the current user.
     */
    @Nonnull
    public GetCurrentMissionSubscription getCurrentMissionSubscription(@Nonnull String missionName)
    {
        return interceptClientUid(new GetCurrentMissionSubscription(missionApi, missionName));
    }

    /**
     * @param missionGuid Guid of the mission to get subscription info for.
     *
     * @return Endpoint to get the {@link MissionSubscription} of a {@link Mission} for the current user.
     * @since 5.7
     */
    @Nonnull
    public GetCurrentMissionSubscriptionByGuid getCurrentMissionSubscriptionByGuid(@Nonnull String missionGuid)
    {
        return interceptClientUid(new GetCurrentMissionSubscriptionByGuid(missionApi, missionGuid));
    }

    /**
     * @param missionName Name of the mission to create subscription for.
     *
     * @return Endpoint to create a new {@link MissionSubscription} to a {@link Mission}.
     */
    @Nonnull
    public CreateMissionSubscription createMissionSubscription(@Nonnull String missionName)
    {
        return interceptClientUid(new CreateMissionSubscription(missionApi, missionName));
    }

    /**
     * @param missionGuid Guid of the mission to create subscription for.
     *
     * @return Endpoint to create a new {@link MissionSubscription} to a {@link Mission}.
     * @since 5.7
     */
    @Nonnull
    public CreateMissionSubscriptionByGuid createMissionSubscriptionByGuid(@Nonnull String missionGuid)
    {
        return interceptClientUid(new CreateMissionSubscriptionByGuid(missionApi, missionGuid));
    }

    /**
     * @param missionName Name of the mission to remove a subscription from.
     *
     * @return Endpoint to remove a {@link MissionSubscription} from a {@link Mission}.
     */
    @Nonnull
    public RemoveMissionSubscription removeMissionSubscription(@Nonnull String missionName)
    {
        return interceptClientUid(new RemoveMissionSubscription(missionApi, missionName));
    }

    /**
     * @param missionGuid Guid of the mission to remove a subscription from.
     *
     * @return Endpoint to remove a {@link MissionSubscription} from a {@link Mission}.
     * @since 5.7
     */
    @Nonnull
    public RemoveMissionSubscriptionByGuid removeMissionSubscriptionByGuid(@Nonnull String missionGuid)
    {
        return interceptClientUid(new RemoveMissionSubscriptionByGuid(missionApi, missionGuid));
    }

    /**
     * @param missionName Name of the mission to update subscriptions of.
     * @param subscriptions Subscriptions to update.
     * Each subscription must declare both {@link MissionSubscription#clientUid()} and {@link MissionSubscription#role()}.
     *
     * @return Endpoint to update roles of some given {@link MissionSubscription} instances.
     */
    @Nonnull
    public SetMissionSubscriptionRoles setMissionSubscriptionRoles(@Nonnull String missionName,
                                                                   @Nonnull List<MissionSubscription> subscriptions)
    {
        return interceptClientUid(new SetMissionSubscriptionRoles(missionApi, missionName, subscriptions));
    }

    /**
     * @param missionGuid Guid of the mission to update subscriptions of.
     * @param subscriptions Subscriptions to update.
     * Each subscription must declare both {@link MissionSubscription#clientUid()} and {@link MissionSubscription#role()}.
     *
     * @return Endpoint to update roles of some given {@link MissionSubscription} instances.
     * @since 5.7
     */
    @Nonnull
    public SetMissionSubscriptionRolesByGuid setMissionSubscriptionRolesByGuid(@Nonnull String missionGuid,
                                                                               @Nonnull List<MissionSubscription> subscriptions)
    {
        return interceptClientUid(new SetMissionSubscriptionRolesByGuid(missionApi, missionGuid, subscriptions));
    }

    /**
     * @param missionName Name of the mission to get subscribers of.
     *
     * @return Endpoint to get a list of all {@link ClientEndpoint#uid() contacts} subscribed to a given mission.
     */
    @Nonnull
    public GetMissionSubscribers getMissionSubscribers(@Nonnull String missionName)
    {
        return interceptClientUid(new GetMissionSubscribers(missionApi, missionName));
    }

    /**
     * @param missionGuid Guid of the mission to get subscribers of.
     *
     * @return Endpoint to get a list of all {@link ClientEndpoint#uid() contacts} subscribed to a given mission.
     * @since 5.7
     */
    @Nonnull
    public GetMissionSubscribersByGuid getMissionSubscribersByGuid(@Nonnull String missionGuid)
    {
        return interceptClientUid(new GetMissionSubscribersByGuid(missionApi, missionGuid));
    }

    /**
     * @return Endpoint to get a list of all {@link ClientEndpoint#uid() contacts} subscribed to all missions.
     */
    @Nonnull
    public GetAllMissionSubscribers getAllMissionSubscribers()
    {
        return interceptClientUid(new GetAllMissionSubscribers(missionApi));
    }

    /**
     * @param missionName Name of the mission to get subscriptions of.
     *
     * @return Endpoint to get a list of all {@link MissionSubscription subscriptions} to a given mission.
     */
    @Nonnull
    public GetMissionSubscriptions getMissionSubscriptions(@Nonnull String missionName)
    {
        return interceptClientUid(new GetMissionSubscriptions(missionApi, missionName));
    }

    /**
     * @param missionGuid Guid of the mission to get subscriptions of.
     *
     * @return Endpoint to get a list of all {@link MissionSubscription subscriptions} to a given mission.
     * @since 5.7
     */
    @Nonnull
    public GetMissionSubscriptionsByGuid getMissionSubscriptionsByGuid(@Nonnull String missionGuid)
    {
        return interceptClientUid(new GetMissionSubscriptionsByGuid(missionApi, missionGuid));
    }

    /**
     * @param missionName Name of the mission to get the role within.
     *
     * @return Endpoint to get the current user's role within a {@link Mission}.
     */
    @Nonnull
    public GetMissionRole getMissionRole(@Nonnull String missionName)
    {
        return interceptClientUid(new GetMissionRole(missionApi, missionName));
    }

    /**
     * @param missionName Guid of the mission to get the role within.
     *
     * @return Endpoint to get the current user's role within a {@link Mission}.
     * @since 5.7
     */
    @Nonnull
    public GetMissionRoleByGuid getMissionRoleByGuid(@Nonnull String missionName)
    {
        return interceptClientUid(new GetMissionRoleByGuid(missionApi, missionName));
    }

    /**
     * @param missionName Name of the mission to role within.
     *
     * @return Endpoint to update a user's role within a {@link Mission}.
     */
    @Nonnull
    public SetMissionRole setMissionRole(@Nonnull String missionName)
    {
        return interceptClientUid(new SetMissionRole(missionApi, missionName));
    }

    /**
     * @param missionGuid Guid of the mission to role within.
     *
     * @return Endpoint to update a user's role within a {@link Mission}.
     * @since 5.7
     */
    @Nonnull
    public SetMissionRoleByGuid setMissionRoleByGuid(@Nonnull String missionGuid)
    {
        return interceptClientUid(new SetMissionRoleByGuid(missionApi, missionGuid));
    }

    /**
     * @return Endpoint to get the list of all {@link Mission missions} a user is invited to.
     */
    @Nonnull
    public GetMissionInvitationsForUser getMissionInvitationsForUser()
    {
        return interceptClientUid(new GetMissionInvitationsForUser(missionApi));
    }

    /**
     * @param missionName Name of the mission to get the invitations to.
     *
     * @return Endpoint to get the list of all invites to a {@link Mission}.
     */
    @Nonnull
    public GetMissionInvitations getMissionInvitations(@Nonnull String missionName)
    {
        return interceptClientUid(new GetMissionInvitations(missionApi, missionName));
    }

    /**
     * @param missionGuid Guid of the mission to get the invitations to.
     *
     * @return Endpoint to get the list of all invites to a {@link Mission}.
     * @since 5.7
     */
    @Nonnull
    public GetMissionInvitationsByGuid getMissionInvitationsByGuid(@Nonnull String missionGuid)
    {
        return interceptClientUid(new GetMissionInvitationsByGuid(missionApi, missionGuid));
    }

    /**
     * @param missionName Name of the mission to create invitations for.
     * @param invitations Invitations to send to the server.
     *
     * @return Endpoint to publish a list of {@link MissionInvitation invitations} to the server.
     */
    @Nonnull
    public SendMissionInvitations sendMissionInvitations(@Nonnull String missionName, @Nonnull List<MissionInvitation> invitations)
    {
        return interceptClientUid(new SendMissionInvitations(missionApi, missionName, invitations));
    }

    /**
     * @param missionGuid Guid of the mission to create invitations for.
     * @param invitations Invitations to send to the server.
     *
     * @return Endpoint to publish a list of {@link MissionInvitation invitations} to the server.
     * @since 5.7
     */
    @Nonnull
    public SendMissionInvitationsByGuid sendMissionInvitationsByGuid(@Nonnull String missionGuid, @Nonnull List<MissionInvitation> invitations)
    {
        return interceptClientUid(new SendMissionInvitationsByGuid(missionApi, missionGuid, invitations));
    }

    /**
     * @param missionName Name of the mission to create an invitation for.
     * @param type Invitation type.
     * @param invitee {@link ClientEndpoint#uid() Unique id} of contact to invite.
     *
     * @return Endpoint to publish a {@link MissionInvitation} to the server.
     */
    @Nonnull
    public SendMissionInvitation sendMissionInvitation(@Nonnull String missionName, @Nonnull InvitationType type,
                                                       @Nonnull String invitee)
    {
        return interceptClientUid(new SendMissionInvitation(missionApi, missionName, type, invitee));
    }

    /**
     * @param missionGuid Guid of the mission to create an invitation for.
     * @param type Invitation type.
     * @param invitee {@link ClientEndpoint#uid() Unique id} of contact to invite.
     *
     * @return Endpoint to publish a {@link MissionInvitation} to the server.
     * @since 5.7
     */
    @Nonnull
    public SendMissionInvitationByGuid sendMissionInvitationByGuid(@Nonnull String missionGuid, @Nonnull InvitationType type,
                                                                   @Nonnull String invitee)
    {
        return interceptClientUid(new SendMissionInvitationByGuid(missionApi, missionGuid, type, invitee));
    }

    /**
     * @param missionName Name of the mission to remove an invitation from.
     * @param type Invitation type.
     * @param invitee {@link ClientEndpoint#uid() Unique id} of contact to uninvite.
     *
     * @return Endpoint to retract a {@link MissionInvitation}.
     */
    @Nonnull
    public RemoveMissionInvitation removeMissionInvitation(@Nonnull String missionName, @Nonnull InvitationType type,
                                                           @Nonnull String invitee)
    {
        return interceptClientUid(new RemoveMissionInvitation(missionApi, missionName, type, invitee));
    }

    /**
     * @param missionGuid Guid of the mission to remove an invitation from.
     * @param type Invitation type.
     * @param invitee {@link ClientEndpoint#uid() Unique id} of contact to uninvite.
     *
     * @return Endpoint to retract a {@link MissionInvitation}.
     * @since 5.7
     */
    @Nonnull
    public RemoveMissionInvitationByGuid removeMissionInvitationByGuid(@Nonnull String missionGuid, @Nonnull InvitationType type,
                                                                       @Nonnull String invitee)
    {
        return interceptClientUid(new RemoveMissionInvitationByGuid(missionApi, missionGuid, type, invitee));
    }

    /**
     * @param entry Entry to publish.
     *
     * @return Endpoint to create a {@link LogEntry} instance on the server.
     */
    @Nonnull
    public CreateLogEntry createLogEntry(@Nonnull LogEntry entry)
    {
        return interceptClientUid(new CreateLogEntry(missionApi, entry));
    }

    /**
     * @param entry Entry to publish.
     *
     * @return Endpoint to update a {@link LogEntry} instance on the server.
     */
    @Nonnull
    public UpdateLogEntry updateLogEntry(@Nonnull LogEntry entry)
    {
        return interceptClientUid(new UpdateLogEntry(missionApi, entry));
    }

    /**
     * @param id The {@link LogEntry#entryUid()} to look-up.
     *
     * @return Endpoint to get a {@link LogEntry} instance by its ID.
     */
    @Nonnull
    public GetLogEntry getLogEntry(@Nonnull String id)
    {
        return interceptClientUid(new GetLogEntry(missionApi, id));
    }

    /**
     * @param missionName Name of the mission to get the log entries of.
     *
     * @return Endpoint to get multiple {@link LogEntry} instances belonging to a {@link Mission}.
     */
    @Nonnull
    public GetLogEntries getLogEntries(@Nonnull String missionName)
    {
        return interceptClientUid(new GetLogEntries(missionApi, missionName));
    }

    /**
     * @return Endpoint to get all {@link LogEntry} instances across all missions.
     */
    @Nonnull
    public GetAllLogEntries getAllLogEntries()
    {
        return interceptClientUid(new GetAllLogEntries(missionApi));
    }

    /**
     * @param id The {@link LogEntry#entryUid()} to remove.
     *
     * @return Endpoint to remove a {@link LogEntry} instance by its ID.
     */
    @Nonnull
    public RemoveLogEntry removeLogEntry(@Nonnull String id)
    {
        return interceptClientUid(new RemoveLogEntry(missionApi, id));
    }

    /**
     * @param missionName Name of the mission to get the cot events of.
     *
     * @return Endpoint to get the latest CoT events as an XML string from a {@link Mission}.
     */
    @Nonnull
    public GetLatestMissionCotEvents getLatestMissionCotEvents(@Nonnull String missionName)
    {
        return interceptClientUid(new GetLatestMissionCotEvents(missionApi, missionName));
    }

    /**
     * @param missionGuid Guid of the mission to get the cot events of.
     *
     * @return Endpoint to get the latest CoT events as an XML string from a {@link Mission}.
     * @since 5.7
     */
    @Nonnull
    public GetLatestMissionCotEventsByGuid getLatestMissionCotEventsByGuid(@Nonnull String missionGuid)
    {
        return interceptClientUid(new GetLatestMissionCotEventsByGuid(missionApi, missionGuid));
    }

    /**
     * @param missionName Name of the mission to get the remote subscriptions of.
     *
     * @return Endpoint to get the associated {@link RemoteSubscription} items attached to a {@link Mission}.
     */
    @Nonnull
    public GetRemoteSubscriptions getRemoteSubscriptions(@Nonnull String missionName)
    {
        return interceptClientUid(new GetRemoteSubscriptions(missionApi, missionName));
    }

    /**
     * @param missionGuid Guid of the mission to get the remote subscriptions of.
     *
     * @return Endpoint to get the associated {@link RemoteSubscription} items attached to a {@link Mission}.
     * @since 5.7
     */
    @Nonnull
    public GetRemoteSubscriptionsByGuid getRemoteSubscriptionsByGuid(@Nonnull String missionGuid)
    {
        return interceptClientUid(new GetRemoteSubscriptionsByGuid(missionApi, missionGuid));
    }

    /**
     * @param missionName Name of the mission to hold the layer.
     * @param layer Layer to publish on the server.
     *
     * @return Endpoint to create a {@link MapLayer} instance on the server.
     */
    @Nonnull
    public CreateMapLayer createMapLayer(@Nonnull String missionName, @Nonnull MapLayer layer)
    {
        return interceptClientUid(new CreateMapLayer(missionApi, missionName, layer));
    }

    /**
     * @param missionGuid Guid of the mission to hold the layer.
     * @param layer Layer to publish on the server.
     *
     * @return Endpoint to create a {@link MapLayer} instance on the server.
     * @since 5.7
     */
    @Nonnull
    public CreateMapLayerByGuid createMapLayerByGuid(@Nonnull String missionGuid, @Nonnull MapLayer layer)
    {
        return interceptClientUid(new CreateMapLayerByGuid(missionApi, missionGuid, layer));
    }

    /**
     * @param missionName Name of the mission that holds the layer.
     * @param layer Layer to update on the server. Must share a {@link MapLayer#uid()} with an existing layer.
     *
     * @return Endpoint to update a {@link MapLayer} instance on the server.
     */
    @Nonnull
    public UpdateMapLayer updateMapLayer(@Nonnull String missionName, @Nonnull MapLayer layer)
    {
        return interceptClientUid(new UpdateMapLayer(missionApi, missionName, layer));
    }

    /**
     * @param missionGuid Guid of the mission that holds the layer.
     * @param layer Layer to update on the server. Must share a {@link MapLayer#uid()} with an existing layer.
     *
     * @return Endpoint to update a {@link MapLayer} instance on the server.
     * @since 5.7
     */
    @Nonnull
    public UpdateMapLayerByGuid updateMapLayerByGuid(@Nonnull String missionGuid, @Nonnull MapLayer layer)
    {
        return interceptClientUid(new UpdateMapLayerByGuid(missionApi, missionGuid, layer));
    }

    /**
     * @param missionName Name of the mission that holds the layer.
     * @param layerUid {@link MapLayer#uid() Unique identifier} of a layer to remove from the mission.
     *
     * @return Endpoint to remove a {@link MapLayer} instance on the server.
     */
    @Nonnull
    public RemoveMapLayer removeMapLayer(@Nonnull String missionName, @Nonnull String layerUid)
    {
        return interceptClientUid(new RemoveMapLayer(missionApi, missionName, layerUid));
    }

    /**
     * @param missionGuid Guid of the mission that holds the layer.
     * @param layerUid {@link MapLayer#uid() Unique identifier} of a layer to remove from the mission.
     *
     * @return Endpoint to remove a {@link MapLayer} instance on the server.
     * @since 5.7
     */
    @Nonnull
    public RemoveMapLayerByGuid removeMapLayerByGuid(@Nonnull String missionGuid, @Nonnull String layerUid)
    {
        return interceptClientUid(new RemoveMapLayerByGuid(missionApi, missionGuid, layerUid));
    }

    /**
     * @param missionName Name of the mission that contains layers to look up.
     *
     * @return Endpoint to remove a {@link MapLayer} instance on the server.
     */
    @Nonnull
    public GetMissionLayers getMissionLayers(@Nonnull String missionName)
    {
        return interceptClientUid(new GetMissionLayers(missionApi, missionName));
    }

    /**
     * @param missionGuid Guid of the mission that contains layers to look up.
     *
     * @return Endpoint to remove a {@link MapLayer} instance on the server.
     * @since 5.7
     */
    @Nonnull
    public GetMissionLayersByGuid getMissionLayersByGuid(@Nonnull String missionGuid)
    {
        return interceptClientUid(new GetMissionLayersByGuid(missionApi, missionGuid));
    }

    /**
     * @param missionName Name of the mission that holds the layer.
     * @param layerUid {@link MissionLayer#uid() Unique identifier} of a layer to get.
     *
     * @return Endpoint to get a {@link MissionLayer} instance from the server.
     */
    @Nonnull
    public GetMissionLayer getMissionLayer(@Nonnull String missionName, @Nonnull String layerUid)
    {
        return interceptClientUid(new GetMissionLayer(missionApi, missionName, layerUid));
    }

    /**
     * @param missionGuid Guid of the mission that holds the layer.
     * @param layerUid {@link MissionLayer#uid() Unique identifier} of a layer to get.
     *
     * @return Endpoint to get a {@link MissionLayer} instance from the server.
     * @since 5.7
     */
    @Nonnull
    public GetMissionLayerByGuid getMissionLayerByGuid(@Nonnull String missionGuid, @Nonnull String layerUid)
    {
        return interceptClientUid(new GetMissionLayerByGuid(missionApi, missionGuid, layerUid));
    }

    /**
     * @param missionName Name of the mission that holds the layer.
     *
     * @return Endpoint to create a {@link MissionLayer} instance from the server.
     */
    @Nonnull
    public CreateMissionLayer createMissionLayer(@Nonnull String missionName)
    {
        return interceptClientUid(new CreateMissionLayer(missionApi, missionName));
    }

    /**
     * @param missionGuid Guid of the mission that holds the layer.
     *
     * @return Endpoint to create a {@link MissionLayer} instance from the server.
     * @since 5.7
     */
    @Nonnull
    public CreateMissionLayerByGuid createMissionLayerByGuid(@Nonnull String missionGuid)
    {
        return interceptClientUid(new CreateMissionLayerByGuid(missionApi, missionGuid));
    }

    /**
     * @param missionName Name of the mission containing the layer.
     * @param layerUid {@link MissionLayer#uid() Unique identifier} of a layer to update.
     *
     * @return Endpoint to update a {@link MissionLayer} instance.
     */
    @Nonnull
    public SetMissionLayerName setMissionLayerName(@Nonnull String missionName, @Nonnull String layerUid)
    {
        return interceptClientUid(new SetMissionLayerName(missionApi, missionName, layerUid));
    }

    /**
     * @param missionGuid Guid of the mission containing the layer.
     * @param layerUid {@link MissionLayer#uid() Unique identifier} of a layer to update.
     *
     * @return Endpoint to update a {@link MissionLayer} instance.
     * @since 5.7
     */
    @Nonnull
    public SetMissionLayerNameByGuid setMissionLayerNameByGuid(@Nonnull String missionGuid, @Nonnull String layerUid)
    {
        return interceptClientUid(new SetMissionLayerNameByGuid(missionApi, missionGuid, layerUid));
    }

    /**
     * @param missionName Name of the mission containing the layer.
     * @param layerUid {@link MissionLayer#uid() Unique identifier} of a layer to update.
     *
     * @return Endpoint to update a {@link MissionLayer} instance.
     */
    @Nonnull
    public SetMissionLayerPosition setMissionLayerPosition(@Nonnull String missionName, @Nonnull String layerUid)
    {
        return interceptClientUid(new SetMissionLayerPosition(missionApi, missionName, layerUid));
    }

    /**
     * @param missionGuid Guid of the mission containing the layer.
     * @param layerUid {@link MissionLayer#uid() Unique identifier} of a layer to update.
     *
     * @return Endpoint to update a {@link MissionLayer} instance.
     * @since 5.7
     */
    @Nonnull
    public SetMissionLayerPositionByGuid setMissionLayerPositionByGuid(@Nonnull String missionGuid, @Nonnull String layerUid)
    {
        return interceptClientUid(new SetMissionLayerPositionByGuid(missionApi, missionGuid, layerUid));
    }

    /**
     * @param missionName Name of the mission containing the layer.
     *
     * @return Endpoint to update a {@link MissionLayer} instance.
     */
    @Nonnull
    public SetMissionLayerParent setMissionLayerParent(@Nonnull String missionName)
    {
        return interceptClientUid(new SetMissionLayerParent(missionApi, missionName));
    }

    /**
     * @param missionGuid Guid of the mission containing the layer.
     *
     * @return Endpoint to update a {@link MissionLayer} instance.
     * @since 5.7
     */
    @Nonnull
    public SetMissionLayerParentByGuid setMissionLayerParentByGuid(@Nonnull String missionGuid)
    {
        return interceptClientUid(new SetMissionLayerParentByGuid(missionApi, missionGuid));
    }

    /**
     * @param missionName Name of the mission containing the layer.
     *
     * @return Endpoint to remove a {@link MissionLayer} instance.
     */
    @Nonnull
    public RemoveMissionLayer removeMissionLayer(@Nonnull String missionName)
    {
        return interceptClientUid(new RemoveMissionLayer(missionApi, missionName));
    }

    /**
     * @param missionGuid Guid of the mission containing the layer.
     *
     * @return Endpoint to remove a {@link MissionLayer} instance.
     * @since 5.7
     */
    @Nonnull
    public RemoveMissionLayerByGuid removeMissionLayerByGuid(@Nonnull String missionGuid)
    {
        return interceptClientUid(new RemoveMissionLayerByGuid(missionApi, missionGuid));
    }

    private <T, E extends IEndpointCaller<T>> E interceptClientUid(@Nonnull E endpoint)
    {
        return IClientAssociatedEndpointCaller.interceptClientUid(endpoint, clientUid);
    }
}
