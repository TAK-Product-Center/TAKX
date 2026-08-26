package gov.takx.core.view.gui.geofence;

import com.atakmap.coremap.conversions.Span;
import com.atakmap.coremap.conversions.SpanUtilities;
import com.jgoodies.forms.factories.Borders;
import com.jgoodies.forms.layout.CellConstraints;
import com.jgoodies.forms.layout.FormLayout;
import gov.tak.api.cot.exporter.ICotExporterManager;
import gov.tak.api.maps.PointMapItem;
import gov.takx.api.annotation.PseudoBean;
import gov.takx.api.geofence.GeoFence;
import gov.takx.api.geofence.IGeoFenceManager;
import gov.takx.api.geofence.MonitoredType;
import gov.takx.api.geofence.Trigger;
import gov.takx.api.mapobject.IShapeMapObject;
import gov.takx.api.persist.IMapObjectFinder;
import gov.takx.commons.swing.ActionPopupFactory;
import gov.takx.commons.swing.AlwaysOnTopJOptionPane;
import gov.takx.commons.swing.documents.LimitedDocument;
import gov.takx.commons.utils.ParseUtils;
import gov.takx.core.geofence.AlertManager;
import gov.takx.core.geofence.AlertOverlaysController;
import gov.takx.core.mapobject.gui.SendAsCotDialog;
import gov.takx.platform.ui.AStandardDialog;
import gov.takx.platform.ui.ResourceDirectory;
import gov.takx.platform.ui.UIResources;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import jakarta.annotation.PostConstruct;
import jakarta.inject.Inject;

import javax.swing.ButtonGroup;
import javax.swing.DefaultComboBoxModel;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JSeparator;
import javax.swing.JTextField;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Insets;
import java.awt.Window;
import java.awt.event.ItemEvent;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

/**
 * Dialog for creating/updating/deleting a GeoFence on a given {@link IShapeMapObject}.
 * The {@link IGeoFenceManager GeoFence Manager} is used to perform the create/update/delete operations.
 * 
 * @see IShapeMapObject#getGeoFence()
 * @see IGeoFenceManager#addOrUpdateGeoFence(GeoFence)
 * @see IGeoFenceManager#removeGeoFence(GeoFence)
 * 
 * @since 5.3
 */
@SuppressWarnings("CdiManagedBeanInconsistencyInspection")
@PseudoBean
public class EditGeoFenceDialog extends AStandardDialog
{
    private static final String OK_DISABLED_TOOLTIP = "You must select custom map item(s) to monitor";

    @Inject
    private IGeoFenceManager geoFenceManager;
    @Inject
    private ICotExporterManager cotExporterManager;
    @Inject
    private AlertManager alertManager;
    @Inject
    private IMapObjectFinder mapObjectFinder;

    @Inject
    private AlertOverlaysController alertOverlaysController;

    private final IShapeMapObject<?> shapeMapObject;
    private final GeoFence geoFence;
    private final Set<String> customUids = new HashSet<>();
    private final boolean editing;

    /**
     * @param owner Owning component
     * @param shapeMapObject Shape map object to process
     */
    public EditGeoFenceDialog(@Nullable Window owner, @Nonnull IShapeMapObject<?> shapeMapObject)
    {
        super(owner);

        this.shapeMapObject = shapeMapObject;
        editing = shapeMapObject.hasGeoFence();
        geoFence = shapeMapObject.getGeoFence();

        initUI();
    }

    @PostConstruct
    private void postConstruct()
    {
        initFromGeoFence();
    }

    private void initUI()
    {
        initComponents();

        dismissAlertsButton.setIcon(UIResources.getActionButtonIcon(ResourceDirectory.TAK_ASSETS_PNG, "Clear-Content_32x32.png"));
        triggerComboBox.setModel(new DefaultComboBoxModel<>(Trigger.values()));
        monitorComboBox.setModel(new DefaultComboBoxModel<>(MonitoredType.values()));
        minElevationTextField.setDocument(new LimitedDocument(Character::isDigit));
        maxElevationTextField.setDocument(new LimitedDocument(Character::isDigit));
    }

    /**
     * Install our GeoFence configuration nto the UI controls.
     */
    private void initFromGeoFence()
    {
        setTitle(editing ? "Edit Geo Fence" : "Create Geo Fence");
        okButton.setText(editing ? "Update" : "Create");
        nameLabel.setText(shapeMapObject.getName());

        updateAlertView();

        if (editing)
        {
            final boolean tracking = geoFence.tracking();
            trackingRadioButton.setSelected(tracking);
            notTrackingRadioButton.setSelected(!tracking);
            setTrackingButtonColor();

            triggerComboBox.setSelectedItem(geoFence.trigger());
            monitorComboBox.setSelectedItem(geoFence.monitoredType());

            final boolean monitorElevation = geoFence.monitorElevation();
            specifyElevationCheckBox.setSelected(monitorElevation);
            setElevationFieldsEnabled(monitorElevation);

            // Elevations are stored/transferred as meters, but we want to display in feet
            setTextFieldInFeet(minElevationTextField, geoFence.minElevation());
            setTextFieldInFeet(maxElevationTextField, geoFence.maxElevation());

            if (geoFence.monitoredType() == MonitoredType.Custom) customUids.addAll(geoFence.customUids());
        } else
        {
            // Install defaults for a new GeoFence
            deleteButton.setVisible(false);
            trackingRadioButton.setSelected(true);
            triggerComboBox.setSelectedItem(Trigger.Entry);
            monitorComboBox.setSelectedItem(MonitoredType.All);
            specifyElevationCheckBox.setSelected(false);
            setTrackingButtonColor();
            setElevationFieldsEnabled(false);
        }

        monitorComboBoxItemStateChanged();
    }

    /**
     * Set the text of the given {@code textField} to the {@code elevation} converted to from meters to feet.
     */
    private void setTextFieldInFeet(JTextField textField, double elevation)
    {
        textField.setText(!Double.isNaN(elevation) ? String.valueOf(Math.round(SpanUtilities.convert(elevation, Span.METER, Span.FOOT))) : "");
    }

    /**
     * Update the UI controls based on the alert count of the GeoFence.
     */
    private void updateAlertView()
    {
        final int alertCount = getAlertCount();
        alertLabel.setText("# Alerts: " + alertCount);

        final boolean hasAlerts = alertCount > 0;
        alertLabel.setForeground(hasAlerts ? Color.RED : Color.WHITE);
        alertButtonsPanel.setVisible(hasAlerts);
    }

    /**
     * @return The number of active alerts for the GeoFence.
     */
    private int getAlertCount()
    {
        return editing ? alertManager.getAlertCount(shapeMapObject.getUid()) : 0;
    }

    /**
     * Set the enabled property on our elevation entry fields.
     */
    private void setElevationFieldsEnabled(boolean enable)
    {
        minElevationTextField.setEnabled(enable);
        maxElevationTextField.setEnabled(enable);
    }

    /**
     * Set the background color of the "tracking" radio button according to its state.  Blue for selected, default for not.
     */
    private void setTrackingButtonColor()
    {
        trackingRadioButton.setBackground(trackingRadioButton.isSelected() ? Color.blue : notTrackingRadioButton.getBackground());
    }

    /**
     * Show/hide the custom map item selection button based on MonitoredType.
     */
    private void monitorComboBoxItemStateChanged()
    {
        final var monitoredType = monitorComboBox.getItemAt(monitorComboBox.getSelectedIndex());
        showCustomSelectorButton.setVisible(monitoredType == MonitoredType.Custom);
        configureOkAndSendButtons();
    }

    /**
     * Update the elevation related fields according to the new state.
     */
    private void specifyElevationCheckBoxItemStateChanged(ItemEvent e)
    {
        setElevationFieldsEnabled(e.getStateChange() == ItemEvent.SELECTED);
    }

    /**
     * Configure the OK and Send buttons based on monitored type and selected custom UIDs.
     */
    private void configureOkAndSendButtons()
    {
        final var monitoredType = monitorComboBox.getItemAt(monitorComboBox.getSelectedIndex());
        final boolean enabled = monitoredType != MonitoredType.Custom || !customUids.isEmpty();
        final String toolTipText = enabled ? "" : OK_DISABLED_TOOLTIP;

        okButton.setEnabled(enabled);
        okButton.setToolTipText(toolTipText);

        sendButton.setEnabled(enabled);
        sendButton.setToolTipText(toolTipText);
    }

    /**
     * Show the explorable selection dialog to allow the user to select which map items to monitor.
     */
    private void showCustomSelector()
    {
        // Map items to pre-select
        final var mapItems = customUids.stream()
                .map(uid -> mapObjectFinder.findMapObjectByUid(uid))
                .filter(PointMapItem.class::isInstance)
                .map(PointMapItem.class::cast)
                .toList();

        new SelectCustomItemsDialog(this, mapItems, selections -> {
            // Replace the custom UIDs with the newly selected ones
            customUids.clear();
            selections.forEach(mapItem -> customUids.add(mapItem.getUid()));
            configureOkAndSendButtons();
        }, null).setVisible(true);
    }

    /**
     * Create a new {@link GeoFence} from the input fields and update/create it.
     *
     * @see IGeoFenceManager#addOrUpdateGeoFence(GeoFence)
     */
    private void okButtonActionPerformed()
    {
        final var monitoredType = monitorComboBox.getItemAt(monitorComboBox.getSelectedIndex());

        final var builder = GeoFence.builder()
                .shapeUid(shapeMapObject.getUid())
                .tracking(trackingRadioButton.isSelected())
                .trigger(triggerComboBox.getItemAt(triggerComboBox.getSelectedIndex()))
                .monitoredType(monitoredType)
                .monitorElevation(specifyElevationCheckBox.isSelected())
                .minElevation(getElevationInMeters(minElevationTextField))
                .maxElevation(getElevationInMeters(maxElevationTextField));

        if (monitoredType == MonitoredType.Custom) builder.customUids(customUids);

        geoFenceManager.addOrUpdateGeoFence(builder.build());

        dispose();
    }

    /**
     * @return The value of the given text field in meters, converted from feet, {@link Double#NaN} if it can't be parsed or is empty
     */
    private double getElevationInMeters(JTextField textField)
    {
        final double elevationFeet = ParseUtils.parseDouble(textField.getText(), Double.NaN);
        return !Double.isNaN(elevationFeet) ? SpanUtilities.convert(elevationFeet, Span.FOOT, Span.METER) : Double.NaN;
    }

    /**
     * Prompt the user, and dismiss alerts if they say yes.
     */
    private void dismissAlerts()
    {
        final int alertCount = getAlertCount();
        final String format = alertCount == 1
                ? "'%s' has %d alert. Would you like to dismiss it?"
                : "'%s' has %d alerts. Would you like to dismiss them?";
        final String msg = String.format(format, shapeMapObject.getName(), alertCount);

        ActionPopupFactory.yesNo(msg, dismissAlertsButton, () -> {
            alertManager.clearAlerts(shapeMapObject.getUid());
            updateAlertView();
        }).show();
    }

    /**
     * Open the GeoFence Alerts panel.
     */
    private void viewAlerts()
    {
        alertOverlaysController.showAlertsInOverlays(geoFence);
        dispose();
    }

    /**
     * Prompt the user, and delete the geoFence if they say yes.
     */
    private void delete()
    {
        ActionPopupFactory.yesNo("Delete Geo Fence?", deleteButton,
                () -> {
                    geoFenceManager.removeGeoFence(geoFence);
                    dispose();
                }).show();
    }

    /**
     * Send the GeoFence out as CoT.  This implies Create/Update.
     */
    private void send()
    {
        okButtonActionPerformed();

        final String mapObjectName = shapeMapObject.getName();
        new SendAsCotDialog(mapObjectName,
                () -> Collections.singletonList(cotExporterManager.export(shapeMapObject)),
                () -> AlwaysOnTopJOptionPane.showErrorDialog("Could not export as CoT:\n   " +
                        mapObjectName + "\n   " + shapeMapObject, "Export failed"))
                .setVisible(true);
    }

    private void cancel()
    {
        dispose();
    }

    @SuppressWarnings("Convert2MethodRef")
    private void initComponents()
    {
        // JFormDesigner - Component initialization - DO NOT MODIFY  //GEN-BEGIN:initComponents  @formatter:off
        var dialogPane = new JPanel();
        var contentPanel = new JPanel();
        var label1 = new JLabel();
        nameLabel = new JLabel();
        var label2 = new JLabel();
        var panel2 = new JPanel();
        notTrackingRadioButton = new JRadioButton();
        trackingRadioButton = new JRadioButton();
        var label3 = new JLabel();
        triggerComboBox = new JComboBox<>();
        var label4 = new JLabel();
        var panel3 = new JPanel();
        monitorComboBox = new JComboBox<>();
        showCustomSelectorButton = new JButton();
        var label5 = new JLabel();
        var panel1 = new JPanel();
        specifyElevationCheckBox = new JCheckBox();
        minElevationTextField = new JTextField();
        var label6 = new JLabel();
        maxElevationTextField = new JTextField();
        var label7 = new JLabel();
        var separator1 = new JSeparator();
        var label8 = new JLabel();
        var statusPanel = new JPanel();
        alertLabel = new JLabel();
        alertButtonsPanel = new JPanel();
        dismissAlertsButton = new JButton();
        var viewAlertsButton = new JButton();
        var buttonBar = new JPanel();
        okButton = new JButton();
        sendButton = new JButton();
        deleteButton = new JButton();
        var cancelButton = new JButton();
        CellConstraints cc = new CellConstraints();

        //======== this ========
        setTitle("Create Geo Fence");
        setModal(true);
        setName("this");
        var contentPane = getContentPane();
        contentPane.setLayout(new BorderLayout());

        //======== dialogPane ========
        {
            dialogPane.setBorder(Borders.createEmptyBorder("7dlu, 7dlu, 7dlu, 7dlu"));
            dialogPane.setName("dialogPane");
            dialogPane.setLayout(new BorderLayout());

            //======== contentPanel ========
            {
                contentPanel.setName("contentPanel");
                contentPanel.setLayout(new FormLayout(
                        "right:default, $lcgap, default:grow",
                        "4*(default, $ugap), bottom:default, bottom:7dlu, $ugap, default"));

                //---- label1 ----
                label1.setText("Name");
                label1.setName("label1");
                contentPanel.add(label1, cc.xy(1, 1));

                //---- nameLabel ----
                nameLabel.setText("text");
                nameLabel.setName("nameLabel");
                contentPanel.add(nameLabel, cc.xy(3, 1));

                //---- label2 ----
                label2.setText("Enabled");
                label2.setName("label2");
                contentPanel.add(label2, cc.xy(1, 3));

                //======== panel2 ========
                {
                    panel2.setName("panel2");
                    panel2.setLayout(new FormLayout(
                            "default, $lcgap, default",
                            "default"));

                    //---- notTrackingRadioButton ----
                    notTrackingRadioButton.setText("OFF");
                    notTrackingRadioButton.setToolTipText("Boundary crossings will NOT be tracked");
                    notTrackingRadioButton.setName("notTrackingRadioButton");
                    notTrackingRadioButton.addActionListener(e -> setTrackingButtonColor());
                    panel2.add(notTrackingRadioButton, cc.xy(1, 1));

                    //---- trackingRadioButton ----
                    trackingRadioButton.setText("Tracking");
                    trackingRadioButton.setToolTipText("Boundary crossings WILL be tracked");
                    trackingRadioButton.setName("trackingRadioButton");
                    trackingRadioButton.addActionListener(e -> setTrackingButtonColor());
                    panel2.add(trackingRadioButton, cc.xy(3, 1));
                }
                contentPanel.add(panel2, cc.xy(3, 3));

                //---- label3 ----
                label3.setText("Trigger");
                label3.setName("label3");
                contentPanel.add(label3, cc.xy(1, 5));

                //---- triggerComboBox ----
                triggerComboBox.setName("triggerComboBox");
                contentPanel.add(triggerComboBox, cc.xy(3, 5));

                //---- label4 ----
                label4.setText("Monitor");
                label4.setName("label4");
                contentPanel.add(label4, cc.xy(1, 7));

                //======== panel3 ========
                {
                    panel3.setName("panel3");
                    panel3.setLayout(new FormLayout(
                            "default:grow, right:pref",
                            "default"));

                    //---- monitorComboBox ----
                    monitorComboBox.setName("monitorComboBox");
                    monitorComboBox.addItemListener(e -> monitorComboBoxItemStateChanged());
                    panel3.add(monitorComboBox, cc.xy(1, 1));

                    //---- showCustomSelectorButton ----
                    showCustomSelectorButton.setText("Select ...");
                    showCustomSelectorButton.setToolTipText("Select Map Items to monitor");
                    showCustomSelectorButton.setName("showCustomSelectorButton");
                    showCustomSelectorButton.addActionListener(e -> showCustomSelector());
                    panel3.add(showCustomSelectorButton, cc.xy(2, 1));
                }
                contentPanel.add(panel3, cc.xy(3, 7));

                //---- label5 ----
                label5.setText("Elevation");
                label5.setName("label5");
                contentPanel.add(label5, new CellConstraints(1, 9, 1, 1, CellConstraints.DEFAULT, CellConstraints.DEFAULT, new Insets(0, 0, 5, 0)));

                //======== panel1 ========
                {
                    panel1.setName("panel1");
                    panel1.setLayout(new FormLayout(
                            "3*(default, $lcgap), default",
                            "default, $lgap, default"));

                    //---- specifyElevationCheckBox ----
                    specifyElevationCheckBox.setText("Specify elevation");
                    specifyElevationCheckBox.setToolTipText("Select to include item elevation in boundary crossing checks");
                    specifyElevationCheckBox.setName("specifyElevationCheckBox");
                    specifyElevationCheckBox.addItemListener(e -> specifyElevationCheckBoxItemStateChanged(e));
                    panel1.add(specifyElevationCheckBox, cc.xywh(1, 1, 5, 1));

                    //---- minElevationTextField ----
                    minElevationTextField.setColumns(10);
                    minElevationTextField.setToolTipText("Minimum altitude in ft MSL");
                    minElevationTextField.setName("minElevationTextField");
                    panel1.add(minElevationTextField, cc.xy(1, 3));

                    //---- label6 ----
                    label6.setText("to");
                    label6.setName("label6");
                    panel1.add(label6, cc.xy(3, 3));

                    //---- maxElevationTextField ----
                    maxElevationTextField.setColumns(10);
                    maxElevationTextField.setToolTipText("Maximum altitude in ft MSL");
                    maxElevationTextField.setName("maxElevationTextField");
                    panel1.add(maxElevationTextField, cc.xy(5, 3));

                    //---- label7 ----
                    label7.setText("ft MSL");
                    label7.setName("label7");
                    panel1.add(label7, cc.xy(7, 3));
                }
                contentPanel.add(panel1, cc.xy(3, 9));

                //---- separator1 ----
                separator1.setForeground(Color.red);
                separator1.setName("separator1");
                contentPanel.add(separator1, cc.xywh(1, 10, 3, 1));

                //---- label8 ----
                label8.setText("Status");
                label8.setName("label8");
                contentPanel.add(label8, cc.xy(1, 12));

                //======== statusPanel ========
                {
                    statusPanel.setName("statusPanel");
                    statusPanel.setLayout(new FormLayout(
                            "default, $lcgap, default:grow",
                            "default"));

                    //---- alertLabel ----
                    alertLabel.setText("# Alerts: n");
                    alertLabel.setName("alertLabel");
                    statusPanel.add(alertLabel, cc.xy(1, 1));

                    //======== alertButtonsPanel ========
                    {
                        alertButtonsPanel.setName("alertButtonsPanel");
                        alertButtonsPanel.setLayout(new FormLayout(
                                "$glue, default, $rgap, default",
                                "fill:default"));

                        //---- dismissAlertsButton ----
                        dismissAlertsButton.setToolTipText("Dismiss Alerts");
                        dismissAlertsButton.setIconTextGap(0);
                        dismissAlertsButton.setName("dismissAlertsButton");
                        dismissAlertsButton.addActionListener(e -> dismissAlerts());
                        alertButtonsPanel.add(dismissAlertsButton, cc.xy(2, 1));

                        //---- viewAlertsButton ----
                        viewAlertsButton.setText("View");
                        viewAlertsButton.setToolTipText("Open the alerts panel");
                        viewAlertsButton.setIconTextGap(0);
                        viewAlertsButton.setMargin(new Insets(0, 4, 0, 4));
                        viewAlertsButton.setName("viewAlertsButton");
                        viewAlertsButton.addActionListener(e -> viewAlerts());
                        alertButtonsPanel.add(viewAlertsButton, cc.xy(4, 1));
                    }
                    statusPanel.add(alertButtonsPanel, cc.xy(3, 1));
                }
                contentPanel.add(statusPanel, cc.xy(3, 12));
            }
            dialogPane.add(contentPanel, BorderLayout.CENTER);

            //======== buttonBar ========
            {
                buttonBar.setBorder(Borders.createEmptyBorder("8dlu, 0dlu, 0dlu, 0dlu"));
                buttonBar.setName("buttonBar");
                buttonBar.setLayout(new FormLayout(
                        "$glue, 3*($button, $rgap), $button",
                        "pref"));

                //---- okButton ----
                okButton.setText("OK");
                okButton.setName("okButton");
                okButton.addActionListener(e -> okButtonActionPerformed());
                buttonBar.add(okButton, cc.xy(2, 1));

                //---- sendButton ----
                sendButton.setText("Send");
                sendButton.setName("sendButton");
                sendButton.addActionListener(e -> send());
                buttonBar.add(sendButton, cc.xy(4, 1));

                //---- deleteButton ----
                deleteButton.setText("Delete");
                deleteButton.setName("deleteButton");
                deleteButton.addActionListener(e -> delete());
                buttonBar.add(deleteButton, cc.xy(6, 1));

                //---- cancelButton ----
                cancelButton.setText("Cancel");
                cancelButton.setName("cancelButton");
                cancelButton.addActionListener(e -> cancel());
                buttonBar.add(cancelButton, cc.xy(8, 1));
            }
            dialogPane.add(buttonBar, BorderLayout.SOUTH);
        }
        contentPane.add(dialogPane, BorderLayout.CENTER);
        pack();
        setLocationRelativeTo(getOwner());

        //---- buttonGroup1 ----
        var buttonGroup1 = new ButtonGroup();
        buttonGroup1.add(notTrackingRadioButton);
        buttonGroup1.add(trackingRadioButton);
        // JFormDesigner - End of component initialization  //GEN-END:initComponents  @formatter:on
    }

    // JFormDesigner - Variables declaration - DO NOT MODIFY  //GEN-BEGIN:variables  @formatter:off
    private JLabel nameLabel;
    private JRadioButton notTrackingRadioButton;
    private JRadioButton trackingRadioButton;
    private JComboBox<Trigger> triggerComboBox;
    private JComboBox<MonitoredType> monitorComboBox;
    private JButton showCustomSelectorButton;
    private JCheckBox specifyElevationCheckBox;
    private JTextField minElevationTextField;
    private JTextField maxElevationTextField;
    private JLabel alertLabel;
    private JPanel alertButtonsPanel;
    private JButton dismissAlertsButton;
    private JButton okButton;
    private JButton sendButton;
    private JButton deleteButton;
    // JFormDesigner - End of variables declaration  //GEN-END:variables  @formatter:on
}
