package gov.tak.core.chat.protocol.geochat;

import gov.tak.api.contact.IContactService;
import gov.tak.api.contact.IGroupContact;
import gov.tak.api.contact.IndividualContactUtils;
import gov.tak.api.icons.RoleIconColorizer;
import gov.tak.api.icons.TeamColorMapper;
import gov.tak.platform.contact.GroupContactBuilder;
import gov.takx.api.persist.IGroupContactPersist;
import gov.takx.core.view.gui.contacts.GroupContactManager;
import gov.takx.core.view.gui.contacts.IndividualContactManager;
import jakarta.annotation.Nonnull;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import javax.swing.Icon;
import javax.swing.ImageIcon;

import java.awt.Color;

/**
 * Manages Teams {@link IGroupContact}s.
 *
 * @since 3.5
 */
@ApplicationScoped
public class GeoChatTeamsManager extends AGeoChatGroupManager
{
    @Inject
    GeoChatTeamsManager(GroupContactManager groupContactManager, IContactService contactService,
                        IGroupContactPersist groupContactPersist, IndividualContactManager individualContactManager)
    {
        super(groupContactManager, contactService, groupContactPersist, individualContactManager,
                IndividualContactUtils::getTeam, individualContactManager::getClientContactTeam);
    }

    @Override
    public boolean isManaged(@Nonnull IGroupContact groupContact)
    {
        final IGroupContact parent = groupContact.getParentContact();
        return parent != null && parent.getUid().equals(groupContactManager.getTeamsContact().getUid());
    }

    @Override
    void customizeGroupContactBuilder(@Nonnull GroupContactBuilder builder)
    {
        // set the parent to the "Teams" IGroupContact
        builder.withParentContact(groupContactManager.getTeamsContact());
    }

    @Nonnull
    @Override
    Icon createIcon(@Nonnull String groupUid)
    {
        return new ImageIcon(
                RoleIconColorizer.colorize(
                        "roles/team.png",
                        new Color(TeamColorMapper.teamToColor(groupUid))));
    }
}
