package gov.takx.core.globe.layers.worldwind;

import gov.nasa.worldwind.avlist.AVList;
import gov.nasa.worldwind.globes.ElevationModel;
import gov.nasa.worldwind.ogc.OGCCapabilities;
import gov.nasa.worldwind.terrain.BasicElevationModelFactory;
import org.w3c.dom.Element;

/**
 * WW elevation model factory that always produces {@code null} models.
 * @since 5.3
 */
public class NoopWorldwindElevationModelFactory extends BasicElevationModelFactory
{
    @Override
    protected ElevationModel doCreateFromCapabilities(OGCCapabilities caps, AVList params)
    {
        return null;
    }

    @Override
    protected ElevationModel createNonCompoundModel(Element domElement, AVList params)
    {
        return null;
    }
}
