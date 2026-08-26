package gov.takx.dataservices.persist;

import gov.nasa.worldwind.geom.LatLon;
import gov.nasa.worldwind.geom.Sector;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.List;

/**
 * Persistent entity for surface polygonal structures.  These have no elevations
 * associated with the vertices.
 *
 * @since 3.0
 */
public abstract class ASurfacePolygonalStructure extends APolygonalStructure<LatLon>
{
    protected ASurfacePolygonalStructure()
    {
    }

    protected ASurfacePolygonalStructure(Sector sector, List<List<LatLon>> blockLocations)
    {
        super(sector, blockLocations);
    }

    @Override
    protected void writeVertex(LatLon location, DataOutputStream dos) throws IOException
    {
        dos.writeDouble(location.getLatitude().degrees);
        dos.writeDouble(location.getLongitude().degrees);
    }

    @Override
    protected LatLon readVertex(DataInputStream dis) throws IOException
    {
        return LatLon.fromDegrees(dis.readDouble(), dis.readDouble());
    }
}
