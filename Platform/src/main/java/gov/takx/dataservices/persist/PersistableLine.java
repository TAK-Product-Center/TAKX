package gov.takx.dataservices.persist;

import gov.nasa.worldwind.geom.LatLon;
import gov.nasa.worldwind.geom.Sector;
import jakarta.persistence.Entity;

import java.util.List;

/**
 * Persisted data for a polyline.
 *
 */
@Entity
public class PersistableLine extends ASurfacePolygonalStructure
{
    /**
     * Hibernate constructor.
     */
    protected PersistableLine()
    {
    }

    public PersistableLine(Sector sector, List<List<LatLon>> locations)
    {
        super(sector, locations);
    }
}
