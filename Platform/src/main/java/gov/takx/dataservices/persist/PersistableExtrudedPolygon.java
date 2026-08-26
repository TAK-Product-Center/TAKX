package gov.takx.dataservices.persist;

import gov.nasa.worldwind.geom.Position;
import gov.nasa.worldwind.geom.Sector;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import jakarta.persistence.Entity;

import java.util.List;

/**
 * Persistable extruded polygon.
 *
 */
@Entity
public class PersistableExtrudedPolygon extends AExtrudedPolygonalStructure
{
    /**
     * There are two types of world wind extruded polygons. The first is an extruded polygon where each point has a
     * specific altitude associated with it. The second type is an extruded polygon whose points are all at the same altitude.
     * This second type of extruded polygon uses an extruded height value to extrude all points vertically to a specific
     * height. This value is stored in a Double containing that height. If this is {@code null} on recreation,
     * we know to create the first type, otherwise we create the second.
     */
    protected Double extrudeHeight;

    /**
     * Hibernate constructor.
     */
    protected PersistableExtrudedPolygon()
    {
    }

    /**
     * PersistableExtrudedPolygon constructor.
     */
    public PersistableExtrudedPolygon(@Nonnull Sector sector, @Nonnull List<List<Position>> blockLocations, @Nullable Double height)
    {
        super(sector, blockLocations);

        extrudeHeight = height;
    }

    /**
     * Set the extruded height for this polygon. A {@code null} value will indicate that this polygon does not have an
     * extruded height value, meaning that each position has its own altitude associated with it.
     *
     * @param extrudeHeight extrude height ({@code null} for no extrusion
     * @since 3.4 (name changed from setExtrudedHeight to setExtrudeHeight)
     */
    public void setExtrudeHeight(@Nullable Double extrudeHeight)
    {
        this.extrudeHeight = extrudeHeight;
    }

    /**
     * @return the extruded height of this polygon, may be {@code null}
     */
    @Nullable
    public Double getExtrudeHeight()
    {
        return extrudeHeight;
    }

    @Override
    public boolean isExtrude()
    {
        return super.isExtrude() || extrudeHeight != null && !extrudeHeight.equals(0d);
    }
}
