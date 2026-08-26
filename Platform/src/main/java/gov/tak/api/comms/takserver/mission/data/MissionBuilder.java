package gov.tak.api.comms.takserver.mission.data;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Set;

/**
 * Builder for {@link Mission}.
 *
 * @since 5.0
 */
public final class MissionBuilder
{
    private final String name;
    private String guid;
    private String description;
    private String chatRoom;
    private String baseLayer;
    private String boundingBox;
    private String boundingPolygon;
    private String path;
    private String classification;
    private String tool = "public";
    private List<String> keywords = Collections.emptyList();
    private String creatorUid = "00000000-0000-0000-0000-000000000000";
    private Long expiration;
    private Instant createTime = Instant.now();
    private MissionRole defaultRole = new MissionRole(RoleType.MISSION_SUBSCRIBER, Collections.emptySet());
    private MissionRole ownerRole = new MissionRole(RoleType.MISSION_OWNER, Collections.emptySet());
    private boolean inviteOnly;
    private boolean passwordProtected;
    private List<ExternalMissionData> externalData = Collections.emptyList();
    private List<MissionAdd.TString> uids = Collections.emptyList();
    private List<MissionAdd.TResource> contents = Collections.emptyList();
    private List<MissionChange> missionChanges = Collections.emptyList();
    private Set<MissionFeed> feeds = Collections.emptySet();
    private List<MapLayer> mapLayers = Collections.emptyList();
    private Set<String> groups = Collections.emptySet();
    private List<LogEntry> logs = Collections.emptyList();

    /**
     * New mission builder.
     *
     * @param name Mission name, must not be {@code null}.
     */
    public MissionBuilder(@Nonnull String name)
    {
        this.name = name;
    }

    /**
     * @param guid Mission guid.
     *
     * @return Builder.
     *
     * @see Mission#guid()
     */
    @Nonnull
    public MissionBuilder withGuid(@Nullable String guid)
    {
        this.guid = guid;
        return this;
    }

    /**
     * @param description Mission description.
     *
     * @return Builder.
     *
     * @see Mission#description()
     */
    @Nonnull
    public MissionBuilder withDescription(@Nullable String description)
    {
        this.description = description;
        return this;
    }

    /**
     * @param chatRoom Mission chat room.
     *
     * @return Builder.
     *
     * @see Mission#chatRoom()
     */
    @Nonnull
    public MissionBuilder withChatRoom(@Nullable String chatRoom)
    {
        this.chatRoom = chatRoom;
        return this;
    }

    /**
     * @param baseLayer Mission base layer.
     *
     * @return Builder.
     *
     * @see Mission#baseLayer()
     */
    @Nonnull
    public MissionBuilder withBaseLayer(@Nullable String baseLayer)
    {
        this.baseLayer = baseLayer;
        return this;
    }

    /**
     * Overwrites the value of {@link #withBoundingPolygon(String)}.
     *
     * @param bbox Mission bounding box.
     *
     * @return Builder.
     *
     * @see Mission#bbox()
     */
    @Nonnull
    public MissionBuilder withBoundingBox(@Nullable String bbox)
    {
        this.boundingPolygon = null;
        this.boundingBox = bbox;
        return this;
    }

    /**
     * Overwrites the value of {@link #withBoundingBox(String)}.
     *
     * @param boundingPolygon Mission bounding polygon.
     *
     * @return Builder.
     *
     * @see Mission#boundingPolygon()
     */
    @Nonnull
    public MissionBuilder withBoundingPolygon(@Nullable String boundingPolygon)
    {
        // TODO TAKX-2131: Move away from allowing single string, enforce point model
        this.boundingBox = null;
        this.boundingPolygon = boundingPolygon;
        return this;
    }

    /**
     * @param path Mission path.
     *
     * @return Builder.
     *
     * @see Mission#path()
     */
    @Nonnull
    public MissionBuilder withPath(@Nullable String path)
    {
        this.path = path;
        return this;
    }

    /**
     * @param classification Mission classification.
     *
     * @return Builder.
     *
     * @see Mission#classification()
     */
    @Nonnull
    public MissionBuilder withClassification(@Nullable String classification)
    {
        this.classification = classification;
        return this;
    }

    /**
     * @param tool Mission tool that created the mission. Required.
     *
     * @return Builder.
     *
     * @see Mission#tool()
     */
    @Nonnull
    public MissionBuilder withTool(@Nonnull String tool)
    {
        this.tool = tool;
        return this;
    }

    /**
     * @param keywords Mission keywords.
     *
     * @return Builder.
     *
     * @see Mission#keywords()
     */
    @Nonnull
    public MissionBuilder withKeywords(@Nullable List<String> keywords)
    {
        this.keywords = Objects.requireNonNullElse(keywords, Collections.emptyList());
        return this;
    }

    /**
     * @param creatorUid Mission creator's uid.
     *
     * @return Builder.
     *
     * @see Mission#creatorUid()
     */
    @Nonnull
    public MissionBuilder withCreatorUid(@Nonnull String creatorUid)
    {
        this.creatorUid = creatorUid;
        return this;
    }

    /**
     * @param expiration Mission expiration time.
     *
     * @return Builder.
     *
     * @see Mission#expiration()
     */
    @Nonnull
    public MissionBuilder withExpiration(@Nullable Long expiration)
    {
        this.expiration = expiration;
        return this;
    }

    /**
     * @param createTime Mission time of creation.
     *
     * @return Builder.
     *
     * @see Mission#createTime()
     */
    @Nonnull
    public MissionBuilder withCreateTime(@Nonnull Instant createTime)
    {
        this.createTime = createTime;
        return this;
    }

    /**
     * @param defaultRole Mission default role for newly invited members.
     *
     * @return Builder.
     *
     * @see Mission#defaultRole()
     */
    @Nonnull
    public MissionBuilder withDefaultRole(@Nonnull MissionRole defaultRole)
    {
        this.defaultRole = defaultRole;
        return this;
    }

    /**
     * @param ownerRole Mission owner role.
     *
     * @return Builder.
     *
     * @see Mission#ownerRole()
     */
    @Nonnull
    public MissionBuilder withOwnerRole(@Nonnull MissionRole ownerRole)
    {
        this.ownerRole = ownerRole;
        return this;
    }

    /**
     * @param inviteOnly Mission flag limiting access to only invitees.
     *
     * @return Builder.
     *
     * @see Mission#inviteOnly()
     */
    @Nonnull
    public MissionBuilder withInviteOnly(boolean inviteOnly)
    {
        this.inviteOnly = inviteOnly;
        return this;
    }

    /**
     * @param passwordProtected Mission flag indicating a password is set.
     *
     * @return Builder.
     *
     * @see Mission#passwordProtected()
     */
    @Nonnull
    public MissionBuilder withPasswordProtected(boolean passwordProtected)
    {
        this.passwordProtected = passwordProtected;
        return this;
    }

    /**
     * @param externalData Mission external data items.
     *
     * @return Builder.
     *
     * @see Mission#externalData()
     */
    @Nonnull
    public MissionBuilder withExternalData(@Nullable List<ExternalMissionData> externalData)
    {
        this.externalData = Objects.requireNonNullElse(externalData, Collections.emptyList());
        return this;
    }

    /**
     * @param uids Mission map item uids.
     *
     * @return Builder.
     *
     * @see Mission#uids()
     */
    @Nonnull
    public MissionBuilder withUids(@Nullable List<MissionAdd.TString> uids)
    {
        this.uids = Objects.requireNonNullElse(uids, Collections.emptyList());
        return this;
    }

    /**
     * @param contents Mission files.
     *
     * @return Builder.
     *
     * @see Mission#contents()
     */
    @Nonnull
    public MissionBuilder withContents(@Nullable List<MissionAdd.TResource> contents)
    {
        this.contents = Objects.requireNonNullElse(contents, Collections.emptyList());
        return this;
    }

    /**
     * @param missionChanges Mission changelog entries.
     *
     * @return Builder.
     *
     * @see Mission#missionChanges()
     */
    @Nonnull
    public MissionBuilder withMissionChanges(@Nullable List<MissionChange> missionChanges)
    {
        this.missionChanges = Objects.requireNonNullElse(missionChanges, Collections.emptyList());
        return this;
    }

    /**
     * @param feeds Mission external feeds.
     *
     * @return Builder.
     *
     * @see Mission#feeds()
     */
    @Nonnull
    public MissionBuilder withFeeds(@Nullable Set<MissionFeed> feeds)
    {
        this.feeds = Objects.requireNonNullElse(feeds, Collections.emptySet());
        return this;
    }

    /**
     * @param mapLayers Mission map layers.
     *
     * @return Builder.
     *
     * @see Mission#mapLayers()
     */
    @Nonnull
    public MissionBuilder withMapLayers(@Nullable List<MapLayer> mapLayers)
    {
        this.mapLayers = Objects.requireNonNullElse(mapLayers, Collections.emptyList());
        return this;
    }

    /**
     * @param groups Mission groups.
     *
     * @return Builder.
     *
     * @see Mission#groups()
     */
    @Nonnull
    public MissionBuilder withGroups(@Nullable Set<String> groups)
    {
        this.groups = Objects.requireNonNullElse(groups, Collections.emptySet());
        return this;
    }

    /**
     * @param logs Mission user logs.
     *
     * @return Builder.
     *
     * @see Mission#logs()
     */
    @Nonnull
    public MissionBuilder withLogs(@Nullable List<LogEntry> logs)
    {
        this.logs = Objects.requireNonNullElse(logs, Collections.emptyList());
        return this;
    }

    /**
     * @return New mission.
     */
    @Nonnull
    public Mission build()
    {
        return new Mission(name, guid, description, chatRoom, baseLayer, boundingBox, boundingPolygon,
                path, classification, tool, keywords, creatorUid, expiration, createTime, defaultRole,
                ownerRole, inviteOnly, passwordProtected, externalData, uids, contents, missionChanges,
                feeds, mapLayers, groups, logs);
    }
}
