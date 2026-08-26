package gov.takx.core.globe.render;

import gov.nasa.worldwind.avlist.AVKey;
import gov.nasa.worldwind.geom.Sector;
import gov.nasa.worldwind.render.DrawContext;
import gov.nasa.worldwind.render.Material;
import gov.takx.api.globe.IApplicationGlobe;
import gov.takx.api.messages.IRaptorDataMessage;
import gov.takx.core.mapdata.MapPointGroup;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.invoke.MethodHandles;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * This class handles the rendering of track points when "Top Down" mode is enabled.  It uses a set of quad trees to
 * organize the track points along geographic regions to allow for quick summary generation (grouping).
 * <p/>
 * Very high-level grouped elements are rendered as PointPlacemark objects, normal groups are rendered as special
 * markers that have a slightly larger size, and actual track points are rendered as Markers.
 *
 * @since 1.13
 */
public class TopDownRenderer extends AStyledRenderingEngine<TopDownSceneGenerator>
{
    private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());
    private static final Logger renderingDebug = LoggerFactory.getLogger(AStyledRenderingEngine.RENDERING_DEBUG_NAMESPACE + MethodHandles.lookup().lookupClass().getSimpleName());

    private Sector boundingSector;
    private final AtomicBoolean viewChanged = new AtomicBoolean(true);

    /**
     * Construct the rendering engine on the given WW instance.
     *
     * @param globe Globe we are rendering for
     * @param renderingContext The rendering context we are working within
     */
    public TopDownRenderer(IApplicationGlobe globe, RenderingContext renderingContext)
    {
        super(globe, renderingContext);

        markerRenderer.setForceSnapToSurface(true);  // We are top down - no elevation on markers
    }

    @PostConstruct
    private void postConstruct()
    {
        initialize(); // initialize now that we have been fully constructed

        // install a view listener so we can minimize the cost of bounding sector calculations

        raptorGlobe.getWorldWindow().getSceneController().addPropertyChangeListener(AVKey.VIEW, pce -> viewChanged.set(true));
    }

    @Override
    public boolean supportsElevation()
    {
        return false;
    }

    @Override
    protected TopDownSceneGenerator createSceneGeneratorIfNeeded(int frame, DrawContext dc)
    {
        generateBoundingSectors(dc);

        // In some cases, we can end up with no bounding sector.  Nothing we can do, so just bail.

        if (boundingSector == null)
        {
            if (logger.isDebugEnabled()) logger.debug("BoundingSector NULL, bailing!");
            return null;
        }

        // If we have a geometry, we always create a new scene

        return new TopDownSceneGenerator(frame, boundingSector, renderingGoal, renderingContext, this, mapObjectVisible);
    }

    @Override
    protected void installScene(TopDownSceneGenerator scene)
    {
        boundingSector = scene.getBoundingSector(); // Install the bounding sector of the scene we calculated

        // Create the renderables for the various sectors we render as debug hints if INFO logging is enabled

        if (renderingDebug.isInfoEnabled())
        {
            addSectorRenderables(Collections.singleton(scene.getBoundingSector()), Material.RED);
            addSectorRenderables(scene.getCellSectors(), Material.CYAN);
            addSectorRenderables(scene.getDetailTreeSectors(), Material.YELLOW);
        }

        // Add in top-quadrant cluster markers
        for (TopDownSceneGenerator.ClusterData clusterData : scene.getTopQuadrantItemList())
        {
            renderables.add(new ClusterMarker(clusterData));
        }

        // Add in group markers
        for (Map.Entry<Integer, List<MapPointGroup>> entry : scene.getGroupMarkerLists().entrySet())
        {
            final int moId = entry.getKey();
            final MapObjectMarkerList markerList = markerLists.get(moId);

            entry.getValue().forEach(markerList::addMapPoint);
        }

        // Create MapPoints for each of the track points determined to be in the scene
        addTrackPointsToScene(scene.getTrackPointLists().values());
    }

    @Override
    protected boolean isPointInScene(IRaptorDataMessage rdm)
    {
        // NOTE: In rare cases, when we change rendering engines while live data is coming in, we can get a message
        // *before* we ever get a rendering cycle (which means we have no surface geometry yet).

        return boundingSector != null && boundingSector.containsDegrees(rdm.getLatitude(), rdm.getLongitude());
    }

    /**
     * Generate the bounding sector for the current viewport.  We try to make this as tight as possible since this
     * bounds the set of objects rendered.
     *
     * @param dc draw context
     */
    private void generateBoundingSectors(DrawContext dc)
    {
        // If we have a computed bounding sector and our view hasn't changed, then we can bail early

        if (boundingSector != null && !viewChanged.compareAndSet(true, false)) return;

        // We used to calculate the bounding sector here, but we already do this in ApplicationOrbitView
        // and store it in the DrawContext so use that.
        boundingSector = dc.getVisibleSector();

        if (logger.isTraceEnabled()) logger.trace("BS: {}", boundingSector);
    }

    /**
     * Calculate the minimum value from a set of inputs.
     *
     * @param numbers Numbers to compare
     * @return minimum value from {@code numbers}
     */
    private double min(double... numbers)
    {
        double min = numbers[0];
        for (int i = 1; i < numbers.length; i++)
        {
            if (numbers[i] < min) min = numbers[i];
        }

        return min;
    }

    /**
     * Calculate the maximum value from a set of inputs.
     *
     * @param numbers Numbers to compare
     * @return maximum value from {@code numbers}
     */
    private double max(double... numbers)
    {
        double max = numbers[0];
        for (int i = 1; i < numbers.length; i++)
        {
            if (numbers[i] > max) max = numbers[i];
        }

        return max;
    }
}
