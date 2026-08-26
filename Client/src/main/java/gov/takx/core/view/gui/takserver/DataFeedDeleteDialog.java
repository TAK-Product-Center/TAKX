package gov.takx.core.view.gui.takserver;

import com.jgoodies.forms.factories.Borders;
import com.jgoodies.forms.layout.CellConstraints;
import com.jgoodies.forms.layout.FormLayout;
import gov.tak.api.comms.takserver.IDataSyncManager;
import gov.tak.api.comms.takserver.ITakServerConnection;
import gov.tak.api.comms.takserver.mission.data.Mission;
import gov.takx.platform.ui.AStandardDialog;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.border.EmptyBorder;

import java.awt.BorderLayout;
import java.awt.Window;
import java.lang.invoke.MethodHandles;
import java.util.function.Consumer;

/**
 * A dialog to prompt a user to delete a mission.
 *
 * @since 5.0
 */
public class DataFeedDeleteDialog extends AStandardDialog
{
    private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    private final IDataSyncManager dataSyncManager;
    private final ITakServerConnection connection;
    private final Mission mission;
    private final Consumer<IDataSyncManager.MissionDeletion> onSuccess;
    private final Consumer<IDataSyncManager.MissionDeletion> onFailure;

    /**
     * @param owner Dialog owner, can be {@code null} to set the main frame as the owner.
     * @param dataSyncManager Sync manager to handle download calls with.
     * @param connection TAK Server connection where the feed is from.
     * @param mission The mission <i>(data feed)</i> to download.
     * @param onSuccess Optional handler for deletion successes.
     * @param onFailure Optional handler for deletion failures.
     */
    public DataFeedDeleteDialog(@Nullable Window owner, @Nonnull IDataSyncManager dataSyncManager,
                                @Nonnull ITakServerConnection connection, @Nonnull Mission mission,
                                @Nullable Consumer<IDataSyncManager.MissionDeletion> onSuccess,
                                @Nullable Consumer<IDataSyncManager.MissionDeletion> onFailure)
    {
        super(owner);
        this.dataSyncManager = dataSyncManager;
        this.connection = connection;
        this.mission = mission;
        this.onSuccess = onSuccess;
        this.onFailure = onFailure;

        initComponents();

        // Update title/label to include feed name.
        String name = mission.name();
        setTitle("Delete feed " + name);
        deleteLabel.setText("Are you sure you want to delete feed " + name);

        // For missions that we own, show the option to delete the mission remotely on the server as well.
        // By default, the items are visible.
        if (!dataSyncManager.isMissionOwner(connection, name)) deleteRemoteCheck.setVisible(false);
    }

    /**
     * Deletes the feed.
     */
    private void delete()
    {
        String name = mission.name();
        boolean deleteLocal = deleteLocalCheck.isSelected();
        boolean deleteRemote = deleteRemoteCheck.isSelected();
        dataSyncManager.delete(connection, name, deleteLocal, deleteRemote).whenComplete((deletion, error) -> {
            if (deletion != null)
            {
                // The deletion was either success, or has an error that clearly indicates what the problem is.
                // The dialog caller can handle those cases.
                if (onSuccess != null)
                {
                    onSuccess.accept(deletion);
                } else if (onFailure != null)
                {
                    onFailure.accept(deletion);
                }
            } else
            {
                // This handles uncaught exceptions, which we have not accounted for any cannot be sure what happened.
                // Things like a NPE in the sync API rather than a problem addressable by the user like a lack of internet connection.
                String title = "Failed to delete feed '" + name + "'";
                String message = "Refer to takx.log for details of the failure";
                logger.error(title, error);
                JOptionPane.showMessageDialog(this, title, message, JOptionPane.ERROR_MESSAGE);
            }
        });
        dispose();
    }

    /**
     * Closes the dialog.
     */
    private void cancel()
    {
        dispose();
    }

    private void initComponents()
    {
        // JFormDesigner - Component initialization - DO NOT MODIFY  //GEN-BEGIN:initComponents
        dialogPane = new JPanel();
        contentPanel = new JPanel();
        deleteLabel = new JLabel();
        optionsLabel = new JLabel();
        deleteLocalCheck = new JCheckBox();
        deleteRemoteCheck = new JCheckBox();
        buttonBar = new JPanel();
        deleteButton = new JButton();
        cancelButton = new JButton();
        CellConstraints cc = new CellConstraints();

        //======== this ========
        setTitle("Delete feed");
        setName("this");
        var contentPane = getContentPane();
        contentPane.setLayout(new BorderLayout());

        //======== dialogPane ========
        {
            dialogPane.setBorder(Borders.createEmptyBorder("7dlu, 7dlu, 7dlu, 7dlu"));
            dialogPane.setName("dialogPane");
            dialogPane.setLayout(new FormLayout(
                    "default:grow",
                    "top:default:grow, bottom:default"));

            //======== contentPanel ========
            {
                contentPanel.setName("contentPanel");
                contentPanel.setLayout(new FormLayout(
                        "default:grow",
                        "default, $lgap, $rgap, 3*($lgap, default)"));

                //---- deleteLabel ----
                deleteLabel.setText("Are you sure you want to delete feed: %NAME%");
                deleteLabel.setName("deleteLabel");
                contentPanel.add(deleteLabel, cc.xy(1, 1));

                //---- optionsLabel ----
                optionsLabel.setText("Additional options:");
                optionsLabel.setName("optionsLabel");
                contentPanel.add(optionsLabel, cc.xy(1, 5));

                //---- deleteLocalCheck ----
                deleteLocalCheck.setText("Delete local feed files");
                deleteLocalCheck.setBorder(new EmptyBorder(0, 10, 0, 0));
                deleteLocalCheck.setToolTipText("Not yet supported");
                deleteLocalCheck.setName("deleteLocalCheck");
                contentPanel.add(deleteLocalCheck, cc.xy(1, 7));

                //---- deleteRemoteCheck ----
                deleteRemoteCheck.setText("Delete remote feed from server (Subscribers will be notified)");
                deleteRemoteCheck.setBorder(new EmptyBorder(0, 10, 0, 0));
                deleteRemoteCheck.setName("deleteRemoteCheck");
                contentPanel.add(deleteRemoteCheck, cc.xy(1, 9));
            }
            dialogPane.add(contentPanel, cc.xy(1, 1));

            //======== buttonBar ========
            {
                buttonBar.setBorder(Borders.createEmptyBorder("5dlu, 0dlu, 0dlu, 0dlu"));
                buttonBar.setName("buttonBar");
                buttonBar.setLayout(new FormLayout(
                        "default:grow, 2*($lcgap, $button)",
                        "pref"));

                //---- deleteButton ----
                deleteButton.setText("Delete");
                deleteButton.setName("deleteButton");
                deleteButton.addActionListener(e -> delete());
                buttonBar.add(deleteButton, cc.xy(3, 1));

                //---- cancelButton ----
                cancelButton.setText("Cancel");
                cancelButton.setName("cancelButton");
                cancelButton.addActionListener(e -> cancel());
                buttonBar.add(cancelButton, cc.xy(5, 1));
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
    private JLabel deleteLabel;
    private JLabel optionsLabel;
    private JCheckBox deleteLocalCheck;
    private JCheckBox deleteRemoteCheck;
    private JPanel buttonBar;
    private JButton deleteButton;
    private JButton cancelButton;
    // JFormDesigner - End of variables declaration  //GEN-END:variables
}
