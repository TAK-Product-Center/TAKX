package gov.takx.core.explorer.common;

import gov.takx.api.annotation.PseudoBean;
import gov.takx.api.coordinatesystem.ILocation;
import gov.takx.api.explorer.ADataMonitorServicesExplorable;
import gov.takx.api.explorer.IExplorable;
import gov.takx.api.explorer.IMapObjectExplorable;
import gov.takx.api.mapobject.IMapObject;
import gov.takx.api.mapobject.IMapObjectController;
import gov.takx.api.mapobject.IMapObjectManager;
import gov.takx.core.mapobject.gui.DeleteAction;
import gov.takx.core.mapobject.gui.MapObjectPopupMenu;
import jakarta.annotation.Nonnull;
import jakarta.inject.Inject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.JPopupMenu;

import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;
import java.beans.PropertyChangeEvent;
import java.lang.invoke.MethodHandles;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * IExplorable implementation for map objects.
 *
 * @since 1.48
 */
@SuppressWarnings("CdiManagedBeanInconsistencyInspection")
@PseudoBean
public class MapObjectExplorable extends ADataMonitorServicesExplorable implements IMapObjectExplorable
{
    private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    /**
     * Map Object property names.  These are to be used as keys for this class' {@link #getValue(String)}
     * implementation.
     */
    protected static final Set<String> KEY_SET =
            Set.of(FAMILY,
                    TYPE,
                    LAST_COMMUNICATION_TIME,
                    IMapObject.ICON,
                    IMapObject.LOCATION,
                    IMapObject.ICON_SCALE,
                    IMapObject.NAME,
                    IMapObject.DESCRIPTION,
                    IMapObject.REMARKS,
                    IMapObject.HIGHEST_STATE,
                    IMapObject.DEFAULT_TRACK_COLOR,
                    IMapObject.VISIBILITY,
                    IMapObject.ALLOW_DRAG,
                    IMapObject.ALLOW_DEVICE_CONTAINMENT,
                    IMapObject.MOST_RECENT_COMMUNICATION_TIME,
                    IMapObject.OLDEST_COMMUNICATION_TIME,
                    IMapObject.SNAP_ICON_TO_SURFACE,
                    IMapObject.ELEVATION_MODE,
                    DATA_MONITORS_KEY);

    protected static final Map<String, String> PROPERTY_NAME_MAP = Map.of(
            IMapObject.VISIBILITY, IExplorable.VISIBLE,
            IMapObject.ICON, IExplorable.ICON_IMAGE,
            IMapObject.NAME, IExplorable.DISPLAY_NAME,
            IMapObject.HIGHEST_STATE, IExplorable.HIGHEST_ALARM_STATE_KEY);

    @Inject
    private IMapObjectManager mapObjectManager;

    protected final IMapObject mapObject;

    public MapObjectExplorable(IMapObject mapObject)
    {
        super(mapObject, List.of(mapObject.getGroupName(), mapObject.getType()), mapObject.getKey(), false, mapObject.getController());

        this.mapObject = mapObject;
        // Pre-populate the visible boolean, otherwise change events don't fire correctly
        visible.set(mapObject.isVisible());
        mapObject.addPropertyChangeListener(this);
    }

    @Nonnull
    @Override
    public String getDisplayName()
    {
        return mapObject.getName();
    }

    /**
     * Get the key collection for key/value pairs.  In this case, it is made up of all the map object's properties.
     *
     * @return key collection for key/value pairs.
     */
    @Override
    public Collection<String> getKeys()
    {
        return KEY_SET;
    }

    @Override
    public boolean hasKey(String key)
    {
        return KEY_SET.contains(key);
    }

    @Override
    public Object getValue(String key)
    {
        if (!hasKey(key))
        {
            logger.debug("MapExplorerObject does not have key={}", key);
            return null;
        }

        // Although this method behaves similarly to a java.util.Map, IMapObjects don't actually support this
        // interface.  A huge switch statement will have to do.
        return switch (key)
        {
            case FAMILY -> mapObject.getFamily();
            case TYPE -> mapObject.getType();
            case LAST_COMMUNICATION_TIME, IMapObject.MOST_RECENT_COMMUNICATION_TIME -> mapObject.getMostRecentCommunicationTime();
            case IMapObject.ICON -> mapObject.getIconImage();
            case IMapObject.LOCATION -> mapObject.getLocation();
            case IMapObject.ICON_SCALE -> mapObject.getIconScale();
            case IMapObject.NAME -> mapObject.getName();
            case IMapObject.DESCRIPTION -> mapObject.getDescription();
            case IMapObject.REMARKS -> mapObject.getRemarks();
            case IMapObject.HIGHEST_STATE, HIGHEST_ALARM_STATE_KEY -> mapObject.getHighestState();
            case IMapObject.DEFAULT_TRACK_COLOR -> mapObject.getDefaultTrackColor();
            case IMapObject.VISIBILITY -> mapObject.isVisible();
            case IMapObject.ALLOW_DRAG -> mapObject.isAllowDrag();
            case IMapObject.ALLOW_DEVICE_CONTAINMENT -> mapObject.isAllowDeviceToBeContainer();
            case IMapObject.OLDEST_COMMUNICATION_TIME -> mapObject.getOldestCommunicationTime();
            case IMapObject.SNAP_ICON_TO_SURFACE -> mapObject.isSnapIconToSurface();
            case IMapObject.ELEVATION_MODE -> mapObject.getElevationMode();
            case DATA_MONITORS_KEY -> getDataMonitors();
            default -> null;
        };
    }

    @Override
    public BufferedImage getIconImage()
    {
        return mapObject.getIconImage();
    }

    @Override
    public boolean isVisible()
    {
        return mapObject.isVisible();
    }

    @Override
    public void setVisible(boolean visible)
    {
        mapObject.setVisible(visible);
    }

    /**
     * {@inheritDoc}
     *
     * @return the result of {@link IMapObjectManager#isVisibilityLocked(String)} for this explorables unique key
     * @since 5.3
     */
    @Override
    public boolean isVisibilityLocked()
    {
        return mapObjectManager.isVisibilityLocked(getUniqueKey());
    }

    @Override
    public void dispose()
    {
        mapObject.removePropertyChangeListener(this);
        super.dispose();
    }

    @Override
    public IMapObject getMapObject()
    {
        return mapObject;
    }

    @Override
    public void showDialog()
    {
        getMapObjectController().showPluginDialog();
    }

    @Override
    public ILocation getLocation()
    {
        return mapObject.getLocation();
    }

    @Override
    public boolean isGeolocated()
    {
        return true;
    }

    @Override
    public IMapObjectController getMapObjectController()
    {
        return mapObject.getController();
    }

    @Override
    public JPopupMenu createPopupMenu(@Nonnull MouseEvent mouseEvent)
    {
        final MapObjectPopupMenu popupMenu = new MapObjectPopupMenu(mouseEvent, getMapObjectController());
        popupMenu.show(mouseEvent.getComponent(), mouseEvent.getX(), mouseEvent.getY());
        return popupMenu;
    }

    @Override
    public void requestDelete()
    {
        new DeleteAction("Delete", mapObject).actionPerformed(null);
    }

    @Override
    public String toString()
    {
        return "MapObjectExplorable{mapObject=" + mapObject + '}';
    }

    @Override
    public void deleteSilently()
    {
        mapObjectManager.deleteMapObject(mapObject);
    }

    @Override
    public boolean supportsSilentDeletion()
    {
        return true;
    }

    /**
     * {@inheritDoc}<br>
     * Additionally, this implementation uses {@link #PROPERTY_NAME_MAP} to map properties from the map object api to those
     * within the explorer api, to further decouple them.
     *
     * @param event A PropertyChangeEvent object describing the event source and the property that has changed.
     * @since 5.3
     */
    @Override
    public void propertyChange(PropertyChangeEvent event)
    {
        super.propertyChange(event);
        if (PROPERTY_NAME_MAP.containsKey(event.getPropertyName())) firePropertyChange(new PropertyChangeEvent(this, PROPERTY_NAME_MAP.get(event.getPropertyName()), event.getOldValue(), event.getNewValue()));
    }
}
