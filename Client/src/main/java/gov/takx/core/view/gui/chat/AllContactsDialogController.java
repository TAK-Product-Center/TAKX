package gov.takx.core.view.gui.chat;

import gov.tak.api.comms.commo.ICommonCommoManager;
import gov.tak.api.comms.takserver.IServerContactManager;
import gov.tak.api.comms.takserver.ITakServerConnection;
import gov.tak.api.comms.takserver.ITakServerConnectionManager;
import gov.tak.api.contact.IContact;
import gov.tak.api.contact.IContactService;
import gov.tak.api.contact.IIndividualContact;
import gov.tak.api.contact.IIndividualContactManager;
import gov.tak.api.contact.IIndividualContactManagerListener;
import gov.tak.api.contact.IndividualContactAttributeNames;
import gov.tak.api.contact.IndividualContactState;
import gov.tak.api.contact.IndividualContactUtils;
import gov.tak.platform.contact.IndividualContactBuilder;
import gov.takx.api.annotation.PseudoBean;
import gov.takx.api.notification.INotificationService;
import gov.takx.api.notification.Notification;
import gov.takx.api.platform.ICoreExecutorService;
import gov.takx.core.view.gui.CoreComponentType;
import gov.takx.core.view.gui.UISpaceManager;
import gov.takx.core.view.gui.contacts.ContactsPanelController;
import gov.takx.core.view.gui.contacts.GroupContactManager;
import gov.takx.core.view.gui.contacts.IndividualContactManager;
import jakarta.annotation.Nonnull;
import jakarta.annotation.PostConstruct;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.jetbrains.annotations.VisibleForTesting;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.invoke.MethodHandles;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * Displays a list of all local and server contacts. When a server contact is selected, an endpoint is configured that
 * allows messages to be sent to the contact when offline.
 *
 * @see ICommonCommoManager#configKnownEndpointForContact(String, String, String) 
 * @since 5.7 (renamed in 5.8)
 */
@ApplicationScoped
public class AllContactsDialogController
{
    private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    private final ICommonCommoManager commonCommoManager;
    private final IServerContactManager serverContactManager;
    private final IndividualContactManager individualContactManager;
    private final UISpaceManager uiSpaceManager;
    private final ContactsPanelController contactsPanelController;
    private final INotificationService notificationService;
    private final ITakServerConnectionManager serverConnectionManager;
    private final GroupContactManager groupContactManager;

    @Inject
    AllContactsDialogController(ICommonCommoManager commonCommoManager, IServerContactManager serverContactManager,
                                IndividualContactManager individualContactManager, UISpaceManager uiSpaceManager,
                                ContactsPanelController contactsPanelController, INotificationService notificationService,
                                ITakServerConnectionManager serverConnectionManager, GroupContactManager groupContactManager)
    {
        this.commonCommoManager = commonCommoManager;
        this.serverContactManager = serverContactManager;
        this.individualContactManager = individualContactManager;
        this.uiSpaceManager = uiSpaceManager;
        this.contactsPanelController = contactsPanelController;
        this.notificationService = notificationService;
        this.serverConnectionManager = serverConnectionManager;
        this.groupContactManager = groupContactManager;
    }

    /**
     * Displays the modal contact selection dialog that includes contacts from all active servers and local contacts.
     *
     * @since 5.8
     */
    public void showSelectionDialog()
    {
        final Supplier<Collection<LocalOrServerContact>> supplier =
                () -> {
                    final var contacts = new ArrayList<LocalOrServerContact>();

                    // Add server contacts
                    serverConnectionManager.getTakServerConnections(true).stream()
                            .filter(sc -> sc.getStreamingNetInterface() != null)
                            .forEach(server -> addServerContacts(server, contacts));

                    // Add local contacts that haven't already been added
                    final var serverContactUids = contacts.stream().map(sc -> sc.clientEndpoint().uid()).toList();
                    contacts.addAll(individualContactManager.getContacts().stream()
                            .filter(ic -> !serverContactUids.contains(ic.getUid()))
                            .map(c -> new LocalOrServerContact(c, null, null, individualContactManager.getContactIconWithStatus(c.getUid())))
                            .toList());

                    // Add the All Chat Rooms group contact
                    contacts.add(new LocalOrServerContact(groupContactManager.getAllChatRoomsContact(), null,
                            null, GroupContactManager.GROUP_ICON));

                    return contacts;
                };

        new AllContactsDialog(supplier, this::addContact).setVisible(true);
    }

    /**
     * Adds a server's contact endpoints to the supplied list.
     * 
     * @param server The server
     * @param contacts The contact list
     * @since 5.8
     */
    private void addServerContacts(@Nonnull ITakServerConnection server,
                                   @Nonnull List<LocalOrServerContact> contacts)
    {
        final var serverName = server.getDisplayName();
        final var streamingNetInterface = server.getStreamingNetInterface();
        final var streamingId = streamingNetInterface != null ? streamingNetInterface.streamId : null;

        if (streamingId == null)
        {
            logger.warn("The provided TAK Server {} does not have a commo interface with a stream id", serverName);

            notificationService.show(new Notification("Cannot access contacts on server " + serverName, Notification.Type.Error));

            return;
        }

        final var endpointsAndIds = serverContactManager.getServerClientEndpoints(server.getId()).values().stream()
                .map(endpoint -> new LocalOrServerContact(null, endpoint, streamingId,
                        individualContactManager.getContactIconWithStatus(endpoint.uid())))
                .toList();

        contacts.addAll(endpointsAndIds);
    }

    /**
     * Opens a chat with a contact. If a server contact, first configures the contact's server clientEndpoint.
     *
     * @param localOrServerContact The local or server contact info
     */
    @VisibleForTesting
    void addContact(@Nonnull LocalOrServerContact localOrServerContact)
    {
        final var localContact = localOrServerContact.localContact();

        if (localContact != null)
        {
            openConversation(localContact);
            return;
        }

        final var clientEndpoint = localOrServerContact.clientEndpoint();
        final var endpointId = localOrServerContact.streamingId();

        final var uid = clientEndpoint.uid();
        final var callsign = clientEndpoint.callsign();
        final var contact = individualContactManager.getContact(uid);

        // If the contact doesn't already exist, we need to listen for its creation so we can update its callsign
        if (contact == null) new ServerContactUpdater(uid, callsign, endpointId, this::openConversation);

        final var endpointContact = commonCommoManager.configKnownEndpointForContact(uid, callsign, endpointId);

        // If the contact already existed and an endpoint was successfully configured, update it with its server endpoint
        if (contact != null && endpointContact != null)
        {
            contact.getAttributes().setAttribute(IndividualContactAttributeNames.ENDPOINT_ID, endpointId);
            openConversation(contact);
        }
    }

    /**
     * Opens the {@link gov.takx.core.view.gui.contacts.ContactsPanel} and starts/opens a conversation with the specified
     * {@link IContact}.
     *
     * @param contact The contact
     */
    private void openConversation(@Nonnull IContact contact)
    {
        uiSpaceManager.showComponent(CoreComponentType.CONTACTS_PANEL);
        contactsPanelController.onSelect(contact);
    }

    /**
     * When a server contact endpoint is registered with commo, the resulting commo {@link com.atakmap.commoncommo.Contact}
     * only provides a UID. This listener waits for the contact to be added to the {@link IIndividualContactManager} and
     * then sets its callsign and server endpoint attribute.
     */
    @PseudoBean
    private static class ServerContactUpdater implements IIndividualContactManagerListener
    {
        private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

        // Timeout before we'll stop listening for the contact to be added
        private static final int LISTEN_TIMEOUT_SECONDS = 1;

        @Inject
        private IContactService contactService;

        @Inject
        private ICoreExecutorService executorService;

        @Inject
        private IIndividualContactManager individualContactManager;

        private final String uid;
        private final String callsign;
        private final String endpointId;
        private final Consumer<IContact> contactConsumer;

        private ScheduledFuture<?> future;

        /**
         * @param uid The contact's UID
         * @param callsign The contact's callsign
         * @param endpointId The endpoint id that is being configured for the contact
         * @param contactConsumer The consumer that will be notified once the contact has been updated
         */
        public ServerContactUpdater(@Nonnull String uid, @Nonnull String callsign, @Nonnull String endpointId,
                                    @Nonnull Consumer<IContact> contactConsumer)
        {
            this.uid = uid;
            this.callsign = callsign;
            this.endpointId = endpointId;
            this.contactConsumer = contactConsumer;
        }

        @PostConstruct
        private void postConstruct()
        {
            individualContactManager.addListener(this);

            // Unregister the listener and log an error if the contact isn't added after a period of time
            future = executorService.schedule(() -> {
                individualContactManager.removeListener(this);
                logger.warn("Contact not added. UID: {} callsign: {} clientEndpoint id: {}", uid, callsign, endpointId);
            }, LISTEN_TIMEOUT_SECONDS, TimeUnit.SECONDS);
        }

        @Override
        public void contactAdded(@Nonnull IIndividualContact contact)
        {
            if (contact.getUid().equals(uid))
            {
                future.cancel(true);
                individualContactManager.removeListener(this);

                // Update the callsign
                final var updatedContact = new IndividualContactBuilder()
                        .withUniqueContactId(uid)
                        .withDisplayName(callsign)
                        .withAttributes(contact.getAttributes())
                        .build();
                contactService.updateContact(updatedContact);

                updatedContact.getAttributes().setAttribute(IndividualContactAttributeNames.ENDPOINT_ID, endpointId);
                IndividualContactUtils.setState(updatedContact, IndividualContactState.inactive);

                contactConsumer.accept(updatedContact);
            }
        }
    }
}
