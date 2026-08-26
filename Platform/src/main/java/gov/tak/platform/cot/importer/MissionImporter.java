package gov.tak.platform.cot.importer;

import com.atakmap.comms.NetConnectString;
import gov.tak.api.comms.takserver.IDataSyncManager;
import gov.tak.api.comms.takserver.ITakServerConnection;
import gov.tak.api.comms.takserver.ITakServerConnectionManager;
import gov.tak.api.contact.IContactNameResolver;
import gov.tak.api.cot.CotTypes;
import gov.tak.api.cot.event.CotDetail;
import gov.tak.api.cot.event.CotEvent;
import gov.tak.api.cot.importer.AbstractCotEventTypeImporter;
import gov.tak.api.cot.importer.ImportResult;
import gov.takx.api.annotation.PseudoBean;
import gov.takx.commons.swing.AlwaysOnTopJOptionPane;
import gov.takx.platform.preferences.PreferenceEntry;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import jakarta.inject.Inject;

import javax.swing.JOptionPane;

import java.util.Objects;

import static gov.tak.api.cot.detail.DetailConstants.ATTR_AUTHOR_UID;
import static gov.tak.api.cot.detail.DetailConstants.ATTR_NAME;
import static gov.tak.api.cot.detail.DetailConstants.ATTR_SERVER;
import static gov.tak.api.cot.detail.DetailConstants.ATTR_TYPE;
import static gov.tak.api.cot.detail.DetailConstants.MISSION;
import static gov.tak.api.cot.detail.DetailConstants.VALUE_TYPE_INVITE;

/**
 * CoT Event Importer that produces subscribes to mission invites.
 *
 * @since 5.0
 */
@PseudoBean
public class MissionImporter extends AbstractCotEventTypeImporter
{
    private static final String ATTR_TOKEN = "token";

    @Inject
    private ITakServerConnectionManager connectionManager;

    @Inject
    private IDataSyncManager dataSyncManager;

    @Inject
    private IContactNameResolver contactNameResolver;

    public MissionImporter()
    {
        super(false, CotTypes.MISSION);
    }

    @Override
    public ImportResult importData(CotEvent event)
    {
        // Event type must match.
        ImportResult result = super.importData(event);
        if (result == ImportResult.SUCCESS)
        {
            CotDetail missionDetail = event.getDetail().getFirstChildByName(MISSION);

            // Detail should have a mission child.
            if (missionDetail == null) return ImportResult.FAILURE;

            // Non-invite types are ignored.
            if (!Objects.equals(VALUE_TYPE_INVITE, missionDetail.getAttribute(ATTR_TYPE))) return ImportResult.IGNORE;

            // A mission invite without a mission name is incomplete.
            String missionName = missionDetail.getAttribute(ATTR_NAME);
            if (missionName == null) return ImportResult.FAILURE;

            // A mission invite without a token is incomplete.
            String subscriptionToken = missionDetail.getAttribute(ATTR_TOKEN);
            if (subscriptionToken == null) return ImportResult.FAILURE;

            // Some invites include explicitly which server the mission invite is for. Some do not...
            String serverAttribute = missionDetail.getAttribute(ATTR_SERVER);
            NetConnectString netConnectString = serverAttribute == null ? null : NetConnectString.fromString(serverAttribute);

            // If there is no matching connection we will just ignore the import.
            ITakServerConnection connection = connectionManager.getTakServerConnections().stream()
                    .filter(c -> {
                        // Skip disconnected/disabled connections
                        if (!c.isConnected() || !c.isEnabled()) return false;

                        // If no 'server' attribute was found, determine the server by whether the mission exists or not.
                        if (netConnectString == null) return dataSyncManager.getMission(c, missionName) != null;

                        // Otherwise, the host attribute of the 'server' attribute will determine the server.
                        return c.getConnectionConfig().getUri().getHost().equals(netConnectString.getHost());
                    })
                    .findFirst().orElse(null);
            if (connection == null) return ImportResult.IGNORE;

            // If needed, confirm that the user wants to accept the invitation
            if (!acceptInvite(missionName, missionDetail.getAttribute(ATTR_AUTHOR_UID))) return ImportResult.IGNORE;

            // We know which connection the mission belongs to, so we'll initiate the subscription.
            dataSyncManager.subscribeWithToken(connection, missionName, subscriptionToken);
            return ImportResult.SUCCESS;
        }
        return result;
    }

    /**
     * Returns a boolean indicating if a feed invite should be accepted, either because auto-accept is enabled or the
     * user has confirmed acceptance.
     *
     * @param missionName The feed mission
     * @param userUid The UID for the user that sent the invitation
     * @return {@code true} if the invitation is accepted, {@code false} otherwise
     */
    private boolean acceptInvite(@Nonnull String missionName, @Nullable String userUid)
    {
        if (PreferenceEntry.DATA_SYNC_AUTO_ACCEPT_FEED_INVITES.getBooleanValue()) return true;

        final String user = contactNameResolver.resolveNameOrElseUnknown(userUid);

        final int subscribe =
                AlwaysOnTopJOptionPane.showOptionDialog(
                        "Would you like to join feed %s sent by %s?".formatted(missionName, user),
                        "Data Sync Feed Invite", JOptionPane.QUESTION_MESSAGE, JOptionPane.YES_NO_OPTION,
                        null, null, JOptionPane.YES_OPTION);

        return subscribe == JOptionPane.YES_OPTION;
    }
}
