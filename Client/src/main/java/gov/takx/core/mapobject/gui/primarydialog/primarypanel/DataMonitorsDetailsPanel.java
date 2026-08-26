package gov.takx.core.mapobject.gui.primarydialog.primarypanel;

import com.formdev.flatlaf.FlatClientProperties;
import com.jgoodies.forms.layout.CellConstraints;
import com.jgoodies.forms.layout.FormLayout;
import gov.takx.api.annotation.InvokeOnEDT;
import gov.takx.api.annotation.PseudoBean;
import gov.takx.api.mapobject.IDataMonitor;
import gov.takx.api.mapobject.IMapObject;
import gov.takx.api.mapobject.IMapObjectController;
import gov.takx.api.plugin.datamonitor.DataMonitorEvent;
import gov.takx.api.plugin.datamonitor.DataMonitorEventType;
import gov.takx.api.timer.ITimerService;
import gov.takx.api.ui.IResourceCleaner;
import gov.takx.commons.lang.ListenerManager;
import gov.takx.core.mapobject.ICoreAudioManager;
import gov.takx.core.mapobject.gui.DataMonitorsPanel;
import gov.takx.platform.ui.UIResources;
import jakarta.annotation.Nonnull;
import jakarta.inject.Inject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.Icon;
import javax.swing.JButton;
import javax.swing.JPanel;
import javax.swing.JToolTip;
import javax.swing.PopupFactory;

import java.lang.invoke.MethodHandles;
import java.util.List;

import static gov.takx.platform.ui.ResourceDirectory.PLATFORM_ICONS;

/**
 * A panel to display data monitor details in the "Properties" panel and "Open" dialog. Relies on {@link DataMonitorsPanel} to show a tree of alarms.
 *
 * @since 5.8 (Pulled out of {@link DeviceMapObjectPrimaryPanel})
 */
@PseudoBean
public class DataMonitorsDetailsPanel extends JPanel implements IResourceCleaner
{
    // TODO DCW TAKX-4510 this entire panel needs a UI/UX overhaul. It has always had weird dimensions and does not resize well. Basically need to
    //  start from scratch and redo the data monitor tree table in DataMonitorsPanel.

    private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    private static final Icon SOUND_ICON = UIResources.getStandardSizedIcon(PLATFORM_ICONS, "volume.png");
    private static final Icon MUTE_ICON = UIResources.getStandardSizedIcon(PLATFORM_ICONS, "volume_off.png");

    private final ListenerManager listenerManager = new ListenerManager();

    @Inject
    private ITimerService timerService;

    @Inject
    private ICoreAudioManager audioManager;

    private IMapObjectController mapObjectController;

    /**
     * Construct a new, empty panel.
     */
    public DataMonitorsDetailsPanel()
    {
        initComponents();
        clearAlarmsButton.putClientProperty(FlatClientProperties.MINIMUM_WIDTH, 0);
    }

    /**
     * @param mapObject the map object to display data monitors for
     */
    public void setMapObject(@Nonnull IMapObject mapObject)
    {
        mapObjectController = mapObject.getController();
        listenerManager.addPropertyChangeListener(mapObjectController, List.of(IDataMonitor.MUTE), e -> setSpeakerIcon());
        dataMonitorsPanel.setMapObject(mapObject);
        setSpeakerIcon();
    }

    @Override
    public void releaseResources()
    {
        listenerManager.unregisterAll();
        dataMonitorsPanel.releaseResources();
    }

    /**
     * Clear any alarming alarms in the alarms panel.
     */
    private void clearAlarmsButtonActionPerformed()
    {
        for (var dataMonitor : mapObjectController.getDataMonitors())
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
        var enableAudio = !mapObjectController.isAudioAlarmPlaybackEnabled();
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
     */
    private void showImmutableDataMonitorsPopup()
    {
        var tooltip = new JToolTip();
        tooltip.setTipText("<html>1 or more data monitors<br>are not mutable.");
        var location = buttonMute.getLocationOnScreen();
        var buttonsSize = buttonMute.getSize();
        var popup = PopupFactory.getSharedInstance().getPopup(buttonMute, tooltip, location.x + buttonsSize.width, location.y + buttonsSize.height);

        popup.show();

        timerService.createTask(popup::hide, 2_000);
    }

    /**
     * Sets the speaker icon.
     */
    @InvokeOnEDT
    private void setSpeakerIcon()
    {
        buttonMute.setIcon(mapObjectController.isAudioAlarmPlaybackEnabled() ? SOUND_ICON : MUTE_ICON);
    }

    private void initComponents()
    {
        // JFormDesigner - Component initialization - DO NOT MODIFY  //GEN-BEGIN:initComponents
        var statusPanel2 = new JPanel();
        var panelDataMonitorButtons = new JPanel();
        clearAlarmsButton = new JButton();
        buttonMute = new JButton();
        dataMonitorsPanel = new DataMonitorsPanel();
        CellConstraints cc = new CellConstraints();

        //======== this ========
        setName("this");
        setLayout(new FormLayout(
                "default:grow",
                "fill:default:grow"));

        //======== statusPanel2 ========
        {
            statusPanel2.setName("statusPanel2");
            statusPanel2.setLayout(new FormLayout(
                    "default:grow",
                    "fill:default, $lgap, fill:default:grow"));

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
            statusPanel2.add(panelDataMonitorButtons, cc.xy(1, 1));

            //---- dataMonitorsPanel ----
            dataMonitorsPanel.setName("dataMonitorsPanel");
            statusPanel2.add(dataMonitorsPanel, cc.xy(1, 3, CellConstraints.FILL, CellConstraints.FILL));
        }
        add(statusPanel2, cc.xy(1, 1));
        // JFormDesigner - End of component initialization  //GEN-END:initComponents
    }

    // JFormDesigner - Variables declaration - DO NOT MODIFY  //GEN-BEGIN:variables
    private JButton clearAlarmsButton;
    private JButton buttonMute;
    private DataMonitorsPanel dataMonitorsPanel;
    // JFormDesigner - End of variables declaration  //GEN-END:variables
}
