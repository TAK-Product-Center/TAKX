package gov.takx.core.globe.layers.mapsforge;

import gov.takx.api.layer.IRaptorLayer;
import gov.takx.core.globe.layers.WWGlobeLayer;

/**
 * Globe layer to "front" a Mapsforge map file layer.
 *
 * @see MapsforgeMapFileLayer
 * @since 1.35
 */
public class MapsForgeGlobeLayer extends WWGlobeLayer
{
    /**
     * Constructor.
     *
     * @param raptorLayer  The raptor layer we represent
     * @param mapFileLayer The underlying WW layer
     */
    MapsForgeGlobeLayer(IRaptorLayer raptorLayer, MapsforgeMapFileLayer mapFileLayer)
    {
        super(raptorLayer, mapFileLayer);

        setSharable(true); // We are sharable
    }
}
