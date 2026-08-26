package gov.takx.api.plugin.io;

import gov.takx.api.annotation.Internal;

/**
 * Interface for a an importer that extends {@link ACancellableImportWizard} for validating a file.  Developers are
 * strongly encouraged to extend {@link ACancellableImportWizard} and ignore this interface completely (used internally).
 * <p>
 * When extending {@link ACancellableImportWizard}, the base class will receive two callback notifications to validate
 * a file.  The first, {@link ACancellableImportWizard#validateFileForImport(String)} where the wizard should do a
 * simple validation to confirm the file is of the correct format.  The second validation (assuming the first one
 * passes) is to allow the parser to do some pre-parsing of the file {@link ACancellableImportWizard#preParseFile(String)}
 * for things like total counts to be imported, and maps that need to be built during a preprocessing step.
 *
 * @see ACancellableImportWizard
 * @since 2.14
 */
@Internal
public interface IImportFileValidator
{
    /**
     * Callback to validate a file.
     *
     * @param filename         filename of file to validate.
     * @param simpleValidation whether or not to just do a simple validation (yeah, I can process) or to validate
     *                         and possibly store some pre-processed data if necessary.
     * @return {@code true} if this is a valid file, {@code false} otherwise.
     */
    boolean validateFile(String filename, boolean simpleValidation);
}
