package gov.takx.core.globe.layers;

import gov.nasa.worldwind.geom.Angle;
import gov.nasa.worldwind.layers.LatLonGraticuleLayer;
import gov.nasa.worldwind.layers.Layer;
import gov.takx.api.annotation.InvokeOnEDT;
import gov.takx.api.coordinatesystem.CoordinateSystem;
import gov.takx.api.layer.IRaptorLayer;
import gov.takx.api.preferences.IPreferenceMonitor;
import gov.takx.platform.preferences.UserPreferenceEntry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.invoke.MethodHandles;

/**
 * Class that wraps a Latitude Longitude grid Layer from World Wind.  The purpose is to correctly
 * set the user preferred CoordinateSystem.
 *
 */
public class LatLonGridGlobeLayer extends WWGlobeLayer
{
    private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    private final IPreferenceMonitor<CoordinateSystem> coordinateSystemMonitor = UserPreferenceEntry.COORDINATE_SYSTEM.newMonitor(CoordinateSystem.class, this::coordinateSystemChanged);

    public LatLonGridGlobeLayer(IRaptorLayer iRaptorLayer, Layer layer)
    {
        super(iRaptorLayer, layer);

        setSharable(true); // We are sharable

        coordinateSystemChanged(coordinateSystemMonitor.getCurrentValue());
    }

    /**
     * Set the coordinate system in the layer so the correct grid is drawn.  Defaults to Decimal Degrees
     * if the coordinate system is unknown or set to MGRS.
     *
     * @param newCoordinateSystem New user specified coordinate system
     */
    @InvokeOnEDT
    private void coordinateSystemChanged(CoordinateSystem newCoordinateSystem)
    {
        if (layer instanceof LatLonGraticuleLayer)
        {
            String angleFormat = Angle.ANGLE_FORMAT_DD;
            switch (newCoordinateSystem)
            {
                case DM_DEGREES:
                    angleFormat = Angle.ANGLE_FORMAT_DM;
                    break;
                case DMS_DEGREES:
                    angleFormat = Angle.ANGLE_FORMAT_DMS;
                    break;
                case DECIMAL_DEGREES:
                case MGRS:
                    //do nothing as the format is already decimal degrees
                    break;
            }
            ((LatLonGraticuleLayer) layer).setAngleFormat(angleFormat);
        }
    }

    @Override
    public void dispose()
    {
        coordinateSystemMonitor.dispose();
        super.dispose();
    }
}
