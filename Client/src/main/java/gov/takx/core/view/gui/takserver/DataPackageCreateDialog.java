package gov.takx.core.view.gui.takserver;

import com.jgoodies.forms.factories.Borders;
import com.jgoodies.forms.layout.CellConstraints;
import com.jgoodies.forms.layout.FormLayout;
import gov.tak.api.comms.takserver.IExtendedDataPackageManager;
import gov.takx.api.annotation.PseudoBean;
import gov.takx.platform.ui.AStandardDialog;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import jakarta.inject.Inject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.io.File;
import java.io.IOException;
import java.lang.invoke.MethodHandles;
import java.util.function.Consumer;

/**
 * Data package creation dialog.
 *
 * @since 3.5
 */
@PseudoBean
public class DataPackageCreateDialog extends AStandardDialog
{
    private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    private final Consumer<File> createdPackageHandler;
    private final Runnable windowClosingRunnable;

    @Inject
    private IExtendedDataPackageManager dataPackageManager;

    /**
     * @param createdPackageHandler Optional handler for reacting to the creation of a data package.
     * @param windowClosingRunnable Runnable to call when the dialog is closing
     */
    public DataPackageCreateDialog(@Nullable Consumer<File> createdPackageHandler, @Nonnull Runnable windowClosingRunnable)
    {
        initComponents();
        this.createdPackageHandler = createdPackageHandler;
        this.windowClosingRunnable = windowClosingRunnable;
    }

    @Override
    public void requestFocus()
    {
        inputText.requestFocus();
    }

    @Override
    protected void onWindowClosing()
    {
        windowClosingRunnable.run();
    }

    /**
     * Creates a new data package with the current text of {@link #inputText}.
     */
    private void onCreate()
    {
        String name = inputText.getText().trim();
        try
        {
            File newDataPackage = dataPackageManager.createNewDataPackage(name);
            if (createdPackageHandler != null) createdPackageHandler.accept(newDataPackage);
            setVisible(false);
            dispose();
        } catch (IOException ex)
        {
            logger.error("Failed to create data package '{}'", name, ex);
        }
    }

    /**
     * Called when the user updates the name to provide for the new package.
     * Ensures {@link #onCreate()} cannot be called with text that is blank or conflicts with an existing package.
     * Feedback on disallowed inputs are shown to the user via {@link #feedbackLabel}.
     *
     * @param e Key release event.
     */
    private void inputTextKeyReleased(KeyEvent e)
    {
        // No blank names allowed.
        String packageName = inputText.getText().trim();
        if (packageName.isBlank())
        {
            okButton.setEnabled(false);
            feedbackLabel.setText("Package name cannot be blank");
            return;
        }

        // No existing name conflicts are allowed either.
        File localPackagePath = dataPackageManager.getLocalImportedPackagePath(packageName);
        if (localPackagePath.exists())
        {
            okButton.setEnabled(false);
            feedbackLabel.setText("A local package with that name already exists");
            return;
        }

        // Package can be made
        okButton.setEnabled(true);
        feedbackLabel.setText("");

        // If the user hit enter, treat it the same as pressing the 'create' button.
        if (e.getKeyCode() == KeyEvent.VK_ENTER) onCreate();
    }

    /**
     * Closes this dialog.
     */
    private void cancel()
    {
        setVisible(false);
        dispose();
    }

    private void initComponents()
    {
        // JFormDesigner - Component initialization - DO NOT MODIFY  //GEN-BEGIN:initComponents
        dialogPane = new JPanel();
        contentPanel = new JPanel();
        messageLabel = new JLabel();
        inputText = new JTextField();
        feedbackLabel = new JLabel();
        buttonBar = new JPanel();
        okButton = new JButton();
        cancelButton = new JButton();
        CellConstraints cc = new CellConstraints();

        //======== this ========
        setTitle("Create Data Package");
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
                        "[200dlu,default]:grow",
                        "2*(default, $lgap), 14dlu"));

                //---- messageLabel ----
                messageLabel.setText("Provide a name for the package");
                messageLabel.setName("messageLabel");
                contentPanel.add(messageLabel, cc.xy(1, 1));

                //---- inputText ----
                inputText.setName("inputText");
                inputText.addKeyListener(new KeyAdapter()
                {
                    @Override
                    public void keyReleased(KeyEvent e)
                    {
                        inputTextKeyReleased(e);
                    }
                });
                contentPanel.add(inputText, cc.xy(1, 3));

                //---- feedbackLabel ----
                feedbackLabel.setForeground(Color.red);
                feedbackLabel.setName("feedbackLabel");
                contentPanel.add(feedbackLabel, cc.xy(1, 5));
            }
            dialogPane.add(contentPanel, BorderLayout.CENTER);

            //======== buttonBar ========
            {
                buttonBar.setBorder(Borders.createEmptyBorder("5dlu, 0dlu, 0dlu, 0dlu"));
                buttonBar.setName("buttonBar");
                buttonBar.setLayout(new FormLayout(
                        "$glue, $button, $rgap, $button",
                        "pref"));

                //---- okButton ----
                okButton.setText("Create");
                okButton.setEnabled(false);
                okButton.setName("okButton");
                okButton.addActionListener(e -> onCreate());
                buttonBar.add(okButton, cc.xy(2, 1));

                //---- cancelButton ----
                cancelButton.setText("Cancel");
                cancelButton.setName("cancelButton");
                cancelButton.addActionListener(e -> cancel());
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
    private JPanel dialogPane;
    private JPanel contentPanel;
    private JLabel messageLabel;
    private JTextField inputText;
    private JLabel feedbackLabel;
    private JPanel buttonBar;
    private JButton okButton;
    private JButton cancelButton;
    // JFormDesigner - End of variables declaration  //GEN-END:variables
}
