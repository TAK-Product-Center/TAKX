package gov.takx.core.view.gui.maps;

import de.micromata.opengis.kml.v_2_2_0.AltitudeMode;
import de.micromata.opengis.kml.v_2_2_0.Geometry;
import de.micromata.opengis.kml.v_2_2_0.LineString;
import de.micromata.opengis.kml.v_2_2_0.Placemark;
import de.micromata.opengis.kml.v_2_2_0.Polygon;
import gov.takx.core.kml.KmlModel;
import gov.takx.core.security.CertUtils;
import gov.takx.core.view.gui.maps.importers.kml.KMLImporter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.lang.invoke.MethodHandles;
import java.util.ArrayList;
import java.util.List;

/**
 * Controller for adding a KML overlay.
 *
 * @see KmlModel
 * @see KMLImporter
 */
public class AddKMLOverlayController extends AddOverlayController
{
    private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    private static final String KML_OVERLAY = "kml_overlay";
    private static final String DISPLAY_NAME = "KML/KMZ Overlay Importer";

    private KmlModel kmlModel;

    public AddKMLOverlayController(File file)
    {
        super(file);
    }

    @Override
    public boolean doesFileHave3DGeometry()
    {
        List<Placemark> placemarks = new ArrayList<>();
        try
        {
            kmlModel = KmlModel.parseModel(getFile());
            kmlModel.visitFeaturesOfType(Placemark.class, placemarks::add);
        } catch (Exception e)
        {
            logger.warn("Unable to parse KML/KMZ file: {}", getFile(), e);
            return false;
        }

        final List<Geometry> geometries = new ArrayList<>();
        for (Placemark placemark : placemarks) geometries.addAll(kmlModel.getGeometry(placemark));

        for (Geometry geometry : geometries)
        {
            if (geometry instanceof Polygon polygon)
            {
                if (Boolean.TRUE.equals(polygon.isExtrude()) || AltitudeMode.CLAMP_TO_GROUND != polygon.getAltitudeMode()) return true;
            } else if (geometry instanceof LineString lineString)
            {
                if (Boolean.TRUE.equals(lineString.isExtrude()) || AltitudeMode.CLAMP_TO_GROUND != lineString.getAltitudeMode()) return true;
            }
        }
        return false;
    }

    /**
     * Static KML simple overlay import.
     *
     * @param kmlFile KML/KMZ file to import as a simple overlay.
     * @since 1.43
     */
    public static void importAsSimpleOverlay(File kmlFile)
    {
        CertUtils.enablePKIKeyUse();
        new KMLImporter(kmlFile).beginImport();
    }

    /**
     * Import the KML Overlay.  Makes sure to enable PKI key use in case it contains a network link requiring
     * a client PKI key.
     */
    @Override
    protected void importAsSimpleOverlay()
    {
        // If we already parsed the KML into a model, then import it directly instead of re-parsing it from source.
        if (kmlModel != null)
        {
            new KMLImporter(kmlModel).beginImport();
        } else
        {
            importAsSimpleOverlay(getFile());
        }
    }

    @Override
    public String getType()
    {
        return KML_OVERLAY;
    }

    @Override
    public String toString()
    {
        return getName();
    }

    @Override
    public String getName()
    {
        return DISPLAY_NAME;
    }
}
