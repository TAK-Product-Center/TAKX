package gov.tak.api.comms.takserver.mission.feign;

import feign.HeaderMap;
import feign.Headers;
import feign.Param;
import feign.QueryMap;
import feign.RequestLine;
import feign.Response;
import gov.tak.api.comms.takserver.mission.MissionEndpoints;
import gov.tak.api.comms.takserver.mission.TakServerApiClient;
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
import gov.tak.api.comms.takserver.mission.call.mission.SetMissionParentByGuid;
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
import gov.tak.api.comms.takserver.mission.data.LogEntry;
import gov.tak.api.comms.takserver.mission.data.MapLayer;
import gov.tak.api.comms.takserver.mission.data.Mission;
import gov.tak.api.comms.takserver.mission.data.MissionChange;
import gov.tak.api.comms.takserver.mission.data.MissionContentKeys;
import gov.tak.api.comms.takserver.mission.data.MissionFeed;
import gov.tak.api.comms.takserver.mission.data.MissionInvitation;
import gov.tak.api.comms.takserver.mission.data.MissionLayer;
import gov.tak.api.comms.takserver.mission.data.MissionRole;
import gov.tak.api.comms.takserver.mission.data.MissionSubEntry;
import gov.tak.api.comms.takserver.mission.data.MissionSubscription;
import gov.tak.api.comms.takserver.mission.data.RemoteSubscription;
import gov.tak.api.comms.takserver.mission.data.Resource;
import gov.takx.api.annotation.NoPluginImplementations;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import java.util.List;
import java.util.Map;

/**
 * REST interface modeling the Mission API from TAK Server.
 * <p>
 * For internal use, see {@link MissionEndpoints},
 * accessible via {@link TakServerApiClient}
 *
 * @implNote Last updated 1/13/2026 reflecting TAK Server 5.7 API
 * @since 3.5
 */
@SuppressWarnings("unused")
@NoPluginImplementations
public interface FeignMissionApi
{
    /**
     * @param headers Additional request headers.
     * @param queryMap Map of query names to values.
     *
     * @return List of all available {@link Mission} instances.
     *
     * @see GetAllMissions
     */
    @Nonnull
    @RequestLine("GET /missions")
    List<Mission> getAllMissions(@Nonnull @HeaderMap Map<String, Object> headers,
                                 @Nonnull @QueryMap Map<String, Object> queryMap);

    /**
     * @param headers Additional request headers.
     * @param queryMap Map of query names to values.
     * @param missionName Name of the mission to get.
     *
     * @return Instance of mission by name, wrapped in a list.
     *
     * @see GetMission
     */
    @Nonnull
    @RequestLine("GET /missions/{missionName}")
    List<Mission> getMission(@Nonnull @HeaderMap Map<String, Object> headers,
                             @Nonnull @QueryMap Map<String, Object> queryMap,
                             @Nonnull @Param("missionName") String missionName);

    /**
     * @param headers Additional request headers.
     * @param queryMap Map of query names to values.
     * @param missionGuid Guid of the mission to get.
     *
     * @return Instance of mission by name, wrapped in a list.
     *
     * @see GetMission
     * @since 5.7
     */
    @Nonnull
    @RequestLine("GET /missions/guid/{missionGuid}")
    List<Mission> getMissionByGuid(@Nonnull @HeaderMap Map<String, Object> headers,
                                   @Nonnull @QueryMap Map<String, Object> queryMap,
                                   @Nonnull @Param("missionGuid") String missionGuid);

    /**
     * @param headers Additional request headers.
     * @param queryMap Map of query names to values.
     * @param missionName Name of the mission to create.
     *
     * @return Created mission from server, wrapped in a list.
     *
     * @see CreateMission
     */
    @Nonnull
    @RequestLine("PUT /missions/{missionName}")
    List<Mission> createMission(@Nonnull @HeaderMap Map<String, Object> headers,
                                @Nonnull @QueryMap Map<String, Object> queryMap,
                                @Nonnull @Param("missionName") String missionName);

    /**
     * @param headers Additional request headers.
     * @param queryMap Map of query names to values.
     * @param missionName Name of the mission to create.
     * @param missionJson Mission json representation. Can be used as an alternative to query map entries.
     *
     * @return Created mission from server, wrapped in a list.
     *
     * @see CreateMission
     */
    @Nonnull
    @Headers("Content-Type: application/json")
    @RequestLine("PUT /missions/{missionName}")
    List<Mission> createMission(@Nonnull @HeaderMap Map<String, Object> headers,
                                @Nonnull @QueryMap Map<String, Object> queryMap,
                                @Nonnull @Param("missionName") String missionName,
                                @Nullable String missionJson);

    /**
     * @param headers Additional request headers.
     * @param queryMap Map of query names to values.
     * @param missionName Name of the mission to create.
     * @param mission Mission instance to publish.
     *
     * @return Created mission from server, wrapped in a list.
     *
     * @see CreateMission
     */
    @Nonnull
    @Headers("Content-Type: application/json")
    @RequestLine("PUT /missions/{missionName}")
    List<Mission> createMission(@Nonnull @HeaderMap Map<String, Object> headers,
                                @Nonnull @QueryMap Map<String, Object> queryMap,
                                @Nonnull @Param("missionName") String missionName,
                                @Nonnull Mission mission);

    /**
     * @param headers Additional request headers.
     * @param queryMap Map of query names to values.
     * @param missionName Name of the mission to copy.
     *
     * @return Copied mission from server.
     *
     * @see CopyMission
     */
    @Nonnull
    @RequestLine("PUT /missions/{missionName}/copy")
    List<Mission> copyMission(@Nonnull @HeaderMap Map<String, Object> headers,
                              @Nonnull @QueryMap Map<String, Object> queryMap,
                              @Nonnull @Param("missionName") String missionName);

    /**
     * @param headers Additional request headers.
     * @param queryMap Map of query names to values.
     * @param missionName Name of the mission to delete.
     *
     * @return Deleted mission from server.
     *
     * @see RemoveMission
     */
    @Nonnull
    @RequestLine("DELETE /missions/{missionName}")
    List<Mission> removeMission(@Nonnull @HeaderMap Map<String, Object> headers,
                                @Nonnull @QueryMap Map<String, Object> queryMap,
                                @Nonnull @Param("missionName") String missionName);

    /**
     * @param headers Additional request headers.
     * @param queryMap Map of query names to values.
     *
     * @return Deleted mission from server.
     *
     * @implNote The deleted mission is specified as a query parameter.
     * @see RemoveMissionByGuid
     * @since 5.7
     */
    @Nonnull
    @RequestLine("DELETE /missions")
    List<Mission> removeMissionByGuid(@Nonnull @HeaderMap Map<String, Object> headers,
                                      @Nonnull @QueryMap Map<String, Object> queryMap);

    /**
     * @param headers Additional request headers.
     * @param queryMap Map of query names to values.
     * @param missionName Name of the mission to get parent of.
     *
     * @return Parent mission on server.
     *
     * @see GetMissionParent
     */
    @Nonnull
    @RequestLine("GET /missions/{missionName}/parent")
    Mission getMissionParent(@Nonnull @HeaderMap Map<String, Object> headers,
                             @Nonnull @QueryMap Map<String, Object> queryMap,
                             @Nonnull @Param("missionName") String missionName);

    /**
     * @param headers Additional request headers.
     * @param queryMap Map of query names to values.
     * @param missionGuid Guid of the mission to get parent of.
     *
     * @return Parent mission on server.
     *
     * @see GetMissionParentByGuid
     * @since 5.7
     */
    @Nonnull
    @RequestLine("GET /missions/guid/{missionGuid}/parent")
    Mission getMissionParentByGuid(@Nonnull @HeaderMap Map<String, Object> headers,
                                   @Nonnull @QueryMap Map<String, Object> queryMap,
                                   @Nonnull @Param("missionGuid") String missionGuid);

    /**
     * @param headers Additional request headers.
     * @param queryMap Map of query names to values.
     * @param missionName Name of the mission to update.
     * @param parentMissionName New parent mission name.
     *
     * @see SetMissionParent
     */
    @RequestLine("PUT /missions/{missionName}/parent/{parentMissionName}")
    void setMissionParent(@Nonnull @HeaderMap Map<String, Object> headers,
                          @Nonnull @QueryMap Map<String, Object> queryMap,
                          @Nonnull @Param("missionName") String missionName,
                          @Nonnull @Param("parentMissionName") String parentMissionName);

    /**
     * @param headers Additional request headers.
     * @param queryMap Map of query names to values.
     * @param missionGuid Guid of the mission to update.
     * @param parentMissionGuid New parent mission guid.
     *
     * @see SetMissionParentByGuid
     * @since 5.7
     */
    @RequestLine("PUT /missions/guid/{missionGuid}/parent/{parentMissionGuid}")
    void setMissionParentByGuid(@Nonnull @HeaderMap Map<String, Object> headers,
                                @Nonnull @QueryMap Map<String, Object> queryMap,
                                @Nonnull @Param("missionGuid") String missionGuid,
                                @Nonnull @Param("parentMissionGuid") String parentMissionGuid);

    /**
     * @param headers Additional request headers.
     * @param queryMap Map of query names to values.
     * @param missionName Name of the mission to update.
     *
     * @see RemoveMissionParent
     */
    @RequestLine("DELETE /missions/{missionName}/parent")
    void removeMissionParent(@Nonnull @HeaderMap Map<String, Object> headers,
                             @Nonnull @QueryMap Map<String, Object> queryMap,
                             @Nonnull @Param("missionName") String missionName);

    /**
     * @param headers Additional request headers.
     * @param queryMap Map of query names to values.
     * @param missionGuid Guid of the mission to update.
     *
     * @see RemoveMissionParentByGuid
     * @since 5.7
     */
    @RequestLine("DELETE /missions/guid/{missionGuid}/parent")
    void removeMissionParentByGuid(@Nonnull @HeaderMap Map<String, Object> headers,
                                   @Nonnull @QueryMap Map<String, Object> queryMap,
                                   @Nonnull @Param("missionGuid") String missionGuid);

    /**
     * @param headers Additional request headers.
     * @param queryMap Map of query names to values.
     * @param missionName Name of the mission to get children of.
     *
     * @return List of child missions.
     *
     * @see GetMissionChildren
     */
    @Nonnull
    @RequestLine("GET /missions/{missionName}/children")
    List<Mission> getChildMissions(@Nonnull @HeaderMap Map<String, Object> headers,
                                   @Nonnull @QueryMap Map<String, Object> queryMap,
                                   @Nonnull @Param("missionName") String missionName);

    /**
     * @param headers Additional request headers.
     * @param queryMap Map of query names to values.
     * @param missionGuid Guid of the mission to get children of.
     *
     * @return List of child missions.
     *
     * @see GetMissionChildrenByGuid
     * @since 5.7
     */
    @Nonnull
    @RequestLine("GET /missions/guid/{missionGuid}/children")
    List<Mission> getChildMissionsByGuid(@Nonnull @HeaderMap Map<String, Object> headers,
                                         @Nonnull @QueryMap Map<String, Object> queryMap,
                                         @Nonnull @Param("missionGuid") String missionGuid);

    /**
     * @param headers Additional request headers.
     * @param queryMap Map of query names to values.
     * @param missionName Name of the mission to update.
     *
     * @see SetMissionPassword
     */
    @RequestLine("PUT /missions/{missionName}/password")
    void setMissionPassword(@Nonnull @HeaderMap Map<String, Object> headers,
                            @Nonnull @QueryMap Map<String, Object> queryMap,
                            @Nonnull @Param("missionName") String missionName);

    /**
     * @param headers Additional request headers.
     * @param queryMap Map of query names to values.
     * @param missionGuid Guid of the mission to update.
     *
     * @see SetMissionPasswordByGuid
     * @since 5.7
     */
    @RequestLine("PUT /missions/guid/{missionGuid}/password")
    void setMissionPasswordByGuid(@Nonnull @HeaderMap Map<String, Object> headers,
                                  @Nonnull @QueryMap Map<String, Object> queryMap,
                                  @Nonnull @Param("missionGuid") String missionGuid);

    /**
     * @param headers Additional request headers.
     * @param queryMap Map of query names to values.
     * @param missionName Name of the mission to update.
     *
     * @see RemoveMissionPassword
     */
    @RequestLine("DELETE /missions/{missionName}/password")
    void removeMissionPassword(@Nonnull @HeaderMap Map<String, Object> headers,
                               @Nonnull @QueryMap Map<String, Object> queryMap,
                               @Nonnull @Param("missionName") String missionName);

    /**
     * @param headers Additional request headers.
     * @param queryMap Map of query names to values.
     * @param missionGuid Guid of the mission to update.
     *
     * @see RemoveMissionPasswordByGuid
     * @since 5.7
     */
    @RequestLine("DELETE /missions/guid/{missionGuid}/password")
    void removeMissionPasswordByGuid(@Nonnull @HeaderMap Map<String, Object> headers,
                                     @Nonnull @QueryMap Map<String, Object> queryMap,
                                     @Nonnull @Param("missionGuid") String missionGuid);

    /**
     * @param headers Additional request headers.
     * @param queryMap Map of query names to values.
     * @param missionName Name of the mission to update.
     *
     * @see SetMissionExpiration
     */
    @RequestLine("PUT /missions/{missionName}/expiration")
    void setMissionExpiration(@Nonnull @HeaderMap Map<String, Object> headers,
                              @Nonnull @QueryMap Map<String, Object> queryMap,
                              @Nonnull @Param("missionName") String missionName);

    /**
     * @param headers Additional request headers.
     * @param queryMap Map of query names to values.
     * @param missionGuid Guid of the mission to update.
     *
     * @see SetMissionExpirationByGuid
     * @since 5.7
     */
    @RequestLine("PUT /missions/guid/{missionGuid}/expiration")
    void setMissionExpirationByGuid(@Nonnull @HeaderMap Map<String, Object> headers,
                                    @Nonnull @QueryMap Map<String, Object> queryMap,
                                    @Nonnull @Param("missionGuid") String missionGuid);

    /**
     * @param headers Additional request headers.
     * @param queryMap Map of query names to values.
     * @param missionName Name of the mission to get.
     *
     * @return KML representation of mission.
     *
     * @see GetMissionKml
     */
    @Nonnull
    @RequestLine("GET /missions/{missionName}/kml")
    Response getMissionKML(@Nonnull @HeaderMap Map<String, Object> headers,
                           @Nonnull @QueryMap Map<String, Object> queryMap,
                           @Nonnull @Param("missionName") String missionName);

    /**
     * @param headers Additional request headers.
     * @param queryMap Map of query names to values.
     * @param missionGuid Guid of the mission to get.
     *
     * @return KML representation of mission.
     *
     * @see GetMissionKmlByGuid
     * @since 5.7
     */
    @Nonnull
    @RequestLine("GET /missions/guid/{missionGuid}/kml")
    Response getMissionKMLByGuid(@Nonnull @HeaderMap Map<String, Object> headers,
                                 @Nonnull @QueryMap Map<String, Object> queryMap,
                                 @Nonnull @Param("missionGuid") String missionGuid);

    /**
     * @param headers Additional request headers.
     * @param queryMap Map of query names to values.
     * @param missionName Name of the mission to update.
     * @param data External data to apply to the mission.
     *
     * @return The applies data, mirrored by the server.
     *
     * @see SetMissionExternalData
     */
    @Nonnull
    @Headers("Content-Type: application/json")
    @RequestLine("POST /missions/{missionName}/externaldata")
    ExternalMissionData setExternalMissionData(@Nonnull @HeaderMap Map<String, Object> headers,
                                               @Nonnull @QueryMap Map<String, Object> queryMap,
                                               @Nonnull @Param("missionName") String missionName,
                                               @Nonnull ExternalMissionData data);

    /**
     * @param headers Additional request headers.
     * @param queryMap Map of query names to values.
     * @param missionGuid Name of the mission to update.
     * @param data External data to apply to the mission.
     *
     * @return The applies data, mirrored by the server.
     *
     * @see SetMissionExternalDataByGuid
     * @since 5.7
     */
    @Nonnull
    @Headers("Content-Type: application/json")
    @RequestLine("POST /missions/guid/{missionGuid}/externaldata")
    ExternalMissionData setExternalMissionDataByGuid(@Nonnull @HeaderMap Map<String, Object> headers,
                                                     @Nonnull @QueryMap Map<String, Object> queryMap,
                                                     @Nonnull @Param("missionGuid") String missionGuid,
                                                     @Nonnull ExternalMissionData data);

    /**
     * @param headers Additional request headers.
     * @param queryMap Map of query names to values.
     * @param missionName Name of the mission to update.
     * @param id {@link ExternalMissionData#uid() External data uid} to remove.
     *
     * @see RemoveMissionExternalData
     */
    @RequestLine("DELETE /missions/{missionName}/externaldata/{id}")
    void removeExternalMissionData(@Nonnull @HeaderMap Map<String, Object> headers,
                                   @Nonnull @QueryMap Map<String, Object> queryMap,
                                   @Nonnull @Param("missionName") String missionName,
                                   @Nonnull @Param("id") String id);

    /**
     * @param headers Additional request headers.
     * @param queryMap Map of query names to values.
     * @param missionGuid Guid of the mission to update.
     * @param id {@link ExternalMissionData#uid() External data uid} to remove.
     *
     * @see RemoveMissionExternalDataByGuid
     * @since 5.7
     */
    @RequestLine("DELETE /missions/guid/{missionGuid}/externaldata/{id}")
    void removeExternalMissionDataByGuid(@Nonnull @HeaderMap Map<String, Object> headers,
                                         @Nonnull @QueryMap Map<String, Object> queryMap,
                                         @Nonnull @Param("missionGuid") String missionGuid,
                                         @Nonnull @Param("id") String id);

    /**
     * @param headers Additional request headers.
     * @param queryMap Map of query names to values.
     * @param missionName Name of the mission to notify.
     * @param id The {@link ExternalMissionData#uid() identifier} of data to notify of an update.
     * @param token Token assigned to the {@link MissionChange} when the remote server
     * creates a change-log entry. Otherwise, seemingly unused.
     *
     * @see NotifyChangedMissionExternalData
     */
    @RequestLine("POST /missions/{missionName}/externaldata/{id}/change")
    void notifyExternalDataChanged(@Nonnull @HeaderMap Map<String, Object> headers,
                                   @Nonnull @QueryMap Map<String, Object> queryMap,
                                   @Nonnull @Param("missionName") String missionName,
                                   @Nonnull @Param("id") String id,
                                   @Nonnull String token);

    /**
     * @param headers Additional request headers.
     * @param queryMap Map of query names to values.
     * @param missionGuid Guid of the mission to notify.
     * @param id The {@link ExternalMissionData#uid() identifier} of data to notify of an update.
     * @param token Token assigned to the {@link MissionChange} when the remote server
     * creates a change-log entry. Otherwise, seemingly unused.
     *
     * @see NotifyChangedMissionExternalDataByGuid
     * @since 5.7
     */
    @RequestLine("POST /missions/guid/{missionGuid}/externaldata/{id}/change")
    void notifyExternalDataChangedByGuid(@Nonnull @HeaderMap Map<String, Object> headers,
                                         @Nonnull @QueryMap Map<String, Object> queryMap,
                                         @Nonnull @Param("missionGuid") String missionGuid,
                                         @Nonnull @Param("id") String id,
                                         @Nonnull String token);

    /**
     * @param headers Additional request headers.
     * @param queryMap Map of query names to values.
     * @param missionName Name of the mission to update.
     *
     * @see AddFeed
     */
    @RequestLine("POST /missions/{missionName}/feed")
    void addFeed(@Nonnull @HeaderMap Map<String, Object> headers,
                 @Nonnull @QueryMap Map<String, Object> queryMap,
                 @Nonnull @Param("missionName") String missionName);

    /**
     * @param headers Additional request headers.
     * @param queryMap Map of query names to values.
     * @param missionName Name of the mission to update.
     * @param uid The {@link MissionFeed#uid()} of feed to remove.
     *
     * @see RemoveFeed
     */
    @RequestLine("DELETE /missions/{missionName}/feed/{uid}")
    void removeFeed(@Nonnull @HeaderMap Map<String, Object> headers,
                    @Nonnull @QueryMap Map<String, Object> queryMap,
                    @Nonnull @Param("missionName") String missionName,
                    @Nonnull @Param("uid") String uid);

    /**
     * @param headers Additional request headers.
     * @param queryMap Map of query names to values.
     * @param missionName Name of the mission to get as an archive.
     *
     * @return Archive <i>(ZIP)</i> of mission.
     *
     * @see GetMissionArchive
     */
    @Nonnull
    @RequestLine("GET /missions/{missionName}/archive")
    Response getMissionArchive(@Nonnull @HeaderMap Map<String, Object> headers,
                               @Nonnull @QueryMap Map<String, Object> queryMap,
                               @Nonnull @Param("missionName") String missionName);

    /**
     * @param headers Additional request headers.
     * @param queryMap Map of query names to values.
     * @param missionGuid Guid of the mission to get as an archive.
     *
     * @return Archive <i>(ZIP)</i> of mission.
     *
     * @see GetMissionArchiveByGuid
     * @since 5.7
     */
    @Nonnull
    @RequestLine("GET /missions/guid/{missionGuid}/archive")
    Response getMissionArchiveByGuid(@Nonnull @HeaderMap Map<String, Object> headers,
                                     @Nonnull @QueryMap Map<String, Object> queryMap,
                                     @Nonnull @Param("missionGuid") String missionGuid);

    // Internal note: TAK-Server on a number of occasions (it is not consistent for all operations
    //  on all models) returns the updated data model when doing an operation that changes the model.
    //  However, instead of giving you the model directly, they wrap it in a list containing just the
    //  single updated value. This is rather odd and annoying as this makes auto-generating REST
    //  clients a bit more tricky due to type-erasure on collection types.
    //  This leaves us with two options:
    //   - Use workarounds for type-erasure on lists
    //   - Return the model type, change JSON parsing to focus on the single value in the list
    //  The first option is more 'true' to the API.
    //  The second option is better for consuming developers. However, there may be some situation
    //   when the list is empty. What do we do then?

    /**
     * @param headers Additional request headers.
     * @param queryMap Map of query names to values.
     * @param missionName Name of the mission to archive.
     *
     * @return The mission that was archived, wrapped in a list.
     *
     * @see SendMissionArchive
     */
    @Nonnull
    @RequestLine("POST /missions/{missionName}/send")
    List<Mission> sendMissionArchive(@Nonnull @HeaderMap Map<String, Object> headers,
                                     @Nonnull @QueryMap Map<String, Object> queryMap,
                                     @Nonnull @Param("missionName") String missionName);

    /**
     * @param headers Additional request headers.
     * @param queryMap Map of query names to values.
     * @param missionGuid Guid of the mission to archive.
     *
     * @return The mission that was archived, wrapped in a list.
     *
     * @see SendMissionArchiveByGuid
     * @since 5.7
     */
    @Nonnull
    @RequestLine("POST /missions/guid/{missionGuid}/send")
    List<Mission> sendMissionArchiveByGuid(@Nonnull @HeaderMap Map<String, Object> headers,
                                           @Nonnull @QueryMap Map<String, Object> queryMap,
                                           @Nonnull @Param("missionGuid") String missionGuid);

    /**
     * @param headers Additional request headers.
     * @param queryMap Map of query names to values.
     * @param missionName Name of the mission to update.
     * @param keys Content keys of items to add to the mission.
     *
     * @return The mission that was updated, wrapped in a list.
     *
     * @see AddMissionContent
     */
    @Nonnull
    @Headers("Content-Type: application/json")
    @RequestLine("PUT /missions/{missionName}/contents")
    List<Mission> addMissionContent(@Nonnull @HeaderMap Map<String, Object> headers,
                                    @Nonnull @QueryMap Map<String, Object> queryMap,
                                    @Nonnull @Param("missionName") String missionName,
                                    @Nonnull MissionContentKeys keys);

    /**
     * @param headers Additional request headers.
     * @param queryMap Map of query names to values.
     * @param missionGuid Guid of the mission to update.
     * @param keys Content keys of items to add to the mission.
     *
     * @return The mission that was updated, wrapped in a list.
     *
     * @see AddMissionContentByGuid
     * @since 5.7
     */
    @Nonnull
    @Headers("Content-Type: application/json")
    @RequestLine("PUT /missions/guid/{missionGuid}/contents")
    List<Mission> addMissionContentByGuid(@Nonnull @HeaderMap Map<String, Object> headers,
                                          @Nonnull @QueryMap Map<String, Object> queryMap,
                                          @Nonnull @Param("missionGuid") String missionGuid,
                                          @Nonnull MissionContentKeys keys);

    /**
     * @param headers Additional request headers.
     * @param queryMap Map of query names to values.
     * @param missionName Name of the mission to update.
     *
     * @return The mission that was updated, wrapped in a list.
     *
     * @see RemoveMissionContent
     */
    @Nonnull
    @RequestLine("DELETE /missions/{missionName}/contents")
    List<Mission> removeMissionContent(@Nonnull @HeaderMap Map<String, Object> headers,
                                       @Nonnull @QueryMap Map<String, Object> queryMap,
                                       @Nonnull @Param("missionName") String missionName);

    /**
     * @param headers Additional request headers.
     * @param queryMap Map of query names to values.
     * @param missionGuid Guid of the mission to update.
     *
     * @return The mission that was updated, wrapped in a list.
     *
     * @see RemoveMissionContentByGuid
     * @since 5.7
     */
    @Nonnull
    @RequestLine("DELETE /missions/guid/{missionGuid}/contents")
    List<Mission> removeMissionContentByGuid(@Nonnull @HeaderMap Map<String, Object> headers,
                                             @Nonnull @QueryMap Map<String, Object> queryMap,
                                             @Nonnull @Param("missionGuid") String missionGuid);

    /**
     * @param headers Additional request headers.
     * @param queryMap Map of query names to values.
     * @param missionName Name of the mission to update.
     * @param missionPackage Raw contents of mission package ZIP archive.
     *
     * @return List of conflicts preventing the addition.
     *
     * @see AddMissionPackage
     */
    @Nonnull
    @Headers("Content-Type: application/octet-stream")
    @RequestLine("PUT /missions/{missionName}/contents/missionpackage")
    // TODO: The response is a list of conflicts, but it assigns 400 range error codes as well.
    //  - Need to make sure the 400 range error code doesn't confuse feign, and we should forcefully
    //    handle content regardless of the response code... unless there is a REAL error that prevents
    //    a normal response. The system for indicating what we should expect here is silly.
    List<MissionChange> addMissionPackage(@Nonnull @HeaderMap Map<String, Object> headers,
                                          @Nonnull @QueryMap Map<String, Object> queryMap,
                                          @Nonnull @Param("missionName") String missionName,
                                          @Nonnull byte[] missionPackage);

    /**
     * @param headers Additional request headers.
     * @param queryMap Map of query names to values.
     * @param missionName Name of the mission to get history of.
     *
     * @return Recent change history of the mission.
     *
     * @see GetMissionChanges
     */
    @Nonnull
    @RequestLine("GET /missions/{missionName}/changes")
    List<MissionChange> getMissionChanges(@Nonnull @HeaderMap Map<String, Object> headers,
                                          @Nonnull @QueryMap Map<String, Object> queryMap,
                                          @Nonnull @Param("missionName") String missionName);

    /**
     * @param headers Additional request headers.
     * @param queryMap Map of query names to values.
     * @param missionGuid Guid of the mission to get history of.
     *
     * @return Recent change history of the mission.
     *
     * @see GetMissionChangesByGuid
     * @since 5.7
     */
    @Nonnull
    @RequestLine("GET /missions/guid/{missionGuid}/changes")
    List<MissionChange> getMissionChangesByGuid(@Nonnull @HeaderMap Map<String, Object> headers,
                                                @Nonnull @QueryMap Map<String, Object> queryMap,
                                                @Nonnull @Param("missionGuid") String missionGuid);

    // Internal note: TAK-Server does not offer add-single keyword capabilities to missions.
    //  To add a single keyword, you must use the set-keywords operation with the new keyword
    //  list including the added keyword. Despite this, they offer removal of a single keyword.
    //  There is a pattern of inconsistent operations being available to different data types
    //  throughout the api, which is unfortunate.
    //  And on the note of inconsistency, when they created guid alternative endpoints for
    //  interacting with missions, they did not make any for keyword management.
    //  In fact, there are several other capability endpoints without guid alternatives.

    /**
     * @param headers Additional request headers.
     * @param queryMap Map of query names to values.
     * @param missionName Name of the mission to update.
     * @param keywords Keywords to assign to the mission.
     *
     * @return The updated mission, wrapped in a list.
     *
     * @see SetMissionKeywords
     */
    @Nonnull
    @Headers("Content-Type: application/json")
    @RequestLine("PUT /missions/{missionName}/keywords")
    List<Mission> setMissionKeywords(@Nonnull @HeaderMap Map<String, Object> headers,
                                     @Nonnull @QueryMap Map<String, Object> queryMap,
                                     @Nonnull @Param("missionName") String missionName,
                                     @Nonnull List<String> keywords);

    /**
     * @param headers Additional request headers.
     * @param queryMap Map of query names to values.
     * @param missionName Name of the mission to update.
     *
     * @return The updated mission, wrapped in a list.
     *
     * @see ClearMissionKeywords
     */
    @Nonnull
    @RequestLine("DELETE /missions/{missionName}/keywords")
    List<Mission> clearMissionKeywords(@Nonnull @HeaderMap Map<String, Object> headers,
                                       @Nonnull @QueryMap Map<String, Object> queryMap,
                                       @Nonnull @Param("missionName") String missionName);

    /**
     * @param headers Additional request headers.
     * @param queryMap Map of query names to values.
     * @param missionName Name of the mission to update.
     * @param keyword Keyword to remove.
     *
     * @return The updated mission, wrapped in a list.
     *
     * @see RemoveMissionKeyword
     */
    @Nonnull
    @RequestLine("DELETE /missions/{missionName}/keywords/{keyword}")
    List<Mission> removeMissionKeyword(@Nonnull @HeaderMap Map<String, Object> headers,
                                       @Nonnull @QueryMap Map<String, Object> queryMap,
                                       @Nonnull @Param("missionName") String missionName,
                                       @Nonnull @Param("keyword") String keyword);

    // Internal note: TAK-Server does not yield meaningful data when manipulating
    //  UID and MissionContent keywords. This is not a consistent design with how
    //  manipulation of mission keywords is handled.

    /**
     * @param headers Additional request headers.
     * @param queryMap Map of query names to values.
     * @param missionName Name of the mission to update.
     * @param uid UID entry to update.
     * @param keywords Keywords to add to the entry.
     */
    @Headers("Content-Type: application/json")
    @RequestLine("PUT /missions/{missionName}/uid/{uid}/keywords")
    void addMissionUidKeywords(@Nonnull @HeaderMap Map<String, Object> headers,
                               @Nonnull @QueryMap Map<String, Object> queryMap,
                               @Nonnull @Param("missionName") String missionName,
                               @Nonnull @Param("uid") String uid,
                               @Nonnull List<String> keywords);

    /**
     * @param headers Additional request headers.
     * @param queryMap Map of query names to values.
     * @param missionName Name of the mission to update.
     * @param uid UID entry to clear of keywords.
     */
    @RequestLine("DELETE /missions/{missionName}/uid/{uid}/keywords")
    void clearMissionUidKeywords(@Nonnull @HeaderMap Map<String, Object> headers,
                                 @Nonnull @QueryMap Map<String, Object> queryMap,
                                 @Nonnull @Param("missionName") String missionName,
                                 @Nonnull @Param("uid") String uid);

    /**
     * @param headers Additional request headers.
     * @param queryMap Map of query names to values.
     * @param missionName Name of the mission to update.
     * @param hash {@link Resource#hash()} in a mission to update.
     * @param keywords Keywords to set.
     *
     * @see SetMissionResourceKeywords
     */
    @Headers("Content-Type: application/json")
    @RequestLine("PUT /missions/{missionName}/content/{hash}/keywords")
    void setMissionContentKeywords(@Nonnull @HeaderMap Map<String, Object> headers,
                                   @Nonnull @QueryMap Map<String, Object> queryMap,
                                   @Nonnull @Param("missionName") String missionName,
                                   @Nonnull @Param("hash") String hash,
                                   @Nonnull List<String> keywords);

    /**
     * @param headers Additional request headers.
     * @param queryMap Map of query names to values.
     * @param missionName Name of the mission with a resource to update.
     * @param hash {@link Resource#hash()} in a mission to update.
     *
     * @see ClearMissionResourceKeyword
     */
    @RequestLine("DELETE /missions/{missionName}/content/{hash}/keywords")
    void clearMissionContentKeywords(@Nonnull @HeaderMap Map<String, Object> headers,
                                     @Nonnull @QueryMap Map<String, Object> queryMap,
                                     @Nonnull @Param("missionName") String missionName,
                                     @Nonnull @Param("hash") String hash);

    /**
     * @param headers Additional request headers.
     * @param queryMap Map of query names to values.
     * @param missionName Name of the mission to generate a token for.
     *
     * @return Read access token.
     *
     * @see CreateMissionReadAccessToken
     */
    @Nonnull
    @RequestLine("GET /missions/{missionName}/token")
    Response createReadOnlyAccessToken(@Nonnull @HeaderMap Map<String, Object> headers,
                                       @Nonnull @QueryMap Map<String, Object> queryMap,
                                       @Nonnull @Param("missionName") String missionName);

    /**
     * @param headers Additional request headers.
     * @param queryMap Map of query names to values.
     * @param missionName Name of the mission to get the subscription info of.
     *
     * @return Subscription info to the given mission for the current user.
     *
     * @see GetCurrentMissionSubscription
     */
    @Nonnull
    @RequestLine("GET /missions/{missionName}/subscription")
    MissionSubscription getSubscriptionForCurrentUser(@Nonnull @HeaderMap Map<String, Object> headers,
                                                      @Nonnull @QueryMap Map<String, Object> queryMap,
                                                      @Nonnull @Param("missionName") String missionName);

    /**
     * @param headers Additional request headers.
     * @param queryMap Map of query names to values.
     * @param missionGuid Guid of the mission to get the subscription info of.
     *
     * @return Subscription info to the given mission for the current user.
     *
     * @see GetCurrentMissionSubscriptionByGuid
     * @since 5.7
     */
    @Nonnull
    @RequestLine("GET /missions/guid/{missionGuid}/subscription")
    MissionSubscription getSubscriptionForCurrentUserByGuid(@Nonnull @HeaderMap Map<String, Object> headers,
                                                            @Nonnull @QueryMap Map<String, Object> queryMap,
                                                            @Nonnull @Param("missionGuid") String missionGuid);

    /**
     * @param headers Additional request headers.
     * @param queryMap Map of query names to values.
     * @param missionName Name of the mission to create a subscription to.
     *
     * @return Subscription info to the given mission.
     *
     * @see CreateMissionSubscription
     */
    @Nonnull
    @RequestLine("PUT /missions/{missionName}/subscription")
    MissionSubscription createMissionSubscription(@Nonnull @HeaderMap Map<String, Object> headers,
                                                  @Nonnull @QueryMap Map<String, Object> queryMap,
                                                  @Nonnull @Param("missionName") String missionName);

    /**
     * @param headers Additional request headers.
     * @param queryMap Map of query names to values.
     * @param missionGuid Guid of the mission to create a subscription to.
     *
     * @return Subscription info to the given mission.
     *
     * @see CreateMissionSubscriptionByGuid
     * @since 5.7
     */
    @Nonnull
    @RequestLine("PUT /missions/guid/{missionGuid}/subscription")
    MissionSubscription createMissionSubscriptionByGuid(@Nonnull @HeaderMap Map<String, Object> headers,
                                                        @Nonnull @QueryMap Map<String, Object> queryMap,
                                                        @Nonnull @Param("missionGuid") String missionGuid);

    /**
     * @param headers Additional request headers.
     * @param queryMap Map of query names to values.
     * @param missionName Name of the mission to remove a subscription from.
     *
     * @see RemoveMissionSubscription
     */
    @RequestLine("DELETE /missions/{missionName}/subscription")
    void removeMissionSubscription(@Nonnull @HeaderMap Map<String, Object> headers,
                                   @Nonnull @QueryMap Map<String, Object> queryMap,
                                   @Nonnull @Param("missionName") String missionName);

    /**
     * @param headers Additional request headers.
     * @param queryMap Map of query names to values.
     * @param missionGuid Guid of the mission to remove a subscription from.
     *
     * @see RemoveMissionSubscriptionByGuid
     * @since 5.7
     */
    @RequestLine("DELETE /missions/guid/{missionGuid}/subscription")
    void removeMissionSubscriptionByGuid(@Nonnull @HeaderMap Map<String, Object> headers,
                                         @Nonnull @QueryMap Map<String, Object> queryMap,
                                         @Nonnull @Param("missionGuid") String missionGuid);

    /**
     * @param headers Additional request headers.
     * @param queryMap Map of query names to values.
     * @param missionName Name of the mission to update subscriptions of.
     * @param subscriptions Subscriptions to update.
     * Each subscription must declare both {@link MissionSubscription#clientUid()} and {@link MissionSubscription#role()}.
     *
     * @see SetMissionSubscriptionRoles
     */
    @Headers("Content-Type: application/json")
    @RequestLine("POST /missions/{missionName}/subscription")
    void setMissionSubscriptionRoles(@Nonnull @HeaderMap Map<String, Object> headers,
                                     @Nonnull @QueryMap Map<String, Object> queryMap,
                                     @Nonnull @Param("missionName") String missionName,
                                     @Nonnull List<MissionSubscription> subscriptions);

    /**
     * @param headers Additional request headers.
     * @param queryMap Map of query names to values.
     * @param missionGuid Guid of the mission to update subscriptions of.
     * @param subscriptions Subscriptions to update.
     * Each subscription must declare both {@link MissionSubscription#clientUid()} and {@link MissionSubscription#role()}.
     *
     * @see SetMissionSubscriptionRolesByGuid
     * @since 5.7
     */
    @Headers("Content-Type: application/json")
    @RequestLine("POST /missions/guid/{missionGuid}/subscription")
    void setMissionSubscriptionRolesByGuid(@Nonnull @HeaderMap Map<String, Object> headers,
                                           @Nonnull @QueryMap Map<String, Object> queryMap,
                                           @Nonnull @Param("missionGuid") String missionGuid,
                                           @Nonnull List<MissionSubscription> subscriptions);

    /**
     * @param headers Additional request headers.
     * @param queryMap Map of query names to values.
     * @param missionName Name of the mission to get subscribers of.
     *
     * @return List of {@link ClientEndpoint#uid() contacts} subscribed to the given mission.
     *
     * @see GetMissionSubscribers
     */
    @Nonnull
    @RequestLine("GET /missions/{missionName}/subscriptions")
    List<String> getMissionSubscribers(@Nonnull @HeaderMap Map<String, Object> headers,
                                       @Nonnull @QueryMap Map<String, Object> queryMap,
                                       @Nonnull @Param("missionName") String missionName);

    /**
     * @param headers Additional request headers.
     * @param queryMap Map of query names to values.
     * @param missionGuid Guid of the mission to get subscribers of.
     *
     * @return List of {@link ClientEndpoint#uid() contacts} subscribed to the given mission.
     *
     * @see GetMissionSubscribersByGuid
     * @since 5.7
     */
    @Nonnull
    @RequestLine("GET /missions/guid/{missionGuid}/subscriptions")
    List<String> getMissionSubscribersByGuid(@Nonnull @HeaderMap Map<String, Object> headers,
                                             @Nonnull @QueryMap Map<String, Object> queryMap,
                                             @Nonnull @Param("missionGuid") String missionGuid);

    /**
     * @param headers Additional request headers.
     * @param queryMap Map of query names to values.
     * @param missionName Name of the mission to get subscriptions of.
     *
     * @return List of {@link MissionSubscription subscriptions} associated with the given mission.
     *
     * @see GetMissionSubscriptions
     */
    @Nonnull
    @RequestLine("GET /missions/{missionName}/subscriptions/roles")
    List<MissionSubscription> getMissionSubscriptions(@Nonnull @HeaderMap Map<String, Object> headers,
                                                      @Nonnull @QueryMap Map<String, Object> queryMap,
                                                      @Nonnull @Param("missionName") String missionName);

    /**
     * @param headers Additional request headers.
     * @param queryMap Map of query names to values.
     * @param missionGuid Guid of the mission to get subscriptions of.
     *
     * @return List of {@link MissionSubscription subscriptions} associated with the given mission.
     *
     * @see GetMissionSubscriptionsByGuid
     * @since 5.7
     */
    @Nonnull
    @RequestLine("GET /missions/guid/{missionGuid}/subscriptions/roles")
    List<MissionSubscription> getMissionSubscriptionsByGuid(@Nonnull @HeaderMap Map<String, Object> headers,
                                                            @Nonnull @QueryMap Map<String, Object> queryMap,
                                                            @Nonnull @Param("missionGuid") String missionGuid);

    /**
     * @param headers Additional request headers.
     * @param queryMap Map of query names to values.
     *
     * @return List of subscription entries outling which {@link ClientEndpoint#uid() clients} are subscribed to which missions.
     *
     * @see GetAllMissionSubscribers
     */
    @Nonnull
    @RequestLine("GET /missions/all/subscriptions")
    List<MissionSubEntry> getAllMissionSubscriptions(@Nonnull @HeaderMap Map<String, Object> headers,
                                                     @Nonnull @QueryMap Map<String, Object> queryMap);

    /**
     * @param headers Additional request headers.
     * @param queryMap Map of query names to values.
     * @param missionName Name of the mission to get a user's role within.
     *
     * @return Role of a user in the mission.
     *
     * @see GetMissionRole
     */
    @RequestLine("GET /missions/{missionName}/role")
    MissionRole getMissionRole(@Nonnull @HeaderMap Map<String, Object> headers,
                               @Nonnull @QueryMap Map<String, Object> queryMap,
                               @Nonnull @Param("missionName") String missionName);

    /**
     * @param headers Additional request headers.
     * @param queryMap Map of query names to values.
     * @param missionGuid Guid of the mission to get a user's role within.
     *
     * @return Role of a user in the mission.
     *
     * @see GetMissionRoleByGuid
     * @since 5.7
     */
    @RequestLine("GET /missions/guid/{missionGuid}/role")
    MissionRole getMissionRoleByGuid(@Nonnull @HeaderMap Map<String, Object> headers,
                                     @Nonnull @QueryMap Map<String, Object> queryMap,
                                     @Nonnull @Param("missionGuid") String missionGuid);

    /**
     * @param headers Additional request headers.
     * @param queryMap Map of query names to values.
     * @param missionName Name of the mission to set a user's role within.
     *
     * @see SetMissionRole
     */
    @RequestLine("PUT /missions/{missionName}/role")
    void setMissionRole(@Nonnull @HeaderMap Map<String, Object> headers,
                        @Nonnull @QueryMap Map<String, Object> queryMap,
                        @Nonnull @Param("missionName") String missionName);

    /**
     * @param headers Additional request headers.
     * @param queryMap Map of query names to values.
     * @param missionGuid Guid of the mission to set a user's role within.
     *
     * @see SetMissionRoleByGuid
     * @since 5.7
     */
    @RequestLine("PUT /missions/guid/{missionGuid}/role")
    void setMissionRoleByGuid(@Nonnull @HeaderMap Map<String, Object> headers,
                              @Nonnull @QueryMap Map<String, Object> queryMap,
                              @Nonnull @Param("missionGuid") String missionGuid);

    /**
     * @param headers Additional request headers.
     * @param queryMap Map of query names to values.
     *
     * @return List of all invitations a user has.
     *
     * @see GetMissionInvitationsForUser
     */
    @Nonnull
    @RequestLine("GET /missions/invitations")
    List<MissionInvitation> getMissionInvitationsForUser(@Nonnull @HeaderMap Map<String, Object> headers,
                                                         @Nonnull @QueryMap Map<String, Object> queryMap);

    /**
     * @param headers Additional request headers.
     * @param queryMap Map of query names to values.
     * @param missionName Name of the mission to look-up invites for.
     *
     * @return List of all invitations a mission has.
     *
     * @see GetMissionInvitations
     */
    @Nonnull
    @RequestLine("GET /missions/{missionName}/invitations")
    List<MissionInvitation> getMissionInvitations(@Nonnull @HeaderMap Map<String, Object> headers,
                                                  @Nonnull @QueryMap Map<String, Object> queryMap,
                                                  @Nonnull @Param("missionName") String missionName);

    /**
     * @param headers Additional request headers.
     * @param queryMap Map of query names to values.
     * @param missionGuid Name of the mission to look-up invites for.
     *
     * @return List of all invitations a mission has.
     *
     * @see GetMissionInvitationsByGuid
     * @since 5.7
     */
    @Nonnull
    @RequestLine("GET /missions/guid/{missionGuid}/invitations")
    List<MissionInvitation> getMissionInvitationsByGuid(@Nonnull @HeaderMap Map<String, Object> headers,
                                                        @Nonnull @QueryMap Map<String, Object> queryMap,
                                                        @Nonnull @Param("missionGuid") String missionGuid);

    /**
     * @param headers Additional request headers.
     * @param queryMap Map of query names to values.
     * @param missionName Name of the mission to create invites for.
     * @param invitations Invitations to publish.
     *
     * @see SendMissionInvitations
     */
    @Headers("Content-Type: application/json")
    @RequestLine("POST /missions/{missionName}/invite")
    void sendMissionInvites(@Nonnull @HeaderMap Map<String, Object> headers,
                            @Nonnull @QueryMap Map<String, Object> queryMap,
                            @Nonnull @Param("missionName") String missionName,
                            @Nonnull List<MissionInvitation> invitations);

    /**
     * @param headers Additional request headers.
     * @param queryMap Map of query names to values.
     * @param missionGuid Guid of the mission to create invites for.
     * @param invitations Invitations to publish.
     *
     * @see SendMissionInvitationsByGuid
     * @since 5.7
     */
    @Headers("Content-Type: application/json")
    @RequestLine("POST /missions/guid/{missionGuid}/invite")
    void sendMissionInvitesByGuid(@Nonnull @HeaderMap Map<String, Object> headers,
                                  @Nonnull @QueryMap Map<String, Object> queryMap,
                                  @Nonnull @Param("missionGuid") String missionGuid,
                                  @Nonnull List<MissionInvitation> invitations);

    /**
     * @param headers Additional request headers.
     * @param queryMap Map of query names to values.
     * @param missionName Name of the mission to create an invitation for.
     * @param type Invitation type.
     * @param invitee {@link ClientEndpoint#uid() Unique id} of contact to invite.
     *
     * @see SendMissionInvitation
     */
    @RequestLine("PUT /missions/{missionName}/invite/{type}/{invitee}")
    void inviteToMission(@Nonnull @HeaderMap Map<String, Object> headers,
                         @Nonnull @QueryMap Map<String, Object> queryMap,
                         @Nonnull @Param("missionName") String missionName,
                         @Nonnull @Param("type") String type,
                         @Nonnull @Param("invitee") String invitee);

    /**
     * @param headers Additional request headers.
     * @param queryMap Map of query names to values.
     * @param missionGuid Guid of the mission to create an invitation for.
     * @param type Invitation type.
     * @param invitee {@link ClientEndpoint#uid() Unique id} of contact to invite.
     *
     * @see SendMissionInvitationByGuid
     * @since 5.7
     */
    @RequestLine("PUT /missions/guid/{missionGuid}/invite/{type}/{invitee}")
    void inviteToMissionByGuid(@Nonnull @HeaderMap Map<String, Object> headers,
                               @Nonnull @QueryMap Map<String, Object> queryMap,
                               @Nonnull @Param("missionGuid") String missionGuid,
                               @Nonnull @Param("type") String type,
                               @Nonnull @Param("invitee") String invitee);

    /**
     * @param headers Additional request headers.
     * @param queryMap Map of query names to values.
     * @param missionName Name of the mission to remove an invitation from.
     * @param type Invitation type.
     * @param invitee {@link ClientEndpoint#uid() Unique id} of contact to uninvite.
     *
     * @see RemoveMissionInvitation
     */
    @RequestLine("DELETE /missions/{missionName}/invite/{type}/{invitee}")
    void uninviteToMission(@Nonnull @HeaderMap Map<String, Object> headers,
                           @Nonnull @QueryMap Map<String, Object> queryMap,
                           @Nonnull @Param("missionName") String missionName,
                           @Nonnull @Param("type") String type,
                           @Nonnull @Param("invitee") String invitee);

    /**
     * @param headers Additional request headers.
     * @param queryMap Map of query names to values.
     * @param missionGuid Name of the mission to remove an invitation from.
     * @param type Invitation type.
     * @param invitee {@link ClientEndpoint#uid() Unique id} of contact to uninvite.
     *
     * @see RemoveMissionInvitationByGuid
     * @since 5.7
     */
    @RequestLine("DELETE /missions/guid/{missionGuid}/invite/{type}/{invitee}")
    void uninviteToMissionByGuid(@Nonnull @HeaderMap Map<String, Object> headers,
                                 @Nonnull @QueryMap Map<String, Object> queryMap,
                                 @Nonnull @Param("missionGuid") String missionGuid,
                                 @Nonnull @Param("type") String type,
                                 @Nonnull @Param("invitee") String invitee);

    /**
     * @param headers Additional request headers.
     * @param queryMap Map of query names to values.
     * @param entry Entry to publish.
     *
     * @return Published log entry from server.
     *
     * @see CreateLogEntry
     */
    @Nonnull
    @Headers("Content-Type: application/json")
    @RequestLine("POST /missions/logs/entries")
    LogEntry createLogEntry(@Nonnull @HeaderMap Map<String, Object> headers,
                            @Nonnull @QueryMap Map<String, Object> queryMap,
                            @Nonnull LogEntry entry);

    /**
     * @param headers Additional request headers.
     * @param queryMap Map of query names to values.
     * @param entry Updated entry.
     *
     * @return Updated entry from server.
     *
     * @see UpdateLogEntry
     */
    @Nonnull
    @Headers("Content-Type: application/json")
    @RequestLine("PUT /missions/logs/entries")
    LogEntry updateLogEntry(@Nonnull @HeaderMap Map<String, Object> headers,
                            @Nonnull @QueryMap Map<String, Object> queryMap,
                            @Nonnull LogEntry entry);

    /**
     * @param headers Additional request headers.
     * @param queryMap Map of query names to values.
     * @param id The {@link LogEntry#entryUid()} to look-up.
     *
     * @return Log entry by uid.
     *
     * @see GetLogEntry
     */
    @Nonnull
    @RequestLine("GET /missions/logs/entries/{id}")
    LogEntry getLogEntry(@Nonnull @HeaderMap Map<String, Object> headers,
                         @Nonnull @QueryMap Map<String, Object> queryMap,
                         @Nonnull @Param("id") String id);

    /**
     * @param headers Additional request headers.
     * @param queryMap Map of query names to values.
     * @param missionName Name of the mission to get log entries of.
     *
     * @return Log entries associated with the given mission.
     *
     * @see GetLogEntries
     */
    @Nonnull
    @RequestLine("GET /missions/{missionName}/log")
    List<LogEntry> getLogEntries(@Nonnull @HeaderMap Map<String, Object> headers,
                                 @Nonnull @QueryMap Map<String, Object> queryMap,
                                 @Nonnull @Param("missionName") String missionName);

    /**
     * @param headers Additional request headers.
     * @param queryMap Map of query names to values.
     * @param id The {@link LogEntry#entryUid()} to remove.
     *
     * @see RemoveLogEntry
     */
    @RequestLine("DELETE /missions/logs/entries/{id}")
    void removeLogEntry(@Nonnull @HeaderMap Map<String, Object> headers,
                        @Nonnull @QueryMap Map<String, Object> queryMap,
                        @Nonnull @Param("id") String id);

    /**
     * @param headers Additional request headers.
     * @param queryMap Map of query names to values.
     *
     * @return All log entries from all missions.
     *
     * @see GetAllLogEntries
     */
    @Nonnull
    @RequestLine("GET /missions/all/logs")
    List<LogEntry> getAllLogEntries(@Nonnull @HeaderMap Map<String, Object> headers,
                                    @Nonnull @QueryMap Map<String, Object> queryMap);

    /**
     * @param headers Additional request headers.
     * @param queryMap Map of query names to values.
     * @param missionName Name of the mission to get the cot events of.
     *
     * @return Latest CoT events as an XML string from the given mission.
     *
     * @see GetLatestMissionCotEvents
     */
    @Nonnull
    @RequestLine("GET /missions/{missionName}/cot")
    Response getLatestMissionCotEvents(@Nonnull @HeaderMap Map<String, Object> headers,
                                       @Nonnull @QueryMap Map<String, Object> queryMap,
                                       @Nonnull @Param("missionName") String missionName);

    /**
     * @param headers Additional request headers.
     * @param queryMap Map of query names to values.
     * @param missionGuid Guid of the mission to get the cot events of.
     *
     * @return Latest CoT events as an XML string from the given mission.
     *
     * @see GetLatestMissionCotEventsByGuid
     * @since 5.7
     */
    @Nonnull
    @RequestLine("GET /missions/guid/{missionGuid}/cot")
    Response getLatestMissionCotEventsByGuid(@Nonnull @HeaderMap Map<String, Object> headers,
                                             @Nonnull @QueryMap Map<String, Object> queryMap,
                                             @Nonnull @Param("missionGuid") String missionGuid);

    /**
     * @param headers Additional request headers.
     * @param queryMap Map of query names to values.
     * @param missionName Name of the mission to get the remote subscriptions of.
     *
     * @return Associated {@link RemoteSubscription} items of the given mission.
     *
     * @see GetRemoteSubscriptions
     */
    @Nonnull
    @RequestLine("GET /missions/{missionName}/contacts")
    List<RemoteSubscription> getRemoteSubscriptions(@Nonnull @HeaderMap Map<String, Object> headers,
                                                    @Nonnull @QueryMap Map<String, Object> queryMap,
                                                    @Nonnull @Param("missionName") String missionName);

    /**
     * @param headers Additional request headers.
     * @param queryMap Map of query names to values.
     * @param missionGuid Guid of the mission to get the remote subscriptions of.
     *
     * @return Associated {@link RemoteSubscription} items of the given mission.
     *
     * @see GetRemoteSubscriptionsByGuid
     * @since 5.7
     */
    @Nonnull
    @RequestLine("GET /missions/guid/{missionGuid}/contacts")
    List<RemoteSubscription> getRemoteSubscriptionsByGuid(@Nonnull @HeaderMap Map<String, Object> headers,
                                                          @Nonnull @QueryMap Map<String, Object> queryMap,
                                                          @Nonnull @Param("missionGuid") String missionGuid);

    /**
     * @param headers Additional request headers.
     * @param queryMap Map of query names to values.
     * @param missionName Name of the mission to hold the layer.
     * @param mapLayer Layer to publish on the server.
     *
     * @return Published map layer.
     *
     * @see CreateMapLayer
     */
    @Nonnull
    @RequestLine("POST /missions/{missionName}/maplayers")
    MapLayer createMapLayer(@Nonnull @HeaderMap Map<String, Object> headers,
                            @Nonnull @QueryMap Map<String, Object> queryMap,
                            @Nonnull @Param("missionName") String missionName,
                            @Nonnull MapLayer mapLayer);

    /**
     * @param headers Additional request headers.
     * @param queryMap Map of query names to values.
     * @param missionGuid Guid of the mission to hold the layer.
     * @param mapLayer Layer to publish on the server.
     *
     * @return Published map layer.
     *
     * @see CreateMapLayerByGuid
     * @since 5.7
     */
    @Nonnull
    @RequestLine("POST /missions/guid/{missionGuid}/maplayers")
    MapLayer createMapLayerByGuid(@Nonnull @HeaderMap Map<String, Object> headers,
                                  @Nonnull @QueryMap Map<String, Object> queryMap,
                                  @Nonnull @Param("missionGuid") String missionGuid,
                                  @Nonnull MapLayer mapLayer);

    /**
     * @param headers Additional request headers.
     * @param queryMap Map of query names to values.
     * @param missionName Name of the mission that holds the layer.
     * @param mapLayer Layer to update on the server. Must share a {@link MapLayer#uid()} with an existing layer.
     *
     * @return Updated map layer.
     *
     * @see UpdateMapLayer
     */
    @Nonnull
    @RequestLine("PUT /missions/{missionName}/maplayers")
    MapLayer updateMapLayer(@Nonnull @HeaderMap Map<String, Object> headers,
                            @Nonnull @QueryMap Map<String, Object> queryMap,
                            @Nonnull @Param("missionName") String missionName,
                            @Nonnull MapLayer mapLayer);

    /**
     * @param headers Additional request headers.
     * @param queryMap Map of query names to values.
     * @param missionGuid Guid of the mission that holds the layer.
     * @param mapLayer Layer to update on the server. Must share a {@link MapLayer#uid()} with an existing layer.
     *
     * @return Updated map layer.
     *
     * @see UpdateMapLayerByGuid
     * @since 5.7
     */
    @Nonnull
    @RequestLine("PUT /missions/guid/{missionGuid}/maplayers")
    MapLayer updateMapLayerByGuid(@Nonnull @HeaderMap Map<String, Object> headers,
                                  @Nonnull @QueryMap Map<String, Object> queryMap,
                                  @Nonnull @Param("missionGuid") String missionGuid,
                                  @Nonnull MapLayer mapLayer);

    /**
     * @param headers Additional request headers.
     * @param queryMap Map of query names to values.
     * @param missionName Name of the mission that holds the layer.
     * @param layerUid {@link MapLayer#uid() Unique identifier} of a layer to remove from the mission.
     *
     * @see RemoveMapLayer
     */
    @RequestLine("DELETE /missions/{missionName}/maplayers/{layerUid}")
    void removeMapLayer(@Nonnull @HeaderMap Map<String, Object> headers,
                        @Nonnull @QueryMap Map<String, Object> queryMap,
                        @Nonnull @Param("missionName") String missionName,
                        @Nonnull @Param("layerUid") String layerUid);

    /**
     * @param headers Additional request headers.
     * @param queryMap Map of query names to values.
     * @param missionGuid Guid of the mission that holds the layer.
     * @param layerUid {@link MapLayer#uid() Unique identifier} of a layer to remove from the mission.
     *
     * @see RemoveMapLayerByGuid
     * @since 5.7
     */
    @RequestLine("DELETE /missions/guid/{missionGuid}/maplayers/{layerUid}")
    void removeMapLayerByGuid(@Nonnull @HeaderMap Map<String, Object> headers,
                              @Nonnull @QueryMap Map<String, Object> queryMap,
                              @Nonnull @Param("missionGuid") String missionGuid,
                              @Nonnull @Param("layerUid") String layerUid);

    /**
     * @param headers Additional request headers.
     * @param queryMap Map of query names to values.
     * @param missionName Name of the mission that contains layers to look up.
     *
     * @return Associated {@link MissionLayer} items of the given mission.
     *
     * @see GetMissionLayers
     */
    @Nonnull
    @RequestLine("GET /missions/{missionName}/layers")
    List<MissionLayer> getMissionLayers(@Nonnull @HeaderMap Map<String, Object> headers,
                                        @Nonnull @QueryMap Map<String, Object> queryMap,
                                        @Nonnull @Param("missionName") String missionName);

    /**
     * @param headers Additional request headers.
     * @param queryMap Map of query names to values.
     * @param missionGuid Guid of the mission that contains layers to look up.
     *
     * @return Associated {@link MissionLayer} items of the given mission.
     *
     * @see GetMissionLayersByGuid
     * @since 5.7
     */
    @Nonnull
    @RequestLine("GET /missions/guid/{missionGuid}/layers")
    List<MissionLayer> getMissionLayersByGuid(@Nonnull @HeaderMap Map<String, Object> headers,
                                              @Nonnull @QueryMap Map<String, Object> queryMap,
                                              @Nonnull @Param("missionGuid") String missionGuid);

    /**
     * @param headers Additional request headers.
     * @param queryMap Map of query names to values.
     * @param missionName Name of the mission that holds the layer.
     * @param layerUid {@link MissionLayer#uid() Unique identifier} of a layer to get.
     *
     * @return The layer by the given uid in the mission.
     *
     * @see GetMissionLayer
     */
    @Nonnull
    @RequestLine("GET /missions/{missionName}/layers/{layerUid}")
    MissionLayer getMissionLayer(@Nonnull @HeaderMap Map<String, Object> headers,
                                 @Nonnull @QueryMap Map<String, Object> queryMap,
                                 @Nonnull @Param("missionName") String missionName,
                                 @Nonnull @Param("layerUid") String layerUid);

    /**
     * @param headers Additional request headers.
     * @param queryMap Map of query names to values.
     * @param missionGuid Guid of the mission that holds the layer.
     * @param layerUid {@link MissionLayer#uid() Unique identifier} of a layer to get.
     *
     * @return The layer by the given uid in the mission.
     *
     * @see GetMissionLayerByGuid
     * @since 5.7
     */
    @Nonnull
    @RequestLine("GET /missions/guid/{missionGuid}/layers/{layerUid}")
    MissionLayer getMissionLayerByGuid(@Nonnull @HeaderMap Map<String, Object> headers,
                                       @Nonnull @QueryMap Map<String, Object> queryMap,
                                       @Nonnull @Param("missionGuid") String missionGuid,
                                       @Nonnull @Param("layerUid") String layerUid);

    /**
     * @param headers Additional request headers.
     * @param queryMap Map of query names to values.
     * @param missionName Name of the mission to hold the layer.
     *
     * @return Created mission layer.
     *
     * @see CreateMissionLayer
     */
    @Nonnull
    @RequestLine("PUT /missions/{missionName}/layers")
    MissionLayer createMissionLayer(@Nonnull @HeaderMap Map<String, Object> headers,
                                    @Nonnull @QueryMap Map<String, Object> queryMap,
                                    @Nonnull @Param("missionName") String missionName);

    /**
     * @param headers Additional request headers.
     * @param queryMap Map of query names to values.
     * @param missionGuid Guid of the mission to hold the layer.
     *
     * @return Created mission layer.
     *
     * @see CreateMissionLayerByGuid
     * @since 5.7
     */
    @Nonnull
    @RequestLine("PUT /missions/guid/{missionGuid}/layers")
    MissionLayer createMissionLayerByGuid(@Nonnull @HeaderMap Map<String, Object> headers,
                                          @Nonnull @QueryMap Map<String, Object> queryMap,
                                          @Nonnull @Param("missionGuid") String missionGuid);

    /**
     * @param headers Additional request headers.
     * @param queryMap Map of query names to values.
     * @param missionName Name of the mission containing the layer.
     * @param layerUid {@link MissionLayer#uid() Unique identifier} of a layer to update.
     *
     * @see SetMissionLayerName
     */
    @RequestLine("PUT /missions/{missionName}/layers/{layerUid}")
    void setMissionLayerName(@Nonnull @HeaderMap Map<String, Object> headers,
                             @Nonnull @QueryMap Map<String, Object> queryMap,
                             @Nonnull @Param("missionName") String missionName,
                             @Nonnull @Param("layerUid") String layerUid);

    /**
     * @param headers Additional request headers.
     * @param queryMap Map of query names to values.
     * @param missionGuid Guid of the mission containing the layer.
     * @param layerUid {@link MissionLayer#uid() Unique identifier} of a layer to update.
     *
     * @see SetMissionLayerNameByGuid
     * @since 5.7
     */
    @RequestLine("PUT /missions/guid/{missionGuid}/layers/{layerUid}")
    void setMissionLayerNameByGuid(@Nonnull @HeaderMap Map<String, Object> headers,
                                   @Nonnull @QueryMap Map<String, Object> queryMap,
                                   @Nonnull @Param("missionGuid") String missionGuid,
                                   @Nonnull @Param("layerUid") String layerUid);

    /**
     * @param headers Additional request headers.
     * @param queryMap Map of query names to values.
     * @param missionName Name of the mission containing the layer.
     * @param layerUid {@link MissionLayer#uid() Unique identifier} of a layer to update.
     *
     * @see SetMissionLayerPosition
     */
    @RequestLine("PUT /missions/{missionName}/layers/{layerUid}/position")
    void setMissionLayerPosition(@Nonnull @HeaderMap Map<String, Object> headers,
                                 @Nonnull @QueryMap Map<String, Object> queryMap,
                                 @Nonnull @Param("missionName") String missionName,
                                 @Nonnull @Param("layerUid") String layerUid);

    /**
     * @param headers Additional request headers.
     * @param queryMap Map of query names to values.
     * @param missionGuid Guid of the mission containing the layer.
     * @param layerUid {@link MissionLayer#uid() Unique identifier} of a layer to update.
     *
     * @see SetMissionLayerPositionByGuid
     * @since 5.7
     */
    @RequestLine("PUT /missions/guid/{missionGuid}/layers/{layerUid}/position")
    void setMissionLayerPositionByGuid(@Nonnull @HeaderMap Map<String, Object> headers,
                                       @Nonnull @QueryMap Map<String, Object> queryMap,
                                       @Nonnull @Param("missionGuid") String missionGuid,
                                       @Nonnull @Param("layerUid") String layerUid);

    /**
     * @param headers Additional request headers.
     * @param queryMap Map of query names to values.
     * @param missionName Name of the mission containing the layer.
     *
     * @see SetMissionLayerParent
     */
    @RequestLine("PUT /missions/{missionName}/layers/parent")
    void setMissionLayerParent(@Nonnull @HeaderMap Map<String, Object> headers,
                               @Nonnull @QueryMap Map<String, Object> queryMap,
                               @Nonnull @Param("missionName") String missionName);

    /**
     * @param headers Additional request headers.
     * @param queryMap Map of query names to values.
     * @param missionGuid Name of the mission containing the layer.
     *
     * @see SetMissionLayerParentByGuid
     * @since 5.7
     */
    @RequestLine("PUT /missions/guid/{missionGuid}/layers/parent")
    void setMissionLayerParentByGuid(@Nonnull @HeaderMap Map<String, Object> headers,
                                     @Nonnull @QueryMap Map<String, Object> queryMap,
                                     @Nonnull @Param("missionGuid") String missionGuid);

    /**
     * @param headers Additional request headers.
     * @param queryMap Map of query names to values.
     * @param missionName Name of the mission containing the layer.
     *
     * @see RemoveMissionLayer
     */
    @RequestLine("DELETE /missions/{missionName}/layers")
    void removeMissionLayer(@Nonnull @HeaderMap Map<String, Object> headers,
                            @Nonnull @QueryMap Map<String, Object> queryMap,
                            @Nonnull @Param("missionName") String missionName);

    /**
     * @param headers Additional request headers.
     * @param queryMap Map of query names to values.
     * @param missionGuid Guid of the mission containing the layer.
     *
     * @see RemoveMissionLayerByGuid
     * @since 5.7
     */
    @RequestLine("DELETE /missions/guid/{missionGuid}/layers")
    void removeMissionLayerByGuid(@Nonnull @HeaderMap Map<String, Object> headers,
                                  @Nonnull @QueryMap Map<String, Object> queryMap,
                                  @Nonnull @Param("missionGuid") String missionGuid);
}
