package gov.takx.core.structures;

import gov.nasa.worldwind.avlist.AVKey;
import gov.nasa.worldwind.avlist.AVList;
import gov.nasa.worldwind.avlist.AVListImpl;
import gov.nasa.worldwind.formats.shapefile.Shapefile;
import gov.nasa.worldwind.geom.LatLon;
import gov.nasa.worldwind.geom.Sector;
import gov.nasa.worldwind.layers.RenderableLayer;
import gov.nasa.worldwind.util.RestorableSupport;
import gov.nasa.worldwind.util.RestorableSupport.StateObject;
import gov.takx.api.annotation.InvokeAndWaitOnEDT;
import gov.takx.api.annotation.PseudoBean;
import gov.takx.api.annotation.VFSManagerScope;
import gov.takx.api.filesystem.IVirtualFileSystem;
import gov.takx.api.filesystem.IVirtualFileSystemManager;
import gov.takx.api.filesystem.VFSScope;
import gov.takx.api.layer.IRaptorLayer;
import gov.takx.api.persist.IPersistServiceProvider;
import gov.takx.api.platform.ICoreExecutorService;
import gov.takx.api.query.param.HQLParameter;
import gov.takx.api.structures.ISurfaceStructure;
import gov.takx.api.structures.ISurfaceStructureService;
import gov.takx.api.structures.ISurfaceStructureServiceListener;
import gov.takx.api.structures.SurfaceStructureServiceEvent;
import gov.takx.commons.utils.StringUtils;
import gov.takx.dataservices.persist.PersistableShapefileShape;
import jakarta.annotation.PostConstruct;
import jakarta.inject.Inject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.lang.invoke.MethodHandles;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * todo: enable other shapes to be added to this layer (potentially replace CachedSurfaceStructureLayer).
 * Custom LargeShapefile extension of WorldWind's RenderableLayer.  Manages and renders a single LargeShapefile.
 *
 */
@PseudoBean
public class LargeShapefileLayer extends RenderableLayer implements IStructureLayer, ISurfaceStructureServiceListener
{
    private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    private static final String SHAPE_PATH_KEY = "shapePath";
    private static final String INVISIBLE_RECORDS_KEY = "invisible";

    @Inject
    private ISurfaceStructureService surfaceStructureService;

    @Inject
    private IPersistServiceProvider persistServiceProvider;

    @Inject
    private ICoreExecutorService executorService;

    @Inject
    @VFSManagerScope(VFSScope.Project)
    private IVirtualFileSystemManager projectVFSManager;

    protected String shapefileRelativePath;
    protected LargeShapefile shape;
    protected IRaptorLayer raptorLayer;

    public LargeShapefileLayer()
    {
        shapefileRelativePath = null;
        shape = null;
        setPickEnabled(true);
        raptorLayer = null;
    }

    @PostConstruct
    private void postConstruct()
    {
        surfaceStructureService.addSurfaceStructureServiceListener(this);
    }

    /**
     * Set the shape file.
     *
     * @param filePath relative path to shape file of interest.
     */
    public void setFilePath(String filePath)
    {
        shapefileRelativePath = filePath;
    }

    /**
     * Set the {@link IRaptorLayer}.
     *
     * @param raptorLayer The layer of interest.
     */
    public void setRaptorLayer(IRaptorLayer raptorLayer)
    {
        this.raptorLayer = raptorLayer;
    }

    /**
     * Get the bounding sector for this layer.
     *
     * @return bounding sector for this layer.
     */
    @Override
    public Sector getSector()
    {
        return shape != null ? shape.getSector() : null;
    }

    /**
     * Set the shapefile shape.
     *
     * @param shape shapefile shape of interest.
     */
    @InvokeAndWaitOnEDT
    public void setShape(LargeShapefile shape)
    {
        removeAllRenderables();
        this.shape = shape;
        shape.setLayer(this);
        setupBoundingSector();
        addRenderable(shape);
    }

    /**
     * Cleanup for disposal, remove structure service listener.
     */
    @Override
    public void dispose()
    {
        super.dispose();

        // Stop listening to structure service
        surfaceStructureService.removeSurfaceStructureServiceListener(this);
    }

    @Override
    public void add(ISurfaceStructure structure)
    {
        // Do nothing, renderables are managed by the underlying shape file and not added to the layer directly
    }

    @Override
    public Collection<? extends ISurfaceStructure> getStructures(Sector extent)
    {
        return shape.getStructures(extent);
    }

    @Override
    public Collection<? extends ISurfaceStructure> getAllStructures()
    {
        return shape.getStructures();
    }

    @Override
    public Collection<? extends ISurfaceStructure> getStructures(LatLon location)
    {
        return shape.getStructures(location);
    }

    @Override
    public ISurfaceStructure getStructureByName(String name)
    {
        return shape.getByName(name);
    }

    @Override
    @SuppressWarnings("unchecked")
    public void onEvent(SurfaceStructureServiceEvent event)
    {
        // Listen for 'structures removed' events and update the shape accordingly.  Individual shape records can
        // not actually be removed from the file, but they can be made invisible, which prevents them from being
        // used in intersection calculations as well as the obvious function of hiding them from view.

        if (ISurfaceStructureService.SURFACE_STRUCTURES_REMOVED.equals(event.getEventName()))
        {
            List<ISurfaceStructure> removedStructures = (List<ISurfaceStructure>) event.getNewValue();
            for (ISurfaceStructure structure : removedStructures)
            {
                shape.hideStructureIfPresent(structure);
            }
        }
    }

    @Override
    public String getRestorableState()
    {
        RestorableSupport rs = RestorableSupport.newRestorableSupport();
        if (shapefileRelativePath != null)
        {
            // Store relative path to shapefile (in virtual file system).
            rs.addStateValueAsString(SHAPE_PATH_KEY, shapefileRelativePath);

            if (shape != null)
            {
                // Store record numbers of all non-visible items, as they are considered 'deleted' and should no longer
                // be used in intersection calculations.
                final String asString = StringUtils.join(shape.getInvisibleRecordNumbers(), " ", null);
                rs.addStateValueAsString(INVISIBLE_RECORDS_KEY, asString);
            }
        }
        return rs.getStateAsXml();
    }

    @Override
    public void restoreState(String state)
    {
        final RestorableSupport rs = RestorableSupport.parse(state);
        shapefileRelativePath = rs.getStateValueAsString(SHAPE_PATH_KEY);

        if (shapefileRelativePath != null)
        {
            // We're called on the EDT, but loading our persisted state and re-parsing the shape file can be
            // very lengthy operations, so kick them off to the executor service and then add the resulting shape
            // on the EDT.

            executorService.submit(() -> reloadShapeFile(rs));
        }
    }

    /**
     * Reload our shape file.
     *
     * @param restorableSupport Our parsed restorable state
     * @since 3.0
     */
    private void reloadShapeFile(RestorableSupport restorableSupport)
    {
        Thread.currentThread().setName("Reload shapes: " + raptorLayer.getName());

        // Restore shape from relative path in virtual file system.
        IVirtualFileSystem fileSystem = projectVFSManager.getVirtualFileSystem(StructureUtils.SHAPE_FILE_DIR);
        File file = new File(fileSystem.getRootDir(), shapefileRelativePath);

        LargeShapefile shape = new LargeShapefile(new Shapefile(file), getPersistableShapes());

        setShape(shape); // This happens on the EDT

        final StateObject invisibleRecordIds = restorableSupport.getStateObject(INVISIBLE_RECORDS_KEY);
        if (invisibleRecordIds != null)
        {
            // Restore record numbers of all non-visible items, as they are considered 'deleted' and should no
            // longer be used in intersection calculations.
            shape.setInvisibleRecordNumbers(stringToIntegers(invisibleRecordIds.getValue()));
        }
    }

    /**
     * Load all our persistable shapes.
     *
     * @return Map of persistable shapes, keyed by record number
     * @since 3.0
     */
    private Map<Integer, PersistableShapefileShape> getPersistableShapes()
    {
        final Map<Integer, PersistableShapefileShape> persistableShapes = new HashMap<>();

        try
        {
            final String hql = "from PersistableShapefileShape where layer=:layer";

            final List<PersistableShapefileShape> shapes = persistServiceProvider.getPersistService().query(hql,
                    new HQLParameter("layer", raptorLayer));

            shapes.forEach(ps -> persistableShapes.put(ps.getRecordNumber(), ps));
        } catch (Exception e)
        {
            logger.error("Error loading surface structures for: {}", raptorLayer, e);
        }

        return persistableShapes;
    }

    /**
     * Setup a bounding sector for LayerViewManager.
     * <p>
     * todo modify LayerViewManager to use getSector() instead.
     */
    private void setupBoundingSector()
    {
        AVList params = (AVList) getValue(AVKey.CONSTRUCTION_PARAMETERS);
        if (params == null)
        {
            params = new AVListImpl();
            setValue(AVKey.CONSTRUCTION_PARAMETERS, params);
        }

        final Sector sector = getSector();
        params.setValue(AVKey.SECTOR, sector);
        setValue(AVKey.SECTOR, sector);
    }

    /**
     * Attempt to convert a String into a List of integers.
     *
     * @param stringOfIntegers String to convert.
     * @return list of integers converted from String.
     */
    private static List<Integer> stringToIntegers(String stringOfIntegers)
    {
        return Stream.of(stringOfIntegers.trim().split(" "))
                .map(intString -> parseInt(intString.trim()))
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
    }

    /**
     * Convert a String into an integer.  Handles {@code null} and empty Strings and catches Exceptions during parsing.
     *
     * @param s String to convert.
     * @return integer converted from String.
     */
    private static Integer parseInt(String s)
    {
        if (StringUtils.isEmpty(s)) return null;

        try
        {
            return Integer.parseInt(s);
        } catch (Exception e)
        {
            logger.warn("Unable to parse integer from: {}", s);
        }
        return null;
    }
}
