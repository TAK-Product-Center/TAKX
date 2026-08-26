package gov.takx.api.aspects;

import gov.takx.platform.utils.ClassLoaderUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.invoke.MethodHandles;
import javax.swing.SwingUtilities;
import java.lang.reflect.InvocationTargetException;
import java.util.concurrent.atomic.AtomicReference;

/**
 * This aspect is responsible for locating all the methods marked with the @InvokeOnEDT annotation and ensuring they
 * execute on the EDT, either because they were called on the EDT or by wrapping them in a runnable and dispatching
 * it using {@code invokeLater}.  Likewise for the @InvokeAndWaitOnEDT, but using {@code invokeAndWait}.
 * <p/>
 * Special handling is provided to ensure that the proper TCCL is installed prior to proceeding with the code
 * execution wrapped by the around advice in this aspect.
 *
 * @see SwingUtilities#invokeLater(Runnable)
 * @see SwingUtilities#invokeAndWait(Runnable)
 * @since 1.25
 */
@SuppressWarnings("DanglingJavadoc")
public aspect DispatchEDTMethods
{
    private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    // Select all the methods annotated with InvokeOnEDT
    pointcut invokeOnEDT(): execution(@gov.takx.api.annotation.InvokeOnEDT * *.*(..));

    // Trap any use of this annotation on methods that have a return type other than void
    declare error : execution(@gov.takx.api.annotation.InvokeOnEDT !void *.*(..))
            :"Do not use InvokeOnEDT with non-void methods (consider InvokeAndWaitOnEDT)";

    /**
     * Advice to ensure a given method call happens on the EDT using invokeLater
     */
    void around(): invokeOnEDT() {

        final Object targetThis = thisJoinPoint.getThis(); // Null on static methods
        final Class<?> thisClass = targetThis != null
                ? targetThis.getClass()
                : thisJoinPoint.getSignature().getDeclaringType();

        final ClassLoader classLoader = thisClass.getClassLoader();

        // Because we have to call proceed() in either code path (there is no actual method to re-invoke),
        // we end up duplicating the push/pop code on both code paths.  Annoying, but I don't see a way around it.

        if (SwingUtilities.isEventDispatchThread())
        {
            if (logger.isTraceEnabled()) logger.trace("Intercepted: {}", AspectUtils.dumpCall(thisJoinPoint));

            ClassLoaderUtil.pushContextClassLoader(classLoader);
            try
            {
                proceed(); // Invoke the original code with the new TCCL pushed
            } finally
            {
                ClassLoaderUtil.popContextClassLoader(); // Ensure that we always pop it if we pushed it
            }
        } else
        {
            if (logger.isDebugEnabled()) logger.debug("Moving: {} ==> EDT", AspectUtils.dumpCall(thisJoinPoint));

            // There is currently a bug in AspectJ when dealing with around advice that includes lambdas and the
            // aspect is compiled with a JDK > 8.  It incorrectly tries to inline the advice, but fails due to
            // issues with proper bootstrap methods for the lambda.
            //
            // Beyond that, currently (as of AspectJ 1.9.4) it can't actually inline aspects that use lambdas.
            // So, regardless of the bug, it causes the creation of additional classes to hold the advice
            // for every method that matches the pointcut.
            //
            // Given all that, the best course of action is to NOT use lambdas here.  Thus, the inner class
            // and the comment to disable the inspection.

            //noinspection Convert2Lambda
            SwingUtilities.invokeLater(new Runnable()
            {
                @Override
                public void run()
                {
                    ClassLoaderUtil.pushContextClassLoader(classLoader);
                    try
                    {
                        proceed(); // Invoke the original code with the new TCCL pushed
                    } finally
                    {
                        ClassLoaderUtil.popContextClassLoader(); // Ensure that we always pop it if we pushed it
                    }
                }
            });
        }
    }

    // Select all the methods annotated with InvokeAndWaitOnEDT
    pointcut invokeAndWaitOnEDT(): execution(@gov.takx.api.annotation.InvokeAndWaitOnEDT * *.*(..));

    /**
     * Advice to ensure a given method call happens on the EDT using invokeAndWait.  If an exception occurs
     * during execution, then try to log the error using the logger from the calling context.
     */
    Object around(): invokeAndWaitOnEDT() {

        final Object targetThis = thisJoinPoint.getThis(); // Null on static methods
        final Class<?> thisClass = targetThis != null
                ? targetThis.getClass()
                : thisJoinPoint.getSignature().getDeclaringType();

        final ClassLoader classLoader = thisClass.getClassLoader();
        final AtomicReference<Object> returnValue = new AtomicReference<>();

        // Because we have to call proceed() in either code path (there is no actual method to re-invoke),
        // we end up duplicating the push/pop code on both code paths.  Annoying, but I don't see a way around it.

        if (SwingUtilities.isEventDispatchThread())
        {
            if (logger.isTraceEnabled()) logger.trace("Intercepted: {}", AspectUtils.dumpCall(thisJoinPoint));
            ClassLoaderUtil.pushContextClassLoader(classLoader);
            try
            {
                returnValue.set(proceed()); // Invoke the original code with the new TCCL pushed
            } finally
            {
                ClassLoaderUtil.popContextClassLoader(); // Ensure that we always pop it if we pushed it
            }
        } else
        {
            if (logger.isDebugEnabled())
            {
                logger.debug("Move and wait: {} ==> EDT", AspectUtils.dumpCall(thisJoinPoint));
            }
            try
            {
                // See the comment above in invokeOnEDT about the use of inner classes vs. lambdas in around advice.

                //noinspection Convert2Lambda
                SwingUtilities.invokeAndWait(
                        new Runnable()
                        {
                            @Override
                            public void run()
                            {
                                ClassLoaderUtil.pushContextClassLoader(classLoader);
                                try
                                {
                                    returnValue.set(proceed()); // Invoke the original code with the new TCCL pushed
                                } finally
                                {
                                    ClassLoaderUtil.popContextClassLoader(); // Ensure that we always pop it if we pushed it
                                }
                            }
                        });
            } catch (InterruptedException | InvocationTargetException e)
            {
                Logger logger = AspectUtils.getLogger(thisEnclosingJoinPointStaticPart);
                logger.error("Failure to invoke call on EDT at: {}", thisEnclosingJoinPointStaticPart.getSourceLocation(), e);
            }
        }

        return returnValue.get();
    }
}
