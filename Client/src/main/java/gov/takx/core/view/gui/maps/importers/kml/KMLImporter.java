package gov.takx.core.view.gui.maps.importers.kml;

import gov.takx.api.annotation.PseudoBean;
import gov.takx.core.kml.KmlModel;
import gov.takx.core.view.gui.maps.importers.IImporter;
import jakarta.annotation.Nonnull;
import jakarta.inject.Inject;

import java.io.File;

/**
 * Importer handling KML/KMZ overlays.
 */
@SuppressWarnings("CdiManagedBeanInconsistencyInspection")
@PseudoBean
public class KMLImporter implements IImporter
{
    @Inject
    private KMLImportController kmlImportController;

    private final String kmlSource;
    private final KmlModel kmlModel;

    /**
     * @param kmlFile KML/KMZ file to process
     */
    public KMLImporter(@Nonnull File kmlFile)
    {
        kmlSource = kmlFile.getAbsolutePath();
        kmlModel = null;
    }

    /**
     * @param kmlModel KML Model to process
     */
    public KMLImporter(@Nonnull KmlModel kmlModel)
    {
        this.kmlModel = kmlModel;
        kmlSource = null;
    }

    @Override
    public void beginImport()
    {
        if (kmlModel != null)
        {
            kmlImportController.importKml(kmlModel);
        } else
        {
            kmlImportController.importKml(kmlSource);
        }
    }
}
