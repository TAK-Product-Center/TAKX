package gov.takx.core.view.gui.maps;

import gov.takx.core.globe.imagerywizard.FileSet;
import gov.takx.core.globe.imagerywizard.ModelFileSetFilter;
import gov.takx.core.view.gui.maps.importers.ModelFilesImporter;
import jakarta.annotation.Nonnull;

import java.io.File;
import java.util.List;
import java.util.Map;

/**
 * Dialog for connecting to local 3D model files so they can be added as a layer on the globe.
 *
 * @since 5.0
 */
public class LoadModelFilesDialog extends ALoadMapFilesDialog
{
    private static final String DESCRIPTION = "<html>Select a file or folder of source 3D model data to load and display " +
            "on the globe. After completion, the 3D model layer can be viewed and managed in the Layers panel.</html>";
    private static final ModelFileSetFilter MODEL_FILE_FILTER = new ModelFileSetFilter();
    // TODO icon
    protected static final String MODEL_ICON = "imagery_files.png";

    /**
     * Constructor for opening the dialog manually from the UI in order to select local 3D model files to import.
     */
    public LoadModelFilesDialog()
    {
        this((File[]) null);
    }

    /**
     * Constructor for displaying the dialog for 3D model files that were dragged and dropped onto the globe.
     *
     * @param files The 3D model files being imported
     */
    public LoadModelFilesDialog(File... files)
    {
        super(true, "Model", MODEL_ICON, DESCRIPTION, MODEL_FILE_FILTER, MODEL_FILE_FILTER, files);
    }

    @Override
    protected void doImport(@Nonnull List<FileSet> selectedFileSets, @Nonnull Map<String, Object> wizardDataMap)
    {
        new ModelFilesImporter(selectedFileSets).beginImport();
    }
}
