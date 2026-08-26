package gov.takx.core.structures;

import com.jogamp.opengl.GL2;
import com.jogamp.opengl.GL2ES1;
import gov.nasa.worldwind.Disposable;
import gov.nasa.worldwind.Locatable;
import gov.nasa.worldwind.avlist.AVKey;
import gov.nasa.worldwind.avlist.AVList;
import gov.nasa.worldwind.avlist.AVListImpl;
import gov.nasa.worldwind.cache.BasicMemoryCache;
import gov.nasa.worldwind.cache.MemoryCache;
import gov.nasa.worldwind.geom.Angle;
import gov.nasa.worldwind.geom.LatLon;
import gov.nasa.worldwind.geom.Position;
import gov.nasa.worldwind.geom.Sector;
import gov.nasa.worldwind.globes.Globe;
import gov.nasa.worldwind.layers.BasicTiledImageLayer;
import gov.nasa.worldwind.layers.Layer;
import gov.nasa.worldwind.pick.PickSupport;
import gov.nasa.worldwind.render.DrawContext;
import gov.nasa.worldwind.render.Renderable;
import gov.nasa.worldwind.util.BasicQuadTree;
import gov.nasa.worldwind.util.measure.LengthMeasurer;
import gov.takx.api.annotation.PseudoBean;
import gov.takx.api.annotation.VFSManagerScope;
import gov.takx.api.filesystem.IVirtualFileSystem;
import gov.takx.api.filesystem.IVirtualFileSystemManager;
import gov.takx.api.filesystem.VFSScope;
import gov.takx.api.globe.IExtendedGlobeManager;
import gov.takx.api.structures.ISurfaceStructure;
import gov.takx.commons.file.FileUtils;
import gov.takx.commons.utils.StringUtils;
import gov.takx.core.globe.imagerywizard.FileStoreUtil;
import gov.takx.core.view.gui.screenimageutils.SnapshotUtils;
import jakarta.inject.Inject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Element;

import java.awt.AlphaComposite;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Composite;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.geom.GeneralPath;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.lang.invoke.MethodHandles;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

/**
 * @since 3.4 (renamed from RaptorCachedSurfaceStructureLayer)
 */
@PseudoBean
public class ApplicationCachedSurfaceStructureLayer extends CachedSurfaceStructureLayer
{
    private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    private static final SurfaceStructureCache DEFAULT_SURFACE_STRUCTURE_CACHE = new SurfaceStructureCache();

    private static final String STRUCTURE_LAYER_IMAGERY = "StructureLayerImagery";

    private static final int DEFAULT_DEPTH = 8;
    private static final double MAX_PITCH_DEGREES = 45;

    @Inject
    private SnapshotUtils snapshotUtils;

    @Inject
    private IExtendedGlobeManager globeManager;

    @Inject
    @VFSManagerScope(VFSScope.Project)
    private IVirtualFileSystemManager vfsManager;

    protected BasicQuadTree<ISurfaceStructure> extentTree = null;
    protected final List<ISurfaceStructure> tempRenderableList = new ArrayList<>();
    protected SurfaceStructureCache surfaceStructureCache = DEFAULT_SURFACE_STRUCTURE_CACHE;

    protected PickSupport pickSupport = new PickSupport();
    private File imageFolder = null;

    // Bounding sector is updated as renderables are added to this layer.  Since there are no renderables on
    // on construction, it defaults to null (originalSector is used during the interim).
    protected Sector boundingSector = null;

    protected int lastVisibleRenderables = 0;

    protected Layer previewImageLayer = null;

    /**
     * Constructs a layer instance.
     *
     * @throws IllegalArgumentException if the coverage sector is {@code null}.
     */
    public ApplicationCachedSurfaceStructureLayer()
    {
        super(Sector.EMPTY_SECTOR);

        //noinspection AssignmentToSuperclassField - we have our own extentTree
        super.extentTree = null;
    }

    public ApplicationCachedSurfaceStructureLayer(String previewLayerRestorableState)
    {
        this();
        previewImageLayer = new StructurePreviewLayer(previewLayerRestorableState);
    }

    /**
     * Indicates whether the layer contains Renderables.
     *
     * @return true if the layer contains Renderables, otherwise false.
     */
    @Override
    public boolean hasItems()
    {
        return extentTree != null && extentTree.hasItems();
    }

    /**
     * Add a surface structure to this layer.
     *
     * @param structure Surface structure to add
     * @since 3.0
     */
    @Override
    public void add(ISurfaceStructure structure)
    {
        // Why are we using a temporary renderable list?  Because we need to know the bounding sector of all the surface
        // structures BEFORE we instantiate the quad tree.  When adding has been completed, the batchAddDone() method
        // should be called.
        tempRenderableList.add(structure);
    }

    /**
     * Override our base class' implementation to prevent raw renderables from being added.
     */
    @Override
    public void add(Renderable item, String name)
    {
        logger.error("addition of Renderable is not supported: {}", item.getClass(), new RuntimeException());
    }

    /**
     * This method should be called after the calling class has completed adding structures to this layer.
     * It calculates the bounding sector of the structures and adds all the structures into a new extent tree.
     */
    @Override
    public void batchAddDone()
    {
        calculateBoundingSector();

        // If add/doneAdding() is used multiple times, extentTree will not be null.  In this case, add all the current
        // items in the extent tree to the temp list so that they will be added to the new extentTree
        if (extentTree != null)
        {
            for (ISurfaceStructure structure : extentTree)
            {
                tempRenderableList.add(structure);
            }
            extentTree.clear();
        }

        Sector bs = boundingSector != null ? boundingSector : Sector.EMPTY_SECTOR;

        extentTree = new BasicQuadTree<>(DEFAULT_DEPTH, bs, null);

        for (ISurfaceStructure structure : tempRenderableList)
        {
            extentTree.add(structure, structure.getSector().asDegreesArray(), structure.getUUID().toString());
        }

        tempRenderableList.clear();

        if (previewImageLayer == null)
        {
            captureExtrudedPolygonImages();
        }
    }

    /**
     * Get the image folder file of the 2D rendered structure images, if one exists.
     *
     * @return the image folder file.
     */
    public File getImageFolder()
    {
        return imageFolder;
    }

    @Override
    public void setEnabled(boolean enabled)
    {
        super.setEnabled(enabled);
        if (previewImageLayer != null)
        {
            previewImageLayer.setEnabled(enabled);
        }
    }

    @Override
    public void setMinActiveAltitude(double minActiveAltitude)
    {
        super.setMinActiveAltitude(minActiveAltitude);
        if (previewImageLayer != null)
        {
            previewImageLayer.setMinActiveAltitude(minActiveAltitude);
        }
    }

    @Override
    public void setMaxActiveAltitude(double maxActiveAltitude)
    {
        super.setMaxActiveAltitude(maxActiveAltitude);
        if (previewImageLayer != null)
        {
            previewImageLayer.setMaxActiveAltitude(maxActiveAltitude);
        }
    }

    /**
     * Create a local raster server layer (structure preview layer) from its creation config and parameters.
     *
     * @param domElement creation document element
     * @param params     creation parameters
     */
    public void createPreviewImageLayer(Element domElement, AVList params)
    {
        previewImageLayer = new StructurePreviewLayer(domElement, params);
        previewImageLayer.setEnabled(true);
        FileStoreUtil.setInstalledDataID(params, domElement, previewImageLayer);
        previewImageLayer.setNetworkRetrievalEnabled(false);
    }

    /**
     * If the preview image layer exists, call its render method.  Then render this layer.
     *
     * @param dc draw context of current globe view.
     */
    @Override
    public void render(DrawContext dc)
    {
        if (previewImageLayer != null)
        {
            previewImageLayer.render(dc);
        }
        super.render(dc);
    }

    @Override
    public boolean isLayerActive(DrawContext dc)
    {
        return doIsLayerActive(dc);
    }

    @Override
    public String getRestorableState()
    {
        if (boundingSector == null)
        {
            return null;
        }
        double[] boundingSectorArray = boundingSector.asDegreesArray();
        Double[] boundingSectorDblArray = new Double[boundingSectorArray.length];
        for (int i = 0; i < boundingSectorArray.length; i++) boundingSectorDblArray[i] = boundingSectorArray[i];
        String rs = StringUtils.join(boundingSectorDblArray) + "," + true;
        if (previewImageLayer != null)
        {
            rs += "," + previewImageLayer.getRestorableState();
        }
        return rs;
    }

    /**
     * Determines, based on the draw context, whether this layer is active.  Modified from the original isLayerActive
     * method, in that it is hard coded to return false when the altitude is greater than 1500m or if the pitch
     * is greater than 45 degrees.
     *
     * @param dc draw context of current globe view.
     * @return {@code true} if the layer is active, or {@code false} otherwise.
     */
    protected boolean doIsLayerActive(DrawContext dc)
    {
        final Position eyePos = dc.getView().getEyePosition();
        if (eyePos == null) return false;

        double altitude = eyePos.getElevation();
        return altitude >= getMinActiveAltitude() && altitude <= 1500 && !isGreaterThanMaxPitch(dc);
    }

    /**
     * Calculates a bounding sector for all surface structures in the temp renderable list.  This is called after
     * items have been added or removed from this layer.
     */
    protected void calculateBoundingSector()
    {
        if (tempRenderableList.isEmpty()) return;

        final List<Sector> sectors = tempRenderableList.stream()
                .map(ISurfaceStructure::getSector)
                .collect(Collectors.toList());

        boundingSector = Sector.union(sectors);

        AVList params = (AVList) getValue(AVKey.CONSTRUCTION_PARAMETERS);
        if (params == null)
        {
            params = new AVListImpl();
            setValue(AVKey.CONSTRUCTION_PARAMETERS, params);
        }
        params.setValue(AVKey.SECTOR, boundingSector);
    }

    /**
     * Remove by item is currently disabled.
     *
     * @param item the Renderable to remove
     * @see #removeByName(String)
     */
    @Override
    public void remove(Renderable item)
    {
        logger.warn("Remove is disabled in this class - use removeByName instead.");
    }

    /**
     * Remove a named Renderable from the layer if it is in the layer.
     * <p/>
     * Note that if this layer has a preview imagery layer, that preview layer will remain unaffected.
     *
     * @param name the name of the Renderable to remove. If {@code null}, no Renderable is removed.
     * @see #remove(gov.nasa.worldwind.render.Renderable)
     */
    @Override
    public void removeByName(String name)
    {
        if (extentTree != null)
        {
            extentTree.removeByName(name);
        }
    }

    /**
     * Returns all Renderables at a specified location.
     *
     * @param location the location of interest.
     * @return the Collection of Renderables at the specified location.
     * @see #getStructures(gov.nasa.worldwind.geom.Sector)
     * @see #getAllStructures()
     */
    @Override
    public Collection<? extends ISurfaceStructure> getStructures(LatLon location)
    {
        return extentTree != null
                ? forceLoadRenderables(extentTree.getItemsAtLocation(location, null))
                : Collections.emptyList();
    }

    /**
     * Returns all Renderables within or intersecting a specified sector.
     *
     * @param extent the location of interest.
     * @return the Collection of Renderables within or intersecting the boundary of the sector.
     * @see #getStructures(gov.nasa.worldwind.geom.LatLon)
     * @see #getAllStructures()
     */
    @Override
    public Collection<? extends ISurfaceStructure> getStructures(Sector extent)
    {
        return extentTree != null
                ? forceLoadRenderables(extentTree.getItemsInRegion(extent, null))
                : Collections.emptyList();
    }

    /**
     * Returns all Renderables in the layer.
     *
     * @return an Iterable over all the Renderables in the layer.
     */
    @Override
    public Collection<? extends ISurfaceStructure> getAllStructures()
    {
        if (extentTree != null)
        {
            return StreamSupport.stream(forceLoadRenderables(extentTree).spliterator(), false)
                    .collect(Collectors.toList());
        } else
        {
            return Collections.emptyList();
        }
    }

    /**
     * Process surface structures and force load the associated renderable.
     *
     * @param structures surface structures for which to load the renderables
     * @return structures
     * @since 3.0
     */
    private <T extends Iterable<ISurfaceStructure>> T forceLoadRenderables(T structures)
    {
        structures.forEach(structure -> surfaceStructureCache.get(structure));

        return structures;
    }

    /**
     * Unwrap the Renderable object from a surface structure, by querying the surface structure cache.
     *
     * @param ss a surface structure
     * @return the Renderable object from the surface structure.
     */
    private Renderable unwrapRenderable(ISurfaceStructure ss)
    {
        return surfaceStructureCache.get(ss);
    }

    @Override
    public ISurfaceStructure getStructureByName(String name)
    {
        return extentTree != null ? extentTree.getByName(name) : null;
    }

    /**
     * Disposes any Renderables implementing @{link Dispose} and removes all Renderables from the layer.
     */
    @Override
    public void dispose()
    {
        disposeRenderables();
        surfaceStructureCache.flushCache();
    }

    /**
     * @return geographic area covered by this layer's renderables
     */
    @Override
    public Sector getSector()
    {
        return boundingSector == null ? Sector.EMPTY_SECTOR : boundingSector;
    }

    /**
     * Call dispose on all the disposable objects in this layer's quad tree.  Then flush out all internal data.
     */
    @Override
    protected void disposeRenderables()
    {
        for (Disposable disposable : extentTree)
        {
            try
            {
                disposable.dispose();
            } catch (Exception ignore)
            {
            }
        }

        tempRenderableList.clear();
        previewImageLayer = null;
        extentTree.clear();
    }

    /**
     * Get the actual renderables (not surface structures) in the given sector.
     *
     * @param sector Sector of interest
     * @return a collection of visible renderables.
     */
    @Override
    public Collection<? extends Renderable> getRenderables(Sector sector)
    {
        return getStructures(sector).stream()
                .map(this::unwrapRenderable)
                .collect(Collectors.toList());
    }

    /**
     * Disabled - none of the objects in this layer are PreRenderables
     */
    @Override
    protected void doPreRender(DrawContext dc)
    {
    }

    /**
     * Create a multi-image 2D representation of the extent of this structure layer.  Each structure is rendered to
     * GeoTIFFs using regular Java2D and stored in an image folder based upon the structure file's name.
     */
    protected void captureExtrudedPolygonImages()
    {
        Sector boundingSector = getSector();
        if (boundingSector.equals(Sector.EMPTY_SECTOR))
        {
            return;
        }
        Sector[] sectors = divideSectorIntoSquares(boundingSector, 1000);

        for (int i = 0; i < sectors.length; i++)
        {
            Sector sector = sectors[i];
            Collection<? extends Renderable> visibleRenderables = getRenderables(sector);
            if (!visibleRenderables.isEmpty())
            {
                BufferedImage image = new BufferedImage(512, 512, BufferedImage.TYPE_INT_ARGB);
                for (Renderable r : visibleRenderables)
                {
                    if (r instanceof SSExtrudedPolygon)
                    {
                        drawExtrudedPolygon(sector, (SSExtrudedPolygon) r, image);
                    }
                }
                try
                {
                    String name = getName();
                    if (StringUtils.isEmpty(name))
                    {
                        name = String.valueOf(System.currentTimeMillis());
                    }

                    if (imageFolder == null)
                    {
                        imageFolder = createTempImageFolder(name + "_geoTiffs");
                    }
                    if (imageFolder == null)
                    {
                        throw new IOException("Failed to create a temporary image folder for structure layer.");
                    }

                    File epFile = new File(imageFolder.getPath(), "ep_" + i + ".tif");
                    if (logger.isDebugEnabled()) logger.debug("writing GeoTIFF: {}", epFile);
                    snapshotUtils.writeGeotiff(image, sector, epFile);
                } catch (IOException e)
                {
                    logger.warn("Failed to write GeoTIFF", e);
                }
            }
        }
    }

    /**
     * Divide the given sector into squares of width/height specified by maxSideLength.  If the delta lat/lon values
     * do not correspond to even-sized squares, expand the sector to allow for even division.
     *
     * @param sector     sector to subdivide into squares
     * @param sideMeters length of each square's sides
     * @return an array of square sectors spanning the input sector.
     * @since 2.37, moved here from the now deleted MultiGeoTiffSnapshotUtil
     */
    private Sector[] divideSectorIntoSquares(Sector sector, double sideMeters)
    {
        Globe globe = globeManager.getPrimaryGlobeDelegate().getWorldWindow().getModel().getGlobe();
        LengthMeasurer lengthMeasurer = new LengthMeasurer();

        LatLon[] corners = sector.getCorners();
        ArrayList<LatLon> widthCorners = new ArrayList<>();
        widthCorners.add(corners[2]);
        widthCorners.add(corners[3]);
        lengthMeasurer.setPositions(widthCorners, 0);

        double width = lengthMeasurer.getLength(globe);
        double deltaLon = sector.getDeltaLonDegrees();
        double sideWidthLon = sideMeters * deltaLon / width;
        int numWidthSides = (int) Math.ceil(width / sideMeters);
        double maxLon = sector.getMinLongitude().getDegrees() + sideWidthLon * numWidthSides;

        ArrayList<LatLon> heightCorners = new ArrayList<>();
        heightCorners.add(corners[0]);
        heightCorners.add(corners[3]);
        lengthMeasurer.setPositions(heightCorners, 0);

        double height = lengthMeasurer.getLength(globe);
        double deltaLat = sector.getDeltaLatDegrees();
        double sideHeightLat = sideMeters * deltaLat / height;
        int numHeightSides = (int) Math.ceil(height / sideMeters);
        double maxLat = sector.getMinLatitude().getDegrees() + sideHeightLat * numHeightSides;

        Sector divisibleSector = sector.union(Angle.fromDegrees(maxLat), Angle.fromDegrees(maxLon));

        double dLat = divisibleSector.getDeltaLat().degrees / numHeightSides;
        double dLon = divisibleSector.getDeltaLon().degrees / numWidthSides;

        Sector[] sectors = new Sector[numHeightSides * numWidthSides];
        int idx = 0;
        for (int row = 0; row < numHeightSides; row++)
        {
            for (int col = 0; col < numWidthSides; col++)
            {
                sectors[idx++] = Sector.fromDegrees(
                        divisibleSector.getMinLatitude().degrees + dLat * row,
                        divisibleSector.getMinLatitude().degrees + dLat * row + dLat,
                        divisibleSector.getMinLongitude().degrees + dLon * col,
                        divisibleSector.getMinLongitude().degrees + dLon * col + dLon);
            }
        }

        return sectors;
    }

    private File createTempImageFolder(String newDirectoryName)
    {
        IVirtualFileSystem fileSystem = vfsManager.getVirtualFileSystem(STRUCTURE_LAYER_IMAGERY);
        File rootDir = fileSystem.getRootDir();
        String targetDirName = FileUtils.getNextNameIteration(rootDir, newDirectoryName); // Ensure a unique name
        File targetDir = new File(rootDir, targetDirName);

        if (!targetDir.mkdirs())
        {
            return null;
        }
        return targetDir;
    }

    /**
     * Disable picking if the pitch is too high.  Otherwise call the real pick method.
     *
     * @param dc        draw context of the current globe view.
     * @param pickPoint picked point.
     */
    @Override
    protected void doPick(DrawContext dc, java.awt.Point pickPoint)
    {
        if (isGreaterThanMaxPitch(dc))
        {
            return;
        }
        doPick(dc, getRenderables(dc.getVisibleSector()), pickPoint);
    }

    /**
     * Render visible structures if the pitch is not too high.
     *
     * @param dc draw context of current globe view.
     */
    @Override
    protected void doRender(DrawContext dc)
    {
        if (isGreaterThanMaxPitch(dc))
        {
            return;
        }
        Collection<? extends Renderable> visibleRenderables = getRenderables(dc.getVisibleSector());
        lastVisibleRenderables = visibleRenderables.size();
        doRender(dc, visibleRenderables);
    }

    /**
     * Determine whether the current pitch is too high.
     *
     * @param dc draw context of current globe view.
     * @return {@code true} if the pitch is too high, {@code false} otherwise.
     */
    protected boolean isGreaterThanMaxPitch(DrawContext dc)
    {
        return dc.getView().getPitch().degrees > MAX_PITCH_DEGREES;
    }

    @Override
    protected void doPick(DrawContext dc, Iterable<? extends Renderable> renderables, java.awt.Point pickPoint)
    {
        pickSupport.clearPickList();
        pickSupport.beginPicking(dc);

        try
        {
            for (Renderable renderable : renderables)
            {
                // If the caller has specified their own Iterable,
                // then we cannot make any guarantees about its contents.
                if (renderable != null)
                {
                    float[] inColor = new float[4];
                    final GL2 gl = (GL2) dc.getGL();
                    gl.glGetFloatv(GL2ES1.GL_CURRENT_COLOR, inColor, 0);
                    final Color color = dc.getUniquePickColor();
                    gl.glColor3ub((byte) color.getRed(), (byte) color.getGreen(), (byte) color.getBlue());

                    try
                    {
                        renderable.render(dc);
                    } catch (Exception e)
                    {
                        logger.error("Exception while picking Renderable");
                        continue; // go on to next renderable
                    }

                    gl.glColor4fv(inColor, 0);

                    if (renderable instanceof Locatable)
                    {
                        pickSupport.addPickableObject(color.getRGB(), renderable,
                                ((Locatable) renderable).getPosition(), false);
                    } else
                    {
                        pickSupport.addPickableObject(color.getRGB(), renderable);
                    }
                }
            }

            pickSupport.resolvePick(dc, pickPoint, this);
        } finally
        {
            pickSupport.endPicking(dc);
        }
    }

    @Override
    protected void doRender(DrawContext dc, Iterable<? extends Renderable> renderables)
    {
        for (Renderable renderable : renderables)
        {
            try
            {
                // If the caller has specified their own Iterable,
                // then we cannot make any guarantees about its contents.
                if (renderable != null)
                {
                    renderable.render(dc);
                }
            } catch (Exception e)
            {
                // continue to next renderable
            }
        }
    }

    @Override
    public String toString()
    {
        return getClass().getName();
    }

    /**
     * A memory cache for managing surface structure instances.  Some WW shapes (i.e. ExtrudedPolygon) use direct
     * Int/Float buffers, which are allocated outside of the Java heap.  When many of these shapes are in memory at
     * the same time (>30K), strange memory problems often arise, sometimes resulting in VM crashes.  This cache (used
     * in tandem with the SurfaceStructureRenderable class and the BasicQuadTree) aggressively minimizes the number of
     * WW shapes in memory, while keeping the shape holder in memory so that the WW shape can be re-created whenever
     * we need them.
     */
    private static class SurfaceStructureCache implements MemoryCache.CacheListener
    {
        private static final long CACHE_SIZE = 2_000L;

        private final BasicMemoryCache memoryCache = new BasicMemoryCache((long) (CACHE_SIZE * 0.40), CACHE_SIZE);

        public SurfaceStructureCache()
        {
            memoryCache.addCacheListener(this);
        }

        /**
         * Use a ISurfaceStructure instance to query the cache for its renderable surface structure.  If the
         * cache has already flushed the renderable, then use the persistable surface structure to re-create the
         * renderable surface structure.
         *
         * @param structure persistable surface structure key for the memory cache.
         * @return a renderable surface structure.
         */
        public Renderable get(ISurfaceStructure structure)
        {
            final Renderable renderable = (Renderable) memoryCache.getObject(structure);

            // Add it to the cache if missing, this will re-create the renderable
            if (renderable == null)
            {
                // Note: this call to structure.getRenderable() will recreate it if it is not present
                memoryCache.add(structure, structure.getRenderable(), 1);
            }
            return structure.getRenderable();
        }

        @Override
        public void entryRemoved(Object key, Object clientObject)
        {
            ((ISurfaceStructure) key).dispose();
        }

        @Override
        public void removalException(Throwable throwable, Object key, Object clientObject)
        {
            logger.warn("Surface structure cache exception on removal.  Key: {}, client object: {}", key, clientObject, throwable);
        }

        public void flushCache()
        {
            memoryCache.clear();
        }
    }

    /**
     * Helper method to convert sector deltas into a buffered image's dimensions.
     *
     * @param sector      sector span of the image
     * @param image       image on which to draw
     * @param aspectRatio aspect ratio ... hmm this is Tom's code.  Better ask him.
     * @return lat/lon scales used to convert to buffered image coordinates.
     */
    private double[] computeScales(Sector sector, BufferedImage image, double aspectRatio)
    {
        int subWidth = aspectRatio >= 1 ? image.getWidth() : (int) (image.getWidth() * aspectRatio);
        int subHeight = aspectRatio >= 1 ? (int) (image.getHeight() / aspectRatio) : image.getHeight();

        double lonScale = subWidth / sector.getDeltaLonDegrees();
        double latScale = subHeight / sector.getDeltaLatDegrees();

        return new double[]{latScale, lonScale};
    }

    /**
     * Draw an extruded polygon on a 2D buffered image.
     *
     * @param sector  sector span of buffered image.
     * @param polygon polygon to draw on the image
     * @param image   image on which to draw
     */
    public void drawExtrudedPolygon(Sector sector, SSExtrudedPolygon polygon, BufferedImage image)
    {
        if (polygon == null)
        {
            return;
        }

        //todo: optimise this section so it doesn't have to be called on each polygon
        double[] scales = computeScales(sector, image, 1);
        double lonScale = scales[1];
        double latScale = scales[0];

        // if the shape has no boundaries, we can't draw anything
        List<List<? extends LatLon>> boundaries = polygon.getBoundaries();
        if (boundaries.size() < 1)
        {
            return;
        }
        int imageHeight = image.getHeight();

        //first, draw outer boundary
        GeneralPath path = createPath(boundaries.get(0).iterator(), sector, imageHeight, latScale, lonScale);

        Graphics2D g2 = image.createGraphics();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        if (polygon.getAttributes().isDrawInterior())
        {
            g2.setPaint(polygon.getAttributes().getInteriorMaterial().getDiffuse());
            g2.fill(path);
        }

        if (polygon.getAttributes().isDrawOutline())
        {
            g2.setPaint(polygon.getAttributes().getOutlineMaterial().getDiffuse());
            g2.setStroke(new BasicStroke((float) polygon.getAttributes().getOutlineWidth()));
            g2.draw(path);
        }

        //next cut holes for inner boundaries
        for (int i = 1; i < boundaries.size(); i++)
        {
            path = createPath(boundaries.get(i).iterator(), sector, imageHeight, latScale, lonScale);

            if (polygon.getAttributes().isDrawInterior())
            {
                Composite oldComposite = g2.getComposite();
                g2.setComposite(AlphaComposite.getInstance(AlphaComposite.CLEAR));
                g2.fill(path);
                //reset alpha composite
                g2.setComposite(oldComposite);
            }

            if (polygon.getAttributes().isDrawOutline())
            {
                g2.setPaint(polygon.getAttributes().getOutlineMaterial().getDiffuse());
                g2.setStroke(new BasicStroke((float) polygon.getAttributes().getOutlineWidth()));
                g2.draw(path);
            }
        }
    }

    /**
     * Create a java2D general path for an extruded polygon's boundary locations.
     *
     * @param locations   an extruded polygon's boundary locations
     * @param sector      sector span of the corresponding image
     * @param imageHeight height of the corresponding image
     * @param latScale    latitude to y conversion scale
     * @param lonScale    longitude to x conversion scale
     * @return a general path for an extruded polygon's boundary locations.
     */
    private GeneralPath createPath(Iterator<? extends LatLon> locations, Sector sector, int imageHeight, double latScale, double lonScale)
    {
        GeneralPath path = new GeneralPath();
        LatLon ll = locations.next();
        double x = lonScale * ll.getLongitude().subtract(sector.getMinLongitude()).degrees;
        double y = imageHeight - latScale * ll.getLatitude().subtract(sector.getMinLatitude()).degrees;
        path.moveTo((float) x, (float) y);

        while (locations.hasNext())
        {
            ll = locations.next();
            x = lonScale * ll.getLongitude().subtract(sector.getMinLongitude()).degrees;
            y = imageHeight - latScale * ll.getLatitude().subtract(sector.getMinLatitude()).degrees;
            path.lineTo((float) x, (float) y);
        }
        return path;
    }

    /**
     * A small modification to LocalRasterServerLayer, which displays itself when the actual structures are invisible
     * (given that the current eye altitude is valid).
     */
    private class StructurePreviewLayer extends BasicTiledImageLayer
    {
        public StructurePreviewLayer(String restorableState)
        {
            super(restorableState);
        }

        public StructurePreviewLayer(Element domElement, AVList params)
        {
            super(domElement, params);
        }

        /**
         * Indicates whether the layer is active opposing whether the owner {@link ApplicationCachedSurfaceStructureLayer layer}
         * is active - and if the eye altitude is within the max/min altitudes of this layer.
         *
         * @param dc the current draw context
         * @return {@code true} if the layer is active, {@code false} otherwise.
         */
        @Override
        public boolean isLayerActive(DrawContext dc)
        {
            if (dc == null || dc.getView() == null)
            {
                return false;
            }

            Position eyePos = dc.getView().getEyePosition();
            if (eyePos == null)
            {
                return false;
            }

            double altitude = eyePos.getElevation();
            boolean validAltitude = altitude >= getMinActiveAltitude() && altitude <= getMaxActiveAltitude();
            return !doIsLayerActive(dc) && validAltitude;
        }
    }
}
