package gov.takx.core.view.gui.maps;

import gov.nasa.worldwind.formats.shapefile.DBaseRecord;
import gov.nasa.worldwind.formats.shapefile.Shapefile;
import gov.nasa.worldwind.formats.shapefile.ShapefileRecord;
import gov.nasa.worldwind.util.CompoundVecBuffer;
import gov.takx.core.globe.layers.ShapefileImportHandler;

import java.io.File;

/**
 */
public class AddShapeOverlayController extends AddOverlayController
{
    public static final String SHP_OVERLAY = "shp_overlay";
    public static final String DISPLAY_NAME = "Shape File Overlay Importer";

    private static final String[] HEIGHT_KEYS = {"height", "Height", "HEIGHT", "HGT", "Hgt", "hgt"};

    public AddShapeOverlayController(File file)
    {
        super(file);
    }

    @Override
    public boolean doesFileHave3DGeometry()
    {
        try (Shapefile sf = new Shapefile(getFile()))
        {
            while (sf.hasNext())
            {
                ShapefileRecord record = sf.nextRecord();
                if (record == null || record.getNumberOfPoints() < 3)
                {
                    continue;
                }

                CompoundVecBuffer buffer = record.getCompoundPointBuffer();
                boolean hasSpecificAltitudes = false;
                int count = 0;
                //make sure we have enough vertices to create a shape and check to see if our coordinate arrays contain
                //more than 2 values. if there is more than two values, we need to treat this as an extruded polygon
                for (double[] position : buffer.getCoords())
                {
                    if (position != null && count < 3)
                    {
                        hasSpecificAltitudes = hasSpecificAltitudes || position.length > 2;
                        count++;
                    }
                }
                if (hasSpecificAltitudes && count >= 3)
                {
                    return true;
                }

                final DBaseRecord attributes = record.getAttributes();

                if (attributes != null)
                {
                    for (String key : HEIGHT_KEYS)
                    {
                        if (attributes.getValue(key) != null)
                        {
                            return true;
                        }
                    }
                }
            }
        }
        return false;
    }

    @Override
    public String getType()
    {
        return SHP_OVERLAY;
    }

    @Override
    protected void importAsSimpleOverlay()
    {
        ShapefileImportHandler.importShapeFile(getFile());
    }

    @Override
    public String toString()
    {
        return getName();
    }

    @Override
    public String getName()
    {
        return DISPLAY_NAME;
    }
}
