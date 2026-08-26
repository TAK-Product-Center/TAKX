package gov.takx.dataservices.persist;

import gov.nasa.worldwind.geom.Position;
import gov.nasa.worldwind.geom.Sector;
import jakarta.annotation.Nonnull;
import jakarta.persistence.Entity;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.List;

/**
 * Persistent entity for extruded polygonal structures.  Each vertex includes an elevation in addition to
 * its lat/lon.
 *
 * @since 3.0
 */
@Entity
public abstract class AExtrudedPolygonalStructure extends APolygonalStructure<Position>
{
    protected int altitudeMode;
    protected boolean isExtrude;

    protected AExtrudedPolygonalStructure()
    {
    }

    protected AExtrudedPolygonalStructure(@Nonnull Sector sector, @Nonnull List<List<Position>> blockLocations)
    {
        super(sector, blockLocations);
    }

    /**
     * Set the altitude mode of the surface structure.
     *
     * @param altitudeMode altitude mode
     */
    public void setAltitudeMode(int altitudeMode)
    {
        this.altitudeMode = altitudeMode;
    }

    /**
     * @return altitude mode
     */
    public int getAltitudeMode()
    {
        return altitudeMode;
    }

    /**
     * Set whether this surface structure is extruded.
     *
     * @param isExtrude extrude
     */
    public void setIsExtrude(boolean isExtrude)
    {
        this.isExtrude = isExtrude;
    }

    /**
     * @return true is extruded, false otherwise
     */
    public boolean isExtrude()
    {
        return isExtrude;
    }

    @Override
    protected void writeVertex(Position location, DataOutputStream dos) throws IOException
    {
        dos.writeDouble(location.getLatitude().degrees);
        dos.writeDouble(location.getLongitude().degrees);
        dos.writeDouble(location.getElevation());
    }

    @Override
    protected Position readVertex(DataInputStream dis) throws IOException
    {
        return Position.fromDegrees(dis.readDouble(), dis.readDouble(), dis.readDouble());
    }
}
