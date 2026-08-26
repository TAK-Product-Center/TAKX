package gov.takx.core.view.gui.preferences.network;

import com.jgoodies.forms.factories.Borders;
import com.jgoodies.forms.layout.CellConstraints;
import com.jgoodies.forms.layout.FormLayout;
import gov.tak.core.comms.commo.MeshEncryptionKeyManager;
import gov.takx.api.annotation.PseudoBean;
import gov.takx.commons.swing.AlwaysOnTopJOptionPane;
import gov.takx.platform.ui.AStandardDialog;
import jakarta.inject.Inject;

import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;

import java.awt.BorderLayout;
import java.util.function.BiConsumer;

/**
 * Dialog to offer operations to work with mesh radio encryption keys.
 * @since 5.4
 */
@PseudoBean
public class MeshEncryptionDialog extends AStandardDialog
{
    @Inject
    private MeshEncryptionKeyManager manager;

    private final BiConsumer<String, Boolean> completionConsumer = new CompletionConsumer();

    public MeshEncryptionDialog()
    {
        initComponents();
        SwingUtilities.invokeLater(this::pack);
    }

    private void load()
    {
        manager.loadMeshEncryptionKey(this, completionConsumer);
        dispose();
    }

    private void forget()
    {
        manager.forgetMeshEncryptionKey(completionConsumer);
        dispose();
    }

    private void generate()
    {
        manager.generateMeshEncryptionKey(this, completionConsumer);
        dispose();
    }

    /**
     * Responds when the encryption key options operation is completed. Expects a String completion message and a
     * boolean value of {@code true} if there was an error or {@code false} if the operation completed successfully.
     */
    private class CompletionConsumer implements BiConsumer<String, Boolean>
    {
        @Override
        public void accept(String message, Boolean isError)
        {
            if (message != null)
            {
                if (isError)
                {
                    AlwaysOnTopJOptionPane.showErrorDialog(message, "Operation Failed");
                } else
                {
                    AlwaysOnTopJOptionPane.showMessageDialog(message, "Operation Failed", JOptionPane.INFORMATION_MESSAGE);
                }
                dispose();
            }
        }
    }

    private void initComponents()
    {
        // JFormDesigner - Component initialization - DO NOT MODIFY  //GEN-BEGIN:initComponents  @formatter:off
        var dialogPane = new JPanel();
        var contentPanel = new JPanel();
        var optionsLabel = new JLabel();
        var buttonBar = new JPanel();
        var loadButton = new JButton();
        var forgetButton = new JButton();
        var generateButton = new JButton();
        CellConstraints cc = new CellConstraints();

        //======== this ========
        setTitle("Mesh Encryption");
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
                        "[default,200dlu]:grow",
                        "fill:default:grow"));

                //---- optionsLabel ----
                optionsLabel.setText("<html>Enables encryption of mesh traffic between devices. Once configured, no plaintext traffic will be processed on the mesh network.</html>");
                optionsLabel.setBorder(Borders.DLU4_BORDER);
                optionsLabel.setName("optionsLabel");
                contentPanel.add(optionsLabel, cc.xy(1, 1));
            }
            dialogPane.add(contentPanel, BorderLayout.CENTER);

            //======== buttonBar ========
            {
                buttonBar.setBorder(Borders.createEmptyBorder("5dlu, 0dlu, 0dlu, 0dlu"));
                buttonBar.setName("buttonBar");
                buttonBar.setLayout(new FormLayout(
                        "$glue, 2*($button, $rgap), $button",
                        "pref"));

                //---- loadButton ----
                loadButton.setText("Load Key");
                loadButton.setName("loadButton");
                loadButton.addActionListener(e -> load());
                buttonBar.add(loadButton, cc.xy(2, 1));

                //---- forgetButton ----
                forgetButton.setText("Forget Key");
                forgetButton.setName("forgetButton");
                forgetButton.addActionListener(e -> forget());
                buttonBar.add(forgetButton, cc.xy(4, 1));

                //---- generateButton ----
                generateButton.setText("Generate Key");
                generateButton.setName("generateButton");
                generateButton.addActionListener(e -> generate());
                buttonBar.add(generateButton, cc.xy(6, 1));
            }
            dialogPane.add(buttonBar, BorderLayout.SOUTH);
        }
        contentPane.add(dialogPane, BorderLayout.CENTER);
        pack();
        setLocationRelativeTo(getOwner());
        // JFormDesigner - End of component initialization  //GEN-END:initComponents  @formatter:on
    }

    // JFormDesigner - Variables declaration - DO NOT MODIFY  //GEN-BEGIN:variables  @formatter:off
    // JFormDesigner - End of variables declaration  //GEN-END:variables  @formatter:on
}
