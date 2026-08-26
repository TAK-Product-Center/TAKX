package gov.tak.core.chat.protocol.geochat;

import gov.tak.api.contact.IContactService;
import gov.tak.api.contact.IGroupContact;
import gov.tak.api.contact.IIndividualContact;
import gov.tak.api.contact.IndividualContactUtils;
import gov.takx.api.persist.IGroupContactPersist;
import gov.takx.core.view.gui.contacts.GroupContactManager;
import gov.takx.core.view.gui.contacts.IndividualContactManager;
import gov.takx.platform.ui.UIResources;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import javax.swing.Icon;

import java.util.Arrays;
import java.util.List;

import static gov.takx.platform.ui.ResourceDirectory.TAK_ASSETS_ICONS;

/**
 * Manages Team Role {@link IGroupContact}s.
 *
 * @since 3.5
 */
@ApplicationScoped
public class GeoChatRolesManager extends AGeoChatGroupManager
{
    /**
     * The roles managed by this manager. Note that the default "Team Member" role is excluded.
     */
    private static final List<String> MANAGED_ROLES =
            Arrays.asList("Forward Observer", "HQ", "K9", "Medic", "RTO", "Sniper", "Team Lead");

    @Inject
    public GeoChatRolesManager(GroupContactManager groupContactManager, IContactService contactService,
                               IGroupContactPersist groupContactPersist,
                               IndividualContactManager individualContactManager)
    {
        super(groupContactManager, contactService, groupContactPersist, individualContactManager,
                GeoChatRolesManager::getRoleIfManaged, individualContactManager::getClientContactRole);
    }

    /**
     * @param role The role UID or name (the values are interchangeable)
     * @return {@code true} if the specific role is managed by this manager; "Team Member" is excluded
     */
    public static boolean isManagedRole(String role)
    {
        return MANAGED_ROLES.contains(role);
    }

    @Override
    public boolean isManaged(@Nonnull IGroupContact groupContact)
    {
        return groupContact.getParentContact() == null && isManagedRole(groupContact.getUid());
    }

    @Nonnull
    @Override
    Icon createIcon(@Nonnull String groupUid)
    {
        return UIResources.getActionButtonIcon(
                TAK_ASSETS_ICONS,
                // Remove whitespace and convert to lowercase to match icon names
                "roles/" + groupUid.replaceAll("\\s", "").toLowerCase() + ".png");
    }

    /**
     * @param contact The contact
     * @return The contact's role if it's managed, or {@code null} if it should be ignored (e.g. "Team Member")
     */
    @Nullable
    private static String getRoleIfManaged(@Nonnull IIndividualContact contact)
    {
        final String role = IndividualContactUtils.getRole(contact);

        return role != null && isManagedRole(role) ? role : null;
    }
}