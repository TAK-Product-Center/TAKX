package gov.takx.core.view.gui;

import gov.takx.api.toolbar.ButtonType;
import gov.takx.api.ui.ComponentPosition;
import gov.takx.core.playback.ui.PlaybackView;
import gov.takx.core.systemmessage.SystemMessagesView;
import gov.takx.core.view.gui.apps.AppsPanel;
import gov.takx.core.view.gui.bloodhound.BloodhoundPanel;
import gov.takx.core.view.gui.breadcrumb.TrackSearchPanel;
import gov.takx.core.view.gui.breadcrumb.TracksPanel;
import gov.takx.core.view.gui.chat.ChatsPanel;
import gov.takx.core.view.gui.connections.ConnectionsPanel;
import gov.takx.core.view.gui.contacts.ContactsPanel;
import gov.takx.core.view.gui.drifter.DrifterPanel;
import gov.takx.core.view.gui.emerency.EmergencyAlertPanel;
import gov.takx.core.view.gui.eventlog.EventLogPanel;
import gov.takx.core.view.gui.explorer.view.MainExplorerPanel;
import gov.takx.core.view.gui.files.FilesPanel;
import gov.takx.core.view.gui.gazetteer.ContextSearchPanel;
import gov.takx.core.view.gui.layers.viewmanager.LayerViewPanel;
import gov.takx.core.view.gui.maptools.ElevationProfilePanel;
import gov.takx.core.view.gui.maptools.elevationtools.ElevationToolsPanel;
import gov.takx.core.view.gui.media.library.MediaLibraryPanel;
import gov.takx.core.view.gui.milsym.MilSymRootPanel;
import gov.takx.core.view.gui.multiselector.MultiSelectorPanel;
import gov.takx.core.view.gui.notification.NotificationsPanel;
import gov.takx.core.view.gui.pointdropper.PointDropperPanel;
import gov.takx.core.view.gui.preferences.MainPreferencesPage;
import gov.takx.core.view.gui.propertiespanel.PropertiesPanel;
import gov.takx.core.view.gui.routes.RoutesPanel;
import gov.takx.core.view.gui.takserver.DataFeedListPanel;
import gov.takx.core.view.gui.takserver.DataPackagePanel;
import gov.takx.core.view.gui.takserver.TakServerPanel;
import gov.takx.platform.ui.ComponentDescriptor;
import gov.takx.platform.ui.ComponentDescriptorBuilder;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import java.awt.Component;

import static gov.takx.api.ui.ComponentPosition.BOTTOM;
import static gov.takx.api.ui.ComponentPosition.LEFT;
import static gov.takx.api.ui.ComponentPosition.RIGHT;

/**
 * Enum identifying the UI components provided by the TAKX core.
 *
 * @since 3.4
 */
public enum CoreComponentType
{
    /** @since 5.7 **/
    EMERGENCY_ALERT_PANEL(EmergencyAlertPanel.class, RIGHT, ButtonType.ALERT),
    APPS_PANEL(AppsPanel.class, LEFT, ButtonType.APPS),
    OVERLAYS_PANEL(MainExplorerPanel.class, LEFT, ButtonType.OVERLAYS),
    CITY_SEARCH_PANEL(ContextSearchPanel.class, LEFT, ButtonType.CITY_SEARCH),
    DEVICES_PANEL(DevicesPanel.class, LEFT, ButtonType.DEVICES),
    MEDIA_PANEL(MediaLibraryPanel.class, LEFT, ButtonType.MEDIA),
    MAPS_PANEL(LayerViewPanel.class, LEFT, ButtonType.MAPS),
    CONNECTIONS_PANEL(ConnectionsPanel.class, LEFT, ButtonType.CONNECTIONS),
    FILES_PANEL(FilesPanel.class, LEFT, ButtonType.FILES),
    /** @since 5.4 **/
    PREFERENCES_PANEL(MainPreferencesPage.class, LEFT, ButtonType.SETTINGS, false, false, false, false),
    /** @since 5.6 */
    ELEVATION_PROFILER_PANEL(ElevationProfilePanel.class, BOTTOM, ButtonType.ELEVATION_PROFILER),
    EVENT_LOG_PANEL(EventLogPanel.class, RIGHT, ButtonType.EVENT_LOG),
    PROPERTIES_PANEL(PropertiesPanel.class, RIGHT, ButtonType.PROPERTIES),
    CHAT_PANEL(ChatsPanel.class, RIGHT, ButtonType.CHATS),
    CONTACTS_PANEL(ContactsPanel.class, RIGHT, ButtonType.CONTACTS),
    TAK_SERVER_PANEL(TakServerPanel.class, RIGHT, ButtonType.TAK_SERVER),
    DATA_PACKAGE_PANEL(DataPackagePanel.class, RIGHT, ButtonType.DATA_PACKAGES),
    DATA_SYNC_PANEL(DataFeedListPanel.class, RIGHT, ButtonType.DATA_SYNC),
    TAC_GRAPHICS_PANEL(MilSymRootPanel.class, RIGHT, ButtonType.TAC_GRAPHICS),
    POINT_DROPPER_PANEL(PointDropperPanel.class, RIGHT, ButtonType.POINT_DROPPER, false, false, true, true),
    ROUTES_PANEL(RoutesPanel.class, RIGHT, ButtonType.ROUTES, false, false, true, true),
    MULTISELECTOR_PANEL(MultiSelectorPanel.class, RIGHT, ButtonType.MULTI_SELECTOR, false, false, true, true),
    /** @since 5.2 **/
    NOTIFICATIONS_PANEL(NotificationsPanel.class, RIGHT, ButtonType.NOTIFICATIONS),
    SYSTEM_MESSAGES_PANEL(SystemMessagesView.class, BOTTOM, ButtonType.SYSTEM_MESSAGES),
    PLAYBACK_PANEL(PlaybackView.class, BOTTOM, ButtonType.PLAYBACK, false, true, false, false),
    /** @since 5.3 **/
    TRACKS_PANEL(TracksPanel.class, RIGHT, ButtonType.TRACKS),
    /** @since 5.4 **/
    TRACK_SEARCH_PANEL(TrackSearchPanel.class, RIGHT, ButtonType.TRACK_SEARCH),
    /** @since 5.6 **/
    DRIFTER_PANEL(DrifterPanel.class, RIGHT, ButtonType.DRIFTER),
    /** @since 5.6 **/
    BLOODHOUND_PANEL(BloodhoundPanel.class, RIGHT, ButtonType.BLOODHOUND),
    /** @since 5.7 */
    ELEVATION_TOOLS(ElevationToolsPanel.class, RIGHT, ButtonType.ELEVATION_TOOLS);

    private final ButtonType buttonType;
    private final ComponentDescriptor componentDescriptor;

    /**
     * Constructor for components that desire to use the following default values for the component descriptor:
     * <ul>
     *     <li>parentScrollingEnabled - false</li>
     *     <li>playbackAware - false</li>
     *     <li>pinned - {@code null} (to accept the application default)</li>
     *     <li>hidePin - false</li>
     * </ul>
     */
    CoreComponentType(@Nonnull Class<? extends Component> componentClass, @Nonnull ComponentPosition componentPosition,
                      @Nonnull ButtonType buttonType)
    {
        this(componentClass, componentPosition, buttonType, false, false, null, false);
    }

    /**
     * @param componentClass The class of the component that will be used for the UI panel
     * @param componentPosition The default position of the component in the UI
     * @param buttonType The type of the button to be associated with the component (note: this should be a singular
     *                   button type)
     * @param parentScrollingEnabled Whether scrolling should be enabled for the parent of the component
     * @param playbackAware Whether the component should be highlighted with a green border during playback
     * @param pinned Whether the component should be pinned by default (may be {@code null} to use the default setting)
     * @param hidePin Whether the pin button should be hidden from the user
     */
    CoreComponentType(@Nonnull Class<? extends Component> componentClass, @Nonnull ComponentPosition componentPosition,
                      @Nonnull ButtonType buttonType, boolean parentScrollingEnabled, boolean playbackAware,
                      @Nullable Boolean pinned, boolean hidePin)
    {
        this.buttonType = buttonType;

        componentDescriptor = new ComponentDescriptorBuilder(componentClass, buttonType.requireDisplayText())
                .setIcon(buttonType.getImageUrl())
                .setComponentPosition(componentPosition)
                .setToolbarButtonVisible(false)
                .setParentScrollingEnabled(parentScrollingEnabled)
                .setPlaybackAware(playbackAware)
                .setPinned(pinned)
                .setHidePin(hidePin)
                .setExternalizable(!hidePin)
                .build();
    }

    /**
     * @return The type of the button to be associated with the component.
     */
    @Nonnull
    public ButtonType getButtonType()
    {
        return buttonType;
    }

    /**
     * @return The component descriptor for the core component.
     */
    @Nonnull
    public ComponentDescriptor getComponentDescriptor()
    {
        return componentDescriptor;
    }

    /**
     * @return The id of the component descriptor for the core component.
     */
    @Nonnull
    public String getComponentDescriptorId()
    {
        return componentDescriptor.getId();
    }
}
