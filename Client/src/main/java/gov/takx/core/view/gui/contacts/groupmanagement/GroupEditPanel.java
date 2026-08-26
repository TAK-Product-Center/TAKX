package gov.takx.core.view.gui.contacts.groupmanagement;

import gov.takx.api.annotation.PseudoBean;
import jakarta.annotation.PostConstruct;
import jakarta.inject.Inject;

import java.awt.Color;

/**
 * Allows the user to edit an existing user-managed group, selecting specific contacts to add to or remove from the group.
 *
 * @since 3.0
 */
@PseudoBean
public class GroupEditPanel extends AGroupPanel
{
    @Inject
    private GroupEditPanelController controller;

    public GroupEditPanel()
    {
        super("Edit Chat");
        groupNameTextField.setEnabled(false);
        groupNameTextField.setDisabledTextColor(Color.WHITE);

        setName("groupEditPanel");
    }

    @PostConstruct
    private void postConstruct()
    {
        setController(controller);
    }
}
