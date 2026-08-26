package gov.takx.core.terrain.experimental;

import gov.nasa.worldwind.geom.Sector;
import gov.nasa.worldwind.globes.ElevationModel;
import gov.nasa.worldwind.globes.Globe;
import gov.nasa.worldwind.terrain.Terrain;
import gov.takx.api.annotation.Headless;
import gov.takx.api.event.StartProjectServices;
import gov.takx.api.event.StopProjectServices;
import gov.takx.api.event.project.ProjectSaving;
import gov.takx.api.layer.IRaptorLayer;
import gov.takx.api.layer.IRaptorLayerManager;
import gov.takx.api.layer.IRaptorLayerManagerListener;
import gov.takx.api.layer.LayerGroup;
import gov.takx.api.structures.IIntersectableShape;
import gov.takx.api.structures.ISurfaceStructure;
import gov.takx.api.structures.ISurfaceStructureService;
import gov.takx.api.terrain.experimental.IHighestResolutionTerrain;
import gov.takx.api.terrain.experimental.ITakElevationModel;
import gov.takx.api.terrain.experimental.ITerrainCalculator;
import gov.takx.api.terrain.experimental.ITerrainCalculatorDelegate;
import gov.takx.api.terrain.experimental.ITerrainService;
import gov.takx.api.terrain.experimental.TerrainCalculator;
import gov.takx.api.terrain.experimental.TerrainUtils;
import gov.takx.core.globe.ApplicationGlobe;
import gov.takx.core.globe.ApplicationGlobeManager;
import gov.takx.core.globe.tak.elevation.TakHighestResolutionTerrain;
import gov.takx.core.project.ServiceOrdering;
import gov.takx.platform.preferences.ProjectPreferenceEntry;
import jakarta.annotation.Priority;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import jakarta.inject.Inject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.lang.invoke.MethodHandles;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

/**
 * Gives access to the core's highest resolution terrain model configured by the user and terrain calculators.
 * Terrain calculators allow occlusion and elevation calculations to be done on a specific {@link Terrain} model.
 * This allows for optimized calculations based on the needs (speed vs accuracy).  The globe terrain model
 * can be used for speed and the {@link IHighestResolutionTerrain} can be used for accuracy.
 *
 * @since 1.35
 */
@ApplicationScoped
public class TerrainService implements ITerrainService, ITerrainCalculatorDelegate, IRaptorLayerManagerListener, PropertyChangeListener
{
    private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    private final ISurfaceStructureService surfaceStructureService;
    private final IRaptorLayerManager layerManager;
    private final ApplicationGlobeManager globeManager;
    private final boolean headless;

    private TakHighestResolutionTerrain coreHighestResolutionTerrain;

    @Inject
    TerrainService(ISurfaceStructureService surfaceStructureService, IRaptorLayerManager layerManager, ApplicationGlobeManager globeManager, @Headless boolean headless)
    {
        this.surfaceStructureService = surfaceStructureService;
        this.layerManager = layerManager;
        this.globeManager = globeManager;
        this.headless = headless;
    }

    private void startService(@Observes @Priority(ServiceOrdering.START_GenericCoreService) StartProjectServices ignored)
    {
        // Creating a new project can leave a stale coreHighestResolutionTerrain, so make sure this is refreshed
        coreHighestResolutionTerrain = null;
        restore();
        updateElevationModelsOfInterest();
    }

    private void stopService(@Observes @Priority(ServiceOrdering.STOP_GenericCoreService) StopProjectServices ignored)
    {
        coreHighestResolutionTerrain = null;
    }

    /**
     * When the project is saved, save our elevation model list.
     */
    private void handleProjectSaving(@SuppressWarnings("UnusedParameters") @Observes ProjectSaving event)
    {
        store();
    }

    @Override
    public TakHighestResolutionTerrain getHighestResolutionTerrain()
    {
        if (coreHighestResolutionTerrain == null)
        {
            ApplicationGlobe appGlobe = globeManager.getPrimaryGlobeDelegate();
            final Globe globe = appGlobe.getWorldWindow().getModel().getGlobe();
            coreHighestResolutionTerrain = new TakHighestResolutionTerrain(globe, headless ? null : appGlobe);
        }
        return coreHighestResolutionTerrain;
    }

    @Override
    public ITerrainCalculator getTerrainCalculator(Terrain terrain)
    {
        return new TerrainCalculator(terrain, this);
    }

    @Override
    public Collection<IIntersectableShape> getShapes()
    {
        return surfaceStructureService.getShapes();
    }

    @Override
    public Collection<ISurfaceStructure> getSurfaceStructures(Sector sector)
    {
        if (sector != null)
        {
            return surfaceStructureService.getStructures(sector);
        }
        return Collections.emptyList();
    }

    @Override
    public void raptorLayerAdded(IRaptorLayer raptorLayer)
    {
        if (raptorLayer.getLayerGroup() != LayerGroup.ELEVATION) return;
        startListeningToLayer(raptorLayer);
        updateElevationModelsOfInterest();
    }

    @Override
    public void raptorLayerRemoved(IRaptorLayer raptorLayer)
    {
        if (raptorLayer.getLayerGroup() != LayerGroup.ELEVATION) return;
        stopListeningToLayer(raptorLayer);
        updateElevationModelsOfInterest();
    }

    @Override
    public void raptorLayersRemoved()
    {
        // Can't remove layer listener here unless we track each of removed layers.  The good news is that
        // before removing any/all layer(s), RaptorLayerManager flushes their listeners.
        updateElevationModelsOfInterest();
    }

    @Override
    public void propertyChange(PropertyChangeEvent evt)
    {
        // Interested in model VISIBILITY changes.  This occurs when an elevation model's checkbox is clicked on the
        // Layer Manager.
        if (evt.getPropertyName().equals(IRaptorLayer.VISIBLE))
        {
            updateElevationModelsOfInterest();
        }
    }

    public void restore()
    {
        layerManager.getLayers().stream()
                .filter(raptorLayer -> raptorLayer.getLayerGroup() == LayerGroup.ELEVATION)
                .forEach(this::startListeningToLayer);

        updateElevationModelsOfInterest();
    }

    private void store()
    {
        if (coreHighestResolutionTerrain != null)
        {
            List<ITakElevationModel> models = coreHighestResolutionTerrain.getElevationModelsOfInterest();

            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < models.size(); i++)
            {
                ITakElevationModel model = models.get(i);
                sb.append(model.getName().replace(",", ""));
                if (i < (models.size() - 1))
                {
                    sb.append(",");
                }
            }

            ProjectPreferenceEntry.SELECTED_ELEVATION_MODELS.put(sb.toString());

            if (logger.isDebugEnabled())
            {
                logger.debug("Stored the following elevation models in project preferences: {}", ProjectPreferenceEntry.SELECTED_ELEVATION_MODELS.getStringValue());
            }
        }
    }

    private void updateElevationModelsOfInterest()
    {
        if (headless) return;

        ElevationModel elevationModel = globeManager.getPrimaryGlobeDelegate().getWorldWindow().getModel().getGlobe().getElevationModel();
        List<ITakElevationModel> enabledModels = TerrainUtils.splitModels(ITakElevationModel.class, elevationModel).stream().filter(ElevationModel::isEnabled).toList();
        getHighestResolutionTerrain().setElevationModelsOfInterest(enabledModels);
    }

    private void startListeningToLayer(IRaptorLayer raptorLayer)
    {
        // Why call stopListeningToLayer() first?  To prevent this object from existing in the property change support
        // in multiple positions.
        stopListeningToLayer(raptorLayer);

        // Actually start listening.
        raptorLayer.addPropertyChangeListener(this);
    }

    private void stopListeningToLayer(IRaptorLayer raptorLayer)
    {
        raptorLayer.removePropertyChangeListener(this);
    }
}
