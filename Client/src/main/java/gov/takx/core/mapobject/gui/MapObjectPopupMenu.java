package gov.takx.core.mapobject.gui;

import com.atakmap.coremap.conversions.Angle;
import com.atakmap.coremap.maps.coords.NorthReference;
import gov.nasa.worldwind.WorldWindow;
import gov.tak.api.cot.exporter.ICotExporterManager;
import gov.tak.api.engine.map.coords.GeoCalculations;
import gov.tak.api.maps.Marker;
import gov.tak.api.maps.coords.GeoPoint;
import gov.tak.api.util.StandardAttributeNames;
import gov.tak.api.video.ConnectionEntry;
import gov.tak.api.video.cot.VideoDetailHandler;
import gov.takx.api.annotation.PseudoBean;
import gov.takx.api.breadcrumb.IBreadcrumbManager;
import gov.takx.api.coordinatesystem.ILocation;
import gov.takx.api.event.mapobject.MapObjectRemoved;
import gov.takx.api.globe.IExtendedGlobeManager;
import gov.takx.api.globe.IGlobeClickService;
import gov.takx.api.globe.IGlobeDelegate;
import gov.takx.api.globe.IGlobeManager;
import gov.takx.api.mapobject.IAnnotationMapObject;
import gov.takx.api.mapobject.ICoreMapObjectManager;
import gov.takx.api.mapobject.IFlatAnnotationMapObject;
import gov.takx.api.mapobject.IIconMapObject;
import gov.takx.api.mapobject.IMapObject;
import gov.takx.api.mapobject.IMapObjectController;
import gov.takx.api.mapobject.IShapeArrowMapObject;
import gov.takx.api.mapobject.IShapeMapObject;
import gov.takx.api.media.IMediaManager;
import gov.takx.api.media.IMediaPlayerManager;
import gov.takx.api.media.IVideo;
import gov.takx.api.media.MediaPlayerOptions;
import gov.takx.api.playback.IPlaybackSystem;
import gov.takx.api.plugin.datamonitor.DataMonitorEvent;
import gov.takx.api.plugin.datamonitor.DataMonitorEventType;
import gov.takx.api.shapes.IMutableBullseye;
import gov.takx.api.shapes.IPolygon;
import gov.takx.api.shapes.IRegularShape;
import gov.takx.api.ui.IDeviceMenuSpaceManager;
import gov.takx.api.ui.IMenuSpaceManager.ContextMenuEnum;
import gov.takx.api.ui.IUserProxy;
import gov.takx.commons.swing.AlwaysOnTopJOptionPane;
import gov.takx.commons.swing.ImageUtils;
import gov.takx.commons.utils.StringUtils;
import gov.takx.core.globe.ApplicationGlobe;
import gov.takx.core.globe.radial.NestedRadialMenuAction;
import gov.takx.core.globe.radial.RadialMenuAction;
import gov.takx.core.globe.radial.RadialMenuHelper;
import gov.takx.core.globe.tak.WidgetLayerManager;
import gov.takx.core.mapobject.DeviceMenuSpaceManager;
import gov.takx.core.mapobject.gui.primarydialog.primarypanel.FieldOfViewPanel;
import gov.takx.core.view.gui.drifter.EditDrifterConfigurationDialog;
import gov.takx.core.view.gui.geofence.EditGeoFenceDialog;
import gov.takx.dataservices.shapes.FieldOfView;
import gov.takx.platform.preferences.PreferenceEntry;
import gov.takx.platform.ui.UIResources;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import jakarta.annotation.PostConstruct;
import jakarta.enterprise.event.Observes;
import jakarta.inject.Inject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.Action;
import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPopupMenu;

import java.awt.Component;
import java.awt.Image;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.lang.invoke.MethodHandles;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Stream;

import static gov.takx.api.ui.IMenuSpaceManager.ContextMenuEnum.BREADCRUMBS;
import static gov.takx.api.ui.IMenuSpaceManager.ContextMenuEnum.CENTER_ON;
import static gov.takx.api.ui.IMenuSpaceManager.ContextMenuEnum.CHANGE_DEGREES;
import static gov.takx.api.ui.IMenuSpaceManager.ContextMenuEnum.CHANGE_DIRECTION;
import static gov.takx.api.ui.IMenuSpaceManager.ContextMenuEnum.CLEAR_ALARMS;
import static gov.takx.api.ui.IMenuSpaceManager.ContextMenuEnum.CONFIGURE;
import static gov.takx.api.ui.IMenuSpaceManager.ContextMenuEnum.DEACTIVATE;
import static gov.takx.api.ui.IMenuSpaceManager.ContextMenuEnum.DEG_GRID_NORTH;
import static gov.takx.api.ui.IMenuSpaceManager.ContextMenuEnum.DEG_MAG_NORTH;
import static gov.takx.api.ui.IMenuSpaceManager.ContextMenuEnum.DEG_TRUE_NORTH;
import static gov.takx.api.ui.IMenuSpaceManager.ContextMenuEnum.DELETE;
import static gov.takx.api.ui.IMenuSpaceManager.ContextMenuEnum.DEVICE_LOG;
import static gov.takx.api.ui.IMenuSpaceManager.ContextMenuEnum.DRIFTER;
import static gov.takx.api.ui.IMenuSpaceManager.ContextMenuEnum.EDITABLE;
import static gov.takx.api.ui.IMenuSpaceManager.ContextMenuEnum.FINE_ADJUST;
import static gov.takx.api.ui.IMenuSpaceManager.ContextMenuEnum.GEOFENCE;
import static gov.takx.api.ui.IMenuSpaceManager.ContextMenuEnum.GOTO;
import static gov.takx.api.ui.IMenuSpaceManager.ContextMenuEnum.HELP;
import static gov.takx.api.ui.IMenuSpaceManager.ContextMenuEnum.HIDE;
import static gov.takx.api.ui.IMenuSpaceManager.ContextMenuEnum.INSPECT_COT;
import static gov.takx.api.ui.IMenuSpaceManager.ContextMenuEnum.KEEP_IN_VIEW;
import static gov.takx.api.ui.IMenuSpaceManager.ContextMenuEnum.LOCK_DISTANCE;
import static gov.takx.api.ui.IMenuSpaceManager.ContextMenuEnum.MEASURE_TO;
import static gov.takx.api.ui.IMenuSpaceManager.ContextMenuEnum.MILS_GRID_NORTH;
import static gov.takx.api.ui.IMenuSpaceManager.ContextMenuEnum.MILS_MAG_NORTH;
import static gov.takx.api.ui.IMenuSpaceManager.ContextMenuEnum.MILS_TRUE_NORTH;
import static gov.takx.api.ui.IMenuSpaceManager.ContextMenuEnum.OPEN;
import static gov.takx.api.ui.IMenuSpaceManager.ContextMenuEnum.ORIENTABLE;
import static gov.takx.api.ui.IMenuSpaceManager.ContextMenuEnum.PIN;
import static gov.takx.api.ui.IMenuSpaceManager.ContextMenuEnum.REMOVE_FROM;
import static gov.takx.api.ui.IMenuSpaceManager.ContextMenuEnum.SEND_AS_COT;
import static gov.takx.api.ui.IMenuSpaceManager.ContextMenuEnum.SENSOR;
import static gov.takx.api.ui.IMenuSpaceManager.ContextMenuEnum.SENSOR_DIRECTION;
import static gov.takx.api.ui.IMenuSpaceManager.ContextMenuEnum.SENSOR_VISIBILITY;
import static gov.takx.api.ui.IMenuSpaceManager.ContextMenuEnum.SHOW;
import static gov.takx.api.ui.IMenuSpaceManager.ContextMenuEnum.TOGGLE_RANGE_RINGS;
import static gov.takx.api.ui.IMenuSpaceManager.ContextMenuEnum.UNDO;
import static gov.takx.api.ui.IMenuSpaceManager.ContextMenuEnum.VIDEO;

/**
 * The Map Object popup menu provides direct access to numerous user-invoked operations on a specific Map Object.
 * Special processing is applied depending on whether the component on which the use clicked is a globe (so we
 * can determine which globe the user is operating upon) or not.  If not, then the menu is more complicated,
 * offering sub-menus for setting the "center on" and "Keep in view" one each of the extant globes.
 * <p>
 * And of course, everything is completely customizable, which menu items will show up, what the text will be and
 * what order they will be in through the DeviceMenuSpaceManager (devices only)
 *
 * @see IGlobeManager#createGlobe()
 * @see DeviceMenuSpaceManager
 */
@SuppressWarnings("CdiManagedBeanInconsistencyInspection")
@PseudoBean(singletonObserver = true)
public class MapObjectPopupMenu extends JPopupMenu
{
    private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    private static final String ASSET_SCHEME = "asset://";

    /**
     * Show this popup menu as a radial menu.
     *
     * @param mouseEvent Event invoking this operation
     * @param controller MapObject controller, to access its menu space manager
     * @since 5.2
     */
    public static void showAsRadialMenu(@Nonnull MouseEvent mouseEvent, @Nonnull IMapObjectController controller)
    {
        new MapObjectPopupMenu(mouseEvent, controller, true).showAsRadialMenu();
    }

    private static final AtomicReference<IMapObject> LAST_MAP_OBJECT = new AtomicReference<>();

    private static final Supplier<Boolean> ENABLED = () -> true;
    private static final Supplier<Boolean> NO_STATE = null;

    private final DataMonitorEvent deactivateAlarmsEvent = new DataMonitorEvent(this, DataMonitorEventType.Deactivate);
    private final DataMonitorEvent clearAlarmsEvent = new DataMonitorEvent(this, DataMonitorEventType.AlarmClear);

    private final Map<ContextMenuEnum, RadialMenuAction> actionMap = new EnumMap<>(ContextMenuEnum.class);
    private final List<ContextMenuEnum> menuOrder;

    private final IMapObject clickedMapObject;
    private final boolean isSelfMarker;
    private final IDeviceMenuSpaceManager menuSpaceManager;
    private final IMapObjectController controller;
    private final MouseEvent mouseEvent;
    private final boolean radialMenu;

    // Indicates the id of the globe selected, or the primary globe if there is only one globe
    private Integer singleGlobeId;

    private ApplicationGlobe lastFocusedGlobe;
    private ILocation mapObjectLocation;

    @Inject
    private ICoreMapObjectManager mapObjectManager;

    @Inject
    private IExtendedGlobeManager globeManager;

    @Inject
    private ICotExporterManager cotExporterManager;

    @Inject
    private IPlaybackSystem playback;

    @Inject
    private IUserProxy userProxy;

    @Inject
    private WidgetLayerManager widgetLayerManager;

    @Inject
    private IMediaManager mediaManager;

    @Inject
    private IMediaPlayerManager mediaPlayerManager;

    @Inject
    private IBreadcrumbManager breadcrumbManager;

    @Inject
    private FineAdjustAction fineAdjustAction;

    @Inject
    private IGlobeClickService globeClickService;

    /**
     * Construct for a given event and map object, for use as a Swing context menu.
     *
     * @param mouseEvent Event invoking this operation
     * @param controller MapObject controller, to access its menu space manager
     */
    public MapObjectPopupMenu(@Nonnull MouseEvent mouseEvent, @Nonnull IMapObjectController controller)
    {
        this(mouseEvent, controller, false);
    }

    /**
     * Construct for a given event and map object, for use as either a Swing context menu or as a Radial menu.
     *
     * @param mouseEvent Event invoking this operation
     * @param controller MapObject controller, to access its menu space manager
     * @param radialMenu Pass {@code true} if this menu should be configured for use as a Radial Menu on the globe
     * @since 5.2
     */
    public MapObjectPopupMenu(@Nonnull MouseEvent mouseEvent, @Nonnull IMapObjectController controller, boolean radialMenu)
    {
        this.mouseEvent = mouseEvent;
        this.controller = controller;
        menuSpaceManager = controller.getMenuSpaceManager();
        clickedMapObject = controller.getMapObject();
        isSelfMarker = clickedMapObject.isSelfMarker();
        this.radialMenu = radialMenu;

        // Get the, possibly custom, ordered list of menu items.
        menuOrder = new ArrayList<>(menuSpaceManager.getMenuOrder());

        LAST_MAP_OBJECT.set(clickedMapObject);
    }

    @PostConstruct
    private void postConstruct()
    {
        // We used to rely on the "last focused globe" to determine where the click happened, but this has
        // turned out to be unreliable (as focus often transfers after the mouse click event is delivered).
        // So, instead we use the component to look up the globe.
        final Component component = mouseEvent.getComponent();

        final IGlobeDelegate clickedGlobe = component instanceof WorldWindow worldWindow
                ? globeManager.getGlobeForComponent(worldWindow)
                : null;

        // Determine if our menus should relate to a single globe, or all globes
        singleGlobeId = globeManager.getPrimaryGlobeDelegate().getGlobeCount() == 1
                ? Integer.valueOf(IGlobeDelegate.PRIMARY_GLOBE_ID)
                : clickedGlobe != null ? clickedGlobe.getGlobeId() : null;

        // Determine the location of the MO
        lastFocusedGlobe = (ApplicationGlobe) globeManager.getLastFocusedGlobe();
        mapObjectLocation = clickedMapObject.inPlaybackMode()
                ? playback.getCurrentLocation(clickedMapObject)
                : clickedMapObject.getLocation();

        createActions();
        createMenu();

        if (!radialMenu) resolveIcons(this);
    }

    /**
     * Traverse the given menu (recursively) and resolve the icon on each menu item.
     *
     * @implNote We only do this when we're NOT being displayed as a radial menu.  If we always applied this
     * logic, the icons look terrible on the radial menu because this process loads the icons sized for a typical
     * context menu, and when the radial menu scales them back up, they are very fuzzy.
     * @since 5.7
     */
    private void resolveIcons(@Nonnull JPopupMenu menu)
    {
        for (var component : menu.getComponents())
        {
            if (component instanceof JMenuItem menuItem) resolveIcon(menuItem);
            if (component instanceof JMenu subMenu) resolveIcons(subMenu.getPopupMenu());
        }
    }

    /**
     * Resolve the icon for a menu item.  This handles icon names that:
     * <ul>
     *     <li>Have a {@code scheme} of {@code 'asset'}, which need to be converted to a proper resource URL</li>
     *     <li>All else, assume it is a URL that can be directly handled by our image readers</li>
     * </ul>
     *
     * @param menuItem Menu item to process
     */
    private void resolveIcon(@Nonnull JMenuItem menuItem)
    {
        if (!(menuItem.getAction() instanceof RadialMenuAction action)) return;

        final String iconName = action.getIconName();
        if (iconName != null)
        {
            try
            {
                // If it is an "asset" reference, then resolve it as a resource in the "/assets" directory
                final var url = iconName.startsWith(ASSET_SCHEME)
                        ? getClass().getResource("/assets/" + iconName.substring(ASSET_SCHEME.length()))
                        : new URL(iconName);

                if (url != null)
                {
                    action.setIcon(UIResources.getStandardSizedIcon(url));
                } else
                {
                    logger.warn("Failed to resolve icon: '{}'", iconName);
                }
            } catch (Exception e)
            {
                logger.error("Failed to resolve icon: {}", iconName, e);
            }
        }
    }

    /**
     * Close the menu if the MO removed is the one we last opened a menu for.
     * @since 5.2
     */
    private void onMapObjectRemoved(@Observes MapObjectRemoved event)
    {
        // If the MO on which a menu was last opened, close the menu.
        if (event.getMapObject().equals(LAST_MAP_OBJECT.get()))
        {
            if (radialMenu)
            {
                widgetLayerManager.closeOpenRadialMenus();
            } else
            {
                setVisible(false);
            }
        }
    }

    /**
     * Create the menu from the list of items in menuOrder and then add the custom menu items added by the plugin.
     */
    private void createMenu()
    {
        // Remove any actions that don't make sense when we're showing as the radial menu
        if (radialMenu)
        {
            menuOrder.remove(GOTO); // Globe is already centered on the Map Object
            menuOrder.remove(SHOW); // It has to be showing already to have invoked the radial menu
        }

        // If the MO is a viewshed, remove all non-relevant items
        if (clickedMapObject.getMetaBoolean(StandardAttributeNames.VIEWSHED, false))
        {
            menuOrder.clear();
            menuOrder.addAll(List.of(DELETE, FINE_ADJUST, MEASURE_TO));
        }

        // Add all our standard menus items
        menuOrder.forEach(this::addStandardMenuItem);

        // Then the additional menu items contributed by a DevicePlugin through the IMenuSpaceManager interface.
        menuSpaceManager.getAdditionalMenuItems().forEach(this::addCustomMenuItem);
    }

    /**
     * Add the menu items provided by plugins. {@link JMenuItem} and {@link JMenu} entries are converted to
     * {@link RadialMenuAction} or {@link NestedRadialMenuAction} respectively, copying data from the source as needed, so that
     * they are handled correctly during radial menu creation.
     * <p/>
     * If the item is anything other than a {@link JMenuItem}, like a separator, it is simply added to the popup menu.
     * In addition, if this is not being constructed as a radial menu, scale any icons down to an appropriate size
     * for a standard context menu.
     *
     * @param item The indexed menu item component
     * @since 3.2
     */
    private void addCustomMenuItem(@Nonnull IDeviceMenuSpaceManager.IIndexedComponent item)
    {
        Component component = item.getComponent();

        // Handle menu items (and sub-menus)
        if (component instanceof JMenuItem menuItem) component = RadialMenuHelper.createRadialMenuItem(menuItem);
        if (!radialMenu) scaleIcons(component);

        // Ensure the developer can specify arbitrarily large numbers to ensure ordering at the end
        final int index = Math.min(item.getIndex(), getComponentCount());
        add(component, index);
    }

    /**
     * Add the menu item for the given menu entry.
     *
     * @param menuItem the menu entry
     * @since 2.37
     */
    private void addStandardMenuItem(@Nonnull ContextMenuEnum menuItem)
    {
        //noinspection EnhancedSwitchMigration
        switch (menuItem)
        {
            case CENTER_ON:
                if (singleGlobeId != null)
                {
                    add(createCenterOnMenuItem(singleGlobeId, false));
                } else
                {
                    add(createGlobeSpecificMenu(CENTER_ON, globeId -> createCenterOnMenuItem(globeId, true)));
                }
                break;
            case KEEP_IN_VIEW:
                if (singleGlobeId != null)
                {
                    add(createKeepInViewMenuItem(singleGlobeId, false));
                } else
                {
                    add(createGlobeSpecificMenu(KEEP_IN_VIEW, globeId -> createKeepInViewMenuItem(globeId, true)));
                }
                break;
            default:
                if (menuItem.shouldAddSeparatorBefore()) add(new Separator());
                final var menuAction = actionMap.get(menuItem);
                if (menuAction != null) add(menuAction.createMenuItem()); // Ignore missing menu actions
                break;
        }
    }

    /**
     * Create a menu of sub-items per globe.
     *
     * @param menuItem        Menu for which we are creating the sub-menu
     * @param menuItemCreator Function to generate the sub-menu item for a given globe id
     * @return menu
     * @since 2.37
     */
    @Nonnull
    private JMenu createGlobeSpecificMenu(@Nonnull ContextMenuEnum menuItem, @Nonnull Function<Integer, JMenuItem> menuItemCreator)
    {
        final JMenu menu = new JMenu(menuItem.toString());
        globeManager.getGlobeIds().stream().map(menuItemCreator).forEach(menu::add);
        return menu;
    }

    /**
     * Construct a checkbox menu item that controls the "center on" state for a map object on a specific globe.
     * The initial "checked" setting of this menu item is derived from the current configuration on the globe.
     *
     * @param globeId      The Id of the globe to operate upon
     * @param includeGlobe Pass {@code true} to include the globe id in the menu item text
     * @return The configured menu item
     */
    @Nonnull
    private JMenuItem createCenterOnMenuItem(int globeId, boolean includeGlobe)
    {
        final String menuText = includeGlobe ? "on Globe " + globeId : CENTER_ON.toString();
        final IGlobeDelegate globe = globeManager.getGlobeDelegate(globeId);
        final boolean wasCenterOn = globe.isCenterOn(clickedMapObject);

        return createAction(CENTER_ON, menuText, e -> {
            if (wasCenterOn)
            {
                globe.stopCenterOn();
            } else
            {
                globe.startCenterOn(clickedMapObject);
            }
        }, () -> mapObjectLocation != null && GeoPoint.isValid(mapObjectLocation.getLatitude(), mapObjectLocation.getLongitude()), () -> wasCenterOn).createMenuItem();
    }

    /**
     * Construct a checkbox menu item that controls the "keep in view" state for a map object on a specific globe.
     * The initial "checked" setting of this menu item is derived from the current configuration on the globe.
     *
     * @param globeId      The Id of the globe to operate upon
     * @param includeGlobe Pass {@code true} to include the globe id in the menu item text
     * @return The configured menu item
     */
    @Nonnull
    private JMenuItem createKeepInViewMenuItem(int globeId, boolean includeGlobe)
    {
        final String menuText = includeGlobe ? "on Globe " + globeId : KEEP_IN_VIEW.toString();
        final IGlobeDelegate globe = globeManager.getGlobeDelegate(globeId);
        final boolean wasKeepInView = globe.isKeepInView(clickedMapObject);

        return createAction(KEEP_IN_VIEW, menuText, e -> {
            if (wasKeepInView)
            {
                globe.removeKeepInView(clickedMapObject);
            } else
            {
                globe.addKeepInView(clickedMapObject);
            }
        }, () -> mapObjectLocation != null && GeoPoint.isValid(mapObjectLocation.getLatitude(), mapObjectLocation.getLongitude()), () -> wasKeepInView).createMenuItem();
    }

    /**
     * Traverse the component as a JMenu with JMenuItem(s) and scale the icons for each by calling
     * {@link #getScaledIcon(Icon)}.  The Radial menu by default will scale icons to an appropriate size for its
     * display, but that size is not compatible for use with a standard Java context menu.
     *
     * @param component the menu component for which to scale icons
     * @since 5.5
     */
    private void scaleIcons(@Nonnull Component component)
    {
        if (component instanceof JMenu menu)
        {
            menu.setIcon(getScaledIcon(menu.getIcon()));
            Stream.of(menu.getMenuComponents()).forEach(this::scaleIcons);
        } else if (component instanceof JMenuItem menuItem)
        {
            menuItem.setIcon(getScaledIcon(menuItem.getIcon()));
        }
    }

    /**
     * If the icon is of type {@code ImageIcon}, scale to {@link ImageUtils#SMALL_ICON_HEIGHT} and return a new
     * ImageIcon from the scaled instance.  Otherwise, return null.
     *
     * @param icon the icon to scale
     * @return a scaled {@code IconImage} if the parameter is of type {@code IconImage}, otherwise null
     * @since 5.5
     */
    @Nullable
    private ImageIcon getScaledIcon(Icon icon)
    {
        if (icon instanceof ImageIcon imageIcon)
        {
            Image newImage = imageIcon.getImage().getScaledInstance(ImageUtils.SMALL_ICON_WIDTH, ImageUtils.SMALL_ICON_HEIGHT, Image.SCALE_SMOOTH);
            return new ImageIcon(newImage);
        }
        return null;
    }

    /**
     * Create the actions for the stock menu choices and add them to a map that will be used when creating the menu.
     */
    private void createActions()
    {
        final String mapObjectUid = clickedMapObject.getUid();

        createAction(OPEN, e -> controller.showPluginDialog());
        createAction(HELP, e -> controller.showHelpDialog());
        createAction(CONFIGURE, e -> controller.showConfigurationDialog());
        createAction(UNDO, e -> controller.undoLastUserLocationChange(), controller::canUndoLastLocation, NO_STATE);
        createAction(DEVICE_LOG, e -> ((IIconMapObject) clickedMapObject).showDeviceLog());
        createAction(HIDE, e -> setVisibilityIfAllowed(false));
        createAction(SHOW, e -> setVisibilityIfAllowed(true));
        createAction(GOTO, e -> lastFocusedGlobe.goTo(mapObjectLocation), () -> mapObjectLocation != null, NO_STATE);
        createAction(DELETE, new DeleteAction(DELETE.toString(), clickedMapObject));
        createAction(REMOVE_FROM, getRemoveFromActionName(), e -> mapObjectManager.removeFromContainer((IIconMapObject) clickedMapObject), ENABLED, NO_STATE);
        createAction(PIN, new PinDynamicMeasureAction(clickedMapObject));
        createAction(LOCK_DISTANCE, e -> {
            var arrow = (IShapeArrowMapObject) clickedMapObject;
            arrow.setRangeLocked(!arrow.isRangeLocked());
        }, ENABLED, () -> ((IShapeArrowMapObject) clickedMapObject).isRangeLocked());
        createAction(MEASURE_TO, new MeasureToAction(MEASURE_TO.toString(), clickedMapObject));
        createAction(BREADCRUMBS, new ToggleBreadcrumbsAction(clickedMapObject, breadcrumbManager), ENABLED,
                () -> breadcrumbManager.areBreadcrumbsEnabled(mapObjectUid));

        final boolean enableFineAdjust = isSelfMarker || clickedMapObject.isAllowDrag();
        createAction(FINE_ADJUST, e -> fineAdjustAction.activate(lastFocusedGlobe, clickedMapObject), () -> enableFineAdjust, NO_STATE);

        // CoT actions - only if the Map Object allows being converted to CoT, and it's not one of our text annotations
        final boolean supportsCoT = !clickedMapObject.getMetaBoolean(StandardAttributeNames.NEVER_COT, false)
                && !(clickedMapObject instanceof IFlatAnnotationMapObject)
                && !(clickedMapObject instanceof IAnnotationMapObject);
        if (supportsCoT) createCotActions();

        // Alarm actions

        createAction(DEACTIVATE, e -> {
            controller.getDataMonitors().forEach(dataMonitor -> dataMonitor.onDataMonitorEvent(deactivateAlarmsEvent));
            controller.logDeviceEvent("Deactivated", "Deactivated Alarms");
        });

        createAction(CLEAR_ALARMS,
                e -> controller.getDataMonitors().forEach(dataMonitor -> dataMonitor.onDataMonitorEvent(clearAlarmsEvent)));

        // Shape specific actions

        createAction(EDITABLE, e -> {
            final var shapeMapObject = (IShapeMapObject<?>) clickedMapObject;
            final boolean wasEditable = shapeMapObject.isEditable();

            shapeMapObject.setEditable(!wasEditable && shouldAllowEdit(shapeMapObject));
        },
                ENABLED,
                this::isShapeEditable);

        createAction(ORIENTABLE, e -> {
            final var shapeMapObject = (IShapeMapObject<?>) clickedMapObject;
            shapeMapObject.setOrientable(!shapeMapObject.isOrientable());
        },
                this::isShapeEditable,
                this::isShapeOrientable);

        if ((clickedMapObject instanceof IShapeMapObject<?> shapeMapObject && (clickedMapObject instanceof IRegularShape || clickedMapObject instanceof IPolygon)))
        {
            createAction(GEOFENCE, e -> new EditGeoFenceDialog(null, shapeMapObject).setVisible(true), ENABLED, shapeMapObject::hasGeoFence);
        }

        createAction(DRIFTER, e -> EditDrifterConfigurationDialog.show(clickedMapObject), ENABLED, NO_STATE);

        if (clickedMapObject instanceof IMutableBullseye bullseye) createBullseyeActions(bullseye);

        FieldOfView fieldOfView = clickedMapObject.getFieldOfView();
        if (clickedMapObject instanceof Marker && fieldOfView != null) createSensorActions(fieldOfView);

        // Video action
        if (clickedMapObject.hasMetaValue(VideoDetailHandler.CONNECTION_ENTRY_ATTR))
        {
            final var ceAttrs = clickedMapObject.getAttributeSet().getAttributeSetAttribute(VideoDetailHandler.CONNECTION_ENTRY_ATTR);
            final String ceUid = ceAttrs.getStringAttribute(VideoDetailHandler.CONNECTION_ENTRY_UID_ATTR, "");
            mediaManager.getAllMedia().stream()
                    .filter(IVideo.class::isInstance)
                    .map(IVideo.class::cast)
                    .filter(video -> {
                        final ConnectionEntry ce = video.getConnectionEntry();
                        return ce != null && ce.getUID().equals(ceUid);
                    })
                    .findFirst()
                    .ifPresent(video -> createAction(VIDEO, e -> mediaPlayerManager.createMediaPlayer(new MediaPlayerOptions(video.getName()), video)));
        }
    }

    /**
     * Create the CoT related actions.
     * @since 5.3
     */
    private void createCotActions()
    {
        final String mapObjectName = clickedMapObject.getName();

        createAction(SEND_AS_COT, e -> new SendAsCotDialog(mapObjectName,
                () -> Collections.singletonList(cotExporterManager.export(clickedMapObject)),
                () -> userProxy.reportWarning("Not Exportable", "Could not export as CoT:\n   " +
                        mapObjectName + "\n   " + clickedMapObject))
                .setVisible(true));

        // The SA Message sender stores the latest SA message in the latestCotEvent on the MO, but we don't want
        // to show the user both options (last and current) for the self location marker, just the last SA message.
        //
        // Don't force the user into a sub-menu if there's only going to be a single option
        if (clickedMapObject.getLatestCotEvent() == null || isSelfMarker)
        {
            createAction(INSPECT_COT, isSelfMarker ? e -> controller.showLatestReceivedCotDialog() : e -> controller.showCotDialog());
        } else
        {
            createAction(INSPECT_COT, INSPECT_COT.toString(), null, ENABLED, NO_STATE, List.of(
                    new RadialMenuAction("Last Received", null, e -> controller.showLatestReceivedCotDialog(), ENABLED, NO_STATE),
                    new RadialMenuAction("Current", null, e -> controller.showCotDialog(), ENABLED, NO_STATE)));
        }
    }

    private void createBullseyeActions(@Nonnull IMutableBullseye bullseye)
    {
        createAction(CHANGE_DIRECTION, e -> bullseye.setPointingEdgeToCenter(!bullseye.isPointingEdgeToCenter()));
        createAction(TOGGLE_RANGE_RINGS, e -> bullseye.setShowRangeRings(!bullseye.isShowRangeRings()), ENABLED, bullseye::isShowRangeRings);
        List<ContextMenuEnum> menuOptions = List.of(MILS_TRUE_NORTH, DEG_TRUE_NORTH, MILS_MAG_NORTH, DEG_MAG_NORTH, MILS_GRID_NORTH, DEG_GRID_NORTH);
        List<RadialMenuAction> degreesSubMenu = new ArrayList<>();
        for (ContextMenuEnum contextMenuEnum : menuOptions)
        {
            Angle bearingUnits;
            NorthReference reference;
            switch (contextMenuEnum)
            {
                case MILS_TRUE_NORTH, DEG_TRUE_NORTH ->
                {
                    reference = NorthReference.TRUE;
                    bearingUnits = contextMenuEnum == MILS_TRUE_NORTH ? Angle.MIL : Angle.DEGREE;
                }
                case MILS_MAG_NORTH, DEG_MAG_NORTH ->
                {
                    reference = NorthReference.MAGNETIC;
                    bearingUnits = contextMenuEnum == MILS_MAG_NORTH ? Angle.MIL : Angle.DEGREE;
                }
                case MILS_GRID_NORTH, DEG_GRID_NORTH ->
                {
                    reference = NorthReference.GRID;
                    bearingUnits = contextMenuEnum == MILS_GRID_NORTH ? Angle.MIL : Angle.DEGREE;
                }
                default ->
                {
                    reference = NorthReference.TRUE;
                    bearingUnits = Angle.DEGREE;
                }
            }

            degreesSubMenu.add(new RadialMenuAction(contextMenuEnum.toString(), null, e -> {
                bullseye.setNorthReference(reference);
                bullseye.setBearingUnits(bearingUnits);
                menuSpaceManager.setPopupMenuText(CHANGE_DEGREES, String.join("\n", contextMenuEnum.toString().split(" ")));
            }, ENABLED, NO_STATE));
        }
        ContextMenuEnum initialOption = menuOptions.stream().map(mo -> switch (bullseye.getNorthReference())
        {
            case TRUE -> bullseye.getBearingUnits() == Angle.DEGREE ? DEG_TRUE_NORTH : MILS_TRUE_NORTH;
            case MAGNETIC -> bullseye.getBearingUnits() == Angle.DEGREE ? DEG_MAG_NORTH : MILS_MAG_NORTH;
            case GRID -> bullseye.getBearingUnits() == Angle.DEGREE ? DEG_GRID_NORTH : MILS_GRID_NORTH;
        }).findFirst().orElse(DEG_TRUE_NORTH);
        createAction(CHANGE_DEGREES, String.join("\n", initialOption.toString().split(" ")), null, ENABLED, NO_STATE, degreesSubMenu);
    }

    private void createSensorActions(@Nonnull FieldOfView fov)
    {
        List<RadialMenuAction> submenu = new ArrayList<>();
        submenu.add(new RadialMenuAction(SENSOR_DIRECTION.toString(), SENSOR_DIRECTION.getIconName(), e -> {
            globeClickService.activate("Click where to point the FOV", location -> {
                globeClickService.deactivate();
                double bearing = GeoCalculations.bearing(fov.getPoint(), location);
                fov.setAzimuth((float) bearing);
            });
        }, ENABLED, NO_STATE));

        submenu.add(new RadialMenuAction(SENSOR_VISIBILITY.toString(), SENSOR_VISIBILITY.getIconName(), e -> fov.setVisible(!fov.isVisible()), ENABLED, NO_STATE));

        ActionListener openPanel = FieldOfViewPanel.getInstance().createPanelActionListener(fov);

        submenu.add(new RadialMenuAction(CONFIGURE + " Sensor", CONFIGURE.getIconName(), openPanel, ENABLED, NO_STATE));
        createAction(SENSOR, SENSOR.toString(), openPanel, ENABLED, NO_STATE, submenu);
    }

    /**
     * Create an action for the given menu item using the supplied action listener, a default name, always
     * enabled and no state.
     *
     * @see #createAction(ContextMenuEnum, String, ActionListener, Supplier, Supplier)
     * @since 2.37
     */
    @SuppressWarnings("UnusedReturnValue")
    @Nonnull
    private RadialMenuAction createAction(@Nonnull ContextMenuEnum menuItem, @Nonnull ActionListener actionListener)
    {
        return createAction(menuItem, actionListener, ENABLED, NO_STATE);
    }

    /**
     * Create an action with a default name that can be enabled/disabled and may have an enabled state
     * and a selected state.
     *
     * @see #createAction(ContextMenuEnum, String, ActionListener, Supplier, Supplier)
     * @since 2.37
     */
    @Nonnull
    private RadialMenuAction createAction(@Nonnull ContextMenuEnum menuItem, @Nonnull ActionListener standardListener,
                                          @Nonnull Supplier<Boolean> enabledSupplier, @Nullable Supplier<Boolean> stateSupplier)
    {
        return createAction(menuItem, menuItem.toString(), standardListener, enabledSupplier, stateSupplier);
    }

    /**
     * Create an action that can be enabled/disabled and may have a "selected" state.
     * The menu space manager is consulted for replacement actions and names.
     *
     * @param menuItem           Menu enum
     * @param standardActionName Name for menu action, might be replaced by menuSpaceManager
     * @param standardListener   Action listener, might be replaced by menuSpaceManager
     * @param enabledSupplier    Supplier determining if the menu item is enabled
     * @param stateSupplier      Supplier determining if the menu item has "state", resulting in a checkbox menu item
     * @since 2.37
     */
    @Nonnull
    private RadialMenuAction createAction(@Nonnull ContextMenuEnum menuItem, @Nonnull String standardActionName,
                                          @Nonnull ActionListener standardListener, @Nonnull Supplier<Boolean> enabledSupplier,
                                          @Nullable Supplier<Boolean> stateSupplier)
    {
        return createAction(menuItem, standardActionName, standardListener, enabledSupplier, stateSupplier, null);
    }

    /**
     * Create an action that can be enabled/disabled, may have a "selected" state, and may have sub-menu actions.
     * The menu space manager is consulted for replacement actions and names.
     *
     * @param menuItem           Menu enum
     * @param standardActionName Name for menu action, might be replaced by menuSpaceManager
     * @param standardListener   Action listener, might be replaced by menuSpaceManager
     * @param enabledSupplier    Supplier determining if the menu item is enabled
     * @param stateSupplier      Supplier determining if the menu item has "state", resulting in a checkbox menu item
     *                           (Ignored for nested menu items)
     * @param subMenus           The submenus that are displayed when this menu action is selected
     * @since 5.0
     */
    @Nonnull
    private RadialMenuAction createAction(@Nonnull ContextMenuEnum menuItem, @Nonnull String standardActionName,
                                          @Nullable ActionListener standardListener, @Nonnull Supplier<Boolean> enabledSupplier,
                                          @Nullable Supplier<Boolean> stateSupplier, @Nullable List<RadialMenuAction> subMenus)
    {
        final String replacementName = menuSpaceManager.getTextReplacement(menuItem);
        final String actionName = replacementName != null ? replacementName : standardActionName;

        final Action redirectedAction = menuSpaceManager.getRedirectedAction(menuItem);
        final ActionListener actionListener = redirectedAction != null ? redirectedAction : standardListener;

        final RadialMenuAction action = subMenus == null
                ? new RadialMenuAction(actionName, menuItem.getIconName(), actionListener, enabledSupplier, stateSupplier)
                : new NestedRadialMenuAction(actionName, menuItem.getIconName(), actionListener, enabledSupplier, subMenus);

        actionMap.put(menuItem, action);

        return action;
    }

    /**
     * @return Action name to use for REMOVE_FROM action, empty string if not contained
     * @since 2.37
     */
    private String getRemoveFromActionName()
    {
        if (clickedMapObject instanceof final IIconMapObject iconMapObject)
        {
            return iconMapObject.isContained()
                    ? "Remove " + clickedMapObject.getName() + " from " + iconMapObject.getParentContainer().getName()
                    : "";
        }
        return "";
    }

    /**
     * @return Is the current MO editable, assumes the MO is a shape
     * @since 2.37
     */
    private boolean isShapeEditable()
    {
        return ((IShapeMapObject<?>) clickedMapObject).isEditable();
    }

    /**
     * @return Is the current MO orientable, assumes the MO is a shape
     * @since 2.37
     */
    private boolean isShapeOrientable()
    {
        return ((IShapeMapObject<?>) clickedMapObject).isOrientable();
    }

    /**
     * Set the visibility of the current MO if it has not been locked.
     *
     * @param visible New visible setting
     * @since 2.37
     */
    private void setVisibilityIfAllowed(boolean visible)
    {
        if (mapObjectManager.isVisibilityLocked(clickedMapObject.getKey()))
        {
            AlwaysOnTopJOptionPane.showMessageDialog(
                    "Map object visibility locked by device and cannot be changed by the user",
                    "Visibility Locked",
                    JOptionPane.INFORMATION_MESSAGE);
        } else
        {
            mapObjectManager.setVisible(clickedMapObject.getKey(), visible);
        }
    }

    /**
     * Confirm if the shape should become editable.
     *
     * @param shapeMapObject the shape map object that the confirmation dialog is for.
     * @return {@code true} if the user confirms the shape should become editable, {@code false} otherwise.
     * @since 2.37
     */
    private boolean shouldAllowEdit(IShapeMapObject<?> shapeMapObject)
    {
        final int listenerCount = shapeMapObject.getShapeListenerCount();

        // If there are no listeners, then no need to bother the user
        if (listenerCount == 0) return true;

        final String message = "Are you sure you want to edit this shape?\n\nIt is being monitored by "
                + (listenerCount > 1 ? "one plugin." : listenerCount + " plugins.");

        final Object[] dlgButtons = {"Yes", "No"};

        final int result = AlwaysOnTopJOptionPane.showOptionDialog(
                message,
                "Confirm Editable - " + shapeMapObject.getName(),
                JOptionPane.WARNING_MESSAGE, JOptionPane.YES_NO_OPTION,
                shapeMapObject.getSizedIcon(ImageUtils.DEFAULT_ICON_WIDTH),
                dlgButtons, dlgButtons[1]);

        return result == JOptionPane.YES_OPTION;
    }

    /**
     * Show this popup menu as a radial menu widget.
     *
     * @since 3.0
     */
    public void showAsRadialMenu()
    {
        //noinspection DataFlowIssue - last focused globe is never null
        widgetLayerManager.showRadialMenu(globeManager.getLastFocusedGlobe(), clickedMapObject, this, mouseEvent.getX(), mouseEvent.getY());
    }

    /**
     * Action listener that responds to menu clicks that enable or disable a marker's breadcrumbs.
     *
     * @param clickedMapObject The map object that was clicked
     * @param breadcrumbManager The breadcrumb manager
     * @since 5.3
     */
    private record ToggleBreadcrumbsAction(IMapObject clickedMapObject, IBreadcrumbManager breadcrumbManager) implements ActionListener
    {
        @Override
        public void actionPerformed(ActionEvent e)
        {
            final String uid = clickedMapObject.getUid();
            final boolean enabled = breadcrumbManager.areBreadcrumbsEnabled(uid);

            if (enabled)
            {
                breadcrumbManager.stopBreadcrumbs(uid);
            } else
            {
                breadcrumbManager.startBreadcrumbs(uid, getCallsign());
            }
        }

        /**
         * Attempts to retrieve the marker's callsign from its metadata. If no metadata value is set, returns either the
         * local user's callsign if the marker is the self marker or the map object's name.
         *
         * @return The track callsign to use for the marker
         * @since 5.4
         */
        private String getCallsign()
        {
            final var callsign = clickedMapObject.getMetaString(StandardAttributeNames.CALLSIGN, "");

            if (!StringUtils.isEmpty(callsign)) return callsign;

            return clickedMapObject.isSelfMarker()
                    ? PreferenceEntry.CALL_SIGN.getStringValue()
                    : clickedMapObject.getName();
        }
    }
}
