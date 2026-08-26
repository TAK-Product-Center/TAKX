package gov.takx.core.view.gui.maps;

import gov.takx.core.globe.imagerywizard.ElevationFileSetFilter;
import gov.takx.core.globe.imagerywizard.FileSet;
import gov.takx.core.view.gui.maps.importers.ElevationFilesImporter;
import jakarta.annotation.Nonnull;

import java.io.File;
import java.util.List;
import java.util.Map;

/**
 * Dialog for connecting to local elevation files so they can be added as a layer on the globe.
 *
 * @since 3.4.1 (renamed from ConvertElevationDialog)
 */
public class LoadElevationFilesDialog extends ALoadMapFilesDialog
{
    protected static final String DESCRIPTION = "<html>Select a folder of source elevation data to load and " +
            "display on the globe. After completion, the generated elevation model layer can be viewed and managed " +
            "in the Layers panel.</html>";
    private static final MapsFileFilter ELEVATION_FILE_FILTER = new MapsFileFilter(true);
    protected static final String ELEVATION_ICON = "elevation_files.png";

    /**
     * Constructor for opening the dialog manually from the UI in order to select local elevation files to import.
     */
    public LoadElevationFilesDialog()
    {
        this((File[]) null);
    }

    /**
     * Constructor for displaying the dialog for elevation files that were dragged and dropped onto the globe.
     *
     * @param files The elevation files being imported
     */
    public LoadElevationFilesDialog(File... files)
    {
        super(true, "Elevation", ELEVATION_ICON, DESCRIPTION, new ElevationFileSetFilter(), ELEVATION_FILE_FILTER, files);
    }

    @Override
    protected void doImport(@Nonnull List<FileSet> selectedFileSets, @Nonnull Map<String, Object> wizardDataMap)
    {
        new ElevationFilesImporter(selectedFileSets, wizardDataMap).beginImport();
    }
}
