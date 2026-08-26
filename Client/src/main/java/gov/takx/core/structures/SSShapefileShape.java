package gov.takx.core.structures;

import gov.nasa.worldwind.formats.shapefile.ShapefileExtrudedPolygons;
import gov.nasa.worldwind.formats.shapefile.ShapefileRecord;
import gov.nasa.worldwind.formats.shapefile.ShapefileRenderable;
import gov.nasa.worldwind.geom.Intersection;
import gov.nasa.worldwind.geom.LatLon;
import gov.nasa.worldwind.geom.Line;
import gov.nasa.worldwind.render.DrawContext;
import gov.nasa.worldwind.render.OrderedRenderable;
import gov.nasa.worldwind.render.Renderable;
import gov.nasa.worldwind.render.ShapeAttributes;
import gov.nasa.worldwind.terrain.Terrain;
import gov.takx.api.coordinatesystem.RaptorLocation;
import gov.takx.api.structures.ISurfaceStructure;
import gov.takx.dataservices.persist.PersistableShapefileShape;

import java.util.List;

/**
 * An ISurfaceStructure implementation for ShapefileExtrudedPolygons.Record.
 *
 */
public class SSShapefileShape extends ASurfaceStructureHolder<OrderedRenderable, PersistableShapefileShape>
{
    private final ShapeRenderable record;

    public SSShapefileShape(LargeShapefile shapefile, ShapefileRecord shapefileRecord, int recordNumber, PersistableShapefileShape persistableShape)
    {
        super(shapefile.getSector(), persistableShape);

        record = new ShapeRenderable(shapefile, shapefileRecord);
        record.setOrdinal(recordNumber);

        setDefaultAttributes();
    }

    public SSShapefileShape(LargeShapefile shapefile, ShapefileRecord shapefileRecord, int recordNumber)
    {
        this(shapefile, shapefileRecord, recordNumber, new PersistableShapefileShape());

        LatLon centroid = sector.getCentroid();

        persistable.setLocation(new RaptorLocation(centroid.latitude.degrees, centroid.longitude.degrees));
        persistable.setRecordNumber(record.getOrdinal());
    }

    @Override
    protected OrderedRenderable makeRenderable()
    {
        return null; // We don't have a renderable, it is all handled by LargeShapefile
    }

    public ShapefileExtrudedPolygons.Record getRecord()
    {
        //noinspection ReturnOfInnerClass
        return record;
    }

    public void setOrdinal(int ordinal)
    {
        record.setOrdinal(ordinal);
    }

    public int getOrdinal()
    {
        return record.getOrdinal();
    }

    public boolean isVisible()
    {
        return record.isVisible();
    }

    public void setVisible(boolean visible)
    {
        record.setVisible(visible);
    }

    @Override
    public Renderable getRenderable()
    {
        return record.getShapefileRenderable();
    }

    /**
     * Get a String representation of this record's extruded height.
     *
     * @return String representation of this record's extruded height.
     */
    public String getHeightString()
    {
        final Double height = record.getHeight();
        return String.format("%.2f m", height != null ? height : 0D);
    }

    @Override
    public boolean isIntersectable()
    {
        return true;
    }

    @Override
    public List<Intersection> intersect(Line line, Terrain terrain) throws InterruptedException
    {
        return record.intersect(line, terrain);
    }

    /**
     * Override to apply the attributes to our record, since the renderable in the base class is always {@code null} for us.
     */
    @Override
    protected void saveAndApplyAttributes(ShapeAttributes attributes)
    {
        super.saveAndApplyAttributes(attributes);

        record.setAttributes(attributes);
    }

    @Override
    protected void applyAttributesToRenderable(ShapeAttributes attributes)
    {
        record.setAttributes(attributes);
    }

    @Override
    public String toString()
    {
        return persistable != null ? persistable.toString() : super.toString();
    }

    private final class ShapeRenderable extends ShapefileExtrudedPolygons.Record implements ISurfaceStructureRenderable
    {
        public ShapeRenderable(ShapefileRenderable shapefileRenderable, ShapefileRecord shapefileRecord)
        {
            super(shapefileRenderable, shapefileRecord);
        }

        public void setOrdinal(int ordinal)
        {
            this.ordinal = ordinal;
        }

        @Override
        public ISurfaceStructure getSurfaceStructure()
        {
            return SSShapefileShape.this;
        }

        @Override
        public void render(DrawContext dc)
        {
            // do nothing, stupid WW GeographicExtent extends Renderable for no apparent reason
        }
    }
}
