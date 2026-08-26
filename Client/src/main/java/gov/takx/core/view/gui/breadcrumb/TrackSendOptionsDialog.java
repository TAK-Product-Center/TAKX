package gov.takx.core.view.gui.breadcrumb;

import com.jgoodies.forms.factories.Borders;
import com.jgoodies.forms.layout.CellConstraints;
import com.jgoodies.forms.layout.FormLayout;
import gov.takx.commons.file.FileUtils;
import gov.takx.platform.ui.AStandardDialog;
import gov.takx.platform.ui.ResourceDirectory;
import gov.takx.platform.ui.UIResources;
import jakarta.annotation.Nonnull;

import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;

import java.awt.event.ActionListener;
import java.io.File;

/**
 * Displays an exported {@link gov.takx.api.breadcrumb.ITrack} file and presents the user with a list of sharing
 * options.
 *
 * @since 5.4
 */
public class TrackSendOptionsDialog extends AStandardDialog
{
    private static final ImageIcon TRACKS_ICON = UIResources.getActionButtonIcon(ResourceDirectory.PLATFORM_ICONS, "breadcrumb_track.png");
    private static final ImageIcon CONTACTS_ICON = UIResources.getActionButtonIcon(ResourceDirectory.PLATFORM_ICONS, "contacts.png");
    private static final ImageIcon EXIT_ICON = UIResources.getActionButtonIcon(ResourceDirectory.PLATFORM_ICONS, "cancel.png");
    private static final ImageIcon FOLDER_ICON = UIResources.getActionButtonIcon(ResourceDirectory.PLATFORM_ICONS, "folder_open.png");
    private static final ImageIcon TAK_SERVER_ICON = UIResources.getActionButtonIcon(ResourceDirectory.PLATFORM_ICONS, "tak_server.png");

    /**
     * @param file An exported {@link gov.takx.api.breadcrumb.ITrack} file
     * @param sendToContactsListener Listener that responds when the user chooses to send the track file to contacts  
     * @param sendToServerListener Listener that responds when the user chooses to send the track file to a TAK server
     */
    public TrackSendOptionsDialog(@Nonnull File file, @Nonnull ActionListener sendToContactsListener,
                                  @Nonnull ActionListener sendToServerListener)
    {
        initComponents();

        exportInfoLabel.setIcon(TRACKS_ICON);

        fileNameLabel.setText(file.getName());
        fileNameLabel.setToolTipText(file.getPath());

        openFileButton.setIcon(FOLDER_ICON);
        openFileButton.addActionListener(e -> FileUtils.browseFile(file.getParentFile()));

        contactsButton.setIcon(CONTACTS_ICON);
        contactsButton.addActionListener(sendToContactsListener);
        contactsButton.addActionListener(e -> dispose());

        serverButton.setIcon(TAK_SERVER_ICON);
        serverButton.addActionListener(sendToServerListener);
        serverButton.addActionListener(e -> dispose());

        exitButton.setIcon(EXIT_ICON);
        exitButton.addActionListener(e -> dispose());

        pack();
        requestFocus();
    }

    private void initComponents()
    {
        // JFormDesigner - Component initialization - DO NOT MODIFY  //GEN-BEGIN:initComponents  @formatter:off
        var contentPanel = new JPanel();
        var filePanel = new JPanel();
        exportInfoLabel = new JLabel();
        fileNameLabel = new JLabel();
        openFileButton = new JButton();
        var buttonsPanel = new JPanel();
        contactsButton = new JButton();
        var shareWithContactsLabel = new JLabel();
        serverButton = new JButton();
        var sendToServerLabel = new JLabel();
        exitButton = new JButton();
        var exitLabel = new JLabel();
        CellConstraints cc = new CellConstraints();

        //======== this ========
        setTitle("Track Exported");
        setName("this");
        var contentPane = getContentPane();
        contentPane.setLayout(new FormLayout(
                "default:grow",
                "fill:default"));

        //======== contentPanel ========
        {
            contentPanel.setBorder(Borders.DLU4_BORDER);
            contentPanel.setName("contentPanel");
            contentPanel.setLayout(new FormLayout(
                    "default:grow",
                    "24dlu, $pgap, default"));

            //======== filePanel ========
            {
                filePanel.setBorder(Borders.createEmptyBorder("0dlu, 4dlu, 0dlu, 4dlu"));
                filePanel.setName("filePanel");
                filePanel.setLayout(new FormLayout(
                        "default, $lcgap, default:grow, $ugap, 24dlu",
                        "fill:24dlu"));

                //---- exportInfoLabel ----
                exportInfoLabel.setText("Exported to:");
                exportInfoLabel.setName("exportInfoLabel");
                filePanel.add(exportInfoLabel, cc.xy(1, 1, CellConstraints.CENTER, CellConstraints.DEFAULT));

                //---- fileNameLabel ----
                fileNameLabel.setText("file");
                fileNameLabel.setName("fileNameLabel");
                filePanel.add(fileNameLabel, cc.xy(3, 1));

                //---- openFileButton ----
                openFileButton.setName("openFileButton");
                filePanel.add(openFileButton, cc.xy(5, 1));
            }
            contentPanel.add(filePanel, cc.xy(1, 1));

            //======== buttonsPanel ========
            {
                buttonsPanel.setName("buttonsPanel");
                buttonsPanel.setLayout(new FormLayout(
                        "24dlu, $lcgap, default:grow",
                        "fill:24dlu, $lgap, fill:24dlu, $pgap, fill:24dlu"));

                //---- contactsButton ----
                contactsButton.setToolTipText("Share with contacts");
                contactsButton.setName("contactsButton");
                buttonsPanel.add(contactsButton, cc.xy(1, 1));

                //---- shareWithContactsLabel ----
                shareWithContactsLabel.setText("Send file to contacts");
                shareWithContactsLabel.setName("shareWithContactsLabel");
                buttonsPanel.add(shareWithContactsLabel, cc.xy(3, 1));

                //---- serverButton ----
                serverButton.setToolTipText("Send file to TAK server");
                serverButton.setName("serverButton");
                buttonsPanel.add(serverButton, cc.xy(1, 3));

                //---- sendToServerLabel ----
                sendToServerLabel.setText("Send track to TAK server");
                sendToServerLabel.setName("sendToServerLabel");
                buttonsPanel.add(sendToServerLabel, cc.xy(3, 3));

                //---- exitButton ----
                exitButton.setToolTipText("Exit");
                exitButton.setName("exitButton");
                buttonsPanel.add(exitButton, cc.xy(1, 5));

                //---- exitLabel ----
                exitLabel.setText("Exit");
                exitLabel.setName("exitLabel");
                buttonsPanel.add(exitLabel, cc.xy(3, 5));
            }
            contentPanel.add(buttonsPanel, cc.xy(1, 3));
        }
        contentPane.add(contentPanel, cc.xy(1, 1));
        pack();
        setLocationRelativeTo(getOwner());
        // JFormDesigner - End of component initialization  //GEN-END:initComponents  @formatter:on
    }

    // JFormDesigner - Variables declaration - DO NOT MODIFY  //GEN-BEGIN:variables  @formatter:off
    private JLabel exportInfoLabel;
    private JLabel fileNameLabel;
    private JButton openFileButton;
    private JButton contactsButton;
    private JButton serverButton;
    private JButton exitButton;
    // JFormDesigner - End of variables declaration  //GEN-END:variables  @formatter:on
}
