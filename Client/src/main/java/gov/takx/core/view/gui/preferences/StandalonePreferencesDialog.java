package gov.takx.core.view.gui.preferences;

import com.jgoodies.forms.factories.Borders;
import com.jgoodies.forms.layout.CellConstraints;
import com.jgoodies.forms.layout.FormLayout;
import gov.takx.api.ui.IProgramPreferencesPanel;
import gov.takx.commons.swing.PanelListener;
import gov.takx.platform.ui.AStandardDialog;
import jakarta.annotation.Nonnull;

import javax.swing.JButton;
import javax.swing.JPanel;
import javax.swing.border.EmptyBorder;

import java.awt.BorderLayout;
import java.awt.Component;

/**
 * Dialog to display an arbitrary preferences panel with ok/cancel/apply buttons.  This is primarily needed to
 * re-host some complicated panels originally in the old preferences dialog that can't really be changed to follow
 * the new "immediate edit" handling of single preference values.
 * 
 * @since 5.4
 */
public class StandalonePreferencesDialog extends AStandardDialog
{
    private final IProgramPreferencesPanel preferencePanel;
    private final PanelListener panelListener;

    /**
     * @param title Dialog title
     * @param preferencePanel Preference panel to show
     */
    public StandalonePreferencesDialog(@Nonnull String title, @Nonnull IProgramPreferencesPanel preferencePanel)
    {
        initComponents();
        setTitle(title);
        this.preferencePanel = preferencePanel;

        panelListener = new PanelListener(applyButton);
        preferencePanel.setPanelListener(panelListener);

        addResourceCleaner(preferencePanel);
        contentPanel.add((Component) preferencePanel, BorderLayout.CENTER);

        pack();
    }

    private void apply()
    {
        if (preferencePanel.applyAction()) panelListener.setPanelClean();
    }

    private void ok()
    {
        if (preferencePanel.applyAction()) dispose();
    }

    private void cancel()
    {
        dispose();
    }

    @SuppressWarnings("DuplicatedCode")
    private void initComponents()
    {
        // JFormDesigner - Component initialization - DO NOT MODIFY  //GEN-BEGIN:initComponents  @formatter:off
        var dialogPane = new JPanel();
        contentPanel = new JPanel();
        var buttonBar = new JPanel();
        var okButton = new JButton();
        var cancelButton = new JButton();
        applyButton = new JButton();
        CellConstraints cc = new CellConstraints();

        //======== this ========
        setTitle("title");
        setModal(true);
        setName("this");
        var contentPane = getContentPane();
        contentPane.setLayout(new BorderLayout());

        //======== dialogPane ========
        {
            dialogPane.setBorder(new EmptyBorder(12, 12, 12, 12));
            dialogPane.setName("dialogPane");
            dialogPane.setLayout(new BorderLayout());

            //======== contentPanel ========
            {
                contentPanel.setName("contentPanel");
                contentPanel.setLayout(new BorderLayout());

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
                    okButton.addActionListener(e -> ok());
                    buttonBar.add(okButton, cc.xy(2, 1));

                    //---- cancelButton ----
                    cancelButton.setText("Cancel");
                    cancelButton.setName("cancelButton");
                    cancelButton.addActionListener(e -> cancel());
                    buttonBar.add(cancelButton, cc.xy(4, 1));

                    //---- applyButton ----
                    applyButton.setText("Apply");
                    applyButton.setEnabled(false);
                    applyButton.setName("applyButton");
                    applyButton.addActionListener(e -> apply());
                    buttonBar.add(applyButton, cc.xy(6, 1));
                }
                contentPanel.add(buttonBar, BorderLayout.SOUTH);
            }
            dialogPane.add(contentPanel, BorderLayout.CENTER);
        }
        contentPane.add(dialogPane, BorderLayout.CENTER);
        pack();
        setLocationRelativeTo(getOwner());
        // JFormDesigner - End of component initialization  //GEN-END:initComponents  @formatter:on
    }

    // JFormDesigner - Variables declaration - DO NOT MODIFY  //GEN-BEGIN:variables  @formatter:off
    private JPanel contentPanel;
    private JButton applyButton;
    // JFormDesigner - End of variables declaration  //GEN-END:variables  @formatter:on
}
