package gov.takx.core.view.gui.contacts;

import gov.tak.api.comms.commo.ICommonCommoManager;
import gov.tak.api.comms.commo.ICotEventListener;
import gov.tak.api.contact.IContact;
import gov.tak.api.contact.IContactListener;
import gov.tak.api.contact.IContactService;
import gov.tak.api.contact.IIndividualContact;
import gov.tak.api.contact.IIndividualContactManager;
import gov.tak.api.contact.IIndividualContactManagerListener;
import gov.tak.api.contact.IndividualContactAttributeNames;
import gov.tak.api.contact.IndividualContactState;
import gov.tak.api.contact.IndividualContactUtils;
import gov.tak.api.cot.CotTypes;
import gov.tak.api.cot.detail.DetailConstants;
import gov.tak.api.cot.event.CotDetail;
import gov.tak.api.cot.event.CotEvent;
import gov.tak.api.cot.importer.ImportResult;
import gov.tak.api.util.StandardAttributeNames;
import gov.tak.core.chat.protocol.geochat.GeoChatConstants;
import gov.takx.api.annotation.NodeUUID;
import gov.takx.api.event.StartProjectServices;
import gov.takx.api.event.StopProjectServices;
import gov.takx.api.mapobject.IMapObject;
import gov.takx.api.mapobject.IMapObjectManager;
import gov.takx.api.mapobject.IMapObjectManagerListener;
import gov.takx.api.persist.IMapObjectFinder;
import gov.takx.api.platform.ICoreExecutorService;
import gov.takx.api.preferences.IPreferenceMonitor;
import gov.takx.api.timer.ITimerService;
import gov.takx.commons.lang.ExceptionUtils;
import gov.takx.core.chat.ConversationStore;
import gov.takx.platform.preferences.PreferenceEntry;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import jakarta.annotation.Priority;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import jakarta.inject.Inject;
import org.apache.commons.text.WordUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.Icon;
import javax.swing.ImageIcon;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.lang.invoke.MethodHandles;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.TimerTask;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Collectors;

import static gov.takx.core.project.ServiceOrdering.START_GenericCoreService;
import static gov.takx.core.project.ServiceOrdering.STOP_GenericCoreService;

/**
 * Manages {@link IIndividualContact}s including their state, team, role and the custom icons that display their current
 * state. With the exception of its icons, an {@link IIndividualContact}'s state should be maintained in its
 * {@link gov.tak.api.util.AttributeSet}.
 *
 * @since 3.4.1
 * @see IndividualContactWithIcon
 */
@ApplicationScoped
public class IndividualContactManager
        implements IIndividualContactManager, IContactListener, IMapObjectManagerListener, PropertyChangeListener
{
    private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    private static final long STALE_CHECK_PERIOD = TimeUnit.SECONDS.toMillis(1);

    // Stale time to use while we are waiting for the contact's first heartbeat message to arrive or when the
    // start/stale times are invalid
    public static final long DEFAULT_STALE_TIME = TimeUnit.MINUTES.toMillis(2);

    // A bug may cause older versions of ATAK to (inconsistently) set the wrong time in CoT messages, off by 7168 days,
    // so we need to normalize it
    private static final long TIME_FIX_OFFSET = TimeUnit.DAYS.toMillis(7168);
    private static final long THIRTY_DAYS = TimeUnit.DAYS.toMillis(30);

    // Map from individual contact UIDs to its contact/icon wrapper
    private final Map<String, IndividualContactWithIcon> contactWithIconMap = new ConcurrentHashMap<>();
    private final Set<IIndividualContactManagerListener> listeners = new CopyOnWriteArraySet<>();

    private final IMapObjectManager mapObjectManager;
    private final ConversationStore conversationStore;
    private long goneTimeoutMs;
    private final IMapObjectFinder mapObjectFinder;
    private final ICoreExecutorService executorService;
    private final IContactService contactService;

    private ContactStatusCotEventListener cotEventListener;
    private CheckStaleContactsTimerTask timerTask;

    private final String clientUid;
    private String clientTeam;
    private String clientRole;
    private final IPreferenceMonitor<String> clientTeamMonitor =
            PreferenceEntry.SA_REPORTING_TEAM.newStringMonitor(this::setClientTeam, false);
    private final IPreferenceMonitor<String> clientRoleMonitor =
            PreferenceEntry.SA_REPORTING_ROLE.newStringMonitor(role -> clientRole = role, false);

    private final IPreferenceMonitor<Integer> contactTimeoutMonitor =
            PreferenceEntry.CONTACT_GONE_TIMEOUT.newIntegerMonitor(
                    goneTimeout -> goneTimeoutMs = TimeUnit.SECONDS.toMillis(goneTimeout), false);

    // WebTAK adds emergency alerts as commo contacts (for some reason?). The corresponding emergency alert CoT message
    // is required to determine that the "contact" is actually an emergency alert, so we track the CoT alert ids here.
    // ATAK and WinTAK appear to rely on CoT messages rather than commo for contact events, so they don't suffer from
    // this issue. We may consider converting to that approach in the future.
    private final Set<String> emergencyAlertUids = ConcurrentHashMap.newKeySet();

    // startService isn't always called when tests are run, so we need to track it
    private boolean started;

    @Inject
    public IndividualContactManager(IMapObjectManager mapObjectManager, ConversationStore conversationStore,
                                    IMapObjectFinder mapObjectFinder, ICoreExecutorService executorService,
                                    IContactService contactService, @NodeUUID UUID nodeUuid)
    {

        this.mapObjectManager = mapObjectManager;
        this.conversationStore = conversationStore;
        this.mapObjectFinder = mapObjectFinder;
        this.executorService = executorService;
        this.contactService = contactService;

        clientUid = nodeUuid.toString();
    }

    @SuppressWarnings("unused")
    private void startService(@Observes @Priority(START_GenericCoreService) StartProjectServices event,
                              ICommonCommoManager commoManager, ITimerService timerService)
    {
        contactService.registerContactListener(this);

        mapObjectManager.addMapObjectManagerListener(this);

        cotEventListener = new ContactStatusCotEventListener();
        commoManager.addCotEventListener(cotEventListener);

        timerTask = new CheckStaleContactsTimerTask();
        timerService.scheduleAtFixedRate(timerTask, 0, STALE_CHECK_PERIOD);

        setClientTeam(clientTeamMonitor.getCurrentValue());
        clientRole = clientRoleMonitor.getCurrentValue();
        goneTimeoutMs = TimeUnit.SECONDS.toMillis(contactTimeoutMonitor.getCurrentValue());

        clientTeamMonitor.start();
        clientRoleMonitor.start();
        contactTimeoutMonitor.start();

        started = true;
    }

    @SuppressWarnings("unused")
    private void stopService(@Observes @Priority(STOP_GenericCoreService) StopProjectServices event,
                             ICommonCommoManager commoManager)
    {
        if (started)
        {
            contactService.unregisterContactListener(this);

            mapObjectManager.removeMapObjectManagerListener(this);

            contactWithIconMap.clear();
            listeners.clear();
            emergencyAlertUids.clear();

            commoManager.removeCotEventListener(cotEventListener);
            cotEventListener = null;

            timerTask.cancel();
            timerTask = null;

            clientTeamMonitor.stop();
            clientRoleMonitor.stop();
            contactTimeoutMonitor.stop();

            started = false;
        }
    }

    /**
     * Set our team name, properly capitalized.
     * @since 5.4.2
     */
    private void setClientTeam(String clientTeam)
    {
        this.clientTeam = WordUtils.capitalize(clientTeam);
    }

    /**
     * @return The local client's contact UID
     * @since 3.5
     */
    public String getClientUid()
    {
        return clientUid;
    }

    /**
     * @return The local client's team
     * @since 3.5
     */
    public String getClientContactTeam()
    {
        return clientTeam;
    }

    /**
     * @return The local client's role
     * @since 3.5
     */
    public String getClientContactRole()
    {
        return clientRole;
    }

    @Override
    @Nonnull
    public Set<IIndividualContact> getContacts()
    {
        return contactWithIconMap.values().stream().map(IndividualContactWithIcon::getContact).collect(Collectors.toSet());
    }

    /**
     * @param listener The listener to add
     */
    public void addListener(@Nonnull IIndividualContactManagerListener listener)
    {
        listeners.add(listener);
    }

    /**
     * @param listener The listener to remove
     */
    public void removeListener(@Nonnull IIndividualContactManagerListener listener)
    {
        listeners.remove(listener);
    }

    @Nonnull
    public Set<IIndividualContactManagerListener> getListeners()
    {
        return new HashSet<>(listeners);
    }

    /**
     * Update an {@link IndividualContactWithIcon}'s state when a message is received from the contact.
     * <p>
     * GeoChat contacts send periodic CoT messages that provide contact timeout info. These messages are handled by 
     * {@link ContactStatusCotEventListener}. Contacts relying on other protocols should be updated manually whenever a 
     * new message arrives.
     *
     * @param contactUid The contact's UID
     * @param messageTime The message time
     */
    public void updateContactState(@Nonnull String contactUid, long messageTime)
    {
        final IIndividualContact individualContact = getContact(contactUid);

        // The contact may not exist yet if it's the contact's first message
        if (individualContact == null) return;

        // GeoChat relies on CoT messages to perform these updates, so exclude GeoChat contacts
        if (!IndividualContactUtils.isStateManagedManually(individualContact) &&
                Arrays.stream(individualContact.getProtocols()).noneMatch(GeoChatConstants.GEOCHAT_PROTOCOL::equals))
        {
            // Only apply newer messages
            if (messageTime > IndividualContactUtils.getMostRecentMessageTime(individualContact))
            {
                updateStaleTime(individualContact, messageTime, messageTime, messageTime + DEFAULT_STALE_TIME);
                IndividualContactUtils.setState(individualContact, IndividualContactState.active);
            }
        }
    }

    /**
     * @param contactUid The contact
     * @return The contact icon that includes an indicator for the contact's current state
     * @since 5.1
     */
    @Nullable
    public Icon getContactIconWithStatus(@Nonnull String contactUid)
    {
        final IndividualContactWithIcon contact = contactWithIconMap.get(contactUid);

        return contact != null ? contact.getIconWithStatus() : null;
    }

    @Override
    @Nullable
    public IIndividualContact getContact(@Nonnull String contactUid)
    {
        final IndividualContactWithIcon contact = contactWithIconMap.get(contactUid);

        return contact != null ? contact.getContact() : null;
    }

    @Override
    public void contactAdded(@Nonnull IContact addedContact)
    {
        if (addedContact instanceof IIndividualContact contact)
        {
            final String uid = contact.getUid();

            if (clientUid.equals(uid)) return; // don't add the local client

            // Ignore a "contact" if it's actually a WebTAK emergency alert
            if (emergencyAlertUids.contains(uid))
            {
                logger.debug("Ignoring WebTAK emergency alert contact: {}", contact);
                return;
            }

            if (contactWithIconMap.containsKey(uid))
            {
                // IContactService sometimes calls contactUpdated before contactAdded
                logger.debug("Trying to add contact that already exists: {}", contact);
                return;
            }

            // Prepare the contact's icon management wrapper before initializing/calling listeners that need an icon
            final var individualContactWithIcon = new IndividualContactWithIcon(contact);
            contactWithIconMap.put(uid, individualContactWithIcon);

            // Configure a listener to forward contact state changes to IndividualContactManager listeners and clear the
            // unread messages if the contact is gone
            contact.getAttributes().addPropertyChangeListener(IndividualContactAttributeNames.STATE,
                    evt -> {
                        // Remove stale unread message counts
                        if (IndividualContactUtils.getState(contact) == IndividualContactState.gone)
                        {
                            conversationStore.getConversationForContact(contact).clearUnreadMessages();
                        }
                        ExceptionUtils.safeDoOperation(listeners, listener -> listener.contactStateChanged(contact));
                    });

            // Initialize new contacts to active unless manually managed
            if (!IndividualContactUtils.isStateManagedManually(contact))
            {
                IndividualContactUtils.setState(contact, IndividualContactState.active);
            }

            // Initialize the stale time
            IndividualContactUtils.setStaleTime(contact, System.currentTimeMillis() + DEFAULT_STALE_TIME);

            // If the map object is available, use it to update the icon and add an icon change listener
            updateIfPresent(contact, mapObject -> {
                updateContactFromMapObject(individualContactWithIcon, mapObject);
                mapObject.addPropertyChangeListener(IMapObject.ICON, this);
            });

            ExceptionUtils.safeDoOperation(listeners, listener -> listener.contactAdded(contact));

            logger.debug("Added individual contact {}", addedContact);
        }
    }

    @Override
    public void contactRemoved(@Nonnull IContact removedContact)
    {
        if (removedContact instanceof IIndividualContact individualContact)
        {
            final String uid = removedContact.getUid();

            if (contactWithIconMap.remove(uid) == null && !emergencyAlertUids.contains(uid))
            {
                logger.warn("Trying to remove contact that doesn't exist: {}", removedContact.getName());
                return;
            }

            updateIfPresent(individualContact, mapObject -> mapObject.removePropertyChangeListener(IMapObject.ICON, this));

            ExceptionUtils.safeDoOperation(listeners, listener -> listener.contactRemoved(individualContact));

            logger.debug("Removed individual contact \"{}\"", removedContact);
        }
    }

    @Override
    public void contactUpdated(@Nonnull IContact updatedContact)
    {
        if (updatedContact instanceof IIndividualContact individualContact)
        {
            final IndividualContactWithIcon contact = contactWithIconMap.get(updatedContact.getUid());

            if (contact == null)
            {
                // IContactService sometimes calls contactUpdated before contactAdded
                logger.debug("Trying to update contact that doesn't exist: {}. Contact will be added", updatedContact);
                contactAdded(updatedContact);
                return;
            }

            contact.updateContact(individualContact);
            conversationStore.updateConversationForContact(individualContact);

            ExceptionUtils.safeDoOperation(listeners, listener -> listener.contactUpdated(individualContact));

            logger.debug("Updated individual contact \"{}\"", updatedContact);
        }
    }

    @Override
    public void clientMapObjectAdded(@Nonnull IMapObject mapObject)
    {
        final IndividualContactWithIcon contact = contactWithIconMap.get(mapObject.getUid());

        if (contact != null)
        {
            updateContactFromMapObject(contact, mapObject);
            mapObject.addPropertyChangeListener(IMapObject.ICON, this);
        }
    }

    @Override
    public void clientMapObjectRemoved(@Nonnull IMapObject mapObject)
    {
        if (contactWithIconMap.containsKey(mapObject.getUid()))
        {
            mapObject.removePropertyChangeListener(IMapObject.ICON, this);
        }
    }

    @Override
    public void mapObjectPartialDelete(@Nonnull IMapObject mapObject, long timeOfDeletion)
    {
    }

    @Override
    public void propertyChange(@Nonnull PropertyChangeEvent evt)
    {
        final IMapObject mapObject = (IMapObject) evt.getSource();
        final IndividualContactWithIcon contactWithIcon = contactWithIconMap.get(mapObject.getUid());

        if (contactWithIcon == null)
        {
            logger.warn("Could not find contact for contact map object {}", mapObject.getKey());
            return;
        }

        updateContactFromMapObject(contactWithIcon, mapObject);
        ExceptionUtils.safeDoOperation(listeners, listener -> listener.contactUpdated(contactWithIcon.getContact()));
    }

    /**
     * Updates an {@link IndividualContactWithIcon} based on the contact's {@link IMapObject} properties, including the
     * contact icon, team and role.
     *
     * @param contact The contact to update
     * @param mapObject The contact's {@link IMapObject} that will provide the current contact properties
     * @since 3.5
     */
    private void updateContactFromMapObject(@Nonnull IndividualContactWithIcon contact, @Nonnull IMapObject mapObject)
    {
        final ImageIcon icon = getSizedContactIcon(mapObject);
        if (icon != null) contact.updateContactIcon(icon);

        final IIndividualContact individualContact = contact.getContact();

        final String team = mapObject.getMetaString(StandardAttributeNames.TEAM, "");
        if (!team.isEmpty()) IndividualContactUtils.setTeam(individualContact, team);

        final String role = mapObject.getMetaString(StandardAttributeNames.ATAK_ROLE_TYPE, "");
        if (!role.isEmpty()) IndividualContactUtils.setRole(individualContact, role);
    }

    /**
     * @param mapObject The contact's {@link IMapObject}
     * @return The icon to use
     */
    @Nullable
    private ImageIcon getSizedContactIcon(@Nonnull IMapObject mapObject)
    {
        return mapObject.getSizedIcon(IndividualContactWithIcon.CONTACT_ICON_SIZE);
    }

    /**
     * Attempts to find the {@link IMapObject} associated with an {@link IContact} and calls the provided consumer if 
     * it's found.
     * 
     * @param contact The contact  
     * @param consumer The consumer to call if the map object is found.
     */
    private void updateIfPresent(@Nonnull IContact contact, @Nonnull Consumer<IMapObject> consumer)
    {
        final IMapObject mapObject = mapObjectFinder.findMapObjectByUid(contact.getUid());

        if (mapObject != null) consumer.accept(mapObject);
    }

    /**
     * Updates the contact's stale time.
     * <p>
     * The stale time is calculated as currentTime + (staleTimeDuration * 1.5) to match ATAK
     *
     * @param messageTime The time the message was generated
     * @param start The start time specified in the most recent message
     * @param stale The stale time specified in the most recent message
     * @return {@code true} if the stale time was updated
     * @since 5.1 (moved from ExtendedIndividualContact)
     */
    private boolean updateStaleTime(@Nonnull IIndividualContact contact, long messageTime, long start, long stale)
    {
        final long currentTime = System.currentTimeMillis();
        final long fixedTime = fixTime(messageTime, currentTime);

        // Only update if it's the newest message
        if (fixedTime > IndividualContactUtils.getMostRecentMessageTime(contact))
        {
            // Try to handle potential CoT startTime/staleTime errors
            final long staleTime = stale >= start ? currentTime + (int) ((stale - start) * 1.5) : currentTime + DEFAULT_STALE_TIME;
            IndividualContactUtils.setStaleTime(contact, staleTime);
            IndividualContactUtils.setMostRecentMessageTime(contact, fixedTime);
            return true;
        }

        return false;
    }

    /**
     * Workaround for an ATAK bug where a message's time may be off by 7168 days. If the time is more than 30 days old,
     * we consider it broken and fix it.
     *
     * @param time The time to check
     * @param currentTime The current time
     * @return The correct time
     * @since 5.1 (moved from ExtendedIndividualContact)
     */
    private long fixTime(long time, long currentTime)
    {
        return currentTime - time > THIRTY_DAYS ? time + TIME_FIX_OFFSET : time;
    }

    /**
     * Listens for CoT messages with types that match {@link CotTypes#PREFIX_FRIENDLY} and {@link CotTypes#TASK_DELETE}
     * and updates contact statuses based on those messages.
     */
    private class ContactStatusCotEventListener implements ICotEventListener
    {
        @Override
        public CompletableFuture<ImportResult> cotEventReceived(@Nonnull CotEvent event)
        {
            return CompletableFuture.supplyAsync(() -> {
                Thread.currentThread().setName("ContactStatusCotEventListener");

                final String type = event.getType();

                // If it's a friendly message, update the contact stale time
                if (type.startsWith(CotTypes.PREFIX_FRIENDLY))
                {
                    updateIfPresent(event.getUID(),
                            contact -> updateStaleTime(contact.getContact(),
                                    event.getTime().getMilliseconds(),
                                    event.getStart().getMilliseconds(),
                                    event.getStale().getMilliseconds()),
                            IndividualContactState.active);
                }
                // Track WebTAK emergency alert "contacts" and remove them if they've already been added
                else if (type.startsWith(CotTypes.PREFIX_EMERGENCY))
                {
                    final String uid = event.getUID();
                    emergencyAlertUids.add(uid);

                    final IIndividualContact contact = getContact(uid);
                    if (contact != null)
                    {
                        logger.debug("Removing WebTAK emergency alert contact: {}", contact);
                        contactService.removeContact(contact.getUid());
                    }
                }
                // If it's a deletion message, immediately set the contact inactive
                else if (CotTypes.TASK_DELETE.equals(type))
                {
                    updateIfPresent(getUidFromLink(event),
                            contact -> event.getTime().getMilliseconds() > IndividualContactUtils.getMostRecentMessageTime(contact.getContact()),
                            IndividualContactState.inactive);
                }

                return ImportResult.IGNORE;
            }, executorService);
        }

        /**
         * Use the provided {@code Consumer} to update the contact specified by {@code uid} if it is in the contact map.
         *
         * @param uid      The contact's UID
         * @param function Called if the contact is present and returns a boolean indicating if the state should be 
         *                 updated
         * @param state    The state to set on the contact after it has been processed
         */
        private void updateIfPresent(@Nullable String uid,
                                     @Nonnull Function<IndividualContactWithIcon, Boolean> function,
                                     @Nonnull IndividualContactState state)
        {
            if (uid == null) return;

            final IndividualContactWithIcon contact = contactWithIconMap.get(uid);

            if (contact != null)
            {
                if (function.apply(contact))
                {
                    final IIndividualContact individualContact = contact.getContact();

                    if (IndividualContactUtils.setState(individualContact, state))
                    {
                        ExceptionUtils.safeDoOperation(listeners, listener -> listener.contactStateChanged(individualContact));
                    }
                }
            }
        }

        /**
         * Returns the UID contained in the {@code link} element of a CoT message or {@code null} if it cannot be found.
         *
         * @param cotEvent The event to inspect
         * @return The UID or {@code null} if the value could not be found
         */
        @Nullable
        private String getUidFromLink(@Nonnull CotEvent cotEvent)
        {
            final CotDetail cotDetail = cotEvent.getDetail();

            if (cotDetail != null)
            {
                final CotDetail linkDetail = cotDetail.getFirstChildByName(DetailConstants.LINK);
                if (linkDetail != null) return linkDetail.getAttribute(DetailConstants.ATTR_UID);
            }

            return null;
        }
    }

    /**
     * Compares each contact's stale time to the current time and marks them as inactive or removes them from the view
     * model as needed.
     *
     * @see PreferenceEntry#CONTACT_GONE_TIMEOUT
     */
    private class CheckStaleContactsTimerTask extends TimerTask
    {
        @Override
        public void run()
        {
            final long currentTime = System.currentTimeMillis();

            contactWithIconMap.values().stream().map(IndividualContactWithIcon::getContact)
                    .filter(contact -> IndividualContactUtils.getState(contact) != IndividualContactState.gone
                            && !IndividualContactUtils.isStateManagedManually(contact))
                    .forEach(contact -> {
                        final long staleTime = IndividualContactUtils.getStaleTime(contact);

                        // the stale time has passed
                        if (staleTime < currentTime)
                        {
                            final long idleTime = currentTime - staleTime;
                            // Contacts that have streaming endpoints should not be removed/gone
                            final var hasStreamingEndpoint =
                                    contact.getAttributes().containsAttribute(IndividualContactAttributeNames.ENDPOINT_ID);

                            IndividualContactUtils.setState(contact,
                                    // a negative gone timeout disables this feature
                                    goneTimeoutMs >= 0 && idleTime > goneTimeoutMs && !hasStreamingEndpoint
                                            ? IndividualContactState.gone
                                            : IndividualContactState.inactive);
                        }
                    });
        }
    }
}