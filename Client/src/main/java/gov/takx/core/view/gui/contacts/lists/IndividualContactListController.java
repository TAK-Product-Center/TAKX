package gov.takx.core.view.gui.contacts.lists;

import gov.tak.api.contact.IIndividualContact;
import gov.tak.api.contact.IIndividualContactManagerListener;
import gov.tak.api.contact.IndividualContactState;
import gov.tak.api.contact.IndividualContactUtils;
import gov.takx.api.annotation.InvokeOnEDT;
import gov.takx.api.annotation.PseudoBean;
import gov.takx.api.ui.IResourceCleaner;
import gov.takx.commons.lang.AlphaNumericComparator;
import gov.takx.core.view.gui.contacts.IndividualContactManager;
import jakarta.annotation.Nonnull;
import jakarta.annotation.PostConstruct;
import jakarta.inject.Inject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.DefaultListModel;
import javax.swing.JList;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

import java.lang.invoke.MethodHandles;
import java.util.Iterator;
import java.util.function.Function;

/**
 * Controller for lists that display {@link IIndividualContact}s. The controller listens for
 * {@link IndividualContactManager} notifications and updates the list model when contacts are added, updated and
 * removed.
 * <p>
 * Note: It's up to the creator of this controller to set an appropriate renderer on the JList.
 *
 * @since 3.5
 * @param <T> The list element type
 */
@PseudoBean
public class IndividualContactListController<T> implements IIndividualContactManagerListener, IResourceCleaner
{
    private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    // orders the contacts in the model by name
    private static final AlphaNumericComparator<IIndividualContact> CONTACT_COMPARATOR =
            new AlphaNumericComparator<>(IIndividualContact::getName);

    private final JList<T> list;
    // returns a list item for a contact
    private final Function<IIndividualContact, T> itemProducer;
    // returns the contact associated with a list item
    private final Function<T, IIndividualContact> contactRetriever;
    private final IContactSelectionListener<T> selectionListener;
    private final DefaultListModel<T> model = new DefaultListModel<>();

    private final ContactListSelectionListener listSelectionListener = new ContactListSelectionListener();

    @Inject
    private IndividualContactManager individualContactManager;

    /**
     * @param list The list that will be managed by this controller
     * @param itemProducer Function that takes a contact and return a new list item for the contact
     * @param contactRetriever Function that takes a list item and returns the contact associated with it
     * @param selectionListener Listener that will be notified when a contact is selected
     */
    public IndividualContactListController(@Nonnull JList<T> list,
                                           @Nonnull Function<IIndividualContact, T> itemProducer,
                                           @Nonnull Function<T, IIndividualContact> contactRetriever,
                                           @Nonnull IContactSelectionListener<T> selectionListener)
    {
        this.list = list;
        this.itemProducer = itemProducer;
        this.contactRetriever = contactRetriever;
        this.selectionListener = selectionListener;
        list.setModel(model);
    }

    @PostConstruct
    private void postConstruct()
    {
        final String clientUid = individualContactManager.getClientUid();

        // Add all individual contacts (besides the client) ordered by name
        individualContactManager.getContacts().stream()
                .filter(contact -> !contact.getUid().equals(clientUid))
                .sorted(CONTACT_COMPARATOR)
                .forEach(this::contactAdded);

        individualContactManager.addListener(this);

        list.addListSelectionListener(listSelectionListener);
    }

    @Override
    public void releaseResources()
    {
        individualContactManager.removeListener(this);
        list.removeListSelectionListener(listSelectionListener);
    }

    /**
     * @return The list model
     */
    public DefaultListModel<T> getModel()
    {
        return model;
    }

    @Override
    public void contactAdded(@Nonnull IIndividualContact contact)
    {
        // If a contact was marked as gone when it was created, don't add it to the model
        if (isGone(contact)) return;

        addToModel(contact);

        logger.debug("Added contact \"{}\" to contacts list panel.", contact);
    }

    @Override
    public void contactRemoved(@Nonnull IIndividualContact contact)
    {
        removeFromModel(contact);

        logger.debug("Removed contact \"{}\" from contacts list panel.", contact);
    }

    @Override
    public void contactUpdated(@Nonnull IIndividualContact contact)
    {
        // Ignore updates if the contact is marked as gone
        if (isGone(contact)) return;

        // re-add to force resort
        removeFromModel(contact);
        addToModel(contact);

        logger.debug("Updated contact \"{}\" in contacts list panel.", contact);
    }

    @Override
    public void contactStateChanged(@Nonnull IIndividualContact contact)
    {
        if (isGone(contact))
        {
            removeFromModel(contact);
        }
        // newly active/inactive and not in model
        else if (findEntry(contact) == null)
        {
            addToModel(contact);
        }
        // repaint to ensure latest state is displayed
        else
        {
            list.repaint();
        }
    }

    /**
     * @param contact The contact to check
     * @return {@code true} if the {@link IIndividualContact} has been marked as {@link IndividualContactState#gone}
     * @since 5.2
     */
    private boolean isGone(@Nonnull IIndividualContact contact)
    {
        return IndividualContactUtils.getState(contact) == IndividualContactState.gone;
    }

    /**
     * @param contact The contact to add to the view model
     * @since 3.4.1
     */
    @InvokeOnEDT
    private void addToModel(@Nonnull IIndividualContact contact)
    {
        // Update events may arrive before add events. Avoid duplicates by ignoring the late add event.
        if (findEntry(contact) != null || individualContactManager.getClientUid().equals(contact.getUid())) return;

        // Insert the new contact in sorted order
        final int size = model.size();
        int index = 0;

        while (index < size && CONTACT_COMPARATOR.compare(contact, contactRetriever.apply(model.getElementAt(index))) >= 0) index++;
        model.insertElementAt(itemProducer.apply(contact), index);
    }

    /**
     * Trivial but included for parity with {@link #addToModel(IIndividualContact)}
     *
     * @param contact The contact to remove from the view model
     */
    @InvokeOnEDT
    private void removeFromModel(@Nonnull IIndividualContact contact)
    {
        model.removeElement(findEntry(contact));
    }

    /**
     * @param contact The contact to find in the model
     * @return The list item or {@code null} if no associated list item is found
     */
    private T findEntry(@Nonnull IIndividualContact contact)
    {
        final Iterator<T> iterator = model.elements().asIterator();

        while (iterator.hasNext())
        {
            final T entry = iterator.next();

            if (contactRetriever.apply(entry).getUid().equals(contact.getUid())) return entry;
        }

        return null;
    }

    /**
     * Listens for list item selections, notifies the {@link IContactSelectionListener} and clears the selection.
     */
    private class ContactListSelectionListener implements ListSelectionListener
    {
        @Override
        public void valueChanged(ListSelectionEvent e)
        {
            final T entry = list.getSelectedValue();

            if (!e.getValueIsAdjusting() && entry != null)
            {
                selectionListener.onSelect(entry);
                list.getSelectionModel().clearSelection();
            }
        }
    }
}