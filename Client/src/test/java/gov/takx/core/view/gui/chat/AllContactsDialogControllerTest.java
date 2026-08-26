package gov.takx.core.view.gui.chat;

import gov.tak.api.comms.commo.ICommonCommoManager;
import gov.tak.api.comms.takserver.mission.data.ClientEndpoint;
import gov.tak.api.contact.IIndividualContact;
import gov.tak.api.contact.IndividualContactAttributeNames;
import gov.tak.api.util.AttributeSet;
import gov.tak.blacksmith.BlacksmithExtension;
import gov.tak.blacksmith.mock.Mocked;
import gov.takx.core.view.gui.contacts.ContactsPanelController;
import gov.takx.core.view.gui.contacts.IndividualContactManager;
import gov.takx.test.AIntegrationTest;
import gov.takx.test.utils.CdiTestUtils;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.clearInvocations;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Tests the {@link AllContactsDialogController}.
 *
 * @since 5.7 (renamed in 5.8)
 */
public class AllContactsDialogControllerTest extends AIntegrationTest
{
    @Mocked
    private ICommonCommoManager commonCommoManager;
    @Mocked
    private ContactsPanelController contactsPanelController;
    @Mocked
    private IndividualContactManager individualContactManager;

    private final AllContactsDialogController controller;

    public AllContactsDialogControllerTest()
    {
        controller = getBean(AllContactsDialogController.class);
        BlacksmithExtension.setBeansToUpdate(CdiTestUtils.getInstance(controller));
    }

    @AfterEach
    void afterEach()
    {
        clearInvocations(commonCommoManager, contactsPanelController);
    }

    @Test
    public void addContact_whenContactDoesNotExist_configuresEndpoint()
    {
        final var endpointId = "tak.server";
        final var uid = "uid";
        final var callsign = "callsign";
        final var username = "username";

        final var clientEndpoint = new ClientEndpoint(callsign, uid, username, Instant.now(), "");

        controller.addContact(new LocalOrServerContact(null, clientEndpoint, endpointId, null));

        verify(commonCommoManager).configKnownEndpointForContact(eq(uid), eq(callsign), eq(endpointId));
        verify(contactsPanelController).onSelect(any());
    }

    @Test
    public void addContact_whenContactExists_configuresEndpointAndContact(@Mocked(allowCaching = false) IIndividualContact contact)
    {
        final var endpointId = "tak.server";
        final var uid = "uid";
        final var callsign = "callsign";
        final var username = "username";

        when(contact.getUid()).thenReturn(uid);
        when(individualContactManager.getContact(uid)).thenReturn(contact);

        final var clientEndpoint = new ClientEndpoint(callsign, uid, username, Instant.now(), "");

        final var attributes = new AttributeSet();
        when(contact.getAttributes()).thenReturn(attributes);

        controller.addContact(new LocalOrServerContact(null, clientEndpoint, endpointId, null));

        verify(commonCommoManager).configKnownEndpointForContact(eq(uid), eq(callsign), eq(endpointId));
        assertThat(attributes.containsAttribute(IndividualContactAttributeNames.ENDPOINT_ID)).isTrue();
        verify(contactsPanelController).onSelect(any());
    }
}
