package gov.takx.core.view.gui.properties;

import com.jgoodies.forms.factories.Borders;
import com.jgoodies.forms.layout.CellConstraints;
import com.jgoodies.forms.layout.FormLayout;
import gov.takx.api.ui.IProgramPreferencesPanel;
import gov.takx.commons.swing.IPanelStateListener;
import gov.takx.commons.swing.PanelListener;
import gov.takx.platform.ui.AStandardDialog;

import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JPanel;

import java.awt.BorderLayout;
import java.util.ArrayList;
import java.util.List;

/**
 * ProjectSettingsDialog class. This is a dialog to hold the project properties panel
 *
 */
public class ProjectSettingsDialog extends AStandardDialog
{
    private final IPanelStateListener panelListener;
    private List<IProgramPreferencesPanel> panelList = new ArrayList<>();

    /**
     * Constructor
     */
    public ProjectSettingsDialog()
    {
        initComponents();
        panelListener = new PanelListener(applyButton);
        initTabs();
        validate();
        panelListener.setPanelClean();
        setLocationRelativeTo(JFrame.getFrames()[0]);
    }

    /**
     * This method closes the panel if all apply actions for any panels have ran.
     */
    private void okButtonActionPerformed()
    {
        if (applyAction())
        {
            dispose();
        }
    }

    /**
     * This method simply closes the dialog
     */
    private void cancelButtonActionPerformed()
    {
        dispose();
    }

    /**
     * This method applies all actions for an contained panels without closing.
     */
    private void applyButtonActionPerformed()
    {
        if (applyAction())
        {
            panelListener.setPanelClean();
        }
    }

    /**
     * This method applies all actions for an contained panels and closes.
     *
     * @return true if all panel apply actions have finished successfully, false otherwise.
     */
    private boolean applyAction()
    {
        for (IProgramPreferencesPanel c : panelList)
        {
            if (!c.applyAction())
            {
                return false;
            }
        }

        return true;
    }

    /**
     * Initializes any panels.
     */
    private void initTabs()
    {
        ProjectSettingsPanel projectPropPanel = new ProjectSettingsPanel();
        projectPropPanel.setPanelListener(panelListener);
        contentPanel.add(projectPropPanel, BorderLayout.CENTER);
        panelList.add(projectPropPanel);
    }

    /**
     * Disposes of the project properties panel and dialog.
     *
     * @since 3.3
     */
    @Override
    protected void onWindowClosing()
    {
        if (panelList != null)
        {
            for (IProgramPreferencesPanel tabPanel : panelList)
            {
                tabPanel.releaseResources();
            }
            panelList.clear();
        }
        panelList = null;
    }

    private void initComponents()
    {
        // JFormDesigner - Component initialization - DO NOT MODIFY  //GEN-BEGIN:initComponents
        var dialogPane = new JPanel();
        contentPanel = new JPanel();
        var buttonBar = new JPanel();
        var okButton = new JButton();
        var cancelButton = new JButton();
        applyButton = new JButton();
        CellConstraints cc = new CellConstraints();

        //======== this ========
        setTitle("Project Settings");
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
                contentPanel.setLayout(new BorderLayout());
            }
            dialogPane.add(contentPanel, BorderLayout.CENTER);

            //======== buttonBar ========
            {
                buttonBar.setBorder(Borders.BUTTON_BAR_GAP_BORDER);
                buttonBar.setName("buttonBar");
                buttonBar.setLayout(new FormLayout(
                        "$glue, $button, $rgap, $button, $lcgap, $button",
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

                //---- applyButton ----
                applyButton.setText("Apply");
                applyButton.setEnabled(false);
                applyButton.setName("applyButton");
                applyButton.addActionListener(e -> applyButtonActionPerformed());
                buttonBar.add(applyButton, cc.xy(6, 1));
            }
            dialogPane.add(buttonBar, BorderLayout.SOUTH);
        }
        contentPane.add(dialogPane, BorderLayout.CENTER);
        pack();
        setLocationRelativeTo(getOwner());
        // JFormDesigner - End of component initialization  //GEN-END:initComponents
    }

    // JFormDesigner - Variables declaration - DO NOT MODIFY  //GEN-BEGIN:variables
    private JPanel contentPanel;
    private JButton applyButton;
    // JFormDesigner - End of variables declaration  //GEN-END:variables
}
