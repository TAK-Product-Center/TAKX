package gov.takx.api.plugin.io;

import gov.takx.commons.wizard.IWizardProperties;
import gov.takx.platform.io.panels.CommonWizardSettings;

import java.io.File;
import java.net.URL;
import java.util.List;
import java.util.Map;

/**
 * Extended interface for an Import / Export Wizard.  Allows for specifying additional attributes, like the ability
 * to process multiples files, and the ability to concurrently process all files at once.  Plugins are encouraged
 * to instead extend the abstract classes {@link AImportExportWizard} or {@link ACancellableImportWizard} rather
 * than implementing this interface directly.
 */
public interface IExtendedImportExportWizard extends IImportExportWizard
{
    /**
     * Get the wizard's display name.
     *
     * @return The wizard's display name.
     */
    String getDisplayName();

    /**
     * Get the icon will be presented to the user to represent this wizard.
     *
     * @return The icon that will be presented to the user.
     */
    URL getIcon();

    /**
     * An optional, brief message to help the user understand the behavior of the
     * importer/exporter. It's highly recommended that implementers provide a help message.
     *
     * @return A brief message explaining the purpose of the wizard or {@code null} if no
     * message is being supplied.
     */
    String getHelpMessage();

    /**
     * Check to see if this wizard wants to offer to process a file.
     *
     * @param fileExtension The file extension.
     * @param file          The file to be processed. <b>Care should be taken to make any interactions with the file extremely
     *                      efficient.</b> The file may be very large and wizard inquiries happen synchronously.
     * @return {@code true} if this wizard would like to offer to import the file, {@code false} otherwise.
     */
    boolean offerToProcessFile(String fileExtension, File file);

    /**
     * Check to see if this wizard can process more than one file at a time. If the wizard supports multiple files,
     * the file(s) will be found in the preconfiguration map with the key
     * {@link CommonWizardSettings#FILES}. If not, the single file will be found with the
     * key {@link CommonWizardSettings#FILE}.
     *
     * @return {@code true} if the wizard supports multiple files, {@code false} otherwise.
     */
    boolean supportsMultipleFiles();

    /**
     * Check to see if this wizard supports processing all files concurrently.  If the parser supports processing
     * all the files concurrently, it should be created by overriding
     * {@link ACancellableImportWizard#createConcurrentImportParser(List, Map, IWizardProperties)}.  Plugins returning
     * {@code true} should also return {@code true} for {@link IExtendedImportExportWizard#supportsMultipleFiles()}.
     *
     * @return {@code true} if processing all files concurrently is supported, otherwise {@code false} (default)
     * @since 5.8
     */
    default boolean supportsConcurrentProcessing()
    {
        return false;
    }

    /**
     * Optionally return true to take precedence over stock / default TAKX importers (i.e. elevation, imagery,
     * file-on-globe, placemarks, kml, etc.  Note, if returning true for this method, you MUST MAKE SURE that this
     * file will be parsed when the doImport method is called.  Meaning, that developers should have robust checking
     * in the offerToProcessFile() method.  Don't just offer to process the file based on the extension.
     *
     * @return true to override, false otherwise (default in base class API).
     * @see #offerToProcessFile(String, File)
     * @since 1.49
     */
    boolean overrideDefaultImporters();
}
