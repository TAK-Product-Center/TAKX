package gov.takx.core.view.gui.layers;

import com.jgoodies.forms.factories.Borders;
import com.jgoodies.forms.layout.CellConstraints;
import com.jgoodies.forms.layout.FormLayout;
import gov.takx.api.annotation.PseudoBean;
import gov.takx.api.coordinatesystem.CoordinateSystem;
import gov.takx.api.coordinatesystem.ILocation;
import gov.takx.core.view.gui.GUIUtils;
import gov.takx.platform.preferences.UserPreferenceEntry;
import gov.takx.platform.ui.AStandardDialog;
import jakarta.annotation.PostConstruct;

import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSeparator;
import javax.swing.JTextField;
import javax.swing.WindowConstants;

import java.awt.BorderLayout;
import java.awt.Dialog;
import java.awt.Font;
import java.awt.geom.Point2D;

import static gov.takx.api.coordinatesystem.CoordinateSystem.computeLatLonFromString;
import static gov.takx.api.coordinatesystem.CoordinateSystem.getLocationString;

/**
 * Edit dialog for editing the point location for geo-rectify
 *
 */
@SuppressWarnings("CdiManagedBeanInconsistencyInspection")
@PseudoBean
public class GeoRectifyControlPointDialog extends AStandardDialog
{
    private final Point2D point;
    private final int pointListIndex;
    private final GeoRectifyDialog.ControlPointListModel controlPointListModel;
    private ILocation pointLocation;

    public GeoRectifyControlPointDialog(Dialog owner, Point2D point, GeoRectifyDialog.ControlPointListModel model)
    {
        this(owner, point, -1, model);
    }

    public GeoRectifyControlPointDialog(Dialog owner, int index, GeoRectifyDialog.ControlPointListModel model)
    {
        this(owner, model.getPoint(index), index, model);
        pointLocation = model.getLocation(index);
    }

    public GeoRectifyControlPointDialog(Dialog owner, Point2D point, int index, GeoRectifyDialog.ControlPointListModel model)
    {
        super(owner);

        this.point = point;
        pointListIndex = index;
        controlPointListModel = model;

        initComponents();

        GUIUtils.addSingleActionDocumentListener(pointLocationTextField, e -> {
            pointLocation = computeLatLonFromString(pointLocationTextField.getText().trim());
            okButton.setEnabled(pointLocation != null);
        });
        getRootPane().setDefaultButton(okButton);
    }

    @PostConstruct
    private void postConstruct()
    {
        pointLocationTextField.setText(getLocationString(pointLocation, UserPreferenceEntry.COORDINATE_SYSTEM.getValueAs(CoordinateSystem.class)));
    }

    private void okButtonActionPerformed()
    {
        if (pointListIndex == -1)
        {
            controlPointListModel.add(point, pointLocation);
        } else
        {
            controlPointListModel.set(pointListIndex, pointLocation);
        }

        dispose();
    }

    private void cancelButtonActionPerformed()
    {
        dispose();
    }

    private void initComponents()
    {
        // JFormDesigner - Component initialization - DO NOT MODIFY  //GEN-BEGIN:initComponents
        var dialogPanel = new JPanel();
        var contentPanel = new JPanel();
        var instructionsLabel = new JLabel();
        var pointLocationLabel = new JLabel();
        pointLocationTextField = new JTextField();
        var exampleLabel = new JLabel();
        var exampleLatLongLabel = new JLabel();
        var separator1 = new JSeparator();
        var imagePointLabel = new JLabel();
        var imagePointTextField = new JTextField();
        var buttonPanel = new JPanel();
        okButton = new JButton();
        var cancelButton = new JButton();
        CellConstraints cc = new CellConstraints();

        //======== this ========
        setTitle("Edit Point");
        setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
        setName("this");
        var contentPane = getContentPane();
        contentPane.setLayout(new BorderLayout());

        //======== dialogPanel ========
        {
            dialogPanel.setBorder(Borders.DIALOG_BORDER);
            dialogPanel.setName("dialogPanel");
            dialogPanel.setLayout(new BorderLayout());

            //======== contentPanel ========
            {
                contentPanel.setName("contentPanel");
                contentPanel.setLayout(new FormLayout(
                        "default, $lcgap, default:grow",
                        "default, $ugap, 3*(default, $lgap), default"));

                //---- instructionsLabel ----
                instructionsLabel.setText("<html>Enter the latitude and longitude that you want the selected image pixel to be mapped to.</html>");
                instructionsLabel.setName("instructionsLabel");
                contentPanel.add(instructionsLabel, cc.xywh(1, 1, 3, 1));

                //---- pointLocationLabel ----
                pointLocationLabel.setText("<html><div align=\"right\">Mapped Location <br>(latitude, longitude):</div></html>");
                pointLocationLabel.setName("pointLocationLabel");
                contentPanel.add(pointLocationLabel, cc.xy(1, 3, CellConstraints.RIGHT, CellConstraints.DEFAULT));

                //---- pointLocationTextField ----
                pointLocationTextField.setName("pointLocationTextField");
                contentPanel.add(pointLocationTextField, cc.xy(3, 3));

                //---- exampleLabel ----
                exampleLabel.setText("Example:");
                exampleLabel.setFont(exampleLabel.getFont().deriveFont(exampleLabel.getFont().getStyle() | Font.ITALIC));
                exampleLabel.setName("exampleLabel");
                contentPanel.add(exampleLabel, cc.xy(1, 5, CellConstraints.RIGHT, CellConstraints.DEFAULT));

                //---- exampleLatLongLabel ----
                exampleLatLongLabel.setText("12.345678, -76.543210");
                exampleLatLongLabel.setFont(exampleLatLongLabel.getFont().deriveFont(exampleLatLongLabel.getFont().getStyle() | Font.ITALIC));
                exampleLatLongLabel.setName("exampleLatLongLabel");
                contentPanel.add(exampleLatLongLabel, cc.xy(3, 5));

                //---- separator1 ----
                separator1.setName("separator1");
                contentPanel.add(separator1, cc.xywh(1, 7, 3, 1));

                //---- imagePointLabel ----
                imagePointLabel.setText("Selected Image Pixel:");
                imagePointLabel.setName("imagePointLabel");
                contentPanel.add(imagePointLabel, cc.xy(1, 9, CellConstraints.RIGHT, CellConstraints.DEFAULT));

                //---- imagePointTextField ----
                imagePointTextField.setOpaque(false);
                imagePointTextField.setEnabled(false);
                imagePointTextField.setName("imagePointTextField");
                imagePointTextField.setText((int) point.getX() + ", " + (int) point.getY());
                contentPanel.add(imagePointTextField, cc.xy(3, 9));
            }
            dialogPanel.add(contentPanel, BorderLayout.CENTER);

            //======== buttonPanel ========
            {
                buttonPanel.setBorder(Borders.BUTTON_BAR_GAP_BORDER);
                buttonPanel.setName("buttonPanel");
                buttonPanel.setLayout(new FormLayout(
                        "$glue, $button, $rgap, $button",
                        "pref"));

                //---- okButton ----
                okButton.setText("OK");
                okButton.setEnabled(false);
                okButton.setName("okButton");
                okButton.addActionListener(e -> okButtonActionPerformed());
                buttonPanel.add(okButton, cc.xy(2, 1));

                //---- cancelButton ----
                cancelButton.setText("Cancel");
                cancelButton.setName("cancelButton");
                cancelButton.addActionListener(e -> cancelButtonActionPerformed());
                buttonPanel.add(cancelButton, cc.xy(4, 1));
            }
            dialogPanel.add(buttonPanel, BorderLayout.SOUTH);
        }
        contentPane.add(dialogPanel, BorderLayout.CENTER);
        setSize(395, 305);
        setLocationRelativeTo(getOwner());
        // JFormDesigner - End of component initialization  //GEN-END:initComponents
    }

    // JFormDesigner - Variables declaration - DO NOT MODIFY  //GEN-BEGIN:variables
    private JTextField pointLocationTextField;
    private JButton okButton;
    // JFormDesigner - End of variables declaration  //GEN-END:variables
}
