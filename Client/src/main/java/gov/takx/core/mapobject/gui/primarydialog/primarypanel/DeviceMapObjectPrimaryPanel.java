package gov.takx.core.mapobject.gui.primarydialog.primarypanel;

import com.formdev.flatlaf.FlatClientProperties;
import com.jgoodies.forms.factories.Borders;
import com.jgoodies.forms.layout.CellConstraints;
import com.jgoodies.forms.layout.FormLayout;
import gov.takx.api.annotation.InvokeOnEDT;
import gov.takx.api.annotation.PseudoBean;
import gov.takx.api.mapobject.ICoreMapObjectManager;
import gov.takx.api.mapobject.IDataMonitor;
import gov.takx.api.mapobject.IIconMapObject;
import gov.takx.api.mapobject.IMapObject;
import gov.takx.api.mapobject.IMapObjectController;
import gov.takx.api.plugin.IRaptorPanel;
import gov.takx.api.plugin.datamonitor.DataMonitorEvent;
import gov.takx.api.plugin.datamonitor.DataMonitorEventType;
import gov.takx.api.timer.ITimerService;
import gov.takx.api.ui.DevicePanelMetadataKeys;
import gov.takx.api.ui.IDeviceSwingUIProvider;
import gov.takx.api.ui.IResourceCleaner;
import gov.takx.commons.swing.ImageUtils;
import gov.takx.core.mapobject.ICoreAudioManager;
import gov.takx.core.mapobject.gui.DataMonitorsPanel;
import gov.takx.platform.ui.UIResources;
import jakarta.annotation.PostConstruct;
import jakarta.inject.Inject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.DefaultListCellRenderer;
import javax.swing.DefaultListModel;
import javax.swing.Icon;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSeparator;
import javax.swing.JToolTip;
import javax.swing.Popup;
import javax.swing.PopupFactory;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Point;
import java.awt.Window;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.beans.PropertyChangeEvent;
import java.lang.invoke.MethodHandles;
import java.util.Arrays;
import java.util.List;

import static gov.takx.platform.ui.ResourceDirectory.PLATFORM_ICONS;

/**
 */
@SuppressWarnings("CdiManagedBeanInconsistencyInspection")
@PseudoBean
public class DeviceMapObjectPrimaryPanel extends AMapObjectPanel
{
    private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    private static final List<String> propertyList = Arrays.asList(
            IIconMapObject.CONTAINED_ADDED,
            IIconMapObject.CONTAINED_REMOVED,
            IIconMapObject.ALLOW_DEVICE_CONTAINMENT);

    private static final Icon SOUND_ICON = UIResources.getStandardSizedIcon(PLATFORM_ICONS, "volume.png");
    private static final Icon MUTE_ICON = UIResources.getStandardSizedIcon(PLATFORM_ICONS, "volume_off.png");

    private static final CellConstraints CELL_CONSTRAINTS = new CellConstraints();

    private JPanel pluginPanel;
    private DataMonitorsPanel dataMonitorsPanel;

    @Inject
    private ITimerService timerService;

    @Inject
    private ICoreMapObjectManager mapObjectManager;

    @Inject
    private ICoreAudioManager audioManager;

    public DeviceMapObjectPrimaryPanel(IMapObjectController controller)
    {
        this(controller, false);
    }

    public DeviceMapObjectPrimaryPanel(IMapObjectController controller, boolean forDialog)
    {
        super(controller);

        initComponents();

        cancelButton.setVisible(forDialog);

        // We can't add this listener until AFTER the UI components have been instantiated (above)
        // because the property change listener fires, trying to update via setLocation and the location
        // label didn't yet exist.
        addProperties(propertyList);

        clearAlarmsButton.putClientProperty(FlatClientProperties.MINIMUM_WIDTH, 0);

        // For contained MapObjects
        containerList.setCellRenderer(new ContainerListCellRenderer());
        containerList.addMouseListener(new ContainerListMouseListener());
        containerList.addListSelectionListener(
                e -> removeButton.setEnabled(!containerList.getSelectionModel().isSelectionEmpty()));

        removeButton.putClientProperty(FlatClientProperties.MINIMUM_WIDTH, 0);
    }

    @PostConstruct
    private void postConstruct()
    {
        final IDeviceSwingUIProvider deviceUiProvider = swingUIBinder.forDevice(mapObjectController);

        // Check to see if the developer has properly implemented the IRaptorPanel to return a JPanel.
        // If not, check to see if the primary panel is the JPanel. We used to assume that the primary
        // panel was a JPanel and have coded our sample plugins that way, but that is not a valid assumption
        // given our API contract. (CORE-3174)
        IRaptorPanel primaryPanel = deviceUiProvider.getPrimaryPanel();

        if (primaryPanel == null)
        {
            pluginPanel = null;
        } else
        {
            final JPanel panelFromPanel = primaryPanel.getPanel();

            pluginPanel = panelFromPanel == null && primaryPanel instanceof JPanel
                    ? (JPanel) primaryPanel
                    : panelFromPanel;
        }

        // Add the plugins' custom panel, if provided
        if (pluginPanel != null)
        {
            panelCustomAndContained.add(pluginPanel, CELL_CONSTRAINTS.xy(1, 1));
        }

        updateForContainment(); // Add containment panel, if the device can contain other MOs

        dataMonitorsPanel = new DataMonitorsPanel(mapObjectController);

        mapObjectController.addPropertyChangeListener(this);

        // Add data monitors to UI (if necessary)
        if (!mapObjectController.getDataMonitors().isEmpty()
                && mapObject.getMetaBoolean(DevicePanelMetadataKeys.SHOW_DATA_MONITORS, true))
        {
            panelStatusHolder.add(statusPanel, CELL_CONSTRAINTS.xy(1, 1));
            panelDataMonitorsPanelHolder.add(dataMonitorsPanel, CELL_CONSTRAINTS.xy(1, 1));
        }

        final boolean isDevice = mapObject.isDevice();

        // Optionally enable help button.
        final JPanel helpPanel = deviceUiProvider.getPluginHelpPanel();
        final boolean enableHelp = isDevice && helpPanel != null;

        // Since we just created it to see if it was non-null, we need to release resources on it
        if (helpPanel instanceof IResourceCleaner resourceCleaner) resourceCleaner.releaseResources();

        helpButton.setEnabled(enableHelp);
        helpButton.setVisible(enableHelp);

        setSpeakerIcon();
    }

    /**
     * Display map object configuration dialog with any custom panels.
     */
    private void configureButtonActionPerformed()
    {
        mapObjectController.showConfigurationDialog();
    }

    private void helpButtonActionPerformed()
    {
        mapObjectController.showHelpDialog();
    }

    private void cancelButtonActionPerformed()
    {
        requestClose();
    }

    /**
     * Clear any alarming alarms in the alarms panel.
     */
    private void clearAlarmsButtonActionPerformed()
    {
        for (IDataMonitor dataMonitor : mapObjectController.getDataMonitors())
        {
            dataMonitor.onDataMonitorEvent(new DataMonitorEvent(dataMonitor, DataMonitorEventType.AlarmClear));
        }

        // Audio Manager is a black box- if there were any audio alarms associated with
        // the data monitors, let the audio manager know that they have been cleared.
        audioManager.clearAudioAlarms(mapObjectController);
    }

    /**
     * Mute any alarming alarms in the alarm panel.
     */
    private void buttonMuteActionPerformed()
    {
        boolean enableAudio = !mapObjectController.isAudioAlarmPlaybackEnabled();
        logger.debug("Toggling alarm state: {}", enableAudio);
        mapObjectController.setAudioAlarmPlayback(enableAudio);

        setSpeakerIcon();

        // If the user was trying to mute all monitors but the current
        // state is still un-muted, it is because 1 or more data monitors
        // is un-mutable. Notify the user.
        if (!enableAudio && mapObjectController.isAudioAlarmPlaybackEnabled())
        {
            showImmutableDataMonitorsPopup();
        }
    }

    /**
     * Show a popup notifying the user that one or more data monitors could not be muted,
     * thus prevented a global mute. Popup will be hidden after two seconds.
     *
     * @since 1.14
     */
    private void showImmutableDataMonitorsPopup()
    {
        JToolTip tooltip = new JToolTip();
        tooltip.setTipText("<html>1 or more data monitors<br>are not mutable.");
        Point location = buttonMute.getLocationOnScreen();
        Dimension buttonsSize = buttonMute.getSize();
        final Popup popup = PopupFactory.getSharedInstance().getPopup(buttonMute, tooltip, location.x + buttonsSize.width, location.y + buttonsSize.height);

        popup.show();

        timerService.createTask(popup::hide, 2_000);
    }

    /**
     * Remove the currently selected contained map object from containment within this map object.
     */
    private void removeButtonActionPerformed()
    {
        for (IIconMapObject mapObject : containerList.getSelectedValuesList())
        {
            removeChild(mapObject);
            mapObjectManager.removeFromContainer(mapObject);
        }
    }

    /**
     * Add a child map object.
     *
     * @param containedMapObject child to add
     */
    private void addChild(final IIconMapObject containedMapObject)
    {
        getContainerListModel().addElement(containedMapObject);
    }

    /**
     * Remove a child map object.
     *
     * @param containable child to remove
     */
    private void removeChild(IIconMapObject containable)
    {
        getContainerListModel().removeElement(containable);
    }

    /**
     * @return List model from {@code containerList} properly typed as a {@code DefaultListModel}.
     * @since 2.37
     */
    private DefaultListModel<IIconMapObject> getContainerListModel()
    {
        return (DefaultListModel<IIconMapObject>) containerList.getModel();
    }

    /**
     * Update our UI to show or hide the contained MOs panel.  If the contained MOs panel should be visible,
     * its model is recreated with the current list of contained MOs.
     *
     * @since 2.37
     */
    private void updateForContainment()
    {
        if (mapObject.isAllowDeviceToBeContainer() && mapObject instanceof IIconMapObject iconMapObject)
        {
            // Create a new model with all the children currently contained by the map object
            containerList.setModel(new DefaultListModel<>());
            iconMapObject.getContainedMapObjects().forEach(this::addChild);

            panelCustomAndContained.add(panelContainedMapObjects, CELL_CONSTRAINTS.xy(1, 2));
        } else
        {
            panelCustomAndContained.remove(panelContainedMapObjects);
        }
    }

    /**
     * Sets the speaker icon.
     */
    private void setSpeakerIcon()
    {
        buttonMute.setIcon(mapObjectController.isAudioAlarmPlaybackEnabled() ? SOUND_ICON : MUTE_ICON);
    }

    @InvokeOnEDT
    @Override
    public void propertyChange(final PropertyChangeEvent evt)
    {
        final String propertyName = evt.getPropertyName();

        if (propertyName.equals(IIconMapObject.CONTAINED_ADDED))
        {
            addChild((IIconMapObject) evt.getNewValue());
        } else if (propertyName.equals(IIconMapObject.CONTAINED_REMOVED))
        {
            removeChild((IIconMapObject) evt.getNewValue());
        } else if (propertyName.equals(IIconMapObject.ALLOW_DEVICE_CONTAINMENT))
        {
            updateForContainment();

            // Force the window containing this panel (if any) to update its layout
            // as well; this is necessary especially when the panel gets bigger, so that
            // it doesn't end up cut off at the bottom.
            Window window = SwingUtilities.windowForComponent(this);
            if (window != null)
            {
                window.pack();
            }
        } else if (propertyName.equalsIgnoreCase(IDataMonitor.MUTE))
        {
            setSpeakerIcon();
        }
    }

    @Override
    public void releaseResources()
    {
        mapObjectController.removePropertyChangeListener(this);
        dataMonitorsPanel.releaseResources();

        if (pluginPanel instanceof IResourceCleaner resourceCleaner) resourceCleaner.releaseResources();

        super.releaseResources();
    }

    private static class ContainerListCellRenderer extends DefaultListCellRenderer
    {
        @Override
        public Component getListCellRendererComponent(JList list, Object value, int index, boolean isSelected, boolean cellHasFocus)
        {
            JLabel label = (JLabel) super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);

            IMapObject mapObject = (IMapObject) value;
            label.setIcon(mapObject.getSizedIcon(ImageUtils.SMALL_ICON_WIDTH));
            label.setText(mapObject.getName());
            return label;
        }
    }

    private class ContainerListMouseListener extends MouseAdapter
    {
        @Override
        public void mouseClicked(MouseEvent e)
        {
            if ((e.getClickCount() > 1) && (e.getButton() == MouseEvent.BUTTON1))
            {
                IMapObject mapObject = containerList.getSelectedValue();
                if (mapObject != null) mapObject.getController().showPluginDialog();
            }
        }
    }

    private void initComponents()
    {
        // JFormDesigner - Component initialization - DO NOT MODIFY  //GEN-BEGIN:initComponents
        var panel5 = new JPanel();
        panelStatusHolder = new JPanel();
        var panel6 = new JPanel();
        panelCustomAndContained = new JPanel();
        var buttonPanelPrimaryButtons = new JPanel();
        var configureButton = new JButton();
        helpButton = new JButton();
        cancelButton = new JButton();
        panelContainedMapObjects = new JPanel();
        var label1 = new JLabel();
        var containerListScrollPane = new JScrollPane();
        containerList = new JList<>();
        removeButton = new JButton();
        statusPanel = new JPanel();
        var statusPanel2 = new JPanel();
        var label5 = new JLabel();
        var panelDataMonitorButtons = new JPanel();
        clearAlarmsButton = new JButton();
        buttonMute = new JButton();
        panelDataMonitorsPanelHolder = new JPanel();
        var statusSeparatorRight = new JSeparator();
        CellConstraints cc = new CellConstraints();

        //======== this ========
        setName("this");
        setLayout(new BorderLayout());

        //======== panel5 ========
        {
            panel5.setName("panel5");
            panel5.setLayout(new FormLayout(
                    "default, $lcgap, default:grow",
                    "fill:default:grow"));

            //======== panelStatusHolder ========
            {
                panelStatusHolder.setName("panelStatusHolder");
                panelStatusHolder.setLayout(new FormLayout(
                        "default:grow",
                        "fill:default:grow"));
            }
            panel5.add(panelStatusHolder, cc.xy(1, 1));

            //======== panel6 ========
            {
                panel6.setName("panel6");
                panel6.setLayout(new FormLayout(
                        "default:grow",
                        "fill:default"));

                //======== panelCustomAndContained ========
                {
                    panelCustomAndContained.setName("panelCustomAndContained");
                    panelCustomAndContained.setLayout(new FormLayout(
                            "default:grow",
                            "2*(fill:default)"));
                }
                panel6.add(panelCustomAndContained, cc.xy(1, 1));
            }
            panel5.add(panel6, cc.xy(3, 1));
        }
        add(panel5, BorderLayout.CENTER);

        //======== buttonPanelPrimaryButtons ========
        {
            buttonPanelPrimaryButtons.setBorder(Borders.BUTTON_BAR_GAP_BORDER);
            buttonPanelPrimaryButtons.setName("buttonPanelPrimaryButtons");
            buttonPanelPrimaryButtons.setLayout(new FormLayout(
                    "$button, $lcgap, $button, $glue, $button, $lcgap, $button",
                    "default:grow"));

            //---- configureButton ----
            configureButton.setText("Configure");
            configureButton.setName("configureButton");
            configureButton.addActionListener(e -> configureButtonActionPerformed());
            buttonPanelPrimaryButtons.add(configureButton, cc.xy(1, 1));

            //---- helpButton ----
            helpButton.setText("Help");
            helpButton.setName("helpButton");
            helpButton.addActionListener(e -> helpButtonActionPerformed());
            buttonPanelPrimaryButtons.add(helpButton, cc.xy(5, 1));

            //---- cancelButton ----
            cancelButton.setText("Close");
            cancelButton.setVisible(false);
            cancelButton.setName("cancelButton");
            cancelButton.addActionListener(e -> cancelButtonActionPerformed());
            buttonPanelPrimaryButtons.add(cancelButton, cc.xy(7, 1));
        }
        add(buttonPanelPrimaryButtons, BorderLayout.SOUTH);

        //======== panelContainedMapObjects ========
        {
            panelContainedMapObjects.setName("panelContainedMapObjects");
            panelContainedMapObjects.setLayout(new FormLayout(
                    "default:grow, $lcgap, default",
                    "default, $lgap, fill:default:grow"));

            //---- label1 ----
            label1.setText("Contained Map Objects");
            label1.setName("label1");
            panelContainedMapObjects.add(label1, cc.xy(1, 1));

            //======== containerListScrollPane ========
            {
                containerListScrollPane.setVerifyInputWhenFocusTarget(false);
                containerListScrollPane.setName("containerListScrollPane");

                //---- containerList ----
                containerList.setVisibleRowCount(-1);
                containerList.setLayoutOrientation(JList.HORIZONTAL_WRAP);
                containerList.setName("containerList");
                containerListScrollPane.setViewportView(containerList);
            }
            panelContainedMapObjects.add(containerListScrollPane, cc.xy(1, 3));

            //---- removeButton ----
            removeButton.setText("Remove");
            removeButton.setEnabled(false);
            removeButton.setToolTipText("Remove Contained Map Object");
            removeButton.setName("removeButton");
            removeButton.addActionListener(e -> removeButtonActionPerformed());
            panelContainedMapObjects.add(removeButton, cc.xy(3, 3, CellConstraints.DEFAULT, CellConstraints.CENTER));
        }

        //======== statusPanel ========
        {
            statusPanel.setName("statusPanel");
            statusPanel.setLayout(new FormLayout(
                    "default, $lcgap, default",
                    "fill:default:grow"));

            //======== statusPanel2 ========
            {
                statusPanel2.setName("statusPanel2");
                statusPanel2.setLayout(new FormLayout(
                        "default",
                        "default, $lgap, fill:default, $lgap, fill:default:grow"));

                //---- label5 ----
                label5.setText("Status");
                label5.putClientProperty("FlatLaf.styleClass", "large");
                label5.setName("label5");
                statusPanel2.add(label5, cc.xy(1, 1, CellConstraints.CENTER, CellConstraints.DEFAULT));

                //======== panelDataMonitorButtons ========
                {
                    panelDataMonitorButtons.setName("panelDataMonitorButtons");
                    panelDataMonitorButtons.setLayout(new FormLayout(
                            "center:default, $lcgap, center:default",
                            "default"));

                    //---- clearAlarmsButton ----
                    clearAlarmsButton.setText("Clear");
                    clearAlarmsButton.setToolTipText("Clear All Alarms");
                    clearAlarmsButton.putClientProperty("FlatLaf.styleClass", "mini");
                    clearAlarmsButton.setName("clearAlarmsButton");
                    clearAlarmsButton.addActionListener(e -> clearAlarmsButtonActionPerformed());
                    panelDataMonitorButtons.add(clearAlarmsButton, cc.xy(1, 1, CellConstraints.CENTER, CellConstraints.DEFAULT));

                    //---- buttonMute ----
                    buttonMute.setToolTipText("Toggle Audible Alerts");
                    buttonMute.setName("buttonMute");
                    buttonMute.addActionListener(e -> buttonMuteActionPerformed());
                    panelDataMonitorButtons.add(buttonMute, cc.xy(3, 1, CellConstraints.FILL, CellConstraints.FILL));
                }
                statusPanel2.add(panelDataMonitorButtons, cc.xy(1, 3));

                //======== panelDataMonitorsPanelHolder ========
                {
                    panelDataMonitorsPanelHolder.setName("panelDataMonitorsPanelHolder");
                    panelDataMonitorsPanelHolder.setLayout(new FormLayout(
                            "default:grow",
                            "fill:default:grow"));
                }
                statusPanel2.add(panelDataMonitorsPanelHolder, cc.xy(1, 5));
            }
            statusPanel.add(statusPanel2, cc.xy(1, 1));

            //---- statusSeparatorRight ----
            statusSeparatorRight.setOrientation(SwingConstants.VERTICAL);
            statusSeparatorRight.setName("statusSeparatorRight");
            statusPanel.add(statusSeparatorRight, cc.xy(3, 1, CellConstraints.CENTER, CellConstraints.DEFAULT));
        }
        // JFormDesigner - End of component initialization  //GEN-END:initComponents
    }

    // JFormDesigner - Variables declaration - DO NOT MODIFY  //GEN-BEGIN:variables
    private JPanel panelStatusHolder;
    private JPanel panelCustomAndContained;
    private JButton helpButton;
    private JButton cancelButton;
    private JPanel panelContainedMapObjects;
    private JList<IIconMapObject> containerList;
    private JButton removeButton;
    private JPanel statusPanel;
    private JButton clearAlarmsButton;
    private JButton buttonMute;
    private JPanel panelDataMonitorsPanelHolder;
    // JFormDesigner - End of variables declaration  //GEN-END:variables
}
