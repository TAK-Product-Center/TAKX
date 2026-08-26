package gov.takx.core.structures;

import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;
import gov.nasa.worldwind.event.SelectEvent;
import gov.nasa.worldwind.event.SelectListener;
import gov.nasa.worldwind.formats.shapefile.Shapefile;
import gov.nasa.worldwind.formats.shapefile.ShapefileRecord;
import gov.nasa.worldwind.formats.shapefile.ShapefileUtils;
import gov.nasa.worldwind.geom.Intersection;
import gov.nasa.worldwind.geom.LatLon;
import gov.nasa.worldwind.geom.Line;
import gov.nasa.worldwind.geom.Sector;
import gov.nasa.worldwind.layers.Layer;
import gov.nasa.worldwind.terrain.Terrain;
import gov.tak.api.util.AttributeSet;
import gov.takx.api.annotation.Headless;
import gov.takx.api.event.StartProjectServices;
import gov.takx.api.event.StopProjectServices;
import gov.takx.api.globe.IApplicationGlobe;
import gov.takx.api.globe.IExtendedGlobeManager;
import gov.takx.api.globe.IGlobeShapeMapObject;
import gov.takx.api.layer.IRaptorLayer;
import gov.takx.api.layer.IRaptorLayerManager;
import gov.takx.api.layer.IRaptorLayerManagerListener;
import gov.takx.api.layer.LayerGroup;
import gov.takx.api.mapobject.IShapeMapObject;
import gov.takx.api.persist.ILayerPersist;
import gov.takx.api.persist.IPersistServiceProvider;
import gov.takx.api.structures.IIntersectable;
import gov.takx.api.structures.IIntersectableShape;
import gov.takx.api.structures.ISurfaceStructure;
import gov.takx.api.structures.ISurfaceStructureCallback;
import gov.takx.api.structures.ISurfaceStructureService;
import gov.takx.api.structures.ISurfaceStructureServiceListener;
import gov.takx.api.structures.SurfaceStructureServiceEvent;
import gov.takx.api.structures.SurfaceStructureServiceEventType;
import gov.takx.commons.swing.AlwaysOnTopJOptionPane;
import gov.takx.commons.utils.StringUtils;
import gov.takx.core.globe.layers.ShapefileImportHandler;
import gov.takx.core.project.ServiceOrdering;
import gov.takx.core.structures.gui.SurfaceStructureDialog;
import gov.takx.core.view.gui.maps.AddKMLOverlayController;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import jakarta.annotation.Priority;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import jakarta.inject.Inject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.AbstractAction;
import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.JPopupMenu;

import java.awt.event.ActionEvent;
import java.awt.event.InputEvent;
import java.awt.event.MouseEvent;
import java.beans.PropertyChangeEvent;
import java.io.File;
import java.lang.invoke.MethodHandles;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Concrete implementation of {@link ISurfaceStructureService}.  We use a {@link CachedSurfaceStructureLayer} to hold
 * all the structures we manage. It is responsible for rendering them and providing the lookup services.
 *
 */
@ApplicationScoped
public class SurfaceStructureService
        implements ICoreSurfaceStructureService, SelectListener, IRaptorLayerManagerListener
{
    private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    /**
     * Threshold for large shapefile structure counts before displaying a dialog warning users that performance may
     * degrade if import is confirmed.
     */
    private static final int LARGE_SHAPEFILE_WARNING_THRESHOLD = 200_000;

    /**
     * Threshold for basic structure importer counts (e.g. non-large shapefile importer) before displaying a warning
     * dialog that performance may degrade if import is confirmed.  The warning dialog also gives the user the choice
     * to create a preview imagery layer, which may improve
     */
    private static final int BASIC_IMPORTER_WARNING_THRESHOLD = 2_000;

    private final Set<String> metaDataOfInterestList = ConcurrentHashMap.newKeySet();
    private final List<ISurfaceStructureServiceListener> serviceListeners = new CopyOnWriteArrayList<>();

    private final Set<ISurfaceStructure> selectedStructures = new HashSet<>();
    private ISurfaceStructure lastHighlightedStructure;

    private IApplicationGlobe primaryGlobe;

    // Start with an empty map in case a method here is called before this service starts.
    // E.g., raptorLayerRemoved, which can be called early in startup if a layer fails to restore.
    private BiMap<IStructureLayer, IRaptorLayer> wwLayer2RaptorLayerMap = HashBiMap.create();

    // Injected components
    private final PersistedSurfaceStructureLoader persistedStructureLoader;
    private final IRaptorLayerManager raptorLayerManager;
    private final IPersistServiceProvider persistServiceProvider;
    private final ILayerPersist layerPersist;
    private final IExtendedGlobeManager globeManager;
    private final boolean headless;

    /**
     * CDI Constructor.
     */
    @Inject
    SurfaceStructureService(IRaptorLayerManager raptorLayerManager, IPersistServiceProvider persistServiceProvider,
                            ILayerPersist layerPersist, IExtendedGlobeManager globeManager,
                            PersistedSurfaceStructureLoader persistedStructureLoader,
                            @Headless boolean headless)
    {
        this.raptorLayerManager = raptorLayerManager;
        this.persistServiceProvider = persistServiceProvider;
        this.layerPersist = layerPersist;
        this.globeManager = globeManager;
        this.persistedStructureLoader = persistedStructureLoader;
        this.headless = headless;
    }

    private void startService(@Observes @Priority(ServiceOrdering.START_SurfaceStructureService) StartProjectServices event)
    {
        if (headless)
        {
            logger.debug("Not starting");
            return;
        }

        raptorLayerManager.addRaptorLayerManagerListener(this); // Watch for layers being created/removed

        primaryGlobe = globeManager.getPrimaryGlobeDelegate();

        // Need to be a select listener on ALL globes
        globeManager.doWhenGlobeReady(globe -> globe.getWorldWindow().addSelectListener(this));

        // Build our layer map (from WW layer to Raptor layer) when the globe is ready
        primaryGlobe.doWhenReady(() -> wwLayer2RaptorLayerMap = raptorLayerManager.getLayerList(LayerGroup.SURFACE_STRUCTURES).stream()
                .collect(Collectors.toMap(rl -> (IStructureLayer) rl.getLayer(), Function.identity(),
                        (oldLayer, newLayer) -> newLayer, HashBiMap::create)));

        persistedStructureLoader.loadPersistedSurfaceStructures(event.getProgressMonitor());
    }

    /**
     * Stop our service.
     */
    private void stopService(@Observes @Priority(ServiceOrdering.STOP_SurfaceStructureService) StopProjectServices event)
    {
        if (headless) return;

        raptorLayerManager.removeRaptorLayerManagerListener(this);
        if (wwLayer2RaptorLayerMap != null) wwLayer2RaptorLayerMap.clear();
        metaDataOfInterestList.clear();
    }

    @Override
    public Collection<ISurfaceStructure> getAllStructures()
    {
        return wwLayer2RaptorLayerMap.keySet().stream()
                .flatMap(structureLayer -> structureLayer.getAllStructures().stream())
                .collect(Collectors.toList());
    }

    @Override
    public Collection<ISurfaceStructure> getStructures(@Nonnull LatLon location)
    {
        return wwLayer2RaptorLayerMap.keySet().stream()
                .flatMap(structureLayer -> structureLayer.getStructures(location).stream())
                .collect(Collectors.toList());
    }

    @Override
    public Collection<ISurfaceStructure> getStructures(@Nonnull Sector sector)
    {
        // Ensure the sector has positive deltaLat and deltaLon.  The structure service requires this.
        // Creating a boundingSector from the original sector will accomplish this.
        final Sector sectorOfInterest = Sector.boundingSector(sector);

        return wwLayer2RaptorLayerMap.keySet().stream()
                .flatMap(structureLayer -> structureLayer.getStructures(sectorOfInterest).stream())
                .collect(Collectors.toList());
    }

    @Override
    public Collection<ISurfaceStructure> getSelectedStructures()
    {
        return Collections.unmodifiableSet(selectedStructures);
    }

    @Override
    public ISurfaceStructure getStructure(UUID uuid)
    {
        final String name = uuid.toString();

        // Process each of our layers and return the first structure found
        return wwLayer2RaptorLayerMap.keySet().stream()
                .map(structureLayer -> structureLayer.getStructureByName(name))
                .filter(Objects::nonNull)
                .findFirst()
                .orElse(null);
    }

    @Override
    public List<String> getLayerList()
    {
        return raptorLayerManager.getLayerList(LayerGroup.SURFACE_STRUCTURES).stream()
                .map(IRaptorLayer::getName)
                .collect(Collectors.toList());
    }

    @Override
    public CompletableFuture<Integer> loadShapeFile(@Nonnull File shapeFile, @Nonnull String layerName, ISurfaceStructureCallback callback)
    {
        final IStructureImporter importer = createStructureImporter(shapeFile, layerName, callback);

        return importer != null
                ? importer.beginImport()
                : CompletableFuture.completedFuture(-1);
    }

    @Override
    public CompletableFuture<Integer> loadShapeFile(@Nonnull File shapeFile)
    {
        if (!StructureUtils.isShapeFile(shapeFile) || checkStructureCount(shapeFile))
        {
            final IStructureImporter importer = createStructureImporter(shapeFile, null, null);

            if (importer != null) return importer.beginImport();
        }

        return CompletableFuture.completedFuture(-1);
    }

    /**
     * Handle a select event on the globe.  Handle highlighting on ROLLOVER events and left/right click events.
     *
     * @param selectEvent Select event to process
     */
    @Override
    public void selected(SelectEvent selectEvent)
    {
        if (selectEvent.hasObjects())
        {
            final Object selectedObject = selectEvent.getTopObject();

            // For ROLLOVER events, we handle the highlighting of surface structures
            if (selectEvent.isRollover())
            {
                handleHighlight(selectedObject);
            } else if (selectedObject instanceof ISurfaceStructureRenderable surfaceStructureRenderable)
            {
                // Get the surface structure from the renderable
                final ISurfaceStructure surfaceStructure = surfaceStructureRenderable.getSurfaceStructure();

                if (logger.isDebugEnabled())
                {
                    logger.debug("event: {} : {}", surfaceStructure.getUUID(), selectEvent.getEventAction());
                }

                if (selectEvent.isLeftClick())
                {
                    handleStructureLeftClick(surfaceStructure, selectEvent.getMouseEvent().getModifiersEx());
                } else if (selectEvent.isRightClick())
                {
                    showPopupMenu(surfaceStructure, selectEvent.getMouseEvent());
                } else if (selectEvent.isLeftDoubleClick())
                {
                    new SurfaceStructureDialog(surfaceStructure, getMetaDataOfInterest()).setVisible(true);
                }
            }
        }
    }

    /**
     * Handle a ROLLOVER event and update the structure highlighting accordingly.
     *
     * @param selectedObject Current "top object", which may be a surface structure
     * @since 3.0
     */
    private void handleHighlight(Object selectedObject)
    {
        if (selectedObject instanceof ISurfaceStructureRenderable)
        {
            // Clear any previous highlight and highlight the current one
            final ISurfaceStructure structure = (((ISurfaceStructureRenderable) selectedObject).getSurfaceStructure());

            if (lastHighlightedStructure == null)
            {
                setHighlightAttributes(structure);
                lastHighlightedStructure = structure;
            } else
            {
                // if it's not the same surface structure, un-highlight the last and highlight this new
                if (lastHighlightedStructure != structure)
                {
                    lastHighlightedStructure.resetAttributes();
                    setHighlightAttributes(structure);
                    lastHighlightedStructure = structure;
                }
            }
        } else
        {
            // Mouse is not over a surface structure, un-highlight the latest surface structure if there is one
            if (lastHighlightedStructure != null)
            {
                lastHighlightedStructure.resetAttributes();
                lastHighlightedStructure = null;
            }
        }
    }

    /**
     * Create an importer for a given shape file (SHP or KML)
     *
     * @param shapeFile File to read for structure definitions
     * @param layerName name for the surface structure layer
     * @param callback  a callback object to be notified on structure file import progress
     * @return a new structure file importer
     */
    private IStructureImporter createStructureImporter(File shapeFile,
                                                       String layerName,
                                                       ISurfaceStructureCallback callback)
    {
        boolean isShapeFile = StructureUtils.isShapeFile(shapeFile);
        boolean isKMLFile = StructureUtils.isKMLFile(shapeFile);

        if (!isShapeFile && !isKMLFile) return null;

        // TODO: I would really like to instantiate the progress bar right here. With a large file it takes
        //       a very long time before anything show up indicating to the user that we are doing something.

        // If possible, import using the new LargeShapefileImporter. This type of importer is used when the
        // file is of type SHP and exclusively contains shapes with defined height.
        //
        // Large shapefile import is not yet supported for KML/KMZ files.
        if (isShapeFile && isShapeFileAllExtrudedPolygons(shapeFile))
        {
            return createLargeShapefileImporter(shapeFile, layerName, callback);
        }

        // If the LargeShapefileImporter was not appropriate, fallback to the classic importer.
        // I.e. SHP doesn't exclusively contain 3D polygons.
        boolean createPreviewLayer = false;

        if (isShapeFile)
        {
            int numberOfRecords = BasicShpStructureFileImporter.getShapeCount(shapeFile);
            if (numberOfRecords > BASIC_IMPORTER_WARNING_THRESHOLD)
            {
                int ret = AlwaysOnTopJOptionPane.showConfirmDialog(JFrame.getFrames()[0],
                        "<html>This shape file contains <font color=\"red\">" + numberOfRecords + "</font> shapes.\n" +
                                "Importing all of these shapes into an ordinary structures layer will degrade\n" +
                                "application performance.  In order to optimise performance, creation of\n" +
                                "a structure layer with preview imagery is recommended.\n\n" +
                                "Choose Yes if you would like to use a preview imagery layer\n" +
                                "Choose No if you would like to create a regular structures layer.\n" +
                                "Choose Cancel if you would like to cancel the import process.",
                        "High Structure Count",
                        JOptionPane.WARNING_MESSAGE,
                        JOptionPane.YES_NO_CANCEL_OPTION);

                if (ret == JOptionPane.CANCEL_OPTION) return null;
                createPreviewLayer = ret == JOptionPane.OK_OPTION;
            }
        }

        final var raptorLayer = getRaptorLayer(shapeFile, layerName, createPreviewLayer);

        return raptorLayer == null ? null
                : isShapeFile
                        ? new BasicShpStructureFileImporter(shapeFile, raptorLayer, callback)
                : new KmlStructureFileImporter(shapeFile, raptorLayer, callback);
    }

    /**
     * Get or create the layer for the given {@code shapeFile}.
     *
     * @param shapeFile Shape file being processed
     * @param layerName Name of layer to find/create, if {@code null} the name of the shape file is used
     * @param createPreviewLayer Pass {@code true} to create a preview image layer
     * @return Appropriate layer, {@code null} if layer creation fails
     * @since 5.5
     */
    private IRaptorLayer getRaptorLayer(@Nonnull File shapeFile, @Nullable String layerName, boolean createPreviewLayer)
    {
        IRaptorLayer raptorLayer;
        if (layerName == null || !getLayerList().contains(layerName))
        {
            Sector sector = getShapeFileSector(shapeFile);
            layerName = layerName != null ? layerName : shapeFile.getName();
            raptorLayer = makeLayer(sector, layerName, createPreviewLayer);
        } else
        {
            raptorLayer = findLayer(layerName);
            updateLayerMap(raptorLayer);
        }
        return raptorLayer;
    }

    /**
     * @return {@code true} if the given shape file contains only extruded (3D) polygons
     * @since 5.5 (moved here from StructureUtils)
     */
    private boolean isShapeFileAllExtrudedPolygons(@Nonnull File file)
    {
        try (Shapefile shapefile = new Shapefile(file))
        {
            if (!ShapefileUtils.hasHeightAttribute(shapefile)) return false;

            // All records must be polygons and have a valid height
            while (shapefile.hasNext())
            {
                ShapefileRecord record = shapefile.nextRecord();

                if (!Shapefile.isPolygonType(record.getShapeType()) || ShapefileUtils.extractHeightAttribute(record) == null)
                {
                    return false;
                }
            }
        }
        return true;
    }

    /**
     * Create a LargeShapefileImporter.  This type of importer is only appropriate for importing SHP files containing
     * shapes with defined height.  2D shapes and KML is not currently supported.  It is geared specifically towards
     * large shapefiles (> 2000 shapes), but can be used with shapefiles of any size.
     *
     * @param shapefile File to read for structure definitions
     * @param layerName name for the surface structure layer
     * @param callback  a callback object to be notified on structure file import progress
     * @return a new structure file importer
     * @since 1.43
     */
    private IStructureImporter createLargeShapefileImporter(File shapefile, String layerName, ISurfaceStructureCallback callback)
    {
        final String newLayerName = layerName != null ? layerName : shapefile.getName();

        LargeShapefileLayer shapeLayer = new LargeShapefileLayer();
        IRaptorLayer raptorLayer = raptorLayerManager.addLayer(shapeLayer, LayerGroup.SURFACE_STRUCTURES);

        raptorLayer.setPickable(true);
        raptorLayer.setName(newLayerName);
        raptorLayer.setDisplayName(newLayerName);
        layerPersist.persistLayer(raptorLayer);

        shapeLayer.setRaptorLayer(raptorLayer);
        updateLayerMap(raptorLayer);

        // The importer is responsible for persisting the shapefile structures.
        return new LargeShapefileImporter(shapefile, raptorLayer, callback);
    }

    @Override
    public void persistStructure(@Nonnull ISurfaceStructure struct)
    {
        persistServiceProvider.getPersistService().persistEntity(struct.getPersistable());
    }

    /**
     * Construct the cached renderable layer that will hold all the structures and wrap it in a {@link IRaptorLayer}.
     *
     * @param boundingSector     The bounding sector for the layer
     * @param name               The name of the layer
     * @param createPreviewLayer specifies whether a 2D imagery preview layer should be created for the structure layer
     * @return constructed layer
     */
    private IRaptorLayer makeLayer(Sector boundingSector, String name, boolean createPreviewLayer)
    {
        double[] boundingSectorArray =
                boundingSector != null ? boundingSector.asDegreesArray() : Sector.FULL_SPHERE.asDegreesArray();
        Double[] boundingSectorDblArray = new Double[boundingSectorArray.length];
        for (int i = 0; i < boundingSectorArray.length; i++) boundingSectorDblArray[i] = boundingSectorArray[i];
        String boundingSectorString = StringUtils.join(boundingSectorDblArray);
        String restorableState = boundingSectorString + "," + createPreviewLayer;

        IRaptorLayer raptorLayer = raptorLayerManager.addLayer(
                name,
                name,
                CachedSurfaceStructureLayer.class.getName(),
                true,
                true,
                null,
                LayerGroup.SURFACE_STRUCTURES,
                null,
                restorableState,
                false);
        layerPersist.persistLayer(raptorLayer);

        updateLayerMap(raptorLayer);

        return raptorLayer;
    }

    @Override
    public void addMetaDataOfInterest(@Nonnull String metaDataKey)
    {
        metaDataOfInterestList.add(metaDataKey);
    }

    @Override
    public List<String> getMetaDataOfInterest()
    {
        return new ArrayList<>(metaDataOfInterestList);
    }

    @Override
    public List<IIntersectableShape> getShapes()
    {
        return primaryGlobe.getGlobeMapObjectManager().getShapeMapObjects().stream()
                .filter(IGlobeShapeMapObject::isIntersectable)
                .map(IntersectableShape::new)
                .collect(Collectors.toList());
    }

    /**
     * CORE-3526
     * <p>
     * Notification of import completion from StructureFileImporter .  Essentially, this method just forwards to
     * fireEvent().  The difference is that it checks the imported structure count before firing the event.  If no
     * structures were imported, the associated layer is removed and a warning is logged/displayed.  The user is then
     * given the option to import the file instead a simple overlay.
     *
     * @param sourceFile                file from which structures were imported.
     * @param structureLayer            WorldWind layer.
     * @param importedSurfaceStructures list of imported surface structures.
     * @since 1.43
     */
    @Override
    public void importComplete(@Nonnull File sourceFile,
                               @Nonnull IStructureLayer structureLayer,
                               @Nonnull List<ISurfaceStructure> importedSurfaceStructures)
    {
        if (logger.isDebugEnabled())
        {
            logger.debug("Import of {} complete, count = {}", sourceFile, importedSurfaceStructures.size());
        }

        if (importedSurfaceStructures.isEmpty())
        {
            String message = "File does not contain valid 2D or 3D surface structures";
            logger.warn(message);
            raptorLayerManager.removeLayer(wwLayer2RaptorLayerMap.get(structureLayer), false);

            if (!headless)
            {
                final int confirmation = AlwaysOnTopJOptionPane.showConfirmDialog(
                        JFrame.getFrames()[0],
                        "<html><center><font color=red>No Surface Structures Imported from File: "
                                + sourceFile.getName() +
                                "</font><br>" + message + "<br><br>" +
                                "Would you like to import this file as a simple overlay instead?</center></html>",
                        "No Surface Structures Imported", JOptionPane.YES_NO_OPTION);

                // If the user selects 'YES', import the file as a simple overlay.
                if (confirmation == JOptionPane.YES_OPTION)
                {
                    // Most likely, the user was trying to import a KML/KMZ file that didn't contain shapes.  Check
                    // the file extension for these type.  If it is KML/KMZ import the file as a simple KML overlay.
                    if (StructureUtils.isKMLFile(sourceFile))
                    {
                        AddKMLOverlayController.importAsSimpleOverlay(sourceFile);
                    }
                    // If it's not KML/KMZ, then it is SHP.  However, this branch should never happen.  Every SHP file
                    // I have ever seen has 2D or 3D shapes, so this may be untestable.
                    else
                    {
                        ShapefileImportHandler.importShapeFile(sourceFile);
                    }
                }
            }
        } else
        {
            fireEvent(this, SurfaceStructureServiceEventType.STRUCTURES_ADDED, SURFACE_STRUCTURES_ADDED, null, importedSurfaceStructures);
        }
    }

    @Override
    public void notifyListenersOfMetaDataChange(@Nonnull IIntersectable structure, @Nonnull String metaDataKey, String oldValue, String newValue)
    {
        fireEvent(structure, SurfaceStructureServiceEventType.METADATA_CHANGE, metaDataKey, oldValue, newValue);
    }

    @Override
    public void notifyListenersOfMetaDataChange(@Nonnull IShapeMapObject<?> shape, @Nonnull String metaDataKey, String oldValue, String newValue)
    {
        final IGlobeShapeMapObject gmo = (IGlobeShapeMapObject) primaryGlobe.getGlobeMapObject(shape.getKey());

        if (gmo.isIntersectable())
        {
            notifyListenersOfMetaDataChange(new IntersectableShape(gmo), metaDataKey, oldValue, newValue);
        }
    }

    @Override
    public void notifyListenerOfShapePropertyChange(@Nonnull IGlobeShapeMapObject shape, @Nonnull PropertyChangeEvent evt)
    {
        if (shape.isIntersectable())
        {
            fireEvent(new IntersectableShape(shape), SurfaceStructureServiceEventType.PROPERTY_CHANGE,
                    evt.getPropertyName(), String.valueOf(evt.getOldValue()), String.valueOf(evt.getNewValue()));
        }
    }

    /**
     * Fire an event of the given type and values to SurfaceStructureServiceListeners.
     *
     * @param source    source object.
     * @param eventType Type of event to fire
     * @param eventName name of the event.
     * @param oldValue  old value (before event).
     * @param newValue  new value (after event).
     * @since 3.0
     */
    private void fireEvent(Object source, SurfaceStructureServiceEventType eventType, String eventName, Object oldValue, Object newValue)
    {
        final SurfaceStructureServiceEvent event =
                new SurfaceStructureServiceEvent(source, eventType, eventName, oldValue, newValue);

        logger.debug("Firing: {}", event);

        serviceListeners.forEach(listener -> listener.onEvent(event));
    }

    @Override
    public void raptorLayerRemoved(IRaptorLayer raptorLayer)
    {
        final IStructureLayer wwLayer = wwLayer2RaptorLayerMap.inverse().get(raptorLayer);

        if (wwLayer != null)
        {
            wwLayer2RaptorLayerMap.remove(wwLayer);
            wwLayer.dispose();
        }
    }

    @Override
    public void raptorLayersRemoved()
    {
        wwLayer2RaptorLayerMap.keySet().forEach(IStructureLayer::dispose);
        wwLayer2RaptorLayerMap.clear();
    }

    @Override
    public void addSurfaceStructureServiceListener(@Nonnull ISurfaceStructureServiceListener listener)
    {
        serviceListeners.add(listener);
    }

    @Override
    public void removeSurfaceStructureServiceListener(@Nonnull ISurfaceStructureServiceListener listener)
    {
        serviceListeners.remove(listener);
    }

    /**
     * Check to see how many structures an SHP file contains. If it exceeds the threshold, warn and confirm that the user
     * wants to import the file.
     *
     * @param file The file to inspect.
     * @return {@code true} iff the file should be imported because it is under the threshold or the user has agreed
     * to import it despite the large number of structures it contains.
     */
    private boolean checkStructureCount(File file)
    {
        final int count;
        try (Shapefile shapefile = new Shapefile(file))
        {
            count = shapefile.getNumberOfRecords();
        }

        if (count > LARGE_SHAPEFILE_WARNING_THRESHOLD)
        {
            final boolean confirmed = AlwaysOnTopJOptionPane.showConfirmDialog(JFrame.getFrames()[0],
                    "<html><center>The selected file contains " + count + " structures.<br>" +
                            "Importing large numbers of structures may cause performance issues.<br><br>" +
                            "Are you sure you want to proceed?",
                    "Very Large Shape File Count",
                    JOptionPane.WARNING_MESSAGE, JOptionPane.YES_NO_OPTION) == JOptionPane.YES_OPTION;

            if (confirmed)
            {
                logger.warn("User agreed to import {} structures from the file {}", count, file.getName());
            } else
            {
                return false;
            }
        }

        return true;
    }

    /**
     * Surface structure delete support.  Cannot be undone.
     *
     * @param structure surface structure to delete.
     * @since 1.42
     */
    private void deleteSurfaceStructure(ISurfaceStructure structure)
    {
        if (logger.isDebugEnabled()) logger.debug("Deleting surface structure with UUID: {}", structure.getUUID());

        // Deselect the structure
        if (selectedStructures.remove(structure))
        {
            deselectStructure(structure);
        }

        // Remove the structure from the globe layer.
        final IRaptorLayer raptorLayer = structure.getLayer();
        final Layer wwLayer = raptorLayer != null ? raptorLayer.getLayer() : null;

        // Remove the structure from the layer
        if (wwLayer instanceof CachedSurfaceStructureLayer cachedLayer)
        {
            cachedLayer.removeStructure(structure);
        }

        // Delete the persistent state of the structure
        persistServiceProvider.getPersistService().deleteEntity(structure.getPersistable());

        // Announce the deletion
        fireEvent(this, SurfaceStructureServiceEventType.STRUCTURES_REMOVED, SURFACE_STRUCTURES_REMOVED, null, Collections.singletonList(structure));
    }

    private void updateLayerMap(IRaptorLayer raptorLayer)
    {
        if (raptorLayer != null)
        {
            wwLayer2RaptorLayerMap.put((IStructureLayer) raptorLayer.getLayer(), raptorLayer);
        }
    }

    /**
     * Find the layer with the provided name.
     *
     * @param name layer name
     * @return The {@link IRaptorLayer}
     */
    private IRaptorLayer findLayer(String name)
    {
        return raptorLayerManager.getLayerList(LayerGroup.SURFACE_STRUCTURES).stream()
                .filter(layer -> layer.getName().equals(name))
                .findFirst()
                .orElse(null);
    }

    private void showPopupMenu(final ISurfaceStructure structure, MouseEvent event)
    {
        JPopupMenu menu = new JPopupMenu();

        menu.add(new AbstractAction("Properties")
        {
            @Override
            public void actionPerformed(ActionEvent event)
            {
                new SurfaceStructureDialog(structure, getMetaDataOfInterest()).setVisible(true);
            }
        });

        menu.add(new AbstractAction("Delete Structure")
        {
            @Override
            public void actionPerformed(ActionEvent e)
            {
                String name = structure.getName() != null
                        ? structure.getName()
                        : "<br>" + structure.getUUID();

                int option = AlwaysOnTopJOptionPane.showConfirmDialog(
                        JFrame.getFrames()[0],
                        "<html><center>Are you sure you want to delete structure <b>" + name + "</b>?<br>" +
                                "<br><font color=red>Warning:</font> This operation cannot be undone.</center></html>",
                        "Confirm Structure Delete",
                        JOptionPane.YES_NO_OPTION);
                if (option == JOptionPane.YES_OPTION)
                {
                    deleteSurfaceStructure(structure);
                }
            }
        });
        menu.show(event.getComponent(), event.getX(), event.getY());
    }

    /**
     * Handle a left click on a structure.  Update our selectedStructures appropriately and notify any listeners.
     *
     * @param structure Structure that was selected
     * @param modifiers Mouse event modifiers to detect the use of the Ctrl key
     */
    private void handleStructureLeftClick(ISurfaceStructure structure, int modifiers)
    {
        final boolean ctrlKeyIsDown = ((modifiers & InputEvent.CTRL_DOWN_MASK) == InputEvent.CTRL_DOWN_MASK);

        if (ctrlKeyIsDown)
        {
            // Individual (additive) select/deselect a structure
            final boolean wasSelected = selectedStructures.remove(structure);

            if (wasSelected)
            {
                deselectStructure(structure);
            } else
            {
                selectStructure(structure);
            }
        } else
        {
            // Single select, deselect previously selected structures, and select the new one
            selectedStructures.forEach(this::deselectStructure);
            selectedStructures.clear();

            selectStructure(structure);
        }
    }

    /**
     * Add the given structure to the selected structures list, mark it as selected, and notify listeners.
     *
     * @param structure Structure to process
     * @since 3.0
     */
    private void selectStructure(ISurfaceStructure structure)
    {
        structure.setSelected(true);
        selectedStructures.add(structure);
        notifyListenersOfSelection(structure, true);
    }

    /**
     * Mark the given structure as NOT selected, and notify listeners.  Note, this method does NOT remove it from the
     * selected list.
     *
     * @param structure Structure to process
     * @since 3.0
     */
    private void deselectStructure(ISurfaceStructure structure)
    {
        structure.setSelected(false);
        notifyListenersOfSelection(structure, false);

        if (lastHighlightedStructure == structure) lastHighlightedStructure = null;
    }

    /**
     * Notify listeners of a change in selection state for a given surface structure.
     *
     * @param structure Structure of interest
     * @param selected  New selection state of structure
     * @since 3.0
     */
    private void notifyListenersOfSelection(ISurfaceStructure structure, boolean selected)
    {
        fireEvent(structure, SurfaceStructureServiceEventType.SELECTED_CHANGE, SURFACE_STRUCTURE_SELECTED, !selected, selected);
    }

    /**
     * Set the rendering attributes on the structure to the <em>highlight</em> settings.
     *
     * @param structure Structure to update
     */
    private void setHighlightAttributes(ISurfaceStructure structure)
    {
        structure.setStructureAttributes(ISurfaceStructure.AttributeStyle.HIGHLIGHTED);
    }

    private static Sector getShapeFileSector(File file)
    {
        if (!StructureUtils.isShapeFile(file)) return Sector.FULL_SPHERE;

        try (Shapefile sf = new Shapefile(file))
        {
            return Sector.fromDegrees(sf.getBoundingRectangle());
        }
    }

    /**
     * Default implementation for {@link IIntersectableShape}
     */
    private static class IntersectableShape implements IIntersectableShape
    {
        private final IGlobeShapeMapObject globeShapeMapObject;
        private final IShapeMapObject<?> shapeMapObject;

        public IntersectableShape(IGlobeShapeMapObject globeShapeMapObject)
        {
            this.globeShapeMapObject = globeShapeMapObject;
            shapeMapObject = (IShapeMapObject<?>) globeShapeMapObject.getMapObject();
        }

        @Override
        public AttributeSet getMetadata()
        {
            return shapeMapObject.getAttributeSet();
        }

        @Override
        public boolean isIntersectable()
        {
            return globeShapeMapObject.isIntersectable();
        }

        @Override
        public List<Intersection> intersect(Line line, Terrain terrain) throws InterruptedException
        {
            return globeShapeMapObject.intersect(line, terrain);
        }

        @Override
        public IShapeMapObject<?> getShapeMapObject()
        {
            return shapeMapObject;
        }
    }
}
