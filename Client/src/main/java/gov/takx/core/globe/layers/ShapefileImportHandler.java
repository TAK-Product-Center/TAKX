package gov.takx.core.globe.layers;

import gov.takx.api.annotation.InvokeOnEDT;
import gov.takx.api.annotation.PseudoBean;
import gov.takx.api.annotation.VFSManagerScope;
import gov.takx.api.filesystem.IVirtualFileSystem;
import gov.takx.api.filesystem.IVirtualFileSystemManager;
import gov.takx.api.filesystem.VFSScope;
import gov.takx.api.layer.ILayerService;
import gov.takx.api.layer.IRaptorLayer;
import gov.takx.api.layer.LayerConfiguration;
import gov.takx.api.layer.builder.ShapefileLayerBuilder;
import gov.takx.commons.file.FileUtils;
import gov.takx.commons.swing.AlwaysOnTopJOptionPane;
import gov.takx.commons.swing.StatusDialog;
import jakarta.inject.Inject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.SwingWorker;

import java.io.File;
import java.io.IOException;
import java.lang.invoke.MethodHandles;
import java.util.Set;
import java.util.concurrent.ExecutionException;

/**
 * Class to add Shapefiles to globe.
 * @since 5.4
 */
@PseudoBean
public class ShapefileImportHandler
{
    private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    private static final String SHAPE_FILE_DIR = "ShapeFiles";

    /**
     * All the supported file extensions that may be part of a shapefile set.
     */
    public static final Set<String> SHAPE_FILE_EXTENSIONS = Set.of("shp", "shx", "dbf", "prj");

    @Inject
    private ILayerService layerService;

    @Inject
    @VFSManagerScope(VFSScope.Project)
    private IVirtualFileSystemManager vfsManager;

    /**
     * Import a shapefile (and all associated files) into VFS and create a new shape file layer from the data.
     * All of this file processing takes place on a background thread, and then the layer creation is handled on
     * the EDT.
     *
     * @param inputFile Path to shape file to import
     */
    @InvokeOnEDT
    public static void importShapeFile(final File inputFile)
    {
        new ShapefileImportHandler().importShapeFileImpl(inputFile);
    }

    private ShapefileImportHandler()
    {
    }

    void importShapeFileImpl(final File inputFile)
    {
        IVirtualFileSystem fileSystem = vfsManager.getVirtualFileSystem(SHAPE_FILE_DIR);
        File rootDir = fileSystem.getRootDir();

        // Take the base name of the shape file and use that to create the directory.
        final String basename = FileUtils.getNameWithoutExtension(inputFile);
        final String targetDirName = FileUtils.getNextNameIteration(rootDir, basename); // Ensure a unique name
        final File targetDir = new File(rootDir, targetDirName);

        // We need to copy all the files associated with the shape file to the target directory.

        final StatusDialog statusDialog = new StatusDialog(false);
        statusDialog.setStatusText("Importing...");
        statusDialog.setVisible(true);

        // In the background, copy all the files into VFS and then kick off the layer creation from the imported files

        new SwingWorker<String, Object>()
        {
            @Override
            protected String doInBackground()
            {
                final File sourceDir = inputFile.getParentFile();

                if (!targetDir.mkdirs()) return "Failed to create import directory: " + targetDir;

                try
                {
                    for (String ext : SHAPE_FILE_EXTENSIONS)
                    {
                        String filename = basename + "." + ext;

                        // If the file exists, then copy it to the target directory
                        File sourceFile = new File(sourceDir, filename);
                        logger.debug("testing: {}", sourceFile);

                        if (sourceFile.exists())
                        {
                            logger.debug("importing: {}", sourceFile);

                            statusDialog.setStatusText("Importing " + filename + " ...");
                            FileUtils.copyProgress(sourceFile, targetDir, statusDialog);
                        }
                    }
                } catch (IOException e)
                {
                    return e.getMessage();
                }

                return null;
            }

            @Override
            protected void done()
            {
                String msg;
                try
                {
                    msg = get();
                } catch (InterruptedException e1)
                {
                    msg = null;
                } catch (ExecutionException e1)
                {
                    msg = e1.getCause().getMessage();
                }

                statusDialog.dispose();

                // If no errors occurred, then create a new layer from the imported shape file
                if (msg == null)
                {
                    // The restore state for this new layer is the relative path in VFS to the .shp file
                    String restorableState = targetDir + File.separator + basename + ".shp";
                    makeLayerFromShapeFile(restorableState);
                } else
                {
                    AlwaysOnTopJOptionPane.showErrorDialog(null, "Unable to import shape file: " + msg, "Error");
                }
            }
        }.execute();
    }

    /**
     * Create a new {@link IRaptorLayer} from the given shape file (as a relative path in VFS).
     *
     * @param shapeFilePath the path to the shape file to process
     */
    private void makeLayerFromShapeFile(String shapeFilePath)
    {
        logger.debug("makeLayerFromShapeFile: {}", shapeFilePath);

        // We need just the base name of the shape file from the path
        String basename = FileUtils.extractFileNameWithoutExtensionFromPath(shapeFilePath);

        LayerConfiguration layerConfiguration = ShapefileLayerBuilder.builder()
                .setName(shapeFilePath)
                .setDisplayName(basename)
                .setFile(new File(shapeFilePath))
                .build();
        layerService.addLayer(layerConfiguration);
    }
}
