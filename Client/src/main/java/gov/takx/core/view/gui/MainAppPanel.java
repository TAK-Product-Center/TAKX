package gov.takx.core.view.gui;

import com.formdev.flatlaf.FlatClientProperties;
import com.jgoodies.forms.layout.CellConstraints;
import com.jgoodies.forms.layout.FormLayout;
import gov.tak.api.comms.takserver.ITakServerConnection;
import gov.tak.api.comms.takserver.ITakServerConnectionManager;
import gov.takx.api.annotation.InvokeOnEDT;
import gov.takx.api.annotation.ProgramPreference;
import gov.takx.api.annotation.PseudoBean;
import gov.takx.api.event.takserver.TakServerConnectionEvent;
import gov.takx.api.platform.IProgressMonitor;
import gov.takx.api.platform.IRaptorLifecycleListener;
import gov.takx.api.toolbar.ButtonType;
import gov.takx.api.ui.IResourceCleaner;
import gov.takx.core.explorer.IExplorerManager;
import gov.takx.core.playback.ui.PlaybackView;
import gov.takx.core.systemmessage.SystemMessageNotifierPanel;
import gov.takx.core.systemmessage.SystemMessagesView;
import gov.takx.core.toolbar.ToolbarConfigurationManager;
import gov.takx.core.view.gui.apps.AppsPanel;
import gov.takx.core.view.gui.bloodhound.BloodhoundPanel;
import gov.takx.core.view.gui.breadcrumb.TrackSearchPanelController;
import gov.takx.core.view.gui.breadcrumb.TracksPanelController;
import gov.takx.core.view.gui.chat.ChatsPanelController;
import gov.takx.core.view.gui.connections.ConnectionsPanel;
import gov.takx.core.view.gui.contacts.ContactsPanel;
import gov.takx.core.view.gui.docking.DockingManager;
import gov.takx.core.view.gui.drifter.DrifterPanel;
import gov.takx.core.view.gui.emerency.EmergencyAlertPanel;
import gov.takx.core.view.gui.eventlog.EventLogPanel;
import gov.takx.core.view.gui.files.FilesPanel;
import gov.takx.core.view.gui.gazetteer.ContextSearchPanel;
import gov.takx.core.view.gui.layers.viewmanager.LayerViewManager;
import gov.takx.core.view.gui.maptools.ElevationProfilePanel;
import gov.takx.core.view.gui.maptools.elevationtools.ElevationToolsPanel;
import gov.takx.core.view.gui.media.library.MediaLibraryPanel;
import gov.takx.core.view.gui.milsym.MilSymRootPaneWrapper;
import gov.takx.core.view.gui.notification.NotificationsPanelController;
import gov.takx.core.view.gui.pointdropper.PointDropperPanel;
import gov.takx.core.view.gui.preferences.PreferencePagesController;
import gov.takx.core.view.gui.propertiespanel.PropertiesPanel;
import gov.takx.core.view.gui.routes.RoutesManager;
import gov.takx.core.view.gui.routes.RoutesPanel;
import gov.takx.core.view.gui.takserver.DataFeedListPanel;
import gov.takx.core.view.gui.takserver.DataPackagePanel;
import gov.takx.core.view.gui.takserver.TakServerPanel;
import gov.takx.platform.preferences.PreferenceEntry;
import gov.takx.platform.ui.ComponentDescriptor;
import gov.takx.platform.ui.UIResources;
import jakarta.annotation.Nonnull;
import jakarta.annotation.PostConstruct;
import jakarta.enterprise.event.Observes;
import jakarta.inject.Inject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.BoxLayout;
import javax.swing.Icon;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.SwingConstants;
import javax.swing.border.TitledBorder;

import java.awt.BorderLayout;
import java.lang.invoke.MethodHandles;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.function.Supplier;

import static gov.takx.platform.ui.ResourceDirectory.PLATFORM_ICONS;

/**
 */
@PseudoBean
public class MainAppPanel extends JPanel
{
    private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    private static final Icon CONNECTED_ICON = UIResources.getActionButtonIcon(PLATFORM_ICONS, "cloud_connected.png");
    private static final Icon DISCONNECTED_ICON = UIResources.getActionButtonIcon(PLATFORM_ICONS, "cloud_disconnected.png");

    @Inject
    private LayerViewManager layerViewManager;

    @Inject
    private IExplorerManager explorerManager;

    @Inject
    private IExtendedUISpaceManager uiSpaceManager;

    @Inject
    private DockingManager dockingManager;

    @Inject
    private ITakServerConnectionManager takServerConnectionManager;

    @Inject
    private ToolbarConfigurationManager toolbarConfigurationManager;

    @Inject
    private RoutesManager routesManager;

    @Inject
    private ChatsPanelController chatsPanelController;

    @Inject
    private NotificationsPanelController notificationsPanelController;

    @Inject
    private TracksPanelController tracksPanelController;

    @Inject
    private TrackSearchPanelController trackSearchPanelController;

    @Inject
    private PreferencePagesController preferencePagesController;

    @Inject
    @ProgramPreference(PreferenceEntry.SIMPLIFIED_USER_INTERFACE)
    private boolean simplifiedUI;

    @Inject
    @ProgramPreference(PreferenceEntry.ENABLE_EVENT_LOG)
    private boolean eventLogEnabled;

    private final List<IRaptorLifecycleListener> statefulComponents = new ArrayList<>();  // Components that have state to manage
    private final List<IResourceCleaner> cleanableComponents = new ArrayList<>();  // Components that have resources

    private CoordinateSystemButton coordinateSystemButton;
    private AudioAlertButton audioAlertButton;

    private NetworkStatusPanel networkStatusPanel;
    private TimePanel timePanel;
    private StatusPanelProgressBar statusPanelProgressBar;
    private SystemMessageNotifierPanel systemMessageNotifierPanel;

    private ConnectionsPanel connectionsPanel;
    private RoutesPanel routesPanel;

    public MainAppPanel()
    {
        initComponents();
    }

    @PostConstruct
    private void postConstruct()
    {
        initCustomComponents();
    }

    void initCustomComponents()
    {
        audioAlertButton = new AudioAlertButton();
        coordinateSystemButton = new CoordinateSystemButton();
        statusPanelProgressBar = new StatusPanelProgressBar();
        networkStatusPanel = new NetworkStatusPanel();
        timePanel = new TimePanel();
        connectionsPanel = new ConnectionsPanel();

        systemMessageNotifierPanel = new SystemMessageNotifierPanel();
        systemMessageNotifierPanel.setVisible(PreferenceEntry.SYSTEM_MESSAGE_COUNT_VISIBLE.getBooleanValue());
        PreferenceEntry.SYSTEM_MESSAGE_COUNT_VISIBLE.newBooleanMonitor(systemMessageNotifierPanel::setVisible);

        routesPanel = new RoutesPanel();
        routesManager.setRoutesPanel(routesPanel);

        takServerButton.setIcon(DISCONNECTED_ICON);

        // Make the TAK server button appear flat. This is achieved in FlatLAF by making it borderless
        takServerButton.putClientProperty(FlatClientProperties.BUTTON_TYPE, FlatClientProperties.BUTTON_TYPE_BORDERLESS);
        takServerButton.addActionListener(e -> uiSpaceManager.toggleComponent(CoreComponentType.TAK_SERVER_PANEL));

        JPanel rightBorderPanel = new JPanel();
        rightBorderPanel.setLayout(new BoxLayout(rightBorderPanel, BoxLayout.Y_AXIS));

        add(rightBorderPanel, BorderLayout.EAST);

        validate();
        repaint();
    }

    void clearState(IProgressMonitor progressMonitor)
    {
        // Clear state for all the components we manage
        cleanableComponents.forEach(IResourceCleaner::releaseResources);
        cleanableComponents.clear();

        statefulComponents.forEach(component -> component.onClearState(progressMonitor));
        statefulComponents.clear();

        networkStatusPanel.stopTimers();
        infoPanel.remove(networkStatusPanel);

        timePanel.stopClockTimer();
        infoPanel.remove(timePanel);

        final JPanel dockingContentPanel = dockingManager.getDockingContentPanel();
        if (dockingContentPanel != null) globeAndBottomPanel.remove(dockingContentPanel);
    }

    /**
     * The docking system is starting, create and add all of our core components so when the layout is loaded
     * it has the components to work with.
     *
     * @since 5.2
     */
    @InvokeOnEDT
    void onDockingStarted()
    {
        logger.debug("onDockingStarted");

        globeAndBottomPanel.add(dockingManager.getDockingContentPanel());

        dockingManager.addPrimaryGlobe(); // Display the initial globe

        if (simplifiedUI)
        {
            configureInfoPanel(coordinateSystemButton, timePanel);
        } else
        {
            addCoreComponents();

            configureInfoPanel(audioAlertButton,
                    coordinateSystemButton,
                    statusPanelProgressBar,
                    systemMessageNotifierPanel,
                    networkStatusPanel,
                    timePanel,
                    takServerButton);
        }

        revalidate();
        repaint();
    }

    /**
     * Load the state of all the stateful components.
     * @since 5.2
     */
    void loadState(IProgressMonitor progressMonitor)
    {
        // Loading the layout here fixes problems where plugin provided panels are showing when they shouldn't.
        // Doing this here will cause the underlying dockables to be shifted around, but our docking manger
        // will accommodate by only showing the UI once everything is in its final place.
        dockingManager.loadLayout();

        // Load state for all the components we manage
        for (IRaptorLifecycleListener ro : statefulComponents) ro.onLoadState(progressMonitor);

        networkStatusPanel.startTimers();
        timePanel.startClockTimer();
    }

    /**
     * Adds the component panels provided by the TAKX core to the user interface.
     * @since 3.4
     */
    private void addCoreComponents()
    {
        addComponent(CoreComponentType.EMERGENCY_ALERT_PANEL, EmergencyAlertPanel::new);
        addComponent(CoreComponentType.PREFERENCES_PANEL, preferencePagesController::getPreferencesPage);
        addComponent(CoreComponentType.APPS_PANEL, AppsPanel::new);
        addComponent(CoreComponentType.OVERLAYS_PANEL, () -> explorerManager.getViewComponent());
        addComponent(CoreComponentType.CITY_SEARCH_PANEL, ContextSearchPanel::new);
        addComponent(CoreComponentType.DEVICES_PANEL, DevicesPanel::new);
        addComponent(CoreComponentType.MEDIA_PANEL, MediaLibraryPanel::new);
        addComponent(CoreComponentType.MAPS_PANEL, () -> layerViewManager.getPanel());
        addComponent(CoreComponentType.CONNECTIONS_PANEL, () -> connectionsPanel);
        addComponent(CoreComponentType.FILES_PANEL, FilesPanel::new);

        if (eventLogEnabled) addComponent(CoreComponentType.EVENT_LOG_PANEL, EventLogPanel::getInstance);
        addComponent(CoreComponentType.PROPERTIES_PANEL, PropertiesPanel::getInstance);
        addComponent(CoreComponentType.CHAT_PANEL, () -> chatsPanelController.getPanel());
        addComponent(CoreComponentType.CONTACTS_PANEL, ContactsPanel::new);
        addComponent(CoreComponentType.TAK_SERVER_PANEL, TakServerPanel::new);
        addComponent(CoreComponentType.DATA_PACKAGE_PANEL, DataPackagePanel::new);
        addComponent(CoreComponentType.DATA_SYNC_PANEL, DataFeedListPanel::getInstance);
        addComponent(CoreComponentType.TAC_GRAPHICS_PANEL, MilSymRootPaneWrapper::new);
        addComponent(CoreComponentType.POINT_DROPPER_PANEL, PointDropperPanel::new);
        addComponent(CoreComponentType.ROUTES_PANEL, () -> routesPanel);
        addComponent(CoreComponentType.DRIFTER_PANEL, DrifterPanel::new);
        addComponent(CoreComponentType.BLOODHOUND_PANEL, BloodhoundPanel::new);
        addComponent(CoreComponentType.ELEVATION_TOOLS, ElevationToolsPanel::new);

        addComponent(CoreComponentType.NOTIFICATIONS_PANEL, () -> notificationsPanelController.getPanel());
        addComponent(CoreComponentType.TRACKS_PANEL, () -> tracksPanelController.getPanel());
        addComponent(CoreComponentType.TRACK_SEARCH_PANEL, () -> trackSearchPanelController.getPanel());
        addComponent(CoreComponentType.SYSTEM_MESSAGES_PANEL, SystemMessagesView::new);
        addComponent(CoreComponentType.PLAYBACK_PANEL, PlaybackView::new);
        addComponent(CoreComponentType.ELEVATION_PROFILER_PANEL, ElevationProfilePanel::new);
    }

    private void configureInfoPanel(@Nonnull JComponent... components)
    {
        final CellConstraints cc = new CellConstraints();
        for (int panelIdx = 0; panelIdx < components.length; panelIdx++)
        {
            infoPanel.add(components[panelIdx], cc.xy(1 + panelIdx * 2, 1));
        }
    }

    /**
     * On any connection event, update our status.  If we have 1 or more connected connections, then set the icon
     * to CONNECTED, else DISCONNECTED.
     *
     * @since 3.1
     */
    private void onTakServerConnectionEvent(@Observes TakServerConnectionEvent ignore)
    {
        final boolean connected = takServerConnectionManager.getTakServerConnections().stream()
                .anyMatch(ITakServerConnection::isConnected);

        takServerButton.setIcon(connected ? CONNECTED_ICON : DISCONNECTED_ICON);
    }

    void shutdown(IProgressMonitor progressMonitor)
    {
        // Shutdown all the components we manage
        for (IRaptorLifecycleListener ro : statefulComponents)
        {
            ro.onShutdown(progressMonitor);
        }
    }

    /**
     * Safely add a component (panel) to the user interface. This method traps exceptions that occur during the creation
     * of the panel and reports them, preventing the UI from being essentially hosed due to an exception in a panel
     * constructor.
     * <p/>
     * If the component is a stateful component, add it to the list.
     *
     * @param coreComponentType The type of the core UI component
     * @param panelSupplier Supplier to produce the component
     * @since 3.4
     */
    private void addComponent(@Nonnull CoreComponentType coreComponentType, @Nonnull Supplier<ATitledPanel> panelSupplier)
    {
        long constructorTime = 0;
        long uiUpdateTime = 0;
        final long start = System.currentTimeMillis();

        try
        {
            final ATitledPanel component = panelSupplier.get();
            constructorTime = System.currentTimeMillis() - start;

            final long start2 = System.currentTimeMillis();
            final ComponentDescriptor componentDescriptor = coreComponentType.getComponentDescriptor();
            final ButtonType buttonType = coreComponentType.getButtonType();

            // The toolbar is created before UI components are set up, so the button already exists and needs to be
            // associated with the component descriptor.
            final UUID singularId = buttonType.getSingularId();
            if (singularId != null)
            {
                toolbarConfigurationManager.associateButtonWithComponentDescriptor(singularId, componentDescriptor.getId());
            } else
            {
                logger.warn("Button type {} is not singular and cannot be associated with this core UI component: {}", buttonType, component);
            }

            uiSpaceManager.addComponent(componentDescriptor, component);

            cleanableComponents.add(component);
            if (component instanceof IRaptorLifecycleListener lifecycleListener) statefulComponents.add(lifecycleListener);

            uiUpdateTime = System.currentTimeMillis() - start2;
        } catch (Exception e)
        {
            logger.error("Failed to create component for {}", coreComponentType, e);
        }

        if (logger.isDebugEnabled())
        {
            final String msg = String.format("%22s => %4d : ctor %3d ui %3d", coreComponentType, System.currentTimeMillis() - start, constructorTime, uiUpdateTime);
            logger.debug("{}", msg);
        }
    }

    private void initComponents()
    {
        // JFormDesigner - Component initialization - DO NOT MODIFY  //GEN-BEGIN:initComponents
        var statusPanel = new JPanel();
        var commsActivityPanel = new CommsActivityPanel();
        infoPanel = new JPanel();
        globeAndBottomPanel = new JPanel();
        takServerButton = new JButton();
        CellConstraints cc = new CellConstraints();

        //======== this ========
        setName("this");
        setLayout(new BorderLayout());

        //======== statusPanel ========
        {
            statusPanel.setRequestFocusEnabled(false);
            statusPanel.setFocusable(false);
            statusPanel.setBorder(new TitledBorder(""));
            statusPanel.setName("statusPanel");
            statusPanel.setLayout(new FormLayout(
                    "2*(default, $lcgap), default:grow",
                    "[26px,default]:grow"));

            //---- commsActivityPanel ----
            commsActivityPanel.setName("commsActivityPanel");
            statusPanel.add(commsActivityPanel, cc.xy(1, 1, CellConstraints.DEFAULT, CellConstraints.CENTER));

            //======== infoPanel ========
            {
                infoPanel.setName("infoPanel");
                infoPanel.setLayout(new FormLayout(
                        "2*(default, $lcgap), default:grow, 4*($lcgap, default)",
                        "default"));
            }
            statusPanel.add(infoPanel, cc.xy(5, 1));
        }
        add(statusPanel, BorderLayout.PAGE_END);

        //======== globeAndBottomPanel ========
        {
            globeAndBottomPanel.setName("globeAndBottomPanel");
            globeAndBottomPanel.setLayout(new BorderLayout());
        }
        add(globeAndBottomPanel, BorderLayout.CENTER);

        //---- takServerButton ----
        takServerButton.setText("TAK Network Status");
        takServerButton.setFocusable(false);
        takServerButton.setFocusPainted(false);
        takServerButton.setHorizontalTextPosition(SwingConstants.LEADING);
        takServerButton.setIconTextGap(2);
        takServerButton.putClientProperty("FlatLaf.styleClass", "medium");
        takServerButton.setName("takServerButton");
        // JFormDesigner - End of component initialization  //GEN-END:initComponents
    }

    // JFormDesigner - Variables declaration - DO NOT MODIFY  //GEN-BEGIN:variables
    private JPanel infoPanel;
    private JPanel globeAndBottomPanel;
    private JButton takServerButton;
    // JFormDesigner - End of variables declaration  //GEN-END:variables
}
