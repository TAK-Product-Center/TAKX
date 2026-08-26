package gov.takx.core.view.gui.contentcleaner;

import com.jgoodies.forms.factories.Borders;
import com.jgoodies.forms.layout.CellConstraints;
import com.jgoodies.forms.layout.FormLayout;
import gov.takx.api.annotation.PseudoBean;
import gov.takx.core.contentcleaner.ContentCleaner;
import gov.takx.platform.ui.AStandardDialog;
import gov.takx.platform.ui.UIResources;
import jakarta.inject.Inject;

import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JToggleButton;
import javax.swing.SwingConstants;

import java.awt.BorderLayout;
import java.awt.Color;

/**
 * Displays the clear content options and calls the {@link ContentCleaner} if the user chooses to clear the app data.
 *
 * @since 5.1
 */
@PseudoBean
public class ContentCleanerDialog extends AStandardDialog
{
    @Inject
    private ContentCleaner cleaner;

    public ContentCleanerDialog()
    {
        initComponents();
        setIconImage(UIResources.DELETE_SWEEP_ICON.getImage());

        lockButton1.setIcon(UIResources.UNLOCKED_ICON);
        lockButton1.setSelectedIcon(UIResources.LOCKED_ICON);
        lockButton2.setIcon(UIResources.UNLOCKED_ICON);
        lockButton2.setSelectedIcon(UIResources.LOCKED_ICON);
        clearContentButton.setIcon(UIResources.DELETE_SWEEP_ICON);
    }

    private void lockButtonClicked()
    {
        // Both toggle buttons must be selected to enable the clear button
        clearContentButton.setEnabled(lockButton1.isSelected() && lockButton2.isSelected());
    }

    private void clearContent()
    {
        dispose();
        cleaner.clean(mapCacheCheckBox.isSelected());
    }

    private void initComponents()
    {
        // JFormDesigner - Component initialization - DO NOT MODIFY  //GEN-BEGIN:initComponents  @formatter:off
        var contentPanel = new JPanel();
        var warningLabel = new JLabel();
        var infoLabel = new JLabel();
        mapCacheCheckBox = new JCheckBox();
        var lockInfoLabel = new JLabel();
        var lockPanel = new JPanel();
        lockButton2 = new JToggleButton();
        lockButton1 = new JToggleButton();
        clearContentButton = new JButton();
        CellConstraints cc = new CellConstraints();

        //======== this ========
        setTitle("Clear Content?");
        setName("this");
        var contentPane = getContentPane();
        contentPane.setLayout(new BorderLayout());

        //======== contentPanel ========
        {
            contentPanel.setBorder(Borders.createEmptyBorder("5dlu, 5dlu, 5dlu, 5dlu"));
            contentPanel.setName("contentPanel");
            contentPanel.setLayout(new FormLayout(
                    "[default,250dlu]:grow",
                    "3*(default, $ugap), default, $lgap, default"));

            //---- warningLabel ----
            warningLabel.setText("Warning: All data will be permanantly erased");
            warningLabel.setForeground(Color.red);
            warningLabel.setHorizontalAlignment(SwingConstants.CENTER);
            warningLabel.setName("warningLabel");
            contentPanel.add(warningLabel, cc.xy(1, 1));

            //---- infoLabel ----
            infoLabel.setText("<html>Clearing content will permanently delete all data stored in the ProjectData directory and shut down the application. Default configurations will be restored when the application is restarted. This operation cannot be undone.</html>");
            infoLabel.setName("infoLabel");
            contentPanel.add(infoLabel, cc.xy(1, 3));

            //---- mapCacheCheckBox ----
            mapCacheCheckBox.setText("Clear maps & imagery");
            mapCacheCheckBox.setToolTipText("Not yet supported");
            mapCacheCheckBox.setName("mapCacheCheckBox");
            contentPanel.add(mapCacheCheckBox, cc.xy(1, 5));

            //---- lockInfoLabel ----
            lockInfoLabel.setText("Lock both buttons to enable");
            lockInfoLabel.setHorizontalAlignment(SwingConstants.CENTER);
            lockInfoLabel.setName("lockInfoLabel");
            contentPanel.add(lockInfoLabel, cc.xy(1, 7));

            //======== lockPanel ========
            {
                lockPanel.setName("lockPanel");
                lockPanel.setLayout(new FormLayout(
                        "2*(24dlu, $lcgap), default:grow",
                        "default"));

                //---- lockButton2 ----
                lockButton2.setName("lockButton2");
                lockButton2.addActionListener(e -> lockButtonClicked());
                lockPanel.add(lockButton2, cc.xy(1, 1));

                //---- lockButton1 ----
                lockButton1.setName("lockButton1");
                lockButton1.addActionListener(e -> lockButtonClicked());
                lockPanel.add(lockButton1, cc.xy(3, 1));

                //---- clearContentButton ----
                clearContentButton.setText("Clear Content");
                clearContentButton.setEnabled(false);
                clearContentButton.setToolTipText("Lock both switches to enable");
                clearContentButton.setName("clearContentButton");
                clearContentButton.addActionListener(e -> clearContent());
                lockPanel.add(clearContentButton, cc.xy(5, 1, CellConstraints.CENTER, CellConstraints.FILL));
            }
            contentPanel.add(lockPanel, cc.xy(1, 9, CellConstraints.CENTER, CellConstraints.DEFAULT));
        }
        contentPane.add(contentPanel, BorderLayout.CENTER);
        pack();
        setLocationRelativeTo(getOwner());
        // JFormDesigner - End of component initialization  //GEN-END:initComponents  @formatter:on
    }

    // JFormDesigner - Variables declaration - DO NOT MODIFY  //GEN-BEGIN:variables  @formatter:off
    private JCheckBox mapCacheCheckBox;
    private JToggleButton lockButton2;
    private JToggleButton lockButton1;
    private JButton clearContentButton;
    // JFormDesigner - End of variables declaration  //GEN-END:variables  @formatter:on
}
