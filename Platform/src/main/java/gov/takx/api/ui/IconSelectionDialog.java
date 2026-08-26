package gov.takx.api.ui;

import com.jgoodies.forms.factories.Borders;
import com.jgoodies.forms.layout.CellConstraints;
import com.jgoodies.forms.layout.FormLayout;
import gov.takx.api.plugin.IPluginDelegate;
import gov.takx.api.registry.IIconSelectionListener;
import gov.takx.platform.ui.AStandardDialog;

import javax.swing.JButton;
import javax.swing.JPanel;

import java.awt.BorderLayout;
import java.awt.Dialog;
import java.awt.Frame;

/**
 * Dialog plugins can use to prompt the user to select an icon. See {@link AIconSelectionPanel} for more info.
 *
 */
@SuppressWarnings("unused") // Called by plugins
public class IconSelectionDialog extends AStandardDialog implements IIconSelectionListener
{
    private IIconSelectionListener selectionListener;
    private String iconPath;

    public IconSelectionDialog(Frame owner, IPluginDelegate delegate)
    {
        super(owner);
        init();
    }

    public IconSelectionDialog(Dialog owner, IPluginDelegate delegate)
    {
        super(owner);
        init();
    }

    private void init()
    {
        initComponents();
        iconSelectionPanel.setIconSelectionListener(this);
    }

    public void setIconSelectionListener(IIconSelectionListener selectionListener)
    {
        this.selectionListener = selectionListener;
    }

    @Override
    public void iconSelected(String iconPath)
    {
        if (!okButton.isEnabled())
        {
            okButton.setEnabled(true);
        }
        this.iconPath = iconPath;
        if (selectionListener != null)
        {
            selectionListener.iconSelected(iconPath);
        }
    }

    private void okButtonActionPerformed()
    {
        if (selectionListener != null)
        {
            selectionListener.iconSelected(iconPath);
        }
        dispose();
    }

    private void cancelButtonActionPerformed()
    {
        setVisible(false);
        dispose();
    }

    private void initComponents()
    {
        // JFormDesigner - Component initialization - DO NOT MODIFY  //GEN-BEGIN:initComponents
        var dialogPane = new JPanel();
        var contentPanel = new JPanel();
        iconSelectionPanel = new IconSelectionPanel();
        var buttonBar = new JPanel();
        okButton = new JButton();
        var cancelButton = new JButton();
        CellConstraints cc = new CellConstraints();

        //======== this ========
        setTitle("Select an Icon");
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
                        "$rgap, fill:default:grow"));

                //---- iconSelectionPanel ----
                iconSelectionPanel.setName("iconSelectionPanel");
                contentPanel.add(iconSelectionPanel, cc.xy(1, 2));
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
                okButton.setText("Ok");
                okButton.setEnabled(false);
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
    private IconSelectionPanel iconSelectionPanel;
    private JButton okButton;
    // JFormDesigner - End of variables declaration  //GEN-END:variables
}
