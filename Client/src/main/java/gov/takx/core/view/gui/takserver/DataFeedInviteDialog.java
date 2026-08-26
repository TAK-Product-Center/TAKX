package gov.takx.core.view.gui.takserver;

import com.atakmap.commoncommo.Contact;
import com.jgoodies.forms.layout.CellConstraints;
import com.jgoodies.forms.layout.FormLayout;
import gov.tak.api.comms.takserver.IDataSyncManager;
import gov.tak.api.comms.takserver.ITakServerConnection;
import gov.tak.api.comms.takserver.mission.MissionEndpoints;
import gov.tak.api.comms.takserver.mission.TakServerApiClient;
import gov.tak.api.contact.IContactNameResolver;
import gov.takx.api.annotation.InvokeOnEDT;
import gov.takx.api.annotation.PseudoBean;
import gov.takx.api.notification.INotificationService;
import gov.takx.api.notification.Notification;
import gov.takx.platform.ui.AStandardDialog;
import jakarta.annotation.Nonnull;
import jakarta.annotation.PostConstruct;
import jakarta.inject.Inject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.border.EmptyBorder;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.Font;
import java.lang.invoke.MethodHandles;
import java.util.Collections;
import java.util.List;

/**
 * A dialog to handle inviting TAK contacts to a given mission.
 *
 * @since 5.0
 */
@PseudoBean
public class DataFeedInviteDialog extends AStandardDialog
{
    private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    private static final ContactInfo DUMMY_PENDING = new ContactInfo("", "<Fetching members...>");
    private static final ContactInfo DUMMY_EMPTY = new ContactInfo("", "<No active members>");

    private final DefaultListModel<ContactInfo> subscriberModel = new DefaultListModel<>();
    private final ITakServerConnection connection;
    private final String missionName;
    private MissionEndpoints missionEndpoints;

    @Inject
    private TakServerApiClient takServerApiClient;

    @Inject
    private IDataSyncManager dataSyncManager;

    @Inject
    private IContactNameResolver contactNameResolver;

    @Inject
    private INotificationService notificationService;

    /**
     * @param connection Connection to server defining the mission.
     * @param missionName Name of the mission to show invites for.
     */
    public DataFeedInviteDialog(@Nonnull ITakServerConnection connection, @Nonnull String missionName)
    {
        this.connection = connection;
        this.missionName = missionName;

        initComponents();

        setTitle("Members: " + missionName);

        membersList.setModel(subscriberModel);
    }

    @PostConstruct
    private void postConstruct()
    {
        missionEndpoints = takServerApiClient.missionEndpoints(connection);
        refreshList();
    }

    /**
     * Requests an up-to-date list of members of the current mission,
     */
    @InvokeOnEDT
    private void refreshList()
    {
        subscriberModel.clear();
        subscriberModel.addElement(DUMMY_PENDING);

        missionEndpoints.getMissionSubscribers(missionName)
                .withAuthorizationTokenIfAvailable(dataSyncManager, connection, missionName)
                .asyncGet()
                .whenComplete((subscribers, error) -> {
                    if (subscribers != null)
                    {
                        repopulateModel(subscribers);
                    } else
                    {
                        String errorMessage = String.format("Failed fetching members of mission '%s'", missionName);
                        logger.debug(errorMessage, error);
                        notificationService.show(Notification.builder(errorMessage, Notification.Type.Error).build());
                    }
                });
    }

    /**
     * Shows a selection dialog for a number of contacts.
     * Invites each selected contact.
     */
    private void showInviteSelection()
    {
        new ContactSelectionDialog(contacts -> dataSyncManager.inviteToMission(connection, missionName, contacts), this::isMissionSubscriber).setVisible(true);
    }

    /**
     * Repopulate the list with callsigns of current mission members.
     *
     * @param subscriberUids Contact uids of members of the mission.
     */
    @InvokeOnEDT
    private void repopulateModel(@Nonnull List<String> subscriberUids)
    {
        // Only handle invitations representing individual users.
        subscriberModel.clear();
        if (subscriberUids.isEmpty())
        {
            subscriberModel.addElement(DUMMY_EMPTY);
        } else
        {
            for (String subscriberUid : subscriberUids)
            {
                String callsign = contactNameResolver.resolveNameOrElseUnknown(subscriberUid);
                subscriberModel.addElement(new ContactInfo(subscriberUid, callsign));
            }
        }

        membersList.revalidate();
        membersList.repaint();
    }

    /**
     * @param contact Contact to check.
     *
     * @return {@code true} when the contact is a known member of the current mission.
     */
    private boolean isMissionSubscriber(@Nonnull Contact contact)
    {
        for (ContactInfo contactInfo : Collections.list(subscriberModel.elements()))
        {
            if (contactInfo.uid.equals(contact.contactUID)) return true;
        }
        return false;
    }

    /**
     * Wrapper for a contact and their displayed callsign.
     *
     * @param uid Contact uid.
     * @param callsign Resolved callsign of contact.
     */
    private record ContactInfo(@Nonnull String uid, @Nonnull String callsign)
    {
        @Override
        public String toString()
        {
            return callsign;
        }
    }

    private void initComponents()
    {
        // JFormDesigner - Component initialization - DO NOT MODIFY  //GEN-BEGIN:initComponents
        var wrapper = new JPanel();
        var membersLabel = new JLabel();
        var listScrollWrapper = new JScrollPane();
        membersList = new JList<>();
        var buttonsWrapper = new JPanel();
        inviteOthersButton = new JButton();
        CellConstraints cc = new CellConstraints();

        //======== this ========
        setMinimumSize(new Dimension(230, 190));
        setName("this");
        var contentPane = getContentPane();
        contentPane.setLayout(new BorderLayout());

        //======== wrapper ========
        {
            wrapper.setBorder(new EmptyBorder(5, 5, 5, 5));
            wrapper.setName("wrapper");
            wrapper.setLayout(new FormLayout(
                    "default:grow, $lcgap, default:grow",
                    "default, $lgap, fill:default:grow, $lgap, default"));

            //---- membersLabel ----
            membersLabel.setText("Active Members");
            membersLabel.setFont(membersLabel.getFont().deriveFont(membersLabel.getFont().getStyle() | Font.BOLD));
            membersLabel.setName("membersLabel");
            wrapper.add(membersLabel, cc.xy(1, 1));

            //======== listScrollWrapper ========
            {
                listScrollWrapper.setName("listScrollWrapper");

                //---- membersList ----
                membersList.setName("membersList");
                listScrollWrapper.setViewportView(membersList);
            }
            wrapper.add(listScrollWrapper, cc.xywh(1, 3, 3, 1, CellConstraints.DEFAULT, CellConstraints.FILL));

            //======== buttonsWrapper ========
            {
                buttonsWrapper.setName("buttonsWrapper");
                buttonsWrapper.setLayout(new FormLayout(
                        "default:grow, $lcgap, right:default",
                        "default"));

                //---- inviteOthersButton ----
                inviteOthersButton.setText("Invite Others");
                inviteOthersButton.setName("inviteOthersButton");
                inviteOthersButton.addActionListener(e -> showInviteSelection());
                buttonsWrapper.add(inviteOthersButton, cc.xy(3, 1));
            }
            wrapper.add(buttonsWrapper, cc.xywh(1, 5, 3, 1));
        }
        contentPane.add(wrapper, BorderLayout.CENTER);
        pack();
        setLocationRelativeTo(getOwner());
        // JFormDesigner - End of component initialization  //GEN-END:initComponents
    }

    // JFormDesigner - Variables declaration - DO NOT MODIFY  //GEN-BEGIN:variables
    private JList<ContactInfo> membersList;
    private JButton inviteOthersButton;
    // JFormDesigner - End of variables declaration  //GEN-END:variables
}
