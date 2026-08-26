package gov.takx.core.globe.imagerywizard;

import gov.takx.api.annotation.PseudoBean;
import gov.takx.api.layer.LayerGroup;
import gov.takx.core.globe.tak.elevation.ElevationLayer;
import gov.takx.core.globe.tak.elevation.ElevationLayerFactory;
import gov.takx.dataservices.persist.RaptorLayer;
import jakarta.inject.Inject;

import java.io.File;

/**
 * Extension of {@link FileSetFilter} that checks with TAK kernel to determine if it can support the
 * elevation data set type.
 *
 * @since 3.4
 */
@PseudoBean
public class ElevationFileSetFilter extends FileSetFilter
{
    @Inject
    private ElevationLayerFactory elevationLayerFactory;

    @Override
    public boolean accept(File file)
    {
        RaptorLayer raptorLayer = new RaptorLayer(file.getName(), file.getName(), ElevationLayer.class.getName(),
                null, true, false, null, LayerGroup.ELEVATION, null,
                file.getAbsolutePath(), false, 0, Double.NaN, Double.NaN, true);
        return elevationLayerFactory.isSupported(raptorLayer);
    }
}
