package gov.tak.platform.contact;

import gov.tak.api.contact.IPersistableGroupContact;

/**
 * Builder for group contacts.
 *
 * @since 3.2
 */
public class GroupContactBuilder extends GroupContactBuilderBase2<IPersistableGroupContact, GroupContactBuilder>
{
    @Override
    public IPersistableGroupContact build()
    {
        prepareForBuild();
        return new GroupContact(uniqueContactId, displayName, attributes, groupMembers, parentContact);
    }
}
