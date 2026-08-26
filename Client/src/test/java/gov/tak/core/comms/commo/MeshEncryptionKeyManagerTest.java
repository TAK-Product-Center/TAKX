package gov.tak.core.comms.commo;

import gov.tak.blacksmith.mock.Mocked;
import gov.takx.api.annotation.SuppressLogging;
import gov.takx.api.preferences.IPreferenceEntryChangeListener;
import gov.takx.api.preferences.PreferenceEntryChange;
import gov.takx.commons.swing.AlwaysOnTopJOptionPane;
import gov.takx.platform.preferences.PreferenceEntry;
import gov.takx.test.AIntegrationTest;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.w3c.dom.Document;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.xpath.XPathFactory;

import java.awt.Component;
import java.io.File;
import java.nio.file.Files;
import java.util.function.BiConsumer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.booleanThat;
import static org.mockito.Mockito.clearInvocations;
import static org.mockito.Mockito.verify;

/**
 * Tests {@link MeshEncryptionKeyManager}
 * 
 * @since 5.0
 */
@SuppressWarnings("JUnitMalformedDeclaration") // method params okay
public class MeshEncryptionKeyManagerTest extends AIntegrationTest
{
    // must be 64 chars
    private static final String TEST_KEY = "0123456789012345678901234567890123456789012345678901234567890123";
    private static final String PREFERENCE_FILE_XML = MeshEncryptionKeyManager.KEY_FILE_TEMPLATE.formatted(TEST_KEY);

    @Mocked
    private Component parent;
    @Mocked
    private BiConsumer<String, Boolean> completionConsumer;

    @BeforeEach
    void beforeEach()
    {
        //noinspection unchecked
        clearInvocations(completionConsumer);
    }

    @Test
    public void loadMeshEncryptionKey_extractsKeyFromFile(@Mocked(allowCaching = false) IPreferenceEntryChangeListener preferenceEntryChangeListener)
    {
        PreferenceEntry.COMMON_COMMO_MESH_ENCRYPTION_KEY.addPreferenceEntryChangeListener(preferenceEntryChangeListener);

        final MeshEncryptionKeyManager manager = getManager(null);
        manager.loadMeshEncryptionKey(parent, completionConsumer);

        final ArgumentCaptor<PreferenceEntryChange> argument = ArgumentCaptor.forClass(PreferenceEntryChange.class);

        // Completion consumer called with a message and boolean indicating there were no errors
        verify(completionConsumer).accept(anyString(), booleanThat(b -> !b));

        // Capture the value passed to the COMMON_COMMO_MESH_ENCRYTION_KEY listener
        verify(preferenceEntryChangeListener).preferenceEntryChanged(argument.capture());

        // The event should contain an entry with the test key
        assertThat(argument.getValue().getPreferenceEntry().getStringValue()).isEqualTo(TEST_KEY);

        // Commo isn't started during tests, so we can't test its response
    }

    @Test
    public void forgetMeshEncryptionKey_clearsKey(@Mocked(allowCaching = false) IPreferenceEntryChangeListener preferenceEntryChangeListener)
    {
        PreferenceEntry.COMMON_COMMO_MESH_ENCRYPTION_KEY.addPreferenceEntryChangeListener(preferenceEntryChangeListener);

        final MeshEncryptionKeyManager manager = getManager(null);
        manager.forgetMeshEncryptionKey(completionConsumer);

        final ArgumentCaptor<PreferenceEntryChange> argument = ArgumentCaptor.forClass(PreferenceEntryChange.class);

        // Completion consumer called with a message and boolean indicating there were no errors
        verify(completionConsumer).accept(anyString(), booleanThat(b -> !b));

        // Capture the value passed to the COMMON_COMMO_MESH_ENCRYTION_KEY listener
        verify(preferenceEntryChangeListener).preferenceEntryChanged(argument.capture());

        // Any String that is not 64 chars will force the key to be cleared
        assertThat(argument.getValue().getPreferenceEntry().getStringValue().length()).isNotEqualTo(64);

        // Commo isn't started during tests, so we can't test its response
    }

    @SuppressLogging(AlwaysOnTopJOptionPane.class)
    @Test
    public void generateMeshEncryptionKey_createsValidKeyFile() throws Exception
    {
        final File tempFile = File.createTempFile("test", ".pref");
        tempFile.deleteOnExit();
        final MeshEncryptionKeyManager manager = getManager(tempFile);

        // Manager should write new preference file to tempFile
        manager.generateMeshEncryptionKey(parent, completionConsumer);

        // Validate the XML by using our XPATH to retrieve the key
        final Document document = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(tempFile);
        final String key = XPathFactory.newInstance().newXPath().evaluate(MeshEncryptionKeyManager.KEY_XPATH, document);

        // Verify the key was extracted and is the correct length
        assertThat(key).isNotNull();
        assertThat(key.length()).isEqualTo(64);
    }

    /**
     * Create a custom {@link MeshEncryptionKeyManager}.
     * 
     * @param file The file that should be returned on calls to 
     * {@link MeshEncryptionKeyManager#getFile(Component, String, String, BiConsumer)} or {@code null} if a new XML preference
     *             file should be returned.
     * @return A new {@link MeshEncryptionKeyManager}.
     */
    private MeshEncryptionKeyManager getManager(@Nullable File file)
    {
        return new MeshEncryptionKeyManager()
        {
            // Instead of prompting a user, we'll provide a temp file
            @Override
            @Nullable
            File getFile(@Nonnull Component parent, @Nonnull String title, @Nullable String approveButtonText,
                         @Nonnull BiConsumer<String, Boolean> completionConsumer)
            {
                try
                {
                    // If the test provided a file, return it
                    if (file != null) return file;

                    // If the test did not provide a file, create one with data
                    final File tempFile = File.createTempFile("test", ".pref");
                    tempFile.deleteOnExit();
                    Files.writeString(tempFile.toPath(), PREFERENCE_FILE_XML);
                    return tempFile;
                } catch (Exception e)
                {
                    return null;
                }
            }
        };
    }
}
