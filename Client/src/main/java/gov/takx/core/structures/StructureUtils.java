package gov.takx.core.structures;

import gov.takx.commons.file.FileUtils;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import java.io.File;
import java.util.Arrays;
import java.util.List;

/**
 * Simple utility methods and constants used for surface structure import.
 */
public class StructureUtils
{
    public static final List<String> SHAPE_FILE_EXTENSIONS = Arrays.asList("shp", "shx", "dbf", "prj");
    public static final List<String> KML_FILE_EXTENSIONS = Arrays.asList("kml", "kmz");
    public static final String SHAPE_FILE_DIR = "ShapeFiles";
    public static final String KML_FILE_DIR = "KMLFiles";

    /**
     * @return {@code true} if the given is not null, exists, and has one of the SHP file extension
     */
    public static boolean isShapeFile(@Nullable File file)
    {
        return isFileValid(file, SHAPE_FILE_EXTENSIONS);
    }

    /**
     * @return {@code true} if the given is not null, exists, and has one of the KML file extension
     */
    public static boolean isKMLFile(@Nullable File file)
    {
        return isFileValid(file, KML_FILE_EXTENSIONS);
    }

    private static boolean isFileValid(@Nullable File file, @Nonnull List<String> extensions)
    {
        if (file == null || !file.exists()) return false;

        return extensions.contains(FileUtils.getExtension(file));
    }
}
