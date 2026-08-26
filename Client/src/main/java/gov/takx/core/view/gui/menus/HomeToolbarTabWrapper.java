package gov.takx.core.view.gui.menus;

import gov.takx.api.annotation.ProgramPreference;
import gov.takx.api.annotation.PseudoBean;
import gov.takx.api.shapes.ShapeType;
import gov.takx.api.toolbar.ButtonType;
import gov.takx.api.toolbar.IToolbarManager;
import gov.takx.api.toolbar.TabType;
import gov.takx.commons.swing.ButtonAction;
import gov.takx.commons.swing.IPopupMenuProvider;
import gov.takx.core.eventlog.LogInspector;
import gov.takx.core.globe.MultiSelectorService;
import gov.takx.core.toolbar.AToolbarComponent;
import gov.takx.core.toolbar.ToolbarSeparator;
import gov.takx.core.view.gui.ConfigurationDialog;
import gov.takx.core.view.gui.CoreComponentType;
import gov.takx.core.view.gui.IExtendedUISpaceManager;
import gov.takx.core.view.gui.breadcrumb.TracksUiUtils;
import gov.takx.core.view.gui.eventlog.EventLogPanel;
import gov.takx.core.view.gui.favorites.ManageFavoritesDialog;
import gov.takx.platform.preferences.PreferenceEntry;
import gov.takx.platform.ui.ResourceDirectory;
import gov.takx.platform.ui.UIResources;
import gov.takx.platform.ui.location.GoToLocationDialog;
import jakarta.annotation.PostConstruct;
import jakarta.inject.Inject;

import javax.swing.Icon;
import javax.swing.JPopupMenu;

import java.util.ArrayList;
import java.util.List;

/**
 */
@PseudoBean
public class HomeToolbarTabWrapper extends AToolbarTabWrapper
{
    private static final String CITY_SEARCH_TOOLTIP = "Search for city locations on the globe.";
    private static final String CONNECTIONS_TOOLTIP = "View the status of your connections and manage connections.";
    private static final String CHATS_TOOLTIP = "View the most recent message from each conversation.";
    private static final String CONTACTS_TOOLTIP = "View and communicate with both TAK and Non-TAK contacts available through a server or the local network.";
    private static final String DATA_PACKAGES_TOOLTIP = "View and manage your Data Packages.";
    private static final String DATA_SYNC_TOOLTIP = "View and subscribe to Data Feeds.";
    private static final String DEVICES_TOOLTIP = "View the devices you currently have installed and add them to the globe.";
    private static final String DRAW_TOOLTIP = "Select the Draw tab.";
    private static final String EVENT_LOG_TOOLTIP = "View a record of all of the events that have happened in this project.";
    private static final String FILES_TOOLTIP = "View and manage the files that have been attached to this project. Attach additional files to this project.";
    private static final String GO_TO_LOCATION_TOOLTIP = "Enter coordinates and zoom to a specific location on the globe.";
    private static final String MAPS_TOOLTIP = "View your map overlays including imagery, elevation, shapes, overlays, and extras. Add, delete, and manage map overlays. Click the lower part of this button to access Favorites.";
    private static final String MEDIA_TOOLTIP = "View audio, video, and image data and add, delete, and manage media files and streams.";
    private static final String MULTI_SELECTOR_TOOLTIP = "Select a group of map features to operate on";
    private static final String NAVIGATION_LOCK_TOOLTIP = "Lock the globe to prevent inadvertent navigation.";
    private static final String NEW_GLOBE_TOOLTIP = "Create a new secondary globe.";
    private static final String NOTIFICATIONS_TOOLTIP = "View the notification message history.";
    private static final String OVERLAYS_TOOLTIP = "View a list of the devices, icons, annotations, and shapes currently on the globe.";
    private static final String PLAYBACK_TOOLTIP = "View the Playback panel and review device locations within this project. Live data will still be received and will be rendered on the screen if applicable.";
    private static final String POINT_DROPPER_TOOLTIP = "Add one or more points to the globe by providing a location for each point.";
    private static final String EMERGENCY_ALERT_TOOLTIP = "Broadcast an emergency alert";
    private static final String PROPERTIES_TOOLTIP = "View and edit the properties of whichever item on the globe is selected.";
    private static final String ROUTES_TOOLTIP = "View and create routes.";
    private static final String SYSTEM_MESSAGES_TOOLTIP = "View the System Messages panel";
    private static final String TAC_GRAPHICS_TOOLTIP = "View 2525 symbols and manage add them to the globe.";
    private static final String TAK_SERVER_TOOLTIP = "View and manage your TAK Server connections.";
    private static final String ELEVATION_TOOLS_TOOLTIP = "Elevation Tools";

    @Inject
    private IToolbarManager toolbarManager;

    @Inject
    @ProgramPreference(PreferenceEntry.GLOBE_ENABLE_MULTIPLE_GLOBES)
    private boolean multipleGlobesEnabled;

    @Inject
    private MultiSelectorService multiSelectorService;

    @PostConstruct
    private void postConstruct()
    {
        List<AToolbarComponent> components = new ArrayList<>();

        var navigationLockPopupMenuProvider = new NavigationLockPopupMenuProvider();
        components.add(createButtonWithPopupMenu(ButtonType.NAVIGATION_LOCK,
                e -> navigationLockPopupMenuProvider.toggleTotalNavigationLock(), navigationLockPopupMenuProvider, NAVIGATION_LOCK_TOOLTIP));
        if (multipleGlobesEnabled) components.add(createButton(ButtonType.NEW_GLOBE, e -> globeManager.createGlobeDockable(), NEW_GLOBE_TOOLTIP));
        components.add(new ToolbarSeparator(TabType.HOME, components.size()));

        components.add(createTogglePanelButton(CoreComponentType.OVERLAYS_PANEL, OVERLAYS_TOOLTIP));

        components.add(createButtonWithPopupMenu(ButtonType.MAPS,
                e -> uiSpaceManager.toggleComponent(CoreComponentType.MAPS_PANEL), new MapsPopupMenuProvider(), MAPS_TOOLTIP)
                .setDisplayText("Maps & Favorites")); // Set custom display text since this does more than display the maps panel
        components.add(createTogglePanelButton(CoreComponentType.ELEVATION_TOOLS, ELEVATION_TOOLS_TOOLTIP));

        components.add(createButtonWithPopupMenu(ButtonType.MULTI_SELECTOR,
                e -> multiSelectorService.arm(ShapeType.FREEHAND_POLYGON),
                new MultiSelectorPopupMenuProvider(), MULTI_SELECTOR_TOOLTIP)
                .setDisplayText("Select")); // Set custom display text since this provides a dropdown
        components.add(new ToolbarSeparator(TabType.HOME, components.size()));

        components.add(createTogglePanelButton(CoreComponentType.EMERGENCY_ALERT_PANEL, EMERGENCY_ALERT_TOOLTIP));
        components.add(new ToolbarSeparator(TabType.HOME, components.size()));

        components.add(createTogglePanelButton(CoreComponentType.POINT_DROPPER_PANEL, POINT_DROPPER_TOOLTIP));
        components.add(createButton(ButtonType.DRAW, e -> toolbarManager.selectSingularTab(TabType.DRAW), DRAW_TOOLTIP));
        components.add(createTogglePanelButton(CoreComponentType.ROUTES_PANEL, ROUTES_TOOLTIP));
        components.add(createButton(ButtonType.GO_TO_LOCATION, e -> new GoToLocationDialog().setVisible(true), GO_TO_LOCATION_TOOLTIP));
        components.add(createTogglePanelButton(CoreComponentType.CITY_SEARCH_PANEL, CITY_SEARCH_TOOLTIP));
        components.add(new ToolbarSeparator(TabType.HOME, components.size()));

        components.add(createTogglePanelButton(CoreComponentType.CHAT_PANEL, CHATS_TOOLTIP));
        components.add(createTogglePanelButton(CoreComponentType.CONTACTS_PANEL, CONTACTS_TOOLTIP));
        components.add(createTogglePanelButton(CoreComponentType.PLAYBACK_PANEL, PLAYBACK_TOOLTIP));
        components.add(createTogglePanelButton(CoreComponentType.DEVICES_PANEL, DEVICES_TOOLTIP));
        components.add(createTogglePanelButton(CoreComponentType.FILES_PANEL, FILES_TOOLTIP));
        components.add(createTogglePanelButton(CoreComponentType.PROPERTIES_PANEL, PROPERTIES_TOOLTIP));
        components.add(new ToolbarSeparator(TabType.HOME, components.size()));

        components.add(createTogglePanelButton(CoreComponentType.CONNECTIONS_PANEL, CONNECTIONS_TOOLTIP));
        components.add(createTogglePanelButton(CoreComponentType.MEDIA_PANEL, MEDIA_TOOLTIP));
        components.add(createTogglePanelButton(CoreComponentType.TAK_SERVER_PANEL, TAK_SERVER_TOOLTIP));
        components.add(createTogglePanelButton(CoreComponentType.DATA_PACKAGE_PANEL, DATA_PACKAGES_TOOLTIP));
        components.add(createTogglePanelButton(CoreComponentType.DATA_SYNC_PANEL, DATA_SYNC_TOOLTIP));
        components.add(createTogglePanelButton(CoreComponentType.TAC_GRAPHICS_PANEL, TAC_GRAPHICS_TOOLTIP));
        components.add(new ToolbarSeparator(TabType.HOME, components.size()));

        components.add(createButtonWithPopupMenu(ButtonType.TRACKS, e -> uiSpaceManager.toggleComponent(CoreComponentType.TRACKS_PANEL),
                new TracksPopupMenuProvider(), "Tracks"));
        components.add(createTogglePanelButton(CoreComponentType.NOTIFICATIONS_PANEL, NOTIFICATIONS_TOOLTIP));

        if (PreferenceEntry.ENABLE_EVENT_LOG.getBooleanValue())
        {
            components.add(createButtonWithPopupMenu(ButtonType.EVENT_LOG,
                    e -> uiSpaceManager.toggleComponent(CoreComponentType.EVENT_LOG_PANEL), new EventLogPopupMenuProvider(), EVENT_LOG_TOOLTIP));
        }

        components.add(createTogglePanelButton(CoreComponentType.SYSTEM_MESSAGES_PANEL, SYSTEM_MESSAGES_TOOLTIP));

        createTab(TabType.HOME, components, true);
    }

    @Override
    protected void addComponents()
    {
        // Nothing to do, we populated our tab upon construction since the user sees it right away on startup
    }

    /**
     * Provider of the popup menu for the maps & favorites button.
     * @since 3.4
     */
    @PseudoBean
    private static class MapsPopupMenuProvider implements IPopupMenuProvider
    {
        private static final Icon MAPS_ICON =
                UIResources.getActionButtonIcon(ResourceDirectory.TAK_ASSETS_PNG, "Maps-And-Favorites_32x32.png");
        private static final Icon MANAGE_FAVORITES_ICON =
                UIResources.getActionButtonIcon(ResourceDirectory.PLATFORM_ICONS, "favorite_settings.png");
        @Inject
        private IExtendedUISpaceManager uiSpaceManager;

        private final JPopupMenu popupMenu = new JPopupMenu();

        public MapsPopupMenuProvider()
        {
            popupMenu.add(new ButtonAction("Maps & Layers", MAPS_ICON, "View and manage map layers and widgets",
                    e -> uiSpaceManager.toggleComponent(CoreComponentType.MAPS_PANEL)));
            popupMenu.add(new ButtonAction("Favorites", MANAGE_FAVORITES_ICON, "View and manage favorites",
                    e -> new ManageFavoritesDialog().setVisible(true)));
        }

        @Override
        public JPopupMenu getPopupMenu()
        {
            return popupMenu;
        }
    }

    /**
     * Provider of the popup menu for the event log button.
     * @since 3.4
     */
    @PseudoBean
    private static class EventLogPopupMenuProvider implements IPopupMenuProvider
    {
        @Inject
        private LogInspector logInspector;

        @Inject
        private IExtendedUISpaceManager uiSpaceManager;

        private final JPopupMenu popupMenu = new JPopupMenu();

        public EventLogPopupMenuProvider()
        {
            popupMenu.add(new ButtonAction(EventLogPanel.TITLE, e -> uiSpaceManager.showComponent(CoreComponentType.EVENT_LOG_PANEL)));
            popupMenu.add(new ButtonAction("Application Log", e -> logInspector.showLog()));
            popupMenu.add(new ButtonAction("Configuration Log", e -> new ConfigurationDialog().setVisible(true)));
        }

        @Override
        public JPopupMenu getPopupMenu()
        {
            return popupMenu;
        }
    }

    /**
     * Provider of the popup menu for the multiselect button.
     * @since 3.5
     */
    @PseudoBean
    private class MultiSelectorPopupMenuProvider implements IPopupMenuProvider
    {
        private static final Icon RECTANGLE_ICON =
                UIResources.getActionButtonIcon(ResourceDirectory.PLATFORM_ICONS, "rectangle.png");
        private static final Icon POLYGON_ICON =
                UIResources.getActionButtonIcon(ResourceDirectory.PLATFORM_ICONS, "polygon.png");
        private static final Icon LASSO_ICON =
                UIResources.getActionButtonIcon(ResourceDirectory.TAK_ASSETS_PNG, "Lasso-Select_32x32.png");
        private final JPopupMenu popupMenu = new JPopupMenu();

        public MultiSelectorPopupMenuProvider()
        {
            // freehand added above as the base button, only these shapes are in the dropdown
            popupMenu.add(new ButtonAction("Rectangle", RECTANGLE_ICON,
                    "Select map features using a rectangle selection tool",
                    e -> multiSelectorService.arm(ShapeType.RECTANGLE)));
            popupMenu.add(new ButtonAction("Polygon", POLYGON_ICON,
                    "Select map features using a polygon selection tool",
                    e -> multiSelectorService.arm(ShapeType.POLYGON)));
            popupMenu.add(new ButtonAction("Lasso", LASSO_ICON,
                    "Select map features using a lasso selection tool",
                    e -> multiSelectorService.arm(ShapeType.FREEHAND_POLYGON)));
        }

        @Override
        public JPopupMenu getPopupMenu()
        {
            return popupMenu;
        }
    }

    /**
     * Provider of the popup menu for the tracks button.
     * @since 5.4
     */
    @PseudoBean
    private static class TracksPopupMenuProvider implements IPopupMenuProvider
    {
        private static final Icon CLEAR_TRACKS_ICON =
                UIResources.getActionButtonIcon(ResourceDirectory.TAK_ASSETS_PNG, "Clear-Tracks_32x32.png");
        private static final Icon NEW_TRACK_ICON =
                UIResources.getActionButtonIcon(ResourceDirectory.TAK_ASSETS_PNG, "New-Track_32x32.png");
        private static final Icon TRACK_SEARCH =
                UIResources.getActionButtonIcon(ResourceDirectory.TAK_ASSETS_PNG, "Track-Search_32x32.png");

        @Inject
        private IExtendedUISpaceManager uiSpaceManager;

        @Inject
        private TracksUiUtils uiUtils;

        private final JPopupMenu popupMenu = new JPopupMenu();

        public TracksPopupMenuProvider()
        {
            popupMenu.add(new ButtonAction("Track Search", TRACK_SEARCH, "Search for tracks",
                    e -> uiSpaceManager.toggleComponent(CoreComponentType.TRACK_SEARCH_PANEL)));
            popupMenu.add(new ButtonAction("New Track", NEW_TRACK_ICON, "Create a new self marker track",
                    e -> uiUtils.handleNewTrackRequest()));
            popupMenu.add(new ButtonAction("Clear Tracks", CLEAR_TRACKS_ICON, "Clear all tracks from the map",
                    e -> uiUtils.handleClearRequest()));
        }

        @Override
        public JPopupMenu getPopupMenu()
        {
            return popupMenu;
        }
    }
}
