package gov.takx.core.mapobject;

import gov.takx.api.annotation.PseudoBean;
import gov.takx.api.mapobject.ICoreMapObjectManager;
import gov.takx.api.mapobject.IMapObject;
import gov.takx.api.mapobject.IMapObjectController;
import gov.takx.api.mapobject.ITrackContextMenuListener;
import gov.takx.api.messages.IRaptorDataMessage;
import gov.takx.api.messages.IRaptorMessage;
import jakarta.annotation.PostConstruct;
import jakarta.inject.Inject;

import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * Popup menu offering plugin supplied user actions on tracks. This menu will be supplied if the user right-clicks
 * on a track on a globe or in the device log.
 *
 * @since 2.26
 */
@SuppressWarnings("CdiManagedBeanInconsistencyInspection")
@PseudoBean
public class TrackPopupMenu extends JPopupMenu
{
    @Inject
    private ICoreMapObjectManager mapObjectManager;

    private final List<IRaptorDataMessage> msgs;

    /**
     * Construct the popup menu for the mouse event on the given track(s).
     *
     * @param msgs The tracks that were clicked
     */
    public TrackPopupMenu(List<IRaptorDataMessage> msgs)
    {
        this.msgs = msgs;
    }

    @PostConstruct
    private void postConstruct()
    {
        createMenu(msgs);
    }

    /**
     * Construct our menu contents from our actions
     */
    private void createMenu(List<IRaptorDataMessage> msgs)
    {
        final Set<JMenuItem> currentMenuItems = new LinkedHashSet<>();

        // Get the collection of unique map objects associated with the RDMs, then add any menu items from
        // listeners registered with the map object controllers.
        msgs.stream()
                .map(IRaptorMessage::getMapObject)
                .distinct() // May be more than one msg from a given MO, so get distinct MOs
                .map(IMapObject::getController)
                .map(IMapObjectController::getContextMenuListeners)
                .forEach(listeners -> addMenuItemsFromListeners(listeners, msgs, currentMenuItems));

        List<ITrackContextMenuListener> listeners = mapObjectManager.getTrackContextMenuListeners();
        addMenuItemsFromListeners(listeners, msgs, currentMenuItems);

        currentMenuItems.forEach(this::add);
    }

    /**
     * Queries the provided listeners for menu items and adds them to the provided set.
     *
     * @param listeners        Listeners to query for menu items
     * @param msgs             The messages for which the popup menu is being shown
     * @param currentMenuItems The set to which to add any new menu items from the listeners
     * @since 2.37
     */
    private void addMenuItemsFromListeners(List<ITrackContextMenuListener> listeners, List<IRaptorDataMessage> msgs,
                                           Set<JMenuItem> currentMenuItems)
    {
        listeners.forEach(listener -> {
            List<JMenuItem> menuItems = listener.onContextMenu(msgs);
            if (menuItems != null)
            {
                currentMenuItems.addAll(menuItems);
            }
        });
    }
}
