package gov.takx.core.structures;

import gov.nasa.worldwind.formats.shapefile.Shapefile;
import gov.takx.api.layer.IRaptorLayer;
import gov.takx.api.structures.ISurfaceStructureCallback;
import gov.takx.commons.file.FileUtils;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.lang.invoke.MethodHandles;

/**
 * Abstract base for importing SHP files as surface structures. All object creation and persistence is done off of the EDT.
 * The only actions that take place on the EDT are the initial creation of the progress panel for this import,
 * and when the surface structure gets added to the renderable layer.
 *
 * @param <L> Renderable layer type
 * @since 5.5 (extracted from StructureFileImporter, which has been deleted)
 */
abstract class AShpStructureFileImporter<L extends IStructureLayer> extends AStructureImporter<L>
{
    private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    protected String copiedFileRelativePath;
    protected Shapefile shapeFile;
    protected String shapeFileType;

    protected AShpStructureFileImporter(@Nonnull File file, @Nonnull IRaptorLayer raptorLayer, @Nullable ISurfaceStructureCallback callback)
    {
        super(file, raptorLayer, callback);
    }

    @Override
    protected boolean prepareForImport()
    {
        // Test for file validity and copy files into project.
        if (!StructureUtils.isShapeFile(originalFile) || !copyOriginalShapeFiles()) return false;

        shapeFile = new Shapefile(copiedFile);
        return (shapeFileType = shapeFile.getShapeType()) != null;
    }

    /**
     * Copy our original shape file and any related files into VFS.  This copy uses the base name of the original file
     * to locate any associated files, such as projection and attribute files.
     *
     * @return {@code true} on success, or {@code false} otherwise
     */
    private boolean copyOriginalShapeFiles()
    {
        if (!makeVfsRootDir(StructureUtils.SHAPE_FILE_DIR)) return false;

        final String basename = FileUtils.getNameWithoutExtension(originalFile);
        final File sourceDir = originalFile.getParentFile();

        try
        {
            for (String ext : StructureUtils.SHAPE_FILE_EXTENSIONS)
            {
                final String filename = basename + "." + ext;

                // If the file exists, then copy it to the target directory
                final var sourceFile = new File(sourceDir, filename);
                final var targetFile = new File(rootVfsImportDir, filename);

                FileUtils.copy(sourceFile, targetFile); // Ignores (skips) missing source files

                // Track the copied location of our primary source file
                if ("shp".equals(ext))
                {
                    final var rootPath = projectVFSManager.getVirtualFileSystem(StructureUtils.SHAPE_FILE_DIR).getRootDir().getPath();
                    copiedFile = targetFile;
                    copiedFileRelativePath = copiedFile.getPath().replace(rootPath, "");
                }
            }
        } catch (IOException e)
        {
            logger.error("Failed to copy file: {}", originalFile, e);
            return false;
        }

        return true;
    }
}
