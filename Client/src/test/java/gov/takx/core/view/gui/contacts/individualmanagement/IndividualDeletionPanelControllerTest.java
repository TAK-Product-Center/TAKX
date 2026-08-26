package gov.takx.core.view.gui.contacts.individualmanagement;

import gov.tak.api.contact.IContactService;
import gov.tak.blacksmith.mock.Mocked;
import gov.takx.core.view.gui.contacts.ContactsPanelController;
import gov.takx.core.view.gui.contacts.lists.IndividualContactCheckBoxCellRenderer;
import gov.takx.test.AIntegrationTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;

public class IndividualDeletionPanelControllerTest extends AIntegrationTest
{
    @Mocked
    private ContactsPanelController contactsPanelController;

    @Mocked
    private IContactService contactService;

    private IndividualDeletionPanelController controller;
    private IndividualDeletionPanel panel;

    @BeforeEach
    void beforeEach()
    {
        controller = new IndividualDeletionPanelController(contactsPanelController, contactService);

        panel = new IndividualDeletionPanel();
        controller.setPanel(panel);
    }

    @Test
    public void setPanel_setsCustomCellRenderer()
    {
        assertThat(panel.getList().getCellRenderer()).isInstanceOf(IndividualContactCheckBoxCellRenderer.class);
    }

    @Test
    void prepare_completesSuccessfully()
    {
        controller.prepare(); // Just confirm that no exceptions are thrown
    }

    @Test
    public void close_showsPrimaryPanel()
    {
        controller.prepare();
        controller.close();

        verify(contactsPanelController).showPrimaryContactsPanel();
    }
}
