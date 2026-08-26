package gov.tak.api.comms.takserver;

import com.atakmap.commoncommo.Contact;
import gov.tak.api.comms.takserver.mission.call.ApiException;
import gov.tak.api.comms.takserver.mission.data.InvitationType;
import gov.tak.api.comms.takserver.mission.data.LogEntry;
import gov.tak.api.comms.takserver.mission.data.Mission;
import gov.tak.api.comms.takserver.mission.data.MissionAdd;
import gov.tak.api.comms.takserver.mission.data.MissionBuilder;
import gov.tak.api.comms.takserver.mission.data.Resource;
import gov.tak.api.comms.takserver.mission.data.UploadedResourceResponse;
import gov.tak.api.cot.event.CotEvent;
import gov.tak.api.maps.MapItem;
import gov.takx.api.annotation.InjectableService;
import gov.takx.api.annotation.NoPluginImplementations;
import gov.takx.api.mapobject.IMapObject;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import java.io.File;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;

/**
 * Service to manage Data-Sync <i>(Mission-Sync)</i> operations. This includes:
 * <ul>
 *     <li>Receive updates of mission availability and detail updates on subscribed and unsubscribed missions</li>
 *     <ul>
 *         <li>Subscribed missions will have additional data in their models filled out</li>
 *     </ul>
 *     <li>Request downloading and subscription to available missions and their content</li>
 * </ul>
 *
 * @since 5.0
 */
@InjectableService
@NoPluginImplementations
public interface IDataSyncManager
{
    /**
     * Adds a listener to be notified of overall mission status, including:
     * <ul>
     *     <li>Newly created missions</li>
     *     <li>Updates to existing missions</li>
     *     <li>Removal of deleted missions</li>
     * </ul>
     *
     * @param listener Listener to add.
     */
    void addDataSyncMissionListener(@Nonnull IDataSyncMissionListener listener);

    /**
     * @param listener Listener to remove.
     */
    void removeDataSyncMissionListener(@Nonnull IDataSyncMissionListener listener);

    /**
     * Adds a listener to be notified of subscribed mission status, including:
     * <ul>
     *     <li>Subscriptions to missions</li>
     *     <li>Ubsubscriptions from missions</li>
     *     <li>Updates to subscribed missions, in a more detailed model than what is provided by {@link IDataSyncMissionListener}. Additional details include:</li>
     *     <ul>
     *         <li>{@link Mission#missionChanges()}</li>
     *         <li>{@link Mission#logs()}</li>
     *         <li>Any password protected content not available from the general {@link gov.tak.api.comms.takserver.mission.call.mission.GetAllMissions} query</li>
     *     </ul>
     * </ul>
     *
     * @param listener Listener to add.
     */
    void addDataSyncSubscribedMissionListener(@Nonnull IDataSyncSubscribedMissionListener listener);

    /**
     * @param listener Listener to remove.
     */
    void removeDataSyncSubscribedMissionListener(@Nonnull IDataSyncSubscribedMissionListener listener);

    /**
     * Adds a listener that will be notified when mission content {@link Resource}s are downloaded.
     * This allows you to listen to the download updates of all missions, as opposed to
     * {@link MissionDownload#fileDownloadFutures()} from the return value of {@link #download(ITakServerConnection, String, String)}
     * which is only accessible to the caller of that method.
     *
     * @param listener The listener to add.
     *
     * @see #download(ITakServerConnection, String, String) Initiates the downloading of a {@code Mission} by name, and all of its associated resources.
     */
    void addDataSyncMissionDownloadListener(@Nonnull IDataSyncMissionDownloadListener listener);

    /**
     * @param listener Listener to remove.
     */
    void removeDataSyncMissionDownloadListener(@Nonnull IDataSyncMissionDownloadListener listener);

    /**
     * @param connection Connection to server defining the mission.
     * @param missionName Name of mission to check ownership of.
     *
     * @return {@code true} when the current account connected to the TAK Server on this TAKX instance is the mission owner.
     */
    boolean isMissionOwner(@Nonnull ITakServerConnection connection, @Nonnull String missionName);

    /**
     * @param connection Connection to server defining the mission.
     * @param missionName Name of mission to check existence of.
     *
     * @return {@code true} when there is a mission by the given name on the given connection we are aware of locally.
     * @since 5.1
     */
    boolean isTracked(@Nonnull ITakServerConnection connection, @Nonnull String missionName);

    /**
     * @param connection Connection to server defining the mission.
     * @param missionName Name of mission to check subscription status of.
     *
     * @return {@code true} when the mission is subscribed to.
     */
    boolean isSubscribed(@Nonnull ITakServerConnection connection, @Nonnull String missionName);

    /**
     * @param connection Connection to server defining the mission.
     * @param missionName Name of mission to check for progress on any downloading,
     * including the mission outline itself or any associated file downloads.
     *
     * @return {@code true} if a mission is fully downloaded <i>(Including all associated resources of the mission)</i>.
     * {@code false} is the same as {@link #getDownloadState(ITakServerConnection, String)} being any value other than {@link MissionDownloadStatus#COMPLETED}
     *
     * @see #getDownloadState(ITakServerConnection, String) More fine-tune status information.
     */
    boolean isDownloaded(@Nonnull ITakServerConnection connection, @Nonnull String missionName);

    /**
     * @param connection Connection to server defining the mission.
     * @param missionName Name of mission to check download status of.
     *
     * @return Status of the mission download.
     *
     * @see #isDownloaded(ITakServerConnection, String) Simpler check to see if the status is equal to {@code MissionDownloadStatus.COMPLETED}.
     */
    @Nonnull
    MissionDownloadStatus getDownloadState(@Nonnull ITakServerConnection connection, @Nonnull String missionName);

    /**
     * When subscribing to missions users are given a token they can use in actions that require
     * verification they are subscribed to an associated mission.
     *
     * @param connection Connection to server defining the mission.
     * @param missionName Name of mission to get the token of.
     *
     * @return Subscription token of mission. Must be subscribed to the mission
     * via {@link #subscribe(ITakServerConnection, String, String)} to have the token recognized.
     * If the mission is not subscribed to then {@code null}.
     *
     * @since 5.1
     */
    @Nullable
    String getMissionSubscriptionToken(@Nonnull ITakServerConnection connection, @Nonnull String missionName);

    /**
     * @param connection Connection to server declaring the resource.
     * @param resource Resource outline.
     *
     * @return Local file path to download location. May be {@code null} if the connection has not been synchronized with yet.
     * File may not immediately exist after {@link #download(ITakServerConnection, String, String)} completes as associated
     * files with the mission are downloaded asynchronously.
     */
    @Nullable
    File getLocalDownloadedResource(@Nonnull ITakServerConnection connection, @Nonnull Resource resource);

    /**
     * @param connection Connection to server declaring the resource.
     * @param uid UID of CoT item.
     *
     * @return Local file path to download location. May be {@code null} if the connection has not been synchronized with yet.
     * File may not immediately exist after {@link #download(ITakServerConnection, String, String)} completes as associated
     * CoT files with the mission are downloaded asynchronously.
     */
    @Nullable
    File getLocalDownloadedCot(@Nonnull ITakServerConnection connection, @Nonnull String uid);

    /**
     * Gets a local resource by its sha256 hash. We use a {@link java.util.concurrent.CompletableFuture} to facilitate
     * yielding values that will be downloaded later in mission synchronization but have not yet been written.
     *
     * @param sha256 Hash of a resource to look up.
     * @return Future containing the local file path to the resource.
     * @since 5.3
     */
    @Nonnull
    CompletableFuture<File> getResourceByHash(@Nonnull String sha256);

    /**
     * Attaches a resource identified by its sha256 hash to a given map object.
     *
     * @param mapItem Map item to attach a file to.
     * @param sha256  Hash of a resource to look up and attach to the given map item.
     * @return Future containing the local file path to the attached resource.
     * @since 5.3
     */
    @Nonnull
    CompletableFuture<File> attachResourceByHash(@Nonnull MapItem mapItem, @Nonnull String sha256);

    /**
     * Requests the local downloading of a mission and all of its associated resources.
     *
     * @param connection Connection to server defining the mission.
     * @param missionName Name of mission to download.
     * @param password Password associated with the mission, if any.
     *
     * @return Future of downloaded mission and its associated resources.
     * Once the {@link MissionDownload#fileDownloadFutures()} are all complete the mission and its resources are all locally downloaded.
     *
     * @see #addDataSyncMissionDownloadListener(IDataSyncMissionDownloadListener) Can alternatively be used to listen to resource download updates generically.
     * @see #isDownloaded(ITakServerConnection, String) Can alternatively be used to check if the mission is <i>fully</i> downloaded, which includes all associated resources.
     * @see #getDownloadState(ITakServerConnection, String) Can alternatively be used to check if the mission is <i>fully</i> downloaded, which includes all associated resources.
     */
    @Nonnull
    CompletableFuture<MissionDownload> download(@Nonnull ITakServerConnection connection, @Nonnull String missionName, @Nullable String password);

    /**
     * Requests the deletion of the local mission download and all of its associated resources, excluding items still used by other downloaded missions.
     * An additional flag can be passed to request deletion of the mission on the connected TAK Server connection if the current user account has permission to do so.
     *
     * @param connection Connection to server defining the mission.
     * @param missionName Name of mission to delete.
     * @param deleteLocal {@code true} to also delete the mission's local files.
     * @param deleteRemote {@code true} to also delete the mission remotely, if allowed.
     *
     * @return Future of the deletion status.
     */
    @Nonnull
    CompletableFuture<MissionDeletion> delete(@Nonnull ITakServerConnection connection, @Nonnull String missionName, boolean deleteLocal, boolean deleteRemote);

    /**
     * Adds a file to the mission.
     *
     * @param connection Connection to server defining the mission.
     * @param missionName Name of mission to add the file to.
     * @param file File to add.
     *
     * @return Future of the addition.
     */
    @Nonnull
    CompletableFuture<MissionAddFile> addFile(@Nonnull ITakServerConnection connection, @Nonnull String missionName,
                                              @Nonnull File file);

    /**
     * Adds a map item to the mission.
     *
     * @param connection Connection to server defining the mission.
     * @param missionName Name of mission to add the map item to.
     * @param mapObject Map item to add.
     *
     * @return Future of the addition.
     */
    @Nonnull
    CompletableFuture<MissionAddMapItem> addMapItem(@Nonnull ITakServerConnection connection, @Nonnull String missionName,
                                                    @Nonnull IMapObject mapObject);

    /**
     * Adds a user log to the mission.
     *
     * @param connection Connection to server defining the mission.
     * @param missionName Name of mission to add the user log to.
     * @param entry User log entry text.
     * @param keywords User log's tags.
     * @param files Files to attach to the user log.
     * @param mapItem Map item to attach to the user log.
     *
     * @return Future of user log addition.
     */
    @Nonnull
    CompletableFuture<MissionAddLogEntry> addUserLog(@Nonnull ITakServerConnection connection, @Nonnull String missionName, @Nonnull String entry,
                                                     @Nullable List<String> keywords, @Nullable List<File> files, @Nullable IMapObject mapItem);

    /**
     * Subscribe to the given mission by name.
     *
     * @param connection Connection to server defining the mission.
     * @param missionName Name of mission to subscribe to.
     * @param password Password associated with the mission, if any.
     *
     * @return Future of downloaded mission and its associated resources.
     * Once the {@link MissionDownload#fileDownloadFutures()} are all complete the mission and its resources are all locally downloaded.
     *
     * @see #addDataSyncSubscribedMissionListener(IDataSyncSubscribedMissionListener) Can be used to track subscription and unsubscription of any mission.
     */
    @Nonnull
    CompletableFuture<MissionDownload> subscribe(@Nonnull ITakServerConnection connection, @Nonnull String missionName, @Nullable String password);

    /**
     * Subscribe to the given mission by name.
     *
     * @param connection Connection to server defining the mission.
     * @param missionName Name of mission to subscribe to.
     * @param token A subscription token to use for accessing the given mission.
     *
     * @return Future of downloaded mission and its associated resources.
     * Once the {@link MissionDownload#fileDownloadFutures()} are all complete the mission and its resources are all locally downloaded.
     *
     * @see gov.tak.api.comms.takserver.mission.call.mission.SendMissionInvitation Used to send an invitation to other TAK clients, which includes a subscription token.
     * @see #addDataSyncSubscribedMissionListener(IDataSyncSubscribedMissionListener) Can be used to track subscription and unsubscription of any mission.
     * @since 5.3
     */
    @Nonnull
    CompletableFuture<MissionDownload> subscribeWithToken(@Nonnull ITakServerConnection connection, @Nonnull String missionName, @Nonnull String token);

    /**
     * Unsubscribe from the given mission by name.
     *
     * @param connection Connection to server defining the mission.
     * @param missionName Name of mission to unsubscribe from.
     *
     * @see #addDataSyncSubscribedMissionListener(IDataSyncSubscribedMissionListener) Can be used to track subscription and unsubscription of any mission.
     */
    void unsubscribe(@Nonnull ITakServerConnection connection, @Nonnull String missionName);

    /**
     * Creates a CoT event that instructs the receiving client's event handler to automatically join the given mission.
     *
     * @param connection Connection to server defining the mission.
     * @param missionName Name of mission to create an invitation to.
     *
     * @return Cot event model for the mission invitation.
     * {@code null} when the invitation cannot be made, likely due to the mission not being tracked by data-sync.
     */
    @Nullable
    CotEvent createInviteCot(@Nonnull ITakServerConnection connection, @Nonnull String missionName);

    /**
     * Requests TAK Server invite the given contacts to a given mission by name.
     *
     * @param connection Connection to server defining the mission.
     * @param missionName Name of mission to create an invitation to.
     * @param contacts List of commo contacts to send the invitation to.
     *
     * @return Future of the invitation being processed by TAK Server.
     * Upon successful completion the server will rebroadcast an invitation to the given contacts.
     * @since 5.7
     */
    @Nonnull
    default CompletableFuture<Void> inviteToMission(@Nonnull ITakServerConnection connection, @Nonnull String missionName, @Nonnull Collection<Contact> contacts)
    {
        return inviteToMission(connection, missionName, InvitationType.CLIENT_UID, contacts.stream().map(c -> c.contactUID).toList());
    }

    /**
     * Requests TAK Server invite the given contacts to a given mission by name.
     *
     * @param connection Connection to server defining the mission.
     * @param missionName Name of mission to create an invitation to.
     * @param invitationType Format type of the provided contacts.
     * @param contacts List of contacts to send the invitation to. See {@link InvitationType}.
     *
     * @return Future of the invitation being processed by TAK Server.
     * Upon successful completion the server will rebroadcast an invitation to the given contacts.
     * @since 5.7
     */
    @Nonnull
    CompletableFuture<Void> inviteToMission(@Nonnull ITakServerConnection connection, @Nonnull String missionName, @Nonnull InvitationType invitationType, @Nonnull Collection<String> contacts);

    /**
     * Gets the current instances of all tracked missions across all connections.
     * <br>
     * The mission models returned will have the highest level of detail available, meaning if a mission is downloaded or subscribed to,
     * that model will be chosen over the lower detail model used for non-downloaded missions.
     *
     * @return Map view of current known missions.
     */
    @Nonnull
    Map<ITakServerConnection, Collection<Mission>> currentMissions();

    /**
     * Gets the current instance of the tracked mission.
     * <br>
     * The mission model returned will have the highest level of detail available, meaning if a mission is downloaded or subscribed to,
     * that model will be chosen over the lower detail model used for non-downloaded missions.
     * <br>
     * May be {@code null} if no such mission is tracked.
     * 
     * @param connection Connection to server defining the mission.
     * @param missionName Name of mission to get instance of.
     * @return Currently tracked instance of mission. {@code null} if no mission by the given name on the connection is tracked.
     */
    @Nullable
    Mission getMission(@Nonnull ITakServerConnection connection, @Nonnull String missionName);

    /**
     * Creates the {@link Mission} on the given {@link ITakServerConnection}, with an optional password, and subscribes to it.
     * Keywords are added during this, so the mission should have them set prior to calling this method.
     *
     * @param connection The TAK Server connection to create the mission against
     * @param mission the Mission model to create on the server
     * @param password the optional password for the mission
     * @return A {@link CompletableFuture<Mission>} that, when completed, contains the newly created mission from the TAK Server
     * @see MissionBuilder
     * @see MissionBuilder#withKeywords(List)
     * @see Mission#keywords()
     * @since 5.2
     */
    @Nonnull
    CompletableFuture<Mission> createMission(@Nonnull ITakServerConnection connection, @Nonnull Mission mission, @Nullable String password);

    /**
     * Holds information about adding a map item to a mission.
     *
     * @param uid Map item uid added.
     * @param error The error that occurred preventing adding the map item. {@code null} if the addition was a success.
     * @param updatedMissionFuture Future of the addition of the uploaded map item to the mission.
     */
    record MissionAddMapItem(@Nullable String uid, @Nullable ApiException error, @Nonnull CompletableFuture<Mission> updatedMissionFuture) implements IPossiblyErroneous
    {
        @Nonnull
        public static MissionAddMapItem failed(@Nonnull ApiException error)
        {
            return new MissionAddMapItem(null, error, CompletableFuture.failedFuture(error));
        }

        /**
         * Should only be used in contexts where the uid is assumed to be non-null.
         *
         * @return {@link #uid()} but assumed to be non-null.
         */
        @Nonnull
        public String requireUid()
        {
            return Objects.requireNonNull(uid);
        }
    }

    /**
     * Holds information about adding a file to a mission.
     *
     * @param response Response from the server outlining the uploaded file's metadata.
     * @param error The error that occurred preventing adding the file. {@code null} if the addition was a success.
     * @param updatedMissionFuture Future of the addition of the uploaded resource to the mission.
     */
    record MissionAddFile(@Nullable UploadedResourceResponse response, @Nullable ApiException error,
            @Nonnull CompletableFuture<Mission> updatedMissionFuture) implements IPossiblyErroneous
    {
    }

    /**
     * Holds information about adding a user log.
     *
     * @param fileAttachFutures Pre-requisite futures for attaching files.
     * @param mapItemAttachFuture Pre-requisite future for attaching a map item. {@code null} when no map items were requested as attachments.
     * @param logEntryFuture Future of the user log addition. {@code null} if there is an error uploading the log entry.
     * @param error The error that occurred preventing log addition. {@code null} if the log creation was a success.
     */
    record MissionAddLogEntry(@Nonnull List<CompletableFuture<MissionAddFile>> fileAttachFutures,
            @Nullable CompletableFuture<MissionAddMapItem> mapItemAttachFuture,
            @Nullable CompletableFuture<LogEntry> logEntryFuture,
            @Nullable ApiException error) implements IPossiblyErroneous
    {
    }

    /**
     * Holds information about the deletion request of a mission.
     *
     * @param local Deletion of the local mission and its files was requested.
     * @param remote Deletion of the remote mission was requested.
     * @param error The error that occurred preventing deletion. {@code null} if the deletion was a success.
     */
    record MissionDeletion(boolean local, boolean remote, @Nullable ApiException error) implements IPossiblyErroneous
    {
    }

    /**
     * Holds a successfully downloaded mission model, and futures for the downloads of all the mission's associated resources and map items.
     * <p>
     * You can check for the completion of resource downloads via:
     * <pre>
     * {@code
     * missionDownload.fileDownloadFutures().stream()
     *     .allMatch(future -> future.isDone() && !future.isCompletedExceptionally());
     * }</pre>
     * Or, if you need to be able to listen asynchronously to the completion of all files being completed:
     * <pre>
     * {@code
     * CompletableFuture.allOf(missionDownload.fileDownloadFutures())
     * }</pre>
     *
     * @param mission The downloaded mission model. {@code null} if an error occurred.
     * @param error The error that occurred preventing mission model downloading.
     * @param fileDownloadFutures List of futures for each {@link Mission#contents()} entry download.
     */
    record MissionDownload(@Nullable Mission mission, @Nullable ApiException error,
            @Nonnull List<CompletableFuture<MissionResourceDownload>> fileDownloadFutures,
            @Nonnull List<CompletableFuture<MissionMapItemDownload>> mapItemDownloadFutures) implements IPossiblyErroneous
    {
        /**
         * @param mission Downloaded mission model.
         * @param fileDownloadFutures Futures of mission resource downloads.
         * @param mapItemDownloadFutures Futures of mission map item downloads.
         *
         * @return Summary of successful download.
         */
        @Nonnull
        public static MissionDownload newSuccessfulDownload(@Nonnull Mission mission,
                                                            @Nonnull List<CompletableFuture<MissionResourceDownload>> fileDownloadFutures,
                                                            @Nonnull List<CompletableFuture<MissionMapItemDownload>> mapItemDownloadFutures)
        {
            return new MissionDownload(mission, null, fileDownloadFutures, mapItemDownloadFutures);
        }

        /**
         * @param error Exception that prevented the mission model from being downloaded.
         *
         * @return Summary of failed download.
         */
        @Nonnull
        public static MissionDownload newFailedDownload(@Nonnull ApiException error)
        {
            return new MissionDownload(null, error, Collections.emptyList(), Collections.emptyList());
        }
    }

    /**
     * Holds a resource model and the local file path where it is downloaded to.
     *
     * @param resource Information about the resource.
     * @param localFilePath File path to where the resource is downloaded to.
     */
    record MissionResourceDownload(@Nonnull IResource resource, @Nonnull File localFilePath)
    {
    }

    /**
     * Holds a CoT item's details and the local file path where the full CoT XML is downloaded to.
     *
     * @param cotDetails Details about the CoT event.
     * @param localFilePath File path to where the CoT XML is downloaded to.
     */
    record MissionMapItemDownload(@Nonnull MissionAdd<String> cotDetails, @Nonnull File localFilePath)
    {
    }
}
