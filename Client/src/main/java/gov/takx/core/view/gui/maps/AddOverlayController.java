package gov.takx.core.view.gui.maps;

import gov.takx.api.annotation.PseudoBean;
import gov.takx.api.structures.ISurfaceStructureService;
import jakarta.inject.Inject;

import java.io.File;

/**
 */
@SuppressWarnings("CdiManagedBeanInconsistencyInspection")
@PseudoBean
public abstract class AddOverlayController
{
    @Inject
    private ISurfaceStructureService surfaceStructureService;

    private final File file;

    protected AddOverlayController(File file)
    {
        this.file = file;
    }

    /**
     * The extending class may optionally override this method to import an overlay file with 3D geometry.
     *
     * @param importAsInteractiveOverlay if {@code true}, then the importer should import as an interactive overlay
     *                                   into the structure file service.  Otherwise, the importer should
     *                                   just import as ordinary shapes.
     */
    public void doImportFile(boolean importAsInteractiveOverlay)
    {
        if (importAsInteractiveOverlay)
        {
            surfaceStructureService.loadShapeFile(getFile());
        } else
        {
            importAsSimpleOverlay();
        }
    }

    /**
     * This method imports the file with out it being registered with the SurfaceStructureService so it will not
     * interact with plugins.
     */
    protected abstract void importAsSimpleOverlay();

    /**
     * The extending class may optionally override this method to detect 3D geometry in the overlay file.  Used in
     * conjunction with detect3DOverlayFileAndImport and doImportFile.
     *
     * @return {@code true} if the file has 3D geometry, or {@code false} otherwise.
     */
    public abstract boolean doesFileHave3DGeometry();

    /**
     * Gets the type of the file to be imported
     *
     * @return The type of the file. e.g. ShapeFile, KML
     */
    public abstract String getType();

    /**
     * Gets the display name for the controller type
     *
     * @return The name that can be displayed in UI components
     */
    public abstract String getName();

    public File getFile()
    {
        return file;
    }
}
