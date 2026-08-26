package gov.takx.platform.cdi;

import gov.takx.api.annotation.Internal;
import gov.takx.api.event.CrossContainerEvent;
import jakarta.annotation.Nonnull;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import jakarta.enterprise.inject.spi.EventMetadata;
import jakarta.inject.Inject;
import org.jboss.weld.environment.se.WeldContainer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.annotation.Annotation;
import java.lang.invoke.MethodHandles;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * This service is responsible for observing {@link CrossContainerEvent cross-container events} fired in a plugin
 * container and re-firing them in all dependent containers.
 *
 * @since 2.35
 */
@Internal
@ApplicationScoped
public class CrossContainerEventDistributor
{
    private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    @Inject
    private WeldContainer container;

    /**
     * In order to avoid a given container receiving an event multiple times, we need to track the containers that
     * have already been notified and avoid firing the event in a container more than once.  This set is static,
     * so it is shared across instances of this class according to the thread delivering the events.
     * <p>
     * The condition that causes multiple notification is a standard "diamond dependency" issue.  Assume A depends
     * on B and C, which each depend on D. Visually, something like:
     * <pre>
     *    A
     *   / \
     *  B   C
     *   \ /
     *    D
     * </pre>
     * With that configuration, an event fired in D would be sent to A twice, along paths D -> B -> A, and D -> C -> A.
     */
    private static final ThreadLocal<Set<WeldContainer>> notifiedContainers = ThreadLocal.withInitial(HashSet::new);

    /**
     * Dependent containers into which to re-fire CrossContainerEvent events
     */
    private Set<WeldContainer> dependentContainers = Collections.emptySet();

    private List<String> dependentContainerNames;

    /**
     * Any CrossContainerEvent fired in this container needs to be refired in all the dependent plugin containers.
     *
     * @param event    Event to refire
     * @param metadata Event metadata, so we can extract the qualifiers used on the event
     */
    private void refireCrossContainerEvents(@Observes CrossContainerEvent event, EventMetadata metadata)
    {
        if (!dependentContainers.isEmpty())
        {
            if (logger.isDebugEnabled())
            {
                logger.debug("{}: Send {} to {}", container.getId(), event.getClass().getSimpleName(), dependentContainerNames);
            }

            final Set<WeldContainer> currentContainers = notifiedContainers.get();
            final boolean initiator = currentContainers.isEmpty(); // Are we the start of the event chain?

            // If we are the initiator, immediately add ourself to the current containers list so that events
            // won't get fired back to the initiator.  This condition happens when an event is fired in a container
            // that has an "implemented by" container.  They end up being a circular dependency since events
            // fired in either the "API" or the implementation container must be fired in their counterpart.

            if (initiator) currentContainers.add(container);

            dependentContainers.stream()
                    .filter(container -> !currentContainers.contains(container)) // Skip containers already processed
                    .forEach(container -> {
                        if (container.isRunning())
                        {
                            currentContainers.add(container); // Track that we've processed it

                            // Re-fire the event with the same type and qualifiers as were used in the original event.

                            container.event()
                                    .select(metadata.getType(), metadata.getQualifiers().toArray(new Annotation[0]))
                                    .fire(event);
                        } else
                        {
                            // Handle dependent containers that have been shutdown
                            if (logger.isDebugEnabled()) logger.debug("{} is shutdown", container.getId());

                            dependentContainers.remove(container); // Never try to process it again
                        }
                    });

            if (initiator) currentContainers.clear(); // Clean up if we're the initiator of the event chain
        } else if (logger.isTraceEnabled())
        {
            logger.trace("{}: No dependent containers for event [{}]", container.getId(), event.getClass().getSimpleName());
        }
    }

    /**
     * Set the dependent containers to which to re-fire events from this plugin's deployment container.
     *
     * @param containers List of dependent containers to which to re-fire events
     */
    public void setDependentContainers(@Nonnull Set<WeldContainer> containers)
    {
        dependentContainers = new HashSet<>(containers);
        dependentContainers.remove(container); // Ensure we never try to re-deliver to ourself

        dependentContainerNames = dependentContainers.stream().map(WeldContainer::getId).collect(Collectors.toList());

        if (logger.isDebugEnabled()) logger.debug("{}: refire to {}", container.getId(), dependentContainerNames);
    }
}
