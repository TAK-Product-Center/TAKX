package gov.takx.core.view.gui.maps.importers.kml;

import gov.nasa.worldwind.WorldWind;
import gov.takx.api.annotation.Headless;
import gov.takx.api.annotation.InvokeOnEDT;
import gov.takx.api.annotation.VFSManagerScope;
import gov.takx.api.event.StartProjectServices;
import gov.takx.api.event.StopProjectServices;
import gov.takx.api.event.project.ProjectOpened;
import gov.takx.api.filesystem.IVirtualFileSystem;
import gov.takx.api.filesystem.IVirtualFileSystemManager;
import gov.takx.api.filesystem.VFSScope;
import gov.takx.api.globe.IExtendedGlobeManager;
import gov.takx.api.notification.INotificationService;
import gov.takx.api.notification.Notification;
import gov.takx.api.platform.ICoreExecutorService;
import gov.takx.api.tasks.ITaskManager;
import gov.takx.api.tasks.TaskGroup;
import gov.takx.commons.file.FileUtils;
import gov.takx.commons.swing.AlwaysOnTopJOptionPane;
import gov.takx.core.filesystem.TemplateFile;
import gov.takx.core.kml.KmlModel;
import gov.takx.core.kml.KmlVisualManager;
import gov.takx.core.kml.event.KmlModelAdded;
import gov.takx.core.kml.event.KmlModelRemoved;
import gov.takx.core.project.ServiceOrdering;
import gov.takx.core.security.CertUtils;
import gov.takx.core.security.ICertificateInstallerListener;
import gov.takx.core.tasks.AExitAffectingTask;
import gov.takx.core.view.gui.preferences.PkiPreferencesDialog;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import jakarta.annotation.Priority;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Event;
import jakarta.enterprise.event.Observes;
import jakarta.inject.Inject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.JFrame;
import javax.swing.JOptionPane;

import java.io.File;
import java.io.InputStream;
import java.lang.invoke.MethodHandles;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.concurrent.CompletableFuture;

/**
 */
@ApplicationScoped
public class KMLImportController implements ICertificateInstallerListener
{
    private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    private boolean sslHandlerInstalled;
    private boolean installingCert;

    private File rootKmlDir;
    private IVirtualFileSystem rootVFS;
    private boolean pkiKeySelected;

    private final IVirtualFileSystemManager projectVFSManager;
    private final boolean headless;
    private final IExtendedGlobeManager globeManager;
    private final INotificationService notificationService;
    private final KmlVisualManager kmlVisualManager;
    private final ICoreExecutorService executorService;
    private final ITaskManager taskManager;
    private final Event<KmlModelAdded> kmlModelAddedEvent;

    @Inject
    KMLImportController(@VFSManagerScope(VFSScope.Project) IVirtualFileSystemManager projectVFSManager,
                        IExtendedGlobeManager globeManager, INotificationService notificationService,
                        @Headless boolean headless, KmlVisualManager kmlVisualManager, ICoreExecutorService executorService,
                        ITaskManager taskManager, Event<KmlModelAdded> kmlModelAddedEvent)
    {
        this.projectVFSManager = projectVFSManager;
        this.globeManager = globeManager;
        this.notificationService = notificationService;
        this.headless = headless;
        this.kmlVisualManager = kmlVisualManager;
        this.executorService = executorService;
        this.taskManager = taskManager;
        this.kmlModelAddedEvent = kmlModelAddedEvent;
    }

    /**
     * Listener callback to get notification whether a cert was successfully installed.
     *
     * @param result RESULTS enum.  Can be SUCCESS, FAILURE, INSTALLED or CANCELLED
     */
    @InvokeOnEDT
    @Override
    public void certificateInstalled(ICertificateInstallerListener.RESULTS result, String host)
    {
        installingCert = false;
        if (result == RESULTS.SUCCESS)
        {
            AlwaysOnTopJOptionPane.showMessageDialog("Successfully installed certificate!", "Certificate installation", JOptionPane.INFORMATION_MESSAGE);
        } else if (result == RESULTS.FAILURE)
        {
            AlwaysOnTopJOptionPane.showErrorDialog("Error installing the certificate from the server", "Certificate Installation");
        }
    }

    /**
     * This is a bit of a hack to get a server name from the name of the Retrieval thread that is running.
     *
     * @param path full path of kml network link request
     * @return the server name as a string
     */
    private String getServerNameFromPath(String path)
    {
        int firstIndex = path.indexOf("http");
        if (firstIndex == -1) return "";
        int slashIndex = path.indexOf("//");
        if (slashIndex == -1) return "";
        int lastIndex = path.indexOf('/', slashIndex + 2);
        if (lastIndex == -1) return "";
        return path.substring(slashIndex + 2, lastIndex);
    }

    /**
     * Import the given KML/KMZ source into VFS and add the KML model to the Overlays.
     * This method should be called from a new thread.
     *
     * @param kmlSource source of the KML file
     * @return Completable future for import operation
     * @since 5.5 renamed from importAndAddKmlLayer
     */
    public CompletableFuture<KmlModel> importKml(@Nonnull String kmlSource)
    {
        final Path path = new KMLArtifactCopier().copyKmlArtifacts(Paths.get(kmlSource), rootKmlDir.toPath());
        return addKmlModel(kmlSource, null, path);
    }

    /**
     * Import the given {@link KmlModel model} into VFS and add it to the Overlays.
     * The import operation is processed on a background thread.
     *
     * @param kmlModel Model to add
     * @return Completable future for import operation
     * @since 5.5 renamed from importAndAddKmlLayer
     */
    public CompletableFuture<KmlModel> importKml(@Nonnull KmlModel kmlModel)
    {
        final Path path = new KMLArtifactCopier().copyKmlArtifacts(kmlModel.sourceFile().toPath(), rootKmlDir.toPath());

        // Update the source location to where we copied it to.  There are logic paths that will get here after
        // parsing the file in its original location, and we don't want to use that as the "source", otherwise
        // if the user deletes this model, we would delete the original and not the copy in VFS.
        kmlModel.setSourceFile(path.toFile());

        return addKmlModel(null, kmlModel, path);
    }

    /**
     * Import the contents of the given InputStream as KML/KMZ.  The contents of the stream will be written into
     * VFS using the name provided as the file name.
     * <p/>
     * The import operation is processed on a background thread.
     *
     * @param source Source stream to process
     * @param name Name to use for VFS file
     * @return Completable future for import operation
     * @since 3.1 (renamed in 5.5)
     */
    public CompletableFuture<KmlModel> importKml(@Nonnull InputStream source, @Nonnull String name)
    {
        final Path path = new KMLArtifactCopier().copyKmlArtifact(source, name, rootKmlDir.toPath());
        return addKmlModel(path.toString(), null, path);
    }

    /**
     * Create/add the KML model specified in {@code kmlSource} or {@code kmlModel}.
     * One of {@code kmlSource} or {@code kmlMode} must be non-null.
     * <p/>
     * The import operation is processed on a background thread.
     *
     * @param kmlSource source of the KML file
     * @param kmlModel pre-parsed KML model to add
     * @param pathInVfs Path to source file in VFS
     * @return Completable future for import operation
     * @since 5.6, signature change
     */
    private CompletableFuture<KmlModel> addKmlModel(@Nullable String kmlSource, @Nullable KmlModel kmlModel, @Nonnull Path pathInVfs)
    {
        if (kmlSource == null && kmlModel == null) throw new IllegalArgumentException("Both kmlSource and kmlModel are null!");

        final var importTask = new KmlImportTask(kmlSource, kmlModel, pathInVfs);
        taskManager.execute(importTask);
        return importTask.completableFuture;
    }

    /**
     * Remove source file associated with the given KML model when the model has been removed.
     * @since 5.5
     */
    private void onKmlModelRemoved(@Observes KmlModelRemoved event)
    {
        final var model = event.kmlModel();
        final var sourceFile = model.sourceFile();
        if (!sourceFile.delete()) logger.warn("Failed to delete KML file: {}", sourceFile);

        kmlVisualManager.removeVisuals(model);
        notificationService.show(Notification.builder("Removed imported KML: " + model.sourceFile().getName(), Notification.Type.Success).build());
    }

    @InvokeOnEDT
    private void installPkiCert()
    {
        // Give the user 1 chance to select the appropriate key after adding a new layer.  If they don't have the
        // correct key, no need to keep trying to get them to select it.  If they just choose the wrong key, they
        // can always go into the user preferences and select the correct key.
        if (!pkiKeySelected)
        {
            PkiPreferencesDialog dlg = new PkiPreferencesDialog();
            dlg.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
            dlg.setAlwaysOnTop(true);
            dlg.setVisible(true);
            String keyEntry = dlg.getSelectedKeyEntry();
            CertUtils.selectUserKey(keyEntry);
            pkiKeySelected = true;
        }

        installingCert = false;
    }

    @InvokeOnEDT
    private void displayKmlImportError(String kmlSource, Throwable cause)
    {
        AlwaysOnTopJOptionPane.showConfirmDialog(
                null,
                "Unable to parse/import " + kmlSource + ". \nError:\n" + cause.getMessage(),
                "KML/KMZ Import Error",
                JOptionPane.ERROR_MESSAGE,
                JOptionPane.DEFAULT_OPTION);
    }

    protected void clearState()
    {
        rootKmlDir = null;
    }

    /**
     * Start our service.
     */
    private void startService(@Observes @Priority(ServiceOrdering.START_GlobeService) StartProjectServices ignored)
    {
        if (headless) return;

        clearState();

        installSSLExceptionHandler();
    }

    /**
     * Stop our service.
     */
    private void stopService(@Observes @Priority(ServiceOrdering.STOP_GlobeService) StopProjectServices ignored)
    {
        clearState();
    }

    /**
     * In the event that a file contains a network link that requires certificate installation, we need to set
     * up a listener for SSLHandshake exceptions.
     */
    private void installSSLExceptionHandler()
    {
        if (sslHandlerInstalled) return; // Already done

        sslHandlerInstalled = true;

        // TODO - Network link implementation, TAKX-3789
        // Set up to receive SSLHandshakeExceptions that occur during resource retrieval.
        WorldWind.getRetrievalService().setSSLExceptionListener((throwable, serverString) -> {
            logger.error("SSL Exception", throwable);
            if (!installingCert)
            {
                String message = throwable.getMessage();
                if (CertUtils.UNSUPPORTED_CERT.equals(message) || CertUtils.INVALID_CERT.equals(message) || CertUtils.HANDSHAKE_CERT.equals(message))
                {
                    installingCert = true;
                    installPkiCert();
                } else  // check to see if it is a server certificate issue (untrusted CA cert).
                {
                    String serverName = getServerNameFromPath(serverString);
                    if (!serverName.isEmpty())
                    {
                        try
                        {
                            installingCert = true;
                            CertUtils.installCert(serverName, this);
                        } catch (Exception e)
                        {
                            logger.error("Error installing certificate", e);
                            installingCert = false;
                        }
                    } else
                    {
                        logger.error("Error.  Could not get server name to retrieve a certificate for from provided path: {}", serverString);
                    }
                }
            }
        });
    }

    /**
     * Once the project is opened, initialize our state.
     */
    private void handleProjectOpened(@SuppressWarnings("UnusedParameters") @Observes ProjectOpened event)
    {
        if (headless) return;

        // Get a path to the project's KML overlay folder
        rootVFS = projectVFSManager.getVirtualFileSystem("overlays", "KML");
        rootKmlDir = rootVFS.getRootDir();

        // If the globe is ready, load the data.  Otherwise, load data on the globe ready event.
        globeManager.getPrimaryGlobeDelegate().doWhenReady(this::loadData);
    }

    /**
     * Look in the project directory for stored KML files and load any that are found
     */
    private void loadData()
    {
        executorService.execute(() -> {
            Thread.currentThread().setName("KML Import reload");

            for (var vfsFile : rootVFS.listFiles())
            {
                final String fileName = vfsFile.getFileName();
                final String extension = FileUtils.getExtension(fileName);
                if ("kml".equalsIgnoreCase(extension) || "kmz".equalsIgnoreCase(extension)) addKmlModel(fileName, null, vfsFile.getFile().toPath());
            }
        });
    }

    /**
     * Managed task to import a KML/KMZ source.
     * @since 5.6
     */
    private final class KmlImportTask extends AExitAffectingTask
    {
        private final String kmlSource;
        private final Path pathInVfs;
        private final KmlModel kmlModel;
        private final CompletableFuture<KmlModel> completableFuture = new CompletableFuture<>();

        KmlImportTask(@Nullable String kmlSource, @Nullable KmlModel kmlModel, @Nonnull Path pathInVfs)
        {
            //noinspection DataFlowIssue - one of kmlSource or kmlModel is non-null, never are both null
            super(TaskGroup.IMPORT_EXPORT, "KML Import: " + (kmlSource != null ? kmlSource : kmlModel.displayName()));
            this.kmlSource = kmlSource;
            this.pathInVfs = pathInVfs;
            this.kmlModel = kmlModel;
        }

        @Override
        public void doCancel()
        {
            // Nothing to do
        }

        @Override
        public String getExitMessage()
        {
            final var handler = getTaskStatusHandler();

            if (handler.isProgressIndeterminate())
            {
                return String.format("%s: still processing (progress unknown)", getName());
            } else
            {
                final int max = handler.getMaxProgress();
                final int min = handler.getMinProgress();
                final int progress = handler.getProgress();
                final int percent = progress * 100 / (max - min);

                return String.format("%s: %d%% (%d of %d) complete", getName(), percent, progress, max);
            }
        }

        @Override
        public void execute()
        {
            final var vfsFile = new TemplateFile(pathInVfs.toString());
            pkiKeySelected = false;

            try
            {
                taskStatusHandler.setProgressIndeterminate(true);
                taskStatusHandler.printMessage("Parsing...");
                final var model = kmlModel != null ? kmlModel : KmlModel.parseModel(vfsFile.getFile());

                taskStatusHandler.printMessage("Importing...");
                kmlVisualManager.createVisuals(model, taskStatusHandler);

                taskStatusHandler.printMessage("Adding to overlays...");
                kmlModelAddedEvent.fire(new KmlModelAdded(model));

                notificationService.show(Notification.builder("Imported KML: " + model.sourceFile().getName(), Notification.Type.Success).build());
                completableFuture.complete(model);
            } catch (Exception e)
            {
                // Drill down to the original exception to get its message.
                Throwable cause = e;
                while (cause.getCause() != null) cause = cause.getCause();
                logger.error("Unable to import {}:\n {}", kmlSource, cause.getMessage(), cause);
                completableFuture.completeExceptionally(e);
                displayKmlImportError(kmlSource, cause);
            }
        }
    }
}
