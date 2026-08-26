package gov.takx.api.aspects;

import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.Signature;
import org.aspectj.lang.reflect.CodeSignature;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.invoke.MethodHandles;
import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.Map;

/**
 * Static utility methods supporting aspects.
 *
 * @since 1.25
 */
public class AspectUtils
{
    private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    private static final Map<Class, Logger> loggerMap = new HashMap<>();

    private AspectUtils()
    {
        // Enforce static usage
    }

    /**
     * Create a string representation of the call described in the given join point.
     *
     * @param jp The join point
     * @return String form showing class name, method name, and all argument names and values
     */
    public static String dumpCall(JoinPoint jp)
    {
        final StringBuilder sb = new StringBuilder();

        final Signature signature = jp.getSignature();
        final String className = signature.getDeclaringType().getSimpleName();

        sb.append(className).append('.').append(signature.getName()).append('('); // class.method(

        String[] names = ((CodeSignature) signature).getParameterNames();
        Object[] args = jp.getArgs();

        for (int i = 0; i < args.length; i++)
        {
            if (i > 0) sb.append(", ");
            sb.append(names[i]).append('=').append(args[i]); // argName=value
        }

        sb.append(')');

        return sb.toString();
    }

    /**
     * Inspect the class from the join point and find its logger object. If none is found, use the one defined here.
     *
     * @param joinPoint The join point containing the type to search
     * @return logger to use
     */
    public static Logger getLogger(JoinPoint.StaticPart joinPoint)
    {
        final Class declaringType = joinPoint.getSignature().getDeclaringType();
        Logger theLogger = loggerMap.get(declaringType);

        if (theLogger == null)
        {
            try
            {
                // The logger object must be a static field called logger.
                final Field loggerField = declaringType.getDeclaredField("logger");
                loggerField.setAccessible(true);  // It is normally private, so make it accessible

                final Object loggerObject = loggerField.get(null);

                // Avoid class cast exceptions with other logger types (like slf4j)
                if (loggerObject instanceof Logger)
                {
                    theLogger = (Logger) loggerObject;
                    loggerMap.put(declaringType, theLogger); // Save it in the map
                } else
                {
                    theLogger = logger;
                }
            } catch (NoSuchFieldException e)
            {
                return logger; // Cannot find a logger object, use the internal one.
            } catch (Exception e)
            {
                throw new RuntimeException(e);
            }
        }

        return theLogger;
    }

    /**
     * We want to find the source location of the caller of the currently executing execution join point.  To do this
     * we need to walk up the call stack past the aspect, past the method being executed and, potentially, past any
     * other aspects at work to get to the actual caller frame.
     *
     * @return String form of caller location
     */
    public static String findCallerOfExecutionJoinPoint()
    {
        final StackTraceElement[] stack = Thread.currentThread().getStackTrace();

        // stack[0] is the call to getStackTrace
        // stack[1] is this method
        // stack[2] is our caller (the execution join point)
        // stack[3] is the advised method
        // stack[4] is the earliest possible frame of interest

        String callerLoc = "<unknown>";
        int stackIndex = 4;
        while (stackIndex < stack.length)
        {
            final StackTraceElement frame = stack[stackIndex];
            final String methodName = frame.getMethodName();
            String className = frame.getClassName();

            if (!(className.toLowerCase().contains("ajc") || methodName.contains("around")))
            {
                int idx = className.lastIndexOf('.');
                if (idx > 0) className = className.substring(idx + 1);
                callerLoc = className + ":" + frame.getLineNumber();
                break; // Found it
            }

            stackIndex += 1;
        }

        return callerLoc;
    }
}
