package gov.tak.api.comms.takserver;

import com.atakmap.commoncommo.CommoException;
import com.atakmap.commoncommo.Contact;
import gov.tak.api.comms.takserver.mission.data.Mission;
import gov.tak.api.comms.takserver.mission.data.UploadedResourceResponse;
import gov.tak.api.cot.exporter.ICotExporterManager;
import gov.tak.platform.datapackage.content.DataPackageContents;
import gov.takx.api.annotation.InjectableService;
import gov.takx.api.annotation.NoPluginImplementations;
import gov.takx.api.mapobject.IMapObject;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import java.io.File;
import java.io.IOException;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.function.Predicate;

/**
 * Service to manage data packages including:
 * <ul>
 *     <li>Querying remote packages</li>
 *     <li>Querying local packages</li>
 *     <li>Creating local packages</li>
 *     <li>Modifying local packages</li>
 *     <li>Uploading local packages to TAK Server</li>
 *     <li>Sending local packages to Commo contacts</li>
 * </ul>
 * Data packages are tracked by storing copies in the project local VFS under {@code DataPackages}.
 * One sub-directory is created per remote server, using the server's connection id as the name.
 * One directory, named {@code LocalImport}, holds all packages directly imported <i>(not downloaded from a server)</i>.
 * And another directory, named {@code PeerImport}, holds all packages directly received from Commo peers.
 *
 * @since 3.5
 */
@InjectableService
@NoPluginImplementations
public interface IDataPackageManager
{
    /**
     * Queries all TAK Server connections for available data packages.
     *
     * @param listener Listener to handle the resulting data package list response, or error handling should the query fail.
     */
    void queryRemoteDataPackages(@Nonnull IDataPackageQueryListener listener);

    /**
     * Queries a specific TAK Server for available data packages.
     *
     * @param connection The TAK Server connection to query for data packages.
     * @param listener Listener to handle the resulting data package list response, or error handling should the query fail.
     */
    void queryRemoteDataPackages(@Nonnull ITakServerConnection connection, @Nonnull IDataPackageQueryListener listener);

    /**
     * Downloads a data package from a specific TAK Server.
     *
     * @param connection The TAK Server connection hosting the data package.
     * @param resource The data package resource information.
     * @param listener Listener to handle the resulting data package download response, or error handling should the download fail.
     */
    void downloadDataPackage(@Nonnull ITakServerConnection connection, @Nonnull IResource resource, @Nonnull IDataPackageDownloadListener listener);

    /**
     * @return A list of all data packages stored locally.
     */
    @Nonnull
    List<DataPackageContents> getAllDataPackageContents();

    /**
     * @return The root directory in which packages are stored.
     *
     * @see #getServerDownloadRootDir(ITakServerConnection) For data packages downloaded from a given server.
     * @see #getPeerImportRootDir() For data packages received from commo contacts.
     * @see #getLocalImportRootDir() For data packages imported by the user.
     */
    @Nonnull
    File getRootDir();

    /**
     * @param connection Connection to TAK Server
     *
     * @return The directory in which downloads from the given server are stored.
     */
    @Nonnull
    File getServerDownloadRootDir(@Nonnull ITakServerConnection connection);

    /**
     * @return The directory in which peer imports from commo contacts are stored.
     */
    @Nonnull
    File getPeerImportRootDir();

    /**
     * @return The directory in which user imported local packages are stored.
     */
    @Nonnull
    File getLocalImportRootDir();

    /**
     * @param packageFile Data package file to check.
     *
     * @return {@code true} if the package was added via local importing.
     */
    boolean isLocalImport(@Nonnull File packageFile);

    /**
     * @param packageFile Data package file to check.
     *
     * @return {@code true} if the package was added via commo peer sharing.
     */
    boolean isPeerImport(@Nonnull File packageFile);

    /**
     * @param packageFile Data package file to check.
     *
     * @return {@code true} if the package was added via downloading from TAK Server.
     */
    boolean isServerDownload(@Nonnull File packageFile);

    /**
     * A look-up for where a data package is, or would be in the VFS if it were imported locally.
     *
     * @param name Name of data package to get the path of, assuming it is sourced from local importing.
     *
     * @return VFS file path to where the data package will be located. The path will always end in {@code .zip} extension.
     */
    @Nonnull
    File getLocalImportedPackagePath(@Nonnull String name);

    /**
     * A look-up for where a data package is, or would be in the VFS if it were transferred from a peer.
     *
     * @param name Name of data package to get the path of, assuming it is sourced from a peer transfer.
     *
     * @return VFS file path to where the data package will be located. The path will always end in {@code .zip} extension.
     */
    @Nonnull
    File getPeerImportedPackagePath(@Nonnull String name);

    /**
     * A look-up for where a data package is, or would be in the VFS if it were downloaded from a given server.
     *
     * @param connection TAK Server connection where the package is sourced from.
     * @param name Name of data package to get the path of, assuming it is downloaded from the given TAK Server.
     *
     * @return VFS file path to where the data package will be located. The path will always end in {@code .zip} extension.
     */
    @Nonnull
    File getServerDownloadedPackagePath(@Nonnull ITakServerConnection connection, @Nonnull String name);

    /**
     * @param name Name of the data package to create. Does not need to include the '.zip' extension.
     *
     * @return File location of created data package.
     *
     * @throws IOException When an existing data package by that name exists, the given name is blank, or creating the file failed for some other reason.
     */
    @Nonnull
    File createNewDataPackage(@Nonnull String name) throws IOException;

    /**
     * @param connection The TAK Server connection where the mission originates from.
     * @param mission The mission to outline in the mission package manifest.
     *
     * @return File location of created mission package. Overwrites old mission package files.
     *
     * @throws IOException When creating the file failed for any reason.
     *
     * @since 5.0
     */
    @Nonnull
    File createNewMissionPackage(@Nonnull ITakServerConnection connection, @Nonnull Mission mission) throws IOException;

    /**
     * @param packageFile Data package file to delete. Must be within the VFS already.
     *
     * @return {@link File#delete()} on the given file.
     *
     * @throws IOException Thrown when the data package file was not within the VFS.
     */
    boolean deleteDataPackage(@Nonnull File packageFile) throws IOException;

    /**
     * @param packageFile Data package file to update. Must be within the VFS already.
     * @param mapObjects Map objects to convert to a CoT event and include in the data package.
     *
     * @return Map of map objects to paths inside zip of added files.
     * Entries with {@code null} values mean that map object could not be converted to CoT.
     * Failure to convert occurs when {@link ICotExporterManager} does not support exporting of the given {@link IMapObject} sub-type.
     *
     * @throws IOException Thrown when the data package file was not within the VFS, or the package could not be updated for any reason.
     */
    @Nonnull
    Map<IMapObject, String> addMapObjectsToDataPackage(@Nonnull File packageFile, @Nonnull Collection<IMapObject> mapObjects) throws IOException;

    /**
     * @param packageFile Data package file to update. Must be within the VFS already.
     * @param mapObject Map object to convert to a CoT event and include in the data package.
     *
     * @return Path inside zip of added file, or {@code null} if the map object could not be converted to CoT.
     * Failure to convert to CoT occurs when {@link ICotExporterManager} does not support exporting of the given {@link IMapObject} sub-type to CoT,
     * resulting in the MapObject not being put into the DataPackage
     *
     * @throws IOException Thrown when the MapObject is convertable, but the package {@link File} could not be updated for any reason
     */
    @Nullable
    String addMapObjectToDataPackage(@Nonnull File packageFile, @Nonnull IMapObject mapObject) throws IOException;

    /**
     * @param packageFile Data package file to update.
     * @param mapObject Map object to convert to a CoT event and include in the data package.
     * @param attachmentFilter An optional filter for controlling which map object attachments are included.
     *
     * @return Path inside zip of added file, or {@code null} if the map object could not be converted to CoT.
     * Failure to convert to CoT occurs when {@link ICotExporterManager} does not support exporting of the given {@link IMapObject} sub-type to CoT,
     * resulting in the MapObject not being put into the DataPackage.
     *
     * @throws IOException Thrown when the MapObject is convertable, but the package {@link File} could not be updated for any reason
     * @since 5.3
     */
    @Nullable
    String addMapObjectToDataPackage(@Nonnull File packageFile, @Nonnull IMapObject mapObject, @Nullable Predicate<File> attachmentFilter) throws IOException;

    /**
     * @param packageFile Data package file to update. Must be within the VFS already.
     * @param file File to add to the data package.
     *
     * @return Path inside zip of added file.
     *
     * @throws IOException Thrown when the data package file was not within the VFS, or the package could not be updated for any reason.
     */
    @Nonnull
    String addFileToDataPackage(@Nonnull File packageFile, @Nonnull File file) throws IOException;

    /**
     * @param packageFile Data package file to update. Must be within the VFS already.
     * @param files Files to add to the data package.
     *
     * @return Map of map objects to paths inside zip of added files.
     *
     * @throws IOException Thrown when the data package file was not within the VFS, or the package could not be updated for any reason.
     */
    @Nonnull
    Map<File, String> addFilesToDataPackage(@Nonnull File packageFile, @Nonnull Collection<File> files) throws IOException;

    /**
     * @param packageFile Data package file to update. Must be within the VFS already.
     * @param imageFile Image file to add to the data package.
     *
     * @return Path inside zip of added file.
     *
     * @throws IOException Thrown when the data package file was not within the VFS, or the package could not be updated for any reason.
     */
    @Nonnull
    String addImageToDataPackage(@Nonnull File packageFile, @Nonnull File imageFile) throws IOException;

    /**
     * @param packageFile Data package file to update. Must be within the VFS already.
     * @param imageFiles Image files to add to the data package.
     *
     * @return Map of map objects to paths inside zip of added files.
     *
     * @throws IOException Thrown when the data package file was not within the VFS, or the package could not be updated for any reason.
     */
    @Nonnull
    Map<File, String> addImagesToDataPackage(@Nonnull File packageFile, @Nonnull Collection<File> imageFiles) throws IOException;

    /**
     * @param packageFile Data package file to update. Must be within the VFS already.
     * @param zipPath File path within the zip <i>(data package)</i> to remove.
     *
     * @return Path removed from the zip. May be {@code null} if the path was not in the data package to begin with.
     *
     * @throws IOException Thrown when the data package file was not within the VFS, or the package could not be updated for any reason.
     */
    @Nullable
    String removeFilesFromDataPackage(@Nonnull File packageFile, @Nonnull String zipPath) throws IOException;

    /**
     * @param packageFile Data package file to update. Must be within the VFS already.
     * @param zipPaths File paths within the zip <i>(data package)</i> to remove.
     *
     * @return Paths removed from the zip. Should match the input collection contents. If it does not, the zip was already missing those paths.
     *
     * @throws IOException Thrown when the data package file was not within the VFS, or the package could not be updated for any reason.
     */
    @Nonnull
    Collection<String> removeFilesFromDataPackage(@Nonnull File packageFile, @Nonnull Collection<String> zipPaths) throws IOException;

    /**
     * Extracts the requested file path from the given data package.
     * If the file has already been extracted, no extraction is done and the existing path is returned.
     *
     * @param packageFile Data package file containing the file to extract.
     * @param path File path within the zip <i>(data package)</i> to extract.
     *
     * @return Path to extracted file.
     *
     * @throws IOException Thrown when the data package file was not within the VFS, or the package could not be read from.
     *
     * @since 5.0
     */
    @Nonnull
    File extractDataPackageFile(@Nonnull File packageFile, @Nonnull String path) throws IOException;

    /**
     * Sends a data package to a contact.
     *
     * @param packageFile File path to local data package.
     * @param contact Contact to send the data package to.
     *
     * @throws CommoException When any common-commo issue occurs transferring the file.
     */
    void sendPackageToContact(@Nonnull File packageFile, @Nonnull Contact contact) throws CommoException;

    /**
     * Sends a data package to a list of contacts.
     *
     * @param packageFile File path to local data package.
     * @param contacts Contacts to send the data package to.
     *
     * @throws CommoException When any common-commo issue occurs transferring the file.
     */
    void sendPackageToContacts(@Nonnull File packageFile, @Nonnull List<Contact> contacts) throws CommoException;

    /**
     * Sends a data package to the given tak server.
     *
     * @param packageFile File path to local data package.
     * @param connection Connection to a tak server to upload to.
     *
     * @return Future of upload response.
     *
     * @throws IOException When the package file cannot be read from.
     */
    CompletableFuture<UploadedResourceResponse> sendPackageToServer(@Nonnull File packageFile, @Nonnull ITakServerConnection connection) throws IOException;

    /**
     * @param listener Listener to add.
     */
    void addDataPackageVfsListener(@Nonnull IDataPackageVfsListener listener);

    /**
     * @param listener Listener to remove.
     */
    void removeDataPackageVfsListener(@Nonnull IDataPackageVfsListener listener);
}
