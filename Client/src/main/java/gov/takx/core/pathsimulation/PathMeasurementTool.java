package gov.takx.core.pathsimulation;

import gov.nasa.worldwind.geom.LatLon;
import gov.nasa.worldwind.geom.Position;
import gov.nasa.worldwind.globes.Globe;
import gov.takx.api.annotation.PseudoBean;
import gov.takx.api.coordinatesystem.ILocation;
import gov.takx.api.coordinatesystem.RaptorLocation;
import gov.takx.api.globe.IGlobeManager;
import gov.takx.core.globe.utils.GlobeRuler;
import jakarta.inject.Inject;

import java.util.ArrayList;
import java.util.List;

/**
 */
@SuppressWarnings("CdiManagedBeanInconsistencyInspection")
@PseudoBean
public class PathMeasurementTool extends GlobeRuler
{
    @Inject
    private IGlobeManager globeManager;

    public PathMeasurementTool(List<? extends LatLon> positions)
    {
        setMeasurementPositions(positions, 0);
    }

    public List<ILocation> getPathLocations()
    {
        List<ILocation> locations = new ArrayList<>();

        Globe globeComponent = globeManager.getPrimaryGlobeDelegate().getWorldWindow().getModel().getGlobe();
        ArrayList<? extends Position> subdividedPositions = subdividePositions(globeComponent, getPositions(), getMaxSegmentLength(), true, getAVKeyPathType());
        for (Position subdividedPosition : subdividedPositions)
        {
            locations.add(new RaptorLocation(subdividedPosition.getLatitude().getDegrees(), subdividedPosition.getLongitude().getDegrees(), subdividedPosition.getElevation()));
        }
        return locations;
    }
}
