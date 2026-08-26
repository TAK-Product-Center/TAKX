package gov.takx.platform.cdi;

import gov.takx.api.annotation.Internal;
import org.jboss.weld.environment.se.WeldContainer;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * This class manages a map from class loader to the appropriate {@link org.jboss.weld.environment.se.WeldContainer}
 * handling that class loader.
 *
 * @since 2.37
 */
@Internal
public class ContainerFinder
{
    private static WeldContainer defaultContainer;
    private static final Map<ClassLoader, WeldContainer> containers = new ConcurrentHashMap<>();

    /**
     * Set the default container to use for any class using a class loader for which no container
     * has been set using {@link #setContainer(ClassLoader, WeldContainer)}. This handles objects whose class loader
     * doesn't map directly to a container (like classes in the shared module).
     *
     * @param container Default container
     */
    public static void setDefaultContainer(WeldContainer container)
    {
        defaultContainer = container;
    }

    /**
     * Set the container for all the classes handled by a given class loader.
     *
     * @param classLoader Class loader for all classes that should use the given container
     * @param container   Container for class loader
     */
    public static void setContainer(ClassLoader classLoader, WeldContainer container)
    {
        containers.put(classLoader, container);
    }

    /**
     * Get the container for the given class loader.  If not found, then use the default.
     *
     * @param classLoader Class loader of interest
     * @return Container for that object
     * @see #setDefaultContainer(WeldContainer)
     */
    public static WeldContainer getContainer(ClassLoader classLoader)
    {
        return containers.getOrDefault(classLoader, defaultContainer);
    }

    /**
     * @return {@code true} when there is either a default container, or any associated with a given classloader.
     */
    public static boolean hasContainers()
    {
        return defaultContainer != null || !containers.isEmpty();
    }
}
