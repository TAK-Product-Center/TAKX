package gov.takx.core.globe;

import gov.nasa.worldwind.geom.Vec4;
import gov.tak.api.engine.map.coords.GeoCalculations;
import gov.tak.api.maps.coords.GeoPoint;
import gov.takx.api.annotation.InvokeAndWaitOnEDT;
import gov.takx.api.annotation.PseudoBean;
import gov.takx.api.coordinatesystem.ILocation;
import gov.takx.api.globe.IExtendedGlobeManager;
import gov.takx.api.globe.IGlobeRangeFanMapObject;
import gov.takx.api.mapobject.IMapObject;
import gov.takx.api.mapobject.IShapeRangeFanMapObject;
import gov.takx.api.mapobject.MapToolType;
import gov.takx.api.platform.IProgressCallback;
import gov.takx.api.shapes.IMutableRangeFan;
import gov.takx.api.shapes.IRangeFan;
import gov.takx.api.shapes.ShapeProperties;
import gov.takx.api.terrain.experimental.IImmutableHighestResolutionTerrain;
import gov.takx.api.terrain.experimental.ITerrainService;
import gov.takx.commons.swing.ErrorReportingSwingWorker;
import gov.takx.core.globe.tak.RenderableManager;
import gov.takx.platform.utils.WWUtils;
import jakarta.annotation.Nonnull;
import jakarta.inject.Inject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.beans.PropertyChangeEvent;
import java.lang.invoke.MethodHandles;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.stream.IntStream;

/**
 * Map object that draws rings to show ranges around a center point. This map object can draw full rings, or arcs
 * to show range fans in a particular direction.
 *
 * @since 1.16
 */
@PseudoBean
public class GlobeRangeFanMapObject extends GlobeShapeMapObject implements IGlobeRangeFanMapObject
{
    private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    private static final List<String> PROPERTIES_TO_WATCH = List.of(
            ShapeProperties.EDITABLE, ShapeProperties.INTERVAL, ShapeProperties.LOCATION, ShapeProperties.LOCATIONS,
            ShapeProperties.MAX_DISTANCE, ShapeProperties.MIN_DISTANCE, ShapeProperties.ORIENTATION,
            ShapeProperties.RANGE_COLORS, ShapeProperties.RANGE_DISTANCES, ShapeProperties.RANGE_LABELS,
            ShapeProperties.RANGE_TYPE, ShapeProperties.SAMPLE_DISTANCE, ShapeProperties.SHOW_EDGES,
            ShapeProperties.SHOW_ORIENTATION, ShapeProperties.SHOW_RANGE_VALUES, ShapeProperties.SWEEP,
            ShapeProperties.VISIBILITY);

    // Properties that require recalculating the range ring positions.
    private static final List<String> REQUIRE_RANGE_RING_GENERATION = List.of(
            ShapeProperties.INTERVAL, ShapeProperties.LOCATION,
            ShapeProperties.MAX_DISTANCE, ShapeProperties.MIN_DISTANCE, ShapeProperties.ORIENTATION,
            ShapeProperties.RANGE_DISTANCES, ShapeProperties.RANGE_TYPE, ShapeProperties.SAMPLE_DISTANCE,
            ShapeProperties.SWEEP);

    // How many points to use to estimate the circle with a polygon.
    private static final int NUM_POINTS_IN_CIRCLE = 72;

    @Inject
    private ITerrainService terrainService;

    @Inject
    private IExtendedGlobeManager globeManager;

    @Inject
    private RenderableManager renderableManager;

    private TerrainFollowingJob terrainFollowingJob; // Protected by synchronized(this)
    private final List<IProgressCallback> progressCallbacks = new CopyOnWriteArrayList<>();

    /**
     * Construct an editable surface shape.  This constructor is used when the user is manually drawing a shape, or we
     * are restoring a shape from persistence. In the latter case, the {@link #setMapObject(IMapObject)}
     * method must be called to restore the properties of the shape.
     */
    public GlobeRangeFanMapObject()
    {
        super(MapToolType.DRAWING_RANGE_FAN);
    }

    /**
     * Construct and initialize with the given shape.
     * This constructor is used when shapes are being created programmatically, such as with the ShapeService.
     * The editability of the shape will come from the mapObject setting.
     *
     * @param mapObject The source map object
     */
    public GlobeRangeFanMapObject(IMapObject mapObject)
    {
        super(mapObject);

        setMapObject(mapObject);
    }

    @Override
    @InvokeAndWaitOnEDT
    public void setMapObject(IMapObject mapObject)
    {
        logger.debug("Setting MO: {}", mapObject);

        super.setMapObject(mapObject);
        setShapeModel(getMapObject().getAssociatedShape());
    }

    /**
     * {@inheritDoc} Overridden to return an instance of {@code IShapeRangeFanMapObject}.
     */
    @Override
    public IShapeRangeFanMapObject getMapObject()
    {
        return (IShapeRangeFanMapObject) super.getMapObject();
    }

    @Override
    public void addProgressCallback(@Nonnull IProgressCallback progressCallback)
    {
        progressCallbacks.add(progressCallback);
    }

    @Override
    public void removeProgressCallback(@Nonnull IProgressCallback progressCallback)
    {
        progressCallbacks.remove(progressCallback);
    }

    /**
     * Fire a terrain calculation progress to all registered callbacks.
     */
    private void fireTerrainProgress(double progress)
    {
        progressCallbacks.forEach(pc -> pc.onProgress(progress));
    }

    /**
     * Fire a terrain calculation error to all registered callbacks.
     */
    private void fireTerrainError(Throwable e)
    {
        progressCallbacks.forEach(pc -> pc.onError(e));
    }

    @Override
    protected List<String> getShapePropertiesToWatch()
    {
        return PROPERTIES_TO_WATCH;
    }

    @Override
    public void propertyChange(PropertyChangeEvent evt)
    {
        if (REQUIRE_RANGE_RING_GENERATION.contains(evt.getPropertyName())) onFollowTerrainParamsChanged();
        super.propertyChange(evt);
    }

    private List<Double> generateRangeDistances()
    {
        IRangeFan rangeFan = requireShapeModel();
        double distance = rangeFan.getMaxDistance() - rangeFan.getMinDistance();
        int numValues = (int) ((distance) / rangeFan.getInterval());
        return numValues > 1
                ? IntStream.range(0, numValues).mapToObj(i -> rangeFan.getMinDistance() + i * (distance) / (numValues - 1)).toList()
                : List.of(distance);
    }

    private synchronized void onFollowTerrainParamsChanged()
    {
        cancelCalculation();
        IRangeFan rangeFan = requireShapeModel();
        if (rangeFan.getRangeType() != IRangeFan.FOLLOW_TERRAIN) return;
        List<Double> rangeDistances = rangeFan.getRangeDistances() != null ? rangeFan.getRangeDistances() : generateRangeDistances();
        JobData jobData = new JobData(terrainService.getHighestResolutionTerrain(), getLocation(), rangeDistances, rangeFan.getSampleDistance(), rangeFan.getOrientation(), rangeFan.getSweep());
        terrainFollowingJob = new TerrainFollowingJob(jobData);
        terrainFollowingJob.execute();
    }

    private synchronized void cancelCalculation()
    {
        if (terrainFollowingJob == null) return;
        terrainFollowingJob.cancel(true);
        terrainFollowingJob = null;
    }

    /**
     * Worker thread to compute terrain following range fan positions.
     * @since 5.5
     */
    private class TerrainFollowingJob extends ErrorReportingSwingWorker<List<List<ILocation>>, Object>
    {
        private final JobData data;

        /**
         * Create a task to build a range fan in the background.
         *
         * @param data data that describes the range fan to create.
         */
        public TerrainFollowingJob(JobData data)
        {
            super("", "");
            setUseDialog(false);
            this.data = data;
        }

        @Override
        protected List<List<ILocation>> doInBackground()
        {
            return calculateFollowTerrain();
        }

        @Override
        protected void onSuccess(List<List<ILocation>> result)
        {
            ((IMutableRangeFan) requireShapeModel()).setFollowTerrainRingLocations(result);
            globeManager.requestRedraw("Range fan changed");
        }

        @Override
        protected void onFailure()
        {
            elist.forEach(GlobeRangeFanMapObject.this::fireTerrainError);
        }

        /**
         * Calculate a list of calculated locations for each ring
         * @return a list of calculated locations for each ring
         */
        private List<List<ILocation>> calculateFollowTerrain()
        {
            if (isCancelled()) return null;
            fireTerrainProgress(0);
            double orientation = data.heading;
            double sweep = data.sweep;

            double portion = sweep / 360.0;
            int numLocations = 1 + (int) (NUM_POINTS_IN_CIRCLE * portion); // points to use in this portion
            double startAngleRadians = Math.toRadians(orientation - (sweep / 2.0));
            double da = (portion * (2 * Math.PI)) / (numLocations - 1); // delta angle through sweep radians

            List<List<ILocation>> result = new ArrayList<>();

            int totalLocations = data.rangeDistances.size() * numLocations;
            int processedLocations = 0;
            for (double rangeDistance : data.rangeDistances)
            {
                List<ILocation> locations = new ArrayList<>(numLocations);
                for (int i = 0; i < numLocations; i++)
                {
                    double azimuthRadians = startAngleRadians + (da * i);

                    if (isCancelled()) return null;
                    ILocation ll = greatCircleTerrainFollowingEndPosition(data.location, data.sampleDistance, azimuthRadians, rangeDistance, data.terrain);
                    locations.add(ll);
                    processedLocations++;
                    fireTerrainProgress((double) processedLocations / totalLocations);
                }
                result.add(locations);
            }
            fireTerrainProgress(1);
            return isCancelled() ? null : result;
        }

        /**
         * Computes the position on a great circle arc taking into account the length of a terrain following path.
         *
         * @param start          Starting position.
         * @param sampleDistance Sampling distance used in terrain following calculation, in meters.
         * @param azimuthRadians azimuth measured clockwise from North (in radians)
         * @param distance       Terrain following path distance, in meters.
         * @param terrain        Terrain to use for the calculation.
         * @return Location of the end position along a great circle arc in the direction of the azimuth.
         */
        private static ILocation greatCircleTerrainFollowingEndPosition(ILocation start, double sampleDistance, double azimuthRadians,
                                                                        double distance, IImmutableHighestResolutionTerrain terrain)
        {
            double d = 0;

            ILocation posA = new GeoPoint(start.getLatitude(), start.getLongitude(), 0);
            ILocation posB = null;

            while (d < distance)
            {
                Vec4 pA = terrain.getSurfacePoint(WWUtils.locationToPosition(posA));

                posB = new GeoPoint(GeoCalculations.pointAtDistance(posA, Math.toDegrees(azimuthRadians), sampleDistance));
                Vec4 pB = terrain.getSurfacePoint(WWUtils.locationToPosition(posB));

                d += pB.distanceTo3(pA);

                if (d < distance) posA = posB;
            }

            // Interpolate between the final two positions because the final step probably overshot the target distance.
            posB = new GeoPoint(GeoCalculations.pointAtDistance(posB, posA, (d - distance) / sampleDistance));

            return posB;
        }
    }

    private record JobData(IImmutableHighestResolutionTerrain terrain, ILocation location, List<Double> rangeDistances, double sampleDistance, double heading, double sweep)
    {
    }
}
