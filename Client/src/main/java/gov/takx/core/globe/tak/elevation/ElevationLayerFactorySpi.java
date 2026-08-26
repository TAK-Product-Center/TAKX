package gov.takx.core.globe.tak.elevation;

import com.atakmap.map.elevation.ElevationSource;
import com.atakmap.spi.InteractiveServiceProvider;
import com.atakmap.spi.PriorityServiceProvider;
import gov.takx.api.layer.IRaptorLayer;
import gov.takx.api.terrain.experimental.ITakElevationModel;
import gov.takx.core.globe.tak.elevation.ElevationLayerFactory.ElevationLayerPair;

/**
 * A service provider interface for creating an {@link ElevationSource} and {@link ITakElevationModel} pair from an {@link IRaptorLayer}.
 *
 * @since 5.3
 */
public interface ElevationLayerFactorySpi extends PriorityServiceProvider<ElevationLayerPair, IRaptorLayer>, InteractiveServiceProvider<ElevationLayerPair, IRaptorLayer>
{
}
