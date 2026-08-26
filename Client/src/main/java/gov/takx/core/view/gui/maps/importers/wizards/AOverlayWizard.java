package gov.takx.core.view.gui.maps.importers.wizards;

import gov.takx.api.io.Format;
import gov.takx.api.io.ImportExportResult;
import gov.takx.api.plugin.io.IImportExportProgressMonitor;
import gov.takx.commons.file.FileUtils;
import gov.takx.commons.wizard.IWizardProperties;
import gov.takx.core.view.gui.maps.AddOverlayController;
import gov.takx.core.view.gui.maps.OverlayUtils;
import jakarta.annotation.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.lang.invoke.MethodHandles;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * For importing raw SHP or KML/KMZ files onto the globe. Note that the wizard will remember whether or not the
 * user selected to import as surface structures and will replicate that mode on import of a new file.
 */
public abstract class AOverlayWizard extends AFileImportWizard
{
    private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());
    private static final String OVERLAY_AS_INTERACTIVE_SHAPES = "DO_OVERLAY_3D";

    private AddOverlayController controller = null;
    private final List<Format> formats = new ArrayList<>();
    private Boolean overlayAsInteractiveShapes = null;

    @Override
    public void preconfigureWizard(Map<String, Object> configuration)
    {
        super.preconfigureWizard(configuration);

        formats.add(new Format(FileUtils.getExtension(file), null, null));
        controller = getController(file);
    }

    @Override
    public ImportExportResult doImport(IWizardProperties props, IImportExportProgressMonitor progressMonitor)
            throws Exception
    {
        if (overlayAsInteractiveShapes != null) controller.doImportFile(overlayAsInteractiveShapes);
        return null;
    }

    @Override
    public String toString()
    {
        return controller.toString();
    }

    @Override
    public void runOperation(@Nullable Map<String, Object> preconfigProps)
    {
        try
        {
            overlayAsInteractiveShapes = preconfigProps != null
                    ? (Boolean) preconfigProps.get(OVERLAY_AS_INTERACTIVE_SHAPES)
                    : false;
            controller.doImportFile(overlayAsInteractiveShapes);
        } catch (Exception e)
        {
            logger.error("Error importing overlay.", e);
        }
    }

    @Override
    @Nullable
    public Map<String, Object> showImporterWizardDialog()
    {
        if (overlayAsInteractiveShapes == null)
        {
            overlayAsInteractiveShapes = OverlayUtils.requestOverlayTypeFromUser(controller.getType());
        }

        // If the user did not select simple or advanced, the import was cancelled
        if (overlayAsInteractiveShapes == null) throw new ImportCancelledException();

        final Map<String, Object> preconfigProps = new HashMap<>();
        preconfigProps.put(OVERLAY_AS_INTERACTIVE_SHAPES, overlayAsInteractiveShapes);
        return preconfigProps;
    }

    /**
     * Get the custom controller that will be used to import the file.
     *
     * @param file The file to import.
     * @return The subclasses custom controller responsible for performing the import.
     */
    protected abstract AddOverlayController getController(File file);
}
