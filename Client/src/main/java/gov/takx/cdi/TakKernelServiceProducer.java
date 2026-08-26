package gov.takx.cdi;

import gov.tak.api.chat.IChatService;
import gov.tak.api.contact.IContactService;
import gov.tak.platform.chat.ChatService;
import gov.tak.platform.contact.Contacts;
import gov.takx.api.annotation.InjectableService;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Produces;

/**
 * Produces takkernel services for injection.
 *
 * @since 3.4
 */
@InjectableService
@ApplicationScoped
public class TakKernelServiceProducer
{
    @SuppressWarnings("unused")
    @Produces
    private IChatService produceChatService()
    {
        return ChatService.getInstance();
    }

    @SuppressWarnings("unused")
    @Produces
    private IContactService produceContactStore()
    {
        return Contacts.getDefaultContactStore();
    }
}
