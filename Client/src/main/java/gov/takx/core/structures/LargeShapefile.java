package gov.takx.core.structures;

import com.jogamp.opengl.GL;
import com.jogamp.opengl.GL2;
import gov.nasa.worldwind.formats.shapefile.Shapefile;
import gov.nasa.worldwind.formats.shapefile.ShapefileExtrudedPolygons;
import gov.nasa.worldwind.formats.shapefile.ShapefileRecord;
import gov.nasa.worldwind.geom.LatLon;
import gov.nasa.worldwind.geom.Sector;
import gov.nasa.worldwind.render.DrawContext;
import gov.takx.api.structures.ISurfaceStructure;
import gov.takx.dataservices.persist.PersistableShapefileShape;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.invoke.MethodHandles;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Special shapefile class which extends WW's ShapefileExtrudedPolygons class.  ShapefileExtrudedPolygons manages a
 * collection of shapefile record renderables, which are rendered like extruded polygons.  This is unusual in that
 * although each record is displayed as a separate object on the globe, and treated individually as ISurfaceStructures
 * in TAKX, they are always managed by ShapefileExtrudedPolygons as a collection. ShapefileExtrudedPolygons is
 * memory, CPU, and GPU efficient compared to a large collection of individual ExtrudedPolygons.
 *
 */
public class LargeShapefile extends ShapefileExtrudedPolygons
{
    private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    /**
     * Associated renderable layer.
     */
    private LargeShapefileLayer layer;

    private final Map<Integer, PersistableShapefileShape> persistableShapes;
    private final IStructureImporter importer;

    /**
     * Collection of structures in this shape.
     */
    protected final StructureCollection structures = new StructureCollection();

    private final int[] glFrontFaceSaved = new int[]{GL.GL_CCW};

    /**
     * Constructor used when processing an import operation.
     *
     * @param shapefile The shapefile to process
     * @param importer  Invoking importer, will not be {@code null}
     * @since 3.0
     */
    public LargeShapefile(Shapefile shapefile, IStructureImporter importer)
    {
        this(shapefile, null, importer);
    }

    /**
     * Constructor used when restoring an existing project.
     *
     * @param shapefile         The shapefile to process
     * @param persistableShapes Map from record number to existing persisted state, may be {@code null}
     * @since 3.0
     */
    public LargeShapefile(Shapefile shapefile, Map<Integer, PersistableShapefileShape> persistableShapes)
    {
        this(shapefile, persistableShapes, null);
    }

    /**
     * @param shapefile         The shapefile to process
     * @param persistableShapes Map from record number to existing persisted state, may be {@code null}
     * @param importer          Invoking importer, may be {@code null} when restoring a project
     * @since 3.0
     */
    private LargeShapefile(Shapefile shapefile, Map<Integer, PersistableShapefileShape> persistableShapes, IStructureImporter importer)
    {
        super(shapefile);

        this.persistableShapes = persistableShapes;
        this.importer = importer;

        actuallyAssembleRecords(shapefile);

        setDefaultBaseDepth(5000); // TODO: WHY?
    }

    @Override
    protected void assembleRecords(Shapefile shapefile)
    {
        // Do nothing, we need to delay actually assembling records so we can get our internal state ready
    }

    private void actuallyAssembleRecords(Shapefile shapefile)
    {
        super.assembleRecords(shapefile);
    }

    /**
     * Set the LargeShapefileLayer for this LayerShapefile.
     *
     * @param layer LargeShapefileLayer for this LayerShapefile.
     */
    public void setLayer(LargeShapefileLayer layer)
    {
        this.layer = layer;
    }

    /**
     * Get the LargeShapefileLayer for this LayerShapefile.
     *
     * @return LargeShapefileLayer for this LayerShapefile.
     */
    public LargeShapefileLayer getLayer()
    {
        return layer;
    }

    /**
     * Get all ISurfaceStructures in this shape.
     *
     * @return collection of all ISurfaceStructures in this shape.
     */
    public List<ISurfaceStructure> getStructures()
    {
        return structures.getStructures();
    }

    /**
     * Get the number of structures in this shape.
     *
     * @return number of structures in this shape.
     */
    public int getSize()
    {
        return structures.getSize();
    }

    /**
     * Get all structures within a Sector in this shape file.
     *
     * @param extent Sector of interest.
     * @return collection of Renderables within the Sector.
     */
    public Collection<? extends ISurfaceStructure> getStructures(Sector extent)
    {
        return structures.getStructures(extent);
    }

    /**
     * Get all structures under a location.
     *
     * @param location location of interest.
     * @return collection of Renderables under a location.
     */
    public Collection<? extends ISurfaceStructure> getStructures(LatLon location)
    {
        return structures.getStructures(location);
    }

    /**
     * Access a structure by its name.
     *
     * @param name name of Renderable.
     * @return Renderable associated with name, if one exists.
     */
    public ISurfaceStructure getByName(String name)
    {
        return structures.getByName(name);
    }

    /**
     * Get an iterable of record numbers of all non-visible structures in this collection.
     *
     * @return iterable of record numbers of all non-visible structures in this collection.
     */
    public Iterable<Integer> getInvisibleRecordNumbers()
    {
        return structures.getInvisibleRecordNumbers();
    }

    /**
     * Set an iterable of record numbers of structures to make invisible in this collection.
     *
     * @param invisible iterable of record numbers of structures to make invisible in this collection.
     */
    public void setInvisibleRecordNumbers(Iterable<Integer> invisible)
    {
        for (int i : invisible)
        {
            structures.setVisible(i, false);
        }
    }

    /**
     * "Removes" a structure from this shape if it is present.  In reality, it doesn't really remove anything, but it
     * does set the structure invisible, which prevents it from being rendered or used in intersection calculations.
     */
    public void hideStructureIfPresent(ISurfaceStructure structure)
    {
        structures.setVisible(structure, false);
    }

    /**
     * Retrieve an individual shape by its record number.
     *
     * @param recordNumber shape's record number
     * @return shape.
     */
    public SSShapefileShape getShapeRecord(int recordNumber)
    {
        final ISurfaceStructure structure = getByName(Integer.toString(recordNumber));
        return structure instanceof SSShapefileShape ? (SSShapefileShape) structure : null;
    }

    @Override
    protected ShapefileExtrudedPolygons.Record createRecord(ShapefileRecord shapefileRecord)
    {
        final int recordNumber = getRecordCount(); // Grab the value that will become the record number

        final PersistableShapefileShape persistableShape = persistableShapes == null
                ? null
                : persistableShapes.get(recordNumber);

        if (logger.isDebugEnabled()) logger.debug("create, record={}, PS={}", recordNumber, persistableShape);

        final SSShapefileShape shapefileShape = persistableShape != null
                ? new SSShapefileShape(this, shapefileRecord, recordNumber, persistableShape)
                : new SSShapefileShape(this, shapefileRecord, recordNumber);

        shapefileShape.setDefaultAttributes();

        structures.addStructure(shapefileShape);

        if (importer != null) importer.addImportedStructure(shapefileShape);

        return shapefileShape.getRecord();
    }

    /**
     * todo: just use a quad-tree here
     * <p/>
     * A little collection class to ease retrieval of structures.
     */
    private static class StructureCollection
    {
        private final Map<String, SSShapefileShape> structureMap = new HashMap<>();

        void addStructure(SSShapefileShape structure)
        {
            structureMap.put(Integer.toString(structure.getOrdinal()), structure);
        }

        /**
         * Get the number of structures in this collection.
         *
         * @return number of structures in this collection.
         */
        public int getSize()
        {
            return structureMap.size();
        }

        /**
         * Set an individual structure's visibility, IF it is contained by this collection.  Invisible structures are
         * ignored by occlusion calculations.
         *
         * @param structure structure of interest.
         * @param visible   {@code true} to set the structure visible, or {@code false} to set it invisible.
         */
        public void setVisible(ISurfaceStructure structure, boolean visible)
        {
            if (structure instanceof SSShapefileShape)
            {
                setVisible(((SSShapefileShape) structure).getOrdinal(), visible);
            }
        }

        /**
         * Set an individual structure's visibility, IF it is contained by this collection.  Invisible structures are
         * ignored by occlusion calculations.
         *
         * @param recordNumber record number of the structure of interest.
         * @param visible      {@code true} to set the structure visible, or {@code false} to set it invisible.
         */
        public void setVisible(int recordNumber, boolean visible)
        {
            SSShapefileShape record = structureMap.get(Integer.toString(recordNumber));
            if (record != null)
            {
                record.setVisible(visible);
            }
        }

        /**
         * Get an iterable of record numbers of all non-visible structures in this collection.
         *
         * @return iterable of record numbers of all non-visible structures in this collection.
         */
        public Iterable<Integer> getInvisibleRecordNumbers()
        {
            return structureMap.values().stream()
                    .filter(shape -> !shape.isVisible())
                    .map(SSShapefileShape::getOrdinal)
                    .collect(Collectors.toList());
        }

        /**
         * Get all VISIBLE structures in this collection.
         *
         * @return all structures in this collection
         */
        public List<ISurfaceStructure> getStructures()
        {
            return structureMap.values().stream()
                    .filter(SSShapefileShape::isVisible)
                    .collect(Collectors.toList());
        }

        /**
         * Access a record (structure) by name.
         *
         * @param name name of record.
         * @return record associated with the name.
         */
        public SSShapefileShape getByName(String name)
        {
            return structureMap.get(name);
        }

        /**
         * Get all visible structures under a location.
         *
         * @param location location of interest.
         * @return collection of Renderables under a location.
         */
        public Collection<? extends ISurfaceStructure> getStructures(LatLon location)
        {
            // todo: bounding sector?
            return structureMap.values().stream()
                    .filter(p -> p.isVisible() && p.getSector().contains(location))
                    .collect(Collectors.toList());
        }

        /**
         * Get all visible structures within a Sector in this shape.
         *
         * @param sectorOfInterest Sector of interest
         * @return collection of Renderables within the Sector
         */
        public Collection<? extends ISurfaceStructure> getStructures(Sector sectorOfInterest)
        {
            final Sector extent = Sector.boundingSector(sectorOfInterest);

            // todo: bounding sector?
            return structureMap.values().stream()
                    .filter(p -> p.isVisible() && (extent.contains(p.getSector()) || extent.intersects(p.getSector())))
                    .collect(Collectors.toList());
        }
    }

    @Override
    protected void makeOrderedSurfaceRenderable(DrawContext dc)
    {
        // performs a visibility test against the top level tile
        this.assembleTiles(dc);

        // don't add an ordered renderable when there's nothing to draw
        if (this.currentTiles.isEmpty()) return;

        this.pickLayer = dc.getCurrentLayer();
        // We want to add this as a normal renderable, *not* as a surface renderable
        dc.addOrderedRenderable(this);
    }

    @Override
    protected void beginDrawing(DrawContext dc)
    {
        super.beginDrawing(dc);
        GL2 gl = dc.getGL().getGL2();
        // Save current front face winding value
        gl.glGetIntegerv(GL.GL_FRONT_FACE, glFrontFaceSaved, 0);
        // Set front face winding to CCW to match what WW expects
        gl.glFrontFace(GL.GL_CCW);
    }

    @Override
    protected void endDrawing(DrawContext dc)
    {
        super.endDrawing(dc);
        GL2 gl = dc.getGL().getGL2(); // GL initialization checks for GL2 compatibility.
        // Restore current front face winding value
        gl.glFrontFace(glFrontFaceSaved[0]);
    }
}
