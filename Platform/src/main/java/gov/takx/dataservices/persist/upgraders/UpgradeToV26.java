package gov.takx.dataservices.persist.upgraders;

import jakarta.annotation.Nonnull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.invoke.MethodHandles;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.HashMap;
import java.util.Map;

/**
 * Upgrade from DB version 25 to 26.  Changes include:
 * <ul>
 *     <li>Add Conversation table</li>
 *     <li>Populate Conversation table with info from ChatMessage table</li>
 * </ul>
 * @since 5.2
 */
public class UpgradeToV26 extends AUpgrader
{
    private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    @Override
    public void doUpgrade() throws UpgraderException
    {
        createTable();
        populateConversationTable();
    }

    /**
     * Creates the Conversation table and adds a uniqueness constraint on UID
     */
    private void createTable()
    {
        // Create table
        executeUpdate("""
                create table Conversation (
                   id serial not null,
                    direct boolean not null,
                    name text not null,
                    uid varchar(255) not null,
                    primary key (id)
                )
                """);

        // Add uniqueness constraint on uid
        addUniqueConstraint("Conversation", new String[]{"uid"}, "uid");
    }

    /**
     * Queries the chat messages and group contacts and uses them to populate the Conversation table. At minimum,
     * conversations will be added with a UID, direct flag and an "unknown" name.
     * <p>
     * Conversations will not be created if the direct/indirect value cannot be determined. This will only happen with
     * conversations that only contain sent messages.
     */
    private void populateConversationTable()
    {
        // Build the group uid to name map
        final Map<String, String> groupNameMap = new HashMap<>();

        try (Statement statement = connection.createStatement();
             ResultSet rs = statement.executeQuery("SELECT uid, name FROM groupcontact"))
        {
            while (rs.next())
            {
                final String uid = rs.getString("uid");
                final String name = rs.getString("name");
                groupNameMap.put(uid, name);
            }
        } catch (SQLException e)
        {
            // Bad, but not fatal
            logger.error("Failed to query groupcontact table", e);
        }

        try
        {
            // Collect all chat messages, ordered by send time and use them to create Conversation objects
            final Map<String, Conversation> conversationMap = new HashMap<>();

            try (Statement statement = connection.createStatement();
                 ResultSet rs = statement.executeQuery("SELECT conversationuid, receivetime, sendername, senderuid, sendtime FROM chatmessage ORDER BY sendtime asc"))
            {
                // Iterate over each chat message
                while (rs.next())
                {
                    final String uid = rs.getString("conversationuid");
                    final long receiveTime = rs.getLong("receivetime");
                    final String senderName = rs.getString("sendername");
                    final String senderUid = rs.getString("senderuid");

                    // Get or create a Conversation object
                    final Conversation conversation = conversationMap.computeIfAbsent(uid, Conversation::new);

                    // Update the Conversation object with info in the chat message
                    updateConversation(conversation, receiveTime, senderUid, senderName, groupNameMap);
                }
            }

            if (conversationMap.isEmpty()) return;

            // Use each Conversation object to insert a new row into the Conversation table
            try (PreparedStatement statement = connection.prepareStatement("INSERT INTO conversation(direct, name, uid) VALUES (?, ?, ?)"))
            {
                for (Conversation conversation : conversationMap.values())
                {
                    if (conversation.direct == null)
                    {
                        logger.warn("Insufficient info for conversation with UID {}. Entry cannot be added to Conversation table.", conversation.uid);
                        continue;
                    }

                    statement.setBoolean(1, conversation.direct);
                    statement.setString(2, conversation.name);
                    statement.setString(3, conversation.uid);

                    statement.execute();
                }
            }
        } catch (SQLException e)
        {
            throw new UpgraderException(e);
        }
    }

    /**
     * Updates a conversation based on details extracted from conversation chat messages
     *
     * @param conversation The conversation to update
     * @param receiveTime The chat message recieve time
     * @param senderUid The chat message sender UID
     * @param senderName The chat message sender name
     * @param groupNameMap A map from each persisted {@link gov.tak.api.contact.IGroupContact} UID to its name
     */
    private void updateConversation(@Nonnull Conversation conversation, long receiveTime, @Nonnull String senderUid,
                                    @Nonnull String senderName, @Nonnull Map<String, String> groupNameMap)
    {
        // If it's a group that still exists in the database, we can set the info we need
        final String groupName = groupNameMap.get(conversation.uid);

        if (groupName != null)
        {
            conversation.name = groupName;
            conversation.direct = false;
            return;
        }

        // If the receiveTime > 0, the message was received from a remote user
        if (receiveTime > 0)
        {
            // If sender UID == conversation UID, it's a direct conversation
            if (conversation.uid.equals(senderUid))
            {
                conversation.name = senderName;
                conversation.direct = true;
            }
            // If sender UID != conversation UID, it's a group conversation. The sender name is not the group name, so
            // that can't be set
            else
            {
                conversation.direct = false;
            }
        }
    }

    /**
     * Holds info about a conversation and is updated as chat messages are processed
     */
    private static class Conversation
    {
        private final String uid;
        private String name = "(unknown)";

        // null by default b/c there are some edge cases where the value cannot be deduced. In this case, an error will
        // be logged and no conversation will be created.
        private Boolean direct = null;

        public Conversation(@Nonnull String uid)
        {
            this.uid = uid;
        }
    }
}
