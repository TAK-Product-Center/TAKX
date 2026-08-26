package gov.takx.core.globe.tak.elevation;

import com.atakmap.map.elevation.ElevationSource;
import gov.nasa.worldwind.avlist.AVKey;
import gov.nasa.worldwind.avlist.AVListImpl;
import gov.nasa.worldwind.geom.Angle;
import gov.nasa.worldwind.geom.LatLon;
import gov.nasa.worldwind.geom.Sector;
import gov.nasa.worldwind.terrain.AbstractElevationModel;
import gov.tak.api.engine.map.coords.IGeoPoint;
import gov.takx.api.terrain.experimental.ElevationSourceUtils;
import gov.takx.api.terrain.experimental.ITakElevationModel;

import java.util.List;

/**
 * An implementation of {@link gov.nasa.worldwind.globes.ElevationModel} that queries an {@link ElevationSource} for elevation data.
 * As noted in {@link ITakElevationModel}, WW ElevationModels originally returned all elevations in MSL, however all classes derived
 * from ITakElevationModel should return elevations in HAE.
 *
 * @since 3.4 (renamed from TakElevationModel and made abstract in 5.3)
 */
abstract class ATakElevationModel extends AbstractElevationModel implements ITakElevationModel
{
    private final ElevationSource elevationSource;
    private final String displayName;

    protected ATakElevationModel(ElevationSource elevationSource, String displayName)
    {
        this.elevationSource = elevationSource;
        this.displayName = displayName;
        setValue(AVKey.CONSTRUCTION_PARAMETERS, new AVListImpl());
    }

    @Override
    public int intersects(Sector sector)
    {
        if (getSector().contains(sector)) return 0;
        if (getSector().intersects(sector)) return 1;
        return -1;
    }

    @Override
    public boolean contains(Angle latitude, Angle longitude)
    {
        return getSector().contains(new LatLon(latitude, longitude));
    }

    @Override
    public double getMaxElevation()
    {
        return 1000;
    }

    @Override
    public double getMinElevation()
    {
        return -100;
    }

    @Override
    public double[] getExtremeElevations(Angle latitude, Angle longitude)
    {
        return new double[]{getMinElevation(), getMaxElevation()};
    }

    @Override
    public double[] getExtremeElevations(Sector sector)
    {
        return new double[]{getMinElevation(), getMaxElevation()};
    }

    @Override
    public double getUnmappedElevation(Angle latitude, Angle longitude)
    {
        return ElevationSourceUtils.queryElevation(elevationSource, latitude.degrees, longitude.degrees, Double.MIN_VALUE, getMissingDataSignal());
    }

    @Override
    public double getElevations(Sector sector, List<? extends LatLon> latlons, double targetResolution, double[] buffer)
    {
        List<IGeoPoint> geoPoints = toGeoPointList(latlons);
        ElevationSourceUtils.queryElevations(elevationSource, geoPoints, targetResolution, buffer, getMissingDataReplacement());
        return targetResolution;
    }

    @Override
    public double getUnmappedElevations(Sector sector, List<? extends LatLon> latlons, double targetResolution, double[] buffer)
    {
        List<IGeoPoint> geoPoints = toGeoPointList(latlons);
        ElevationSourceUtils.queryElevations(elevationSource, geoPoints, targetResolution, buffer, getMissingDataSignal());
        return targetResolution;
    }

    @Override
    public void setExtremesCachingEnabled(boolean enabled)
    {

    }

    @Override
    public boolean isExtremesCachingEnabled()
    {
        return false;
    }

    @Override
    public ElevationSource getElevationSource()
    {
        return elevationSource;
    }

    @Override
    public String getName()
    {
        return displayName;
    }

    private List<IGeoPoint> toGeoPointList(List<? extends LatLon> latlons)
    {
        return latlons.stream()
                .map(l -> (IGeoPoint) new gov.tak.api.engine.map.coords.GeoPoint(l.latitude.degrees, l.longitude.degrees))
                .toList();
    }
}
