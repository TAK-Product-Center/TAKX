package gov.takx.core.mapdata;

import gov.nasa.worldwind.geom.Angle;
import gov.nasa.worldwind.geom.Position;

/**
 * This class is a Position and additionally holds a time (the time at which something was at that position).
 * It is used primarily for holding the vertices of connecting lines.  The time is required to properly order
 * the points along the connecting line.
 * <p/>
 * This is  DTO object, in that it is created directly from a database query.  There is no "table" that holds
 * these objects, they are extracted from the RDM table as needed.
 *
 */
public class MapPosition extends Position
{
    private final long time;

    public MapPosition(double lat, double lon, double elevation, long time)
    {
        super(Angle.fromDegrees(lat), Angle.fromDegreesLongitude(lon), elevation);
        this.time = time;
    }

    public long getTime()
    {
        return time;
    }
}
