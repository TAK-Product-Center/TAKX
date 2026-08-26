package gov.takx.core.mapobject.gui;

import com.jgoodies.forms.layout.CellConstraints;
import com.jgoodies.forms.layout.FormLayout;
import gov.tak.api.cot.detail.DetailConstants;
import gov.tak.api.cot.event.CotDetail;
import gov.tak.api.cot.event.CotEvent;
import gov.tak.api.util.StandardAttributeNames;
import gov.takx.api.annotation.InvokeOnEDT;
import gov.takx.api.mapobject.IMapObject;
import gov.takx.api.ui.IResourceCleaner;
import gov.takx.commons.lang.ListenerManager;
import gov.takx.commons.swing.JTextFieldLabel;
import jakarta.annotation.Nullable;

import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingConstants;

import java.beans.PropertyChangeEvent;
import java.util.List;

/**
 * Panel to display device details. Currently, these come from the {@code takv} CoT detail block.
 *
 * @since 5.8
 */
public class DeviceDetailsPanel extends JPanel implements IResourceCleaner
{
    private static final List<String> LISTENER_PROPERTIES = List.of(StandardAttributeNames.COT_EVENT_XML, StandardAttributeNames.BATTERY);
    private final ListenerManager listenerManager = new ListenerManager();
    private IMapObject mapObject;
    private boolean empty;

    /**
     * Construct a new empty panel.
     */
    @SuppressWarnings("unused")
    public DeviceDetailsPanel()
    {
        initComponents();
    }

    @Override
    public void releaseResources()
    {
        listenerManager.unregisterAll();
    }

    /**
     * @return {@code true} if there is no map object, or it does not contain valid data
     */
    public boolean isEmpty()
    {
        return empty;
    }

    /**
     * @param mapObject the map object to monitor and display data for
     */
    public void setMapObject(@Nullable IMapObject mapObject)
    {
        if (this.mapObject == mapObject) return;
        if (this.mapObject != null) listenerManager.unregisterAll();
        this.mapObject = mapObject;
        if (this.mapObject != null) listenerManager.addPropertyChangeListener(mapObject, LISTENER_PROPERTIES, this::update);
        update((PropertyChangeEvent) null);
    }

    @InvokeOnEDT
    private void update(PropertyChangeEvent ignored)
    {
        final var takvDetail = getTakvDetail();
        final boolean haveTakvDetail = takvDetail != null;

        final var takPlatform = haveTakvDetail ? takvDetail.getAttribute(DetailConstants.ATTR_PLATFORM, "") : "";
        final var takVersion = haveTakvDetail ? takvDetail.getAttribute(DetailConstants.ATTR_VERSION, "") : "";
        final var takPlatformAndVersion = takPlatform + (takPlatform.isEmpty() ? "" : " ") + takVersion;
        final var osVersion = haveTakvDetail ? takvDetail.getAttribute(DetailConstants.ATTR_OS, "") : "";
        final var device = haveTakvDetail ? takvDetail.getAttribute(DetailConstants.ATTR_DEVICE, "") : "";
        final var battery = mapObject.getMetaLong(StandardAttributeNames.BATTERY, -1);

        takVersionField.setText(takPlatformAndVersion);
        osVersionField.setText(osVersion);
        deviceField.setText(device);
        batteryField.setText(battery != -1 ? battery + "%" : "");
        empty = takPlatformAndVersion.isEmpty() && osVersion.isEmpty() && device.isEmpty();
    }

    @Nullable
    private CotDetail getTakvDetail()
    {
        final CotEvent cotEvent = mapObject.getLatestCotEvent();
        final CotDetail detail = cotEvent != null ? cotEvent.getDetail() : null;
        return detail != null ? detail.getFirstChildByName(DetailConstants.TAKV) : null;
    }

    @SuppressWarnings("RedundantExplicitVariableType")
    private void initComponents()
    {
        // JFormDesigner - Component initialization - DO NOT MODIFY  //GEN-BEGIN:initComponents  @formatter:off
        var takVersionLabel = new JLabel();
        takVersionField = new JTextFieldLabel();
        var osVersionLabel = new JLabel();
        osVersionField = new JTextFieldLabel();
        var deviceLabel = new JLabel();
        deviceField = new JTextFieldLabel();
        var label1 = new JLabel();
        batteryField = new JLabel();
        CellConstraints cc = new CellConstraints();

        //======== this ========
        setName("this");
        setLayout(new FormLayout(
                "default, $lcgap, default:grow",
                "3*(default, $lgap), default"));

        //---- takVersionLabel ----
        takVersionLabel.setText("TAK:");
        takVersionLabel.setName("takVersionLabel");
        add(takVersionLabel, cc.xy(1, 1));

        //---- takVersionField ----
        takVersionField.setText("TAKX-MIL 10.1.2");
        takVersionField.setHorizontalAlignment(SwingConstants.RIGHT);
        takVersionField.setName("takVersionField");
        add(takVersionField, cc.xy(3, 1, CellConstraints.RIGHT, CellConstraints.DEFAULT));

        //---- osVersionLabel ----
        osVersionLabel.setText("OS:");
        osVersionLabel.setName("osVersionLabel");
        add(osVersionLabel, cc.xy(1, 3));

        //---- osVersionField ----
        osVersionField.setText("Windows 11 , version 10.0, arch amd64");
        osVersionField.setHorizontalAlignment(SwingConstants.RIGHT);
        osVersionField.setName("osVersionField");
        add(osVersionField, cc.xy(3, 3, CellConstraints.RIGHT, CellConstraints.DEFAULT));

        //---- deviceLabel ----
        deviceLabel.setText("Device:");
        deviceLabel.setName("deviceLabel");
        add(deviceLabel, cc.xy(1, 5));

        //---- deviceField ----
        deviceField.setText("Unknown");
        deviceField.setHorizontalAlignment(SwingConstants.RIGHT);
        deviceField.setName("deviceField");
        add(deviceField, cc.xy(3, 5, CellConstraints.RIGHT, CellConstraints.DEFAULT));

        //---- label1 ----
        label1.setText("Battery:");
        label1.setName("label1");
        add(label1, cc.xy(1, 7, CellConstraints.DEFAULT, CellConstraints.TOP));

        //---- batteryField ----
        batteryField.setText("100%");
        batteryField.setHorizontalAlignment(SwingConstants.RIGHT);
        batteryField.setName("batteryField");
        add(batteryField, cc.xy(3, 7, CellConstraints.RIGHT, CellConstraints.DEFAULT));
        // JFormDesigner - End of component initialization  //GEN-END:initComponents  @formatter:on
    }

    // JFormDesigner - Variables declaration - DO NOT MODIFY  //GEN-BEGIN:variables  @formatter:off
    private JTextFieldLabel takVersionField;
    private JTextFieldLabel osVersionField;
    private JTextFieldLabel deviceField;
    private JLabel batteryField;
    // JFormDesigner - End of variables declaration  //GEN-END:variables  @formatter:on
}
