package gov.takx.dataservices.persist;

import gov.nasa.worldwind.geom.Position;
import gov.nasa.worldwind.geom.Sector;
import jakarta.persistence.Entity;

import java.util.Collections;
import java.util.List;

/**
 * Persistable extruded line.
 *
 */
@Entity
public class PersistableExtrudedLine extends AExtrudedPolygonalStructure
{
    protected boolean isTesselate; // is terrain conforming

    /**
     * Hibernate constructor.
     */
    protected PersistableExtrudedLine()
    {
    }

    public PersistableExtrudedLine(Sector sector, List<Position> locations)
    {
        super(sector, Collections.singletonList(locations));
    }

    /**
     * Set whether or not the surface structure conforms to terrain.
     *
     * @param isTessellate true to conform, false to not conform
     */
    public void setTessellate(boolean isTessellate)
    {
        isTesselate = isTessellate;
    }

    /**
     * @return is terrain conforming
     */
    public boolean isTessellate()
    {
        return isTesselate;
    }
}
