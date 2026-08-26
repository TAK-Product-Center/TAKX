package gov.takx.core.globe.imagerywizard;

import com.atakmap.map.layer.raster.DatasetDescriptorFactory2;
import gov.takx.core.globe.tak.elevation.ElevationLayerManager;
import gov.takx.core.view.gui.maps.importers.wizards.CustomMobacLayerWizard;
import gov.takx.core.view.gui.maps.importers.wizards.WmsImportWizard;
import org.apache.logging.log4j.core.util.FileUtils;

import java.io.File;

/**
 * Extension of {@link FileSetFilter} that checks with TAK kernel to determine if it can support the
 * imagery data set type.
 */
public class ImagerySetFilter extends FileSetFilter
{
    private static final WmsImportWizard WMS_IMPORT_WIZARD = new WmsImportWizard();
    private static final CustomMobacLayerWizard CUSTOM_MOBAC_LAYER = new CustomMobacLayerWizard();

    @Override
    public boolean accept(File file)
    {
        // TODO: TAKX-4610 TAKX-4596 - temporary fix to reject anything that is an online source
        String fileExtension = FileUtils.getFileExtension(file);
        fileExtension = fileExtension != null ? fileExtension : "";
        boolean onlineImagerySource = WMS_IMPORT_WIZARD.offerToProcessFile(fileExtension, file) ||
                CUSTOM_MOBAC_LAYER.offerToProcessFile(fileExtension, file);
        if (onlineImagerySource) return false;

        // GDAL gets a little noisy here.
        org.gdal.gdal.gdal.PushErrorHandler();
        boolean result = DatasetDescriptorFactory2.isSupported(file) && !ElevationLayerManager.isSupportedDtedFile(file);
        org.gdal.gdal.gdal.PopErrorHandler();
        return result;
    }
}
