package gov.tak.core.comms.takserver.mission;

import gov.tak.api.comms.takserver.ITakServerConnection;
import gov.tak.api.comms.takserver.mission.data.Mission;
import gov.tak.api.comms.takserver.mission.data.MissionAdd;
import gov.tak.api.comms.takserver.mission.data.Resource;
import gov.tak.core.comms.takserver.DataSyncManager;
import gov.takx.commons.lang.ExceptionUtils;
import gov.takx.commons.utils.StringUtils;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import java.util.Collections;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Data-sync component for tracking {@link Mission} models for a given server connection.
 *
 * @since 5.7
 */
public class MissionManager implements IMissionSyncComponent
{
    private final Map<String, String> nameToGuid = new ConcurrentHashMap<>();
    // All maps keyed by guid
    private final Map<String, Mission> missions = new ConcurrentHashMap<>();
    private final Map<String, Mission> downloadedMissions = new ConcurrentHashMap<>();
    private final Map<String, String> missionPasswords = new ConcurrentHashMap<>();
    private final Map<String, String> missionSubTokens = new ConcurrentHashMap<>();
    private final DataSyncManager dataSyncManager;
    private ITakServerConnection connection;

    /**
     * @param dataSyncManager Parent data-sync manager.
     * @param connection      Associated TAK server connection.
     */
    public MissionManager(@Nonnull DataSyncManager dataSyncManager, @Nonnull ITakServerConnection connection)
    {
        this.dataSyncManager = dataSyncManager;
        this.connection = connection;
    }

    /**
     * @param mission         Mission to store.
     * @param notifyListeners Flag to notify/skip listeners.
     */
    public void putMission(@Nonnull Mission mission, boolean notifyListeners)
    {
        String guid = mission.guid();
        nameToGuid.put(mission.name(), guid);
        Mission oldMission = missions.put(guid, mission);

        // Notify listeners.
        if (notifyListeners && !Objects.equals(mission, oldMission))
        {
            ExceptionUtils.safeDoOperation(
                    dataSyncManager.getDataSyncMissionListeners(),
                    oldMission == null
                            ? listener -> listener.onNewMission(connection, mission)
                            : listener -> listener.onUpdateMission(connection, oldMission, mission));
        }
    }

    /**
     * @param missionName Mission name.
     * @return Removed base mission model, if previously tracked.
     */
    @Nullable
    public Mission removeMissionByName(@Nonnull String missionName)
    {
        return mapNameToGuid(missionName).map(this::removeMissionByGuid).orElse(null);
    }

    /**
     * @param missionGuid Mission guid.
     * @return Removed base mission model, if previously tracked.
     */
    @Nullable
    public Mission removeMissionByGuid(@Nonnull String missionGuid)
    {
        Mission removed = missions.remove(missionGuid);
        if (removed != null) ExceptionUtils.safeDoOperation(dataSyncManager.getDataSyncMissionListeners(),
                listener -> listener.onRemoveMission(connection, removed));
        return removed;
    }

    /**
     * @param mission         Mission to store.
     * @param password        Mission password.
     * @param token           Mission subscription token.
     * @param notifyListeners Flag to notify/skip listeners.
     */
    public void putDownloadedMission(@Nonnull Mission mission, @Nullable String password, @Nullable String token, boolean notifyListeners)
    {
        putMission(mission, notifyListeners);

        String guid = mission.guid();
        Mission oldMission = downloadedMissions.put(guid, mission);
        if (!StringUtils.isBlank(password)) missionPasswords.put(guid, password);
        if (!StringUtils.isBlank(token)) missionSubTokens.put(guid, token);

        // Notify listeners.
        if (notifyListeners && !Objects.equals(mission, oldMission) && missionSubTokens.containsKey(guid))
        {
            ExceptionUtils.safeDoOperation(
                    dataSyncManager.getDataSyncSubscribedMissionListeners(),
                    oldMission == null
                            ? listener -> listener.onSubscribe(connection, mission)
                            : listener -> listener.onSubscribedMissionUpdate(connection, oldMission, mission));
        }
    }

    /**
     * @param missionName Mission name.
     * @return Removed full mission model, if previously tracked.
     */
    @Nullable
    public Mission removeDownloadedMissionByName(@Nonnull String missionName)
    {
        return mapNameToGuid(missionName)
                .map(this::removeDownloadedMissionByGuid)
                .orElse(null);
    }

    /**
     * @param missionGuid Mission guid.
     * @return Removed full mission model, if previously tracked.
     */
    @Nullable
    public Mission removeDownloadedMissionByGuid(@Nonnull String missionGuid)
    {
        boolean wasSubscribed = missionSubTokens.containsKey(missionGuid);
        Mission removed = downloadedMissions.remove(missionGuid);
        if (removed != null && wasSubscribed)
        {
            ExceptionUtils.safeDoOperation(dataSyncManager.getDataSyncSubscribedMissionListeners(),
                    listener -> listener.onUnsubscribe(connection, removed));
        }
        return removed;
    }

    /**
     * @param missionName Name of the mission to look up.
     * @return Base mission model, if tracked.
     */
    @Nullable
    public Mission getMissionByName(@Nonnull String missionName)
    {
        return mapNameToGuid(missionName)
                .map(this::getMissionByGuid)
                .orElse(null);
    }

    /**
     * @param missionGuid Guid of the mission to look up.
     * @return Base mission model, if tracked.
     */
    @Nullable
    public Mission getMissionByGuid(@Nonnull String missionGuid)
    {
        return missions.get(missionGuid);
    }

    /**
     * @param missionName Name of the downloaded mission to look up.
     * @return Full mission model, if tracked.
     */
    @Nullable
    public Mission getDownloadedMissionByName(@Nonnull String missionName)
    {
        return mapNameToGuid(missionName)
                .map(this::getDownloadedMissionByGuid)
                .orElse(null);
    }

    /**
     * @param missionGuid Guid of the downloaded mission to look up.
     * @return Full mission model, if tracked.
     */
    @Nullable
    public Mission getDownloadedMissionByGuid(@Nonnull String missionGuid)
    {
        return downloadedMissions.get(missionGuid);
    }

    /**
     * @param missionName Mission name.
     * @param token       Subscription token of mission to store.
     */
    public void putMissionSubscriptionTokenByName(@Nonnull String missionName, @Nonnull String token)
    {
        mapNameToGuid(missionName).ifPresent(guid -> putMissionSubscriptionTokenByGuid(guid, token));
    }

    /**
     * @param missionGuid Mission guid.
     * @param token       Subscription token of mission to store.
     */
    public void putMissionSubscriptionTokenByGuid(@Nonnull String missionGuid, @Nonnull String token)
    {
        missionSubTokens.put(missionGuid, token);
    }

    /**
     * @param missionName Name of the subscribed mission to get subscription token of.
     * @return Mission subscription token, if tracked.
     */
    @Nullable
    public String getMissionSubscriptionTokenByName(@Nonnull String missionName)
    {
        return mapNameToGuid(missionName)
                .map(this::getMissionSubscriptionTokenByGuid)
                .orElse(null);
    }

    /**
     * @param missionGuid Guid of the subscribed mission to get token of.
     * @return Mission subscription token, if tracked.
     */
    @Nullable
    public String getMissionSubscriptionTokenByGuid(@Nonnull String missionGuid)
    {
        return missionSubTokens.get(missionGuid);
    }

    /**
     * @param missionName Name of the subscribed mission to remove local token for.
     * @return Removed mission subscription token, if previously tracked.
     */
    @Nullable
    public String removeMissionSubscriptionTokenByName(@Nonnull String missionName)
    {
        return mapNameToGuid(missionName)
                .map(this::removeMissionSubscriptionTokenByGuid)
                .orElse(null);
    }

    /**
     * @param missionGuid Guid of the subscribed mission to remove local token for.
     * @return Removed mission subscription token, if previously tracked.
     */
    @Nullable
    public String removeMissionSubscriptionTokenByGuid(@Nonnull String missionGuid)
    {
        return missionSubTokens.remove(missionGuid);
    }

    /**
     * @param missionGuid Guid of the subscribed mission to get token of.
     * @return Mission password, if tracked.
     */
    @Nullable
    public String getMissionPasswordByGuid(@Nonnull String missionGuid)
    {
        return missionPasswords.get(missionGuid);
    }

    /**
     * @param missionName Name of the mission to look up.
     * @return {@code true} when the given {@link Mission mission model} is downloaded.
     * @see MissionTransfers Mission transfer manager for details about the full mission and all referenced contents being downloaded.
     */
    public boolean isDownloadedByName(@Nonnull String missionName)
    {
        return mapNameToGuid(missionName)
                .map(this::isDownloadedByGuid)
                .orElse(false);
    }

    /**
     * @param missionGuid Guid of the mission to look up.
     * @return {@code true} when the given {@link Mission mission model} is downloaded.
     * @see MissionTransfers Mission transfer manager for details about the full mission and all referenced contents being downloaded.
     */
    public boolean isDownloadedByGuid(@Nonnull String missionGuid)
    {
        return downloadedMissions.containsKey(missionGuid);
    }

    /**
     * @param missionName Name of the mission to look up.
     * @return {@code true} when subscribed to the given mission.
     */
    public boolean isSubscribedByName(@Nonnull String missionName)
    {
        return mapNameToGuid(missionName)
                .map(this::isSubscribedByGuid)
                .orElse(false);
    }

    /**
     * @param missionGuid Guid of the mission to look up.
     * @return {@code true} when subscribed to the given mission.
     */
    public boolean isSubscribedByGuid(@Nonnull String missionGuid)
    {
        return missionSubTokens.containsKey(missionGuid);
    }

    /**
     * @param uid Map item UID, {@link gov.tak.api.maps.MapItem#getUid()}.
     * @return {@code true} if any downloaded mission declares the given map item in {@link Mission#uids()}.
     */
    public boolean isMapItemUidOwned(@Nonnull String uid)
    {
        return downloadedMissions.values().stream()
                .flatMap(mission -> mission.uids().stream())
                .map(MissionAdd.TString::data)
                .anyMatch(uid::equals);
    }

    /**
     * @param uid Map item UID, {@link gov.tak.api.maps.MapItem#getUid()}.
     * @return List of all missions that declare the given map item in {@link Mission#uids()}.
     */
    @Nonnull
    public List<Mission> getMapItemUidOwners(@Nonnull String uid)
    {
        return downloadedMissions.values().stream()
                .filter(mission -> mission.uids().stream().anyMatch(u -> u.data().equals(uid)))
                .toList();
    }

    /**
     * @param sha256 File sha256.
     * @return {@code true} if any downloaded mission declares the given file resource in {@link Mission#contents()}.
     */
    public boolean isResourceOwned(@Nonnull String sha256)
    {
        return downloadedMissions.values().stream()
                .flatMap(mission -> mission.contents().stream())
                .map(MissionAdd.TResource::data)
                .map(Resource::hash)
                .anyMatch(sha256::equals);
    }

    /**
     * @param missionName Mission name.
     * @return Guid of mission, if tracked.
     */
    @Nullable
    public String getGuidFromName(@Nonnull String missionName)
    {
        return nameToGuid.get(missionName);
    }

    /**
     * @param missionName Mission name.
     * @return Optional of mapped mission guid, empty if not tracked.
     */
    @Nonnull
    private Optional<String> mapNameToGuid(@Nonnull String missionName)
    {
        return Optional.ofNullable(getGuidFromName(missionName));
    }

    /**
     * @return Set of all mission models.
     */
    @Nonnull
    public Set<Mission> getMissions()
    {
        // Collect highest detail mission models
        Map<String, Mission> copy = new HashMap<>();
        copy.putAll(missions);
        copy.putAll(downloadedMissions);

        // Flatten to set
        Set<Mission> set = Collections.newSetFromMap(new IdentityHashMap<>());
        set.addAll(copy.values());
        return set;
    }

    /**
     * @return Map of all downloaded missions, keyed by mission GUID.
     */
    @Nonnull
    protected Map<String, Mission> getDownloadedMissionMap()
    {
        return Collections.unmodifiableMap(downloadedMissions);
    }

    /**
     * @return Map of all mission subscription tokens, keyed by mission GUID.
     */
    @Nonnull
    protected Map<String, String> getMissionSubscriptionTokenMap()
    {
        return Collections.unmodifiableMap(missionSubTokens);
    }

    /**
     * @return Map of all mission passwords, keyed by mission GUID.
     */
    @Nonnull
    protected Map<String, String> getMissionPasswords()
    {
        return Collections.unmodifiableMap(missionPasswords);
    }

    @Nonnull
    @Override
    public ITakServerConnection getConnection()
    {
        return connection;
    }

    @Override
    public void setConnection(@Nonnull ITakServerConnection connection)
    {
        this.connection = connection;
    }
}