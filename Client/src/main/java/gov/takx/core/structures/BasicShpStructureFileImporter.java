package gov.takx.core.structures;

import gov.nasa.worldwind.formats.shapefile.Shapefile;
import gov.nasa.worldwind.formats.shapefile.ShapefileRecord;
import gov.nasa.worldwind.formats.shapefile.ShapefileUtils;
import gov.nasa.worldwind.geom.Sector;
import gov.takx.api.layer.IRaptorLayer;
import gov.takx.api.structures.ISurfaceStructureCallback;
import gov.takx.core.view.gui.maps.importers.SimpleMapFilesImporter;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.lang.invoke.MethodHandles;

/**
 * Surface structure importer handling SHP files that include elements other than 3D polygons, and as such cannot be
 * handled by the optimized {@link LargeShapefileImporter}.
 *
 * @since 5.5 (extracted from StructureFileImporter, which has been deleted)
 */
public class BasicShpStructureFileImporter extends AShpStructureFileImporter<CachedSurfaceStructureLayer>
{
    private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    public BasicShpStructureFileImporter(@Nonnull File file, @Nonnull IRaptorLayer raptorLayer, @Nullable ISurfaceStructureCallback callback)
    {
        super(file, raptorLayer, callback);
    }

    /**
     * @return the number of shape records in the given file
     */
    public static int getShapeCount(File f)
    {
        return getShapeCount(new Shapefile(f), true);
    }

    @Override
    protected int countShapes()
    {
        return getShapeCount(shapeFile, false);
    }

    @Override
    protected void loadStructures()
    {
        createSurfaceStructures();
        shapeFile.close();
    }

    /**
     * After all the structures have been created, we need to add the renderables to the layer.
     */
    @Override
    protected void finalizeImport()
    {
        if (logger.isDebugEnabled()) logger.debug("Finalizing {} shapes", importedStructures.size());

        // Add all the renderables to their layer
        importedStructures.forEach(structureLayer::add);

        // tell the layer that no more structures will be added so that the quad tree may be created with the
        // current bounding sector
        structureLayer.batchAddDone();

        if (structureLayer instanceof ApplicationCachedSurfaceStructureLayer cachedLayer)
        {
            new SimpleMapFilesImporter(cachedLayer.getImageFolder(), cachedLayer.getName()).updateFileSetsAndImport();
        }
    }

    /**
     * Count the number of shapes in a given shape file.
     *
     * @param shapeFile Shape file to process
     * @param close     Pass {@code true} if the given shape file should be closed after processing
     * @return number of shapes in the file, 0 if type is not supported, 1 for point or polyline, or the
     * number of records for polygons.
     * @since 3.0
     */
    private static int getShapeCount(Shapefile shapeFile, boolean close)
    {
        final String shapeFileType = shapeFile.getShapeType();

        if (shapeFileType == null) return 0;

        final int count = Shapefile.isPointType(shapeFileType) || Shapefile.isPolylineType(shapeFileType)
                ? 1
                : shapeFile.getNumberOfRecords();

        if (close) shapeFile.close();

        return count;
    }

    private void createSurfaceStructures()
    {
        if (shapeFileType != null)
        {
            if (Shapefile.isPolylineType(shapeFileType))
            {
                createPolylineForShapeFile();
            } else
            {
                while (shapeFile.hasNext())
                {
                    final var record = shapeFile.nextRecord();
                    final String shapeType = record.getShapeType();
                    if (shapeType != null && Shapefile.isPolygonType(shapeType))
                    {
                        try
                        {
                            createPolygon(record);
                        } catch (IllegalArgumentException e)
                        {
                            // If this turns out to be too noisy, we can investigate further
                            logger.warn("Error creating polygon", e);
                        }
                        testAborted();
                    }
                }
            }
        }
    }

    private void createPolygon(ShapefileRecord sfr)
    {
        final var sector = Sector.fromDegrees((sfr).getBoundingRectangle());
        final var buffer = sfr.getCompoundPointBuffer();

        if (buffer == null || buffer.getCoords() == null || buffer.getPositions() == null)
        {
            logger.warn("Something is null about our shape: {}", sfr.getRecordNumber());
            return;
        }

        // ensure we have enough vertices to create a polygon.

        final var iterator = buffer.getCoords().iterator();
        int count = 0;

        while (iterator.hasNext())
        {
            if (++count == 3) break;
        }

        if (count < 3)
        {
            logger.warn("Not enough points to make a polygon: {}", sfr.getRecordNumber());
            return;
        }

        // Check to see if polygon has a height value (is extrudable).
        //
        // As with a polyline (see the comments in createPolylineForShapeFile below), the shape coordinates can not
        // specify individual heights.  So, just check for a height attribute on the record.

        final Double height = ShapefileUtils.extractHeightAttribute(sfr);

        final var structure = height == null
                ? new SSPolygon(sector, buffer)
                : new SSExtrudedPolygon(sector, buffer, height);

        addImportedStructure(structure);
    }

    /**
     * Create a polyline representing the bounding rectangle of the shape file.
     */
    private void createPolylineForShapeFile()
    {
        final var sector = Sector.fromDegrees(shapeFile.getBoundingRectangle());
        final var buffer = shapeFile.getPointBuffer();

        if (buffer == null || buffer.getCoords() == null)
        {
            logger.warn("Bounding sector for shape file is undefined: {}", originalFile);
            return;
        }

        // The previous code attempted to determine if the polyline had a height on any of the vertex coordinates.
        // However, it was doing it in such a way that it could never have worked.  The coordinates for a polyline
        // are ALWAYS 2-tuples (lat/lon).  If we wanted to support POLYLINE_Z types, we would need to extract
        // the Z values and compose our own Position elements including those values per-vertex.
        //
        // Further, the code then tested to see if the shapefile had a "height" attribute. However, the "file"
        // can't have that attribute since we create it with an empty AVList.  Note that individual records in
        // the file might have that attribute, but the original code was not processing individual records.
        // So, again, there was no possibility that a height was extracted.
        //
        // Given that this code has never worked, and there is no current requirement to support polyline shapes
        // with Z values, I'm not going to implement that. We can do so if it ever becomes a requirement.
        //
        // So, for now, we just create a surface polyline.

        addImportedStructure(new SSLine(sector, buffer));
    }
}
