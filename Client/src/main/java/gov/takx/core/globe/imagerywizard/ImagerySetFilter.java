package gov.takx.core.globe.imagerywizard;

import com.atakmap.map.layer.raster.DatasetDescriptorFactory2;
import gov.takx.core.globe.tak.elevation.ElevationLayerManager;

import java.io.File;

/**
 * Extension of {@link FileSetFilter} that checks with TAK kernel to determine if it can support the
 * imagery data set type.
 */
public class ImagerySetFilter extends FileSetFilter
{
    @Override
    public boolean accept(File file)
    {
        // GDAL gets a little noisy here.
        org.gdal.gdal.gdal.PushErrorHandler();
        boolean result = DatasetDescriptorFactory2.isSupported(file) && !ElevationLayerManager.isSupportedDtedFile(file);
        org.gdal.gdal.gdal.PopErrorHandler();
        return result;
    }
}
