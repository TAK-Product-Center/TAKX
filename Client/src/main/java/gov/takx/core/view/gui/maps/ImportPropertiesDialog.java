package gov.takx.core.view.gui.maps;

import com.jgoodies.forms.layout.CellConstraints;
import com.jgoodies.forms.layout.FormLayout;
import gov.takx.platform.ui.AStandardDialog;
import gov.takx.platform.ui.ResourceDirectory;
import gov.takx.platform.ui.UIResources;

import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.border.EmptyBorder;

import java.awt.Dialog;
import java.awt.Dimension;

/**
 * Prompt notifying users of options when importing files.
 *
 * @see  gov.takx.core.view.gui.maps.importers.FileImporter
 * @since 5.7
 */
public class ImportPropertiesDialog extends AStandardDialog
{
    private boolean cancelled;

    public ImportPropertiesDialog()
    {
        initComponents();

        imageLabel.setIcon(UIResources.getFeatureIcon(ResourceDirectory.PLATFORM_ICONS, "imagery_files.png"));
    }

    @Override
    protected void onEscape()
    {
        super.onEscape();

        cancelled = true;
    }

    @Override
    protected void onClosedByXButton()
    {
        cancelled = true;
    }

    /**
     * @return {@code true} when the user has opted to copy to the VFS.
     *        {@code false} to keep the file in its existing location.
     */
    public boolean isCopyToVfs()
    {
        return copyCheck.isSelected();
    }

    /**
     * @return {@code true} when the user has cancelled the operation (x button close, or hitting escape).
     */
    public boolean isCancelled()
    {
        return cancelled;
    }

    private void importPressed()
    {
        setVisible(false);
    }

    private void initComponents()
    {
        // JFormDesigner - Component initialization - DO NOT MODIFY  //GEN-BEGIN:initComponents  @formatter:off
        var headerPanel = new JPanel();
        imageLabel = new JLabel();
        var messageLabel = new JLabel();
        var inputPanel = new JPanel();
        copyCheck = new JCheckBox();
        var importButton = new JButton();
        CellConstraints cc = new CellConstraints();

        //======== this ========
        setTitle("Copy to project?");
        setMaximumSize(new Dimension(500, 400));
        setModalityType(Dialog.ModalityType.APPLICATION_MODAL);
        setPreferredSize(new Dimension(465, 225));
        setName("this");
        var contentPane = getContentPane();
        contentPane.setLayout(new FormLayout(
                "default:grow",
                "top:default:grow, $rgap, default"));

        //======== headerPanel ========
        {
            headerPanel.setBorder(new EmptyBorder(10, 10, 10, 10));
            headerPanel.setMaximumSize(new Dimension(460, 400));
            headerPanel.setName("headerPanel");
            headerPanel.setLayout(new FormLayout(
                    "default, $lcgap, $rgap, $lcgap, default",
                    "default"));

            //---- imageLabel ----
            imageLabel.setName("imageLabel");
            headerPanel.add(imageLabel, cc.xy(1, 1));

            //---- messageLabel ----
            messageLabel.setText("<html>\nDo you want to copy this imagery file to the project?\n<p><p>\nCopying the file to the project allows TAKX to centrally track it along with any other contents. However, on devices with limited storage space you may not want to copy a file. If you do not copy a file to the project it is <i>your responsibility</i> to ensure it stays exactly where it is to ensure TAKX can utilize it properly.</html>");
            messageLabel.setName("messageLabel");
            headerPanel.add(messageLabel, cc.xy(5, 1));
        }
        contentPane.add(headerPanel, cc.xy(1, 1));

        //======== inputPanel ========
        {
            inputPanel.setBorder(new EmptyBorder(10, 10, 10, 10));
            inputPanel.setName("inputPanel");
            inputPanel.setLayout(new FormLayout(
                    "default, $lcgap, default:grow, $lcgap, default",
                    "default"));

            //---- copyCheck ----
            copyCheck.setText("Copy to project");
            copyCheck.setSelected(true);
            copyCheck.setName("copyCheck");
            inputPanel.add(copyCheck, cc.xy(1, 1));

            //---- importButton ----
            importButton.setText("Import");
            importButton.setName("importButton");
            importButton.addActionListener(e -> importPressed());
            inputPanel.add(importButton, cc.xy(5, 1));
        }
        contentPane.add(inputPanel, cc.xy(1, 3));
        pack();
        setLocationRelativeTo(getOwner());
        // JFormDesigner - End of component initialization  //GEN-END:initComponents  @formatter:on
    }

    // JFormDesigner - Variables declaration - DO NOT MODIFY  //GEN-BEGIN:variables  @formatter:off
    private JLabel imageLabel;
    private JCheckBox copyCheck;
    // JFormDesigner - End of variables declaration  //GEN-END:variables  @formatter:on
}
