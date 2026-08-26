package gov.takx.commons.swing;

import com.jgoodies.forms.factories.Borders;
import com.jgoodies.forms.layout.CellConstraints;
import com.jgoodies.forms.layout.FormLayout;
import gov.takx.platform.ui.AStandardDialog;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;

import java.awt.BorderLayout;
import java.awt.Window;

/**
 * Reusable dialog wrapper used to prompt the user with a provided {@link JComponent} and manage listener callbacks.
 *
 * @since 3.3
 */
@SuppressWarnings("unused")
public class SingleComponentDialog extends AStandardDialog
{
    private final IDialogListener listener;

    /**
     * @param component Component to be wrapped by this dialog.
     * @param listener Listener to receive information of dialog actions.
     */
    public SingleComponentDialog(@Nonnull JComponent component, @Nonnull IDialogListener listener)
    {
        this(component, listener, null);
    }

    /**
     * @param component Component to be wrapped by this dialog.
     * @param listener Listener to receive information of dialog actions.
     * @param title Title to give to the dialog.
     */
    public SingleComponentDialog(@Nonnull JComponent component, @Nonnull IDialogListener listener, @Nullable String title)
    {
        this(component, listener, title, null);
    }

    /**
     * @param component Component to be wrapped by this dialog.
     * @param listener Listener to receive information of dialog actions.
     * @param title Title to give to the dialog.
     * @param description A string description to add to the dialog.
     */
    public SingleComponentDialog(@Nonnull JComponent component, @Nonnull IDialogListener listener, @Nullable String title, @Nullable String description)
    {
        this(null, component, listener, title, description);
    }

    /**
     * @param owner The window to set as the owner of this dialog.
     * @param component Component to be wrapped by this dialog.
     * @param listener Listener to receive information of dialog actions.
     */
    public SingleComponentDialog(@Nullable Window owner, @Nonnull JComponent component, @Nonnull IDialogListener listener)
    {
        this(owner, component, listener, null);
    }

    /**
     * @param owner The window to set as the owner of this dialog.
     * @param component Component to be wrapped by this dialog.
     * @param listener Listener to receive information of dialog actions.
     * @param title Title to give to the dialog.
     */
    public SingleComponentDialog(@Nullable Window owner, @Nonnull JComponent component, @Nonnull IDialogListener listener, @Nullable String title)
    {
        this(owner, component, listener, title, null);
    }

    /**
     * @param owner The window to set as the owner of this dialog.
     * @param component Component to be wrapped by this dialog.
     * @param listener Listener to receive information of dialog actions.
     * @param title Title to give to the dialog.
     * @param description A string description to add to the dialog.
     */
    public SingleComponentDialog(@Nullable Window owner, @Nonnull JComponent component, @Nonnull IDialogListener listener, @Nullable String title, @Nullable String description)
    {
        super(owner);

        initComponents();

        this.listener = listener;

        if (title != null)
        {
            setTitle(title);
        }

        final CellConstraints cc = new CellConstraints();

        if (description != null)
        {
            descriptionLabel.setText(description);
            dialogPane.add(descriptionPanel, BorderLayout.NORTH);
        }

        dialogPane.add(component, BorderLayout.CENTER);

        addCloseHandler(d -> listener.onCancel());
    }

    /**
     * Let the listener know that the "ok" button was clicked and a time was selected.
     */
    private void okButtonActionPerformed()
    {
        listener.onConfirm();
    }

    /**
     * Let the listener know that the "cancel" button was clicked.
     */
    private void cancelButtonActionPerformed()
    {
        dispose();
    }

    private void initComponents()
    {
        // JFormDesigner - Component initialization - DO NOT MODIFY  //GEN-BEGIN:initComponents
        dialogPane = new JPanel();
        var buttonBar = new JPanel();
        var okButton = new JButton();
        var cancelButton = new JButton();
        descriptionPanel = new JPanel();
        descriptionLabel = new JLabel();
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

        //======== descriptionPanel ========
        {
            descriptionPanel.setName("descriptionPanel");
            descriptionPanel.setLayout(new FormLayout(
                    "default",
                    "default, $lgap"));

            //---- descriptionLabel ----
            descriptionLabel.setText("text");
            descriptionLabel.setName("descriptionLabel");
            descriptionPanel.add(descriptionLabel, cc.xy(1, 1));
        }
        // JFormDesigner - End of component initialization  //GEN-END:initComponents
    }

    // JFormDesigner - Variables declaration - DO NOT MODIFY  //GEN-BEGIN:variables
    private JPanel dialogPane;
    private JPanel descriptionPanel;
    private JLabel descriptionLabel;
    // JFormDesigner - End of variables declaration  //GEN-END:variables
}
