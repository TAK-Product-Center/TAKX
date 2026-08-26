package gov.takx.core.view.gui.maps.exporters.dataset;

import com.jgoodies.forms.factories.Borders;
import com.jgoodies.forms.layout.CellConstraints;
import com.jgoodies.forms.layout.FormLayout;
import gov.takx.platform.ui.AStandardDialog;

import javax.swing.JButton;
import javax.swing.JPanel;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;

import java.awt.BorderLayout;
import java.awt.Dialog;

/**
 * Dialog that allows the user to select a map set name and an export destination.
 *
 * @since 1.43
 */
public class MapSetExportFileSelectionDialog extends AStandardDialog
{
    private static final String TITLE = "Build Map Set";

    public enum Action
    {
        SAVE,
        CANCEL
    }

    private Action retCode = Action.CANCEL;

    public MapSetExportFileSelectionDialog(Dialog owner, long size)
    {
        super(owner);
        setTitle(TITLE);
        initComponents();

        destinationPanel1.getSizeLabel().setText(MapBuilderUtils.getByteString(size));

        DocumentListener listener = buildListener();
        destinationPanel1.getTextFieldDir().getDocument().addDocumentListener(listener);
        destinationPanel1.getTextFieldName().getDocument().addDocumentListener(listener);

        updateBuildButtonState();
    }

    public String getDir()
    {
        return destinationPanel1.getDir();
    }

    public String getDestinationName()
    {
        return destinationPanel1.getDestinationName();
    }

    public Action getRetCode()
    {
        return retCode;
    }

    private void cancelButtonActionPerformed()
    {
        retCode = Action.CANCEL;
        performDialogClose();
    }

    private void okButtonActionPerformed()
    {
        retCode = Action.SAVE;
        performDialogClose();
    }

    private void performDialogClose()
    {
        setVisible(false);
        dispose();
    }

    private DocumentListener buildListener()
    {
        return new DocumentListener()
        {
            @Override
            public void insertUpdate(DocumentEvent e)
            {
                updateBuildButtonState();
            }

            @Override
            public void removeUpdate(DocumentEvent e)
            {
                updateBuildButtonState();
            }

            @Override
            public void changedUpdate(DocumentEvent e)
            {
                updateBuildButtonState();
            }
        };
    }

    private void updateBuildButtonState()
    {
        buildButton.setEnabled(destinationPanel1.isValidEntries());
    }

    private void initComponents()
    {
        // JFormDesigner - Component initialization - DO NOT MODIFY  //GEN-BEGIN:initComponents
        var dialogPane = new JPanel();
        var contentPanel = new JPanel();
        destinationPanel1 = new DestinationPanel();
        var buttonBar = new JPanel();
        buildButton = new JButton();
        var cancelButton = new JButton();
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
                        "$lcgap, default:grow, $lcgap",
                        "$lgap, default, $lgap"));

                //---- destinationPanel1 ----
                destinationPanel1.setName("destinationPanel1");
                contentPanel.add(destinationPanel1, cc.xy(2, 2));
            }
            dialogPane.add(contentPanel, BorderLayout.CENTER);

            //======== buttonBar ========
            {
                buttonBar.setBorder(Borders.BUTTON_BAR_GAP_BORDER);
                buttonBar.setName("buttonBar");
                buttonBar.setLayout(new FormLayout(
                        "$glue, $button, $rgap, $button",
                        "pref"));

                //---- buildButton ----
                buildButton.setText("Build");
                buildButton.setName("buildButton");
                buildButton.addActionListener(e -> okButtonActionPerformed());
                buttonBar.add(buildButton, cc.xy(2, 1));

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
    private DestinationPanel destinationPanel1;
    private JButton buildButton;
    // JFormDesigner - End of variables declaration  //GEN-END:variables
}
