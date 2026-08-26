package gov.takx.core.view.gui.contacts.lists;

import gov.tak.api.contact.IIndividualContact;
import gov.tak.api.contact.IndividualContactAttributeNames;
import gov.tak.api.contact.IndividualContactState;
import gov.tak.api.contact.IndividualContactUtils;
import gov.tak.api.util.AttributeSet;
import gov.tak.blacksmith.mock.Mocked;
import gov.tak.core.chat.protocol.geochat.GeoChatConstants;
import gov.tak.platform.contact.Contacts;
import gov.tak.platform.contact.IndividualContactBuilder;
import gov.takx.test.AIntegrationTest;
import gov.takx.test.utils.SwingTestUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import javax.swing.DefaultListModel;
import javax.swing.JList;
import javax.swing.ListSelectionModel;

import java.util.function.Function;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.clearInvocations;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

/**
 * Tests the {@link ContactsPanelIndividualListController}
 *
 * @since 3.5
 */
public class IndividualListControllerTest extends AIntegrationTest
{
    @Mocked
    private IContactSelectionListener<IIndividualContact> listener;

    private JList<IIndividualContact> list;
    private IndividualContactListController<IIndividualContact> controller;

    @BeforeEach
    public void beforeEach()
    {
        list = new JList<>();
        controller = new IndividualContactListController<>(list, Function.identity(), Function.identity(), listener);
    }

    @Test
    public void contactAddedCalled_listModelUpdated()
    {
        final IIndividualContact contact1 = createContact("1", "1");
        final IIndividualContact contact2 = createContact("2", "2");

        controller.contactAdded(contact1);
        controller.contactAdded(contact2);

        final DefaultListModel<IIndividualContact> model = getModel();
        assertThat(model.getElementAt(0)).isEqualTo(contact1);
        assertThat(model.getElementAt(1)).isEqualTo(contact2);

    }

    @Test
    public void contactAddedCalled_whenContactIsGone_listModelNotUpdated()
    {
        final IIndividualContact contact = createContact("1", "1");
        IndividualContactUtils.setState(contact, IndividualContactState.gone);

        controller.contactAdded(contact);

        assertThat(getModel().isEmpty()).isTrue();

    }

    @Test
    public void contactAddedCalled_listModelIsOrdered()
    {
        final IIndividualContact contact1 = createContact("aaa", "aaa");
        final IIndividualContact contact2 = createContact("bbb", "bbb");
        final IIndividualContact contact3 = createContact("ccc", "ccc");

        // add in reverse order
        controller.contactAdded(contact2);
        controller.contactAdded(contact3);
        controller.contactAdded(contact1);

        // confirm correct order
        final DefaultListModel<IIndividualContact> model = getModel();
        assertThat(model.getElementAt(0)).isEqualTo(contact1);
        assertThat(model.getElementAt(1)).isEqualTo(contact2);
        assertThat(model.getElementAt(2)).isEqualTo(contact3);
    }

    @Test
    public void contactSelected_listenerNotified()
    {
        final IIndividualContact contact1 = createContact("1", "1");
        final IIndividualContact contact2 = createContact("2", "2");

        controller.contactAdded(contact2);
        controller.contactAdded(contact1);

        SwingTestUtils.waitForEdtEvents();

        //noinspection unchecked
        clearInvocations(listener);

        final ListSelectionModel selectionModel = list.getSelectionModel();
        selectionModel.setSelectionInterval(0, 0);
        selectionModel.clearSelection();
        selectionModel.setSelectionInterval(0, 0);
        selectionModel.clearSelection();
        selectionModel.setSelectionInterval(1, 1);

        verify(listener, times(2)).onSelect(contact1);
        verify(listener, times(1)).onSelect(contact2);
    }

    @Test
    public void contactUpdatedCalled_contactUpdatedInModel()
    {
        final IIndividualContact contact1 = createContact("1", "1");
        IIndividualContact contact2 = createContact("2", "2");

        controller.contactAdded(contact1);
        controller.contactAdded(contact2);

        contact2 = createContact(contact2.getUid(), "new");

        controller.contactUpdated(contact2);

        final IIndividualContact modelContact2 = getModel().getElementAt(1);
        assertThat(modelContact2.getName()).isEqualTo(contact2.getName());
    }

    @Test
    public void contactUpdatedCalled_whenContactIsGone_contactNotAddedToModel()
    {
        final IIndividualContact contact = createContact("1", "1");
        IndividualContactUtils.setState(contact, IndividualContactState.gone);

        controller.contactUpdated(contact);

        assertThat(getModel().isEmpty()).isTrue();
    }

    @Test
    public void contactRemovedCalled_contactRemoveFromModel()
    {
        final IIndividualContact contact1 = createContact("1", "1");
        final IIndividualContact contact2 = createContact("2", "2");
        final IIndividualContact contact3 = createContact("3", "3");

        controller.contactAdded(contact1);
        controller.contactAdded(contact2);
        controller.contactAdded(contact3);

        controller.contactRemoved(contact2);

        final DefaultListModel<IIndividualContact> model = getModel();
        assertThat(model.getSize()).isEqualTo(2);
        assertThat(model.getElementAt(0)).isEqualTo(contact1);
        assertThat(model.getElementAt(1)).isEqualTo(contact3);
    }

    @Test
    public void contactStateChangeCalled_modelUpdated()
    {
        final IIndividualContact contact1 = createContact("1", "1");
        final IIndividualContact contact2 = createContact("2", "2");
        final IIndividualContact contact3 = createContact("3", "3");

        controller.contactAdded(contact1);
        controller.contactAdded(contact2);
        controller.contactAdded(contact3);

        // Contact inactive, should not be removed
        IndividualContactUtils.setState(contact1, IndividualContactState.inactive);
        controller.contactStateChanged(contact1);
        assertThat(getModel().getElementAt(0)).isEqualTo(contact1);

        // Contact gone, should be removed
        IndividualContactUtils.setState(contact1, IndividualContactState.gone);
        controller.contactStateChanged(contact1);
        assertThat(getModel().getElementAt(0)).isEqualTo(contact2);

        // Contact active again, should be re-added
        IndividualContactUtils.setState(contact1, IndividualContactState.active);
        controller.contactStateChanged(contact1);
        assertThat(getModel().getElementAt(0)).isEqualTo(contact1);
    }

    private IIndividualContact createContact(String uid, String name)
    {
        final IIndividualContact contact = new IndividualContactBuilder()
                .withUniqueContactId(uid)
                .withDisplayName(name)
                .build();

        final AttributeSet attributeSet = contact.getAttributes();
        attributeSet.setAttribute(Contacts.PROTOCOL_ATTRIBUTE_KEY, new String[]{GeoChatConstants.GEOCHAT_PROTOCOL});
        attributeSet.setAttribute(IndividualContactAttributeNames.STATE, IndividualContactState.active.name());

        return contact;
    }

    /**
     * Waits on EDT before returning the list model to ensure modifications have been applied before the model is
     * accessed.
     *
     * @return The list model
     * @since 5.2
     */
    private DefaultListModel<IIndividualContact> getModel()
    {
        SwingTestUtils.waitForEdtEvents();

        return controller.getModel();
    }
}