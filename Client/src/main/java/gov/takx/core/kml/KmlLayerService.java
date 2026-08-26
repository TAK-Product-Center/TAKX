package gov.takx.core.kml;

import gov.takx.api.kml.IKmlLayerService;
import gov.takx.core.view.gui.maps.importers.kml.KMLImportController;
import jakarta.annotation.Nonnull;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;

/**
 * Service to support plugin's adding KML/KMZ content to the Overlays.
 *
 * @since 1.24
 */
@ApplicationScoped
public class KmlLayerService implements IKmlLayerService
{
    private final KMLImportController importController;

    @Inject
    KmlLayerService(KMLImportController importController)
    {
        this.importController = importController;
    }

    @Override
    public void loadKmlFile(@Nonnull File kmlFile) throws IOException
    {
        if (!kmlFile.exists()) throw new IOException("File " + kmlFile.getAbsolutePath() + " does not exist");

        importController.importKml(kmlFile.getAbsolutePath()).join(); // Block until complete
    }

    @Override
    public void loadKml(@Nonnull InputStream source, @Nonnull String name)
    {
        importController.importKml(source, name).join(); // Block until complete
    }
}
