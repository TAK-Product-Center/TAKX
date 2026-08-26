package gov.tak.platform.chat;

import gov.tak.api.annotation.IncubatingApi;
import gov.tak.api.chat.ChatOperationResult;
import gov.tak.api.chat.IChatMessage;
import gov.tak.api.chat.IChatMessageBuilder;
import gov.tak.api.chat.IChatMessageFinder;
import gov.tak.api.chat.IChatOperationCallback;
import gov.tak.api.chat.IChatService;
import gov.tak.api.chat.IChatServiceListener;
import gov.tak.api.chat.protocol.IChatProtocolHandler;
import gov.tak.api.chat.protocol.IChatProtocolHandlerDelegate;
import gov.tak.api.contact.IContact;
import gov.tak.api.contact.IContactService;
import gov.tak.api.contact.IGroupContact;
import gov.tak.api.contact.IIndividualContact;
import gov.tak.api.maps.coords.GeoPoint;
import gov.tak.api.util.AttributeSet;
import gov.tak.platform.chat.client.IChatServiceClient;
import gov.tak.platform.chat.client.IChatServiceClientDelegate;
import gov.tak.platform.contact.Contacts;
import gov.tak.platform.contact.IndividualContactBuilder;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArraySet;

/**
 * Internal implementation of the {@link IChatService} interface that also acts as the entry point to the Chat Service
 * API via the {@link #getInstance()} method.
 *
 * @since 3.0 (moved from gov.tak.api in 3.4)
 */
@IncubatingApi(since = "3.0")
public class ChatService implements IChatService
{
    private static final Logger logger = LoggerFactory.getLogger(ChatService.class);

    private volatile boolean started;

    // The protocol handled by the host's default protocol handler
    private String defaultProtocol;

    // The delegate injected into protocol handlers to provide them with chat services
    private ProtocolHandlerDelegate protocolHandlerDelegate;

    // The chat client
    private IChatServiceClient client;

    // Contact service used to retrieve contacts by UID
    private IContactService contactService;

    // Protocol to handler map, including the primary host and plugin handlers
    private final Map<String, IChatProtocolHandler> protocolToHandlerMap = new ConcurrentHashMap<>();

    // All the listeners (except for the client which is handled separately)
    private final Set<IChatServiceListener> listeners = new CopyOnWriteArraySet<>();

    // A local client contact instance for use in groups
    private IContact clientContact;

    // Use initialization-on-demand holder to ensure thread safety while maintaining performance.
    private static final class ChatServiceHolder
    {
        static final IChatService CHAT_SERVICE_INSTANCE = new ChatService();
    }

    ChatService()
    {
    }

    /**
     * @return The chat service instance used to interact with the Chat Service API`
     */
    public static IChatService getInstance()
    {
        return ChatServiceHolder.CHAT_SERVICE_INSTANCE;
    }

    /**
     * Starts the chat service.
     * <p>
     * This method should <em>only</em> be used by the host application and must be called before any messages can be sent.
     *
     * @param defaultProtocolHandler The default protocol handler
     * @param client The client that will process incoming messages
     * @param contactService The service used to manage contacts
     * @since 3.4 (moved from interface)
     */
    public synchronized void start(@Nonnull IChatProtocolHandler defaultProtocolHandler, @Nonnull IChatServiceClient client,
                                   @Nonnull IContactService contactService)
    {
        this.client = client;
        this.contactService = contactService;

        if (started || defaultProtocol != null)
        {
            logger.error("Attempting to start a running chat service");
            throw new IllegalStateException("Attempting to start a running chat service");
        }

        defaultProtocol = defaultProtocolHandler.getProtocol();

        final var existingHandler = protocolToHandlerMap.get(defaultProtocol);
        if (existingHandler != null)
        {
            logger.warn("Registered handler specifies the default protocol '{}'; ignoring {}", defaultProtocol, existingHandler.getClass());
        }

        protocolToHandlerMap.put(defaultProtocol, defaultProtocolHandler);

        client.setChatService(this, new ChatServiceClientDelegate());

        clientContact = makeClientContact(client);
        protocolHandlerDelegate = new ProtocolHandlerDelegate();

        // Start all the protocol handlers
        protocolToHandlerMap.values().forEach(this::startProtocolHandler);

        started = true;
    }

    /**
     * Stops the chat service and all registered service handlers.
     * <p>
     * This method should <em>only</em> be called by the host application. No messages can be set after it is called.
     * @since 3.4 (moved from interface)
     */
    public void stop()
    {
        started = false;

        protocolToHandlerMap.values().forEach(IChatProtocolHandler::stop);
        protocolToHandlerMap.clear();

        defaultProtocol = null;
        protocolHandlerDelegate = null;
    }

    @Nonnull
    @Override
    public String getDefaultProtocol()
    {
        return defaultProtocol;
    }

    @Override
    public synchronized void registerProtocolHandler(@Nonnull IChatProtocolHandler handler) throws IllegalArgumentException
    {
        if (handler.getProtocol().equals(defaultProtocol))
        {
            throw new IllegalArgumentException("You cannot register a handler on the host's default protocol: " + defaultProtocol);
        }

        // Don't use computeIfAbsent (ATAK support)
        final String protocol = handler.getProtocol();
        final var existingHandler = protocolToHandlerMap.get(protocol);

        if (existingHandler == null)
        {
            protocolToHandlerMap.put(protocol, handler);
            if (started) startProtocolHandler(handler);
        } else
        {
            throw new IllegalArgumentException("Cannot register IChatProtocolHandler for protocol " + protocol +
                    " because it is already handled by " + existingHandler.getClass());
        }
    }

    /**
     * Start the given protocol handler and update our known protocols list.
     * @since 5.3
     */
    private void startProtocolHandler(@Nonnull IChatProtocolHandler handler)
    {
        handler.start(protocolHandlerDelegate);
        updateProtocols(clientContact, handler.getProtocol(), false);
    }

    @Override
    public void unregisterProtocolHandler(@Nonnull IChatProtocolHandler handler)
    {
        final String protocol = handler.getProtocol();

        if (protocol.equals(defaultProtocol))
        {
            throw new IllegalArgumentException("You cannot unregister the host's default protocol handler: " + defaultProtocol);
        }

        if (protocolToHandlerMap.remove(protocol) != null)
        {
            handler.stop();
            updateProtocols(clientContact, protocol, true);
        }
    }

    @Override
    public void addListener(@Nonnull IChatServiceListener listener)
    {
        listeners.add(listener);
    }

    @Override
    public void removeListener(@Nonnull IChatServiceListener listener)
    {
        listeners.remove(listener);
    }

    @Override
    public void sendMessage(@Nonnull IChatMessage message, @Nullable IChatOperationCallback callback)
            throws IllegalArgumentException
    {
        final IContact targetContact = contactService.getContact(message.getConversationUid());
        final String[] protocols = targetContact.getProtocols();
        final IChatProtocolHandler handler = findHandler(protocols);

        if (handler != null)
        {
            final ChatOperationResult result = handler.sendMessage(targetContact, message);

            if (result.getStatus() == ChatOperationResult.Status.SUCCESS)
            {
                client.messageSent(targetContact, message);

                for (IChatServiceListener listener : listeners)
                {
                    try
                    {
                        listener.messageSent(targetContact, message);
                    } catch (Exception e)
                    {
                        logger.error("Error notifying chat listener {} -> messageSent()", listener, e);
                    }
                }
            }

            if (callback != null) callback.operationAttempted(result);
        } else
        {
            if (callback != null)
            {
                notifyCallbackNoMatchingProtocol(callback, ChatOperationResult.Operation.SEND_MESSAGE, protocols,
                        "Unable to send message to contact " + targetContact, targetContact);
            }
        }
    }

    /**
     * Requests that a group contact be updated to contain the provided set of group members.
     *
     * @param groupContact The group identifier
     * @param groupMembers The updated set of group members
     * @param callback Optional callback that is notified with the group update status
     */
    public void updateGroup(@Nonnull IGroupContact groupContact, @Nonnull Set<IContact> groupMembers,
                            @Nullable IChatOperationCallback callback)
    {
        final String[] protocols = groupContact.getProtocols();
        final IChatProtocolHandler handler = findHandler(protocols);

        if (handler != null)
        {
            // Ensure the client contact is in the group
            groupMembers = addClientContact(groupMembers);

            // Verify that all proposed group members have the requested protocol.
            verifyContactProtocol(groupMembers, handler.getProtocol());

            final ChatOperationResult result = handler.updateGroup(groupContact, groupMembers);

            if (callback != null) callback.operationAttempted(result);
        } else
        {
            if (callback != null)
            {
                notifyCallbackNoMatchingProtocol(callback, ChatOperationResult.Operation.UPDATE_GROUP, protocols,
                        "Unable to update group " + groupContact.getName(), groupContact);
            }
        }
    }

    /**
     * Requests that a new group contact be created for the given chat service protocol (e.g. GeoChat, XMPP, etc.). All
     * requested group members must support the given chat service protocol, and group members cannot be empty.
     *
     * @param groupName The name of the group to create (i.e. the display name of the IGroupContact that will be created)
     * @param groupMembers The group members that should be affiliated with the created group contact
     * @param parentGroup The parent group contact, or {@code null} if the group being created is not a subgroup
     * @param protocol The chat service protocol that should be used to create the group contact
     * @param callback Optional callback that is notified of the group creation request result
     * @throws IllegalArgumentException if group members is empty or a given group member does not support the requested protocol
     */
    public void createGroup(@Nonnull String groupName, @Nonnull Set<IContact> groupMembers,
                            @Nullable IGroupContact parentGroup, @Nonnull String protocol,
                            @Nullable IChatOperationCallback callback)
            throws IllegalArgumentException
    {
        // Ensure the client contact is in the group
        groupMembers = addClientContact(groupMembers);

        // Verify that all proposed group members have the requested protocol.
        verifyContactProtocol(groupMembers, protocol);

        final String[] protocols = {protocol};
        final IChatProtocolHandler handler = findHandler(protocols);

        if (handler != null)
        {
            final ChatOperationResult result = handler.createGroup(groupName, groupMembers,
                    parentGroup != null ? parentGroup : handler.getDefaultGroupContact());

            if (callback != null) callback.operationAttempted(result);
        } else
        {
            if (callback != null)
            {
                notifyCallbackNoMatchingProtocol(callback, ChatOperationResult.Operation.CREATE_GROUP, protocols,
                        "Unable to create group " + groupName, null);
            }
        }
    }

    /**
     * Requests that a group contact be deleted.
     *
     * @param groupContact The group identifier
     * @param callback Optional callback that is notified with the group deletion request result
     */
    public void deleteGroup(@Nonnull IGroupContact groupContact, @Nullable IChatOperationCallback callback)
    {
        final String[] protocols = groupContact.getProtocols();
        final IChatProtocolHandler handler = findHandler(protocols);

        if (handler != null)
        {
            final ChatOperationResult result = handler.deleteGroup(groupContact);

            if (callback != null) callback.operationAttempted(result);
        } else
        {
            if (callback != null)
            {
                notifyCallbackNoMatchingProtocol(callback, ChatOperationResult.Operation.DELETE_GROUP, protocols,
                        "Unable to delete group " + groupContact.getName(), groupContact);
            }
        }
    }

    @Nonnull
    @Override
    public IChatMessageBuilder getChatMessageBuilder(@Nonnull String text, long sendTime,
                                                     @Nonnull String conversationUid)
    {
        return client.getChatMessageBuilder(text, sendTime, conversationUid);
    }

    @Nonnull
    @Override
    public IChatMessageFinder getChatMessageFinder()
    {
        return client.getChatMessageFinder();
    }

    /**
     * Modifies an {@link IContact}'s protocol list by adding or removing a protocol.
     * 
     * @param contact The contact to update
     * @param protocol The protocol to add or remove
     * @param remove If {@code true}, the protocol will be removed. If {@code false}, the protocol will be added.
     * @since 3.5
     */
    private void updateProtocols(@Nonnull IContact contact, @Nonnull String protocol, boolean remove)
    {
        final List<String> protocols = new ArrayList<>(Arrays.asList(contact.getProtocols()));

        if (remove)
        {
            protocols.remove(protocol);
        } else
        {
            if (!protocols.contains(protocol)) protocols.add(protocol);
        }

        contact.setProtocols(protocols.toArray(new String[0]));
    }

    /**
     * Find the correct provider and send a message receipt back to the sender.
     *
     * @param contact The sender
     * @param messageUid The UID of the message being acknowledged
     * @param read {@code true} if the message is being confirmed as "read"
     * @param callback Updated with status of send attempt
     * @since 3.4
     */
    private void sendReceipt(@Nonnull IIndividualContact contact, @Nonnull String messageUid, boolean read,
                             @Nullable IChatOperationCallback callback)
    {
        final String[] protocols = contact.getProtocols();
        final IChatProtocolHandler handler = findHandler(protocols);

        if (handler != null)
        {
            final ChatOperationResult result = handler.sendReceipt(contact, messageUid, read);

            if (callback != null) callback.operationAttempted(result);
        } else
        {
            if (callback != null)
            {
                notifyCallbackNoMatchingProtocol(callback, ChatOperationResult.Operation.SEND_RECEIPT, protocols,
                        "Unable to send receipt to " + contact, contact);
            }
        }
    }

    /**
     * Finds a {@link IChatProtocolHandler} that can handle one of the specified protocols. The {@link #defaultProtocol}
     * will be prioritized.
     *
     * @param protocols The protocols
     * @return The handler that matches one of the protocols or {@code null} if there's no match
     * @throws IllegalArgumentException if there are no protocols to check
     * @since 3.4
     */
    @Nullable
    private IChatProtocolHandler findHandler(@Nonnull String[] protocols) throws IllegalArgumentException
    {
        if (protocols.length == 0) throw new IllegalArgumentException("Can't find handler given no protocols");

        final List<String> protocolList = Arrays.asList(protocols);

        // Prioritize the default protocol by checking it first
        if (protocolList.contains(defaultProtocol)) return protocolToHandlerMap.get(defaultProtocol);

        // don't use streams (ATAK support)
        for (Map.Entry<String, IChatProtocolHandler> entry : protocolToHandlerMap.entrySet())
        {
            if (protocolList.contains(entry.getKey())) return entry.getValue();
        }

        return null;
    }

    private void notifyCallbackNoMatchingProtocol(IChatOperationCallback callback, ChatOperationResult.Operation operation,
                                                  String[] protocols, String customMessage, IContact contact)
    {
        callback.operationAttempted(new ChatOperationResult(operation, ChatOperationResult.Status.FAILURE,
                "Could not find chat service handler for protocol(s) "
                        + Arrays.toString(protocols) + ". " + customMessage,
                contact));
    }

    /**
     * Verifies that all the contacts in the set support the specified protocol.
     *
     * @param contacts The contacts to check
     * @param protocol The protocol that must be supported
     * @throws IllegalArgumentException if any contact does not support the protocol
     * @since 3.4
     */
    private void verifyContactProtocol(@Nonnull Set<IContact> contacts, @Nonnull String protocol)
            throws IllegalArgumentException
    {
        for (IContact contact : contacts)
        {
            if (!contact.supportsProtocol(protocol))
            {
                throw new IllegalArgumentException("Group member '" + contact.getName() + "' does not support protocol '" +
                        protocol + "' required to create new group contact.");
            }
        }
    }

    /**
     * Checks to see if the client {@link IContact} is in a given set. If not, a new set is created because we cannot
     * guarantee that the set is mutable and the client is added.
     *
     * @param contacts The set of Contacts
     * @return A set that includes the client contact.
     * @since 3.4
     */
    private Set<IContact> addClientContact(Set<IContact> contacts)
    {
        // Make sure the client is in the group
        if (!contacts.contains(clientContact))
        {
            // Use a new set in case the existing one is immutable
            contacts = new HashSet<>(contacts);
            contacts.add(clientContact);
        }

        return contacts;
    }

    /**
     * Create a contact for the local client.
     *
     * @return A local client contact.
     * @since 3.4
     */
    private IContact makeClientContact(IChatServiceClient client)
    {
        final AttributeSet attributeSet = new AttributeSet();
        attributeSet.setAttribute(Contacts.PROTOCOL_ATTRIBUTE_KEY, new String[]{defaultProtocol});

        return new IndividualContactBuilder()
                .withUniqueContactId(client.getUid())
                .withDisplayName(client.getName())
                .withAttributes(attributeSet)
                .build();
    }

    /**
     * Provides {@link IChatProtocolHandler}'s with chat services.
     *
     * @since 3.4
     */
    private final class ProtocolHandlerDelegate implements IChatProtocolHandlerDelegate
    {
        @Override
        public String getClientUid()
        {
            return client.getUid();
        }

        @Override
        public String getClientName()
        {
            return client.getName();
        }

        @Override
        public GeoPoint getClientLocation()
        {
            return client.getLocation();
        }

        @Nonnull
        @Override
        public IChatMessageBuilder getChatMessageBuilder(@Nonnull String senderUid, @Nonnull String senderName,
                                                         @Nonnull String text, long sendTime,
                                                         @Nonnull String conversationUid)
        {
            return client.getChatMessageBuilder(senderUid, senderName, text, sendTime, conversationUid);
        }

        @Override
        public void receiveMessage(@Nonnull IContact contact, @Nonnull IChatMessage message)
        {
            client.messageReceived(contact, message);

            for (IChatServiceListener listener : listeners)
            {
                try
                {
                    listener.messageReceived(contact, message);
                } catch (Exception e)
                {
                    logger.error("Error notifying chat listener {} -> messageReceived", listener, e);
                }
            }
        }

        @Override
        public void receiveReceipt(@Nonnull ChatReceipt receipt)
        {
            client.receiveReceipt(receipt);
        }
    }

    /**
     * Provides the {@link IChatServiceClient} with chat services.
     *
     * @since 3.4
     */
    private final class ChatServiceClientDelegate implements IChatServiceClientDelegate
    {
        @Override
        public void sendReceipt(@Nonnull IIndividualContact contact, @Nonnull String messageUid, boolean read,
                                @Nullable IChatOperationCallback callback)
        {
            ChatService.this.sendReceipt(contact, messageUid, read, callback);
        }

        @Override
        public String getDefaultProtocol()
        {
            return defaultProtocol;
        }
    }
}
