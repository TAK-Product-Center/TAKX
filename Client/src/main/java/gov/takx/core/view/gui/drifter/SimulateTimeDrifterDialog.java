package gov.takx.core.view.gui.drifter;

import com.jgoodies.forms.factories.Borders;
import com.jgoodies.forms.layout.CellConstraints;
import com.jgoodies.forms.layout.FormLayout;
import gov.takx.api.mapobject.IMapObject;
import gov.takx.commons.swing.FormButtonBar;
import gov.takx.commons.swing.TimePicker;
import gov.takx.core.drifter.DrifterController;
import gov.takx.platform.ui.AStandardDialog;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import jakarta.inject.Inject;

import javax.swing.JLabel;
import javax.swing.JPanel;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.Insets;
import java.awt.event.ActionEvent;

/**
 * Dialog to allow user to simulate a time delay (fast-forward) on an {@link IMapObject} with a drifter configuration. If
 * the {@link #mapObject} field is {@code null} then the delay is applied to all drifter configurations.
 * 
 * @since 5.6
 */
public class SimulateTimeDrifterDialog extends AStandardDialog
{
    @Inject
    private DrifterController controller;
    private IMapObject mapObject;

    /**
     * Construct a new dialog instance with no map object.
     */
    public SimulateTimeDrifterDialog()
    {
        initComponents();
        buttonBar.getOkButton().setText("Go");
        buttonBar.addApplyListener(this::apply);
        buttonBar.attachFieldChangeListener(delayField, delayField::addChangeListener, null);
    }

    /**
     * @return the map object that this delay will be applied to, or {@code null} to apply to all drifter configurations
     */
    @Nullable
    public IMapObject getMapObject()
    {
        return mapObject;
    }

    /**
     * @param mapObject the map object that this delay will be applied to, or {@code null} to apply to all drifter configurations
     */
    public void setMapObject(@Nullable IMapObject mapObject)
    {
        this.mapObject = mapObject;
        if (mapObject != null) setTitle(getTitle() + " - " + mapObject.getName());
    }

    private void apply(@Nonnull ActionEvent actionEvent)
    {
        if (mapObject == null)
        {
            controller.simulateTimeAll(delayField.getTime());
        } else
        {
            controller.simulateTime(mapObject, delayField.getTime());
        }
    }

    /**
     * Convenience method to create and display an instance for the given map object.
     *
     * @param mapObject the map object
     * @return the dialog instance
     */
    public static SimulateTimeDrifterDialog show(@Nonnull IMapObject mapObject)
    {
        var dialog = new SimulateTimeDrifterDialog();
        dialog.setMapObject(mapObject);
        dialog.setVisible(true);
        return dialog;
    }

    private void initComponents()
    {
        // JFormDesigner - Component initialization - DO NOT MODIFY  //GEN-BEGIN:initComponents  @formatter:off
        var dialogPanel = new JPanel();
        var formPanel = new JPanel();
        var delayLabel = new JLabel();
        delayField = new TimePicker();
        buttonBar = new FormButtonBar();
        CellConstraints cc = new CellConstraints();

        //======== this ========
        setTitle("Simulate Time Delay");
        setMaximumSize(new Dimension(290, 155));
        setPreferredSize(new Dimension(290, 155));
        setResizable(false);
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
                        "default"));

                //---- delayLabel ----
                delayLabel.setText("Delay:");
                delayLabel.setName("delayLabel");
                formPanel.add(delayLabel, new CellConstraints(1, 1, 1, 1, CellConstraints.DEFAULT, CellConstraints.CENTER, new Insets(15, 0, 0, 0)));

                //---- delayField ----
                delayField.setTime(0L);
                delayField.setName("delayField");
                formPanel.add(delayField, cc.xy(3, 1));
            }
            dialogPanel.add(formPanel, BorderLayout.CENTER);

            //---- buttonBar ----
            buttonBar.setApplyButtonVisible(false);
            buttonBar.setErrorLabelVisible(false);
            buttonBar.setDeleteButtonVisible(false);
            buttonBar.setName("buttonBar");
            dialogPanel.add(buttonBar, BorderLayout.SOUTH);
        }
        contentPane.add(dialogPanel, BorderLayout.CENTER);
        pack();
        setLocationRelativeTo(getOwner());
        // JFormDesigner - End of component initialization  //GEN-END:initComponents  @formatter:on
    }

    // JFormDesigner - Variables declaration - DO NOT MODIFY  //GEN-BEGIN:variables  @formatter:off
    private TimePicker delayField;
    private FormButtonBar buttonBar;
    // JFormDesigner - End of variables declaration  //GEN-END:variables  @formatter:on
}
