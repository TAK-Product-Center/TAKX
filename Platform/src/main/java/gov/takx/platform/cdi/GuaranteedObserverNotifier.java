package gov.takx.platform.cdi;

import gov.takx.api.annotation.Internal;
import gov.takx.api.event.ITerminateObserverNotification;
import jakarta.enterprise.inject.spi.EventMetadata;
import jakarta.enterprise.inject.spi.ObserverMethod;
import org.jboss.weld.bootstrap.api.ServiceRegistry;
import org.jboss.weld.environment.se.WeldContainer;
import org.jboss.weld.event.ObserverNotifier;
import org.jboss.weld.module.ObserverNotifierFactory;
import org.jboss.weld.resolution.TypeSafeObserverResolver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.invoke.MethodHandles;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * The standard observer notifier in Weld stops delivering a given event when any observer method throws an exception.
 * That semantic is not acceptable for TAKX, since we want to ensure that all observers get notified.
 * <p>
 * This class overrides the {@link #notifySyncObservers} method in order to implement the guaranteed delivery semantics.
 *
 * @since 2.0.0
 */
@Internal
public class GuaranteedObserverNotifier extends ObserverNotifier
{
    private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    static final ObserverNotifierFactory FACTORY = GuaranteedObserverNotifier::new;
    private final Comparator<ObserverMethod<?>> PRIORITY_COMPARATOR = Comparator.comparing(ObserverMethod::getPriority);

    private final String contextId;

    private volatile boolean initialized = false;
    private volatile InjectableObserverManager injectableObserverManager;

    GuaranteedObserverNotifier(String contextId, TypeSafeObserverResolver resolver, ServiceRegistry services, boolean strict)
    {
        super(contextId, resolver, services, strict);

        this.contextId = contextId;
    }

    /**
     * Find the {@link InjectableObserverManager} in our container.
     * @since 5.2
     */
    private synchronized void initialize(WeldContainer container)
    {
        if (!initialized)
        {
            injectableObserverManager = container.select(InjectableObserverManager.class).get();
            initialized = true;
        }
    }

    /**
     * Overridden so that we can guarantee that ALL observers are notified of the event, even if an observer throws
     * an exception, unless that exception is tagged with {@link ITerminateObserverNotification}.
     */
    @SuppressWarnings("unchecked")
    @Override
    protected <T> void notifySyncObservers(List<ObserverMethod<? super T>> observers, T event, EventMetadata metadata, ObserverExceptionHandler handler)
    {
        logger.debug("{}: Notify: {} of {}", contextId, observers, event);

        // When the container is finally "running", we need to initialize handling for @PseudoBean observers
        final var container = WeldContainer.instance(contextId);
        if (!initialized && container.isRunning()) initialize(container);

        // Merge in any observer methods from @PseudoBeans, and sort on priority
        if (injectableObserverManager != null && metadata != null)
        {
            final var pseudoBeanObservers =
                    (List<? extends ObserverMethod<T>>) injectableObserverManager.getEventObservers(event.getClass(), metadata);

            if (!pseudoBeanObservers.isEmpty())
            {
                final List<ObserverMethod<? super T>> orderedObservers = new ArrayList<>(observers.size() + pseudoBeanObservers.size());

                orderedObservers.addAll(observers);
                orderedObservers.addAll(pseudoBeanObservers);
                orderedObservers.sort(PRIORITY_COMPARATOR);

                observers = orderedObservers;
                logger.debug("FINAL {}", observers);
            }
        }

//        doTimedDispatch(observers, event, metadata, 500);

        super.notifySyncObservers(observers, event, metadata, throwable -> {
            if (throwable instanceof ITerminateObserverNotification && throwable instanceof RuntimeException)
            {
                throw (RuntimeException) throwable;
            } else
            {
                logger.error("{}: Failed to deliver event [{}]", contextId, event, throwable);
            }
        });
    }

    @Override
    protected <T> void notifyTransactionObservers(List<ObserverMethod<? super T>> observers, T event, EventMetadata metadata, ObserverExceptionHandler handler)
    {
        // We don't support transactional observers, so don't bother processing them
    }

    /**
     * Dispatch the event to each observer, timing how long it takes.  Report any call that takes more than
     * {@code threshold} milliseconds to complete.
     *
     * @since 5.1
     */
    @SuppressWarnings("unused") // Useful for debugging events (like StartProjectServices) to find slow handlers
    private <T> void doTimedDispatch(List<ObserverMethod<? super T>> observers, T event, EventMetadata metadata, int threshold)
    {
        observers.forEach(observer -> {
            final long start = System.currentTimeMillis();

            super.notifySyncObservers(List.of(observer), event, metadata, throwable -> {
                if (throwable instanceof ITerminateObserverNotification && throwable instanceof RuntimeException)
                {
                    throw (RuntimeException) throwable;
                } else
                {
                    logger.error("{}: Failed to deliver event [{}]", contextId, event, throwable);
                }
            });

            final long time = System.currentTimeMillis() - start;
            if (time > threshold) logger.warn("Event {} took: {} in {} [{}]",
                    event.getClass().getSimpleName(), time, observer.getDeclaringBean().getBeanClass().getSimpleName(), contextId);
        });
    }
}
