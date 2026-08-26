package gov.takx.core.view.gui.takserver;

import com.jgoodies.forms.factories.Borders;
import com.jgoodies.forms.layout.CellConstraints;
import com.jgoodies.forms.layout.FormLayout;
import gov.tak.api.comms.takserver.IDataSyncManager;
import gov.tak.api.comms.takserver.IGroupManager;
import gov.tak.api.comms.takserver.ITakServerConnection;
import gov.tak.api.comms.takserver.ITakServerConnectionManager;
import gov.tak.api.comms.takserver.mission.MissionEndpoints;
import gov.tak.api.comms.takserver.mission.TakServerApiClient;
import gov.tak.api.comms.takserver.mission.call.mission.CreateMission;
import gov.tak.api.comms.takserver.mission.data.Group;
import gov.tak.api.comms.takserver.mission.data.Mission;
import gov.tak.api.comms.takserver.mission.data.MissionBuilder;
import gov.tak.api.comms.takserver.mission.data.MissionRole;
import gov.tak.api.comms.takserver.mission.data.RoleType;
import gov.takx.api.annotation.PseudoBean;
import gov.takx.api.platform.ICoreExecutorService;
import gov.takx.commons.utils.RegexDocumentFilter;
import gov.takx.platform.ui.AStandardDialog;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import jakarta.inject.Inject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.DefaultComboBoxModel;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTextField;

import java.awt.BorderLayout;
import java.awt.Font;
import java.awt.Window;
import java.awt.event.ItemEvent;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.lang.invoke.MethodHandles;
import java.net.URI;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.regex.Pattern;

/**
 * Dialog for editing or creating new data-sync feeds (Missions).
 *
 * @see #forCreation(Window)
 * @see #forEditing(Window, ITakServerConnection, Mission)
 * @since 5.0
 */
@PseudoBean
public class DataFeedEditorDialog extends AStandardDialog
{
    private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    private static final Pattern KEYWORDS_PATTERN = Pattern.compile("[\\s\\w\\-().,$@+=\\/\\\\!&]+");
    private static final Pattern KEYWORD_PATTERN = Pattern.compile("[\\w()-.$@+=\\/\\\\!&]+");

    @Inject
    private TakServerApiClient takServerApiClient;
    @Inject
    private ITakServerConnectionManager connectionManager;
    @Inject
    private IDataSyncManager dataSyncManager;
    @Inject
    private IGroupManager groupsManager;
    @Inject
    private ICoreExecutorService executorService;

    private Consumer<Mission> onOk = unused -> {
        // no-op by default
    };

    private DataFeedEditorDialog(@Nullable Window owner)
    {
        super(owner);
        initComponents();
        new RegexDocumentFilter(KEYWORDS_PATTERN).install(tagsText);
        serverCombo.setRenderer((list, value, index, isSelected, cellHasFocus) -> {
            if (value != null) return new JLabel(value.getUri().toString());
            return new JLabel("No available servers");
        });
    }

    /**
     * Creates a dialog for creating a new {@link Mission}. Pressing 'OK' will submit a {@link CreateMission} request.
     *
     * @param window Parent window for dialog relative positioning.
     *
     * @return New editor window for creating a mission.
     */
    @Nonnull
    public static DataFeedEditorDialog forCreation(@Nullable Window window)
    {
        DefaultComboBoxModel<ITakServerConnection> connectionsModel = new DefaultComboBoxModel<>();
        DefaultComboBoxModel<String> groupsModel = new DefaultComboBoxModel<>();

        DataFeedEditorDialog dialog = new DataFeedEditorDialog(window);

        // Populate available groups based on the selected connection.
        Consumer<ITakServerConnection> populateGroupsFromConnection = selectedConnection -> dialog.groupsManager.getGroups(selectedConnection).whenComplete(((results, error) -> {
            if (results != null)
            {
                if (results.wasSuccess())
                {
                    List<String> groupNames = results.groups().stream().map(Group::name).toList();
                    groupsModel.removeAllElements();
                    groupsModel.addAll(groupNames);
                    if (!groupNames.isEmpty()) dialog.groupCombo.setSelectedIndex(0);
                } else
                    error = results.requireError();
            }

            if (error != null)
            {
                String title = "Failed to retrieve groups";
                String message = "The groups for '" + selectedConnection.getUri() + "' could not be fetched";
                JOptionPane.showMessageDialog(dialog, title, message, JOptionPane.ERROR_MESSAGE);
            }
        }));
        dialog.groupCombo.setModel(groupsModel);
        dialog.serverCombo.addItemListener(event -> {
            if (event.getStateChange() == ItemEvent.SELECTED && event.getItem() instanceof ITakServerConnection selectedConnection)
                populateGroupsFromConnection.accept(selectedConnection);
        });

        // Populate the server selection model with servers that are enabled.
        // Select first connected server.
        Collection<? extends ITakServerConnection> connections = dialog.connectionManager.getTakServerConnections();
        connectionsModel.addAll(connections.stream().filter(ITakServerConnection::isConnected).toList());
        for (ITakServerConnection connection : connections)
        {
            if (connection.isConnected())
            {
                connectionsModel.setSelectedItem(connection);
                break;
            }
        }

        // If there are no connected servers, just choose the first option.
        // If there are no servers at all, disable the input. The cell renderer will tell the user
        // that no servers are available, the 'ok' button will stay disabled.
        if (connectionsModel.getSelectedItem() == null)
        {
            if (connectionsModel.getSize() > 0)
            {
                connectionsModel.setSelectedItem(connectionsModel.getElementAt(0));
            } else
            {
                dialog.serverCombo.setEnabled(false);
            }
        }

        // If there was a selection made, populate the groups.
        if (connectionsModel.getSelectedItem() instanceof ITakServerConnection selectedConnection)
        {
            populateGroupsFromConnection.accept(selectedConnection);
        }
        dialog.setTitle("Create new feed");
        dialog.serverCombo.setModel(connectionsModel);
        dialog.onOk = createdMission -> dialog.dataSyncManager.createMission((ITakServerConnection) connectionsModel.getSelectedItem(), createdMission, dialog.passwordText.getText());

        // TODO: Auto-publish check-box + support
        //   - Adds all newly made map items to the feed automatically
        //   - Auto-publish flag is not part of the model, so we need to track that separately

        return dialog;
    }

    /**
     * Creates a dialog for editing elements of an existing {@link Mission}.
     * Pressing 'OK' will submit multiple requests, one for each modified element.
     *
     * @param window Parent window for dialog relative positioning.
     * @param serverConnection The server connection where the mission is from.
     * @param mission The mission to edit.
     *
     * @return New editor window for creating a mission.
     */
    @Nonnull
    public static DataFeedEditorDialog forEditing(@Nullable Window window,
                                                  @Nonnull ITakServerConnection serverConnection,
                                                  @Nonnull Mission mission)
    {
        DataFeedEditorDialog dialog = new DataFeedEditorDialog(window);
        dialog.setTitle("Edit feed");

        // Select group-box items
        DefaultComboBoxModel<ITakServerConnection> serverModel = new DefaultComboBoxModel<>(new ITakServerConnection[]{serverConnection});
        serverModel.setSelectedItem(serverConnection);

        // Disable inputs that do not support modification from the Mission API
        dialog.nameText.setText(mission.name());
        dialog.nameText.setEnabled(false);
        dialog.descriptionText.setText(mission.description());
        dialog.descriptionText.setEnabled(false);
        dialog.groupCombo.setEnabled(false);
        dialog.chatCombo.setEnabled(false);
        dialog.serverCombo.setModel(serverModel);
        dialog.serverCombo.setEditable(false);
        dialog.serverCombo.setEnabled(false);
        dialog.passwordLabel.setText("New Password");
        dialog.okButton.setEnabled(true);

        // TODO: TAKX-2102 Support inviting TAK contacts when editing an existing feed
        //  - Extra button becomes visible which opens a new UI for inviting contacts

        // Fill in values from existing mission
        dialog.fillMutableValuesFromMission(mission);
        String[] groups;
        if (mission.groups() != null && !mission.groups().isEmpty())
        {
            groups = mission.groups().toArray(new String[0]);
        } else
        {
            // Shouldn't happen unless the mission model is incomplete for some reason.
            groups = new String[]{"No specified group"};
        }
        DefaultComboBoxModel<String> groupModel = new DefaultComboBoxModel<>(groups);
        dialog.groupCombo.setModel(groupModel);
        dialog.groupCombo.setSelectedIndex(0);
        dialog.groupCombo.setSelectedItem(groups[0]);

        // Create generic error handling since a lot of the following actions are repetitive
        String missionName = mission.name();
        URI serverUri = serverConnection.getUri();
        BiConsumer<String, Throwable> errorHandling = (item, error) -> {
            if (error != null)
            {
                String title = "Failed to update feed";
                String message = "Server '" + serverUri + "' refused updating of feed '" + missionName + "' " + item;
                logger.error(message, error);
                JOptionPane.showMessageDialog(dialog, message, title, JOptionPane.ERROR_MESSAGE);
            }
        };

        dialog.onOk = editedMission -> {
            MissionEndpoints missionEndpoints = dialog.takServerApiClient.missionEndpoints(serverConnection);
            String token = dialog.dataSyncManager.getMissionSubscriptionToken(serverConnection, missionName);

            // Update default role
            if (!Objects.equals(mission.defaultRole().type(), editedMission.defaultRole().type()))
                missionEndpoints.setMissionRole(missionName)
                        .withRole(editedMission.defaultRole().type())
                        .setAuthorizationToken(token)
                        .asyncGet(dialog.executorService)
                        .whenComplete((unused, error) -> errorHandling.accept("default role", error));

            // Update password
            if (!dialog.passwordText.getText().isBlank())
                missionEndpoints.setMissionPassword(missionName)
                        .withPassword(dialog.passwordText.getText())
                        .setAuthorizationToken(token)
                        .asyncGet(dialog.executorService)
                        .whenComplete((unused, error) -> errorHandling.accept("password", error));

            // Update keywords
            if (!Objects.equals(mission.keywords(), editedMission.keywords()))
                missionEndpoints.setMissionKeywords(missionName, editedMission.keywords())
                        .setAuthorizationToken(token)
                        .asyncGet(dialog.executorService)
                        .whenComplete((unused, error) -> errorHandling.accept("keywords", error));
        };
        return dialog;
    }

    /**
     * Called when the ok button is pressed.
     */
    private void onOk()
    {
        onOk.accept(createMissionModel());
        dispose();
    }

    /**
     * Called when the cancel button is pressed.
     */
    private void onCancel()
    {
        dispose();
    }

    /**
     * @return New mission model from inputs.
     */
    @Nonnull
    private Mission createMissionModel()
    {
        String name = nameText.getText().trim();
        String description = descriptionText.getText().trim();
        List<String> keywords = Arrays.stream(tagsText.getText().split("\\s*,\\s*"))
                .map(tag -> tag.trim().replace(" ", "_"))
                .filter(tag -> KEYWORD_PATTERN.matcher(tag).matches())
                .toList();
        String creatorUid = takServerApiClient.getClientUid().toString();
        RoleType defaultRoleType = RoleType.values()[roleCombo.getSelectedIndex()];
        MissionRole defaultRole = new MissionRole(defaultRoleType, Collections.emptySet());
        String password = passwordText.getText();
        Set<String> groups = Set.of(groupCombo.getSelectedItem().toString());
        return new MissionBuilder(name)
                .withDescription(description)
                .withChatRoom(null) // TODO TAKX-1404: Chat room combo-box + support
                .withKeywords(keywords)
                .withCreatorUid(creatorUid)
                .withExpiration(null) // TODO: WinTAK doesn't offer an expire time input, but should we?
                .withDefaultRole(defaultRole)
                .withPasswordProtected(!password.isEmpty())
                .withGroups(groups)
                .build();
    }

    /**
     * Called by any UI component when updated to ensure no invalid inputs can be fed into the creation of a {@link Mission}.
     */
    private void onUpdate()
    {
        okButton.setEnabled(!nameText.getText().isBlank() && serverCombo.getSelectedItem() != null);
    }

    /**
     * Copies values from a given mission into the editor form inputs.
     *
     * @param mission Mission to copy values of.
     */
    public void fillMutableValuesFromMission(@Nonnull Mission mission)
    {
        MissionRole defaultRole = mission.defaultRole();
        int roleIndex = defaultRole == null ? RoleType.MISSION_SUBSCRIBER.ordinal() : defaultRole.type().ordinal();
        roleCombo.setSelectedIndex(roleIndex);
        tagsText.setText(String.join(", ", mission.keywords()));

        // Blank means no change for passwords.
        passwordText.setText("");
    }

    private void initComponents()
    {
        // JFormDesigner - Component initialization - DO NOT MODIFY  //GEN-BEGIN:initComponents
        dialogPane = new JPanel();
        contentPanel = new JPanel();
        nameLabel = new JLabel();
        nameText = new JTextField();
        serverLabel = new JLabel();
        serverCombo = new JComboBox<>();
        groupLabel = new JLabel();
        groupCombo = new JComboBox<>();
        roleLabel = new JLabel();
        roleCombo = new JComboBox<>();
        optionalFieldsLabel = new JLabel();
        passwordLabel = new JLabel();
        passwordText = new JTextField();
        descriptionLabel = new JLabel();
        descriptionText = new JTextField();
        chatLabel = new JLabel();
        chatCombo = new JComboBox<>();
        tagsLabel = new JLabel();
        tagsText = new JTextField();
        buttonBar = new JPanel();
        okButton = new JButton();
        cancelButton = new JButton();
        CellConstraints cc = new CellConstraints();

        //======== this ========
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
                        "right:default, $lcgap, [400px,default]:grow",
                        "8*(default, $lgap), default"));

                //---- nameLabel ----
                nameLabel.setText("Name");
                nameLabel.setName("nameLabel");
                contentPanel.add(nameLabel, cc.xy(1, 1));

                //---- nameText ----
                nameText.setName("nameText");
                nameText.addKeyListener(new KeyAdapter()
                {
                    @Override
                    public void keyReleased(KeyEvent e)
                    {
                        onUpdate();
                    }
                });
                contentPanel.add(nameText, cc.xy(3, 1));

                //---- serverLabel ----
                serverLabel.setText("Server");
                serverLabel.setName("serverLabel");
                contentPanel.add(serverLabel, cc.xy(1, 3));

                //---- serverCombo ----
                serverCombo.setName("serverCombo");
                contentPanel.add(serverCombo, cc.xy(3, 3));

                //---- groupLabel ----
                groupLabel.setText("Group");
                groupLabel.setName("groupLabel");
                contentPanel.add(groupLabel, cc.xy(1, 5));

                //---- groupCombo ----
                groupCombo.setModel(new DefaultComboBoxModel<>(new String[]{
                        "<fetching groups...>"
                }));
                groupCombo.setName("groupCombo");
                contentPanel.add(groupCombo, cc.xy(3, 5));

                //---- roleLabel ----
                roleLabel.setText("Default Role");
                roleLabel.setName("roleLabel");
                contentPanel.add(roleLabel, cc.xy(1, 7));

                //---- roleCombo ----
                roleCombo.setModel(new DefaultComboBoxModel<>(new String[]{
                        "Owner",
                        "Subscriber",
                        "Read-only subscriber"
                }));
                roleCombo.setSelectedIndex(1);
                roleCombo.setName("roleCombo");
                contentPanel.add(roleCombo, cc.xy(3, 7));

                //---- optionalFieldsLabel ----
                optionalFieldsLabel.setText("Optional Fields");
                optionalFieldsLabel.setFont(new Font("Segoe UI", Font.BOLD, 18));
                optionalFieldsLabel.setName("optionalFieldsLabel");
                contentPanel.add(optionalFieldsLabel, cc.xywh(1, 9, 3, 1));

                //---- passwordLabel ----
                passwordLabel.setText("Password");
                passwordLabel.setName("passwordLabel");
                contentPanel.add(passwordLabel, cc.xy(1, 11));

                //---- passwordText ----
                passwordText.setName("passwordText");
                contentPanel.add(passwordText, cc.xy(3, 11));

                //---- descriptionLabel ----
                descriptionLabel.setText("Description");
                descriptionLabel.setName("descriptionLabel");
                contentPanel.add(descriptionLabel, cc.xy(1, 13));

                //---- descriptionText ----
                descriptionText.setName("descriptionText");
                contentPanel.add(descriptionText, cc.xy(3, 13));

                //---- chatLabel ----
                chatLabel.setText("Chat Room");
                chatLabel.setName("chatLabel");
                contentPanel.add(chatLabel, cc.xy(1, 15));

                //---- chatCombo ----
                chatCombo.setModel(new DefaultComboBoxModel<>(new String[]{
                        "No XMPP chat rooms configured"
                }));
                chatCombo.setSelectedIndex(0);
                chatCombo.setEnabled(false);
                chatCombo.setName("chatCombo");
                contentPanel.add(chatCombo, cc.xy(3, 15));

                //---- tagsLabel ----
                tagsLabel.setText("Tags");
                tagsLabel.setName("tagsLabel");
                contentPanel.add(tagsLabel, cc.xy(1, 17));

                //---- tagsText ----
                tagsText.setName("tagsText");
                contentPanel.add(tagsText, cc.xy(3, 17));
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
                okButton.setText("OK");
                okButton.setEnabled(false);
                okButton.setName("okButton");
                okButton.addActionListener(e -> onOk());
                buttonBar.add(okButton, cc.xy(2, 1));

                //---- cancelButton ----
                cancelButton.setText("Cancel");
                cancelButton.setName("cancelButton");
                cancelButton.addActionListener(e -> onCancel());
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
    private JLabel nameLabel;
    private JTextField nameText;
    private JLabel serverLabel;
    private JComboBox<ITakServerConnection> serverCombo;
    private JLabel groupLabel;
    private JComboBox<String> groupCombo;
    private JLabel roleLabel;
    private JComboBox<String> roleCombo;
    private JLabel optionalFieldsLabel;
    private JLabel passwordLabel;
    private JTextField passwordText;
    private JLabel descriptionLabel;
    private JTextField descriptionText;
    private JLabel chatLabel;
    private JComboBox<String> chatCombo;
    private JLabel tagsLabel;
    private JTextField tagsText;
    private JPanel buttonBar;
    private JButton okButton;
    private JButton cancelButton;
    // JFormDesigner - End of variables declaration  //GEN-END:variables
}
