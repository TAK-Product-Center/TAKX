package gov.takx.core.mapobject;

import gov.tak.api.maps.Marker;
import gov.tak.api.maps.PointMapItem;
import gov.tak.api.util.StandardAttributeNames;
import gov.takx.api.annotation.PseudoBean;
import gov.takx.api.mapobject.IAnnotationMapObject;
import gov.takx.api.mapobject.IFlatAnnotationMapObject;
import gov.takx.api.mapobject.IIconMapObject;
import gov.takx.api.mapobject.IMapObject;
import gov.takx.api.mapobject.IShapeArrowMapObject;
import gov.takx.api.mapobject.IShapeMapObject;
import gov.takx.api.shapes.IBullseye;
import gov.takx.api.shapes.IPolygon;
import gov.takx.api.shapes.IRangeFan;
import gov.takx.api.shapes.IRegularShape;
import gov.takx.api.ui.IDeviceMenuSpaceManager;
import gov.takx.api.ui.IDeviceSwingUIProvider;
import gov.takx.api.ui.IResourceCleaner;
import gov.takx.core.globe.TerrainPopupMenuProvider;
import gov.takx.core.uibridge.SwingUIBinder;
import gov.takx.platform.preferences.PreferenceEntry;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import jakarta.annotation.PostConstruct;
import jakarta.inject.Inject;

import javax.swing.Action;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;

import java.awt.Component;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static gov.takx.api.ui.IMenuSpaceManager.ContextMenuEnum.BREADCRUMBS;
import static gov.takx.api.ui.IMenuSpaceManager.ContextMenuEnum.CENTER_ON;
import static gov.takx.api.ui.IMenuSpaceManager.ContextMenuEnum.CHANGE_DEGREES;
import static gov.takx.api.ui.IMenuSpaceManager.ContextMenuEnum.CHANGE_DIRECTION;
import static gov.takx.api.ui.IMenuSpaceManager.ContextMenuEnum.CLEAR_ALARMS;
import static gov.takx.api.ui.IMenuSpaceManager.ContextMenuEnum.DEACTIVATE;
import static gov.takx.api.ui.IMenuSpaceManager.ContextMenuEnum.DEVICE_LOG;
import static gov.takx.api.ui.IMenuSpaceManager.ContextMenuEnum.DRIFTER;
import static gov.takx.api.ui.IMenuSpaceManager.ContextMenuEnum.EDITABLE;
import static gov.takx.api.ui.IMenuSpaceManager.ContextMenuEnum.GEOFENCE;
import static gov.takx.api.ui.IMenuSpaceManager.ContextMenuEnum.HELP;
import static gov.takx.api.ui.IMenuSpaceManager.ContextMenuEnum.HIDE;
import static gov.takx.api.ui.IMenuSpaceManager.ContextMenuEnum.INSPECT_COT;
import static gov.takx.api.ui.IMenuSpaceManager.ContextMenuEnum.KEEP_IN_VIEW;
import static gov.takx.api.ui.IMenuSpaceManager.ContextMenuEnum.LOCK_DISTANCE;
import static gov.takx.api.ui.IMenuSpaceManager.ContextMenuEnum.OPEN;
import static gov.takx.api.ui.IMenuSpaceManager.ContextMenuEnum.ORIENTABLE;
import static gov.takx.api.ui.IMenuSpaceManager.ContextMenuEnum.PIN;
import static gov.takx.api.ui.IMenuSpaceManager.ContextMenuEnum.REMOVE_FROM;
import static gov.takx.api.ui.IMenuSpaceManager.ContextMenuEnum.SEND_AS_COT;
import static gov.takx.api.ui.IMenuSpaceManager.ContextMenuEnum.SHOW;
import static gov.takx.api.ui.IMenuSpaceManager.ContextMenuEnum.TOGGLE_RANGE_RINGS;
import static gov.takx.api.ui.IMenuSpaceManager.ContextMenuEnum.UNDO;
import static gov.takx.api.ui.IMenuSpaceManager.ContextMenuEnum.values;

/**
 * Menu space manager for device plugins.  Handles plugin customization of the standard context menu for Map Objects.
 * <p>
 * Note that instances of this class are used for ALL types of Map Objects, not just devices.  But, only devices
 * associated with a plugin can have menu customizations.
 *
 */
@SuppressWarnings("CdiManagedBeanInconsistencyInspection")
@PseudoBean
public class DeviceMenuSpaceManager implements IDeviceMenuSpaceManager
{
    // Menu items that are only available for Devices
    private static final List<ContextMenuEnum> DEVICE_ONLY_MENU_ITEMS = Arrays.asList(
            CLEAR_ALARMS, DEACTIVATE, DEVICE_LOG, HELP);

    // Menu items that are only available for Shapes
    private static final List<ContextMenuEnum> SHAPE_ONLY_MENU_ITEMS = Arrays.asList(EDITABLE, ORIENTABLE, GEOFENCE);

    // Menu items that can not be overridden/redirected
    private static final List<ContextMenuEnum> NON_OVERRIDABLE_MENU_ITEMS = Arrays.asList(CENTER_ON, KEEP_IN_VIEW);

    @Inject
    private SwingUIBinder swingUIBinder;

    @Inject
    private TerrainPopupMenuProvider terrainPopupMenuProvider;

    private final Map<ContextMenuEnum, Action> redirectionMap = new EnumMap<>(ContextMenuEnum.class);
    private final List<IIndexedComponent> additionalMenuItems = new ArrayList<>();
    private final Set<ContextMenuEnum> removedMenuEntries = EnumSet.noneOf(ContextMenuEnum.class);
    private final Map<ContextMenuEnum, String> renamedMenuEntries = new EnumMap<>(ContextMenuEnum.class);
    private final MapObjectController controller;
    private final IMapObject mapObject;
    private final List<ContextMenuEnum> menuOrder = new ArrayList<>(Arrays.asList(values()));

    /**
     * Construct the menu manager for the given controller.
     *
     * @since 2.37
     */
    DeviceMenuSpaceManager(@Nonnull MapObjectController controller)
    {
        this.controller = controller;
        mapObject = controller.getMapObject();
    }

    @PostConstruct
    private void postConstruct()
    {
        removeExcludedMenuItems();
    }

    @Nullable
    @Override
    public Action getRedirectedAction(@Nonnull ContextMenuEnum menuEntry)
    {
        return redirectionMap.get(menuEntry);
    }

    @Nullable
    @Override
    public String getTextReplacement(@Nonnull ContextMenuEnum menuEntry)
    {
        return renamedMenuEntries.get(menuEntry);
    }

    @Nonnull
    @Override
    public List<IIndexedComponent> getAdditionalMenuItems()
    {
        return Collections.unmodifiableList(additionalMenuItems);
    }

    @Nonnull
    @Override
    public List<ContextMenuEnum> getMenuOrder()
    {
        final ArrayList<ContextMenuEnum> finalOrder = new ArrayList<>(menuOrder);

        // Remove all entries specified by our plugin
        finalOrder.removeAll(removedMenuEntries);

        // Remove any menu items based on the current state of the MO
        if (!controller.canUndoLastLocation()) finalOrder.remove(UNDO);

        finalOrder.remove(mapObject.isVisible() ? SHOW : HIDE);

        final boolean isIconMo = mapObject instanceof IIconMapObject;

        if (!isIconMo || !((IIconMapObject) mapObject).isContained())
        {
            finalOrder.remove(REMOVE_FROM);

            // Markers don't have device logs, so remove the option if this is not an IIconMapObject
            if (!isIconMo) finalOrder.remove(DEVICE_LOG);
        }

        final boolean neverCot = mapObject.getMetaBoolean(StandardAttributeNames.NEVER_COT, false)
                // Note: new instances are NEVER_COT, but leaving these to support existing MOs
                || mapObject instanceof IFlatAnnotationMapObject
                || mapObject instanceof IAnnotationMapObject;

        // Remove the "inspect CoT" menu item if inspection is disabled
        if (neverCot || !PreferenceEntry.DEV_MODE_COT_MESSAGE_INSPECTION_ENABLED.getBooleanValue()) finalOrder.remove(INSPECT_COT);
        if (neverCot) finalOrder.remove(SEND_AS_COT);

        return finalOrder;
    }

    /**
     * Remove Menu items from the given list based on what type of map object this is, whether it has alarms and if it
     * provides UI elements for HELP.
     *
     * @since 2.37
     */
    private void removeExcludedMenuItems()
    {
        if (!(mapObject instanceof IShapeMapObject)) menuOrder.removeAll(SHAPE_ONLY_MENU_ITEMS);

        if (!(mapObject instanceof IRegularShape || mapObject instanceof IPolygon) || mapObject instanceof IRangeFan || mapObject instanceof IBullseye) menuOrder.remove(GEOFENCE);

        if (mapObject instanceof IBullseye)
        {
            menuOrder.remove(EDITABLE);
            menuOrder.remove(ORIENTABLE);
            menuOrder.remove(OPEN);
        } else
        {
            menuOrder.remove(TOGGLE_RANGE_RINGS);
            menuOrder.remove(CHANGE_DIRECTION);
            menuOrder.remove(CHANGE_DEGREES);
        }

        if (!(mapObject instanceof IShapeArrowMapObject arrow && arrow.getAssociatedShape().isDynamic()))
        {
            menuOrder.remove(PIN);
            menuOrder.remove(LOCK_DISTANCE);
        }

        if (!(mapObject instanceof Marker)) menuOrder.remove(BREADCRUMBS);
        if (!(mapObject instanceof PointMapItem)) menuOrder.remove(DRIFTER);

        if (mapObject.isDevice())
        {
            final IDeviceSwingUIProvider uiProvider = swingUIBinder.forDevice(controller);

            final JPanel helpPanel = uiProvider.getPluginHelpPanel();
            if (helpPanel == null) menuOrder.remove(HELP);

            // Since we just created it to see if it was non-null, we need to release resources on it
            if (helpPanel instanceof IResourceCleaner resourceCleaner) resourceCleaner.releaseResources();

            // If the MO can't, or doesn't, have alarms, then remove the "clear alarms" or "deactivate" menu items
            if (!mapObject.canHaveAlarms() || mapObject.getController().getDataMonitors().isEmpty())
            {
                menuOrder.remove(CLEAR_ALARMS);
                menuOrder.remove(DEACTIVATE);
            }
        } else
        {
            menuOrder.removeAll(DEVICE_ONLY_MENU_ITEMS);
        }
    }

    @Override
    public void redirectPopupMenuAction(@Nonnull ContextMenuEnum menuEntry, @Nullable Action action)
    {
        if (null == action)
        {
            redirectionMap.remove(menuEntry);
        } else
        {
            if (NON_OVERRIDABLE_MENU_ITEMS.contains(menuEntry))
            {
                throw new IllegalArgumentException("Can not override menu item: " + menuEntry);
            }
            redirectionMap.put(menuEntry, action);
        }
    }

    @Override
    public void setPopupMenuVisible(@Nonnull ContextMenuEnum menuEntry, boolean visible)
    {
        if (visible)
        {
            removedMenuEntries.remove(menuEntry);
        } else
        {
            removedMenuEntries.add(menuEntry);
        }
    }

    @Override
    public void setPopupMenuText(@Nonnull ContextMenuEnum menuEntry, @Nullable String text)
    {
        if (NON_OVERRIDABLE_MENU_ITEMS.contains(menuEntry))
        {
            throw new IllegalArgumentException("Can not override menu item: " + menuEntry);
        }
        renamedMenuEntries.put(menuEntry, text);
    }

    @Override
    public void setMenuOrder(@Nonnull List<ContextMenuEnum> menuEntries)
    {
        menuOrder.clear();
        menuOrder.addAll(menuEntries);

        // Ensure the plugin didn't try to include entries that aren't applicable to this MO
        removeExcludedMenuItems();
    }

    @Override
    public void addDevicePopupMenuComponent(@Nonnull Component component)
    {
        addDevicePopupMenuComponent(component, -1);
    }

    @Override
    public void addDevicePopupMenuComponent(@Nonnull Component component, int index)
    {
        removeDevicePopupMenuComponent(component); // In case it was already added with a possibly different index

        int insertIndex = index == -1 ? Integer.MAX_VALUE : index;

        additionalMenuItems.add(new IndexedComponent(component, insertIndex));
        additionalMenuItems.sort(Comparator.comparingInt(IIndexedComponent::getIndex)); // Keep it sorted
    }

    @Override
    public void addDevicePopupMenuSeparator()
    {
        addDevicePopupMenuSeparator(-1);
    }

    @Override
    public void addDevicePopupMenuSeparator(int index)
    {
        addDevicePopupMenuComponent(new JPopupMenu.Separator(), index);
    }

    @Override
    public void removeDevicePopupMenuComponent(@Nonnull Component component)
    {
        additionalMenuItems.removeIf(indexedMenuItem -> indexedMenuItem.getComponent().equals(component));
    }

    @Override
    public void addTerrainPopupMenuItem(@Nonnull JMenuItem menuItem)
    {
        terrainPopupMenuProvider.addTerrainPopupMenuItem(menuItem);
    }

    @Override
    public void removeTerrainPopupMenuItem(@Nonnull JMenuItem menuItem)
    {
        terrainPopupMenuProvider.removeTerrainPopupMenuItem(menuItem);
    }

    /**
     * Wrapper to track menu component and index.
     */
    @SuppressWarnings("ClassCanBeRecord") // Converting to a record would be a breaking change to the public interface
    private static class IndexedComponent implements IIndexedComponent
    {
        private final Component component;
        private final int index;

        private IndexedComponent(@Nonnull Component component, int index)
        {
            this.component = component;
            this.index = index;
        }

        @Nonnull
        @Override
        public Component getComponent()
        {
            return component;
        }

        @Override
        public int getIndex()
        {
            return index;
        }
    }
}
