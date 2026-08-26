package gov.takx.dataservices.persist;

import gov.nasa.worldwind.geom.LatLon;
import gov.nasa.worldwind.geom.Sector;
import jakarta.persistence.Entity;

import java.util.List;

/**
 * Persisted data for a polygon.
 *
 */
@Entity
public class PersistablePolygon extends ASurfacePolygonalStructure
{
    /**
     * Hibernate constructor.
     */
    protected PersistablePolygon()
    {
    }

    public PersistablePolygon(Sector sector, List<List<LatLon>> blockLocations)
    {
        super(sector, blockLocations);
    }
}
