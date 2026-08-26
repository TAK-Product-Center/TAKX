package gov.takx.core.placemarkoverlay;

import gov.nasa.worldwind.layers.RenderableLayer;
import gov.takx.api.annotation.Headless;
import gov.takx.api.annotation.UiEventBus;
import gov.takx.api.annotation.VFSManagerScope;
import gov.takx.api.coordinatesystem.ILocation;
import gov.takx.api.event.StartProjectServices;
import gov.takx.api.event.StopProjectServices;
import gov.takx.api.event.project.ProjectOpened;
import gov.takx.api.filesystem.IVFSFile;
import gov.takx.api.filesystem.IVirtualFileSystem;
import gov.takx.api.filesystem.IVirtualFileSystemManager;
import gov.takx.api.filesystem.VFSScope;
import gov.takx.api.globe.IApplicationGlobe;
import gov.takx.api.globe.IExtendedGlobeManager;
import gov.takx.api.layer.IRaptorLayer;
import gov.takx.api.layer.IRaptorLayerManager;
import gov.takx.api.layer.IRaptorLayerManagerListener;
import gov.takx.api.layer.LayerGroup;
import gov.takx.api.ui.event.GlobeCreatedEvent;
import gov.takx.commons.file.FilePath;
import gov.takx.core.placemarkoverlay.gui.AddNewPlacemarkDialog;
import gov.takx.core.placemarkoverlay.gui.AddNewPlacemarkOverlayDialog;
import gov.takx.core.placemarkoverlay.gui.PlacemarkOverlayPropertiesPanel;
import gov.takx.core.project.ServiceOrdering;
import gov.takx.core.view.gui.propertiespanel.RemovePropertiesItem;
import jakarta.annotation.Priority;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import jakarta.inject.Inject;
import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.AWTEvent;
import java.awt.Toolkit;
import java.io.File;
import java.lang.invoke.MethodHandles;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 */
@ApplicationScoped
public class PlacemarkOverlayManager implements IRaptorLayerManagerListener
{
    private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    /**
     * The VFS namespace for all overlay related data.
     */
    public static final String OVERLAY_VFS_NAMESPACE = "overlays";

    /**
     * The VFS subdirectory in which placemark data is placed.
     */
    public static final String PLACEMARKS_VFS_SUBDIRECTORY = "placemarks";

    private PlacemarkSelectListener placemarkOverlaySelectListener; // Listens for placemark selections.

    // A map from each PlacemarkOverlay to its IRaptorLayer.
    private final Map<PlacemarkOverlay, IRaptorLayer> placemarkOverlayMap = new HashMap<>();

    // The dialog used to configure a new placemark overlay.
    private AddNewPlacemarkOverlayDialog addNewPlacemarkOverlayDialog;

    // The panel that shows the details for a selected placemark.
    private PlacemarkOverlayPropertiesPanel propertiesPanel;

    @Inject
    @VFSManagerScope(VFSScope.Project)
    private IVirtualFileSystemManager projectVFSManager;

    @Inject
    @VFSManagerScope(VFSScope.Global)
    private IVirtualFileSystemManager globalVFSManager;

    private IVirtualFileSystem projectVFS; // The project placemark VFS.
    private IVirtualFileSystem globalVFS; // The global placemark VFS.

    @Inject
    private IRaptorLayerManager raptorLayerManager;

    @Inject
    @Headless
    private boolean headless;

    @Inject
    private IExtendedGlobeManager globeManager;

    @Inject
    @UiEventBus
    private EventBus uiEventBus;

    /**
     * Show the dialog.
     */
    public void showAddNewPlacemarkOverlayDialog()
    {
        AddNewPlacemarkOverlayDialog addNewPlacemarkOverlayDialog = getAddNewPlacemarkOverlayDialog();
        addNewPlacemarkOverlayDialog.setVisible(true);
        addNewPlacemarkOverlayDialog.toFront();
    }

    /**
     * Create and return the dialog.
     *
     * @return The placemark overlay creation dialog.
     */
    public AddNewPlacemarkOverlayDialog getAddNewPlacemarkOverlayDialog()
    {
        if (addNewPlacemarkOverlayDialog == null)
        {
            addNewPlacemarkOverlayDialog = new AddNewPlacemarkOverlayDialog(this);
        }

        return addNewPlacemarkOverlayDialog;
    }

    public void armNewPlacemarkLocationListener()
    {
        placemarkOverlaySelectListener.armNewPlacemarkLocationListener();
    }

    public void showAllPlacemarks()
    {
        for (IRaptorLayer layer : placemarkOverlayMap.values())
        {
            layer.setVisible(true);
        }
    }

    public void hideAllPlacemarks()
    {
        for (IRaptorLayer layer : placemarkOverlayMap.values())
        {
            layer.setVisible(false);
        }
    }

    public void showAddNewPlacemarkDialog(ILocation location)
    {
        AddNewPlacemarkDialog addNewPlacemarkDialog = new AddNewPlacemarkDialog(this, location);

        addNewPlacemarkDialog.setVisible(true);
        addNewPlacemarkDialog.toFront();
    }

    public Set<PlacemarkOverlay> getLayers()
    {
        return placemarkOverlayMap.keySet();
    }

    public PlacemarkOverlayPropertiesPanel getPropertiesPanel()
    {
        if (propertiesPanel == null)
        {
            propertiesPanel = new PlacemarkOverlayPropertiesPanel();
        }
        return propertiesPanel;
    }

    /**
     * Add a placemark to a placemark overlay.
     *
     * @param overlay   The target overlay.
     * @param placemark The new placemark.
     * @param imagePath The image to use for the placemark.
     */
    public void addPlacemark(PlacemarkOverlay overlay, Map<String, String> placemark, String imagePath)
    {
        CSVPlaceMark renderable = overlay.addPlacemark(placemark, imagePath);

        IRaptorLayer overlayLayer = placemarkOverlayMap.get(overlay);
        ((RenderableLayer) overlayLayer.getLayer()).addRenderable(renderable);

        placemarkOverlaySelectListener.select(renderable);
    }

    /**
     * Check to see if a placemark layer name already exists.
     *
     * @param layerName The name to check.
     * @return {@code true} iff the name exists.
     */
    public boolean doesLayerNameExist(String layerName)
    {
        for (IRaptorLayer layer : placemarkOverlayMap.values())
        {
            if (layer.getName().equals(layerName))
            {
                return true;
            }
        }

        return false;
    }

    /**
     * Remove a placemark layer.
     *
     * @param layer The {@link IRaptorLayer} associated with the {@link PlacemarkOverlay}.
     */
    public void removeOverlay(IRaptorLayer layer)
    {
        for (Map.Entry<PlacemarkOverlay, IRaptorLayer> entry : placemarkOverlayMap.entrySet())
        {
            if (entry.getValue() == layer)
            {
                PlacemarkOverlay placemarkOverlay = entry.getKey();
                placemarkOverlayMap.remove(placemarkOverlay);

                // Notify the overlay that it should delete any files it created.
                placemarkOverlay.deletePersistedData();

                // Update the metadata file so that the overlay is removed.
                updatePersistableData(placemarkOverlay.getScope());

                // Clear the properties panel if necessary.
                final CSVPlaceMark selectedPlacemark = propertiesPanel != null
                        ? propertiesPanel.getSelectedPlacemark()
                        : null;

                if (selectedPlacemark != null)
                {
                    uiEventBus.post(new RemovePropertiesItem(selectedPlacemark));
                }

                return;
            }
        }
    }

    /**
     * Get the file path for the specified scope.
     *
     * @param scope The VFS scope.
     * @return The file path for the specified scope.
     */
    public String getFilePath(VFSScope scope)
    {
        return getVFS(scope).getRootDir().getPath();
    }

    public boolean isLayerVisible(PlacemarkOverlay overlay)
    {
        IRaptorLayer layer = placemarkOverlayMap.get(overlay);
        return layer != null && layer.isVisible();
    }

    /**
     * Start our service.  Only really start if we are running headed and we're not under test.
     */
    private void startService(@Observes @Priority(ServiceOrdering.START_GlobeService) StartProjectServices ignored)
    {
        if (headless) return;

        placemarkOverlaySelectListener = new PlacemarkSelectListener();

        // Add listeners
        Toolkit.getDefaultToolkit().addAWTEventListener(placemarkOverlaySelectListener, AWTEvent.MOUSE_EVENT_MASK);
        raptorLayerManager.addRaptorLayerManagerListener(this);

        // Now we need to register the placemark listener as a select listener on all globes
        for (IApplicationGlobe globe : globeManager.getAllGlobeDelegates())
        {
            globe.addMouseListener(placemarkOverlaySelectListener);
            globe.addMouseMotionListener(placemarkOverlaySelectListener);
            globe.getWorldWindow().addSelectListener(placemarkOverlaySelectListener);
        }

        uiEventBus.register(this);
    }

    /**
     * Stop our service.
     */
    private void stopService(@Observes @Priority(ServiceOrdering.STOP_GlobeService) StopProjectServices ignored)
    {
        if (headless) return;

        if (placemarkOverlaySelectListener != null)
        {
            Toolkit.getDefaultToolkit().removeAWTEventListener(placemarkOverlaySelectListener);
        }

        raptorLayerManager.removeRaptorLayerManagerListener(this);

        placemarkOverlaySelectListener = null;
        placemarkOverlayMap.clear();

        globalVFS = null;
        projectVFS = null;

        if (uiEventBus.isRegistered(this)) uiEventBus.unregister(this);
    }

    /**
     * Handle the creation of a new globe.  Add a mouse listener to it so we can properly handle events.
     *
     * @param e The globe creation event
     */
    @SuppressWarnings("UnusedDeclaration")
    @Subscribe
    public void onGlobeCreated(GlobeCreatedEvent e)
    {
        final IApplicationGlobe globe = e.getGlobe();

        globe.addMouseListener(placemarkOverlaySelectListener);
        globe.addMouseMotionListener(placemarkOverlaySelectListener);
        globe.getWorldWindow().addSelectListener(placemarkOverlaySelectListener);
    }

    /**
     * Get the VFS associated with the specified scope.
     *
     * @param scope The VFS scope.
     * @return The {@link IVirtualFileSystem} for the specified scope.
     */
    public IVirtualFileSystem getVFS(VFSScope scope)
    {
        switch (scope)
        {
            case Global:
                return globalVFS;
            case Project:
                return projectVFS;
            default:
                logger.warn("Unknown VFS scope: {}. Using Project VFS.", scope);
                return projectVFS;
        }
    }

    /**
     * Add an overlay to the globe and this manager.
     *
     * @param placemarkOverlay The overlay to add.
     * @param visible          Flag indicating if it's currently visible.
     */

    public void addOverlay(PlacemarkOverlay placemarkOverlay, boolean visible)
    {
        addOverlay(placemarkOverlay, visible, false);
    }

    /**
     * Add an overlay to the globe and this manager.
     *
     * @param placemarkOverlay The overlay to add.
     * @param visible          Flag indicating if it's currently visible.
     * @param isRestore        Flag indicating if we're restoring from metadata, in which case we don't need to write out the
     *                         metadata file.
     */
    private void addOverlay(PlacemarkOverlay placemarkOverlay, boolean visible, boolean isRestore)
    {
        RenderableLayer renderableLayer = new RenderableLayer();
        renderableLayer.setName(placemarkOverlay.getLayerName());
        renderableLayer.addRenderables(placemarkOverlay.getData());
        renderableLayer.setPickEnabled(true);

        IRaptorLayer placemarkOverlayLayer = globeManager.getPrimaryGlobeDelegate().addLayer(renderableLayer, LayerGroup.PLACEMARK_OVERLAY);
        placemarkOverlayLayer.setPickable(true);
        placemarkOverlayLayer.setDisplayName(placemarkOverlay.getLayerName());
        placemarkOverlayLayer.setVisible(visible);

        placemarkOverlayMap.put(placemarkOverlay, placemarkOverlayLayer);

        // Update the metadata file each time a new layer is added.
        if (!isRestore)
        {
            updatePersistableData(placemarkOverlay.getScope());
        }
    }

    /**
     * Update the metadata file associated with the scope that had an overlay added/removed.
     *
     * @param scope The scope of the overlay that was added or removed.
     */
    private void updatePersistableData(VFSScope scope)
    {
        // Iterate over all the placemark overlays and extract their persist-able data if they match the scope.
        List<Map<String, String>> persistableData =
                placemarkOverlayMap.keySet().stream()
                        .filter(placemarkOverlay -> scope == placemarkOverlay.getScope())
                        .map(PlacemarkOverlay::getMetadata)
                        .collect(Collectors.toList());

        // Now that all the metadata for the overlays in the scope set that was modified has been collected,
        // write it out to disk.
        try
        {
            // Check to see if the file exists.
            FilePath filePath = new FilePath(PlacemarkOverlayUtils.PERSIST_FILENAME);
            IVFSFile vfsFile = getVFS(scope).getFile(filePath);

            if (vfsFile == null)
            {
                vfsFile = getVFS(scope).createFile(filePath);
            }

            // Write the data to the file.
            PlacemarkOverlayUtils.writePlacemarkOverlayMetadata(vfsFile.getFile(), persistableData);
        } catch (Exception e)
        {
            logger.error("Failed to write out overlay data", e);
        }
    }

    /**
     * Restores all Placemark Overlay layers associated with this project.
     */
    private void restorePersistedData()
    {
        restorePersistedData(VFSScope.Global);
        restorePersistedData(VFSScope.Project);
    }

    /**
     * Restore the placemark overlays from the specified scope's VFS.
     *
     * @param scope The scope of the VFS from which the file will be obtained.
     */
    private void restorePersistedData(VFSScope scope)
    {
        File metadataPersistFile = new File(getFilePath(scope), PlacemarkOverlayUtils.PERSIST_FILENAME);

        if (!metadataPersistFile.canRead())
        {
            return;
        }

        try
        {
            List<Map<String, String>> dataList = PlacemarkOverlayUtils.readPlacemarkOverlayMetadata(metadataPersistFile);

            for (Map<String, String> data : dataList)
            {
                PlacemarkOverlay placemarkOverlay = PlacemarkOverlayUtils.loadFromPersistedData(data, scope, getVFS(scope));

                if (placemarkOverlay != null)
                {
                    addOverlay(placemarkOverlay, Boolean.parseBoolean(data.get(PlacemarkOverlay.LAYER_VISIBLE)), true);
                }
            }
        } catch (Exception e)
        {
            logger.error("Error restoring placemark layer.", e);
        }
    }

    /**
     * Check to see if a layer is a placemark layer.
     *
     * @param layer The layer to check.
     * @return {@code true} iff the layer is a placemark layer.
     */
    private boolean isLayerPlacemarkOverlay(IRaptorLayer layer)
    {
        return placemarkOverlayMap.containsValue(layer);
    }

    /**
     * Respond to a project opened event.  Load our data from VFS.
     */
    private void onProjectOpened(@Observes ProjectOpened event)
    {
        projectVFS = projectVFSManager.getVirtualFileSystem(OVERLAY_VFS_NAMESPACE, PLACEMARKS_VFS_SUBDIRECTORY);
        globalVFS = globalVFSManager.getVirtualFileSystem(OVERLAY_VFS_NAMESPACE, PLACEMARKS_VFS_SUBDIRECTORY);

        // Load our persisted data when the globe is ready
        globeManager.getPrimaryGlobeDelegate().doWhenReady(this::restorePersistedData);
    }

    @Override
    public void raptorLayerRemoved(IRaptorLayer raptorLayer)
    {
        if (isLayerPlacemarkOverlay(raptorLayer))
        {
            removeOverlay(raptorLayer);
        }
    }
}
