package gov.takx.core.eventbus;

import gov.takx.api.annotation.InvokeOnEDT;
import gov.takx.api.annotation.UiEventBus;
import gov.takx.core.event.ClientEventIndex;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Produces;
import jakarta.inject.Singleton;
import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.EventBusBuilder;

import javax.swing.SwingUtilities;

import java.util.concurrent.Executors;

/**
 * Producer for event bus instances, either the default bus if no qualifier is specified, or the UI bus if the
 * {@link UiEventBus} qualifier is used. When using the {@link UiEventBus} the annotated method is invoked
 * on the EDT as if you had also used {@link InvokeOnEDT}.
 *
 * @see UiEventBus
 * @since 3.0 (renamed from EventServiceProducer)
 */
@SuppressWarnings("unused") // Used by injection points
@ApplicationScoped
public class EventBusProducer
{
    /**
     * If we ever move {@link EventBus} handling into Platform, we will want to also provide the {@code PlatformEventIndex}.
     * It currently does not generate because there are no {@link org.greenrobot.eventbus.Subscribe} annotated methods in Platform.
     * @since 5.5.1
     */
    private static final ClientEventIndex CLIENT_EVENT_INDEX = new ClientEventIndex();

    /**
     * Bus used for standard event dispatch.
     * @since 5.5.1
     */
    private final EventBus bus;

    /**
     * Bus used for UI event dispatch.
     * @since 5.5.1
     */
    private final EventBus uiBus;

    public EventBusProducer()
    {
        EventBusBuilder builder = EventBus.builder()
                .logger(new EventBusLoggerWrapper())
                .sendNoSubscriberEvent(false)
                .addIndex(CLIENT_EVENT_INDEX);

        // Create the bus, and install it as the default.
        bus = builder.build();
        builder.installDefaultEventBus();

        // Create the UI bus that dispatches on the EDT.
        uiBus = builder.executorService(Executors.newSingleThreadExecutor(task -> {
            Thread thread = new Thread(() -> SwingUtilities.invokeLater(task));
            thread.setDaemon(true);
            thread.setName("edt-event-bus");
            return thread;
        })).build();
    }

    @Produces
    @Singleton
    private EventBus produceEventBus()
    {
        return bus;
    }

    @Produces
    @Singleton
    @UiEventBus
    private EventBus produceUiEventBus()
    {
        return uiBus;
    }
}
