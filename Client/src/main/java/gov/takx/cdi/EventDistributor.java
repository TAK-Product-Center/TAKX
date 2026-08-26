package gov.takx.cdi;

import gov.takx.api.event.PlatformEvent;
import jakarta.annotation.Priority;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import jakarta.enterprise.inject.spi.EventMetadata;
import jakarta.inject.Inject;
import org.jboss.weld.environment.se.WeldContainer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.annotation.Annotation;
import java.lang.invoke.MethodHandles;

import static jakarta.interceptor.Interceptor.Priority.PLATFORM_AFTER;

/**
 * This service is responsible for observing {@link PlatformEvent platform events} fired in the core container
 * and re-firing them in all plugin containers.
 *
 * @since 2.0.0
 */
@SuppressWarnings("unused")
@ApplicationScoped
public class EventDistributor
{
    private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    @Inject
    WeldContainer container;

    /**
     * Any {@link PlatformEvent} fired in the core container needs to be refired in all the plugin containers.
     * <p/>
     * Note: we want to fire events to plugins AFTER all the core observers have been notified, thus the use of
     * a large priority value on this observer.
     *
     * @param event Event to refire
     * @param metadata Event metadata, so we can extract the qualifiers used on the event
     */
    private void refirePlatformEvents(@Observes @Priority(PLATFORM_AFTER) PlatformEvent event, EventMetadata metadata)
    {
        if (logger.isDebugEnabled()) logger.debug("Send: {} to containers: {}", event, WeldContainer.getRunningContainerIds());

        final var eventType = metadata.getType();
        final var annotations = metadata.getQualifiers().toArray(new Annotation[0]);

        WeldContainer.getRunningContainerIds().stream()
                .map(WeldContainer::instance)
                .filter(c -> c != null && c != container) // Don't re-fire to our own container (or infinite loops occur)
                .forEach(c -> c.event().select(eventType, annotations).fire(event));
    }
}
