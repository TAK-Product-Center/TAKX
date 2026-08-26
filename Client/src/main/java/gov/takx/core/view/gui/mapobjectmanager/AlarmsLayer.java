package gov.takx.core.view.gui.mapobjectmanager;

import gov.nasa.worldwind.avlist.AVKey;
import gov.nasa.worldwind.event.SelectEvent;
import gov.nasa.worldwind.event.SelectListener;
import gov.nasa.worldwind.layers.Layer;
import gov.nasa.worldwind.layers.RenderableLayer;
import gov.nasa.worldwind.layers.WorldMapLayer;
import gov.nasa.worldwind.pick.PickedObjectList;
import gov.nasa.worldwind.render.DrawContext;
import gov.nasa.worldwind.render.Size;
import gov.nasa.worldwind.util.tree.BasicFrameAttributes;
import gov.nasa.worldwind.util.tree.BasicTree;
import gov.nasa.worldwind.util.tree.BasicTreeAttributes;
import gov.nasa.worldwind.util.tree.BasicTreeModel;
import gov.nasa.worldwind.util.tree.BasicTreeNode;
import gov.nasa.worldwind.util.tree.Tree;
import gov.nasa.worldwind.util.tree.TreeNode;
import gov.takx.api.annotation.InvokeOnEDT;
import gov.takx.api.annotation.ProgramPreference;
import gov.takx.api.annotation.PseudoBean;
import gov.takx.api.annotation.RemoveObserver;
import gov.takx.api.apps.IAppInstance;
import gov.takx.api.apps.ICoreAppManager;
import gov.takx.api.event.mapobject.MapObjectAdded;
import gov.takx.api.event.mapobject.MapObjectRemoved;
import gov.takx.api.globe.IExtendedGlobeManager;
import gov.takx.api.globe.IWorldWindowGL;
import gov.takx.api.layer.IRaptorLayer;
import gov.takx.api.layer.IRaptorLayerManager;
import gov.takx.api.mapobject.IDataMonitor;
import gov.takx.api.mapobject.IMapObject;
import gov.takx.api.mapobject.IMapObjectManager;
import gov.takx.api.platform.IDataMonitorServices;
import gov.takx.api.platform.IProgressMonitor;
import gov.takx.api.plugin.app.IAppListener;
import gov.takx.api.plugin.app.IImmutableAppInstance;
import gov.takx.api.plugin.datamonitor.DataMonitorEvent;
import gov.takx.api.plugin.datamonitor.DataMonitorEventType;
import gov.takx.commons.constants.State;
import gov.takx.commons.swing.GraphicsUtils;
import gov.takx.commons.swing.ImageUtils;
import gov.takx.core.globe.layers.WWGlobeLayer;
import gov.takx.platform.preferences.PreferenceEntry;
import gov.takx.platform.ui.FontUtils;
import gov.takx.platform.ui.UIResources;
import gov.takx.platform.utils.DataMonitorUtils;
import gov.takx.platform.utils.StateIconUtils;
import jakarta.annotation.PostConstruct;
import jakarta.enterprise.event.Observes;
import jakarta.inject.Inject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.imageio.ImageIO;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Image;
import java.awt.Point;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.File;
import java.io.IOException;
import java.lang.invoke.MethodHandles;
import java.net.URL;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static gov.takx.commons.swing.ImageUtils.DEFAULT_LED_HEIGHT;
import static gov.takx.commons.swing.ImageUtils.DEFAULT_LED_WIDTH;
import static gov.takx.platform.ui.ResourceDirectory.PLATFORM_ICONS;

/**
 * Provides a dynamic "heads-up" {@link Tree} display of all {@link IDataMonitor}s that are <b>currently alarming</b>.
 * Allows the user to click on each displayed {@link IMapObject} and {@link IDataMonitor} in the
 * same way that they can click on them in the Map Object tree and map object primary panel, respectively.
 *
 * @since 1.11
 */
@SuppressWarnings("CdiManagedBeanInconsistencyInspection")
@PseudoBean
public class AlarmsLayer extends WWGlobeLayer implements IAppListener
{
    private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    /**
     * Root node icon. (Currently, the root node is not visible.)
     */
    private static final URL ALARMS_LAYER_ICON_PATH = UIResources.getUrl(PLATFORM_ICONS, "volume.png"); // TODO KMB: change to alarm/alert icon?

    /**
     * The name of the layer that may overlay with this layer.
     */
    private static final String WORLD_MAP_NAME = "World Overview Map";

    /**
     * The default minimum state that a data monitor must reach to be displayed in the HUD.
     *
     * @since 1.25
     */
    private static final State DEFAULT_MIN_STATE = State.Warning;

    /**
     * This class listens to property changes on its map object. This list specifies the properties
     * that this class is interested in listening to.
     */
    private static final List<String> MAP_OBJECT_PROPERTY_LIST = Arrays.asList(IMapObject.NAME, IMapObject.ICON);

    /**
     * Reference to the manager that will allow access to {IMapObjectController}s as this layer receives
     * notifications of the {IMapObject}s that are being added to the session.
     * <p>
     * Access to the {IMapObjectController}s is necessary in order to retrieve the data monitors associated
     * with each map object.
     */
    @Inject
    private IMapObjectManager mapObjectManager;

    @Inject
    private ICoreAppManager appManager;

    @Inject
    private IExtendedGlobeManager globeManager;

    @Inject
    private IRaptorLayerManager layerManager;

    @Inject
    private StateIconUtils stateIconUtils;

    @Inject
    @ProgramPreference(PreferenceEntry.MIN_HUD_DATA_MONITOR_STATE)
    private String minHudStateName;

    /**
     * The WorldWind tree that is used to display the {@link IMapObject} - {@link IDataMonitor} hierarchy.
     */
    private final Tree tree;

    /**
     * Custom implementation of the WorldWind {@link Tree} layout to provide custom rendering of the alarms layer.
     */
    private DataMonitorTreeLayout layout;

    /**
     * A mapping from each {@link IDataMonitor} to the {@link DataMonitorNode} that represents it in the
     * {@link #tree}.
     * <p>
     * When events are published by {IDataMonitor}s, this class is able to use this map to determine which
     * node needs to be updated in the tree.
     */
    private final Map<IDataMonitor, DataMonitorNode> monitorToNodeMap = new HashMap<>();

    /**
     * The current set of images that have been generated to represent statuses. Allows reuse of
     * images that have already been drawn.
     */
    private final Map<String, BufferedImage> statusImages = new HashMap<>();

    /**
     * Image that indicates that a data monitor is muted.
     */
    private BufferedImage muteImage;

    /**
     * Boolean indicating whether or not the user has selected to hide the layer. This needs to be
     * tracked locally because the layer is also enabled/disabled bases on whether or not there are
     * any alarms to display.
     *
     * @since 1.14
     */
    private boolean showLayer;

    /**
     * The root of the tree.
     *
     * @since 1.14
     */
    private final TreeNode root;

    /**
     * A reference to the world map layer, if it exists.
     *
     * @since 1.14
     */
    private IRaptorLayer worldOverviewRaptorLayer;

    /**
     * Indicates if the world map layer is currently visible.
     *
     * @since 1.14
     */
    private boolean worldMapLayerShowing;

    /**
     * The width of the world map layer.
     *
     * @since 1.14
     */
    private int worldMapWidth;

    /**
     * The height of the world map layer.
     *
     * @since 1.14
     */
    private int worldMapHeight;

    /**
     * Lock object used to ensure only one thread is modifying the tree model at a time.
     *
     * @since 1.14
     */
    private final Object lock = new Object();

    /**
     * The minimum state level that a data monitor must reach to be displayed in the heads up display.
     *
     * @since 1.25
     */
    private State minStateToDisplay;

    private final IWorldWindowGL canvas;

    /**
     * Constructor.
     *
     * @param raptorLayer The raptor layer definition from which to create the globe layer.
     * @param layer       The WorldWind layer representation of this layer.
     * @param canvas      The primary display area for WorldWind.
     */
    public AlarmsLayer(IRaptorLayer raptorLayer, Layer layer, final IWorldWindowGL canvas)
    {
        super(raptorLayer, layer);

        this.canvas = canvas;

        // Construct the tree in which the map objects and data monitors will be displayed.
        tree = createDataMonitorSelector();

        // Keep a reference to the tree's model for easy access.
        root = tree.getModel().getRoot();

        // Add the tree to the layer.
        ((RenderableLayer) layer).addRenderable(tree);

        // Specify the custom controller that will handle mouse events
        // issued by the WorldWind canvas. The controller will receive
        // all mouse events and will filter them based on event details.
        canvas.addSelectListener(new DataMonitorSelectListener());

        // Start the layer off as disabled. Enable when an alarm is added.
        this.layer.setEnabled(false);
    }

    @PostConstruct
    private void postConstruct()
    {
        // Get the preference indicating the minimum state that a data monitor must be in before
        // it is displayed in the HUD and verify that it's a valid value. Otherwise, use the default.
        // This has to be done here rather than the constructor otherwise the state name value is null.
        State statePref = State.stringToState(minHudStateName);
        minStateToDisplay = statePref != State.Unknown ? statePref : DEFAULT_MIN_STATE;

        // Set the initial size and location of the layer.
        updateLayerSizeAndLocation();

        // Register to receive notifications when the canvas is resized
        // so this layer can be resized accordingly.
        canvas.addComponentListener(new CanvasComponentAdapter());

        startup();
    }

    private void startup()
    {
        // Listen for globe initialization to check for the world map.
        globeManager.getPrimaryGlobeDelegate().doWhenReady(this::checkWorldMapLayerExistence);

        // Add all existing device MOs.
        // Now that the tree is set up, force an update of the visibility of existing data monitor nodes.
        mapObjectManager.getMapObjectControllers()
                .forEach(mapObjectController -> {
                    // Add the device map object to the tree
                    onMapObjectAdded(mapObjectController.getMapObject());

                    // Now that the tree is set up, force an update of the visibility of existing data monitor nodes.
                    mapObjectController.getDataMonitors().forEach(this::updateDataMonitor);
                });

        // Add all existing apps.
        appManager.getApps().stream()
                .filter(IImmutableAppInstance::isRunning)
                .forEach(app -> addOwnerNode(app.getAppDelegate().getAppDataMonitorServices()));

        // Listen for app changes
        appManager.addListener(this);
    }

    @RemoveObserver
    @Override
    public void onShutdown(IProgressMonitor progressMonitor)
    {
        monitorToNodeMap.clear();
        statusImages.clear();

        super.onShutdown(progressMonitor);
    }

    @Override
    public void onAppStarted(IImmutableAppInstance app)
    {
        addOwnerNode(((IAppInstance) app).getAppDelegate().getAppDataMonitorServices());
    }

    @Override
    public void onAppStopped(IImmutableAppInstance app)
    {
        removeOwnerNode(((IAppInstance) app).getAppDelegate().getAppDataMonitorServices());
    }

    @Override
    public void setEnabled(boolean enabled)
    {
        // Track the user's choice.
        showLayer = enabled;

        // Check to see if anything should be displayed.
        updateLayerVisibility();
    }

    /**
     * Selectively forward render requests to the WorldWind representation of this layer
     * based on whether or not it's currently enabled.
     *
     * @param dc The current draw context for rendering.
     */
    @Override
    public void render(DrawContext dc)
    {
        super.render(dc);

        checkWorldMapVisibility(dc);

        if (layer.isEnabled())
        {
            layer.render(dc);
        }
    }

    /**
     * Notified by {@link IMapObjectManager} when a new map object is added.
     *
     * @param event The event providing the added map object
     * @since 2.38 - formerly clientMapObjectAdded
     */
    private void onClientMapObjectAdded(@Observes MapObjectAdded event)
    {
        onMapObjectAdded(event.getMapObject());
    }

    /**
     * Notified by {@link IMapObjectManager} when a map object is removed.
     *
     * @param event The event providing the removed map object
     * @since 2.38 - formerly clientMapObjectRemoved
     */
    private void onClientMapObjectRemoved(@Observes MapObjectRemoved event)
    {
        onMapObjectRemoved(event.getMapObject());
    }

    /**
     * Respond to a new map object by adding it and its data monitors to the tree.
     *
     * @param mapObject The new map object.
     */
    private void onMapObjectAdded(IMapObject mapObject)
    {
        // Only process devices that can have alarms
        if (!(mapObject.isDevice() && mapObject.canHaveAlarms())) return;

        // Add to tree
        addOwnerNode(mapObject.getController(), mapObject);
    }

    /**
     * Remove devices from the tree.
     * <p>
     * Currently, the map object must first be hidden from view as a work-around to a bug in WorldWind.
     *
     * @param mapObject The map object to remove.
     */
    private void onMapObjectRemoved(IMapObject mapObject)
    {
        // Only process devices that can have alarms
        if (!(mapObject.isDevice() && mapObject.canHaveAlarms())) return;

        // Remove from tree
        removeOwnerNode(mapObject.getController());
    }

    /**
     * Listen for {@link IDataMonitor} PropertyChangeEvents. This class is specifically
     * concerned with state change events so that it can update the state tree node that
     * corresponds to the {@link IDataMonitor} that issued the event.
     *
     * @param evt An event issued by an {@link IDataMonitor}.
     */
    @Override
    public void propertyChange(PropertyChangeEvent evt)
    {
        String propertyName = evt.getPropertyName();
        if (propertyName.equals(IDataMonitor.STATE_CHANGED) || propertyName.equals(IDataMonitor.MUTE))
        {
            updateDataMonitor((IDataMonitor) evt.getSource());
        }
    }

    /**
     * Update the state of the data monitor node.
     *
     * @param monitor The data monitor that has been updated.
     */
    private void updateDataMonitor(IDataMonitor monitor)
    {
        // Find the tree node representation of the IDataMonitor
        final DataMonitorNode dmn = monitorToNodeMap.get(monitor);

        if (dmn != null)
        {
            // Change the icon.
            updateDataMonitorIcon(dmn, getStatusImage(monitor));

            // Check map object/data monitor visibility.
            dmn.setVisible(monitor.getUnclearedAlarmState().ordinal() >= minStateToDisplay.ordinal());
        }
    }

    /**
     * Respond to resizing of the WorldWindowGLCanvas and toggling of the World Map layer.
     */
    private void updateLayerSizeAndLocation()
    {
        // Adjust the max size of the tree frame depending on whether or no the world map is showing and the size of
        // the viewport size.
        layout.getFrame().setMaxSize(new Size(Size.EXPLICIT_DIMENSION, 0.5, AVKey.FRACTION, Size.EXPLICIT_DIMENSION, calculateMaxHeightPercentage(), AVKey.FRACTION));

        // Place in upper-left portion of screen, offset 20 pixels from the top and left.
        // If the world map is visible, increase the offset from the top by the height of the world map, its
        // padding from the top of the viewport, and padding between the map and the alarms layer.
        layout.setScreenLocation(
                new Point(
                        20,                                  // Offset by 20 pixels from left of viewport.
                        worldMapLayerShowing ?                  // If the map is showing,
                                (int) calculateWorldMapHeight() + 45 :  // offset by the height of the map + padding.
                                20));                                   // Otherwise, just offset by the standard 20 pixels.
    }

    /**
     * Create a WorldWind {@link Tree} that is customized to display map objects and data monitors.
     *
     * @return A WorldWind tree.
     */
    private Tree createDataMonitorSelector()
    {
        String title = "Alerts";

        // Create a WorldWind tree.
        BasicTree selector = new BasicTree()
        {
            // Default value is 1. Returning 0 keeps this layer in front of map objects.
            @Override
            public double getDistanceFromEye()
            {
                return 0;
            }
        };

        // Create the model that will maintain the tree contents.
        BasicTreeModel model = new BasicTreeModel();

        // Create the root node of the tree.
        TreeNode root = new BasicTreeNode(title, ALARMS_LAYER_ICON_PATH);

        // Set the tree's model.
        selector.setModel(model);

        // Define the root node in the model.
        model.setRoot(root);

        // Use a custom tree layout that will allow us to: 1) resize the tree
        // in response to changes in the size of the WorldWindowGLCanvas and
        // 2) define custom node renderers that will generate HotSpots that contain
        // a reference to the node that created it.
        layout = new DataMonitorTreeLayout(selector, 0, 0);
        layout.setDrawSelectedSymbol(false);

        // Set our custom layout as the tree's layout.
        selector.setLayout(layout);

        // Not sure why we're doing this...
        layout.getFrame().setIconImageSource(ALARMS_LAYER_ICON_PATH);

        // Get the default font that is used throughout the UI.
        Font font = FontUtils.DEFAULT_FONT;

        // Prepare the tree's visual attributes (unselected tree).
        // Method names describe what's going on here.
        BasicTreeAttributes attributes = new BasicTreeAttributes();

        // If we couldn't capture the font from the UIManager, use the default tree font.
        if (font == null) font = attributes.getFont();
        final Font largerBoldFont = font.deriveFont(font.getStyle() | Font.BOLD, font.getSize() + 4f);

        // Prepare the tree's visual attributes
        // Method names describe what's going on here.
        attributes.setRootVisible(false);
        attributes.setFont(largerBoldFont);
        attributes.setIconSize(new Dimension(DEFAULT_LED_WIDTH, DEFAULT_LED_HEIGHT));
        attributes.setColor(Color.white);
        layout.setAttributes(attributes);
        layout.setHighlightAttributes(attributes);

        BasicFrameAttributes frameAttributes = new BasicFrameAttributes();
        frameAttributes.setBackgroundOpacity(0.25);
        frameAttributes.setForegroundOpacity(1.0);
        frameAttributes.setFont(largerBoldFont);
        frameAttributes.setIconSize(new Dimension(DEFAULT_LED_WIDTH, DEFAULT_LED_HEIGHT));
        frameAttributes.setTitleBarColor(Color.black, Color.black);
        frameAttributes.setBackgroundColor(Color.black, Color.black);
        frameAttributes.setForegroundColor(Color.white);
        layout.getFrame().setAttributes(frameAttributes);
        layout.getFrame().setHighlightAttributes(frameAttributes);

        // Set the frame's title. Currently hidden from view...
        layout.getFrame().setFrameTitle(title);

        // Expand the root so that it's children will be
        // visible.
        selector.expandPath(root.getPath());

        return selector;
    }

    /**
     * Add all the {IDataMonitor}s associated with a parent.
     *
     * @param dataMonitorServices A reference to the owner services.
     * @return The node created to represent the owner.
     */
    private OwnerNode addOwnerNode(IDataMonitorServices dataMonitorServices)
    {
        return addOwnerNode(dataMonitorServices, null);
    }

    /**
     * Add all the {IDataMonitor}s associated with a parent.
     *
     * @param dataMonitorServices A reference to the owner services.
     * @return The node created to represent the owner.
     */
    private OwnerNode addOwnerNode(IDataMonitorServices dataMonitorServices, IMapObject mapObject)
    {
        // Get a node for map object.
        OwnerNode ownerNode = mapObject != null ? new MapObjectNode(dataMonitorServices, mapObject) : new OwnerNode(dataMonitorServices);

        ownerNode.setVisible(false);

        // In order to improve performance, don't add every MO node to the tree.  Adding a node to the tree forces
        // a redraw on the globe, which can be costly with large numbers of MOs/tracks.  This is especially evident
        // when an importer creates a large number of MOs, especially if those MOs will never have DataMonitors
        // associated with them.

        // The call to addDataMonitor below will add the owner node as needed, so we don't do it here

        // Add data monitor node(s) to the map object node.
        dataMonitorServices.getDataMonitors().forEach(dm -> addDataMonitor(ownerNode, dm));

        // Update data monitors
        ownerNode.dataMonitorServices.getDataMonitors().forEach(this::updateDataMonitor);

        return ownerNode;
    }

    /**
     * Remove an owner node, removing its children, cleaning up its resources and removing it from the tree.
     *
     * @param dataMonitorServices The owner's data monitor services.
     */
    private void removeOwnerNode(IDataMonitorServices dataMonitorServices)
    {
        OwnerNode result = null;

        synchronized (lock)
        {
            for (TreeNode node : root.getChildren())
            {
                OwnerNode ownerNode = (OwnerNode) node;

                if (ownerNode.getKey().equals(dataMonitorServices.getKey()))
                {
                    result = ownerNode;
                    break;
                }
            }
        }

        // Node removal doesn't need to be done in the lock, in fact doing so could lead to a deadlock on the EDT
        // when entering invokeAndWait down the call chain.
        if (result != null)
        {
            removeOwnerNodeFromTree(result);
        }
    }

    /**
     * Add a new owner node to the tree in sorted order if it is not already in the tree.
     *
     * @param ownerNode The new owner node to add
     */
    @InvokeOnEDT
    private void addOwnerNodeToTreeIfNotPresent(OwnerNode ownerNode)
    {
        if (ownerNode.getParent() != null)
        {
            // Nothing to do if the node is already in the tree
            return;
        }

        int index = 0;
        final String name = ownerNode.getName();

        synchronized (lock)
        {
            for (TreeNode node : root.getChildren())
            {
                if (((OwnerNode) node).getName().compareToIgnoreCase(name) > 0)
                {
                    break;
                }

                index += 1;
            }

            // Index is where to insert the new node
            root.addChild(index, ownerNode);
        }
    }

    /**
     * Remove a owner from the tree, cleaning up all references to its data monitors.
     *
     * @param ownerNode The owner to remove.
     */
    private void removeOwnerNodeFromTree(OwnerNode ownerNode)
    {
        // Remove all the data monitor -> node entries from the map.
        for (TreeNode node : ownerNode.getChildren())
        {
            DataMonitorNode dataMonitorNode = ((DataMonitorNode) node);
            dataMonitorNode.dataMonitor.removePropertyChangeListener(this);
            monitorToNodeMap.remove(dataMonitorNode.dataMonitor);
            recursivelyRemoveChildren(dataMonitorNode);
        }

        // Remove the owner node from the tree.
        removeChild(root, ownerNode);

        updateLayerVisibility();
        ownerNode.releaseResources();
    }

    /**
     * Add a data monitor and all of its children to the tree.
     *
     * @param ownerNode The parent object.
     * @param monitor   The monitor to add.
     */
    private void addDataMonitor(OwnerNode ownerNode, IDataMonitor monitor)
    {
        // Add the owner node if it is not already in the tree
        addOwnerNodeToTreeIfNotPresent(ownerNode);

        // Only add top-level data monitors here.
        if (monitor.getParent() == null) addDataMonitorNode(ownerNode, monitor);
    }

    /**
     * Add a new data monitor node with the given parent node. adding all children recursively.
     *
     * @param parentNode Node under which to add the new nodes
     * @param monitor    Data monitor from which to create tree nodes
     */
    private void addDataMonitorNode(BasicTreeNode parentNode, IDataMonitor monitor)
    {
        DataMonitorNode dataMonitorNode = new DataMonitorNode(monitor);
        dataMonitorNode.setVisible(false);

        // Add this data monitor node to its parent node.
        parentNode.addChild(dataMonitorNode);

        // Listen for property changes so we can respond when the data monitor changes states.
        monitor.addPropertyChangeListener(this);

        // Map the data monitor to the tree node that represents it so we can update the correct node when data
        // monitor events are received.
        monitorToNodeMap.put(monitor, dataMonitorNode);

        // Add all the children of this data monitor.
        recursivelyAddChildren(dataMonitorNode);
    }

    /**
     * Add 2nd- to n-level data monitors.
     *
     * @param parentNode The next node to add.
     */
    private void recursivelyAddChildren(DataMonitorNode parentNode)
    {
        List<IDataMonitor> children = parentNode.dataMonitor.getChildren();

        // Add the next child and all of its children.
        for (IDataMonitor monitor : children)
        {
            addDataMonitorNode(parentNode, monitor);
        }
    }

    /**
     * Remove data monitor from a parent.
     *
     * @param basicTreeNode The parent object.
     * @param monitor       The monitor to remove.
     */
    private void removeDataMonitor(BasicTreeNode basicTreeNode, IDataMonitor monitor)
    {
        for (TreeNode child : basicTreeNode.getChildren())
        {
            DataMonitorNode dataMonitorNode = (DataMonitorNode) child;

            if (dataMonitorNode.dataMonitor == monitor)
            {
                // Remove top-level data monitor node.
                removeChild(basicTreeNode, child);

                // Remove the reference from the data monitor -> tree node map.
                monitor.removePropertyChangeListener(this);
                monitorToNodeMap.remove(monitor);

                // Remove all monitorToNodeMap references to its children nodes.
                recursivelyRemoveChildren(dataMonitorNode);

                break;
            }
        }
    }

    /**
     * Just remove and re-add the given node.  This will result in it being re-sorted.
     *
     * @param mapObjectNode Node to remove and re-insert
     */
    @InvokeOnEDT
    private void reinsertMapObjectNode(MapObjectNode mapObjectNode)
    {
        synchronized (lock)
        {
            root.removeChild(mapObjectNode);

            addOwnerNodeToTreeIfNotPresent(mapObjectNode);
        }
    }

    /**
     * Remove a node from the tree on the EDT.
     *
     * @param parent The parent node.
     * @param child  The child to remove from the parent.
     */
    @InvokeOnEDT
    private void removeChild(TreeNode parent, TreeNode child)
    {
        synchronized (lock)
        {
            parent.removeChild(child);
        }
    }

    /**
     * Change a data monitor's icon.
     *
     * @param dmn   The data monitor node to update.
     * @param image The new image.
     */
    @InvokeOnEDT
    private void updateDataMonitorIcon(final DataMonitorNode dmn, final Image image)
    {
        dmn.setImageSource(image);
    }

    /**
     * Ensure that the specified node is expanded in the tree.
     *
     * @param node The node to expand.
     */
    @InvokeOnEDT
    private void expandNodeBranch(final TreeNode node)
    {
        tree.expandPath(node.getPath());
    }

    /**
     * Remove 2nd- to n-level data monitors.
     *
     * @param rootNode The next node to remove.
     */
    private void recursivelyRemoveChildren(DataMonitorNode rootNode)
    {
        Iterable<TreeNode> children = rootNode.getChildren();

        // Add the next child and all of its children.
        for (TreeNode child : children)
        {
            DataMonitorNode dataMonitorNode = (DataMonitorNode) child;
            dataMonitorNode.dataMonitor.removePropertyChangeListener(this);
            monitorToNodeMap.remove(dataMonitorNode.dataMonitor);

            // Add all the children of this data monitor.
            recursivelyRemoveChildren(dataMonitorNode);
        }
    }

    /**
     * Show or hide a node.
     *
     * @param node The node whose visibility should be updated.
     * @param show Boolean indicating if the node should be shown or not.
     */
    private void showNodeInTree(TreeNode node, boolean show)
    {
        if (show)
        {
            expandNodeBranch(node);
        }

        TreeNode parent = node.getParent();

        if (parent != null && parent != root && !parentHasOtherVisibleChildren(node))
        {
            parent.setVisible(show);
        }
    }

    /**
     * Check to see if a node has any visible children besides the one requesting the check.
     * The caller should verify that the parent is not {@code null} before calling this method.
     *
     * @param inquirer The node requesting the check.
     * @return {@code true} if the parent has other visible children, {@code false} otherwise.
     */
    private boolean parentHasOtherVisibleChildren(TreeNode inquirer)
    {
        for (TreeNode child : inquirer.getParent().getChildren())
        {
            if (child != inquirer && child.isVisible())
            {
                return true;
            }
        }

        return false;
    }

    /**
     * Update the visibility for this layer.
     * <p>
     * If at least one {MapObjectNode} must be visible, enable this layer.
     * If no {MapObjectNode}s are visible, this layer will be disabled.
     */
    private void updateLayerVisibility()
    {
        if (showLayer && tree != null)
        {
            boolean show = false;

            synchronized (lock)
            {
                for (TreeNode child : root.getChildren())
                {
                    if (child.isVisible())
                    {
                        show = true;
                        break;
                    }
                }
            }

            layer.setEnabled(show);

            // If it's showing, refresh.
            if (show)
            {
                layout.invalidate();
            }
        } else
        {
            layer.setEnabled(false);
        }
    }

    /**
     * Check to see if the world map layer exists. If it does, get its original width and height. These values will be
     * used to calculate the location of this alarms layer while the world map layer is showing.
     *
     * @since 1.14
     */
    private void checkWorldMapLayerExistence()
    {
        // We must check to verify that the globe layer manager is ready. It
        // will be accessed by the call to IRaptorLayer.getLayer() below.
        if (globeManager.getPrimaryGlobeDelegate().getGlobeLayerManager() != null)
        {
            // If it exists, capture its current configuration details.
            if ((worldOverviewRaptorLayer = layerManager.getLayer(WORLD_MAP_NAME)) != null)
            {
                // Get the image used for the world map layer to determine the original height and width.
                // These values will be used in conjunction with the scaling factor to determine the current
                // size of the world map layer.
                BufferedImage image = ImageUtils.getImage(new File(worldOverviewRaptorLayer.getIconPath()), false);
                if (image == null) logger.warn("Could not get World Map Layer dimensions; using defaults");
                worldMapWidth = image != null ? image.getWidth() : 256;
                worldMapHeight = image != null ? image.getHeight() : 128;
            }
        }
    }

    private void checkWorldMapVisibility(DrawContext dc)
    {
        if (worldOverviewRaptorLayer != null)
        {
            boolean isVisible = worldOverviewRaptorLayer.isVisible() && worldOverviewRaptorLayer.getLayer().isLayerActive(dc);

            if (isVisible != worldMapLayerShowing)
            {
                worldMapLayerShowing = isVisible;
                updateLayerSizeAndLocation();
            }
        }
    }

    /**
     * Calculate the current height of the world map.
     *
     * @return The current height of the world map, in pixels.
     * @since 1.14
     */
    private double calculateWorldMapHeight()
    {
        // Get the scale of the map layer relative to the viewport.
        double worldMapLayerToViewportScale = ((WorldMapLayer) worldOverviewRaptorLayer.getLayer()).getToViewportScale();

        // Current width of globe view.
        int width = globeManager.getPrimaryGlobeDelegate().getWorldWindow().getComponent().getSize().width;

        // Percentage of icon.
        double scale = Math.min(1d, (worldMapLayerToViewportScale * width) / worldMapWidth);

        // Return the actual height of the World Map Layer.
        return worldMapHeight * scale;
    }

    /**
     * Calculate the max percentage of the screen's height that this layer should be allowed to consume.
     * The percentage is based on the height of the viewport, less 40 pixels (20 pixels of padding on the
     * top and bottom of the frame).
     * <p>
     * If the world map is showing, the pixels being used by the map and its padding are also subtracted from
     * the viewport's pixel height before calculating the percentage.
     *
     * @return The percentage of the screen the alarms layer will be allowed to take up (vertically).
     * @since 1.14
     */
    private double calculateMaxHeightPercentage()
    {
        int height = globeManager.getPrimaryGlobeDelegate().getWorldWindow().getComponent().getSize().height;

        if (worldMapLayerShowing)
        {
            // Add top padding (20), map height (calculated), padding between map and alarms layer (25)
            // and bottom padding (20).
            double mapAndPaddingHeight = 20 + calculateWorldMapHeight() + 25 + 20;

            return 1.0 - (mapAndPaddingHeight / height);
        } else
        {
            // 20 pixels padding on top and bottom = 40
            return (height - 40.0) / height;
        }
    }

    /**
     * Generate a status image for a data monitor based on the user's status image preferences. If the
     * user's status image preferences are not available, use the default values.
     *
     * @param monitor The monitor that requires a status image.
     * @return The image that represents the monitor's current status.
     */
    private BufferedImage getStatusImage(IDataMonitor monitor)
    {
        return statusImages.computeIfAbsent(getImageKeyForState(monitor), k -> generateStatusImage(monitor));
    }

    /**
     * Generate the image key for the current data monitors status.
     *
     * @param monitor Data monitor to process
     * @return Image key for the status of the data monitor
     */
    private String getImageKeyForState(IDataMonitor monitor)
    {
        // Get the monitor state and the visual state, which may differ from the actual state if the user is required
        // to acknowledge alarms.
        return String.valueOf(monitor.getCurrentState()) + ':' +
                monitor.getUnclearedAlarmState() + ':' +
                monitor.getCustomIcon() + ':' +
                monitor.isMuted();
    }

    /**
     * Generate the image to be used in the HUD to represent the current state of the data monitor.
     *
     * @param monitor Data monitor of interest
     * @return Image to use
     */
    private BufferedImage generateStatusImage(IDataMonitor monitor)
    {
        State newState = monitor.getCurrentState();

        // The monitor's visual state, which may differ from the actual
        // state if the user is required to acknowledge alarms.
        State visualState = monitor.getUnclearedAlarmState();

        // Get the color and shape that represent the current state of the monitor.
        final Color stateColor = stateIconUtils.getColorForState(newState);
        final GraphicsUtils.Shape shape = stateIconUtils.getShapeForState(newState);

        // The monitor's actual state.
        BufferedImage statusImage = GraphicsUtils.createShape(shape, DEFAULT_LED_WIDTH, 200, stateColor);

        // Check to see if the monitor has a custom icon.
        URL customIconUrl = monitor.getCustomIcon();

        // If the monitor has a custom icon, overlay it onto the image.
        if (customIconUrl != null)
        {
            try
            {
                BufferedImage customImage = ImageIO.read(customIconUrl);

                // The result of this method will be a statusImage
                // with a scaled customImage overlaid onto it.
                DataMonitorUtils.overlayImage(statusImage, customImage);
            } catch (IOException ioe)
            {
                logger.error("Error loading custom icon.", ioe);
            }
        }

        // Check to see if an ack state overlay is required.
        if (visualState != newState && visualState.ordinal() > State.Active.ordinal())
        {
            statusImage = stateIconUtils.overlayVisualState(statusImage, visualState);
        }

        if (monitor.isMuted()) statusImage = overlayMutedState(statusImage);

        // Draw a border around the image.
        if (shape == GraphicsUtils.Shape.TRIANGLE)
        {
            statusImage = GraphicsUtils.createOutlinedImage(statusImage, stateColor.getRGB(), 255);
        }
        return statusImage;
    }

    private BufferedImage overlayMutedState(BufferedImage statusImage)
    {
        if (muteImage == null)
        {
            muteImage = DataMonitorUtils.getMutedStateImage(statusImage.getWidth() / 2);
        }
        return DataMonitorUtils.overlayMuteStatus(statusImage, muteImage);
    }

    /**
     * Respond the WorldWindowGLCanvas resize events.
     */
    private class CanvasComponentAdapter extends ComponentAdapter
    {
        @Override
        public void componentResized(ComponentEvent e)
        {
            updateLayerSizeAndLocation();
        }
    }

    /**
     * Class that responds to click events generated by the {@link Tree}.
     */
    private class DataMonitorSelectListener implements SelectListener
    {
        /**
         * Analyze mouse events to see if the clicked item was a tree node. If it was,
         * extract the source {@link MapObjectNode} or {@link DataMonitorNode} and process the click accordingly.
         *
         * @param selectEvent The mouse event to process.
         */
        @Override
        public void selected(SelectEvent selectEvent)
        {
            MouseEvent e = selectEvent.getMouseEvent();

            // We only care about right-clicks and double-clicks.
            if (e != null && e.getID() == MouseEvent.MOUSE_CLICKED && (e.getClickCount() == 2 || e.getButton() != 1))
            {
                // Get all the objects associated with the click location.
                PickedObjectList list = selectEvent.getObjects();

                // Check to see if there is more than one object that was clicked.
                // A list size of one indicates that it was only the WorldWindowGLCanvas
                // that was clicked.
                if (!list.isEmpty())
                {
                    // Get the top item on the list.
                    Object obj = list.getTopPickedObject().getObject();

                    // Determine if this was a click on a data monitor
                    // or a map object.
                    if (obj instanceof DataMonitorNode)
                    {
                        // Generate the appropriate event.
                        DataMonitorEventType type =
                                e.getButton() != 1
                                        ? DataMonitorEventType.RightClick
                                        : DataMonitorEventType.AlarmClear;

                        ((DataMonitorNode) obj).dataMonitor.onDataMonitorEvent(
                                new DataMonitorEvent(this, type, Map.of(IDataMonitor.MOUSE_EVENT, e)));
                    }
                    // Map objects only care about right-clicks.
                    else if (obj instanceof MapObjectNode && e.getButton() != 1)
                    {
                        final var moc = mapObjectManager.getMapObjectController(((OwnerNode) obj).getKey());
                        // center the globe on the selected object
                        globeManager.getLastFocusedGlobe().goToAndTranslateMouseEvent(moc.getLocationOnGlobe(), e);
                        moc.createPopupMenu(e);
                    }
                }
            }
        }
    }

    /**
     * Tree node that represents an {@link IMapObject} in the {@link #tree}. Augments the standard
     * {@link AlarmsLayer.OwnerNode} by adding a {@link PropertyChangeListener}
     * to the {@link IMapObject} and responding to MapObject-specific property changes (name, icon, etc.).
     *
     * @since 2.16
     */
    private class MapObjectNode extends OwnerNode
    {
        /**
         * The map object this node represents.
         */
        private final IMapObject mapObject;

        /**
         * Constructor.
         *
         * @param dataMonitorServices The controller for the map object that this node will represent.
         */
        MapObjectNode(IDataMonitorServices dataMonitorServices, IMapObject mapObject)
        {
            super(dataMonitorServices);

            this.mapObject = mapObject;

            // Listen for name and icon MO property change events
            mapObject.addPropertyChangeListener(MAP_OBJECT_PROPERTY_LIST, this);
        }

        /**
         * Respond to map object name changes, map object security level changes and data monitors
         * being added or removed from the map object.
         *
         * @param evt An event issued by the map object that this node represents.
         */
        @Override
        public void propertyChange(PropertyChangeEvent evt)
        {
            String propertyName = evt.getPropertyName();

            // Update the name displayed in the map object's tree node and
            // resort the list of map objects in the tree.
            switch (propertyName)
            {
                case IMapObject.NAME ->
                {
                    text = ((IMapObject) evt.getSource()).getName();
                    reinsertMapObjectNode(this);
                }
                case IMapObject.ICON ->
                {
                    // Update the map object's icon in the tree.
                    Object oldImage = getImageSource();
                    Object newImage = dataMonitorServices.getIconImage();

                    setImageSource(newImage);

                    // This will force a repaint.
                    firePropertyChange(AVKey.IMAGE, oldImage, newImage);
                }
                default -> super.propertyChange(evt);
            }
        }

        @Override
        public void releaseResources()
        {
            super.releaseResources();
            mapObject.removePropertyChangeListener(MAP_OBJECT_PROPERTY_LIST, this);
        }
    }

    /**
     * Represents a tree node that owns a set of data monitors. The owner node will monitor the addition and removal of
     * data monitors from the owner object and update the tree accordingly.
     *
     * @since 2.16
     */
    private class OwnerNode extends BasicTreeNode
    {
        /**
         * The owner's data monitor services.
         */
        protected IDataMonitorServices dataMonitorServices;

        /**
         * Constructor.
         *
         * @param dataMonitorServices The data monitor services.
         */
        public OwnerNode(IDataMonitorServices dataMonitorServices)
        {
            super(dataMonitorServices.getDisplayName(), dataMonitorServices.getIconImage());

            // Listen for data monitors being added and removed
            dataMonitorServices.addPropertyChangeListener(this);

            this.dataMonitorServices = dataMonitorServices;
        }

        public String getName()
        {
            return dataMonitorServices.getDisplayName();
        }

        public String getKey()
        {
            return dataMonitorServices.getKey();
        }

        @Override
        public void propertyChange(PropertyChangeEvent propertyChangeEvent)
        {
            String propertyName = propertyChangeEvent.getPropertyName();

            switch (propertyName)
            {
                case IDataMonitorServices.DATA_MONITOR_ADDED -> addDataMonitor(this, (IDataMonitor) propertyChangeEvent.getNewValue());
                case IDataMonitorServices.DATA_MONITOR_REMOVED -> removeDataMonitor(this, (IDataMonitor) propertyChangeEvent.getOldValue());
                default -> super.propertyChange(propertyChangeEvent);
            }
        }

        public void releaseResources()
        {
            dataMonitorServices.removePropertyChangeListener(this);
        }

        /**
         * Change the visibility of this {@link TreeNode} based on the {@code show} flag.
         *
         * @param show Boolean indicating if this node should be shown.
         */
        @InvokeOnEDT
        @Override
        public void setVisible(final boolean show)
        {
            super.setVisible(show);
            showNodeInTree(this, show);
            updateLayerVisibility();
        }
    }

    /**
     * {@link TreeNode} representing a data monitor.
     */
    private class DataMonitorNode extends BasicTreeNode
    {
        /**
         * The data monitor that this node represents in the tree.
         */
        private final IDataMonitor dataMonitor;

        /**
         * Constructor.
         *
         * @param dataMonitor The data monitor that this node represents.
         */
        DataMonitorNode(IDataMonitor dataMonitor)
        {
            super(dataMonitor.getDisplayName(), getStatusImage(dataMonitor));
            this.dataMonitor = dataMonitor;
        }

        /**
         * Get the text to display next to the alarm icon in the HUD.  We want the developer to be able to change
         * the display name, so get it from the DM each time.
         *
         * @return the text to display next to the alarm icon
         * @since 2.8
         */
        @Override
        public String getText()
        {
            return dataMonitor.getDisplayName();
        }

        /**
         * Change the visibility of this {@link TreeNode} based on the {@code show} flag.
         *
         * @param show Boolean indicating if this node should be shown.
         */
        @InvokeOnEDT
        @Override
        public void setVisible(final boolean show)
        {
            super.setVisible(show);
            showNodeInTree(this, show);
            updateLayerVisibility();
        }
    }
}
