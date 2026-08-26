package gov.takx.core.globe.render;

import gov.nasa.worldwind.geom.Sector;
import gov.nasa.worldwind.render.DrawContext;
import gov.nasa.worldwind.render.Material;
import gov.nasa.worldwind.terrain.SectorGeometry;
import gov.nasa.worldwind.terrain.SectorGeometryList;
import gov.takx.api.globe.IApplicationGlobe;
import gov.takx.api.messages.IRaptorDataMessage;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.invoke.MethodHandles;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * This is the rendering engine for "Spatial" rendering.  It can render regions at any pitch angle, but it has
 * no grouping logic.  It will show as many points as possible (up to the rendering limit) and then it will
 * cull points from the scene to avoid exceeding that limit.
 *
 * @see RenderingModel
 * @since 1.14
 */
public class SpatialRenderer extends AStyledRenderingEngine<SpatialSceneGenerator>
{
    private static final Logger renderingDebug = LoggerFactory.getLogger(AStyledRenderingEngine.RENDERING_DEBUG_NAMESPACE + MethodHandles.lookup().lookupClass().getSimpleName());

    private SectorGeometryList geometryList;
    private final Set<Sector> glSectors = new HashSet<>();
    private boolean discardSavedScene;

    /**
     * Construct the rendering engine on the given WW instance.
     *
     * @param globe Globe we are rendering for
     * @param renderingContext The rendering context we are working within
     */
    public SpatialRenderer(IApplicationGlobe globe, RenderingContext renderingContext)
    {
        super(globe, renderingContext);
    }

    @PostConstruct
    private void postConstruct()
    {
        initialize(); // initialize now that we have been fully constructed
    }

    @Override
    public boolean supportsElevation()
    {
        return true;
    }

    @Override
    protected SpatialSceneGenerator createSceneGeneratorIfNeeded(int frame, DrawContext dc)
    {
        // Must make a copy of the geometry list since getSurfaceGeometry returns a reference to an internal collection
        final SectorGeometryList sectorGeometries = new SectorGeometryList(dc.getSurfaceGeometry());
        final Set<Sector> newGlSectors = sectorGeometries.stream()
                .map(SectorGeometry::getSector)
                .collect(Collectors.toSet());

        // Determine if we need to calculate a new scene.  If any of the following are true, then we need a new scene:
        //    1. We have been told to discard the saved scene
        //    2. We have no saved scene
        //    3. The scene geometry has changed

        final boolean needNewScene = discardSavedScene
                || geometryList == null // No saved scene
                || geometryList.size() != sectorGeometries.size() || !glSectors.containsAll(newGlSectors);

        if (needNewScene)
        {
            discardSavedScene = false;
            geometryList = sectorGeometries;
            glSectors.clear();
            glSectors.addAll(newGlSectors);

            return new SpatialSceneGenerator(frame, sectorGeometries, renderingContext, renderingGoal, this, mapObjectVisible);
        } else
        {
            return null;
        }
    }

    @Override
    protected void installScene(SpatialSceneGenerator scene)
    {
        discardSavedScene = false;

        // Draw the sector outlines if enabled
        if (renderingDebug.isInfoEnabled())
        {
            final List<Sector> sectors = geometryList.stream()
                    .map(SectorGeometry::getSector)
                    .collect(Collectors.toList());
            addSectorRenderables(sectors, Material.GREEN);

            addSectorRenderables(scene.getDetailTreeSectors(), Material.ORANGE);
        }

        // Now install all the track points determined to be in the scene
        addTrackPointsToScene(scene.getTrackPointsToRender().values());
    }

    @Override
    protected boolean isPointInScene(IRaptorDataMessage rdm)
    {
        // NOTE: In rare cases, when we change rendering engines while live data is coming in, we can get a message
        // *before* we ever get a rendering cycle (which means we have no surface geometry yet).
        if (geometryList == null) return false;

        // If any of the current surface geometries contain the point, then it's in the scene
        final double latitude = rdm.getLatitude();
        final double longitude = rdm.getLongitude();

        return glSectors.stream().anyMatch(s -> s.containsDegrees(latitude, longitude));
    }

    @Override
    protected void discardCurrentScene()
    {
        discardSavedScene = true;
    }
}
