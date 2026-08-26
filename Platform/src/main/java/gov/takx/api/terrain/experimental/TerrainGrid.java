package gov.takx.api.terrain.experimental;

import gov.nasa.worldwind.geom.Angle;
import gov.nasa.worldwind.geom.LatLon;
import gov.nasa.worldwind.geom.Position;
import gov.nasa.worldwind.geom.Sector;
import gov.nasa.worldwind.globes.Globe;
import gov.nasa.worldwind.util.measure.LengthMeasurer;
import gov.takx.api.annotation.Experimental;
import gov.takx.api.coordinatesystem.ILocation;
import gov.takx.api.coordinatesystem.RaptorLocation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.invoke.MethodHandles;
import java.util.ArrayList;

/**
 */
@Experimental
public class TerrainGrid
{
    private static Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());
    private final Globe globe;
    private final double globeRadius;
    private final double minLatitude;
    private final double maxLatitude;
    private final double minLongitude;
    private final double maxLongitude;
    private final double xWidth;
    private final double yWidth;
    private final int numberOfGridPointsX;
    private final int numberOfGridPointsY;
    private final Sector sector;
    private final double stepSizeInMeters;

    public TerrainGrid(Globe globe, ILocation locationA, ILocation locationB, Double stepSize)
    {
        this(globe,
                Sector.boundingSector(
                        LatLon.fromDegrees(locationA.getLatitude(), locationA.getLongitude()),
                        LatLon.fromDegrees(locationB.getLatitude(), locationB.getLongitude())),
                stepSize);
    }

    public TerrainGrid(Globe globe,
                       Sector sector,
                       Double approxStepSizeInMeters)
    {
        this.globe = globe;
        globeRadius = globe.getRadius();

        this.sector = sector;

        minLatitude = sector.getMinLatitude().degrees;
        minLongitude = sector.getMinLongitude().degrees;
        maxLatitude = sector.getMaxLatitude().degrees;
        maxLongitude = sector.getMaxLongitude().degrees;

        double widthOfGrid = getWidthOfGrid();
        double heightOfGrid = getHeightOfGrid();

        if (approxStepSizeInMeters == null)
        {
            approxStepSizeInMeters = Math.sqrt((heightOfGrid * widthOfGrid) / 2);
        }

        numberOfGridPointsX = Math.round((float) ((widthOfGrid * 1000) / approxStepSizeInMeters)) + 1;
        this.stepSizeInMeters = (widthOfGrid * 1000) / (numberOfGridPointsX - 1);
        numberOfGridPointsY = Math.round((float) ((heightOfGrid * 1000) / this.stepSizeInMeters)) + 1;

        xWidth = getWidthOfGrid();
        yWidth = getHeightOfGrid();

        if (logger.isTraceEnabled())
        {
            logger.trace("widthOfGrid km= {}", xWidth);
            logger.trace("heightOfGrid km= {}", yWidth);
            logger.trace("original stepSizeInMeters = {}", approxStepSizeInMeters);
            logger.trace("approxStepSizeInMetersW = {}", this.stepSizeInMeters);
            logger.trace("approxStepSizeInMetersH = {}", yWidth * 1000 / (numberOfGridPointsY - 1));
            logger.trace("numberOfGridPointsX = {}", numberOfGridPointsX);
            logger.trace("numberOfGridPointsY = {}", numberOfGridPointsY);
        }
    }

    public double getStepSizeInMeters()
    {
        return stepSizeInMeters;
    }

    public int getNumberOfGridPointsX()
    {
        return numberOfGridPointsX;
    }

    public int getNumberOfGridPointsY()
    {
        return numberOfGridPointsY;
    }

    public double getGlobeRadius()
    {
        return globeRadius;
    }

    public double getHeightOfGrid()
    {
        //Determine the height of the region of interest
        ArrayList<Position> positions = new ArrayList<>();
        positions.add(Position.fromDegrees(minLatitude, minLongitude, 0));
        positions.add(Position.fromDegrees(maxLatitude, minLongitude, 0));
        LengthMeasurer lengthMeasurer = new LengthMeasurer(positions);
        return lengthMeasurer.getLength(globe) / 1000;
    }

    public double getWidthOfGrid()
    {
        //Determine the width of the region of interest
        ArrayList<Position> positions = new ArrayList<>();
        positions.add(Position.fromDegrees(minLatitude, minLongitude, 0));
        positions.add(Position.fromDegrees(minLatitude, maxLongitude, 0));
        LengthMeasurer lengthMeasurer = new LengthMeasurer(positions);
        return lengthMeasurer.getLength(globe) / 1000;
    }

    public double getLonFromX(double x)
    {
        //multiply the sector width by the ratio of where x is on the xy grid
        double xRatio = x / xWidth;
        Angle lonFromMinLon = sector.getDeltaLon().multiply(xRatio);
        Angle lon = Angle.fromDegrees(minLongitude).add(lonFromMinLon);

        return lon.getDegrees();
    }

    public double getLatFromY(double y)
    {

        //multiply the sector height by the ratio of where y is on the xy grid
        double yRatio = (y / yWidth);
        Angle latFromMinLat = sector.getDeltaLat().multiply(yRatio);
        Angle lat = Angle.fromDegrees(minLatitude).add(latFromMinLat);
        return lat.getDegrees();
    }

    public ILocation getMinLocation()
    {
        return new RaptorLocation(minLatitude, minLongitude);
    }

    public ILocation getMaxLocation()
    {
        return new RaptorLocation(maxLatitude, maxLongitude);
    }

    public Sector getSector()
    {
        return sector;
    }

    @Override
    public String toString()
    {
        return "GridData{" +
                ", stepSizeInMeters=" + stepSizeInMeters +
                ", numberOfGridPointsY=" + numberOfGridPointsY +
                ", numberOfGridPointsX=" + numberOfGridPointsX +
                ", yWidth=" + yWidth +
                ", xWidth=" + xWidth +
                ", minLatitude=" + minLatitude +
                ", maxLatitude=" + maxLatitude +
                ", minLongitude=" + minLongitude +
                ", maxLongitude=" + maxLongitude +
                '}';
    }
}
