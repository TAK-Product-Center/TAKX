package gov.takx.api.aspects;

import gov.takx.api.annotation.UseClientClassLoader;
import gov.takx.api.annotation.UsePluginClassLoader;
import gov.takx.platform.utils.ClassLoaderUtil;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.invoke.MethodHandles;
import java.util.TimerTask;
import java.util.concurrent.Callable;

/**
 * This aspect is responsible for pushing the correct Thread Context ClassLoader (TCCL) in certain method calls.
 * This is a critical piece of ensuring that plugins can operate in the new isolated class loader model without
 * having to really know that it exists.
 * <p>
 * These aspects handle the following cases:
 * <ol>
 * <li>Every call to {@link Runnable#run()} to set the TCCL to the class loader for the Runnable.  This
 * ensures that any dynamic code loading that needs to occur within the runnable has the proper class loader.</li>
 * <br>
 * <li>Every call to {@link Callable#call()} to set the TCCL to the class loader for the Callable.  This
 * ensures that any dynamic code loading that needs to occur within the callable has the proper class loader.</li>
 * <br>
 * <li>Methods in a descendant package of {@code gov.takx} that are marked as needing to have the
 * Client module's classloader temporarily installed (using the {@link UseClientClassLoader} annotation).</li>
 * <br>
 * <li>Methods that are marked as needing to have the containing plugin's module classloader temporarily installed
 * (using the {@link UsePluginClassLoader} annotation).</li>
 * <br>
 * </ol>
 *
 * @since 1.36
 */
@Aspect
public class ContextClassLoaderInstaller
{
    private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    /**
     * Find the execution of any Runnable#run() method.
     * <p>
     * Specifically exclude the runnables created in DispatchEDTMethods since they are a factory for runnables
     * and the class loader of that runnable is not the proper class loader to use.  The runnables in DispatchEDTMethods
     * will take care of the TCCL installation on their own.
     */
    @Around(value = "!within(gov.takx.api.aspects.ContextClassLoaderInstaller) && !within(DispatchEDTMethods)" +
            "    && execution(void Runnable.run()) && this(runnable)", argNames = "joinPoint,runnable")
    public void aroundRunnableRun(ProceedingJoinPoint joinPoint, Runnable runnable)
    {
        try
        {
            pushTCCLAndProceed(joinPoint);
        } catch (RuntimeException e)
        {
            // Special handling for TimerTask objects.  Since an uncaught exception in a TimerTask will cause
            // the executing Timer to die, leading to an unusable timer, we need to catch, report, and ignore
            // exceptions coming out of the run() method of TimerTask instances.  Otherwise, the DEFAULT_TIMER
            // in the TimerService can become unusable due to errant plugin code.
            //
            // Yes, there is a very small chance that this operation might affect a plugin that had its own
            // timer service and was relying on the fact that exceptions cause it to shut down. I think it's
            // worth the risk to protect a core service.

            if (runnable instanceof TimerTask)
            {
                logger.error("Exception in TimerTask has been suppressed to protect the TimerService: {}", runnable, e);
            } else
            {
                throw e; // Otherwise, rethrow it
            }
        }
    }

    /**
     * Find the execution of any {@link Callable#call()} method.
     */
    @Around(value = "!within(gov.takx.api.aspects.ContextClassLoaderInstaller) && execution(* java.util.concurrent.Callable.call())", argNames = "joinPoint")
    public Object aroundCallableCall(ProceedingJoinPoint joinPoint)
    {
        return pushTCCLAndProceed(joinPoint);
    }

    /**
     * Select all the methods annotated with {@link UsePluginClassLoader}, in any codebase.
     */
    @Around(value = "execution(@gov.takx.api.annotation.UsePluginClassLoader  * *.*(..))", argNames = "joinPoint")
    public Object pushPluginCL(ProceedingJoinPoint joinPoint)
    {
        return pushTCCLAndProceed(joinPoint);
    }

    /**
     * Select all the methods annotated with {@link UseClientClassLoader} and in our codebase only.
     */
    @Around(value = "execution(@gov.takx.api.annotation.UseClientClassLoader * gov.tak*..*.*(..))", argNames = "joinPoint")
    public Object pushClientCL(ProceedingJoinPoint joinPoint)
    {
        return pushTCCLAndProceed(joinPoint);
    }

    /**
     * Call {@code proceed} on the given join point, ensuring that the appropriate TCCL for the given object
     * is installed.
     *
     * @param joinPoint Join point being processed
     * @return invoked method return value
     */
    private Object pushTCCLAndProceed(ProceedingJoinPoint joinPoint)
    {
        boolean pushed = false;
        try
        {
            pushed = pushTCCLIfNotSame(joinPoint.getSignature().getDeclaringType());

            return joinPoint.proceed(); // Invoke the original code with the new TCCL pushed
        } catch (Throwable e)
        {
            throw new RuntimeException(e);
        } finally
        {
            if (pushed) ClassLoaderUtil.popContextClassLoader(); // Ensure that we always pop it if we pushed it
        }
    }

    /**
     * Push the appropriate TCCL for the given type unless it is already installed as the TCCL.
     *
     * @param type The class declaring the method in question (from which to determine the needed class loader)
     * @return {@code true} if the TCCL was pushed, {@code false} if not
     */
    private boolean pushTCCLIfNotSame(Class<?> type)
    {
        // Don't bother to push if it is the same CL
        final var classLoader = type.getClassLoader();
        if (Thread.currentThread().getContextClassLoader() != classLoader)
        {
            ClassLoaderUtil.pushContextClassLoader(classLoader);
            return true;
        }
        return false;
    }
}
