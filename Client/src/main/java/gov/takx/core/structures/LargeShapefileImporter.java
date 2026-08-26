package gov.takx.core.structures;

import gov.nasa.worldwind.formats.shapefile.Shapefile;
import gov.takx.api.annotation.PseudoBean;
import gov.takx.api.layer.IRaptorLayer;
import gov.takx.api.structures.ISurfaceStructureCallback;

import java.io.File;

/**
 * Large Shapefile Importer.  Specialized handling for SHP files that strictly contain 3D polygons.
 *
 * @see LargeShapefile
 * @see LargeShapefileLayer
 */
@PseudoBean
public class LargeShapefileImporter extends AShpStructureFileImporter<LargeShapefileLayer>
{
    private LargeShapefile shape;

    public LargeShapefileImporter(File file, IRaptorLayer raptorLayer, ISurfaceStructureCallback callback)
    {
        super(file, raptorLayer, callback);
    }

    @Override
    protected boolean prepareForImport()
    {
        if (!super.prepareForImport()) return false;

        structureLayer.setFilePath(copiedFileRelativePath);
        return true;
    }

    @Override
    protected int countShapes()
    {
        return Shapefile.isPolygonType(shapeFileType) ? shapeFile.getNumberOfRecords() : 0;
    }

    @Override
    protected void loadStructures()
    {
        // Just constructing the LargeShapefile causes the load to occur.
        // We have to construct a new Shapefile instance since its content cannot be iterated more than once. :(
        shape = new LargeShapefile(new Shapefile(copiedFile), this);
    }

    @Override
    protected void finalizeImport()
    {
        structureLayer.setShape(shape); // Install the shape (which does all the actual rendering)
    }
}
