package gov.takx.core.globe.tak.elevation;

import com.atakmap.map.elevation.ElevationSource;
import com.atakmap.map.layer.AbstractLayer;

/**
 * A {@link com.atakmap.map.layer.Layer} that is paired with an {@link ElevationSource} in the {@link ElevationLayerManager}
 * and monitored for visibility.
 *
 * @since 3.4
 */
public class ElevationLayer extends AbstractLayer
{
    public ElevationLayer(String name)
    {
        super(name);
    }
}
