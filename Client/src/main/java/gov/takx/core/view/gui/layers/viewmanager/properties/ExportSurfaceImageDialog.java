package gov.takx.core.view.gui.layers.viewmanager.properties;

import com.jgoodies.forms.factories.Borders;
import com.jgoodies.forms.layout.CellConstraints;
import com.jgoodies.forms.layout.FormLayout;
import gov.takx.platform.ui.AStandardDialog;

import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextArea;

import java.awt.BorderLayout;

/**
 */
class ExportSurfaceImageDialog extends AStandardDialog
{
    public ExportSurfaceImageDialog(String imageFile, String geoFile)
    {
        initComponents();
        imageLabel.setText(imageFile);
        geoFileLabel.setText(geoFile);
    }

    private void okButtonActionPerformed()
    {
        dispose();
    }

    private void initComponents()
    {
        // JFormDesigner - Component initialization - DO NOT MODIFY  //GEN-BEGIN:initComponents
        var dialogPane = new JPanel();
        var contentPanel = new JPanel();
        var destinationLabel = new JLabel();
        imageLabel = new JLabel();
        var label1 = new JLabel();
        geoFileLabel = new JLabel();
        var label2 = new JTextArea();
        var buttonBar = new JPanel();
        var okButton = new JButton();
        CellConstraints cc = new CellConstraints();

        //======== this ========
        setTitle("Export Complete");
        setName("this");
        var contentPane = getContentPane();
        contentPane.setLayout(new BorderLayout());

        //======== dialogPane ========
        {
            dialogPane.setBorder(Borders.DIALOG_BORDER);
            dialogPane.setName("dialogPane");
            dialogPane.setLayout(new BorderLayout());

            //======== contentPanel ========
            {
                contentPanel.setName("contentPanel");
                contentPanel.setLayout(new FormLayout(
                        "left:default, $lcgap, default:grow",
                        "default, $lgap, default, $ugap, default"));

                //---- destinationLabel ----
                destinationLabel.setText("Image file:");
                destinationLabel.setName("destinationLabel");
                contentPanel.add(destinationLabel, cc.xy(1, 1));

                //---- imageLabel ----
                imageLabel.setText("text");
                imageLabel.setName("imageLabel");
                contentPanel.add(imageLabel, cc.xy(3, 1));

                //---- label1 ----
                label1.setText("Geographic information file:");
                label1.setName("label1");
                contentPanel.add(label1, cc.xy(1, 3));

                //---- geoFileLabel ----
                geoFileLabel.setText("text");
                geoFileLabel.setName("geoFileLabel");
                contentPanel.add(geoFileLabel, cc.xy(3, 3));

                //---- label2 ----
                label2.setRows(2);
                label2.setFocusable(false);
                label2.setEditable(false);
                label2.setWrapStyleWord(true);
                label2.setOpaque(false);
                label2.setLineWrap(true);
                label2.setText("The Geographic Information File contains the location of the image on the map. Make sure that this file exists in the same directory as the image file when the image is imported elsewhere.");
                label2.setName("label2");
                contentPanel.add(label2, cc.xywh(1, 5, 3, 1));
            }
            dialogPane.add(contentPanel, BorderLayout.NORTH);

            //======== buttonBar ========
            {
                buttonBar.setBorder(Borders.BUTTON_BAR_GAP_BORDER);
                buttonBar.setName("buttonBar");
                buttonBar.setLayout(new FormLayout(
                        "$glue, $button",
                        "pref"));

                //---- okButton ----
                okButton.setText("OK");
                okButton.setName("okButton");
                okButton.addActionListener(e -> okButtonActionPerformed());
                buttonBar.add(okButton, cc.xy(2, 1));
            }
            dialogPane.add(buttonBar, BorderLayout.SOUTH);
        }
        contentPane.add(dialogPane, BorderLayout.CENTER);
        pack();
        setLocationRelativeTo(getOwner());
        // JFormDesigner - End of component initialization  //GEN-END:initComponents
    }

    // JFormDesigner - Variables declaration - DO NOT MODIFY  //GEN-BEGIN:variables
    private JLabel imageLabel;
    private JLabel geoFileLabel;
    // JFormDesigner - End of variables declaration  //GEN-END:variables
}
