package gov.takx.core.view.gui.takserver;

import com.jgoodies.forms.factories.Borders;
import com.jgoodies.forms.layout.CellConstraints;
import com.jgoodies.forms.layout.FormLayout;
import gov.tak.api.comms.takserver.IDataSyncManager;
import gov.tak.api.comms.takserver.ITakServerConnection;
import gov.tak.api.comms.takserver.mission.data.Mission;
import gov.tak.api.contact.IContactNameResolver;
import gov.takx.platform.ui.AStandardDialog;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPasswordField;

import java.awt.BorderLayout;
import java.awt.Window;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.lang.invoke.MethodHandles;
import java.util.Objects;
import java.util.function.Consumer;

/**
 * A dialog to prompt a user to download or subscribe to a mission.
 *
 * @since 5.0
 */
public class DataFeedDownloadDialog extends AStandardDialog
{
    private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    private final IDataSyncManager dataSyncManager;
    private final ITakServerConnection connection;
    private final Mission mission;
    private final Consumer<IDataSyncManager.MissionDownload> onSuccess;
    private final Consumer<IDataSyncManager.MissionDownload> onFailure;

    /**
     * @param owner Dialog owner, can be {@code null} to set the main frame as the owner.
     * @param dataSyncManager Sync manager to handle download calls with.
     * @param contactNameResolver Name resolver to identify a feed's creator with.
     * @param connection TAK Server connection where the feed is from.
     * @param mission The mission <i>(data feed)</i> to download.
     * @param onSuccess Optional handler for download successes.
     * @param onFailure Optional handler for download failures.
     */
    public DataFeedDownloadDialog(@Nullable Window owner, @Nonnull IDataSyncManager dataSyncManager,
                                  @Nonnull IContactNameResolver contactNameResolver,
                                  @Nonnull ITakServerConnection connection, @Nonnull Mission mission,
                                  @Nullable Consumer<IDataSyncManager.MissionDownload> onSuccess,
                                  @Nullable Consumer<IDataSyncManager.MissionDownload> onFailure)
    {
        super(owner);
        this.dataSyncManager = dataSyncManager;
        this.connection = connection;
        this.mission = mission;
        this.onSuccess = onSuccess;
        this.onFailure = onFailure;

        initComponents();

        // Update labels.
        String creator = Objects.requireNonNullElse(contactNameResolver.resolveName(mission.creatorUid()), "Unknown");
        serverLabel.setText(connection.getUri().toString());
        nameLabel.setText(mission.name());
        descriptionLabel.setText(mission.description());
        createdByLabel.setText(creator);

        // For missions with password protection, the password input field is enabled and required to download/subscribe to the feed.
        // Since that is the default UI state, missions without password protection should hide the password input field and enable the download buttons.
        if (!mission.passwordProtected())
        {
            passwordPrefix.setVisible(false);
            passwordTextField.setVisible(false);
            downloadButton.setEnabled(true);
            downloadAndSyncButton.setEnabled(true);
        }
    }

    /**
     * Downloads the feed and its associated resource, without subscribing to updates.
     */
    private void download()
    {
        handleDownloadAndSync(false);
    }

    /**
     * Downloads and subscribes to the feed.
     */
    private void downloadAndSync()
    {
        handleDownloadAndSync(true);
    }

    /**
     * @param sync {@code true} to stay synced with the feed's changes after downloading.
     */
    private void handleDownloadAndSync(boolean sync)
    {
        String name = mission.name();
        String password = mission.passwordProtected() ? new String(passwordTextField.getPassword()) : null;
        var future = sync ? dataSyncManager.subscribe(connection, name, password) : dataSyncManager.download(connection, name, password);
        future.whenComplete((downloaded, error) -> handleDownload(downloaded, error, sync ? "sync" : "download", name));
        dispose();
    }

    /**
     * Closes the dialog.
     */
    private void cancel()
    {
        dispose();
    }

    private void passwordUpdated()
    {
        // Password protected missions require password input before allowing the request to go through.
        boolean hasInput = passwordTextField.getPassword().length > 0;
        downloadButton.setEnabled(hasInput);
        downloadAndSyncButton.setEnabled(hasInput);
    }

    private void handleDownload(@Nullable IDataSyncManager.MissionDownload downloaded, @Nullable Throwable error, @Nonnull String verb, @Nonnull String name)
    {
        if (downloaded != null)
        {
            // Download was either a success, or failure where we can somewhat identify what went wrong.
            if (downloaded.wasSuccess() && onSuccess != null)
            {
                onSuccess.accept(downloaded);
            } else if (onFailure != null)
            {
                onFailure.accept(downloaded);
            }
        } else if (error != null)
        {
            // This handles uncaught exceptions, which we have not accounted for any cannot be sure what happened.
            // Things like a NPE in the sync API rather than a problem addressable by the user like a lack of internet connection.
            String title = "Failed to " + verb + " feed '" + name + "'";
            String message = "Refer to takx.log for details of the failure";
            logger.error(title, error);
            JOptionPane.showMessageDialog(this, title, message, JOptionPane.ERROR_MESSAGE);
        }
    }

    private void initComponents()
    {
        // JFormDesigner - Component initialization - DO NOT MODIFY  //GEN-BEGIN:initComponents
        dialogPane = new JPanel();
        contentPanel = new JPanel();
        namePrefix = new JLabel();
        nameLabel = new JLabel();
        serverPrefix = new JLabel();
        serverLabel = new JLabel();
        descriptionPrefix = new JLabel();
        descriptionLabel = new JLabel();
        createdByPrefix = new JLabel();
        createdByLabel = new JLabel();
        passwordPrefix = new JLabel();
        passwordTextField = new JPasswordField();
        buttonBar = new JPanel();
        downloadAndSyncButton = new JButton();
        downloadButton = new JButton();
        cancelButton = new JButton();
        CellConstraints cc = new CellConstraints();

        //======== this ========
        setTitle("Download Feed");
        setName("this");
        var contentPane = getContentPane();
        contentPane.setLayout(new BorderLayout());

        //======== dialogPane ========
        {
            dialogPane.setBorder(Borders.createEmptyBorder("7dlu, 7dlu, 7dlu, 7dlu"));
            dialogPane.setName("dialogPane");
            dialogPane.setLayout(new FormLayout(
                    "default:grow",
                    "fill:default:grow, fill:default"));

            //======== contentPanel ========
            {
                contentPanel.setName("contentPanel");
                contentPanel.setLayout(new FormLayout(
                        "left:default, 10dlu, default:grow",
                        "4*([12dlu,default], $lgap), [19dlu,default]"));

                //---- namePrefix ----
                namePrefix.setText("Name:");
                namePrefix.setName("namePrefix");
                contentPanel.add(namePrefix, cc.xy(1, 1));

                //---- nameLabel ----
                nameLabel.setName("nameLabel");
                contentPanel.add(nameLabel, cc.xy(3, 1));

                //---- serverPrefix ----
                serverPrefix.setText("Server:");
                serverPrefix.setName("serverPrefix");
                contentPanel.add(serverPrefix, cc.xy(1, 3));

                //---- serverLabel ----
                serverLabel.setName("serverLabel");
                contentPanel.add(serverLabel, cc.xy(3, 3));

                //---- descriptionPrefix ----
                descriptionPrefix.setText("Description:");
                descriptionPrefix.setName("descriptionPrefix");
                contentPanel.add(descriptionPrefix, cc.xy(1, 5));

                //---- descriptionLabel ----
                descriptionLabel.setName("descriptionLabel");
                contentPanel.add(descriptionLabel, cc.xy(3, 5));

                //---- createdByPrefix ----
                createdByPrefix.setText("Created by:");
                createdByPrefix.setName("createdByPrefix");
                contentPanel.add(createdByPrefix, cc.xy(1, 7));

                //---- createdByLabel ----
                createdByLabel.setName("createdByLabel");
                contentPanel.add(createdByLabel, cc.xy(3, 7));

                //---- passwordPrefix ----
                passwordPrefix.setText("Password:");
                passwordPrefix.setName("passwordPrefix");
                contentPanel.add(passwordPrefix, cc.xy(1, 9));

                //---- passwordTextField ----
                passwordTextField.setName("passwordTextField");
                passwordTextField.addKeyListener(new KeyAdapter()
                {
                    @Override
                    public void keyReleased(KeyEvent e)
                    {
                        passwordUpdated();
                    }
                });
                contentPanel.add(passwordTextField, cc.xy(3, 9));
            }
            dialogPane.add(contentPanel, cc.xy(1, 1));

            //======== buttonBar ========
            {
                buttonBar.setBorder(Borders.createEmptyBorder("5dlu, 0dlu, 0dlu, 0dlu"));
                buttonBar.setName("buttonBar");
                buttonBar.setLayout(new FormLayout(
                        "$glue, $lcgap, 2*($button, 4dlu), $button",
                        "pref"));

                //---- downloadAndSyncButton ----
                downloadAndSyncButton.setText("Download and stay synced");
                downloadAndSyncButton.setEnabled(false);
                downloadAndSyncButton.setName("downloadAndSyncButton");
                downloadAndSyncButton.addActionListener(e -> downloadAndSync());
                buttonBar.add(downloadAndSyncButton, cc.xy(3, 1));

                //---- downloadButton ----
                downloadButton.setText("Download once");
                downloadButton.setEnabled(false);
                downloadButton.setName("downloadButton");
                downloadButton.addActionListener(e -> download());
                buttonBar.add(downloadButton, cc.xy(5, 1));

                //---- cancelButton ----
                cancelButton.setText("Cancel");
                cancelButton.setName("cancelButton");
                cancelButton.addActionListener(e -> cancel());
                buttonBar.add(cancelButton, cc.xy(7, 1));
            }
            dialogPane.add(buttonBar, cc.xy(1, 2));
        }
        contentPane.add(dialogPane, BorderLayout.CENTER);
        pack();
        setLocationRelativeTo(getOwner());
        // JFormDesigner - End of component initialization  //GEN-END:initComponents
    }

    // JFormDesigner - Variables declaration - DO NOT MODIFY  //GEN-BEGIN:variables
    private JPanel dialogPane;
    private JPanel contentPanel;
    private JLabel namePrefix;
    private JLabel nameLabel;
    private JLabel serverPrefix;
    private JLabel serverLabel;
    private JLabel descriptionPrefix;
    private JLabel descriptionLabel;
    private JLabel createdByPrefix;
    private JLabel createdByLabel;
    private JLabel passwordPrefix;
    private JPasswordField passwordTextField;
    private JPanel buttonBar;
    private JButton downloadAndSyncButton;
    private JButton downloadButton;
    private JButton cancelButton;
    // JFormDesigner - End of variables declaration  //GEN-END:variables
}
