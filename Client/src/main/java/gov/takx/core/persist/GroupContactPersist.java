package gov.takx.core.persist;

import gov.tak.api.contact.IGroupContact;
import gov.tak.api.contact.IPersistableGroupContact;
import gov.tak.platform.contact.GroupContact;
import gov.takx.api.persist.IGroupContactPersist;
import gov.takx.api.persist.IPersistServiceProvider;
import gov.takx.api.persist.IPersistable;
import gov.takx.api.query.param.HQLParameter;
import gov.takx.core.view.gui.contacts.GroupContactManager;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.invoke.MethodHandles;
import java.util.List;

/**
 * Responsible for reading and writing {@link IGroupContact}s to and from the database.
 *
 * @since 3.2
 */
@ApplicationScoped
public class GroupContactPersist implements IGroupContactPersist
{
    private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    public static final String GROUP_CONTACT_TABLE = GroupContact.class.getSimpleName();

    private static final String UNIQUE_CONTACT_QUERY = "FROM " + GROUP_CONTACT_TABLE + " WHERE uid = :uid";

    private final IPersistServiceProvider persistServiceProvider;
    private final GroupContactManager groupContactManager;

    @Inject
    public GroupContactPersist(IPersistServiceProvider persistServiceProvider, GroupContactManager groupContactManager)
    {
        this.persistServiceProvider = persistServiceProvider;
        this.groupContactManager = groupContactManager;
    }

    @Override
    public void persistGroupContact(@Nonnull IGroupContact groupContact)
    {
        final String uid = groupContact.getUid();

        if (!(groupContact instanceof IPersistableGroupContact groupContactEntity))
        {
            logger.debug("Attempted to persist group contact that is not an entity: uid={} class={}", uid, groupContact.getClass());
            return;
        }

        persistServiceProvider.getPersistService().persistEntity(groupContactEntity);
    }

    @Override
    public boolean groupContactExists(@Nonnull String uid)
    {
        final long count = persistServiceProvider.getPersistService()
                .count(GROUP_CONTACT_TABLE, "uid = :uid", new HQLParameter("uid", uid));

        return count > 0;
    }

    @Nullable
    @Override
    public IPersistableGroupContact findGroupContact(@Nonnull String uid)
    {
        return findGroupContact(uid, true);
    }

    @Override
    @Nonnull
    public List<IGroupContact> findAllGroupContacts()
    {
        final List<IGroupContact> allGroupContacts = persistServiceProvider.getPersistService()
                .query("FROM " + GROUP_CONTACT_TABLE);

        // Find the parent contact for each group and reattach it.
        allGroupContacts.forEach(this::reattachParentContact);

        return allGroupContacts;
    }

    @Override
    public void deleteGroupContact(@Nonnull String contactUid)
    {
        // No need to query and reattach the parent because we're deleting the contact
        final IGroupContact existingContact = findGroupContact(contactUid, false);

        if (existingContact == null)
        {
            logger.debug("Attempted to delete group contact that is not currently persisted: uid={}", contactUid);
            return;
        }

        persistServiceProvider.getPersistService().deleteEntity((IPersistable) existingContact);
    }

    /**
     * @param uid The group contact's UID
     * @param reattachParent Pass {@code true} if the contact's parent should be queried and attached
     * @return The group contact or {@code null} if it isn't found
     * @since 3.5
     */
    @Nullable
    private IPersistableGroupContact findGroupContact(@Nonnull String uid, boolean reattachParent)
    {
        final GroupContact groupContact = persistServiceProvider.getPersistService()
                .queryUnique(UNIQUE_CONTACT_QUERY, new HQLParameter("uid", uid));

        if (groupContact != null && reattachParent) reattachParentContact(groupContact);

        return groupContact;
    }

    /**
     * Reattach the parent contact on the given group contact, if any.
     *
     * @param groupContact The group contact to process
     */
    private void reattachParentContact(IGroupContact groupContact)
    {
        final String parentUid = ((IPersistableGroupContact) groupContact).getParentUid();

        if (parentUid != null)
        {
            final IGroupContact userGroups = groupContactManager.getRootGroupContact();
            final IGroupContact teams = groupContactManager.getTeamsContact();

            if (parentUid.equals(userGroups.getUid()))
            {
                groupContact.setParentContact(userGroups);
            } else if (parentUid.equals(teams.getUid()))
            {
                groupContact.setParentContact(teams);
            } else
            {
                final IGroupContact parent = findGroupContact(parentUid);

                if (parent != null)
                {
                    groupContact.setParentContact(parent);
                } else
                {
                    logger.error("Group contact {} (UID: {}) has a parent (UID: {}) that does not exist",
                            groupContact.getName(), groupContact.getUid(), parentUid);
                }
            }
        }
    }
}
