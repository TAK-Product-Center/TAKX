package gov.takx.core.globe.tak.elevation;

import com.atakmap.map.elevation.ElevationManager;
import com.atakmap.map.elevation.ElevationSource;
import com.atakmap.map.elevation.ElevationSourceBuilder;
import com.atakmap.map.layer.feature.geometry.Envelope;
import gov.nasa.worldwind.geom.Angle;
import gov.nasa.worldwind.geom.Intersection;
import gov.nasa.worldwind.geom.LatLon;
import gov.nasa.worldwind.geom.Position;
import gov.nasa.worldwind.geom.Sector;
import gov.nasa.worldwind.geom.Vec4;
import gov.nasa.worldwind.globes.Globe;
import gov.nasa.worldwind.terrain.CompoundElevationModel;
import gov.tak.api.engine.map.IMapRendererEnums.DisplayOrigin;
import gov.tak.api.engine.map.MapSceneModel;
import gov.tak.api.engine.map.coords.GeoCalculations;
import gov.tak.api.engine.map.coords.GeoPoint;
import gov.tak.api.engine.map.coords.IGeoPoint;
import gov.tak.api.engine.math.PointD;
import gov.tak.api.engine.math.Ray;
import gov.tak.api.engine.math.Vector;
import gov.takx.api.coordinatesystem.ILocation;
import gov.takx.api.terrain.experimental.ElevationSourceUtils;
import gov.takx.api.terrain.experimental.IHighestResolutionTerrain;
import gov.takx.api.terrain.experimental.ITakElevationModel;
import gov.takx.api.terrain.experimental.ITerrainListener;
import gov.takx.api.terrain.experimental.PositionLineSegment;
import gov.takx.api.terrain.experimental.TerrainUtils;
import gov.takx.core.globe.ApplicationGlobe;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Stream;

/**
 * An implementation of {@link IHighestResolutionTerrain} that works with {@link ITakElevationModel}.
 *
 * @since 5.2
 */
public class TakHighestResolutionTerrain implements IHighestResolutionTerrain
{
    // Altitude modes
    public static final int ABSOLUTE = 0;
    public static final int CLAMP_TO_GROUND = 1;
    public static final int RELATIVE_TO_GROUND = 2;
    public static final int CONSTANT = 3;

    private final Globe globe;
    private final ApplicationGlobe appGlobe;
    private List<ITakElevationModel> elevationModelsOfInterest = Collections.emptyList();
    private ElevationSource elevationSourceOfInterest;
    private boolean useBestLocallyAvailableResolution;
    private long timeout = 25000L;
    private long totalValueCount;
    private long invalidCount;
    private boolean dynamicValueReplacement;
    private boolean trackInvalidData;
    private Sector sector;
    private final List<ITerrainListener> terrainListeners = new ArrayList<>();
    private double verticalExaggeration;

    public TakHighestResolutionTerrain(@Nonnull Globe globe, @Nullable ApplicationGlobe appGlobe)
    {
        this.globe = globe;
        this.appGlobe = appGlobe;
    }

    @Override
    public Vec4 getSurfacePoint(Position position)
    {
        return getSurfacePoint(position.latitude, position.longitude, position.elevation);
    }

    @Override
    public Vec4 getSurfacePoint(Angle latitude, Angle longitude, double metersOffset)
    {
        double elevation = getElevation(latitude.degrees, longitude.degrees);
        if (appGlobe != null)
        {
            MapSceneModel mapSceneModel = appGlobe.getPortableMapRenderer().getMapSceneModel(false, DisplayOrigin.UpperLeft);
            var geoPoint = new GeoPoint(latitude.degrees, longitude.degrees, elevation + metersOffset);
            PointD forward = new PointD();
            mapSceneModel.mapProjection.forward(geoPoint, forward);
            return new Vec4(forward.x, forward.y, forward.z);
        } else
        {
            return new Vec4(longitude.degrees, latitude.degrees, elevation);
        }
    }

    @Override
    public Intersection[] intersect(@Nonnull Position pA, @Nonnull Position pB)
    {
        return intersect(pA, pB, RELATIVE_TO_GROUND);
    }

    @Override
    public Intersection[] intersect(@Nonnull Position pA, @Nonnull Position pB, int altitudeMode)
    {
        var intersections = intersect(List.of(new PositionLineSegment(pA, pB)), altitudeMode, 0);
        return intersections.values().stream().findFirst().orElse(null);
    }

    @Override
    public Map<PositionLineSegment, Intersection[]> intersect(Collection<PositionLineSegment> positionLineSegments, int altitudeMode, int limit)
    {
        Envelope.Builder builder = new Envelope.Builder();
        positionLineSegments.stream()
                .flatMap(p -> Stream.of(p.posA(), p.posB()))
                .forEach(p -> builder.add(p.longitude.degrees, p.latitude.degrees, p.elevation));
        Envelope envelope = builder.build();

        HashMap<Ray, PositionLineSegment> rayMap = new HashMap<>();
        for (PositionLineSegment positionLineSegment : positionLineSegments)
        {
            Position pA = positionLineSegment.posA();
            Position pB = positionLineSegment.posB();
            double altitudeA = pA.getAltitude();
            double altitudeB = pB.getAltitude();
            if (altitudeMode == RELATIVE_TO_GROUND)
            {
                altitudeA += getElevation(pA);
                altitudeB += getElevation(pB);
            } else if (altitudeMode == CLAMP_TO_GROUND)
            {
                altitudeA = getElevation(pA);
                altitudeB = getElevation(pB);
            }

            PointD rayOrg = new PointD(pA.longitude.degrees, pA.latitude.degrees, altitudeA);
            PointD rayTgt = new PointD(pB.longitude.degrees, pB.latitude.degrees, altitudeB);
            Ray ray = new Ray(rayOrg, new Vector(rayTgt.x - rayOrg.x, rayTgt.y - rayOrg.y, rayTgt.z - rayOrg.z));
            rayMap.put(ray, positionLineSegment);
        }
        var intersect = ElevationSourceUtils.intersect(getElevationSourceOfInterest(), envelope, rayMap.keySet(), limit);
        MapSceneModel mapSceneModel = appGlobe != null ? appGlobe.getPortableMapRenderer().getMapSceneModel(false, DisplayOrigin.UpperLeft) : null;
        Map<PositionLineSegment, Intersection[]> results = new HashMap<>();
        for (var entry : intersect.entrySet())
        {
            PositionLineSegment positionLineSegment = rayMap.get(entry.getKey());
            if (positionLineSegment != null)
            {
                Position posA = positionLineSegment.posA();
                Position posB = positionLineSegment.posB();
                if (mapSceneModel == null) continue;
                PointD pointA = new PointD();
                PointD pointB = new PointD();
                if (!mapSceneModel.mapProjection.forward(new GeoPoint(posA.getLatitude().degrees, posA.getLongitude().degrees, posA.getAltitude()), pointA)
                        || !mapSceneModel.mapProjection.forward(new GeoPoint(posB.getLatitude().degrees, posB.getLongitude().degrees, posB.getAltitude()), pointB))
                    continue;
                double lineSegmentDistance = distance2(pointA, pointB);
                Intersection[] intersections = entry.getValue().stream()
                        .map(p -> {
                            PointD pointC = new PointD();
                            if (!mapSceneModel.mapProjection.forward(new GeoPoint(p.getLatitude(), p.getLongitude(), p.getAltitude()), pointC)) return null;
                            double intersectionSegmentDistance = distance2(pointA, pointC);
                            if (intersectionSegmentDistance > lineSegmentDistance) return null;
                            Vec4 point = new Vec4(pointC.x, pointC.y, pointC.z);
                            Position position = Position.fromDegrees(p.getLatitude(), p.getLongitude(), p.getAltitude());
                            return new Intersection(point, position, false, null);
                        })
                        .filter(Objects::nonNull)
                        .toArray(Intersection[]::new);
                results.put(positionLineSegment, intersections);
            }
        }
        return results;
    }

    @Override
    public Double getElevation(LatLon location)
    {
        return getElevation(location.latitude.degrees, location.longitude.degrees);
    }

    @Override
    public double getElevation(double latitude, double longitude)
    {
        ElevationSource elevationSource = getElevationSourceOfInterest();
        return ElevationSourceUtils.queryElevation(elevationSource, latitude, longitude, Double.MIN_VALUE, null);
    }

    @Override
    public void getElevations(Sector sector, List<? extends LatLon> latlons, double[] buffer)
    {
        // calculate a reasonable resolution
        double hypMeters = GeoCalculations.distance(new gov.tak.api.maps.coords.GeoPoint(sector.getMinLatitude().degrees, sector.getMinLongitude().degrees),
                new gov.tak.api.maps.coords.GeoPoint(sector.getMaxLatitude().degrees, sector.getMaxLongitude().degrees));
        double targetResolution = hypMeters / 200;
        List<IGeoPoint> geoPoints = toGeoPointList(latlons);
        ElevationSource elevationSource = getElevationSourceOfInterest();
        ElevationSourceUtils.queryElevations(elevationSource, geoPoints, targetResolution, buffer, null);
    }

    @Override
    public boolean createHeightmap(double[] result, double gsd, ElevationManager.HeightmapParams params, ElevationManager.HeightmapStrategy strategy)
    {
        return ElevationManager.createHeightmap(result, getElevationSourceOfInterest(), gsd, params, strategy);
    }

    @Override
    public void createHeightmapLLA(double[] result, double gsd, ElevationManager.HeightmapParams params)
    {
        ElevationSourceUtils.createHeightmapLLA(getElevationSourceOfInterest(), gsd, params, result);
    }

    @Override
    public double getBestResolutionForSector(Sector sector)
    {
        return getBestResolutionForArea(new gov.tak.api.maps.coords.GeoPoint(sector.getMinLatitude().degrees, sector.getMinLongitude().degrees),
                new gov.tak.api.maps.coords.GeoPoint(sector.getMaxLatitude().degrees, sector.getMaxLongitude().degrees));
    }

    @Override
    public double getBestResolutionForArea(ILocation locationA, ILocation locationB)
    {
        CompoundElevationModel compoundElevationModel = new CompoundElevationModel();
        getElevationModelsOfInterest().forEach(compoundElevationModel::addElevationModel);
        Sector sector = null;
        if (locationA != null && locationB != null)
        {
            double minLatitude = Math.min(locationA.getLatitude(), locationB.getLatitude());
            double maxLatitude = Math.max(locationA.getLatitude(), locationB.getLatitude());
            double minLongitude = Math.min(locationA.getLongitude(), locationB.getLongitude());
            double maxLongitude = Math.max(locationA.getLongitude(), locationB.getLongitude());
            sector = Sector.fromDegrees(minLatitude, maxLatitude, minLongitude, maxLongitude);
        }
        // if sector is null, getBestResolution will return the elevation model's best overall resolution
        return compoundElevationModel.getBestResolution(sector);
    }

    @Override
    public List<ITakElevationModel> getAllElevationModels()
    {
        return new ArrayList<>(TerrainUtils.splitModels(ITakElevationModel.class, globe.getElevationModel()));
    }

    @Override
    public List<ITakElevationModel> getElevationModelsOfInterest()
    {
        return elevationModelsOfInterest != null ? elevationModelsOfInterest : getAllElevationModels();
    }

    @Override
    public void setElevationModelsOfInterest(List<ITakElevationModel> elevationModels)
    {
        elevationModelsOfInterest = new ArrayList<>(elevationModels);
        elevationSourceOfInterest = null;
        for (ITerrainListener listener : terrainListeners)
        {
            listener.elevationModelsChanged(new ArrayList<>(elevationModels));
        }
    }

    @Nonnull
    public ElevationSource getElevationSourceOfInterest()
    {
        if (elevationSourceOfInterest == null)
        {
            ElevationSource[] elevationSources = getElevationModelsOfInterest().stream()
                    .map(ITakElevationModel::getElevationSource)
                    .toArray(ElevationSource[]::new);
            elevationSourceOfInterest = ElevationSourceBuilder.multiplex("Highest Resolution Terrain Multiplex", elevationSources);
        }
        return elevationSourceOfInterest;
    }

    @Override
    public void setGeometryCacheSize(long size)
    {
        // No cache used
    }

    @Override
    public boolean isUsingBestLocallyAvailableResolution()
    {
        return useBestLocallyAvailableResolution;
    }

    @Override
    public void enableUseBestLocallyAvailableResolution(boolean enable)
    {
        useBestLocallyAvailableResolution = enable;
    }

    @Override
    public void setTimeout(long timeout)
    {
        this.timeout = timeout;
    }

    @Override
    public long getTotalValueCount()
    {
        return totalValueCount;
    }

    @Override
    public long getInvalidCount()
    {
        return invalidCount;
    }

    @Override
    public double getInvalidRatio()
    {
        return totalValueCount != 0 ? (double) invalidCount / totalValueCount : 0;
    }

    @Override
    public void clearValueCounts()
    {
        totalValueCount = 0;
        invalidCount = 0;
    }

    @Override
    public boolean isDynamicValueReplacementEnabled()
    {
        return dynamicValueReplacement;
    }

    @Override
    public void setDynamicValueReplacementEnabled(boolean enabled)
    {
        dynamicValueReplacement = enabled;
    }

    @Override
    public boolean isTrackInvalidDataEnabled()
    {
        return trackInvalidData;
    }

    @Override
    public void setTrackInvalidDataEnabled(boolean enabled)
    {
        trackInvalidData = enabled;
    }

    @Override
    public void setSectorOfInterest(Sector sector)
    {
        this.sector = sector;
        clearValueCounts();
    }

    @Override
    public void clearCache()
    {
        // Nothing cached
    }

    @Override
    public void addTerrainListener(ITerrainListener listener)
    {
        terrainListeners.add(listener);
    }

    @Override
    public void removeTerrainListener(ITerrainListener listener)
    {
        terrainListeners.remove(listener);
    }

    @Override
    public Globe getGlobe()
    {
        return globe;
    }

    @Override
    public double getVerticalExaggeration()
    {
        return verticalExaggeration;
    }

    private static List<IGeoPoint> toGeoPointList(List<? extends LatLon> latlons)
    {
        return latlons.stream()
                .map(l -> (IGeoPoint) new GeoPoint(l.latitude.degrees, l.longitude.degrees))
                .toList();
    }

    /**
     * Gets the squared distance between two points. Avoids sqrt() when only interested in relative distance.
     */
    private static double distance2(PointD p1, PointD p2)
    {
        final double dx = (p2.x - p1.x);
        final double dy = (p2.y - p1.y);
        final double dz = (p2.z - p1.z);

        return dx * dx + dy * dy + dz * dz;
    }
}
