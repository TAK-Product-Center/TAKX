package gov.takx.core.view.gui.layers.viewmanager;

import gov.nasa.worldwind.SceneController;
import gov.nasa.worldwind.WWObject;
import gov.nasa.worldwind.avlist.AVKey;
import gov.nasa.worldwind.avlist.AVList;
import gov.nasa.worldwind.event.RenderingEvent;
import gov.nasa.worldwind.event.RenderingListener;
import gov.nasa.worldwind.geom.Position;
import gov.nasa.worldwind.geom.Sector;
import gov.nasa.worldwind.globes.ElevationModel;
import gov.nasa.worldwind.layers.TiledImageLayer;
import gov.nasa.worldwind.terrain.CompoundElevationModel;
import gov.nasa.worldwind.wms.WMSTiledImageLayer;
import gov.takx.api.annotation.Headless;
import gov.takx.api.annotation.InvokeAndWaitOnEDT;
import gov.takx.api.coordinatesystem.CoordinateSystem;
import gov.takx.api.coordinatesystem.ILocation;
import gov.takx.api.coordinatesystem.RaptorLocation;
import gov.takx.api.event.StartProjectServices;
import gov.takx.api.event.StopProjectServices;
import gov.takx.api.event.project.ProjectOpened;
import gov.takx.api.event.project.ProjectSaving;
import gov.takx.api.globe.IExtendedGlobeManager;
import gov.takx.api.layer.IRaptorLayer;
import gov.takx.api.layer.IRaptorLayerManager;
import gov.takx.api.layer.IRaptorLayerManagerListener;
import gov.takx.api.layer.LayerGroup;
import gov.takx.commons.utils.StringUtils;
import gov.takx.core.globe.LayerBoundsManager;
import gov.takx.core.globe.imagerywizard.InstallConstants;
import gov.takx.core.layer.RaptorLayerManager;
import gov.takx.core.project.ServiceOrdering;
import gov.takx.core.view.gui.layers.DefaultLayerUtils;
import gov.takx.platform.preferences.ProjectPreferenceEntry;
import gov.takx.platform.preferences.UserPreferenceEntry;
import gov.takx.platform.ui.UIResources;
import gov.takx.platform.utils.units.AltitudeUnits;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Priority;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import jakarta.inject.Inject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.SwingUtilities;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.lang.invoke.MethodHandles;
import java.net.URI;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.stream.Collectors;

import static gov.takx.platform.ui.ResourceDirectory.PLATFORM_ICONS;

/**
 */
@ApplicationScoped
public class LayerViewManager implements IRaptorLayerManagerListener, PropertyChangeListener, RenderingListener
{
    private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    private static final URI NETWORKED_ICON_URI = UIResources.getUri(PLATFORM_ICONS, "network.png");
    private static final URI NOT_NETWORKED_ICON_URI = UIResources.getUri(PLATFORM_ICONS, "network_disconnected.png");
    private static final URI VISIBLE_ICON_URI = UIResources.getUri(PLATFORM_ICONS, "visibility.png");

    private LayerViewPanel layerManagerPanel;
    private final Map<IRaptorLayer, Boolean> favoriteLayersMap = new ConcurrentHashMap<>();
    private final List<IRaptorLayer> favoriteLayers = new CopyOnWriteArrayList<>();
    private IRaptorLayer selectedLayer;
    private final List<IRaptorLayer> selectedLayers = new ArrayList<>();
    private boolean deleteEnabled;
    private boolean goToEnabled;
    private SceneController sceneController;
    private long lastViewStateId = -1;

    @Inject
    @Headless
    private boolean headless;

    @Inject
    private IRaptorLayerManager layerManager;

    @Inject
    private IExtendedGlobeManager globeManager;

    @Inject
    private DefaultLayerUtils defaultLayerUtils;

    @Inject
    private LayerBoundsManager layerBoundsManager;

    private void initUI()
    {
        layerManagerPanel = new LayerViewPanel(this);

        SwingUtilities.invokeLater(() -> LayerViewUtils.GROUP_ORDER.stream()
                .filter(group -> !LayerViewUtils.IGNORED_GROUPS.contains(group) && !LayerViewUtils.EXTRAS_TOOLS_GROUP_NAMES.contains(group))
                .forEach(layerManagerPanel::addGroup));

        setSelectedLayers(null);
    }

    /**
     * Get the layer view manager panel.
     */
    @InvokeAndWaitOnEDT
    public LayerViewPanel getPanel()
    {
        if (!headless && layerManagerPanel == null) initUI();
        return layerManagerPanel;
    }

    /**
     * Use {@link RaptorLayerManager} to move a layer from one location in its {@link LayerGroup} to a different location.
     */
    public void moveLayer(String groupName, int sourceIndex, int destIndex)
    {
        LayerGroup group = LayerViewUtils.nameToGroup(groupName);
        if (group == null) return;

        List<IRaptorLayer> layers = layerManager.getLayerList(group);
        IRaptorLayer layer = layers.get(layers.size() - 1 - sourceIndex);
        int convertedToRow = layers.size() - 1 - destIndex;
        layerManager.moveLayer(layer.getKey(), convertedToRow);
        layerManagerPanel.refreshTable(groupName);

        logger.debug("Moved layer '{}' {} to row = {}", layer, sourceIndex, destIndex);
    }

    /**
     * Get the selected layer.
     */
    public IRaptorLayer getSelectedLayer()
    {
        return selectedLayer;
    }

    /**
     * Get the selected layers.
     *
     * @return list of selected layers.
     */
    public List<IRaptorLayer> getSelectedLayers()
    {
        return new ArrayList<>(selectedLayers);
    }

    /**
     * Attempt to shift the Globe camera view to the selected layer.  This may not be possible if there is no selected
     * layer, or the selected layer doesn't provide valid location information.
     */
    public void goToSelectedLayer()
    {
        globeManager.getLastFocusedGlobe().goTo(selectedLayer);
    }

    /**
     * Determine whether a layer is a favorite.
     */
    public boolean isFavorite(IRaptorLayer layer)
    {
        if (!favoriteLayersMap.containsKey(layer)) setFavorite(layer, false);
        return favoriteLayersMap.get(layer);
    }

    /**
     * Set or unset a layer as a favorite.
     */
    public void setFavorite(IRaptorLayer layer, boolean favorite)
    {
        favoriteLayersMap.put(layer, favorite);
        if (favorite && !favoriteLayers.contains(layer))
        {
            favoriteLayers.add(layer);
        } else
        {
            favoriteLayers.remove(layer);
        }
        refreshFavorites();
    }

    /**
     * Get the favorite layer at a given row index.  This method is used to back the Favorite's layer table model.
     */
    public IRaptorLayer getFavoriteLayer(int index)
    {
        index = getFavoriteLayersCount() - 1 - index;
        if (index >= 0 && index < getFavoriteLayersCount()) return favoriteLayers.get(index);
        return null;
    }

    /**
     * Get the number of favorite layers.
     */
    public int getFavoriteLayersCount()
    {
        return favoriteLayers.size();
    }

    /**
     * Toggles the visibility of the bounding box for the {@link IRaptorLayer}
     * @param layer the layer to toggle the bounding box visibility for
     * @since 5.2
     */
    public void toggleBoundsLayer(@Nonnull IRaptorLayer layer)
    {
        if (layerBoundsManager.canShowBoundingBox(layer)) layerBoundsManager.toggleBoundsVisibility(layer);
    }

    /**
     * Generate an HTML tooltip for a layer.
     */
    public String getLayerTooltip(IRaptorLayer layer)
    {
        final var altitudeUnits = UserPreferenceEntry.DISPLAY_ALTITUDE_UNITS.getValueAs(AltitudeUnits.class);

        double minAltitude = layer.getMinActiveAltitude();
        double maxAltitude = layer.getMaxActiveAltitude();

        boolean isModel = layer.getLayerGroup() == LayerGroup.ELEVATION;

        StringBuilder sb = new StringBuilder("<html>");
        sb.append(layer.getDisplayName());
        sb.append("<br>");
        sb.append("Min alt: ").append(altitudeUnits.format(minAltitude, 0));
        sb.append(" Max alt: ").append(altitudeUnits.format(maxAltitude, 0));
        sb.append(getExtendedTooltip(layer));

        if (defaultLayerUtils.isDefaultLayer(layer))
        {
            sb.append("<br>Default ");
            sb.append(!isModel ? "Layer" : "Model");
        }

        if (layer.isLayerRendered())
        {
            sb.append("<br>");
            sb.append("<img src=\"");
            sb.append(VISIBLE_ICON_URI);
            sb.append("\">&nbsp;In View");
        }

        if (isModel || layer.getLayer() instanceof WMSTiledImageLayer)
        {
            sb.append("<br>");
            if (layer.isNetworkRetrievalEnabled())
            {
                sb.append("<img src=\"");
                sb.append(NETWORKED_ICON_URI);
                sb.append("\">&nbsp;Network Retrieval Enabled");
            } else
            {
                sb.append("<img src=\"");
                sb.append(NOT_NETWORKED_ICON_URI);
                sb.append("\">&nbsp;Network Retrieval Disabled");
            }
        }

        sb.append("</html>");
        return sb.toString();
    }

    /**
     * Get a {@link IRaptorLayer} from the {@link RaptorLayerManager} based upon its {@link LayerGroup}'s name and an
     * index within the group. This is the primary backing lookup method for the layer tables. The index is modified to
     * provide reverse list access within a layer group due to the need for layer tables to display layers in reverse
     * order. Reverse order corresponds to top listed layers displaying on the top of the WW Globe.
     */
    public IRaptorLayer getLayer(String groupName, int index)
    {
        LayerGroup group = LayerViewUtils.nameToGroup(groupName);
        if (group == null) return null;
        List<IRaptorLayer> layers = getLayerManager().getLayerList(group);
        return layers.get(layers.size() - 1 - index);
    }

    /**
     * Set the selected layers.  Go To and Properties buttons may be enabled or disabled depending on what is selected.
     */
    public void setSelectedLayers(List<IRaptorLayer> selectedLayers)
    {
        this.selectedLayers.clear();
        if (selectedLayers != null) this.selectedLayers.addAll(selectedLayers);

        // Setup delete and properties buttons
        if (selectedLayers == null || selectedLayers.isEmpty())
        {
            deleteEnabled = false;
            layerManagerPanel.setPropertiesEnabled(false);
        } else
        {
            layerManagerPanel.setPropertiesEnabled(true);
            deleteEnabled = selectedLayers.stream().noneMatch(IRaptorLayer::isPermanent);
        }
        layerManagerPanel.setDeleteEnabled(deleteEnabled);

        // Setup goTo button
        if (selectedLayers != null && !selectedLayers.isEmpty())
        {
            selectedLayer = selectedLayers.get(0);
            goToEnabled = layerBoundsManager.supportsGoTo(selectedLayer);
            layerManagerPanel.setGoToEnabled(goToEnabled);
            layerManagerPanel.onLayerSelected(selectedLayer);
        } else
        {
            goToEnabled = false;
            layerManagerPanel.setGoToEnabled(false);
            selectedLayer = null;
        }
    }

    /**
     * Determine whether a Go To-able layer is selected.
     */
    public boolean isGoToEnabled()
    {
        return goToEnabled;
    }

    public boolean isDeleteEnabled()
    {
        return deleteEnabled;
    }

    @Override
    public void raptorLayerAdded(IRaptorLayer raptorLayer)
    {
        update(raptorLayer, LayerEventType.ADDED);
    }

    @Override
    public void raptorModelAdded(IRaptorLayer raptorLayer)
    {
        update(raptorLayer, LayerEventType.ADDED);
    }

    @Override
    public void raptorLayerRemoved(IRaptorLayer raptorLayer)
    {
        update(raptorLayer, LayerEventType.REMOVED_SINGLE);
    }

    @Override
    public void raptorModelRemoved(IRaptorLayer raptorLayer)
    {
        update(raptorLayer, LayerEventType.REMOVED_SINGLE);
    }

    @Override
    public void raptorLayersRemoved()
    {
        update(null, LayerEventType.REMOVED_ALL);
    }

    @Override
    public void raptorLayerMoved(IRaptorLayer raptorLayer)
    {
        update(raptorLayer, LayerEventType.MOVED);
    }

    @Override
    public void raptorLayerSelected(IRaptorLayer raptorLayer)
    {
        update(raptorLayer, LayerEventType.SELECTED);
    }

    /**
     * Start our service.
     */
    private void startService(@SuppressWarnings("UnusedParameters") @Observes @Priority(ServiceOrdering.START_LayerViewManager) StartProjectServices event)
    {
        if (headless)
        {
            logger.debug("Not starting");
            return;
        }

        globeManager.getPrimaryGlobeDelegate().doWhenReady(this::globeReady);
        layerManager.addRaptorLayerManagerListener(this);

        // Force the panel to be realized if it is not already, and then load its state
        getPanel().loadState();

        SwingUtilities.invokeLater(() -> update(null, LayerEventType.LOAD_STATE));
    }

    /**
     * Stop our service.
     */
    private void stopService(@Observes @Priority(ServiceOrdering.STOP_GenericCoreService) StopProjectServices ignored)
    {
        if (headless) return;

        selectedLayer = null;
        sceneController = null;
        goToEnabled = false;
        if (layerManagerPanel != null) layerManagerPanel.clearState();
        favoriteLayersMap.clear();
        favoriteLayers.clear();
        layerManager.removeRaptorLayerManagerListener(this);
    }

    private void globeReady()
    {
        layerManager.getLayers().forEach(layer -> layer.addPropertyChangeListener(this));
        layerManager.getModelLayers().forEach(model -> model.addPropertyChangeListener(this));
        globeManager.getPrimaryGlobeDelegate().getWorldWindow().addRenderingListener(this);
        update(null, LayerEventType.GLOBE_INITIALIZED);
    }

    /**
     * Property change events from a specific {@link IRaptorLayer}.
     */
    @Override
    public void propertyChange(PropertyChangeEvent evt)
    {
        logger.debug("Raptor Layer change event: {}", evt);
        if (IRaptorLayer.VISIBLE.equals(evt.getPropertyName())) layerManagerPanel.repaint();
    }

    /**
     * When the project is opened, load the preferences.
     */
    private void onProjectOpened(@SuppressWarnings("unused") @Observes ProjectOpened event)
    {
        restoreFromPreferences(); // Attempt to load preferences
    }

    /**
     * When the project is saving, save the preferences.
     */
    private void onProjectSaving(@SuppressWarnings("unused") @Observes ProjectSaving event)
    {
        storeToPreferences();
    }

    /**
     * Attempt to remove any currently selected layers from the project.  A dialog will be displayed for the user to
     * confirm layer deletion.  This will remove the layers from the project.
     */
    public void deleteSelectedLayers()
    {
        final List<IRaptorLayer> filteredLayers =
                selectedLayers.stream().filter(layer -> !layer.isPermanent()).collect(Collectors.toList());

        if (!filteredLayers.isEmpty())
        {
            layerManager.removeLayers(filteredLayers, true);

            // CORE-3600: If the user actually does the delete, then the selectedLayers list will be updated
            // automatically when the layer removed event is processed when updateGroup is called.
            // So, don't clear it here.
        }
    }

    /**
     * Implementation of WW {@link RenderingListener#stageChanged} interface. Basically, this method tracks if/when each
     * layer is rendered on the globe. {@link IRaptorLayer#layerIsRenderedChanged} is updated upon layers rendering.
     */
    @Override
    public void stageChanged(RenderingEvent event)
    {
        // Three events are fired per frame, we only want to react to one of them
        if (!event.getStage().equals(RenderingEvent.AFTER_BUFFER_SWAP)) return;

        if (sceneController == null) sceneController = globeManager.getPrimaryGlobeDelegate().getWorldWindow().getSceneController();

        // We only want to perform these checks if the view has actually changed, so test the view state id.
        final long viewStateID = sceneController.getView().getViewStateID();

        if (lastViewStateId == viewStateID) return;

        lastViewStateId = viewStateID;

        // This is the time the entire scene was last rendered.
        final Long frameTimestamp = (Long) sceneController.getValue(AVKey.FRAME_TIMESTAMP);
        if (frameTimestamp == null) return;

        final long timestamp = frameTimestamp;
        boolean update = false;

        // Check imagery layers for recent render
        for (IRaptorLayer raptorLayer : layerManager.getLayerList(LayerGroup.IMAGERY))
        {
            boolean rendered = isRendered(raptorLayer, raptorLayer.getLayer(), timestamp);
            if (raptorLayer.layerIsRenderedChanged(rendered)) update = true;
        }

        // If a layer above was recently rendered, repaint some panels
        if (update)
        {
            layerManagerPanel.repaintGroup(LayerGroup.IMAGERY.getName());
            layerManagerPanel.repaintGroup(LayerGroup.ELEVATION.getName());
            layerManagerPanel.repaintGroup(LayerViewUtils.FAVORITES);

            logger.debug("Repainted layer manager panel at {}", timestamp);
        }
    }

    private void refreshFavorites()
    {
        layerManagerPanel.refreshFavorites();
    }

    private boolean isRendered(IRaptorLayer raptorLayer, WWObject layer, long sceneTimestamp)
    {
        if (layer instanceof TiledImageLayer || layer instanceof ElevationModel)
        {
            return isRecentlyRendered(layer, sceneTimestamp);
        } else if (raptorLayer.getLayerGroup() == LayerGroup.IMAGERY && layer != null)
        {
            // If the imagery layer is not a typical TiledImageLayer, then just do a basic view check
            Position eyePosition = sceneController.getView().getEyePosition();
            if (eyePosition != null)
            {
                final double elevation = eyePosition.getElevation();
                return raptorLayer.isVisible() &&
                        elevation >= raptorLayer.getMinActiveAltitude() && elevation <= raptorLayer.getMaxActiveAltitude();
            }
        }
        return false;
    }

    /**
     * Determine whether a layer/model has recently been rendered.  If the AVList is actually a
     * CompoundElevationModel, then this method will also recursively determine whether its children have recently
     * rendered.
     */
    private boolean isRecentlyRendered(WWObject layer, long sceneTimestamp)
    {
        // If the list is a compound elevation model, recursively check its children for recent renders.
        if (layer instanceof CompoundElevationModel)
        {
            for (ElevationModel subModel : ((CompoundElevationModel) layer).getElevationModels())
            {
                if (isRecentlyRendered(subModel, sceneTimestamp)) return true;
            }
        }

        // Test specific layer/model for recent rendering
        final Long layerTimestamp = (Long) layer.getValue(AVKey.FRAME_TIMESTAMP);
        return layerTimestamp != null && Math.abs(layerTimestamp - sceneTimestamp) == 0;
    }

    /**
     * Add or remove a layer to/from a group (depending on the event type).  If no layer was specified, then update all
     * groups.
     */
    private void update(IRaptorLayer layer, LayerEventType eventType)
    {
        if (layer == null) // No layer was provided, so just update everything
        {
            Arrays.stream(LayerGroup.values()).forEach(this::updateGroup);
            return;
        }

        LayerGroup layerGroup = layer.getLayerGroup();
        updateGroup(layerGroup);

        if (LayerEventType.ADDED == eventType)
        {
            notifyGroupLayerAdded(layerGroup.getName(), layer);
            layer.addPropertyChangeListener(this);
        } else if (LayerEventType.REMOVED_SINGLE == eventType)
        {
            setFavorite(layer, false);
            layer.removePropertyChangeListener(this);
        }
    }

    /**
     * Update a group.  This will cause the group's task pane/table to be refreshed.
     */
    private void updateGroup(LayerGroup layerGroup)
    {
        int count = getLayerManager().getLayerCount(layerGroup);
        String groupName = LayerViewUtils.convertLayerGroupName(layerGroup.getName());
        boolean visible = count > 0;
        if (LayerViewUtils.EXTRA_TOOLS.equals(groupName)) visible = true;
        getPanel().updateGroup(groupName, visible);
    }

    private void notifyGroupLayerAdded(String group, IRaptorLayer layer)
    {
        group = LayerViewUtils.convertLayerGroupName(group);
        layerManagerPanel.addGroupLayer(group, layer);
    }

    private IRaptorLayerManager getLayerManager()
    {
        return layerManager;
    }

    private void storeToPreferences()
    {
        // Using layer IDs because they are the only field that I am aware of that should be unique and consistent
        // across different layers.
        final List<Integer> ids = favoriteLayers.stream()
                .map(IRaptorLayer::getId)
                .filter(Objects::nonNull)
                .collect(Collectors.toList());

        ProjectPreferenceEntry.LAYER_MANAGER_FAVORITES.put(StringUtils.join(ids, ",", null));

        if (layerManagerPanel != null)
        {
            ProjectPreferenceEntry.LAYER_MANAGER_OPEN_GROUPS.put(StringUtils.join(layerManagerPanel.getOpenGroups(), ",", null));
        }
    }

    private void restoreFromPreferences()
    {
        final String favoriteList = ProjectPreferenceEntry.LAYER_MANAGER_FAVORITES.getStringValue();
        if (!favoriteList.isEmpty())
        {
            Map<Integer, IRaptorLayer> idToLayer = new HashMap<>();
            for (IRaptorLayer layer : getLayerManager().getLayers()) idToLayer.put(layer.getId(), layer);
            for (IRaptorLayer layer : getLayerManager().getModelLayers()) idToLayer.put(layer.getId(), layer);

            favoriteLayers.clear();

            for (int id : StringUtils.splitToInt(favoriteList, -1))
            {
                if (id == -1) continue;

                IRaptorLayer layer = idToLayer.get(id);
                if (layer != null)
                {
                    logger.debug("Restoring favorites layer: {}", layer);
                    setFavorite(layer, true);
                }
            }
            refreshFavorites();
        }

        final String openGroups = ProjectPreferenceEntry.LAYER_MANAGER_OPEN_GROUPS.getStringValue();
        if (!openGroups.isEmpty() && layerManagerPanel != null)
        {
            layerManagerPanel.setOpenGroups(Arrays.asList(favoriteList.split(",")));
        }
    }

    /**
     * Get extended information to put in a tooltip for a specified {@link IRaptorLayer}.
     *
     * @param layer The layer of interest
     * @return String of extended layer information.
     * @since 1.43
     */
    private String getExtendedTooltip(IRaptorLayer layer)
    {
        StringBuilder sb = new StringBuilder();
        AVList avList = layer.getLayerGroup() == LayerGroup.IMAGERY ? layer.getLayer() : null;

        if (avList != null)
        {
            sb.append(getSectorString(avList));
            if (avList.hasKey(InstallConstants.SIZE))
            {
                String bytesString = avList.getValue(InstallConstants.SIZE).toString();
                double kb = Double.parseDouble(bytesString) / (1000D * 1000D);
                sb.append("<br>").append("Size: ").append(String.format("%.2f MB", kb));
            }
            if (avList.hasKey(InstallConstants.RESOLUTION))
            {
                String resolutionString = avList.getValue(InstallConstants.RESOLUTION).toString();
                double resolutionRadians = Double.parseDouble(resolutionString);
                double globeRadius = globeManager.getPrimaryGlobeDelegate()
                        .getWorldWindow()
                        .getModel()
                        .getGlobe()
                        .getRadius();
                double resolution = resolutionRadians * globeRadius;
                sb.append("<br>").append("Resolution: ").append(String.format("%.2f m", resolution));
            }
            if (avList.hasKey(InstallConstants.DATA_SOURCE))
            {
                sb.append("<br>").append(avList.getValue(InstallConstants.DATA_SOURCE));
            }
        }
        return sb.toString();
    }

    /**
     * Get a formatted sector String from a WW layer/elevation model.  Unfortunately, Sector information is not stored
     * in a consistent way.  It may be an actual Sector, an un-formatted String, or a set of min/max lat/lon Strings
     * that can be parsed as Doubles.
     *
     * @param avList WW layer or elevation model.
     * @return a String representing the Sector information from the layer/elevation model.
     */
    private String getSectorString(AVList avList)
    {
        Double minLat = null;
        Double minLon = null;
        Double maxLat = null;
        Double maxLon = null;

        // Best case scenario - an actual Sector object is stored in the avList;
        Object o = avList.getValue(AVKey.SECTOR);
        if (o instanceof Sector s)
        {
            // Ignore Sectors that cover the entire globe or are empty.
            if (!s.equals(Sector.FULL_SPHERE) && !s.equals(Sector.EMPTY_SECTOR))
            {
                minLat = s.getMinLatitude().degrees;
                minLon = s.getMinLongitude().degrees;
                maxLat = s.getMaxLatitude().degrees;
                maxLon = s.getMaxLongitude().degrees;
            }
        } else
        {
            // Otherwise, look for key/values for min/max lat/lon Strings and parse them as doubles.
            Object minLatObject = avList.getValue("minLatitudeDegrees");
            Object minLonObject = avList.getValue("minLongitudeDegrees");
            Object maxLatObject = avList.getValue("maxLatitudeDegrees");
            Object maxLonObject = avList.getValue("maxLongitudeDegrees");
            if (minLatObject instanceof String &&
                    minLonObject instanceof String &&
                    maxLatObject instanceof String &&
                    maxLonObject instanceof String)
            {
                minLat = Double.parseDouble((String) minLatObject);
                minLon = Double.parseDouble((String) minLonObject);
                maxLat = Double.parseDouble((String) maxLatObject);
                maxLon = Double.parseDouble((String) maxLonObject);
            }
        }

        // If actual min/max lat/lon information was found, determine the coordinate system and units of measure.
        //noinspection ConstantConditions
        if (minLat != null && minLon != null && maxLat != null && maxLon != null)
        {
            final var coordinateSystem = UserPreferenceEntry.COORDINATE_SYSTEM.getValueAs(CoordinateSystem.class);
            final var altitudeUnits = UserPreferenceEntry.DISPLAY_ALTITUDE_UNITS.getValueAs(AltitudeUnits.class);

            // Finally create a String for the Sector.
            return getSectorString(minLat, minLon, maxLat, maxLon, coordinateSystem, altitudeUnits);
        }
        return "";
    }

    private String getSectorString(double minLat,
                                   double minLon,
                                   double maxLat,
                                   double maxLon,
                                   CoordinateSystem coordinateSystem,
                                   AltitudeUnits altitudeUnits)
    {
        return "<br>Min Corner: "
                + formatLocation(minLat, minLon, coordinateSystem, altitudeUnits)
                + "<br>Max Corner: "
                + formatLocation(maxLat, maxLon, coordinateSystem, altitudeUnits);
    }

    private String formatLocation(double lat, double lon, CoordinateSystem coordinateSystem,
                                  AltitudeUnits altitudeUnits)
    {
        ILocation location = new RaptorLocation(lat, lon);
        return CoordinateSystem.getFormattedLocationString(location, coordinateSystem, altitudeUnits);
    }

    /**
     * Enum to manage different types of {@link IRaptorLayer} events.
     */
    private enum LayerEventType
    {
        ADDED, REMOVED_SINGLE, REMOVED_ALL, MOVED, SELECTED, LOAD_STATE, GLOBE_INITIALIZED
    }
}
