package gov.takx.platform.cdi;

import gov.takx.api.annotation.PseudoBean;
import gov.takx.bootstrap.ApplicationModuleFinder;
import jakarta.annotation.Priority;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import jakarta.enterprise.event.Startup;
import org.jboss.weld.environment.se.WeldContainer;

import static jakarta.interceptor.Interceptor.Priority.PLATFORM_BEFORE;

/**
 * Observer on the {@link Startup} event.  As a plugin container is constructed, we need to catch the Startup
 * event as early as possible and record the classloader associated with the container so
 * {@link gov.takx.api.aspects.PostInitInjector} can properly handle PseudoBean classes in that container.
 * <p/>
 * This is only needed in order to handle the case where a plugin deployment has an observer on {@link Startup} and
 * during its execution, it constructs an {@link PseudoBean} class.
 *
 * @see ContainerFinder
 * @since 3.4
 */
@ApplicationScoped
public class StartupHandler
{
    // Tracks the class loader for the container being constructed on a given thread
    private static final ThreadLocal<ClassLoader> CLASS_LOADER = new ThreadLocal<>();

    /**
     * Set the class loader for the container being constructed on this thread.
     */
    public static void setClassLoader(ClassLoader classLoader)
    {
        CLASS_LOADER.set(classLoader);
    }

    /**
     * Handle the Startup event, recording the recorded class loader with the ContainerFinder.
     */
    private void onStartup(@Observes @Priority(PLATFORM_BEFORE) Startup ignored, WeldContainer container)
    {
        if (ApplicationModuleFinder.CLIENT_MODULE_ID.equals(container.getId()))
        {
            ContainerFinder.setDefaultContainer(container);
        } else
        {
            ContainerFinder.setContainer(CLASS_LOADER.get(), container);
        }
    }
}
