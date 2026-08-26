package gov.takx.core.globe.tak.ww;

import com.atakmap.map.opengl.GLMapView;
import gov.nasa.worldwind.geom.Angle;
import gov.nasa.worldwind.geom.LatLon;
import gov.nasa.worldwind.geom.Sector;
import gov.nasa.worldwind.globes.Earth;

import java.util.List;

/**
 * An implementation of {@link Earth} that can defer elevation and surface point calls to TAK engine.
 *
 * @since 5.2
 */
public class TakEarthGlobe extends Earth
{
    private GLMapView glMapView;

    /**
     * Set the current {@link GLMapView} for this globe.
     * @param glMapView the map view
     */
    public void setGlMapView(GLMapView glMapView)
    {
        this.glMapView = glMapView;
    }

    @Override
    public double getElevations(Sector sector, List<? extends LatLon> latlons, double targetResolution, double[] elevations)
    {
        if (glMapView != null)
        {
            int index = 0;
            for (LatLon latlon : latlons)
            {
                elevations[index++] = getElevation(latlon.latitude, latlon.longitude);
            }

            return targetResolution;
        }
        return super.getElevations(sector, latlons, targetResolution, elevations);
    }

    @Override
    public double[] getElevations(Sector sector, List<? extends LatLon> latlons, double[] targetResolution, double[] elevations)
    {
        if (glMapView != null)
        {
            int index = 0;
            for (LatLon latlon : latlons)
            {
                elevations[index++] = getElevation(latlon.latitude, latlon.longitude);
            }

            // getElevation() pulls from the in-memory rendered terrain mesh and there is no way to get
            // the returned resolution so assume requested resolution was achieved.
            return targetResolution;
        }
        return super.getElevations(sector, latlons, targetResolution, elevations);
    }

    @Override
    public double getElevation(Angle latitude, Angle longitude)
    {
        return glMapView != null ? glMapView.getTerrainMeshElevation(latitude.degrees, longitude.degrees)
                : super.getElevation(latitude, longitude);
    }
}
