package gov.tak.core.comms.takserver;

import com.atakmap.commoncommo.CommoException;
import com.atakmap.commoncommo.Contact;
import com.atakmap.commoncommo.MissionPackageReceiveStatusUpdate;
import com.atakmap.commoncommo.MissionPackageTransferException;
import com.atakmap.commoncommo.MissionPackageTransferStatus;
import com.atakmap.comms.NetConnectString;
import com.atakmap.filesystem.HashingUtils;
import com.google.common.annotations.VisibleForTesting;
import feign.Response;
import gov.tak.api.comms.commo.ICommonCommoManager;
import gov.tak.api.comms.takserver.IDataPackageDownloadListener;
import gov.tak.api.comms.takserver.IDataPackageManager;
import gov.tak.api.comms.takserver.IDataPackageQueryListener;
import gov.tak.api.comms.takserver.IDataPackageVfsListener;
import gov.tak.api.comms.takserver.IExtendedDataPackageManager;
import gov.tak.api.comms.takserver.IResource;
import gov.tak.api.comms.takserver.ITakServerConnection;
import gov.tak.api.comms.takserver.ITakServerConnectionManager;
import gov.tak.api.comms.takserver.mission.TakServerApiClient;
import gov.tak.api.comms.takserver.mission.data.AssociatedResource;
import gov.tak.api.comms.takserver.mission.data.Mission;
import gov.tak.api.comms.takserver.mission.data.Resource;
import gov.tak.api.comms.takserver.mission.data.UploadedResourceResponse;
import gov.tak.api.contact.IContactNameResolver;
import gov.tak.api.cot.event.CotEvent;
import gov.tak.api.cot.exporter.ICotExporterManager;
import gov.tak.core.comms.commo.MissionPackageIOHandler;
import gov.tak.platform.datapackage.PackageImporter;
import gov.tak.platform.datapackage.PackageReader;
import gov.tak.platform.datapackage.content.DataPackageContents;
import gov.tak.platform.datapackage.content.DataPackageEntry;
import gov.tak.platform.datapackage.content.PackageManifest;
import gov.tak.platform.datapackage.content.PackageManifestContent;
import gov.tak.platform.datapackage.content.PackageManifestContents;
import gov.takx.api.annotation.VFSManagerScope;
import gov.takx.api.event.ShuttingDown;
import gov.takx.api.filesystem.IVirtualFileSystemManager;
import gov.takx.api.filesystem.VFSScope;
import gov.takx.api.mapobject.IMapObject;
import gov.takx.api.notification.INotificationService;
import gov.takx.api.notification.Notification;
import gov.takx.api.platform.ICoreExecutorService;
import gov.takx.api.systemmessage.ISystemMessageManager;
import gov.takx.api.tasks.ITaskManager;
import gov.takx.api.tasks.ITaskStatusHandler;
import gov.takx.api.tasks.TaskCompletionStatus;
import gov.takx.api.tasks.TaskGroup;
import gov.takx.commons.file.FilePath;
import gov.takx.commons.file.FileUtils;
import gov.takx.commons.file.SizeFormatter;
import gov.takx.core.chat.ChatDataPackageService;
import gov.takx.core.tasks.AExitAffectingTask;
import gov.takx.platform.preferences.PreferenceEntry;
import gov.takx.platform.utils.ErrorThrowingFunction;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import jakarta.inject.Inject;
import jakarta.xml.bind.JAXBContext;
import jakarta.xml.bind.JAXBException;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.http.HttpStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedOutputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.StringReader;
import java.lang.invoke.MethodHandles;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.attribute.FileTime;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CancellationException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipOutputStream;

import static gov.takx.api.filesystem.VfsConstants.FILES_DIR;

/**
 * Service implementation of {@link IDataPackageManager}.
 *
 * @since 3.5
 */
@ApplicationScoped
public class DataPackageManager implements IExtendedDataPackageManager, IDataPackageVfsListener, MissionPackageIOHandler.PackageReceiver
{
    private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());
    private static final String VERIFICATION_FAILED_MSG = "Download file '%s' from %s has incorrect SHA256 hash";
    private static final String DOWNLOAD_SUCCEEDED = "Downloaded '%s' from %s, size = %s";
    private static final String DOWNLOAD_FAILED = "Failed to download '%s' from %s (status code %d)";
    @VisibleForTesting
    static final String MANIFEST_PATH = "MANIFEST/manifest.xml";
    private static final String UNKNOWN = "Unknown";
    private static final String DATA_PACKAGES_DIR = "DataPackages";
    private static final String LOCAL_IMPORT_DIR = "LocalImport";
    private static final String PEER_IMPORT_DIR = "PeerImport";
    private static final String EXTRACTED_DIR = "extracted";
    private static final Predicate<Path> IS_ZIP_FILE =
            path -> !Files.isDirectory(path) && StringUtils.endsWithIgnoreCase(path.toString(), ".zip");
    private static final Path JAVA_IO_TMPDIR_PATH = Path.of(System.getProperty("java.io.tmpdir"));;
    private static final Path TEMP_DIR_PATH;

    static
    {
        Path tempDir = JAVA_IO_TMPDIR_PATH;
        try
        {
            // creating this here with some arbitrary prefix is required to ensure that this functionality passes tests on the pipeline
            tempDir = Files.createTempDirectory(tempDir, "takx_temp_dp");
            tempDir.toFile().deleteOnExit();
        } catch (IOException e)
        {
            logger.error("Could not create the DataPackage temp directory", e);
        }
        TEMP_DIR_PATH = tempDir;
    }

    /**
     * @since 5.2 (made constant)
     */
    @VisibleForTesting
    static final String EMPTY_MANIFEST = """
            <MissionPackageManifest version="2">
              <Configuration>
                <Parameter name="name" value="%s" />
                <Parameter name="uid" value="%s" />
              </Configuration>
              <Contents/>
            </MissionPackageManifest>
            """;

    /**
     * @since 5.2
     */
    @VisibleForTesting
    static final String EMPTY_TEMP_MANIFEST = """
            <MissionPackageManifest version="2">
              <Configuration>
                <Parameter name="name" value="%s" />
                <Parameter name="uid" value="%s" />
                <Parameter name="onReceiveImport" value="true"/>
                <Parameter name="onReceiveDelete" value="true"/>
              </Configuration>
              <Contents/>
            </MissionPackageManifest>
            """;

    private final Map<File, PendingTransfer> pendingTransfers = new ConcurrentHashMap<>();
    private final List<IDataPackageVfsListener> vfsListeners = new CopyOnWriteArrayList<>();
    private final List<ITransferListener> transferLogListeners = new CopyOnWriteArrayList<>();
    private final List<TransferLogItem> transferLog = new CopyOnWriteArrayList<>();
    private final TakServerApiClient takServerApiClient;
    private final ITakServerConnectionManager takServerConnectionManager;
    private final IContactNameResolver contactNameResolver;
    private final ICommonCommoManager commonCommoManager;
    private final ICotExporterManager cotExporterManager;
    private final ICoreExecutorService executorService;
    private final ITaskManager taskManager;
    private final ISystemMessageManager messageManager;
    private final IVirtualFileSystemManager vfsManager;
    private final INotificationService notificationService;
    private final ChatDataPackageService chatDataPackageService;
    private JAXBContext manifestJaxbContext;

    @Inject
    DataPackageManager(@VFSManagerScope(VFSScope.Project) IVirtualFileSystemManager vfsManager,
                       TakServerApiClient takServerApiClient, ITakServerConnectionManager takServerConnectionManager,
                       IContactNameResolver contactNameResolver, ICommonCommoManager commonCommoManager,
                       ICotExporterManager cotExporterManager, MissionPackageIOHandler missionPackageIOHandler,
                       ITaskManager taskManager, ICoreExecutorService executorService, ISystemMessageManager messageManager,
                       INotificationService notificationService, ChatDataPackageService chatDataPackageService)
    {
        this.takServerApiClient = takServerApiClient;
        this.takServerConnectionManager = takServerConnectionManager;
        this.contactNameResolver = contactNameResolver;
        this.commonCommoManager = commonCommoManager;
        this.cotExporterManager = cotExporterManager;
        this.executorService = executorService;
        this.taskManager = taskManager;
        this.messageManager = messageManager;
        this.vfsManager = vfsManager;
        this.notificationService = notificationService;
        this.chatDataPackageService = chatDataPackageService;
        missionPackageIOHandler.setReceiver(this);
    }

    /**
     * Cleanup our temp package directory.
     * @since 5.3
     */
    private void onShutdown(@Observes ShuttingDown ignored)
    {
        try
        {
            // Don't attempt to delete if we failed to create our unique DP temp dir
            // If TEMP_DIR_PATH doesn't exist, the call to isSameFile will throw an exception
            if (TEMP_DIR_PATH.toFile().exists() && !Files.isSameFile(TEMP_DIR_PATH, JAVA_IO_TMPDIR_PATH))
            {
                org.apache.commons.io.FileUtils.forceDeleteOnExit(TEMP_DIR_PATH.toFile());
            }
        } catch (IOException e)
        {
            logger.warn("Failed to delete temporary directory: {}", TEMP_DIR_PATH, e);
        }
    }

    @Override
    public void queryRemoteDataPackages(@Nonnull IDataPackageQueryListener listener)
    {
        for (ITakServerConnection connection : takServerConnectionManager.getTakServerConnections())
        {
            if (connection.isConnected()) queryRemoteDataPackages(connection, listener);
        }
    }

    @Override
    public void queryRemoteDataPackages(@Nonnull ITakServerConnection connection, @Nonnull IDataPackageQueryListener listener)
    {
        takServerApiClient.syncEndpoints(connection)
                .searchResources()
                .withMissionPackageFiltering()
                .asyncGet(executorService)
                .orTimeout(PreferenceEntry.DATA_PACKAGE_QUERY_TIMEOUT_SEC.getIntegerValue(), TimeUnit.SECONDS)
                .thenAccept(results -> {
                    List<AssociatedResource> enhancedResults = results.stream()
                            .map(DataPackageManager::ensureResourceDataIsFilled)
                            .map(this::associateResourceToCallsign)
                            .filter(associatedResource -> !chatDataPackageService.isChatPackage(associatedResource.fileName()))
                            .toList();
                    listener.onPackageListResponse(connection, enhancedResults);
                })
                .exceptionally(throwable -> {
                    String connectionName = connection.getDisplayName();
                    String errorMessage = String.format("Failed to query Data Packages from: '%s'", connectionName);
                    TakServerUtilities.handleConnectionError(notificationService, connectionName, throwable, errorMessage);
                    logger.debug(errorMessage, throwable);
                    listener.onPackageListError(connection, throwable);
                    return null;
                });
    }

    @Override
    public void downloadDataPackage(@Nonnull ITakServerConnection connection, @Nonnull IResource resource,
                                    @Nonnull IDataPackageDownloadListener listener)
    {
        final File downloadDirFile = getServerDownloadRootDir(connection);
        final Path downloadDirPath = downloadDirFile.toPath();

        // Ensure that the download directory exists
        if (!downloadDirFile.exists() && !downloadDirFile.mkdirs())
        {
            final String msg = "Failed to create directory: " + downloadDirPath;
            logger.error("Failed to create directory: {}", downloadDirPath);

            listener.onPackageDownloadError(connection, new IOException(msg));
        } else
        {
            final DownloadRequest downloadRequest = new DownloadRequest(connection, resource, downloadDirPath);
            final DownloadTask task = new DownloadTask(downloadRequest);

            taskManager.execute(task);

            task.getDownloadFuture().thenAccept(download -> {
                Path localPath = download.getLocalFilePath();
                AssociatedResource associatedResource = associateResourceToCallsign(resource);

                // Notify listeners of the new package
                DataPackageContents contents = mapToContentsWithOwner(localPath.toFile(), associatedResource.creatorUid());
                onPackageAdded(localPath.toFile(), contents);

                // Pass back to listener param
                listener.onPackageDownloadSuccess(connection, associatedResource, localPath);
            }).exceptionally(t -> {
                logger.error("Failed to download data package: {}", resource.name(), t);
                listener.onPackageDownloadError(connection, t);
                return null;
            });
        }
    }

    @Override
    @Nonnull
    public List<DataPackageContents> getAllDataPackageContents()
    {
        return getAllDataPackages().stream().map(p -> mapToContentsWithOwner(p, null)).toList();
    }

    @Override
    @Nonnull
    public File getRootDir()
    {
        return ensureExists(vfsManager.getRoot(new FilePath(FILES_DIR, DATA_PACKAGES_DIR).toString()));
    }

    @Override
    @Nonnull
    public File getServerDownloadRootDir(@Nonnull ITakServerConnection connection)
    {
        return ensureExists(new File(getRootDir(), connection.getId().toString()));
    }

    @Override
    @Nonnull
    public File getPeerImportRootDir()
    {
        return ensureExists(new File(getRootDir(), PEER_IMPORT_DIR));
    }

    @Override
    @Nonnull
    public File getLocalImportRootDir()
    {
        return ensureExists(new File(getRootDir(), LOCAL_IMPORT_DIR));
    }

    @Nonnull
    @Override
    public File createNewDataPackage(@Nonnull String name) throws IOException
    {
        return createNewDataPackage(name, this::getLocalImportedPackagePath, EMPTY_MANIFEST);
    }

    @Nonnull
    @Override
    public File createNewTempDataPackage(@Nonnull String name) throws IOException
    {
        final File tempDir = Files.createTempDirectory(TEMP_DIR_PATH, "dp").toFile();

        final File dataPackageFile = createNewDataPackage(name,
                // We need a temp directory to allow for duplicate file names
                fileName -> getImportedPackagePath(fileName, tempDir),
                EMPTY_TEMP_MANIFEST);

        // This must be done after the folder contents are created for the deletion to work
        org.apache.commons.io.FileUtils.forceDeleteOnExit(tempDir);

        return dataPackageFile;
    }

    @Nonnull
    @Override
    public File createNewMissionPackage(@Nonnull ITakServerConnection connection, @Nonnull Mission mission) throws IOException
    {
        // Create or replace any package with the expected name, as we expect following mission exports to override the same file.
        String name = mission.name();
        File newPackageFile = getLocalImportedPackagePath(name);
        if (newPackageFile.exists()) newPackageFile.delete();

        // Create a zip file at the location with a basic mission package manifest.
        String netConnectString = connection.getConnectionConfig().getNetConnectString().toString();
        String missionUid = netConnectString.replace(':', '-') + '-' + name;
        String emptyManifest = """
                <MissionPackageManifest version="2">
                  <Configuration>
                    <Parameter name="name" value="%s" />
                    <Parameter name="uid" value="%s" />
                    <Parameter name="onReceiveImport" value="True" />
                    <Parameter name="onReceiveDelete" value="False" />
                    <Parameter name="mission_uid" value="%s" />
                    <Parameter name="mission_label" value="%s" />
                    <Parameter name="mission_lastsynctime" value="%d" />
                    <Parameter name="mission_name" value="%s" />
                    <Parameter name="mission_server" value="%s" />
                  </Configuration>
                  <Contents/>
                </MissionPackageManifest>
                """.formatted(name, name,
                missionUid,
                name,
                FileTime.from(Instant.now()).toMillis(),
                netConnectString,
                UUID.randomUUID().toString());

        try (ZipOutputStream out = new ZipOutputStream(new FileOutputStream(newPackageFile)))
        {
            out.putNextEntry(new ZipEntry(MANIFEST_PATH));
            out.write(emptyManifest.getBytes());
            out.closeEntry();
        }

        String owner = Objects.requireNonNullElse(contactNameResolver.resolveName(mission.creatorUid()), DataPackageContents.LOCAL_IMPORT_OWNER);
        onPackageAdded(newPackageFile, new DataPackageContents(newPackageFile, owner));

        return newPackageFile;
    }

    @Override
    public boolean deleteDataPackage(@Nonnull File packageFile) throws IOException
    {
        assertPackageWithinVFS(packageFile);

        boolean deleted = packageFile.delete();

        // Notify listeners of the removal
        if (deleted) onPackageRemoved(packageFile);

        return deleted;
    }

    @Nonnull
    @Override
    public Map<IMapObject, String> addMapObjectsToDataPackage(@Nonnull File packageFile, @Nonnull Collection<IMapObject> mapObjects) throws IOException
    {
        Map<IMapObject, Integer> mapObjectToResultIndex = new IdentityHashMap<>();
        List<MemoryContentToAdd> contentToAdd = new ArrayList<>();
        for (IMapObject mapObject : mapObjects)
        {
            CotEvent exported = cotExporterManager.export(mapObject);
            if (exported != null)
            {
                // Add the map object CoT.
                byte[] mapObjectXmlBytes = exported.toString().getBytes(StandardCharsets.UTF_8);
                String mapObjectUid = mapObject.getUid();
                mapObjectToResultIndex.put(mapObject, contentToAdd.size());
                contentToAdd.add(new MemoryContentToAdd(mapObjectXmlBytes, mapObjectUid + ".cot", "map-items", entry -> entry.setUid(mapObjectUid)));

                // Add associated attachments if they exist.
                List<File> mapObjectAttachments = mapObject.getAttachments();
                for (File attachment : mapObjectAttachments)
                {
                    if (!attachment.exists()) continue;
                    contentToAdd.add(new MemoryContentToAdd(Files.readAllBytes(attachment.toPath()), attachment.getName(), "attachments", entry -> entry.setUid(mapObjectUid)));
                }
            } else
            {
                logger.error("Could not export map object to CoT xml: '{}' of type '{}'", mapObject.getName(), mapObject.getType());
                return Collections.emptyMap();
            }
        }

        // Add the contents to the zip.
        List<String> addedZipPaths = addFileToDataPackageInternalViaMemory(packageFile, contentToAdd);

        // Map our map object to content addition index to the resulting zip paths.
        Map<IMapObject, String> mapObjectToAddedZipPaths = mapObjectToResultIndex.keySet().stream()
                .collect(Collectors.toMap(Function.identity(), mo -> addedZipPaths.get(mapObjectToResultIndex.get(mo))));
        if (!mapObjectToAddedZipPaths.isEmpty())
        {
            onPackageUpdated(packageFile, mapToContentsWithOwner(packageFile, null));
        }
        return mapObjectToAddedZipPaths;
    }

    @Nullable
    @Override
    public String addMapObjectToDataPackage(@Nonnull File packageFile, @Nonnull IMapObject mapObject) throws IOException
    {
        return addMapObjectToDataPackage(packageFile, mapObject, null);
    }

    @Nullable
    @Override
    public String addMapObjectToDataPackage(@Nonnull File packageFile, @Nonnull IMapObject mapObject, @Nullable Predicate<File> attachmentFilter) throws IOException
    {
        CotEvent exported = cotExporterManager.export(mapObject);
        if (exported != null)
        {
            // Add the map object CoT.
            byte[] mapObjectXmlBytes = exported.toString().getBytes(StandardCharsets.UTF_8);
            String mapObjectUid = mapObject.getUid();
            List<MemoryContentToAdd> contentToAdd = new ArrayList<>();
            contentToAdd.add(new MemoryContentToAdd(mapObjectXmlBytes, mapObjectUid + ".cot", "map-items", entry -> entry.setUid(mapObjectUid)));

            // Add associated attachments if they exist and pass the attachment filter (if one is given).
            List<File> mapObjectAttachments = mapObject.getAttachments();
            for (File attachment : mapObjectAttachments)
            {
                if (!attachment.exists() || (attachmentFilter != null && !attachmentFilter.test(attachment))) continue;
                contentToAdd.add(new MemoryContentToAdd(Files.readAllBytes(attachment.toPath()), attachment.getName(), "attachments", entry -> entry.setUid(mapObjectUid)));
            }

            // Update the data package and notify listeners that the package was updated.
            String mapObjectPath = addFileToDataPackageInternalViaMemory(packageFile, contentToAdd).get(0);
            onPackageUpdated(packageFile, mapToContentsWithOwner(packageFile, null));
            return mapObjectPath;
        } else
        {
            logger.error("Could not export map object to CoT xml: '{}' of type '{}'", mapObject.getName(), mapObject.getType());
        }
        return null;
    }

    @Nonnull
    @Override
    public String addFileToDataPackage(@Nonnull File packageFile, @Nonnull File file) throws IOException
    {
        String addedPath = addFileToDataPackageInternalViaFile(packageFile, List.of(new FileContentToAdd(file, "files", null))).get(file);
        onPackageUpdated(packageFile, mapToContentsWithOwner(packageFile, null));
        return addedPath;
    }

    @Nonnull
    @Override
    public Map<File, String> addFilesToDataPackage(@Nonnull File packageFile, @Nonnull Collection<File> files) throws IOException
    {
        Map<File, String> addedImages = addFileToDataPackageInternalViaFile(packageFile, files.stream().map(file -> new FileContentToAdd(file, "files", null)).toList());
        if (!addedImages.isEmpty()) onPackageUpdated(packageFile, mapToContentsWithOwner(packageFile, null));
        return addedImages;
    }

    @Nonnull
    @Override
    public String addImageToDataPackage(@Nonnull File packageFile, @Nonnull File imageFile) throws IOException
    {
        String addedPath = addFileToDataPackageInternalViaFile(packageFile, List.of(new FileContentToAdd(imageFile, "images", null))).get(imageFile);
        onPackageUpdated(packageFile, mapToContentsWithOwner(packageFile, null));
        return addedPath;
    }

    @Nonnull
    @Override
    public Map<File, String> addImagesToDataPackage(@Nonnull File packageFile, @Nonnull Collection<File> imageFiles) throws IOException
    {
        Map<File, String> addedImages = addFileToDataPackageInternalViaFile(packageFile, imageFiles.stream().map(file -> new FileContentToAdd(file, "images", null)).toList());
        if (!addedImages.isEmpty()) onPackageUpdated(packageFile, mapToContentsWithOwner(packageFile, null));
        return addedImages;
    }

    @Nullable
    @Override
    public String removeFilesFromDataPackage(@Nonnull File packageFile, @Nonnull String zipPath) throws IOException
    {
        Collection<String> removed = removeFilesFromDataPackageInternal(packageFile, Set.of(zipPath));
        if (removed.isEmpty()) return null;
        onPackageUpdated(packageFile, mapToContentsWithOwner(packageFile, null));
        return removed.iterator().next();
    }

    @Nonnull
    @Override
    public Collection<String> removeFilesFromDataPackage(@Nonnull File packageFile, @Nonnull Collection<String> zipPaths) throws IOException
    {
        Collection<String> removed = removeFilesFromDataPackageInternal(packageFile, zipPaths);
        if (!removed.isEmpty()) onPackageUpdated(packageFile, mapToContentsWithOwner(packageFile, null));
        return removed;
    }

    @Nonnull
    @Override
    public File extractDataPackageFile(@Nonnull File packageFile, @Nonnull String path) throws IOException
    {
        String pathFileName = FilenameUtils.getName(path);
        if (!packageFile.exists()) throw new FileNotFoundException("The data package '" + pathFileName + "' does not exist");

        File extractedDirectory = getExtractedDirectory();
        File packageDir = new File(extractedDirectory, FileUtils.getNameWithoutExtension(packageFile));
        File extractedFile = new File(packageDir, pathFileName);
        if (!packageDir.exists()) packageDir.mkdirs();
        if (!extractedFile.exists())
        {
            try (ZipFile zipFile = new ZipFile(packageFile))
            {
                ZipEntry entry = zipFile.getEntry(path);
                if (entry == null) throw new FileNotFoundException("The path '" + path + "' does not exist in the data package '" + pathFileName + "'");
                InputStream in = zipFile.getInputStream(entry);
                Files.write(extractedFile.toPath(), in.readAllBytes());
            }
        }
        return extractedFile;
    }

    @Nonnull
    @Override
    public File copyPackageToVFS(@Nonnull File packageFile) throws IOException
    {
        // When copying packages to the VFS we want to always allow this operation.
        // So, when an existing file with the same name exists we want to modify the incoming package file to have a unique name when copied over.
        File target = FileUtils.getUniqueFileName(getLocalImportRootDir(), packageFile);

        // Ensure all imported files have '.zip' extensions
        if (!"zip".equals(FileUtils.getExtension(target)))
        {
            target = new File(target.getAbsolutePath() + ".zip");
        }

        Files.copy(packageFile.toPath(), target.toPath());

        // Notify listeners of the new package
        DataPackageContents contents = mapToContentsWithOwner(target, null);
        onPackageAdded(target, contents);

        return target;
    }

    @Override
    public void clearTransferLog()
    {
        transferLog.clear();
        for (ITransferListener listener : transferLogListeners)
        {
            try
            {
                listener.onTransfersCleared();
            } catch (Throwable t)
            {
                logger.error("Encountered an error in listener handling transfer log clearing", t);
            }
        }
    }

    @Nonnull
    @Override
    public List<TransferLogItem> getTransferLog()
    {
        return Collections.unmodifiableList(transferLog);
    }

    @Override
    public void addTransferListener(@Nonnull ITransferListener listener)
    {
        transferLogListeners.add(listener);
    }

    @Override
    public void sendPackageToContact(@Nonnull File packageFile, @Nonnull Contact contact) throws CommoException
    {
        if (!PreferenceEntry.DATA_PACKAGE_ENABLE_SHARING.getBooleanValue())
        {
            logger.warn("Aborted sending data package '{}', file sharing is disabled in the user-preferences", packageFile.getName());
            return;
        }

        commonCommoManager.sendMissionPackage(contact, packageFile);

        addOutgoingTransferLog(packageFile, contact);
    }

    @Override
    public void sendPackageToContacts(@Nonnull File packageFile, @Nonnull List<Contact> contacts) throws CommoException
    {
        if (!PreferenceEntry.DATA_PACKAGE_ENABLE_SHARING.getBooleanValue())
        {
            logger.warn("Aborted sending data package '{}', file sharing is disabled in the user-preferences", packageFile.getName());
            return;
        }

        commonCommoManager.sendMissionPackage(contacts, packageFile);

        for (Contact contact : contacts)
        {
            addOutgoingTransferLog(packageFile, contact);
        }
    }

    @Nonnull
    @Override
    public CompletableFuture<UploadedResourceResponse> sendPackageToServer(@Nonnull File packageFile, @Nonnull ITakServerConnection connection) throws IOException
    {
        return takServerApiClient.syncEndpoints(connection)
                .upload(packageFile.toPath())
                .withDataPackageKeywords()
                .withName(packageFile.getName())
                .asyncGet(executorService);
    }

    @Override
    public void addDataPackageVfsListener(@Nonnull IDataPackageVfsListener listener)
    {
        vfsListeners.add(listener);
    }

    @Override
    public void removeDataPackageVfsListener(@Nonnull IDataPackageVfsListener listener)
    {
        vfsListeners.remove(listener);
    }

    @Nonnull
    @Override
    public File handlePackageReceive(@Nonnull String fileName, @Nonnull String sha256hash, long sizeBytes, @Nonnull String senderCallsign) throws MissionPackageTransferException
    {
        if (!PreferenceEntry.DATA_PACKAGE_ENABLE_SHARING.getBooleanValue())
        {
            throw new MissionPackageTransferException(MissionPackageTransferStatus.FINISHED_DISABLED_LOCALLY);
        }

        // The destination file path.
        // If it exists, we throw an 'already exists' message.
        File peerDownloadDir = getPeerImportRootDir();
        File localPath = getPeerImportedPackagePath(fileName);
        if (localPath.exists())
        {
            String existingHash = HashingUtils.sha256sum(localPath);
            if (existingHash.equals(sha256hash)) throw new MissionPackageTransferException(MissionPackageTransferStatus.FINISHED_FILE_EXISTS);
            localPath = FileUtils.getUniqueFileName(peerDownloadDir, localPath);
        }

        // Record hash so we can check for a match when the transfer is complete.
        pendingTransfers.put(localPath, new PendingTransfer(sha256hash, senderCallsign));
        return localPath;
    }

    @Override
    public void handlePackageReceiveStatus(@Nonnull MissionPackageReceiveStatusUpdate update)
    {
        File packageFile = update.localFile;
        String name = packageFile.getName();

        if (update.status == MissionPackageTransferStatus.FINISHED_SUCCESS)
        {
            PendingTransfer pendingTransfer = pendingTransfers.remove(packageFile);
            String callsign = pendingTransfer.ownerCallsign();
            addTransferLog(packageFile, callsign, null);
            String receiveMessage = chatDataPackageService.isChatPackage(packageFile) ? "Received chat attachment from user: " + callsign : "Received data package '" + name + "' from user: " + callsign;
            notificationService.show(new Notification(receiveMessage, Notification.Type.Info));

            String expectedHash = pendingTransfer.fileSha256Hash();
            if (expectedHash != null)
            {
                String actualHash = HashingUtils.sha256sum(packageFile);
                if (!Objects.equals(expectedHash, actualHash))
                {
                    logger.warn("The data package '{}' did not match the expected hash of '{}' got '{}'", name, expectedHash, actualHash);
                }
            } else
            {
                // Should not occur, as we record the hash when the transfer initializes
                logger.warn("The data package '{}' received has no associated hash to validate against", name);
            }

            // Import the data package
            try
            {
                // TODO: This prevents the data package from being acknowledged as being from a peer
                new PackageImporter(packageFile, callsign).importPackage();
            } catch (IOException ex)
            {
                logger.warn("Failed importing contents of the data package '{}'", name, ex);
            }

            // For chat data packages there are some contents we may need to hold on to
            // that are not handled by the standard package import process such as displaying contents in the UI.
            if (chatDataPackageService.isChatPackage(packageFile)) chatDataPackageService.receivePackage(callsign, packageFile);

            // If the data package is marked to be deleted on receipt, delete it and return without notifying the add listeners
            if (isDeleteOnReceive(packageFile))
            {
                if (packageFile.delete())
                {
                    logger.debug("Deleted data package '{}' that is marked to be deleted on receipt", name);
                } else
                {
                    logger.warn("Failed to delete data package '{}' that is marked to be deleted on receipt", name);
                }

                return;
            }

            // Notify listeners of the new package
            DataPackageContents contents = mapToContentsWithOwner(packageFile, null);
            onPackageAdded(packageFile, contents);
        }
    }

    /**
     * Adds an outbound entry to {@link #transferLog} and notifies all {@link #transferLogListeners}.
     *
     * @param packageFile File path to local data package.
     * @param contact Contact we are sending the data package to.
     */
    private void addOutgoingTransferLog(@Nonnull File packageFile, @Nonnull Contact contact)
    {
        addTransferLog(packageFile, null, contactNameResolver.resolveName(contact.contactUID));
    }

    /**
     * Adds an entry to {@link #transferLog} and notifies all {@link #transferLogListeners}.
     *
     * @param packageFile File path to local data package.
     * @param fromCallsign Callsign the data package transfer is coming from {@code null} if we are the sender.
     * @param toCallsign Callsign the data package transfer is addressed to. {@code null} if we are the receiver.
     */
    private void addTransferLog(@Nonnull File packageFile, @Nullable String fromCallsign, @Nullable String toCallsign)
    {
        TransferLogItem logItem = new TransferLogItem(packageFile, fromCallsign, toCallsign);
        transferLog.add(logItem);
        for (ITransferListener listener : transferLogListeners)
        {
            safeListenerCall(packageFile, "transfer log update", () -> listener.onNewTransfer(logItem));
        }
    }

    /**
     * @param packageFile Data package file to update.
     * @param pathsToRemove Paths in the zip file to remove.
     *
     * @return Paths removed from the zip. Should match the input collection contents. If it does not, the zip was already missing those paths.
     *
     * @throws IOException When modifying the data package file fails for any reason.
     */
    @Nonnull
    private Collection<String> removeFilesFromDataPackageInternal(@Nonnull File packageFile, @Nonnull Collection<String> pathsToRemove) throws IOException
    {
        assertPackageWithinVFS(packageFile);

        // We'll be writing to a temp file and overwriting the original package file later
        File newPackageFile = File.createTempFile("datapackage", ".zip");

        // First add the given file to the target zip
        ZipOutputStream out = new ZipOutputStream(new FileOutputStream(newPackageFile));

        // Then write the contents of the original data package, modifying the manifest to add the entry
        Set<String> removedPaths = new HashSet<>();
        try (ZipFile zipFile = new ZipFile(packageFile))
        {
            List<? extends ZipEntry> zipEntries = Collections.list(zipFile.entries());
            for (ZipEntry ze : zipEntries)
            {
                String entryName = ze.getName();
                if (pathsToRemove.contains(entryName))
                {
                    // Record which files were successfully removed.
                    removedPaths.add(entryName);
                } else if (entryName.equals(MANIFEST_PATH))
                {
                    // Instead of reusing the existing entry, a new one is made since we're modifying it, and the existing one
                    // has values set (like the size and crc) which will not match when our modifications are made.
                    out.putNextEntry(new ZipEntry(MANIFEST_PATH));
                    byte[] bytes = zipFile.getInputStream(ze).readAllBytes();
                    String manifestXml = new String(bytes);

                    // Read the existing XML into the manifest model, remove entries matching removed paths, then write back the modified XML.
                    PackageManifest manifest = (PackageManifest) getManifestJaxbContext().createUnmarshaller().unmarshal(new StringReader(manifestXml));
                    PackageManifestContents contents = manifest.getContents();
                    for (PackageManifestContent content : new ArrayList<>(contents.getContents()))
                    {
                        if (pathsToRemove.contains(content.getManifestUid()))
                        {
                            contents.removeContent(content);
                        }
                    }
                    String modifiedManifestXml = manifest.toXml();
                    out.write(modifiedManifestXml.getBytes());
                    out.closeEntry();
                } else
                {
                    out.putNextEntry(ze);
                    IOUtils.copy(zipFile.getInputStream(ze), out);
                    out.closeEntry();
                }
            }
        } catch (JAXBException ex)
        {
            // We didn't finish creating the file, so delete it
            Files.deleteIfExists(newPackageFile.toPath());
            throw new IOException(ex);
        } finally
        {
            out.close();
        }

        // Finally, overwrite the original
        Files.move(newPackageFile.toPath(), packageFile.toPath(), StandardCopyOption.REPLACE_EXISTING);

        return removedPaths;
    }

    /**
     * @param packageFile Data package file to update.
     * @param models List of file contents to add.
     *
     * @return List of added file paths inside the zip. The order will match the list of model inputs.
     *
     * @throws IOException When modifying the data package file fails for any reason.
     * @since 5.3
     */
    @Nonnull
    private List<String> addFileToDataPackageInternalViaMemory(@Nonnull File packageFile, @Nonnull List<MemoryContentToAdd> models) throws IOException
    {
        assertPackageWithinVFS(packageFile);

        // First add the given files to the target zip
        Map<String, ManifestEntryAdapter> addedZipPaths = new LinkedHashMap<>(models.size());
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ZipOutputStream out = new ZipOutputStream(baos);
        for (MemoryContentToAdd model : models)
        {
            String hash = HashingUtils.sha256sum(model.fileContents());
            String prefix = model.prefixName() + "/" + hash;
            String addedZipPath = prefix + "/" + model.fileToAddName();
            addedZipPaths.put(addedZipPath, model.entryAdapter());
            out.putNextEntry(new ZipEntry(addedZipPath));
            out.write(model.fileContents());
            out.closeEntry();
        }

        // Then write the rest of the original ZIP contents to the target zip and overwrite the original.
        File newPackageFile = File.createTempFile("datapackage", ".zip");
        try
        {
            copyExistingPackageContentsToDestination(out, packageFile, addedZipPaths);
            byte[] content = baos.toByteArray();
            Files.write(newPackageFile.toPath(), content);
            Files.move(newPackageFile.toPath(), packageFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException ex)
        {
            // We didn't finish creating the file, so delete it.
            Files.deleteIfExists(newPackageFile.toPath());
            throw ex;
        }

        return new ArrayList<>(addedZipPaths.keySet());
    }

    /**
     * @param packageFile Data package file to update.
     * @param models List of files to add.
     *
     * @return Map of files to the added file paths inside the zip.
     *
     * @throws IOException When modifying the data package file fails for any reason.
     * @since 5.3
     */
    @Nonnull
    private Map<File, String> addFileToDataPackageInternalViaFile(@Nonnull File packageFile, @Nonnull List<FileContentToAdd> models) throws IOException
    {
        // Sanity check inputs
        for (FileContentToAdd model : models)
        {
            if (!model.fileToAdd().exists())
            {
                throw new IOException("Cannot add '" + model.fileToAdd() + "' to data-package, as it does not exist");
            }
        }
        assertPackageWithinVFS(packageFile);

        // Read existing package structure, will use for de-duplication.
        List<DataPackageEntry> existingEntries = mapToContentsWithOwner(packageFile, null).getEntryList();

        // First add the given files to the target zip.
        File newPackageFile = File.createTempFile("datapackage", ".zip");
        Map<String, ManifestEntryAdapter> zipPathToAdapterMap = new LinkedHashMap<>(models.size());
        Map<File, String> fileToAddedZipPathMap = new LinkedHashMap<>(models.size());
        ZipOutputStream out = new ZipOutputStream(new BufferedOutputStream(new FileOutputStream(newPackageFile)));
        for (FileContentToAdd model : models)
        {
            File fileToAdd = model.fileToAdd();
            String hash = HashingUtils.sha256sum(fileToAdd);

            // Data packages created with standard TAK products will put files in a standardized file path structure:
            //  files/{sha256}/{filename}
            //  images/{sha256}/{filename}
            // If we observe a folder with a matching hash we should skip adding this entry as it would be a duplicate.
            // Third party data-packages may not conform to this standard, but its essentially free vs streaming and
            // hashing actual existing file contents.
            if (existingEntries.stream().anyMatch(e -> e.zipEntryPath.matches(".+/" + hash + "/.+"))) continue;

            String prefix = model.prefixName() + "/" + hash;
            String addedZipPath = prefix + "/" + fileToAdd.getName();
            zipPathToAdapterMap.put(addedZipPath, model.entryAdapter());
            fileToAddedZipPathMap.put(fileToAdd, addedZipPath);
            InputStream in = new FileInputStream(fileToAdd);
            out.putNextEntry(new ZipEntry(addedZipPath));
            in.transferTo(out);
            out.closeEntry();
            in.close();
        }

        // Skip any IO copying on disk if there are no actual changes made.
        if (fileToAddedZipPathMap.isEmpty()) return Collections.emptyMap();

        // Then write the rest of the original ZIP contents to the target zip and overwrite the original.
        try
        {
            copyExistingPackageContentsToDestination(out, packageFile, zipPathToAdapterMap);
            Files.move(newPackageFile.toPath(), packageFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException ex)
        {
            // We didn't finish creating the file, so delete it.
            Files.deleteIfExists(newPackageFile.toPath());
            throw ex;
        }

        return fileToAddedZipPathMap;
    }

    /**
     * @param out         Destination to write to.
     * @param packageFile Original data package file to copy contents from.
     * @param newEntries  Map of new zip paths added that are already present in the destination ZIP stream,
     *                    to the optional adapters to manipulate the new manifest entry with.
     *
     * @throws IOException When writing to the data package file fails for any reason.
     * @since 5.3
     */
    private void copyExistingPackageContentsToDestination(@Nonnull ZipOutputStream out, @Nonnull File packageFile,
                                                          @Nonnull Map<String, ManifestEntryAdapter> newEntries)
            throws IOException
    {
        // Then write the contents of the original data package, modifying the manifest to add the entry
        try (out; ZipFile zipFile = new ZipFile(packageFile))
        {
            List<? extends ZipEntry> zipEntries = Collections.list(zipFile.entries());
            for (ZipEntry ze : zipEntries)
            {
                String entryName = ze.getName();
                if (newEntries.containsKey(entryName))
                {
                    // If we are adding a file path to the package that already is in there, we will skip copying the old one over.
                    // The file already got added to the new package in the logic above.
                    logger.debug("Skipping old entry of '{}'", entryName);
                } else if (entryName.equals(MANIFEST_PATH))
                {
                    // Instead of reusing the existing entry, a new one is made since we're modifying it, and the existing one
                    // has values set (like the size and crc) which will not match when our modifications are made.
                    out.putNextEntry(new ZipEntry(MANIFEST_PATH));
                    byte[] bytes = zipFile.getInputStream(ze).readAllBytes();
                    String manifestXml = new String(bytes);

                    // Read the existing XML into the manifest model, add our new entries, then write back the modified XML.
                    PackageManifest manifest = (PackageManifest) getManifestJaxbContext().createUnmarshaller().unmarshal(new StringReader(manifestXml));
                    PackageManifestContents contents = manifest.getContents();
                    for (var entry : newEntries.entrySet())
                    {
                        String addedZipPath = entry.getKey();
                        ManifestEntryAdapter entryAdapter = entry.getValue();
                        PackageManifestContent newContent = new PackageManifestContent();
                        newContent.setManifestUid(addedZipPath);
                        if (entryAdapter != null) entryAdapter.handleAddedContent(newContent);
                        contents.addContent(newContent);
                    }
                    String modifiedManifestXml = manifest.toXml();
                    out.write(modifiedManifestXml.getBytes());
                    out.closeEntry();
                } else
                {
                    out.putNextEntry(ze);
                    zipFile.getInputStream(ze).transferTo(out);
                    out.closeEntry();
                }
            }
        } catch (JAXBException ex)
        {
            throw new IOException(ex);
        }
    }

    /**
     * Finalize the download of a file.
     * Validate its SHA, and report success/error to system messages.
     *
     * @param downloadTask Download task being processed.
     * @param response HTTP response to pull data from.
     *
     * @return Updated download request.
     */
    @Nonnull
    private DownloadRequest applyDownloadRequest(@Nonnull DownloadTask downloadTask, @Nonnull Response response)
    {
        final DownloadRequest downloadRequest = downloadTask.getRequest();

        // Copy over status code
        downloadRequest.setStatusCode(response.status());

        final IResource fileResource = downloadRequest.getFileResource();
        final String filename = fileResource.fileName();
        final ITakServerConnection connection = downloadRequest.getServerConnection();
        final NetConnectString server = connection.getConnectionConfig().getNetConnectString();

        if (downloadRequest.isSuccess())
        {
            final Path localDownloadDir = downloadRequest.getDownloadDir();
            final Path localFilePath = downloadRequest.getLocalFilePath();

            // Write the file, reporting progress as we go
            long size = downloadRequest.getFileResource().size();
            try (Response.Body body = response.body())
            {
                // Ensure directory exists before writing
                if (!Files.isDirectory(localDownloadDir)) Files.createDirectories(localDownloadDir);

                ITaskStatusHandler statusHandler = downloadTask.getTaskStatusHandler();
                InputStream input = body.asInputStream();
                byte[] buffer = new byte[1024];
                long count = 0;
                if (input == null)
                {
                    throw new IOException("Response body for download was empty or could not be streamed");
                }
                try (OutputStream output = Files.newOutputStream(localFilePath))
                {
                    final CompletableFuture<DownloadRequest> future = downloadTask.getDownloadFuture();
                    int n;
                    while ((n = input.read(buffer)) != -1)
                    {
                        if (future.isCancelled()) return downloadRequest;
                        output.write(buffer, 0, n);
                        count += n;
                        int progress = (int) (100 * ((double) size / count));
                        statusHandler.setProgress(progress);
                    }
                }
                statusHandler.setProgress(100);

                // Validate the downloaded package is a well-formed ZIP file with at least one entry.
                try (ZipFile tmpZip = new ZipFile(localFilePath.toFile()))
                {
                    if (!tmpZip.entries().hasMoreElements()) throw new IOException("Data package has zero entries");
                } catch (IOException ex)
                {
                    String message = String.format("The remote data package '%s' was not a valid ZIP file, or was empty", filename);
                    Files.delete(localFilePath);
                    logger.warn(message);
                    messageManager.error(message);
                    return downloadRequest;
                }
            } catch (IOException ex)
            {
                final String message = String.format(DOWNLOAD_FAILED, filename, server, downloadRequest.getStatusCode());
                logger.warn(message);
                messageManager.error(message);
                return downloadRequest;
            }

            // Verify the file matches the reported hash.
            if (!HashingUtils.verify(localFilePath.toFile(), fileResource.size(), fileResource.hash()))
            {
                final String message = String.format(VERIFICATION_FAILED_MSG, filename, server);
                logger.warn(message);
                messageManager.error(message);

                throw new VerificationException("SHA256 hash mismatch on: " + filename);
            }

            final String message = String.format(DOWNLOAD_SUCCEEDED, filename, server, fileResource.size());
            logger.debug(message);
            messageManager.info(message);
        } else
        {
            final String message = String.format(DOWNLOAD_FAILED, filename, server, downloadRequest.getStatusCode());
            logger.warn(message);
            messageManager.error(message);
        }

        return downloadRequest;
    }

    /**
     * In some cases a client can upload an incomplete data package model to TAK Server.
     * The file is listed but may not have all the required fields.
     *
     * @param resource Resource to check for required fields.
     *
     * @return Same resource for a valid model, or a copy with default values if any required field is {@code null}.
     */
    @Nonnull
    private static Resource ensureResourceDataIsFilled(@Nonnull Resource resource)
    {
        String creatorUid = resource.creatorUid();
        String submitter = resource.submitter();
        String fileName = resource.fileName();
        String name = resource.name();
        if (creatorUid == null || submitter == null || fileName == null || name == null)
        {
            if (creatorUid == null) creatorUid = submitter == null ? UNKNOWN : submitter;
            if (submitter == null) submitter = UNKNOWN;
            if (fileName == null) fileName = UUID.randomUUID() + ".zip";
            if (name == null) name = fileName;
            return new Resource(resource.uid(), resource.hash(), name,
                    fileName, resource.submissionTime(),
                    submitter, creatorUid, resource.keywords(),
                    resource.mimeType(), resource.size(), resource.expiration());
        }
        return resource;
    }

    /**
     * @param resource Resource to map to an {@link AssociatedResource}.
     *
     * @return Resource with owner association recorded.
     */
    @Nonnull
    private AssociatedResource associateResourceToCallsign(@Nonnull IResource resource)
    {
        if (resource instanceof AssociatedResource existingAssociation) return existingAssociation;

        String uploader = contactNameResolver.resolveNameOrElseUnknown(resource.creatorUid());
        return new AssociatedResource(resource, uploader);
    }

    /**
     * Maps the owner uid associated with the data package to a current callsign before wrapping the file up in {@link DataPackageContents}.
     *
     * @param packageFile Data package to map to {@link DataPackageContents}.
     * @param ownerUid If known by the calling context, the uid of the client responsible for creation of the data package.
     * Will be {@code null} for local imports.
     *
     * @return Data package contents with resolved owner callsign.
     */
    @Nonnull
    private DataPackageContents mapToContentsWithOwner(@Nonnull File packageFile, @Nullable String ownerUid)
    {
        String owner;
        if (isLocalImport(packageFile))
        {
            // Local imports always have the same dummy name as the owner
            owner = DataPackageContents.LOCAL_IMPORT_OWNER;
        } else
        {
            // Packages coming from peers, or TAK Server should be resolvable by the package owner's uid.
            owner = contactNameResolver.resolveNameOrElseUnknown(ownerUid);
        }

        return new DataPackageContents(packageFile, owner);
    }

    /**
     * @return {@link JAXBContext} for {@link PackageManifest} types.
     *
     * @throws IOException Wrapped {@link JAXBException} from creating a new instance.
     */
    @Nonnull
    private synchronized JAXBContext getManifestJaxbContext() throws IOException
    {
        if (manifestJaxbContext == null)
        {
            try
            {
                manifestJaxbContext = JAXBContext.newInstance(PackageManifest.class);
                return manifestJaxbContext;
            } catch (JAXBException e)
            {
                throw new IOException(e);
            }
        }
        return manifestJaxbContext;
    }

    /**
     * @return A list of all data package file paths from all servers and local imports.
     */
    @Nonnull
    private List<File> getAllDataPackages()
    {
        // This assumes all data packages we receive are named with '.zip'
        // When we receive/download files we should be appending the extension to any files missing it.
        try (Stream<Path> stream = Files.walk(getRootDir().toPath()))
        {
            return stream
                    .filter(IS_ZIP_FILE)
                    .filter(path -> !chatDataPackageService.isChatPackage(path))
                    .map(Path::toFile).collect(Collectors.toList());
        } catch (IOException e)
        {
            logger.error("Failed to get data packages:", e);
            return Collections.emptyList();
        }
    }

    @Override
    public boolean isLocalImport(@Nonnull File file)
    {
        return file.getParentFile().equals(getLocalImportRootDir());
    }

    @Override
    public boolean isPeerImport(@Nonnull File file)
    {
        return file.getParentFile().equals(getPeerImportRootDir());
    }

    @Override
    public boolean isServerDownload(@Nonnull File packageFile)
    {
        return !isLocalImport(packageFile) && !isPeerImport(packageFile);
    }

    @Nonnull
    @Override
    public File getLocalImportedPackagePath(@Nonnull String name)
    {
        return getImportedPackagePath(name, getLocalImportRootDir());
    }

    @Nonnull
    @Override
    public File getPeerImportedPackagePath(@Nonnull String name)
    {
        return new File(getPeerImportRootDir(), zipify(name));
    }

    @Nonnull
    @Override
    public File getServerDownloadedPackagePath(@Nonnull ITakServerConnection connection, @Nonnull String name)
    {
        return new File(getServerDownloadRootDir(connection), zipify(name));
    }

    /**
     * Some operations require data packages to be within the VFS.
     * Data packages are placed in the VFS when:
     * <ul>
     *     <li>Imported and calling {@link #copyPackageToVFS(File)}</li>
     *     <li>Downloaded via {@link #downloadDataPackage(ITakServerConnection, IResource, IDataPackageDownloadListener)}</li>
     * </ul>
     *
     * @param packageFile File path to data package to check.
     *
     * @throws IOException When the data package is not within the VFS.
     */
    private void assertPackageWithinVFS(@Nonnull File packageFile) throws IOException
    {
        if (isTempPackage(packageFile)) return;

        if (!packageFile.toPath().startsWith(getRootDir().toPath()))
        {
            throw new IOException("Data package '" + packageFile.getName() + "' is not within the VFS");
        }
    }

    /**
     * @return The directory path where extracted files are kept.
     */
    @Nonnull
    private File getExtractedDirectory()
    {
        return new File(getRootDir(), EXTRACTED_DIR);
    }

    /**
     * @param dir Directory to create if it does not yet exist.
     */
    private static File ensureExists(File dir)
    {
        if (!dir.exists() && !dir.mkdirs()) logger.error("Failed to create dir {}", dir);
        return dir;
    }

    @Override
    public void onPackageAdded(@Nonnull File packageFile, @Nonnull DataPackageContents contents)
    {
        if (isTempPackage(packageFile)) return;

        for (IDataPackageVfsListener listener : vfsListeners)
        {
            safeListenerCall(packageFile, "addition", () -> listener.onPackageAdded(packageFile, contents));
        }
    }

    @Override
    public void onPackageUpdated(@Nonnull File packageFile, @Nonnull DataPackageContents contents)
    {
        if (isTempPackage(packageFile)) return;

        for (IDataPackageVfsListener listener : vfsListeners)
        {
            safeListenerCall(packageFile, "update", () -> listener.onPackageUpdated(packageFile, contents));
        }
    }

    @Override
    public void onPackageRemoved(@Nonnull File packageFile)
    {
        if (isTempPackage(packageFile)) return;

        for (IDataPackageVfsListener listener : vfsListeners)
        {
            safeListenerCall(packageFile, "removal", () -> listener.onPackageRemoved(packageFile));
        }
    }

    /**
     * In some cases, incoming data packages will not have the {@code .zip} file extension.
     * For users, this is just a QoL improvement to add the extension.
     *
     * @param fileName Input file name.
     *
     * @return File name, ensuring it ends in the {@code .zip} extension.
     */
    @Nonnull
    private static String zipify(@Nonnull String fileName)
    {
        return StringUtils.endsWithIgnoreCase(fileName, ".zip") ? fileName : fileName + ".zip";
    }

    /**
     * Wrap an action in try-catch. Calling contexts in a loop can act on all values even if one or more fails.
     *
     * @param packageFile Package file related to action.
     * @param action Action run.
     * @param consumer Action handler.
     */
    private static void safeListenerCall(@Nonnull File packageFile, @Nonnull String action, @Nonnull Runnable consumer)
    {
        try
        {
            consumer.run();
        } catch (Throwable t)
        {
            logger.error("Encountered an error in listener handling {} of data package '{}'", action, packageFile.getName(), t);
        }
    }

    /**
     * Creates a new local data package file.
     *
     * @param name The data package name
     * @param packageFileFunction Takes a validated file name and returns a new data package file
     * @param emptyManifest The manifest to populate and include
     * @return The data package file
     * @throws IOException When an existing data package by that name exists, the given name is blank, or creating the file failed for some other reason
     * @since 5.2
     */
    private File createNewDataPackage(@Nonnull String name,
                                      @Nonnull ErrorThrowingFunction<String, File, IOException> packageFileFunction,
                                      @Nonnull String emptyManifest)
            throws IOException
    {
        // While technically allowed locally, transferring a file with a blank name is not allowed, so we'll deny
        // the creation of those here.
        if (name.isBlank()) throw new IOException("Data package name cannot be blank");

        final File newPackageFile = packageFileFunction.apply(name);

        if (newPackageFile.exists()) throw new IOException(newPackageFile.getName() + " already exists, please try a different name");

        // Create a zip file at the location with an empty manifest.
        final String manifest = emptyManifest.formatted(name, UUID.randomUUID().toString());
        try (ZipOutputStream out = new ZipOutputStream(new FileOutputStream(newPackageFile)))
        {
            out.putNextEntry(new ZipEntry(MANIFEST_PATH));
            out.write(manifest.getBytes());
            out.closeEntry();
        }

        onPackageAdded(newPackageFile, new DataPackageContents(newPackageFile, DataPackageContents.LOCAL_IMPORT_OWNER));

        return newPackageFile;
    }

    /**
     * @param name The package name
     * @param directory The directory where the package file should be created
     * @return A new package file
     * @since 5.2
     */
    @Nonnull
    private File getImportedPackagePath(@Nonnull String name, @Nonnull File directory)
    {
        final String cleanFileName = FileUtils.sanitizeForFilename(name); // Avoid path manipulation attacks
        return new File(directory, zipify(cleanFileName));
    }

    /**
     * @param packageFile The package file to check
     * @return {@code true} if the file is a temp data package
     * @since 5.2
     */
    @SuppressWarnings("BooleanMethodIsAlwaysInverted")
    private boolean isTempPackage(@Nonnull File packageFile)
    {
        return chatDataPackageService.isChatPackage(packageFile) || packageFile.toPath().startsWith(TEMP_DIR_PATH);
    }

    /**
     * Checks the mission package zip manifest's "onReceiveDelete" attribute.
     * 
     * @param packageFile The package file to check
     * @return {@code true} if the "onReceiveDelete" attribute is set to {@code true}
     * @since 5.5
     */
    @VisibleForTesting
    boolean isDeleteOnReceive(@Nonnull File packageFile)
    {
        try (var reader = new PackageReader(packageFile))
        {
            final var configuration = reader.getManifest().getConfiguration();
            return configuration != null && "true".equals(configuration.getParameterValue("onReceiveDelete"));
        } catch (IOException e)
        {
            logger.error("Error inspecting data package manifest file", e);
        }

        return false;
    }

    /**
     * @param fileSha256Hash Expected sha-256 hash of file upon transfer completion.
     * @param ownerCallsign Callsign of the client initiating the transfer.
     *
     * @since 5.0
     */
    record PendingTransfer(String fileSha256Hash, String ownerCallsign)
    {
    }

    /**
     * Outline adapter for content added to data package's manifest.
     */
    private interface ManifestEntryAdapter
    {
        /**
         * Intercept the new manifest entry, allowing addition of extra parameters.
         *
         * @param entry New manifest entry to add.
         */
        void handleAddedContent(@Nonnull PackageManifestContent entry);
    }

    /**
     * Background task monitoring a file download from a TAK Server.
     */
    private class DownloadTask extends AExitAffectingTask
    {
        private final CompletableFuture<DownloadRequest> downloadFuture = new CompletableFuture<>();
        private final DownloadRequest request;

        protected DownloadTask(@Nonnull DownloadRequest request)
        {
            super(TaskGroup.FILE_TRANSFER, "Download: " + request.getFileResource().fileName());
            this.request = request;
        }

        /**
         * @return Download request information.
         */
        @Nonnull
        public DownloadRequest getRequest()
        {
            return request;
        }

        /**
         * @return Future of the download request.
         */
        @Nonnull
        public CompletableFuture<DownloadRequest> getDownloadFuture()
        {
            return downloadFuture;
        }

        @Override
        public void execute() throws Exception
        {
            IResource fileResource = request.getFileResource();
            if (logger.isDebugEnabled()) logger.debug("Downloading: {} ({})", fileResource.fileName(), SizeFormatter.formatEstimate(fileResource.size()));

            takServerApiClient.syncEndpoints(request.getServerConnection())
                    .getContent()
                    .withHash(fileResource.hash())
                    .asyncGet(executorService)
                    .thenApply(response -> applyDownloadRequest(this, response))
                    .whenComplete((req, err) -> {
                        if (err != null)
                        {
                            downloadFuture.completeExceptionally(err);
                        } else
                        {
                            downloadFuture.complete(req);
                        }
                    });

            try
            {
                downloadFuture.join(); // Just wait on the future, status updates will be happening in the background.
            } catch (CancellationException e)
            {
                getTaskStatusHandler().setCompletionStatus(TaskCompletionStatus.CANCELED);
            } catch (CompletionException e)
            {
                getTaskStatusHandler().setException(e.getCause());
            }
        }

        @Override
        public boolean allowExit()
        {
            return !downloadFuture.isDone(); // Not until we're done
        }

        @Override
        public void doCancel()
        {
            downloadFuture.cancel(true);
        }

        @Override
        public String getExitMessage()
        {
            final ITaskStatusHandler handler = getTaskStatusHandler();

            IResource fileResource = request.getFileResource();
            if (handler.isProgressIndeterminate())
            {
                return String.format("'%s' still downloading (progress unknown)", fileResource.fileName());
            } else
            {
                return String.format("Download of '%s' at %d%% complete", fileResource.fileName(), handler.getProgress());
            }
        }
    }

    /**
     * Encapsulation of data relevant to a file download request.
     */
    private static class DownloadRequest
    {
        private final ITakServerConnection serverConnection;
        private final IResource fileResource;
        private final Path downloadDir;

        private int statusCode;

        /**
         * @param serverConnection Server from which to request the file
         * @param fileResource File resource describing the file
         * @param downloadDir Directory into which to download the file
         */
        public DownloadRequest(@Nonnull ITakServerConnection serverConnection,
                               @Nonnull IResource fileResource, @Nonnull Path downloadDir)
        {
            this.serverConnection = serverConnection;
            this.fileResource = fileResource;
            this.downloadDir = downloadDir;
        }

        /**
         * @return HTTP status code from download request
         */
        public int getStatusCode()
        {
            return statusCode;
        }

        /**
         * Set the HTTP status code.
         */
        public void setStatusCode(int statusCode)
        {
            this.statusCode = statusCode;
        }

        /**
         * @return {@code true} if the download was successful, i.e., returned an HTTP status code of OK (200).
         */
        public boolean isSuccess()
        {
            return statusCode == HttpStatus.SC_OK;
        }

        /**
         * @return Connection to the TAK Server the file is being downloaded from.
         */
        @Nonnull
        public ITakServerConnection getServerConnection()
        {
            return serverConnection;
        }

        /**
         * @return Information about the file being downloaded.
         *
         * @since 3.5 (was getFileEntry())
         */
        @Nonnull
        public IResource getFileResource()
        {
            return fileResource;
        }

        /**
         * @return The path to the local file downloaded
         */
        @Nonnull
        public Path getLocalFilePath()
        {
            String fileName = fileResource.fileName();
            if (!StringUtils.endsWithIgnoreCase(fileName, ".zip")) fileName += ".zip";
            return downloadDir.resolve(fileName);
        }

        /**
         * @return The path to the directory containing the local download target path.
         *
         * @since 3.5
         */
        @Nonnull
        public Path getDownloadDir()
        {
            return downloadDir;
        }

        @Override
        public String toString()
        {
            return "DownloadRequest{" +
                    "serverConnection=" + serverConnection.getDisplayName() +
                    ", fileResource=" + fileResource +
                    ", downloadDir=" + downloadDir +
                    ", statusCode=" + statusCode +
                    '}';
        }
    }

    /**
     * @param fileContents Contents of file.
     * @param fileToAddName Name of file.
     * @param prefixName Directory prefix to use in the output zip file when this file is added.
     * @param entryAdapter Optional adapter to manipulate the new manifest entry for the file with.
     * @since 5.3
     */
    private record MemoryContentToAdd(@Nonnull byte[] fileContents, @Nonnull String fileToAddName,
            @Nonnull String prefixName, @Nullable ManifestEntryAdapter entryAdapter)
    {
    }

    /**
     * @param fileToAdd File to add.
     * @param prefixName Directory prefix to use in the output zip file when this file is added.
     * @param entryAdapter  Optional adapter to manipulate the new manifest entry for the file with.
     * @since 5.3
     */
    private record FileContentToAdd(@Nonnull File fileToAdd, @Nonnull String prefixName, @Nullable ManifestEntryAdapter entryAdapter)
    {
    }
}
