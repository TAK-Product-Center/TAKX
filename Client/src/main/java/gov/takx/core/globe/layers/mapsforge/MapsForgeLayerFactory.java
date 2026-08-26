package gov.takx.core.globe.layers.mapsforge;

import gov.takx.api.annotation.VFSManagerScope;
import gov.takx.api.configuration.IProgramEnvironment;
import gov.takx.api.filesystem.IVirtualFileSystem;
import gov.takx.api.filesystem.IVirtualFileSystemManager;
import gov.takx.api.filesystem.VFSScope;
import gov.takx.api.layer.IRaptorLayer;
import gov.takx.api.layer.IRaptorLayerManager;
import gov.takx.api.layer.LayerGroup;
import gov.takx.commons.file.FileUtils;
import gov.takx.core.globe.layers.LayerImportParameters;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.SwingUtilities;

import java.io.File;
import java.io.IOException;
import java.lang.invoke.MethodHandles;

/**
 * Factory service to generate {@link MapsForgeGlobeLayer} instances.
 *
 * @since 2.0.0
 */
@ApplicationScoped
public class MapsForgeLayerFactory
{
    private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    // The directory in VFS where we store the imported map files
    private static final String MAPS_FILE_DIR = "MapFiles";

    private final IProgramEnvironment programEnvironment;
    private final IRaptorLayerManager layerManager;
    private final IVirtualFileSystemManager projectVFSManager;

    @Inject
    public MapsForgeLayerFactory(IProgramEnvironment programEnvironment, IRaptorLayerManager layerManager,
                                 @VFSManagerScope(VFSScope.Project) IVirtualFileSystemManager projectVFSManager)
    {
        this.programEnvironment = programEnvironment;
        this.layerManager = layerManager;
        this.projectVFSManager = projectVFSManager;
    }

    /**
     * Create a new {@link MapsForgeGlobeLayer} with a {@link MapsforgeMapFileLayer} as its underlying WW layer.
     *
     * @param raptorLayer     The raptor layer we represent
     * @param restorableState The restorable state for the {@link MapsforgeMapFileLayer}
     */
    public MapsForgeGlobeLayer makeGlobeLayer(IRaptorLayer raptorLayer, String restorableState)
    {
        final MapsforgeMapFileLayer mapFileLayer = MapsforgeMapFileLayer.makeLayer(getMapFile(restorableState, raptorLayer.isPermanent()));
        return new MapsForgeGlobeLayer(raptorLayer, mapFileLayer);
    }

    /**
     * Construct a new Globe layer that renders the map data in the given file.  This method should never be
     * called on the EDT since it performs potentially long-running file operations.
     *
     * @param mapFile Map file to process
     * @param importParameters Layer import parameters.
     */
    public void makeLayerFromMapFile(File mapFile, LayerImportParameters importParameters)
    {
        if (SwingUtilities.isEventDispatchThread())
        {
            throw new IllegalStateException("Never call makeLayerFromMapFile on the EDT");
        }

        try
        {
            String restorableState = importParameters.copyToVfs() ? copyFileToVFS(mapFile) : mapFile.getAbsolutePath();

            final String name = mapFile.getName(); // Original name (as imported by user)

            layerManager.addLayer(name, name, MapsforgeMapFileLayer.class.getName(), true, false, null,
                    LayerGroup.IMAGERY, null, restorableState, false);
        } catch (Exception e)
        {
            logger.error("Unable to import {}", mapFile, e);

            // Drill down to the original exception to get it's message.
            Throwable cause = e;
            while (cause.getCause() != null)
            {
                cause = cause.getCause();
            }

            if (e != cause) logger.error("Root cause: {}", cause.getMessage());
        }
    }

    /**
     * Determine the file path to the data, whether it be in the VFS or stored externally.
     *
     * @param filePath File path. Can be a single element / file name if content is stored in the VFS.
     *                 Otherwise, if the path is absolute then it is an external file not stored in the VFS.
     * @return File path.
     */
    private File getMapFile(String filePath, boolean isPermanent)
    {
        // Absolute path, keep as-is.
        final File initialPath = new File(filePath);
        if (initialPath.isAbsolute()) return initialPath;

        // Relative file path, it should be found in the VFS.
        final File root = isPermanent
                ? new File(programEnvironment.getGlobeLayersDirectoryPath())
                : projectVFSManager.getVirtualFileSystem(MAPS_FILE_DIR).getRootDir();

        return new File(root, filePath);
    }

    /**
     * Copy the given file into our VFS store.  Return the name of the created file, which may have been made unique
     * by {@link FileUtils#getUniqueFileName(File, File)}.
     *
     * @param mapFile Map file to import
     * @return Name of imported file
     * @throws IOException If file copy fails
     */
    private String copyFileToVFS(File mapFile) throws IOException
    {
        final IVirtualFileSystem fileSystem = projectVFSManager.getVirtualFileSystem(MAPS_FILE_DIR);

        File target = FileUtils.getUniqueFileName(fileSystem.getRootDir(), mapFile);

        FileUtils.copyProgress(mapFile, target, null);

        return target.getName();
    }
}
