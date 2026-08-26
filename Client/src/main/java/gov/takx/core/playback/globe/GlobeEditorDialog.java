package gov.takx.core.playback.globe;

import com.jgoodies.forms.factories.Borders;
import com.jgoodies.forms.layout.CellConstraints;
import com.jgoodies.forms.layout.FormLayout;
import gov.takx.api.mapobject.IMapObject;
import gov.takx.api.playback.globe.IGlobeModel;
import gov.takx.platform.ui.AStandardDialog;

import javax.swing.JButton;
import javax.swing.JPanel;

import java.awt.BorderLayout;
import java.util.Collection;
import java.util.List;

/**
 */
public class GlobeEditorDialog extends AStandardDialog
{
    private final GlobeEditorPanel panel;
    private boolean okPressed = false;

    public GlobeEditorDialog(String laneName, IGlobeModel globeModel, Collection<IMapObject> currentMapObjects)
    {
        setTitle("Configure " + laneName);
        initComponents();

        panel = new GlobeEditorPanel(globeModel, currentMapObjects);
        contentPanel.add(panel, new CellConstraints(1, 1));
    }

    public List<IMapObject> getSelectedMapObjects()
    {
        return panel.getSelectedMapObjects();
    }

    public boolean isOkPressed()
    {
        return okPressed;
    }

    private void okButtonActionPerformed()
    {
        dispose();
        okPressed = true;
    }

    private void cancelButtonActionPerformed()
    {
        dispose();
    }

    private void initComponents()
    {
        // JFormDesigner - Component initialization - DO NOT MODIFY  //GEN-BEGIN:initComponents
        dialogPane = new JPanel();
        contentPanel = new JPanel();
        buttonBar = new JPanel();
        okButton = new JButton();
        cancelButton = new JButton();
        CellConstraints cc = new CellConstraints();

        //======== this ========
        setModal(true);
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
                        "default:grow",
                        "default:grow"));
            }
            dialogPane.add(contentPanel, BorderLayout.CENTER);

            //======== buttonBar ========
            {
                buttonBar.setBorder(Borders.BUTTON_BAR_GAP_BORDER);
                buttonBar.setName("buttonBar");
                buttonBar.setLayout(new FormLayout(
                        "$glue, $button, $rgap, $button",
                        "pref"));

                //---- okButton ----
                okButton.setText("OK");
                okButton.setName("okButton");
                okButton.addActionListener(e -> okButtonActionPerformed());
                buttonBar.add(okButton, cc.xy(2, 1));

                //---- cancelButton ----
                cancelButton.setText("Cancel");
                cancelButton.setName("cancelButton");
                cancelButton.addActionListener(e -> cancelButtonActionPerformed());
                buttonBar.add(cancelButton, cc.xy(4, 1));
            }
            dialogPane.add(buttonBar, BorderLayout.SOUTH);
        }
        contentPane.add(dialogPane, BorderLayout.CENTER);
        pack();
        setLocationRelativeTo(getOwner());
        // JFormDesigner - End of component initialization  //GEN-END:initComponents
    }

    // JFormDesigner - Variables declaration - DO NOT MODIFY  //GEN-BEGIN:variables
    private JPanel dialogPane;
    private JPanel contentPanel;
    private JPanel buttonBar;
    private JButton okButton;
    private JButton cancelButton;
    // JFormDesigner - End of variables declaration  //GEN-END:variables
}
