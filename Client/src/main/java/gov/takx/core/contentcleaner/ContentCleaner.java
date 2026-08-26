package gov.takx.core.contentcleaner;

import com.atakmap.net.AtakAuthenticationDatabase;
import com.atakmap.net.AtakCertificateDatabase;
import com.google.common.annotations.VisibleForTesting;
import gov.takx.api.configuration.IProgramEnvironment;
import gov.takx.api.notification.INotificationService;
import gov.takx.api.notification.Notification;
import gov.takx.client.platform.ApplicationController;
import gov.takx.commons.file.FileUtils;
import gov.takx.core.configuration.ApplicationInstance;
import gov.takx.core.preferences.PreferenceManager;
import gov.takx.core.view.gui.ProgressDialog;
import gov.takx.platform.preferences.PreferenceEntry;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.lang.invoke.MethodHandles;
import java.util.List;
import java.util.stream.Stream;

/**
 * Deletes the ProjectData directory contents (optionally retaining cached map data) and shuts down the application.
 *
 * @since 5.1
 */
@ApplicationScoped
public class ContentCleaner
{
    private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    @VisibleForTesting
    static final List<PreferenceEntry> REMOVE_BEFORE_CLONE_PREFS = List.of(PreferenceEntry.CALL_SIGN, PreferenceEntry.NODE_UUID);

    @VisibleForTesting
    static final String MAP_CACHE_DIR = "OfflineCache";

    private final ApplicationController applicationController;
    private final IProgramEnvironment programEnvironment;
    private final INotificationService notificationService;
    private final ApplicationInstance applicationInstance;
    private final PreferenceManager preferenceManager;

    @VisibleForTesting
    Thread cleanerThread;

    @Inject
    ContentCleaner(ApplicationController applicationController, IProgramEnvironment programEnvironment,
                   INotificationService notificationService, ApplicationInstance applicationInstance,
                   PreferenceManager preferenceManager)
    {
        this.applicationController = applicationController;
        this.programEnvironment = programEnvironment;
        this.notificationService = notificationService;
        this.applicationInstance = applicationInstance;
        this.preferenceManager = preferenceManager;
    }

    /**
     * Deletes the ProjectData directory and shuts down the application.
     *
     * @param deleteCachedMapData Pass {@code true} if cached map data should be deleted
     */
    public void clean(boolean deleteCachedMapData)
    {
        logger.info("Deleting ProgramData content. Cached map data will be {}.", deleteCachedMapData ? "deleted" : "preserved");

        final boolean underTest = programEnvironment.isPlatformUnderTest();

        // Get off the EDT
        cleanerThread = new Thread(() -> {
            Thread.currentThread().setName("Content Cleaner");

            // Show dialog after the project is closed (MainAppFrame uses its own during project close)
            if (!underTest) showProgressDialog();

            applicationController.shutdown(false, true);
            deleteProjectData(deleteCachedMapData);

            if (!underTest) System.exit(0);
        });
        cleanerThread.start();
    }

    /**
     * Removes the node UUID and callsign from the ProgramProperties.xml file, deletes the
     * {@link ApplicationInstance#getNodeUuidFile() node UUID file} and exits the program to prepare it for cloning.
     *
     * @since 5.7
     */
    public void cleanProgramProperties()
    {
        final var uuidFile = applicationInstance.getNodeUuidFile();

        logger.info("Removing the callsign and node UUID and deleting {}", uuidFile.getAbsolutePath());

        final var uuidDeleted = !uuidFile.exists() || uuidFile.delete();

        if (uuidDeleted)
        {
            // Clean up the ProgramProperties
            final var preferences = preferenceManager.getPreferences();
            REMOVE_BEFORE_CLONE_PREFS.forEach(preferenceEntry -> preferences.remove(preferenceEntry.getKey()));
            preferenceManager.savePreferences();

            applicationController.shutdown(false);
        } else
        {
            final var message = "Failed to delete " + uuidFile.getAbsolutePath() + ". Process aborted.";
            notificationService.show(new Notification(message, Notification.Type.Error), INotificationService.MANUAL_CLOSE);
        }
    }

    /**
     * @param deleteCachedMapData Pass {@code true} if cached map data should be deleted
     */
    private void deleteProjectData(boolean deleteCachedMapData)
    {
        // We need to explicitly shut down the certificates and credentials databases to unlock the files
        AtakAuthenticationDatabase.dispose();
        AtakCertificateDatabase.dispose();

        //noinspection DataFlowIssue - listFiles() should never return null
        Stream.of(new File(programEnvironment.getProjectDataDirectory()).listFiles())
                .filter(file -> deleteCachedMapData || !file.getName().equals(MAP_CACHE_DIR)) // Skip PMS log file
                .forEach(this::delete);

        // ProgramProperties.xml will be saved just before the app exits. For simplicity, just mark it for deletion.
        programEnvironment.getInstanceProgramPropertiesFile().deleteOnExit();
    }

    /**
     *  @param file The file or directory to delete
     */
    private void delete(File file)
    {
        if (!file.exists()) return;

        if (file.isDirectory())
        {
            FileUtils.deleteDirectory(file); // Exceptions will be logged
        } else
        {
            if (!file.delete()) logger.warn("Unable to delete file {}", file);
        }
    }

    /**
     * Displays an indeterminate {@link ProgressDialog}.
     */
    private static void showProgressDialog()
    {
        final ProgressDialog progressDialog = new ProgressDialog(null);

        progressDialog.setTitle("Content Cleaner");
        progressDialog.setText("Clearing content and shutting down");
        progressDialog.setSubstatus("Deleting files");
        progressDialog.setMaximum(-1); // Indeterminate
        progressDialog.setModal(false);
        progressDialog.setVisible(true);
    }
}
