package gov.takx.core.view.gui.contacts.groupmanagement;

import gov.tak.api.contact.IGroupContact;
import gov.tak.api.contact.IGroupContactManager;
import gov.takx.api.annotation.PseudoBean;
import jakarta.annotation.PostConstruct;
import jakarta.inject.Inject;

import java.util.Set;
import java.util.stream.Collectors;

/**
 * Allows the user to create a new user-managed group, selecting specific contacts to include in the group. Note that the
 * group name cannot be edited; a new group should be created instead.
 *
 * @since 3.0
 */
@PseudoBean
public class GroupCreationPanel extends AGroupPanel
{
    private static final String DEFAULT_NAME_PREFIX = "New Group ";

    @Inject
    private GroupCreationPanelController controller;

    @Inject
    private IGroupContactManager groupContactManager;

    public GroupCreationPanel()
    {
        super("New Chat");
        setName("groupCreationPanel");
    }

    @PostConstruct
    private void postConstruct()
    {
        setController(controller);
    }

    /**
     * Set the initial group name to the next available default name.
     *
     * @since 3.5
     */
    public void initGroupName()
    {
        final Set<String> groupNames = groupContactManager.getGroupContacts().stream()
                .map(IGroupContact::getName)
                .collect(Collectors.toSet());

        int counter = 1;
        String name = DEFAULT_NAME_PREFIX + counter;

        while (groupNames.contains(name)) name = DEFAULT_NAME_PREFIX + ++counter;

        groupNameTextField.setText(name);
    }
}
