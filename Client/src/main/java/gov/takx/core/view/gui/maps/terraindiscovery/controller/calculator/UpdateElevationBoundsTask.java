package gov.takx.core.view.gui.maps.terraindiscovery.controller.calculator;

import gov.nasa.worldwind.geom.Position;
import gov.nasa.worldwind.geom.Sector;
import gov.nasa.worldwind.globes.Globe;
import gov.nasa.worldwind.layers.RenderableLayer;
import gov.nasa.worldwind.util.measure.LengthMeasurer;
import gov.takx.api.annotation.PseudoBean;
import gov.takx.api.globe.IGlobeManager;
import gov.takx.api.globe.IWorldWindowGL;
import gov.takx.api.terrain.experimental.ElevationContourLines;
import gov.takx.api.terrain.experimental.ITakElevationModel;
import gov.takx.api.terrain.experimental.TerrainGrid;
import gov.takx.core.globe.tak.elevation.ElevationLayerManager;
import gov.takx.core.view.gui.maps.terraindiscovery.controller.ITerrainDiscoveryController;
import gov.takx.core.view.gui.maps.terraindiscovery.controller.TerrainDiscoveryUtils;
import jakarta.inject.Inject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.Color;
import java.lang.invoke.MethodHandles;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.StreamSupport;

/**
 */
@SuppressWarnings("CdiManagedBeanInconsistencyInspection")
@PseudoBean
public class UpdateElevationBoundsTask extends TerrainDiscoveryTask
{
    private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    @Inject
    private IGlobeManager globeManager;

    @Inject
    private ElevationLayerManager elevationLayerManager;

    private final ITerrainDiscoveryController controller;
    private final double earthRadius;

    public UpdateElevationBoundsTask(ITerrainDiscoveryController controller, double earthRadius)
    {
        this.controller = controller;
        this.earthRadius = earthRadius;
    }

    @Override
    protected void run() throws Exception
    {
        RenderableLayer renderableBoundsLayer = controller.getModel().getElevationBoundsLayer();

        // Create the elevation bounds visualization
        ElevationContourLines contourLines = createElevationContourLines();
        renderableBoundsLayer.removeAllRenderables();
        renderableBoundsLayer.addRenderable(contourLines);
    }

    @Override
    public String getDisplayName()
    {
        return "Calculating terrain bounds";
    }

    private ElevationContourLines createElevationContourLines()
    {
        Sector sector = controller.getBoundingSector();

        final IWorldWindowGL wwd = globeManager.getPrimaryGlobeDelegate().getWorldWindow();
        Globe globe = wwd.getModel().getGlobe();
        double minLatitude = sector.getMinLatitude().degrees;
        double minLongitude = sector.getMinLongitude().degrees;
        double maxLatitude = sector.getMaxLatitude().degrees;
        double maxLongitude = sector.getMaxLongitude().degrees;

        //Determine the height of the region of interest
        ArrayList<Position> positions = new ArrayList<>();
        positions.add(Position.fromDegrees(minLatitude, minLongitude, 0));
        positions.add(Position.fromDegrees(maxLatitude, minLongitude, 0));
        LengthMeasurer lengthMeasurer = new LengthMeasurer(positions);
        double height = lengthMeasurer.getLength(globe);

        //Determine the width of the region of interest
        positions = new ArrayList<>();
        positions.add(Position.fromDegrees(minLatitude, minLongitude, 0));
        positions.add(Position.fromDegrees(minLatitude, maxLongitude, 0));
        lengthMeasurer = new LengthMeasurer(positions);
        double width = lengthMeasurer.getLength(globe);

        double stepSize = Math.sqrt((height * width) / 20000);

        TerrainGrid terrainGrid = TerrainDiscoveryUtils.makeTerrainGrid(
                wwd,
                controller.getModel().getSelectedGeofence(),
                stepSize);

        if (logger.isDebugEnabled())
        {
            logger.debug("Total number of points in bounds terrain grid: {}", terrainGrid.getNumberOfGridPointsX() * terrainGrid.getNumberOfGridPointsY());
        }

        List<ITakElevationModel> models = StreamSupport.stream(controller.getModel().getVisibleElevationLayers().spliterator(), true)
                .map(m -> elevationLayerManager.getElevationModel(m))
                .filter(Objects::nonNull)
                .toList();

        double[][] dataMatrix = TerrainDiscoveryUtils.getResolutionsMatrix(
                terrainGrid,
                models,
                minLatitude,
                minLongitude,
                maxLatitude,
                maxLongitude);

        // Create the contour lines object
        double[] resolutions = TerrainDiscoveryUtils.matrixToSortedArray(dataMatrix);
        List<Color> resolutionColors = new ArrayList<>();
        for (double resolution : resolutions)
        {
            resolutionColors.add(controller.getColorForResolution(resolution));
        }

        // Apply earth's radius to the resolutions AFTER the colors have been generated
        for (int i = 0; i < resolutions.length; i++)
        {
            resolutions[i] = resolutions[i] * earthRadius;
        }

        for (double[] row : dataMatrix)
        {
            for (int i = 0; i < row.length; i++)
            {
                row[i] = row[i] * earthRadius;
            }
        }

        return new ElevationContourLines(terrainGrid, dataMatrix, resolutions, resolutionColors);
    }
}
