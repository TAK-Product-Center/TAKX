package gov.takx.core.globe.layers;

import gov.nasa.worldwind.WorldWindow;
import gov.nasa.worldwind.avlist.AVKey;
import gov.nasa.worldwind.avlist.AVList;
import gov.nasa.worldwind.avlist.AVListImpl;
import gov.nasa.worldwind.data.BasicDataRasterReaderFactory;
import gov.nasa.worldwind.data.BufferWrapperRaster;
import gov.nasa.worldwind.data.BufferedImageRaster;
import gov.nasa.worldwind.data.DataRaster;
import gov.nasa.worldwind.data.DataRasterReader;
import gov.nasa.worldwind.data.DataRasterReaderFactory;
import gov.nasa.worldwind.data.GDALDataRaster;
import gov.nasa.worldwind.formats.gcps.GCPSReader;
import gov.nasa.worldwind.formats.tab.TABRasterReader;
import gov.nasa.worldwind.formats.tiff.GeotiffImageReaderSpi;
import gov.nasa.worldwind.geom.Angle;
import gov.nasa.worldwind.geom.Intersection;
import gov.nasa.worldwind.geom.LatLon;
import gov.nasa.worldwind.geom.Line;
import gov.nasa.worldwind.geom.Matrix;
import gov.nasa.worldwind.geom.Position;
import gov.nasa.worldwind.geom.Sector;
import gov.nasa.worldwind.geom.Vec4;
import gov.nasa.worldwind.globes.Globe;
import gov.nasa.worldwind.layers.Layer;
import gov.nasa.worldwind.layers.RenderableLayer;
import gov.nasa.worldwind.render.SurfaceImage;
import gov.nasa.worldwind.util.ImageUtil;
import gov.nasa.worldwind.util.RasterControlPointList;
import gov.nasa.worldwind.util.WWIO;
import gov.nasa.worldwind.util.WWUtil;
import gov.nasa.worldwind.view.BasicView;
import gov.nasa.worldwind.view.orbit.OrbitView;
import gov.takx.api.annotation.ProgramPreference;
import gov.takx.api.annotation.PseudoBean;
import gov.takx.api.annotation.RemoveObserver;
import gov.takx.api.annotation.VFSManagerScope;
import gov.takx.api.coordinatesystem.ILocation;
import gov.takx.api.event.project.ProjectClosing;
import gov.takx.api.filesystem.IVirtualFileSystem;
import gov.takx.api.filesystem.IVirtualFileSystemManager;
import gov.takx.api.filesystem.VFSScope;
import gov.takx.api.globe.IExtendedGlobeManager;
import gov.takx.api.layer.IRaptorLayer;
import gov.takx.api.platform.ICoreExecutorService;
import gov.takx.api.ui.IUserProxy;
import gov.takx.commons.file.FilePath;
import gov.takx.commons.file.FileUtils;
import gov.takx.commons.file.IFileProgress;
import gov.takx.commons.swing.ImageUtils;
import gov.takx.core.globe.layers.undo.SurfaceImageUndoHandler;
import gov.takx.platform.preferences.PreferenceEntry;
import gov.takx.platform.utils.WWUtils;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import jakarta.annotation.PostConstruct;
import jakarta.enterprise.event.Observes;
import jakarta.inject.Inject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.spi.IIORegistry;
import javax.imageio.stream.ImageInputStream;
import javax.swing.SwingUtilities;

import java.awt.geom.Point2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.lang.invoke.MethodHandles;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import static gov.takx.core.view.gui.maps.importers.SurfaceImageryFileImporter.GEO_FILE_EXTENSION;
import static gov.takx.core.view.gui.maps.importers.SurfaceImageryFileImporter.SURFACE_IMAGERY_DIR;

/**
 * Class for Surface Image layers. Surface images may be georeferenced by the user as a rubber sheeted image.
 * The position of the image on the globe will be saved in the database, and can also be exported in an XML
 * format. When a new image is loaded this class will search for a .rxg file (that is created by TAKX when
 * a surface image is exported), a World File, or a GCPS file to give the image an initial location on the map.
 *
 */
@SuppressWarnings("CdiManagedBeanInconsistencyInspection")
@PseudoBean
public class SurfaceImageLayer extends WWGlobeLayer
{
    private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    @Inject
    @ProgramPreference(PreferenceEntry.MAX_SURFACE_IMAGE_DIMENSION)
    private int maxDimension;

    @Inject
    @VFSManagerScope(VFSScope.Project)
    private IVirtualFileSystemManager virtualFileSystemManager;

    @Inject
    private ICoreExecutorService executorService;

    @Inject
    private IExtendedGlobeManager globeManager;

    @Inject
    private IUserProxy userProxy;

    private final BasicDataRasterReaderFactory factory = new BasicDataRasterReaderFactory();

    private SurfaceImage surfaceImage;
    private SurfaceImageController controller;
    private FilePath filePath;
    private File rootDir;
    private SurfaceImageUndoHandler undoHandler;

    private List<SurfaceImageLayerState.ControlPoint> controlPoints = new ArrayList<>();

    static
    {
        // Remove the default tiff reader.
        var registry = IIORegistry.getDefaultInstance();
        var tiffReader = registry.getServiceProviderByClass(GeotiffImageReaderSpi.class);
        if (tiffReader != null) registry.deregisterServiceProvider(tiffReader);
    }

    public SurfaceImageLayer(IRaptorLayer raptorLayer, Layer layer)
    {
        super(raptorLayer, layer);

        setSharable(true); // We are sharable

        if (logger.isDebugEnabled()) logger.debug("Creating surface image layer: {}", this);

        setName(raptorLayer.getName());
        setOpacity(raptorLayer.getOpacity());
        raptorLayer.setNetworkRetrievalEnabled(false);
        setEditable(raptorLayer.isEditable());
    }

    @PostConstruct
    private void postConstruct()
    {
        IVirtualFileSystem fileSystem = virtualFileSystemManager.getVirtualFileSystem(SURFACE_IMAGERY_DIR);
        rootDir = fileSystem.getRootDir();

        String restoreState = getRaptorLayer().getRestorableState();
        if (restoreState != null)
        {
            try
            {
                restoreStateInternal(restoreState);
            } catch (IOException e)
            {
                logger.error("Could not restore the state for a surface image layer", e);
            }
        }
    }

    /**
     * Copied from gov.nasa.worldwind.layers.SurfaceImageLayer
     */
    private static DataRaster openDataRaster(Object src, AVList params, DataRasterReaderFactory factory)
    {
        final File rasterFile = WWIO.getFileForLocalAddress(src);

        if (null == params)
        {
            params = new AVListImpl();
        }

        DataRaster raster;

        try
        {
            DataRasterReader reader = factory.findReaderFor(rasterFile, params);
            DataRaster[] rasters = reader.read(rasterFile, params);
            if (null == rasters || rasters.length == 0 || null == rasters[0])
            {
                logger.debug("Failed to read image {}", src);
                return null;
            }

            raster = rasters[0];

            if (raster.getSector() == null && params.hasKey(AVKey.SECTOR))
            {
                Object o = params.getValue(AVKey.SECTOR);
                if (o instanceof Sector sector)
                {
                    if (raster instanceof GDALDataRaster)
                    {
                        ((GDALDataRaster) raster).setSector(sector);
                    } else
                    {
                        raster.setValue(AVKey.SECTOR, sector);
                    }
                }
            }

            if (raster.getSector() == null)
            {
                logger.debug("Failed to read image {}: Spatial reference is null", src);
            }

            return raster;
        } catch (Throwable t)
        {
            if (logger.isDebugEnabled()) logger.debug(" Failed to read image {} : {}", src, WWUtil.extractExceptionReason(t));
        }

        return null;
    }

    public static File getGeoInformationFileName(File imageFile)
    {
        String newFileName = WWIO.replaceSuffix(imageFile.getName(), "." + GEO_FILE_EXTENSION);
        return new File(imageFile.getParent(), newFileName);
    }

    /**
     * Update the bounds of the image in the layer properties. Used by "Go To" feature.
     *
     * @since 1.14
     */
    public void updateSector()
    {
        setSectorLayerConstructionParameter(layer, surfaceImage.getSector());
    }

    /**
     * Are any undo operations queued?
     *
     * @return {@code true} if any undo operations are available
     */
    public boolean isUndoAvailable()
    {
        return undoHandler.isUndoAvailable();
    }

    /**
     * Apply the last recorded undo operation.
     */
    public void undoUserResize()
    {
        if (undoHandler.isUndoAvailable()) undoHandler.undo();
    }

    /**
     * Queue loading an image from the given path.
     *
     * @param filePath File path to the image file.
     * @param corners  The corner points of the image.
     * @since 5.7 (renamed from setImage)
     * @see #setImage(SurfaceImage)
     */
    private void queueImage(@Nonnull final String filePath, @Nullable final List<LatLon> corners)
    {
        queueImage(new FilePath(filePath), corners);
    }

    /**
     * Queue loading an image from the given path.
     *
     * @param filePath File path to the image file.
     * @param corners  The corner points of the image.
     * @since 5.7 (renamed from setImage)
     * @see #setImage(SurfaceImage)
     */
    private void queueImage(@Nonnull final FilePath filePath, @Nullable final List<LatLon> corners)
    {
        this.filePath = filePath;
        queueImage(getImageFile(), corners);
    }

    /**
     * Queue loading an image from the given path.
     *
     * @param file    File path to the image file.
     * @param corners The corner points of the image.
     * @since 5.7
     * @see #setImage(SurfaceImage)
     */
    private void queueImage(@Nonnull final File file, @Nullable final List<LatLon> corners)
    {
        logger.debug("Building surface image from {}", file);
        if (!file.exists())
        {
            logger.error("Cannot create image layer from path that does not exist: {}", file);
            return;
        }

        // Create the surface image on a different thread.  This is because the first call to "createSurfaceImage()"
        // Will initiate a GDAL library load in WorldWind, which takes several seconds.
        // When a project contains Surface Image Layer(s), this method will be called upon project startup.  We want
        // to avoid hanging the EDT during startup.
        executorService.execute(() -> {
            try
            {
                Thread.currentThread().setName("Surface Image Loader");
                SurfaceImage image = createSurfaceImage(file, corners);
                if (image != null)
                {
                    final SurfaceImage img = image;
                    SwingUtilities.invokeLater(() -> setImage(img));
                }
            } catch (IOException e)
            {
                logger.error("Unable to create surface image from: {}", file.getName(), e);
            }
        });
    }

    /**
     * If there is a controller, then disarm it and remove its listener registrations.
     */
    private void clearController()
    {
        if (controller != null)
        {
            controller.setArmed(false);
            controller.dispose();
            controller = null;
        }
    }

    @Override
    public String getRestorableState()
    {
        if (filePath != null && surfaceImage != null)
        {
            var state = new SurfaceImageLayerState(filePath.toString(), surfaceImage.getCorners(), getControlPoints());
            return state.buildRestorableState();
        }
        return "";
    }

    /**
     * Restore the layer from an XML string. The restorable state specifies the location of the image
     * file, any positions of the corners of the image, and any control points used to geo-rectify the image
     *
     * @param stateInXml Layer state in XML.
     * @throws RuntimeException if problems are encountered reading the image, or if the image
     *                          exceeds the maximum dimension.
     */
    @Override
    public void restoreState(String stateInXml)
    {
        try
        {
            restoreStateInternal(stateInXml);
        } catch (IOException e)
        {
            logger.warn("Unable to restore surface image layer", e);
        }
    }

    /**
     * Internal implementation of {@link #restoreState(String)} that can throw an IOException.
     * If a problem is encountered when the constructor calls restoreState(), this method
     * needs to be able to propagate an IOException past the constructor to prevent the layer
     * from being instantiated. GlobeLayerManager expects a {@code null} return value if instantiation
     * fails. See RPTR-2563.
     *
     * @param stateInXml Layer state in XML.
     * @throws IOException Thrown if there is a problem importing the image (e.g. image too large).
     */
    public void restoreStateInternal(String stateInXml)
            throws IOException
    {
        SurfaceImageLayerState state = SurfaceImageLayerState.createFromString(stateInXml);
        queueImage(state.filePath(), state.corners());
        controlPoints = new ArrayList<>(state.controlPoints()); // Need a mutable list
    }

    /**
     * @return Contents of the surface image. Can be {@code null} if the file could not be read.
     */
    @Nullable
    public BufferedImage getImage()
    {
        return readImage(getImageFile());
    }

    private void setImage(@Nonnull SurfaceImage image)
    {
        surfaceImage = image;
        RenderableLayer renderableLayer = (RenderableLayer) layer;
        renderableLayer.removeAllRenderables();
        renderableLayer.addRenderable(surfaceImage);

        clearController();

        controller = new SurfaceImageController(this, surfaceImage);

        undoHandler = new SurfaceImageUndoHandler(controller); // And construct an undo handler for this controller
    }

    /**
     * @return File path to the image file.
     * @since 5.7
     */
    @Nonnull
    public File getImageFile()
    {
        return new File(getImageFilePath());
    }

    /**
     * @return Absolute path to the image file.
     */
    @Nonnull
    public String getImageFilePath()
    {
        // As of 5.7 there is an option for keeping imagery files at their original locations
        // rather than copying them to the VFS. If this path is represented as an absolute one, then
        // it is in its original location and is not relative to the VFS root directory.
        String fullPath = filePath.toString();
        if (new File(fullPath).isAbsolute()) return fullPath;

        // For non-absolute paths, these are relative to the VFS root directory.
        return filePath.toSystemPath(rootDir);
    }

    /**
     * Method for transforming a georeferenced source image into a geographically aligned destination image.
     * The source image is georeferenced by either three or four control points. Each control point maps a location in
     * the source image to a geographic location.
     *
     * @param imagePoints three or four control points in the source image.
     * @param geoPoints   three or four geographic locations corresponding to each source control point.
     * @throws IllegalArgumentException if {@code imagePoints} or {@code geoPoints} is {@code null}, or if either
     *                                  {@code imagePoints} or {@code geoPoints} have length less than 3.
     */
    public void warpImageWithControlPoints(Point2D[] imagePoints, ILocation[] geoPoints)
    {
        if (geoPoints == null) throw new IllegalArgumentException("geoPoints array is null");
        LatLon[] latLon = new LatLon[geoPoints.length];

        int i = 0;
        for (ILocation point : geoPoints)
        {
            latLon[i] = WWUtils.locationToLatLon(point);
            i++;
        }

        warpImageWithControlPoints(imagePoints, latLon);
    }

    @Nullable
    private BufferedImage readImage(@Nonnull File file)
    {
        try
        {
            // Attempt to read with default Java image libs.
            ImageInputStream stream = ImageIO.createImageInputStream(file);
            Iterator<ImageReader> iterator = ImageIO.getImageReaders(stream);
            while (iterator.hasNext())
            {
                ImageReader ir = iterator.next();
                String name = ir.getClass().getName();
                if (name.equalsIgnoreCase("com.sun.media.imageioimpl.plugins.jpeg.CLibJPEGImageReader"))
                {
                    Object jpgReader = IIORegistry.getDefaultInstance().getServiceProviderByClass(ir.getClass());
                    if (jpgReader != null)
                    {
                        IIORegistry.getDefaultInstance().deregisterServiceProvider(jpgReader);
                    }
                }
            }
            stream.close();
            return ImageIO.read(file);
        } catch (Exception e)
        {
            //
        }

        try
        {
            // Attempt to read image with WW's data raster readers.
            DataRaster raster = openDataRaster(file.getPath(), null, factory);
            if (raster != null)
            {
                BufferedImage image = getBufferedImage(raster);
                if (image != null)
                {
                    return image;
                }
            }
        } catch (Exception e)
        {
            if (logger.isDebugEnabled()) logger.debug("Exception reading image", e);
        }

        return null;
    }

    /**
     * Create a new surface image, optionally with set corner points. If the image corners are not
     * specified, and a TAB file or GPCS file for the image is found, the new image will be created
     * using it's position info. Otherwise, the image will be created in the center of the viewport.
     *
     * @param file    File to create image from.
     * @param corners The corner points of the image.
     * @return The new surface image, or {@code null} if the file could not be read.
     * @throws IOException If an error occurs reading the image file, or if the image exceeds the maximum
     *                     dimension.
     */
    @Nullable
    private SurfaceImage createSurfaceImage(@Nonnull File file, @Nullable List<LatLon> corners) throws IOException
    {
        SurfaceImage si = null;

        BufferedImage image = readImage(file);
        if (image == null)
        {
            showGeneralImageError(file);
            return null;
        }

        // Check the image's dimensions after it has been created and resize if necessary.
        if (image.getWidth() > maxDimension || image.getHeight() > maxDimension)
        {
            /*
             * PLUGIN-3134:
             *
             * Automatically resize the image to an appropriate resolution. The original file is stored in the VFS to
             * maintain the best possible surface image quality if the program properties max dimension field is
             * changed to a different value.
             * @since 1.43
             */
            image = ImageUtils.getScaledInstanceRespectingAspectRatio(image, maxDimension, maxDimension);
            if (logger.isDebugEnabled())
            {
                logger.debug("Re-sized image to width={}, height={}", image.getWidth(), image.getHeight());
            }
        }

        // If corners were provided, create a new image using those corner points
        if (corners != null && !corners.isEmpty())
        {
            si = new SurfaceImage(image, corners);
        }

        // Otherwise look for a TAB file with geo information
        if (si == null)
        {
            File tabFile = getAssociatedTABFile(file);
            if (tabFile != null)
            {
                si = createSurfaceImageFromTABFile(image, tabFile);
            }
        }

        // Otherwise try for a GCPS file
        if (si == null)
        {
            File gcpsFile = getAssociatedGCPSFile(file);
            if (gcpsFile != null)
            {
                si = createSurfaceImageFromGCPSFile(image, gcpsFile);
            }
        }

        // If all else fails, just create an image in the center of the viewport
        // Use the viewport of the
        if (si == null)
        {
            si = createSurfaceImageFromViewport(image);
        }

        // Set the layer's sector bounds to enable the "Go To" feature.
        setSectorLayerConstructionParameter(layer, si.getSector());

        return si;
    }

    private void showGeneralImageError(File file)
    {
        userProxy.reportError("Unable to open image", "Unable to open image " + file.getName(), null);
    }

    private SurfaceImage createSurfaceImageFromViewport(BufferedImage image)
    {
        // Use the viewport of the last focused globe, since it's the best we can do
        final WorldWindow wwd = globeManager.getLastFocusedGlobe().getWorldWindow();

        final Position position = getNewShapePosition(wwd);
        final Angle heading = getNewShapeHeading(wwd, false);
        final double sizeInMeters = getViewportScaleFactor(wwd);

        List<LatLon> corners = createSquareInViewport(wwd, position, heading, sizeInMeters);

        return new SurfaceImage(image, corners);
    }

    private SurfaceImage createSurfaceImageFromTABFile(BufferedImage image, File tabFile)
            throws java.io.IOException
    {
        TABRasterReader reader = new TABRasterReader();
        RasterControlPointList controlPoints = reader.read(tabFile);

        return createSurfaceImageFromControlPoints(image, controlPoints);
    }

    private SurfaceImage createSurfaceImageFromGCPSFile(BufferedImage image, File gcpsFile)
            throws java.io.IOException
    {
        GCPSReader reader = new GCPSReader();
        RasterControlPointList controlPoints = reader.read(gcpsFile);

        return createSurfaceImageFromControlPoints(image, controlPoints);
    }

    private SurfaceImage createSurfaceImageFromControlPoints(BufferedImage image, RasterControlPointList controlPoints)
    {
        int numControlPoints = controlPoints.size();
        Point2D[] imagePoints = new Point2D[numControlPoints];
        LatLon[] geoPoints = new LatLon[numControlPoints];

        for (int i = 0; i < numControlPoints; i++)
        {
            RasterControlPointList.ControlPoint p = controlPoints.get(i);
            imagePoints[i] = p.getRasterPoint();
            geoPoints[i] = p.getWorldPointAsLatLon();
        }

        BufferedImage destImage = new BufferedImage(image.getWidth(), image.getHeight(), BufferedImage.TYPE_INT_ARGB);
        Sector sector = ImageUtil.warpImageWithControlPoints(image, imagePoints, geoPoints, destImage);

        return new SurfaceImage(destImage, sector);
    }

    private File getAssociatedTABFile(File file)
    {
        File tabFile = TABRasterReader.getTABFileFor(file);
        if (tabFile != null && tabFile.exists())
        {
            TABRasterReader reader = new TABRasterReader();
            if (reader.canRead(tabFile))
            {
                return tabFile;
            }
        }

        return null;
    }

    private File getAssociatedGCPSFile(File file)
    {
        File gcpsFile = GCPSReader.getGCPSFileFor(file);
        if (gcpsFile != null && gcpsFile.exists())
        {
            GCPSReader reader = new GCPSReader();
            if (reader.canRead(gcpsFile))
            {
                return gcpsFile;
            }
        }

        return null;
    }

    /**
     * Transform an image using an array of WorldWind LatLon objects.
     *
     * @param imagePoints three or four control points in the source image.
     * @param geoPoints   three or four geographic locations corresponding to each source control point.
     * @throws IllegalArgumentException if {@code imagePoints} or {@code geoPoints} is {@code null}, or if either
     *                                  {@code imagePoints} or {@code geoPoints} have length less than 3.
     */
    private void warpImageWithControlPoints(Point2D[] imagePoints, LatLon[] geoPoints)
    {
        File source = getImageFile();
        BufferedImage sourceImage = readImage(source);
        if (sourceImage == null)
        {
            logger.error("Image control point warp failed, cannot read image from source path: {}", source);
            return;
        }

        Matrix gridToGeographic = Matrix.fromImageToGeographic(imagePoints, geoPoints);
        List<LatLon> corners = ImageUtil.computeImageCorners(sourceImage.getWidth(), sourceImage.getHeight(), gridToGeographic);

        queueImage(filePath, corners);
    }

    /**
     * Copied from gov.nasa.worldwind.layers.SurfaceImageLayer
     */
    private BufferedImage getBufferedImage(DataRaster raster)
    {
        if (null == raster)
        {
            return null;
        }

        if (raster instanceof GDALDataRaster)
        {
            AVList params = new AVListImpl();

            params.setValue(AVKey.WIDTH, raster.getWidth());
            params.setValue(AVKey.HEIGHT, raster.getHeight());
            params.setValue(AVKey.SECTOR, raster.getSector());

            raster = raster.getSubRaster(params);
        }

        if (raster instanceof BufferedImageRaster)
        {
            return ((BufferedImageRaster) raster).getBufferedImage();
        }

        if (raster instanceof BufferWrapperRaster)
        {
            return ImageUtil.visualize((BufferWrapperRaster) raster);
        }

        return null;
    }

    @Override
    public void setEditable(boolean editable)
    {
        setPickEnabled(editable);

        if (controller != null)
        {
            controller.setArmed(editable);
        }
    }

    @Override
    public void setEnabled(boolean enabled)
    {
        if (!enabled && controller != null)
        {
            controller.setArmed(false);
        }
        super.setEnabled(enabled);
    }

    public void export(File destination, boolean createGeoFile, IFileProgress progress) throws IOException
    {
        File source = getImageFile();

        if (progress != null)
        {
            FileUtils.copyProgress(source, destination, progress);
        } else
        {
            FileUtils.copy(source, destination);
        }

        if (createGeoFile)
        {
            File geoInfo = getGeoInformationFileName(destination);

            var state = new SurfaceImageLayerState(destination.getName(), surfaceImage.getCorners(), controlPoints);
            Files.writeString(geoInfo.toPath(), state.buildRestorableState());
        }
    }

    private double getViewportScaleFactor(@Nonnull WorldWindow wwd)
    {
        BasicView view = (BasicView) wwd.getView();

        // Get zoom if we have an OrbitView, otherwise use eye position elevation
        double zoomOrElevation = (view instanceof OrbitView)
                ? ((OrbitView) view).getZoom()
                : view.getEyePosition().elevation;

        return zoomOrElevation / 8.0;
    }

    private Position getNewShapePosition(@Nonnull WorldWindow wwd)
    {
        Line ray = new Line(wwd.getView().getEyePoint(), wwd.getView().getForwardVector());
        Intersection[] intersection = wwd.getSceneController().getTerrain().intersect(ray);

        if (intersection != null && intersection.length != 0)
        {
            return wwd.getModel().getGlobe().computePositionFromPoint(intersection[0].getIntersectionPoint());
        } else if (wwd.getView() instanceof BasicView)
        {
            return ((BasicView) wwd.getView()).getCenterPosition();
        }

        return Position.ZERO;
    }

    private Angle getNewShapeHeading(WorldWindow wwd, boolean matchViewHeading)
    {
        if (matchViewHeading)
        {
            if (wwd == null) throw new IllegalArgumentException("matchViewHeading == true requires non-null wwd");

            return wwd.getView().getHeading();
        }

        return Angle.ZERO;
    }

    private List<LatLon> createSquareInViewport(@Nonnull WorldWindow wwd, @Nonnull Position position,
                                                @Nonnull Angle heading, double sizeInMeters)
    {
        Globe globe = wwd.getModel().getGlobe();
        Matrix transform = Matrix.IDENTITY;
        transform = transform.multiply(globe.computeModelCoordinateOriginTransform(position));
        transform = transform.multiply(Matrix.fromRotationZ(heading.multiply(-1)));

        double widthOver2 = sizeInMeters / 2.0;
        double heightOver2 = sizeInMeters / 2.0;
        Vec4[] points = {
                new Vec4(-widthOver2, -heightOver2, 0.0).transformBy4(transform), // lower left
                new Vec4(widthOver2, -heightOver2, 0.0).transformBy4(transform), // lower right
                new Vec4(widthOver2, heightOver2, 0.0).transformBy4(transform), // upper right
                new Vec4(-widthOver2, heightOver2, 0.0).transformBy4(transform)  // upper left
        };

        LatLon[] locations = new LatLon[points.length];
        for (int i = 0; i < locations.length; i++)
        {
            locations[i] = new LatLon(globe.computePositionFromPoint(points[i]));
        }

        return Arrays.asList(locations);
    }

    /**
     * Ensure that our observation is removed when the layer is disposed.
     *
     * @since 21.37
     */
    @RemoveObserver
    @Override
    public void dispose()
    {
        super.dispose();
    }

    /**
     * Clean up our undo handler when the project is closing.
     *
     * @since 2.37
     */
    private void onProjectClosing(@SuppressWarnings("unused") @Observes ProjectClosing event)
    {
        if (undoHandler != null)
        {
            undoHandler.shutdown();
            undoHandler = null;
        }

        clearController();
    }

    /**
     * @return the list of persistent georectified control points
     *
     * @since 5.1.0
     */
    public List<SurfaceImageLayerState.ControlPoint> getControlPoints()
    {
        return Collections.unmodifiableList(controlPoints);
    }

    /**
     * Set the control points using the correlated {@code points} and {@code locations} lists.
     *
     * @param points the list of screen points
     * @param locations the list of geographic locations associated with each screen point
     *
     * @since 5.1.0
     */
    public void setControlPoints(List<Point2D> points, List<ILocation> locations)
    {
        var pointIt = points.iterator();
        var locationIt = locations.iterator();

        controlPoints.clear();

        while (pointIt.hasNext() && locationIt.hasNext())
        {
            controlPoints.add(new SurfaceImageLayerState.ControlPoint(pointIt.next(), locationIt.next()));
        }
    }
}
