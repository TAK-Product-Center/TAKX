package gov.takx.core.view.gui.maps.exporters.dataset;

import gov.takx.api.annotation.InvokeOnEDT;
import gov.takx.api.io.Format;
import gov.takx.api.io.ImportExportResult;
import gov.takx.api.plugin.io.ACancellableWizard;
import gov.takx.api.plugin.io.IExportWizard;
import gov.takx.api.plugin.io.IImportExportProgressMonitor;
import gov.takx.commons.swing.AlwaysOnTopJOptionPane;
import gov.takx.commons.wizard.IWizardProperties;
import gov.takx.core.dataset.DataSetBuilderParameters;
import gov.takx.core.globe.imagerywizard.ImagerySetFilter;
import gov.takx.core.view.gui.maps.importers.ImageryFilesImporter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.JOptionPane;

import java.io.File;
import java.lang.invoke.MethodHandles;
import java.util.Arrays;
import java.util.List;

/**
 * Wizard used to walk a user through the steps required to build a new map set.
 *
 * @since 1.43
 */
public class MapSetBuilderWizard extends ACancellableWizard implements IExportWizard
{
    private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    /**
     * The map set builder. Keep a reference so that it can be cancelled.
     */
    private MapSetBuilder builder;

    @Override
    public void setFormat(Format format)
    {
        // n/a
    }

    @Override
    public String getFirstWizardPanel()
    {
        return "";
    }

    @Override
    public ImportExportResult doExport(IWizardProperties props, IImportExportProgressMonitor progressMonitor)
    {
        int autoRetryAttempts = 30;  // 30 attempts of potential 10 minute network retries equals could possibly be 5 hours of retries
        int manualRetryAttempts = 3;

        progressMonitor.printMessage("Preparing Map Set data...");

        String fullPath = props.getStringProperty(DataSetBuilderConstants.TARGET_DIR) + File.separator + props.getStringProperty(DataSetBuilderConstants.NAME);

        //noinspection unchecked
        builder = new MapSetBuilder(
                props.getStringProperty(DataSetBuilderConstants.NAME),
                fullPath,
                (List<DataSetBuilderParameters>) props.getProperty(DataSetBuilderConstants.LAYERS),
                progressMonitor);

        ImportExportResult result = new ImportExportResult();
        builder.build(result);

        // Automatically reattempt to rebuild MapSet
        if (!result.getSuccess())
        {
            progressMonitor.setProgressIndeterminate(true);
            retryExport(builder, result, autoRetryAttempts, "Automatic");
        }

        // Alert user and get input if they want to retry again
        if (!result.getSuccess())
        {
            // Allow user to retry building map
            int userOption = AlwaysOnTopJOptionPane.showConfirmDialog(null, String.format("Failed to auto-reconnect to network %d time(s) already.\n %s \nDo you want to retry Map Set Builder", autoRetryAttempts, result.getMessage()));
            if (userOption == JOptionPane.YES_OPTION)
            {
                // Attempt to reconnect 3 times
                retryExport(builder, result, manualRetryAttempts, "Manual");

                // Finally display any errors if result is false
                if (!result.getSuccess())
                {
                    displayError(result.getMessage());
                }
            }
        }

        // When the build is complete, null out the reference.
        builder = null;

        // If the user requested to add the new map data to TAKX, add it.
        if (props.getBooleanProperty(DataSetBuilderConstants.CONNECT) && result.getSuccess())
        {
            progressMonitor.printMessage("Connecting to Map Set...");
            connectToCache(fullPath, props.getStringProperty(DataSetBuilderConstants.NAME));
        }

        return result;
    }

    private void retryExport(MapSetBuilder builder, ImportExportResult result, int numOfRetries, String retryMode)
    {
        for (int i = 0; i < numOfRetries; i++)
        {
            logger.info("Attempting retries {} of {} from {} mode", i, numOfRetries, retryMode);

            builder.retry(result);
            if (result.getSuccess())
            {
                break;
            }
        }
    }

    @Override
    public String getDisplayName()
    {
        return "Map Set Builder";
    }

    @Override
    public void cancel()
    {
        super.cancel();

        if (builder != null)
        {
            builder.stop();
        }
    }

    @InvokeOnEDT
    private void displayError(String message)
    {
        AlwaysOnTopJOptionPane.showErrorDialog(message, "Map Set Builder Error");
    }

    /**
     * Connect to any descriptors found in the cache location file store. Make sure to execute this
     * in the background since it can be a lengthy process.
     */
    private void connectToCache(String dir, String displayName)
    {
        ImagerySetFilter filter = new ImagerySetFilter();
        File basePath = new File(dir);
        List<File> files = Arrays.asList(basePath.listFiles(filter::accept));
        new ImageryFilesImporter(files, displayName, dir).beginImport();
    }
}
