package gov.takx.core.layer;

import gov.nasa.worldwind.layers.Layer;
import gov.takx.api.annotation.InvokeAndWaitOnEDT;
import gov.takx.api.annotation.InvokeOnEDT;
import gov.takx.api.annotation.ProgramPreference;
import gov.takx.api.annotation.UiEventBus;
import gov.takx.api.configuration.IProgramEnvironment;
import gov.takx.api.event.StartProjectServices;
import gov.takx.api.event.StopProjectServices;
import gov.takx.api.event.project.ProjectSaving;
import gov.takx.api.globe.IGlobeLayerManager;
import gov.takx.api.layer.IRaptorLayer;
import gov.takx.api.layer.IRaptorLayerManager;
import gov.takx.api.layer.IRaptorLayerManagerListener;
import gov.takx.api.layer.LayerGroup;
import gov.takx.api.persist.ILayerPersist;
import gov.takx.api.project.IProjectManager;
import gov.takx.api.ui.event.GlobeRemovedEvent;
import gov.takx.commons.swing.AlwaysOnTopJOptionPane;
import gov.takx.commons.utils.ParseUtils;
import gov.takx.commons.utils.SafeXmlDocumentUtils;
import gov.takx.commons.utils.StringUtils;
import gov.takx.core.globe.tak.CacheConflictException;
import gov.takx.core.globe.tak.OfflineCacheRegistry;
import gov.takx.core.globe.tak.elevation.ElevationLayerManager;
import gov.takx.core.project.ServiceOrdering;
import gov.takx.dataservices.persist.RaptorLayer;
import gov.takx.platform.preferences.PreferenceEntry;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Priority;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import jakarta.inject.Inject;
import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Predicate;

import static gov.takx.api.layer.IRaptorLayer.DEFAULT_MAX_ACTIVE_ALTITUDE;
import static gov.takx.api.layer.IRaptorLayer.DEFAULT_MIN_ACTIVE_ALTITUDE;

/**
 * Service to manage all layers, organized by {@link LayerGroup layer group}.
 */
@ApplicationScoped
public class RaptorLayerManager implements IRaptorLayerManager
{
    private static final Logger logger = LoggerFactory.getLogger(RaptorLayerManager.class.getName());

    // The set of layer groups that we save and restore
    // Note that analytic surfaces and surface structures are not saved (currently)
    private static final List<LayerGroup> LAYER_GROUPS_TO_SAVE = List.of(
            LayerGroup.EXTRAS,
            LayerGroup.IMAGERY,
            LayerGroup.MODELS,
            LayerGroup.FEATURES,
            LayerGroup.SURFACE_IMAGERY,
            LayerGroup.TOOLS,
            LayerGroup.SHAPE_FILES,
            LayerGroup.SURFACE_STRUCTURES,
            LayerGroup.ELEVATION);

    private static final Predicate<IRaptorLayerManagerListener> PRIORITY_LAYER =
            listener -> (listener instanceof IGlobeLayerManager || listener instanceof ElevationLayerManager);
    private static final Predicate<IRaptorLayerManagerListener> NOT_PRIORITY_LAYER = PRIORITY_LAYER.negate();

    // Map from layer group to list of layers for that group
    private final Map<LayerGroup, List<IRaptorLayer>> layerLists = new ConcurrentHashMap<>();

    private final ILayerPersist layerPersist;
    private final IProgramEnvironment programEnvironment;
    private final IProjectManager projectManager;
    private final EventBus uiEventBus;
    private final double defaultSplitScale;
    private final boolean closedNetwork;
    private final LayerDefinitionSyncManager layerDefinitionSyncManager;
    private final OfflineCacheRegistry offlineCacheRegistry;

    private final List<IRaptorLayerManagerListener> listeners = new CopyOnWriteArrayList<>();

    @Inject
    RaptorLayerManager(ILayerPersist layerPersist, IProgramEnvironment programEnvironment, IProjectManager projectManager,
                       @UiEventBus EventBus uiEventBus,
                       @ProgramPreference(PreferenceEntry.GLOBE_SPLIT_SCALE) double defaultSplitScale,
                       @ProgramPreference(PreferenceEntry.CLOSED_NETWORK) boolean closedNetwork,
                       LayerDefinitionSyncManager layerDefinitionSyncManager, OfflineCacheRegistry offlineCacheRegistry)
    {
        this.layerPersist = layerPersist;
        this.programEnvironment = programEnvironment;
        this.projectManager = projectManager;
        this.uiEventBus = uiEventBus;
        this.defaultSplitScale = defaultSplitScale;
        this.closedNetwork = closedNetwork;
        this.layerDefinitionSyncManager = layerDefinitionSyncManager;
        this.offlineCacheRegistry = offlineCacheRegistry;
    }

    @Override
    public void removeRaptorLayerManagerListener(IRaptorLayerManagerListener raptorLayerManagerListener)
    {
        listeners.remove(raptorLayerManagerListener);
    }

    @Override
    public void addRaptorLayerManagerListener(IRaptorLayerManagerListener raptorLayerManagerListener)
    {
        listeners.add(raptorLayerManagerListener);
    }

    @Override
    public IRaptorLayer getLayer(String key)
    {
        return layerLists.values().stream()
                .flatMap(Collection::stream)
                .filter(raptorLayer -> raptorLayer.getKey().equals(key))
                .findFirst()
                .orElse(null);
    }

    @Override
    public IRaptorLayer addLayer(String name, String className, LayerGroup layerGroup)
    {
        return addLayer(name, name, className, true, false, null, layerGroup, null, null, false);
    }

    @Override
    public IRaptorLayer addLayer(String name, String displayName, String className, boolean visible, boolean pickable,
                                 String iconPath, LayerGroup layerGroup, String type, String restorableXMLString,
                                 boolean permanent)
    {
        return addLayer(name, displayName, className, visible, pickable, iconPath, layerGroup,
                type, restorableXMLString, permanent,
                defaultSplitScale, DEFAULT_MIN_ACTIVE_ALTITUDE, DEFAULT_MAX_ACTIVE_ALTITUDE);
    }

    @Override
    public IRaptorLayer addLayer(String name, String displayName, String className, boolean visible, boolean pickable,
                                 String iconPath, LayerGroup layerGroup, String type, String restorableXMLString,
                                 boolean permanent, double splitScale, double minActiveAltitude, double maxActiveAltitude)
    {
        final RaptorLayer layer = new RaptorLayer(name, displayName, className, null, visible, pickable,
                iconPath, layerGroup, type, restorableXMLString, permanent, splitScale,
                minActiveAltitude, maxActiveAltitude, true);

        addLayerToGroup(layer, layerGroup);

        return layer;
    }

    @Override
    public IRaptorLayer addLayer(Layer layer, LayerGroup layerGroup)
    {
        return addLayer(layer, layerGroup, ALL_GLOBES);
    }

    @Override
    public IRaptorLayer addLayer(Layer layer, LayerGroup layerGroup, int globeId)
    {
        final RaptorLayer raptorLayer = new RaptorLayer(layer, true, false,
                null, layerGroup, null, layer.getRestorableState(), false,
                defaultSplitScale, DEFAULT_MIN_ACTIVE_ALTITUDE, DEFAULT_MAX_ACTIVE_ALTITUDE);

        if (globeId != ALL_GLOBES) raptorLayer.setSpecificGlobeId(globeId);

        addLayerToGroup(raptorLayer, layerGroup);

        return raptorLayer;
    }

    @Override
    public IRaptorLayer addLayer(com.atakmap.map.layer.Layer layer, LayerGroup layerGroup, int globeId)
    {
        return addLayer(layer, true, false, null, layerGroup, null, null, false, globeId);
    }

    @Override
    public IRaptorLayer addLayer(com.atakmap.map.layer.Layer layer, boolean visible, boolean pickable, String iconPath, LayerGroup layerGroup,
                                 String type, String restorableXMLString, boolean permanent, int globeId)
    {
        final RaptorLayer raptorLayer = new RaptorLayer(layer, visible, pickable, iconPath, layerGroup, type, restorableXMLString, permanent,
                defaultSplitScale, DEFAULT_MIN_ACTIVE_ALTITUDE, DEFAULT_MAX_ACTIVE_ALTITUDE);

        if (globeId != ALL_GLOBES) raptorLayer.setSpecificGlobeId(globeId);

        addLayerToGroup(raptorLayer, layerGroup);

        return raptorLayer;
    }

    @Override
    public IRaptorLayer addLayerFromXML(String name, String configFileName, LayerGroup layerGroup)
    {
        return addLayerFromXML(name, name, configFileName, true, false, null, layerGroup, null, configFileName, false,
                defaultSplitScale, DEFAULT_MIN_ACTIVE_ALTITUDE, DEFAULT_MAX_ACTIVE_ALTITUDE);
    }

    @Override
    public IRaptorLayer addLayerFromXML(String name, String displayName, String configFileName, boolean visible,
                                        boolean pickable, String iconPath, LayerGroup layerGroup, String type,
                                        String restorableXMLString, boolean permanent, double splitScale,
                                        double minActiveAltitude, double maxActiveAltitude)
    {
        final RaptorLayer layer = new RaptorLayer(name, displayName, null, configFileName, visible, pickable, iconPath, layerGroup,
                type, restorableXMLString, permanent, splitScale, minActiveAltitude, maxActiveAltitude, true);

        addLayerToGroup(layer, layerGroup);

        return layer;
    }

    /**
     * Add a newly created {@link IRaptorLayer} to the given group and notify the listeners on the EDT.
     *
     * @param layer the new layer
     * @param layerGroup Layer group to add the layer to
     */
    private synchronized void addLayerToGroup(IRaptorLayer layer, LayerGroup layerGroup)
    {
        final var layerList = getRawLayerList(layerGroup);

        // If this new layer is in a persistable group, and it is not a pre-constructed TAK layer,
        // then set the order key and persist the layer.
        boolean persisted = false;
        if (LAYER_GROUPS_TO_SAVE.contains(layer.getLayerGroup()) && layer.getTakConstructionLayer() == null)
        {
            // Adding a layer to the end of the list doesn't require that we recalculate (and re-persist) all
            // the layers in the group.  We just need to ensure that the order key of the new layer is set properly.
            final int maxOrderKey = layerList.stream().mapToInt(IRaptorLayer::getOrderkey).max().orElse(-1);
            layer.setOrderkey(maxOrderKey + 1);

            layerPersist.persistLayer(layer);
            persisted = true;
        }

        layerList.add(layer);

        try
        {
            fireLayerAdded(layer);
        } catch (CacheConflictException e)
        {
            logger.debug("Deleting conflicting layer: {} from group {}", layer, layerGroup);
            layerList.remove(layer);
            if (persisted) layerPersist.deleteLayer(layer);
            throw e;
        }
    }

    /**
     * Send notifications for all the layers in a given group.
     *
     * @param layerGroup The group of layers for which to fire added events
     */
    private void fireGroupLayersAdded(final LayerGroup layerGroup)
    {
        final var badLayers = new ArrayList<IRaptorLayer>();
        final var layerList = getRawLayerList(layerGroup);

        layerList.forEach(layer -> {
            try
            {
                fireLayerAdded(layer);
            } catch (CacheConflictException e)
            {
                logger.error("Cannot add layer '{}' due to: {}", layer, e.getMessage());
                badLayers.add(layer);
            }
        });

        badLayers.forEach(layerList::remove);
    }

    /**
     * Send notifications that a layer has been added.  Globe layer managers are notified on the calling thread, while
     * all other listeners are notified on the EDT.
     *
     * @param layer The layer that has been added
     */
    private void fireLayerAdded(final IRaptorLayer layer)
    {
        // Dispatch the GlobeLayerManager callbacks immediately.  This ensures that the associated Globe/WW layer is
        // created synchronously with this layer creation.  If this is not done, then uses of new RaptorLayer instances
        // could have null underlying layers since this operation was previously pushed to the EDT (thus asynchronously
        // w.r.t. the creation of the RaptorLayer).

        listeners.stream()
                .filter(PRIORITY_LAYER)
                .forEach(listener -> listener.raptorLayerAdded(layer));

        // For legacy implementations, we still send out "model" events until we remove the deprecated events
        if (layer.getLayerGroup() == LayerGroup.ELEVATION)
        {
            listeners.stream()
                    .filter(PRIORITY_LAYER)
                    .forEach(listener -> listener.raptorModelAdded(layer));
        }

        // Invoke the rest on the EDT
        SwingUtilities.invokeLater(() -> listeners.stream()
                .filter(NOT_PRIORITY_LAYER)
                .forEach(listener -> listener.raptorLayerAdded(layer)));

        if (layer.getLayerGroup() == LayerGroup.ELEVATION)
        {
            SwingUtilities.invokeLater(() -> listeners.stream()
                    .filter(NOT_PRIORITY_LAYER)
                    .forEach(listener -> listener.raptorModelAdded(layer)));
        }
    }

    @Nonnull
    @Override
    public List<IRaptorLayer> getLayerList(LayerGroup layerGroup)
    {
        return List.copyOf(getRawLayerList(layerGroup));
    }

    /**
     * @return Direct reference to the list of layers for the given layer group.
     * @since 5.1
     */
    @Nonnull
    private List<IRaptorLayer> getRawLayerList(LayerGroup layerGroup)
    {
        return layerLists.computeIfAbsent(layerGroup, k -> new CopyOnWriteArrayList<>());
    }

    @Override
    public int getIndex(IRaptorLayer layer)
    {
        return getLayerList(layer.getLayerGroup()).indexOf(layer);
    }

    @Override
    public int getLayerCount(LayerGroup layerGroup)
    {
        return getLayerList(layerGroup).size();
    }

    /**
     * Clear the contents of all the layer lists and fire events to listeners that the layers have been removed.
     */
    @InvokeAndWaitOnEDT
    private void clearLayers()
    {
        // Clear the lists and remove listeners
        for (List<IRaptorLayer> raptorLayers : layerLists.values())
        {
            raptorLayers.forEach(IRaptorLayer::removeAllRaptorLayerListeners);
            raptorLayers.clear(); // Remove all the layers in this group
        }

        listeners.forEach(IRaptorLayerManagerListener::raptorLayersRemoved);
        listeners.clear();
    }

    /**
     * Handle the removal of a globe.  Remove any layers that were added specifically to this globe.
     *
     * @param e The globe removal event
     */
    @SuppressWarnings("UnusedDeclaration")
    @Subscribe
    public void onGlobeRemoved(GlobeRemovedEvent e)
    {
        int globeId = e.getGlobeId();
        if (globeId > 1)
        {
            final List<IRaptorLayer> layersToRemove = layerLists.values().stream()
                    .flatMap(raptorLayers -> raptorLayers.stream()
                            .filter(raptorLayer -> raptorLayer.getSpecificGlobeId() == globeId)
                            .toList().stream())
                    .toList();

            layersToRemove.forEach(layerToRemove -> removeLayer(layerToRemove, false));
        }
    }

    @Override
    public void removeLayers(List<IRaptorLayer> layers, boolean confirm)
    {
        final boolean remove = !confirm ||
                confirmDelete("Are you sure you want to delete " + (layers.size() > 1 ? "these layers?" : "this layer?"));

        if (remove) layers.forEach(this::removeLayer);
    }

    @Override
    public void removeLayer(IRaptorLayer layer, boolean confirm)
    {
        final boolean remove = !confirm || confirmDelete("Are you sure you want to delete this layer?");
        if (remove) removeLayer(layer);
    }

    private boolean confirmDelete(String msg)
    {
        final Object[] dlgButtons = {"Delete", "Cancel"};
        final int result = AlwaysOnTopJOptionPane.showOptionDialog(msg, "Confirm Delete",
                JOptionPane.WARNING_MESSAGE, JOptionPane.YES_NO_OPTION, null,
                dlgButtons, dlgButtons[1]);

        return result == 0;
    }

    private void removeLayer(final IRaptorLayer layer)
    {
        final LayerGroup layerGroup = layer.getLayerGroup();
        logger.debug("Deleting layer: {} from group {}", layer, layerGroup);

        layer.setDeleted(true);
        getRawLayerList(layerGroup).remove(layer);
        offlineCacheRegistry.unregisterCacheFile(layer.getName());

        // Only persist the deletion of this layer if it is in our list of groups to persist
        if (LAYER_GROUPS_TO_SAVE.contains(layerGroup)) layerPersist.persistLayer(layer);

        fireLayerRemoved(layer);
    }

    @InvokeOnEDT
    private void fireLayerRemoved(IRaptorLayer layer)
    {
        listeners.forEach(listener -> listener.raptorLayerRemoved(layer));

        // For legacy implementations, we still send out "model" events until we remove the deprecated events
        if (layer.getLayerGroup() == LayerGroup.ELEVATION)
        {
            listeners.forEach(listener -> listener.raptorModelRemoved(layer));
        }

        layer.removeAllRaptorLayerListeners(); // Clean up any leftover listeners
    }

    @Override
    public synchronized void moveLayer(final String key, final int index)
    {
        final IRaptorLayer layer = getLayer(key);
        if (layer == null) return;

        final var layerList = getRawLayerList(layer.getLayerGroup());

        if (index < 0 || index >= layerList.size()) return;

        layerList.remove(layer);
        layerList.add(index, layer);

        // Moving a layer requires that we recalculate the order key for all layers in the same group
        updateLayerOrder(layerList);

        fireLayerMoved(layer);
    }

    @InvokeOnEDT
    private void fireLayerMoved(IRaptorLayer layer)
    {
        listeners.forEach(listener -> listener.raptorLayerMoved(layer));
    }

    @Override
    public List<IRaptorLayer> getLayers()
    {
        // Add all the layers except elevation models
        return layerLists.entrySet().stream()
                .filter(entry -> entry.getKey() != LayerGroup.ELEVATION)
                .flatMap(entry -> entry.getValue().stream())
                .toList();
    }

    // When we remove this deprecated method, make sure to include ELEVATION layers in getLayers() above
    @Override
    public List<IRaptorLayer> getModelLayers()
    {
        return getLayerList(LayerGroup.ELEVATION);
    }

    @Override
    public void selectLayer(IRaptorLayer layer)
    {
        listeners.forEach(listener -> listener.raptorLayerSelected(layer));
    }

    /**
     * Start the service.  Load all the layer definitions.
     */
    private void startService(@Observes @Priority(ServiceOrdering.START_RaptorLayerManager) StartProjectServices event)
    {
        event.getProgressMonitor().setText("Loading layer manager...");

        restoreLayers();
        layerDefinitionSyncManager.loadImageryLayers();

        uiEventBus.register(this);
    }

    /**
     * Stop our service.
     */
    private void stopService(@Observes @Priority(ServiceOrdering.STOP_RaptorLayerManager) StopProjectServices event)
    {
        if (uiEventBus.isRegistered(this)) uiEventBus.unregister(this);

        clearLayers();
    }

    /**
     * Handle the saving event, persist all layers.
     *
     * @since 2.37
     */
    private void onProjectSaving(@Observes ProjectSaving event)
    {
        LAYER_GROUPS_TO_SAVE.forEach(group -> getRawLayerList(group).forEach(layerPersist::persistLayer));
    }

    /**
     * Persist the list of layers as a group and save the current ordering by setting the orderKey on each layer.
     *
     * @param layers Layer list to persist
     * @since 5.1 (name change)
     */
    private void updateLayerOrder(List<IRaptorLayer> layers)
    {
        int orderKey = 0;
        for (IRaptorLayer layer : layers)
        {
            if (shouldPersist(layer))
            {
                layer.setOrderkey(orderKey++);
                layerPersist.persistLayer(layer);
            }
        }
    }

    /**
     * Determine if a layer should be stored to persistence.  In the case that a layer is defined by a class name
     * and that class cannot be accessed from the core classloader (which includes the WW classes), then that
     * class will not be restorable because WW will not be able to construct an instance of it.
     *
     * @param layer Layer to test
     * @return {@code true} if the layer can be restored (and thus should be persisted), {@code false} otherwise
     * @since 3.4
     */
    private boolean shouldPersist(IRaptorLayer layer)
    {
        if (layer.isPermanent()) return true;

        final String className = layer.getClassName();
        if (className == null || className.startsWith("gov.nasa.worldwind") || className.startsWith("gov.takx.core")) return true;

        try
        {
            Class.forName(className);
            return true;
        } catch (ClassNotFoundException e)
        {
            logger.debug("Layer {} will not be persisted: {}", layer.getKey(), className);
            return false;
        }
    }

    private void restoreLayers()
    {
        try
        {
            boolean newProject = projectManager.getCurrentOpenProject().isNew();

            if (!newProject) loadExistingLayers();

            final File layersFile = programEnvironment.getGlobeLayersFile();
            if (logger.isInfoEnabled()) logger.info("Loading layers from {}", layersFile.getAbsolutePath());

            final var document = SafeXmlDocumentUtils.getDocumentBuilder().parse(layersFile);

            final var xpath = XPathFactory.newInstance().newXPath();
            final var layerGroups = (NodeList) xpath.evaluate("/layerGroups/layerGroup", document, XPathConstants.NODESET);

            for (int i = 0; i < layerGroups.getLength(); i++)
            {
                final var layerGroupNode = layerGroups.item(i);
                final var layerGroup = LayerGroup.valueOf(getStringFromNode(xpath, "@name", layerGroupNode));
                final var layers = (NodeList) xpath.evaluate("layer", layerGroupNode, XPathConstants.NODESET);

                // For all projects, we create any missing layers that were not previously deleted by the user.
                // This handles both the new project case and the case of a project being upgraded with a
                // configuration that may include new default layers.
                createLayersFromNodes(layerGroup, layers, xpath);
            }

            // If this is an existing project, then we need to send events for the restored persistent layers.
            if (!newProject)
            {
                // Since all layers that are NOT persisted will have been newly created even on an existing
                // project, we only want to fire layer added events for layers that ARE persisted.
                LAYER_GROUPS_TO_SAVE.forEach(this::fireGroupLayersAdded);
            }
        } catch (Exception e)
        {
            logger.error("Failed to load layer definitions", e);
            throw new RuntimeException("Failed to restore layer state", e);
        }
    }

    /**
     * Load all the layers defined in the database into their respective groups.  Avoid loading duplicates that
     * may exist (for an unknown reason).
     */
    private void loadExistingLayers()
    {
        logger.debug("Retrieving layers from the database");

        for (LayerGroup group : LAYER_GROUPS_TO_SAVE)
        {
            // Apparently, there are conditions under which the layers can end up being saved multiple times
            // in the DB (I haven't tracked that bug down yet).  We need to protect against this condition since
            // it can lead to lots of strange behavior, especially in WW renderable layers.

            final List<IRaptorLayer> uniqueLayers = new ArrayList<>();
            final Set<String> layerKeys = new HashSet<>();

            // Query all layers that are not deleted
            for (IRaptorLayer layer : layerPersist.query(group, false))
            {
                // If we are on a closed network, then don't bother adding our system managed layers
                if (closedNetwork && ((RaptorLayer) layer).isSystemManaged())
                {
                    logger.debug("Skipping layer {} on a closed network", layer.getDisplayName());
                    continue;
                }

                // Since RaptorLayer doesn't define an equals(), we have to scan the hard way
                final String key = layer.getKey();

                if (layerKeys.add(key))
                {
                    if (logger.isDebugEnabled()) logger.debug("add layer {}/{}", group, layer.getName());
                    uniqueLayers.add(layer);
                } else
                {
                    logger.error("Duplicate layer definition found in database, skipping duplicate of: {}", key);
                }
            }

            layerLists.put(group, uniqueLayers);
        }
    }

    /**
     * Create any layers (as defined in the XML config file) that are missing from the current set of layers for
     * the layer group specified.
     *
     * @param layerGroup The layer group in which we are to create layers
     * @param layerNodes The node list of layers from the XML layer group definition
     * @param xpath      The XPath processor to use for querying the node
     * @throws XPathExpressionException on any failure in Xpath processing
     */
    private void createLayersFromNodes(LayerGroup layerGroup, NodeList layerNodes, XPath xpath)
            throws XPathExpressionException
    {
        final var layerList = getRawLayerList(layerGroup);
        final var deletedLayerNames = layerPersist.query(layerGroup, true).stream().map(IRaptorLayer::getName).toList();

        for (int i = 0, nodeCount = layerNodes.getLength(); i < nodeCount; i++)
        {
            final Node layerNode = layerNodes.item(i);
            final String layerName = getStringFromNode(xpath, "@name", layerNode);

            // If the layer is present or has been deleted, we don't want to create a new layer
            if (findLayer(layerList, layerName) == null && !deletedLayerNames.contains(layerName))
            {
                // If we are on a closed network, then don't bother adding our system managed layers
                final boolean systemManaged = Boolean.parseBoolean(getStringFromNode(xpath, "@systemManaged", layerNode));

                if (systemManaged && closedNetwork)
                {
                    logger.debug("Skipping layer {} on a closed network", layerName);
                    continue;
                }

                final String displayName = getStringFromNode(xpath, "@displayName", layerNode);
                final String className = getStringFromNode(xpath, "@class", layerNode);
                final String configFileName = getStringFromNode(xpath, "@configFile", layerNode);
                final String iconPath = getStringFromNode(xpath, "@iconPath", layerNode);
                final String type = getStringFromNode(xpath, "@type", layerNode);
                final boolean visible = toBoolean(getStringFromNode(xpath, "@visible", layerNode));
                final boolean pickable = toBoolean(getStringFromNode(xpath, "@pickable", layerNode));
                final boolean permanent = toBoolean(getStringFromNode(xpath, "@permanent", layerNode));
                final double splitScale = ParseUtils.parseDouble(getStringFromNode(xpath, "@splitScale", layerNode), defaultSplitScale);
                final double minActiveAltitude = ParseUtils.parseDouble(getStringFromNode(xpath, "@minActiveAltitude", layerNode), DEFAULT_MIN_ACTIVE_ALTITUDE);
                final double maxActiveAltitude = ParseUtils.parseDouble(getStringFromNode(xpath, "@maxActiveAltitude", layerNode), DEFAULT_MAX_ACTIVE_ALTITUDE);

                String restoreXMLString = getStringFromNode(xpath, "@restoreXMLString", layerNode);

                // If restoreXMLString is not available, attempt to load restorable state XML from a restorable state location.
                if (StringUtils.isEmpty(restoreXMLString))
                {
                    String restorableStateLocation = getStringFromNode(xpath, "@restorableStateLocation", layerNode);
                    restoreXMLString = loadLayerState(restorableStateLocation);
                }

                // notify layer manager of new layer
                final boolean configFileEmpty = configFileName.isEmpty();
                final boolean classNameEmpty = className.isEmpty();

                if (classNameEmpty == configFileEmpty)
                {
                    logger.warn("On Layer '{}' exactly one of className or configFileName must be specified.", layerName);
                    logger.debug("Skipping creation of layer {}:{}", layerGroup, layerName);
                } else
                {
                    logger.debug("add from node: {}/{}", layerGroup, layerName);

                    final IRaptorLayer raptorLayer = !classNameEmpty
                            // Construct from class name
                            ? addLayer(layerName, displayName, className, visible, pickable, iconPath, layerGroup,
                                    type, restoreXMLString, permanent, splitScale, minActiveAltitude, maxActiveAltitude)
                            // Construct from XML config file name
                            : addLayerFromXML(layerName, displayName, configFileName, visible, pickable, iconPath,
                                    layerGroup, type, restoreXMLString, permanent, splitScale, minActiveAltitude,
                                    maxActiveAltitude);

                    if (systemManaged) ((RaptorLayer) raptorLayer).setSystemManaged(); // Mark system managed layers
                }
            } else
            {
                logger.debug("Skipping creation of layer {}:{}", layerGroup, layerName);
            }
        }
    }

    /**
     * Get a String value from the given node using the given xpath expression.
     * @since 3.4
     */
    private String getStringFromNode(XPath xpath, String expression, Node layerNode) throws XPathExpressionException
    {
        return (String) xpath.evaluate(expression, layerNode, XPathConstants.STRING);
    }

    /**
     * Locate a layer by name in a given list of layers.
     *
     * @param layerList List of layers to search
     * @param layerName Name of layer to find
     * @return Layer if found, {@code null} otherwise
     */
    private IRaptorLayer findLayer(List<IRaptorLayer> layerList, String layerName)
    {
        return layerList.stream()
                .filter(layer -> layer.getName().equals(layerName))
                .findFirst()
                .orElse(null);
    }

    /**
     * Convert a string to a boolean, defaulting to {@code true} if the string is anything other than
     * {@code false} (ignoring case).
     *
     * @param boolStr String to convert
     * @return boolean value
     */
    private boolean toBoolean(String boolStr)
    {
        return !"false".equalsIgnoreCase(boolStr);
    }

    private String loadLayerState(String restorableStateFilename)
    {
        if (StringUtils.isEmpty(restorableStateFilename, true)) return "";

        File f = new File(programEnvironment.getProgramRoot(), restorableStateFilename);
        if (!f.canRead()) return "";

        try
        {
            return Files.readString(f.toPath(), StandardCharsets.UTF_8);
        } catch (Exception e)
        {
            logger.error("Failed to load restorable state from: {}", f, e);
        }
        return "";
    }
}
