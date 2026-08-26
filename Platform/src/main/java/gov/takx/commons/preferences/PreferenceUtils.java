package gov.takx.commons.preferences;

import gov.takx.commons.swing.AlwaysOnTopJOptionPane;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.invoke.MethodHandles;
import java.net.URL;
import java.nio.file.AccessDeniedException;
import java.util.prefs.BackingStoreException;
import java.util.prefs.InvalidPreferencesFormatException;
import java.util.prefs.Preferences;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Utility methods for working with preferences files.
 */
public final class PreferenceUtils
{
    private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    public static final String RX_NODE_KEY = "rx";

    // patterns used to identify idiotic "FileNotFound" errors that are actually the result
    // of Java being utterly clueless about file system permissions.
    private static final Pattern[] PERMISSION_ERROR_PATTERNS = {
            Pattern.compile(" \\(Access is denied\\)\\Z"),
            Pattern.compile(" \\(Permission denied\\)\\Z")
    };

    /**
     * This class is not intended to be instantiated, it only provides static methods.  This constructor
     * enforces that (and quiets PMD).
     */
    private PreferenceUtils()
    {
    }

    /**
     * Write the given preferences to the specified file.
     *
     * @param preferences Preferences node to save
     * @param file        File to write into
     */
    public static void savePreferences(Preferences preferences, File file) throws IOException, BackingStoreException
    {
        // Save to XML file
        try (FileOutputStream fos = new FileOutputStream(file))
        {
            preferences.flush();
            preferences.exportSubtree(fos);
        } catch (FileNotFoundException e)
        {
            throw fixPermissionsExceptions("Unable to save preferences file", e);
        }
    }

    /**
     * Get a sub-node of the user preferences node.  The node referenced will be located beneath
     * the "rx" node in the user root.  If the node is not present, then read preferences from the
     * given file and then obtain the specified node.
     *
     * @param file file to get Preferences from
     * @param node Node name
     * @return the Project Preferences
     * @throws FileNotFoundException If the specified file is not present or readable
     */
    public static Preferences getPreferences(File file, String node) throws FileNotFoundException
    {
        try
        {
            if (!Preferences.userRoot().nodeExists(RX_NODE_KEY) || (node != null && !Preferences.userRoot().node(RX_NODE_KEY).nodeExists(node)))
            {
                return getPreferencesFromFile(file, node);
            } else
            {
                return getPreferences(node);
            }
        } catch (BackingStoreException e)
        {
            logger.error("Failed to get preference node: {}", node, e);
            return null;
        }
    }

    /**
     * Forcibly get the specified preferences node from the given file.  This will remove the named node
     * from memory, then read the specified preferences file, and then return the named node.
     *
     * @param file Preferences file to read.
     * @param node Node to return
     * @return preferences node
     * @throws FileNotFoundException if the file specified is not present or readable
     */
    public static Preferences getPreferencesFromFile(File file, String node) throws FileNotFoundException
    {
        deletePreferencesNodeFromMemory(node);

        try
        {
            importPreferencesFromFile(file);
        } catch (FileNotFoundException e)
        {
            throw e;
        } catch (IOException | InvalidPreferencesFormatException e)
        {
            // Unify these into FileNotFound
            throw new FileNotFoundException("Failed to load preferences: " + file + "\nCaused by: " + e.getMessage());
        }

        return getPreferences(node);
    }

    /**
     * Forcibly get the specified preferences node from the given file.  This will remove the named node
     * from memory, then read the specified preferences file, and then return the named node.
     * <p>
     * This method allows checked exceptions to come through so that the caller can detect and respond to that case.
     *
     * @param file Preferences file to read
     * @param node Node to return
     * @return preferences node
     * @throws FileNotFoundException             if the file specified is not present or readable
     * @throws InvalidPreferencesFormatException If the preferences file could not be parsed
     * @since 2.29
     */
    public static Preferences getPreferencesFromFileChecked(File file, String node) throws IOException, InvalidPreferencesFormatException
    {
        deletePreferencesNodeFromMemory(node);
        importPreferencesFromFile(file);

        return getPreferences(node);
    }

    /**
     * Get a sub-node of the user preferences node.  The node referenced will be located beneath
     * the "rx" node in the user root.
     *
     * @param node The node to retrieve, if {@code null}, then the "rx" node itself is returned
     * @return The Preferences node, may be empty if the node didn't previously exist
     * @see #RX_NODE_KEY
     */
    public static Preferences getPreferences(String node)
    {
        Preferences preferences = Preferences.userRoot().node(RX_NODE_KEY);
        return (node == null) ? preferences : preferences.node(node);
    }

    /**
     * Import preferences from the file path specified.  First try to load it as a
     * resource (prepending a "/" to make it absolute).  If that is not found, then
     * try to read it as a normal file.
     *
     * @param filePath relative path to file to read
     */
    public static void importPreferencesFromResourceOrFile(String filePath)
    {
        logger.trace("loading preferences from: {}", filePath);

        final URL resource = PreferenceUtils.class.getResource("/" + filePath);

        try (InputStream is = resource != null ? resource.openStream() : new FileInputStream(filePath))
        {
            Preferences.importPreferences(is);
        } catch (IOException | InvalidPreferencesFormatException e)
        {
            logger.error("Unable to load preferences from file: {}", filePath, e);
        }
    }

    /**
     * Delete the named node from the RX user node.
     *
     * @param node Node to delete
     */
    private static void deletePreferencesNodeFromMemory(String node)
    {
        try
        {
            Preferences.userRoot().node(RX_NODE_KEY).node(node).removeNode();
        } catch (BackingStoreException | IllegalStateException e)
        {
            logger.error("Failed to delete preference node: {}", node, e);
        }
    }

    /**
     * Import preferences from the given file.
     *
     * @param file File from which to read preferences
     * @throws FileNotFoundException             if the give file is not found or readable
     * @throws InvalidPreferencesFormatException If the preferences file could not be parsed
     */
    private static void importPreferencesFromFile(File file) throws IOException, InvalidPreferencesFormatException
    {
        try (InputStream in = new FileInputStream(file))
        {
            Preferences.importPreferences(in);
        }
    }

    /**
     * Convenience function for displaying an error dialog explaining the exceptions that can be
     * thrown by other functions in this class.  Also logs them to the provided logger.
     *
     * @param logger The calling class's logger.  an "error" message will be generated if the
     *               exception is anything other than a filesystem permissions error.
     * @param e      The exception to report.  Should be an exception thrown by a function in
     *               this utility class.
     */
    public static void showPreferencesErrorMessage(Logger logger, Throwable e)
    {
        if (e instanceof AccessDeniedException accessDeniedException)
        {
            AlwaysOnTopJOptionPane.showErrorDialog(
                    "Access denied while trying to save application preferences:\n" + accessDeniedException.getFile(),
                    "Access Denied");
        } else if (e instanceof IOException)
        {
            logger.error("IOException while trying to save application preferences", e);

            AlwaysOnTopJOptionPane.showErrorDialog("I/O error while trying to save application preferences", "I/O Error");
        } else
        {
            logger.error("Exception while trying to save application preferences", e);

            AlwaysOnTopJOptionPane.showErrorDialog("Internal error while trying to save application preferences", "Internal error");
        }
    }

    /**
     * For some reason, a lot of Java library code throws "FileNotFound" exceptions when it
     * actually means "access denied", despite the existence of a separate exception class
     * for that express purpose.  This function attempts to detect and fix this buffoonery.
     *
     * @param reason The "reason" string to be assigned to the AccessDeniedException, if
     *               the exception turns out to be a permission error.
     * @param e      The "FileNotFound" exception that might be incorrectly labeled.
     */
    private static IOException fixPermissionsExceptions(String reason, FileNotFoundException e)
    {

        String message = e.getMessage();
        for (Pattern pattern : PERMISSION_ERROR_PATTERNS)
        {
            Matcher matcher = pattern.matcher(message);
            if (matcher.find())
            {
                String path = matcher.replaceFirst("");
                return new AccessDeniedException(path, null, reason);
            }
        }

        return e;
    }
}
