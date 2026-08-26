package gov.takx.core.contentcleaner;

import gov.tak.blacksmith.Blacksmith;
import gov.tak.blacksmith.BlacksmithExtension;
import gov.tak.blacksmith.mock.Mocked;
import gov.tak.blacksmith.mock.strategy.MockStrategy;
import gov.tak.blacksmith.mock.strategy.MockitoStrategy;
import gov.takx.api.annotation.SuppressLogging;
import gov.takx.api.configuration.IProgramEnvironment;
import gov.takx.api.notification.INotificationService;
import gov.takx.client.platform.ApplicationController;
import gov.takx.core.configuration.ApplicationInstance;
import gov.takx.core.preferences.PreferenceManager;
import gov.takx.platform.preferences.PreferenceEntry;
import jakarta.annotation.Nonnull;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.ArgumentCaptor;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.prefs.Preferences;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.longThat;
import static org.mockito.Mockito.clearInvocations;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Tests {@link ContentCleaner}.
 *
 * @since 5.1
 */
@ExtendWith(BlacksmithExtension.class)
public class ContentCleanerTest
{
    @Mocked
    private ApplicationController applicationController;
    @Mocked
    private IProgramEnvironment programEnvironment;
    @Mocked
    private INotificationService notificationService;
    @Mocked
    private ApplicationInstance applicationInstance;
    @Mocked
    private PreferenceManager preferenceManager;
    @Mocked
    private File mockFile;

    @TempDir
    static Path tempDir;

    private ContentCleaner contentCleaner;
    private static File projectDataDir;

    static
    {
        MockStrategy mockStrategy = new MockitoStrategy().cached();
        BlacksmithExtension.setBlacksmithFactory(classes -> new Blacksmith(classes).withMockStrategy(mockStrategy));
    }

    @BeforeAll
    static void beforeAll()
    {
        projectDataDir = Path.of(tempDir.toString(), "ProjectData").toFile();
        //noinspection ResultOfMethodCallIgnored
        projectDataDir.mkdir();
    }

    @SuppressWarnings("ResultOfMethodCallIgnored")
    @BeforeEach
    public void beforeEach() throws IOException
    {
        contentCleaner = new ContentCleaner(applicationController, programEnvironment, notificationService,
                applicationInstance, preferenceManager);

        // Create the map cache directory and a second dummy dir that we'll confirm has been deleted
        new File(projectDataDir, ContentCleaner.MAP_CACHE_DIR).mkdir();
        final File other = new File(projectDataDir, "other");
        other.mkdir();
        final Path nestedFilePath = other.toPath().resolve("nestedFile");
        if (!nestedFilePath.toFile().exists()) Files.createFile(nestedFilePath);

        when(programEnvironment.isPlatformUnderTest()).thenReturn(true);
        when(programEnvironment.getProjectDataDirectory()).thenReturn(projectDataDir.getPath());
        when(programEnvironment.getInstanceProgramPropertiesFile()).thenReturn(mockFile);

        clearInvocations(applicationController, programEnvironment, notificationService, applicationInstance);
    }

    // @SuppressLogging prevents takkernel from complaining that the DBs haven't been initialized
    @SuppressLogging(com.atakmap.coremap.log.Log.class)
    @Test
    public void clean_closesProjectAndShutsDown() throws InterruptedException
    {
        cleanAndWait(true);

        verify(applicationController).shutdown(false, true);
    }

    @SuppressLogging(com.atakmap.coremap.log.Log.class)
    @Test
    public void clean_whenDeleteCachedMapDataFalse_preservesCacheDirectory() throws InterruptedException
    {
        cleanAndWait(false);

        final File[] files = projectDataDir.listFiles();

        //noinspection DataFlowIssue
        assertThat(files.length).isEqualTo(1);
        assertThat(files[0].getName()).isEqualTo(ContentCleaner.MAP_CACHE_DIR);
    }

    @SuppressLogging(com.atakmap.coremap.log.Log.class)
    @Test
    public void clean_whenDeleteCachedMapDataTrue_deletesCacheDirectory() throws InterruptedException
    {
        cleanAndWait(true);

        //noinspection DataFlowIssue
        assertThat(projectDataDir.listFiles().length).isEqualTo(0);
    }

    @Test
    public void cleanProgramProperties_whenFails_notifiesUser(@Mocked(allowCaching = false) File testFile)
    {
        setupProgramPropertiesFiles(testFile, false);

        contentCleaner.cleanProgramProperties();

        verify(testFile).delete();
        verify(notificationService).show(any(), longThat(l -> l == INotificationService.MANUAL_CLOSE));
        verify(applicationController, never()).shutdown(anyBoolean());
    }

    @Test
    public void cleanProgramProperties_deletesFilesAndExits(@Mocked(allowCaching = false) File testFile, @Mocked Preferences preferences)
    {
        setupProgramPropertiesFiles(testFile, true);

        when(preferenceManager.getPreferences()).thenReturn(preferences);

        contentCleaner.cleanProgramProperties();

        verify(testFile).delete();

        // Verify that all preference entries that should not be cloned are deleted
        final var prefKeys = ContentCleaner.REMOVE_BEFORE_CLONE_PREFS.stream().map(PreferenceEntry::getKey).toList();
        final var argumentCaptor = ArgumentCaptor.forClass(String.class);
        verify(preferences, times(ContentCleaner.REMOVE_BEFORE_CLONE_PREFS.size())).remove(argumentCaptor.capture());
        assertThat(argumentCaptor.getAllValues()).containsExactlyElementsOf(prefKeys);

        verify(applicationController).shutdown(anyBoolean());
    }

    private void setupProgramPropertiesFiles(@Nonnull File testFile, boolean allowDelete)
    {
        when(programEnvironment.getInstanceProgramPropertiesFile()).thenReturn(testFile);
        when(applicationInstance.getNodeUuidFile()).thenReturn(testFile);

        when(testFile.exists()).thenReturn(true);
        when(testFile.delete()).thenReturn(allowDelete);
    }

    private void cleanAndWait(boolean deleteCachedMapData) throws InterruptedException
    {
        contentCleaner.clean(deleteCachedMapData);
        contentCleaner.cleanerThread.join(5000);

        verify(applicationController).shutdown(anyBoolean(), eq(true)); // Give the Thread time to finish
    }
}
