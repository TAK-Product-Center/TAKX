package gov.takx.core.view.gui.media;

import com.jgoodies.forms.factories.Borders;
import com.jgoodies.forms.layout.CellConstraints;
import com.jgoodies.forms.layout.FormLayout;
import gov.takx.api.media.IMedia;
import gov.takx.api.media.IPlayable;
import gov.takx.platform.ui.AStandardDialog;

import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.border.LineBorder;

import java.awt.BorderLayout;
import java.awt.Color;

/**
 * Display basic media properties
 *
 * @since 2.8
 */
public class MediaPropertiesDialog extends AStandardDialog
{
    public MediaPropertiesDialog(IMedia media)
    {
        initComponents();

        nameField.setText(media.getName());
        urlField.setText(media.getSourceUri().toString());
        if (media instanceof IPlayable && ((IPlayable) media).getDuration() != null)
        {
            durationField.setText("" + ((IPlayable) media).getDuration() + "s");
        } else
        {
            durationField.setVisible(false);
        }
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
        var label1 = new JLabel();
        nameField = new JLabel();
        var label2 = new JLabel();
        urlField = new JLabel();
        var label3 = new JLabel();
        durationField = new JLabel();
        var buttonBar = new JPanel();
        var okButton = new JButton();
        CellConstraints cc = new CellConstraints();

        //======== this ========
        setTitle("Media Properties");
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
                        "2*(default, $lgap), default"));

                //---- label1 ----
                label1.setText("Name:");
                label1.setName("label1");
                contentPanel.add(label1, cc.xy(1, 1));

                //---- nameField ----
                nameField.setText("text");
                nameField.setBorder(new LineBorder(Color.gray, 1, true));
                nameField.setName("nameField");
                contentPanel.add(nameField, cc.xy(3, 1));

                //---- label2 ----
                label2.setText("URL:");
                label2.setName("label2");
                contentPanel.add(label2, cc.xy(1, 3));

                //---- urlField ----
                urlField.setText("text");
                urlField.setBorder(new LineBorder(Color.gray, 1, true));
                urlField.setName("urlField");
                contentPanel.add(urlField, cc.xy(3, 3));

                //---- label3 ----
                label3.setText("Duration:");
                label3.setName("label3");
                contentPanel.add(label3, cc.xy(1, 5));

                //---- durationField ----
                durationField.setText("text");
                durationField.setBorder(new LineBorder(Color.gray, 1, true));
                durationField.setName("durationField");
                contentPanel.add(durationField, cc.xy(3, 5));
            }
            dialogPane.add(contentPanel, BorderLayout.CENTER);

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
    private JLabel nameField;
    private JLabel urlField;
    private JLabel durationField;
    // JFormDesigner - End of variables declaration  //GEN-END:variables
}
