package gov.takx.dataservices.persist;

import gov.nasa.worldwind.geom.LatLon;
import gov.takx.api.structures.ISurfaceStructure;
import jakarta.persistence.Entity;
import jakarta.persistence.Transient;

import java.util.Collections;

/**
 * Persistable object for SSShapefileRecord.  LargeShapefile is responsible for creation of all the structure in the
 * shapefile, so this class only required to maintain UUID and metadata values.
 *
 */
@Entity
public class PersistableShapefileShape extends PersistableSurfaceStructure
{
    private int recordNumber = -1;

    @Transient
    protected ISurfaceStructure renderable;

    public PersistableShapefileShape()
    {
    }

    /**
     * Set the record number for this persistable shapefile shape.  The record number is the identifying value which
     * enables this object to link to its associated SSShapefile after loading a project.
     *
     * @param recordNumber record number for this persistable shapefile shape.
     */
    public void setRecordNumber(int recordNumber)
    {
        this.recordNumber = recordNumber;
    }

    /**
     * Get the record number for this persistable shapefile shape.  The record number is the identifying value which
     * enables this object to link to its associated SSShapefile after loading a project.
     *
     * @return record number for this persistable shapefile shape.
     */
    public int getRecordNumber()
    {
        return recordNumber;
    }

    @Override
    public Iterable<? extends LatLon> getVertices()
    {
        return Collections.emptyList();
    }
}
