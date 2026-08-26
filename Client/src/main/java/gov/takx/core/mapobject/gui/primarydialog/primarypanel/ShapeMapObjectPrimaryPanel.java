package gov.takx.core.mapobject.gui.primarydialog.primarypanel;

import com.jgoodies.forms.factories.Borders;
import com.jgoodies.forms.layout.CellConstraints;
import com.jgoodies.forms.layout.FormLayout;
import gov.takx.api.mapobject.IMapObjectController;

import javax.swing.JButton;
import javax.swing.JPanel;

/**
 * @author Proprietary information subject to the terms of a Non-Disclosure Agreement.
 */
public class ShapeMapObjectPrimaryPanel extends AMapObjectPanel
{
    public ShapeMapObjectPrimaryPanel(IMapObjectController controller)
    {
        super(controller);
        initComponents();
    }

    private void configButtonActionPerformed()
    {
        getMapObjectController().showConfigurationDialog();
    }

    private void initComponents()
    {
        // JFormDesigner - Component initialization - DO NOT MODIFY  //GEN-BEGIN:initComponents
        var buttonPanelPrimaryButtons = new JPanel();
        var configButton = new JButton();
        CellConstraints cc = new CellConstraints();

        //======== this ========
        setLayout(new FormLayout(
                "$lcgap, [285dlu,default]:grow",
                "fill:default"));

        //======== buttonPanelPrimaryButtons ========
        {
            buttonPanelPrimaryButtons.setBorder(Borders.BUTTON_BAR_GAP_BORDER);
            buttonPanelPrimaryButtons.setLayout(new FormLayout(
                    "55dlu",
                    "fill:20dlu:grow"));

            //---- configButton ----
            configButton.setText("Configure");
            configButton.addActionListener(e -> configButtonActionPerformed());
            buttonPanelPrimaryButtons.add(configButton, cc.xy(1, 1));
        }
        add(buttonPanelPrimaryButtons, cc.xy(2, 1));
        // JFormDesigner - End of component initialization  //GEN-END:initComponents
    }

    // JFormDesigner - Variables declaration - DO NOT MODIFY  //GEN-BEGIN:variables
    // JFormDesigner - End of variables declaration  //GEN-END:variables
}
