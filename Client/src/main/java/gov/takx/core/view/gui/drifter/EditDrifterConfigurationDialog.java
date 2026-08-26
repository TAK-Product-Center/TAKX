package gov.takx.core.view.gui.drifter;

import com.jgoodies.forms.factories.Borders;
import com.jgoodies.forms.layout.CellConstraints;
import com.jgoodies.forms.layout.FormLayout;
import gov.takx.api.annotation.PseudoBean;
import gov.takx.api.drifter.DrifterConfiguration;
import gov.takx.api.mapobject.IMapObject;
import gov.takx.api.mapobject.IRouteMapObject;
import gov.takx.api.routes.IRouteEditCompleteListener;
import gov.takx.api.routes.IRoutesManager;
import gov.takx.api.shapes.IRoute;
import gov.takx.commons.swing.FormButtonBar;
import gov.takx.commons.swing.FormLayoutUtils;
import gov.takx.core.drifter.DrifterController;
import gov.takx.core.mapobject.gui.MapObjectField;
import gov.takx.core.view.gui.maptools.BearingUnitComboBox;
import gov.takx.core.view.gui.maptools.SpeedUnitComboBox;
import gov.takx.platform.preferences.UserPreferenceEntry;
import gov.takx.platform.ui.AStandardDialog;
import gov.takx.platform.utils.units.BearingUnits;
import gov.takx.platform.utils.units.SpeedUnits;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import jakarta.inject.Inject;

import javax.swing.ButtonGroup;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JSpinner;
import javax.swing.SpinnerNumberModel;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.util.Objects;

/**
 * A swing dialog for editing the {@link DrifterConfiguration} for an {@link IMapObject}. Validation and button logic is driven
 * heavily by {@link FormButtonBar}.
 *
 * @since 5.6
 */
@PseudoBean
public class EditDrifterConfigurationDialog extends AStandardDialog
{
    @Inject
    private IRoutesManager routesManager;
    @Inject
    private DrifterController controller;

    private final IRouteEditCompleteListener routeEditCompleteListener = this::routeEditComplete;
    private IMapObject mapObject;

    /**
     * Construct a new dialog instance with no field data prefilled.
     */
    public EditDrifterConfigurationDialog()
    {
        initComponents();

        // Setup fields and layout
        speedUnitField.setSelectedItem(UserPreferenceEntry.DISPLAY_SPEED_UNITS.getValueAs(SpeedUnits.class));
        bearingUnitField.setSelectedItem(UserPreferenceEntry.DISPLAY_BEARING_UNIT.getValueAs(BearingUnits.class));
        routeField.setFilter(e -> e instanceof IRoute);
        FormLayoutUtils.hideRow(bearingPanel);
        FormLayoutUtils.hideRow(routeField);

        // Attach fields
        buttonBar.attachField(speedField, null);
        buttonBar.attachField(speedUnitField, null);
        buttonBar.attachField(pausedField, null);
        buttonBar.attachField(bearingField, null);
        buttonBar.attachField(bearingUnitField, null);
        buttonBar.attachField(typeConstantField, this::validateType);
        buttonBar.attachField(typeRouteField, this::validateType);
        buttonBar.attachFieldActionListener(routeField, routeField::addActionListener, e -> typeRouteField.isSelected() && routeField.getSelectedItem() == null ? "Must select a Route" : null);
    }

    /**
     * Convenience method to create and display an instance for the given map object in a single call since we cant do it
     * all in the constructor.
     *
     * @param mapObject the map object
     * @return the dialog instance
     */
    @Nonnull
    public static EditDrifterConfigurationDialog show(@Nonnull IMapObject mapObject)
    {
        var dialog = new EditDrifterConfigurationDialog();
        dialog.setMapObject(mapObject);
        dialog.setVisible(true);
        return dialog;
    }

    /**
     * @param mapObject the map object to use to populate dialog form fields
     */
    public void setMapObject(@Nonnull IMapObject mapObject)
    {
        this.mapObject = mapObject;
        setTitle(getTitle() + " - " + mapObject.getName());

        mapObjectField.setSelectedItem(mapObject);
        var currentConfig = mapObject.getDrifterConfiguration();
        if (currentConfig != null)
        {
            speedUnitField.setSelectedItem(UserPreferenceEntry.DISPLAY_SPEED_UNITS.getValueAs(SpeedUnits.class));
            speedUnitField.setValueMetersPerSecond(currentConfig.speed());
            bearingUnitField.setSelectedItem(UserPreferenceEntry.DISPLAY_BEARING_UNIT.getValueAs(BearingUnits.class));
            bearingUnitField.setValueDegrees(currentConfig.bearing());
            pausedField.setSelected(currentConfig.paused());
            var route = controller.getRoute(currentConfig);
            if (route != null)
            {
                typeRouteField.doClick();
                routeField.setSelectedItem(route);
            } else
            {
                typeConstantField.doClick();
            }
        }
        buttonBar.setPanelClean();
    }

    private String validateType(Object ignored)
    {
        if (!typeRouteField.isSelected() && !typeConstantField.isSelected()) return "Must select a heading type";
        return null;
    }

    private void typeChanged(ActionEvent ignored)
    {
        FormLayoutUtils.hideOrRestoreRow(routeField, typeConstantField.isSelected());
        FormLayoutUtils.hideOrRestoreRow(bearingPanel, !typeConstantField.isSelected());
        pack();
        routeField.setSelectedItem(typeConstantField.isSelected() ? null : routeField.getSelectedItem());
    }

    private void routeFieldNewActionPerformed(ActionEvent ignored)
    {
        routesManager.addRouteEditListener(routeEditCompleteListener);
        routesManager.startNewRouteEditing();
        setVisible(false);
    }

    private void buttonBarApply(ActionEvent ignored)
    {
        var config = new DrifterConfiguration(
                speedUnitField.getValueMetersPerSecond(),
                bearingUnitField.getValueDegrees(),
                pausedField.isSelected(),
                0,
                typeConstantField.isSelected() ? null : Objects.requireNonNull(routeField.getSelectedItem()).getUid());
        controller.updateConfiguration(mapObject, config);
    }

    private void buttonBarDelete(ActionEvent e)
    {
        if (controller.deleteConfiguration(mapObject))
        {
            ((FormButtonBar.DeleteActionEvent) e).successful();
        }
    }

    private void routeEditComplete(@Nullable IRoute route, boolean cancelled)
    {
        routesManager.removeRouteEditListener(routeEditCompleteListener);
        if (!cancelled)
        {
            routeField.setSelectedItem(route != null ? route.getShapeMapObject() : null);
        }
        setVisible(true);
    }

    @SuppressWarnings({"Convert2MethodRef", "RedundantExplicitVariableType"})
    private void initComponents()
    {
        // JFormDesigner - Component initialization - DO NOT MODIFY  //GEN-BEGIN:initComponents  @formatter:off
        var dialogPanel = new JPanel();
        var formPanel = new JPanel();
        var mapObjectLabel = new JLabel();
        mapObjectField = new MapObjectField<>();
        var pausedLabel = new JLabel();
        pausedField = new JCheckBox();
        var speedLabel = new JLabel();
        var speedPanel = new JPanel();
        speedField = new JSpinner();
        speedUnitField = new SpeedUnitComboBox();
        var typeLabel = new JLabel();
        var typePanel = new JPanel();
        typeConstantField = new JRadioButton();
        typeRouteField = new JRadioButton();
        var routeLabel = new JLabel();
        routeField = new MapObjectField<>();
        var bearingLabel = new JLabel();
        bearingPanel = new JPanel();
        bearingField = new JSpinner();
        bearingUnitField = new BearingUnitComboBox();
        buttonBar = new FormButtonBar();
        CellConstraints cc = new CellConstraints();

        //======== this ========
        setTitle("Drifter Configuration");
        setMinimumSize(new Dimension(400, 75));
        setName("this");
        var contentPane = getContentPane();
        contentPane.setLayout(new BorderLayout());

        //======== dialogPanel ========
        {
            dialogPanel.setBorder(Borders.DIALOG_BORDER);
            dialogPanel.setName("dialogPanel");
            dialogPanel.setLayout(new BorderLayout());

            //======== formPanel ========
            {
                formPanel.setName("formPanel");
                formPanel.setLayout(new FormLayout(
                        "default, $lcgap, default:grow",
                        "default, $ugap, 4*(default, $lgap), default"));

                //---- mapObjectLabel ----
                mapObjectLabel.setText("Map Object:");
                mapObjectLabel.setName("mapObjectLabel");
                formPanel.add(mapObjectLabel, cc.xy(1, 1));

                //---- mapObjectField ----
                mapObjectField.setEditable(false);
                mapObjectField.setName("mapObjectField");
                formPanel.add(mapObjectField, cc.xy(3, 1));

                //---- pausedLabel ----
                pausedLabel.setText("Paused");
                pausedLabel.setName("pausedLabel");
                formPanel.add(pausedLabel, cc.xy(1, 3));

                //---- pausedField ----
                pausedField.setName("pausedField");
                formPanel.add(pausedField, cc.xy(3, 3, CellConstraints.LEFT, CellConstraints.DEFAULT));

                //---- speedLabel ----
                speedLabel.setText("Speed:");
                speedLabel.setName("speedLabel");
                formPanel.add(speedLabel, cc.xy(1, 5));

                //======== speedPanel ========
                {
                    speedPanel.setName("speedPanel");
                    speedPanel.setLayout(new FormLayout(
                            "default:grow, $lcgap, default",
                            "default"));

                    //---- speedField ----
                    speedField.setModel(new SpinnerNumberModel(0.0, 0.0, null, 1.0));
                    speedField.setName("speedField");
                    speedPanel.add(speedField, cc.xy(1, 1));

                    //---- speedUnitField ----
                    speedUnitField.setSpinner(speedField);
                    speedUnitField.setName("speedUnitField");
                    speedPanel.add(speedUnitField, cc.xy(3, 1));
                }
                formPanel.add(speedPanel, cc.xy(3, 5));

                //---- typeLabel ----
                typeLabel.setText("Heading:");
                typeLabel.setName("typeLabel");
                formPanel.add(typeLabel, cc.xy(1, 7));

                //======== typePanel ========
                {
                    typePanel.setName("typePanel");
                    typePanel.setLayout(new FormLayout(
                            "default, $lcgap, default",
                            "default"));

                    //---- typeConstantField ----
                    typeConstantField.setText("Constant");
                    typeConstantField.setName("typeConstantField");
                    typeConstantField.addActionListener(e -> typeChanged(e));
                    typePanel.add(typeConstantField, cc.xy(1, 1));

                    //---- typeRouteField ----
                    typeRouteField.setText("Route");
                    typeRouteField.setName("typeRouteField");
                    typeRouteField.addActionListener(e -> typeChanged(e));
                    typePanel.add(typeRouteField, cc.xy(3, 1));
                }
                formPanel.add(typePanel, cc.xy(3, 7));

                //---- routeLabel ----
                routeLabel.setText("Route:");
                routeLabel.setName("routeLabel");
                formPanel.add(routeLabel, cc.xy(1, 9));

                //---- routeField ----
                routeField.setEmptyText("No Route Selected");
                routeField.setSelectPrompt("Select a Route to drift on.");
                routeField.setName("routeField");
                routeField.addNewObjectActionListener(e -> routeFieldNewActionPerformed(e));
                formPanel.add(routeField, cc.xy(3, 9));

                //---- bearingLabel ----
                bearingLabel.setName("bearingLabel");
                formPanel.add(bearingLabel, cc.xy(1, 11));

                //======== bearingPanel ========
                {
                    bearingPanel.setName("bearingPanel");
                    bearingPanel.setLayout(new FormLayout(
                            "default:grow, $lcgap, default",
                            "default"));

                    //---- bearingField ----
                    bearingField.setModel(new SpinnerNumberModel(0.0, 0.0, null, 0.5));
                    bearingField.setName("bearingField");
                    bearingPanel.add(bearingField, cc.xy(1, 1));

                    //---- bearingUnitField ----
                    bearingUnitField.setSpinner(bearingField);
                    bearingUnitField.setName("bearingUnitField");
                    bearingPanel.add(bearingUnitField, cc.xy(3, 1));
                }
                formPanel.add(bearingPanel, cc.xy(3, 11));
            }
            dialogPanel.add(formPanel, BorderLayout.CENTER);

            //---- buttonBar ----
            buttonBar.setDeleteButtonVisible(false);
            buttonBar.setName("buttonBar");
            buttonBar.addApplyListener(e -> buttonBarApply(e));
            buttonBar.addDeleteListener(e -> buttonBarDelete(e));
            dialogPanel.add(buttonBar, BorderLayout.SOUTH);
        }
        contentPane.add(dialogPanel, BorderLayout.CENTER);
        pack();
        setLocationRelativeTo(getOwner());

        //---- typeButtonGroup ----
        var typeButtonGroup = new ButtonGroup();
        typeButtonGroup.add(typeConstantField);
        typeButtonGroup.add(typeRouteField);
        // JFormDesigner - End of component initialization  //GEN-END:initComponents  @formatter:on
    }

    // JFormDesigner - Variables declaration - DO NOT MODIFY  //GEN-BEGIN:variables  @formatter:off
    private MapObjectField<IMapObject> mapObjectField;
    private JCheckBox pausedField;
    private JSpinner speedField;
    private SpeedUnitComboBox speedUnitField;
    private JRadioButton typeConstantField;
    private JRadioButton typeRouteField;
    private MapObjectField<IRouteMapObject> routeField;
    private JPanel bearingPanel;
    private JSpinner bearingField;
    private BearingUnitComboBox bearingUnitField;
    private FormButtonBar buttonBar;
    // JFormDesigner - End of variables declaration  //GEN-END:variables  @formatter:on
}
