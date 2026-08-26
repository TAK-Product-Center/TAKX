package gov.takx.api.plugin.io;

import gov.takx.api.io.ImportExportResult;
import gov.takx.commons.wizard.IWizardProperties;

import java.io.IOException;
import java.util.Map;

/**
 * Interface for a cancellable import parser.  Wizards that extend {@link ACancellableImportWizard} should contribute
 * parsers that implement this interface.
 *
 * @see ACancellableImportWizard#createImportParser(String, Map, IWizardProperties)
 * @since 2.14
 */
public interface ICancellableImportParser
{
    /**
     * Parse the data, return a result code.
     *
     * @return result of parsing the data
     * @throws IOException file io exception
     */
    ImportExportResult parse() throws IOException;

    /**
     * Cancel the parsing.  Usually user initiated through the progress panel
     */
    void cancel();

    /**
     * Set the monitor, allowing the import plugin parser to update progress.
     *
     * @param monitor import monitor
     */
    void setMonitor(IImportMonitor monitor);
}
