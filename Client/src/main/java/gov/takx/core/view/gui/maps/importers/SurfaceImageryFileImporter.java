package gov.takx.core.view.gui.maps.importers;

import gov.nasa.worldwind.avlist.AVKey;
import gov.nasa.worldwind.avlist.AVList;
import gov.nasa.worldwind.data.BasicDataRasterReaderFactory;
import gov.nasa.worldwind.data.DataRasterReader;
import gov.nasa.worldwind.data.GDALDataRasterReader;
import gov.nasa.worldwind.geom.Sector;
import gov.nasa.worldwind.util.WWIO;
import gov.takx.api.annotation.PseudoBean;
import gov.takx.api.annotation.VFSManagerScope;
import gov.takx.api.filesystem.IVirtualFileSystem;
import gov.takx.api.filesystem.IVirtualFileSystemManager;
import gov.takx.api.filesystem.VFSScope;
import gov.takx.commons.file.FileUtils;
import gov.takx.core.globe.layers.LayerImportParameters;
import gov.takx.core.globe.layers.SurfaceImageLayerState;
import jakarta.annotation.Nonnull;
import jakarta.inject.Inject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.lang.invoke.MethodHandles;
import java.util.List;

/**
 * Support tool to copy an image file into VFS and construct the restorable state a layer would need to display it.
 *
 * @since 2.35
 */
@PseudoBean
public class SurfaceImageryFileImporter
{
    private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    public static final String SURFACE_IMAGERY_DIR = "SurfaceImagery";
    public static final String GEO_FILE_EXTENSION = "rxg";

    @Inject
    @VFSManagerScope(VFSScope.Project)
    private IVirtualFileSystemManager virtualFileSystemManager;

    private final LayerImportParameters importParameters;

    /**
     * @param importParameters Layer import parameters.
     * @since 5.7
     */
    public SurfaceImageryFileImporter(@Nonnull LayerImportParameters importParameters)
    {
        this.importParameters = importParameters;
    }

    /**
     * Add a surface imagery file to the project file system. If there is a .rxg file alongside the image,
     * then the geo points of the image corners will be read from this file.
     *
     * @param file Image file to add.
     * @return Restorable state string that represents the new image and it's position on the map.
     * @throws IOException If the file can not be opened.
     */
    public String importSurfaceImageryFile(File file) throws IOException
    {
        String importPath;
        if (importParameters.copyToVfs())
        {
            IVirtualFileSystem fileSystem = virtualFileSystemManager.getVirtualFileSystem(SURFACE_IMAGERY_DIR);
            File target = FileUtils.getUniqueFileName(fileSystem.getRootDir(), file);

            FileUtils.copyProgress(file, target, null);
            importPath = target.getName();
        } else
        {
            importPath = file.getAbsolutePath();
        }

        File geoFile = getAssociatedRaptorGeoFile(file);
        if (geoFile != null)
        {
            return SurfaceImageLayerState.createFromFile(geoFile).buildRestorableState();
        } else
        {
            // Attempt to extract the image corners using WW's data raster readers
            try
            {
                BasicDataRasterReaderFactory factory = new BasicDataRasterReaderFactory();
                DataRasterReader reader = factory.findReaderFor(file, null);
                if (reader != null)
                {
                    AVList metadata = reader.readMetadata(file, null);
                    if (metadata.hasKey(AVKey.SECTOR))
                    {
                        Sector sector = (Sector) metadata.getValue(AVKey.SECTOR);
                        return new SurfaceImageLayerState(importPath, List.of(sector.getCorners())).buildRestorableState();
                    }
                }
            } catch (Exception e)
            {
                if (logger.isDebugEnabled()) logger.debug("Unable to get raster sector", e);
            }
        }

        return new SurfaceImageLayerState(importPath).buildRestorableState();
    }

    /**
     * CORE-3462
     * checks to see if the file is a valid geotiff image for import
     *
     * @param file selected tif file
     * @return false if the file holds elevation data, true otherwise
     */
    public static boolean isValidImageGeotiffFile(File file)
    {
        if (getAssociatedRaptorGeoFile(file) != null)
        {
            return true;
        }
        try
        {
            GDALDataRasterReader gdalDataRasterReader = new GDALDataRasterReader();
            AVList metadata = gdalDataRasterReader.readMetadata(file, null);
            return metadata.getValue(AVKey.PIXEL_FORMAT) != AVKey.ELEVATION;
        } catch (Exception e)
        {
            if (logger.isDebugEnabled()) logger.debug("exception validating geotiff file {}", e.getMessage());
        }
        return true;
    }

    private static File getAssociatedRaptorGeoFile(File file)
    {
        File parent = file.getParentFile();
        if (parent == null)
        {
            return null;
        }

        String geoFilename = WWIO.replaceSuffix(file.getName(), "." + GEO_FILE_EXTENSION);

        if (file.getName().equalsIgnoreCase(geoFilename))
        {
            return null;
        }

        // Find the first sibling with the matching filename, and RXG extension.
        File[] children = parent.listFiles();
        if (children != null)
        {
            for (java.io.File child : children)
            {
                if (!child.equals(file) && child.getName().equalsIgnoreCase(geoFilename))
                {
                    return child;
                }
            }
        }

        return null;
    }
}
