package gov.takx.core.view.gui.maps.terraindiscovery.controller.calculator;

import gov.nasa.worldwind.geom.Sector;
import gov.takx.api.layer.IRaptorLayer;
import gov.takx.core.view.gui.maps.terraindiscovery.controller.ITerrainDiscoveryController;
import gov.takx.core.view.gui.maps.terraindiscovery.controller.TerrainDiscoveryUtils;
import gov.takx.core.view.gui.maps.terraindiscovery.controller.render.ResolutionRenderable;
import gov.takx.core.view.gui.maps.terraindiscovery.controller.render.SurfaceMultiSector;
import gov.takx.core.view.gui.maps.terraindiscovery.model.ITerrainDiscoveryModel;
import gov.takx.platform.utils.ShapeUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.Color;
import java.lang.invoke.MethodHandles;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

/**
 */
public class TerrainDiscoveryCalculator
{
    private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    private final ITerrainDiscoveryController controller;
    private TerrainDiscoveryThreadPool threadPool;
    private Thread setupThread;
    private final AtomicReference<Sector> updatedSector;

    public TerrainDiscoveryCalculator(ITerrainDiscoveryController controller)
    {
        this.controller = controller;
        updatedSector = new AtomicReference<>(null);
    }

    public synchronized void calculate()
    {
        cancelWork();

        final ITerrainDiscoveryModel model = controller.getModel();

        // Fill the sector with a transparent black background
        final Sector sector = TerrainDiscoveryUtils.rectangleToSector(model.getSelectedGeofence());
        if (sector == null)
        {
            logger.warn("Unable to calculate - null Sector");
            return;
        }

        if (logger.isDebugEnabled())
        {
            logger.debug("Running full calculation");
        }

        controller.setProgressStarted();
        controller.clearVisualizations();

        controller.createAnalyticsLayers();

        model.getTerrainLayer().setSector(sector);

        final SurfaceMultiSector negative = new SurfaceMultiSector(sector, true);
        negative.setSurfaceColor(Color.BLACK);
        negative.setOpacity(0.3d);
        controller.getView().setPreparingEnabled(true);
        controller.getModel().setNegativeSpaceShape(negative);

        /**
         * Even though the tasks will be run in a thread pool, preparing these tasks in the first place can also be
         * time consuming.  Due to this, just offload the setup onto a different thread.
         *
         * This thread can be interrupted.  The more long-running methods and loops should contain interrupt checks.
         */
        setupThread = new Thread()
        {
            @Override
            public void run()
            {
                try
                {
                    List<TerrainDiscoveryTask> tasks = new ArrayList<>();

                    ITerrainDiscoveryModel model = controller.getModel();

                    // Update tile collections before doing anything else
                    for (IRaptorLayer elevationLayer : model.getElevationLayers())
                    {
                        model.getTileCollection(elevationLayer).update(sector);
                    }

                    // Synchronize tile collections' enabled flag with the elevation layer visibility
                    controller.updateTileCollectionsEnabled();

                    updatedSector.set(sector);

                    // TODO TAKX-2695: Re-enable elevation bounds creation.
                    // Create a task to update the elevation bounds contour lines
                    //tasks.add(new UpdateElevationBoundsTask(controller, ShapeUtils.RADIUS_OF_EARTH));

                    // Calculate the resolutions shape
                    tasks.add(new ResolutionRenderableTask(sector));

                    // Iterate through the selected elevation layers and add their availability to the surface sector
                    // shape
                    for (final IRaptorLayer elevationLayer : model.getElevationLayers())
                    {
                        // Allow interruptions.
                        if (Thread.currentThread().isInterrupted())
                        {
                            return;
                        }

                        Sector intersectedSector = sector.intersection(elevationLayer.getSector());
                        if (intersectedSector != null)
                        {
                            // Create tasks for coverage and image creation.
                            TerrainLookUpTasks terrainLookUp = new TerrainLookUpTasks(
                                    controller,
                                    elevationLayer,
                                    model.getTileCollection(elevationLayer),
                                    negative,
                                    sector,
                                    model.areImageCalculationsEnabled());

                            List<TerrainDiscoveryTask> lookupTasks = terrainLookUp.createTasks();

                            /**
                             * Return if the prep has been interrupted.
                             */
                            if (Thread.currentThread().isInterrupted() || lookupTasks == null)
                            {
                                return;
                            }

                            tasks.addAll(lookupTasks);
                        } else if (logger.isDebugEnabled())
                        {
                            logger.debug("Unable to calculate for layer {}.  No sector intersection", elevationLayer);
                        }
                    }

                    if (Thread.currentThread().isInterrupted())
                    {
                        return;
                    }

                    threadPool.submitTasks(tasks);
                } finally
                {
                    controller.getView().setPreparingEnabled(false);
                }
            }
        };
        setupThread.start();
    }

    /**
     * This is a calculation method intended to quickly calculate only the resolution boundaries and the resolution
     * shape.  This represents a subset of the tasks that are done in the full calculation, but should be far less
     * time consuming.  This method should be called when toggling the visibility of elevation models.
     */
    public synchronized void refreshResolutions()
    {
        Sector updated = updatedSector.get();
        final ITerrainDiscoveryModel model = controller.getModel();
        final Sector sector = TerrainDiscoveryUtils.rectangleToSector(model.getSelectedGeofence());

        // Do not calculate resolution if a full calculation is not yet completed.
        if (sector == null || updated == null || !updated.equals(sector))
        {
            return;
        }

        if (logger.isDebugEnabled())
        {
            logger.debug("Running resolution calculation");
        }

        cancelWork();
        controller.setProgressStarted();

        model.getTerrainLayer().setSector(sector);
        controller.getView().setPreparingEnabled(true);
        setupThread = new Thread()
        {
            @Override
            public void run()
            {
                try
                {
                    List<TerrainDiscoveryTask> tasks = new ArrayList<>();

                    // TODO TAKX-2695: Re-enable elevation bounds creation.
                    // Create a task to update the elevation bounds contour lines
                    //tasks.add(new UpdateElevationBoundsTask(controller, ShapeUtils.RADIUS_OF_EARTH));

                    // Calculate the resolutions shape
                    tasks.add(new ResolutionRenderableTask(sector));

                    if (Thread.currentThread().isInterrupted())
                    {
                        return;
                    }

                    threadPool.submitTasks(tasks);
                } finally
                {
                    controller.getView().setPreparingEnabled(false);
                }
            }
        };
        setupThread.start();
    }

    public synchronized void initialize()
    {
        if (threadPool != null)
        {
            threadPool.dispose();
        }
        threadPool = new TerrainDiscoveryThreadPool(controller);
    }

    public synchronized void reset()
    {
        if (threadPool != null)
        {
            threadPool.dispose();
        }
        threadPool = null;
    }

    public synchronized void cancelWork()
    {
        if (threadPool != null)
        {
            threadPool.clear();
        }
        if (setupThread != null)
        {
            setupThread.interrupt();
            setupThread = null;
        }
    }

    public synchronized void clearSector()
    {
        // Clear the 'updated tile collections' sector
        updatedSector.set(null);
    }

    private class ResolutionRenderableTask extends TerrainDiscoveryTask
    {
        private final Sector sector;

        public ResolutionRenderableTask(Sector sector)
        {
            this.sector = sector;
        }

        @Override
        public String getDisplayName()
        {
            return "Calculating resolution...";
        }

        @Override
        protected void run() throws Exception
        {
            ResolutionRenderable resolutionShape = new ResolutionRenderable(sector, ShapeUtils.RADIUS_OF_EARTH);
            resolutionShape.update(controller);
            controller.getModel().setResolutionShape(resolutionShape);
        }
    }
}
