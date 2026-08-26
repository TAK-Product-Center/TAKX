package gov.takx.core.mapobject.gui.devicelog;

import gov.takx.api.annotation.PseudoBean;
import gov.takx.api.messages.IRaptorDataMessage;
import gov.takx.api.messages.IRaptorDataStructure;
import gov.takx.api.messages.NoSuchStructureException;
import gov.takx.api.persist.IPersistServiceProvider;
import gov.takx.api.query.IQueryConstraints;
import gov.takx.api.query.IQueryService;
import gov.takx.commons.swing.AlwaysOnTopFileChooserFactory;
import gov.takx.commons.swing.AlwaysOnTopJOptionPane;
import gov.takx.commons.swing.ErrorReportingSwingWorker;
import gov.takx.platform.io.csv.CsvExportStream;
import gov.takx.platform.preferences.UserPreferenceEntry;
import gov.takx.platform.utils.units.AltitudeUnits;
import gov.takx.platform.utils.units.SpeedUnits;
import jakarta.inject.Inject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.JFileChooser;
import javax.swing.JOptionPane;
import javax.swing.filechooser.FileFilter;
import javax.swing.filechooser.FileNameExtensionFilter;

import java.awt.Container;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.lang.invoke.MethodHandles;
import java.util.Iterator;
import java.util.List;

/**
 * Swing worker to handle the export operation on the device log.
 *
 * @since 1.49
 */
@SuppressWarnings({"CdiManagedBeanInconsistencyInspection", "CdiInjectionPointsInspection", "SpringAutowiredFieldsWarningInspection"})
@PseudoBean
public class ExportWorker extends ErrorReportingSwingWorker<Integer, IRaptorDataMessage>
{
    private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    private final IQueryService queryService;
    private final IRaptorDataStructure structure;
    private final List<IRaptorDataMessage> messages;
    private File selectedFile;
    private volatile boolean cancelled = false;

    @Inject
    private IPersistServiceProvider persistServiceProvider;

    /**
     * Construct an export worker to export ALL the messages of the given structure.
     *
     * @param queryService Query service to use
     * @param structure    The structure who's RDMs are to be exported, used to define CSV mapping
     */
    ExportWorker(IQueryService queryService, IRaptorDataStructure structure)
    {
        this(queryService, structure, null);
    }

    /**
     * Construct an export worker to export the given messages.
     *
     * @param queryService Query service to use
     * @param structure    The structure defining the RDMs to be exported, used to define CSV mapping
     * @param messages     The messages to export
     */
    ExportWorker(IQueryService queryService, IRaptorDataStructure structure, List<IRaptorDataMessage> messages)
    {
        super("Export Device Log Data Error", "Error exporting data from the device log to csv.");

        this.queryService = queryService;
        this.structure = structure;
        this.messages = messages;
    }

    /**
     * Query the user for the file to export to.
     *
     * @param parent UI parent for dialog construction
     * @return {@code true} if the user selected a file
     */
    boolean queryUserForExportFile(Container parent)
    {
        final JFileChooser chooser = AlwaysOnTopFileChooserFactory.getFileChooser(this);
        chooser.setAcceptAllFileFilterUsed(false);

        if (chooser.getChoosableFileFilters().length < 1)
        {
            FileFilter csvType = new FileNameExtensionFilter("Comma Separated Value (.csv)", "csv");
            chooser.addChoosableFileFilter(csvType);
        }

        int result = chooser.showSaveDialog(parent);

        if (JFileChooser.APPROVE_OPTION == result)
        {
            String absoluteFile = chooser.getSelectedFile().getAbsolutePath();

            boolean nameChanged = false;
            if (!absoluteFile.toLowerCase().endsWith(".csv"))
            {
                nameChanged = true;
                absoluteFile += ".csv";
            }

            final File file = new File(absoluteFile);

            if (logger.isDebugEnabled()) logger.debug("Selected file {}", file);

            int overwriteFile = JOptionPane.YES_OPTION;

            // The file chooser will have already have queries the user the exact file name matched an existing file.
            // However, if we added the ".csv" and that matches an existing file, we need to query the user.

            if (nameChanged && file.exists())
            {
                overwriteFile = AlwaysOnTopJOptionPane.showOptionDialog(chooser.getTopLevelAncestor(),
                        "The selected file '" + file.getName() + "' already exists. Do you want to overwrite it?",
                        "The file already exists", JOptionPane.QUESTION_MESSAGE,
                        JOptionPane.YES_NO_OPTION, null, new Object[]{"Yes", "No"}, JOptionPane.NO_OPTION);
            }

            if (overwriteFile == JOptionPane.YES_OPTION)
            {
                selectedFile = file;
                return true;
            }
        }

        return false;
    }

    @Override
    public Integer doInBackground()
    {
        Iterator<IRaptorDataMessage> iter = null;

        try (CsvExportStream csvExporter = new CsvExportStream(new FileOutputStream(selectedFile), structure))
        {
            csvExporter.setAltitudeUnits(UserPreferenceEntry.DISPLAY_ALTITUDE_UNITS.getValueAs(AltitudeUnits.class));
            csvExporter.setSpeedUnits(UserPreferenceEntry.DISPLAY_SPEED_UNITS.getValueAs(SpeedUnits.class));

            if (messages == null)
            {
                // Export all messages with the configured structure

                final IQueryConstraints constraints = queryService.createEmptyConstraints();
                constraints.setMaxMessageCount(-1);
                constraints.setSortingOptions(IQueryConstraints.SortOn.RECEIVE_DATE, IQueryConstraints.SortOrder.DESCENDING);
                constraints.addMessageStructureConstraint(structure.getMessageType());

                final long total = queryService.count(constraints);
                iter = queryService.iterator(constraints);

                export(csvExporter, total, iter);
            } else
            {
                export(csvExporter, messages.size(), messages.iterator()); // Export just the configured messages
            }
        } catch (NoSuchStructureException e)
        {
            logger.error("Error processing Export All request.", e);
        } catch (IOException ioe)
        {
            logger.warn("Unable to successfully export RDMs to CSV file at {}: {}", selectedFile.getAbsolutePath(), ioe.getMessage(), ioe);
        } finally
        {
            if (iter != null) queryService.closeIterator(iter);
            persistServiceProvider.cleanupPersistService(); // All done - be tidy
        }
        return 0;
    }

    /**
     * Export the messages in the given iterable.
     *
     * @param csvExporter CSV exporter to use
     * @param total       Total number of messages being exported
     * @param iterator    message iterator
     * @throws IOException export exception
     */
    private void export(CsvExportStream csvExporter, long total, Iterator<IRaptorDataMessage> iterator) throws IOException
    {
        if (logger.isDebugEnabled()) logger.debug("Total records to write: {}", total);

        long count = 0;

        while (iterator.hasNext() && !cancelled)
        {
            csvExporter.write(iterator.next());

            int percent = (int) (100 * (double) count++ / total);
            setProgress(percent);
        }
    }

    @Override
    public void done()
    {
        setProgress(0);
        super.done();
    }

    void cancelExport()
    {
        cancelled = true;
    }
}
