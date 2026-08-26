package gov.takx.core.globe.utils;

import gov.nasa.worldwind.WorldWind;
import gov.nasa.worldwind.geom.LatLon;
import gov.nasa.worldwind.globes.Globe;
import gov.nasa.worldwind.util.measure.LengthMeasurer;
import gov.takx.api.annotation.PseudoBean;
import gov.takx.api.globe.IGlobeManager;
import gov.takx.platform.utils.ILengthMeasurer;
import jakarta.inject.Inject;

import java.util.ArrayList;
import java.util.List;

/**
 */
@PseudoBean
public class GlobeRuler extends LengthMeasurer implements ILengthMeasurer
{
    @Inject
    private IGlobeManager globeManager;

    private List<? extends LatLon> positions = null;
    private MeasurementPathType pathType = ILengthMeasurer.MeasurementPathType.Linear;

    @Override
    public void setMeasurementPositions(List<? extends LatLon> positions, double elevation)
    {
        this.positions = positions;

        setPositions(new ArrayList<>(positions), elevation);
    }

    @Override
    public List<? extends LatLon> getMeasurementPositions()
    {
        return positions;
    }

    @Override
    public MeasurementPathType getMeasurementPathType()
    {
        return pathType;
    }

    @Override
    public void setMeasurementPathType(MeasurementPathType pathType)
    {
        this.pathType = pathType;

        switch (pathType)
        {
            case GreatCircle -> setPathType(WorldWind.GREAT_CIRCLE);
            case Linear -> setPathType(WorldWind.LINEAR);
            case RhumbLine -> setPathType(WorldWind.RHUMB_LINE);
        }
    }

    @Override
    public double computeLength()
    {
        final Globe globe = globeManager.getPrimaryGlobeDelegate().getWorldWindow().getModel().getGlobe();
        return computeLength(globe, isFollowTerrain());
    }
}
