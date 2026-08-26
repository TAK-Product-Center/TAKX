package gov.takx.core.view.gui;

import com.jgoodies.forms.factories.Borders;
import com.jgoodies.forms.layout.CellConstraints;
import com.jgoodies.forms.layout.FormLayout;
import gov.takx.api.annotation.ProgramPreference;
import gov.takx.api.annotation.PseudoBean;
import gov.takx.commons.swing.AlwaysOnTopJOptionPane;
import gov.takx.core.configuration.ProgramEnvironment;
import gov.takx.platform.preferences.PreferenceEntry;
import gov.takx.platform.ui.AStandardDialog;
import jakarta.annotation.PostConstruct;
import jakarta.inject.Inject;

import javax.swing.ImageIcon;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTextArea;

import java.awt.BorderLayout;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

/**
 * Dialog for displaying information about the application.
 * @since 3.2
 */
@PseudoBean
public class AboutDialog extends AStandardDialog
{
    private int logoClickCount = 0;

    @Inject
    protected ProgramEnvironment programEnvironment;

    @Inject
    @ProgramPreference(PreferenceEntry.NAME)
    String programName;

    @Inject
    @ProgramPreference(PreferenceEntry.APPLICATION_TITLE)
    String appTitle;

    @Inject
    @ProgramPreference(PreferenceEntry.SUPPORT_EMAIL)
    String supportEmail;

    public AboutDialog()
    {
        super(JFrame.getFrames()[0]);
        initComponents();
    }

    @PostConstruct
    private void postConstruct()
    {
        logoLabel.setIcon(new ImageIcon(getClass().getResource("/icons/logo_256.png")));
        setTitle("About " + (appTitle != null ? appTitle : programName));
        infoTextArea.setText(programEnvironment.getProgramProperties() + "\nContact: " + supportEmail);
        pack();
    }

    private void logoLabelMouseClicked()
    {
        logoClickCount++;

        if (logoClickCount % 10 == 0)
        {
            logoClickCount = 0;
            final int dialogResult = AlwaysOnTopJOptionPane.showConfirmDialog(this,
                    "Are you sure you want to unlock developer settings?",
                    "Unlock Developer Settings", JOptionPane.OK_CANCEL_OPTION);

            if (dialogResult == JOptionPane.OK_OPTION) PreferenceEntry.DEV_MODE_SETTINGS_ENABLED.putAndSave(true);
        }
    }

    @SuppressWarnings("ConstantConditions")
    private void initComponents()
    {
        // JFormDesigner - Component initialization - DO NOT MODIFY  //GEN-BEGIN:initComponents
        var dialogPane = new JPanel();
        var contentPanel = new JPanel();
        logoLabel = new JLabel();
        infoTextArea = new JTextArea();
        CellConstraints cc = new CellConstraints();

        //======== this ========
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
                        "default, $lcgap, default:grow",
                        "default"));

                //---- logoLabel ----
                logoLabel.setIcon(null);
                logoLabel.setName("logoLabel");
                logoLabel.addMouseListener(new MouseAdapter()
                {
                    @Override
                    public void mouseClicked(MouseEvent e)
                    {
                        logoLabelMouseClicked();
                    }
                });
                contentPanel.add(logoLabel, cc.xy(1, 1));

                //---- infoTextArea ----
                infoTextArea.setEditable(false);
                infoTextArea.setRows(13);
                infoTextArea.putClientProperty("FlatLaf.styleClass", "monospaced");
                infoTextArea.setName("infoTextArea");
                contentPanel.add(infoTextArea, cc.xy(3, 1));
            }
            dialogPane.add(contentPanel, BorderLayout.CENTER);
        }
        contentPane.add(dialogPane, BorderLayout.CENTER);
        pack();
        setLocationRelativeTo(getOwner());
        // JFormDesigner - End of component initialization  //GEN-END:initComponents
    }

    // JFormDesigner - Variables declaration - DO NOT MODIFY  //GEN-BEGIN:variables
    private JLabel logoLabel;
    private JTextArea infoTextArea;
    // JFormDesigner - End of variables declaration  //GEN-END:variables
}
