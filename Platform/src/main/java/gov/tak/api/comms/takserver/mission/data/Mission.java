package gov.tak.api.comms.takserver.mission.data;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonSetter;
import com.fasterxml.jackson.annotation.Nulls;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import gov.tak.api.comms.takserver.mission.AMissionRoleDeserializer;
import gov.tak.api.comms.takserver.mission.MissionAddDeserializer;
import gov.tak.api.comms.takserver.mission.call.mission.GetLogEntries;
import gov.tak.api.comms.takserver.mission.call.mission.GetMission;

import java.time.Instant;
import java.util.List;
import java.util.Set;

/**
 * @param name Name of the mission.
 * @param guid Unique identifier of the mission.
 * @param description Description of the mission.
 * @param chatRoom Associated XMPP chat room URL.
 * @param baseLayer Associated {@link MapLayer} by its {@link MapLayer#uid()} for missions outlining a COP.
 * @param bbox Boundings of the mission. Bounding box in the format {@code <max lat>, <min lon>, <min lat>, <max lon>}
 * <i>({@code top, left, bottom, right})</i>.
 * @param boundingPolygon Boundings of the mission. Bounding polygon in the format {@code <lat> <lon>, <lat> <lon>, ...} <i>(WKT format)</i>.
 * @param path Path in the COP hierarchy. Outlines an absolute path like a file system.
 * @param classification Classification level in CAPCO format.
 * @param tool Name of the tool that created the mission.
 * By default, if no value is specified {@code public} is used. Special cases may change this value.
 * @param keywords Keywords assigned to the mission.
 * @param creatorUid The {@link ClientEndpoint#uid()} of the user who created the mission.
 * @param expiration Timestamp in epoch millis for the mission to delete itself.
 * @param createTime The timestamp of the mission's creation.
 * @param defaultRole Default role to assign to users when they are invited to the mission.
 * @param ownerRole Role of the mission owner.
 * @param inviteOnly Flag to limit access to only users granted access via a {@link MissionInvitation}.
 * @param passwordProtected Flag to indicate a mission has a password set.
 * @param externalData List of {@link ExternalMissionData} items associated with the mission.
 * @param uids The map item uids <i>(Wrapped with time and attribution info)</i> associated with the mission.
 * @param contents Associated {@link Resource files} attached to the mission.
 * @param missionChanges Changelog entries of the mission. Oldest items appear first in the list.
 * @param feeds Associated feeds added to the mission. The server may yield these in randomized order, so we will locally sort them by name.
 * @param mapLayers Associated layers added to the mission.
 * @param groups Group names the mission is accessible to.
 * @param logs Underlying elements of the changelog entries.
 * Typically accessed via {@link GetLogEntries} or {@link GetMission#withLogs(boolean) GetMission with 'logs=true'}.
 *
 * @since 3.5
 *
 * @see MissionBuilder
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record Mission(String name,
        String guid,
        String description,
        String chatRoom,
        String baseLayer,
        String bbox,
        String boundingPolygon,
        String path,
        String classification,
        String tool,
        @JsonSetter(nulls = Nulls.AS_EMPTY) List<String> keywords,
        String creatorUid,
        Long expiration,
        Instant createTime,
        @JsonDeserialize(using = AMissionRoleDeserializer.Default.class) MissionRole defaultRole,
        @JsonDeserialize(using = AMissionRoleDeserializer.Owner.class) MissionRole ownerRole,
        boolean inviteOnly,
        boolean passwordProtected,
        @JsonSetter(nulls = Nulls.AS_EMPTY) List<ExternalMissionData> externalData,
        @JsonSetter(nulls = Nulls.AS_EMPTY) @JsonDeserialize(contentUsing = MissionAddDeserializer.class) List<MissionAdd.TString> uids,
        @JsonSetter(nulls = Nulls.AS_EMPTY) @JsonDeserialize(contentUsing = MissionAddDeserializer.class) List<MissionAdd.TResource> contents,
        @JsonSetter(nulls = Nulls.AS_EMPTY) List<MissionChange> missionChanges,
        @JsonSetter(nulls = Nulls.AS_EMPTY) Set<MissionFeed> feeds,
        @JsonSetter(nulls = Nulls.AS_EMPTY) List<MapLayer> mapLayers,
        @JsonSetter(nulls = Nulls.AS_EMPTY) Set<String> groups,
        @JsonSetter(nulls = Nulls.AS_EMPTY) List<LogEntry> logs)
{
}
