package gov.takx.core.view.gui.maps;

import gov.takx.core.globe.imagerywizard.FileSet;
import gov.takx.core.globe.imagerywizard.ImagerySetFilter;
import gov.takx.core.view.gui.maps.importers.ImageryFilesImporter;
import jakarta.annotation.Nonnull;

import java.io.File;
import java.util.List;
import java.util.Map;

/**
 * Dialog for connecting to local imagery files so they can be added as a layer on the globe.
 *
 * @since 3.4.1 (renamed from ConvertImageryDialog)
 */
public class LoadImageryFilesDialog extends ALoadMapFilesDialog
{
    private static final String DESCRIPTION = "<html>Select a file or folder of source imagery to load and display " +
            "on the globe. After completion, the imagery layer can be viewed and managed in the Layers panel.</html>";
    private static final MapsFileFilter IMAGERY_FILE_FILTER = new MapsFileFilter(false);
    protected static final String IMAGERY_ICON = "imagery_files.png";

    /**
     * Constructor for opening the dialog manually from the UI in order to select local imagery files to import.
     */
    public LoadImageryFilesDialog()
    {
        this((File[]) null);
    }

    /**
     * Constructor for displaying the dialog for imagery files that were dragged and dropped onto the globe.
     *
     * @param files The imagery files being imported
     */
    public LoadImageryFilesDialog(File... files)
    {
        super(true, "Imagery", IMAGERY_ICON, DESCRIPTION, new ImagerySetFilter(), IMAGERY_FILE_FILTER, files);
    }

    @Override
    protected void doImport(@Nonnull List<FileSet> selectedFileSets, @Nonnull Map<String, Object> wizardDataMap)
    {
        new ImageryFilesImporter(selectedFileSets, wizardDataMap).beginImport();
    }
}
