package gov.takx.api.aspects;

import gov.takx.api.annotation.RemoveObserver;
import gov.takx.platform.cdi.ContainerFinder;
import gov.takx.platform.cdi.InjectableObserverManager;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.jboss.weld.environment.se.WeldContainer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.invoke.MethodHandles;

/**
 * This aspect provides "before" advice for any method that is annotated with {@link RemoveObserver}.
 * It calls {@link InjectableObserverManager#removeObserver(Object)} on the object instance at join point.
 *
 * @since 2.37
 */
@Aspect
public class ObserverRemover
{
    private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    @Before("execution(* *(..)) && @annotation(gov.takx.api.annotation.RemoveObserver)")
    public void beforeRemoveObserver(JoinPoint call)
    {
        logger.debug("remove observer: {}", call);

        final Object instance = call.getThis();
        final ClassLoader classLoader = instance.getClass().getClassLoader();
        final WeldContainer container = ContainerFinder.getContainer(classLoader);

        if (container != null)
        {
            container.select(InjectableObserverManager.class).get().removeObserver(instance);
        } else
        {
            logger.error("[FATAL] Unable to locate container for object of type[{}] with class loader [{}]", instance.getClass(), classLoader);
        }
    }
}
