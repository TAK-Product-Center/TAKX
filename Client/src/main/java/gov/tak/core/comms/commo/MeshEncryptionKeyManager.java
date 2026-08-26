package gov.tak.core.comms.commo;

import android.util.Base64;
import com.atakmap.coremap.filesystem.FileSystemUtils;
import com.google.common.annotations.VisibleForTesting;
import gov.takx.commons.file.FileUtils;
import gov.takx.commons.swing.AlwaysOnTopFileChooser;
import gov.takx.commons.swing.AlwaysOnTopJOptionPane;
import gov.takx.platform.preferences.PreferenceEntry;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import jakarta.enterprise.context.ApplicationScoped;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;

import javax.swing.JFileChooser;
import javax.swing.JOptionPane;
import javax.swing.filechooser.FileFilter;
import javax.swing.filechooser.FileNameExtensionFilter;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.xpath.XPathFactory;

import java.awt.Component;
import java.io.File;
import java.io.IOException;
import java.lang.invoke.MethodHandles;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.SecureRandom;
import java.util.function.BiConsumer;

/**
 * Manages the mesh encryption keys.
 *
 * @since 5.0
 */
@ApplicationScoped
public class MeshEncryptionKeyManager
{
    private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());
    @VisibleForTesting
    static final String KEY_XPATH =
            "/preferences/preference[@name = 'com.atakmap.app_preferences']/entry[@key = 'networkMeshKey']";
    private static final String PREF_FILE_EXT = "pref";

    // String used to generate XML key files
    @VisibleForTesting
    static final String KEY_FILE_TEMPLATE = """
            <?xml version='1.0' standalone='yes'?>
            <preferences>
                <preference version='1' name='com.atakmap.app_preferences'>
                    <entry key='networkMeshKey' class='class java.lang.String'>%s</entry>
                </preference>
            </preferences>
            """;
    private final FileFilter preferenceFileFilter =
            new FileNameExtensionFilter("Preference File (.pref)", PREF_FILE_EXT);

    /**
     * Loads an encryption key preference file, prompting the user to select the file.
     *
     * @param parent The parent component
     * @param completionConsumer The consumer to notify when the operation is complete
     */
    public void loadMeshEncryptionKey(@Nonnull Component parent, @Nonnull BiConsumer<String, Boolean> completionConsumer)
    {
        // Get the key value from the XML
        try
        {
            // Get the source file from the user
            final File keyFile = getFile(parent, "Open Mesh Encryption Key File", null, completionConsumer);

            // User cancelled
            if (keyFile == null) return;

            // Get the key from the document
            final Document document = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(keyFile);
            final String key = XPathFactory.newInstance().newXPath().evaluate(KEY_XPATH, document);

            if (key.isBlank())
            {
                logger.error("No encryption key found in file {}", keyFile.getPath());
                completionConsumer.accept("No encryption key found in file; no changes were made", true);
                return;
            }

            PreferenceEntry.COMMON_COMMO_MESH_ENCRYPTION_KEY.put(key);

            completionConsumer.accept("Key loaded", false);
        } catch (Exception e)
        {
            AlwaysOnTopJOptionPane.showErrorDialog("There was a problem getting the key from the file. No " +
                    "changes were made.", "Key File Error");
            logger.error("Error loading mesh key from file", e);
            completionConsumer.accept("Error reading file; no changes were made.", true);
        }
    }

    /**
     * Remove the loaded encryption key.
     *
     * @param completionConsumer The consumer to notify when the operation is complete. Expects a String completion
     *                           message and a boolean value of {@code true} if there was an error or {@code false} if
     *                           the operation completed successfully.
     */
    public void forgetMeshEncryptionKey(@Nonnull BiConsumer<String, Boolean> completionConsumer)
    {
        // The value is always reset to the empty String after an update, so using an empty String here won't trigger
        // a preference change event. Any other String that isn't 64 chars long will disable encryption
        PreferenceEntry.COMMON_COMMO_MESH_ENCRYPTION_KEY.put("-");
        completionConsumer.accept("Key removed", false);
    }

    /**
     * Generates a new encryption key and saves it to a file chosen by the user. Offers to load the new key.
     *
     * @param parent The parent component
     * @param completionConsumer The consumer to notify when the operation is complete. Expects a String completion
     *                           message and a boolean value of {@code true} if there was an error or {@code false} if
     *                           the operation completed successfully. consumer to notify when the operation is complete
     */
    public void generateMeshEncryptionKey(@Nonnull Component parent, @Nonnull BiConsumer<String, Boolean> completionConsumer)
    {
        // Prompt the user for the destination file
        File keyFile = getFile(parent, "Choose Key File Location", "Select", completionConsumer);

        // User cancelled
        if (keyFile == null) return;

        // Prepare the file
        if (!FileUtils.hasExtension(keyFile, PREF_FILE_EXT)) keyFile = new File(keyFile.getPath() + '.' + PREF_FILE_EXT);
        final Path keyFilePath = keyFile.toPath();

        // Generate the key file contents
        final byte[] bytes = new byte[256];
        new SecureRandom().nextBytes(bytes);

        // The substring range of 12-76 is copied directly from ATAK and is just an arbitrary way of reducing the length
        // to 64
        final String keyByteString =
                new String(Base64.encode(bytes, Base64.URL_SAFE | Base64.NO_WRAP), FileSystemUtils.UTF8_CHARSET)
                        .substring(12, 76);

        final String keyFileContents = KEY_FILE_TEMPLATE.formatted(keyByteString);

        // Save the file
        try
        {
            Files.writeString(keyFilePath, keyFileContents);
        } catch (IOException e)
        {
            completionConsumer.accept("Error saving file; no changes were made", true);
            return;
        }

        // Offer to load the new key
        final int loadKeyResult =
                AlwaysOnTopJOptionPane.showOptionDialog("Encryption key file created. Would you like to load the key now?",
                        "Load Key", JOptionPane.QUESTION_MESSAGE, JOptionPane.YES_NO_OPTION, null,
                        new String[]{"Yes", "No"}, null);

        if (loadKeyResult == JOptionPane.YES_OPTION)
        {
            PreferenceEntry.COMMON_COMMO_MESH_ENCRYPTION_KEY.put(keyByteString);
            completionConsumer.accept("Key generated and loaded; key file can be shared with other users", false);
        } else
        {
            completionConsumer.accept("Key generated; key file can be shared with other users", false);
        }
    }

    /**
     * Prompts the user to select a file. If the user cancels the operation, the {@code completionConsumer} will be 
     * notified before {@code null} is returned.
     * 
     * @param parent The parent component
     * @param title The text to use as the file selection dialog title
     * @param approveButtonText Optional approve button text; if {code null}, the default value will be used
     * @param completionConsumer The consumer to notify when the operation is complete. Expects a String completion
     *                           message and a boolean value of {@code true} if there was an error or {@code false} if
     *                           the operation completed successfully.
     * @return The selected file or {@code null} if the user cancels the operation
     */
    @VisibleForTesting
    @Nullable
    File getFile(@Nonnull Component parent, @Nonnull String title, @Nullable String approveButtonText,
                 @Nonnull BiConsumer<String, Boolean> completionConsumer)
    {
        // Get the source file from the user
        final AlwaysOnTopFileChooser fileChooser = new AlwaysOnTopFileChooser();
        fileChooser.setFileFilter(preferenceFileFilter);
        fileChooser.setDialogTitle(title);
        if (approveButtonText != null) fileChooser.setApproveButtonText(approveButtonText);

        if (fileChooser.showOpenDialog(parent) != JFileChooser.APPROVE_OPTION)
        {
            completionConsumer.accept(null, false);
            return null;
        }

        return fileChooser.getSelectedFile();
    }
}
