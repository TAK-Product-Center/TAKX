package gov.tak.api.time;

import java.lang.management.ManagementFactory;
import java.lang.management.RuntimeMXBean;

/**
 * Facade class to emulate the {@code SystemClock} class on Android.
 *
 * @since 3.0
 */
public class SystemClock
{
    private static final RuntimeMXBean runtimeMXBean = ManagementFactory.getRuntimeMXBean();

    /**
     * Returns milliseconds since the JVM started.
     *
     * @return elapsed milliseconds since JVM start
     */
    public static long elapsedRealtime()
    {
        return runtimeMXBean.getUptime();
    }
}
